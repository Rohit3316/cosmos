package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileStorageTransferRequest {
    private String bucketName;
    private String fileUrl;
    private String fileName;
    private String checksum;
    private Long fileId;
    private String fileType;
}
