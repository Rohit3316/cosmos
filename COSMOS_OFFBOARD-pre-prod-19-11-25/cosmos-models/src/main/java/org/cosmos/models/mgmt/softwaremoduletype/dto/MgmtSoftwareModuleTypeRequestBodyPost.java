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
 * Request Body for SoftwareModuleType POST.
 *
 */
public class MgmtSoftwareModuleTypeRequestBodyPost extends MgmtSoftwareModuleTypeRequestBodyPut {

    @JsonProperty(required = true)
    @Schema(example = "Example name")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "Example key")
    private String key;


    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setDescription(final String description) {
        super.setDescription(description);
        return this;
    }

    @Override
    public MgmtSoftwareModuleTypeRequestBodyPost setColour(final String colour) {
        super.setColour(colour);
        return this;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     *            the name to set
     *
     * @return updated body
     */
    public MgmtSoftwareModuleTypeRequestBodyPost setName(final String name) {
        this.name = name;
        return this;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     * @return updated body
     */
    public MgmtSoftwareModuleTypeRequestBodyPost setKey(final String key) {
        this.key = key;
        return this;
    }

}
