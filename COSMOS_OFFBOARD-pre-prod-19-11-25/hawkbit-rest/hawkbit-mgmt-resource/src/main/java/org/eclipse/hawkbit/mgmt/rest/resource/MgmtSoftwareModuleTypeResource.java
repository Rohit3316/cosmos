/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.Hidden;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.softwaremoduletype.dto.MgmtSoftwareModuleType;
import org.cosmos.models.mgmt.softwaremoduletype.dto.MgmtSoftwareModuleTypeRequestBodyPost;
import org.cosmos.models.mgmt.softwaremoduletype.dto.MgmtSoftwareModuleTypeRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleTypeMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.springframework.beans.factory.annotation.Value;
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
 * REST Resource handling for {@link SoftwareModuleType} CRUD operations.
 *
 */
@RestController
@Hidden
public class MgmtSoftwareModuleTypeResource implements MgmtSoftwareModuleTypeRestApi {

    @Value("${hawkbit.default.maxAssignments}")
    private Integer defaultMaxAssignments;


    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final EntityFactory entityFactory;

    MgmtSoftwareModuleTypeResource(final SoftwareModuleTypeManagement softwareModuleTypeManagement,
            final EntityFactory entityFactory) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtSoftwareModuleType>> getTypes(
            @PathVariable("tenantId") final Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleTypeSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModuleType> findModuleTypessAll;
        Long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = softwareModuleTypeManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<SoftwareModuleType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = softwareModuleTypeManagement.findAll(pageable);
            countModulesAll = softwareModuleTypeManagement.count();
        }

        final List<MgmtSoftwareModuleType> rest = MgmtSoftwareModuleTypeMapper
                .toTypesResponse(tenantId, findModuleTypessAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSoftwareModuleType> getSoftwareModuleType(
            @PathVariable("tenantId") final Long tenantId,
            @PathVariable("typeId") final Long typeId) {
        final SoftwareModuleType foundType = findSoftwareModuleTypeWithExceptionIfNotFound(typeId);

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(tenantId, foundType));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteSoftwareModuleType(
            @PathVariable("tenantId") final Long tenantId,
            @PathVariable("typeId") final Long typeId) {
        softwareModuleTypeManagement.delete(typeId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSoftwareModuleType> updateSoftwareModuleType(
            @PathVariable("tenantId") final Long tenantId,
            @PathVariable("typeId") final Long typeId,
            @RequestBody final MgmtSoftwareModuleTypeRequestBodyPut restSoftwareModuleType) {

        final SoftwareModuleType updatedSoftwareModuleType = softwareModuleTypeManagement.update(entityFactory
                .softwareModuleType().update(typeId).description(restSoftwareModuleType.getDescription())
                .colour(restSoftwareModuleType.getColour()).maxAssignments(restSoftwareModuleType.getMaxAssignments()));

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(tenantId, updatedSoftwareModuleType));
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtSoftwareModuleType>> createSoftwareModuleTypes(
            @PathVariable("tenantId") final Long tenantId,
            @RequestBody List<MgmtSoftwareModuleTypeRequestBodyPost> softwareModuleTypes) {

        for (MgmtSoftwareModuleTypeRequestBodyPost softwareModuleType : softwareModuleTypes){
            if (softwareModuleType.getMaxAssignments() == null){
                softwareModuleType.setMaxAssignments(defaultMaxAssignments);
            }
        }

        final List<SoftwareModuleType> createdSoftwareModules = softwareModuleTypeManagement
                .create(MgmtSoftwareModuleTypeMapper.smFromRequest(entityFactory, softwareModuleTypes));

        return new ResponseEntity<>(MgmtSoftwareModuleTypeMapper.toTypesResponse(tenantId, createdSoftwareModules),
                HttpStatus.CREATED);
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long softwareModuleTypeId) {
        return softwareModuleTypeManagement.get(softwareModuleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, softwareModuleTypeId));
    }

}