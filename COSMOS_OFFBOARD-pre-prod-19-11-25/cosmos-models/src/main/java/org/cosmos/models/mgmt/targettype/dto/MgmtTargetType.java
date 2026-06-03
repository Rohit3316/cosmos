/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.targettype.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtNamedEntity;

/**
 * A json annotated rest model for TargetType to RESTful API
 * representation.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetType extends MgmtNamedEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "26")
    private Long typeId;

    @JsonProperty
    @Schema(example = "rgb(255,255,255")
    private String colour;

    /**
     * @return target type ID
     */
    public Long getTypeId() {
        return typeId;
    }

    /**
     * @param typeId
     *          Target type ID
     */
    public void setTypeId(final Long typeId) {
        this.typeId = typeId;
    }

    /**
     * 
     * @return the colour
     */
    public String getColour() {
        return colour;
    }

    /**
     * @param colour
     *            the colour to set
     */
    public void setColour(String colour) {
        this.colour = colour;
    }
}
