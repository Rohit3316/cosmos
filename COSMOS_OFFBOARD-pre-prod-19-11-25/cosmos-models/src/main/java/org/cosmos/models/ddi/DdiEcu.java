package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Getter;


/**
 * This class represents an ECU (Electronic Control Unit) in the DDI model.
 * It includes properties like ECU node ID, ECU model type and a list of software associated with the ECU.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DdiEcu {

    /**
     * The unique identifier of the ECU node.
     * It is a required field.
     */
    @NotNull
    @Schema(example = "4023")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String ecuNodeId;

    /**
     * The model type of the ECU.
     * It is a required field.
     */
    @NotNull
    @Schema(example = "ST")
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    private String ecuModelType;

    /**
     * The list of software associated with the ECU.
     * It is a required field.
     */
    @NotNull
    List<DdiSoftware> software;

    @JsonProperty("esp")
    private DdiEspTypes espTypes;


    /**
     * Constructor for the DdiEcu class.
     * @param ecuNodeId The unique identifier of the ECU node.
     * @param ecuModelType The model type of the ECU.
     * @param software The list of software associated with the ECU.
     */
    public DdiEcu(@NotNull String ecuNodeId, @NotNull String ecuModelType, @NotNull List<DdiSoftware> software, DdiEspTypes espTypes) {
        this.ecuNodeId = ecuNodeId;
        this.ecuModelType = ecuModelType;
        this.software = software;
        this.espTypes = espTypes;
    }

    public List<DdiSoftware> getSoftware() {
        return software;
    }


    public void setEcuNodeId(String ecuNodeId) {
        this.ecuNodeId = ecuNodeId;
    }

    public void setEcuModelType(String ecuModelType) {
        this.ecuModelType = ecuModelType;
    }

    public void setEspTypes(DdiEspTypes espTypes) {
        this.espTypes = espTypes;
    }

}
