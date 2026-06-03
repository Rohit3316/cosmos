package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.annotations.TraceableField;

import jakarta.validation.constraints.NotNull;
import java.util.List;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SoftwareModuleArtifactBindingRequest {

    @JsonProperty(required = true)
    @Schema(example = "4")
    @NotNull(message = "Software module Id is required")
    @TraceableField
    Integer softwareModuleId;

    @JsonProperty
    @Schema(example = "[1,2,3]")
    List<Integer> sourceVersion;


    @NotNull(message = "Target Version is required")
    @JsonProperty(required = true)
    @Schema(example = "5")
    Integer targetVersion;
}
