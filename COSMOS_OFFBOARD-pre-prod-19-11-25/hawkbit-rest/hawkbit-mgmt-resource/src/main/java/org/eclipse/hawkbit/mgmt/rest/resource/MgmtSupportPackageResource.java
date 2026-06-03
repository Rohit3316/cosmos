package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSupportPackageRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.S3Service;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityCannotNullException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeHelper;
import org.eclipse.hawkbit.repository.jpa.utils.SupportPackageManagementUtil;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.rest.exception.ResponseExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for managing support packages .
 */
@Slf4j
@RestController
@Tag(name = "Support Packages")
public class MgmtSupportPackageResource implements MgmtSupportPackageRestApi {


    private static final Logger LOG = LoggerFactory.getLogger(MgmtSupportPackageResource.class);
    private static final String LOCATION_HEADER = "Location";
    private static final String ESP = "ESP";
    private static final String RSP = "RSP";
    // add FINISHING,CANCELING and CANCELED to the list of closed rollouts once added to RolloutStatus
    private static final List<RolloutStatus> ROLLOUT_STATUS_CLOSED_FOR_UPDATES = Arrays.asList(RolloutStatus.DELETED,
            RolloutStatus.DELETING, RolloutStatus.FINISHED, RolloutStatus.FINISHING, RolloutStatus.CANCELING, RolloutStatus.CANCELED);
    private static final List<RolloutStatus> ROLLOUT_STATUSES_ALLOWED_FOR_DELETE = Arrays.asList(RolloutStatus.DELETED,
            RolloutStatus.DELETING, RolloutStatus.DRAFT);
    private static final List<RolloutStatus> ROLLOUT_STATUSES_FOR_CREATE_SUPPORT_PACKAGES = List.of(RolloutStatus.PAUSED, RolloutStatus.RUNNING);
    private final TargetManagement targetManagement;
    private final RolloutManagement rolloutManagement;
    private final SupportPackageManagement supportPackageManagement;
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    private final EcuModelManagement ecuModelManagement;
    private final SupportPackageFileSystemProperties supportPackageFileSystemProperties;
    private final S3Service s3Service;
    private final String cdnHost;
    private final String cdnRootDirectory;
    private final org.eclipse.hawkbit.tenancy.TenantAware tenantAware;
    @Value("${hawkbit.artifact.validate-file-size}")
    private boolean validateFileSize;
    /**
     * The name of the S3 bucket where support packages are stored.
     * <p>
     * This value is injected from the application configuration property
     * {@code cosmos.server.s3.support-package.bucket.name}.
     */
    @Value("${cosmos.server.s3.support-package.bucket.name}")
    private String bucketName;

    public MgmtSupportPackageResource(TargetManagement targetManagement,
                                      RolloutManagement rolloutManagement,
                                      SupportPackageManagement supportPackageManagement,
                                      ArtifactUrlHandlerProperties artifactUrlHandlerProperties,
                                      EcuModelManagement ecuModelManagement,
                                      SupportPackageFileSystemProperties supportPackageFileSystemProperties, S3Service s3Service,
                                      org.eclipse.hawkbit.tenancy.TenantAware tenantAware,
                                      @Value("${hawkbit.artifact.url.cdn.host}") String cdnHost,
                                      @Value("${hawkbit.artifact.url.cdn.rootDirectory}") String cdnRootDirectory) {
        this.targetManagement = targetManagement;
        this.rolloutManagement = rolloutManagement;
        this.supportPackageManagement = supportPackageManagement;
        this.artifactUrlHandlerProperties = artifactUrlHandlerProperties;
        this.ecuModelManagement = ecuModelManagement;
        this.supportPackageFileSystemProperties = supportPackageFileSystemProperties;
        this.s3Service = s3Service;
        this.tenantAware = tenantAware;
        this.cdnHost = cdnHost;
        this.cdnRootDirectory = cdnRootDirectory;
    }

