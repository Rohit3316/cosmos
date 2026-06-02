package org.cosmos.sns.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.sns.SnsPublishable;

/**
 * Request to delete a file to the CDN.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CdnDeleteRequest implements SnsPublishable {

    /**
     * Id of the file to be deleted from CDN
     */
    private Long fileId;

    /**
     * Type of the file to be deleted from CDN
     * eg: artifact, esp, rsp
     */
    private String fileType;

    /**
     * File path in the CDN where the file is located
     */
    private String filePath;

    /**
     * Tenant ID associated with the request.
     */
    private Long tenantId;
}
