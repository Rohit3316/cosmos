package org.eclipse.hawkbit.mgmt.rest.resource.awsServices;

import java.net.URL;

public interface S3Service {
    URL generatePresignedUrl(String bucketName, String objectKey, Long preSignedUrlExpiryTime);
    String buildS3ObjectName(String tenant, String checksum, String fileName);
   String buildS3SupportPkgObjectName(String tenant, String checksum, String fileName, String fileType);


    boolean isValidGetUrl(URL url);
}
