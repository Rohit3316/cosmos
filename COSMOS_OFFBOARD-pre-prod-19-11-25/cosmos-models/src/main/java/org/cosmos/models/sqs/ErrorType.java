package org.cosmos.models.sqs;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of error.
 */
public enum ErrorType {

    TRANSIENT_ERROR("TRANSIENT_ERROR"),
    PERMANENT_FAILURE("PERMANENT_FAILURE");

    private final String type;

    /**
     * Constructor for ErrorType.
     *
     * @param type of error
     */
    ErrorType(String type) {
        this.type = type;
    }

    /**
     * Gets the type of error.
     *
     * @return the type of error
     */
    @JsonValue
    public String getType() {
        return type;
    }
}
