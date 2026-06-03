package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

import jakarta.validation.Valid;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;


/**
 * This class represents the request body for updating a rollout.
 * It includes fields that capture the details required for modifying a rollout,
 * including its description, start and end times, log settings, and other rollout-specific parameters.
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = false)
public class MgmtRolloutUpdateRequest {


    /**
     * The description of the rollout update.
     * This field is optional and can be used to provide additional context about the rollout.
     */
    private String description;

    /**
     * The priority of the rollout update.
     * This field indicates the importance of the rollout, with higher values representing higher priority.
     */

    private MgmtRolloutPriority priority;

    /**
     * The start type of the rollout update.
     * This field indicates how the rollout should be initiated, such as manually or automatically.
     */

    private MgmtRolloutStartType startType;

    /**
     * The forced time for the rollout, representing when the rollout should be executed,
     * even if certain conditions aren't met.
     */
    private Long forcedTime;

    /**
     * The weight of the rollout, which could represent its priority or the amount of work it involves.
     */
    private Integer weight;

    /**
     * The start time of the rollout, represented as a timestamp (in milliseconds).
     */
    private Long startAt;

    /**
     * The end time of the rollout, represented as a timestamp (in milliseconds).
     * This field is required for the update request.
     */
    private Long endAt;

    /**
     * The number of retry attempts for downloading the rollout content.
     */
    private Integer downloadRetryCount;

    /**
     * The maximum allowed duration for downloading the rollout content (in seconds).
     */
    private Integer maxDownloadDurationTimer;

    /**
     * The maximum allowed duration for downloading the rollout content over Wi-Fi (in seconds).
     */
    private Integer maxDownloadWifiDurationTimer;

    /**
     * The maximum allowed duration for downloading the rollout content over cellular network (in seconds).
     */
    private Integer maxDownloadCellularDurationTimer;

    /**
     * Indicates whether user acceptance is required before the rollout can begin.
     */
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    /**
     * The type of connectivity required for the rollout, such as cellular or Wi-Fi.
     */
    private MgmtRolloutConnectivityType connectivityType;

    /**
     * The maximum amount of time the rollout can take before it must be completed.
     */
    private Integer maxUpdateTime;

    /**
     * The log request details for the rollout.
     */
    @JsonProperty("log")
    @Valid
    private MgmtRolloutLogRequest log = new MgmtRolloutLogRequest();

    /**
     * The deployment metadata for the rollout.
     */
    @JsonProperty("deploymentMetadata")
    private MgmtRolloutDeployment deploymentMetadata;

    /**
     * Maximum allowed package size in bytes for ESP, RSP, and artifacts.
     * Must be zero or positive. Example: 104857600 (100 MB).
     */
    @Schema(
            description = "Maximum allowed package size in bytes for ESP, RSP, and artifacts. Must be zero or positive. Example: 104857600 (100 MB).",
            example = "104857600"
    )
    @JsonProperty(value = "maxPackageSize", required = false)
    @PositiveOrZero(message = "maxPackageSize must be zero or a positive value")
    private Long maxPackageSize;

    /**
     * The type of the rollout.
     */
    @Schema(example = "FOTA", description = "Used to set the Rollout Type, " +
            "which can be one of the following: FOTA  (firmware update), AOTA (application update).")
    @JsonProperty("type")
    @NotNull(message = "type is required")
    private MgmtRolloutType type;

    /**
     * The update action for the rollout.
     * Only applicable when "type" is AOTA.
     */
    @Schema(example = "INSTALL", description = "Used to set the Update Action for AOTA rollouts. " +
            "Possible values: INSTALL, UNINSTALLANY, UNINSTALLSPECIFIC. Only valid if type == AOTA.")
    @JsonProperty("updateAction")
    private MgmtUpdateAction updateAction;

    /**
     * List of software versions to uninstall.
     * Only required when "updateAction" is UNINSTALLSPECIFIC.
     */
    @Schema(description = "List of software versions to uninstall. " +
            "At least one version should be provided when updateAction == UNINSTALLSPECIFIC.")
    @JsonProperty("updateActionUninstallVersion")
    private List<String> updateActionUninstallVersion;

    /**
     * The vehicle log level for the rollout.
     * This field indicates the verbosity of logs to be collected from the vehicle during the rollout.
     */
    @Schema(example = "4", description = "The vehicle log level for the rollout.")
    @JsonProperty(required = false)
    private Integer vehicleLogLevel;

}
