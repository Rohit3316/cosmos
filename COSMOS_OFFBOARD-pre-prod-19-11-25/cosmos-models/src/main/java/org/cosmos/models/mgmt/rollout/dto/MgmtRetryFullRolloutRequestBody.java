package org.cosmos.models.mgmt.rollout.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;

/**
 * DTO for retrying a full rollout operation.
 * Contains details such as description, start type, start date, and end date.
 */
@Schema(description = "Request body for retrying a full rollout")
@Getter
@Setter
public class MgmtRetryFullRolloutRequestBody {


    /**
     * The description of the rollout.
     */
    @Schema(description = "Description of the retry request", required = true, example = "Retry operation for failed devices")
    @NotBlank(message = "Description cannot be blank")
    private String description;

    /**
     * The start type of the rollout.
     */
    @Schema(description = "Start type of the rollout", required = true, example = "SCHEDULED")
    @NotNull(message = "Start type is mandatory")
    private MgmtRolloutStartType startType;

    /**
     * The start date of the rollout in epoch seconds.
     */

    @Schema(description = "Start date in epoch seconds", required = true, example = "1697040000")
    @Positive(message = "startDate must be a positive number in seconds")
    private Long startDate;

    /**
     * The end date of the rollout in epoch seconds.
     */
    @Schema(description = "End date in epoch seconds", required = true, example = "1697126400")
    @Positive(message = "endDate must be a positive number in seconds")
    private Long endDate;


    /**
     * The vehicle log level for the rollout.
     */
    @Schema(description = "Vehicle log level for the rollout", required = false, example = "4")
    @Positive(message = "vehicleLogLevel must be a positive integer")
    private Integer vehicleLogLevel;


}
