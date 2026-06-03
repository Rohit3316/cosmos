package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.models.sqs.FileType;
import org.cosmos.sns.exception.SnsException;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.models.SnsConstants;
import org.cosmos.sns.services.SnsServiceType;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.validation.ValidationException;

import static org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties.S3.Type.ARTIFACT;

/**
 * Service for handling S3 file deletion operations for artifacts via SNS.
 */
@Service
public class ArtifactsFileRemovalService implements FileRemovalService {

    private static final Logger LOG = LoggerFactory.getLogger(ArtifactsFileRemovalService.class);

    private final ArtifactFilesystemProperties artifactFilesystemProperties;
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    private final TenantAware tenantAware;
    private final SnsServiceFactory snsServiceFactory;
    private final SystemManagement systemManagement;
    private final ArtifactsRepository artifactsRepository;

    public ArtifactsFileRemovalService(ArtifactFilesystemProperties artifactFilesystemProperties,
                                       ArtifactUrlHandlerProperties artifactUrlHandlerProperties,
                                       TenantAware tenantAware,
                                       SnsServiceFactory snsServiceFactory,
                                       SystemManagement systemManagement,
                                       ArtifactsRepository artifactsRepository) {
        this.artifactFilesystemProperties = artifactFilesystemProperties;
        this.artifactUrlHandlerProperties = artifactUrlHandlerProperties;
        this.tenantAware = tenantAware;
        this.snsServiceFactory = snsServiceFactory;
        this.systemManagement = systemManagement;
        this.artifactsRepository = artifactsRepository;
    }

    private Artifacts getArtifacts(Long fileId) {
        return artifactsRepository.getArtifactsById(fileId).orElseThrow(()->new EntityNotFoundException(Artifacts.class,fileId));
    }

    /**
     * Removes the specified artifact file from S3 storage via SNS.
     *
     * @param fileId the fileId of artifact to be removed
     * @throws IllegalArgumentException if the file is not an instance of Artifacts
     * @throws SnsException             if an error occurs while publishing the message to SNS
     */
    @Override
    @Retryable(include = {SnsException.class}, maxAttempts = SnsConstants.SNS_RETRY_COUNT,
            backoff = @Backoff(delay = SnsConstants.SNS_RETRY_DELAY_IN_MILLIS))
    public void removeFileFromStorage(Long fileId) throws ValidationException {
        LOG.info("Removing artifact file with id {} from storage",fileId);
        Artifacts artifact=getArtifacts(fileId);

        if(artifact.getFileStatus().equals(FileTransferStatus.STORAGE_DELETE_SUCCESSFUL)){
            throw new ValidationException("File is already deleted from storage");
        }

        String s3KeyPath = S3FileUtil.generateS3KeyPath(
                artifactUrlHandlerProperties.getS3().getDirectory(),
                tenantAware.getCurrentTenant(),
                artifact.getSha256Hash(),
                ARTIFACT.getFileType(),
                artifact.getFileName()
        );

        S3FileDeletionRequest deletionRequest = S3FileUtil.buildDeletionRequest(
                artifactFilesystemProperties.getS3bucket().getName(),
                s3KeyPath,
                artifact.getId(),
               ARTIFACT.getFileType(),
                systemManagement.getTenantMetadata().getTenantId()
        );

        publishDeletionRequest(deletionRequest, SnsServiceType.S3_FILE_DELETE);
        updateFileStatus((JpaArtifacts) artifact,FileTransferStatus.DELETING_FROM_STORAGE);
    }

    @Override
    public void removeFileFromCDN(Long fileId) {
        LOG.info("Removing artifact file with id {} from CDN",fileId);
        Artifacts artifact=getArtifacts(fileId);
        Long tenantId=systemManagement.getTenantMetadata().getTenantId();
        final String cdnFilePath=getCdnFilePath(artifact);
        var cdnDeleteRequest=CDNFileUtil.buildDeletionRequest(artifact.getId(),cdnFilePath,ARTIFACT.getFileType(),tenantId);
        publishDeletionRequest(cdnDeleteRequest, SnsServiceType.CDN_DELETE);
        updateFileStatus((JpaArtifacts) artifact,FileTransferStatus.DELETING_FROM_CDN);
    }

    @Override
    public FileType getFileType() {
        return FileType.ARTIFACT;
    }

    private String getCdnFilePath(Artifacts artifact){
        String directoryPlaceHolder=artifactUrlHandlerProperties.getCdn().getDirectory();
        String tenant=systemManagement.currentTenant();
        return CDNFileUtil.getCdnFilePath(directoryPlaceHolder,tenant,artifact.getSha256Hash(),artifact.getFileName(),ARTIFACT.getFileType());

    }

    /**
     * Publishes the S3FileDeletionRequest to SNS.
     *
     * @param deletionRequest the S3FileDeletionRequest
     * @throws SnsException if an error occurs while publishing the message to SNS
     */
    private void publishDeletionRequest(SnsPublishable deletionRequest, SnsServiceType snsServiceType) {

        LOG.debug("Preparing to delete artifact file from S3 with request: {}", deletionRequest);
        try {
            PublishResponse response = snsServiceFactory.getInstance(snsServiceType).publishMessage(deletionRequest).join();
            LOG.debug("Successfully published message to SNS for artifact deletion. Request: {}, Response: {}", deletionRequest, response);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to publish message to SNS for artifact deletion. Request: %s, Error: %s", deletionRequest, e.getMessage());
            throw new SnsException(errorMessage, e);
        }
    }

    private void updateFileStatus(JpaArtifacts artifacts, FileTransferStatus status) {
        artifacts.setFileStatus(status.toString());
        artifactsRepository.save(artifacts);
    }
}
