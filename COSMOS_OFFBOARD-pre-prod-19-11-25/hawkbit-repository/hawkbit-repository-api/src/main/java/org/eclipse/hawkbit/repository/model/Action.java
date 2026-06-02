/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import jakarta.validation.constraints.NotEmpty;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;

/**
 * Update operations to be executed by the target.
 */
public interface Action extends TenantAwareBaseEntity {

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_SCHEDULE_LENGTH = 128;

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_DURATION_LENGTH = 16;

    /**
     * Maximum length of controllerId.
     */
    int MAINTENANCE_WINDOW_TIMEZONE_LENGTH = 8;

    /**
     * Maximum length of external reference.
     */
    int EXTERNAL_REF_MAX_LENGTH = 512;

    /**
     * Minimum weight to indicate the priority of {@link Action}.
     */
    int WEIGHT_MIN = 0;
    /**
     * Maximum weight to indicate the priority of {@link Action}.
     */
    int WEIGHT_MAX = 1000;

    /**
     * @return the distributionSet
     */
    DistributionSet getDistributionSet();

    /**
     * @return <code>true</code> when action is in state
     * {@link DeviceActionStatus#CANCELING} or {@link DeviceActionStatus#CANCELED}, false
     * otherwise
     */
    default boolean isCancelingOrCanceled() {
        return DeviceActionStatus.CANCELING == getStatus() || DeviceActionStatus.CANCELED == getStatus();
    }

    /**
     * @return current {@link DeviceActionStatus} of the {@link Action}.
     */
    DeviceActionStatus getStatus();

    void setStatus(DeviceActionStatus status);

    /**
     * @return <code>true</code> if {@link Action} is still running.
     */
    boolean isActive();

    /**
     * @return the {@link MgmtRolloutUserAcceptanceRequired}
     */
    MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired();

    /**
     * @return {@link Target} of this {@link Action}.
     */
    Target getTarget();

    long getForcedTime();

    /**
     * @return priority of the {@link Action}.
     */
    Optional<Integer> getWeight();

    /**
     * @return rolloutGroup related to this {@link Action}.
     */
    RolloutGroup getRolloutGroup();

    /**
     * @return rollout related to this {@link Action}.
     */
    Rollout getRollout();

    /**
     * @return maintenance window schedule related to this {@link Action}.
     */
    String getMaintenanceWindowSchedule();

    /**
     * @return maintenance window duration related to this {@link Action}.
     */
    String getMaintenanceWindowDuration();

    /**
     * @return maintenance window time zone related to this {@link Action}.
     */
    String getMaintenanceWindowTimeZone();

    /**
     * @return externalRef of the action
     */
    String getExternalRef();

    /**
     * @param externalRef associated with this action
     */
    void setExternalRef(@NotEmpty String externalRef);

    /**
     * @return the username that initiated this action (directly or indirectly)
     */
    String getInitiatedBy();

    /**
     * @return the latest action status code. Performance optimization to not
     * query the action status table for the last action status code.
     */
    Optional<Integer> getLastActionStatusCode();

    /**
     * Checks if user acceptance is not required for the action.
     *
     * @return true if user acceptance is not required, false otherwise.
     */
    default boolean isUserAcceptanceNotRequired() {
        return MgmtRolloutUserAcceptanceRequired.NO == getUserAcceptanceRequired();
    }

    /**
     * Returns the start time of next available maintenance window for the
     * {@link Action} as {@link ZonedDateTime}. If a maintenance window is
     * already active, the start time of currently active window is returned.
     *
     * @return the start time as { @link Optional<ZonedDateTime>}.
     */
    Optional<ZonedDateTime> getMaintenanceWindowStartTime();

    /**
     * The method checks whether the action has a maintenance schedule defined
     * for it. A maintenance schedule defines a set of maintenance windows
     * during which actual update can be performed. A valid schedule defines at
     * least one maintenance window.
     *
     * @return true if action has a maintenance schedule, else false.
     */
    boolean hasMaintenanceSchedule();

    /**
     * The method checks whether the maintenance schedule has already lapsed for
     * the action, i.e. there are no more windows available for maintenance.
     * Controller manager uses the method to check if the maintenance schedule
     * has lapsed, and automatically cancels the action if it is lapsed.
     *
     * @return true if maintenance schedule has lapsed, else false.
     */
    boolean isMaintenanceScheduleLapsed();

    /**
     * The method checks whether a maintenance window is available for the
     * action to proceed. If it is available, a 'true' value is returned. The
     * maintenance window is considered available: 1) If there is no maintenance
     * schedule at all, in which case device can start update any time after
     * download is finished; or 2) the current time is within a scheduled
     * maintenance window start and end time.
     *
     * @return true if maintenance window is available, else false.
     */
    boolean isMaintenanceWindowAvailable();

    /**
     * Checks if the action is waiting for confirmation.
     *
     * @return true if the action is waiting for confirmation, else false
     */
    boolean isWaitingConfirmation();

    /**
     * check if the action has deployment logs
     *
     * @return true if action has deployment logs available, else false
     */
    boolean isDeploymentLogAvailable();

    /**
     * Get the action's {@link DeploymentLog}
     *
     * @return Set of {@link DeploymentLog}
     */
    Set<DeploymentLog> getDeploymentLogs();

    List<ActionStatus> getActionStatus();

}
