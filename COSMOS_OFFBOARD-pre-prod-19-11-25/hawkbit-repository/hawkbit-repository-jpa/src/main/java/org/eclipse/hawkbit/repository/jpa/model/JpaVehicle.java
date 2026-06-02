package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * JPA implementation of {@link Vehicle}.
 */
@Entity
@Table(name = "sp_vehicle_model", indexes = {@Index(name = "sp_idx_vehicle_name", columnList = "name")})
@Setter
public class JpaVehicle extends AbstractJpaBaseEntity implements Vehicle {

    public JpaVehicle() {
    }

    public JpaVehicle(String name) {
        this.name = name;
    }

    public JpaVehicle(String name, String ercType) {
        this.name = name;
        this.ercType = ercType;
    }

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Vehicle Name is Mandatory")
    @Size(min = Vehicle.VEHICLE_NAME_MIN_SIZE, max = Vehicle.VEHICLE_NAME_MAX_SIZE)
    @Column(name = "name", unique = true)
    private String name;

    @Column(name = "erc_type", length = 10)
    private String ercType;

    @Override
    public String getName() {
        return name;
    }

    public String getErcType() {
        return ercType;
    }

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaEcuModel.class)
    @JoinTable(name = "sp_vehicle_ecu", joinColumns = {
            @JoinColumn(name = "vehicle_model_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_vehicle_model"))}, inverseJoinColumns = {
            @JoinColumn(name = "ecu_model_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ecu_model"))})
    private Set<EcuModel> vehicleEcu;

    public Set<EcuModel> getVehicleEcu() {
        if (vehicleEcu == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(vehicleEcu);
    }

    public void setVehicleEcu(Set<EcuModel> vehicleEcu) {
        this.vehicleEcu = vehicleEcu;
    }

    public boolean addVehicleEcu(final EcuModel ecuModel) {
        if (vehicleEcu == null) {
            vehicleEcu = new HashSet<>();
        }

        return vehicleEcu.add(ecuModel);
    }
}
