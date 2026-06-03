/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionsettype.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request Body for DistributionSetType PUT, i.e. update.
 *
 */
public class MgmtDistributionSetTypeRequestBodyPut {

    @JsonProperty
    @Schema(example = "Example description")
    private String description;

    @JsonProperty
    @Schema(example = "rgb(86,37,99)")
    private String colour;

    public String getDescription() {
        return description;
    }

    public MgmtDistributionSetTypeRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

    public String getColour() {
        return colour;
    }

    public MgmtDistributionSetTypeRequestBodyPut setColour(final String colour) {
        this.colour = colour;
        return this;
    }

}