    /**
     * Creates a support package for a specific rollout.
     *
     * @param tenantId       The tenant ID.
     * @param rolloutId      The rollout ID.
     * @param supportPackage The support package creation request.
     * @throws EntityNotFoundException If the rollout or target is not found.
     * @throws ValidationException     If the validation fails for ESP file type.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSupportPackage> createSupportPackageWithFileUrl(@PathVariable("tenantId") Long tenantId,
                                                                              @PathVariable("rolloutId") @TraceableField Long rolloutId,
                                                                              @Valid
                                                                              @RequestBody MgmtFileUrlSupportPackageCreateRequest supportPackage) {
        log.debug("Received request to create support package via File Url");
        return createSupportPackage(tenantId, rolloutId, supportPackage);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSupportPackage> createSupportPackageWithFile(Long tenantId, Long rolloutId, MultipartFile file, String fileName, String fileTypeStr, String fileVersion, List<String> controllerIds, String sha256, String ecuNodeAddress, String fileContentDescription, String fileInfoUrl, String metaData) {

        //Map the fileMetadata with object & convert the filetype String to enum.
        MgmtSupportPackageFileType fileType;
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, String> fileMetadata;
        try {
            fileMetadata = objectMapper.readValue(metaData, new TypeReference<>() {
            });
            fileType = MgmtSupportPackageFileType.valueOf(fileTypeStr);
        } catch (JsonProcessingException e) {
            throw new ValidationException("Invalid metadata format", e);

        } catch (IllegalArgumentException e) {
            return handleEnumConversionException(fileTypeStr);
        }

        if (ESP.equals(fileType.getCategory())) {
            if (controllerIds == null || controllerIds.isEmpty()) {
                throw new ValidationException("ControllerId is not provided in the request");
            }
            if (ecuNodeAddress == null || ecuNodeAddress.isEmpty()) {
                throw new ValidationException("Ecu node address is not provided in the request");
            }
        }

        MgmtFileSupportPackageCreateRequest mgmtFileSupportPackageCreateRequest =
                MgmtFileSupportPackageCreateRequest.builder()
                        .file(file)
                        .fileName(fileName)
                        .fileType(fileType)
                        .sha256(sha256)
                        .fileVersion(fileVersion)
                        .controllerIds(controllerIds)
                        .ecuNodeAddress(ecuNodeAddress)
                        .fileContentDescription(fileContentDescription)
                        .fileInfoUrl(fileInfoUrl)
                        .fileMetadata(fileMetadata)
                        .build();

        if (file.isEmpty()) {
            throw new EntityCannotNullException("File cannot be empty");
        }

        return createSupportPackage(tenantId, rolloutId, mgmtFileSupportPackageCreateRequest);
    }

    private ResponseEntity<MgmtSupportPackage> createSupportPackage(Long tenantId, Long rolloutId, MgmtBaseSupportPackageCreateRequest supportPackage) {
        log.debug("Creating new support package for rollout: {}", rolloutId);

        if (validateFileSize) {
            if (supportPackage instanceof MgmtFileUrlSupportPackageCreateRequest urlRequest) {
                FileSizeHelper.isFileSizeAcceptable(urlRequest.getFileUrl());
            } else if (supportPackage instanceof MgmtFileSupportPackageCreateRequest fileRequest) {
                FileSizeHelper.isFileSizeAcceptable(fileRequest.getFile().getSize());
            }
        }

        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        // check rollout stats is any one of these values in the list
        if (ROLLOUT_STATUS_CLOSED_FOR_UPDATES.contains(rollout.getStatus())) {
            throw new ValidationException("Support packages (ESP/RSP) cannot be added to a closed (FINISHED/CANCELED) rollout");
        }

        boolean isEsp = supportPackage.getFileType().getCategory().equals(ESP);
        JpaEsp espRequest = MgmtRolloutMapper.fromEspRequest(supportPackage, rollout);

        if (isEsp) {
            supportPackageManagement.validateTargetsForRollout(rollout, espRequest);
            validateEspPackage(supportPackage, rollout);
        } else if (!rollout.getStatus().toString().equalsIgnoreCase(String.valueOf(RolloutStatus.DRAFT))) {
            throw new ValidationException("Support package of RSP file type cannot be added except when the rollout in DRAFT status");
        }

        String sanitizedFileName = SupportPackageManagementUtil.sanitizeFileName(supportPackage.getFileName());
        supportPackage.setFileName(sanitizedFileName);

        MgmtSupportPackage response;
        if (isEsp) {
            supportPackageManagement.checkForESPSupportingPackageType(espRequest, rollout);
            List<Esp> espSupportPackages = new ArrayList<>();
            Esp esp = supportPackageManagement.handleEspSupportPackage(espRequest, supportPackage, rolloutId, tenantId);
            espSupportPackages.add(esp);
            response = MgmtRolloutMapper.toResponseEspRolloutPkgId(espSupportPackages);
        } else {
            List<Rsp> rspSupportPackages = new ArrayList<>();
            JpaRsp rspRequest = MgmtRolloutMapper.fromRspRequest(supportPackage);
            Rsp rsp = supportPackageManagement.handleRspSupportPackage(rspRequest, supportPackage, rolloutId, tenantId);
            rspSupportPackages.add(rsp);
            response = MgmtRolloutMapper.toResponseRspRolloutPkgId(rspSupportPackages);
        }
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * validates the ESP support package by checking if the VINs and ECU Node Address are present and correctly associated.
     *
     * @param supportPackage
     */
    private void validateEspPackage(MgmtBaseSupportPackageCreateRequest supportPackage, Rollout rollout) {

        if (supportPackage.getControllerIds().isEmpty()) {
            throw new ValidationException("Controller Id  cannot be empty for ESP");
        }
        if (supportPackage.getEcuNodeAddress().isEmpty()) {
            throw new ValidationException("ECU Node Address cannot be empty for ESP");
        }
        if (!ecuModelManagement.isEcuNodeAddressExists(supportPackage.getEcuNodeAddress())) {
            throw new ValidationException("ECU Node Address not found in COSMOS");
        }
        if (!targetManagement.isEcuNodeAddressMatchControllerIds(supportPackage.getEcuNodeAddress(), supportPackage.getControllerIds())) {
            throw new ValidationException("ControllerIds do not match with ECU Node Address");
        }
    }

