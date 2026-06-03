package org.cosmos.models.mgmt.rollout.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * Represents the total target count and their statuses in a rollout action.
 * This class holds the total number of targets and their breakdown by various statuses.
 */
@Getter
@Setter
public class MgmtRolloutTotalTargetCountActionStatus {

    /**
     * The total number of targets associated with the rollout.
     */
    private Long totalTargets;

    /**
     * A map representing the count of targets categorized by their statuses.
     * The key represents the status (e.g., "running", "finished"), and the value represents the count.
     */
    private Map<String, Long> totalTargetsPerStatus;
}
