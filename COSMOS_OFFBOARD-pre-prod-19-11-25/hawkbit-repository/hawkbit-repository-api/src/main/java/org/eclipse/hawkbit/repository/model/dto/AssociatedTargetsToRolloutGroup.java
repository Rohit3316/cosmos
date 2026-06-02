package org.eclipse.hawkbit.repository.model.dto;

import lombok.Builder;
import lombok.Data;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;

import java.util.List;

/**
 * Data transfer object to collect targets associated to a rollout group.
 *
 */
@Data
@Builder
public class AssociatedTargetsToRolloutGroup {

    RolloutGroup rolloutGroup;
    List<Target> targets;
}
