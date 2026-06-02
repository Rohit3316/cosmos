/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.builder.AbstractTargetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.springframework.util.StringUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaTargetCreate extends AbstractTargetUpdateCreate<TargetCreate> implements TargetCreate {

    private final TargetTypeManagement targetTypeManagement;

    private final VehicleManagement vehicleManagement;

    /**
     * Constructor
     *
     * @param targetTypeManagement Target type management
     * @param vehicleManagement
     */
    JpaTargetCreate(final TargetTypeManagement targetTypeManagement, VehicleManagement vehicleManagement) {
        super(null);
        this.targetTypeManagement = targetTypeManagement;
        this.vehicleManagement = vehicleManagement;
    }

    @Override
    public JpaTarget build() {
        JpaTarget target;

        if (!StringUtils.hasText(securityToken)) {
            target = new JpaTarget(controllerId, name, serialNumber, vehicleModelId, vin);
        } else {
            target = new JpaTarget(controllerId, name, serialNumber, securityToken, vehicleModelId, vin);
        }

        if (targetTypeId != null){
            TargetType targetType = targetTypeManagement.get(targetTypeId)
                    .orElseThrow(() -> new EntityNotFoundException(TargetType.class, targetTypeId));
            target.setTargetType(targetType);
        }

        if(target.getVehicleModelId()!= null) {
            Vehicle vehicle = vehicleManagement.get(vehicleModelId).orElseThrow(() -> new EntityNotFoundException(Vehicle.class, vehicleModelId));
            target.setVehicleModelId(vehicle.getId());
        }
        target.setDescription(description);
        target.setUpdateStatus(getStatus().orElse(TargetUpdateStatus.UNKNOWN));
        getLastTargetQuery().ifPresent(target::setLastTargetQuery);

        return target;
    }

}
