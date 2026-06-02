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
 * Enum representing the update actions for a rollout.
 * Each action has a corresponding string value and description.
 */
public enum MgmtUpdateAction {

    /**
     * Install - To install the selected software and target version.
     */
    INSTALL("INSTALL", "Install the selected software and target version"),

    /**
     * Uninstall Any Version - To uninstall any version of the software (SCOMOID).
     */
    UNINSTALLANY("UNINSTALLANY", "Uninstall any version of the software (SCOMOID)"),

    /**
     * Uninstall Specific Versions - To uninstall only the selected software versions for a software (SCOMOID).
     */
    UNINSTALLSPECIFIC("UNINSTALLSPECIFIC", "Uninstall only the selected software versions (SCOMOID)");

    @JsonValue
    @Getter
    private final String action;

    private final String description;

    MgmtUpdateAction(String action, String description) {
        this.action = action;
        this.description = description;
    }
}
