package org.eclipse.hawkbit.repository.jpa.service;

import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class to handle rollout operations in Async mode
 */

@Service
@Slf4j
public class RolloutAsyncService {

    @Autowired
    private StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction;

    /**
     * Starts all rollout groups in an async mode.
     * <p>
     * This method filters the rollout groups to get only those that are in the QUEUED status,
     * sorts them by their ID in descending order, and then triggers the start of the next group
     * in the sorted order.
     * </p>
     * <p>
     * If an exception occurs during the async processing, the transaction is rolled back.
     * </p>
     *
     * @param rollout   The rollout entity whose groups need to be started
     * @param allGroups The list of rollout groups associated with the rollout
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startAllGroupsAsync(Rollout rollout, List<RolloutGroup> allGroups) {
        try {
            final List<JpaRolloutGroup> queuedGroups = allGroups.stream()
                    .map(JpaRolloutGroup.class::cast)
                    .sorted(Comparator.comparingLong(RolloutGroup::getId).reversed())
                    .filter(g -> RolloutGroupStatus.QUEUED.equals(g.getStatus()))
                    .toList();

            log.debug("Starting of the Rollout Groups in Async mode");
            startNextRolloutGroupAction.startAllChildGroups(rollout, queuedGroups);
        } catch (Exception e) {
            log.error("Error in starting async rollout group processing for rollout: {}", rollout.getId(), e);
            throw e; // This will trigger transaction rollback
        }
    }
}