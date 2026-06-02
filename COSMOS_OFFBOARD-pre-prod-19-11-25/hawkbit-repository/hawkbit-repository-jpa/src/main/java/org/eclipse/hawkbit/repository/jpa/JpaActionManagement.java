/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.CANCELED;
import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.DD_SENT;
import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.ERROR_RESPONSE_CODE;
import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.FINISHED_FAILURE;
import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.FINISHED_NOT_EXECUTED;
import static org.cosmos.models.mgmt.device.constants.DeviceActionStatus.FINISHED_SUCCESS;

/**
 * Implements utility methods for managing {@link Action}s
 */
public class JpaActionManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaActionManagement.class);
    private static final List<DeviceActionStatus> COMPLETED_ACTION_STATUSES =
            List.of(FINISHED_SUCCESS, FINISHED_FAILURE, CANCELED);

    protected final ActionRepository actionRepository;
    protected final ActionStatusRepository actionStatusRepository;
    protected final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository;
    protected final QuotaManagement quotaManagement;
    protected final RepositoryProperties repositoryProperties;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected JpaActionManagement(final ActionRepository actionRepository,
                                  final ActionStatusRepository actionStatusRepository,
                                  final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository,
                                  final QuotaManagement quotaManagement,
                                  final RepositoryProperties repositoryProperties) {
        this.actionRepository = actionRepository;
        this.actionStatusRepository = actionStatusRepository;
        this.actionStatusUserAcceptanceRepository = actionStatusUserAcceptanceRepository;
        this.quotaManagement = quotaManagement;
        this.repositoryProperties = repositoryProperties;
    }

    protected List<Action> findActiveActionsWithHighestWeightConsideringDefault(final String controllerId,
                                                                                final int maxActionCount) {
        if (!actionRepository.activeActionExistsForControllerId(controllerId)) {
            return Collections.emptyList();
        }
        List<JpaAction> ddSentActions = findActiveActionsHavingStatus(controllerId, DeviceActionStatus.DD_SENT);
        if (!ddSentActions.isEmpty()) {
            return ddSentActions.stream()
                    .limit(maxActionCount)
                    .collect(Collectors.toList());
        }
        final List<Action> actions = new ArrayList<>();
        final PageRequest pageable = PageRequest.of(0, maxActionCount);
        List<Action> actionsWithWeight = actionRepository
                .findByTargetControllerIdAndActiveIsTrueAndWeightIsNotNullAndStatusNotInOrderByWeightDescIdAsc(
                        pageable, controllerId, COMPLETED_ACTION_STATUSES)
                .getContent();

        List<Action> actionsWithoutWeight = actionRepository
                .findByTargetControllerIdAndActiveIsTrueAndWeightIsNullAndStatusNotInOrderByIdAsc(
                        pageable, controllerId, COMPLETED_ACTION_STATUSES)
                .getContent();

        actions.addAll(actionsWithWeight);
        actions.addAll(actionsWithoutWeight);

        final Comparator<Action> actionImportance = Comparator.comparingInt(this::getWeightConsideringDefault)
                .reversed().thenComparing(Action::getId);
        return actions.stream().sorted(actionImportance).limit(maxActionCount).collect(Collectors.toList());
    }

    protected List<JpaAction> findActiveActionsHavingStatus(final String controllerId, final DeviceActionStatus status) {
        if (!actionRepository.activeActionExistsForControllerId(controllerId)) {
            return Collections.emptyList();
        }
        return Collections
                .unmodifiableList(actionRepository.findByTargetIdAndIsActiveAndActionStatus(controllerId, status));
    }

    protected Action addActionStatus(final JpaActionStatusCreate statusCreate, DdiStatus ddiStatus) {
        final Long actionId = statusCreate.getActionId();
        final JpaActionStatus actionStatus = statusCreate.build();
        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);
        if (actionStatus.getStatus().equals(FINISHED_SUCCESS) || actionStatus.getStatus().equals(FINISHED_FAILURE)) {
            action.setStatus(statusCreate.build().getStatus());
            action.setActive(false);
        }
        if (actionStatus.getStatus().equals(CANCELED) || actionStatus.getStatus().equals(DD_SENT)) {
            if (!action.getStatus().equals(CANCELED)) {
                action.setStatus(actionStatus.getStatus());
                actionRepository.save(action);
            } else {
                LOG.info("Action {} already canceled ", actionId);
            }
        }
        if (isUpdatingActionStatusAllowed(action, actionStatus)) {
            return handleAddUpdateActionStatus(actionStatus, action, ddiStatus);
        }

        LOG.debug("Update of actionStatus {} for action {} not possible since action not active anymore.",
                actionStatus.getStatus(), action.getId());
        return action;
    }

    /**
     * ActionStatus updates are allowed mainly if the action is active. If the
     * action is not active we accept further status updates if permitted so by
     * repository configuration. In this case, only the values: Status.ERROR and
     * Status.FINISHED are allowed. In the case of a DOWNLOAD_ONLY action, we accept
     * status updates only once.
     */
    protected boolean isUpdatingActionStatusAllowed(final JpaAction action, final JpaActionStatus actionStatus) {

        final boolean isIntermediateFeedback = (FINISHED_FAILURE != actionStatus.getStatus()) &&
                (FINISHED_SUCCESS != actionStatus.getStatus()) &&
                (FINISHED_NOT_EXECUTED != actionStatus.getStatus()) &&
                (ERROR_RESPONSE_CODE != actionStatus.getStatus());

        final boolean isAllowedByRepositoryConfiguration = !repositoryProperties.isRejectActionStatusForClosedAction()
                && isIntermediateFeedback;


//        return action.isActive() || isAllowedByRepositoryConfiguration;
        return true; // TODO: Fix this once the flow is ready
    }

    protected int getWeightConsideringDefault(final Action action) {
        return action.getWeight().orElse(repositoryProperties.getActionWeightIfAbsent());
    }

    protected JpaAction getActionAndThrowExceptionIfNotFound(final Long actionId) {
        return actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
    }


    /**
     * Sets {@link TargetUpdateStatus} based on given {@link ActionStatus}.
     */
    protected Action handleAddUpdateActionStatus(final JpaActionStatus actionStatus, final JpaAction action, final DdiStatus ddiStatus) {
        // information status entry - check for a potential DOS attack
        assertActionStatusQuota(action);
        assertActionStatusMessageQuota(actionStatus);
        actionStatus.setAction(action);
        if(ddiStatus != null && ddiStatus.getDownload() != null) {
            try {
                String downloadJson = objectMapper.writeValueAsString(ddiStatus.getDownload());
                actionStatus.setDownloadProgress(downloadJson);
            } catch (JsonProcessingException e) {
                LOG.error("Failed to serialize DdiDownload to JSON", e);
            }
        }

        onActionStatusUpdate(actionStatus.getStatus(), action);

        actionStatusRepository.save(actionStatus);

        action.setLastActionStatusCode(actionStatus.getCode().orElse(null));
        return actionRepository.save(action);
    }

    protected void onActionStatusUpdate(final DeviceActionStatus updatedActionStatus, final JpaAction action) {
        // can be overwritten to intercept the persistence of the action status
    }

    protected void assertActionStatusQuota(final JpaAction action) {
        QuotaHelper.assertAssignmentQuota(action.getId(), 1, quotaManagement.getMaxStatusEntriesPerAction(),
                ActionStatus.class, Action.class, actionStatusRepository::countByActionId);
    }

    protected void assertActionStatusMessageQuota(final JpaActionStatus actionStatus) {
        QuotaHelper.assertAssignmentQuota(actionStatus.getId(), actionStatus.getMessages().size(),
                quotaManagement.getMaxMessagesPerActionStatus(), "Message", ActionStatus.class.getSimpleName(), null);
    }
}
