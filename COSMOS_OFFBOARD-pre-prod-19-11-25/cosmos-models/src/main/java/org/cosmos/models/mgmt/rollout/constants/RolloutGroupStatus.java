package org.cosmos.models.mgmt.rollout.constants;

/**
 * Rollout group state machine.
 */
public enum RolloutGroupStatus {

    /**
     * Group has been defined, but not all targets have been assigned yet.
     */
    CREATING(1),

    /**
     * Group has been defined, but not all targets have been assigned yet.
     */
    DRAFT(1),

    /**
     * Group is freezing from further changes.
     */
    FREEZING(2),

    /**
     * Ready to start the group.
     */
    READY(3),

    /**
     * Group is unfrozen to make any changes.
     */
    UNFREEZING(4),

    /**
     * Group is going to be in queue as rollout moves to running.
     */
    QUEUING(5),

    /**
     * Group is going to be in queued and waiting for other group to finish
     * so that queued group can be started as rollout moves to running.
     */
    QUEUED(6),

    /**
     * Rollout group is starting
     */
    STARTING(7),
    /**
     * Rollout group is running
     */
    RUNNING(8),
    /**
     * Rollout group is being paused
     */
    PAUSING(9),
    /**
     * Rollout group is paused
     */
    PAUSED(10),
    /**
     * Rollout group is resuming
     */
    RESUMING(11),
    /**
     * Rollout group is being cancelled
     */
    CANCELING(12),
    /**
     * Rollout group is cancelled
     */
    CANCELED(13),
    /**
     * Rollout group is finishing
     */
    FINISHING(14),
    /**
     * Rollout group is finished
     */
    FINISHED(15),

    /**
     * Group is finished and has errors.
     */
    ERROR(16),

    /**
     * Rollout group is deleting
     */
    DELETING(17),

    /**
     * Rollout group is deleted
     */
    DELETED(18),

    /**
     * Rollout group is retrying
     */
    RETRYING(19),

    /**
     * Rollout group is retried
     */
    RETRY(20);


    private final Integer value;

    RolloutGroupStatus(Integer value) {
        this.value = value;
    }

    public int getValue(){ return value; }
}
