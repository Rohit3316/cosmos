package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.Vehicle;

public interface VehicleCreate {

    /**
     * @param name
     *            for {@link Vehicle#getName()} filled with the vehicle model name
     * @return updated builder instance
     */
    VehicleCreate name(@Size(min = 1, max = Vehicle.VEHICLE_NAME_MAX_SIZE) @NotNull String name);


    /**
     * @return peek on current state of {@link Vehicle} in the builder
     */
    Vehicle build();
}
