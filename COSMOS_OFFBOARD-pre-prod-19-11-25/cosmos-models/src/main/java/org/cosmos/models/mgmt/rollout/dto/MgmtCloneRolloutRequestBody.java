package org.cosmos.models.mgmt.rollout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;

/**
 * DTO for cloning a rollout in the management system.
 * <p>
 * Contains details such as the name, description, start type, start date, and end date
 * required to clone an existing rollout.
 * </p>
 */
@Schema(description = "Request body for Cloning a  rollout")
@Getter
@Setter
public class MgmtCloneRolloutRequestBody {

    @Schema(description = "ID of the rollout to be cloned", required = true, example = "rollout-12345")
    @NotBlank(message = "Name cannot be blank")
    private String name;
    /**
     * The description of the rollout.
     */
    @Schema(description = "Description of the retry request", example = "Retry operation for failed devices")
    private String description;

    /**
     * The start type of the rollout.
     */
    @Schema(description = "Start type of the rollout", example = "SCHEDULED")
    private MgmtRolloutStartType startType;

    /**
     * The start date of the rollout in epoch seconds.
     */

    @Schema(description = "Start date in epoch seconds", example = "1697040000")
    @Positive(message = "startDate must be a positive number in seconds")
    private Long startDate;

    /**
     * The end date of the rollout in epoch seconds.
     */
    @Schema(description = "End date in epoch seconds", required = true, example = "1697126400")
    @Positive(message = "endDate must be a positive number in seconds")
    private Long endDate;




}
