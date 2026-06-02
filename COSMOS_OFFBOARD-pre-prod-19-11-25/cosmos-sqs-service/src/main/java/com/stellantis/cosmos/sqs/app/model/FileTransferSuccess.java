package com.stellantis.cosmos.sqs.app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.sqs.FileType;

import jakarta.validation.constraints.NotNull;

/**
 * Payload class that will be sent from lambda when the file upload is success
 */


@Data
@AllArgsConstructor
@NoArgsConstructor
public class FileTransferSuccess {
    @NotNull
    private FileType fileType;
    @NotNull
    private String checksum;
    @NotNull
    private Long fileId;
    @NotNull
    private String fileUploadStatus;
    @NotNull
    private Long tenantId;
}