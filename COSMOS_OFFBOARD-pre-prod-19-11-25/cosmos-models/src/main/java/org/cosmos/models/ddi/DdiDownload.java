/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import jakarta.validation.constraints.NotNull;

/**
 * Download information of the action package which can be an intermediate or
 * final update.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiDownload {

    @Getter
    @NotNull
    private final Integer percentage;

    private final DdiPackage pkg;

    /**
     * Constructor.
     *
     * @param percentage
     *            as download percentage
     * @param pkg
     *            if not yet finished
     */
    @JsonCreator
    public DdiDownload(@JsonProperty("percentage") final Integer percentage,
                     @JsonProperty("package") final DdiPackage pkg) {
        this.percentage = percentage;
        this.pkg = pkg;
    }

    public DdiPackage getPackage() {
        return pkg;
    }

    @Override
    public String toString() {
        return "Download [percentage=" + percentage + ", package=" + pkg + "]";
    }
}