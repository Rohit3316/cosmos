/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.cosmos.models.mgmt.MgmtId;
import org.cosmos.models.mgmt.MgmtMetadata;
import org.cosmos.models.mgmt.MgmtMetadataBodyPut;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.action.dto.MgmtAction;
import org.cosmos.models.mgmt.action.dto.MgmtActionRequestBodyPut;
import org.cosmos.models.mgmt.action.dto.MgmtActionStatus;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentResponseBody;
import org.cosmos.models.mgmt.polling.dto.MgmtPollingFeedback;
import org.cosmos.models.mgmt.target.constants.MgmtTargetAttributes;
import org.cosmos.models.mgmt.target.dto.MgmtDistributionSetAssignments;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.target.dto.MgmtTargetAutoConfirm;
import org.cosmos.models.mgmt.target.dto.MgmtTargetAutoConfirmUpdate;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.cosmos.models.mgmt.target.dto.MgmtTargetTenantRequest;
import org.cosmos.models.mgmt.target.dto.MgmtTargetUpdateRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutHistoryResponse;
import org.eclipse.hawkbit.repository.dto.VehicleInventoryDTO;
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

/**
 * API for handling target operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Targets", description = "REST API for Target CRUD operations.")
public interface MgmtTargetRestApi {

    /**
     * Handles the GET request of retrieving a single target.
     *
     * @param targetId the ID of the target to retrieve
     * @param tenantId the ID of the tenant
     * @return a single target with status OK.
     */
    @Operation(summary = "Return target by id", description = "Handles the GET request of retrieving a single target. Required Permission: READ_TARGET.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtTarget> getTarget(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") long tenantId);

    /**
     * Handles the GET request of retrieving all targets.
     *
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be
     *                          present in the rest request then default value will be applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param rsqlParam         the search parameter in the request URL, syntax
     *                          {@code q=name==abc}
     * @param tenantId          the ID of the tenant
     * @return a list of all targets for a defined or default page request with
     * status OK. The response is always paged. In any failure the
     * JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all targets", description = "Handles the GET request of retrieving all targets. Required permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtTarget>> getTargets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving all generic feedback for a target id.
     *
     * @param targetId          the path parameter which contains the ID of the target
     * @param pagingOffsetParam the offset of list of ric feedback for pagination, might not be
     *                          present in the rest request then default value will be applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param tenantId          the ID of the tenant
     * @return a list of all generic feedback for a target id for a defined or default page request with
     * status OK. The response is always paged. In any failure the
     * JsonResponseExceptionHandler is handling the response.
     * deprecated since we are removing sp_polling and its references
     */
    @Operation(summary = "Return all generic feedbacks", description = "Handles the GET request of retrieving all generic feedback for a target id. Required permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })

    @Deprecated
    @GetMapping(value = MgmtRestConstants.TARGET_GEN_FEEDBACK_V1_REQUEST_MAPPING, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtPollingFeedback> getAllGenericFeedback(
            @PathVariable("controllerId") String targetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the POST request of creating new targets. The request body must
     * always be a list of targets.
     *
     * @param targets  the targets to be created.
     * @param tenantId the ID of the tenant
     * @return In case all targets could successful created the ResponseEntity
     * with status code 201 with a list of successfully created
     * entities. In any failure the JsonResponseExceptionHandler is
     * handling the response.
     */
    @Operation(summary = "Create target(s)", description = "Handles the POST request of creating new targets. The request body must always be a list of targets. Required Permission: CREATE_TARGET")
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
    @PostMapping(value = MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, consumes = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<List<MgmtTarget>> createTargets(List<MgmtTargetRequestBody> targets, @PathVariable("tenantId") final Long tenantId);

    /**
     * Handles the PUT request of updating a target. The ID is within the URL
     * path of the request. A given ID in the request body is ignored. It's not
     * possible to set fields to {@code null} values.
     *
     * @param targetId   the path parameter which contains the ID of the target
     * @param tenantId   the ID of the tenant
     * @param targetRest the request body which contains the fields which should be
     *                   updated, fields which are not given are ignored for the
     *                   udpate.
     * @return the updated target response which contains all fields also fields
     * which have not updated
     */
    @Operation(summary = "Update target by id", description = "Handles the PUT request of updating a target. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtTarget> updateTarget(@PathVariable("controllerId") String targetId,
                                            MgmtTargetUpdateRequestBody targetRest,
                                            @PathVariable("tenantId") final Long tenantId);

    /**
     * Handles the DELETE request of deleting a target.
     *
     * @param targetId the ID of the target to be deleted
     * @param tenantId the ID of the tenant
     * @return If the given targetId could exists and could be deleted Http OK.
     * In any failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Operation(summary = "Delete target by id", description = "Handles the DELETE request of deleting a single target. Required Permission: DELETE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteTarget(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") final Long tenantId);

    /**
     * Handles the DELETE (unassign) request of a target type.
     *
     * @param targetId the ID of the target
     * @param tenantId the ID of the tenant
     * @return If the given targetId could exists and could be unassign Http OK.
     * In any failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Deprecated
    @Operation(summary = "Unassign target type from target.", description = "Remove the target type from a target. The target type will be set to null. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @Hidden
    @DeleteMapping(value = MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING)
    ResponseEntity<Void> unassignTargetType(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the POST (assign) request of a target type.
     *
     * @param targetId the ID of the target
     * @param tenantId the ID of the tenant
     * @return If the given targetId could exists and could be assign Http OK.
     * In any failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Deprecated
    @Operation(summary = "Assign target type to a target", description = "Assign or update the target type of a target. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully assigned"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @Hidden
    @PostMapping(value = MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, consumes = {MediaTypes.HAL_JSON_VALUE,

            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> assignTargetType(@PathVariable("controllerId") String targetId, MgmtId targetTypeId, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the attributes of a specific
     * target.
     *
     * @param targetId the ID of the target to retrieve the attributes.
     * @param tenantId the ID of the tenant
     * @return the target attributes as map response with status OK
     */
    @Operation(summary = "Return attributes of a specific target", description = "Handles the GET request of retrieving the attributes of a specific target. Reponse is a key/value list. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_CNTRL_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Hidden
    ResponseEntity<MgmtTargetAttributes> getAttributes(@PathVariable("controllerId") String targetId,@PathVariable("tenantId") Long tenantId);


    @GetMapping(value = MgmtRestConstants.TARGET_CNTRL_SA_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtTargetAttributes> getSoftwareAttributes(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the Actions of a specific target.
     *
     * @param targetId          to load actions for
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be
     *                          present in the rest request then default value will be applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param rsqlParam         the search parameter in the request URL, syntax
     *                          {@code q=status==pending}
     * @param tenantId          the ID of the tenant
     * @return a list of all Actions for a defined or default page request with
     * status OK. The response is always paged. In any failure the
     * JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all actions for a specific target", description = "Handles the GET request of retrieving the full action history of a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtAction>> getAllActions(@PathVariable("controllerId") String targetId,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
                                                        @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving a specific Actions of a specific
     * Target.
     *
     * @param targetId to load the action for
     * @param actionId to load
     * @param tenantId the ID of the tenant
     * @return the action
     */
    @Operation(summary = "Return action by id of a specific target", description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Hidden
    ResponseEntity<MgmtAction> getAction(@PathVariable("controllerId") String targetId,
                                         @PathVariable("actionId") Long actionId,
                                         @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the DELETE request of canceling an specific Actions of a specific
     * Target.
     *
     * @param targetId the ID of the target in the URL path parameter
     * @param actionId the ID of the action in the URL path parameter
     * @param force    optional parameter, which indicates a force cancel
     * @param tenantId the ID of the tenant
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Cancel action for a specific target", description = "Cancels an active action, only active actions can be deleted. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING)
    ResponseEntity<Void> cancelAction(@PathVariable("controllerId") String targetId,
                                      @PathVariable("actionId") Long actionId,
                                      @RequestParam(value = "force", required = false, defaultValue = "false") boolean force,
                                      @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the PUT update request to switch an action from soft to forced.
     *
     * @param targetId     the ID of the target in the URL path parameter
     * @param actionId     the ID of the action in the URL path parameter
     * @param actionUpdate to update the action
     * @param tenantId     the ID of the tenant
     * @return status no content in case cancellation was successful
     */
    @Operation(summary = "Switch an action from soft to forced", description = "Handles the PUT request to switch an action from soft to forced. Required Permission: UPDATE_TARGET.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    @Hidden
    ResponseEntity<MgmtAction> updateAction(@PathVariable("controllerId") String targetId,
                                            @PathVariable("actionId") Long actionId, MgmtActionRequestBodyPut actionUpdate,
                                            @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the ActionStatus of a specific
     * target and action.
     *
     * @param targetId          of the the action
     * @param actionId          of the status we are intend to load
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be
     *                          present in the rest request then default value will be applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param tenantId          the ID of the tenant
     * @return a list of all ActionStatus for a defined or default page request
     * with status OK. The response is always paged. In any failure the
     * JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return status of a specific action on a specific target", description = "Handles the GET request of retrieving a specific action on a specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, produces = {MediaTypes.HAL_JSON_VALUE,

            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(@PathVariable("controllerId") String targetId,
                                                                    @PathVariable("actionId") Long actionId,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                                    @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the assigned distribution set of an
     * specific target.
     *
     * @param targetId the ID of the target to retrieve the assigned distribution
     * @param tenantId the ID of the tenant
     * @return the assigned distribution set with status OK, if none is assigned
     * than {@code null} content (e.g. "{}")
     */

    @Operation(summary = "Return the assigned distribution set of a specific target", description = "Handles the GET request of retrieving the assigned distribution set of an specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_ASSIGN_DSET_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Deprecated
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") Long tenantId);

    /**
     * Changes the assigned distribution set of a target.
     *
     * @param targetId      of the target to change
     * @param dsAssignments the requested Assignments that shall be made
     * @param offline       to <code>true</code> if update was executed offline, i.e. not
     *                      managed by hawkBit.
     * @param tenantId      the ID of the tenant
     * @return status OK if the assignment of the targets was successful and a
     * complex return body which contains information about the assigned
     * targets and the already assigned targets counters
     */

    @Operation(summary = "Assigns a distribution set to a specific target", description = "Handles the POST request for assigning a distribution set to a specific target. Required Permission: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully added"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    @Deprecated
    @Hidden
    ResponseEntity<MgmtTargetAssignmentResponseBody> postAssignedDistributionSet(
            @PathVariable("controllerId") String targetId, MgmtDistributionSetAssignments dsAssignments,
            @RequestParam(value = "offline", required = false) Boolean offline,
            @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the installed distribution set of
     * an specific target.
     *
     * @param targetId the ID of the target to retrieve
     * @param tenantId the ID of the tenant
     * @return the assigned installed set with status OK, if none is installed
     * than {@code null} content (e.g. "{}")
     */

    @Operation(summary = "Return installed distribution set of a specific target", description = "Handles the GET request of retrieving the installed distribution set of an specific target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_INSTALL_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Deprecated
    ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(@PathVariable("controllerId") String targetId, @PathVariable("tenantId") Long tenantId);

    /**
     * Gets a paged list of meta data for a target.
     *
     * @param targetId          the ID of the target for the meta data
     * @param pagingOffsetParam the offset of list of targets for pagination, might not be
     *                          present in the rest request then default value will be applied
     * @param pagingLimitParam  the limit of the paged request, might not be present in the
     *                          rest request then default value will be applied
     * @param sortParam         the sorting parameter in the request URL, syntax
     *                          {@code field:direction, field:direction}
     * @param rsqlParam         the search parameter in the request URL, syntax
     *                          {@code q=key==abc}
     * @param tenantId          the ID of the tenant
     * @return status OK if get request is successful with the paged list of
     * meta data
     */
    @Operation(summary = "Return metadata for specific target", description = "Get a paged list of meta data for a target. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtMetadata>> getMetadata(@PathVariable("controllerId") String targetId,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
                                                        @PathVariable("tenantId") Long tenantId);

    /**
     * Gets a single meta data value for a specific key of a target.
     *
     * @param targetId the ID of the target to get the meta data from
     * @param key      the key of the meta data entry to retrieve the value from
     * @param tenantId the ID of the tenant
     * @return status OK if get request is successful with the value of the meta
     * data
     */

    @Operation(summary = "Return single metadata value for a specific key of a target", description = "Get a single meta data value for a meta data key. Required permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING, produces = {
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtMetadata> getMetadataValue(@PathVariable("controllerId") String targetId,
                                                  @PathVariable("key") String key,
                                                  @PathVariable("tenantId") Long tenantId);

    /**
     * Updates a single meta data value of a target.
     *
     * @param targetId the ID of the target to update the meta data entry
     * @param key      the key of the meta data to update the value
     * @param metadata update body
     * @param tenantId the ID of the tenant
     * @return status OK if the update request is successful and the updated
     * meta data result
     */

    @Operation(summary = "Updates a single meta data value of a target", description = "Update a single meta data value for speficic key. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("controllerId") String targetId,
                                                @PathVariable("key") String key, MgmtMetadataBodyPut metadata,
                                                @PathVariable("tenantId") Long tenantId);

    /**
     * Deletes a single meta data entry from the target.
     *
     * @param targetId the ID of the target to delete the meta data entry
     * @param key      the key of the meta data to delete
     * @param tenantId the ID of the tenant
     * @return status OK if the delete request is successful
     */

    @Operation(summary = "Deletes a single meta data entry from a target", description = "Delete a single meta data. Required permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteMetadata(@PathVariable("controllerId") String targetId,
                                        @PathVariable("key") String key, @PathVariable("tenantId") Long tenantId);

    /**
     * Creates a list of meta data for a specific target.
     *
     * @param targetId     the ID of the targetId to create meta data for
     * @param metadataRest the list of meta data entries to create
     * @param tenantId     the ID of the tenant
     * @return status created if post request is successful with the value of
     * the created meta data
     */
    @Operation(summary = "Create a list of meta data for a specific target", description = "Create a list of meta data entries Required permissions: READ_REPOSITORY and UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING, consumes = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaTypes.HAL_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<List<MgmtMetadata>> createMetadata(@PathVariable("controllerId") String targetId,
                                                      List<MgmtMetadata> metadataRest, @PathVariable("tenantId") Long tenantId);

    /**
     * Get the current auto-confirm state for a specific target.
     *
     * @param targetId to check the state for
     * @param tenantId the ID of the tenant
     * @return the current state as {@link MgmtTargetAutoConfirm}
     */

    @Operation(summary = "Return the current auto-confitm state for a specific target", description = "Handles the GET request to check the current auto-confirmation state of a target. Required Permission: READ_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_CONFIRM_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Hidden
    ResponseEntity<MgmtTargetAutoConfirm> getAutoConfirmStatus(@PathVariable("controllerId") String targetId,@PathVariable("tenantId") Long tenantId);

    /**
     * Activate auto-confirm on a specific target.
     *
     * @param targetId to activate auto-confirm on
     * @param update   properties to update
     * @param tenantId the ID of the tenant
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a
     * success
     */

    @Operation(summary = "Activate auto-confirm on a specific target", description = "Handles the POST request to activate auto-confirmation for a specific target. As a result all current active as well as future actions will automatically be confirmed by mentioning the initiator as triggered person. Actions will be automatically confirmed, as long as auto-confirmation is active. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully activated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_ACTIVATE_V1_REQUEST_MAPPING)
    @Hidden
    ResponseEntity<Void> activateAutoConfirm(@PathVariable("controllerId") String targetId,
                                             @RequestBody(required = false) MgmtTargetAutoConfirmUpdate update,
                                             @PathVariable("tenantId") Long tenantId);

    /**
     * Deactivate auto-confirm on a specific target.
     *
     * @param targetId to deactivate auto-confirm on
     * @param tenantId the ID of the tenant
     * @return {@link org.springframework.http.HttpStatus#OK} in case of a
     * success
     */

    @Operation(summary = "Deactivate auto-confirm on a specific target", description = "Handles the POST request to deactivate auto-confirmation for a specific target. All active actions will remain unchanged while all future actions need to be confirmed, before processing with the deployment. Required Permission: UPDATE_TARGET")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deactivated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Target not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_DEACTIVATE_V1_REQUEST_MAPPING)
    @Hidden
    ResponseEntity<Void> deactivateAutoConfirm(@PathVariable("controllerId") String targetId,
                                               @PathVariable("tenantId") Long tenantId);


    /**
     * Update the tenant association for a specific target.
     *
     * @param targetId to deactivate auto-confirm on
     * @param tenantId the ID of the tenant
     * @return {@link org.springframework.http.HttpStatus#OK} in case of success
     */
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g., invalid parameters"),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (e.g., read-only)"),
            @ApiResponse(responseCode = "404", description = "Target not found"),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource."),
            @ApiResponse(responseCode = "406", description = "In case Accept header is specified and not application/json."),
            @ApiResponse(responseCode = "409", description = "Conflict - Entity modified by another request."),
            @ApiResponse(responseCode = "415", description = "Unsupported Media Type"),
            @ApiResponse(responseCode = "429", description = "Too many requests - try again after some time.")
    })
    @PutMapping(value = MgmtRestConstants.TARGET_ID_UPDATETENANT_V1_REQUEST_MAPPING)
    ResponseEntity<Void> updateTenant(@PathVariable("controllerId") String targetId,
                                      @PathVariable("tenantId") Long tenantId, @RequestBody MgmtTargetTenantRequest request);

    /**
     * Fetch rollout history for a given VIN (controllerId).
     *
     * @param tenantId the tenant ID
     * @param vin the VIN/controllerId
     * @return list of rollout history records
     */
    @Operation(summary = "Get rollout history for a VIN", description = "Fetches rollout history for a given VIN (controllerId). Required Permission: READ_TARGET.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved", content = @Content(mediaType = "application/json", schema = @Schema(implementation = MgmtRolloutHistoryResponse.class))),
            @ApiResponse(responseCode = "404", description = "VIN not found", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(mediaType = "application/json")),
            @ApiResponse(responseCode = "403", description = "Forbidden", content = @Content(mediaType = "application/json"))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_ID_FETCH_VIN_HISTORY, produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<MgmtRolloutHistoryResponse>> getRolloutHistoryByVin(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("vin") String vin,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit);


    @Operation(
            summary = "Return vehicle inventory details for a controller",
            description = "Handles the GET request for retrieving the inventory details of a controller by its ID. Required Permission: READ_TARGET."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved vehicle inventory details"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Controller not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping("/management/v1/controllers/{currentTargetId}/inventory")
     ResponseEntity<List<VehicleInventoryDTO>> getVehicleInventoryDetails(@PathVariable("currentTargetId") String currentTargetId);
}
