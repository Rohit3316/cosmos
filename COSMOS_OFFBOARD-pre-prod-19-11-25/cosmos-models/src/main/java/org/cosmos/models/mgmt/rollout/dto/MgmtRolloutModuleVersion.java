package org.cosmos.models.mgmt.rollout.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents the version details of a rollout module.
 * This class holds the module ID and the corresponding software version target ID
 * associated with a rollout.
 */
@Setter
@Getter
public class MgmtRolloutModuleVersion {

    /**
     * The unique identifier for the module.
     */
    private Long moduleId;

    /**
     * The unique identifier for the target software version associated with the module.
     */
    private Long softwareVersionTargetId;

    /**
     * Constructs a new MgmtRolloutModuleVersion with the given module ID
     * and software version target ID.
     *
     * @param moduleId              the unique identifier for the module
     * @param softwareVersionTargetId the unique identifier for the target software version
     */
    public MgmtRolloutModuleVersion(Long moduleId, Long softwareVersionTargetId) {
        this.moduleId = moduleId;
        this.softwareVersionTargetId = softwareVersionTargetId;
    }
}

