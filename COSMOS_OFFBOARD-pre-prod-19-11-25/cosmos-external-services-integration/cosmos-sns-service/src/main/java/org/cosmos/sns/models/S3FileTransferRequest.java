package org.cosmos.sns.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.sns.SnsPublishable;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class S3FileTransferRequest implements SnsPublishable {
    /**
     * URL of the file to be uploaded/downloaded from S3
     */
    private String fileURL;

    /**
     * Name of the S3 bucket where the file is located
     */
    private String bucketName;

    /**
     * Checksum of the file to be uploaded/downloaded from S3
     */
    private String checksum;

    /**
     * Name of the file to be uploaded/downloaded from S3
     */
    private String fileName;

    /**
     * Id of the file to be uploaded/downloaded from S3
     */
    private Long fileId;

    /**
     * Type of the file to be uploaded/downloaded from S3
     */
    private String fileType;

    /**
     * tenantId of the request
     */

    private Long tenantId;
}