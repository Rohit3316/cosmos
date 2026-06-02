package org.eclipse.hawkbit.feignclient.kafka;

import feign.Headers;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.RolloutErrorMessage;
import org.cosmos.models.kafka.RolloutStatusMessage;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.eclipse.hawkbit.feignclient.configuration.FeignClientConfiguration;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * {@code KafkaClient} is a declarative Feign client for communicating with the external KAFKA service.
 * <p>
 * It provides methods to publish various types of vehicle-related messages such as status updates,
 * rollout results, file transfers, and error events to the KAFKA system.
 * </p>
 *
 * <p>
 * The base URL for the KAFKA service is configured via the property:
 * <pre>{@code external.services.kafka-service.baseurl}</pre>
 * </p>
 *
 * <p>
 * This client uses {@link FeignClientConfiguration} for custom Feign setup.
 * </p>
 */
@FeignClient(name = "kafkaClient", url = "${external.services.kafka-service.baseurl}", configuration = FeignClientConfiguration.class)
public interface KafkaClient {

    // KAFKA endpoint paths
    String KAFKA_VEHICLE_STATUS_URL = "kafka/vehiclestatus";
    String KAFKA_ROLLOUT_STATUS_URL = "kafka/rolloutstatus";
    String KAFKA_ROLLOUT_ERROR_URL = "kafka/rollouterror";
    String KAFKA_FILE_UPLOAD_URL = "kafka/fileupload";
    String KAFKA_UPLOAD_ERROR_URL = "kafka/fileupload/error";
    String KAFKA_GENERAL_FEEDBACK_IDLE_STATUS_URL = "kafka/general/idle";
    String KAFKA_GENERAL_FEEDBACK_ERROR_STATUS_URL = "kafka/general/error";
    String KAFKA_FILE_DELETE_ERROR_URL = "kafka/fileDelete/error";
    String KAFKA_INVENTORY_URL = "kafka/inventory";
    String KAFKA_DD_ARTIFACT_EXPIRY_URL = "kafka/artifact/error";
    String KAFKA_SEND_EVENT_URL = "kafka/sendEvent";
    /**
     * Publishes a generic event message with headers and payload to the KAFKA service.
     *
     * @param eventTemplate the event template containing headers and payload
     * @param messageType the type of the message (used for topic routing)
     */
    @PostMapping(value = KAFKA_SEND_EVENT_URL, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Content-Type: application/json")
    void publishEvent(@RequestBody KafkaEventTemplate eventTemplate, @RequestParam(value = "messageType") String messageType);

    /**
     * Publishes a vehicle status message to the KAFKA service.
     *
     * @param message the vehicle status message to be published
     */
    @PostMapping(KAFKA_VEHICLE_STATUS_URL)
    void publishVehicleStatus(@RequestBody VehicleStatusMessage message);

    /**
     * Publishes a rollout status message to the KAFKA service.
     *
     * @param message the rollout status message to be published.
     */
    @PostMapping(KAFKA_ROLLOUT_STATUS_URL)
    void publishRolloutStatus(@RequestBody RolloutStatusMessage message);

    /**
     * Publishes a rollout error message to the KAFKA service.
     *
     * @param message the rollout error message to be published.
     */
    @PostMapping(value = KAFKA_ROLLOUT_ERROR_URL, consumes = MediaType.APPLICATION_JSON_VALUE)
    @Headers("Content-Type: application/json")
    void publishRolloutError(@RequestBody RolloutErrorMessage message);

    /**
     * Publishes a file transfer status message to the KAFKA service.
     *
     * @param message the file transfer status message to be published.
     */
    @PostMapping(KAFKA_FILE_UPLOAD_URL)
    void publishFileTransferStatus(@RequestBody FileUploadStatusMessage message);

    /**
     * Publishes a file transfer error message to the KAFKA service.
     *
     * @param message the file transfer error status message to be published.
     */
    @PostMapping(KAFKA_UPLOAD_ERROR_URL)
    void publishFileTransferError(@RequestBody FileUploadErrorMessage message);

    /**
     * Publishes a general feedback 'IDLE' Status  message to the KAFKA service.
     *
     * @param message the General IDLE  message to be published.
     */
    @PostMapping(KAFKA_GENERAL_FEEDBACK_IDLE_STATUS_URL)
    void publishGeneralFeedbackIdle(@RequestBody GeneralIdleMessage message);

    /**
     * Publishes a general feeback 'ERC' Status message to the KAFKA service.
     *
     * @param message the General ERROR  message to be published.
     */
    @PostMapping(KAFKA_GENERAL_FEEDBACK_ERROR_STATUS_URL)
    void publishGeneralFeedbackError(@RequestBody GeneralErrorMessage message);

    /**
     * Publishes a file delete error message to the KAFKA service.
     *
     * @param message the file delete error status message to be published.
     */
    @PostMapping(KAFKA_FILE_DELETE_ERROR_URL)
    void publishFileDeleteError(@RequestBody FileDeleteErrorMessage message);

    /**
     * Publishes an inventory message to the KAFKA service.
     *
     * @param message the inventory message to be published.
     */
    @PostMapping(KAFKA_INVENTORY_URL)
    void publishInventory(@RequestBody InventoryMessage message);

    /**
     * Publishes a DD Artifact error message to the KAFKA service.
     *
     * @param message the DD Artifact error message to be published.
     */
    @PostMapping(KAFKA_DD_ARTIFACT_EXPIRY_URL)
    void publishDdArtifactExpiry(@RequestBody FileDeleteErrorMessage message);
}