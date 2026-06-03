package org.eclipse.hawkbit.repository.jpa.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.services.ISnsServiceFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SupportPackageCdnFileUploadService extends AbstractCdnFileUploadService<BaseSupportPackage> {

    @Value("${cosmos.server.s3.support-package.bucket.name}")
    private String supportPackageBucketName;

    private final SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties;
    private final RspRepository rspRepository;
    private final EspRepository espRepository;

    /**
     * Constructs a new {@code SupportPackageCdnFileUploadService}.
     *
     * @param systemManagement                   the system management service
     * @param snsServiceFactory                  the SNS service factory
     * @param tenantAware                        the tenant-aware service
     * @param tenantConfigurationManagement      the tenant configuration management service
     * @param supportPackageUrlHandlerProperties the properties for support package URL handling
     * @param rspRepository                      the repository for RSP entities
     * @param espRepository                      the repository for ESP entities
     */
    @Autowired
    public SupportPackageCdnFileUploadService(SystemManagement systemManagement,
                                              ISnsServiceFactory snsServiceFactory,
                                              TenantAware tenantAware,
                                              TenantConfigurationManagement tenantConfigurationManagement,
                                              SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties,
                                              RspRepository rspRepository,
                                              EspRepository espRepository) {
        super(systemManagement, snsServiceFactory, tenantAware, tenantConfigurationManagement);
        this.supportPackageUrlHandlerProperties = supportPackageUrlHandlerProperties;
        this.rspRepository = rspRepository;
        this.espRepository = espRepository;
    }

    /**
     * Uploads a support package to the configured CDN.
     *
     * @param supportPackage the support package to upload
     * @throws IllegalArgumentException if the support package is invalid or has unsupported file types
     */
    @Override
    public void uploadFile(BaseSupportPackage supportPackage) throws IllegalArgumentException {

        FileType fileType = resolveFileType(supportPackage);
        String cdnDirectory = getCdnDirectory(fileType);
        String s3AbsolutePathFileName = buildS3AbsolutePathFileName(supportPackage.getSha256Hash(), supportPackage.getFileName(), fileType);
        String cdnPath = generateCdnPath(supportPackage.getTenant(), supportPackage.getSha256Hash(), fileType, cdnDirectory);

        CdnUploadRequest request = CdnUploadRequest.builder()
                .bucketName(supportPackageBucketName)
                .s3FileName(s3AbsolutePathFileName)
                .cdnFileName(supportPackage.getFileName())
                .cdn(getTenantCdn())
                .cdnDirPath(cdnPath)
                .fileType(fileType.name())
                .tenantId(systemManagement.getTenantMetadata().getTenantId())
                .fileId(supportPackage.getId())
                .build();

        handleCdnUpload(supportPackage, request);
    }

    /**
     * Resolves the {@link FileType} from the support package.
     *
     * @param supportPackage the support package
     * @return the resolved file type
     * @throws IllegalArgumentException if the file type is unsupported
     */
    private FileType resolveFileType(BaseSupportPackage supportPackage) throws IllegalArgumentException {
        try {
            return FileType.valueOf(supportPackage.getFileType().getCategory());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported support package type: " + supportPackage.getFileType(), e);
        }
    }

    /**
     * Gets the CDN directory path for the specified file type.
     *
     * @param fileType the file type
     * @return the CDN directory path
     * @throws IllegalArgumentException if the file type is unsupported
     */
    private String getCdnDirectory(FileType fileType) {
        return switch (fileType) {
            case RSP -> {
                String rspDir = supportPackageUrlHandlerProperties.getRsp().getCdn().getDirectory();
                log.debug("Resolved RSP CDN directory: {}", rspDir);
                yield rspDir;
            }
            case ESP -> {
                String espDir = supportPackageUrlHandlerProperties.getEsp().getCdn().getDirectory();
                log.debug("Resolved ESP CDN directory: {}", espDir);
                yield espDir;
            }
            default -> throw new IllegalArgumentException("Unsupported support package type: " + fileType);
        };
    }

    /**
     * Updates the status of the support package to indicate it is uploading to the CDN.
     *
     * @param file the support package to update
     */
    @Override
    protected void updateFileStatus(BaseSupportPackage file) {
        FileType fileType = resolveFileType(file);
        log.debug("Updating file status for file ID: {}, Type: {}", file.getId(), fileType);

        if (fileType == FileType.RSP) {
            updateRspFileStatus((JpaRsp) file);
        } else if (fileType == FileType.ESP) {
            updateEspFileStatus((JpaEsp) file);
        }
    }

    /**
     * Updates the status of an RSP file.
     *
     * @param rsp the RSP file
     */
    private void updateRspFileStatus(JpaRsp rsp) {
        rsp.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.toString());
        rspRepository.save(rsp);
        log.info("Updated RSP file status to UPLOADING_TO_CDN for file ID: {}", rsp.getId());
    }

    /**
     * Updates the status of an ESP file.
     *
     * @param esp the ESP file
     */
    private void updateEspFileStatus(JpaEsp esp) {
        esp.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.toString());
        espRepository.save(esp);
        log.info("Updated ESP file status to UPLOADING_TO_CDN for file ID: {}", esp.getId());
    }
}
