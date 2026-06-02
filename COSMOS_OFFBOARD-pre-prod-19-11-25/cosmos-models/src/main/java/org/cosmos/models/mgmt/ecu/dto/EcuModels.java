package org.cosmos.models.mgmt.ecu.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EcuModels {
    @JsonProperty
    @Schema(example = "7")
    private Long ecuModelId;

    public Long getEcuModelId() {
        return ecuModelId;
    }

    public void setEcuModelId(Long ecuModelId) {
        this.ecuModelId = ecuModelId;
    }
}
