/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.MgmtMetadata;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.distributionset.dto.MgmtActionId;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPost;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentResponseBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.rest.data.ResponseList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtDistributionSetMapper {

    /**
     * Default {@link NamedVersionedEntity#getVersion()}.
     */
    public static final String DEFAULT_VERSION = "1.0";

    private MgmtDistributionSetMapper() {
        // Utility class
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost}s to {@link DistributionSet}s.
     *
     * @param sets to convert
     * @return converted list of {@link DistributionSet}s
     */
    public static List<DistributionSetCreate> dsFromRequest(final Collection<MgmtDistributionSetRequestBodyPost> sets,
                                                     final EntityFactory entityFactory) {

        return sets.stream().map(dsRest -> fromRequest(dsRest, entityFactory)).toList();
    }

    /**
     * {@link MgmtDistributionSetRequestBodyPost} to {@link DistributionSet}.
     *
     * @param dsRest to convert
     * @return converted {@link DistributionSet}
     */
    private static DistributionSetCreate fromRequest(final MgmtDistributionSetRequestBodyPost dsRest,
                                                     final EntityFactory entityFactory) {

        final Map<Long, Long> smChanged = new HashMap<>();
        if (dsRest.getOs() != null) {
            smChanged.put(dsRest.getOs().getId(), Long.parseLong(dsRest.getOs().getSoftwareVersionTargetId()));
        }

        if (dsRest.getApplication() != null) {
            smChanged.put(dsRest.getApplication().getId(), Long.parseLong(dsRest.getApplication().getSoftwareVersionTargetId()));
        }

        if (dsRest.getRuntime() != null) {
            smChanged.put(dsRest.getRuntime().getId(), Long.parseLong(dsRest.getRuntime().getSoftwareVersionTargetId()));
        }

        if (dsRest.getModules() != null) {
            dsRest.getModules().forEach(module ->
                    smChanged.put(module.getId(), Long.parseLong(module.getSoftwareVersionTargetId())));
        }

        return entityFactory.distributionSet().create().name(dsRest.getName())
                .version(Objects.isNull(dsRest.getVersion()) ? DEFAULT_VERSION : dsRest.getVersion())
                .description(dsRest.getDescription()).type(dsRest.getType()).modules(smChanged)
                .requiredMigrationStep(dsRest.isRequiredMigrationStep()).softwareDowngradeEnabled(dsRest.isSoftwareDowngradeEnabled());
    }

    public static List<MetaData> fromRequestDsMetadata(final List<MgmtMetadata> metadata, final EntityFactory entityFactory) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream()
                .map(metadataRest -> entityFactory.generateDsMetadata(metadataRest.getKey(), metadataRest.getValue()))
                .toList();
    }

    public static MgmtDistributionSet toResponse(final DistributionSet distributionSet, final long tenantId) {
        if (distributionSet == null) {
            return null;
        }
        final MgmtDistributionSet response = new MgmtDistributionSet();
        MgmtRestModelMapper.mapNamedToNamed(response, distributionSet);

        response.setDsId(distributionSet.getId());
        response.setComplete(distributionSet.isComplete());
        response.setType(distributionSet.getType().getKey());
        response.setTypeName(distributionSet.getType().getName());
        response.setDeleted(distributionSet.isDeleted());
        response.setValid(distributionSet.isValid());
        response.setSoftwareDowngradeEnabled(distributionSet.isSoftwareDowngradeEnabled());

        distributionSet.getModules()
                .forEach(module -> response.getModules().add(MgmtSoftwareModuleMapper.toResponse(tenantId, module)));

        response.setRequiredMigrationStep(distributionSet.isRequiredMigrationStep());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getDistributionSet(tenantId, response.getDsId()))
                .withSelfRel().expand());

        return response;
    }

    public static void addLinks(final DistributionSet distributionSet, final MgmtDistributionSet response, final long tenantId) {
        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getAssignedSoftwareModules(tenantId, response.getDsId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null))
                .withRel(MgmtRestConstants.DISTRIBUTIONSET_V1_MODULE).expand());

        response.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class)
                .getDistributionSetType(tenantId, distributionSet.getType().getId())).withRel("type").expand());

        response.add(linkTo(methodOn(MgmtDistributionSetRestApi.class).getMetadata(tenantId, response.getDsId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("metadata")
                .expand());
    }

    public static MgmtTargetAssignmentResponseBody toResponse(final DistributionSetAssignmentResult dsAssignmentResult, final Long tenantId) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        result.setAlreadyAssigned(dsAssignmentResult.getAlreadyAssigned());
        result.setAssignedActions(dsAssignmentResult.getAssignedEntity().stream()
                .map(a -> new MgmtActionId(a.getTarget().getControllerId(), a.getId(), tenantId)).toList());
        return result;
    }

    public static MgmtTargetAssignmentResponseBody toResponse(
            final List<DistributionSetAssignmentResult> dsAssignmentResults,
            final Long tenantId) {
        final MgmtTargetAssignmentResponseBody result = new MgmtTargetAssignmentResponseBody();
        final int alreadyAssigned = dsAssignmentResults.stream()
                .mapToInt(DistributionSetAssignmentResult::getAlreadyAssigned).sum();
        final List<MgmtActionId> assignedActions = dsAssignmentResults.stream()
                .flatMap(assignmentResult -> assignmentResult.getAssignedEntity().stream())
                .map(action -> new MgmtActionId
                        (action.getTarget().getControllerId(), action.getId(), tenantId))
                .toList();
        result.setAlreadyAssigned(alreadyAssigned);
        result.setAssignedActions(assignedActions);
        return result;
    }

    public static List<MgmtDistributionSet> toResponseDistributionSets(final Collection<DistributionSet> sets, final long tenantId) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                sets.stream().map(set -> MgmtDistributionSetMapper.toResponse(set, tenantId)).toList());
    }

    public static MgmtMetadata toResponseDsMetadata(final DistributionSetMetadata metadata) {
        final MgmtMetadata metadataRest = new MgmtMetadata();
        metadataRest.setKey(metadata.getKey());
        metadataRest.setValue(metadata.getValue());
        return metadataRest;
    }

    public static List<MgmtMetadata> toResponseDsMetadata(final List<DistributionSetMetadata> metadata) {

        final List<MgmtMetadata> mappedList = new ArrayList<>(metadata.size());
        for (final DistributionSetMetadata distributionSetMetadata : metadata) {
            mappedList.add(toResponseDsMetadata(distributionSetMetadata));
        }
        return mappedList;
    }

    public static List<MgmtDistributionSet> toResponseFromDsList(final List<DistributionSet> sets, final long tenantId) {
        if (sets == null) {
            return Collections.emptyList();
        }

        return sets.stream()
                .map(set -> MgmtDistributionSetMapper.toResponse(set, tenantId))
                .toList();
    }
}
