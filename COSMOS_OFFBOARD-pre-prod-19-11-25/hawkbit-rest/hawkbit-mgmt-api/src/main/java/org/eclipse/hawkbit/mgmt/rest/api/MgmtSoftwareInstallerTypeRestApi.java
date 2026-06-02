/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.mgmt.rest.api;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.softwareinstallertype.dto.SoftwareInstallerTypeResponse;
import org.eclipse.hawkbit.rest.swagger.SwaggerConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * REST Resource handling for software installer types fetch operation.
 * It will be used by DOCG.
 */
@Tag(name = SwaggerConstants.SOFTWARE_INSTALLER_TYPES, description = SwaggerConstants.SOFTWARE_INSTALLER_TYPES_DESCRIPTION)
public interface MgmtSoftwareInstallerTypeRestApi {


    /**
     * Handles the GET request of retrieving all Software Installer Types .
     *
     * @return ALL Software Installer Type details with status code 200
     * responseEntity of list of Software Installer Types Response with status ok if successful.
     */
    @Operation(summary = "Returns the detailed List of All Software Installer Types", description = "Handles the GET request of retrieving All Software Installer Types. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All Software Installer Types successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "No Software Installer Types found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWARE_INSTALLER_TYPES_MAPPING)
    List<SoftwareInstallerTypeResponse> getSoftwareInstallerTypes(@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                                  @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam);

}
