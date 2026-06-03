/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Set;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.action.dto.MgmtAction;
import org.cosmos.models.mgmt.action.dto.MgmtDeploymentLog;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtActionMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ConditionalOnProperty(name = "hawkbit.rest.MgmtActionResource.enabled", matchIfMissing = true)
@Tag(name = "Actions")
public class MgmtActionResource implements MgmtActionRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtActionResource.class);

    private final DeploymentManagement deploymentManagement;

    public MgmtActionResource(final DeploymentManagement deploymentManagement) {
        this.deploymentManagement = deploymentManagement;
    }

    @Override
    @TenantAware
    @Deprecated
    public ResponseEntity<PagedList<MgmtAction>> getActions(final int pagingOffsetParam, final int pagingLimitParam,
                                                            final String sortParam, final String rsqlParam, final String representationModeParam, final Long tenantId) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> actions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            actions = this.deploymentManagement.findActions(rsqlParam, pageable);
            totalActionCount = this.deploymentManagement.countActions(rsqlParam);
        } else {
            actions = this.deploymentManagement.findActionsAll(pageable);
            totalActionCount = this.deploymentManagement.countActionsAll();
        }

        final MgmtRepresentationMode repMode = getRepresentationModeFromString(representationModeParam);

        return ResponseEntity
                .ok(new PagedList<>(MgmtActionMapper.toResponse(actions.getContent(), repMode, tenantId,deploymentManagement), totalActionCount));

    }

    @Override
    @TenantAware
    @TraceableMethod
    @Deprecated
    public ResponseEntity<MgmtAction> getAction( @TraceableField final Long actionId, final Long tenantId) {

        LOG.debug("Received get action request");

        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        return ResponseEntity.ok(MgmtActionMapper.toResponse(action, MgmtRepresentationMode.FULL, tenantId, deploymentManagement));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtDeploymentLog>> getActionsDeploymentLogs(@PathVariable("controllerId") String controllerId, @TraceableField Long actionId, final Long tenantId) {
        LOG.debug("Received get action deployment logs request");
        Action action = deploymentManagement.findByTargetIdAndActionIdAndActive(controllerId, actionId, true).orElseThrow(EntityNotFoundException::new);
        Set<DeploymentLog> deploymentLogs = action.getDeploymentLogs();
        return ResponseEntity
                .ok(new PagedList<>(MgmtActionMapper.toResponse(deploymentLogs, actionId, controllerId), deploymentLogs.size()));
    }

    private MgmtRepresentationMode getRepresentationModeFromString(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam)
                .orElseGet(() -> {
                    // no need for a 400, just apply a safe fallback
                    LOG.warn("Received an invalid representation mode: {}", representationModeParam);
                    return MgmtRepresentationMode.COMPACT;
                });
    }

}