package org.cosmos.sns.exception;

/**
 * Exception thrown when an error occurs in the SNS service.
 */
public class SnsException extends RuntimeException {

    public SnsException(String message, Throwable cause) {
        super(message, cause);
    }

}
