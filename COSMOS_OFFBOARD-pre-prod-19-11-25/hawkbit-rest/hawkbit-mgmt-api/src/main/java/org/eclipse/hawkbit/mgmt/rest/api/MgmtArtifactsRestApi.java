/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsUpdateRequest;
import org.cosmos.models.mgmt.artifacts.dto.ScomoArtifactBindingRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
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
import org.springframework.web.multipart.MultipartFile;

/**
 * REST API providing (read-only) access to artifacts.
 */
// no request mapping specified here to avoid CVE-2021-22044 in Feign client
@Tag(name = "Artifacts", description = "REST API providing (read-only) access to artifacts.")
public interface MgmtArtifactsRestApi {

    /**
     * Handles the CREATE of artifacts.
     */
    @Operation(summary = "Create new artifacts", description = "Handles the POST request for creating new artifacts. The request body must always be a list of types. Required Permission: CREATE_REPOSITORY")
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
    @PostMapping(value = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL, consumes = {MediaType.APPLICATION_JSON_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtArtifacts> createArtifacts(@RequestBody @Valid MgmtArtifactsRequest artifactsRequestBodyPost, @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the CREATE of artifacts.
     */
    @Operation(summary = "Create new artifacts with file in body", description = "Handles the POST request for creating new artifacts. The request body must always be a list of types. Required Permission: CREATE_REPOSITORY")
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
    @PostMapping(value = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING, consumes = {MediaType.MULTIPART_FORM_DATA_VALUE}, produces = {MediaTypes.HAL_JSON_VALUE,
            MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<MgmtArtifacts> createArtifactsWithFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("filename") String filename,
            @RequestParam("fileType") String fileType,
            @RequestParam("description") String description,
            @RequestParam("sha256") String sha256,
            @RequestParam("signatureExpiryDate") Long signatureExpiryDate,
            @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the GET request of downloading a specific file {@link MgmtArtifacts} by artifactId
     * its <code>artifactId</code>.
     *
     * @param artifactId The ID of the requested artifacts
     * @return the {@link MgmtArtifacts}
     */
    @Operation(summary = "Download artifacts by artifactId", description = "Handles the GET request of downloading a single artifacts by artifactId.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.DOWNLOAD_ARTIFACT_V1_REQUEST_MAPPING, produces = {
            MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> downloadArtifact(@PathVariable("artifactId") String artifactId, @PathVariable("tenantId") Long tenantId);

    @Operation(
            summary = "Retrieve artifact information",
            description = "Handles the GET request for retrieving information about a specific artifact identified by artifactId and tenantId. The response contains details about the artifact including its filename, file type, description, SHA-256 hash, file size, software modules, and associated links. Required Permission: READ_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(value = MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING)
    ResponseEntity<MgmtArtifacts> getArtifactResource(@PathVariable("artifactId") String artifactId,
                                                      @PathVariable("tenantId") Long tenantId);

    /**
     * Handles the MODIFY the description of artifacts.
     */
    @Operation(summary = "Modify the description of artifacts", description = "Handles the PUT request for modifying an artifact. The request body must always be a list of types. Required Permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempted with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(value = MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING, consumes = {MediaType.APPLICATION_JSON_VALUE})
    ResponseEntity<Void> modifyDescriptionArtifacts(@PathVariable("artifactId") String artifactId, @RequestBody @Valid MgmtArtifactsUpdateRequest  artifactsRequestBodyPost, @PathVariable("tenantId") Long tenantId);


    @Operation(summary = "Associate Artifacts with Software Modules", description = "Handles the POST request for associating artifacts with software modules. The request body must always be a list of software module details. Required Permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully associated artifacts with Software Modules"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING)
    void createArtifactSoftwareModuleAssociation(@PathVariable("artifactId") String artifactId,
                                                 @PathVariable("tenantId") Long tenantId,
                                                 @Valid
                                                 @RequestBody SoftwareModuleArtifactBindingRequest softwareModuleArtifactBindingRequest);

    @Operation(summary  = "Associate an artifact with scomo", description = "Handles the POST request for associating an artifact with scomo. The request body contains the Scomo details. Required Permission: CREATE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully associated Artifact with Scomo"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.CREATE_ARTIFACTS_SCOMOS_ASSOCIATION_V1_REQUEST_MAPPING)
    void createArtifactSoftwareModuleAssociation(@PathVariable("artifactId") String artifactId,
                                                 @PathVariable("tenantId") Long tenantId,
                                                 @Valid
                                                 @RequestBody ScomoArtifactBindingRequest softwareModuleArtifactBindingRequest);

    @Hidden
    @Operation(summary = "Delete Artifacts", description = "Handles the DELETE request for removing an artifact by its ID. The artifact will only be deleted if it is not associated with any software module. Required Permission: DELETE_ARTIFACT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted the artifact"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. artifact is linked to a software module", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions to delete the artifact.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict, e.g. in case an entity is modified by another user in another request at the same time. You may retry your deletion request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempted with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.ARTIFACTS_RESOURCE_V1_REQUEST_MAPPING)
    ResponseEntity<Void> deleteArtifacts(@PathVariable("artifactId") String artifactId);

    @Operation(summary = "Unlink Software Modules from Artifacts", description = "Handles the DELETE request for dissociating artifacts from software modules. The request must specify the artifact ID and the list of software module details. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully unlinked artifacts with Software Modules"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.UNLINK_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING)
    ResponseEntity<Void> unlinkArtifactSoftwareModuleAssociation(@PathVariable("tenantId") Long tenantId,
                                                                 @PathVariable("artifactId") String artifactId,
                                                                 @PathVariable("softwareModuleId") String softwareModuleId);

    @Operation(summary = "Unlink Artifact from Scomo", description = "Handles the DELETE request for unlinking an artifact from scomo. The request must specify the artifact ID and scomo ID. Required Permission: DELETE_REPOSITORY")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully unlinked the artifact from scomo"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "E.g. in case an entity is created or modified by another user in another request at the same time. You may retry your modification request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempt with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.UNLINK_ARTIFACT_SCOMO_V1_REQUEST_MAPPING)
    ResponseEntity<Void> unlinkArtifactScomo(@PathVariable("tenantId") Long tenantId,
                                             @PathVariable("artifactId") String artifactId,
                                             @PathVariable("scomoId") String scomoId);

    @Operation(summary = "Purge Artifacts", description = "Handles the DELETE request for purging an artifact by its ID. Required Permission: DELETE_ARTIFACT")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully purged the artifact"),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions to purge the artifact.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The HTTP request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict, e.g. in case an entity is modified by another user in another request at the same time. You may retry your deletion request.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "The request was attempted with a media-type which is not supported by the server for this resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Bad Gateway. This will be received while calling CDN Purge API due to an invalid response from its host server or experiences other issues, such as server overloads, network problems, and configuration issues.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.PURGE_ARTIFACTS_V1_REQUEST_MAPPING)
    ResponseEntity<Void> purgeArtifacts(
            @Parameter(description = "Unique identifier of the artifact to purge", example = "12345")
            @PathVariable("artifactId") String artifactId,
            @PathVariable("tenantId") Long tenantId
    );

    @Operation(
            summary = "Replace an existing artifact with a new file",
            description = "Handles the PUT request for replacing an artifact. The request supports both file upload and file URL. Required Permission: REPLACE_ARTIFACT"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Artifact successfully replaced"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters or request", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "404", description = "Artifact not found", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "HTTP method not allowed", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "Not acceptable", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "409", description = "Conflict occurred", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "415", description = "Unsupported media type", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "502", description = "Bad Gateway from downstream service", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PutMapping(
            value = MgmtRestConstants.REPLACE_ARTIFACTS_V1_REQUEST_MAPPING,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
    )
    ResponseEntity<MgmtArtifacts> replaceArtifacts(
            @Parameter(description = "The new artifact file (optional if fileURL is provided)") @RequestParam(value = "file", required = false) MultipartFile file,
            @Parameter(description = "Name of the new file") @RequestParam("filename") @NotNull @NotEmpty String filename,
            @Parameter(description = "Type of the file (e.g., DELTA or FULL)") @RequestParam("fileType") @NotNull @NotEmpty String fileType,
            @Parameter(description = "Description of the artifact (optional)") @RequestParam(value = "description", required = false) String description,
            @Parameter(description = "SHA-256 hash of the artifact to be replaced") @PathVariable("oldSha256") @NotNull @NotEmpty String oldSha256,
            @Parameter(description = "Signature expiry date (epoch second)") @RequestParam("signatureExpiryDate") @NotNull Long signatureExpiryDate,
            @Parameter(description = "URL of the new file (optional if file is provided)") @RequestParam(value = "fileURL", required = false) String fileURL,
            @Parameter(description = "Tenant identifier") @PathVariable("tenantId") @NotNull Long tenantId,
            @Parameter(description = "SHA-256 hash of the new artifact") @RequestParam("sha256") @NotNull @NotEmpty String newSha256
    );

    @Operation(
            summary = "Return all artifacts",
            description = "Handles the GET request of retrieving all artifacts. Required Permission: READ_REPOSITORY"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @GetMapping(
            value = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING,
            produces = { MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE }
    )
    ResponseEntity<PagedList<MgmtArtifacts>> getArtifacts(
            @Parameter(description = "Tenant identifier", example = "1", required = true)
            @PathVariable("tenantId") @NotNull Long tenantId,

            @Parameter(description = "Paging offset (zero-based)", example = "0")
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET)
            @Min(0) int pagingOffsetParam,

            @Parameter(description = "Paging limit (max items per page)", example = "20")
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT)
            @Min(1) int pagingLimitParam,

            @Parameter(description = "Sorting parameter, e.g. 'filename:ASC'", example = "sort=filename:DESC")
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false)
            String sortParam,

            @Parameter(description = "Search filter in RSQL format", example = "q=filename==test.bin")
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false)
            String rsqlParam
    );

}