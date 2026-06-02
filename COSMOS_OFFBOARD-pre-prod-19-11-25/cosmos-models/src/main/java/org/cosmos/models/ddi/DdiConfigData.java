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
import java.util.Map;
import jakarta.validation.constraints.NotEmpty;

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfigData {

    @NotEmpty
    private final Map<String, DdiConfigDataDevice> data;

    @Schema(example = "merge")
    private final DdiUpdateMode mode;

    /**
     * Constructor.
     *
     * @param data
     *            contains the attributes.
     * @param mode
     *            defines the mode of the update (replace, merge, remove)
     */
    @JsonCreator
    public DdiConfigData(@JsonProperty(value = "data") final Map<String, DdiConfigDataDevice> data,
                         @JsonProperty(value = "mode") final DdiUpdateMode mode) {
        this.data = data;
        this.mode = mode;
    }

    public Map<String, DdiConfigDataDevice> getData() {
        return data;
    }

    public DdiUpdateMode getMode() {
        return mode;
    }

    @Override
    public String toString() {
        return "ConfigData [data=" + data + ", mode=" + mode + ", toString()=" + super.toString() + "]";
    }

}
