package org.cosmos.models.mgmt.rollout.constants;

/**
 * Enum representing the retry modes for multiple devices in a rollout.
 */
public enum RetryMode {
    ALL_SUCCEEDED_VEHICLES,
    ALL_FAILED_VEHICLES,
    ALL_CANCELED_VEHICLES,
    ALL_NOT_EXECUTED_VEHICLES,
    FULL,
    INDIVIDUAL_SUCCEEDED_VEHICLES,
    INDIVIDUAL_FAILED_VEHICLES,
    INDIVIDUAL_CANCELED_VEHICLES,
    INDIVIDUAL_NOT_EXECUTED_VEHICLES
}