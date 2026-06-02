/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.targetfilter.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtBaseEntity;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * A json annotated rest model for Target Filter Queries to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetFilterQuery extends MgmtBaseEntity {

    @JsonProperty(value = "id", required = true)
    @Schema(example = "2")
    private Long filterId;

    @JsonProperty
    @Schema(example = "filterName")
    private String name;

    @JsonProperty
    @Schema(example = "name==*")
    private String query;

    @JsonProperty
    @Schema(example = "15")
    private Long autoAssignDistributionSet;

    @JsonProperty
    private MgmtRolloutUserAcceptanceRequired autoAssignUserAcceptanceRequired;

    @JsonProperty
    @Schema(example = "")
    private Integer autoAssignWeight;

    @JsonProperty
    @Schema(example = "false")
    private Boolean confirmationRequired;

    public Long getFilterId() {
        return filterId;
    }

    public void setFilterId(final Long filterId) {
        this.filterId = filterId;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(final String query) {
        this.query = query;
    }

    public Long getAutoAssignDistributionSet() {
        return autoAssignDistributionSet;
    }

    public void setAutoAssignDistributionSet(final Long autoAssignDistributionSet) {
        this.autoAssignDistributionSet = autoAssignDistributionSet;
    }

    public MgmtRolloutUserAcceptanceRequired getAutoAssignUserAcceptanceRequired() {
        return autoAssignUserAcceptanceRequired;
    }

    public void setAutoAssignUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.autoAssignUserAcceptanceRequired = userAcceptanceRequired;
    }

    public Integer getAutoAssignWeight() {
        return autoAssignWeight;
    }

    public void setAutoAssignWeight(final Integer autoAssignWeight) {
        this.autoAssignWeight = autoAssignWeight;
    }

    public Boolean getConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(final Boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }
}
