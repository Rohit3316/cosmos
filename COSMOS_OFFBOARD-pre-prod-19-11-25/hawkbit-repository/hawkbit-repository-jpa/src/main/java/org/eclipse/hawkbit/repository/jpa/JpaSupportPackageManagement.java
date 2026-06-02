package org.eclipse.hawkbit.repository.jpa;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.ddi.DdiSignatureType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.EspFileTypeForDevices;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.cosmos.sns.services.SnsServiceType;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageFields;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp_;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.jpa.utils.SupportPackageManagementUtil;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import jakarta.validation.ValidationException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import static software.amazon.awssdk.utils.BinaryUtils.toHex;

@Service
@Slf4j
public class JpaSupportPackageManagement implements SupportPackageManagement {

    private static final String ESP = "ESP";
    private final EspEcuRolloutRepository espEcuRolloutRepository;
    private final RspRolloutRepository rspRolloutRepository;
    private final EspRepository espRepository;
    private final RolloutManagement rolloutManagement;
    private final RspRepository rspRepository;
    private final TenantAware tenantAware;
    private final ISnsServiceFactory snsServiceFactory;
    private final EntityManager entityManager;
    private final S3MultipartFileUpload s3MultipartFileUpload;
    private final S3Repository s3Repository;
    private final SupportPackageUrlHandlerProperties supportUrlHandlerProperties;
    private final RolloutTargetGroupRepository rolloutTargetGroupRepository;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final TargetRepository targetRepository;
    private final TargetManagement targetManagement;
    private final DdiSignatureService ddiSignatureService;
    private final PKIManagement pkiManagement;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    @Value("${cosmos.server.s3.support-package.bucket.name}")
    private String supportPackageBucketName;


