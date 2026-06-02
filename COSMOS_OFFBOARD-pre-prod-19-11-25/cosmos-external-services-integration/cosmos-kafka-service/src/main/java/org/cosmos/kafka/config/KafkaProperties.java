package org.cosmos.kafka.config;


import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Kafka integration in the application.
 * This class maps the Kafka-related properties defined in the application's
 * configuration files (e.g., application.properties)
 * under the prefix "cosmos.server.kafka".
 */

@Component
@ConfigurationProperties(prefix = "cosmos.server.kafka")
@Data
public class KafkaProperties {

    /**
     * List of Kafka bootstrap servers used to establish the initial connection
     * to the Kafka cluster.
     */
    private String bootstrapServers;

    /**
     * The topic name to which messages will be sent or from which messages will be consumed.
     * This is the Kafka topic that the application interacts with.
     */
    private String topic;

    /**
     * The group ID for the Kafka consumer, which is used to identify the consumer group
     * that this application belongs to.
     */
    private String groupId;

    /**
     * Kafka rollout topic
     */
    private String rolloutStatusTopic;

    /**
     * Kafka rollout vehicle status topic
     */
    private String rolloutVehicleStatusTopic;

    /**
     * Kafka vehicle inventory topic
     */
    private String vehicleInventoryTopic;

    /**
     * Kafka package status topic
     */
    private String packageStatusTopic;

    /**
     * Kafka vehicle general topic
     */
    private String vehicleGeneralTopic;

    @PostConstruct
    public void logConfig() {
        System.out.println("Kafka Properties Loaded: " + this);
    }
}