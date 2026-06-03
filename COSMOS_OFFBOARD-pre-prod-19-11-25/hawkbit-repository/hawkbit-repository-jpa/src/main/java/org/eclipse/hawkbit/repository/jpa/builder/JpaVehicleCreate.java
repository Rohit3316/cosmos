package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractVehicleCreate;
import org.eclipse.hawkbit.repository.builder.VehicleCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;

public class JpaVehicleCreate extends AbstractVehicleCreate<VehicleCreate> implements VehicleCreate {


    @Override
    public JpaVehicle build() {
        return new JpaVehicle(vehicleModelName);
    }
}
