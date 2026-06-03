/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
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

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.action.dto.MgmtAction;
import org.cosmos.models.mgmt.action.dto.MgmtDeploymentLog;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDeploymentLogRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.eclipse.hawkbit.utils.MapperUtil;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtActionMapper {

    private MgmtActionMapper() {
        // Utility class
    }

    /**
     * Create a response for actions.
     *
     * @param actions list of actions
     * @param repMode the representation mode
     * @return the response
     */
    public static List<MgmtAction> toResponse(final Collection<Action> actions, final MgmtRepresentationMode repMode, final long tenantId, final DeploymentManagement deploymentManagement) {
        if (actions == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(actions.stream().map(action -> MgmtActionMapper.toResponse(action, repMode, tenantId, deploymentManagement))
                .toList());
    }

    public static MgmtAction toResponse(final Action action, final MgmtRepresentationMode repMode, final long tenantId, final DeploymentManagement deploymentManagement) {
        final String controllerId = action.getTarget().getControllerId();
        if (repMode == MgmtRepresentationMode.COMPACT) {
            return MgmtTargetMapper.toResponse(controllerId, action, tenantId, deploymentManagement);
        }
        return MgmtTargetMapper.toResponseWithLinks(controllerId, action, tenantId, deploymentManagement);
    }

    /**
     * Create list of {@link MgmtDeploymentLog} response from given collection of {@link DeploymentLog}, actionId and tenantId.
     *
     * @param deploymentLogs the collection of {@link DeploymentLog}
     * @param actionId       the actionId
     * @return List<MgmtDeploymentLog> the {@link MgmtDeploymentLog} list
     */
    public static List<MgmtDeploymentLog> toResponse(final Collection<DeploymentLog> deploymentLogs, final long actionId, final String controllerId) {
        return deploymentLogs.stream().map(deploymentLog -> toResponse(deploymentLog, actionId, controllerId)).toList();
    }

    /**
     * Create {@link MgmtDeploymentLog} response from given {@link DeploymentLog}, actionId and tenantId.
     * @param deploymentLog the {@link DeploymentLog}
     * @param actionId      the actionId
     * @return {@link MgmtDeploymentLog}
     */
    public static MgmtDeploymentLog toResponse(final DeploymentLog deploymentLog, final long actionId, final String controllerId) {
        return MapperUtil.convert(deploymentLog, MgmtDeploymentLog.class).add(linkTo(methodOn(MgmtDeploymentLogRestApi.class)
                .downloadDeploymentLog(controllerId, actionId, deploymentLog.getId())).withRel(MgmtRestConstants.DEPLOYMENT_LOGS_DOWNLOAD));
    }

}
