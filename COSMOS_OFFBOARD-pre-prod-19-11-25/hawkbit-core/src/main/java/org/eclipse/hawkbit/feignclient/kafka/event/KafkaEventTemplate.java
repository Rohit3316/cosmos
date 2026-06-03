package org.eclipse.hawkbit.feignclient.kafka.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Template for sending generic events to Kafka.
 * <p>
 * Encapsulates a header (with metadata such as tenant, rolloutName, vin, otaMasterSerialNumber, and fileType)
 * and a payload (such as InventoryMessage, VehicleStatusMessage, etc.) for flexible event publishing.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KafkaEventTemplate {
    /**
     * The header containing metadata for the event.
     */
    private KafkaEventHeader header;

    /**
     * The payload object to be sent (e.g., InventoryMessage, VehicleStatusMessage).
     */
    private Object payload;
}
