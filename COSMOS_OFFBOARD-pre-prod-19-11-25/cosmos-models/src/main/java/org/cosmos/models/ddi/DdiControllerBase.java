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
import lombok.Setter;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.springframework.hateoas.RepresentationModel;

/**
 * {@link DdiControllerBase} resource content.
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiControllerBase extends RepresentationModel<DdiControllerBase> {

    @JsonProperty
    private DdiConfig config;

    @JsonProperty
    @Setter
    private Boolean canceled;


    /**
     * Constructor.
     *
     * @param config configuration of the SP target
     */
    public DdiControllerBase(final DdiConfig config) {
        this.config = config;
    }

    public DdiControllerBase() {
        // needed for json create
    }

    public DdiConfig getConfig() {
        return config;
    }

    public Boolean setCanceledBasedOnExecutionStatus(DeviceActionStatus executionStatus) {
        if ((executionStatus == DeviceActionStatus.CANCELED_ACCEPT)
                || (executionStatus == DeviceActionStatus.CANCELED)
                || (executionStatus == DeviceActionStatus.CANCELING)) {
            return true;
        } else if (executionStatus == DeviceActionStatus.CANCELED_REJECT) {
            return false;
        } else {
            return null; // Do not include the field in the response
        }
    }


}