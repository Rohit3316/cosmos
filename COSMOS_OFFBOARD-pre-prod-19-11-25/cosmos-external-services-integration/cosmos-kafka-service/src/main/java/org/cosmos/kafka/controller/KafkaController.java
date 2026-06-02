package org.cosmos.kafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cosmos.kafka.service.KafkaProducerService;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.RolloutErrorMessage;
import org.cosmos.models.kafka.RolloutStatusMessage;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling Kafka-related API endpoints.
 * Provides endpoints for sending messages to different Kafka topics.
 * This controller is only active when Kafka is enabled.
 */
@ConditionalOnProperty(name = "cosmos.server.kafka.enabled", havingValue = "true", matchIfMissing = false)
@RestController
@RequestMapping(Constants.BASE_URL)
public class KafkaController {
    // The generic type here is KafkaProducerService<Object> to allow any object, not just String
    private final KafkaProducerService<Object> kafkaProducerService;
    public static final String KAFKA_SEND_EVENT_URL = Constants.SEND_EVENT;
    public KafkaController(KafkaProducerService<Object> kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    /**
     * Endpoint to send inventory messages to the Kafka inventory topic.
     *
     * @param inventoryMessage The inventory message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.INVENTORY_URL)
    public ResponseEntity<Void> sendInventory(@Validated @RequestBody InventoryMessage inventoryMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.INVENTORY, inventoryMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send rollout status messages to the Kafka rollout status topic.
     *
     * @param rolloutStatusMessage The rollout status message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.ROLLOUT_STATUS_URL)
    public ResponseEntity<Void> sendRolloutStatus(@Validated @RequestBody RolloutStatusMessage rolloutStatusMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.ROLLOUT_STATUS, rolloutStatusMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send vehicle status messages to the Kafka vehicle status topic.
     *
     * @param vehicleStatusMessage The vehicle status message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.VEHICLE_STATUS_URL)
    public ResponseEntity<Void> sendVehicleStatus(@Validated @RequestBody VehicleStatusMessage vehicleStatusMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.VEHICLE_STATUS, vehicleStatusMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send rollout error messages to the Kafka rollout error topic.
     *
     * @param rolloutErrorMessage The rollout error message to be sent.
     */
    @PostMapping(Constants.ROLLOUT_ERROR_URL)
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Void> sendRolloutError(@Validated @RequestBody RolloutErrorMessage rolloutErrorMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.ROLLOUT_ERROR, rolloutErrorMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send file upload status messages to the Kafka file upload status topic.
     *
     * @param fileUploadStatusMessage The file upload status message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.FILE_UPLOAD_URL)
    public ResponseEntity<Void> sendFileUploadStatus(@Validated @RequestBody FileUploadStatusMessage fileUploadStatusMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.FILE_UPLOAD, fileUploadStatusMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send file upload error messages to the Kafka file upload error topic.
     *
     * @param fileUploadErrorMessage The file upload error message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.FILE_UPLOAD_ERROR_URL)
    public ResponseEntity<Void> sendFileUploadError(@Validated @RequestBody FileUploadErrorMessage fileUploadErrorMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.FILE_UPLOAD_ERROR, fileUploadErrorMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send general idle messages to the Kafka general idle topic.
     *
     * @param generalIdleMessage The general idle message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.GENERAL_IDLE_URL)
    public ResponseEntity<Void> sendGeneralIdle(@Validated @RequestBody GeneralIdleMessage generalIdleMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.GENERAL_IDLE, generalIdleMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send general error messages to the Kafka general error topic.
     *
     * @param generalErrorMessage The general error message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.GENERAL_ERROR_URL)
    public ResponseEntity<Void> sendGeneralError(@Validated @RequestBody GeneralErrorMessage generalErrorMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.GENERAL_ERROR, generalErrorMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send file delete error messages to the Kafka file delete error topic.
     *
     * @param fileDeleteErrorMessage The file delete error message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.FILE_DELETE_ERROR_URL)
    public ResponseEntity<Void> sendFileDeleteError(@Validated @RequestBody FileDeleteErrorMessage fileDeleteErrorMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.FILE_DELETE_ERROR, fileDeleteErrorMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send dd Artifact expiry  messages to the Kafka dd artifact expiry topic.
     *
     * @param fileDeleteErrorMessage The dd Artifact expiry message to be sent.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(Constants.DD_ARTIFACT_EXPIRY)
    public ResponseEntity<Void> sendDdArtifactExpiry(@Validated @RequestBody FileDeleteErrorMessage fileDeleteErrorMessage) throws JsonProcessingException {
        sendMessageToTopic(Constants.DD_ARTIFACT_EXPIRY, fileDeleteErrorMessage);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Endpoint to send generic Kafka event messages (header + payload).
     *
     * @param eventTemplate The KafkaEventTemplate containing header and payload.
     * @param messageType   The type of the message, used for topic routing.
     * @return A ResponseEntity with HTTP status 201 Created if the message is sent successfully.
     */
    @PostMapping(KAFKA_SEND_EVENT_URL)
    public ResponseEntity<Void> sendEvent(
            @Validated @RequestBody KafkaEventTemplate eventTemplate,
            @RequestParam(value = "messageType", required = false) String messageType) throws JsonProcessingException {
        // Pass the eventTemplate object directly to the producer service
        sendMessageToTopic(messageType, eventTemplate);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Sends a message to the specified Kafka topic.
     * @param messageType The type of the message, which determines the Kafka topic to send the message to.
     * @param objectInput The object to be sent as a message.
     */
    private void sendMessageToTopic(String messageType, Object objectInput) {
        kafkaProducerService.sendMessage(messageType, objectInput);
    }

    @PostMapping(Constants.SEND)
    public ResponseEntity<String> sendMessage(@RequestParam("message") String message) {
        kafkaProducerService.sendMessage(Constants.ROLLOUT_STATUS, message);
        return ResponseEntity.ok("Message sent: " + message);
    }
}
