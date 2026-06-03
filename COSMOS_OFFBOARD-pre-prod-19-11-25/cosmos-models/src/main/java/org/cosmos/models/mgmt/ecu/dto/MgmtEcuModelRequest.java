package org.cosmos.models.mgmt.ecu.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body of PUT/POST ECU Model.
 */
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MgmtEcuModelRequest {

    @JsonProperty("ecuModelType")
    @NotNull
    @NotEmpty
    @Size(min = 1, max = 10)
    @Schema(description = "Type of the ECU model", example = "BT")
    private String ecuModelType;

    @JsonProperty("ecuModelName")
    @NotNull
    @NotEmpty
    @Size(min = 1, max = 25)
    @Schema(description = "Name of the ECU model", example = "HPC")
    private String ecuModelName;

    @JsonProperty("ecuNodeId")
    @NotNull
    @NotEmpty
    @Size(min = 1, max = 25)
    @Schema(description = "Node ID of the ECU", example = "30A0")
    private String ecuNodeId;
}