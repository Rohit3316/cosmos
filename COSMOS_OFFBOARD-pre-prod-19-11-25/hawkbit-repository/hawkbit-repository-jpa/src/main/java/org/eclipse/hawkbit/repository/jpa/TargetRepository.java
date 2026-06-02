/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;
import jakarta.persistence.EntityManager;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Target} repository.
 */
@Transactional(readOnly = true)
public interface TargetRepository extends BaseEntityRepository<JpaTarget, Long>, JpaSpecificationExecutor<JpaTarget> {

    /**
     * @param controllerId the controllerId or VIN of target
     * @param name         the name of target
     * @param serialNumber the serial number of target
     * @return JpaTarget
     */
    List<Target> findByControllerIdOrNameOrSerialNumber(String controllerId, String name, String serialNumber);


    /**
     * Sets {@link JpaTarget#getAssignedDistributionSet()}.
     *
     * @param set        to use
     * @param status     to set
     * @param modifiedAt current time
     * @param modifiedBy current auditor
     * @param targets    to update
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
                                                   @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
                                                   @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    /**
     * Sets {@link JpaTarget#getAssignedDistributionSet()},
     * {@link JpaTarget#getInstalledDistributionSet()} and
     * {@link JpaTarget#getInstallationDate()}
     *
     * @param set        to use
     * @param status     to set
     * @param modifiedAt current time
     * @param modifiedBy current auditor
     * @param targets    to update
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.assignedDistributionSet = :set, t.installedDistributionSet = :set, t.installationDate = :lastModifiedAt, t.lastModifiedAt = :lastModifiedAt, t.lastModifiedBy = :lastModifiedBy, t.updateStatus = :status WHERE t.id IN :targets")
    void setAssignedAndInstalledDistributionSetAndUpdateStatus(@Param("status") TargetUpdateStatus status,
                                                               @Param("set") JpaDistributionSet set, @Param("lastModifiedAt") Long modifiedAt,
                                                               @Param("lastModifiedBy") String modifiedBy, @Param("targets") Collection<Long> targets);

    /**
     * Deletes the {@link Target}s with the given target IDs.
     *
     * @param targetIDs to be deleted
     */
    @Modifying
    @Transactional
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("DELETE FROM JpaTarget t WHERE t.id IN ?1")
    void deleteByIdIn(Collection<Long> targetIDs);

    /**
     * Finds all {@link Target}s in the repository.
     * <p>
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default List<JpaTarget> findAll() {
        return this.findAll((Specification<JpaTarget>) null);
    }

    /**
     * Finds all {@link Target}s in the repository sorted.
     * <p>
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param sort instructions to sort result by
     * @return {@link List} of {@link Target}s
     */
    @NonNull
    default List<JpaTarget> findAll(@NonNull Sort sort) {
        return this.findAll((Specification<JpaTarget>) null, sort);
    }

    /**
     * Finds a page of {@link Target}s in the repository.
     * <p>
     * Calls version with (empty) spec to allow injecting further specs
     *
     * @param pageable paging context
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default Page<JpaTarget> findAll(@NonNull Pageable pageable) {
        return this.findAll((Specification<JpaTarget>) null, pageable);
    }

    /**
     * Finds {@link Target}s in the repository matching a list of ids.
     * <p>
     * Calls version based on spec to allow injecting further specs
     *
     * @param ids ids to filter for
     * @return {@link List} of {@link Target}s
     */
    @Override
    @NonNull
    default List<JpaTarget> findAllById(Iterable<Long> ids) {
        final List<Long> collectedIds = StreamSupport.stream(ids.spliterator(), true).toList();
        return this.findAll(TargetSpecifications.hasIdIn(collectedIds));
    }

    /**
     * Finds {@link Target} in the repository matching an id.
     * <p>
     * Calls version based on spec to allow injecting further specs
     *
     * @param id id to filter for
     * @return {@link Optional} of {@link Target}
     */
    @Override
    @NonNull
    default Optional<JpaTarget> findById(@NonNull Long id) {
        return this.findOne(TargetSpecifications.hasId(id));
    }

    /**
     * Checks whether {@link Target} in the repository matching an id exists or not.
     * <p>
     * Calls version based on spec to allow injecting further specs
     *
     * @param id id to check for
     * @return true if target with id exists
     */
    @Override
    default boolean existsById(@NonNull Long id) {
        return this.exists(TargetSpecifications.hasId(id));
    }

    /**
     * Checks whether {@link Target} in the repository matching a spec exists or not.
     *
     * @param spec to check for existence
     * @return true if target with id exists
     */
    default boolean exists(@NonNull Specification<JpaTarget> spec) {
        return this.count(spec) > 0;
    }

