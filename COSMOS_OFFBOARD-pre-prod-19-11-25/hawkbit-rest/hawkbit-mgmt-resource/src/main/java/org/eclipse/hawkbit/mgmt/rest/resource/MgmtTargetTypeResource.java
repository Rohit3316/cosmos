/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.Hidden;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetType;
import org.cosmos.models.mgmt.distributionsettype.constants.MgmtDistributionSetTypeAssignment;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetType;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetTypeRequestBodyPost;
import org.cosmos.models.mgmt.targettype.dto.MgmtTargetTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetTypeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetTypeMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Resource handling for {@link TargetType} CRUD operations.
 */
@RestController
@Hidden
public class MgmtTargetTypeResource implements MgmtTargetTypeRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetTypeResource.class);

    private final TargetTypeManagement targetTypeManagement;
    private final EntityFactory entityFactory;

    public MgmtTargetTypeResource(final TargetTypeManagement targetTypeManagement, final EntityFactory entityFactory) {
        this.targetTypeManagement = targetTypeManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTargetType>> getTargetTypes(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            @PathVariable("tenantId") final Long tenantId) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetTypeSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<TargetType> findTargetTypesAll;
        long countTargetTypesAll;
        if (rsqlParam != null) {
            findTargetTypesAll = targetTypeManagement.findByRsql(pageable, rsqlParam);
            countTargetTypesAll = ((Page<TargetType>) findTargetTypesAll).getTotalElements();
        } else {
            findTargetTypesAll = targetTypeManagement.findAll(pageable);
            countTargetTypesAll = targetTypeManagement.count();
        }

        final List<MgmtTargetType> rest = MgmtTargetTypeMapper.toListResponse(findTargetTypesAll.getContent(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, countTargetTypesAll));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetType> getTargetType(@PathVariable("typeId") final Long typeId, @PathVariable("tenantId") Long tenantId) {
        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(typeId);
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(foundType, tenantId);
        MgmtTargetTypeMapper.addLinks(response, tenantId);
        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteTargetType(@PathVariable("typeId") final Long typeId) {
        LOG.debug("Delete {} target type", typeId);
        targetTypeManagement.delete(typeId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetType> updateTargetType(@PathVariable("typeId") final Long typeId,
                                                           @RequestBody final MgmtTargetTypeRequestBodyPut restTargetType,
                                                           @PathVariable("tenantId") final Long tenantId) {

        final TargetType updated = targetTypeManagement
                .update(entityFactory.targetType().update(typeId).name(restTargetType.getName())
                        .description(restTargetType.getDescription()).colour(restTargetType.getColour()));
        final MgmtTargetType response = MgmtTargetTypeMapper.toResponse(updated, tenantId);
        MgmtTargetTypeMapper.addLinks(response, tenantId);
        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtTargetType>> createTargetTypes(
            @RequestBody final List<MgmtTargetTypeRequestBodyPost> targetTypes,
            @PathVariable("tenantId") final Long tenantId) {

        final List<TargetType> createdTargetTypes = targetTypeManagement
                .create(MgmtTargetTypeMapper.targetFromRequest(entityFactory, targetTypes));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtTargetTypeMapper.toListResponse(createdTargetTypes, tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtDistributionSetType>> getCompatibleDistributionSets(
            @PathVariable("typeId") final Long typeId, @PathVariable("tenantId") final Long tenantId) {

        final TargetType foundType = findTargetTypeWithExceptionIfNotFound(typeId);
        return ResponseEntity
                .ok(MgmtDistributionSetTypeMapper.toListResponse(foundType.getCompatibleDistributionSetTypes(), tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> removeCompatibleDistributionSet(@PathVariable("typeId") final Long typeId,
                                                                @PathVariable("dsTypeId") final Long dsTypeId) {

        targetTypeManagement.unassignDistributionSetType(typeId, dsTypeId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> addCompatibleDistributionSets(@PathVariable("typeId") final Long typeId,
                                                              @RequestBody final List<MgmtDistributionSetTypeAssignment> distributionSetTypeIds) {

        targetTypeManagement.assignCompatibleDistributionSetTypes(typeId, distributionSetTypeIds.stream()
                .map(MgmtDistributionSetTypeAssignment::getId).toList());
        return ResponseEntity.ok().build();
    }

    private TargetType findTargetTypeWithExceptionIfNotFound(final Long typeId) {
        return targetTypeManagement.get(typeId)
                .orElseThrow(() -> new EntityNotFoundException(TargetType.class, typeId));
    }

}