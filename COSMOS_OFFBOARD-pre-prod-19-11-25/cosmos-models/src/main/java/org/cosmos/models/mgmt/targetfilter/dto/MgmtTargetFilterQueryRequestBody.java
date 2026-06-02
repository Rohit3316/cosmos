/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.targetfilter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for target PUT/POST commands.
 *
 */
public class MgmtTargetFilterQueryRequestBody {
    @JsonProperty(required = true)
    @Schema(example = "filterName")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "controllerId==example-target-*")
    private String query;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }
}
