package org.eclipse.hawkbit.repository.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 *
 * Wrapper class to hold the validation result, non-empty groups list and remaining targets for processing.
 */
@Data
@Builder
public class RolloutGroupsValidationWrapper {

    /**
     * The validation result of the rollout groups.
     */
    private RolloutGroupsValidation validation;

    /**
     * List of non-empty rollout groups.
     */
    private List<RolloutGroup> nonEmptyGroups;

    /**
     * The number of remaining targets for processing.
     */
    private Long remainingTargets;

}
