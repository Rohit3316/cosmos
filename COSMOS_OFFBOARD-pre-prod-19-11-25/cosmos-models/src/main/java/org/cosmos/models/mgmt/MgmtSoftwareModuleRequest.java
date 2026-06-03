/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a module within a software module association request.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleRequest {

    @NotNull(message = "moduleId is mandatory for each module.")
    @Positive(message = "moduleId must be a positive integer.")
    @JsonProperty(required = true)
    @Schema(example = "1", description = "Software Module ID received from /GET api")
    public Long moduleId;

    @NotNull(message = "softwareVersionTargetId is mandatory for each module.")
    @Positive(message = "softwareVersionTargetId must be a positive integer.")
    @JsonProperty(required = true)
    @Schema(example = "6", description = "Target Software Version ID received from /GET api")
    public Long softwareVersionTargetId;

}