package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Set;
import org.cosmos.models.mgmt.supportpackage.dto.EspFileTypeForDevices;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * This interface represents a repository for managing {@link JpaEspEcuRollout} entities in a JPA-based data store.
 * It extends the Spring Data JPA {@link JpaRepository} interface, providing basic CRUD operations and additional
 * query methods for {@link JpaEspEcuRollout} entities.
 */
@Repository
public interface EspEcuRolloutRepository extends JpaRepository<JpaEspEcuRollout, Long> {

    //Jpa query to pull list of EspEcuRollout by rolloutId and list of ecuNodeAddress
    List<JpaEspEcuRollout> findByRolloutIdAndEcuNodeAddressIn(Long rolloutId, Set<String> ecuNodeAddress);

    /**
     * Deletes all {@link JpaEspEcuRollout} entities associated with the given rollout ID and support package IDs.
     *
     * @param rolloutId         The ID of the rollout.
     * @param supportPackageIds The IDs of the support packages.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaEspEcuRollout e WHERE e.rollout.id = :rolloutId AND e.supportPackage.id IN :supportPackageIds")
    void deleteByRolloutIdAndSupportPackageIdIn(@Param("rolloutId") Long rolloutId, @Param("supportPackageIds") List<Long> supportPackageIds);

    /**
     * Retrieves the number of {@link JpaEspEcuRollout} entities associated with the given support package ID.
     *
     * @param supportPackageId The ID of the support package.
     * @return The number of {@link JpaEspEcuRollout} entities associated with the given support package ID.
     */
    Long countBySupportPackageId(Long supportPackageId);

    List<EspEcuRollout> findByRolloutIdAndSupportPackageIdAndEcuNodeAddressAndControllerIdIn(Long rolloutId, Long supportPackageId, String ecuNodeAddress, List<String> vins);

    /*
     * clean up the entire table by tenant
     * */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaEspEcuRollout t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Finds all {@link EspEcuRollout} entities by rollout ID and a list of controller IDs.
     *
     * @param rolloutId     The ID of the rollout.
     * @param controllerIds The list of controller IDs.
     * @return A list of matching {@link EspEcuRollout} entities.
     */
    @Query("SELECT e FROM JpaEspEcuRollout e JOIN FETCH e.rollout JOIN FETCH e.supportPackage WHERE e.rollout.id = :rolloutId AND e.controllerId IN :controllerIds")
    List<JpaEspEcuRollout> findWithRolloutAndSupportPackageByRolloutIdAndControllerIdIn(@Param("rolloutId") Long rolloutId, @Param("controllerIds") List<String> controllerIds);

    /**
     * Finds all controller IDs associated with a specific rollout ID and  ECU node addresses.
     *
     * @param rolloutId      The ID of the rollout.
     * @param ecuNodeAddress ECU node addresses.
     * @return A list of controller IDs associated with the specified rollout ID and ECU node addresses.
     */
    @Query("SELECT new org.cosmos.models.mgmt.supportpackage.dto.EspFileTypeForDevices (er.controllerId, esp.fileType)" +
            "FROM JpaEspEcuRollout er " +
            "JOIN er.supportPackage esp " +
            "WHERE er.rollout.id = :rolloutId " +
            "AND er.ecuNodeAddress = :ecuNodeAddress")
    List<EspFileTypeForDevices> findControllerIdAndFileTypeByRolloutIdAndEcuNodeAddress(
            @Param("rolloutId") Long rolloutId,
            @Param("ecuNodeAddress") String ecuNodeAddress);
}