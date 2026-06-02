/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A json annotated rest model for Userinfo to RESTful API representation.
 *
 */
public class MgmtUserInfo {

	@Schema(example = "xyz")
    private String username;
	@Schema(example = "default")
    private String tenant;

    /**
     * @return Username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username Username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Tenant
     */
    public String getTenant() {
        return tenant;
    }

    /**
     * @param tenant Tenant
     */
    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

}