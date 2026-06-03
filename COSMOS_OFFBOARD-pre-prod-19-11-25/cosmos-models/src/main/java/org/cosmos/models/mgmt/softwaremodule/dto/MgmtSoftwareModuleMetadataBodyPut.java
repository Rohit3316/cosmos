/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The representation of an meta data in the REST API for PUT/Update.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtSoftwareModuleMetadataBodyPut {

    @JsonProperty
    @Schema(example = "68541061AA000000000000000000007670181252*133156002JY0000*")
    private String value;
    @JsonProperty
    @Schema(example = "true")
    private Boolean targetVisible;

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }

    public Boolean isTargetVisible() {
        return targetVisible;
    }

    public void setTargetVisible(final Boolean targetVisible) {
        this.targetVisible = targetVisible;
    }
}
