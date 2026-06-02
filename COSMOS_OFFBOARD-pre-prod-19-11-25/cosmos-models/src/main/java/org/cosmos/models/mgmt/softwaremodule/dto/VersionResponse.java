/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for target PUT/POST users.
 *
 */
public class VersionResponse {
	
	@JsonProperty
	@Schema(example = "16")
	private long id;
	
    @JsonProperty(required = true)
    @Schema(example = "68541061AX00000000000000000008*670181252*133156002JY0000*")
    private String name;

    @JsonProperty(required = true)
    @Schema(example = "10")
    private Integer number;

    @JsonProperty(required = true)
    @Schema(example = "Example description")
    private String description;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

}
