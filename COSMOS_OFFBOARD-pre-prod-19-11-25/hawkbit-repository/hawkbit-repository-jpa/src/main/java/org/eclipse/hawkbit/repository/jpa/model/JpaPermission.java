package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * JPA implementation of a {@link Permission}.
 *
 */
@Entity
@Table(name = "sp_permission", uniqueConstraints = @UniqueConstraint(columnNames = { "name"}, name = "uk_permission"))
public class JpaPermission extends AbstractJpaBaseEntity implements Permission {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = Permission.NAME_MAX_SIZE, nullable = false)
    @Size(min = 1, max = Permission.NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Column(name = "description", length = Permission.DESCRIPTION_MAX_SIZE, nullable = false)
    @Size(min = 1, max = Permission.DESCRIPTION_MAX_SIZE)
    @NotNull
    private String description;

    @Column(name = "active")
    @ObjectTypeConverter(name = "permissionActive", objectType = Boolean.class, dataType = String.class, conversionValues = {
            @ConversionValue(objectValue = "true", dataValue = "TRUE"),
            @ConversionValue(objectValue = "false", dataValue = "FALSE") })
    @Convert("permissionActive")
    private boolean active = true;

    public JpaPermission(){}

    public JpaPermission(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public Boolean isActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
