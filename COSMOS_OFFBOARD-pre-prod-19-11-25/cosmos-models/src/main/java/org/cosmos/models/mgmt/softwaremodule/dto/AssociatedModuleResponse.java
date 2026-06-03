package org.cosmos.models.mgmt.softwaremodule.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a response object for an associated software module.
 * Contains details about the module's ID and the associated software version target ID.
 */
@Setter
@Getter
public class AssociatedModuleResponse {

    /* Unique identifier for the module */
    private Long moduleId;

    /* Unique identifier for the associated software version target */
    private Long softwareVersionTargetId;

    /**
     * Constructor to initialize an AssociatedModuleResponse with given moduleId and softwareVersionTargetId.
     *
     * @param moduleId the ID of the module
     * @param softwareVersionTargetId the ID of the associated software version target
     */
    public AssociatedModuleResponse(Long moduleId, Long softwareVersionTargetId) {
        this.moduleId = moduleId;
        this.softwareVersionTargetId = softwareVersionTargetId;
    }
}
