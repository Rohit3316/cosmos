/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetType;
import org.cosmos.models.mgmt.distributionsettype.constants.MgmtDistributionSetTypeAssignment;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetType;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetTypeRequestBodyPost;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetTypeRequestBodyPut;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * REST Resource handling for TargetType CRUD operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Hidden
public interface MgmtTargetTypeRestApi {

	/**
	 * Handles the GET request of retrieving all TargetTypes.
	 *
	 * @param pagingOffsetParam the offset of list of target types for pagination,
	 *                          might not be present in the rest request then
	 *                          default value will be applied
	 * @param pagingLimitParam  the limit of the paged request, might not be present
	 *                          in the rest request then default value will be
	 *                          applied
	 * @param sortParam         the sorting parameter in the request URL, syntax
	 *                          {@code field:direction, field:direction}
	 * @param rsqlParam         the search parameter in the request URL, syntax
	 *                          {@code q=name==abc}
	 * @return a list of all TargetTypes for a defined or default page request with
	 *         status OK. The response is always paged. In any failure the
	 *         JsonResponseExceptionHandler is handling the response.
	 */
	@Operation(summary = "Return all target types", description = "Handles the GET request of retrieving all target types.")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@GetMapping(value = MgmtRestConstants.TARGET_TYPE_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
			MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
			@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) int pagingOffsetParam,
			@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) int pagingLimitParam,
			@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) String sortParam,
			@RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam,
			@PathVariable("tenantId") Long tenantId);

	/**
	 * Handles the GET request of retrieving a single TargetType.
	 *
	 * @param typeId the ID of the target type to retrieve
	 * @return a single target type with status OK.
	 */
	@Operation(summary = "Return target type by id", description = "Handles the GET request of retrieving a single target type")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Target type not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@GetMapping(value = MgmtRestConstants.TARGET_TYPE_ID_V1_REQUEST_MAPPING, produces = {
			MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<MgmtTargetType> getTargetType(@PathVariable("typeId") Long typeId, @PathVariable("tenantId") Long tenantId);

	/**
	 * Handles the DELETE request for a single Target Type.
	 *
	 * @param typeId the ID of the target type to retrieve
	 * @return status OK if delete is successful.
	 */
	@DeleteMapping(value = MgmtRestConstants.TARGET_TYPE_ID_V1_REQUEST_MAPPING)
	@TenantAware
	ResponseEntity<Void> deleteTargetType(@PathVariable("typeId") Long typeId);

	/**
	 * Handles the PUT request of updating a Target Type.
	 *
	 * @param typeId         the ID of the target type in the URL
	 * @param restTargetType the target type to be updated.
	 * @return status OK if update is successful
	 */
	@Operation(summary = "Update target type by id", description = "Handles the PUT request for a single target type. Required Permission: UPDATE_TARGET")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully updated"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Target type not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@PutMapping(value = MgmtRestConstants.TARGET_TYPE_ID_V1_REQUEST_MAPPING, consumes = {
			MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<MgmtTargetType> updateTargetType(@PathVariable("typeId") Long typeId,
													MgmtTargetTypeRequestBodyPut restTargetType,
													@PathVariable("tenantId") Long tenantId);

	/**
	 * Handles the POST request of creating new Target Types. The request body must
	 * always be a list of types.
	 *
	 * @param targetTypes the target types to be created.
	 * @return In case all target types could be successfully created the
	 *         ResponseEntity with status code 201 - Created but without
	 *         ResponseBody. In any failure the JsonResponseExceptionHandler is
	 *         handling the response.
	 */
	@Operation(summary = "Create target types", description = "Handles the POST request for creating new target types. The request body must always be a list of types. Required Permission: CREATE_TARGET")
	@ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Successfully created"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Target type not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@PostMapping(value = MgmtRestConstants.TARGET_TYPE_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
			MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<List<MgmtTargetType>> createTargetTypes(List<MgmtTargetTypeRequestBodyPost> targetTypes, @PathVariable("tenantId") Long tenantId);

	/**
	 * Handles the GET request of retrieving the list of compatible distribution set
	 * types in that target type.
	 *
	 * @param typeId   of the TargetType.
	 * @param tenantId
	 * @return Unpaged list of distribution set types and OK in case of success.
	 */
	@Operation(summary = "Return list of compatible distribution set types", description = "Handles the GET request of retrieving the list of compatible distribution set types in that target type. Required Permission: READ_TARGET, READ_REPOSITORY")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Distribution set type was not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@GetMapping(value = MgmtRestConstants.TARGET_TYPE_COMP_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(@PathVariable("typeId") Long typeId, @PathVariable("tenantId") Long tenantId);

	/**
	 * Handles DELETE request for removing the compatibility of a distribution set
	 * type from the target type.
	 *
	 * @param targetTypeId   of the TargetType.
	 * @param distributionSetTypeId of the DistributionSetType.
	 * @return OK if the request was successful
	 */
	@Operation(summary = "Remove compatibility of distribution set type from the target type", description = "Handles the DELETE request for removing a distribution set type from a single target type. Required Permission: UPDATE_TARGET and READ_REPOSITORY")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully deleted"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "404", description = "Distribution set type was not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@DeleteMapping(value = MgmtRestConstants.TARGET_TYPE_COMP_ID_V1_REQUEST_MAPPING)
	@TenantAware
	ResponseEntity<Void> removeCompatibleDistributionSet(@PathVariable("typeId") Long targetTypeId,
			@PathVariable("dsTypeId") Long distributionSetTypeId);

	/**
	 * Handles the POST request for adding the compatibility of a distribution set
	 * type to a target type.
	 *
	 * @param typeId                 of the TargetType.
	 * @param distributionSetTypeIds of the DistributionSetTypes as a List.
	 * @return OK if the request was successful
	 */
	@Operation(summary = "Adding compatibility of a distribution set type to a target type", description = "Handles the POST request for adding compatible distribution set types to a target type. Required Permission: UPDATE_TARGET and READ_REPOSITORY")
	@ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Successfully added"),
			@ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
			@ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))) })
	@PostMapping(value = MgmtRestConstants.TARGET_TYPE_COMP_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
					MediaType.APPLICATION_JSON_VALUE })
	@TenantAware
	ResponseEntity<Void> addCompatibleDistributionSets(@PathVariable("typeId") final Long typeId,
			final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds);
}
