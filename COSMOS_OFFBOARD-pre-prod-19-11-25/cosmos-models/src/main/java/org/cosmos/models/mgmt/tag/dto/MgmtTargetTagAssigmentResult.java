/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.tag.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;

import java.util.List;

/**
 * * A json annotated rest model for TargetTagAssigmentResult to RESTful API
 * representation.
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtTargetTagAssigmentResult {

    @JsonProperty
    private List<MgmtTarget> assignedTargets;

    @JsonProperty
    private List<MgmtTarget> unassignedTargets;

    public void setAssignedTargets(final List<MgmtTarget> assignedTargets) {
        this.assignedTargets = assignedTargets;
    }

    public List<MgmtTarget> getAssignedTargets() {
        return assignedTargets;
    }

    public void setUnassignedTargets(final List<MgmtTarget> unassignedTargets) {
        this.unassignedTargets = unassignedTargets;
    }

    public List<MgmtTarget> getUnassignedTargets() {
        return unassignedTargets;
    }

}