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
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtDistributionSetAutoAssignment;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQuery;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQueryRequestBody;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterVinListQuery;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterVinListRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetFilterQueryRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetFilterQueryMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.AutoAssignDistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
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

import java.time.Instant;
import java.util.List;

/**
 * REST Resource handling target CRUD operations.
 */
@RestController
@Hidden
public class MgmtTargetFilterQueryResource implements MgmtTargetFilterQueryRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetFilterQueryResource.class);

    private final TargetFilterQueryManagement filterManagement;

    private final EntityFactory entityFactory;

    private final TenantConfigHelper tenantConfigHelper;

    private final SystemManagement systemManagement;

    private final TargetTagManagement targetTagManagement;

    private final TargetManagement targetManagement;


    MgmtTargetFilterQueryResource(final TargetFilterQueryManagement filterManagement, final EntityFactory entityFactory,
                                  final SystemSecurityContext systemSecurityContext,
                                  final TenantConfigurationManagement tenantConfigurationManagement,
                                  final SystemManagement systemManagement,
                                  final TargetTagManagement targetTagManagement,
                                  final TargetManagement targetManagement) {
        this.filterManagement = filterManagement;
        this.entityFactory = entityFactory;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
        this.systemManagement = systemManagement;
        this.targetTagManagement = targetTagManagement;
        this.targetManagement = targetManagement;
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetFilterQuery> getFilter(@PathVariable("filterId") final Long filterId, @PathVariable("tenantId") final Long tenantId) {
        final TargetFilterQuery findTarget = findFilterWithExceptionIfNotFound(filterId);
        // to single response include poll status
        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(findTarget,
                tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
        MgmtTargetFilterQueryMapper.addLinks(response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getFilters(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam, @PathVariable("tenantId") final Long tenantId) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<TargetFilterQuery> findTargetFiltersAll;
        final Long countTargetsAll;
        if (rsqlParam != null) {
            final Page<TargetFilterQuery> findFilterPage = filterManagement.findByRsql(pageable, rsqlParam);
            countTargetsAll = findFilterPage.getTotalElements();
            findTargetFiltersAll = findFilterPage;
        } else {
            findTargetFiltersAll = filterManagement.findAll(pageable);
            countTargetsAll = filterManagement.count();
        }

        final List<MgmtTargetFilterQuery> rest = MgmtTargetFilterQueryMapper
                .toResponse(findTargetFiltersAll.getContent(), tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, countTargetsAll));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetFilterQuery> createFilter(
            @RequestBody final MgmtTargetFilterQueryRequestBody filter, @PathVariable("tenantId") final Long tenantId) {
        final TargetFilterQuery createdTarget = filterManagement
                .create(MgmtTargetFilterQueryMapper.fromRequest(entityFactory, filter));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(createdTarget,
                tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
        MgmtTargetFilterQueryMapper.addLinks(response, tenantId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetFilterQuery> updateFilter(@PathVariable("filterId") final Long filterId,
                                                              @RequestBody final MgmtTargetFilterQueryRequestBody targetFilterRest, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("updating target filter query {}", filterId);

        final TargetFilterQuery updateFilter = filterManagement
                .update(entityFactory.targetFilterQuery().update(filterId).name(targetFilterRest.getName())
                        .query(targetFilterRest.getQuery()));

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter,
                tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
        MgmtTargetFilterQueryMapper.addLinks(response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteFilter(@PathVariable("filterId") final Long filterId, @PathVariable("tenantId") final Long tenantId) {
        filterManagement.delete(filterId);
        LOG.debug("{} target filter query deleted, return status {}", filterId, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetFilterQuery> postAssignedDistributionSet(
            @PathVariable("filterId") final Long filterId,
            @RequestBody final MgmtDistributionSetAutoAssignment autoAssignRequest, @PathVariable("tenantId") final Long tenantId) {

        final boolean confirmationRequired = autoAssignRequest.isConfirmationRequired() == null
                ? tenantConfigHelper.isConfirmationFlowEnabled()
                : autoAssignRequest.isConfirmationRequired();

        final AutoAssignDistributionSetUpdate update = MgmtTargetFilterQueryMapper
                .fromRequest(entityFactory, filterId, autoAssignRequest).confirmationRequired(confirmationRequired);

        final TargetFilterQuery updateFilter = filterManagement.updateAutoAssignDS(update);

        final MgmtTargetFilterQuery response = MgmtTargetFilterQueryMapper.toResponse(updateFilter,
                tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
        MgmtTargetFilterQueryMapper.addLinks(response, tenantId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(
            @PathVariable("filterId") final Long filterId, @PathVariable("tenantId") final Long tenantId) {
        final TargetFilterQuery filter = findFilterWithExceptionIfNotFound(filterId);
        final DistributionSet autoAssignDistributionSet = filter.getAutoAssignDistributionSet();

        if (autoAssignDistributionSet == null) {
            return ResponseEntity.noContent().build();
        }

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(autoAssignDistributionSet, tenantId);
        MgmtDistributionSetMapper.addLinks(autoAssignDistributionSet, response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteAssignedDistributionSet(@PathVariable("filterId") final Long filterId, @PathVariable("tenantId") final Long tenantId) {
        filterManagement.updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(null));

        return ResponseEntity.noContent().build();
    }

    private TargetFilterQuery findFilterWithExceptionIfNotFound(final Long filterId) {
        return filterManagement.get(filterId)
                .orElseThrow(() -> new EntityNotFoundException(TargetFilterQuery.class, filterId));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetFilterVinListQuery> createTargetFilterVinList(
            @RequestBody MgmtTargetFilterVinListRequestBody mgmtTargetFilterVinListRequestBody, @PathVariable("tenantId") final Long tenantId) {

            String tagName = MgmtRestConstants.TAG + Instant.now().getEpochSecond();
            List<String> controllerIds = mgmtTargetFilterVinListRequestBody.getControllerIds().stream().distinct()
                    .toList();

            List<Target> targetList = targetManagement.getByControllerID(controllerIds);
            List<String> found = targetList.stream().map(Target::getControllerId).toList();

            MgmtTargetFilterVinListQuery response = new MgmtTargetFilterVinListQuery();
            response.setNotPresent(
                    controllerIds.stream().filter(id -> !(found).contains(id)).toList());

            List<String> validControllerIds = (found).stream().filter(controllerIds::contains)
                    .toList();

            if (response.getNotPresent().size() == controllerIds.size()) {
                throw new EntityNotFoundException(Target.class, controllerIds, found);
            }

            targetManagement.assignTag(validControllerIds,
                    targetTagManagement.create(entityFactory.tag().create().name(tagName)).getId());

            final MgmtTargetFilterQuery mgmtTargetFilter = MgmtTargetFilterQueryMapper.toResponse(
                    filterManagement.create(entityFactory.targetFilterQuery().create()
                            .name(MgmtRestConstants.FILTER + Instant.now().getEpochSecond()).query("tag==" + tagName)),
                    tenantConfigHelper.isConfirmationFlowEnabled(), tenantId);
            MgmtTargetFilterQueryMapper.addLinks(mgmtTargetFilter, tenantId);

            response.setTargetFilter(mgmtTargetFilter);
            return new ResponseEntity<>(response, HttpStatus.CREATED);


    }

}