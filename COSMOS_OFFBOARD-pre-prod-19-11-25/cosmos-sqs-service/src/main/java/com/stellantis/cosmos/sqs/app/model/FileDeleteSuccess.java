package com.stellantis.cosmos.sqs.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;

import jakarta.validation.constraints.NotNull;

/**
 * Represents a file delete success message.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileDeleteSuccess {

    /**
     * The ID of the file that was successfully deleted.
     */
    @NotNull
    private Long fileId;

    /**
     * The type of the file that was successfully deleted.
     */
    @NotNull
    private FileType fileType;

    /**
     * The status of the delete operation
     */
    @NotNull
    private FileTransferStatus status;

    /**
     * The ID of the tenant associated with the file deletion.
     */
    @NotNull
    private Long tenantId;
}
