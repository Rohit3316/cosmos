package org.eclipse.hawkbit.repository.jpa.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.persistence.annotations.CascadeOnDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JPA implementation of {@link EcuModel}.
 */
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sp_ecu_model", uniqueConstraints = @UniqueConstraint(columnNames = {"ecu_model_name", "ecu_node_id"}, name = "uk_ecu_node_id_name"))
@Data
public class JpaEcuModel extends AbstractJpaBaseEntity implements EcuModel {

    public JpaEcuModel() {
    }

    public JpaEcuModel(JpaEcuModelType ecuModelType, String ecuModelName, String ecuNodeId) {
        this.ecuModelType = ecuModelType;
        this.ecuModelName = ecuModelName;
        this.ecuNodeId = ecuNodeId;
    }

    @Serial
    private static final long serialVersionUID = 1L;

    @NotNull(message = "Ecu Model Name is Mandatory")
    @Size(min = EcuModel.ECU_NAME_MIN_SIZE, max = EcuModel.ECU_NAME_MAX_SIZE)
    @Column(name = "ecu_model_name")
    private String ecuModelName;

    @NotNull(message = "Ecu Model Type is Mandatory")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ecu_model_type", nullable = false, foreignKey = @ForeignKey(name = "fk_ecu_model_type"))
    private JpaEcuModelType ecuModelType;

    @NotNull(message = "Ecu Model Id is Mandatory")
    @Column(name = "ecu_node_id")
    private String ecuNodeId;

    @CascadeOnDelete
    @ManyToMany(mappedBy = "softwareEcuModels", targetEntity = JpaSoftwareModule.class, fetch = FetchType.LAZY)
    private Set<SoftwareModule> softwareModules;

    @CascadeOnDelete
    @ManyToMany(mappedBy = "vehicleEcu", targetEntity = JpaVehicle.class, fetch = FetchType.LAZY)
    private List<Vehicle> vehicleModel;
    
    public List<Vehicle> getVehicleModel() {
        return vehicleModel;
    }

    public void setVehicleModel(List<Vehicle> vehicleModel) {
        this.vehicleModel = vehicleModel;
    }

    public String getEcuModelType() {
        return ecuModelType.getEcuModelTypeName();
    }

    @Override
    public Set<SoftwareModule> getSoftwareModules() {
        if (softwareModules == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(softwareModules);
    }
}
