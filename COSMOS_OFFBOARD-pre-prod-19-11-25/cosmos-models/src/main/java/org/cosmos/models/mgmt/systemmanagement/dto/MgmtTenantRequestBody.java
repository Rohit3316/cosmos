/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.systemmanagement.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;


/**
 * Model representation of an Cache entry as json.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTenantRequestBody {

    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    @Schema(example = "default")
    private String tenant;

    public MgmtTenantRequestBody(String tenant) {
        this.tenant = tenant;
    }

    public MgmtTenantRequestBody() {
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant() {
        return tenant;
    }

    @Override
    public String toString() {
        return "MgmtTenantRequestBody{" +
                "tenant='" + tenant + '\'' +
                '}';
    }
}