    /**
     * Retrieves all support packages (ESP and RSP) for a specific rollout with pagination and sorting options.
     *
     * @param tenantId          The ID of the tenant.
     * @param rolloutId         The ID of the rollout for which support packages are to be retrieved.
     * @param pagingOffsetParam The offset for pagination, indicating the starting point of the result set.
     * @param pagingLimitParam  The maximum number of results to return per page.
     * @param sortParam         The sorting criteria for the results (optional).
     * @return A ResponseEntity containing a paginated list of MgmtSupportPackage objects.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtSupportPackage>> getAllSupportPackages(@PathVariable("tenantId") Long tenantId,
                                                                               @PathVariable("rolloutId") @TraceableField Long rolloutId,
                                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) String rsqlParam) {
        LOG.debug("Received request to get all support packages");

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSupportPackagesParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        LOG.debug("Pageable created with offset: {}, limit: {}, sort: {}", sanitizedOffsetParam, sanitizedLimitParam, sorting);

        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        Slice<Esp> espSupportPackages;
        Slice<Rsp> rspSupportPackages;
        if (rsqlParam != null && !rsqlParam.isEmpty()) {
            espSupportPackages = supportPackageManagement.findEspsByRolloutRsql(rsqlParam, rollout.getId(), pageable);
            rspSupportPackages = supportPackageManagement.findRspsByRolloutRsql(rsqlParam, rollout.getId(), pageable);
        } else {
            espSupportPackages = supportPackageManagement.getESPSupportPackages(rollout.getId(), pageable);
            rspSupportPackages = supportPackageManagement.getRSPSupportPackages(rollout.getId(), pageable);
        }

        if (espSupportPackages.isEmpty() && rspSupportPackages.isEmpty()) {
            throw new EntityNotFoundException(String.format("Support Packages not found for the rollout ID: %d", rolloutId));
        }

        List<MgmtSupportPackage> response = MgmtRolloutMapper.toResponseRollout(espSupportPackages.getContent()
                , rspSupportPackages.getContent());
        for (MgmtSupportPackage pkg : response) {
            addDownloadLinks(rolloutId, pkg);
        }
        return ResponseEntity.ok().body(new PagedList<>(response, supportPackageManagement.countSupportPackages(rolloutId)));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public void deleteSupportPackage(Long tenantId, @TraceableField Long rolloutId) {
        LOG.debug("Received request to delete support package");
        // check if the rollout exists
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        // check rollout status is not DRAFT then throw exception
        // check rollout stats is any one of these values in the list
        if (!ROLLOUT_STATUSES_ALLOWED_FOR_DELETE.contains(rollout.getStatus())) {
            throw new ValidationException("Support packages (ESP/RSP) cannot be deleted to a rollout with status in " + rollout.getStatus());
        }
        List<Esp> esp = supportPackageManagement.getESPSupportPackages(rollout.getId());
        List<Rsp> rsp = supportPackageManagement.getRSPSupportPackages(rollout.getId());
        supportPackageManagement.deleteSupportPackage(rollout.getId(), esp, rsp);
    }

    /**
     * Retrieves a specific support package for a given tenant and rollout.
     *
     * @param tenantId  The tenant ID.
     * @param rolloutId The rollout ID.
     * @param packageId The support package ID.
     * @return A ResponseEntity containing a list of MgmtSupportPackage objects.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSupportPackage> getSupportPackageById(@PathVariable("tenantId") Long tenantId,
                                                                    @PathVariable("rolloutId") @TraceableField Long rolloutId,
                                                                    @PathVariable("type") String type,
                                                                    @PathVariable("packageId") @TraceableField Long packageId) {
        LOG.debug("Received request to get support package by ID");
        MgmtSupportPackage response;
        // check if the rollout exists
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        if (ESP.equalsIgnoreCase(type)) {
            List<Esp> espSupportPackages = supportPackageManagement.getESPSupportPackageById(rollout.getId(), packageId);
            response = MgmtRolloutMapper.toResponseEspRolloutPkgId(espSupportPackages);
        } else if (RSP.equalsIgnoreCase(type)) {
            List<Rsp> rspSupportPackages = supportPackageManagement.getRSPSupportPackageById(rollout.getId(), packageId);
            response = MgmtRolloutMapper.toResponseRspRolloutPkgId(rspSupportPackages);
        } else {
            throw new IllegalArgumentException("Incorrect type: " + type + ". Valid types are 'ESP' and 'RSP'.");
        }
        addDownloadLinks(rolloutId, response);
        return ResponseEntity.ok().body(response);
    }

    /**
     * Adds download links to the given support package based on the rollout ID and file type.
     * <p>
     * This method generates URLs for downloading the support package from the CDN and HTTP endpoints.
     * It uses the artifact URL handler properties and tenant-aware information to construct the links.
     *
     * @param rolloutId      The ID of the rollout associated with the support package.
     * @param supportPackage The support package to which the download links will be added.
     */
    private void addDownloadLinks(Long rolloutId, MgmtSupportPackage supportPackage) {
        String cdnDirectory = artifactUrlHandlerProperties.getCdn().getDirectory()
                .replace("{tenant}", tenantAware.getCurrentTenant())
                .replace("{type}", getPackageTypeByFileType(supportPackage.getFileType()))
                .replace("{SHA256}", supportPackage.getSha_256().replaceAll("(.{2})", "$1/"));

        String downloadUrl = artifactUrlHandlerProperties.getProtocols().get("download-cdn-http").getRef()
                .replace("{hawkbit.artifact.url.cdn.host}", cdnHost)
                .replace("{hawkbit.artifact.url.cdn.rootDirectory}", cdnRootDirectory)
                .replace("{artifactFileName}", supportPackage.getFilename())
                .replace("{hawkbit.artifact.url.cdn.directory}", cdnDirectory);

        // TODO: Hardcode should be removed once the support-packages download API is implemented
        String downloadHttpUrl = "https://mgmt-api.host.com/management/v1/rollouts/" + rolloutId +
                "/support-packages/" + supportPackage.getSupportPackageId() + "/download";

        supportPackage.add(Link.of(downloadUrl).withRel(MgmtRestConstants.DOWNLOAD));
        supportPackage.add(Link.of(downloadHttpUrl).withRel(MgmtRestConstants.DOWNLOAD_HTTP));
    }


