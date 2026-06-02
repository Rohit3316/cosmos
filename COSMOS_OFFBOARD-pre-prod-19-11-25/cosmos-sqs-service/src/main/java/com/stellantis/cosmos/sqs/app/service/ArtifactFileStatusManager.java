package com.stellantis.cosmos.sqs.app.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Service for managing file statuses of artifact entities.
 * <p>
 * Updates the status and MD5 hash of artifact files in the repository.
 * Implements {@link FileStatusManagement}
 */
@Slf4j
@Service
public final class ArtifactFileStatusManager implements FileStatusManagement {

    private final ArtifactsRepository artifactsRepository;
    private final ChecksumCalculator checksumCalculator;
    private final S3Repository s3Repository;
    private final ArtifactUrlHandlerProperties artifactUrlHandlerProperties;

    @Value("${cosmos.server.artifacts.s3.bucket.name}")
    private String bucketName;

    @Autowired
    public ArtifactFileStatusManager(ArtifactsRepository artifactsRepository,
                                     ChecksumCalculator checksumCalculator,
                                     ArtifactUrlHandlerProperties artifactUrlHandlerProperties,
                                     S3Repository s3Repository) {
        this.artifactsRepository = Objects.requireNonNull(artifactsRepository, "artifactsRepository must not be null");
        this.checksumCalculator = Objects.requireNonNull(checksumCalculator, "checksumCalculator must not be null");
        this.s3Repository = Objects.requireNonNull(s3Repository, "s3Repository must not be null");
        this.artifactUrlHandlerProperties = Objects.requireNonNull(artifactUrlHandlerProperties, "artifactUrlHandlerProperties must not be null");
    }

    /**
     * Retrieves a {@link JpaArtifacts} entity by its ID.
     * <p>
     * Throws an {@link EntityNotFoundException} if the artifact does not exist.
     *
     * @param fileId the unique identifier of the artifact
     * @return the {@link JpaArtifacts} entity
     * @throws EntityNotFoundException if no artifact is found for the given ID
     */
    private JpaArtifacts getArtifactOrThrow(Long fileId) {
        return artifactsRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.error("Artifact not found for fileId: {}", fileId);
                    return new EntityNotFoundException(Artifacts.class, fileId);
                });
    }

    /**
     * Updates the status and MD5 hash of an artifact file in the repository after a successful file transfer.
     * <p>
     * This method retrieves the artifact by its ID, updates its file size (fetched from S3),
     * sets the new file status, and updates the MD5 hash (converted from Base64 to hex).
     * The updated artifact is then saved back to the repository.
     *
     * @param fileId the unique identifier of the artifact file
     * @param status the new status to set for the artifact file
     * @param md5    the MD5 hash of the file, encoded in Base64
     * @param tenant the tenant identifier for multi-tenancy support
     */
    @Override
    public void updateFileStatus(Long fileId, String status, String md5, String tenant) {
        JpaArtifacts artifacts = getArtifactOrThrow(fileId);
        artifacts.setFileSize(fetchFileSizeFromS3(artifacts.getSha256Hash(), artifacts.getFileName(), tenant));
        artifacts.setFileStatus(status);
        artifacts.setMd5Hash(checksumCalculator.convertBase64ToHex(md5));
        Artifacts savedArtifacts = artifactsRepository.save(artifacts);
        log.debug("Artifact updated and saved: {}", savedArtifacts);
    }

    /**
     * Updates the status of an artifact file in the repository when a deletion or transfer operation fails.
     * <p>
     * Retrieves the artifact by its ID, sets the new file status, and saves the updated entity.
     *
     * @param fileId the unique identifier of the artifact file
     * @param status the new status to set for the artifact file
     * @throws EntityNotFoundException if no artifact is found for the given ID
     */
    @Override
    public void updateFileStatus(Long fileId, String status) {
        JpaArtifacts artifacts = getArtifactOrThrow(fileId);
        artifacts.setFileStatus(status);
        Artifacts savedArtifacts = artifactsRepository.save(artifacts);
        log.debug("Artifact updated and saved: {}", savedArtifacts);
    }

    /**
     * Retrieves the file name associated with the specified artifact file ID.
     * <p>
     * Looks up the artifact in the repository by its ID and returns its file name.
     * If the artifact does not exist, an {@link EntityNotFoundException} is thrown and an error is logged.
     *
     * @param fileId the unique identifier of the artifact file
     * @return the file name of the artifact
     * @throws EntityNotFoundException if no artifact is found for the given ID
     */
    @Override
    public String getFileName(Long fileId) {
        return artifactsRepository.findById(fileId)
                .map(JpaArtifacts::getFileName)
                .orElseThrow(() -> {
                    log.error("Entity not found for artifact with id: {}", fileId);
                    return new EntityNotFoundException(Artifacts.class, fileId);
                });
    }

    /**
     * Returns the file type managed by this service.
     * <p>
     * This implementation always returns {@link FileType#ARTIFACT}, indicating that
     * this manager handles artifact files.
     *
     * @return the {@link FileType} representing artifact files
     */
    @Override
    public FileType getFileType() {
        return FileType.ARTIFACT;
    }

    /**
     * Fetches the size of an artifact file stored in S3.
     * <p>
     * Constructs an {@link S3FileUpload} object using the provided SHA-256 hash, file name,
     * tenant identifier, and S3 bucket information, then queries the S3 repository for the file size.
     * <p>
     * This method is typically used to update artifact metadata after file upload or to verify file integrity.
     *
     * @param sha256   the SHA-256 hash of the artifact file, used to generate the S3 key
     * @param fileName the name of the artifact file in S3
     * @param tenant   the tenant identifier for multi-tenancy support
     * @return the size of the file in bytes
     */
    @Override
    public Long fetchFileSizeFromS3(String sha256, String fileName, String tenant) {
        S3FileUpload s3FileUpload = S3FileUtil.buildS3FileUpload(
                artifactUrlHandlerProperties.getS3().getDirectory(),
                tenant, sha256, bucketName, fileName, getFileType().getType().toLowerCase());
        return s3Repository.getFileSizeFromS3(s3FileUpload);
    }
}
