/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionset.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtMaintenanceWindowRequestBody;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * Request Body of Target for assignment operations (ID only).
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetAssignmentRequestBody {

	@Schema(example = "10")
    private String id;
	
	@Schema(example = "10")
    private long forcetime;

    @JsonProperty("userAcceptanceRequired")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;
    
    private MgmtMaintenanceWindowRequestBody maintenanceWindow;
    
    @Schema(example = "600")
    private Integer weight;
    
    @Schema(example = "true")
    private Boolean confirmationRequired;

    /**
     * JsonCreator Constructor
     * 
     * @param id
     *            Mandatory ID of the target that should be assigned
     */
    @JsonCreator
    public MgmtTargetAssignmentRequestBody(@JsonProperty(required = true, value = "id") final String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    public void setUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
    }

    public long getForcetime() {
        return forcetime;
    }

    public void setForcetime(final long forcetime) {
        this.forcetime = forcetime;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public MgmtMaintenanceWindowRequestBody getMaintenanceWindow() {
        return maintenanceWindow;
    }

    public void setMaintenanceWindow(final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

    public Boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
