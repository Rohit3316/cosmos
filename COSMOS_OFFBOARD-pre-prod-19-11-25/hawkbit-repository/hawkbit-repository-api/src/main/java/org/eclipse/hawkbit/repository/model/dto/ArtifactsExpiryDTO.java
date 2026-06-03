package org.eclipse.hawkbit.repository.model.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO to collect expired artifacts information like Artifact name, expiry date and version
 *
 */
@Setter
@Getter
public class ArtifactsExpiryDTO {

    private String fileName;
    private Long expiryDate;
    private String versionName;

    public ArtifactsExpiryDTO(String fileName, Long expiryDate, String versionName) {
        this.fileName = fileName;
        this.versionName = versionName;
        this.expiryDate = expiryDate;
    }


}
