package com.stellantis.cosmos.sqs.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a file transfer failure message.
 * This class contains details about the file transfer failure, including the file ID,
 * file type, error type, storage type, error description, retry count, and tenant ID.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTransferFailure {

    /**
     * The ID of the file that failed to transfer.
     */
    @NotNull
    private Long fileId;

    /**
     * The type of the file that failed to transfer.
     */
    @NotNull
    private FileType fileType;

    /**
     * The type of error that occurred during the file transfer.
     */
    @NotBlank
    private String errorType;

    /**
     * The type of storage where the file transfer failed.
     */
    @NotNull
    private StorageType storageType;

    /**
     * The ID of the tenant associated with the file transfer.
     */
    @NotNull
    private Long tenantId;

    /**
     * A description of the error that occurred during the file transfer.
     */
    private String errorDescription;

    /**
     * The number of times the file transfer has been retried.
     */
    private int retryCount;


}