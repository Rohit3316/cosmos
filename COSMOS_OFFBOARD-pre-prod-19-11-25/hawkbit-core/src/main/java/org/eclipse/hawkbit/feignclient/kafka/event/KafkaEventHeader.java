package org.eclipse.hawkbit.feignclient.kafka.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.sqs.FileType;

/**
 * Header metadata for Kafka event messages.
 * <p>
 * Contains contextual information to be sent along with the payload.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // Exclude null fields from serialization
public class KafkaEventHeader {
    /**
     * The tenant identifier for multi-tenant environments.
     */
    private String tenant;

    /**
     * The name of the rollout associated with the event.
     */
    private String rolloutName;

    /**
     * The VIN (Vehicle Identification Number) or unique vehicle identifier.
     */
    private String vin;

    /**
     * The OTA master serial number for tracking.
     */
    private String otaMasterSerialNumber;

    /**
     * The type of file related to the event (ARTIFACT, ESP, or RSP).
     */
    private String fileType;
}
