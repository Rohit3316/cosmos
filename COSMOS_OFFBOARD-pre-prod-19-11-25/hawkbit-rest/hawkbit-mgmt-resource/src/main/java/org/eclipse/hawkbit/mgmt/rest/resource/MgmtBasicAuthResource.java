/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.models.mgmt.auth.dto.MgmtUserInfo;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtBasicAuthRestApi;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Resource handling basic auth validation.
 */
@RestController
@Tag(name = "Basic Authentication")
public class MgmtBasicAuthResource implements MgmtBasicAuthRestApi {

    private final TenantAware tenantAware;

    /**
     * Default constructor
     *
     * @param tenantAware
     *          tenantAware
     */
    public MgmtBasicAuthResource(TenantAware tenantAware) {
        this.tenantAware = tenantAware;
    }

    @Override
    @Deprecated
    public ResponseEntity<MgmtUserInfo> validateBasicAuth() {
        MgmtUserInfo userInfo = new MgmtUserInfo();
        userInfo.setUsername(tenantAware.getCurrentUsername());
        userInfo.setTenant(tenantAware.getCurrentTenant());
        return ResponseEntity.ok(userInfo);
    }
}
