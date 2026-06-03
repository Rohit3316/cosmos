/**
 * Utility class for handling various operations related to rollouts and rollout groups.
 * Provides methods for retrieving, updating, and processing rollouts, rollout groups,
 * and their associated actions. Includes functionality for status updates, validation,
 * and action processing. Designed to work with JPA repositories for database interactions.
 * Utilizes logging for debugging and tracking operations.
 */
package org.eclipse.hawkbit.repository.jpa.utils;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService.LOGGER;

@Slf4j
public class RolloutSchedulerUtils {

    /**
     * Retrieves the rollout groups by rollout ID
     * @param rolloutId
     * @return
     */
    public static List<JpaRolloutGroup> getRolloutGroupsByRolloutId(JpaRollout rolloutId, RolloutGroupRepository rolloutGroupRepository) {
        return rolloutGroupRepository.findByRolloutId(rolloutId.getId());
    }


    public static List<Long> getTargetIdsByRolloutGroupIds(List<Long> rolloutGroupId, RolloutTargetGroupRepository rolloutTargetGroupRepository) {
        return rolloutTargetGroupRepository.getTargetIdsByRolloutGroupIds(rolloutGroupId);
    }

    /**
     * Sets the status of the rollout
     *
     * @param rollout
     */
    public static void updateRolloutStatus(JpaRollout rollout, RolloutStatus status, RolloutRepository rolloutRepository) {
        rollout.setStatus(status);
        rolloutRepository.save(rollout);
        LOGGER.debug("Rollout is set to {} status", status);
    }

    public static void updateRolloutStatusWithLastCheck(JpaRollout jpaRollout, RolloutStatus status, int i, RolloutRepository rolloutRepository) {
        jpaRollout.setStatus(RolloutStatus.RUNNING);
        jpaRollout.setLastCheck(i);
        jpaRollout.setActualStartDate(Instant.now().getEpochSecond());
        rolloutRepository.save(jpaRollout);
        LOGGER.debug("Rollout is set to {} status", status);
    }

    public static void updateRolloutAndSetBoolean(JpaRollout jpaRollout, RolloutStatus status, boolean booleanStatus, RolloutRepository rolloutRepository) {
        jpaRollout.setStatus(status);
        jpaRollout.setDeleted(booleanStatus);
        rolloutRepository.save(jpaRollout);

    }

    public static JpaRollout updateRolloutStateAndSendToKafka(JpaRollout jpaRollout, RolloutStatus status, RolloutRepository rolloutRepository) {
        jpaRollout.setStatus(RolloutStatus.READY);
        JpaRollout savedRollout = rolloutRepository.save(jpaRollout);
        LOGGER.debug("Rollout is set to {} status", status);
        return savedRollout;
    }

    public static void updateRolloutGroupStatus(JpaRolloutGroup rolloutGroup, RolloutGroupStatus status, RolloutGroupRepository rolloutGroupRepository) {
        rolloutGroup.setStatus(status);
        rolloutGroupRepository.save(rolloutGroup);
        LOGGER.debug("Rollout is set to {} status", status);
    }

    public static void updatedRolloutAndGroupsTotalTargetCount(JpaRollout jpaRollout, JpaRolloutGroup jpaRolloutGroup, long updatedTargetCount, long countTargetsOfRolloutGroup, RolloutRepository rolloutRepository , RolloutGroupRepository rolloutGroupRepository) {
        jpaRollout.setTotalTargets(updatedTargetCount);
        jpaRolloutGroup.setTotalTargets((int) countTargetsOfRolloutGroup);
        rolloutGroupRepository.save(jpaRolloutGroup);
    }

    /**
     * Retrieves actions by  rollout group IDs
     *
      * @param rolloutGroupId
     * @param pageRequest
     * @return
     */

    public static Page<JpaAction> getActionsByRolloutGroup(List<Long>rolloutGroupId, PageRequest pageRequest, ActionRepository actionRepository) {
        return actionRepository.findByRolloutGroupIdInAndActive(rolloutGroupId, pageRequest, true);
    }

    /**
     * Sets the status of the rollout groups
     * Sets the status of the rollout groups to the specified new status.
     *
     * @param rolloutGroup
     */
    public static void setRolloutGroupsStatus(List<Long> rolloutGroup, RolloutGroupStatus newStatus, RolloutGroupRepository rolloutGroupRepository) {
        if (!rolloutGroup.isEmpty()) {
            rolloutGroup.forEach(group -> {
                JpaRolloutGroup jpaRolloutGroup = rolloutGroupRepository.findById(group)
                        .orElseThrow(() -> new EntityNotFoundException("RolloutGroup not found"));
                jpaRolloutGroup.setStatus(newStatus);
                rolloutGroupRepository.save(jpaRolloutGroup);
                LOGGER.debug("RolloutGroup status is set to {}", newStatus);
            });
        }
    }

    /**
     * Processes the actions and checks if the actions are in RUNNING and updates it to newStatus
     *
     * @param action
     */
    public static void processActions(JpaAction action, List<DeviceActionStatus> checkStatusIn, DeviceActionStatus setNewStatus, ActionRepository actionRepository, boolean active) {
        if (checkStatusIn.contains(action.getStatus())) {
            log.debug("Updated action status to {}", setNewStatus);
            action.setStatus(setNewStatus);
            action.setActive(active);
            actionRepository.save(action);
        }
    }


    public static boolean checkAllActionsIfCompleted(List<JpaAction> actionIds, List<DeviceActionStatus> deviceStatuses) {
        return actionIds.stream()
                .allMatch(action -> deviceStatuses.contains(action.getStatus()));
    }

    /**
     * This method processes a list of rollout groups by updating the status of their associated actions and the groups themselves to the specified statuses.
     * It retrieves actions for each group, updates their status
     *
     * @param rolloutGroup
     */
    public static void processRolloutGroups(List<Long> rolloutGroup,DeviceActionStatus setDeviceActionStatus, DeviceActionStatus deviceStatus, RolloutGroupStatus groupStatus, RolloutGroupRepository rolloutGroupRepository, ActionRepository actionRepository) {
        rolloutGroup.forEach(groupId -> {
            JpaRolloutGroup group = rolloutGroupRepository.findById(groupId)
                    .orElseThrow(() -> new EntityNotFoundException("RolloutGroup not found"));
            List<JpaAction> actions = actionRepository.getActionsByRolloutGroupId(groupId, true);
            actions.forEach(action -> {
                if (action.getStatus().equals(setDeviceActionStatus)) {
                    action.setStatus(deviceStatus);
                actionRepository.save(action);
                }
            });
            LOGGER.debug("Actions status set to {}", deviceStatus);
            group.setStatus(groupStatus);
            rolloutGroupRepository.save(group);
            LOGGER.debug("RolloutGroup {} status set to", group.getId());
        });
    }

    /**
     * Checks if all rollout groups are in given states or not
     *
     * @param rollout
     * @return
     */
    public static boolean checkIfAllGroupsUpdated(JpaRollout rollout, List<RolloutGroupStatus> groupStatuses, RolloutGroupRepository rolloutGroupRepository) {
        return rolloutGroupRepository.findByRolloutId(rollout.getId()).stream()
                .allMatch(group -> groupStatuses.contains(group.getStatus()));
    }

}
