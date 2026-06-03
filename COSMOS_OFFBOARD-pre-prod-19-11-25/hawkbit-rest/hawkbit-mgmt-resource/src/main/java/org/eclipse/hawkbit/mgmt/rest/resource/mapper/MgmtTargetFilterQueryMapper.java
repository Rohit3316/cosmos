/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtDistributionSetAutoAssignment;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQuery;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQueryRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.builder.TargetFilterQueryCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtTargetFilterQueryMapper {

    private MgmtTargetFilterQueryMapper() {
        // Utility class
    }

    public static List<MgmtTargetFilterQuery> toResponse(final List<TargetFilterQuery> filters,
                                                  final boolean confirmationFlowEnabled, final Long tenantId) {
        if (CollectionUtils.isEmpty(filters)) {
            return Collections.emptyList();
        }
        return filters.stream().map(filter -> toResponse(filter, confirmationFlowEnabled, tenantId)).collect(Collectors.toList());
    }

    public static MgmtTargetFilterQuery toResponse(final TargetFilterQuery filter, final boolean confirmationFlowEnabled, final Long tenantId) {
        final MgmtTargetFilterQuery targetRest = new MgmtTargetFilterQuery();
        targetRest.setFilterId(filter.getId());
        targetRest.setName(filter.getName());
        targetRest.setQuery(filter.getQuery());

        targetRest.setCreatedBy(filter.getCreatedBy());
        targetRest.setLastModifiedBy(filter.getLastModifiedBy());

        targetRest.setCreatedAt(filter.getCreatedAt());
        targetRest.setLastModifiedAt(filter.getLastModifiedAt());

        final DistributionSet distributionSet = filter.getAutoAssignDistributionSet();
        if (distributionSet != null) {
            targetRest.setAutoAssignDistributionSet(distributionSet.getId());
            targetRest.setAutoAssignUserAcceptanceRequired(filter.getAutoAssignUserAcceptanceRequired());
            filter.getAutoAssignWeight().ifPresent(targetRest::setAutoAssignWeight);
            if (confirmationFlowEnabled) {
                targetRest.setConfirmationRequired(filter.isConfirmationRequired());
            }
        }

        targetRest.add(
                linkTo(methodOn(MgmtTargetFilterQueryRestApi.class).getFilter(filter.getId(), tenantId)).withSelfRel().expand());

        return targetRest;
    }

    public static void addLinks(final MgmtTargetFilterQuery targetRest, final Long tenantId) {
        targetRest.add(linkTo(methodOn(MgmtTargetFilterQueryRestApi.class)
                .postAssignedDistributionSet(targetRest.getFilterId(), null, tenantId)).withRel("autoAssignDS").expand());
    }

    public static TargetFilterQueryCreate fromRequest(final EntityFactory entityFactory,
                                               final MgmtTargetFilterQueryRequestBody filterRest) {

        return entityFactory.targetFilterQuery().create().name(filterRest.getName()).query(filterRest.getQuery());
    }

    public static AutoAssignDistributionSetUpdate fromRequest(final EntityFactory entityFactory, final long filterId,
                                                       final MgmtDistributionSetAutoAssignment assignRest) {
        final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired =assignRest.getUserAcceptanceRequired();

        return entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(assignRest.getId()).userAcceptanceRequired(userAcceptanceRequired)
                .weight(assignRest.getWeight());
    }

}
