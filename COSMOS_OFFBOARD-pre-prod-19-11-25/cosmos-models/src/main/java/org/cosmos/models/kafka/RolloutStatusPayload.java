package org.cosmos.models.kafka;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolloutStatusPayload {

    /**
     * The type of the message.
     * Can be INFO or ERROR.
     */
    @NotBlank
    private String type;

    /**
     * The status of the rollout.
     * ENUM: DRAFT, READY, RUNNING, PAUSED, CANCELLED, FINISHED, etc.
     */
    @NotBlank
    private String status;

    /**
     * A list of error codes, if any occurred during rollout.
     */
    private List<String> errorCode;

    /**
     * A list of error messages, if any occurred during rollout.
     */
    private List<String> errorMessages;

    /**
     * The timestamp in epoch seconds.
     */
    @NotNull
    private Long timestamp;
}
