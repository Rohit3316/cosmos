package org.cosmos.models.mgmt.device.constants;

/**
 * Device Action status as reported by the controller.
 * <p>
 * Be aware that JPA is persisting the ordinal number of the enum by means
 * the ordered number in the enum. So don't re-order the enums within the
 * Status enum declaration!
 */

public enum DeviceActionStatus {

    /**
     * Action is being started for the given target.
     */
    STARTING(0),
    /**
     * Action is still running for the given target.
     */
    RUNNING(1),
    /**
     * Action is being paused for the given target.
     */
    PAUSING(2),
    /**
     * Action is paused for the given target.
     */
    PAUSED(3),
    /**
     * Action is being resumed for the given target.
     */
    RESUMING(3),
    /**
     * Action is being cancelled for the given target.
     */
    CANCELING(4),
    /**
     * Action is cancelled for the given target.
     */
    CANCELED(5),
    /**
     * Action is being retrying for the given target.
     */
    RETRYING(6),

    /**
     * Action has been sent for deployment.
     */
    DD_SENT(7),

    /**
     * Action has been accepted for deployment.
     */
    DD_ACCEPTED(8),

    /**
     * Action download has started.
     */
    DOWNLOAD_STARTED(9),

    /**
     * Action download is in progress.
     */
    DOWNLOAD_IN_PROGRESS(10),

    /**
     * User has scheduled the installation for the given action
     */
    USER_SCHEDULED(11),

    /**
     * User has accepted the installation for the given action
     */
    USER_ACCEPTED(12),

    /**
     * User has ignored the installation for the given action
     */
    USER_IGNORED(13),

    /**
     * Action is finishing successfully.
     */
    FINISHING_SUCCESS(14),

    /**
     * Action is finishing with failure.
     */
    FINISHING_FAILURE(15),

    /**
     * Action has finished successfully.
     */
    FINISHED_SUCCESS(16),

    /**
     * Action has finished with failure.
     */
    FINISHED_FAILURE(17),

    /**
     * Log upload is in progress.
     */
    LOG_UPLOAD_IN_PROGRESS(18),

    /**
     * Log upload was successful.
     */
    LOG_UPLOAD_SUCCESS(19),

    /**
     * Log upload failed.
     */
    LOG_UPLOAD_FAILURE(20),
    /**
     * FINISHING but not executed
     */
    FINISHING_NOT_EXECUTED(21),
    /**
     * FINISHED but not executed
     */
    FINISHED_NOT_EXECUTED(22),
    /**
     * Action was canceled and accepted.
     */
    CANCELED_ACCEPT(23),

    /**
     * Action was canceled and rejected.
     */
    CANCELED_REJECT(24),

    /**
     * Action download completed successfully.
     */
    DOWNLOAD_COMPLETED(25),

    /**
     * Action encountered an error response code.
     */
    ERROR_RESPONSE_CODE(26),

    /**
     * Action is pending logs.
     */
    PENDING_LOGS(27);

    private final Integer value;

    DeviceActionStatus(Integer value) {
        this.value = value;
    }
    
    public int getValue(){ return value; }
}
