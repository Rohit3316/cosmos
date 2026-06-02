package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.hawkbit.repository.model.dto.RoleDTO;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for {@link Role} management operations.
 *
 */
public interface RoleManagement {

    /**
     * Get list of roles.
     * @return role list
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLE_READ)
    List<Role> getRoles();

    /**
     * Get a role for given role_id
     * @param id long
     * @return role
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLE_READ)
    Role getRole(final Long id);

    /**
     * Get a role for given role name
     * @param name String
     * @return role
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLE_READ)
    Role getRoleByName(final String name);

    /**
     * Create a role
     * @param roles list of role
     * @return role created
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLE_CREATE_UPDATE)
    List<Role> create(final @NotEmpty List<RoleDTO> roles);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_ROLE_DELETE)
    void delete(final @NotEmpty Collection<Long> ids);
}
