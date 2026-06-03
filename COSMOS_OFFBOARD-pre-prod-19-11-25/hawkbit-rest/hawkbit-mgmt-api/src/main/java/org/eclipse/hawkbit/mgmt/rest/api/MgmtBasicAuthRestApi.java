/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.auth.dto.MgmtUserInfo;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Api for handling basic auth user validation
 */
@SuppressWarnings("squid:S1609")
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Basic Authentication", description = "API for basic auth user validation.")
public interface MgmtBasicAuthRestApi {
    /**
     * Handles the GET request of basic auth.
     *
     * @return the userinfo with status OK.
     */
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Validate Basic Authentication",
            description = "Retrieve user information from Basic Authentication"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Basic Authentication validated successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = MgmtUserInfo.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error"
            )
    })
    @GetMapping(value = MgmtRestConstants.AUTH_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    @Deprecated
    ResponseEntity<MgmtUserInfo> validateBasicAuth();
}