    public JpaSupportPackageManagement(EspEcuRolloutRepository espEcuRolloutRepository,
                                       RspRolloutRepository rspRolloutRepository,
                                       EspRepository espRepository,
                                       RolloutManagement rolloutManagement, RspRepository rspRepository,
                                       TenantAware tenantAware, ISnsServiceFactory snsServiceFactory,
                                       EntityManager entityManager, S3MultipartFileUpload s3MultipartFileUpload,
                                       S3Repository s3Repository, SupportPackageUrlHandlerProperties supportUrlHandlerProperties,
                                       RolloutTargetGroupRepository rolloutTargetGroupRepository, RolloutGroupRepository rolloutGroupRepository,
                                       TargetRepository targetRepository, TargetManagement targetManagement,
                                       DdiSignatureService ddiSignatureService, PKIManagement pkiManagement,
                                       VirtualPropertyReplacer virtualPropertyReplacer, Database database) {
        this.espEcuRolloutRepository = espEcuRolloutRepository;
        this.rspRolloutRepository = rspRolloutRepository;
        this.espRepository = espRepository;
        this.rolloutManagement = rolloutManagement;
        this.rspRepository = rspRepository;
        this.tenantAware = tenantAware;
        this.snsServiceFactory = snsServiceFactory;
        this.entityManager = entityManager;
        this.s3MultipartFileUpload = s3MultipartFileUpload;
        this.s3Repository = s3Repository;
        this.supportUrlHandlerProperties = supportUrlHandlerProperties;
        this.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.targetRepository = targetRepository;
        this.targetManagement = targetManagement;
        this.ddiSignatureService = ddiSignatureService;
        this.pkiManagement = pkiManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    private void publishS3FileTransferRequest(S3FileTransferRequest s3FileTransferRequest) {
        try {
            PublishResponse message = snsServiceFactory.getInstance(SnsServiceType.S3_FILE_TRANSFER).publishMessage(s3FileTransferRequest).join();
            log.debug("Transfer request published and message Id: {}", message.messageId());
        } catch (CompletionException e) {
            log.error("Unable to send  s3FileTransferRequest message{} to SNS with reason:{}", s3FileTransferRequest.toString(), e.getMessage());
        }
    }

    private void updateSupportPackageStatusAndMD5(MgmtBaseSupportPackageCreateRequest packageCreateRequest, MessageDigest md5Digest) {
        if (packageCreateRequest.getFileType().getCategory().equals(ESP)) {
            espRepository.findBySha256HashIgnoreCase(packageCreateRequest.getSha256())
                    .ifPresent(e -> {
                        JpaEsp jpaEsp = (JpaEsp) e;
                        jpaEsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
                        jpaEsp.setMd5Hash(toHex(md5Digest.digest()));
                        espRepository.save(jpaEsp);
                    });
        } else {
            rspRepository.findBySha256HashIgnoreCase(packageCreateRequest.getSha256())
                    .ifPresent(r -> {
                        JpaRsp jpaRsp = (JpaRsp) r;
                        jpaRsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
                        jpaRsp.setMd5Hash(toHex(md5Digest.digest()));
                        rspRepository.save(jpaRsp);
                    });
        }
    }

    private void updateSupportPackage(MgmtBaseSupportPackageCreateRequest packageCreateRequest) {
        if (packageCreateRequest.getFileType().getCategory().equals(ESP)) {
            espRepository.findBySha256HashIgnoreCase(packageCreateRequest.getSha256())
                    .ifPresent(e -> {
                        JpaEsp jpaEsp = (JpaEsp) e;
                        jpaEsp.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.toString());
                        espRepository.save(jpaEsp);
                    });
        } else {
            rspRepository.findBySha256HashIgnoreCase(packageCreateRequest.getSha256())
                    .ifPresent(r -> {
                        JpaRsp jpaRsp = (JpaRsp) r;
                        jpaRsp.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.toString());
                        rspRepository.save(jpaRsp);
                    });
        }
    }

    private S3FileTransferRequest createS3DownloadRequest(MgmtFileUrlSupportPackageCreateRequest packageCreateRequest, Long tenantId) {
        Long fileId = getFileId(packageCreateRequest);
        String fileType = packageCreateRequest.getFileType().getCategory();

        return S3FileTransferRequest.builder()
                .fileId(fileId)
                .fileName(S3FileUtil.generateS3KeyPath(getStorageDirectoryPlaceHolder(), tenantAware.getCurrentTenant(), packageCreateRequest.getSha256(), fileType, SupportPackageManagementUtil.sanitizeFileName(packageCreateRequest.getFileName())))
                .fileURL(packageCreateRequest.getFileUrl())
                .bucketName(supportPackageBucketName)
                .checksum(packageCreateRequest.getSha256())
                .fileType(fileType)
                .tenantId(tenantId)
                .build();
    }

    private Long getFileId(MgmtBaseSupportPackageCreateRequest packageCreateRequest) {
        String sha256 = packageCreateRequest.getSha256();
        if (ESP.equals(packageCreateRequest.getFileType().getCategory())) {
            log.info("all esp:{}", espRepository.findAll());
            return espRepository.findBySha256HashIgnoreCase(sha256)
                    .map(Esp::getId)
                    .orElse(null);
        } else {
            return rspRepository.findBySha256HashIgnoreCase(sha256)
                    .map(Rsp::getId)
                    .orElse(null);
        }
    }

    private Rollout getRollout(Long rolloutId) {
        return rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(String.valueOf(rolloutId)));
    }

    /**
     * handles the creation, association or replacement of ESP support package
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Esp handleEspSupportPackage(Esp esp, MgmtBaseSupportPackageCreateRequest supportPackageCreateRequest, Long rolloutId, Long tenantId) {
        Rollout rollout = getRollout(rolloutId);
        Optional<Esp> existingEsp = getEspBySha256(esp.getSha256Hash());
        List<String> controllerIds = Optional.ofNullable(esp.getEspEcuRollouts())
                .orElse(Collections.emptyList())
                .stream()
                .map(EspEcuRollout::getControllerId)
                .toList();
        String ecuNodeAddress = esp.getEspEcuRollouts().isEmpty() ? null : esp.getEspEcuRollouts().get(0).getEcuNodeAddress();
        if (existingEsp.isPresent()) {

            if (!esp.getFileType().equals(existingEsp.get().getFileType())) {
                throw new EntityAlreadyExistsException("The given entity already exists in database");
            }

            log.debug("SHA256 already exists, associating the existing ESP file with ID: {} and rollout ID: {}",
                    existingEsp.get().getId(), rolloutId);
            return associateEspEcuRolloutsWithExistingEsp((JpaEsp) existingEsp.get(), rollout, controllerIds, ecuNodeAddress);
        } else {
            Esp newEsp = createOrReplaceEspSupportPackage((JpaEsp) esp, rollout);
            entityManager.flush();
            log.debug("Support package created, initiating the File download: {}", esp);

            uploadSupportPackageAndGenerateSignature(newEsp.getId(), supportPackageCreateRequest, tenantId, null);
            return newEsp;
        }

    }

    /**
     * Get esp for given sha265 hash
     */
    private Optional<Esp> getEspBySha256(String sha256) {
        return espRepository.findBySha256HashIgnoreCase(sha256);
    }

    /**
     * Associate EspEcuRollouts With Existing Esp
     */
    private Esp associateEspEcuRolloutsWithExistingEsp(JpaEsp esp, Rollout rollout, List<String> controllerIds, String ecuNodeAddress) {
        Set<String> existingKeys = espEcuRolloutRepository
                .findByRolloutIdAndSupportPackageIdAndEcuNodeAddressAndControllerIdIn(
                        rollout.getId(), esp.getId(), ecuNodeAddress, controllerIds)
                .stream()
                .map(r -> r.getEcuNodeAddress() + r.getControllerId())
                .collect(Collectors.toSet());

        List<JpaEspEcuRollout> newEspEcuRollouts = createEspEcuRollouts(esp, controllerIds, ecuNodeAddress, rollout)
                .stream()
                .filter(r -> !existingKeys.contains(r.getEcuNodeAddress() + r.getControllerId()))
                .toList();


        if (newEspEcuRollouts.isEmpty()) {
            throw new EntityAlreadyExistsException("The given entity already exists in database");
        }
        return espEcuRolloutRepository.saveAll(newEspEcuRollouts).get(0).getSupportPackage();
    }

    /**
     * Validates that the provided controller IDs are not already associated with the given rollout
     * using a different file name for the same ESP file type.
     *
     * @param packageRequest The request containing the support package details and controller IDs.
     * @param rollout        The rollout entity to check against.
     * @throws EntityNotFoundException if one or more controller IDs are already linked to the same file type with a different file name.
     */
    public void checkForESPSupportingPackageType(Esp esp, Rollout rollout) {

        List<String> controllerIds = Optional.ofNullable(esp.getEspEcuRollouts())
                .orElse(Collections.emptyList())
                .stream()
                .map(EspEcuRollout::getControllerId)
                .toList();

        List<JpaEspEcuRollout> existingAssociations = espEcuRolloutRepository
                .findWithRolloutAndSupportPackageByRolloutIdAndControllerIdIn(rollout.getId(), controllerIds);

        Set<String> conflictingControllerIds = existingAssociations.stream()
                .filter(e -> e.getSupportPackage().getFileType() == esp.getFileType() &&
                        !e.getSupportPackage().getFileName().equals(esp.getFileName()))
                .map(JpaEspEcuRollout::getControllerId)
                .collect(Collectors.toSet());

        if (!conflictingControllerIds.isEmpty()) {
            throw new ValidationException(String.format(
                    "One or more ControllerIDs are already associated with rollout %s with different ESP file of the same file type %s",
                    rollout.getId(), esp.getFileType()));
        }
    }
    /**
     * Counts the total number of ESP and RSP entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to count ESP and RSP entities for.
     * @return The total count of ESP and RSP entities associated with the given rollout ID.
     */
    @Override
    public long countSupportPackages(long rolloutId) {
        long espCount = espRepository.countByEspEcuRolloutsRolloutId(rolloutId);
        long rspCount = rspRepository.countByRspRolloutsRolloutId(rolloutId);
        log.debug("Count for rollout ID {}: ESP count = {}, RSP count = {}", rolloutId, espCount, rspCount);
        return espCount + rspCount;
    }


    /**
     * Create EspEcuRollouts
     */
    private List<JpaEspEcuRollout> createEspEcuRollouts(Esp esp, List<String> controllerIds, String ecuNodeAddress, Rollout rollout) {
        return SupportPackageManagementUtil.toJpaEspEcuRollout(rollout, controllerIds, ecuNodeAddress, (JpaEsp) esp);
    }

    /**
     * create or replace esp based on the condition if sha256 is different
     */
    private Esp createOrReplaceEspSupportPackage(JpaEsp esp, Rollout rollout) {
        Optional<List<Esp>> espListWithDifferentSha256 = getEspWithDifferentSha256(esp,rollout);
        if (espListWithDifferentSha256.isPresent() && !espListWithDifferentSha256.get().isEmpty()) {
            return replaceEsp(espListWithDifferentSha256.get(),esp);
        } else {
            return createNewEspSupportPackage(esp, rollout);
        }
    }

    /**
     * create new esp support package
     */
    private Esp createNewEspSupportPackage(JpaEsp esp, Rollout rollout) {
        // esp already has espEcuRollouts properly set by mapper
        return espRepository.save(esp);
    }

    /**
     * Finds distinct Esp entities based on the provided criteria of filetype, ecuNodeAddress, controllerIDs and sha256.
     *
     * @return An Optional containing a list of distinct Esp entities that match the provided criteria, or an empty Optional if no entities were found.
     */
    private Optional<List<Esp>> getEspWithDifferentSha256(JpaEsp esp,Rollout rollout) {
        List<String> controllerIds = esp.getEspEcuRollouts().stream()
                .map(EspEcuRollout::getControllerId)
                .toList();
        return espRepository.findDistinctByFileTypeAndEspEcuRolloutsEcuNodeAddressAndEspEcuRolloutsRolloutAndEspEcuRolloutsControllerIdInAndSha256HashNotLike(
                esp.getFileType(),esp.getEspEcuRollouts().get(0).getEcuNodeAddress(),rollout,controllerIds,esp.getSha256Hash());

    }

    /**
     * Finds all Esp entities associated with a given rollout ID and support package ID.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @return A list of Esp entities associated with the given rollout ID and support package ID.
     */
    @Override
    public List<Esp> getESPSupportPackages(Long rolloutId) {
        return Collections.unmodifiableList(espRepository.findESPSupportPackagesByRolloutId(rolloutId));
    }

    /**
     * Retrieves a paginated slice of ESP support packages associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to retrieve ESP support packages for.
     * @param pageable  The pagination and sorting information.
     * @return A {@link Slice} containing the ESP support packages for the specified rollout ID.
     */
    @Override
    public Slice<Esp> getESPSupportPackages(Long rolloutId, Pageable pageable) {
        return espRepository.findESPSupportPackagesByRolloutId(rolloutId, pageable);
    }

    /**
     * Finds a paginated slice of RSP support packages for a specific rollout
     * based on the provided RSQL query.
     *
     * @param rsqlParam The RSQL query string to filter the RSP support packages.
     * @param rolloutId The ID of the rollout to retrieve RSP support packages for.
     * @param pageable  The pagination and sorting information.
     * @return A {@link Slice} containing the filtered RSP support packages.
     */
    @Override
    public Slice<Rsp> findRspsByRolloutRsql(String rsqlParam, Long rolloutId, Pageable pageable) {
        final List<Specification<JpaRsp>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, SupportPackageFields.class, virtualPropertyReplacer, database),
                byRspRolloutIdSpec(rolloutId));

        return JpaManagementHelper.findAllWithCountBySpec(rspRepository, pageable, specList);
    }

    /**
     * Finds a paginated slice of ESP support packages for a specific rollout
     * based on the provided RSQL query.
     *
     * @param rsqlParam The RSQL query string to filter the ESP support packages.
     * @param rolloutId The ID of the rollout to retrieve ESP support packages for.
     * @param pageable  The pagination and sorting information.
     * @return A {@link Slice} containing the filtered ESP support packages.
     */
    @Override
    public Slice<Esp> findEspsByRolloutRsql(String rsqlParam, Long rolloutId, Pageable pageable) {
        final List<Specification<JpaEsp>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, SupportPackageFields.class, virtualPropertyReplacer, database),
                byEspRolloutIdSpec(rolloutId));

        return JpaManagementHelper.findAllWithCountBySpec(espRepository, pageable, specList);
    }

    /**
     * Builds a JPA specification to filter RSP entities by rollout ID.
     *
     * @param rolloutId The ID of the rollout to filter RSP entities for.
     * @return A {@link Specification} to filter RSP entities by rollout ID.
     */
    private Specification<JpaRsp> byRspRolloutIdSpec(final Long rolloutId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<JpaRspRollout> subRoot = subquery.from(JpaRspRollout.class);

            subquery.select(subRoot.get(JpaRspRollout_.SUPPORT_PACKAGE).get("id"))
                    .where(cb.equal(subRoot.get(JpaRspRollout_.ROLLOUT).get("id"), rolloutId));

            return root.get(JpaRsp_.ID).in(subquery);
        };
    }

    /**
     * Builds a JPA specification to filter ESP entities by rollout ID.
     *
     * @param rolloutId The ID of the rollout to filter ESP entities for.
     * @return A {@link Specification} to filter ESP entities by rollout ID.
     */
    private Specification<JpaEsp> byEspRolloutIdSpec(final Long rolloutId) {
        return (root, query, cb) -> {
            Subquery<Long> subquery = query.subquery(Long.class);
            Root<JpaEspEcuRollout> subRoot = subquery.from(JpaEspEcuRollout.class);

            subquery.select(subRoot.get(JpaEspEcuRollout_.SUPPORT_PACKAGE).get("id"))
                    .where(cb.equal(subRoot.get(JpaEspEcuRollout_.ROLLOUT).get("id"), rolloutId));

            return root.get(JpaEsp_.ID).in(subquery);
        };
    }


    /**
     * Finds all Esp entities associated with a given rollout ID and support package ID.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @return A list of Esp entities associated with the given rollout ID and support package ID.
     */
    @Override
    public List<Esp> getESPSupportPackages(Long rolloutId, List<String> controllerIds) {
        return Collections.unmodifiableList(espRepository.findEspSupportPackagesByRolloutIdAndControllerIdsIn(rolloutId, controllerIds));
    }


    /**
     * Finds all Esp entities associated with a given rollout ID and support package ID.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @param packageId The ID of the support package to find Esp entities for.
     * @return A list of Esp entities associated with the given rollout ID and support package ID.
     */
    @Override
    public List<Esp> getESPSupportPackageById(Long rolloutId, Long packageId) {
        if (espRepository.findESPSupportPackagesByPackageId(rolloutId, packageId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Support package retrieval failed. Please check the rollout ID: %d and package ID: %d", rolloutId, packageId));
        }
        return Collections.unmodifiableList(espRepository.findESPSupportPackagesByPackageId(rolloutId, packageId));
    }

    /**
     * Retrieves all RSP support packages for a specific rollout.
     *
     * @param rolloutId The ID of the rollout.
     * @return A list of {@link Rsp} objects associated with the given rollout ID.
     */
    @Override
    public List<Rsp> getRSPSupportPackages(Long rolloutId) {
        return Collections.unmodifiableList(rspRepository.findRSPSupportPackagesByRolloutId(rolloutId));
    }

    /**
     * Retrieves a paginated slice of RSP support packages associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to retrieve RSP support packages for.
     * @param pageable  The pagination and sorting information.
     * @return A {@link Slice} containing the RSP support packages for the specified rollout ID.
     */
    @Override
    public Slice<Rsp> getRSPSupportPackages(Long rolloutId, Pageable pageable) {
        return rspRepository.findRSPSupportPackagesByRolloutId(rolloutId, pageable);
    }

    /**
     * Retrieves all RSP support packages for a specific rollout and support package ID.
     *
     * @param rolloutId The ID of the rollout.
     * @param packageId The ID of the support package.
     * @return A list of {@link Rsp} objects associated with the given rollout ID and package ID.
     */
    @Override
    public List<Rsp> getRSPSupportPackageById(Long rolloutId, Long packageId) {
        if (rspRepository.findRSPSupportPackagesByPackageId(rolloutId, packageId).isEmpty()) {
            throw new EntityNotFoundException(String.format("Support package retrieval failed. Please check the rollout ID: %d and package ID: %d", rolloutId, packageId));
        }
        return Collections.unmodifiableList(rspRepository.findRSPSupportPackagesByPackageId(rolloutId, packageId));
    }

    /**
     * Deletes all {@link JpaEspEcuRollout} entities associated with the given rollout ID and support package IDs.
     */
    @Override
    @Transactional
    public void unLinkEspEcuRollout(Long rolloutId, List<Long> supportPackageIds) {
        log.debug("Unlinking ESP for rollout ID: {}", rolloutId);
        espEcuRolloutRepository.deleteByRolloutIdAndSupportPackageIdIn(rolloutId, supportPackageIds);
    }

    @Override
    @Transactional
    public void deleteEsp(List<Esp> esp) {
        Set<String> sha256Duplicates = new HashSet<>();
        List<JpaEsp> deleteEsp = esp.stream().filter(e -> espEcuRolloutRepository.countBySupportPackageId(e.getId()) == 0)
                .map(e -> {
                    if (!sha256Duplicates.contains(e.getSha256Hash() + e.getFileName())) {
                        FileRemovalServiceFactory.getInstance(FileType.ESP).removeFileFromStorage(e.getId());
                        sha256Duplicates.add(e.getSha256Hash() + e.getFileName());
                    }
                    return (JpaEsp) e;
                }).toList();
        espRepository.deleteAll(deleteEsp);
    }

    /**
     * Deletes all {@link JpaRspRollout} entities associated with the given rollout ID and support package IDs.
     */
    @Override
    @Transactional
    public void unLinkRspRollout(Long rolloutId, List<Long> supportPackageIds) {
        log.debug("Unlinking RSP for rollout ID: {}", rolloutId);
        rspRolloutRepository.deleteByRolloutIdAndSupportPackageIdIn(rolloutId, supportPackageIds);
    }

    @Override
    @Transactional
    public void deleteRsp(List<Rsp> rsp) {
        Set<String> sha256Duplicates = new HashSet<>();
        List<JpaRsp> deleteRsp = rsp.stream().filter(r -> rspRolloutRepository.countBySupportPackageId(r.getId()) == 0)
                .map(r -> {
                    if (!sha256Duplicates.contains(r.getSha256Hash() + r.getFileName())) {
                        FileRemovalServiceFactory.getInstance(FileType.RSP).removeFileFromStorage(r.getId());
                        sha256Duplicates.add(r.getSha256Hash() + r.getFileName());
                    }
                    return (JpaRsp) r;
                }).toList();
        rspRepository.deleteAll(deleteRsp);
    }

    @Override
    @Transactional
    public void deleteSupportPackage(final Long rolloutId, final List<Esp> esp, final List<Rsp> rsp) {
        if (esp.isEmpty() && rsp.isEmpty()) {
            throw new EntityNotFoundException(String.format("Support Packages not found for the rollout ID: %d", rolloutId));
        }
        if (!esp.isEmpty()) {
            List<Long> espIds = esp.stream().map(Esp::getId).toList();
            unLinkEspEcuRollout(rolloutId, espIds);
            deleteEsp(esp);
            log.debug("Unlink ESP support packages from the rollout: {}", rolloutId);
        }
        if (!rsp.isEmpty()) {
            List<Long> rspIds = rsp.stream().map(Rsp::getId).toList();
            unLinkRspRollout(rolloutId, rspIds);
            deleteRsp(rsp);
            log.debug("Unlink RSP support packages from the rollout: {}", rolloutId);
        }
    }

    @Override
    public void initiateFileDownloadToS3(MgmtFileUrlSupportPackageCreateRequest packageCreateRequest, Long tenantId) {
        log.debug("Initiating file download to S3 for support package: {}", packageCreateRequest);
        S3FileTransferRequest s3FileTransferRequest = createS3DownloadRequest(packageCreateRequest, tenantId);
        log.debug("Publishing file transfer request: {}", s3FileTransferRequest);
        publishS3FileTransferRequest(s3FileTransferRequest);
        log.debug("Updating support package file status to uploading to storage status");
        updateSupportPackage(packageCreateRequest);
    }

    /**
     * Initiates the direct file upload to S3 for a multipart file.
     * Updates the file status and MD5 hash.
     *
     * @param fileRequest The request object containing the file information.
     */
    private void initiateMultipartFileUpload(MgmtFileSupportPackageCreateRequest fileRequest) {
        S3FileUpload fileUpload = getS3FileUploadEntity(fileRequest.getSha256(), SupportPackageManagementUtil.sanitizeFileName(fileRequest.getFileName()),
                fileRequest.getFileType().getCategory());

        try (final InputStream inputStream = fileRequest.getFile().getInputStream()) {
            final MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            InputStream md5DigestStream = new DigestInputStream(inputStream, md5Digest);
            final MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            InputStream sha256DigestStream = new DigestInputStream(md5DigestStream, sha256Digest);

            S3FileUtil.initiateFileUploadToS3(
                    sha256DigestStream,
                    sha256Digest,
                    fileUpload,
                    fileRequest.getSha256(),
                    log::error,
                    s3Repository::deleteFileFromS3,
                    (fileUpload1, inputStream1) -> {
                        try {
                            s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload1, inputStream1);
                        } catch (Exception e) {
                            throw new S3Exception("An error occurred while uploading file to S3", e);
                        }
                    }
            );

            updateSupportPackageStatusAndMD5(fileRequest, md5Digest);
        } catch (Exception e) {
            log.debug("Error during file upload to s3: {}", e.getMessage());
            throw new S3Exception("File upload failed", e);
        }

    }

    /**
     * Build file upload object for s3.
     *
     * @param sha256           - of the provided file
     * @param filename         - name of the file
     * @param fileTypeCategory - category of the fileType- ESP/RSP
     * @return {@link S3FileUpload}
     */
    private S3FileUpload getS3FileUploadEntity(String sha256, String filename, String fileTypeCategory) {
        try {

            return S3FileUpload.builder()
                    .bucketName(supportPackageBucketName)
                    .filename(filename)
                    .keyPath(S3FileUtil.generateS3KeyPath(getStorageDirectoryPlaceHolder(), tenantAware.getCurrentTenant(), sha256, fileTypeCategory))
                    .build();

        } catch (Exception e) {
            throw new GenericSpServerException("Error while uploading support packages to storage", e);
        }
    }


    @Override
    public List<Esp> getEspByRolloutIdAndControllerId(String controllerId, Long rolloutId) {
        return espRepository.findEspByRolloutIdAndControllerId(rolloutId, controllerId);

    }

    /**
     * replace esp with different sha256
     */
    private Esp replaceEsp(List<Esp> espListWithDifferentSha256, JpaEsp newEsp) {
        List<JpaEsp> jpaEspList = espListWithDifferentSha256.stream().map(espWithDifferentSha256 -> SupportPackageManagementUtil.toJpaEsp(newEsp, espWithDifferentSha256)).toList();
        return espRepository.saveAll(jpaEspList).get(0);
    }

    /**
     * handles the creation, association or replacement of RSP support package
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rsp handleRspSupportPackage(Rsp rsp, MgmtBaseSupportPackageCreateRequest mgmtSupportPackageCreateRequest, Long rolloutId, Long tenantId) {
        Rollout rollout = getRollout(rolloutId);
        checkForRspSupportingPackageType((JpaRsp) rsp, rolloutId);
        Optional<Rsp> existingRsp = getRspBySha256(rsp.getSha256Hash());
        if (existingRsp.isPresent()) {
            if (!rsp.getFileType().equals(existingRsp.get().getFileType())) {
                throw new EntityAlreadyExistsException("The given entity already exists in database");
            }
            log.debug("SHA256 already exists, associating the existing RSP file with ID: {} and rollout ID: {}",
                    existingRsp.get().getId(), rolloutId);
            return associateRolloutWithRsp(existingRsp.get(), rollout);
        } else {
            Rsp newRsp = createOrReplaceRsp((JpaRsp) rsp, rollout);
            entityManager.flush();
            log.debug("Support package created, initiating the File download: {}", rsp);

            uploadSupportPackageAndGenerateSignature(newRsp.getId(), mgmtSupportPackageCreateRequest, tenantId, null);
            return newRsp;
        }
    }

    /**
     * get RSP by given sha256
     */
    private Optional<Rsp> getRspBySha256(String sha256) {
        return rspRepository.findBySha256HashIgnoreCase(sha256);
    }

    /**
     * Validates that the provided package file type is not already associated with the given rollout.
     *
     * @param packageCreateRequest The request containing the support package details.
     * @param rolloutId            The rollout entity to check against.
     * @throws ValidationException if rollout is already linked to the same file type with a different RSP file name.
     */
    private void checkForRspSupportingPackageType(JpaRsp rsp, Long rolloutId) {
        log.debug("Checking for RSP supporting package type: {} for the rolloutID: {}", rsp.getFileType(), rolloutId);
        if (rspRepository.countByRspRolloutsRolloutIdAndFileType(rolloutId, rsp.getFileType()) > 0) {
            throw new ValidationException(String.format("Rollout %s is already associated with RSP file of the file type %s",
                    rolloutId, rsp.getFileType()));
        }
    }

    /**
     * Associate Rollout With Rsp
     */
    private JpaRsp associateRolloutWithRsp(Rsp rsp, Rollout rollout) throws EntityAlreadyExistsException {
        JpaRspRollout rspRollout = createRspRollout(rsp, rollout);
        if (rspRolloutRepository.findByRolloutIdAndSupportPackageId(rollout.getId(), rsp.getId()) != null) {
            throw new EntityAlreadyExistsException("The given entity already exists in database");
        } else {
            return rspRolloutRepository.save(rspRollout).getSupportPackage();
        }
    }

    /**
     * Create RspRollout
     */
    private JpaRspRollout createRspRollout(Rsp rsp, Rollout rollout) {
        return SupportPackageManagementUtil.toJpaRspRollout(rollout, rsp);
    }

    /**
     * find distinct Rsp With Different Sha256 for same fileType
     */
    private Optional<List<Rsp>> getRspWithDifferentSha256(JpaRsp rsp, Rollout rollout) {
        return rspRepository.findListOfRspWithSha256HashNotLike(
                rsp.getFileType(), rollout, rsp.getSha256Hash());
    }

    /**
     * create Or Replace Rsp
     */
    private Rsp createOrReplaceRsp(JpaRsp rsp, Rollout rollout) {

        var rspListWithDifferentSha256 = getRspWithDifferentSha256(rsp, rollout);
        if (rspListWithDifferentSha256.isPresent() && !rspListWithDifferentSha256.get().isEmpty()) {
            return replaceRsp(rspListWithDifferentSha256.get(), rsp);
        } else {
            return createNewRsp(rsp, rollout);
        }
    }

    /**
     * create new rsp support package
     */
    private Rsp createNewRsp(JpaRsp rsp, Rollout rollout) {
        JpaRspRollout jpaRspRollout = createRspRollout(rsp, rollout);
        rsp.setRspRollouts(List.of(jpaRspRollout));
        return rspRepository.save(rsp);
    }


    /**
     * replace RSP
     */
    private Rsp replaceRsp(List<Rsp> rspList, JpaRsp newRsp) {

        List<JpaRsp> jpaRspList = rspList.stream()
                .map(rsp -> SupportPackageManagementUtil.toJpaRsp(newRsp, rsp))
                .toList();
        return rspRepository.saveAll(jpaRspList).get(0);
    }

    /**
     * Returns a list of ESP ECU Rollout entities by rollout ID and list of ECU node addresses.
     *
     * @param rolloutId      The ID of the rollout.
     * @param ecuNodeAddress The list of ECU node addresses.
     * @return A list of ESP ECU Rollout entities that match the provided criteria.
     */
    public List<EspEcuRollout> getByRolloutIdAndEcuNodeAddressList(Long rolloutId, Set<String> ecuNodeAddress) {
        return Collections.unmodifiableList(espEcuRolloutRepository.findByRolloutIdAndEcuNodeAddressIn(rolloutId, ecuNodeAddress));
    }

    @Override
    public void generateAndCacheSignaturesForSupportPackage(Long fileId, String sha256, MgmtSupportPackageFileType fileType, Rollout rollout) {
        List<SigningCertificateConfiguration> signingConfigurations = pkiManagement.getAllSigningCertificateConfigurations();
        ddiSignatureService.generateAndCacheSignatures(fileId, sha256, DdiSignatureType.valueOf(fileType.getCategory()), signingConfigurations, null);
    }

    /**
     * Handles file upload to S3 based on the input type of file ie, Multipart File or File Url
     *
     * @param packageCreateRequest File upload request object
     * @param tenantId             tenant id
     * @param supportPackageFileId support package file id
     */
    private void uploadSupportPackageAndGenerateSignature(Long supportPackageFileId, MgmtBaseSupportPackageCreateRequest packageCreateRequest, Long tenantId, Rollout rollout) {
        //Upload directly to s3 of multipart file is provided
        if (packageCreateRequest instanceof MgmtFileSupportPackageCreateRequest fileRequest) {
            initiateMultipartFileUpload(fileRequest);


            //Support package is successfully uploaded to s3, now generate and cache signatures with all the available certificate configurations
            generateAndCacheSignaturesForSupportPackage(supportPackageFileId, packageCreateRequest.getSha256(), packageCreateRequest.getFileType(), null);
        } else if (packageCreateRequest instanceof MgmtFileUrlSupportPackageCreateRequest fileUrlRequest) {
            initiateFileDownloadToS3(fileUrlRequest, tenantId);
        } else {
            throw new IllegalArgumentException("Unsupported support package upload type");
        }
    }

    /**
     * Returns the storage directory placeholder for S3 file upload.
     *
     * @return The storage directory placeholder for S3 file upload.
     */
    private String getStorageDirectoryPlaceHolder() {
        return supportUrlHandlerProperties.getEsp().getS3().getDirectory();
    }

    /**
     * Validates that the provided controller IDs are not already associated with the given rollout
     * using the same ECU node address and file type.
     * <p>
     * If a conflict is found, a {@link ValidationException} is thrown indicating that the device
     * of the specified file type already exists for the given rollout ID and controller IDs.
     *
     * @param rollout               The rollout entity to validate against.
     * @param supportPackageRequest The support package containing the controller IDs and file type.
     * @throws ValidationException If a controller ID is already associated with the same file type
     *                             for the given rollout ID and ECU node address.
     */
    @Override
    public void validateTargetsForRollout(Rollout rollout, Esp esp) {

        List<String> controllerIds = Optional.ofNullable(esp.getEspEcuRollouts())
                .orElse(Collections.emptyList())
                .stream()
                .map(EspEcuRollout::getControllerId)
                .toList();
        String ecuNodeAddress = esp.getEspEcuRollouts().isEmpty() ? null : esp.getEspEcuRollouts().get(0).getEcuNodeAddress();

        List<EspFileTypeForDevices> existingControllerIds = espEcuRolloutRepository.findControllerIdAndFileTypeByRolloutIdAndEcuNodeAddress(rollout.getId(), ecuNodeAddress);

        List<String> conflictingControllerIds = controllerIds.stream()
                .filter(controllerId -> existingControllerIds.stream()
                        .anyMatch(existing -> existing.controllerId().equals(controllerId) && existing.fileType().equals(esp.getFileType())))
                .collect(Collectors.toList());

        if (!conflictingControllerIds.isEmpty()) {
            log.error("Validation failed for Rollout ID: {}, Controller IDs: {}", rollout.getId(), conflictingControllerIds);
            throw new ValidationException("The device of this fileType already exists for the rollout Id: " + rollout.getId() + ", Controller IDs: " + conflictingControllerIds);
        }
    }
}