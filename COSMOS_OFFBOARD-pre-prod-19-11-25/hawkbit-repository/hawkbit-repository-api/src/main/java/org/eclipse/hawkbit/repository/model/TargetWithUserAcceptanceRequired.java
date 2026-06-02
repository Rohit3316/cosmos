/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.eclipse.hawkbit.repository.exception.InvalidMaintenanceScheduleException;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Objects;

/**
 * A custom view on {@link Target} with {@link MgmtRolloutUserAcceptanceRequired}.
 *
 */
public class TargetWithUserAcceptanceRequired {
    private final String controllerId;
    private final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;
    private final long forceTime;
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private final Integer weight;
    private String maintenanceSchedule;
    private String maintenanceWindowDuration;
    private String maintenanceWindowTimeZone;
    private final boolean confirmationRequired;

    /**
     * Constructor that uses {@link MgmtRolloutUserAcceptanceRequired#NO}
     * 
     * @param controllerId
     *            ID if the controller
     */
    public TargetWithUserAcceptanceRequired(final String controllerId) {
        this(controllerId, MgmtRolloutUserAcceptanceRequired.YES, 0, null, false);
    }

    /**
     * Constructor that leaves the maintenance info empty
     *
     * @param controllerId
     *            for which the action is created.
     * @param userAcceptanceRequired
     *            specified for the action.
     * @param forceTime
     *            after that point in time the action is exposed as forced
     * @param weight
     *            the priority of an {@link Action}
     * @param confirmationRequired
     *            sets the confirmation required flag when starting the
     *            {@link Action}
     */
    public TargetWithUserAcceptanceRequired(
            final String controllerId, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final long forceTime,
            final Integer weight, final boolean confirmationRequired) {
        this.controllerId = controllerId;
        this.userAcceptanceRequired = userAcceptanceRequired != null ? userAcceptanceRequired : MgmtRolloutUserAcceptanceRequired.YES;
        this.forceTime = forceTime;
        this.weight = weight;
        this.confirmationRequired = confirmationRequired;
    }

    /**
     * Constructor that also accepts maintenance schedule parameters and checks
     * for validity of the specified maintenance schedule.
     *
     * @param controllerId
     *            for which the action is created.
     * @param userAcceptanceRequired
     *            specified for the action.
     * @param forceTime
     *            after that point in time the action is exposed as forced
     * @param weight
     *            the priority of an {@link Action}
     * @param maintenanceSchedule
     *            is the cron expression to be used for scheduling maintenance
     *            windows. Expression has 6 mandatory fields and 1 last optional
     *            field: "second minute hour dayofmonth month weekday year"
     * @param maintenanceWindowDuration
     *            in HH:mm:ss format specifying the duration of a maintenance
     *            window, for example 00:30:00 for 30 minutes
     * @param maintenanceWindowTimeZone
     *            is the time zone specified as +/-hh:mm offset from UTC, for
     *            example +02:00 for CET summer time and +00:00 for UTC. The
     *            start time of a maintenance window calculated based on the
     *            cron expression is relative to this time zone.
     *
     * @throws InvalidMaintenanceScheduleException
     *             if the parameters do not define a valid maintenance schedule.
     */
    public TargetWithUserAcceptanceRequired(final String controllerId, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final long forceTime,
                                            final Integer weight, final String maintenanceSchedule, final String maintenanceWindowDuration,
                                            final String maintenanceWindowTimeZone, final boolean confirmationRequired) {
        this(controllerId, userAcceptanceRequired, forceTime, weight, confirmationRequired);

        this.maintenanceSchedule = maintenanceSchedule;
        this.maintenanceWindowDuration = maintenanceWindowDuration;
        this.maintenanceWindowTimeZone = maintenanceWindowTimeZone;
    }

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        if (userAcceptanceRequired != null) {
            return userAcceptanceRequired;
        }
        // default value
        return MgmtRolloutUserAcceptanceRequired.YES;
    }

    public long getForceTime() {
        return forceTime;
    }

    public Integer getWeight() {
        return weight;

    }

    public String getControllerId() {
        return controllerId;
    }

    /**
     * Returns the maintenance schedule for the {@link Action}.
     *
     * @return cron expression as {@link String}.
     */
    public String getMaintenanceSchedule() {
        return this.maintenanceSchedule;
    }

    /**
     * Returns the duration of maintenance window for the {@link Action}.
     *
     * @return duration in HH:mm:ss format as {@link String}.
     */
    public String getMaintenanceWindowDuration() {
        return maintenanceWindowDuration;
    }

    /**
     * Returns the timezone of maintenance window for the {@link Action}.
     *
     * @return the timezone offset from UTC in +/-hh:mm as {@link String}.
     */
    public String getMaintenanceWindowTimeZone() {
        return maintenanceWindowTimeZone;
    }

    /**
     * Return if a confirmation is required for this assignment (depends on confirmation flow active)
     *
     * @return the flag
     */
    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    @Override
    public String toString() {
        return "TargetWithUserAcceptanceRequired [controllerId=" + controllerId + ", userAcceptanceRequired=" + getUserAcceptanceRequired() + ", forceTime="
                + getForceTime() + ", weight=" + getWeight() + ", maintenanceSchedule=" + getMaintenanceSchedule()
                + ", maintenanceWindowDuration=" + getMaintenanceWindowDuration() + ", maintenanceWindowTimeZone="
                + getMaintenanceWindowTimeZone() + ", confirmationRequired=" + isConfirmationRequired() + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(userAcceptanceRequired, controllerId, forceTime, weight, confirmationRequired, maintenanceSchedule,
                maintenanceWindowDuration, maintenanceWindowTimeZone);
    }

    @SuppressWarnings("squid:S1067")
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TargetWithUserAcceptanceRequired other = (TargetWithUserAcceptanceRequired) obj;
        return Objects.equals(userAcceptanceRequired, other.userAcceptanceRequired) && Objects.equals(controllerId, other.controllerId)
                && Objects.equals(forceTime, other.forceTime) && Objects.equals(weight, other.weight)
                && Objects.equals(confirmationRequired, other.confirmationRequired)
                && Objects.equals(maintenanceSchedule, other.maintenanceSchedule)
                && Objects.equals(maintenanceWindowDuration, other.maintenanceWindowDuration)
                && Objects.equals(maintenanceWindowTimeZone, other.maintenanceWindowTimeZone);
    }

}
