/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body for target PUT/POST users.
 *
 */
public class MgmtCreateUserRequestBody {
    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    @Schema(example = "xyz")
    private String username;

    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    @Schema(example = "xyz")
    private String password;

    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    private String firstname;

    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    private String lastname;

    @JsonProperty(required = true)
    @NotNull
    @NotEmpty
    private List<Long> tenantIds;

    public List<Long> getTenantIds() {
        return tenantIds;
    }

    public void setTenantIds(List<Long> tenantIds) {
        this.tenantIds = tenantIds;
    }

    public String getUsername() {return username;}

    public void setUsername(final String username){ this.username = username;}

    public String getFirstname() {return firstname;}

    public void setFirstname(final String firstname){ this.firstname = firstname;}

    public String getLastname() {return lastname;}

    public void setLastname(final String lastname){ this.lastname = lastname;}

    public String getPassword() {return password;}

    public void setPassword(final String password){ this.password = password;}


}
