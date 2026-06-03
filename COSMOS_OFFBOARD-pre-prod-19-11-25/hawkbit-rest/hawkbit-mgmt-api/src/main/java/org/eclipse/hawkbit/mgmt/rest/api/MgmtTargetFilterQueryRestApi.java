/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
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
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtDistributionSetAutoAssignment;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQuery;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQueryRequestBody;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterVinListQuery;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterVinListRequestBody;
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
 * Api for handling target operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Hidden
public interface MgmtTargetFilterQueryRestApi {

    /**
     * Handles the GET request of retrieving a single target filter.
     *
     * @param filterId
     *            the ID of the target filter to retrieve
     * @param tenantId the ID of the tenant
     * @return a single target with status OK.
     */
	@Operation(summary = "Return target filter query by id", description = "Handles the GET request of retrieving a single target filter query. Required permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target filter query not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> getFilter(@PathVariable("filterId") Long filterId, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving all filters.
     *
     * @param pagingOffsetParam
     *            the offset of list of targets for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=name==abc}
     * @param tenantId the ID of the tenant
     * @return a list of all targets for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
	@Operation(summary = "Return all target filter queries", description = "Handles the GET request of retrieving all target filter queries. Required permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING_TENANT, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTargetFilterQuery>> getFilters(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,  @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the POST request of creating new target filters. The request body
     * must always be a list of target filters.
     *
     * @param filter
     *            the filters to be created.
     * @param tenantId the ID of the tenant
     * @return In case all filters were successfully created the ResponseEntity
     *         with status code 201 with a list of successfully created entities
     *         is returned. In any failure the JsonResponseExceptionHandler is
     *         handling the response.
     */
	@Operation(summary = "Create target filter", description = "Handles the POST request to create a new target filter query. Required permission: CREATE_TARGET")
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
    @PostMapping(value = MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING_TENANT, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> createFilter(@RequestBody MgmtTargetFilterQueryRequestBody filter,  @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the PUT request of updating a target filter. The ID is within the
     * URL path of the request. A given ID in the request body is ignored. It's
     * not possible to set fields to {@code null} values.
     *
     * @param filterId
     *            the path parameter which contains the ID of the target filter
     * @param targetFilterRest
     *            the request body which contains the fields which should be
     *            updated, fields which are not given are ignored for the
     *            update.
     * @param tenantId the ID of the tenant
     * @return the updated target filter response which contains all fields
     *         including fields which have not been updated
     */
	@Operation(summary = "Updates target filter query by id", description = "Handles the PUT request of updating a target filter query. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully updated"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target filter query not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> updateFilter(@PathVariable("filterId") Long filterId,
            @RequestBody MgmtTargetFilterQueryRequestBody targetFilterRest,  @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the DELETE request of deleting a target filter.
     *
     * @param filterId
     *            the ID of the target filter to be deleted
     * @param tenantId the ID of the tenant
     * @return If the given controllerId could exists and could be deleted Http
     *         OK. In any failure the JsonResponseExceptionHandler is handling
     *         the response.
     */
	@Operation(summary = "Delete target filter by id", description = "Handles the DELETE request of deleting a target filter query. Required permission: DELETE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target filter query not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> deleteFilter(@PathVariable("filterId") Long filterId,  @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving the distribution set for auto
     * assignment of an specific target filter.
     *
     * @param filterId
     *            the ID of the target to retrieve the assigned distribution
     * @param tenantId the ID of the tenant
     * @return the assigned distribution set with status OK, if none is assigned
     *         than {@code null} content (e.g. "{}")
     */
	@Operation(summary = "Return distribution set for auto assignment of a specific target filter", description = "Handles the GET request of retrieving the auto assign distribution set. Required permission: READ_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(@PathVariable("filterId") Long filterId, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the POST request for changing distribution set for auto
     * assignment of a target filter.
     *
     * @param filterId
     *            of the target to change
     * @param dsIdWithActionType
     *            id of the distribution set and the action type for auto
     *            assignment
     * @param tenantId the ID of the tenant
     * @return http status
     */
	@Operation(summary = "Set auto assignment of distribution set for a target filter query", description = "Handles the POST request of setting the auto assign distribution set for a target filter query. Required permissions: UPDATE_TARGET and READ_REPOSITORY")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully added"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target filter not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(@PathVariable("filterId") Long filterId,
            @RequestBody MgmtDistributionSetAutoAssignment dsIdWithActionType,  @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the DELETE request for removing the distribution set for auto
     * assignment of a target filter.
     *
     * @param filterId
     *            of the target to change
     * @param tenantId the ID of the tenant
     * @return http status
     */
	@Operation(summary = "Remove Distribution Set for auto assignment of a target filter", description = "Removes the auto assign distribution set from the target filter query. Required permission: UPDATE_TARGET")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "404", description = "Target filter query not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT)
    ResponseEntity<Void> deleteAssignedDistributionSet(@PathVariable("filterId") Long filterId,  @PathVariable("tenantId") Long tenantId);

	/**
	 * Handles the POST request of creating new target filters for given VinList.
     * 
	 * @param filter
     * @param tenantId the ID of the tenant
     *
	 * @return In case filter is successfully created the ResponseEntity
     *         with status code 200 and MgmtTargetFilterVinListQuery entities
     *         is returned.
	 */
	@Operation(summary = "Create Target Filter for Vin List", description = "Handles the POST request to create a new target filter query for Vin List. Required permission: CREATE_TARGET")
	@ApiResponses(value = {
	        @ApiResponse(responseCode = "201", description = "Successfully created"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "404", description = "Entity Not Found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "500", description = "Internal Server Error", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
	    })
    @PostMapping(value = MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetFilterVinListQuery> createTargetFilterVinList(@RequestBody MgmtTargetFilterVinListRequestBody mgmtTargetFilterVinListRequestBody,  @PathVariable("tenantId") Long tenantId);

}
