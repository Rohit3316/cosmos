package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.jpa.model.converter.InventoryAttributeConverter;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.cosmos.models.ddi.DeviceInventoryDetails;

/**
 * JPA implementation of {@link TargetInventory}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sp_target_inventory")
public class JpaTargetInventory extends AbstractJpaBaseEntity implements TargetInventory {

    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_inventory_target"))
    private JpaTarget target;

    @Convert(converter = InventoryAttributeConverter.class)
    @Column(name = "inventory")
    private DeviceInventoryDetails inventory;

    @Column(name = "raw_inventory")
    private String rawInventory;
}
