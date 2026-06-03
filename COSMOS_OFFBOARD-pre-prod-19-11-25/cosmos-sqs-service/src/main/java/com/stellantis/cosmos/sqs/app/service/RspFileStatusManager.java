package com.stellantis.cosmos.sqs.app.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for managing file statuses of RSP entities.
 * <p>
 * Updates the status and MD5 hash of RSP files in the repository.
 * Implements {@link FileStatusManagement}
 */
@Slf4j
@Service
public final class RspFileStatusManager implements FileStatusManagement {

    private final RspRepository rspRepository;
    private final ChecksumCalculator checksumCalculator;
    private final S3Repository s3Repository;
    private final SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties;
    private final SupportPackageManagement supportPackageManagement;

    @Value("${cosmos.server.s3.support-package.bucket.name}")
    private String bucketName;

    @Autowired
    public RspFileStatusManager(RspRepository rspRepository,
                                ChecksumCalculator checksumCalculator,
                                SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties,
                                S3Repository s3Repository,
                                SupportPackageManagement supportPackageManagement) {
        this.rspRepository = Objects.requireNonNull(rspRepository, "rspRepository must not be null");
        this.checksumCalculator = Objects.requireNonNull(checksumCalculator, "checksumCalculator must not be null");
        this.s3Repository = Objects.requireNonNull(s3Repository, "s3Repository must not be null");
        this.supportPackageUrlHandlerProperties = Objects.requireNonNull(supportPackageUrlHandlerProperties, "supportPackageUrlHandlerProperties must not be null");
        this.supportPackageManagement = Objects.requireNonNull(supportPackageManagement, "supportPackageManagement must not be null");
    }

    /**
     * Retrieves a {@link JpaRsp} entity by its ID.
     * <p>
     * Throws an {@link EntityNotFoundException} if RSP does not exist.
     *
     * @param fileId the unique identifier of the RSP
     * @return the {@link JpaRsp} entity
     * @throws EntityNotFoundException if no RSP is found for the given ID
     */
    private JpaRsp getRspOrThrow(Long fileId) {
        return rspRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("RSP not found for fileId: {}", fileId);
                    return new EntityNotFoundException(Rsp.class, fileId);
                });
    }

    /**
     * Updates the status and MD5 hash of an RSP file in the repository after a successful file transfer.
     * <p>
     * This method retrieves the RSP by its ID, updates its file size (fetched from S3),
     * sets the new file status, and updates the MD5 hash (converted from Base64 to hex).
     * The updated RSP is then saved back to the repository.
     *
     * @param fileId the unique identifier of the RSP file
     * @param status the new status to set for the RSP file
     * @param md5    the MD5 hash of the file, encoded in Base64
     * @param tenant the tenant identifier for multi-tenancy support
     */
    @Override
    public void updateFileStatus(Long fileId, String status, String md5, String tenant) {
        JpaRsp rsp = getRspOrThrow(fileId);
        rsp.setFileSize(fetchFileSizeFromS3(rsp.getSha256Hash(), rsp.getFileName(), tenant));
        rsp.setFileStatus(status);
        rsp.setMd5Hash(checksumCalculator.convertBase64ToHex(md5));
        Rsp savedRsp = rspRepository.save(rsp);
        log.debug("RSP updated and saved: {}", savedRsp);

        //Support package is successfully uploaded to storage, now generate and cache signatures with all the available certificate configurations
        supportPackageManagement.generateAndCacheSignaturesForSupportPackage(rsp.getId(), rsp.getSha256Hash(), rsp.getFileType(), null);
    }

    /**
     * Updates the status of an RSP file in the repository when a deletion or transfer operation fails.
     * <p>
     * Retrieves the RSP by its ID, sets the new file status, and saves the updated entity.
     *
     * @param fileId the unique identifier of the RSP file
     * @param status the new status to set for the RSP file
     * @throws EntityNotFoundException if no RSP is found for the given ID
     */
    @Override
    public void updateFileStatus(Long fileId, String status) {
        JpaRsp rsp = getRspOrThrow(fileId);
        rsp.setFileStatus(status);
        Rsp savedRsp = rspRepository.save(rsp);
        log.debug("RSP updated and saved: {}", savedRsp);
    }

    /**
     * Retrieves the file name associated with the specified RSP file ID.
     * <p>
     * Looks up the RSP in the repository by its ID and returns its file name.
     * If the RSP does not exist, an {@link EntityNotFoundException} is thrown and an error is logged.
     *
     * @param fileId the unique identifier of the RSP file
     * @return the file name of the RSP
     * @throws EntityNotFoundException if no RSP is found for the given ID
     */
    @Override
    public String getFileName(Long fileId) {
        return rspRepository.findById(fileId)
                .map(JpaRsp::getFileName)
                .orElseThrow(() -> {
                    log.error("Entity not found for rsp with id:{}", fileId);
                    return new EntityNotFoundException(Rsp.class, fileId);
                });
    }

    /**
     * Returns the file type managed by this service.
     * <p>
     * This implementation always returns {@link FileType#RSP}, indicating that
     * this manager handles RSP files.
     *
     * @return the {@link FileType} representing RSP files
     */
    @Override
    public FileType getFileType() {
        return FileType.RSP;
    }

    /**
     * Fetches the size of an RSP file stored in S3.
     * <p>
     * Constructs an {@link S3FileUpload} object using the provided SHA-256 hash, file name,
     * tenant identifier, and S3 bucket information, then queries the S3 repository for the file size.
     * <p>
     * This method is typically used to update RSP metadata after file upload or to verify file integrity.
     *
     * @param sha256   the SHA-256 hash of the RSP file, used to generate the S3 key
     * @param fileName the name of the RSP file in S3
     * @param tenant   the tenant identifier for multi-tenancy support
     * @return the size of the file in bytes
     */
    @Override
    public Long fetchFileSizeFromS3(String sha256, String fileName, String tenant) {
        S3FileUpload s3FileUpload = S3FileUtil.buildS3FileUpload(
                supportPackageUrlHandlerProperties.getRsp().getS3().getDirectory(),
                tenant, sha256, bucketName, fileName, getFileType().getType());
        return s3Repository.getFileSizeFromS3(s3FileUpload);
    }
}