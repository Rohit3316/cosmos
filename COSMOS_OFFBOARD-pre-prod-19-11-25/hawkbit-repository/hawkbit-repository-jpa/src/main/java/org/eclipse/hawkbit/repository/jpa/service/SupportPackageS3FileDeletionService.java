package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.sns.exception.SnsException;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.models.SnsConstants;
import org.cosmos.sns.services.SnsServiceType;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * Service for managing the deletion of support package files from s3 via sns.
 */
public abstract class SupportPackageS3FileDeletionService  {

    private static final Logger LOG = LoggerFactory.getLogger(SupportPackageS3FileDeletionService.class);

    protected final SupportPackageFileSystemProperties supportPackageFileSystemProperties;
    protected final SupportPackageUrlHandlerProperties supportUrlHandlerProperties;
    private final TenantAware tenantAware;
    private final SnsServiceFactory snsServiceFactory;
    private final SystemManagement systemManagement;

    @Autowired
    protected SupportPackageS3FileDeletionService(SupportPackageFileSystemProperties supportPackageFileSystemProperties,
                                               SupportPackageUrlHandlerProperties supportUrlHandlerProperties,
                                               TenantAware tenantAware,
                                               SnsServiceFactory snsServiceFactory,
                                               SystemManagement systemManagement) {
        this.supportPackageFileSystemProperties = supportPackageFileSystemProperties;
        this.supportUrlHandlerProperties = supportUrlHandlerProperties;
        this.tenantAware = tenantAware;
        this.snsServiceFactory = snsServiceFactory;
        this.systemManagement=systemManagement;

    }

    /**
     * Removes the specified support package file from S3 storage via SNS.
     *
     * @param supportPackage     the support package to be removed
     * @param fileType the type of the file
     * @throws IllegalArgumentException if the file is not an instance of BaseSupportPackage
     * @throws SnsException             if an error occurs while publishing the message to SNS
     */

    @Retryable(include = {SnsException.class}, maxAttempts = SnsConstants.SNS_RETRY_COUNT,
            backoff = @Backoff(delay = SnsConstants.SNS_RETRY_DELAY_IN_MILLIS))
    public void removeFileFromStorage(BaseSupportPackage supportPackage, String fileType,String s3Directory) {

        String s3KeyPath = S3FileUtil.generateS3KeyPath(
                s3Directory,
                tenantAware.getCurrentTenant(),
                supportPackage.getSha256Hash(),
                fileType,
                supportPackage.getFileName()
        );

        S3FileDeletionRequest deletionRequest = S3FileUtil.buildDeletionRequest(
                supportPackageFileSystemProperties.getS3bucket().getName(),
                s3KeyPath,
                supportPackage.getId(),
                fileType,
                systemManagement.getTenantMetadata().getTenantId()
        );

        publishDeletionRequest(deletionRequest,SnsServiceType.S3_FILE_DELETE);
    }

    /**
     * Publishes the S3FileDeletionRequest to SNS.
     *
     * @param deletionRequest the S3FileDeletionRequest
     * @throws SnsException if an error occurs while publishing the message to SNS
     */
    private void publishDeletionRequest(SnsPublishable deletionRequest, SnsServiceType snsServiceType) {
        try {
            PublishResponse response = snsServiceFactory.getInstance(snsServiceType).publishMessage(deletionRequest).join();
            LOG.debug("Successfully published message to SNS for support package deletion. Request: {}, Response: {}", deletionRequest, response);
        } catch (Exception e) {
            String errorMessage = String.format("Failed to publish message to SNS for support package deletion. Request: %s, Error: %s", deletionRequest, e.getMessage());
            throw new SnsException(errorMessage, e);
        }
    }
}
