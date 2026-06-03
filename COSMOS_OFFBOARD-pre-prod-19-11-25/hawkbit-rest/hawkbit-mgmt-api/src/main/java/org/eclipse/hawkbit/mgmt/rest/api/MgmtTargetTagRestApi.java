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
import org.cosmos.models.mgmt.tag.dto.MgmtAssignedTargetRequestBody;
import org.cosmos.models.mgmt.tag.dto.MgmtTag;
import org.cosmos.models.mgmt.tag.dto.MgmtTagRequestBodyPut;
import org.cosmos.models.mgmt.tag.dto.MgmtTargetTagAssigmentResult;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * REST Resource handling for TargetTag CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Hidden
public interface MgmtTargetTagRestApi {

    /**
     * Handles the GET request of retrieving all target tags.
     *
     * @param pagingOffsetParam
     *            the offset of list of target tags for pagination, might not be
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
     * @return a list of all target tags for a defined or default page request
     *         with status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
	@Operation(summary = "Return all target tags", description = "Handles the GET request of retrieving all target tags.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
	@GetMapping(value = MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING_TENANT, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTag>> getTargetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
			@PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving a single target tag.
     *
     * @param tagId
     *            the ID of the target tag to retrieve
	 * @param tenantId the ID of the tenant
     *
     * @return a single target tag with status OK.
     */

	@Operation(summary = "Return target tag by id", description = "Handles the GET request of retrieving a single target tag.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "404", description = "Target tag not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
	@GetMapping(value = MgmtRestConstants.TARGET_TAG_ID_V1_REQUEST_MAPPING_TENANT, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
    ResponseEntity<MgmtTag> getTargetTag(@PathVariable("tagId") Long tagId,
										 @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the POST request of creating new target tag. The request body
     * must always be a list of tags.
     *
     * @param tags
     *            the target tags to be created.
	 * @param tenantId the ID of the tenant
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created. The Response Body are the created
     *         target tags but without ResponseBody.
     */
	@Operation(summary = "Create target tag(s)", description = "Handles the POST request of creating new target tag. The request body must always be a list of target tags.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Successfully created"),
        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
    })
	@PostMapping(value = MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING_TENANT, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTag>> createTargetTags(List<MgmtTagRequestBodyPut> tags,
												   @PathVariable("tenantId") Long tenantId);

    /**
     *
     * Handles the PUT request of updating a single targetr tag.
     *
     * @param tagId
     *            the ID of the target tag
	 * @param tenantId the ID of the tenant
     * @param restTargetTagRest
     *            the the request body to be updated
     * @return status OK if update is successful and the updated target tag.
     */

	 @Operation(summary = "Update target tag by id", description = "Handles the PUT request of updating a target tag.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Successfully updated"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "404", description = "Target tag not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
	        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @PutMapping(value = MgmtRestConstants.TARGET_TAG_ID_V1_REQUEST_MAPPING_TENANT, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTag> updateTargetTag(@PathVariable("tagId") Long tagId,
											@PathVariable("tenantId") Long tenantId,
											MgmtTagRequestBodyPut restTargetTagRest);

    /**
     * Handles the DELETE request for a single target tag.
     *
     * @param tagId
     *            the ID of the target tag
	 * @param tenantId the ID of the tenant
     * @return status OK if delete as successfully.
     *
     */

	 @Operation(summary = "Delete target tag by id", description = "Handles the DELETE request of deleting a single target tag.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "404", description = "Target tag not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @DeleteMapping(value = MgmtRestConstants.TARGET_TAG_ID_V1_REQUEST_MAPPING_TENANT)
    ResponseEntity<Void> deleteTargetTag(@PathVariable("tagId") Long tagId,
										 @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of retrieving all assigned targets by the given
     * tag id.
     *
     * @param tagId
     *            the ID of the target tag to retrieve
	 * @param tenantId the ID of the tenant
     * @param pagingOffsetParam
     *            the offset of list of target tags for pagination, might not be
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
     *
     * @return the list of assigned targets.
     */
	 @Operation(summary = "Return assigned targets for tag", description = "Handles the GET request of retrieving a list of assigned targets.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "404", description = "Target tag not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @GetMapping(value = MgmtRestConstants.TARGET_TAG_ASSIGN_V1_REQUEST_MAPPING_TENANT, produces = { MediaTypes.HAL_JSON_VALUE,

                    MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(@PathVariable("tagId") Long tagId,
															 @PathVariable("tenantId") Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Handles the PATCH request to toggle the assignment of targets by the given
     * tag id.
     *
     * @param tagId
     *            the ID of the target tag to retrieve
	 * @param tenantId the ID of the tenant
     * @param assignedTargetRequestBodies
     *            list of controller ids to be toggled
     *
     * @return the list of assigned targets and unassigned targets.
     */

	 @Operation(summary = "Toggles target tag assignment", description = "Handles the PATCH request of toggle target assignment. The request body must always be a list of controller ids.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Successfully toggled"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
	        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @PatchMapping(value = MgmtRestConstants.TARGET_TAG_ASSIGN_V1_REQUEST_MAPPING_TENANT, consumes = {

                    MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = {
                            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtTargetTagAssigmentResult> toggleTagAssignment(@PathVariable("tagId") Long tagId,
																	 @PathVariable("tenantId") Long tenantId,
            List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the POST request to assign targets to the given tag id.
     *
     * @param tagId
     *            the ID of the target tag to retrieve
	 * @param tenantId the ID of the tenant
     * @param assignedTargetRequestBodies
     *            list of controller ids to be assigned
     *
     * @return the list of assigned targets.
     */
	 @Operation(summary = "Assign target(s) to given tagId", description = "Handles the POST request of target assignment. Already assigned target will be ignored.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "201", description = "Successfully assigned"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request."),
	        @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @PostMapping(value = MgmtRestConstants.TARGET_TAG_ASSIGN_V1_REQUEST_MAPPING_TENANT, consumes = { MediaTypes.HAL_JSON_VALUE,

                    MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
                            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtTarget>> assignTargets(@PathVariable("tagId") Long tagId,
												   @PathVariable("tenantId") Long tenantId,
            List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies);

    /**
     * Handles the DELETE request to unassign one target from the given tag id.
     *
     * @param tagId
     *            the ID of the target tag
     * @param controllerId
     *            the ID of the target to unassign
	 * @param tenantId the ID of the tenant
     * @return http status code
     */
	 @Operation(summary = "Unassign target from a given tagId", description = "Handles the DELETE request to unassign the given target.")
	    @ApiResponses(value = {
	        @ApiResponse(responseCode = "200", description = "Successfully deleted"),
	        @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "401", description = "The request requires user authentication."),
	        @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies."),
	        @ApiResponse(responseCode = "404", description = "Target not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
	        @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource."),
	        @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json."),
	        @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.")
	    })
	 @DeleteMapping(value = MgmtRestConstants.TARGET_TAG_UNASSIGN_V1_REQUEST_MAPPING_TENANT)

    ResponseEntity<Void> unassignTarget(@PathVariable("tagId") Long tagId,
										@PathVariable("controllerId") String controllerId,
										@PathVariable("tenantId") Long tenantId);
}
