/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.action.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * A json annotated model for Action updates in RESTful API representation.
 *
 */
public class MgmtActionRequestBodyPut {

    @JsonProperty("userAcceptanceRequired")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    public void setUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
    }

}
