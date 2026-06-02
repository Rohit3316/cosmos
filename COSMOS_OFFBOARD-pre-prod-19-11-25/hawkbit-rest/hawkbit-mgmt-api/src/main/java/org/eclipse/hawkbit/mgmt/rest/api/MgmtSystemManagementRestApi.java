/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.util.Collection;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtSystemCache;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtSystemStatisticsRest;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * System management capabilities by REST.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "System Configuration", description = "REST API for handling system specific configuration operations.")
public interface MgmtSystemManagementRestApi {

    /**
     * Collects and returns system usage statistics. It provides a system wide
     * overview and tenant based stats.
     *
     * @return system usage statistics
     */
    @GetMapping(value = MgmtRestConstants.SYSTEM_STATISTICS_V1_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSystemStatisticsRest> getSystemUsageStats();

    /**
     * Returns a list of all caches.
     *
     * @return a list of caches for all tenants
     */
    @GetMapping(value = MgmtRestConstants.SYSTEM_CACHE_V1_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Collection<MgmtSystemCache>> getCaches();

    /**
     * Invalidates all caches for all tenants.
     *
     * @return a list of cache names which has been invalidated
     */
    @DeleteMapping(value = MgmtRestConstants.SYSTEM_CACHE_V1_MAPPING)
    ResponseEntity<Collection<String>> invalidateCaches();

}
