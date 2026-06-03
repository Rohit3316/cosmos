/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Threshold to calculate if the {@link RolloutGroup#getSuccessConditionExp()} is reached and the
 * next rollout group can get started.
 */
public class ThresholdRolloutGroupSuccessCondition
        implements RolloutGroupConditionEvaluator<RolloutGroup.RolloutGroupSuccessCondition> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ThresholdRolloutGroupSuccessCondition.class);
    private static final List<DeviceActionStatus> ROLLOUT_FINISH_STATUS_LIST=List.of(DeviceActionStatus.FINISHED_FAILURE,DeviceActionStatus.FINISHED_SUCCESS,DeviceActionStatus.FINISHED_NOT_EXECUTED, DeviceActionStatus.CANCELED,DeviceActionStatus.CANCELING);

    private final ActionRepository actionRepository;

    public ThresholdRolloutGroupSuccessCondition(final ActionRepository actionRepository) {
        this.actionRepository = actionRepository;
    }

    @Override
    public RolloutGroup.RolloutGroupSuccessCondition getCondition() {
        return RolloutGroup.RolloutGroupSuccessCondition.THRESHOLD;
    }

    @Override
    public boolean eval(final Rollout rollout, final RolloutGroup rolloutGroup, final String expression) {

        final long totalGroup = rolloutGroup.getTotalTargets();
        if (totalGroup == 0) {
            // in case e.g. targets has been deleted we don't have any
            // actions left for this group, so the group is finished
            return true;
        }


        final long finished = this.actionRepository.countByRolloutIdAndRolloutGroupIdAndStatusIn(rollout.getId(),
                rolloutGroup.getId(), ROLLOUT_FINISH_STATUS_LIST);
        try {
            final int threshold = Integer.parseInt(expression);
            // calculate threshold
            return ((float) finished / (float) totalGroup) >= ((float) threshold / 100F);

        } catch (final NumberFormatException e) {
            LOGGER.error("Cannot evaluate condition expression " + expression, e);
            return false;
        }
    }

}
