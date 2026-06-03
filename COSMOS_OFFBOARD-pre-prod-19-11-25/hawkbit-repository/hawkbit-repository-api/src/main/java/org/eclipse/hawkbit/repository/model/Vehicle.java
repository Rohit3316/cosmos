package org.eclipse.hawkbit.repository.model;

import java.util.Set;

/**
 * <p>
 * The {@link Vehicle} is the user that can do operation on the platform
 * </p>
 */
public interface Vehicle extends BaseEntity {

    int VEHICLE_NAME_MAX_SIZE = 25;

    int VEHICLE_NAME_MIN_SIZE = 1;

    String getName();

    Set<EcuModel> getVehicleEcu();

    String getErcType();
}
