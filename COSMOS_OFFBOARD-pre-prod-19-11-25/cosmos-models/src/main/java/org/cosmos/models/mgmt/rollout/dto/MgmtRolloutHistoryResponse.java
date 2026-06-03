package org.cosmos.models.mgmt.rollout.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * Represents a record of rollout history for a specific VIN, including details such as rollout name, dates, module, version, status, and messages.
 */
@Data
@Schema(description = "Represents a record of rollout history for a specific VIN, including details such as rollout name, dates, module, version, status, and messages.")
public class MgmtRolloutHistoryResponse {
    /** Name of the rollout */
    @Schema(description = "Name of the rollout")
    private String rolloutName;

    /** Start date of the rollout */
    @Schema(description = "Start date of the rollout")
    private String rolloutStartDate;

    /** End date of the rollout */
    @Schema(description = "End date of the rollout")
    private String rolloutEndDate;

    /** SCOMOID (software module identifier) associated with the rollout */
    @Schema(description = "SCOMOID associated with the rollout")
    private String scomoid;

    /** Target version deployed during the rollout */
    @Schema(description = "Target version deployed")
    private String targetVersion;

    /** Latest status of the rollout */
    @Schema(description = "Latest status of the rollout")
    private String latestStatus;

    /** Message or description of the latest status */
    @Schema(description = "Message or description of the latest status")
    private String message;

    /** Date of the latest status update (epoch milliseconds) */
    @Schema(description = "Date of the latest status update")
    private String latestStatusDate;
   }
