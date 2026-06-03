package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationInfo;
import org.cosmos.models.mgmt.pki.dto.SigningCertificateConfigurationUpdateInfo;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.cosmos.models.mgmt.MgmtRestConstants.SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING;
import static org.cosmos.models.mgmt.MgmtRestConstants.SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING_BY_ECU_ID_ISSUER;

/**
 * REST API for PKI Management, specifically for managing signing certificate configurations.
 */

@Tag(name = "PKI Management", description = "PKI Management REST APIs for managing signing related configurations.")
@RestController
public interface MgmtPkiManagementRestApi {


    /**
     * Adds a new DD Signing Certificate Configuration.
     *
     * <p>Handles the POST request for adding new DD Signing Certificate Configuration.
     * The request body must always be a list of types.
     * <br>Required Permission: CREATE_REPOSITORY
     *
     * @param config the signing certificate configuration info to add
     * @return the created signing certificate configuration
     */
    @Operation(
            summary = "Add new DD Signing Certificate Configuration",
            description = "Handles the POST request for adding new DD Signing Certificate Configuration. The request body must always be a list of types. Required Permission: CREATE_REPOSITORY"
    )
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
    @PostMapping(
            value = SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING,
            consumes = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<SigningCertificateConfiguration> addSigningConfiguration(@RequestBody @Valid SigningCertificateConfigurationInfo config);

    /**
     * Retrieves a DD Signing Certificate Configuration by ID.
     *
     * <p>Handles the GET request for fetching a specific DD Signing Certificate Configuration.
     * <br>Required Permission: READ_REPOSITORY
     *
     * @param ecuIdIssuer the ECU ID Issuer of the signing certificate configuration
     * @return the signing certificate configuration
     */
    @Operation(
            summary = "Get DD Signing Certificate Configuration by ID",
            description = "Handles the GET request for fetching a specific DD Signing Certificate Configuration. Required Permission: READ_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Signing certificate configuration not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(
            value = SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING_BY_ECU_ID_ISSUER,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<SigningCertificateConfiguration> getSigningConfiguration(@PathVariable @NotBlank String ecuIdIssuer);


    /**
     * Retrieves all DD Signing Certificate Configurations.
     *
     * <p>Handles the GET request for fetching all DD Signing Certificate Configurations.
     * <br>Required Permission: READ_REPOSITORY
     *
     * @return list of signing certificate configurations
     */
    @Operation(
            summary = "Get all DD Signing Certificate Configurations",
            description = "Handles the GET request for fetching all DD Signing Certificate Configurations. Required Permission: READ_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Signing certificate configuration not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(
            value = SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<List<SigningCertificateConfiguration>> getAllSigningConfigurations();


    /**
     * Updates an existing DD Signing Certificate Configuration.
     *
     * <p>Handles the PUT request for updating a DD Signing Certificate Configuration.
     * <br>Required Permission: UPDATE_REPOSITORY
     *
     * @param ecuIdIssuer the ECU ID Issuer of the signing certificate configuration to update
     * @param updateConfig the updated signing certificate configuration info
     * @return the updated signing certificate configuration
     */
    @Operation(
            summary = "Update DD Signing Certificate Configuration",
            description = "Handles the PUT request for updating a DD Signing Certificate Configuration. Required Permission: UPDATE_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Signing certificate configuration not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempted with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(
            value = SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING_BY_ECU_ID_ISSUER,
            consumes = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<SigningCertificateConfiguration> updateSigningConfiguration(@PathVariable @NotBlank String ecuIdIssuer, @RequestBody @Valid SigningCertificateConfigurationUpdateInfo updateConfig);

    /**
     * Deletes a DD Signing Certificate Configuration by ID.
     *
     * <p>Handles the DELETE request for removing a DD Signing Certificate Configuration.
     * <br>Required Permission: DELETE_REPOSITORY
     *
     * @param ecuIdIssuer the ECU ID Issuer of the signing certificate configuration to delete
     * @return no content
     */
    @Operation(
            summary = "Delete DD Signing Certificate Configuration",
            description = "Handles the DELETE request for removing a DD Signing Certificate Configuration. Required Permission: DELETE_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted the signing certificate configuration"),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete the signing certificate configuration.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Signing certificate configuration not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict, e.g. in case an entity is modified by another user in another request at the same time. You may retry your deletion request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempted with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(
            value = SIGNING_CERTIFICATE_CONFIGURATION_V1_REQUEST_MAPPING_BY_ECU_ID_ISSUER
    )
    ResponseEntity<Void> deleteSigningConfiguration(@PathVariable @NotBlank String ecuIdIssuer);

}