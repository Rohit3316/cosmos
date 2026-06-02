package org.cosmos.models.kafka;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a message containing the General Feedback error status
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneralErrorMessage {

    /**
     * Unique identifier for the vehicle
     */
    @NotBlank
    private String vehicleId;

    /**
     * The error status code of the vehicle (e.g., ERC)
     */
    @NotBlank
    private String status;

    /**
     * List of error codes related to the vehicle error (e.g., ERR_28102)
     */
    @NotNull
    private List<String> errorCode;

    /**
     * List of error messages related to the vehicle error (e.g., "Memory leak")
     */
    @NotNull
    private List<String> errorMessages;

    /**
     * Timestamp of when the error message was generated
     */
    @NotNull
    private Long timestamp;
}
