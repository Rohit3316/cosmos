package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.ddi.DeviceInventoryDetails;

/**
 * {@link Target}'s inventory details
 */
public interface TargetInventory extends BaseEntity {

    /**
     * {@link Target} of inventory
     *
     * @return {@link Target}
     */
    Target getTarget();

    /**
     * Inventory Details
     *
     * @return DeviceInventoryDetails
     */
    DeviceInventoryDetails getInventory();

    /**
     * Raw Inventory Details
     *
     * @return String
     */
    String getRawInventory();
}