    /**
     * Downloads a specific support package for a given tenant and rollout.
     *
     * @param tenantId  The tenant ID.
     * @param rolloutId The rollout ID.
     * @param packageId The support package ID.
     * @param type      The type of the support package (ESP/RSP).
     * @return A ResponseEntity containing the support package file for download.
     */

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> downloadSupportPackage(@PathVariable Long tenantId, @PathVariable @TraceableField Long rolloutId, @PathVariable @TraceableField Long packageId,
                                                       @RequestParam String type) {

        LOG.debug("Received request to download support package");
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        if (ESP.equalsIgnoreCase(type)) {
            LOG.debug("Fetching ESP support package for rolloutId={}, packageId={}", rolloutId, packageId);
            List<Esp> espSupportPackages = supportPackageManagement.getESPSupportPackageById(rollout.getId(), packageId);
            return generateResponseForEspSupportPackage(espSupportPackages, rolloutId, packageId);

        } else if (RSP.equalsIgnoreCase(type)) {
            LOG.debug("Fetching RSP support package for rolloutId={}, packageId={}", rolloutId, packageId);
            List<Rsp> rspSupportPackages = supportPackageManagement.getRSPSupportPackageById(rollout.getId(), packageId);
            return generateResponseForRspSupportPackage(rspSupportPackages, rolloutId, packageId);

        } else {
            LOG.error("Invalid type: {}. Valid types are 'ESP' and 'RSP'.", type);
            throw new IllegalArgumentException("Incorrect type: " + type + ". Valid types are 'ESP' and 'RSP'.");
        }

    }

