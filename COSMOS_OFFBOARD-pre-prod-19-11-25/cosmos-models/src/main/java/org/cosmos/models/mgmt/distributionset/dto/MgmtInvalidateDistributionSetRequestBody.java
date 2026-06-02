/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionset.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.distributionset.constants.MgmtCancelationType;

/**
 * A json annotated rest model for invalidate DistributionSet requests.
 *
 */
public class MgmtInvalidateDistributionSetRequestBody {

    @NotNull
    @JsonProperty
    private MgmtCancelationType actionCancelationType;
    @JsonProperty
    @Schema(example = "true")
    private boolean cancelRollouts;

    public MgmtCancelationType getActionCancelationType() {
        return actionCancelationType;
    }

    public void setActionCancelationType(final MgmtCancelationType actionCancelationType) {
        this.actionCancelationType = actionCancelationType;
    }

    public boolean isCancelRollouts() {
        return cancelRollouts;
    }

    public void setCancelRollouts(final boolean cancelRollouts) {
        this.cancelRollouts = cancelRollouts;
    }

}
