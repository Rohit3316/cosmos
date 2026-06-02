package org.cosmos.models.mgmt.softwaremodule.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import org.cosmos.models.mgmt.softwaremodule.dto.AssociatedModuleResponse;


/**
 * Represents the response for software module association with a rollout.
 * Provides details about the distribution set and the associated software modules.
 */
@Getter
@Setter
public class MgmtSoftwareModuleAssociationResponse {

    /**
     * The unique identifier of the associated distribution set.
     */
    private Long distributionSetId;

    /**
     * The type of the distribution set (e.g., "os", "firmware").
     */
    private String distributionSetType;

    /**
     * A list of associated software modules, each containing module and version details.
     */
    private List<AssociatedModuleResponse> associatedModules;
}
