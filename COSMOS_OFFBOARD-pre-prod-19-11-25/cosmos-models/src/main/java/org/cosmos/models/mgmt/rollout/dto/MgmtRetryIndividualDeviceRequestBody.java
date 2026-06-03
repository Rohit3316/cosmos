package org.cosmos.models.mgmt.rollout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;


@Schema(description = "Request object for retrying individual device")
@Getter
@Setter
public class MgmtRetryIndividualDeviceRequestBody {

    @Schema(description = "Description of the retry request", required = true, example = "Retry operation for individual device")
    @NotBlank(message = "Description cannot be blank")
    private String description;

    @Schema(description = "Retry mode", required = true, example = "IMMEDIATE")
    @NotNull(message = "Retry mode is mandatory")
    private RetryMode retryMode;

    @Schema(description = "Start type of the rollout", required = true, example = "SCHEDULED")
    @NotNull(message = "Start type is mandatory")
    private MgmtRolloutStartType startType;

    @Schema(description = "Start date in epoch seconds", required = true, example = "1697040000")
    @Positive(message = "startDate must be a positive number in seconds")
    private Long startDate;

    @Schema(description = "End date in epoch seconds", required = true, example = "1697126400")
    @Positive(message = "endDate must be a positive number in seconds")
    private Long endDate;

    @Schema(description = "Vehicle log level for the rollout", required = false, example = "4")
    @Positive(message = "vehicleLogLevel must be a positive integer")
    private Integer vehicleLogLevel;
}
