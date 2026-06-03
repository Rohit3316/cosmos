package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.annotations.TraceableObject;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactReplacementRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsUpdateRequest;
import org.cosmos.models.mgmt.artifacts.dto.ScomoArtifactBindingRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtArtifactsRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.S3Service;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtArtifactsMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.utils.SupportPackageManagementUtil;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.constants.ArtifactsAuditStatus;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.rest.swagger.SwaggerConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@RestController
@Tag(name = SwaggerConstants.ARTIFACTS)
@Validated
public class MgmtArtifactsResource implements MgmtArtifactsRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtArtifactsResource.class);
    private static final String VERSION_ALREADY_EXISTS = "The combination of source version %s and target version %s already exists for a different artifact for module %s";
    private static final String LOCATION_HEADER = "Location";

    private final ArtifactsManagement artifactsManagement;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final ArtifactFilesystemProperties artifactFilesystemProperties;
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    private final S3Service s3Service;

    MgmtArtifactsResource(final ArtifactsManagement artifactsManagement,
                          final ArtifactFilesystemProperties artifactFilesystemProperties,
                          final ArtifactUrlHandlerProperties artifactUrlHandlerProperties,
                          final SoftwareModuleManagement softwareModuleManagement, S3Service s3Service) {
        this.artifactsManagement = artifactsManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactFilesystemProperties = artifactFilesystemProperties;
        this.artifactUrlHandlerProperties = artifactUrlHandlerProperties;
        this.s3Service = s3Service;
    }

    /**
     * Creates a new artifact for the specified tenant.
     * <p>
     * This method processes a request to create an artifact using the provided details in the request body.
     * It validates the input, delegates the creation to the artifacts management service, and returns the created artifact.
     * <p>
     * Outcomes:
     * <ul>
     *   <li>Returns HTTP 201 (Created) with the created artifact details if successful.</li>
     *   <li>Throws {@link GenericSpServerException} if any error occurs during creation.</li>
     * </ul>
     *
     * @param artifactsRequest the request body containing artifact details; must be valid and not null
     * @param tenantId         the ID of the tenant for which the artifact is created
     * @return ResponseEntity containing the created artifact and HTTP status
     * @throws GenericSpServerException if an error occurs during artifact creation
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtArtifacts> createArtifacts(@RequestBody @Valid @TraceableObject MgmtArtifactsRequest artifactsRequest, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received Create artifacts request");
        try {
            Artifacts artifacts = artifactsManagement.createArtifactFromFileURL(artifactsRequest, tenantId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MgmtSoftwareModuleMapper.toCreateResponse(artifacts));
        } catch (ValidationException | AbstractServerRtException ee) {
            throw ee;
        } catch (Exception e) {
            throw new GenericSpServerException(e.getMessage(), e);
        }
    }

    /**
     * Modifies the description of an existing artifact identified by its ID.
     * This method processes the incoming request to update the description of an artifact.
     * It first validates that the provided description is not null. It then attempts to retrieve
     * the artifact by its ID. If the artifact is found, it updates the description and saves the changes.
     * <p>
     * The method handles various outcomes:
     * - If the description in the request body is null, it immediately returns a Bad Request (400) status.
     * - If the artifact is not found, it returns a Not Found (404) status.
     * - If the artifact is found and updated successfully, it returns an OK (200) status.
     * - If any other exceptions are encountered during the process (for instance, database connectivity issues),
     * it logs the error and returns an Internal Server Error (500) status.
     * <p>
     * In the event that updating the artifact description would violate unique constraints
     * (handled via EntityAlreadyExistsException), the method will rethrow that specific exception,
     * indicating that the operation could not be completed due to existing constraints.
     *
     * @param artifactId       The ID of the artifact to modify, which should be a valid identifier for an existing artifact.
     * @param artifactsRequest The request body containing the new description for the artifact.
     *                         This object must contain a non-null description field.
     * @return ResponseEntity<Void> A ResponseEntity object representing the outcome of the operation.
     * It contains no body but a status code indicating the result of the request.
     * @throws EntityAlreadyExistsException If the description update attempts to violate unique constraints,
     *                                      such as setting a description that must be unique but already exists.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> modifyDescriptionArtifacts(@TraceableField @PathVariable("artifactId") String artifactId, @RequestBody @Valid @TraceableObject MgmtArtifactsUpdateRequest artifactsRequest
            , @PathVariable("tenantId") Long tenantId) {

        LOG.debug("Received Modify description artifacts request");

        if (artifactsRequest.getDescription() == null) {
            throw new ValidationException("The description is empty");
        }

        Artifacts existingArtifacts = findArtifactsByIdOrElseThrowNotFound(artifactId);
        existingArtifacts.setDescription(artifactsRequest.getDescription());
        artifactsManagement.update(existingArtifacts);

        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * Finds artifacts by their file name and throws an exception if not found.
     *
     * @param fileName the name of the file to search for
     * @return the list of artifacts with the given file name
     */
    private List<Artifacts> findArtifactsByFileName(final String fileName) {

        return artifactsManagement.findByFileName(fileName);

    }

    /**
     * Creates a new artifact with an uploaded file for the specified tenant.
     * <p>
     * This endpoint allows clients to upload a file and create an artifact record in the system.
     * The file is processed and stored, and metadata such as filename, file type, description,
     * SHA-256 hash, and signature expiry date are recorded.
     * <p>
     * Outcomes:
     * <ul>
     *   <li>Returns HTTP 201 (Created) with the created artifact details if successful.</li>
     *   <li>Throws {@link ValidationException} if validation fails (e.g., missing or invalid parameters).</li>
     *   <li>Throws {@link AbstractServerRtException} for server-side errors.</li>
     *   <li>Throws {@link GenericSpServerException} for unexpected errors.</li>
     * </ul>
     *
     * @param file                the uploaded file to be associated with the artifact (must not be null or empty)
     * @param filename            the name of the file (must not be null or empty)
     * @param fileType            the type of the file (e.g., FULL, DELTA)
     * @param description         a description of the artifact
     * @param sha256              the SHA-256 hash of the file for integrity verification
     * @param signatureExpiryDate the expiry date of the artifact's signature (in epoch milliseconds)
     * @param tenantId            the ID of the tenant for which the artifact is created
     * @return ResponseEntity containing the created artifact and HTTP status
     * @throws ValidationException       if any validation error occurs
     * @throws AbstractServerRtException for server-side runtime exceptions
     * @throws GenericSpServerException  for unexpected errors during artifact creation
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtArtifacts> createArtifactsWithFile(
            @RequestParam("file") MultipartFile file,
            @TraceableField @RequestParam("filename") String filename,
            @RequestParam("fileType") String fileType,
            @RequestParam("description") String description,
            @TraceableField @RequestParam("sha256") String sha256,
            @RequestParam("signatureExpiryDate") Long signatureExpiryDate,
            @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received Create artifacts with file request");
        try {
            Artifacts artifacts = artifactsManagement.createArtifactFromMultipartFile(
                    file, filename, fileType, description, sha256, signatureExpiryDate);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(MgmtSoftwareModuleMapper.toCreateResponse(artifacts));
        } catch (ValidationException | AbstractServerRtException ee) {
            throw ee;
        } catch (Exception e) {
            throw new GenericSpServerException(e.getMessage(), e);
        }
    }


    /**
     * Retrieves an artifact by its ID.
     *
     * @param id the ID of the artifact to retrieve
     * @return the found artifact
     */
    private Artifacts findArtifactsByIdOrElseThrowNotFound(final String id) {

        return artifactsManagement.getArtifactsById(Long.parseLong(id))
                .orElseThrow(() -> new EntityNotFoundException(Artifacts.class, id));
    }

    /**
     * Downloads the specified artifact.
     *
     * @param artifactId the ID of the artifact to download
     * @return a ResponseEntity containing the resource to download
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> downloadArtifact(@TraceableField @PathVariable("artifactId") final String artifactId, @PathVariable("tenantId") Long tenantId) {

        LOG.debug("Received Download artifact request");
        Artifacts artifact = findArtifactsByIdOrElseThrowNotFound(artifactId);

        String fileName = artifact.getFileName();
        String tenant = artifact.getTenant();
        String bucketName = artifactFilesystemProperties.getS3bucket().getName();
        String checksum = artifact.getSha256Hash();
        String s3ObjName = s3Service.buildS3ObjectName(tenant, checksum, fileName);
        Long preSignedUrlExpiryTime = artifactFilesystemProperties.getS3bucket().getPreSignedUrlExpiryTime();
        URL url = s3Service.generatePresignedUrl(bucketName, s3ObjName, preSignedUrlExpiryTime);

        if (s3Service.isValidGetUrl(url)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(LOCATION_HEADER, url.toString());
            return new ResponseEntity<>(headers, HttpStatus.valueOf(302));
        } else {
            throw new EntityNotFoundException("Artifact Not Available on Server");
        }
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtArtifacts> getArtifactResource(@TraceableField @PathVariable("artifactId") String artifactId,
                                                             @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received get Artifact resource request");
        Artifacts artifact = artifactsManagement.getArtifactsById(Long.parseLong(artifactId)).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, artifactId));
        final MgmtArtifacts response = MgmtSoftwareModuleMapper.toResponse(artifact);
        String downloadUrl = artifactUrlHandlerProperties.getCustom().getDownload().replace("{tenant}", String.valueOf(tenantId)).replace("{SHA256}",
                response.getHashes().getSha256()).replace("{artifactFileName}", response.getFilename());

        response.add(Link.of(downloadUrl).withRel(MgmtRestConstants.DOWNLOAD));
        response.add(linkTo(methodOn(MgmtArtifactsRestApi.class).downloadArtifact(artifactId, tenantId))
                .withRel(MgmtRestConstants.DOWNLOAD_HTTP).expand());
        return ResponseEntity.ok().body(response);
    }

    /**
     * Handles the POST request for associating artifacts with software module.
     * The request body must always be a software module detail.
     * Required Permission: CREATE_REPOSITORY
     *
     * @param artifactId                           The ID of the artifact to associate with software modules
     * @param tenantId                             The ID of the tenant
     * @param softwareModuleArtifactBindingRequest SoftwareModuleArtifactBinding object representing the association between artifacts and software modules
     */

    @Override
    @TenantAware
    @TraceableMethod
    public void createArtifactSoftwareModuleAssociation(@TraceableField final String artifactId, final Long tenantId, final @Valid @TraceableObject SoftwareModuleArtifactBindingRequest softwareModuleArtifactBindingRequest) {
        artifactsManagement.createArtifactSoftwareModuleAssociation(artifactId, tenantId, softwareModuleArtifactBindingRequest);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public void createArtifactSoftwareModuleAssociation(@TraceableField final String artifactId, final Long tenantId, final @Valid @TraceableObject ScomoArtifactBindingRequest scomoArtifactBindingRequest) {

        LOG.debug("Received Create Artifact Software Module Scomo Name Association request");

        SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoArtifactBindingRequest.getScomoId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoArtifactBindingRequest.getScomoId()));

        SoftwareModuleArtifactBindingRequest softwareRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModule.getId().intValue())
                .sourceVersion(scomoArtifactBindingRequest.getSourceVersion())
                .targetVersion(scomoArtifactBindingRequest.getTargetVersion())
                .build();

        createArtifactSoftwareModuleAssociation(artifactId, tenantId, softwareRequest);

    }

    /**
     * Handles the DELETE request for removing an artifact.
     * If the artifact is linked to a software module, the deletion is not allowed and a ValidationException is thrown.
     * An audit record of the artifact is created before deletion.
     * Required Permission: DELETE_REPOSITORY
     *
     * @param artifactId The ID of the artifact to be deleted.
     * @return A ResponseEntity with HTTP status OK if the artifact is successfully deleted.
     * @throws EntityNotFoundException if the artifact with the specified ID is not found.
     * @throws ValidationException     if the artifact is linked to a software module and cannot be deleted.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteArtifacts(@TraceableField @PathVariable("artifactId") String artifactId) {
        LOG.debug("Received Delete artifact request");

        Artifacts artifact = artifactsManagement.getArtifactsById(Long.parseLong(artifactId)).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, artifactId));

        List<ArtifactSoftwareModuleAssociation> associations = artifactsManagement.findAssociationByArtifactId(artifact.getId());

        if (!associations.isEmpty()) {
            throw new ValidationException("Artifact is linked to software module cannot be deleted.");
        }

        artifactsManagement.deleteArtifactsById(artifact.getId(), ArtifactsAuditStatus.DELETED);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles the DELETE request for dissociating artifacts from software modules.
     * Required Permission: CREATE_REPOSITORY
     *
     * @param artifactId       The ID of the artifact to dissociate from the software module
     * @param softwareModuleId The ID of the software module from which to dissociate the artifact
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unlinkArtifactSoftwareModuleAssociation(final Long tenantId, @TraceableField final String artifactId, @TraceableField final String softwareModuleId) {
        artifactsManagement.unlinkArtifactSoftwareModuleAssociation(tenantId, artifactId, softwareModuleId);
        return ResponseEntity.ok().build();
    }

    /**
     * Handles the DELETE request for dissociating artifacts from software modules.
     * Required Permission: CREATE_REPOSITORY
     *
     * @param artifactId The ID of the artifact to dissociate from the software module
     * @param scomoId    The name of the software module from which to dissociate the artifact
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unlinkArtifactScomo(@PathVariable("tenantId") Long tenantId, @TraceableField final String artifactId, @TraceableField final String scomoId) {

        LOG.debug("Received Unlink Artifact Software Module Scomo Association request");

        SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        return unlinkArtifactSoftwareModuleAssociation(tenantId, artifactId, softwareModule.getId().toString());
    }

    /**
     * Handles the DELETE request for purging an artifact from CDN.
     * An audit record of the artifact is created before deletion.
     * Required Permission: DELETE_REPOSITORY
     *
     * @param artifactId The ID of the artifact to be purged.
     * @return A ResponseEntity with HTTP status OK if the artifact is successfully purged.
     * @throws EntityNotFoundException if the artifact with the specified ID is not found.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> purgeArtifacts(@TraceableField @PathVariable("artifactId") String artifactId, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received Purge artifact request");
        Artifacts artifact = artifactsManagement.getArtifactsById(Long.parseLong(artifactId)).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, artifactId));
        artifactsManagement.purgeArtifacts(artifact);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Replaces an existing artifact with a new file and metadata.
     * <p>
     * This method processes a request to replace an artifact, updating its file, filename, file type,
     * description, SHA-256 hashes, signature expiry date, and file URL for the specified tenant.
     * </p>
     *
     * @param file                the new artifact file to upload (may be null if using fileURL)
     * @param filename            the name of the new file
     * @param fileType            the type of the file (e.g., FULL, DELTA)
     * @param description         a description for the artifact
     * @param oldSha256           the SHA-256 hash of the artifact being replaced
     * @param signatureExpiryDate the expiry date of the artifact's signature (in epoch milliseconds)
     * @param fileURL             the URL of the new file (if not uploading directly)
     * @param tenantId            the ID of the tenant
     * @param newSha256           the SHA-256 hash of the new artifact
     * @return ResponseEntity\<Void\> with HTTP 200 (OK) if the replacement is successful
     * @throws GenericSpServerException if the replacement fails
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtArtifacts> replaceArtifacts(
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam("filename") @TraceableField String filename,
            @RequestParam("fileType") @TraceableField String fileType,
            @RequestParam(value = "description", required = false) String description,
            @PathVariable("oldSha256") @TraceableField String oldSha256,
            @RequestParam("signatureExpiryDate") @TraceableField Long signatureExpiryDate,
            @RequestParam(value = "fileURL", required = false) String fileURL,
            @PathVariable("tenantId") @TraceableField Long tenantId,
            @RequestParam("sha256") @TraceableField String newSha256) {

        LOG.debug("Received replace artifact request for filename: {}, tenantId: {}", filename, tenantId);

        try {
            MgmtArtifactReplacementRequest replaceArtifactsRequest = MgmtArtifactReplacementRequest.builder()
                    .file(file)
                    .filename(SupportPackageManagementUtil.sanitizeFileName(filename))
                    .fileType(fileType.toUpperCase())
                    .description(description)
                    .oldSha256(oldSha256)
                    .signatureExpiryDate(signatureExpiryDate)
                    .newSha256(newSha256)
                    .fileURL(fileURL)
                    .build();

            Artifacts artifact = artifactsManagement.replaceArtifacts(replaceArtifactsRequest, tenantId);
            final MgmtArtifacts response = MgmtSoftwareModuleMapper.toResponse(artifact);
            return ResponseEntity.ok().body(response);
        } catch (ValidationException | AbstractServerRtException ee) {
            LOG.error("Validation or server exception during artifact replacement: {}", ee.getMessage(), ee);
            throw ee;
        } catch (Exception e) {
            LOG.error("Unexpected error during artifact replacement: {}", e.getMessage(), e);
            throw new GenericSpServerException(e.getMessage(), e);
        }
    }

    /**
     * Retrieves a paginated and optionally filtered list of artifacts for a given tenant.
     * <p>
     * Supports pagination, sorting, and RSQL-based filtering. Returns a {@link PagedList} containing
     * the mapped artifact DTOs and the total count of matching artifacts.
     * </p>
     *
     * @param tenantId          the ID of the tenant whose artifacts are to be retrieved
     * @param pagingOffsetParam the zero-based offset for pagination
     * @param pagingLimitParam  the maximum number of items per page
     * @param sortParam         the sorting parameter (e.g., "filename:ASC"), may be null
     * @param rsqlParam         the RSQL filter string, may be null or blank
     * @return ResponseEntity containing a paged list of {@link MgmtArtifacts} and the total count
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtArtifacts>> getArtifacts(
            @TraceableField Long tenantId,
            @TraceableField int pagingOffsetParam,
            @TraceableField int pagingLimitParam,
            @TraceableField String sortParam,
            @TraceableField String rsqlParam) {

        LOG.debug("Received getArtifacts request for tenantId={}, offset={}, limit={}, sortParam={}, rsqlParam={}",
                tenantId, pagingOffsetParam, pagingLimitParam, sortParam, rsqlParam);

        // Sanitize paging and sorting parameters to ensure valid values
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeArtifactsSortParam(sortParam);

        // Build pageable object for query
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Artifacts> artifactsSlice;
        final long totalArtifacts;

        // Apply RSQL filtering if provided, otherwise fetch all
        if (rsqlParam != null && !rsqlParam.trim().isEmpty()) {
            LOG.debug("Applying RSQL filter: {}", rsqlParam);
            artifactsSlice = artifactsManagement.findByRsql(pageable, rsqlParam);
            // Cast to Page to get total elements for filtered result
            totalArtifacts = ((Page<Artifacts>) artifactsSlice).getTotalElements();
        } else {
            LOG.debug("Fetching all artifacts without RSQL filter");
            artifactsSlice = artifactsManagement.findAll(pageable);
            totalArtifacts = artifactsManagement.count();
        }

        final List<MgmtArtifacts> responseList = MgmtArtifactsMapper.mapToMgmtArtifactsResponse(artifactsSlice.getContent());
        LOG.debug("Returning {} artifacts (total count: {}) for tenantId={}", responseList.size(), totalArtifacts, tenantId);
        return ResponseEntity.ok(new PagedList<>(responseList, totalArtifacts));
    }

}
