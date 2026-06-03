package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a message containing inventory details to be sent to a message queue.
 * This class is used to encapsulate the data related to inventory messages.
 */
@Data
@Builder
@AllArgsConstructor
public class InventoryMessage {

    /**
     * Details about the inventory. This field is mandatory and cannot be blank.
     */
    @NotBlank
    private String inventoryDetails;

    /**
     * Signature associated with the inventory details. This field is mandatory and cannot be null.
     */
    @NotNull
    private InventorySignature inventorySignature;

    /**
     * Vehicle Identification Number (VIN) associated with the inventory. This field is mandatory and cannot be blank.
     */
    @NotBlank
    private String vin;

    /**
     * Default constructor for InventoryMessage.
     * Initializes an instance with default values.
     */
    public InventoryMessage() {
        // Default constructor
    }
}
