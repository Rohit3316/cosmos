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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.action.dto.DeviceActionStatusTimestampResponse;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryIndividualDeviceRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutResponseBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutTargetActionsResponse;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutUpdateRequest;
import org.cosmos.models.mgmt.rollout.dto.RetryMultipleDevicesRequest;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupResponseBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssociationResponse;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;


/**
 * REST Resource handling rollout CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Rollouts", description = "REST API for Rollout CRUD operations.")
public interface MgmtRolloutRestApi {

    /**
     * Handles the GET request of retrieving all rollouts.
     *
     * @param pagingOffsetParam       the offset of list of rollouts for pagination, might not be
     *                                present in the rest request then default value will be applied
     * @param pagingLimitParam        the limit of the paged request, might not be present in the
     *                                rest request then default value will be applied
     * @param sortParam               the sorting parameter in the request URL, syntax
     *                                {@code field:direction, field:direction}
     * @param rsqlParam               the search parameter in the request URL, syntax
     *                                {@code q=name==abc}
     * @param representationModeParam the representation mode parameter specifying whether a compact
     *                                or a full representation shall be returned
     * @return a list of all rollouts for a defined or default page request with
     * status OK. The response is always paged. In any failure the
     * JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Rollouts", description = "Handles the GET request of retrieving all rollouts. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(
            @PathVariable("tenantId") Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT) String representationModeParam);

    /**
     * Handles the GET request of retrieving a single rollout.
     *
     * @param rolloutId the ID of the rollout to retrieve
     * @return a single rollout with status OK.
     */
    @Operation(summary = "Return single Rollout", description = "Handles the GET request of retrieving a single rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request for creating rollout.
     *
     * @param rolloutRequestBody the rollout body to be created.
     * @return In case rollout could successful created the ResponseEntity with
     * status code 201 with the successfully created rollout. In any
     * failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Operation(summary = "Create a new Rollout", description = "Handles the POST request of creating new rollout. Required Permission: CREATE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, consumes = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtRolloutResponseBody> create(@PathVariable("tenantId") Long tenantId,
                                                   @NotNull @Valid MgmtRolloutRestRequestBody rolloutRequestBody);

    /**
     * Handles the request for approving a rollout.
     *
     * @param rolloutId the ID of the rollout to be approved.
     * @param remark    an optional remark on the approval decision
     * @return OK response (200) if rollout is approved now. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Approve a Rollout", description = "Handles the PUT request of approving a created rollout. Only possible if approval workflow is enabled in system configuration and rollout is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully approved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @Deprecated
    @PutMapping(value = MgmtRestConstants.ROLLOUT_APPROVE_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> approve(@PathVariable("tenantId") Long tenantId,
                                 @PathVariable("rolloutId") Long rolloutId,
                                 @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the request for denying the approval of a rollout.
     *
     * @param rolloutId the ID of the rollout to be denied.
     * @param remark    an optional remark on the denial decision
     * @return OK response (200) if rollout is denied now. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Deny a Rollout", description = "Handles the PUT request of denying a created rollout. Only possible if approval workflow is enabled in system configuration and rollout is in state WAITING_FOR_APPROVAL. Required Permission: APPROVE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully denied"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @Deprecated
    @PutMapping(value = MgmtRestConstants.ROLLOUT_DENY_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> deny(@PathVariable("tenantId") Long tenantId,
                              @PathVariable("rolloutId") Long rolloutId,
                              @RequestParam(value = "remark", required = false) String remark);

    /**
     * Handles the update request for freezing a rollout.
     *
     * @param rolloutId the ID of the rollout to be frozen.
     * @return OK response (200) if rollout moved to Ready state. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Freeze a Rollout", description = "Handles the PUT request of freezing a rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully moved to Ready state"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> freeze(@PathVariable("tenantId") Long tenantId,
                                @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the update request for starting a rollout.
     *
     * @param rolloutId the ID of the rollout to be started.
     * @return OK response (200) if rollout could be started. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Start a Rollout", description = "Handles the PUT request of starting a created rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully started"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_START_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> start(@PathVariable("tenantId") Long tenantId,
                               @PathVariable("rolloutId") Long rolloutId);

    @Operation(summary = "Pause a device action", description = "Handles the PUT request of pausing a running device action. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully paused"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> pauseDeviceAction(@PathVariable("tenantId") Long tenantId,
                                           @PathVariable("rolloutId") Long rolloutId,
                                           @PathVariable("controllerId") String controllerId);

    /**
     * Handles the cancel request for canceling a deviceAction.
     *
     * @param tenantId
     * @param rolloutId
     * @param controllerId
     * @return OK response (200) if cancel deviceAction is success
     * In case of any exception the corresponding errors occur.
     */
    @Operation(summary = "Cancel a device action", description = "Handles the PUT request of canceling a running/paused or dd_sent device action. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Successfully Accepted cancel Request"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict, Device is already canceled.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> cancelDeviceAction(@PathVariable("tenantId") Long tenantId,
                                            @PathVariable("rolloutId") Long rolloutId,
                                            @PathVariable("controllerId") String controllerId);

    /**
     * Handles the update request for pausing a rollout.
     *
     * @param rolloutId the ID of the rollout to be paused.
     * @return OK response (200) if rollout could be paused. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Pause a Rollout", description = "Handles the PUT request of pausing a running rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Successfully Accepted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> pause(@PathVariable("tenantId") Long tenantId,
                               @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the update request for cancelling a rollout.
     *
     * @param rolloutId the ID of the rollout to be canceled.
     * @return OK response (200) if rollout could be canceled. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Cancel a Rollout", description = "Handles the PUT request of cancelling a running rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully cancelled"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> cancel(@PathVariable("tenantId") Long tenantId,
                                @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the DELETE request for deleting a rollout.
     *
     * @param rolloutId the ID of the rollout to be deleted.
     * @return OK response (200) if rollout could be deleted. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Delete a Rollout", description = "Handles the DELETE request of deleting a rollout. Required Permission: DELETE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> delete(@PathVariable("tenantId") Long tenantId,
                                @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the update request for resuming a rollout.
     *
     * @param rolloutId the ID of the rollout to be resumed.
     * @return OK response (200) if rollout could be resumed. In case of any
     * exception the corresponding errors occur.
     */
    @Operation(summary = "Resume a Rollout", description = "Handles the PUT request of resuming a paused rollout. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully resumed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> resume(@PathVariable("tenantId") Long tenantId,
                                @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the GET request of retrieving all rollout groups referred to a
     * rollout.
     *
     * @param pagingOffsetParam       the offset of list of rollout groups for pagination, might not
     *                                be present in the rest request then default value will be
     *                                applied
     * @param pagingLimitParam        the limit of the paged request, might not be present in the
     *                                rest request then default value will be applied
     * @param sortParam               the sorting parameter in the request URL, syntax
     *                                {@code field:direction, field:direction}
     * @param rsqlParam               the search parameter in the request URL, syntax
     *                                {@code q=name==abc}
     * @param representationModeParam the representation mode parameter specifying whether a compact
     *                                or a full representation shall be returned
     * @return a list of all rollout groups referred to a rollout for a defined
     * or default page request with status OK. The response is always
     * paged. In any failure the JsonResponseExceptionHandler is
     * handling the response.
     */
    @Operation(summary = "Return all rollout groups referred to a Rollout", description = "Handles the GET request of retrieving all deploy groups of a specific rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_GROUP_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(@PathVariable("tenantId") Long tenantId,
                                                                             @PathVariable("rolloutId") Long rolloutId,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE_DEFAULT) String representationModeParam);


    @Operation(summary = "Add Device Details", description = "Handles the POST request of Adding details to a rollout ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, consumes =
            MediaType.MULTIPART_FORM_DATA_VALUE, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtAddDeviceDetailsResponse> addDeviceDetailsApi(@PathVariable("tenantId") Long tenantId,
                                                                     @PathVariable("rolloutId") Long rolloutId,
                                                                     @RequestPart("targetDevices") MultipartFile targetDevices,
                                                                     @RequestParam(value = "groups", required = false) String groups);


    @Operation(summary = "Delete Device Details", description = "Handles the DELETE request to remove device details from a specific rollout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device Deletion Successful"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> deleteDeviceDetailsApi(@PathVariable("tenantId") Long tenantId,
                                                @PathVariable("rolloutId") Long rolloutId,
                                                @RequestPart("targetDevices") MultipartFile targetDevices,
                                                @RequestParam(value = "deleteEsp", required = false, defaultValue = "false") Boolean deleteEsp);

    /**
     * Handles the GET request for retrieving a single rollout group.
     *
     * @param rolloutId the rolloutId to retrieve the group from
     * @param groupId   the groupId to retrieve the rollout group
     * @return the OK response containing the MgmtRolloutGroupResponseBody
     */
    @Operation(summary = "Return single rollout group", description = "Handles the GET request of a single deploy group of a specific rollout. Required Permission: READ_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_GROUP_ID_V1_REQUEST_MAPPING_TENANT, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(@PathVariable("tenantId") Long tenantId,
                                                                 @PathVariable("rolloutId") Long rolloutId,
                                                                 @PathVariable("groupId") Long groupId);

    /**
     * Retrieves all targets related to a specific rollout group.
     *
     * @param rolloutId         the ID of the rollout
     * @param groupId           the ID of the rollout group
     * @param pagingOffsetParam the offset of list of rollout groups for pagination, might not
     *                          be present in the rest request then default value will be
     *                          applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param rsqlParam         the search parameter in the request URL, syntax
     *                          {@code q=name==abc}
     * @return a paged list of targets related to a specific rollout and rollout
     * group.
     */
    @Operation(summary = "Return all targets related to a specific rollout group", description = "Handles the GET request of retrieving all targets of a single deploy group of a specific rollout. Required Permissions: READ_ROLLOUT, READ_TARGET.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_GROUP_TRG_V1_REQUEST_MAPPING_TENANT, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(@PathVariable("tenantId") Long tenantId,
                                                                 @PathVariable("rolloutId") Long rolloutId,
                                                                 @PathVariable("groupId") Long groupId,
                                                                 @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                                 @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
                                                                 @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                                 @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the POST request to force trigger processing next group of a
     * rollout even success threshold isn't yet met
     *
     * @param rolloutId the ID of the rollout to trigger next group.
     * @return OK response (200). In case of any exception the corresponding
     * errors occur.
     */
    @Operation(summary = "Force trigger processing next group of a Rollout", description = "Handles the POST request of triggering the next group of a rollout. Required Permission: UPDATE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully triggered"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> triggerNextGroup(@PathVariable("tenantId") Long tenantId,
                                          @PathVariable("rolloutId") Long rolloutId);

    /**
     * Handles the POST request to force trigger processing of all the groups of a
     * rollout even success threshold isn't yet met
     *
     * @param rolloutId the ID of the rollout.
     * @return Accepted response (202). In case of any exception the corresponding
     * errors occur.
     */
    @Operation(summary = "Force start processing all the groups of a Rollout", description = "Handles the POST request to start all the groups of a rollout. Required Permission: TENANT_ADMIN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Successfully accepted to start all the groups"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_GROUP_START_ALL_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> startAllGroups(@PathVariable("tenantId") Long tenantId,
                                        @PathVariable("rolloutId") Long rolloutId);

    /**
     * Unfreezes a rollout by changing its state from READY to DRAFT.
     *
     * @param tenantId  the ID of the tenant
     * @param rolloutId the ID of the rollout to unfreeze
     * @return a ResponseEntity with status code 200 if the operation is successful
     */
    @Operation(summary = "Unfreeze a Rollout", description = "Changes the rollout state from READY to DRAFT. Required Permission: HANDLE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully unfroze the rollout"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e., read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_UNFREEZE_V1_REQUEST_MAPPING_TENANT, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> unfreeze(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") Long rolloutId);


    @Operation(
            summary = "Resume a device action",
            description = "Handles the PUT request for resuming a paused device action. Required Permission: HANDLE_ROLLOUT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully resumed"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (e.g., read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict due to simultaneous modifications.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> resumeDeviceAction(@PathVariable("tenantId") Long tenantId,
                                            @PathVariable("rolloutId") Long rolloutId,
                                            @PathVariable("controllerId") String controllerId);

    /**
     * Handles the POST request of associating software modules to a rollout.
     *
     * @param tenantId               the ID of the tenant
     * @param rolloutId              the ID of the rollout to which the software modules will be associated
     * @param softwareModuleRequests the body of the request containing the action and the list of software modules
     * @return an empty response with status OK if the operation was successful.
     */
    @Operation(summary = "Associate Software Modules to Target Version",
            description = "Handles the POST request of associating software modules to a rollout. Required Permission: WRITE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully Associated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions or entity is not allowed to be changed.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software module or target version not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case the accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict due to simultaneous modifications.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, consumes = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtSoftwareModuleAssociationResponse> associateSoftwareModules(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") Long rolloutId, @Valid @RequestBody List<MgmtSoftwareModuleRequest> softwareModuleRequests);

    /**
     * Handles the DELETE request of unlinking software modules from a rollout.
     *
     * @param tenantId               the ID of the tenant
     * @param rolloutId              the ID of the rollout from which the software modules will be unlinked
     * @param softwareModuleRequests the body of the request containing the list of software modules to unlink
     * @return an empty response with status OK if the operation was successful.
     */
    @Operation(summary = "Unlink Software Modules from a Rollout",
            description = "Handles the request to unlink software modules and target versions from a rollout.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully Unlinked"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions or entity is not allowed to be changed.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software module, target version, or rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case the accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict due to simultaneous modifications.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/softwares", consumes = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> unlinkSoftwareModules(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") Long rolloutId, @Valid @RequestBody List<MgmtSoftwareModuleRequest> softwareModuleRequests);

    /**
     * Handles the PUT request to update the rollout details for a specific tenant.
     *
     * @param tenantId       the ID of the tenant
     * @param rolloutId      the ID of the rollout to be updated
     * @param rolloutRequest the body of the request containing the rollout details
     * @return an empty response with status OK if the operation was successful.
     */
    @Operation(summary = "Update Rollout Details",
            description = "Handles the request to update the rollout details for a specific tenant.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "successfully resumed the device actions"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found - Rollout or tenant does not exist.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case the accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict due to simultaneous modifications.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}",
            consumes = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtRolloutResponseBody> updateRollout(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") Long rolloutId, @Valid @RequestBody MgmtRolloutUpdateRequest rolloutRequest);

    /**
     * Deletes a specific group identified by `groupId` from the given rollout identified by `rolloutId`.
     * Validates whether the group exists and belongs to the specified rollout, and performs the deletion if successful.
     *
     * @param tenantId  the ID of the tenant
     * @param rolloutId the ID of the rollout that the group belongs to
     * @param groupId   the ID of the group to be deleted
     * @return a ResponseEntity with HTTP status 200 (OK) if the group is successfully deleted
     * @throws EntityNotFoundException if the group does not exist, does not belong to the specified rollout,
     *                                 or cannot be deleted due to other conditions
     */
    @Operation(summary = "Delete a Rollout Group", description = "Handles the DELETE request of deleting a rollout group. Required Permission: DELETE_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> deleteRolloutGroup(@PathVariable("tenantId") Long tenantId,
                                            @PathVariable("rolloutId") Long rolloutId,
                                            @PathVariable("groupId") Long groupId);

    /**
     * Retrieves all action statuses associated with a specific rollout and controller within a tenant.
     *
     * @param tenantId     the ID of the tenant
     * @param controllerId the ID of the controller
     * @param rolloutId    the ID of the rollout
     * @return a {@link ResponseEntity} containing a list of {@link DeviceActionStatusTimestampResponse}
     */
    @Operation(
            summary = "Retrieve action statuses for a specific rollout and controller",
            description = "Fetches all action statuses associated with a given rollout and controller within a tenant."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of action statuses retrieved successfully.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeviceActionStatusTimestampResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Authentication required.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Access denied or data volume restriction.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "405", description = "HTTP method not allowed.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "406", description = "Unsupported Accept header.",
                    content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "429", description = "Rate limit exceeded.",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping(
            value = MgmtRestConstants.ACTION_STATUS_V1_REQUEST_MAPPING_TENANT,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<List<DeviceActionStatusTimestampResponse>> fetchActionStatuses(@PathVariable("tenantId") Long tenantId,
                                                                                  @PathVariable("rolloutId") Long rolloutId,
                                                                                  @PathVariable("controllerId") String controllerId);

    /**
     * Handles the GET request of retrieving all targets for a specific rollout.
     *
     * @param rolloutId the ID of the rollout to retrieve targets for
     * @param tenantId  the ID of the tenant
     * @return a list of all targets for the specified rollout with status OK.
     */
    @Operation(summary = "Return all targets for a specific rollout",
            description = "Handles the GET request of retrieving all targets for a specific rollout. " +
                    "Required permission: READ_TARGET and READ_ROLLOUT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403",
                    description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Rollout not found.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405",
                    description = "The http request method is not allowed on the resource.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406",
                    description = "In case accept header is specified and not application/json.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429",
                    description = "Too many requests. The server will refuse further attempts and the client has to wait another second.",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ROLLOUT_TARGETS_V1_REQUEST_MAPPING_TENANT,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<List<MgmtRolloutTargetActionsResponse>> getTargetsActionsByRolloutId(
            @PathVariable("rolloutId") Long rolloutId,
            @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the PUT request for retrying a Full Rollout which are in FINISHED/CANCELED state.
     *
     * @param tenantID
     * @param rolloutID
     * @param retryRequest
     * @return
     */
    @Operation(
            summary = "Retry Full Rollout",
            description = "Handles the PUT request for retying a Full Rollout which are in FINISHED/CANCELED state. Required Permission: HANDLE_ROLLOUT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retried the Full Rollout"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found - Rollout not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict - Rollout is not in a state that allows retrying", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(
            value = MgmtRestConstants.RETRY_FULL_ROLLOUT,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<Void> retryFullRollout(@PathVariable("tenantID") Long tenantID,
                                          @PathVariable("rolloutID") Long rolloutID,
                                          @Valid @RequestBody MgmtRetryFullRolloutRequestBody retryRequest);


    @Operation(
            summary = "Retry multiple devices",
            description = "Handles the PUT request for retrying multiple devices in a rollout. Required Permission: HANDLE_ROLLOUT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retried devices"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found - Rollout or devices not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(
            value = MgmtRestConstants.RETRY_MULTIPLE_DEVICES,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<Void> retryMultipleDevices(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("rolloutId") Long rolloutId,
            @Valid @RequestBody RetryMultipleDevicesRequest retryRequest
    );

    /**
     * Handles the POST request for cloning an Existing Rollout to create a new Rollout with existing details.
     *
     * @param tenantID                the ID of the tenant
     * @param rolloutID               the ID of the rollout to be cloned
     * @param cloneRolloutRequestBody the body of the request containing details for the new cloned rollout
     * @return a ResponseEntity containing the details of the newly created cloned rollout
     */

    @Operation(
            summary = "Clone a Rollout",
            description = "Handles the POST request for cloning an Existing Rollout to create a new Rollout with existing details. Required Permission: HANDLE_ROLLOUT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retried the Full Rollout"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found - Rollout not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict - Rollout is not in a state that allows retrying", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(
            value = MgmtRestConstants.CLONE_ROLLOUT,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<MgmtRolloutResponseBody> cloneRollout(@PathVariable("tenantId") Long tenantID,
                                                         @PathVariable("rolloutId") Long rolloutID,
                                                         @Valid @RequestBody MgmtCloneRolloutRequestBody cloneRolloutRequestBody);


    @Operation(
            summary = "Retry individual device",
            description = "Handles the PUT request for retrying individual device in a rollout. Required Permission: HANDLE_ROLLOUT"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retried individual device"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Forbidden - Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Not Found - Rollout or devices not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict - Rollout is not in a state that allows retrying", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests - Rate limit exceeded", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(
            value = MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE,
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<Void> retryIndividualDevice(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("rolloutId") Long rolloutId,
            @PathVariable("controllerId") String controllerId,
            @Valid @RequestBody MgmtRetryIndividualDeviceRequestBody retryRequest
    );

}
