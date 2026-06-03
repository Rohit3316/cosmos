/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.cosmos.models.mgmt.MgmtNamedEntity;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the response body for a management rollout.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@JsonPropertyOrder({"rolloutId"})
public class MgmtRolloutResponseBody extends MgmtNamedEntity {

    /**
     * The ID of the rollout.
     */
    @JsonProperty(value = "id", required = true)
    @Schema(example = "2")
    private Long rolloutId;

    /**
     * The name of the rollout.
     */
    @JsonProperty(required = true)
    @Schema(example = "Name of entity")
    private String name;

    /**
     * The priority of the rollout.
     */
    @JsonProperty
    @Schema(example = "regular")
    private MgmtRolloutPriority priority;

    /**
     * The start type of the rollout.
     */
    @JsonProperty
    @Schema(example = "manual")
    private MgmtRolloutStartType startType;


    /**
     * The status of the rollout.
     */
    @JsonProperty(required = true)
    @Schema(example = "ready")
    private String status;

    /**
     * Indicates if user acceptance is required for the rollout.
     */
    @Schema(example = "yes")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    /**
     * The start time of the rollout.
     */
    @JsonProperty
    @Schema(example = "1691065753")
    private Long startAt;

    /**
     * The end time of the rollout.
     */
    @JsonProperty
    @Schema(example = "1691065753")
    private Long endAt;

    /**
     * The connectivity type for the rollout.
     */
    @JsonProperty
    @Schema(example = "wifi_preferred")
    private MgmtRolloutConnectivityType connectivityType;

    /**
     * The total number of groups associated with the rollout.
     * Example: 1
     */
    @JsonProperty
    @Schema(example = "1")
    private int totalGroups;

    /**
     * The list of module versions included in the rollout.
     * Each item in the list contains the module ID and its associated software version target ID.
     */
    @JsonProperty("modules")
    private List<MgmtRolloutModuleVersion> modules;

    /**
     * The total number of targets associated with the rollout.
     */
    @JsonProperty("totalTargets")
    private Long totalTargets;

    /**
     * A map representing the breakdown of targets by their statuses.
     * The key represents the status (e.g., "running", "finished"), and the value represents the count of targets in that status.
     */
    @JsonProperty("totalTargetsPerStatus")
    private Map<String, Long> totalTargetsPerStatus = new HashMap<>();


    /**
     * The log request details for the rollout.
     */
    private MgmtRolloutLogRequest log;

    /**
     * The deployment metadata for the rollout.
     */
    private MgmtRolloutDeployment deploymentMetadata;

    /**
     * The number of retry attempts for downloading.
     */
    private Integer downloadRetryCount;

    /**
     * The maximum duration for downloading.
     */
    private Integer maxDownloadDurationTimer;

    /**
     * The maximum duration for downloading over WiFi.
     */
    private Integer maxDownloadWifiDurationTimer;

    /**
     * The maximum duration for downloading over cellular.
     */
    private Integer maxDownloadCellularDurationTimer;

    /**
     * The maximum time for updating.
     */
    @Schema(example = "1800")
    private Integer maxUpdateTime;


    /**
     * The maximum allowed package size in bytes for ESP, RSP, and artifacts.
     * Example: 104857600 (100 MB)
     */
    @Schema(description = "Maximum allowed package size in bytes for ESP, RSP, and artifacts", example = "104857600")
    @JsonProperty("maxPackageSize")
    private Long maxPackageSize;

    /**
     * The rollout metadata.
     */
    @JsonProperty("rolloutMetaData")
    private  MgmtRolloutMetaData rolloutMetaData;


}