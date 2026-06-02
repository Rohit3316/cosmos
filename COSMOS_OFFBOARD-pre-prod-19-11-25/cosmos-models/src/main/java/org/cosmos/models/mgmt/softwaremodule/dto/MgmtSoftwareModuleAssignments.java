/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtId;

import jakarta.validation.constraints.NotNull;

/**
 * Request Body of SoftwareModule for assignment operations (ID only).
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleAssignments extends MgmtId {

    @JsonProperty(required = true)
    @Schema(example = "16")
    @NotNull
    private String softwareVersionTargetId;

    public MgmtSoftwareModuleAssignments() {
    }

    /**
     * Constructor
     *
     * @param softwareVersionTargetId
     *            ID of object
     */
    @JsonCreator
    public MgmtSoftwareModuleAssignments(final String softwareVersionTargetId) {
        this.softwareVersionTargetId = softwareVersionTargetId;
    }


    /**
     * @return the ID
     */
    public String getSoftwareVersionTargetId() {
        return softwareVersionTargetId;
    }

    /**
     * @param softwareVersionTargetId
     *            the ID to set
     */
    public void setSoftwareVersionTargetId(final String softwareVersionTargetId) {
        this.softwareVersionTargetId = softwareVersionTargetId;
    }
}
