package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Collections;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * This class represents a Deployment Descriptor in the DDI model.
 * It includes properties like download handling type, update handling type, connectivity type and a list of ECUs.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DdiDeploymentDescriptor {


    @Schema(description = "The name of the rollout")
    private String rolloutName;

    @Schema(description = "A brief description of the rollout")
    private String description;

    @Schema(description = "The unique identifier for the action")
    private Long actionId;

    private HandlingType download;

    private HandlingType update;




    @JsonProperty("deploymentMetadata")
    @NotNull
    @Valid
    private DdiDeploymentMetadata deploymentMetadata;

    @JsonProperty("ecus")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Valid
    private List<DdiEcu> ecus;

    @JsonProperty("rsp")
    @Valid
    private DdiRspTypes rspTypes;


    /**
     * Constructor.
     */
    public DdiDeploymentDescriptor() {
        // needed for json create.
    }

    public DdiDeploymentDescriptor(String rolloutName, String description, Long actionId, HandlingType download, HandlingType update, List<DdiEcu> ecus, DdiRspTypes ddiRspTypes, DdiDeploymentMetadata deploymentMetadata) {
        this.rolloutName = rolloutName;
        this.description = description;
        this.actionId = actionId;
        this.download = download;
        this.update = update;

        this.ecus = ecus;
        this.rspTypes = ddiRspTypes;
        this.deploymentMetadata = deploymentMetadata;
    }

    public List<DdiEcu> getEcus() {
        if (ecus == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(ecus);
    }

    /**
     * The handling type for the update action.
     */
    public enum HandlingType {

        /**
         * Not necessary for the command.
         */
        SKIP("skip"),

        /**
         * Try to execute (local applications may intervene by SP control API).
         */
        ATTEMPT("attempt"),

        /**
         * Execution independent of local intervention attempts.
         */
        FORCED("forced");

        @Schema(example = "xyz")
        private final String name;

        HandlingType(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

}