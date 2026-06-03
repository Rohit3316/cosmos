/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Model for log request details in a rollout.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MgmtRolloutLogRequest {

    /**
     * Indicates if log collection is required for this rollout.
     */
    @Schema(description = "Log collection requirement for this rollout", required = true, example = "true")
    @JsonProperty("collectionRequired")
    private Boolean collectionRequired;

    /**
     * Maximum number of VINs to collect the success logs for.
     */
    @Schema(description = "Maximum no. of VIN to collect the success logs for", required = true, example = "100")
    @JsonProperty("maxSuccessVin")
    @PositiveOrZero
    private Integer maxSuccessVin;

    /**
     * Maximum number of VINs to collect the failure logs for.
     */
    @Schema(description = "Maximum no. of VIN to collect the failure logs for", required = true,example = "100")
    @JsonProperty("maxFailureVin")
    @PositiveOrZero
    private Integer maxFailureVin;

    /**
     * Maximum size of individual log file in bytes.
     */
    @Schema(description = "Maximum size of individual log file in bytes", required = true, example = "1048576")
    @JsonProperty("maxEachFileSize")
    @PositiveOrZero
    private Integer maxEachFileSize;

    /**
     * Maximum size of all log files in bytes.
     */
    @Schema(description = "Maximum size of all log files in bytes", required = true, example = "10485760")
    @JsonProperty("maxAllFileSize")
    @PositiveOrZero
    private Integer maxAllFileSize;

    /**
     * Maximum number of files to request per VIN.
     */
    @Schema(description = "Maximum number of files to request per VIN minimum is 1", required = true, example = "5")
    @JsonProperty("maxNumberOfFiles")
    @Min(1)
    private Integer maxNumberOfFiles;

    /**
     * Log level.
     */
    @Schema(description = "Log level", required = false, example = "4")
    @JsonProperty("level")
    private Integer level;
}
