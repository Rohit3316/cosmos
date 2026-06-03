/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactReplacementRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.model.S3FileUpload;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.cosmos.sns.services.SnsServiceType;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.artifact.repository.model.ArtifactsHash;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.ArtifactsFields;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.VehicleFields;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.ArtifactsCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityCannotNullException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaArtifactsCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.mapper.MgmtArtifactSoftwareModuleAssociationMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.service.ArtifactsFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.CDNFileUtil;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.jpa.specifications.SoftwareModuleSpecification;
import org.eclipse.hawkbit.repository.jpa.utils.FileSizeHelper;
import org.eclipse.hawkbit.repository.jpa.utils.SupportPackageManagementUtil;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SourceTargetVersionPair;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.constants.ArtifactsAuditStatus;
import org.eclipse.hawkbit.repository.model.inventory.ArtifactsUpload;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.validation.ValidationException;
import jakarta.persistence.EntityManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.sql.Types.NULL;
import static java.time.ZoneOffset.UTC;
import static org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus.ACTIVE;
import static org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus.REPLACED;
import static org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.S3.Type.ARTIFACT;
import static software.amazon.awssdk.utils.BinaryUtils.toHex;

/**
 * JPA based {@link ArtifactsManagement} implementation.
 */
@Slf4j
@Validated
public class JpaArtifactsManagement implements ArtifactsManagement {
    public static final String FILE_SEPERATOR = "/";
    private static final String ARTIFACT_FILETYPE = "ARTIFACT";
    private static final String PURGED = "PURGED";
    private static final String INFO = "INFO";
    private static final String VERSION_ALREADY_EXISTS = "The combination of source version %s and target version %s already exists for a different artifact for module %s";
    private final ArtifactsRepository artifactsRepository;
    private final ArtifactModuleLinkRepository artifactModuleLinkRepository;
    private final TenantAware tenantAware;
    private final ArtifactFilesystemProperties artifactFilesystemProperties;
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    private final SoftwareModuleRepository softwareModuleRepository;
    private final SystemSecurityContext systemSecurityContext;
    private final SystemManagement systemManagement;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final ISnsServiceFactory snsServiceFactory;
    private final RolloutManagement rolloutManagement;
    private final ActionArtifactRepository actionArtifactRepository;
    private final KafkaMessageService kafkaMessageService;
    @Value("${cosmos.server.artifacts.s3.bucket.name}")
    private String bucketName;
    @Value("${cosmos.server.artifacts.purged.vins.batch.size:100}")
    private int purgedVinsBatchSize;
    private static final int FIRST_PAGE_NUMBER = 0;
    private final ArtifactsFileRemovalService artifactsFileRemovalService;
    @Value("${hawkbit.artifact.validate-file-size}")
    private boolean validateFileSize;
    private final S3Repository s3Repository;
    private final S3MultipartFileUpload s3MultipartFileUpload;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final VersionManagement versionManagement;
    private final EntityManager entityManager;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    JpaArtifactsManagement(ArtifactsRepository artifactsRepository, ArtifactModuleLinkRepository artifactModuleLinkRepository,
                           TenantAware tenantAware, ArtifactFilesystemProperties artifactFilesystemProperties,
                           ArtifactUrlHandlerProperties artifactUrlHandlerProperties, SoftwareModuleRepository softwareModuleRepository,
                           SystemSecurityContext systemSecurityContext, SystemManagement systemManagement,
                           TenantConfigurationManagement tenantConfigurationManagement,
                           ISnsServiceFactory snsServiceFactory, RolloutManagement rolloutManagement,
                           ActionArtifactRepository actionArtifactRepository, KafkaMessageService kafkaMessageService,
                           ArtifactsFileRemovalService artifactsFileRemovalService,
                           S3Repository s3Repository, S3MultipartFileUpload s3MultipartFileUpload, SoftwareModuleManagement softwareModuleManagement,
                           VersionManagement versionManagement, EntityManager entityManager, VirtualPropertyReplacer virtualPropertyReplacer,
                           Database database) {
        this.artifactsRepository = artifactsRepository;
        this.artifactModuleLinkRepository = artifactModuleLinkRepository;
        this.tenantAware = tenantAware;
        this.artifactFilesystemProperties = artifactFilesystemProperties;
        this.artifactUrlHandlerProperties = artifactUrlHandlerProperties;
        this.softwareModuleRepository = softwareModuleRepository;
        this.systemSecurityContext = systemSecurityContext;
        this.systemManagement = systemManagement;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.snsServiceFactory = snsServiceFactory;
        this.rolloutManagement = rolloutManagement;
        this.actionArtifactRepository = actionArtifactRepository;
        this.kafkaMessageService = kafkaMessageService;
        this.artifactsFileRemovalService = artifactsFileRemovalService;
        this.s3Repository = s3Repository;
        this.s3MultipartFileUpload = s3MultipartFileUpload;
        this.softwareModuleManagement = softwareModuleManagement;
        this.versionManagement = versionManagement;
        this.entityManager = entityManager;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    // Helper methods to compute hashes
    private static DigestInputStream wrapInDigestInputStream(final InputStream input, final MessageDigest mdMD5, final MessageDigest mdSHA256) {
        return new DigestInputStream(new DigestInputStream(input, mdSHA256), mdMD5);
    }

    /**
     * Creates a new {@link Artifacts} entity based on the provided {@link ArtifactsCreate} object.
     *
     * @param c the creation object containing the data for the new entity
     * @return the created {@link Artifacts} entity
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifacts create(final ArtifactsCreate c) {
        final JpaArtifactsCreate create = (JpaArtifactsCreate) c;
        return artifactsRepository.save(create.build());
    }

    /**
     * Deletes the {@link Artifacts} entity with the specified ID.
     *
     * @param id the ID of the entity to delete
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteArtifactsById(final Long id, final ArtifactsAuditStatus status) {

        //Delete artifact from CDN using lambda
        FileRemovalServiceFactory.getInstance(org.cosmos.models.sqs.FileType.ARTIFACT).removeFileFromCDN(id);
        JpaArtifacts artifact = artifactsRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, id));
        artifact.setArtifactStatus(status.name());
        artifactsRepository.save(artifact);
    }

    /**
     * Retrieves a list of {@link Artifacts} entities by their file name.
     *
     * @param fileName the file name to search for
     * @return an unmodifiable list of found {@link Artifacts} entities
     */
    @Override
    public List<Artifacts> findByFileName(final String fileName) {

        return Collections.unmodifiableList(artifactsRepository.findByFileName(fileName));
    }

    /**
     * Retrieves an {@link Artifacts} entity by SHA-256 hash.
     *
     * @param sha256 the SHA-256 hash to search for
     * @return an {@link Artifacts} entity
     */
    @Override
    public Optional<Artifacts> findBySha256Hash(final String sha256) {

        return artifactsRepository.findBySha256HashIgnoreCase(sha256);
    }

    /**
     * Retrieves an {@link Artifacts} entity by its ID.
     *
     * @param artifactsId the ID of the entity to retrieve
     * @return the found {@link Artifacts} entity wrapped in an {@link Optional}
     */
    @Override
    public Optional<Artifacts> getArtifactsById(long artifactsId) {
        return artifactsRepository.getArtifactsById(artifactsId);
    }

    /**
     * Updates the given {@link Artifacts} entity.
     *
     * @param artifacts the artifacts entity to be updated
     */

    @Override
    public void update(Artifacts artifacts) {
        final JpaArtifacts jpaArtifacts = (JpaArtifacts) artifacts;
        artifactsRepository.save(jpaArtifacts);
    }

    @Override
    public Page<Artifacts> findAll(Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(artifactsRepository, pageable, null);
    }

    @Override
    public long count() {
        return artifactsRepository.count();
    }

    @Override
    public boolean artifactExistsByFilename(String filename) {
        return false;
    }

    @Override
    public Long countAssociationsByArtifactIdAndSoftwareModuleId(long artifactId, long softwareModuleId) {
        return artifactModuleLinkRepository.countByArtifactIdAndSoftwareModuleId(artifactId, softwareModuleId);
    }

    /**
     * This method creates or updates the associations between artifacts and software modules.
     *
     * @param iAssociationList A set of artifact-software module associations to be created or updated.
     * @throws IllegalArgumentException If the provided artifact or association list is null.
     */
    @Override
    @Transactional
    public void createOrUpdateArtifactSoftwareModuleAssociation(Set<ArtifactSoftwareModuleAssociation> iAssociationList) {
        Set<JpaArtifactSoftwareModuleAssociationEntity> associationList = iAssociationList.stream()
                .map(JpaArtifactSoftwareModuleAssociationEntity.class::cast)
                .collect(Collectors.toSet());
        artifactModuleLinkRepository.saveAll(associationList);
        JpaArtifacts artifact = associationList.iterator().next().getArtifact();
        // Upload the artifact to CDN
        CdnUploadRequest cdnUploadRequest = new CdnUploadRequest();
        cdnUploadRequest.setBucketName(bucketName);
        cdnUploadRequest.setS3FileName(buildS3AbsolutePathFileName(artifact.getSha256Hash(), artifact.getFileName()));
        cdnUploadRequest.setCdnFileName(artifact.getFileName());
        cdnUploadRequest.setCdn(getTenantCdn());
        cdnUploadRequest.setTenantId(systemManagement.getTenantMetadata().getTenantId());
        cdnUploadRequest.setCdnDirPath(generateCdnPath(artifact.getTenant(), artifact.getSha256Hash()));
        cdnUploadRequest.setFileType(Constants.ARTIFACT);
        cdnUploadRequest.setFileId(artifact.getId());
        try {
            if(!FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.equals(artifact.getFileStatus())){
                PublishResponse message = snsServiceFactory.getInstance(SnsServiceType.CDN_UPLOAD).publishMessage(cdnUploadRequest).join();
                log.debug("Message published to SNS for uploading to CDN successfully with messageId: {}", message.messageId());
                artifact.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.toString());
                artifactsRepository.save(artifact);
            }
        } catch (CompletionException e) {
            log.error("Failed to publish message {} to SNS for Cdn upload with the reason {}", cdnUploadRequest, e.getMessage());
        }
    }

