package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a message containing the status of a vehicle during a rollout process.
 */
@Data
@Builder
@AllArgsConstructor
public class VehicleStatusMessage {

    /**
     * The name of the rollout process to which this vehicle status message pertains.
     * This field cannot be blank.
     */
    @NotBlank
    private String rolloutName;

    /**
     * The unique identifier of the vehicle.
     * This field cannot be blank.
     */
    @NotBlank
    private String vehicleId;

    /**
     * The current status of the vehicle.
     * This field cannot be blank.
     */
    @NotBlank
    private String status;

    /**
     * A list of messages or logs related to the vehicle's status.
     * This field cannot be empty.
     */
    @NotEmpty
    private String[] messages;

    /**
     * The timestamp of when the vehicle status was recorded.
     * This field cannot be null.
     */
    @NotNull
    private Long timestamp;

    /**
     * Default constructor for creating an instance of VehicleStatusMessage.
     */
    public VehicleStatusMessage() {
        // Default constructor
    }
}
