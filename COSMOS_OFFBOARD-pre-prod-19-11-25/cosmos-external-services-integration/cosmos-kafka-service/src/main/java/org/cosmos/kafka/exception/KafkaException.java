package org.cosmos.kafka.exception;

/**
 * Custom exception class for handling Kafka-related errors.
 * This class extends RuntimeException and provides constructors
 * to create exceptions with a message and/or a cause.
 */
public class KafkaException extends RuntimeException{

    /**
     * Default constructor for KafkaException.
     * Initializes a new instance of KafkaException with message and a cause.
     * @param message The detail message for the exception.
     * @param cause The cause of the exception, which can be another throwable.
     */
    public KafkaException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for KafkaException with a message.
     * Initializes a new instance of KafkaException with the specified message.
     *
     * @param message The detail message for the exception.
     */
    public KafkaException(String message) {
        super(message);
    }
}
