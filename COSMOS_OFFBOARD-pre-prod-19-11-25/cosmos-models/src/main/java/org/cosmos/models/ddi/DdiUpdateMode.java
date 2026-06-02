/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumerates the supported update modes. Each mode represents an attribute
 * update strategy.
 *
 * @see DdiConfigData
 */
public enum DdiUpdateMode {

    /**
     * Merge update strategy
     */
    MERGE("merge"),

    /**
     * Replacement update strategy
     */
    REPLACE("replace"),

    /**
     * Removal update strategy
     */
    REMOVE("remove");

    @Schema(example = "xyz")
    private final String name;

    DdiUpdateMode(final String name) {
        this.name = name;
    }

    @JsonValue
    public String getName() {
        return name;
    }

}
