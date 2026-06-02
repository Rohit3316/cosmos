package org.cosmos.models.mgmt.action.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.hateoas.RepresentationModel;

/**
 * A json annotated rest model for {@link DeploymentLog} to RESTful API representation.
 */

@Data
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtDeploymentLog extends RepresentationModel<MgmtDeploymentLog> {

    @JsonProperty("file_name")
    @Schema(example = "name")
    private String fileName;

    @JsonProperty("file_size")
    @Schema(example = "1024L")
    private Long fileSize;

    @JsonProperty("sequence")
    @Schema(example = "1")
    private Integer sequence;

    @JsonProperty("byte_size")
    @Schema(example = "1024L")
    private Long byteSize;

    @JsonProperty("byte_range")
    @Schema(example = "1024L")
    private Long byteRange;

    @JsonProperty("is_last_chunk")
    @Schema(example = "true")
    private Boolean isLastChunk;

    @JsonProperty("is_last_file")
    @Schema(example = "false")
    private Boolean isLastFile;

    @JsonProperty("sha256_hash")
    @Schema(example = "bf93d4b873f34cc73887140bc862991f91e62286a981e784073d67a3e723238e")
    private String sha256Hash;

    @JsonProperty("file_path")
    @Schema(example = "/1/1/", description = "/{rolloutId}/{actionId}/")
    private String filePath;
}
