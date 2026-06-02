/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.distributionset.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.LinkRelation;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;
import java.util.Objects;

/**
 * Representation of an Action Id as a Json Object with link to the Action
 * resource
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MgmtActionId extends RepresentationModel<MgmtActionId> {

	@Schema(example = "7")
    private long actionId;

    public MgmtActionId() {
    }

    /**
     * Constructor
     *
     * @param actionId
     *            the actionId
     * @param controllerId
     *            the controller Id
     */
    public MgmtActionId(final String controllerId, final long actionId, final Long tenantId) {
        this.actionId = actionId;
        String selfHref = UriComponentsBuilder.fromPath(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING + "/{controllerId}/actions/{actionId}")
                .buildAndExpand(Map.of(
                        "controllerId", controllerId,
                        "actionId", actionId,
                        "tenantId", tenantId
                ))
                .toUriString();
        add(Link.of(selfHref, LinkRelation.of("self")));
    }

    @JsonProperty("id")
    public long getActionId() {
        return actionId;
    }

    public void setActionId(final long actionId) {
        this.actionId = actionId;
    }

    @Override
    public boolean equals(final Object obj) {
        return super.equals(obj) && this.getClass().isInstance(obj) && actionId == ((MgmtActionId) obj).getActionId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), actionId);
    }
}