    /**
     * Count number of {@link Target}s in the repository.
     * <p>
     * Calls version with an empty spec to allow injecting further specs
     *
     * @return number of targets in the repository
     */
    @Override
    default long count() {
        return this.count((Specification<JpaTarget>) null);
    }

    /**
     * Counts {@link Target} instances of given type in the repository.
     *
     * @param targetTypeId to search for
     * @return number of found {@link Target}s
     */
    long countByTargetTypeId(Long targetTypeId);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager} anyhow.
     * The DB should take care of optimizing this away.
     *
     * @param tenant to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTarget t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Get value of an attribute based on the provided {@link Target#getId()} and attribute key
     *
     * @param targetId of target id
     * @param key      of attribute key
     * @return String
     * of attribute value
     */
    @Query("SELECT ca FROM JpaTarget ta " +
            " join ta.controllerAttributes ca WHERE ta.id = :targetId and KEY(ca) = :key")
    String getTargetAttributesByTargetIdAndKey(@Param("targetId") Long targetId, @Param("key") String key);


    /**
     * Finds {@link Target}s in the repository for given vehicle model id.
     *
     * @param vehicleModelId
     * @return {@link List} of {@link Target}s
     */
    List<JpaTarget> findByVehicleModelId(Long vehicleModelId);

    /**
     * Finds {@link JpaTarget}s in the repository for given list of controller ids.
     *
     * @param vins List of controller ids (VINs) to filter for
     * @return {@link List} of {@link JpaTarget}s that match the given controller ids/VINs
     *
     */
    List<JpaTarget> findByControllerIdIn(List<String> vins);

    /**
     * Sets the lastTargetQuery for given controller ids in the tenant
     *
     * @param lastTargetQuery to set
     * @param controllerIds   list of ids to set
     * @param tenant          to set the data
     * @return number of ids updated
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.lastTargetQuery = :lastTargetQuery " +
            "WHERE t.controllerId IN :controllerIds AND t.tenant = :tenant")
    int updateLastTargetQuery(@Param("lastTargetQuery") Long lastTargetQuery,
                              @Param("controllerIds") List<String> controllerIds,
                              @Param("tenant") String tenant);

    /**
     * Finds list of {@link Target}s in the repository for given controller ids.
     *
     * @param controllerIds - list of controller ids
     * @return {@link List} of {@link Target}s
     */
    List<Target> findAllByControllerIdIn(List<String> controllerIds);

    /**
     * Finds the tenant associated with the given controllerId.
     *
     * @param controllerId the controller ID
     * @return the tenant name
     */
    @Query(value = "SELECT tenant FROM sp_target WHERE controller_id = ?1", nativeQuery = true)
    Optional<String> findTenantByControllerId(String controllerId);


    /**
     * @param newTenant the new tenant to be assigned to the target
     * @param controllerId the unique identifier of the target (controller ID)
     * @param currentTenant the current tenant of the target, used to confirm the update condition
     * @return the number of rows affected by the update operation (1 if successful, 0 if no match found)
     */

    @Modifying
    @Transactional
    @Query("UPDATE JpaTarget t SET t.tenant = :newTenant " +
            "WHERE t.controllerId = :controllerId AND t.tenant = :currentTenant")
    int updateTenant(@Param("newTenant") String newTenant,
                     @Param("controllerId") String controllerId,
                     @Param("currentTenant") String currentTenant);

    /**
     *
     * @param controllerId
     * @return An {@link Optional} containing the matching {@link JpaTarget} entity, or an empty {@link Optional}
     *         if no target with the specified {@code controllerId} exists.
     */

    @Query(value = "SELECT * FROM sp_target t WHERE t.controller_id = ?1", nativeQuery = true)
    Optional<JpaTarget> findByControllerId(@Param("controllerId") String controllerId);

    /**
     * Retrieves a list of target IDs based on the provided controller IDs.
     * <p>
     * This method performs a database query to find target IDs whose controller ID matches any of the given controller IDs.
     * </p>
     *
     * @param controllerIds A list of controller IDs to filter the targets by.
     * @return A list of target IDs associated with the provided controller IDs.
     */
    @Query("SELECT t.id FROM JpaTarget t WHERE t.controllerId IN :controllerIds")
    List<Long> findTargetIdByControllerIds(@Param("controllerIds") List<String> controllerIds);

    /**
     * Finds all {@link JpaTarget}s associated with a specific rollout ID.
     *
     * @param rolloutId the ID of the rollout to filter targets by
     * @return a page of {@link JpaTarget}s associated with the specified rollout ID
     */
    @Query("SELECT DISTINCT t FROM JpaTarget t JOIN RolloutTargetGroup rtg ON rtg.target = t WHERE rtg.rolloutGroup.rollout.id = :rolloutId")
    List<JpaTarget> findTargetsByRolloutId(@Param("rolloutId") Long rolloutId);
}
