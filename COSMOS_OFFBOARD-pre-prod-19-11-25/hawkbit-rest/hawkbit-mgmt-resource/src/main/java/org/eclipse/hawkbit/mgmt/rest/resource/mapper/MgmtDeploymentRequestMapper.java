/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.MgmtMaintenanceWindowRequestBody;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentRequestBody;
import org.cosmos.models.mgmt.target.dto.MgmtDistributionSetAssignment;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.model.DeploymentRequestBuilder;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

/**
 * A mapper for assignment requests
 */
public final class MgmtDeploymentRequestMapper {
    private MgmtDeploymentRequestMapper() {
        // Utility class
    }

    /**
     * Convert assignment information to an {@link DeploymentRequestBuilder}
     * 
     * @param dsAssignment
     *            DS assignment information
     * @param targetId
     *            target to assign the DS to
     * @return resulting {@link DeploymentRequestBuilder}
     */
    public static DeploymentRequestBuilder createAssignmentRequestBuilder(
            final MgmtDistributionSetAssignment dsAssignment, final String targetId) {

        return createAssignmentRequestBuilder(targetId, dsAssignment.getId(), dsAssignment.getUserAcceptanceRequired(),
                dsAssignment.getForcetime(), dsAssignment.getWeight(), dsAssignment.getMaintenanceWindow());
    }

    /**
     * Convert assignment information to an {@link DeploymentRequestBuilder}
     * 
     * @param targetAssignment
     *            target assignment information
     * @param dsId
     *            DS to assign the target to
     * @return resulting {@link DeploymentRequestBuilder}
     */
    public static DeploymentRequestBuilder createAssignmentRequestBuilder(
            final MgmtTargetAssignmentRequestBody targetAssignment, final Long dsId) {
        return createAssignmentRequestBuilder(targetAssignment.getId(), dsId, targetAssignment.getUserAcceptanceRequired(),
                targetAssignment.getForcetime(), targetAssignment.getWeight(), targetAssignment.getMaintenanceWindow());
    }

    private static DeploymentRequestBuilder createAssignmentRequestBuilder(final String targetId, final Long dsId,
                                                                           final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final long forcetime, final Integer weight,
                                                                           final MgmtMaintenanceWindowRequestBody maintenanceWindow) {
        final DeploymentRequestBuilder request = DeploymentManagement.deploymentRequest(targetId, dsId)
                .setUserAcceptanceRequired(userAcceptanceRequired).setForceTime(forcetime).setWeight(weight);
        if (maintenanceWindow != null) {
            final String cronSchedule = maintenanceWindow.getSchedule();
            final String duration = maintenanceWindow.getDuration();
            final String timezone = maintenanceWindow.getTimezone();
            MaintenanceScheduleHelper.validateMaintenanceSchedule(cronSchedule, duration, timezone);
            request.setMaintenance(cronSchedule, duration, timezone);
        }
        return request;
    }
}
