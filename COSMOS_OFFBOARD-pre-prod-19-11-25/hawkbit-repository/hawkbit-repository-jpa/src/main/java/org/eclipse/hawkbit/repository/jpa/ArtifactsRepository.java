package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ArtifactsRepository extends BaseEntityRepository<JpaArtifacts, Long>, JpaSpecificationExecutor<JpaArtifacts> {

    /**
     * Find the list of {@link JpaArtifacts} for given fileName
     *
     * @param fileName {@link Artifacts#getFileName()} ()}
     * @return JpaArtifacts
     */
    List<JpaArtifacts> findByFileName(String fileName);

    /**
     * Finds the list of {@link JpaArtifacts} for the given SHA-256 hash.
     *
     * @param sha256 the SHA-256 hash to search for
     * @return a list of {@link JpaArtifacts} that match the given SHA-256 hash
     */
    Optional<Artifacts> findBySha256HashIgnoreCase(String sha256);

    /**
     * Retrieves an Artifacts by Id.
     *
     * @param id the ID of the artifacts
     * @return the found {@link Artifacts}
     */
    Optional<Artifacts> getArtifactsById(Long id);
}