package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.S3Repository;
import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.api.DeploymentLogUrlProperties;
import org.eclipse.hawkbit.repository.DeploymentLogManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.JpaDeploymentLog;
import org.eclipse.hawkbit.repository.jpa.service.S3FileUtil;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.DeploymentLogUpload;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.ValidationException;
import java.util.Optional;

/**
 * JPA implementation of {@link DeploymentLogManagement} for handling deployment log uploads and metadata storage.
 */
@Transactional(readOnly = true)
@Validated
public class JpaDeploymentLogManagement implements DeploymentLogManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaDeploymentLogManagement.class);

    private static final Long REQUESTED_DEPLOYMENT_LOG_FILE_COUNT = 1L;

    private final TenantAware tenantAware;

    private final DeploymentLogRepository deploymentLogRepository;

    private final QuotaManagement quotaManagement;

    private final S3MultipartFileUpload s3MultipartFileUpload;

    private final S3Repository s3Repository;

    private final DeploymentLogUrlProperties deploymentLogUrlProperties;

    @Value("${cosmos.server.s3.deployment.log.bucket.name}")
    private String deploymentLogBucketName;

    public JpaDeploymentLogManagement(
            final TenantAware tenantAware,
            final DeploymentLogRepository deploymentLogRepository,
            final QuotaManagement quotaManagement,
            final S3MultipartFileUpload s3MultipartFileUpload,
            final S3Repository s3Repository,
            final DeploymentLogUrlProperties deploymentLogUrlProperties
    ) {
        this.tenantAware = tenantAware;
        this.deploymentLogRepository = deploymentLogRepository;
        this.quotaManagement = quotaManagement;
        this.s3MultipartFileUpload = s3MultipartFileUpload;
        this.s3Repository = s3Repository;
        this.deploymentLogUrlProperties = deploymentLogUrlProperties;
    }

    /**
     * Returns the total number of deployment log entries stored in the repository.
     * <p>
     * This method delegates to the underlying {@link DeploymentLogRepository} to count all deployment log records.
     * It is typically used for monitoring, quota enforcement, or administrative purposes to determine the current
     * number of deployment logs managed by the system.
     *
     * @return the total count of deployment log entries in the repository
     */
    @Override
    public long count() {
        return deploymentLogRepository.count();
    }

    /**
     * Creates a new deployment log entry and uploads the associated file to S3.
     * <p>
     * This method performs the following steps:
     * <ol>
     *   <li>Validates the deployment log upload request for constraints such as duplicate file chunk, maximum file count, total file size, and individual file size.</li>
     *   <li>Uploads the provided multipart file to S3, generating a key path and calculating the SHA-256 hash for integrity verification.</li>
     *   <li>Stores the deployment log metadata in the repository, including file details, S3 path, and hash.</li>
     * </ol>
     * <p>
     * The method is transactional and will retry on {@link org.springframework.dao.ConcurrencyFailureException} up to the configured maximum attempts.
     *
     * @param deploymentLogUpload the metadata for the deployment log upload, including action, filename, sequence, file size, byte size, range, and flags
     * @param file                the multipart file to be uploaded and logged
     * @return the created {@link DeploymentLog} entity containing metadata and storage information
     * @throws jakarta.validation.ValidationException if any validation constraint is violated (duplicate, quota exceeded, etc.)
     * @throws org.cosmos.s3.exception.S3Exception  if the file upload to S3 fails
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public DeploymentLog create(final DeploymentLogUpload deploymentLogUpload, MultipartFile file) throws S3Exception {
        validateDeploymentLogUpload(deploymentLogUpload);
        uploadDeploymentLogToS3(file, deploymentLogUpload);
        return storeDeploymentLogMetadata(deploymentLogUpload);
    }

    /**
     * Validates the deployment log upload request for various constraints.
     * <p>
     * This method performs the following checks:
     * <ul>
     *   <li>Duplicate file chunk: Ensures that a file chunk with the same action ID, filename, and sequence does not already exist.</li>
     *   <li>Maximum file count: Ensures that the total number of files uploaded for the given action does not exceed the allowed limit.</li>
     *   <li>Maximum total file size: Ensures that the cumulative size of all files for the action does not exceed the allowed limit.</li>
     *   <li>Maximum individual file size: Ensures that the size of the current file chunk does not exceed the allowed limit.</li>
     * </ul>
     * If any validation fails, a {@link ValidationException} is thrown.
     *
     * @param deploymentLogUpload the deployment log upload metadata containing action, filename, sequence, byte size, and rollout information
     * @throws ValidationException if any validation constraint is violated
     */
    private void validateDeploymentLogUpload(final DeploymentLogUpload deploymentLogUpload) {
        final Long actionId = deploymentLogUpload.getAction().getId();
        final String filename = deploymentLogUpload.getFilename();
        final Integer sequence = deploymentLogUpload.getSequence();
        final Long fileSize = deploymentLogUpload.getFileSize();
        final Rollout rollout = deploymentLogUpload.getAction().getRollout();

        validateDuplicateFile(actionId, filename, sequence);
        validateFileCount(actionId, rollout.getLogMaxNumberOfFiles());
        validateTotalFileSize(actionId, rollout.getLogMaxAllFileSize(), fileSize);
        validateIndividualFileSize(actionId, rollout.getLogMaxEachFileSize(), fileSize);
    }

    /**
     * Validates whether a deployment log file chunk with the specified action ID, filename, and sequence already exists.
     * <p>
     * This method checks for duplicate file chunk uploads to prevent overwriting or redundant storage.
     * If a duplicate is detected, it logs an error and throws a {@link ValidationException}.
     *
     * @param actionId the ID of the deployment action associated with the log file
     * @param filename the name of the file being uploaded
     * @param sequence the sequence number of the file chunk
     * @throws ValidationException if a duplicate file chunk is found for the given action ID, filename, and sequence
     */
    private void validateDuplicateFile(Long actionId, String filename, Integer sequence) {
        LOG.debug("Validating duplicate file: actionId={}, filename={}, sequence={}", actionId, filename, sequence);
        if (checkDeploymentLogExists(actionId, filename, sequence)) {
            LOG.error("Duplicate file chunk detected for actionId={}, filename={}, sequence={}", actionId, filename, sequence);
            throw new ValidationException(String.format(
                    "Duplicate file chunk was uploaded for actionId=%d, filename=%s, sequence=%d",
                    actionId, filename, sequence));
        }
        LOG.debug("Duplicate file check validation passed for actionId={}, filename={}, sequence={}", actionId, filename, sequence);
    }


    /**
     * Validates that the number of deployment log files uploaded for a given action does not exceed the allowed maximum.
     * <p>
     * This method checks if the total number of files (including the current upload request) for the specified action ID
     * exceeds the provided maximum file count limit. If the limit is reached or exceeded, it logs an error and throws a
     * {@link ValidationException} to prevent further uploads.
     *
     * @param actionId            the ID of the deployment action for which the file count is being validated
     * @param logMaxNumberOfFiles the maximum allowed number of deployment log files for the action
     * @throws ValidationException if the total number of files exceeds the allowed maximum
     */
    private void validateFileCount(Long actionId, Integer logMaxNumberOfFiles) {
        LOG.debug("Validating file count for actionId={}, max allowed files={}", actionId, logMaxNumberOfFiles);
        if (isMaxDeploymentLogFileUploadReached(actionId, logMaxNumberOfFiles)) {
            LOG.error("Max deployment log file count reached for actionId={}. Limit: {}", actionId, logMaxNumberOfFiles);
            throw new ValidationException(String.format("Max deployment log file count reached for actionId=%d. Limit: %d",
                    actionId, logMaxNumberOfFiles));
        }
        LOG.debug("File count validation passed for actionId={}", actionId);
    }

    /**
     * Validates that the cumulative size of all deployment log files for a given action does not exceed the allowed maximum.
     * <p>
     * This method checks if the total byte size of all files (including the current upload request) for the specified action ID
     * exceeds the provided maximum total file size limit. If the limit is exceeded, it logs an error and throws a
     * {@link ValidationException} to prevent further uploads.
     *
     * @param actionId          the ID of the deployment action for which the total file size is being validated
     * @param logMaxAllFileSize the maximum allowed total size (in bytes) of all deployment log files for the action
     * @param fileSize          the size of the current file chunk being uploaded
     * @throws ValidationException if the cumulative file size exceeds the allowed maximum
     */
    private void validateTotalFileSize(Long actionId, Integer logMaxAllFileSize, Long fileSize) {
        LOG.debug("Validating total file size for actionId: {}, logMaxAllFileSize: {}, fileSize: {}", actionId, logMaxAllFileSize, fileSize);
        if (isDeploymentLogTotalFileSizeExceeded(logMaxAllFileSize, fileSize)) {
            LOG.error("Max total file size exceeded for actionId={}. Limit: {}", actionId, logMaxAllFileSize);
            throw new ValidationException(String.format("Max total file size exceeded for actionId=%d. Limit: %d",
                    actionId, logMaxAllFileSize));
        }
        LOG.info("Total file size validation passed for actionId: {}", actionId);
    }

    /**
     * Validates that the size of the individual deployment log file chunk does not exceed the allowed maximum.
     * <p>
     * This method checks if the byte size of the current file chunk for the specified action ID
     * exceeds the provided maximum individual file size limit. If the limit is exceeded, it logs an error and throws a
     * {@link ValidationException} to prevent the upload.
     *
     * @param actionId           the ID of the deployment action for which the individual file size is being validated
     * @param logMaxEachFileSize the maximum allowed size (in bytes) for a single deployment log file chunk
     * @param fileSize           the byte size of the current file chunk being uploaded
     * @throws ValidationException if the individual file size exceeds the allowed maximum
     */
    private void validateIndividualFileSize(Long actionId, Integer logMaxEachFileSize, Long fileSize) {
        LOG.debug("Validating file size for actionId: {}, maxEachFileSize: {}, fileSize: {}", actionId, logMaxEachFileSize, fileSize);
        if (isDeploymentLogIndividualFileSizeExceeded(logMaxEachFileSize, fileSize)) {
            LOG.error("Max individual file size exceeded for actionId={}. Limit: {}", actionId, logMaxEachFileSize);
            throw new ValidationException(String.format("Max individual file size exceeded for actionId=%d. Limit: %d",
                    actionId, logMaxEachFileSize));
        }
        LOG.info("Individual file size validation passed for actionId: {}", actionId);
    }

    /**
     * Uploads a deployment log file to S3 and logs the upload metadata.
     * <p>
     * This method extracts metadata from the provided {@link DeploymentLogUpload} object,
     * generates the S3 key path, builds the S3 file upload configuration, and uploads the file
     * to S3 using multipart upload. It also calculates the SHA-256 hash of the uploaded file
     * for integrity verification.
     * <p>
     * The extracted metadata includes rollout ID, action ID, filename, original filename,
     * file size, sequence number, byte size, byte range, and flags indicating if this is the
     * last chunk or last file. The S3 key path is generated based on rollout ID, action ID,
     * and filename. The file is uploaded to S3, and the SHA-256 hash is computed and logged.
     * <p>
     * If the upload fails, the method deletes any partially uploaded file from S3 and throws
     * an {@link S3Exception}.
     *
     * @param file                the multipart file to upload
     * @param deploymentLogUpload the deployment log upload metadata containing action, rollout, filename, sequence, file size, byte size, range, and flags
     * @throws S3Exception if the file upload to S3 fails
     */
    private void uploadDeploymentLogToS3(MultipartFile file, DeploymentLogUpload deploymentLogUpload) throws S3Exception {
        final String tenant = deploymentLogUpload.getAction().getTenant().toUpperCase();
        final Long rolloutId = deploymentLogUpload.getAction().getRollout().getId();
        final Long actionId = deploymentLogUpload.getAction().getId();
        final String filename = deploymentLogUpload.getFilename();
        final String originalFileName = deploymentLogUpload.getFileOriginalName();
        final Long fileSize = deploymentLogUpload.getFileSize();
        final Integer sequence = deploymentLogUpload.getSequence();
        final Boolean isLastFile = deploymentLogUpload.getIsLastFile();

        LOG.debug("Extracted metadata - rolloutId: {}, actionId: {}, filename: {}, originalFileName: {}, fileSize: {}, sequence: {}, isLastFile: {}",
                rolloutId, actionId, filename, originalFileName, fileSize, sequence, isLastFile);

        String s3KeyPath = deploymentLogUrlProperties.getS3().getDirectory()
                .replace("{tenant}", tenant)
                .replace("{rolloutId}", String.valueOf(rolloutId))
                .replace("{actionId}", String.valueOf(actionId));

        deploymentLogUpload.setFilePath(s3KeyPath);

        LOG.debug("Generated S3 key path: {}", s3KeyPath);

        S3FileUpload s3FileUpload = buildS3FileUploadObject(filename, s3KeyPath);

        LOG.info("Preparing to upload deployment log file. Bucket: {}, Key: {}, Filename: {}, Original filename: {}",
                deploymentLogBucketName, s3KeyPath, filename, originalFileName);

        String sha256Hex = s3MultipartFileUpload.uploadFileToS3(file, s3FileUpload);
        deploymentLogUpload.setProvidedSha256Sum(sha256Hex);

        LOG.debug("File uploaded to S3. SHA-256: {}", sha256Hex);

    }

    /**
     * Builds an {@link S3FileUpload} object for the given filename and S3 key path.
     * <p>
     * This method uses the configured S3 bucket name and the provided filename and key path
     * to construct an {@link S3FileUpload} instance using its builder.
     *
     * @param filename  the name of the file to be uploaded to S3
     * @param s3KeyPath the S3 key path where the file will be stored
     * @return a configured {@link S3FileUpload} object for the file upload
     */
    private S3FileUpload buildS3FileUploadObject(String filename, String s3KeyPath) {
        return S3FileUpload.builder()
                .bucketName(deploymentLogBucketName)
                .filename(filename)
                .keyPath(s3KeyPath)
                .build();
    }

    /**
     * Checks if the maximum allowed number of deployment log files for a given action has been reached.
     * <p>
     * This method retrieves the current count of deployment log files associated with the specified action ID
     * from the repository, adds the count for the current upload request, and compares the total to the provided
     * maximum file count limit. If the total exceeds the allowed maximum, it returns {@code true}, indicating
     * that no more files can be uploaded for this action.
     * <p>
     * This validation is used to enforce quota limits and prevent excessive file uploads for a deployment action.
     *
     * @param actionId   the ID of the deployment action for which the file count is being checked
     * @param logMaxFile the maximum allowed number of deployment log files for the action
     * @return {@code true} if the total number of files (including the current request) exceeds the allowed maximum; {@code false} otherwise
     */
    private boolean isMaxDeploymentLogFileUploadReached(Long actionId, Integer logMaxFile) {
        Optional<Long> deploymentLogFileCount = deploymentLogRepository.findDeploymentLogCountbyActionId(actionId);
        long totalDeploymentLogFileCount = deploymentLogFileCount.orElse(0L) + REQUESTED_DEPLOYMENT_LOG_FILE_COUNT;
        return totalDeploymentLogFileCount > logMaxFile;
    }

    /**
     * Checks if the cumulative size of all deployment log files for an action exceeds the allowed maximum.
     * <p>
     * This method compares the provided byte size (which may represent the total size of files being uploaded)
     * against the maximum allowed total file size for all deployment log files associated with a deployment action.
     * If the provided byte size is greater than the allowed maximum, the method returns {@code true}, indicating
     * that the upload would exceed the quota.
     *
     * @param logMaxAllFileSize the maximum allowed total size (in bytes) for all deployment log files for the action
     * @param providedFileSize  the cumulative byte size of files being considered for upload
     * @return {@code true} if the provided byte size exceeds the allowed maximum; {@code false} otherwise
     */
    private boolean isDeploymentLogTotalFileSizeExceeded(Integer logMaxAllFileSize, Long providedFileSize) {
        return providedFileSize > logMaxAllFileSize.longValue();
    }

    /**
     * Checks if the size of an individual deployment log file chunk exceeds the allowed maximum.
     * <p>
     * This method compares the provided byte size of a single file chunk against the maximum allowed size
     * for an individual deployment log file chunk. If the provided byte size is greater than the allowed maximum,
     * the method returns {@code true}, indicating that the upload would exceed the quota for a single file.
     *
     * @param logMaxEachFileSize the maximum allowed size (in bytes) for a single deployment log file chunk
     * @param providedFileSize   the size of the file chunk being considered for upload
     * @return {@code true} if the provided byte size exceeds the allowed maximum; {@code false} otherwise
     */
    private boolean isDeploymentLogIndividualFileSizeExceeded(Integer logMaxEachFileSize, Long providedFileSize) {
        return providedFileSize > logMaxEachFileSize.longValue();
    }

    /**
     * Stores deployment log metadata in the repository.
     * <p>
     * This method creates or updates a {@link JpaDeploymentLog} entity using the metadata provided
     * in the {@link DeploymentLogUpload} object. It extracts all relevant fields such as filename,
     * original filename, file size, byte size, sequence, byte range, chunk and file flags, SHA-256 hash,
     * file path, and action ID. If an existing deployment log is found (currently always null, but can be
     * replaced with a lookup), it updates the entity; otherwise, it creates a new instance.
     * <p>
     * The method then persists the entity using the {@link DeploymentLogRepository} and returns the saved
     * {@link DeploymentLog} object.
     *
     * @param deploymentLogUpload the deployment log upload metadata containing all necessary fields for storage
     * @return the saved {@link DeploymentLog} entity with updated metadata
     */
    private DeploymentLog storeDeploymentLogMetadata(final DeploymentLogUpload deploymentLogUpload) {
        final String providedFilename = deploymentLogUpload.getFilename();
        final String originalFileName = deploymentLogUpload.getFileOriginalName();
        final Long fileSize = deploymentLogUpload.getFileSize();
        final Integer sequence = deploymentLogUpload.getSequence();
        final Boolean isLastFile = deploymentLogUpload.getIsLastFile();
        final String sha256Hash = deploymentLogUpload.getProvidedSha256Sum();
        final String filePath = deploymentLogUpload.getFilePath();
        final Long actionId = deploymentLogUpload.getAction().getId();
        final Long byteSize = deploymentLogUpload.getByteSize();
        final Long range = deploymentLogUpload.getRange();
        final Boolean isLastChunk = deploymentLogUpload.getIsLastChunk();
        final DeploymentLog existingDeploymentLog = null; // Replace with actual lookup if needed

        LOG.info("Storing deployment log metadata: providedFilename={}, originalFileName={}, actionId={}, sequence={}, fileSize={}, isLastFile={}, sha256Hash={}, filePath={}",
                providedFilename, originalFileName, actionId, sequence, fileSize, isLastFile, sha256Hash, filePath);

        JpaDeploymentLog deploymentLog = (JpaDeploymentLog) existingDeploymentLog;
        if (existingDeploymentLog == null) {
            LOG.debug("No existing deployment log found, creating new JpaDeploymentLog instance.");
            deploymentLog = new JpaDeploymentLog(actionId, providedFilename, sequence, fileSize, byteSize, range,
                    isLastChunk, isLastFile, sha256Hash, filePath);
        } else {
            LOG.debug("Updating existing deployment log with new metadata.");
        }
        deploymentLog.setAction(actionId);
        deploymentLog.setFileName(providedFilename);
        deploymentLog.setFileOriginalName(originalFileName);
        deploymentLog.setSequence(sequence);
        deploymentLog.setFileSize(fileSize);
        deploymentLog.setByteSize(byteSize);
        deploymentLog.setByteRange(range);
        deploymentLog.setIsLastChunk(isLastChunk);
        deploymentLog.setIsLastFile(isLastFile);
        deploymentLog.setSha256Hash(sha256Hash);
        deploymentLog.setFilePath(filePath);
        LOG.debug("DeploymentLog after metadata update: {}", deploymentLog);
        DeploymentLog savedLog = deploymentLogRepository.save(deploymentLog);
        LOG.info("DeploymentLog saved to repository: fileName={}", savedLog.getFileName());
        return savedLog;
    }

    /**
     * Checks if a deployment log entry exists for the given action ID, file name, and sequence number.
     * <p>
     * This method queries the underlying {@link DeploymentLogRepository} to determine whether a deployment log
     * file chunk has already been uploaded for the specified action, file name, and sequence. It is used to
     * prevent duplicate uploads and enforce uniqueness constraints for deployment log files.
     *
     * @param actionId the ID of the deployment action associated with the log file
     * @param fileName the name of the file being checked
     * @param sequence the sequence number of the file chunk
     * @return {@code true} if a deployment log entry exists for the given parameters; {@code false} otherwise
     */
    @Override
    public boolean checkDeploymentLogExists(Long actionId, String fileName, Integer sequence) {
        return deploymentLogRepository.deploymentLogExistsForActionIdFileNameAndSequence(actionId, fileName, sequence);
    }

    /**
     * Finds a deployment log entry by its unique ID.
     * <p>
     * This method retrieves a {@link DeploymentLog} entity from the repository using the provided deployment log ID.
     * If no entry is found, it throws an {@link EntityNotFoundException}. This is typically used to fetch metadata
     * or details about a specific deployment log file.
     *
     * @param deploymentLogId the unique identifier of the deployment log entry to retrieve
     * @return the {@link DeploymentLog} entity corresponding to the given ID
     * @throws EntityNotFoundException if no deployment log entry exists for the provided ID
     */
    @Override
    public DeploymentLog findDeploymentLogById(Long deploymentLogId) {
        return deploymentLogRepository.findById(deploymentLogId)
                .orElseThrow(() -> new EntityNotFoundException(DeploymentLog.class, deploymentLogId));
    }

}
