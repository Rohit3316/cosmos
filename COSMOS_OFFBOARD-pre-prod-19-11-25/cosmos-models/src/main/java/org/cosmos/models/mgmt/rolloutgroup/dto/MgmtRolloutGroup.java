/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rolloutgroup.dto;

import org.cosmos.models.mgmt.rollout.dto.AbstractMgmtRolloutConditionsEntity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Model for defining the Attributes of a Rollout Group
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtRolloutGroup extends AbstractMgmtRolloutConditionsEntity {

	@Schema(example = "controllerId==1C4SJVGJ0NS100113*")
    private String targetFilterQuery;
	
	@Schema(example = "20.0")
    private Float targetPercentage;
	
	@Schema(example = "false")
    private Boolean confirmationRequired;

    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    public void setTargetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = targetFilterQuery;
    }

    public Float getTargetPercentage() {
        return targetPercentage;
    }

    public void setTargetPercentage(Float targetPercentage) {
        this.targetPercentage = targetPercentage;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
