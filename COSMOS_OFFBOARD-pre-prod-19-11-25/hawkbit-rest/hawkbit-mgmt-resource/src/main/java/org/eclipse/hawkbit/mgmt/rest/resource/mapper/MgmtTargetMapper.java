/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import java.time.ZoneId;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.cosmos.models.mgmt.MgmtMaintenanceWindow;
import org.cosmos.models.mgmt.MgmtMetadata;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.action.dto.MgmtAction;
import org.cosmos.models.mgmt.action.dto.MgmtActionStatus;
import org.cosmos.models.mgmt.target.dto.MgmtPollStatus;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.target.dto.MgmtTargetAutoConfirm;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.pagination.SortDirection;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtTargetMapper {

    private MgmtTargetMapper() {
        // Utility class
    }

    /**
     * Add links to a target response.
     *
     * @param response the target response
     */
    public static void addTargetLinks(final MgmtTarget response, final Long tenantId) {
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAttributes(response.getControllerId(), tenantId))
                .withRel(MgmtRestConstants.TARGET_V1_ATTRIBUTES).expand());
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAllActions(response.getControllerId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionFields.ID.getFieldName() + ":" + SortDirection.DESC, null, tenantId))
                .withRel(MgmtRestConstants.TARGET_V1_ACTIONS).expand());
        response.add(linkTo(methodOn(MgmtTargetRestApi.class).getMetadata(response.getControllerId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null, tenantId)).withRel("metadata")
                .expand());
        if (response.getTargetType() != null) {
            response.add(linkTo(methodOn(MgmtTargetTypeRestApi.class).getTargetType(response.getTargetType(), tenantId))
                    .withRel("targetType").expand());
        }
        if (response.getAutoConfirmActive() != null) {
            response.add(linkTo(methodOn(MgmtTargetRestApi.class).getAutoConfirmStatus(response.getControllerId(), tenantId))
                    .withRel("autoConfirm").expand());
        }
    }

    public static MgmtTargetAutoConfirm getTargetAutoConfirmResponse(final Target target, final Long tenantId) {
        final AutoConfirmationStatus status = target.getAutoConfirmationStatus();
        final MgmtTargetAutoConfirm response;
        if (status != null) {
            response = MgmtTargetAutoConfirm.active(status.getActivatedAt());
            response.setInitiator(status.getInitiator());
            response.setRemark(status.getRemark());
            response.add(linkTo(methodOn(MgmtTargetRestApi.class).deactivateAutoConfirm(target.getControllerId(), tenantId))
                    .withRel(MgmtRestConstants.TARGET_V1_DEACTIVATE_AUTO_CONFIRM).expand());
        } else {
            response = MgmtTargetAutoConfirm.disabled();
            response.add(linkTo(methodOn(MgmtTargetRestApi.class).activateAutoConfirm(target.getControllerId(), null, tenantId))
                    .withRel(MgmtRestConstants.TARGET_V1_ACTIVATE_AUTO_CONFIRM).expand());
        }
        return response;
    }

    public static void addPollStatus(final Target target, final MgmtTarget targetRest) {
        final PollStatus pollStatus = target.getPollStatus();
        if (pollStatus != null) {
            final MgmtPollStatus pollStatusRest = new MgmtPollStatus();
            pollStatusRest.setLastRequestAt(
                    Date.from(pollStatus.getLastPollDate().atZone(ZoneId.of("UTC")).toInstant()).getTime());
            pollStatusRest.setNextExpectedRequestAt(
                    Date.from(pollStatus.getNextPollDate().atZone(ZoneId.of("UTC")).toInstant()).getTime());
            pollStatusRest.setOverdue(pollStatus.isOverdue());
            targetRest.setPollStatus(pollStatusRest);
        }
    }

    /**
     * Create a response for targets.
     *
     * @param targets list of targets
     * @return the response
     */
    public static List<MgmtTarget> toResponse(final Collection<Target> targets, final TenantConfigHelper configHelper, final Long tenantId) {
        if (targets == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                targets.stream().map(target -> toResponse(target, configHelper, tenantId)).collect(Collectors.toList()));
    }

    /**
     * Create a response for target.
     *
     * @param target the target
     * @return the response
     */
    public static MgmtTarget toResponse(final Target target, final TenantConfigHelper configHelper, final long tenantId) {
        if (target == null) {
            return null;
        }
        final MgmtTarget targetRest = new MgmtTarget();
        targetRest.setControllerId(target.getControllerId());
        targetRest.setDescription(target.getDescription());
        targetRest.setName(target.getName());
        targetRest.setVin(target.getVin());
        targetRest.setSerialNumber(target.getSerialNumber());
        targetRest.setUpdateStatus(target.getUpdateStatus().name().toLowerCase());
        targetRest.setVehicleModelId(target.getVehicleModelId());
        targetRest.setTargetId(target.getId());
        targetRest.setCreatedBy(target.getCreatedBy());
        targetRest.setLastModifiedBy(target.getLastModifiedBy());

        targetRest.setCreatedAt(target.getCreatedAt());
        targetRest.setLastModifiedAt(target.getLastModifiedAt());

        targetRest.setSecurityToken(target.getSecurityToken());
        targetRest.setRequestAttributes(target.isRequestControllerAttributes());

        // last target query is the last controller request date
        final Long lastTargetQuery = target.getLastTargetQuery();
        final Long installationDate = target.getInstallationDate();

        if (lastTargetQuery != null) {
            targetRest.setLastControllerRequestAt(lastTargetQuery);
        }
        if (installationDate != null) {
            targetRest.setInstalledAt(installationDate);
        }
        if (target.getTargetType() != null) {
            targetRest.setTargetType(target.getTargetType().getId());
            targetRest.setTargetTypeName(target.getTargetType().getName());
        }
        if (configHelper.isConfirmationFlowEnabled()) {
            targetRest.setAutoConfirmActive(target.getAutoConfirmationStatus() != null);
        }

        targetRest.add(
                linkTo(methodOn(MgmtTargetRestApi.class).getTarget(target.getControllerId(), tenantId)).withSelfRel().expand());

        return targetRest;
    }

    public static List<TargetCreate> fromRequest(final EntityFactory entityFactory,
                                          final Collection<MgmtTargetRequestBody> targetsRest) {
        if (targetsRest == null) {
            return Collections.emptyList();
        }

        return targetsRest.stream().map(targetRest -> fromRequest(entityFactory, targetRest))
                .collect(Collectors.toList());
    }

    private static TargetCreate fromRequest(final EntityFactory entityFactory, final MgmtTargetRequestBody targetRest) {

        String controllerId = String.join("_",targetRest.getVin(),targetRest.getSerialNumber());

        return entityFactory.target().create().controllerId(controllerId).name(targetRest.getName())
                .description(targetRest.getDescription()).securityToken(targetRest.getSecurityToken())
                .targetType(targetRest.getTargetType()).serialNumber(targetRest.getSerialNumber())
                .vehicleModelId(targetRest.getVehicleModelId()).vin(targetRest.getVin());
    }

    public static List<MetaData> fromRequestTargetMetadata(final List<MgmtMetadata> metadata,
                                                    final EntityFactory entityFactory) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream().map(
                        metadataRest -> entityFactory.generateTargetMetadata(metadataRest.getKey(), metadataRest.getValue()))
                .collect(Collectors.toList());
    }

    public static List<MgmtActionStatus> toActionStatusRestResponse(final Collection<ActionStatus> actionStatus,
                                                             final DeploymentManagement deploymentManagement) {
        if (actionStatus == null) {
            return Collections.emptyList();
        }

        return actionStatus.stream()
                .map(status -> toResponse(status,
                        deploymentManagement.findMessagesByActionStatusId(
                                        PageRequest.of(0, MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT), status.getId())
                                .getContent()))
                .collect(Collectors.toList());
    }

    public static MgmtAction toResponse(final String targetId, final Action action, final long tenantId, final DeploymentManagement deploymentManagement) {
        final MgmtAction result = new MgmtAction();

        result.setActionId(action.getId());
        result.setForceTime(action.getForcedTime());
        action.getWeight().ifPresent(result::setWeight);
        result.setUserAcceptanceRequired(action.getUserAcceptanceRequired());
        result.setStatus(action.getStatus().toString().toLowerCase());

        Page<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(Pageable.unpaged(), action.getId());
        result.setActionStatusCount(actionStatus.getSize());

        action.getLastActionStatusCode().ifPresent(result::setLastStatusCode);

        final Rollout rollout = action.getRollout();
        if (rollout != null) {
            result.setRollout(rollout.getId());
            result.setRolloutName(rollout.getName());
            result.setRolloutGroupId(action.getRolloutGroup().getId());
            result.setTargetId(action.getTarget().getId());
        }

        if (action.hasMaintenanceSchedule()) {
            final MgmtMaintenanceWindow maintenanceWindow = new MgmtMaintenanceWindow();
            maintenanceWindow.setSchedule(action.getMaintenanceWindowSchedule());
            maintenanceWindow.setDuration(action.getMaintenanceWindowDuration());
            maintenanceWindow.setTimezone(action.getMaintenanceWindowTimeZone());

            action.getMaintenanceWindowStartTime()
                    .ifPresent(nextStart -> maintenanceWindow.setNextStartAt(nextStart.toInstant().getEpochSecond()));
            result.setMaintenanceWindow(maintenanceWindow);
        }

        MgmtRestModelMapper.mapBaseToBase(result, action);

        result.add(
                linkTo(methodOn(MgmtTargetRestApi.class).getAction(targetId, action.getId(), tenantId)).withSelfRel().expand());

        if (action.isDeploymentLogAvailable()) {
            result.add(linkTo(methodOn(MgmtActionRestApi.class).getActionsDeploymentLogs(action.getTarget().getControllerId(), action.getId(), tenantId))
                    .withRel(MgmtRestConstants.DEPLOYMENT_LOGS_LINK).expand());
        }

        return result;
    }

    public static MgmtAction toResponseWithLinks(final String controllerId, final Action action, final Long tenantId, final DeploymentManagement deploymentManagement) {
        final MgmtAction result = toResponse(controllerId, action, tenantId, deploymentManagement);

        if (action.isCancelingOrCanceled()) {
            result.add(linkTo(methodOn(MgmtTargetRestApi.class).getAction(controllerId, action.getId(), tenantId))
                    .withRel(MgmtRestConstants.TARGET_V1_CANCELED_ACTION).expand());
        }

        result.add(linkTo(methodOn(MgmtTargetRestApi.class).getTarget(controllerId, tenantId)).withRel("target")
                .withName(action.getTarget().getName()).expand());

        final DistributionSet distributionSet = action.getDistributionSet();
        result.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(tenantId, distributionSet.getId()))
                .withRel("distributionset").withName(distributionSet.getName() + ":" + distributionSet.getVersion())
                .expand());

        result.add(linkTo(methodOn(MgmtTargetRestApi.class).getActionStatusList(controllerId, action.getId(), 0,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE,
                ActionStatusFields.ID.getFieldName() + ":" + SortDirection.DESC, tenantId))
                .withRel(MgmtRestConstants.TARGET_V1_ACTION_STATUS).expand());

        final Rollout rollout = action.getRollout();
        if (rollout != null) {
            result.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRollout(tenantId, rollout.getId()))
                    .withRel(MgmtRestConstants.TARGET_V1_ROLLOUT).withName(rollout.getName()).expand());
        }

        return result;
    }

    public static List<MgmtAction> toResponse(final String targetId, final Collection<Action> actions, final Long tenantId, final DeploymentManagement deploymentManagement) {
        if (actions == null) {
            return Collections.emptyList();
        }

        return actions.stream().map(action -> toResponse(targetId, action, tenantId, deploymentManagement)).collect(Collectors.toList());
    }


    private static MgmtActionStatus toResponse(final ActionStatus actionStatus, final List<String> messages) {
        final MgmtActionStatus result = new MgmtActionStatus();

        result.setMessages(messages);
        result.setReportedAt(actionStatus.getCreatedAt());
        result.setStatusId(actionStatus.getId());
        result.setType(actionStatus.getStatus().name().toLowerCase());
        actionStatus.getCode().ifPresent(result::setCode);

        return result;
    }

    public static MgmtMetadata toResponseTargetMetadata(final TargetMetadata metadata) {
        final MgmtMetadata metadataRest = new MgmtMetadata();
        metadataRest.setKey(metadata.getKey());
        metadataRest.setValue(metadata.getValue());
        return metadataRest;
    }

    public static List<MgmtMetadata> toResponseTargetMetadata(final List<TargetMetadata> metadata) {
        return metadata.stream().map(MgmtTargetMapper::toResponseTargetMetadata).collect(Collectors.toList());
    }
}
