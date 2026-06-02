/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetType;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetTypeRequestBodyPost;
import org.cosmos.models.mgmt.softwaremoduletype.constants.MgmtSoftwareModuleTypeAssigment;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.DistributionSetTypeCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.rest.data.ResponseList;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtDistributionSetTypeMapper {

    // private constructor, utility class
    private MgmtDistributionSetTypeMapper() {

    }

    public static List<DistributionSetTypeCreate> smFromRequest(final EntityFactory entityFactory,
                                                         final Collection<MgmtDistributionSetTypeRequestBodyPost> smTypesRest) {
        if (smTypesRest == null) {
            return Collections.emptyList();
        }

        return smTypesRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).toList();
    }

    private static DistributionSetTypeCreate fromRequest(final EntityFactory entityFactory,
                                                         final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return entityFactory.distributionSetType().create().key(smsRest.getKey()).name(smsRest.getName())
                .description(smsRest.getDescription()).colour(smsRest.getColour())
                .mandatory(getMandatoryModules(smsRest)).optional(getOptionalmodules(smsRest));
    }

    private static Collection<Long> getMandatoryModules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getMandatorymodules()).map(
                        modules -> modules.stream().map(MgmtSoftwareModuleTypeAssigment::getId).toList())
                .orElse(Collections.emptyList());
    }

    private static Collection<Long> getOptionalmodules(final MgmtDistributionSetTypeRequestBodyPost smsRest) {
        return Optional.ofNullable(smsRest.getOptionalmodules()).map(
                        modules -> modules.stream().map(MgmtSoftwareModuleTypeAssigment::getId).toList())
                .orElse(Collections.emptyList());
    }

    public static List<MgmtDistributionSetType> toListResponse(final Collection<DistributionSetType> types, final long tenantId) {
        if (types == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                types.stream().map(type -> MgmtDistributionSetTypeMapper.toResponse(type, tenantId)).toList());
    }

    public static MgmtDistributionSetType toResponse(final DistributionSetType type, final long tenantId) {
        final MgmtDistributionSetType result = new MgmtDistributionSetType();

        MgmtRestModelMapper.mapNamedToNamed(result, type);
        result.setKey(type.getKey());
        result.setModuleId(type.getId());
        result.setDeleted(type.isDeleted());
        result.setColour(type.getColour());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getDistributionSetType(tenantId, result.getModuleId()))
                .withSelfRel().expand());

        return result;
    }

    public static void addLinks(final MgmtDistributionSetType result, final long tenantId) {

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getMandatoryModules(tenantId, result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_MANDATORY_MODULES).expand());

        result.add(linkTo(methodOn(MgmtDistributionSetTypeRestApi.class).getOptionalModules(tenantId, result.getModuleId()))
                .withRel(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_OPTIONAL_MODULES).expand());
    }

}
