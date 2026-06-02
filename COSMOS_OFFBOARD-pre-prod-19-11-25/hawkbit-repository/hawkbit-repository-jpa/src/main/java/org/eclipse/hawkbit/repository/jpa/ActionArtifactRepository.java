package org.eclipse.hawkbit.repository.jpa;


import org.eclipse.hawkbit.repository.jpa.model.ActionArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionArtifact;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for managing `JpaActionArtifact` entities.
 * <p>
 * This interface extends the Spring Data JPA `JpaRepository` to provide
 * CRUD operations and additional query methods for `JpaActionArtifact` entities.
 * <p>
 * Key Features:
 * - Automatically provides basic CRUD operations (save, find, delete, etc.).
 * - Can be extended with custom query methods if needed.
 * - Annotated with `@Repository` to indicate that it is a Spring-managed component.
 *
 * @see JpaRepository
 * @see JpaActionArtifact
 */
@Repository
public interface ActionArtifactRepository extends JpaRepository<JpaActionArtifact, ActionArtifact> {

    /**
     * Finds all actions associated with a given artifact ID.
     *
     * @param artifactId the ID of the artifact to find actions for
     * @return a list of {@link JpaAction} entities associated with the specified artifact ID
     */
    List<JpaAction> findActionsByArtifactId(@Param("artifactId") Long artifactId);

    /**
     * Finds all action artifacts associated with a given artifact.
     *
     * @param artifactId the artifact id to find action artifacts for
     * @param pageable pagination information
     * @return a page of {@link JpaAction} entities associated with the specified artifact
     */
    Page<JpaActionArtifact> findActionsByArtifactId(Long artifactId, Pageable pageable);

}
