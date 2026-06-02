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
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.tag.dto.MgmtAssignedTargetRequestBody;
import org.cosmos.models.mgmt.tag.dto.MgmtTag;
import org.cosmos.models.mgmt.tag.dto.MgmtTagRequestBodyPut;
import org.cosmos.models.mgmt.tag.dto.MgmtTargetTagAssigmentResult;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetTagRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTagMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST Resource handling for tag CRUD operations.
 *
 */
@RestController
@Hidden
public class MgmtTargetTagResource implements MgmtTargetTagRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetTagResource.class);

    private final TargetTagManagement tagManagement;

    private final TargetManagement targetManagement;

    private final EntityFactory entityFactory;

    private final TenantConfigHelper tenantConfigHelper;

    MgmtTargetTagResource(final TargetTagManagement tagManagement, final TargetManagement targetManagement,
                          final EntityFactory entityFactory, final SystemSecurityContext securityContext,
                          final TenantConfigurationManagement configurationManagement) {
        this.tagManagement = tagManagement;
        this.targetManagement = targetManagement;
        this.entityFactory = entityFactory;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(securityContext, configurationManagement);
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTag>> getTargetTags(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            @PathVariable("tenantId") final Long tenantId) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTagSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        Page<TargetTag> findTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = this.tagManagement.findAll(pageable);

        } else {
            findTargetsAll = this.tagManagement.findByRsql(pageable, rsqlParam);

        }

        final List<MgmtTag> rest = MgmtTagMapper.toResponse(findTargetsAll.getContent(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, findTargetsAll.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTag> getTargetTag(@PathVariable("tagId") final Long tagId,
                                                @PathVariable("tenantId") final Long tenantId) {
        final TargetTag tag = findTargetTagById(tagId);

        final MgmtTag response = MgmtTagMapper.toResponse(tag, tenantId);
        MgmtTagMapper.addLinks(tag, response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtTag>> createTargetTags(@RequestBody final List<MgmtTagRequestBodyPut> tags,
                                                          @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("creating {} target tags", tags.size());
        final List<TargetTag> createdTargetTags = this.tagManagement
                .create(MgmtTagMapper.mapTagFromRequest(entityFactory, tags));
        return new ResponseEntity<>(MgmtTagMapper.toResponse(createdTargetTags, tenantId), HttpStatus.CREATED);
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTag> updateTargetTag(@PathVariable("tagId") final Long tagId,
                                                   @PathVariable("tenantId") final Long tenantId,
                                                   @RequestBody final MgmtTagRequestBodyPut restTargetTagRest) {
        LOG.debug("update {} target tag", restTargetTagRest);

        final TargetTag updateTargetTag = tagManagement
                .update(entityFactory.tag().update(tagId).name(restTargetTagRest.getName())
                        .description(restTargetTagRest.getDescription()).colour(restTargetTagRest.getColour()));

        LOG.debug("target tag updated");

        final MgmtTag response = MgmtTagMapper.toResponse(updateTargetTag, tenantId);
        MgmtTagMapper.addLinks(updateTargetTag, response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteTargetTag(@PathVariable("tagId") final Long tagId,
                                                @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Delete {} target tag", tagId);
        final TargetTag targetTag = findTargetTagById(tagId);

        this.tagManagement.delete(targetTag.getName());

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(@PathVariable("tagId") final Long tagId,
                                                                    @PathVariable("tenantId") final Long tenantId,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                    @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> findTargetsAll;
        if (rsqlParam == null) {
            findTargetsAll = targetManagement.findByTag(pageable, tagId);
        } else {
            findTargetsAll = targetManagement.findByRsqlAndTag(pageable, rsqlParam, tagId);
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(findTargetsAll.getContent(), tenantConfigHelper, tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, findTargetsAll.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtTargetTagAssigmentResult> toggleTagAssignment(
            @PathVariable("tagId") final Long tagId, @PathVariable("tenantId") final Long tenantId,
            @RequestBody final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Toggle Target assignment {} for target tag {}", assignedTargetRequestBodies.size(), tagId);

        final TargetTag targetTag = findTargetTagById(tagId);
        final TargetTagAssignmentResult assigmentResult = this.targetManagement
                .toggleTagAssignment(findTargetControllerIds(assignedTargetRequestBodies), targetTag.getName());

        final MgmtTargetTagAssigmentResult tagAssigmentResultRest = new MgmtTargetTagAssigmentResult();
        tagAssigmentResultRest.setAssignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getAssignedEntity(), tenantConfigHelper, tenantId));
        tagAssigmentResultRest.setUnassignedTargets(
                MgmtTargetMapper.toResponse(assigmentResult.getUnassignedEntity(), tenantConfigHelper, tenantId));
        return ResponseEntity.ok(tagAssigmentResultRest);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtTarget>> assignTargets(@PathVariable("tagId") final Long tagId, @PathVariable("tenantId") final Long tenantId,
                                                          @RequestBody final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        LOG.debug("Assign Targets {} for target tag {}", assignedTargetRequestBodies.size(), tagId);
        final List<Target> assignedTarget = this.targetManagement
                .assignTag(findTargetControllerIds(assignedTargetRequestBodies), tagId);
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtTargetMapper.toResponse(assignedTarget, tenantConfigHelper, tenantId));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unassignTarget(@PathVariable("tagId") final Long tagId,
                                               @TraceableField @PathVariable("controllerId") final String controllerId,
                                               @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received unassign Target request");
        this.targetManagement.unAssignTag(controllerId, tagId);
        return ResponseEntity.ok().build();
    }

    private TargetTag findTargetTagById(final Long tagId) {
        return tagManagement.get(tagId)
                .orElseThrow(() -> new EntityNotFoundException(TargetTag.class, tagId));
    }

    private List<String> findTargetControllerIds(
            final List<MgmtAssignedTargetRequestBody> assignedTargetRequestBodies) {
        return assignedTargetRequestBodies.stream().map(MgmtAssignedTargetRequestBody::getControllerId)
                .collect(Collectors.toList());
    }

}