package org.cosmos.sns.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.sns.SnsPublishable;

/**
 * Request to upload a file to the CDN.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CdnUploadRequest implements SnsPublishable {

    /**
     * Name of the S3 bucket where the file is located
     */
    private String bucketName;

    /**
     * Name of the file to be uploaded from S3
     */
    private String s3FileName;

    /**
     * Name of the file to be uploaded to the CDN
     */
    private String cdnFileName;

    /**
     * CDN to upload the file to
     */
    private String cdn;

    /**
     * Directory path in the CDN where the file should be uploaded
     */
    private String cdnDirPath;

    /**
     * Type of the file to be uploaded to the CDN
     */
    private String fileType;

    /**
     * Id of the file to be uploaded to the CDN
     */
    private Long fileId;

    /**
     * tenantId
     */
    private Long tenantId;

}
