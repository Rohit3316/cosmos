package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.Size;


/**
 * Hashes for given Artifact.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DdiDDArtifactHash {

    @JsonProperty
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(example = "a03b221c6c6eae7122ca51695d456d5222e524889136394944b2f9763b483615")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String sha256;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(example = "a03b221c6c6eae7122ca51695d456d5222e524889136394944b2f9763b483615")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String md5;


}
