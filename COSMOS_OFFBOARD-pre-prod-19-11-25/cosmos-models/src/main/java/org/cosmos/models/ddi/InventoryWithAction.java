package org.cosmos.models.ddi;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor(force = true)
/**
 * Represents an inventory with an associated action.
 * This class contains details about the device inventory and the ID of the action
 * associated with it.
 */

public class InventoryWithAction {

    private DeviceInventoryDetails inventory;

    private Long actionId;

    public InventoryWithAction(final DeviceInventoryDetails inventory, Long actionId) {
        this.inventory = inventory;
        this.actionId = actionId;
    }


}
