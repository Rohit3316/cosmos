/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.tag.dto.MgmtTag;
import org.cosmos.models.mgmt.tag.dto.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 *
 */
public final class MgmtTagMapper {
    private MgmtTagMapper() {
        // Utility class
    }

    public static List<MgmtTag> toResponse(final List<TargetTag> targetTags, final Long tenantId) {
        final List<MgmtTag> tagsRest = new ArrayList<>();
        if (targetTags == null) {
            return tagsRest;
        }

        for (final TargetTag target : targetTags) {
            final MgmtTag response = toResponse(target, tenantId);

            tagsRest.add(response);
        }
        return new ResponseList<>(tagsRest);
    }

    public static MgmtTag toResponse(final TargetTag targetTag, final Long tenantId) {
        final MgmtTag response = new MgmtTag();
        if (targetTag == null) {
            return response;
        }

        mapTag(response, targetTag);

        response.add(
                linkTo(methodOn(MgmtTargetTagRestApi.class).getTargetTag(targetTag.getId(), tenantId)).withSelfRel().expand());

        return response;
    }

    public static void addLinks(final TargetTag targetTag, final MgmtTag response,final Long tenantId) {
        response.add(linkTo(methodOn(MgmtTargetTagRestApi.class).getAssignedTargets(targetTag.getId(),tenantId,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("assignedTargets")
                        .expand());

    }

    public static List<MgmtTag> toResponseDistributionSetTag(final Long tenantId, final List<DistributionSetTag> distributionSetTags) {
        final List<MgmtTag> tagsRest = new ArrayList<>();
        if (distributionSetTags == null) {
            return tagsRest;
        }

        for (final DistributionSetTag distributionSetTag : distributionSetTags) {
            final MgmtTag response = toResponse(tenantId, distributionSetTag);

            tagsRest.add(response);
        }
        return new ResponseList<>(tagsRest);
    }

    public static MgmtTag toResponse(final Long tenantId, final DistributionSetTag distributionSetTag) {
        final MgmtTag response = new MgmtTag();
        if (distributionSetTag == null) {
            return null;
        }

        mapTag(response, distributionSetTag);

        response.add(
                linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getDistributionSetTag(tenantId, distributionSetTag.getId()))
                        .withSelfRel().expand());

        return response;
    }

    public static void addLinks(final Long tenantId, final DistributionSetTag distributionSetTag, final MgmtTag response) {
        response.add(linkTo(methodOn(MgmtDistributionSetTagRestApi.class).getAssignedDistributionSets(
                tenantId, distributionSetTag.getId(), MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null))
                        .withRel("assignedDistributionSets").expand());
    }

    public static List<TagCreate> mapTagFromRequest(final EntityFactory entityFactory,
            final Collection<MgmtTagRequestBodyPut> tags) {
        return tags.stream()
                .map(tagRest -> entityFactory.tag().create().name(tagRest.getName())
                        .description(tagRest.getDescription()).colour(tagRest.getColour()))
                .collect(Collectors.toList());
    }

    private static void mapTag(final MgmtTag response, final Tag tag) {
        MgmtRestModelMapper.mapNamedToNamed(response, tag);
        response.setTagId(tag.getId());
        response.setColour(tag.getColour());
    }
}
