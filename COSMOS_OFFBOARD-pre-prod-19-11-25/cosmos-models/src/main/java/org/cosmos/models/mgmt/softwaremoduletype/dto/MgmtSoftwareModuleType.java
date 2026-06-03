/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremoduletype.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtNamedEntity;

/**
 * A json annotated rest model for SoftwareModuleType to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleType extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "83")
    private Long moduleId;

    @JsonProperty(required = true)
    @Schema(example = "OS")
    private String key;

    @JsonProperty
    @Schema(example = "1")
    private Integer maxAssignments;

    @JsonProperty
    @Schema(example = "false")
    private boolean deleted;

    @JsonProperty
    @Schema(example = "brown")
    private String colour;

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(final Long moduleId) {
        this.moduleId = moduleId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public Integer getMaxAssignments() {
        return maxAssignments;
    }

    public void setMaxAssignments(final Integer maxAssignments) {
        this.maxAssignments = maxAssignments;
    }

    public String getColour() {
        return colour;
    }

    public void setColour(String colour) {
        this.colour = colour;
    }
}
