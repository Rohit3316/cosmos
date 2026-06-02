package org.cosmos.models.mgmt.rolloutgroup.dto;

/**
 * Represents a target associated with a rollout group in the management system.
 * This record holds identifiers linking a rollout group to its target entity.
 *
 * @param rolloutGroupId the unique identifier of the rollout group
 * @param targetId       the unique identifier of the target associated with the rollout group
 */
public record MgmtRolloutGroupTarget(Long rolloutGroupId, Long targetId) {
}
