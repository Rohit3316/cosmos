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

import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.system.dto.MgmtSystemTenantConfigurationValue;
import org.cosmos.models.mgmt.system.constants.MgmtSystemTenantConfigurationValueRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenant;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantCloneRequestBody;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantRequestBody;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantResponse;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationResponse;
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
 * REST Resource for handling tenant specific configuration operations.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Tenants", description = "REST API for handling tenant specific configuration operations.")
public interface MgmtTenantManagementRestApi {

    @Operation(summary = "Return all tenant specific configuration values", description = "The GET request returns a list of all possible configuration keys for the tenant. Required Permission: READ_TENANT_CONFIG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TENANTID_CONFIG_SYSTEM_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Map<String, MgmtSystemTenantConfigurationValue>> getTenantConfiguration(@NotNull @PathVariable("tenantId") long tenantId);

    /**
     * Handles the GET request of receiving a tenant specific configuration
     * value.
     *
     * @param key the name of the configuration key
     * @return if the given configuration value exists and could be get HTTP OK.
     * In any failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Operation(summary = "Return a tenant specific configuration value", description = "The GET request returns the configuration value of a specific configuration key for the tenant. Required Permission: READ_TENANT_CONFIG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Configuration key not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtSystemTenantConfigurationValue> getTenantConfigurationValue(
            @NotNull @PathVariable("tenantId") long tenantId,
            @PathVariable("key") String key);


    /**
     * Handles the DELETE request of deleting a tenant specific configuration
     * value.
     *
     * @param key the Name of the configuration key
     * @return if the given configuration value exists and could be deleted HTTP
     * OK. In any failure the JsonResponseExceptionHandler is handling
     * the response.
     */
    @Operation(summary = "Delete a tenant specific configuration value", description = "The DELETE request removes a tenant specific configuration value for the tenant. Afterwards the global default value is used. Required Permission: DELETE_TENANT_CONFIG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> deleteTenantConfigurationValue(@PathVariable("tenantId") long tenantId, @PathVariable("key") String key);


    /**
     * Handles the PUT request for updating a tenant specific configuration
     * value.
     *
     * @param key                    the name of the configuration key
     * @param configurationValueRest the new value for the configuration
     * @return if the given configuration value exists and could be get HTTP OK.
     * In any failure the JsonResponseExceptionHandler is handling the
     * response.
     */
    @Operation(summary = "Update a tenant specific configuration value.", description = "The PUT request changes a configuration value of a specific configuration key for the tenant. Required Permission: UPDATE_TENANT_CONFIG")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Configuration key not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, consumes = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtSystemTenantConfigurationValue> updateTenantConfigurationValue(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("key") String key, MgmtSystemTenantConfigurationValueRequest configurationValueRest);

    /**
     * Deletes the tenant data of a given tenant. USE WITH CARE!
     *
     * @param tenant to delete
     * @return HttpStatus.OK
     */
    @Operation(summary = "Deletes the tenant data", description = "The DELETE request removes a tenant data. Required Permission: DELETE_TENANT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteTenant(@PathVariable("tenantId") long tenantId);

    /**
     * This API allow to create a tenant
     *
     * @param tenantRequestBody to create
     * @return HttpStatus.CREATED
     */
    @Operation(summary = "Creates tenant", description = "Handles the POST request for creating tenant. Required Permission: CREATE_TENANT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))})
    @PostMapping(value = MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> createTenant(@RequestBody @Valid MgmtTenantRequestBody tenantRequestBody);

    /**
     * retrieve desired tenant information and his configurations
     *
     * @param tenant
     * @return
     */
    @Operation(summary = "Retrieve desired tenant information and his configurations", description = "The GET request for retrieving desired tenant information and configurations. Required Permission: READ_TENANT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING,
            consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtTenantResponse> getTenant(@PathVariable("tenantId") long tenantId);


    /**
     * This API allow to create a tenant
     *
     * @param tenantRequestBody to create
     * @return HttpStatus.CREATED
     */
    @Operation(summary = "Clones tenant", description = "Handles the POST request for cloning tenant. Required Permission: CREATE_TENANT")
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))})
    @PostMapping(value = MgmtRestConstants.BASE_CLONE_V1_REQUEST_MAPPING, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> cloneTenant(@PathVariable("tenantId") long tenantId, @RequestBody @Valid MgmtTenantCloneRequestBody tenantRequestBody);

    /**
     * retrieve All tenants {ID, Name}
     *
     * @return list of tenants based on search criteria
     */
    @GetMapping(value = MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<PagedList<MgmtTenant>> getAllTenants(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam
    );


    /**
     * Validate tenants from operator's JWT
     * @param request
     * @return
     */
    @Operation(summary = "Validate tenant availability in the system", description = "This POST request validates a list of tenants from the operator's JWT and returns the valid and available tenants from the database. " +
            "Required Permission: READ_TENANT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved valid tenants"),
            @ApiResponse(responseCode = "400", description = "Bad Request - invalid request body or parameters",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "User authentication required",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions or restricted access",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "HTTP method not allowed on this resource",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "Unsupported Accept header format",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests; client should retry later",
                    content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.BASE_TENANT_VALIDATE, produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}, consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<MgmtTenantValidationResponse> validateTenants(@Valid @RequestBody MgmtTenantValidationRequest request);
}

