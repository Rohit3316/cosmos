package org.eclipse.hawkbit.repository.model;

import java.util.List;

/**
 * This interface represents an Esp (ECU Specific Package) and provides methods to access its related data.
 *
 */
public interface Esp extends BaseSupportPackage {

    /**
     * Retrieves a list of {@link EspEcuRollout} objects associated with this Esp.
     * Each {@link EspEcuRollout} represents a specific ECU node address,  rollout, controllerId association for the Esp.
     *
     * @return A list of {@link EspEcuRollout} objects representing the ECUNodeAddress, rollout and controllerId associated with this Esp.
     */
    List<EspEcuRollout> getEspEcuRollouts();


}