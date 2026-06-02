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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * This class represents a Deployment Metadata in the Rollout model.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MgmtRolloutDeployment {

    /**
     * Required State of Charge (Battery) Conditions for Update.
     */
    @JsonProperty("requiredStateOfCharge")
    private Map<MgmtRolloutRequiredStateOfCharge, String> requiredStateOfCharge = new HashMap<>();

    /**
     * Specifies whether the download needs to happen from CDN or external USB.
     */
    @JsonProperty(value = "requiredMedia", defaultValue = "0")
    @Schema(example = "0")
    private MgmtRolloutRequiredMedia requiredMedia;

    /**
     * Specifies if the onboard is allowed to perform a downgrade if the artifacts provided
     * are lower in version than the existing version on the ECU.
     */
    @JsonProperty(value = "downgradeAllowed", defaultValue = "0")
    @Schema(example = "0")
    private MgmtRolloutDowngradeAllowed downgradeAllowed;

    /**
     * The estimated update time required to finish updates in seconds.
     */
    @Schema(example = "1000", description = "The estimated update time required to finish updates in seconds.")
    @JsonProperty(value = "estimatedUpdateTime", required = true)
    @PositiveOrZero
    private Integer estimatedUpdateTime;


}