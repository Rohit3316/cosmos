package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import org.springframework.hateoas.RepresentationModel;

/**
 * Download information for all artifacts related to a specific {@link DdiChunk}
 * .
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiArtifact extends RepresentationModel<DdiArtifact> {

    @NotNull
    @JsonProperty
    @Schema(example = "binary.tgz")
    private String filename;

    @JsonProperty
    @Schema(example = "160")
    private String sourceVersion;

    @JsonProperty
    @Schema(example = "79")
    private String targetVersion;

    @JsonProperty
    private DdiArtifactHash hashes;

    @JsonProperty
    @Schema(example = "3")
    private Long size;

    public DdiArtifactHash getHashes() {
        return hashes;
    }

    public void setHashes(final DdiArtifactHash hashes) {
        this.hashes = hashes;
    }

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

    public void setSourceVersion(String sourceVersion) {
        this.sourceVersion = sourceVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

}