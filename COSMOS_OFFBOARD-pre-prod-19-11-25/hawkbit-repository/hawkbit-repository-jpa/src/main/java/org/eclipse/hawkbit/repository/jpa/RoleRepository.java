package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * The repository interface for the {@link Role} model.
 */
@Repository
@Transactional(readOnly = true)
public interface RoleRepository extends JpaRepository<JpaRole, Long> {

    /**
     * Find a role by its name
     * @param name String
     * @return role
     */
    Optional<Role> findByName(final String name);
}
