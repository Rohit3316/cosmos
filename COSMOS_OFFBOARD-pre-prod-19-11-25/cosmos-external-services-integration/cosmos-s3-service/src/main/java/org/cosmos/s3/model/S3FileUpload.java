package org.cosmos.s3.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class S3FileUpload {
    private String bucketName;
    private String keyPath;
    private String filename;
    private String filePath;
}
