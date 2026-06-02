package org.cosmos.models.mgmt.rollout.constants;

import lombok.Getter;

/**
 * Enum representing various action links for a rollout.
 */
@Getter
public enum MgmtRolloutActionLinks {

    /**
     * freeze action link.
     */
    FREEZE("freeze"),

    /**
     * unfreeze action link.
     */
    UNFREEZE("unfreeze"),

    /**
     * Start action link.
     */
    START("start"),

    /**
     * Pause action link.
     */
    PAUSE("pause"),

    /**
     * Resume action link.
     */
    RESUME("resume"),

    /**
     * cancel action link.
     */
    CANCEL("cancel"),

    /**
     * delete action link.
     */
    DELETE("delete"),

    /**
     * Trigger next group action link.
     */
    TRIGGER_NEXT_GROUP("triggerNextGroup"),

    /**
     * Approve action link.
     */
    APPROVE("approve"),

    /**
     * Deny action link.
     */
    DENY("deny"),

    /**
     * Groups action link.
     */
    GROUPS("groups"),

    /**
     * Support packages action link.
     */
    SUPPORT_PACKAGES("supportPackages"),

    /**
     * Softwares action link
     */
    SOFTWARES("softwares");

    /**
     * -- GETTER --
     * Gets the link associated with the action.
     */
    private final String link;

    /**
     * Constructor for MgmtRolloutActionLinks.
     *
     * @param link the link associated with the action
     */
    MgmtRolloutActionLinks(String link) {
        this.link = link;
    }

}