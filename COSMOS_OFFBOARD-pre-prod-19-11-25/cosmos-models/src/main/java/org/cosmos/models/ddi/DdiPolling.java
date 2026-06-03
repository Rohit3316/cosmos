/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Polling interval for the SP target.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiPolling {

    @JsonProperty
    @Schema(example = "12:00:00")
    private String sleep;

    /**
     * Constructor.
     *
     * @param sleep
     *            between polls
     */
    public DdiPolling(final String sleep) {
        this.sleep = sleep;
    }

    /**
     * Constructor.
     *
     */
    public DdiPolling() {
        // needed for json create
    }

    public String getSleep() {
        return sleep;
    }

}

