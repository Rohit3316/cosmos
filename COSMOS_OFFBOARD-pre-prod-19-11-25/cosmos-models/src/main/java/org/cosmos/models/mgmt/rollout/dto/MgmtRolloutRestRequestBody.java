/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.cosmos.annotations.TraceableField;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;

import java.util.List;

/**
 * Model for request containing a rollout body e.g. in a POST request of
 * creating a rollout via REST API.
 */
@EqualsAndHashCode(callSuper = false)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class MgmtRolloutRestRequestBody extends AbstractMgmtRolloutConditionsEntity {

    /**
     * The name of the rollout.
     */
    @JsonProperty(required = true)
    @Schema(example = "R2PM_U29.18_U29.44_NA_CUS-LARGE_20240327_X", description = "Rollout Name")
    @TraceableField("rolloutName")
    private String name;

    /**
     * The priority of the rollout.
     */
    @Schema(example = "regular", description = "Used to set the Rollout Priority, " +
            "which can be one of the following: regular, critical, urgent ordered by priority level.")
    @JsonProperty("priority")
    private MgmtRolloutPriority priority = MgmtRolloutPriority.REGULAR;

    /**
     * The execution type of the rollout.
     */
    @Schema(example = "manual", description = "Used to set the Rollout Start Type, " +
            "which can be one of the following: manual, auto, scheduled.")
    @JsonProperty("startType")
    private MgmtRolloutStartType startType = MgmtRolloutStartType.MANUAL;

    /**
     * Indicates if user acceptance is required for the rollout.
     */
    @Schema(example = "yes", description = "Indicates if user acceptance is required for the rollout. " +
            "Possible values are: yes, no, optional.")
    @JsonProperty("userAcceptanceRequired")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired = MgmtRolloutUserAcceptanceRequired.YES;

    /**
     * The start date of the rollout in seconds since epoch.
     */
    @Schema(example = "1759741859", description = "The start date of the rollout in seconds since epoch. ")
    @JsonProperty("startDate")
    private Long startDate;

    /**
     * The end date of the rollout in seconds since epoch.
     */
    @JsonProperty(required = true)
    @Schema(example = "1759741859", description = "The end date of the rollout in seconds since epoch. ")
    @TraceableField("rolloutEndDate")
    private Long endDate;

    /**
     * The connectivity type for the rollout.
     */
    @Schema(example = "wifi_preferred")
    @JsonProperty("connectivityType")
    private MgmtRolloutConnectivityType connectivityType = MgmtRolloutConnectivityType.WIFI_PREFERRED;

    /**
     * The log request details for the rollout.
     */
    @JsonProperty("log")
    @Schema(description = "This contains all the details of log collection parameters for the rollout.")
    @Valid
    private MgmtRolloutLogRequest log = new MgmtRolloutLogRequest();

    /**
     * The deployment metadata for the rollout.
     */
    @JsonProperty("deploymentMetadata")
    private MgmtRolloutDeployment deploymentMetadata = new MgmtRolloutDeployment();

    /**
     * The number of retry attempts allowed for download.
     */
    @JsonProperty("downloadRetryCount")
    @PositiveOrZero
    @Schema(description = "The number of retry attempts allowed for download.", example = "3")
    private Integer downloadRetryCount;

    /**
     * The maximum number of days allowed for download.
     */
    @JsonProperty("maxDownloadDurationTimer")
    @PositiveOrZero
    @Schema(description = "The maximum number of days allowed for download.", example = "7")
    private Integer maxDownloadDurationTimer;

    /**
     * The maximum number of days allowed for download over WiFi only.
     */
    @JsonProperty("maxDownloadWifiDurationTimer")
    @PositiveOrZero
    @Schema(description = "The maximum number of days allowed for download over WiFi only.", example = "3")
    private Integer maxDownloadWifiDurationTimer;

    /**
     * The maximum number of days allowed for download over cellular only.
     */
    @JsonProperty("maxDownloadCellularDurationTimer")
    @PositiveOrZero
    @Schema(description = "The maximum number of days allowed for download over cellular only.", example = "2")
    private Integer maxDownloadCellularDurationTimer;

    /**
     * The maximum update time required to finish updates in seconds.
     */
    @Schema(example = "1800", description = "The maximum update time required to finish updates in seconds.")
    @JsonProperty("maxUpdateTime")
    @PositiveOrZero
    private Integer maxUpdateTime;


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
     */
    @Schema(example = "4",description = "The vehicle log level for the rollout.")
    @JsonProperty(required = false)
    private Integer vehicleLogLevel;

}
