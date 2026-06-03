package org.cosmos.s3.exception;

/**
 * Exception thrown when an error occurs in the S3 service.
 */
public class S3Exception extends RuntimeException {

    public S3Exception(String message, Throwable cause) {
        super(message, cause);
    }

    public S3Exception(String message) {
        super(message);
    }
}
