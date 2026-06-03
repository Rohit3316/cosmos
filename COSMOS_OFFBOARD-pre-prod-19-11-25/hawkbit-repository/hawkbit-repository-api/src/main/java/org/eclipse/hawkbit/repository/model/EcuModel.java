package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * The {@link EcuModel} is the user that can do operation on the platform
 * </p>
 */
public interface EcuModel extends BaseEntity {

    int ECU_NAME_MAX_SIZE = 25;

    int ECU_NAME_MIN_SIZE = 1;

    String getEcuModelType();

    String getEcuModelName();

    String getEcuNodeId();

    List<Vehicle> getVehicleModel();

    Set<SoftwareModule> getSoftwareModules();
}
