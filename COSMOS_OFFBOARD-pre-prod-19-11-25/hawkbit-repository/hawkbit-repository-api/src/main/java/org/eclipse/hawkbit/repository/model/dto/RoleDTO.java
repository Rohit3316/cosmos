package org.eclipse.hawkbit.repository.model.dto;

import java.util.Set;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.hawkbit.repository.model.Role;

public class RoleDTO {

    @Size(min = 1, max = Role.NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Size(min = 1, max = Role.DESCRIPTION_MAX_SIZE)
    @NotNull
    private String description;

    private Set<Permission> permissions;

    public RoleDTO(){}

    public RoleDTO(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public RoleDTO(String name, String description, Set<Permission> permissions) {
        this.name = name;
        this.description = description;
        this.permissions = permissions;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<Permission> permissions) {
        this.permissions = permissions;
    }
}
