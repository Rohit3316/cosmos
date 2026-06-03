package org.cosmos.models.mgmt.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuVehicleModelResponse;

/**
 * Response body of PUT/POST Vehicle Model.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MgmtVehicleResponse {
    @JsonProperty("vehicleModelId")
    @NotNull
    private long id;

    @JsonProperty("vehicleModelName")
    @NotNull
    private String name;

    @JsonProperty("ecuModels")
    List<MgmtEcuVehicleModelResponse> ecuModels;

    @JsonProperty("ercType")
    private String ercType;
}
