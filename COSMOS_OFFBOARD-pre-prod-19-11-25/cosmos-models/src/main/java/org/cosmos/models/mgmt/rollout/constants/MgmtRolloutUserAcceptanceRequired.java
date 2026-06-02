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
import java.util.Arrays;
import lombok.Getter;

/**
 * Enum representing the connectivity types for a rollout.
 * Each connectivity type has a corresponding string value and description.
 */
public enum MgmtRolloutUserAcceptanceRequired {

    /**
     * When it is provided in the Deployment Base / DD, onboard will always ask for Customer Acceptance for installation.
     */
    YES("yes", "When it is provided in the Deployment Base / DD, onboard will always ask for Customer Acceptance for installation"),

    /**
     * When it is provided in the Deployment Base / DD, onboard will not ask for Customer Acceptance for installation.
     */
    NO("no", "When it is provided in the Deployment Base / DD, onboard will not ask for Customer Acceptance for installation");

    @JsonValue
    @Getter
    private final String name;

    private final String description;

    MgmtRolloutUserAcceptanceRequired(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public static MgmtRolloutUserAcceptanceRequired fromStringIgnoreCase(String name) {
        for (MgmtRolloutUserAcceptanceRequired type : MgmtRolloutUserAcceptanceRequired.values()) {
            if (type.getName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown enum type " + name);
    }

    public static String[] names() {
        return Arrays.stream(MgmtRolloutUserAcceptanceRequired.values()).map(Enum::name).toArray(String[]::new);
    }


}
