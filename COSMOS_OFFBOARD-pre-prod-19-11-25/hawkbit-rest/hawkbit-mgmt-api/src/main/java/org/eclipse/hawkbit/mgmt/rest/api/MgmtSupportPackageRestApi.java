package org.eclipse.hawkbit.mgmt.rest.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST API for managing Support Packages
 */
@Tag(name = "Support Packages", description = "REST APIs for accessing Support Packages.")
public interface MgmtSupportPackageRestApi {

    /**
     *
     * Creates a new support package for a specific rollout.
     *
     * @param tenantId                        The ID of the tenant.
     * @param rolloutId                       The ID of the rollout.
     * @param mgmtSupportPackageCreateRequest The request object containing the necessary information to create a support package.
     */
    @Operation(summary = "Return 201", description = "Handles the creation of ESP and RSP.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully retrieved"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @PostMapping(value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS)
    @TenantAware
    @ResponseStatus(HttpStatus.CREATED)
    ResponseEntity<MgmtSupportPackage> createSupportPackageWithFileUrl(@PathVariable("tenantId") Long tenantId,
                                                            @PathVariable("rolloutId") Long rolloutId,
                                                            @RequestBody MgmtFileUrlSupportPackageCreateRequest mgmtSupportPackageCreateRequest);



    @Operation(summary = "Return 201", description = "Handles the creation of ESP and RSP with file attachment.<br>" +
            "<ul><li><i>Note: Controller Ids should be a comma-separated string (e.g., controllerId1, controllerId2).</i></li></ul>")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Successfully created support package"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })


@PostMapping(
    value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS,
    consumes = {MediaType.MULTIPART_FORM_DATA_VALUE},
    produces = {MediaTypes.HAL_JSON_VALUE, MediaType.APPLICATION_JSON_VALUE}
)
@TenantAware
@ResponseStatus(HttpStatus.CREATED)
ResponseEntity<MgmtSupportPackage> createSupportPackageWithFile(
    @Parameter(description = "The ID of the tenant") @PathVariable("tenantId") Long tenantId,
    @Parameter(description = "The ID of the rollout") @PathVariable("rolloutId") Long rolloutId,
    @Parameter(description = "The file to upload") @RequestParam("file") MultipartFile file,
    @Parameter(description = "The name of the file") @RequestParam("fileName") String fileName,
    @Parameter(description = "The type of the file") @RequestParam("fileType") String fileType,
    @Parameter(description = "The version of the file") @RequestParam("fileVersion") String fileVersion,
    @Parameter(description = "Comma-separated list of controller IDs", required = false) @RequestParam(value = "controllerIds", required = false) List<String> controllerIds,
    @Parameter(description = "SHA-256 hash of the file") @RequestParam("sha256") String sha256,
    @Parameter(description = "ECU node address", required = false) @RequestParam(value = "ecuNodeAddress", required = false) String ecuNodeAddress,
    @Parameter(description = "Description of the file content") @RequestParam("fileContentDescription") String fileContentDescription,
    @Parameter(description = "URL with file information") @RequestParam("fileInfoUrl") String fileInfoUrl,
    @Parameter(description = "Metadata for the file") @RequestParam("fileMetadata") String fileMetadata
);
    /**
     * Retrieves all support packages (ESP and RSP) for a specific rollout with pagination and sorting options.
     *
     * @param tenantId         The ID of the tenant.
     * @param rolloutId        The ID of the rollout for which support packages are to be retrieved.
     * @param pagingOffsetParam The offset for pagination, indicating the starting point of the result set.
     * @param pagingLimitParam  The maximum number of results to return per page.
     * @param sortParam         The sorting criteria for the results (optional).
     * @return A ResponseEntity containing a paginated list of MgmtSupportPackage objects.
     */
    @GetMapping(value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS)
    ResponseEntity<PagedList<MgmtSupportPackage>> getAllSupportPackages(@PathVariable("tenantId") Long tenantId,
                                                                        @PathVariable("rolloutId") Long rolloutId,
                                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                        @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam);

    /**
     * Unlinks or deletes support packages for a specific rollout.
     *
     * @param tenantId  The ID of the tenant.
     * @param rolloutId The ID of the rollout for which ESP/RSP packages have to be unlinked or deleted.
     */
    @Operation(summary = "Return 200", description = "Handles the =deletion of ESP and RSP.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted support-packages"),
            @ApiResponse(responseCode = "400", description = "Bad Request - e.g. invalid parameters", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "401", description = "The request requires user authentication.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions, entity is not allowed to be changed (i.e. read-only) or data volume restriction applies.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "405", description = "The http request method is not allowed on the resource.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "406", description = "In case accept header is specified and not application/json.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true))),
            @ApiResponse(responseCode = "429", description = "Too many requests. The server will refuse further attempts and the client has to wait another second.", content = @Content(mediaType = "application/json", schema = @Schema(hidden = true)))
    })
    @DeleteMapping(value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS)
    @ResponseStatus(HttpStatus.OK)
    void deleteSupportPackage(@PathVariable("tenantId") Long tenantId,
                              @PathVariable("rolloutId") Long rolloutId);


    /**
     * Retrieves a specific support package for a given tenant and rollout.
     *
     * @param tenantId       The ID of the tenant.
     * @param rolloutId      The ID of the rollout.
     * @param packageId The ID of the support package.
     * @return A ResponseEntity containing a list of MgmtSupportPackage objects.
     */
    @GetMapping(value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_ID_V1_REQUEST_MAPPING_TENANT_TARGETS)
    ResponseEntity<MgmtSupportPackage> getSupportPackageById(@PathVariable("tenantId") Long tenantId,
                                                                   @PathVariable("rolloutId") Long rolloutId,
                                                                   @PathVariable("type") String type,
                                                                   @PathVariable("packageId") Long packageId);



    /**
     * Downloads a specific support package for a given tenant and rollout.
     *
     * @param tenantId   The ID of the tenant.
     * @param rolloutId  The ID of the rollout.
     * @param packageId  The ID of the support package.
     * @param type       The type of the support package (ESP/RSP).
     * @return A ResponseEntity containing the support package file for download.
     */
    @GetMapping(value = MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS)
    ResponseEntity<Void> downloadSupportPackage(@PathVariable Long tenantId,
                                                   @PathVariable Long rolloutId,
                                                    @PathVariable Long packageId,
                                                    @RequestParam String type) ;
}
