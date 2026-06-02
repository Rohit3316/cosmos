package org.cosmos.models.mgmt.ecu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@Description("Response model representing the ECU vehicle model information")
public class MgmtEcuVehicleModelResponse {

    /**
     * The unique identifier for the ECU model.
     */
    @JsonProperty("ecuModelId")
    @NotNull
    private long id;

    /**
     * The type of the ECU model.
     */
    @JsonProperty("ecuModelType")
    @NotNull
    private String ecuModelType;

    /**
     * The name of the ECU model.
     */
    @JsonProperty("ecuModelName")
    @NotNull
    private String ecuModelName;

    /**
     * The node ID of the ECU.
     */
    @JsonProperty("ecuNodeId")
    private String ecuNodeId;

}
