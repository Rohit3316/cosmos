package org.eclipse.hawkbit.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
public enum VehicleFields implements FieldNameProvider {
    /**
     * The unique identifier for the vehicle model.
     */
    ID("id"),
    /**
     * The name of the vehicle model.
     */
    NAME("name"),
    /**
     * The unique identifier for the vehicle ECU.
     */
    VEHICLE_ECU_ID("vehicleEcu.id"),
    /**
     * The type of the vehicle ECU model.
     */
    VEHICLE_ECU_TYPE("vehicleEcu.ecuModelType"),
    /**
     * The name of the vehicle ECU model.
     */
    VEHICLE_ECU_NAME("vehicleEcu.ecuModelName"),
    /**
     * The node ID of the vehicle ECU.
     */
    VEHICLE_ECU_NODE_ID("vehicleEcu.ecuNodeId");

    private final String fieldName;
}
