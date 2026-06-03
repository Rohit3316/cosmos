package org.cosmos.models.mgmt.ecu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cosmos.models.mgmt.vehicle.dto.MgmtEcuVehicleModelResponse;

import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Response body of PUT/GET ECU Model.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MgmtEcuModelResponse {
    @JsonProperty("ecuModelId")
    @NotNull
    @Schema(description = "Unique identifier of the ECU model", example = "1001")
    private long id;

    @JsonProperty("ecuModelType")
    @NotNull
    @Schema(description = "Type of the ECU model", example = "BT")
    private String ecuModelType;

    @JsonProperty("ecuModelName")
    @NotNull
    @Schema(description = "Name of the ECU model", example = "HPC")
    private String ecuModelName;

    @JsonProperty("ecuNodeId")
    @Schema(description = "Node ID of the ECU", example = "02A0")
    private String ecuNodeId;

    @JsonProperty("vehicleModels")
    @Schema(description = "List of associated vehicle models")
    private List<MgmtEcuVehicleModelResponse> vehicleModels;
}