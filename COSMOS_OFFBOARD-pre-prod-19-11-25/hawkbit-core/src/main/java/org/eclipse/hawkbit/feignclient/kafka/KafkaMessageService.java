package org.eclipse.hawkbit.feignclient.kafka;

import java.util.Collections;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.RolloutErrorMessage;
import org.cosmos.models.kafka.RolloutStatusMessage;
import org.cosmos.models.kafka.RolloutStatusPayload;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Service for publishing a variety of domain-specific messages to the KAFKA service.
 * <p>
 * <strong>Responsibilities:</strong>
 * <ul>
 *     <li>Publishes rollout status and error messages</li>
 *     <li>Sends vehicle status updates</li>
 *     <li>Handles inventory status messages</li>
 *     <li>Manages file upload, delete, and expiry statuses</li>
 *     <li>Delivers general feedback messages (idle and error)</li>
 * </ul>
 * <p>
 * <strong>Features:</strong>
 * <ul>
 *     <li>Executes asynchronous operations via Spring’s {@code @Async} annotation and task executor</li>
 *     <li>Centralizes all KAFKA interactions through the {@link KafkaClient}</li>
 * </ul>
 * <p>
 * <strong>Usage:</strong> Inject this service where KAFKA publishing is required and invoke the relevant method with a valid message object.
 */
@Service
@Slf4j
public class KafkaMessageService {

    private final KafkaClient kafkaClient;

    public KafkaMessageService(KafkaClient kafkaClient) {
        this.kafkaClient = kafkaClient;
    }







    /**
     * Asynchronously publishes a vehicle status message to the Kafka service.
     * <p>
     * Constructs a {@link VehicleStatusMessage} using the provided rollout name, controller ID, and status,
     * sets the current timestamp, and sends it via the {@link KafkaClient}.
     * <p>
     * Any exceptions during publishing are caught and logged.
     *
     * @param rolloutName  the name of the rollout associated with the vehicle status
     * @param controllerId the unique identifier of the vehicle/controller
     * @param status       the status message to be included for the vehicle
     */
    @Async
    public void publishVehicleStatus(String rolloutName, String controllerId, String status) {
        try {
            VehicleStatusMessage vehicleStatusMessage = VehicleStatusMessage.builder()
                    .rolloutName(rolloutName)
                    .status(String.valueOf(DeviceActionStatus.RUNNING))
                    .vehicleId(controllerId)
                    .timestamp(Instant.now().getEpochSecond())
                    .messages(new String[]{status})
                    .build();

            kafkaClient.publishVehicleStatus(vehicleStatusMessage);
            log.info("Vehicle status message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending vehicle status message to Kafka service: {}", e.getMessage(), e);
        }
    }

