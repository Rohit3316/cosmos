/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Builder to update the auto assign {@link DistributionSet} of a
 * {@link TargetFilterQuery} entry. Defines all fields that can be updated.
 */
public class AutoAssignDistributionSetUpdate {
    private final long targetFilterId;
    private Long dsId;
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    private Boolean confirmationRequired;

    /**
     * Constructor
     * 
     * @param targetFilterId
     *            ID of {@link TargetFilterQuery} to update
     */
    public AutoAssignDistributionSetUpdate(final long targetFilterId) {
        this.targetFilterId = targetFilterId;
    }

    /**
     * Specify {@link DistributionSet}
     * 
     * @param dsId
     *            ID of the {@link DistributionSet}
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate ds(final Long dsId) {
        this.dsId = dsId;
        return this;
    }

    /**
     * Specify {@link DistributionSet}
     * 
     * @param userAcceptanceRequired
     *            {@link MgmtRolloutUserAcceptanceRequired} used for the auto assignment
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate userAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
        return this;
    }

    /**
     * Specify weight of resulting {@link Action}
     * 
     * @param weight
     *            weight used for the auto assignment
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate weight(final Integer weight) {
        this.weight = weight;
        return this;
    }

    /**
     * Specify initial confirmation state of resulting {@link Action}
     *
     * @param confirmationRequired
     *            if confirmation is required for this auto assignment (considered
     *            with confirmation flow active)
     * @return updated builder instance
     */
    public AutoAssignDistributionSetUpdate confirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return this;
    }

    public Long getDsId() {
        return dsId;
    }

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    public Integer getWeight() {
        return weight;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public long getTargetFilterId() {
        return targetFilterId;
    }

}
