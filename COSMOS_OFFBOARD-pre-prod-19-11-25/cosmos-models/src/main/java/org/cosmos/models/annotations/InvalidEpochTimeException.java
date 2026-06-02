package org.cosmos.models.annotations;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Exception thrown when an invalid epoch time is encountered during deserialization.
 */
public class InvalidEpochTimeException extends JsonProcessingException {

    /**
     * Constructs a new InvalidEpochTimeException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidEpochTimeException(String message) {
        super(message);
    }

}