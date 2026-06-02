/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PUT/POST Software Module Versions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MgmtAddVersionRequestBody {
    @JsonProperty(required = true)
    @Schema(example = "68541061AX00000000000000000008*670181252*133156002JY0000*")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "16")
    private Integer number;

    @JsonProperty(required = true)
    @Schema(example = "Example description")
    private String description;
}
