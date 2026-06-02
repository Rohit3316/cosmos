package org.cosmos.models.mgmt.rollout.constants;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum to specify If the download needs to happen from CDN or external USB
 */
public enum MgmtRolloutRequiredMedia {

    FROM_CDN("0"),

    FROM_USB("1");

    private final String value;

    MgmtRolloutRequiredMedia(String value) {
        this.value = value;
    }

    @JsonCreator
    public static MgmtRolloutRequiredMedia fromValue(String value) {
        for (MgmtRolloutRequiredMedia media : MgmtRolloutRequiredMedia.values()) {
            if (media.value.equals(value)) {
                return media;
            }
        }
        throw new IllegalArgumentException("Value not supported: " + value);
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}