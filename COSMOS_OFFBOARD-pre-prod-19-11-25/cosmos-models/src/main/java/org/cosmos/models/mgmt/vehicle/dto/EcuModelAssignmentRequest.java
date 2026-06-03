package org.cosmos.models.mgmt.vehicle.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;


@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuModelAssignmentRequest {

    @JsonProperty(required = true)
    @Schema(example = "2")
    private Long ecuModelId;

    public Long getEcuModelId() {
        return ecuModelId;
    }

    public void setEcuModelId(Long ecuModelId) {
        this.ecuModelId = ecuModelId;
    }
}
