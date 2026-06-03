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
 * Enum representing the priority levels for a rollout.
 * Each priority level has a corresponding string value and description.
 */
public enum MgmtRolloutPriority {

    /**
     * Regular rollout - Standard or Low priority
     */
    REGULAR("regular", "Standard or Low priority"),
    /**
     * Critical rollout - Critical or Medium priority
     */
    CRITICAL("critical", "Critical or Medium priority"),
    /**
     * Urgent Rollout - Urgent or highest priority
     */
    URGENT("urgent", "Urgent or Highest priority");

    @JsonValue
    @Getter
    private final String priority;

    private final String description;

    MgmtRolloutPriority(String priority, String description) {
        this.priority = priority;
        this.description = description;
    }

}