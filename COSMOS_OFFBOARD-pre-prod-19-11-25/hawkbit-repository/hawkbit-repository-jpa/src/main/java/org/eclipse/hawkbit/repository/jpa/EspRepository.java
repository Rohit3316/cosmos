package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;

import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Esp entities using JPA.
 */
@Repository
public interface EspRepository extends JpaRepository<JpaEsp, Long>, JpaSpecificationExecutor<JpaEsp> {

    /**
     * Finds an Esp entity by its SHA-256 hash.
     *
     * @param sha256 The SHA-256 hash of the Esp entity to find.
     * @return An Optional containing the found Esp entity, or an empty Optional if no entity was found.
     */
    Optional<Esp> findBySha256HashIgnoreCase(String sha256);

    /**
     * Finds distinct Esp entities based on the provided criteria.
     *
     * @param fileType       The file type of the Esp entities to find.
     * @param ecuNodeAddress The ECU node address of the Esp entities to find.
     * @param rollout        The rollout of the Esp entities to find.
     * @param controllerIds           A list of VINs to filter the Esp entities by.
     * @param sha256         The SHA-256 hash to exclude from the search.
     * @return An Optional containing a list of distinct Esp entities that match the provided criteria, or an empty Optional if no entities were found.
     */
    Optional<List<Esp>> findDistinctByFileTypeAndEspEcuRolloutsEcuNodeAddressAndEspEcuRolloutsRolloutAndEspEcuRolloutsControllerIdInAndSha256HashNotLike(MgmtSupportPackageFileType fileType, String ecuNodeAddress, Rollout rollout, List<String> controllerIds, String sha256);


    /**
     * Finds all Esp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @return A list of Esp entities associated with the given rollout ID.
     */
    @Query("SELECT sr " +
            "FROM JpaEsp sr " +
            "JOIN sr.espEcuRollouts se " +
            "WHERE se.rollout.id = :rolloutId")
    List<Esp> findESPSupportPackagesByRolloutId(@Param("rolloutId") Long rolloutId);


    /**
     * Retrieves a paginated list of Esp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @param pageable  The pagination information, including page number and size.
     * @return A Page containing Esp entities associated with the given rollout ID.
     */
    @Query("SELECT sr " +
            "FROM JpaEsp sr " +
            "JOIN sr.espEcuRollouts se " +
            "WHERE se.rollout.id = :rolloutId")
    Page<Esp> findESPSupportPackagesByRolloutId(@Param("rolloutId") Long rolloutId, Pageable pageable);


      /**
     * Finds all Esp entities associated with a given rollout ID and a list of controller IDs.
     *
     * @param rolloutId The ID of the rollout to find Esp entities for.
     * @param controllerIds A list of controller IDs to filter the Esp entities by.
     * @return A list of Esp entities associated with the given rollout ID and controller IDs.
     */
    @Query("SELECT esp " +
            "FROM JpaEsp esp " +
            "JOIN esp.espEcuRollouts se " +
            "WHERE se.rollout.id=:rolloutId " +
            "and se.controllerId in :controllerIds ")
    List<Esp> findEspSupportPackagesByRolloutIdAndControllerIdsIn(@Param("rolloutId") Long rolloutId, @Param("controllerIds") List<String> controllerIds);

    /**
     * Finds all Esp entities associated with a given rollout ID and support package ID.
     *
     * @param rolloutId        The ID of the rollout to find Esp entities for.
     * @param supportPackageId The ID of the support package to find Esp entities for.
     * @return A list of Esp entities associated with the given rollout ID and support package ID.
     */
    @Query("SELECT sr " +
            "FROM JpaEsp sr " +
            "JOIN sr.espEcuRollouts se " +
            "WHERE se.rollout.id = :rolloutId AND sr.id = :supportPackageId ")
    List<Esp> findESPSupportPackagesByPackageId(@Param("rolloutId") Long rolloutId,
                                                @Param("supportPackageId") Long supportPackageId);


    @Query("SELECT e.supportPackage FROM JpaEspEcuRollout e WHERE e.rollout.id = :rolloutId AND e.controllerId = :controllerId")
    List<Esp> findEspByRolloutIdAndControllerId(@Param("rolloutId") Long rolloutId, @Param("controllerId") String controllerId);

    /**
     * Counts the number of Esp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to count Esp entities for.
     * @return The count of Esp entities associated with the given rollout ID.
     */
    long countByEspEcuRolloutsRolloutId(Long rolloutId);

}
