/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.Hidden;
import org.cosmos.models.mgmt.MgmtId;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetType;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetTypeRequestBodyPost;
import org.cosmos.models.mgmt.distributionsettype.dto.MgmtDistributionSetTypeRequestBodyPut;
import org.cosmos.models.mgmt.softwaremoduletype.dto.MgmtSoftwareModuleType;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetTypeMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleTypeMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.SoftwareModuleTypeNotInDistributionSetTypeException;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
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

import java.util.Collections;
import java.util.List;

/**
 * REST Resource handling for {@link DistributionSetType} CRUD operations.
 */
@RestController
@Hidden
public class MgmtDistributionSetTypeResource implements MgmtDistributionSetTypeRestApi {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final DistributionSetTypeManagement distributionSetTypeManagement;

    private final EntityFactory entityFactory;

    MgmtDistributionSetTypeResource(final SoftwareModuleTypeManagement softwareModuleTypeManagement,
                                    final DistributionSetTypeManagement distributionSetTypeManagement, final EntityFactory entityFactory) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.entityFactory = entityFactory;
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtDistributionSetType>> getDistributionSetTypes(
            @PathVariable("tenantId") long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetTypeSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<DistributionSetType> findModuleTypessAll;
        long countModulesAll;
        if (rsqlParam != null) {
            findModuleTypessAll = distributionSetTypeManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<DistributionSetType>) findModuleTypessAll).getTotalElements();
        } else {
            findModuleTypessAll = distributionSetTypeManagement.findAll(pageable);
            countModulesAll = distributionSetTypeManagement.count();
        }

        final List<MgmtDistributionSetType> rest = MgmtDistributionSetTypeMapper
                .toListResponse(findModuleTypessAll.getContent(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetType> getDistributionSetType(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(typeId);

        final MgmtDistributionSetType reponse = MgmtDistributionSetTypeMapper.toResponse(foundType, tenantId);
        MgmtDistributionSetTypeMapper.addLinks(reponse, tenantId);

        return ResponseEntity.ok(reponse);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteDistributionSetType(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId) {
        distributionSetTypeManagement.delete(typeId);

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetType> updateDistributionSetType(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId,
            @RequestBody final MgmtDistributionSetTypeRequestBodyPut restDistributionSetType) {

        final DistributionSetType updated = distributionSetTypeManagement.update(entityFactory.distributionSetType()
                .update(typeId).description(restDistributionSetType.getDescription())
                .colour(restDistributionSetType.getColour()));

        final MgmtDistributionSetType reponse = MgmtDistributionSetTypeMapper.toResponse(updated, tenantId);
        MgmtDistributionSetTypeMapper.addLinks(reponse, tenantId);

        return ResponseEntity.ok(reponse);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtDistributionSetType>> createDistributionSetTypes(
            @PathVariable("tenantId") long tenantId,
            @RequestBody final List<MgmtDistributionSetTypeRequestBodyPost> distributionSetTypes) {

        final List<DistributionSetType> createdSoftwareModules = distributionSetTypeManagement
                .create(MgmtDistributionSetTypeMapper.smFromRequest(entityFactory, distributionSetTypes));

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MgmtDistributionSetTypeMapper.toListResponse(createdSoftwareModules, tenantId));
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final Long typeId) {
        return distributionSetTypeManagement.get(typeId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, typeId));
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtSoftwareModuleType>> getMandatoryModules(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(typeId);
        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toTypesResponse(tenantId, foundType.getMandatoryModuleTypes()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSoftwareModuleType> getMandatoryModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId,
            @PathVariable("moduleTypeId") final Long moduleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(typeId);
        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(moduleTypeId);

        if (!foundType.containsMandatoryModuleType(foundSmType)) {
            throw new SoftwareModuleTypeNotInDistributionSetTypeException(moduleTypeId, typeId);
        }

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(tenantId, foundSmType));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtSoftwareModuleType> getOptionalModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId,
            @PathVariable("moduleTypeId") final Long moduleTypeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(typeId);
        final SoftwareModuleType foundSmType = findSoftwareModuleTypeWithExceptionIfNotFound(moduleTypeId);

        if (!foundType.containsOptionalModuleType(foundSmType)) {
            throw new SoftwareModuleTypeNotInDistributionSetTypeException(moduleTypeId, typeId);
        }

        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toResponse(tenantId, foundSmType));
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtSoftwareModuleType>> getOptionalModules(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId) {

        final DistributionSetType foundType = findDistributionSetTypeWithExceptionIfNotFound(typeId);
        return ResponseEntity.ok(MgmtSoftwareModuleTypeMapper.toTypesResponse(tenantId, foundType.getOptionalModuleTypes()));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> removeMandatoryModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId,
            @PathVariable("moduleTypeId") final Long moduleTypeId) {
        distributionSetTypeManagement.unassignSoftwareModuleType(typeId, moduleTypeId);

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> removeOptionalModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId,
            @PathVariable("moduleTypeId") final Long moduleTypeId) {

        return removeMandatoryModule(tenantId, typeId, moduleTypeId);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> addMandatoryModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId, @RequestBody final MgmtId smtId) {

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(typeId,
                Collections.singletonList(smtId.getId()));

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> addOptionalModule(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("typeId") final Long typeId, @RequestBody final MgmtId smtId) {

        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(typeId,
                Collections.singletonList(smtId.getId()));

        return ResponseEntity.ok().build();
    }

    private SoftwareModuleType findSoftwareModuleTypeWithExceptionIfNotFound(final Long moduleTypeId) {

        return softwareModuleTypeManagement.get(moduleTypeId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, moduleTypeId));
    }
}
