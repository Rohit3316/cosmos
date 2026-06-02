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
import org.cosmos.models.mgmt.tag.dto.MgmtAssignedDistributionSetRequestBody;
import org.cosmos.models.mgmt.tag.dto.MgmtDistributionSetTagAssigmentResult;
import org.cosmos.models.mgmt.tag.dto.MgmtTag;
import org.cosmos.models.mgmt.tag.dto.MgmtTagRequestBodyPut;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTagMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
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
 * REST Resource handling for {@link DistributionSetTag} CRUD operations.
 */
@RestController
@Hidden
public class MgmtDistributionSetTagResource implements MgmtDistributionSetTagRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtDistributionSetTagResource.class);

    private final DistributionSetTagManagement distributionSetTagManagement;

    private final DistributionSetManagement distributionSetManagement;

    private final EntityFactory entityFactory;

    MgmtDistributionSetTagResource(final DistributionSetTagManagement distributionSetTagManagement,
                                   final DistributionSetManagement distributionSetManagement, final EntityFactory entityFactory) {
        this.distributionSetTagManagement = distributionSetTagManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.entityFactory = entityFactory;
    }

    private static List<Long> findDistributionSetIds(
            final List<MgmtAssignedDistributionSetRequestBody> assignedDistributionSetRequestBodies) {
        return assignedDistributionSetRequestBodies.stream()
                .map(MgmtAssignedDistributionSetRequestBody::getDistributionSetId).toList();
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTag>> getDistributionSetTags(
            @PathVariable("tenantId") Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSetTag> distributionSetTags;
        final long count;
        if (rsqlParam == null) {
            distributionSetTags = distributionSetTagManagement.findAll(pageable);
            count = distributionSetTagManagement.count();

        } else {
            final Page<DistributionSetTag> page = distributionSetTagManagement.findByRsql(pageable, rsqlParam);
            distributionSetTags = page;
            count = page.getTotalElements();

        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponseDistributionSetTag(tenantId, distributionSetTags.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, count));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTag> getDistributionSetTag(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId) {
        final DistributionSetTag distributionSetTag = findDistributionTagById(tagId);

        final MgmtTag response = MgmtTagMapper.toResponse(tenantId, distributionSetTag);
        MgmtTagMapper.addLinks(tenantId, distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtTag>> createDistributionSetTags(
            @PathVariable("tenantId") Long tenantId,
            @RequestBody final List<MgmtTagRequestBodyPut> tags) {
        LOG.debug("creating {} ds tags", tags.size());

        final List<DistributionSetTag> createdTags = distributionSetTagManagement
                .create(MgmtTagMapper.mapTagFromRequest(entityFactory, tags));

        return new ResponseEntity<>(MgmtTagMapper.toResponseDistributionSetTag(tenantId, createdTags), HttpStatus.CREATED);
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTag> updateDistributionSetTag(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId,
            @RequestBody final MgmtTagRequestBodyPut restDSTagRest) {

        final DistributionSetTag distributionSetTag = distributionSetTagManagement
                .update(entityFactory.tag().update(tagId).name(restDSTagRest.getName())
                        .description(restDSTagRest.getDescription()).colour(restDSTagRest.getColour()));

        final MgmtTag response = MgmtTagMapper.toResponse(tenantId, distributionSetTag);
        MgmtTagMapper.addLinks(tenantId, distributionSetTag, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteDistributionSetTag(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId) {
        LOG.debug("Delete {} distribution set tag", tagId);
        final DistributionSetTag tag = findDistributionTagById(tagId);

        distributionSetTagManagement.delete(tag.getName());

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtDistributionSet>> getAssignedDistributionSets(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        Page<DistributionSet> findDistrAll;
        if (rsqlParam == null) {
            findDistrAll = distributionSetManagement.findByTag(pageable, tagId);

        } else {
            findDistrAll = distributionSetManagement.findByRsqlAndTag(pageable, rsqlParam, tagId);
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper
                .toResponseFromDsList(findDistrAll.getContent(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, findDistrAll.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetTagAssigmentResult> toggleTagAssignment(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Toggle distribution set assignment {} for ds tag {}", assignedDSRequestBodies.size(),
                tagId);

        final DistributionSetTag tag = findDistributionTagById(tagId);

        final DistributionSetTagAssignmentResult assigmentResult = this.distributionSetManagement
                .toggleTagAssignment(findDistributionSetIds(assignedDSRequestBodies), tag.getName());

        final MgmtDistributionSetTagAssigmentResult tagAssigmentResultRest = new MgmtDistributionSetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getAssignedEntity(), tenantId));
        tagAssigmentResultRest.setUnassignedDistributionSets(
                MgmtDistributionSetMapper.toResponseDistributionSets(assigmentResult.getUnassignedEntity(), tenantId));

        LOG.debug("Toggled assignedDS {} and unassignedDS{}", assigmentResult.getAssigned(),
                assigmentResult.getUnassigned());

        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtDistributionSet>> assignDistributionSets(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId,
            @RequestBody final List<MgmtAssignedDistributionSetRequestBody> assignedDSRequestBodies) {
        LOG.debug("Assign DistributionSet {} for ds tag {}", assignedDSRequestBodies.size(), tagId);
        final List<DistributionSet> assignedDs = this.distributionSetManagement
                .assignTag(findDistributionSetIds(assignedDSRequestBodies), tagId);
        LOG.debug("Assigned DistributionSet {}", assignedDs.size());
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDistributionSets(assignedDs, tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> unassignDistributionSet(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("tagId") final Long tagId,
            @PathVariable("distributionSetId") final Long distributionSetId) {
        LOG.debug("Unassign ds {} for ds tag {}", distributionSetId, tagId);
        this.distributionSetManagement.unAssignTag(distributionSetId, tagId);
        return ResponseEntity.ok().build();
    }

    private DistributionSetTag findDistributionTagById(final Long tagId) {
        return distributionSetTagManagement.get(tagId)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetTag.class, tagId));
    }
}