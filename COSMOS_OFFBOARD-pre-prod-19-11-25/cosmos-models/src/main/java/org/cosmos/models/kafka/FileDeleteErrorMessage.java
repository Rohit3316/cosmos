package org.cosmos.models.kafka;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a message containing the file delete error
 */

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FileDeleteErrorMessage {

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
     * Name of the artifact file
     * This is the name of the file that was attempted to be deleted/purged.
     */
    private String fileName;

    /**
     * SHA-256 hash of the file
     * This is used to uniquely identify the file's content.
     */
    private String sha256;

    /**
     * Delete failure status for a file e.g. STORAGE_DELETION_FAILED, STORAGE_DELETION_FAILED
     */
    @NotBlank
    private String status;

    /**
     * Error messages related to the file deletion failure
     */
    @NotNull
    private List<String> errorMessages;

    /**
     * Error codes related to the file deletion failure
     */
    private List<String> errorCodes;

    /**
     * List of vehicle IDs
     * This is used to identify which vehicles are affected by the purged artifact file.
     * This is optional and can be empty if the file is not associated with any vehicle and
     * will be valid for status PURGED
     */
    private List<String> vehicleId;

    /**
     * Timestamp of the file deletion failure status message
     *
     */
    @NotNull
    private Long timestamp;

}
