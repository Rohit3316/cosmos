package org.cosmos.kafka.service.impl.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

/**
 * This service listens to messages from a Kafka topic and processes them.
 * It uses the @KafkaListener annotation to specify the topic and group ID.
 */

@Slf4j
@Service
@Profile("postgresql")
@ConditionalOnProperty(name = "cosmos.server.kafka.enabled", havingValue = "true", matchIfMissing = false)// Only loads for local testing
public class KafkaConsumerServiceImpl {

    /**
     * Consumes messages from Kafka topic.
     * This is only active in lcoal 'postgresql' profile for testing purposes.
     */
    @KafkaListener(topics = "${cosmos.server.kafka.topic}", groupId = "${cosmos.server.kafka.group-id}")
    public void consumeMessage(String message) {
        log.info("Message received from Kafka: {}",message);
        // Add logic to process the consumed message
    }
}
