package org.cosmos.models.kafka;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a message containing the General Feedback Idle messages
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralIdleMessage {

    /**
     * Unique identifier for the vehicle
     */
    @NotBlank
    private String vehicleId;

    /**
     * The current status of the vehicle (e.g., IDLE, RUNNING, etc.)
     */
    @NotBlank
    private String status;

    /**
     * List of messages related to the vehicle's status
     */
    @NotNull
    private List<String> messages;

    /**
     * Timestamp of when the status message was generated
     */
    @NotNull
    private Long timestamp;
}

