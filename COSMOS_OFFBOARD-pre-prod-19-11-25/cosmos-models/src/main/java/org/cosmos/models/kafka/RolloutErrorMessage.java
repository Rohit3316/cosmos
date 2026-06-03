package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Represents a message containing error details related to a rollout process.
 * This class encapsulates information about the rollout name, status, error code,
 * error messages, and the timestamp when the error occurred.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolloutErrorMessage {

    @NotBlank
    @NotNull
    private String rolloutName;

    @NotBlank
    @NotNull
    private String status;

    @NotBlank
    @NotNull
    private String errorCode;

    @NotEmpty
    @NotNull
    private List<String> errorMessages;

    @Positive
    private long timeStamp;
}