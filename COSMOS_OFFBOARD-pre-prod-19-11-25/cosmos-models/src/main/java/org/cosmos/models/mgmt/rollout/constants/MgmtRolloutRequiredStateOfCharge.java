package org.cosmos.models.mgmt.rollout.constants;

/**
 * Enum representing the required state of charge conditions for an update.
 */
public enum MgmtRolloutRequiredStateOfCharge {
    /**
     * Required battery percentage for the update.
     */
    BATTERY_PERCENTAGE,

    /**
     * Required battery temperature for the update.
     */
    BATTERY_TEMPERATURE,

    /**
     * Required battery temperature in metric units for the update.
     */
    BATTERY_TEMP_METRIC
}