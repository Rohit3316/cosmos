package com.stellantis.cosmos.sqs.app.service;

import org.cosmos.models.sqs.ActionType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;

import java.util.List;

/**
 * Interface for managing file processing error logs.
 * <p>
 * Provides methods to:
 * - Persist file processing error logs.
 */
public interface FileProcessingErrorLogsManagement {

    /**
     * Persists file processing error logs in the database
     *
     * @param fileType - the type of file being processed - ARTIFACT, ESP or RSP
     * @param logMessage - the error message
     * @param logTypeId - the ID of the file type in the log
     * @param storageType - the type of storage - S3 or CDN
     * @param retryCount - the number of times the file processing has been retried
     * @param action - the action - UPLOAD or DELETE
     */
    void persistFileProcessingErrorLogs(FileType fileType, String logMessage, Long logTypeId, StorageType storageType, Integer retryCount, ActionType action);

    /**
     * Get distinct failure messages for a given file id & file type
     *
     * @param fileId - the ID of the file
     * @param fileType - the type of file being processed - ARTIFACT, ESP or RSP
     * @return - list of distinct failure messages
     */
    List<String> getDistinctFailureMessages(Long fileId, FileType fileType);

}

