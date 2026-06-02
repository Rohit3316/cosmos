package org.cosmos.models.mgmt.rollout.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to specify whether DowngradeAllowed
 */
public enum MgmtRolloutDowngradeAllowed {

    NO("0"),

    YES("1");

    private final String value;

    MgmtRolloutDowngradeAllowed(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MgmtRolloutDowngradeAllowed fromValue(String value) {
        for (MgmtRolloutDowngradeAllowed allowed : MgmtRolloutDowngradeAllowed.values()) {
            if (allowed.value.equals(value)) {
                return allowed;
            }
        }
        throw new IllegalArgumentException("Value not supported: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}