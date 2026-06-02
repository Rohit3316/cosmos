package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.springframework.hateoas.RepresentationModel;

/**
 * Download information for all artifacts related to a specific {@link DdiSoftware}
 * .
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DdiSoftwareArtifact extends RepresentationModel<DdiSoftwareArtifact> {

    @NotNull
    @JsonProperty
    @Schema(example = "binary.tgz")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String filename;

    @JsonProperty
    @Schema(example = "160")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String sourceVersion;

    @JsonProperty
    @Schema(example = "79")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String targetVersion;

    @JsonProperty
    @Schema(example = "3")
    private Long size;

    @JsonProperty("artifactType")
    @NotNull
    @Schema(example = "fileType")
    private ArtifactType artifactType;

    @JsonProperty("hashes")
    @NotNull
    @Schema(example = "hashes")
    private DdiDDArtifactHash hashes;

    @JsonProperty("expiryDate")
    @NotNull
    @Schema(example = "1718095596287")
    private Long expiryDate;

    //create getters and setters

    public String getFilename() {
        return filename;
    }

    public void setFilename(final String fileName) {
        filename = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(final Long size) {
        this.size = size;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public void setSourceVersion(final String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(final String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public void setArtifactType(final ArtifactType artifactType) {
        this.artifactType = artifactType;
    }

    public DdiDDArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DdiDDArtifactHash hashes) {
        this.hashes = hashes;
    }

    public Long getExpiryDate() {
        return expiryDate;
    }
    public void setExpiryDate(final Long expiryDate) {
        this.expiryDate = expiryDate;
    }
}
