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
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Action fulfillment progress by means of gives the achieved amount of maximal
 * of possible levels.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiPackage {

    @NotNull
    @Schema(example = "2")
    private final Integer cnt;

    @Schema(example = "5")
    private final Integer of;

    /**
     * Constructor.
     *
     * @param cnt
     *            achieved amount
     * @param of
     *            maximum levels
     */
    @JsonCreator
    public DdiPackage(@JsonProperty("cnt") final Integer cnt, @JsonProperty("of") final Integer of) {
        this.cnt = cnt;
        this.of = of;
    }

    public Integer getCnt() {
        return cnt;
    }

    public Integer getOf() {
        return of;
    }

    @Override
    public String toString() {
        return "Package [cnt=" + cnt + ", of=" + of + "]";
    }

}