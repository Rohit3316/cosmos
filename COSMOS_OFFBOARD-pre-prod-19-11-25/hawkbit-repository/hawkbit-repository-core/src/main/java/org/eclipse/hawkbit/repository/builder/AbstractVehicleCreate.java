package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

public class AbstractVehicleCreate<T> extends AbstractBaseEntityBuilder {

    protected String vehicleModelName;

    public T name(final String vehicleModelName) {
        this.vehicleModelName = vehicleModelName;
        return (T) this;
    }

    public Optional<String> getVehicleModelName() {
        return Optional.ofNullable(vehicleModelName);
    }
}
