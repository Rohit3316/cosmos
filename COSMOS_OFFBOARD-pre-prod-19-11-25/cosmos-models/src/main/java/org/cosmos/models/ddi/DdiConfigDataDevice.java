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
import java.util.List;

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiConfigDataDevice {

    @Schema(example = "9")
    private String hwVersion;
    @Schema(example = "10")
    private String serialNumber;
    @Schema(example = "xyz")
    private String hwModel;
    private List<DdiConfigDataSoftware> sw;

    @Schema(example = "xyz")
    private String type;
    @Schema(example = "xyz")
    private String value;

    @JsonCreator
    public DdiConfigDataDevice(
            @JsonProperty(value = "hwVersion") final String hwVersion,
            @JsonProperty(value = "serialNumber") final String serialNumber,
            @JsonProperty(value = "hwModel") final String hwModel,
            @JsonProperty(value = "sw") final List<DdiConfigDataSoftware> sw,
            @JsonProperty(value = "type") final String type,
            @JsonProperty(value = "value") final String value
    ) {
        this.hwVersion = hwVersion;
        this.serialNumber = serialNumber;
        this.hwModel = hwModel;
        this.sw = sw;
        this.type = type;
        this.value = value;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getHwVersion() {
        return hwVersion;
    }

    public void setHwVersion(String hwVersion) {
        this.hwVersion = hwVersion;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getHwModel() {
        return hwModel;
    }

    public void setHwModel(String hwModel) {
        this.hwModel = hwModel;
    }

    public List<DdiConfigDataSoftware> getSw() {
        return sw;
    }

    public void setSw(List<DdiConfigDataSoftware> sw) {
        this.sw = sw;
    }

    @Override
    public String toString() {
        return "DdiConfigDataDevice{" +
                "hwVersion='" + hwVersion + '\'' +
                ", serialNumber='" + serialNumber + '\'' +
                ", hwModel='" + hwModel + '\'' +
                ", sw=" + sw +
                ", type='" + type + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}