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

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfigDataSoftware {

    @Schema(example = "160")
    private String swComponentID;
    @Schema(example = "7")
    private String swVersion;


    @JsonCreator
    public DdiConfigDataSoftware(@JsonProperty(value = "swComponentID") final String swComponentID,
                                 @JsonProperty(value = "swVersion") final String swVersion) {
        this.swComponentID = swComponentID;
        this.swVersion = swVersion;
    }

    public String getSwComponentID() {
        return swComponentID;
    }

    public void setSwComponentID(String swComponentID) {
        this.swComponentID = swComponentID;
    }

    public String getSwVersion() {
        return swVersion;
    }

    public void setSwVersion(String swVersion) {
        this.swVersion = swVersion;
    }

    @Override
    public String toString() {
        return "DdiConfigDataSoftware{" +
                "swComponentID='" + swComponentID + '\'' +
                ", swVersion='" + swVersion + '\'' +
                '}';
    }
}
