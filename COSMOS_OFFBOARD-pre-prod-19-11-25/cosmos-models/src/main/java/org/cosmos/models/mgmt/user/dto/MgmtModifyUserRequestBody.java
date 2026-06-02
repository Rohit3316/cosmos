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

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Request body for target PUT/POST users.
 *
 */
public class MgmtModifyUserRequestBody {

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



}
