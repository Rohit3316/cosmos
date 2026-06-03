package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a message containing the file upload status
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadStatusMessage {

    /**
     * File type of the file e.g. artifact, rsp, esp
     */
    @NotBlank
    private String fileType;

    /**
     * Type of the message
     */
    private String type;

    /**
     * Id of the file
     */
    @NotNull
    private Long fileId;

    /**
     * Name of the file
     */
    private String fileName;

    /**
     * Upload status of the file e.g. STORAGE_UPLOAD_SUCCESSFUL, CDN_UPLOAD_SUCCESSFUL, STORAGE_UPLOAD_FAILED, CDN_UPLOAD_FAILED
     */
    @NotBlank
    private String status;

    /**
     * Timestamp of the file upload status message
     *
     */
    @NotNull
    private Long timestamp;

}
