package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A json annotated rest model for {@link Artifacts}'s associated {@link SoftwareModule} to RESTful API representation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtArtifactSoftwareModuleAssociate {

    @JsonProperty("id")
    @Schema(example = "3")
    private Long softwareModuleId;

    @JsonProperty
    @Schema(example = "68541061AX00000000000000000008*670181252*133156002JY0000*")
    private String sourceVersionForDelta;

    @JsonProperty
    @Schema(example = "[]")
    private Set<String> sourceVersionsForFull;

    @JsonProperty
    @Schema(example = "68541061AA000000000000000000007670181252*133156002JY0000*")
    private String targetVersion;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MgmtArtifactSoftwareModuleAssociate that = (MgmtArtifactSoftwareModuleAssociate) o;
        return Objects.equals(softwareModuleId, that.softwareModuleId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(softwareModuleId);
    }
}