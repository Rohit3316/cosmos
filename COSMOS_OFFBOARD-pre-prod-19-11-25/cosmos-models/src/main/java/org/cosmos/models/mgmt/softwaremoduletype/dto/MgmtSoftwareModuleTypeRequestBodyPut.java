/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremoduletype.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request Body for SoftwareModuleType PUT.
 *
 */
public class MgmtSoftwareModuleTypeRequestBodyPut {

    @JsonProperty
    @Schema(example = "Example description")
    private String description;

    @JsonProperty
    @Schema(example = "rgb(0,0,255")
    private String colour;

    @JsonProperty
    @Schema(example = "16")
    private Integer maxAssignments;


    public String getDescription() {
        return description;
    }

    public MgmtSoftwareModuleTypeRequestBodyPut setDescription(final String description) {
        this.description = description;
        return this;
    }

    public String getColour() {
        return colour;
    }

    public MgmtSoftwareModuleTypeRequestBodyPut setColour(final String colour) {
        this.colour = colour;
        return this;
    }

    /**
     * @return the maxAssignments
     */
    public Integer getMaxAssignments() {
        return maxAssignments;
    }

    /**
     * @param maxAssignments
     *            the maxAssignments to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleTypeRequestBodyPut setMaxAssignments(final Integer maxAssignments) {
        if (maxAssignments != null && maxAssignments > 0){
            this.maxAssignments = maxAssignments;
        }
        return this;
    }

}
