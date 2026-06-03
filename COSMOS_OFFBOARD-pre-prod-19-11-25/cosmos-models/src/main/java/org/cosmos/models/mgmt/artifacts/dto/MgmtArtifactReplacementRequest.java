package org.cosmos.models.mgmt.artifacts.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class MgmtArtifactReplacementRequest {

    private MultipartFile file;
    private String filename;
    private String fileType;
    private String description;
    private String oldSha256;
    private Long signatureExpiryDate;
    private String fileURL;
    private String newSha256;

}
