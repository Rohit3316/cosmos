package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.EcuModelType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;

@Entity
@Table(name = "sp_ecu_model_type", indexes = {@Index(name = "sp_idx_ecu_model_type", columnList = "name")})
@Setter
public class JpaEcuModelType extends AbstractJpaBaseEntity implements EcuModelType {

    private static final long serialVersionUID = 1L;

    @Size(min = SoftwareInstallerType.INSTALLER_TYPE_NAME_MIN_SIZE, max = SoftwareInstallerType.INSTALLER_TYPE_NAME_MAX_SIZE)
    @Column(name = "name", unique = true)
    @NotNull
    private String name;

    @Column(name = "description", length = NamedEntity.DESCRIPTION_MAX_SIZE)
    @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
    private String description;

    @Column(name = "deleted")
    private boolean deleted;

    public JpaEcuModelType() {

    }

    public JpaEcuModelType(String name) {
        this.name = name;
    }

    public JpaEcuModelType(String name, String description, boolean deleted) {
        this.name = name;
        this.description = description;
        this.deleted = deleted;
    }

    @Override
    public String getEcuModelTypeName() {
        return name;
    }
}
