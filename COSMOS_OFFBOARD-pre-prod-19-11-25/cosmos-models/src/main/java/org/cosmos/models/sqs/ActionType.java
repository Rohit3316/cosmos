package org.cosmos.models.sqs;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of action.
 */
public enum ActionType {
    UPLOAD("UPLOAD"), DOWNLOAD("DOWNLOAD"), DELETE("DELETE");

    private final String type;

    /**
     * Constructor for ActionType.
     *
     * @param action the type of action
     */
    ActionType(String action) {
        this.type = action;
    }

    /**
     * Gets the type of action.
     *
     * @return the type of action
     */
    @JsonValue
    public String getType() {
        return type;
    }
}
