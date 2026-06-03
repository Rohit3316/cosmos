package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaPermission;
import org.eclipse.hawkbit.repository.model.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * The repository interface for the {@link Permission} model.
 */
@Repository
public interface PermissionRepository extends JpaRepository<JpaPermission, Long> {

    /**
     * Find a permission by its name
     * @param name String
     * @return permission
     */
    Optional<Permission> findByName(final String name);

    /**
     * Find all {@link Permission} by names.
     *
     * @param permissions name
     *            to search for
     * @return found {@link Permission} or <code>null</code>
     */
    @Query("SELECT p FROM JpaPermission p WHERE p.name IN ?1")
    List<Permission> findAllByNames(Iterable<String> permissions);
}
