package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.NoArgsConstructor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * This class represents a Deployment Descriptor Base in the DDI model.
 * It includes properties like deployment description and action history.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"deploymentDescription", "actionHistory"})
@NoArgsConstructor
public class DdiDeploymentDescriptorBase {

    @JsonProperty("deploymentDescription")
    @NotNull
    @Valid
    private DdiDeploymentDescriptor deploymentDescription;

    @JsonProperty("deploymentSignature")
    @NotNull
    @Size(max = 8192, message = "Signature cannot exceed 8192 characters")
    String deploymentSignature;

    public DdiDeploymentDescriptorBase(DdiDeploymentDescriptor ddiDeployment, String deploymentSignature) {
        this.deploymentDescription = ddiDeployment;
        this.deploymentSignature = deploymentSignature;
    }

    public DdiDeploymentDescriptor getDeploymentDescription() {
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
