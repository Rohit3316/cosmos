package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.VehicleBuilder;
import org.eclipse.hawkbit.repository.builder.VehicleCreate;

public class JpaVehicleBuilder implements VehicleBuilder {


    @Override
    public VehicleCreate create() {
        return new JpaVehicleCreate();
    }
}
