/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.user.dto;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body for target PUT/POST users.
 */
public class MgmtGetUserResponse {

	@Schema(example = "7")
    private Long id;
	@Schema(example = "xyz")
    private String username;
    private Map<String, String> tenant;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Map<String, String> getTenant() {
        return tenant;
    }

    public void setTenant(Map<String, String> tenant) {
        this.tenant = tenant;
    }
}
