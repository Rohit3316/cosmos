package org.cosmos.models.mgmt.rollout.constants;

import lombok.Getter;

/**
 * Enum representing the various statuses a rollout can have.
 */
@Getter
public enum RolloutStatus {

    DRAFT(1),          // Rollout is in draft mode
    FREEZING(2),       // Rollout is being frozen
    READY(3),          // Rollout is ready
    UNFREEZING(4),     // Rollout is being unfrozen
    DELETING(5),       // Rollout is being deleted
    STARTING(6),       // Rollout is starting
    RUNNING(7),        // Rollout is running
    PAUSING(8),        // Rollout is being paused
    PAUSED(9),         // Rollout is paused
    RESUMING(10),      // Rollout is resuming
    CANCELING(11),    // Rollout is being cancelled
    CANCELED(12),      // Rollout is cancelled
    FINISHING(13),     // Rollout is finishing
    FINISHED(14),      // Rollout is finished
    RETRYING(15),      // Rollout is retrying
    ERROR(16),
    DELETED(17),
    CLONING(18),     // Rollout is being cloned
    RETRY(19);

    private final Integer value;

    RolloutStatus(Integer value) {
        this.value = value;
    }

}