package org.cosmos.models.mgmt.artifacts.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.annotations.TraceableField;

/**
 * DTO for an SCOMO artifact binding request.
 *
 * Contains the information required to associate one or more
 * source versions of an artifact with a specific target version
 * in the context of SCOMO management.
 *
 * Fields:
 * - scomoId: Unique identifier of the SCOMO (required)
 * - sourceVersion: List of source versions to be bound (optional)
 * - targetVersion: Target version to which the source versions will be bound (required)
 */
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ScomoArtifactBindingRequest {

    @JsonProperty(required = true)
    @Schema(example = "scomo")
    @NotNull(message = "Scomo Id is required")
    @TraceableField
    String scomoId;

    @JsonProperty
    @Schema(example = "[1,2,3]")
    List<Integer> sourceVersion;


    @NotNull(message = "Target Version is required")
    @JsonProperty(required = true)
    @Schema(example = "5")
    Integer targetVersion;
}
