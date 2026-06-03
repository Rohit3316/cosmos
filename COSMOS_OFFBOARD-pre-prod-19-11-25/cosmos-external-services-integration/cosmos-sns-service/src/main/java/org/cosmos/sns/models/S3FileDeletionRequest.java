package org.cosmos.sns.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.sns.SnsPublishable;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a request to delete a file from an S3 bucket.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class S3FileDeletionRequest implements SnsPublishable {

    /**
     * The type of the file to be deleted.
     */
    @NotEmpty(message = "File type cannot be empty")
    private String fileType;

    /**
     * The path of the file to be deleted.
     */
    @NotEmpty(message = "File path cannot be empty")
    private String filePath;

    /**
     * The name of the S3 bucket containing the file to be deleted.
     */
    @NotEmpty(message = "Bucket name cannot be empty")
    private String bucketName;

    /**
     * The ID of the file to be deleted.
     */
    @NotNull(message = "File ID cannot be null")
    private Long fileId;

    /**
     * The tenant ID associated with the request.
     */
    private Long tenantId;
}