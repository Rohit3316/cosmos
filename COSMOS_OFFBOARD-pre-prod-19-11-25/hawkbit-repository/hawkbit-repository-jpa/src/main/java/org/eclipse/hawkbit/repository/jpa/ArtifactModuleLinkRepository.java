package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;
import jakarta.transaction.Transactional;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * This interface represents a repository for managing associations between artifacts and software modules.
 * It extends the Spring Data JPA Repository interface, which provides CRUD operations and additional functionalities.
 */
public interface ArtifactModuleLinkRepository extends JpaRepository<JpaArtifactSoftwareModuleAssociationEntity, Long> {

    /**
     * Finds an association between an artifact and a software module by their IDs.
     *
     * @param artifactId the ID of the artifact
     * @return an {@link Optional} containing the found {@link JpaArtifactSoftwareModuleAssociationEntity}, or {@link Optional#empty()} if not found
     */
    List<ArtifactSoftwareModuleAssociation> findByArtifactId(@Param("artifactId") long artifactId);

    /**
     * Counts the number of associations between artifacts and software modules with the given artifact ID and software module ID.
     *
     * @param artifactId       the ID of the artifact
     * @param softwareModuleId the ID of the software module
     * @return the number of associations
     */
    @Query("SELECT COUNT(a) FROM JpaArtifactSoftwareModuleAssociationEntity a WHERE a.artifact.id = :artifactId AND a.softwareModule.id = :softwareModuleId")
    Long countByArtifactIdAndSoftwareModuleId(long artifactId, long softwareModuleId);

    /**
     * Deletes all associations between artifacts and software modules with the given artifact ID.
     *
     * @param artifact the {@link Artifacts}
     */
    @Modifying
    @Transactional
    void deleteByArtifact(final Artifacts artifact);

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given artifact ID, software module ID, target version ID, source version ID.
     *
     * @param softwareModuleId the ID of software module
     * @param sourceVersionId  the ID of source version
     * @param targetVersionId  the ID of target version
     * @param artifactId       the ID of artifact
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    Optional<ArtifactSoftwareModuleAssociation> findBySoftwareModuleIdAndSourceVersionIdAndTargetVersionIdAndArtifactId(@Param("softwareModuleId") Long softwareModuleId, @Param("sourceVersionId") Long sourceVersionId, @Param("targetVersionId") Long targetVersionId, @Param("artifactId") Long artifactId);

    /**
     * Returns all the associations for artifacts with the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     */
    List<ArtifactSoftwareModuleAssociation> findBySoftwareModuleId(@Param("softwareModuleId") Long softwareModuleId);

    /**
     * Finds a list of {@link ArtifactSoftwareModuleAssociation} entities based on the given artifact ID and software module ID.
     *
     * @param artifactId       the ID of the artifact
     * @param softwareModuleId the ID of the software module
     * @return an {@link Optional} containing a list of {@link ArtifactSoftwareModuleAssociation} entities
     * matching the provided artifact ID and software module ID.
     * If no associations are found, the {@link Optional} will be empty.
     */
    @Query("SELECT sma FROM JpaArtifactSoftwareModuleAssociationEntity sma " +
            "JOIN FETCH sma.softwareModule sm " +
            "JOIN FETCH sm.assignedTo " +
            "WHERE sma.artifact.id = :artifactId AND sma.softwareModule.id = :softwareModuleId")
    Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsByArtifactIdAndSoftwareModuleId(
            @Param("artifactId") long artifactId,
            @Param("softwareModuleId") long softwareModuleId);

    /**
     * Finds a list of {@link ArtifactSoftwareModuleAssociation} entities based on the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     * @return an {@link Optional} containing a list of {@link ArtifactSoftwareModuleAssociation} entities
     * matching the provided software module ID.
     * If no associations are found, the {@link Optional} will be empty.
     */
    @Query("SELECT sma FROM JpaArtifactSoftwareModuleAssociationEntity sma " +
            "JOIN FETCH sma.softwareModule sm " +
            "JOIN FETCH sm.assignedTo " +
            "WHERE sma.softwareModule.id = :softwareModuleId")
    Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsBySoftwareModuleId(
            @Param("softwareModuleId") long softwareModuleId);

    /**
     * Searches for local artifacts for a base software module.
     *
     * @param pageReq          Pageable
     * @param softwareModuleId software module id
     * @return Page<Artifacts>
     */
    Page<ArtifactSoftwareModuleAssociation> findBySoftwareModuleId(Pageable pageReq, Long softwareModuleId);

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given software module ID and target version ID.
     *
     * @param softwareModuleId the ID of the software module
     * @param targetVersionId  the ID of the target version
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    Optional<ArtifactSoftwareModuleAssociation> findFirstBySoftwareModuleIdAndTargetVersionId(@Param("softwareModuleId") Long softwareModuleId, @Param("targetVersionId") Long targetVersionId);

    @Modifying
    @Query("DELETE FROM JpaArtifactSoftwareModuleAssociationEntity a WHERE a.softwareModule = :softwareModule")
    void deleteBySoftwareModule(@Param("softwareModule") JpaSoftwareModule softwareModule);
}
