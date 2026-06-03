package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Set;

/**
 * JPA implementation of a {@link Role}.
 *
 */
@Entity
@Table(name = "sp_role", uniqueConstraints = @UniqueConstraint(columnNames = { "name"}, name = "uk_role"))
public class JpaRole extends AbstractJpaBaseEntity implements Role {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = Role.NAME_MAX_SIZE, nullable = false)
    @Size(min = 1, max = Role.NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Column(name = "description", length = Role.DESCRIPTION_MAX_SIZE, nullable = false)
    @Size(min = 1, max = Role.DESCRIPTION_MAX_SIZE)
    @NotNull
    private String description;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaPermission.class, fetch = FetchType.LAZY)
    @JoinTable(name = "sp_role_permission",
            joinColumns = {
                    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_role_permission_role"))
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "permission_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_role_permission_permission"))}
    )
    private Set<Permission> permissions;

    @Column(name = "active")
    @ObjectTypeConverter(name = "roleActive", objectType = Boolean.class, dataType = String.class, conversionValues = {
            @ConversionValue(objectValue = "true", dataValue = "TRUE"),
            @ConversionValue(objectValue = "false", dataValue = "FALSE") })
    @Convert("roleActive")
    private boolean active = true;

    public JpaRole(){}

    public JpaRole(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public JpaRole(String name, String description, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions;
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

    @Override
    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JpaRole jpaRole = (JpaRole) o;
        return Objects.equals(name, jpaRole.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, permissions, active);
    }
}
