package org.cosmos.models.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Represents a message containing the file upload error
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileUploadErrorMessage {

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
    @NotBlank
    private String fileName;

    /**
     * Upload error status of the file e.g. STORAGE_UPLOAD_ERROR, CDN_UPLOAD_ERROR
     */
    @NotBlank
    private String status;

    /**
     * List of error codes related to the file upload
     */
    private List<String> errorCode;

    /**
     * Error messages related to the file upload
     */
    @NotNull
    private List<String> errorMessages;

    /**
     * Timestamp of the file upload status message
     *
     */
    @NotNull
    private Long timestamp;

}
