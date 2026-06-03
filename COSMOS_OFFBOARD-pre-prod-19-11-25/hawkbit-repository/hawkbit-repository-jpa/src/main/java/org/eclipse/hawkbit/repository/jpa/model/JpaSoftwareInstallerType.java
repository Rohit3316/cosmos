package org.eclipse.hawkbit.repository.jpa.model;


import java.io.Serial;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Setter;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;

/**
 * JPA entity for the software installer type.
 *
 */
@Entity
@Table(name = "sp_software_installer_type", indexes = {@Index(name = "sp_idx_software_installer_type", columnList = "name")})
@Setter
public class JpaSoftwareInstallerType extends AbstractJpaBaseEntity implements SoftwareInstallerType {

    @Serial
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

    public JpaSoftwareInstallerType() {
        // Default constructor needed for JPA entities
    }

    public JpaSoftwareInstallerType(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String getNameAndDescription() {
        return name + " : " + description;
    }
}
