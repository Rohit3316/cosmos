package org.cosmos.kafka.service.impl.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.cosmos.kafka.config.KafkaProperties;
import org.cosmos.kafka.service.KafkaProducerService;
import org.cosmos.kafka.utils.Constants;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * KafkaProducerServiceImpl is a service that implements the KafkaProducerService interface.
 * It is responsible for sending messages to a Kafka topic using the provided KafkaTemplate.
 */
@Service
public class KafkaProducerServiceImpl<T> implements KafkaProducerService<T> {
    /**
     * KafkaProperties are used to configure the Kafka producer.
     */
    private final KafkaProperties kafkaProperties;
    /**
     * KafkaTemplate is used to send messages to the Kafka topic.
     */
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public KafkaProducerServiceImpl(KafkaProperties kafkaProperties, KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaProperties = kafkaProperties;
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends a message to the specified Kafka topic.
     * Assumes the message is always a KafkaEventTemplate.
     * Only the payload is sent as the Kafka value,
     * and all non-null header fields are set as Kafka headers.
     *
     * @param messageType The type of the message, which determines the Kafka topic to send the message to.
     * @param message     The message to be sent to the Kafka topic.
     */
    @Override
    public void sendMessage(String messageType, T message) {
        String topic = getTopicNameByMessageType(messageType);
        KafkaEventTemplate eventTemplate = (KafkaEventTemplate) message;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String payload = mapper.writeValueAsString(eventTemplate.getPayload());
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, payload);

            addKafkaHeaders(record.headers(), eventTemplate.getHeader());

            // Cast kafkaTemplate to KafkaTemplate<String, String> for this send
            @SuppressWarnings("unchecked")
            KafkaTemplate<String, String> stringKafkaTemplate = (KafkaTemplate<String, String>) (KafkaTemplate<?, ?>) this.kafkaTemplate;
            stringKafkaTemplate.send(record);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize KafkaEventTemplate payload", e);
        }
    }

    /**
     * Adds non-null header fields from KafkaEventHeader to Kafka record headers.
     */
    private void addKafkaHeaders(Headers headers, KafkaEventHeader header) {
        if (header == null) return;

        addHeaderIfNotNull(headers, "tenant", header.getTenant());
        addHeaderIfNotNull(headers, "rolloutName", header.getRolloutName());
        addHeaderIfNotNull(headers, "vin", header.getVin());
        addHeaderIfNotNull(headers, "otaMasterSerialNumber", header.getOtaMasterSerialNumber());
        addHeaderIfNotNull(headers, "fileType", header.getFileType() != null ? header.getFileType().toString() : null);
    }

    private void addHeaderIfNotNull(Headers headers, String key, String value) {
        if (value != null) {
            headers.add(key, value.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns the topic name based on the message type.
     * @param messageType The type of the message, which determines the Kafka topic to send the message to.
     * @return The name of the Kafka topic corresponding to the message type.
     */
    private String getTopicNameByMessageType(String messageType) {
        return switch (messageType) {
            case Constants.INVENTORY -> kafkaProperties.getVehicleInventoryTopic();
            case Constants.ROLLOUT_STATUS,
                    Constants.ROLLOUT_ERROR -> kafkaProperties.getRolloutStatusTopic();
            case Constants.VEHICLE_STATUS -> kafkaProperties.getRolloutVehicleStatusTopic();
            case Constants.FILE_UPLOAD,
                    Constants.FILE_UPLOAD_ERROR,
                    Constants.FILE_DELETE_ERROR,
                    Constants.DD_ARTIFACT_EXPIRY -> kafkaProperties.getPackageStatusTopic();
            case Constants.GENERAL_IDLE,
                    Constants.GENERAL_ERROR -> kafkaProperties.getVehicleGeneralTopic();
            default -> throw new IllegalArgumentException("Invalid message type: " + messageType);
        };
    }
}
