package com.stellantis.cosmos.sqs.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.sqs.ErrorType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;

/**
 * Represents a file delete failure message.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDeleteFailure {

    /**
     * The ID of the tenant associated with the file deletion failure.
     */
    private Long tenantId;

    /**
     * The ID of the file that failed to delete.
     */
    private Long fileId;

    /**
     * The type of the file that failed to delete.
     */
    private FileType fileType;

    /**
     * The type of error that occurred during the file deletion.
     */
    private ErrorType errorType;

    /**
     * A description of the error that occurred during the file deletion.
     */
    private String errorDescription;

    /**
     * The type of storage where the file deletion failed.
     */
    private StorageType storageType;

    /**
     * The number of times the file deletion has been retried.
     */
    private int retryCount;

}