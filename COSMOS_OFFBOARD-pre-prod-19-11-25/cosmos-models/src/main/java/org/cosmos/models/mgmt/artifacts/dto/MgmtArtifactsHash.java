package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Hashes for given Artifact.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MgmtArtifactsHash {

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(example = "0d1b08c34858921bc7c662b228acb7ba")
    private String md5;

    @JsonProperty
    @Schema(example = "a03b221c6c6eae7122ca51695d456d5222e524889136394944b2f9763b483615")
    private String sha256;
}
