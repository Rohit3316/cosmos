package com.stellantis.cosmos.sqs.app.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.EspEcuRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Service for managing file statuses of ESP entities.
 * <p>
 * Updates the status and MD5 hash of ESP files in the repository.
 * Implements {@link FileStatusManagement}
 */
@Slf4j
@Service
public final class EspFileStatusManager implements FileStatusManagement {

    private final EspRepository espRepository;
    private final ChecksumCalculator checksumCalculator;
    private final SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties;
    private final S3Repository s3Repository;
    private final SupportPackageManagement supportPackageManagement;

    @Value("${cosmos.server.s3.support-package.bucket.name}")
    private String bucketName;

    @Autowired
    public EspFileStatusManager(EspRepository espRepository,
                                ChecksumCalculator checksumCalculator,
                                SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties,
                                S3Repository s3Repository,
                                SupportPackageManagement supportPackageManagement) {
        this.espRepository = Objects.requireNonNull(espRepository, "espRepository must not be null");
        this.checksumCalculator = Objects.requireNonNull(checksumCalculator, "checksumCalculator must not be null");
        this.s3Repository = Objects.requireNonNull(s3Repository, "s3Repository must not be null");
        this.supportPackageUrlHandlerProperties = Objects.requireNonNull(supportPackageUrlHandlerProperties, "supportPackageUrlHandlerProperties must not be null");
        this.supportPackageManagement = Objects.requireNonNull(supportPackageManagement, "supportPackageManagement must not be null");
    }

    /**
     * Retrieves a {@link JpaEsp} entity by its ID.
     * <p>
     * Throws an {@link EntityNotFoundException} if ESP does not exist.
     *
     * @param fileId the unique identifier of the ESP
     * @return the {@link JpaEsp} entity
     * @throws EntityNotFoundException if no ESP is found for the given ID
     */
    private JpaEsp getEspOrThrow(Long fileId) {
        return espRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("ESP not found for fileId: {}", fileId);
                    return new EntityNotFoundException(Esp.class, fileId);
                });
    }

    /**
     * Updates the status and MD5 hash of an ESP file in the repository after a successful file transfer.
     * <p>
     * This method retrieves the ESP by its ID, updates its file size (fetched from S3),
     * sets the new file status, and updates the MD5 hash (converted from Base64 to hex).
     * The updated ESP is then saved back to the repository.
     *
     * @param fileId the unique identifier of the ESP file
     * @param status the new status to set for the ESP file
     * @param md5    the MD5 hash of the file, encoded in Base64
     * @param tenant the tenant identifier for multi-tenancy support
     */
    @Override
    public void updateFileStatus(Long fileId, String status, String md5, String tenant) {
        JpaEsp esp = getEspOrThrow(fileId);
        esp.setFileSize(fetchFileSizeFromS3(esp.getSha256Hash(), esp.getFileName(), tenant));
        esp.setFileStatus(status);
        esp.setMd5Hash(checksumCalculator.convertBase64ToHex(md5));
        Esp savedEsp = espRepository.save(esp);
        log.debug("ESP updated and saved: {}", savedEsp);

        //Support package is successfully uploaded to storage, now generate and cache signatures with all the available certificate configurations
        supportPackageManagement.generateAndCacheSignaturesForSupportPackage(esp.getId(), esp.getSha256Hash(), esp.getFileType(), null);
    }

    /**
     * Updates the status of an ESP file in the repository when a deletion or transfer operation fails.
     * <p>
     * Retrieves the ESP by its ID, sets the new file status, and saves the updated entity.
     *
     * @param fileId the unique identifier of the ESP file
     * @param status the new status to set for the ESP file
     * @throws EntityNotFoundException if no ESP is found for the given ID
     */
    @Override
    public void updateFileStatus(Long fileId, String status) {
        JpaEsp esp = getEspOrThrow(fileId);
        esp.setFileStatus(status);
        Esp savedEsp = espRepository.save(esp);
        log.debug("ESP updated and saved: {}", savedEsp);
    }

    /**
     * Retrieves the file name associated with the specified ESP file ID.
     * <p>
     * Looks up the ESP in the repository by its ID and returns its file name.
     * If the ESP does not exist, an {@link EntityNotFoundException} is thrown and an error is logged.
     *
     * @param fileId the unique identifier of the ESP file
     * @return the file name of the ESP
     * @throws EntityNotFoundException if no ESP is found for the given ID
     */
    @Override
    public String getFileName(Long fileId) {
        return espRepository.findById(fileId)
                .map(JpaEsp::getFileName)
                .orElseThrow(() -> {
                    log.error("Entity not found for esp with id:{}", fileId);
                    return new EntityNotFoundException(Esp.class, fileId);
                });
    }

    /**
     * Returns the file type managed by this service.
     * <p>
     * This implementation always returns {@link FileType#ESP}, indicating that
     * this manager handles ESP files.
     *
     * @return the {@link FileType} representing ESP files
     */
    @Override
    public FileType getFileType() {
        return FileType.ESP;
    }

    /**
     * Fetches the size of an ESP file stored in S3.
     * <p>
     * Constructs an {@link S3FileUpload} object using the provided SHA-256 hash, file name,
     * tenant identifier, and S3 bucket information, then queries the S3 repository for the file size.
     * <p>
     * This method is typically used to update ESP metadata after file upload or to verify file integrity.
     *
     * @param sha256   the SHA-256 hash of the ESP file, used to generate the S3 key
     * @param fileName the name of the ESP file in S3
     * @param tenant   the tenant identifier for multi-tenancy support
     * @return the size of the file in bytes
     */
    @Override
    public Long fetchFileSizeFromS3(String sha256, String fileName, String tenant) {
        S3FileUpload s3FileUpload = S3FileUtil.buildS3FileUpload(
                supportPackageUrlHandlerProperties.getEsp().getS3().getDirectory(),
                tenant, sha256, bucketName, fileName, getFileType().getType());
        return s3Repository.getFileSizeFromS3(s3FileUpload);
    }
}