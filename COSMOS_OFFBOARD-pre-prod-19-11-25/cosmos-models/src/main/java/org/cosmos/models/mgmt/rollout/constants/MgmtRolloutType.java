/**
 * Copyright (c) 2025 Your Company Name.
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
 * Enum representing the types of rollouts.
 * Each rollout type has a corresponding string value and description.
 */
public enum MgmtRolloutType {

    /**
     * FOTA - Selected when firmware level updates need to be made
     * and the vehicle needs to follow a specific behavior for FOTA update.
     */
    FOTA("FOTA", "Firmware update with specific vehicle behavior"),

    /**
     * AOTA - Selected when only applications need to be updated
     * and the vehicle needs to follow a specific behavior for AOTA update.
     */
    AOTA("AOTA", "Application update with specific vehicle behavior");

    @JsonValue
    @Getter
    private final String type;

    private final String description;

    MgmtRolloutType(String type, String description) {
        this.type = type;
        this.description = description;
    }
}