    /**
     * Generates a response for ESP support package.
     *
     * @param espSupportPackages The ESP support packages.
     * @param rolloutId          The rollout ID.
     * @param packageId          The package ID.
     * @return A ResponseEntity containing the support package file for download.
     */
    private ResponseEntity<Void> generateResponseForEspSupportPackage(List<Esp> espSupportPackages, Long rolloutId, Long packageId) {
        if (espSupportPackages == null || espSupportPackages.isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("ESP Support Packages not found for the rollout ID: %d and package ID: %d", rolloutId, packageId));
        }
        Esp espPackage = espSupportPackages.get(0);
        return generateS3Response(espPackage.getTenant(), espPackage.getSha256Hash(), espPackage.getFileName(), ESP);
    }

    /**
     * Generates a response for RSP support package.
     *
     * @param rspSupportPackages The RSP support packages.
     * @param rolloutId          The rollout ID.
     * @param packageId          The package ID.
     * @return A ResponseEntity containing the support package file for download.
     */
    private ResponseEntity<Void> generateResponseForRspSupportPackage(List<Rsp> rspSupportPackages, Long rolloutId, Long packageId) {
        if (rspSupportPackages == null || rspSupportPackages.isEmpty()) {
            throw new EntityNotFoundException(
                    String.format("RSP Support Packages not found for the rollout ID: %d and package ID: %d", rolloutId, packageId));
        }
        Rsp rspPackage = rspSupportPackages.get(0);
        return generateS3Response(rspPackage.getTenant(), rspPackage.getSha256Hash(), rspPackage.getFileName(), RSP);

    }


    /**
     * Generates a response for S3 support package.
     *
     * @param tenant   The tenant.
     * @param checksum The checksum.
     * @param fileName The file name.
     * @return A ResponseEntity containing the support package file for download.
     */
    private ResponseEntity<Void> generateS3Response(String tenant, String checksum, String fileName, String fileType) {
        LOG.debug("Generating S3 response for tenant={}, checksum={}, fileName={}", tenant, checksum, fileName);
        String s3ObjName = s3Service.buildS3SupportPkgObjectName(tenant, checksum, fileName, fileType);
        Long preSignedUrlExpiryTime = supportPackageFileSystemProperties.getS3bucket().getPreSignedUrlExpiryTime();
        URL url = s3Service.generatePresignedUrl(bucketName, s3ObjName, preSignedUrlExpiryTime);

        if (s3Service.isValidGetUrl(url)) {
            LOG.info("Generated pre-signed URL for S3 object");
            HttpHeaders headers = new HttpHeaders();
            headers.add(LOCATION_HEADER, url.toString());
            return new ResponseEntity<>(headers, HttpStatus.valueOf(302));
        } else {
            LOG.error("Failed to generate pre-signed URL for S3 object: {}", s3ObjName);
            throw new EntityNotFoundException("SupportPkg Not Available on Server");
        }
    }

    private String getPackageTypeByFileType(String fileType) {
        return MgmtSupportPackageFileType.valueOf(fileType).getCategory();
    }

    /**
     * Handles the error conversion exception for the {@link MgmtSupportPackageFileType}.
     *
     * @param fileType string representation of the file type
     * @return ResponseEntity with the error message
     */
    @SuppressWarnings({"unchecked", "deprecation"})
    private ResponseEntity<MgmtSupportPackage> handleEnumConversionException(String fileType) {

        HttpMessageNotReadableException httpMessageNotReadableException = new HttpMessageNotReadableException("", new InvalidFormatException(null, "", fileType, MgmtSupportPackageFileType.class));
        return (ResponseEntity<MgmtSupportPackage>) new ResponseExceptionHandler()
                .handleInvalidFormatExceptionWithFieldName((InvalidFormatException) httpMessageNotReadableException.getCause(), httpMessageNotReadableException, "fileType");
    }

}
