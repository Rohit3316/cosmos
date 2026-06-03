package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a message for tracking the status of a rollout.
 * This class encapsulates details related to the rollout process, including its name, status, and start time.
 */
@Data
@Builder
@AllArgsConstructor
public class RolloutStatusMessage {

    /**
     * The name of the rollout.
     * This field identifies the specific rollout being tracked.
     */
    @NotBlank
    private String rolloutName;

    /**
     * The status of the rollout.
     * This field indicates the current state or progress of the rollout (e.g., "in-progress", "completed").
     */
    @NotBlank
    private String status;

    /**
     * The start time of the rollout in milliseconds since the epoch.
     * This field records when the rollout process started, useful for tracking and logging purposes.
     */
    @NotNull
    private Long startTime;

    /**
     * Default constructor for RolloutStatusMessage.
     * Initializes an instance with default values.
     */
    public RolloutStatusMessage() {
        // Default constructor
    }
}