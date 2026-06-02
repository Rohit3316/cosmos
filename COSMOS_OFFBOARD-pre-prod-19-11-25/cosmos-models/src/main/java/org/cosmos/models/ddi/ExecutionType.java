/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Data;
import lombok.Getter;

/**
 * Enum representing the priority levels for a rollout.
 * Each priority level has a corresponding string value and description.
 */
@Getter
public enum ExecutionType {

    /**
     * Regular rollout - Standard or Low priority
     */
    IDLE("IDLE"),
    /**
     * Critical rollout - Critical or Medium priority
     */
    ERC("ERC");

    @JsonValue
    private final String executionType;

    ExecutionType(String executionType) {
        this.executionType = executionType;
    }

}