    /**
     * Asynchronously publishes a vehicle status message to the Kafka service with retry support.
     * <p>
     * Sends the provided {@link VehicleStatusMessage} using the {@link KafkaClient}.
     * If sending fails, the operation will be retried according to the configured retry policy.
     * Logs a warning if a retry attempt is being made.
     *
     * @param message the {@link VehicleStatusMessage} to be published to the Kafka service
     */
    @Async
    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${cosmos.server.kafka.retryMaxAttempt}",
            backoff = @Backoff(delayExpression = "${cosmos.server.kafka.retryDelay}",
                    multiplierExpression = "${cosmos.server.kafka.retryMultiplier}")
    )
    public void sendVehicleStatusMessage(VehicleStatusMessage message) {
        if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            log.warn("Attempt {} to connect to the Kafka server failed. Retrying...",
                    RetrySynchronizationManager.getContext().getRetryCount());
        }

        kafkaClient.publishVehicleStatus(message);
        log.info("Vehicle status message sent successfully to Kafka service");
    }

    /**
     * Asynchronously publishes an inventory message to the Kafka service with retry support.
     * <p>
     * Sends the provided {@link InventoryMessage} using the {@link KafkaClient}.
     * If sending fails, the operation will be retried according to the configured retry policy.
     * Logs a warning if a retry attempt is being made.
     * <p>
     * This method is executed asynchronously using Spring’s task executor.
     *
     * @param message the {@link InventoryMessage} to be published to the Kafka service
     */
    @Async
    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${cosmos.server.kafka.retryMaxAttempt}",
            backoff = @Backoff(delayExpression = "${cosmos.server.kafka.retryDelay}",
                    multiplierExpression = "${cosmos.server.kafka.retryMultiplier}")
    )
    public void sendInventoryMessage(InventoryMessage message) {
        if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            log.warn("Attempt {} to connect to the Kafka server failed. Retrying...",
                    RetrySynchronizationManager.getContext().getRetryCount());
        }
        kafkaClient.publishInventory(message);
        log.info("Inventory message sent successfully to Kafka service");
    }

    /**
     * Asynchronously publishes a rollout status message to the Kafka service with retry support.
     * <p>
     * Sends the provided {@link RolloutStatusMessage} using the {@link KafkaClient}.
     * If sending fails, the operation will be retried according to the configured retry policy.
     * Logs a warning if a retry attempt is being made.
     * <p>
     * This method is executed asynchronously using Spring’s task executor.
     *
     * @param message the {@link RolloutStatusMessage} to be published to the Kafka service
     */
    @Async
    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${cosmos.server.kafka.retryMaxAttempt}",
            backoff = @Backoff(delayExpression = "${cosmos.server.kafka.retryDelay}",
                    multiplierExpression = "${cosmos.server.kafka.retryMultiplier}")
    )
    public void sendRolloutStatusMessage(RolloutStatusMessage message) {
        if (RetrySynchronizationManager.getContext().getRetryCount() > 0) {
            log.warn("Attempt {} to connect to the Kafka server failed. Retrying...",
                    RetrySynchronizationManager.getContext().getRetryCount());
        }
        kafkaClient.publishRolloutStatus(message);
        log.info("Rollout status message sent successfully to Kafka service");
    }

    /**
     * Asynchronously publishes a file upload status message to the Kafka service.
     * <p>
     * This method sends the provided {@link FileUploadStatusMessage} to the Kafka service
     * using the {@link KafkaClient}. Any exceptions during publishing are caught and logged.
     * <p>
     * Executed asynchronously using Spring’s task executor.
     *
     * @param fileUploadStatusMessage the file upload status message to be published
     */
    @Async
    public void sendFileUploadStatus(FileUploadStatusMessage fileUploadStatusMessage) {
        try {
            kafkaClient.publishFileTransferStatus(fileUploadStatusMessage);
            log.info("File upload status message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending File upload status message to Kafka service: {}", e.getMessage());
        }
    }

    /**
     * Asynchronously publishes a file upload error message to the Kafka service.
     * <p>
     * This method sends the provided {@link FileUploadErrorMessage} to the Kafka service
     * using the {@link KafkaClient}. Any exceptions during publishing are caught and logged.
     * <p>
     * Executed asynchronously using Spring’s task executor.
     *
     * @param fileUploadErrorMessage the file upload error message to be published to the Kafka service
     */
    @Async
    public void sendFileUploadErrorStatus(FileUploadErrorMessage fileUploadErrorMessage) {
        try {
            log.debug("Sending file upload error message to Kafka service: {}", fileUploadErrorMessage);
            kafkaClient.publishFileTransferError(fileUploadErrorMessage);
            log.info("File upload error message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending File upload error message to Kafka service: {}", e.getMessage());
        }
    }

    /**
     * Asynchronously publishes a general feedback 'IDLE' status message to the Kafka service.
     * <p>
     * This method sends the provided {@link GeneralIdleMessage} to the Kafka service
     * using the {@link KafkaClient}. Any exceptions during publishing are caught and logged.
     * <p>
     * Executed asynchronously using Spring’s task executor.
     *
     * @param generalIdleMessage the {@link GeneralIdleMessage} to be published to the Kafka service.
     */
    @Async
    public void sendGeneralFeedbackIdle(GeneralIdleMessage generalIdleMessage) {
        try {
            log.debug("Sending general feedback 'IDLE' status message to Kafka service: {}", generalIdleMessage);
            kafkaClient.publishGeneralFeedbackIdle(generalIdleMessage);
            log.info("General feedback 'IDLE' status message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending general feedback 'IDLE' status message to Kafka service: {}", e.getMessage());
        }

    }

    /**
     * Asynchronously publishes a general feedback 'ERC' status message to the Kafka service.
     * <p>
     * Sends the provided {@link GeneralErrorMessage} to the Kafka service using the {@link KafkaClient}.
     * Any exceptions during publishing are caught and logged.
     * <p>
     * This method is executed asynchronously using Spring’s task executor.
     *
     * @param generalErrorMessage the {@link GeneralErrorMessage} to be published to the Kafka service.
     */
    @Async
    public void sendGeneralFeedbackError(GeneralErrorMessage generalErrorMessage) {
        try {
            log.debug("Sending general feedback 'ERROR' status message to Kafka service: {}", generalErrorMessage);
            kafkaClient.publishGeneralFeedbackError(generalErrorMessage);
            log.info("General feedback 'ERC' status message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending general feedback 'ERC' status message to Kafka service: {}", e.getMessage());
        }
    }

    /**
     * Asynchronously publishes a file delete error message to the Kafka service.
     * <p>
     * This method sends the provided {@link FileDeleteErrorMessage} to the Kafka service
     * using the {@link KafkaClient}. Any exceptions during publishing are caught and logged.
     * <p>
     * Executed asynchronously using Spring’s task executor.
     *
     * @param fileDeleteError the {@link FileDeleteErrorMessage} representing the file delete error to be published.
     */
    @Async
    public void sendFileDeleteError(FileDeleteErrorMessage fileDeleteError) {
        try {
            log.debug("Sending file delete error message to Kafka service: {}", fileDeleteError);
            kafkaClient.publishFileDeleteError(fileDeleteError);
            log.info("File delete error message sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending File delete error message to Kafka service: {}", e.getMessage());
        }
    }

    /**
     * Asynchronously publishes a Device Descriptor (DD) Artifact expiry message to the Kafka service.
     * <p>
     * This method sends the provided {@link FileDeleteErrorMessage} to the Kafka service
     * using the {@link KafkaClient} to indicate that a DD Artifact has expired.
     * Any exceptions during publishing are caught and logged.
     * <p>
     * Executed asynchronously using Spring’s task executor.
     *
     * @param fileDeleteError the {@link FileDeleteErrorMessage} representing the DD Artifact expiry event to be published.
     */
    @Async
    public void sendDdArtifactExpiry(FileDeleteErrorMessage fileDeleteError) {
        try {
            log.debug("Sending dd Artifact expiry message to Kafka service: {}", fileDeleteError);
            kafkaClient.publishDdArtifactExpiry(fileDeleteError);
            log.info("Dd Artifact expiry message  sent successfully to Kafka service");
        } catch (Exception e) {
            log.error("Error occurred while sending Dd Artifact expiry message to Kafka service: {}", e.getMessage());
        }
    }

    /**
     * Recovers from an exception thrown during the retry attempts.
     * Logs an error message and rethrows the exception.
     *
     * @param e the exception that occurred during the retry attempts
     */
    @Recover
    public void recover(Exception e) {
        log.error("All retry attempts have been exhausted. Error occurred while sending message to Kafka: {}", e.getMessage());
    }

    /**
     * Generic method to send a Kafka event with a message type.
     * Logs the message type and calls publishEvent.
     */
    @Async
    @Retryable(
            value = {Exception.class},
            maxAttemptsExpression = "${cosmos.server.kafka.retryMaxAttempt}",
            backoff = @Backoff(delayExpression = "${cosmos.server.kafka.retryDelay}",
                    multiplierExpression = "${cosmos.server.kafka.retryMultiplier}")
    )
    public void sendKafkaEventWithType(KafkaEventTemplate eventTemplate, String messageType) {
        log.info("Publishing message of type [{}]", messageType);
        kafkaClient.publishEvent(eventTemplate, messageType);
        log.info("{} sent successfully to Kafka service", messageType);
    }

}
