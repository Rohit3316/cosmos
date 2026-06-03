/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rollout.constants;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * Enum representing the start types for a rollout.
 * Each start type has a corresponding string value and description.
 */
public enum MgmtRolloutStartType {

    /**
     * A trigger using Execution interface will be required to move from “Ready“ to “Running“ status
     */
    MANUAL("manual", "A trigger using Execution interface will be required to move from “Ready“ to “Running“ status"),
    /**
     * Rollout will automatically move from “Ready“ to “Running” status
     */
    AUTO("auto", "Rollout will automatically move from “Ready“ to “Running” status"),
    /**
     * Rollout will automatically move from “Ready“ to “Running“  status on the provided Scheduled date using a trigger on UTC based timezone
     */
    SCHEDULED("scheduled", "Rollout will automatically move from “Ready“ to “Running“  status on the provided Scheduled date using a trigger on UTC based timezone");

    @JsonValue
    @Getter
    private final String name;

    private final String description;

    MgmtRolloutStartType(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
