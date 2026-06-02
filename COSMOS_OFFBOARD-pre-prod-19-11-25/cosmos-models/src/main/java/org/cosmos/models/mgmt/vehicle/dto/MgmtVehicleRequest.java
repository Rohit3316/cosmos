package org.cosmos.models.mgmt.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request body of PUT/POST Vehicle Model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MgmtVehicleRequest {

    @JsonProperty("vehicleModelName")
    @NotNull
    @NotEmpty
    @Size(min = 1, max = 25)
    @Schema(description = "Name of the vehicle model", example = "P1H")
    private String name;

    @JsonProperty("ercType")
    @Size(max = 10)
    @Schema(description = "ERC type of the vehicle model", example = "STLAB")
    private String ercType;
}