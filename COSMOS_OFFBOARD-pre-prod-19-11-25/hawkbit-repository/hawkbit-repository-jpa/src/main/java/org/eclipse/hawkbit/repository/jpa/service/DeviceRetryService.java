package org.eclipse.hawkbit.repository.jpa.service;

import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService.LOGGER;

/**
 * Service to handle device retries within rollouts.
 */
@Service
@Slf4j
public class DeviceRetryService {

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private EntityManager entityManager;

    /**
     * Safely retries a single device update in its own transaction.
     * Uses pessimistic locking to avoid concurrent modifications.
     */

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateActionAndTargetGroup(Long actionId, RetryMode retryMode) {
        LOGGER.debug("Starting atomic update for action {}", actionId);
        JpaAction managedAction = actionRepository.findByIdWithAssociations(actionId)
                .orElse(null);

        if (managedAction == null || !managedAction.isActive()) {
            LOGGER.warn("Action {} not found or already inactive, skipping update.", actionId);
            return;
        }

        JpaRolloutGroup group = (JpaRolloutGroup) managedAction.getRolloutGroup();
        JpaTarget target = (JpaTarget) managedAction.getTarget();

        if (group == null || target == null) {
            throw new IllegalStateException("Action " + actionId + " has missing RolloutGroup or Target association.");
        }

        LOGGER.debug("Group ID: {} | Target ID: {}", group.getId(), target.getId());
        RolloutTargetGroup managedTargetGroup = rolloutTargetGroupRepository
                .findByRolloutGroupAndTarget(group.getId(), target.getId())
                .orElseThrow(() -> new IllegalStateException("Missing RolloutTargetGroup (RTG) mapping for Action " + actionId));

        managedTargetGroup.setRetryEnabled(true);
        managedTargetGroup.setDeviceRetryCount(managedTargetGroup.getDeviceRetryCount() + 1);
        entityManager.merge(managedTargetGroup);

        LOGGER.debug("Successfully updated RollouttargetGroup", managedTargetGroup.getId());

        actionRepository.updateActiveStatus(actionId);
        LOGGER.info("Successfully updated Action {} and RTG in new transaction.", actionId);
    }

    /**
     * Moves the rollout to RETRY status and updates relevant rollout groups.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void moveRolloutToRetryStatus(Long rolloutId, int updatedActionCount) {
        LOGGER.info("Finalizing rollout {} status transition. Updated {} actions.", rolloutId, updatedActionCount);

        JpaRollout managedRollout = rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(JpaRollout.class, rolloutId));

        final Integer NEW_STATUS = RolloutGroupStatus.RETRYING.getValue();
        final Integer FINISHED_STATUS = RolloutGroupStatus.FINISHED.getValue();
        final Integer CANCELED_STATUS = RolloutGroupStatus.CANCELED.getValue();
        final Integer FINISHING_STATUS = RolloutGroupStatus.FINISHING.getValue();
        int groupsUpdated = 0;

        groupsUpdated += rolloutGroupRepository.updateGroupsStatusToOneStatus(rolloutId, NEW_STATUS, FINISHED_STATUS);
        groupsUpdated += rolloutGroupRepository.updateGroupsStatusToOneStatus(rolloutId, NEW_STATUS, CANCELED_STATUS);
        groupsUpdated += rolloutGroupRepository.updateGroupsStatusToOneStatus(rolloutId, NEW_STATUS, FINISHING_STATUS);

        LOGGER.info("Rollout {} moved {} groups from FINISHED/CANCELED/FINISHING to RETRYING.", rolloutId, groupsUpdated);

        final Integer FINAL_GROUP_STATUS = RolloutGroupStatus.RETRY.getValue();
        int groupsFinalized = rolloutGroupRepository.updateGroupsStatusToOneStatus(rolloutId, FINAL_GROUP_STATUS, NEW_STATUS);


        LOGGER.info("Rollout {} moved {} groups from RETRYING to RETRY.", rolloutId, groupsFinalized);

        managedRollout.setStatus(RolloutStatus.RETRY);
        LOGGER.info("Rollout {} successfully moved to RETRY status.", rolloutId);
    }

    /**
     * Determines the action statuses that should be retried based on the specified retry mode.
     *
     * @param retryMode the retry mode to evaluate
     * @return a list of action status values to be retried
     */
    public List<Integer> getActionStatusesToRetry(RetryMode retryMode) {
        switch (retryMode) {
            case ALL_FAILED_VEHICLES:
                return List.of(DeviceActionStatus.FINISHED_FAILURE.getValue(),
                        DeviceActionStatus.FINISHING_FAILURE.getValue());
            case ALL_CANCELED_VEHICLES:
                return List.of(DeviceActionStatus.CANCELED.getValue());
            case ALL_NOT_EXECUTED_VEHICLES:
                return List.of(DeviceActionStatus.FINISHED_NOT_EXECUTED.getValue());
            default:
                return Collections.emptyList();
        }
    }
}