    /**
     * Generates the CDN path for the artifact.
     *
     * @param tenant the tenant
     * @param sha256 the SHA256 hash
     * @return the CDN path
     */
    private String generateCdnPath(String tenant, String sha256) {
        return CDNFileUtil.getCdnFilePath(artifactUrlHandlerProperties.getCdn().getDirectory(), tenant, sha256, ARTIFACT.getFileType());
    }

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} by the given artifact ID and software module ID.
     *
     * @param artifactId the ID of the artifact
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    @Override
    public List<ArtifactSoftwareModuleAssociation> findAssociationByArtifactId(long artifactId) {
        return artifactModuleLinkRepository.findByArtifactId(artifactId);
    }

    /**
     * Retrieves a list of {@link ArtifactSoftwareModuleAssociation} entities based on the given artifact ID and software module ID.
     *
     * @param artifactId       the ID of the artifact
     * @param softwareModuleId the ID of the software module
     * @return an {@link Optional} containing a list of {@link ArtifactSoftwareModuleAssociation} entities
     * matching the given artifact ID and software module ID.
     * If no associations are found, the {@link Optional} will be empty.
     */
    public Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsByArtifactIdAndSoftwareModuleId(
            long artifactId, long softwareModuleId) {

        // Fetch the list of associations from the repository
        return artifactModuleLinkRepository
                .findAssociationsByArtifactIdAndSoftwareModuleId(artifactId, softwareModuleId);
    }

    /**
     * Retrieves a list of {@link ArtifactSoftwareModuleAssociation} entities based on the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     * @return an {@link Optional} containing a list of {@link ArtifactSoftwareModuleAssociation} entities
     * matching the given software module ID.
     * If no associations are found, the {@link Optional} will be empty.
     */
    public Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsBySoftwareModuleId(long softwareModuleId) {

        // Fetch the list of associations from the repository
        return artifactModuleLinkRepository
                .findAssociationsBySoftwareModuleId(softwareModuleId);
    }

    /**
     * Deletes the {@link ArtifactSoftwareModuleAssociation} entity with the specified ID.
     *
     * @param id the ID of the association to delete
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteArtifactSoftwareModuleAssociation(final long id) {
        artifactModuleLinkRepository.deleteById(id);
    }

    /**
     * Purge the {@link Artifacts} from CDN and unlink software modules.
     *
     * @param artifact the {@link Artifacts} entity for which the associations are being deleted and purged.
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void purgeArtifacts(Artifacts artifact) {
        if (artifact.getArtifactStatus().equals(ArtifactsAuditStatus.PURGED.name())) {
            log.error("Artifact ID is already purged: {}", artifact.getId());
            throw new ValidationException("Artifact is already purged with ID: " + artifact.getId());
        }

        // Delete associations
        artifactModuleLinkRepository.deleteByArtifact(artifact);

        //delete artifact and capture audit
        deleteArtifactsById(artifact.getId(), ArtifactsAuditStatus.PURGED);

        // Cancel Device Actions
        cancelDeviceActionsForArtifact(artifact);
    }

    /**
     * Cancels all device actions which are in DD_SENT linked to the purging artifact.
     * <p>
     * This method retrieves all actions associated with the artifact that have passed the DD_SENT state
     * but are not yet finished or canceled. It attempts to cancel these actions.
     * </p>
     *
     * @param artifact The {@link Artifacts} entity whose associated device actions are to be canceled.
     */
    private void cancelDeviceActionsForArtifact(Artifacts artifact) {
        Page<JpaActionArtifact> actions;
        int page = FIRST_PAGE_NUMBER;
        do {
            actions = actionArtifactRepository.findActionsByArtifactId(artifact.getId(), PageRequest.of(page, purgedVinsBatchSize));
            if (actions.getContent().isEmpty()) {
                log.info("No actions found to be cancelled for artifact with ID: {}", artifact.getId());
                return;
            }
            List<JpaAction> actionPage = actions.getContent().stream().map(JpaActionArtifact::getAction).toList();
            log.debug("Processing page for purged artifact cancel action {} with {} actions for artifact ID: {}", page, actionPage.size(), artifact.getId());
            cancelActions(actionPage, artifact);
            page++;
        } while (actions.hasNext());
    }

    /**
     * Cancels the actions that are in DD_SENT state for the given artifact.
     * <p>
     * This method iterates through the list of actions, checks if they are in DD_SENT state,
     * and attempts to cancel them using the {@link RolloutManagement} service.
     * </p>
     *
     * @param actions  The list of {@link JpaAction} entities to be canceled.
     * @param artifact The {@link Artifacts} entity associated with the actions.
     */
    void cancelActions(List<JpaAction> actions, Artifacts artifact) {
        log.info("Found {} actions with DD_SENT for artifact ID: {}", actions.size(), artifact.getId());
        for (JpaAction action : actions) {
            Long rolloutId = action.getRollout().getId();
            String controllerId = action.getTarget().getControllerId();
            log.debug("Attempting to cancel action with DD_SENT for rollout ID: {} and controller ID: {}", rolloutId, controllerId);
            try {
                rolloutManagement.cancelDeviceAction(rolloutId, controllerId);
                log.info("Successfully canceled action for rollout ID: {} and controller ID: {}", rolloutId, controllerId);
            } catch (Exception e) {
                log.error("Failed to cancel action for rollout ID: {} and controller ID: {} due to: {}", rolloutId, controllerId, e.getMessage(), e);
            }
        }
        log.info("Notifying DOCG about purged artifact VINs for artifact ID: {}", artifact.getId());
        notifyPurgedArtifactVins(actions, artifact);
    }

    /**
     * Notifies the DOCG about the purged artifact VINs.
     * <p>
     * This method sends a message to the JMS service indicating that the artifact has been purged
     * and includes the associated error messages.
     * </p>
     *
     * @param actions The list of actions associated with the purged artifact.
     */
    void notifyPurgedArtifactVins(List<JpaAction> actions, Artifacts artifact) {
        log.debug("Notifying DOCG about purged artifact VINs for artifact ID: {}", artifact.getId());

        FileDeleteErrorMessage fileDeleteErrorMessage = FileDeleteErrorMessage.builder()
                .type(INFO)
                .fileId(artifact.getId())
                .fileName(artifact.getFileName())
                .sha256(artifact.getSha256Hash())
                .status(PURGED)
                .vehicleId(actions.stream()
                        .map(JpaAction::getTarget)
                        .map(Target::getControllerId)
                        .toList())
                .timestamp(Instant.now().atZone(ZoneId.of("UTC")).toEpochSecond())
                .build();


        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .fileType(ARTIFACT_FILETYPE)
                .build();
        log.debug("Header For Purged Artifact: {}", header);

        // Wrap in KafkaEventTemplate
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(fileDeleteErrorMessage)
                .build();
        log.debug("Payload For Purged Artifact: {}", header);


        // Send the FileDeleteErrorMessage using KafkaMessageService with messageType
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.FILE_DELETE_ERROR);
        log.debug("Purge Artifact VINs notification sent to DOCG for artifact ID: {}", artifact.getId());
    }

    /**
     * Upload a new {@link Artifacts} entity to S3.
     */
    @Override
    public S3FileUpload getS3FileUploadEntity(String sha256, String filename) {
        try {
            final String s3KeyPath = S3FileUtil.generateS3KeyPath(artifactUrlHandlerProperties.getS3().getDirectory(),
                    tenantAware.getCurrentTenant(), sha256, ARTIFACT.getFileType());
            return S3FileUpload.builder()
                    .bucketName(bucketName)
                    .filename(filename)
                    .keyPath(s3KeyPath)
                    .build();

        } catch (Exception e) {
            throw new GenericSpServerException("Error while uploading artifact to CDN", e);
        }
    }


    /**
     * @param artifactsUpload
     * @param fileSize
     * @param md5Digest
     * @return
     * @throws IOException
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifacts saveArtifacts(ArtifactsUpload artifactsUpload, final Long fileSize, MessageDigest md5Digest) throws IOException {

        byte[] md5Bytes = md5Digest.digest();
        String md5Checksum = toHex(md5Bytes); // Convert to hex string

        JpaArtifacts jpaArtifacts = new JpaArtifacts();
        jpaArtifacts.setFileName(artifactsUpload.getFilename());
        jpaArtifacts.setFileType(FileType.valueOf(artifactsUpload.getFileType()));
        jpaArtifacts.setDescription(artifactsUpload.getDescription());
        jpaArtifacts.setSha256Hash(artifactsUpload.getArtifactsHash().getSha256());
        jpaArtifacts.setMd5Hash(md5Checksum);
        jpaArtifacts.setFileSize(fileSize);
        jpaArtifacts.setExpiryDate(artifactsUpload.getSignatureExpiryDate());
        jpaArtifacts.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        jpaArtifacts.setArtifactStatus(ACTIVE.name());
        return artifactsRepository.save(jpaArtifacts);
    }


    /**
     * Retrieves the count of all artifact software module associations
     *
     * @return the count of all artifact software module associations
     */
    @Override
    public Long artifactSoftwareModuleAssociationCount() {
        return artifactModuleLinkRepository.count();
    }

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given artifact ID, software module ID, target version ID, source version ID.
     *
     * @param softwareModuleId the ID of software module
     * @param sourceVersionId  the ID of source version
     * @param targetVersionId  the ID of target version
     * @param artifactId       the ID of artifact
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    @Override
    public Optional<ArtifactSoftwareModuleAssociation> findArtifactSoftwareModuleAssociation(Long softwareModuleId, Long sourceVersionId, Long targetVersionId, Long artifactId) {
        return artifactModuleLinkRepository.findBySoftwareModuleIdAndSourceVersionIdAndTargetVersionIdAndArtifactId(softwareModuleId, sourceVersionId, targetVersionId, artifactId);
    }

    /**
     * Returns all the source and target version pairs for artifacts with the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     */
    @Override
    public Set<SourceTargetVersionPair<Long, Long>> findDistinctSourceAndTargetVersionsBySoftwareModuleId(Long softwareModuleId) {
        return artifactModuleLinkRepository.findBySoftwareModuleId(softwareModuleId).stream().map(result -> new SourceTargetVersionPair<>(result.getSourceVersion() != null ? result.getSourceVersion().getId() : null, result.getTargetVersion().getId())).collect(Collectors.toSet());
    }

    /**
     * Creates a new {@link Artifacts} entity based on the provided {@link ArtifactsCreate} object.
     *
     * @param artifactsUpload the creation object containing the data for the new entity
     * @return the created {@link Artifacts} entity
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Artifacts intiateFileUpload(final ArtifactsUpload artifactsUpload, Long identifier, Long tenantId, String sha256) {


        // Create SNS Message
        S3FileTransferRequest s3FileTransferRequest = S3FileTransferRequest.builder()
                .fileURL(artifactsUpload.getFileURL())
                .bucketName(bucketName)
                .fileId(identifier)
                .tenantId(tenantId)
                .fileName(buildS3AbsolutePathFileName(sha256, artifactsUpload.getFilename()))
                .fileType(Constants.ARTIFACT)
                .checksum(artifactsUpload.getArtifactsHash().getSha256())
                .build();

        try {
            // Publish message to SNS asynchronously
            PublishResponse message = snsServiceFactory.getInstance(SnsServiceType.S3_FILE_TRANSFER).publishMessage(s3FileTransferRequest).join();
            log.debug("Message published to SNS successfully with messageId: {}", message.messageId());

            // Find the JpaArtifacts entity by ID
            JpaArtifacts jpaArtifacts = artifactsRepository.findById(identifier)
                    .orElseThrow(() -> new EntityNotFoundException(JpaArtifacts.class, identifier));

            // Update the file status
            jpaArtifacts.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.toString());

            // Save the updated entity
            return artifactsRepository.save(jpaArtifacts);

        } catch (
                Exception e)  //NOSONAR
        {
            // Handle SNS publish failure
            log.error("Failed to publish message to SNS with reason: {}", e.getMessage());
            // Re-throw the exception wrapped in a runtime exception
            throw new RuntimeException("Failed to publish message to SNS", e);
        }

    }


    /**
     * Ensures the specified directory exists, creating it if necessary.
     *
     * @param path the path to the directory
     * @throws IOException if an error occurs while creating the directory
     */
    private void checkExistingDirectory(String path) throws IOException {
        Path directoryPath = Paths.get(path);
        if (!Files.exists(directoryPath)) {
            try {
                Files.createDirectories(directoryPath);
            } catch (IOException e) {
                throw new IOException("Failed to create directory: " + path, e);
            }
        }
    }

    /**
     * Copy input stream to file storage location
     *
     * @param fileName            the name of the file
     * @param fileStorageLocation the location to store the file
     * @param inputStream         the input stream to copy
     * @return the size of the copied file
     * @throws IOException if an error occurs while copying the file
     */
    private long createArtifactFile(String fileName, String fileStorageLocation, InputStream inputStream) throws IOException {
        checkExistingDirectory(fileStorageLocation);
        Path filePath = Paths.get(fileStorageLocation, fileName);
        return Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given software module ID and target version ID.
     *
     * @param softwareModuleId the ID of the software module
     * @param targetVersionId  the ID of the target version
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    @Override
    public Optional<ArtifactSoftwareModuleAssociation> findFirstBySoftwareModuleIdAndTargetVersionId(Long softwareModuleId, Long targetVersionId) {
        return artifactModuleLinkRepository.findFirstBySoftwareModuleIdAndTargetVersionId(softwareModuleId, targetVersionId);
    }

    /**
     * Validates the fields of the provided {@link MgmtArtifactsRequest} before artifact creation.
     * <ul>
     *   <li>Validates the file URL format.</li>
     *   <li>Checks that the signature expiry date is valid and in the future.</li>
     *   <li>Ensures the file type is supported.</li>
     *   <li>Verifies the SHA-256 hash format.</li>
     *   <li>Checks for existing files with the same SHA-256 hash.</li>
     * </ul>
     */
    @Override
    @Transactional
    public Artifacts createArtifactFromFileURL(MgmtArtifactsRequest artifactsRequest, Long tenantId) {
        log.debug("Starting createArtifactFromFileURL with filename: {}, tenantId: {}", artifactsRequest.getFilename(), tenantId);
        try {
            // Validate request fields
            validateFileURL(artifactsRequest.getFileURL());
            validateSignatureExpiryDate(artifactsRequest.getSignatureExpiryDate());
            validateFileType(artifactsRequest.getFileType());
            validateSHA256(artifactsRequest.getSha256());
            validateExistingFile(artifactsRequest.getSha256());

            // Optionally validate file size if enabled
            long fileSize = validateFileSize
                    ? FileSizeHelper.isFileSizeAcceptable(artifactsRequest.getFileURL())
                    : 0L;

            // Create ArtifactsUpload object with sanitized filename and provided metadata
            ArtifactsUpload artifactUpload = new ArtifactsUpload(
                    artifactsRequest.getFileURL(),
                    SupportPackageManagementUtil.sanitizeFileName(artifactsRequest.getFilename()),
                    artifactsRequest.getFileType(),
                    artifactsRequest.getDescription(),
                    artifactsRequest.getSignatureExpiryDate(),
                    fileSize,
                    new ArtifactsHash(null, artifactsRequest.getSha256())
            );
            log.debug("ArtifactsUpload created for filename: {}, fileSize: {}", artifactUpload.getFilename(), fileSize);

            Artifacts artifacts = saveArtifactInitial(artifactUpload);
            entityManager.flush();
            long identifier = artifacts.getId();
            if (identifier == NULL) {
                log.error("Artifact creation failed: generated identifier is 0 for filename: {}", artifactUpload.getFilename());
                throw new EntityNotFoundException("Artifact Not Found");
            }

            log.debug("Artifact initial save successful, identifier: {}", identifier);

            intiateFileUpload(artifactUpload, identifier, tenantId, artifactsRequest.getSha256());
            log.info("Artifact file upload initiated for identifier: {}, filename: {}", identifier, artifactUpload.getFilename());

            return artifacts;
        } catch (Exception e) {
            log.error("Failed to initiate file upload for artifact Name: {}, tenantId: {}. Reason: {}",
                    artifactsRequest.getFilename(), tenantId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Creates a new {@link Artifacts} entity from a multipart file upload.
     * <p>
     * Steps:
     * <ul>
     *   <li>Validates if the file already exists by SHA-256 hash.</li>
     *   <li>Checks if the file is empty.</li>
     *   <li>Validates SHA-256 hash, signature expiry date, and file type.</li>
     *   <li>Optionally validates the file size.</li>
     *   <li>Sanitizes and sets metadata for the artifact request.</li>
     *   <li>Computes MD5 and SHA-256 digests for the file.</li>
     *   <li>Uploads the file to S3 using multipart upload.</li>
     *   <li>Validates the computed SHA-256 hash against the provided hash.</li>
     *   <li>Creates and persists the artifact entity.</li>
     * </ul>
     *
     * @param file                the multipart file to upload
     * @param filename            the name of the file
     * @param fileType            the type of the file (e.g., DELTA, FULL)
     * @param description         the description of the artifact
     * @param sha256              the expected SHA-256 hash of the file
     * @param signatureExpiryDate the expiry date of the artifact signature (Unix timestamp)
     * @return the created {@link Artifacts} entity
     * @throws Exception if validation fails or file upload encounters an error
     */
    @Override
    public Artifacts createArtifactFromMultipartFile(MultipartFile file, String filename, String fileType, String description, String sha256, Long signatureExpiryDate) throws Exception {
        log.debug("Starting createArtifactFromMultipartFile with filename: {}, fileType: {}, sha256: {}, signatureExpiryDate: {}",
                filename, fileType, sha256, signatureExpiryDate);
        try (final InputStream inputStream = file.getInputStream()) {

            validateArtifactInputs(file, filename, fileType, sha256, signatureExpiryDate);

            // Prepare artifact request metadata
            log.debug("Preparing MgmtArtifactsRequest for filename: {}", filename);
            MgmtArtifactsRequest artifactsRequest = MgmtArtifactsRequest.builder()
                    .file(file)
                    .filename(SupportPackageManagementUtil.sanitizeFileName(filename))
                    .fileType(fileType)
                    .description(description)
                    .sha256(sha256)
                    .signatureExpiryDate(signatureExpiryDate)
                    .build();

            // Compute MD5 and SHA-256 digests
            log.debug("Computing MD5 and SHA-256 digests for filename: {}", filename);
            final MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            InputStream md5DigestStream = new DigestInputStream(inputStream, md5Digest);
            final MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            InputStream sha256DigestStream = new DigestInputStream(md5DigestStream, sha256Digest);

            // Create ArtifactsUpload object
            ArtifactsUpload artifactsUpload = new ArtifactsUpload(inputStream, artifactsRequest.getFilename(), fileType, description, signatureExpiryDate,
                    ArtifactsHash.builder().sha256(sha256).build());

            // Prepare S3 upload entity
            S3FileUpload fileUpload = getS3FileUploadEntity(artifactsRequest.getSha256(), SupportPackageManagementUtil.sanitizeFileName(artifactsRequest.getFilename()));

            // Upload file to S3 using multipart upload
            log.debug("Uploading file to S3 using multipart upload for filename: {}", filename);
            s3MultipartFileUpload.uploadFileToS3Multipart(fileUpload, sha256DigestStream);

            // Validate computed SHA-256 hash
            String computedSha256 = computeSha256Hash(sha256Digest);
            log.debug("Validating computed SHA-256 hash for filename: {}", filename);
            validateComputedHash(computedSha256, sha256, fileUpload);

            // Save artifact entity
            log.debug("Saving artifact entity for filename: {}", filename);
            return saveArtifacts(artifactsUpload, file.getSize(), md5Digest);

        } catch (Exception e) {
            log.error("Error occurred while creating artifact from multipart file: {}, reason: {}", filename, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validates the inputs for creating an artifact from a multipart file upload.
     * <ul>
     *   <li>Checks if an artifact with the same SHA-256 hash already exists.</li>
     *   <li>Ensures the uploaded file is not empty.</li>
     *   <li>Validates the SHA-256 hash format, signature expiry date, and file type.</li>
     *   <li>Optionally validates the file size if enabled.</li>
     * </ul>
     *
     * @param file                the multipart file to validate
     * @param filename            the name of the file
     * @param fileType            the type of the file (e.g., DELTA, FULL)
     * @param sha256              the expected SHA-256 hash of the file
     * @param signatureExpiryDate the expiry date of the artifact signature (Unix timestamp)
     * @throws EntityAlreadyExistsException if a file with the same SHA-256 already exists
     * @throws EntityCannotNullException    if the file is empty or required fields are missing
     * @throws ValidationException          if any validation fails (hash, file type, expiry date, or file size)
     */
    private void validateArtifactInputs(MultipartFile file, String filename, String fileType, String sha256, Long signatureExpiryDate) {
        // Check if an artifact with the same SHA-256 already exists
        validateExistingFile(sha256);

        // Check if the file is empty
        if (file.isEmpty()) {
            log.error("File is empty for filename: {}", filename);
            throw new EntityCannotNullException("File cannot be empty");
        }

        // Validate SHA-256, signature expiry date, and file type
        validateSHA256(sha256);
        validateSignatureExpiryDate(signatureExpiryDate);
        validateFileType(fileType);

        // Optionally validate file size
        if (validateFileSize) {
            log.debug("Validating file size for filename: {}", filename);
            FileSizeHelper.isFileSizeAcceptable(file.getSize());
        }
    }

    /**
     * Validates the computed hash against the provided SHA-256 hash.
     *
     * @param computedSha256 the computed SHA-256 hash
     * @param sha256         the provided SHA-256 hash
     * @throws ValidationException if the hashes do not match
     */
    private void validateComputedHash(String computedSha256, String sha256, S3FileUpload fileUpload) {
        log.debug("Validating computed SHA-256 hash: {} against provided SHA-256 hash: {}", computedSha256, sha256);
        if (!computedSha256.equalsIgnoreCase(sha256)) {
            log.error("SHA-256 mismatch: computed hash {} does not match provided hash {}", computedSha256, sha256);
            s3Repository.deleteFileFromS3(fileUpload);
            throw new ValidationException("Integrity check failed, SHA-256 mismatch.");
        }
        log.debug("SHA-256 hash validation successful.");
    }

    /**
     * Computes the SHA-256 hash of the given MessageDigest.
     *
     * @param sha256Digest the MessageDigest instance containing the data to hash
     * @return the computed SHA-256 hash as a hexadecimal string
     */
    private String computeSha256Hash(MessageDigest sha256Digest) {
        return HexFormat.of().formatHex(sha256Digest.digest());
    }

    /**
     * Checks if an artifact with the given SHA-256 hash already exists in the repository.
     * Throws an {@link EntityAlreadyExistsException} if a file with the same hash is found.
     *
     * @param sha256 the SHA-256 hash to check for existence
     * @throws EntityAlreadyExistsException if a file with the given SHA-256 already exists
     */
    private void validateExistingFile(String sha256) {
        log.debug("Checking for existing file with SHA-256: {}", sha256);
        findBySha256Hash(sha256)
                .ifPresent(existing -> {
                    throw new EntityAlreadyExistsException("File already exists in COSMOS with SHA-256: " + sha256);
                });
    }

    /**
     * Validates that the provided SHA-256 hash is non-empty and matches the expected format.
     *
     * @param sha256 the SHA-256 hash string to validate
     * @throws EntityCannotNullException if the hash is empty
     * @throws ValidationException       if the hash does not match the SHA-256 format
     */
    public static void validateSHA256(String sha256) {
        log.debug("Validating SHA-256: {}", sha256);
        if (sha256 == null || sha256.isEmpty()) {
            throw new EntityCannotNullException("sha256 cannot be empty");
        }
        final Pattern sha256Pattern = Pattern.compile("^[a-fA-F0-9]{64}$");
        if (!sha256Pattern.matcher(sha256).matches()) {
            throw new ValidationException("sha256 is not valid");
        }
    }

    /**
     * Validates if the provided file type is a valid type.
     *
     * @param type the file type to be validated
     * @throws ValidationException if the file type is not valid
     */
    private void validateFileType(String type) {
        log.debug("Validating file type: {}", type);
        for (FileType fileType : FileType.values()) {
            if (fileType.getTypeName().equalsIgnoreCase(type)) {
                return;
            }
        }
        throw new ValidationException("'fileType' should be either 'DELTA' or 'FULL'");
    }

    /**
     * Validates if the provided URL string is a well-formed and valid URL.
     *
     * @param url the URL string to validate
     * @throws ValidationException if the URL is not valid or cannot be parsed
     */
    private void validateFileURL(String url) {
        log.debug("Validating file URL: {}", url);
        try {
            new URL(url).toURI();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new ValidationException("'fileURL' should be a valid URL to download the artifact", e);
        }
    }

    /**
     * Validates that the provided signature expiry date is not empty and is set to a future date.
     *
     * @param signatureExpiryDate the expiry date of the signature as a Unix timestamp (seconds since epoch)
     * @throws EntityCannotNullException if the expiry date is empty
     * @throws ValidationException       if the expiry date is in the past or is the current date
     */
    private void validateSignatureExpiryDate(Long signatureExpiryDate) {
        log.debug("Validating signature expiry date: {}", signatureExpiryDate);
        if (signatureExpiryDate.describeConstable().isEmpty()) {
            throw new EntityCannotNullException("Signature Expiry Date cannot be empty. Add a valid expiry date and try again.");
        }
        LocalDate currentDate = Instant.now().atZone(UTC).toLocalDate();
        var endDate = Instant.ofEpochSecond(signatureExpiryDate).atZone(UTC).toLocalDate();
        if (endDate.isBefore(currentDate.plusDays(1))) {
            throw new ValidationException("'signatureExpiryDate' should not be past date or current date");
        }
    }

    /**
     * Replaces an existing artifact with a new one based on the provided {@link MgmtArtifactReplacementRequest}.
     * <p>
     * This method validates the replacement request, retrieves the existing artifact, and determines if it is linked to any software modules.
     * If the artifact is not linked, it creates the replacement artifact, removes the files for the existing artifact, and marks it as replaced.
     * If the artifact is linked to software modules, the replacement logic is not yet implemented.
     * </p>
     *
     * @param replaceArtifactsRequest the request containing details for the artifact replacement
     * @param tenantId                the tenant ID
     * @throws Exception if validation fails or artifact creation encounters an error
     */
    @Override
    @Transactional
    public Artifacts replaceArtifacts(MgmtArtifactReplacementRequest replaceArtifactsRequest, Long tenantId) throws Exception {
        // Validate inputs and retrieve the existing artifact
        Artifacts oldArtifacts = validateReplaceArtifactsInputs(replaceArtifactsRequest);
        List<ArtifactSoftwareModuleAssociation> softwareModuleAssociation = new ArrayList<>(oldArtifacts.getArtifactSoftwareModuleAssociations());
        Artifacts newArtifact = null;
        // If artifact is not linked to any software modules
        if (softwareModuleAssociation.isEmpty()) {
            newArtifact = createReplacementArtifacts(replaceArtifactsRequest, tenantId);
            removeFilesForExistingArtifact(oldArtifacts);
            markArtifactAsReplaced((JpaArtifacts) oldArtifacts);
        } else {
            newArtifact = createReplacementArtifacts(replaceArtifactsRequest, tenantId);
            for (ArtifactSoftwareModuleAssociation association : softwareModuleAssociation) {
                unlinkArtifactSoftwareModuleAssociation(tenantId, oldArtifacts.getId().toString(), association.getSoftwareModule().getId().toString());
            }

           // Flush and clear persistence context
            entityManager.flush();
            entityManager.clear();

            // Re-link software module associations to the new artifact
            for (ArtifactSoftwareModuleAssociation association : softwareModuleAssociation) {
                SoftwareModuleArtifactBindingRequest request = SoftwareModuleArtifactBindingRequest.builder()
                        .softwareModuleId(association.getSoftwareModule().getId().intValue())
                        .sourceVersion(association.getSourceVersion() != null ? List.of(association.getSourceVersion().getId().intValue()) : null)
                        .targetVersion(association.getTargetVersion().getId().intValue())
                        .build();

                // Call the method to create the association for the new artifact
                createArtifactSoftwareModuleAssociation(
                        newArtifact.getId().toString(),
                        tenantId,
                        request
                );
            }
            removeFilesForExistingArtifact(oldArtifacts);
            markArtifactAsReplaced((JpaArtifacts) oldArtifacts);
        }
        return newArtifact;
    }

    /**
     * Marks the given artifact as replaced by updating its status to "REPLACED" and saving it.
     *
     * @param existingArtifacts the artifact entity to mark as replaced
     */
    private void markArtifactAsReplaced(JpaArtifacts existingArtifacts) {
        existingArtifacts.setArtifactStatus(REPLACED.name());
        artifactsRepository.save(existingArtifacts);
        log.info("Artifact ID {} status updated to REPLACED.", existingArtifacts.getId());
    }

    /**
     * Removes existing artifact files from CDN and/or storage based on their current status.
     * Logs each removal action and warns if no action is taken.
     *
     * @param oldArtifacts the artifact entity whose files are to be removed
     */
    private void removeFilesForExistingArtifact(Artifacts oldArtifacts) {
        String status = oldArtifacts.getFileStatus().name();
        Long artifactId = oldArtifacts.getId();

        log.debug("Attempting to remove files for artifact ID {} with status {}", artifactId, status);

        if (FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name().equals(status)) {
            log.debug("Removing file from CDN and storage for artifact ID {}", artifactId);
            artifactsFileRemovalService.removeFileFromCDN(artifactId);
            artifactsFileRemovalService.removeFileFromStorage(artifactId);
        } else if (FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name().equals(status)
                || FileTransferStatus.UPLOADING_TO_CDN.name().equals(status)) {
            log.debug("Removing file from storage for artifact ID {}", artifactId);
            artifactsFileRemovalService.removeFileFromStorage(artifactId);
        } else {
            log.warn("No removal action taken for artifact ID {} with status {}", artifactId, status);
        }
    }

    /**
     * Creates a replacement artifact based on the provided {@link MgmtArtifactReplacementRequest}.
     * <p>
     * Determines whether to create the replacement artifact using a file URL or a multipart file,
     * validates the input, and initiates the appropriate artifact creation process.
     * Throws a {@link ValidationException} if neither a file URL nor a file is provided.
     *
     * @param replaceArtifactsRequest the request containing details for the replacement artifact
     * @param tenantId the tenant ID
     * @throws Exception if artifact creation or validation fails
     */
    private Artifacts createReplacementArtifacts(MgmtArtifactReplacementRequest replaceArtifactsRequest, Long tenantId) throws Exception {
        String fileURL = replaceArtifactsRequest.getFileURL();
        MultipartFile file = replaceArtifactsRequest.getFile();
        String filename = replaceArtifactsRequest.getFilename();
        String fileType = replaceArtifactsRequest.getFileType();
        String description = replaceArtifactsRequest.getDescription();
        String newSha256 = replaceArtifactsRequest.getNewSha256();
        Long signatureExpiryDate = replaceArtifactsRequest.getSignatureExpiryDate();

        boolean isFileUrlProvided = fileURL != null && !fileURL.isEmpty();
        boolean isFileProvided = file != null && !file.isEmpty();
        Artifacts artifacts = null;

        log.debug("createReplacementArtifacts called with isFileUrlProvided={}, isFileProvided={}, filename={}, fileType={}, tenantId={}",
                isFileUrlProvided, isFileProvided, filename, fileType, tenantId);

        if (isFileUrlProvided) {
            // Create artifact using the provided file URL
            log.info("Creating replacement artifact using file URL: {}", fileURL);
            MgmtArtifactsRequest artifactsRequest = MgmtArtifactsRequest.builder()
                    .fileURL(fileURL)
                    .filename(filename)
                    .fileType(fileType)
                    .description(description)
                    .sha256(newSha256)
                    .signatureExpiryDate(signatureExpiryDate)
                    .build();
            artifacts = createArtifactFromFileURL(artifactsRequest, tenantId);
            log.info("Replacement artifact created from file URL for tenantId={}", tenantId);
        } else if (isFileProvided) {
            // Create artifact using the provided multipart file
            log.info("Creating replacement artifact using multipart file: {}", filename);
            artifacts = createArtifactFromMultipartFile(file, filename, fileType, description, newSha256, signatureExpiryDate);
            log.info("Replacement artifact created from multipart file for tenantId={}", tenantId);
        } else {
            log.error("Neither fileURL nor file provided for artifact replacement");
            throw new ValidationException("Either a valid fileURL or a non-empty file must be provided for artifact replacement");
        }
        return artifacts;
    }

    /**
     * Validates the inputs for replacing an artifact using the provided {@link MgmtArtifactReplacementRequest}.
     * <p>
     * This method extracts and validates the old artifact's SHA-256 hash, file type, and replacement file or file URL
     * from the request. It ensures the old artifact exists, the file type matches, and the artifact is in an ACTIVE state.
     * Throws an exception if any validation fails.
     *
     * @param replaceArtifactsRequest the request containing replacement artifact details
     * @return the existing {@link Artifacts} entity to be replaced
     * @throws EntityNotFoundException if the old artifact does not exist
     * @throws ValidationException if validation of inputs fails
     */
    private Artifacts validateReplaceArtifactsInputs(MgmtArtifactReplacementRequest replaceArtifactsRequest) throws EntityNotFoundException, ValidationException {

        String oldSha256 = replaceArtifactsRequest.getOldSha256();
        String fileType = replaceArtifactsRequest.getFileType();
        String fileURL = replaceArtifactsRequest.getFileURL();
        MultipartFile file = replaceArtifactsRequest.getFile();

        log.debug("Validating replace artifact inputs: oldSha256={}, fileType={}, fileURL={}, filePresent={}",
                oldSha256, fileType, fileURL, file != null);

        // Check if the old artifact exists by SHA-256 hash
        Optional<Artifacts> oldArtifactsOpt = findBySha256Hash(oldSha256);
        if (oldArtifactsOpt.isEmpty()) {
            log.error("No artifact found with sha256: {}", oldSha256);
            throw new EntityNotFoundException("No artifact found with sha256");
        }
        Artifacts oldArtifacts = oldArtifactsOpt.get();
        log.debug("Found artifact for replacement: id={}, status={}, fileType={}", oldArtifacts.getId(),
                oldArtifacts.getArtifactStatus(), oldArtifacts.getFileType());

        // Verify that either a file or fileURL is provided for replacement
        boolean isFileProvided = file != null && !file.isEmpty();
        boolean isFileURLProvided = fileURL != null && !fileURL.isEmpty();
        if (!isFileProvided && !isFileURLProvided) {
            log.error("Neither file nor fileURL provided for artifact replacement");
            throw new ValidationException("Either a file or fileURL must be provided for replacement");
        }

        // Validate fileType format
        try {
            FileType.valueOf(fileType);
        } catch (Exception e) {
            log.error("Invalid fileType provided: {}", fileType);
            throw new ValidationException("'fileType' should be either 'DELTA' or 'FULL'");
        }

        // Ensure the file type matches the artifact being replaced
        if (!oldArtifacts.getFileType().getTypeName().equalsIgnoreCase(fileType)) {
            log.error("File type mismatch: existing={}, provided={}", oldArtifacts.getFileType(), fileType);
            throw new ValidationException("'fileType' should be the same as the fileType of the artifact being replaced");
        }

        if (!oldArtifacts.getArtifactStatus().equalsIgnoreCase(ACTIVE.name())) {
            log.error("Artifact with sha256 {} is not ACTIVE, cannot replace", oldSha256);
            throw new ValidationException("File with provided sha256 does not exist in COSMOS");
        }

        log.debug("Artifact replacement input validation successful for artifact id={}", oldArtifacts.getId());
        return oldArtifacts;
    }

    @Override
    public Page<Artifacts> findBySoftwareModule(final Pageable pageReq, final long swId) {
        throwExceptionIfSoftwareModuleDoesNotExist(swId);

        Page<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociations = artifactModuleLinkRepository.findBySoftwareModuleId(pageReq, swId);
        Page<Artifacts> artifactsPage = artifactSoftwareModuleAssociations.map(ArtifactSoftwareModuleAssociation::getArtifact);

        return artifactsPage;
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long swId) {
        if (!softwareModuleRepository.existsById(swId)) {
            throw new EntityNotFoundException(SoftwareModule.class, swId);
        }
    }

    private String getTenantCdn() {
        final String cdn = systemManagement.getTenantCdn();
        return StringUtils.hasText(cdn) ? cdn : tenantConfigurationManagement
                .getGlobalConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, String.class);
    }

    private String buildS3AbsolutePathFileName(String sha256, String fileName) {
        return S3FileUtil.generateS3KeyPath(artifactUrlHandlerProperties.getS3().getDirectory(), tenantAware.getCurrentTenant(), sha256, ARTIFACT.getFileType(), fileName);
    }

    // Save the initial artifact to generate the ID
    @Transactional
    @Override
    public JpaArtifacts saveArtifactInitial(ArtifactsUpload artifactsUpload) {
        JpaArtifacts jpaArtifacts = new JpaArtifacts();
        jpaArtifacts.setFileName(artifactsUpload.getFilename());
        jpaArtifacts.setFileType(FileType.valueOf(artifactsUpload.getFileType()));
        jpaArtifacts.setDescription(artifactsUpload.getDescription());
        jpaArtifacts.setExpiryDate(artifactsUpload.getSignatureExpiryDate());
        jpaArtifacts.setFileSize(artifactsUpload.getFileSize());
        if (artifactsUpload.getArtifactsHash() != null) {
            jpaArtifacts.setSha256Hash(artifactsUpload.getArtifactsHash().getSha256());
        }
        jpaArtifacts.setArtifactStatus(ACTIVE.name());
        return artifactsRepository.save(jpaArtifacts);
    }

    @Override
    public void unlinkArtifactSoftwareModuleAssociation(Long tenantId, String artifactId, String softwareModuleId) {
        log.debug("Received Unlink Artifact Software Module Association request");
        Artifacts artifact = getArtifactsById(Long.parseLong(artifactId)).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, artifactId));

        SoftwareModule softwareModule = softwareModuleManagement.getSoftwareModuleById(Long.parseLong(softwareModuleId)).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociations = findAssociationsByArtifactIdAndSoftwareModuleId(artifact.getId(), softwareModule.getId())
                .orElseThrow(() -> new EntityNotFoundException("There is not any assignments"));
        // Check if the list is empty before processing
        if (artifactSoftwareModuleAssociations.isEmpty()) {
            throw new EntityNotFoundException("There is not any assignments");
        }

        // Assuming there is at least one association, get the first one and its target version
        ArtifactSoftwareModuleAssociation firstAssociation = artifactSoftwareModuleAssociations.get(0);
        Long targetVersionId = firstAssociation.getTargetVersion().getId();

        List<IDistributionSetModule> softwareModuleDist = softwareModule.getDsmRelation();

        // Check if the software module has any Distribution Set Modules.
        if (softwareModuleDist != null && !softwareModuleDist.isEmpty()) {
            // Iterate through the distribution Set Modules
            for (IDistributionSetModule distSetModule : softwareModuleDist) {
                // Compare target version od DistributionSetModule and artifactSoftwareModuleAssociation
                if (distSetModule.getVersion().getId().equals(targetVersionId)) {
                    throw new IllegalArgumentException("Software module is assigned to distribution set. Cannot be deleted.");
                }
            }
        }

        // Delete all associations related to the artifact and software module
        for (ArtifactSoftwareModuleAssociation association : artifactSoftwareModuleAssociations) {
            deleteArtifactSoftwareModuleAssociation(association.getId());
        }
    }

    @Override
    @Transactional
    public void createArtifactSoftwareModuleAssociation(String artifactId, Long tenantId, SoftwareModuleArtifactBindingRequest softwareModuleArtifactBindingRequest) {
        log.debug("Received Create Artifact Software Module Association request");
        // Check Artifact Exists
        Artifacts artifact = getArtifactsById(Long.parseLong(artifactId)).orElseThrow(() -> new EntityNotFoundException(Artifacts.class, artifactId));

        if (!ArtifactsStatus.ACTIVE.name().equals(artifact.getArtifactStatus())) {
            throw new ValidationException("Artifact is not in an active state");
        }

        // Validating Artifact Expiry Date
        if (Instant.now().getEpochSecond() > artifact.getExpiryDate()) {
            throw new ValidationException("Signature of the artifact is expired");
        }

        Integer softwareModuleId = softwareModuleArtifactBindingRequest.getSoftwareModuleId();
        List<Integer> sourceVersionList = softwareModuleArtifactBindingRequest.getSourceVersion();
        Integer targetVersionId = softwareModuleArtifactBindingRequest.getTargetVersion();

        // Check software module exists
        SoftwareModule softwareModule = softwareModuleManagement.get(softwareModuleId).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        // Populate source and target version combination
        Set<SourceTargetVersionPair<Long, Long>> distinctSourceAndTargetVersionsBySoftwareModuleId = findDistinctSourceAndTargetVersionsBySoftwareModuleId(Long.valueOf(softwareModuleId));

        if (artifact.getFileType().equals(FileType.DELTA)) {
            validateDeltaArtifactSoftwareModule(sourceVersionList, softwareModuleId, targetVersionId, artifact, distinctSourceAndTargetVersionsBySoftwareModuleId);
        } else if (artifact.getFileType().equals(FileType.FULL)) {
            validateFullArtifactSoftwareModule(softwareModuleArtifactBindingRequest, sourceVersionList, distinctSourceAndTargetVersionsBySoftwareModuleId, targetVersionId, softwareModuleId);
        }

        // Validate whether software module ID and target version is linked
        Set<ArtifactSoftwareModuleAssociation> associationsList = createAssociationsList(artifact, softwareModuleArtifactBindingRequest, softwareModule, validateVersions(softwareModule, targetVersionId.longValue()));

        log.info("Creating or updating artifact software module association for artifact {}", artifactId);

        createOrUpdateArtifactSoftwareModuleAssociation(associationsList);
    }

    private void validateDeltaArtifactSoftwareModule(List<Integer> sourceVersionList, Integer softwareModuleId, Integer targetVersionId, Artifacts artifact, Set<SourceTargetVersionPair<Long, Long>> distinctSourceAndTargetVersionsBySoftwareModuleId) {

        // Source Version List cannot be Null or Empty
        if (sourceVersionList == null || sourceVersionList.isEmpty()) {
            throw new ValidationException(String.format("The source version is mandatory for Delta type for module %s", softwareModuleId));
        }

        // Source Version List's size should be one
        if (sourceVersionList.size() > 1) {
            throw new ValidationException(String.format("Source version for delta cannot be more than one for module %s", softwareModuleId));
        }

        // Source Version List cannot be Null
        if (sourceVersionList.get(0) == null) {
            throw new ValidationException(String.format("The source version is mandatory for Delta type for module %s", softwareModuleId));
        }

        // Target and Source Version cannot be the same
        if (Objects.equals(sourceVersionList.get(0), targetVersionId)) {
            throw new ValidationException(String.format("The source and target versions must not be the same for module %s", softwareModuleId));
        }

        // Check if the Artifact Software Module Association already exists
        if (countAssociationsByArtifactIdAndSoftwareModuleId(artifact.getId(), softwareModuleId) > 0) {
            throw new ValidationException(String.format("The association for artifact %s and software module %s already exists", artifact.getId(), softwareModuleId));
        }

        // Check if the source and target combination already exists for different artifact
        if (distinctSourceAndTargetVersionsBySoftwareModuleId.contains(new SourceTargetVersionPair<>(Long.valueOf(sourceVersionList.get(0)), Long.valueOf(targetVersionId)))) {
            throw new ValidationException(String.format(VERSION_ALREADY_EXISTS, sourceVersionList.get(0), targetVersionId, softwareModuleId));
        }
    }

    private static void validateFullArtifactSoftwareModule(SoftwareModuleArtifactBindingRequest softwareModuleArtifactBindingRequest, List<Integer> sourceVersionList, Set<SourceTargetVersionPair<Long, Long>> distinctSourceAndTargetVersionsBySoftwareModuleId, Integer targetVersionId, Integer softwareModuleId) {
        // Check if the source and target combination already exists with source version as null, if present, filter out
        if ((sourceVersionList == null || sourceVersionList.isEmpty()) && distinctSourceAndTargetVersionsBySoftwareModuleId.contains(new SourceTargetVersionPair<>(null, Long.valueOf(targetVersionId)))) {
            throw new ValidationException(String.format(VERSION_ALREADY_EXISTS, null, targetVersionId, softwareModuleId));
        }

        // Removing Duplicate Source Versions from the Source Versions List
        // If the Source and Target Versions are the same, replacing Source Version with Null
        // Check if the source and target combination already exists, if present, filter out
        if (sourceVersionList != null && !sourceVersionList.isEmpty()) {
            softwareModuleArtifactBindingRequest.setSourceVersion(sourceVersionList.stream().map(sourceVersionId -> {
                if (Objects.equals(sourceVersionId, targetVersionId)) sourceVersionId = null;
                return sourceVersionId;
            }).filter(sourceVersionId -> {
                Long sourceVersion;
                if (sourceVersionId != null) sourceVersion = Long.valueOf(sourceVersionId);
                else sourceVersion = null;
                return !distinctSourceAndTargetVersionsBySoftwareModuleId.contains(new SourceTargetVersionPair<>(sourceVersion, Long.valueOf(targetVersionId)));
            }).distinct().toList());
        } else {
            softwareModuleArtifactBindingRequest.setSourceVersion(Collections.singletonList(null));
        }

        // If the source version list is empty after all filtration, throw an exception
        if (softwareModuleArtifactBindingRequest.getSourceVersion().isEmpty()) {
            throw new ValidationException(String.format(VERSION_ALREADY_EXISTS, null, targetVersionId, softwareModuleId));
        }
    }

    private Set<ArtifactSoftwareModuleAssociation> createAssociationsList(Artifacts artifact, SoftwareModuleArtifactBindingRequest request, SoftwareModule module, Version targetVersion) {
        Set<ArtifactSoftwareModuleAssociation> associationsList = new HashSet<>();
        if (artifact.getFileType().equals(FileType.FULL)) {
            if (request.getSourceVersion() != null && !request.getSourceVersion().isEmpty()) {
                associationsList.addAll(request.getSourceVersion().stream().map(sourceVersionId -> {
                    // Validate whether software module ID and source version is linked
                    Version sourceVersion = (sourceVersionId == null) ? null : validateVersions(module, Long.valueOf(sourceVersionId));
                    // Populate entity object into a set
                    return MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(
                            artifact, sourceVersion, targetVersion, module);
                }).collect(Collectors.toSet()));
            } else {
                // Populate entity object into a set
                associationsList.add(MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(
                        artifact, null, targetVersion, module));
            }
        } else if (artifact.getFileType().equals(FileType.DELTA)) {
            // Validate whether software module ID and source version is linked
            Version sourceVersion = validateVersions(module, Long.valueOf(request.getSourceVersion().get(0)));
            // Populate entity object into a set
            associationsList.add(MgmtArtifactSoftwareModuleAssociationMapper.toArtifactSoftwareModuleAssociationEntity(artifact, sourceVersion, targetVersion, module));
        }
        return associationsList;
    }

    private Version validateVersions(SoftwareModule module, Long versionId) {
        Version version = versionManagement.getById(versionId).orElseThrow(() -> new EntityNotFoundException(Version.class, versionId));
        if (version.getSoftwareModuleId() == null || (!Objects.equals(version.getSoftwareModuleId().getId(), module.getId()))) {
            throw new EntityNotFoundException(String.format("The given Version %s is not available for the Software module %s", versionId, module.getId()));
        }
        return version;
    }

    /**
     * Retrieves a paginated list of {@link Artifacts} matching the given RSQL filter expression.
     * <p>
     * Supports advanced filtering using RSQL syntax and pagination via {@link Pageable}.
     * </p>
     *
     * @param pageable  the pagination and sorting information (must not be {@code null})
     * @param rsqlParam the RSQL filter string (must not be {@code null} or blank)
     * @return a {@link Page} of {@link Artifacts} matching the filter criteria
     * @throws IllegalArgumentException if {@code rsqlParam} is null or blank
     */
    @Override
    public Page<Artifacts> findByRsql(final Pageable pageable, final String rsqlParam) {
        log.debug("Finding artifacts by RSQL: {}", rsqlParam);
        final List<Specification<JpaArtifacts>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ArtifactsFields.class, virtualPropertyReplacer, database)
        );
        return JpaManagementHelper.findAllWithCountBySpec(artifactsRepository, pageable, specList);
    }
}