package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.s3.model.S3FileUpload;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.eclipse.hawkbit.exception.GenericSpServerException;

import jakarta.validation.ValidationException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Utility class for S3 file operations, including key path generation and deletion request handling.
 */
public final class S3FileUtil {

    private S3FileUtil() {
    }

    /**
     * Generates the S3 key path based on a directory template.
     *
     * @param directoryTemplate the directory template
     * @param tenant            the tenant name
     * @param sha256            the SHA256 hash of the file
     * @param fileType          the file type
     * @param fileName          the name of the file
     * @return the complete S3 key path
     */
    public static String generateS3KeyPath(String directoryTemplate, String tenant, String sha256, String fileType, String fileName) {
        return directoryTemplate
                .replace("{tenant}", tenant)
                .replace("{type}", fileType)
                .replace("{SHA256}", sha256)
                + fileName;
    }

    /**
     * Generates the S3 key path based on a directory template.
     *
     * @param directoryTemplate the directory template
     * @param tenant            the tenant name
     * @param sha256            the SHA256 hash of the file
     * @param fileType          the file type
     * @return the complete S3 key path
     */
    public static String generateS3KeyPath(String directoryTemplate, String tenant, String sha256, String fileType) {
        return directoryTemplate
                .replace("{tenant}", tenant)
                .replace("{type}", fileType)
                .replace("{SHA256}", sha256);
    }

    /**
     * Generates the S3 key path by replacing placeholders in the directory template with rolloutId, actionId, and fileName.
     *
     * @param directoryTemplate the directory template containing placeholders
     * @param rolloutId         the rollout ID to replace in the template
     * @param actionId          the action ID to replace in the template
     * @return the generated S3 key path with all placeholders replaced
     */
    public static String generateS3KeyPath(String directoryTemplate, String tenant, Long rolloutId, Long actionId) {
        return directoryTemplate
                .replace("tenant", tenant)
                .replace("{rolloutId}", String.valueOf(rolloutId))
                .replace("{actionId}", String.valueOf(actionId));
    }

    /**
     * Builds an S3FileDeletionRequest.
     *
     * @param bucketName the S3 bucket name
     * @param filePath   the file path in S3
     * @param fileId     the file ID
     * @param fileType   the file type
     * @return the S3FileDeletionRequest
     */
    public static S3FileDeletionRequest buildDeletionRequest(String bucketName, String filePath, Long fileId, String fileType, Long tenantId) {
        return S3FileDeletionRequest.builder()
                .bucketName(bucketName)
                .filePath(filePath)
                .fileId(fileId)
                .fileType(fileType)
                .tenantId(tenantId)
                .build();
    }


    /**
     * Handles the file upload to S3.
     *
     * @param sha256DigestStream the input stream for the SHA256 digest
     * @param sha256Digest       the MessageDigest instance for SHA256
     * @param s3FileUpload       the S3 file upload entity
     * @param sha256             the SHA256 hash of the file
     * @param logErrorConsumer   the consumer for logging errors
     * @param deleteFileConsumer the consumer for deleting the file from S3
     * @param fileUploadConsumer the consumer for uploading the file to S3
     */
    public static void initiateFileUploadToS3(
            InputStream sha256DigestStream,
            MessageDigest sha256Digest,
            S3FileUpload s3FileUpload,
            String sha256,
            Consumer<String> logErrorConsumer,
            Consumer<S3FileUpload> deleteFileConsumer,
            BiConsumer<S3FileUpload, InputStream> fileUploadConsumer) {

        fileUploadConsumer.accept(s3FileUpload, sha256DigestStream);
        String computedSha256 = computeSha256Hash(sha256Digest);
        validateComputedHash(computedSha256, sha256, s3FileUpload, deleteFileConsumer, logErrorConsumer);
    }

    /**
     * Validates the computed hash against the provided SHA-256 hash.
     *
     * @param computedSha256     the computed SHA-256 hash
     * @param sha256             the provided SHA-256 hash
     * @param fileUpload         the S3 file upload entity
     * @param deleteFileConsumer the consumer for deleting the file from S3
     * @throws ValidationException if the hashes do not match
     */
    public static void validateComputedHash(String computedSha256, String sha256, S3FileUpload fileUpload, Consumer<S3FileUpload> deleteFileConsumer, Consumer<String> logErrorConsumer) {
        logErrorConsumer.accept(String.format("Validating computed SHA-256 hash: %s against provided SHA-256 hash: %s", computedSha256, sha256));
        if (!computedSha256.equalsIgnoreCase(sha256)) {
            logErrorConsumer.accept(String.format("SHA-256 mismatch: computed hash %s does not match provided hash %s", computedSha256, sha256));
            deleteFileConsumer.accept(fileUpload);
            throw new ValidationException("Integrity check failed, SHA-256 mismatch.");
        }
        logErrorConsumer.accept("SHA-256 hash validation successful.");
    }

    /**
     * Computes the  SHA-256 hash of the given MessageDigest.
     *
     * @param sha256Digest the MessageDigest instance containing the data to hash
     * @return the computed SHA-256 hash as a hexadecimal string
     */
    public static String computeSha256Hash(MessageDigest sha256Digest) {
        return HexFormat.of().formatHex(sha256Digest.digest());
    }

    /**
     * Creates a new {@link S3FileUpload} object with the specified parameters.
     *
     * @param directory  the S3 directory template, containing placeholders for tenant, type, and SHA-256 hash
     * @param tenant     the tenant name to be used in the S3 key path
     * @param sha256     the SHA-256 hash of the file, used for integrity and key path generation
     * @param bucketName the name of the S3 bucket where the file will be stored
     * @param fileName   the name of the file to be uploaded
     * @param fileType   the type/category of the file (e.g., firmware, document)
     * @return a new {@link S3FileUpload} object populated with the provided details
     * @throws GenericSpServerException if an error occurs during the creation of the {@link S3FileUpload} object
     */
    public static S3FileUpload buildS3FileUpload(String directory, String tenant, String sha256, String bucketName, String fileName, String fileType) {
        String s3KeyPath = generateS3KeyPath(directory, tenant, sha256, fileType);
        try {
            return S3FileUpload.builder()
                    .bucketName(bucketName)
                    .filename(fileName)
                    .keyPath(s3KeyPath)
                    .build();
        } catch (RuntimeException e) {
            throw new GenericSpServerException(
                    String.format("Error while creating S3FileUpload for bucket: %s, file: %s, keyPath: %s", bucketName, fileName, s3KeyPath), e);
        }
    }

}
