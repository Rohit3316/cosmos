package org.eclipse.hawkbit.repository.model;

/**
 * This interface represents a relationship between an ECU Specific Package (ESP),
 * a Vehicle Identification Number (VIN),
 * a rollout, and an ECU node address.
 */
public interface EspEcuRollout {
    
    /**
     * Returns the ESP associated with this relationship.
     *
     * @return the ESP
     */
    Esp getSupportPackage();

    /**
     * Returns the Vehicle Identification Number (VIN) associated with this relationship.
     *
     * @return the VIN
     */
    String getControllerId();

    /**
     * Returns the rollout associated with this relationship.
     *
     * @return the rollout
     */
    Rollout getRollout();

    /**
     * Returns the ECU node address associated with this relationship.
     *
     * @return the ECU node address
     */
    String getEcuNodeAddress();
}
