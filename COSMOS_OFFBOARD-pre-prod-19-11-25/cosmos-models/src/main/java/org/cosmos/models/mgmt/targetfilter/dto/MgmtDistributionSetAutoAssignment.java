/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.targetfilter.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtId;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * Request Body of DistributionSet Id and User Acceptance Required for target filter auto
 * assignment operation.
 */
public class MgmtDistributionSetAutoAssignment extends MgmtId {

    @JsonProperty(required = false)
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    @JsonProperty(required = false)
    @Schema(example = "160")
    private Integer weight;

    @JsonProperty(required = false)
    @Schema(example = "true")
    private Boolean confirmationRequired;

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    public void setUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
