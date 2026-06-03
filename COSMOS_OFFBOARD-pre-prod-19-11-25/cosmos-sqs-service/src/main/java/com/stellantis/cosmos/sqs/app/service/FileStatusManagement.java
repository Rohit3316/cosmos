package com.stellantis.cosmos.sqs.app.service;

import org.cosmos.models.sqs.FileType;
/**
 * Interface for managing file statuses across different file types.
 * Provides methods to:
 * - Update the file status and MD5 hash of a file.
 * - Retrieve the type of file being managed.
 *
 * @see FileType
 */

public interface FileStatusManagement {

    /**
     *
     * Updates the status and MD5 hash of files in the repository.
     */
    void updateFileStatus(Long fileId, String status, String md5, String tenant);

    /**
     * Updates the status of files in the repository.
     *
     * @param fileId - the id of the file
     * @param status - the status to be updated
     */
    void updateFileStatus(Long fileId, String status);

    /**
     * Retrieves the file name of the file with the given id.
     * @param fileId - the id of the file
     * @return the name of the file
     */
    String getFileName(Long fileId);
    /**
     *
     * retrieves the file type to which implementation is being done.
     */
    FileType getFileType();

    /**
     * Fetches the file size from S3 storage using the file's SHA-256 hash, file name, and tenant identifier.
     *
     * @param sha256   the SHA-256 hash of the file
     * @param fileName the name of the file
     * @param tenant   the tenant identifier
     * @return the size of the file in bytes
     */
    Long fetchFileSizeFromS3(String sha256, String fileName, String tenant);

}
