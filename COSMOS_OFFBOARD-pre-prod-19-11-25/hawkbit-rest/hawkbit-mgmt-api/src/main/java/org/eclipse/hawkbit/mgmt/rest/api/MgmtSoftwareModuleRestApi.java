/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
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
import java.util.List;
import jakarta.validation.constraints.NotNull;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignEcuModelRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleMetadata;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleMetadataBodyPut;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPut;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * REST Resource handling for SoftwareModule and related Artifact CRUD
 * operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Software Modules", description = "REST API for SoftwareModule and related Artifact CRUD operations.")
public interface MgmtSoftwareModuleRestApi {

    /**
     * Handles the GET request of retrieving all softwaremodules.
     *
     * @param pagingOffsetParam
     *            the offset of list of modules for pagination, might not be
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
     * @return a list of all modules for a defined or default page request with
     *         status OK. The response is always paged. In any failure the
     *         JsonResponseExceptionHandler is handling the response.
     */
    @Operation(summary = "Return all Software modules", description = "Handles the GET request of retrieving all softwaremodules. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(@PathVariable("tenantId") final Long tenantId,
                                                                     @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                     @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                     @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                     @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Handles the GET request of retrieving a single software module.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     *
     * @return a single softwareModule with status OK.
     */
    @Operation(summary = "Return Software Module by id", description = "Handles the GET request of retrieving a single softwaremodule. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found ", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_ID_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                         @PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Handles the GET request of retrieving a single software module by scomo id.
     *
     * @param scomoId the name of the software module
     *
     * @return a single softwareModule with status OK.
     */
    @Operation(summary = "Return Software Module by scomo id", description = "Handles the GET request of retrieving a single softwaremodule by scomoId. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found ", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                         @PathVariable("scomoId") final String scomoId);

    /**
     * Handles the POST request of creating new softwaremodules. The request
     * body must always be a list of modules.
     *
     * @param softwareModules
     *            the modules to be created.
     * @return In case all modules could successful created the ResponseEntity
     *         with status code 201 - Created but without ResponseBody. In any
     *         failure the JsonResponseExceptionHandler is handling the
     *         response.
     */
    @Operation(summary = "Create Software Module(s)", description = "Handles the POST request of creating new software modules. The request body must always be a list of modules. Required Permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, consumes = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(@PathVariable("tenantId") final Long tenantId,
                                                                   final List<MgmtSoftwareModuleRequestBodyPost> softwareModules);

    /**
     * Handles the PUT request of updating a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module in the URL
     * @param restSoftwareModule
     *            the modules to be updated.
     * @return status OK if update was successful
     */
    @Operation(summary = "Update Software Module", description = "Handles the PUT request for a single softwaremodule within Hawkbit. Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.SOFTWAREMODULE_ID_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                            @PathVariable("softwareModuleId") final Long softwareModuleId,
                                                            final MgmtSoftwareModuleRequestBodyPut restSoftwareModule);


    /**
     * Handles the PUT request of updating a software module using Scomo Id.
     *
     * @param scomoId the name of the software module
     * @param restSoftwareModule
     *            the modules to be updated.
     * @return status OK if update was successful
     */
    @Operation(summary = "Update Software Module using Scomo Id", description = "Handles the PUT request for a single softwaremodule within Hawkbit. Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                            @PathVariable("scomoId") final String scomoId,
                                                            final MgmtSoftwareModuleRequestBodyPut restSoftwareModule);
    /**
     * Handles the DELETE request for a single software module.
     *
     * @param softwareModuleId
     *            the ID of the module to retrieve
     * @return status OK if delete was successful.
     *
     */
    @Operation(summary = "Delete Software Module by Id", description = "Handles the DELETE request for a single softwaremodule within Hawkbit. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.SOFTWAREMODULE_ID_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                              @PathVariable("softwareModuleId") final Long softwareModuleId);

    /**
     * Handles the DELETE request for a single software module by Scomo Id.
     *
     * @param scomoId the name of the software module
     * @return status OK if delete was successful.
     *
     */
    @Operation(summary = "Delete Software Module by Scomo Id", description = "Handles the DELETE request for a single softwaremodule within Hawkbit. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                              @PathVariable("scomoId") final String scomoId);


    /**
     * Gets a paged list of meta data for a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module for the meta data
     * @param pagingOffsetParam
     *            the offset of list of meta data for pagination, might not be
     *            present in the rest request then default value will be applied
     * @param pagingLimitParam
     *            the limit of the paged request, might not be present in the
     *            rest request then default value will be applied
     * @param sortParam
     *            the sorting parameter in the request URL, syntax
     *            {@code field:direction, field:direction}
     * @param rsqlParam
     *            the search parameter in the request URL, syntax
     *            {@code q=key==abc}
     * @return status OK if get request is successful with the paged list of
     *         meta data
     */
    @Operation(summary = "Return meta data for a Software Module", description = "Get a paged list of meta data for a software module. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found ", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,

            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(@PathVariable("tenantId") final Long tenantId,
                                                                      @PathVariable("softwareModuleId") final Long softwareModuleId,
                                                                      @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                      @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                      @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                      @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam);

    /**
     * Gets a single meta data value for a specific key of a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to get the meta data from
     * @param key
     *            the key of the meta data entry to retrieve the value from
     * @return status OK if get request is successful with the value of the meta
     *         data
     */
    @Operation(summary = "Return single meta data value for a specific key of a Software Module", description = "Get a single meta data value for a meta data key. Required Permission: READ_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found ", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,

            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(@PathVariable("tenantId") final Long tenantId,
                                                                @PathVariable("softwareModuleId") final Long softwareModuleId,
                                                                @PathVariable("key") final String key);

    /**
     * Updates a single meta data value of a software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to update the meta data entry
     * @param key
     *            the key of the meta data to update the value
     * @param metadata
     *            body to update
     * @return status OK if the update request is successful and the updated
     *         meta data result
     */
    @Operation(summary = "Update a single meta data value of a Software Module", description = "Update a single meta data value for speficic key. Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING, produces = { MediaTypes.HAL_JSON_VALUE,

            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<MgmtSoftwareModuleMetadata> updateMetadata(@PathVariable("tenantId") final Long tenantId,
                                                              @PathVariable("softwareModuleId") final Long softwareModuleId,
                                                              @PathVariable("key") final String key, final MgmtSoftwareModuleMetadataBodyPut metadata);

    /**
     * Deletes a single meta data entry from the software module.
     *
     * @param softwareModuleId
     *            the ID of the software module to delete the meta data entry
     * @param key
     *            the key of the meta data to delete
     * @return status OK if the delete request is successful
     */
    @Operation(summary = "Delete single meta data entry from the software module", description = "Delete a single meta data. Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING)

    ResponseEntity<Void> deleteMetadata(@PathVariable("tenantId") final Long tenantId,
                                        @PathVariable("softwareModuleId") final Long softwareModuleId,
                                        @PathVariable("key") final String key);

    /**
     * Creates a list of meta data for a specific software module.
     *
     * @param softwareModuleId
     *            the ID of the distribution set to create meta data for
     * @param metadataRest
     *            the list of meta data entries to create
     * @return status created if post request is successful with the value of
     *         the created meta data
     */
    @Operation(summary = "Creates a list of meta data for a specific Software Module", description = "Create a list of meta data entries Required Permission: UPDATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING, consumes = { MediaType.APPLICATION_JSON_VALUE,

            MediaTypes.HAL_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<List<MgmtSoftwareModuleMetadata>> createMetadata(@PathVariable("tenantId") final Long tenantId,
                                                                    @PathVariable("softwareModuleId") final Long softwareModuleId,
                                                                    final List<MgmtSoftwareModuleMetadata> metadataRest);


    @Operation(summary = "Handle Ecu Model Assignment to Software module", description = "Handles the PUT request for a assigning Ecu model to a software module.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignEcuModelToSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                        @PathVariable("softwareModuleId") @NotNull final Long softwareModuleId,
                                                        final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody);

    /**
     * Handles the Ecu model assignment to a software module using Scomo Id.
     *
     * @param scomoId the name of the software module
     * @param assignEcuModelRequestBody - the request body containing the Ecu model assignment details
     * @return status OK if the assignment was successful
     */
    @Operation(summary = "Handle Ecu Model Assignment to Software module using Scomo Id", description = "Handles the PUT request for a assigning Ecu model to a software module.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }, produces = { MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE })
    ResponseEntity<Void> assignEcuModelToSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                        @PathVariable("scomoId") @NotNull final String scomoId,
                                                        final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody);

    @Operation(summary = "Remove association of ECU Models from Software Module", description = "Handles the PUT request for removing association of Ecu model from a software module.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed associations"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> removeEcuModelsFromSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                           @PathVariable("softwareModuleId") @NotNull final Long softwareModuleId,
                                                           final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody);

    /**
     * Handles removal of association of ECU Models from Software Module using Scomo Id
     *
     * @param scomoId the name of the software module
     * @param assignEcuModelRequestBody - the request body containing the Ecu model assignment details
     * @return
     */
    @Operation(summary = "Remove association of ECU Models from Software Module using Scomo Id", description = "Handles the PUT request for removing association of Ecu model from a software module.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed associations"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Software Module not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> removeEcuModelsFromSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                           @PathVariable("scomoId") @NotNull final String scomoId,
                                                           final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody);
}
