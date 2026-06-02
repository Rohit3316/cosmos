package org.eclipse.hawkbit.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.ValidString;

import jakarta.validation.constraints.NotEmpty;
import java.io.InputStream;

/**
 * Represents the data required to upload a deployment log file.
 * Immutable and thread-safe.
 */
@Getter
@Builder
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DeploymentLogUpload {

    /**
     * The input stream of the file to upload.
     */
    private InputStream inputStream;

    /**
     * The name of the file to be stored.
     */
    @NotEmpty
    @ValidString
    private String filename;

    /**
     * The original name of the file.
     */
    private String fileOriginalName;

    /**
     * The action to perform.
     */
    private Action action;

    /**
     * SHA-256 checksum for integrity verification.
     */
    private String providedSha256Sum;

    /**
     * Whether to override existing file.
     */
    private Boolean overrideExisting;

    /**
     * MIME type of the file.
     */
    private String contentType;

    /**
     * Size of the file in bytes.
     */
    private Long fileSize;

    /**
     * Sequence number for chunked uploads.
     */
    private Integer sequence;

    /**
     * The size of the current chunk in bytes.
     */
    private Long byteSize;

    /**
     * The starting byte position of the current chunk.
     */
    private Long range;

    /**
     * Indicates if this is the last chunk of the file.
     */
    private Boolean isLastChunk;

    /**
     * Indicates if this is the last file.
     */
    private Boolean isLastFile;

    /**
     * Path where the file should be stored.
     */
    private String filePath;

}
