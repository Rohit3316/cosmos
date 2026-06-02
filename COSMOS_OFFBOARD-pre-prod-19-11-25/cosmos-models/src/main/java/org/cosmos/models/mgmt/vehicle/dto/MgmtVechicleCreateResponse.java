package org.cosmos.models.mgmt.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MgmtVechicleCreateResponse {
    @JsonProperty("vehicleModelId")
    @NotNull
    @Schema(description = "Unique identifier of the vehicle model", example = "2001")
    private long id;

    @JsonProperty("vehicleModelName")
    @NotNull
    @Schema(description = "Name of the vehicle model", example = "P1H")
    private String name;

    @JsonProperty("ercType")
    @Schema(description = "ERC type of the vehicle model", example = "STLAB")
    private String ercType;
}