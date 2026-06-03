package org.cosmos.models.ddi;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Collections;
import java.util.List;

/**
 * This class represents a Software in the DDI model.
 * It includes properties like software type, software format, software name, software installer type, software hashes and a list of software artifacts.
 * It is annotated with JsonIgnoreProperties to ignore unknown properties during deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class DdiSoftware {

    @JsonProperty("swType")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    @Schema(example = "bApp", description = "The software type.")
    private String swType;

    @JsonProperty("swFormat")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    @Schema(example = "1.2.0", description = "The software format or version scheme.")
    private String swFormat;

    @JsonProperty("swName")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    @Schema(example = "oneApp", description = "The name of the software.")
    private String swName;

    @JsonProperty("swInstallerType")
    @NotNull
    @Size(max = 256, message = "Field cannot exceed 256 characters")
    @Schema(example = "installer", description = "Type of installer used for this software.")
    private String swInstallerType;

    @JsonProperty("swArtifact")
    @NotNull
    @Schema(description = "The software artifact details (e.g., URL, hash, metadata).")
    private DdiSoftwareArtifact swArtifact;

    @JsonProperty("updateAction")
    @NotNull
    @Schema(example = "INSTALL", description = "The action to be performed (INSTALL, UNINSTALL, etc.).")
    private MgmtUpdateAction updateAction;

    @JsonProperty("updateActionUninstallVersion")
    @Schema(example = "[\"68541061AX00000000000000000008*670181252*133156002JY0925*\"]",
            description = "List of uninstall version identifiers applicable for UNINSTALLSPECIFIC action.")
    private List<String> uninstallVersions;

    /**
     * Returns an immutable copy of uninstallVersions to prevent external modification.
     */
    public List<String> getUninstallVersions() {
        return uninstallVersions == null ? Collections.emptyList() : List.copyOf(uninstallVersions);
    }
}

