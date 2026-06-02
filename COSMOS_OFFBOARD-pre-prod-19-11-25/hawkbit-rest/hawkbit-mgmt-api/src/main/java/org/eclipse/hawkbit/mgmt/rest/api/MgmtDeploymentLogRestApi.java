/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import java.io.InputStream;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.action.dto.MgmtDeploymentLog;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;


/**
 * REST API providing (read-only) access to deployment log.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Deployment Log", description = "REST API providing (read-only) access to deployment log.")
public interface MgmtDeploymentLogRestApi {


    /**
     * Handles the GET request of downloading a specific file {@link MgmtDeploymentLog} by controllerId, actionId and deploymentLogId
     * its <code>deploymentLogId</code>.
     *
     * @param controllerId The ID of the requested controller
     * @param actionId The ID of the requested action
     * @param deploymentLogId The ID of the requested deployment log
     * @return the {@link MgmtDeploymentLog}
     */
    @Operation(summary = "Download DeploymentLog by deploymentLogId", description = "Handles the GET request of downloading a single deploymentLog by deploymentLogId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Deployment log not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DOWNLOAD_DEPLOYMENT_LOG_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<InputStream> downloadDeploymentLog(@PathVariable("controllerId") final String controllerId, @PathVariable("actionId") final Long actionId, @PathVariable("deploymentLogId") final Long deploymentLogId);

}