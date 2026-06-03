package org.cosmos.kafka.service;

/**
 * This interface defines a service for sending messages to a Kafka topic.
 * It is generic and can be used with any type of message.
 */

public interface KafkaProducerService<T> {
    /**
     * Sends a message to the specified Kafka topic.
     *
     * @param messageType   The type of the message.
     * @param message The message to be sent to the Kafka topic.
     */
    void sendMessage(String messageType, T message);
}
