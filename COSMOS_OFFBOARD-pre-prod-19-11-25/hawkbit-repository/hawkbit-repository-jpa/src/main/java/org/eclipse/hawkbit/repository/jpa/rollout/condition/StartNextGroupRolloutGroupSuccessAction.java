/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.security.SystemSecurityContext;

/**
 * Success action which starts the next following {@link RolloutGroup}.
 */
@Slf4j
public class StartNextGroupRolloutGroupSuccessAction implements RolloutGroupActionEvaluator<RolloutGroup.RolloutGroupSuccessAction> {


    private final RolloutGroupRepository rolloutGroupRepository;

    private final DeploymentManagement deploymentManagement;

    private final SystemSecurityContext systemSecurityContext;

    public StartNextGroupRolloutGroupSuccessAction(final RolloutGroupRepository rolloutGroupRepository,
                                                   final DeploymentManagement deploymentManagement, final SystemSecurityContext systemSecurityContext) {
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.deploymentManagement = deploymentManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    public RolloutGroup.RolloutGroupSuccessAction getAction() {
        return RolloutGroup.RolloutGroupSuccessAction.NEXTGROUP;
    }

    @Override
    public void exec(final Rollout rollout, final RolloutGroup rolloutGroup) {
        systemSecurityContext.runAsSystem(() -> {
            startNextGroup(rollout, rolloutGroup);
            return null;
        });
    }

    private void startNextGroup(final Rollout rollout, final RolloutGroup parentRolloutGroup) {
        log.debug("Starting to process next group for parent rollout group: {} in rollout: {}", parentRolloutGroup.getId(), rollout.getId());

        // Retrieve all child-group actions which need to be started.
        List<JpaRolloutGroup> childrenRolloutGroups = rolloutGroupRepository.findByParentIdAndStatus(parentRolloutGroup.getId(), RolloutGroupStatus.QUEUED);
        log.debug("Found {} child groups in QUEUED status for parent rollout group: {}", childrenRolloutGroups.size(), parentRolloutGroup.getId());
        // create action and start the next child group
        startAllChildGroups(rollout, childrenRolloutGroups);
        log.debug("Finished processing next group for parent rollout group: {} in rollout: {}", parentRolloutGroup.getId(), rollout.getId());

    }

    public void startAllChildGroups(Rollout rollout, List<JpaRolloutGroup> childrenRolloutGroups) {
        childrenRolloutGroups.forEach(childGroup -> {
            log.info("Processing child group: {}", childGroup.getId());
            long countOfStartedActions = deploymentManagement.createActionsForRolloutGroup(rollout, childGroup);
            log.debug("Created {} actions for child group: {}", countOfStartedActions, childGroup.getId());

            if (countOfStartedActions > 0) {
                // Set the status of the child group to RUNNING
                childGroup.setStatus(RolloutGroupStatus.RUNNING);
                rolloutGroupRepository.save(childGroup);
                log.debug("Set status of child group: {} to RUNNING", childGroup.getId());
            } else {
                // Set the status of the child group to FINISHED and start its child groups
                childGroup.setStatus(RolloutGroupStatus.FINISHING);
                rolloutGroupRepository.save(childGroup);
                log.debug("Set status of child group: {} to FINISHING", childGroup.getId());
                startNextGroup(rollout, childGroup);
            }
        });
    }
}
