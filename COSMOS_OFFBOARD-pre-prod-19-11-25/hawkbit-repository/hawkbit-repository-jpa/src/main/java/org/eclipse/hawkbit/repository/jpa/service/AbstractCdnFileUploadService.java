package org.eclipse.hawkbit.repository.jpa.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sns.SnsPublishable;
import org.cosmos.models.sqs.FileType;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.cosmos.sns.services.SnsServiceType;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Slf4j
public abstract class AbstractCdnFileUploadService<T extends SnsPublishable> implements CdnFileUploadService<T> {

    protected final SystemManagement systemManagement;
    protected final ISnsServiceFactory snsServiceFactory;
    protected final TenantAware tenantAware;
    protected final TenantConfigurationManagement tenantConfigurationManagement;

    /**
     * Constructor for AbstractCdnFileUploadService.
     *
     * @param systemManagement              the system management service
     * @param snsServiceFactory             the SNS service factory
     * @param tenantAware                   the tenant-aware service
     * @param tenantConfigurationManagement the tenant configuration management service
     */
    protected AbstractCdnFileUploadService(SystemManagement systemManagement,
                                           ISnsServiceFactory snsServiceFactory,
                                           TenantAware tenantAware,
                                           TenantConfigurationManagement tenantConfigurationManagement) {
        this.systemManagement = systemManagement;
        this.snsServiceFactory = snsServiceFactory;
        this.tenantAware = tenantAware;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
    }

    /**
     * Retrieves the CDN URL for the current tenant. If the tenant-specific CDN is not available,
     * it falls back to a globally configured default CDN.
     *
     * @return the CDN URL
     */
    protected String getTenantCdn() {
        final String cdn = systemManagement.getTenantCdn();
        return StringUtils.hasText(cdn)
                ? cdn
                : tenantConfigurationManagement.getGlobalConfigurationValue(
                TenantConfigurationProperties.TenantConfigurationKey.DEFAULT_CDN, String.class
        );
    }

    /**
     * Constructs the S3 absolute path for the file using tenant, file type, SHA256 hash, and file name.
     *
     * @param sha256   the SHA256 hash of the file
     * @param fileName the name of the file
     * @param fileType the type of the file
     * @return the constructed S3 absolute file path
     */
    protected String buildS3AbsolutePathFileName(String sha256, String fileName, FileType fileType) {
        return tenantAware.getCurrentTenant() + Constants.FILE_SEPARATOR + fileType.getType() +
                Constants.FILE_SEPARATOR + sha256 + Constants.FILE_SEPARATOR + fileName;
    }

    /**
     * Generates the CDN path for the given tenant, file hash, file type, and directory template.
     *
     * @param tenant           the tenant ID
     * @param sha256           the SHA256 hash of the file
     * @param fileType         the type of the file
     * @param directoryTemplate the directory template
     * @return the constructed CDN path
     */
    protected String generateCdnPath(String tenant, String sha256, FileType fileType, String directoryTemplate) {
        return directoryTemplate
                .replace("{tenant}", tenant)
                .replace("{type}", fileType.getType())
                .replace("{SHA256}", sha256.replaceAll("(.{2})", "$1/"));
    }

    /**
     * Handles the file upload to the CDN by publishing a message to an SNS topic.
     *
     * @param file             the file to upload
     * @param cdnUploadRequest the CDN upload request containing metadata and upload details
     */
    protected void handleCdnUpload(T file, CdnUploadRequest cdnUploadRequest) {
        try {
            PublishResponse message = snsServiceFactory.getInstance(SnsServiceType.CDN_UPLOAD)
                    .publishMessage(cdnUploadRequest)
                    .join();
            log.debug("Message published to SNS for uploading to CDN successfully with messageId: {}", message.messageId());
            updateFileStatus(file);
        } catch (Exception e) {
            log.error("Failed to publish message {} to SNS for Cdn upload with the reason {}", cdnUploadRequest, e.getMessage());
        }
    }

    /**
     * Abstract method to update the file status after initiating the upload process.
     *
     * @param file the file whose status is to be updated
     */
    protected abstract void updateFileStatus(T file);

}
