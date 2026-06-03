/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.action.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtBaseEntity;
import org.cosmos.models.mgmt.MgmtMaintenanceWindow;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * A json annotated rest model for Action to RESTful API representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtAction extends MgmtBaseEntity {

    @JsonProperty("id")
    @Schema(example = "7")
    private Long actionId;

    @JsonProperty
    @Schema(example = "finished")
    private String status;

    @JsonProperty
    @Schema(example = "3")
    private int actionStatusCount;

    @JsonProperty
    @Schema(example = "1691065903238")
    private Long forceTime;

    @JsonProperty(value = "forceType")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;

    @JsonProperty
    @Schema(example = "600")
    private Integer weight;

    @JsonProperty
    @Schema(hidden = true)
    private MgmtMaintenanceWindow maintenanceWindow;

    @JsonProperty
    @Schema(example = "1")
    private Long rolloutId;

    @JsonProperty
    @Schema(example = "200")
    private String rolloutName;

    @JsonProperty
    @Schema(example = "3")
    private Long rolloutGroupId;

    @JsonProperty
    @Schema(example = "3")
    private Long targetId;

    @JsonProperty
    @Schema(example = "200")
    private Integer lastStatusCode;

    public MgmtMaintenanceWindow getMaintenanceWindow() {
        return maintenanceWindow;
    }

    public void setMaintenanceWindow(final MgmtMaintenanceWindow maintenanceWindow) {
        this.maintenanceWindow = maintenanceWindow;
    }

    public Long getForceTime() {
        return forceTime;
    }

    public void setForceTime(final Long forceTime) {
        this.forceTime = forceTime;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(final Integer weight) {
        this.weight = weight;
    }

    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    public void setUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public Long getActionId() {
        return actionId;
    }

    public void setActionId(final Long actionId) {
        this.actionId = actionId;
    }


    public Long getRollout() {
        return rolloutId;
    }

    public void setRollout(final Long rolloutId) {
        this.rolloutId = rolloutId;
    }

    public String getRolloutName() {
        return rolloutName;
    }

    public void setRolloutName(final String rolloutName) {
        this.rolloutName = rolloutName;
    }

    public int getActionStatusCount() {
        return actionStatusCount;
    }

    public void setActionStatusCount(final int actionStatusCount) {
        this.actionStatusCount = this.actionStatusCount;
    }

    public Long getRolloutGroupId() {
        return rolloutGroupId;
    }

    public void setRolloutGroupId(final Long rolloutGroupId) {
        this.rolloutGroupId = rolloutGroupId;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(final Long targetId) {
        this.targetId = targetId;
    }

    public Integer getLastStatusCode() {
        return lastStatusCode;
    }

    public void setLastStatusCode(final Integer lastStatusCode) {
        this.lastStatusCode = lastStatusCode;
    }

}
