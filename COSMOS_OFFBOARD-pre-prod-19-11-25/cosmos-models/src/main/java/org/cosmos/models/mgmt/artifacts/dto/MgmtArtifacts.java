package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtBaseEntity;

import java.util.Set;

/**
 * A json annotated rest model for Artifacts to RESTful API representation.
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class MgmtArtifacts extends MgmtBaseEntity {

    @JsonProperty("id")
    @Schema(example = "3")
    private Long artifactId;

    @JsonProperty
    @Schema(example = "file1")
    private String filename;

    @JsonProperty
    @Schema(example = "delta")
    private FileType fileType;

    @JsonProperty
    @Schema(example = "description")
    private String description;

    @JsonProperty
    @Schema(example = "123L")
    private Long signatureExpiryDate;

    @JsonProperty("size")
    @Schema(example = "3")
    private String fileSize;

    @JsonProperty
    private Set<MgmtArtifactSoftwareModuleAssociate> softwareModules;

    @JsonProperty
    private MgmtArtifactsHash hashes;
}
