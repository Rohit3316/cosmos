package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;

/**
 * This class represents a Deployment Descriptor Base in the DDI model.
 * It includes properties like deployment description and action history.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"deploymentDescription", "deploymentSignature"})
@NoArgsConstructor
public class EncodedDdiDeploymentDescriptor {

    @JsonProperty("deploymentDescription")
    @NotNull
    @Schema(required = true)
    @Valid
    private String deploymentDescription;

    @JsonProperty("deploymentSignature")
    @NotNull
    @Schema(required = true)
    @Size(max = 8192, message = "Signature cannot exceed 8192 characters")
    String deploymentSignature;

    public EncodedDdiDeploymentDescriptor(String ddiDeployment, String deploymentSignature) {
        this.deploymentDescription = ddiDeployment;
        this.deploymentSignature = deploymentSignature;
    }

    public String getDeploymentDescription() {
        return deploymentDescription;
    }

    public String getDeploymentSignature() {
        return deploymentSignature;
    }

    @Override
    public String toString() {
        return "DeploymentBase [deploymentDescription=" + deploymentDescription + "]";
    }

}
