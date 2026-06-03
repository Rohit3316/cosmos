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
import jakarta.persistence.EntityManager;

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


/**
 * The repository interface for the {@link Rollout} model.
 */
@Transactional(readOnly = true)
public interface RolloutRepository
        extends BaseEntityRepository<JpaRollout, Long>, JpaSpecificationExecutor<JpaRollout> {

    /**
     * Retrieves all {@link Rollout} for given status.
     *
     * @param status
     *            the status of the rollouts to find
     * @return the list of {@link Rollout} for specific status
     */
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT sm.id FROM JpaRollout sm WHERE sm.status IN ?1")
    List<Long> findByStatusIn(Collection<RolloutStatus> status);

    /**
     * Retrieves an Rollout with all lazy attributes.
     *
     * @param rolloutId
     *            the ID of the rollout
     * @return the found {@link Rollout}
     */
    Optional<JpaRollout> getRolloutById(Long rolloutId);

    /**
     * Retrieves all {@link Rollout} for a specific {@code name}
     *
     * @param name
     *            the rollout name
     * @return {@link Rollout} for specific name
     */
    Optional<Rollout> findByName(String name);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaRollout t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Retrieves all {@link Rollout}s for a specific {@link DistributionSet} in
     * a given {@link RolloutStatus}.
     *
     * @param set
     *            the distribution set
     * @param status
     *            the status of the rollout
     * @return {@link Rollout} for specific distribution set
     */
    List<Rollout> findByDistributionSetAndStatusIn(DistributionSet set, Collection<RolloutStatus> status);

    /**
     * Retrieves all {@link Rollout}s for a specific list of {@link DistributionSet} in
     * a given {@link RolloutStatus}.
     *
     * @param sets   the lis of distribution set
     * @param status the status of the rollout
     * @return {@link Rollout} for specific distribution set
     */
    List<Rollout> findByDistributionSetInAndStatusIn(List<DistributionSet> sets, Collection<RolloutStatus> status);

    /**
     * Counts all {@link Rollout}s for a specific {@link DistributionSet} in a
     * given {@link RolloutStatus}.
     *
     * @param distributionSetId
     *            the distribution set
     * @param status
     *            the status of the rollout
     * @return the count
     */
    long countByDistributionSetIdAndStatusIn(long distributionSetId, Collection<RolloutStatus> status);

    @Query("SELECT r from JpaRollout r join r.rolloutGroups rg " +
            "join rg.rolloutTargetGroup rtg where rtg.target=:targetId AND " +
            "r.status<>:status")
    List<JpaRollout> findNotFinishedStatusRolloutByTargetId(@Param("targetId") JpaTarget targetId, @Param("status") RolloutStatus status);


    @Query("SELECT r from JpaRollout r join r.rolloutGroups rg " +
            "join rg.rolloutTargetGroup rtg where rtg.target IN :targetIds")
    List<Rollout> findAllRolloutByTargetIds(@Param("targetIds") List<JpaTarget> targetIds);

    /**
     *
     * @param targetId
     * @return the list of rollouts
     */
    @Query("SELECT r from JpaRollout r join r.rolloutGroups rg " +
            "join rg.rolloutTargetGroup rtg where rtg.target=:targetId")
    List<JpaRollout> findAllRolloutByTargetId(@Param("targetId") JpaTarget targetId);



    /**
     * Retrieves a list of all {@link Rollout} entities that are associated with the specified distribution set ID.
     *
     * This query selects all rollouts linked to the given distribution set, identified by the provided
     * {@code distributionSetId}. The results will include all rollouts that belong to the distribution set.
     *
     * @param distributionSetId the ID of the distribution set whose rollouts are to be fetched
     * @return a list of {@link Rollout} entities associated with the specified distribution set
     */
    @Query("SELECT r FROM JpaRollout r WHERE r.distributionSet.id = :distributionSetId")
    List<Rollout> findAllByDistributionSetId(@Param("distributionSetId") long distributionSetId);

    /**
     * Finds rollouts by distribution set ID.
     *
     * @param distributionSetId the ID of the distribution set
     * @return a list of rollouts associated with the given distribution set ID
     */
    List<Rollout> findByDistributionSetId(Long distributionSetId);

    /**
     * Updates the status of rollouts based on their current status and expiration time.
     * <p>
     * This method modifies the status of all {@link JpaRollout} entities whose current status
     * is in the provided list of old statuses and whose `endAt` timestamp is earlier than
     * the specified current time in seconds.
     * </p>
     *
     * @param newStatus    The new status to set for the rollouts.
     * @param oldStatuses  A list of statuses to filter the rollouts that need to be updated.
     * @param nowInSeconds The current time in seconds, used to determine expired rollouts.
     */
    @Modifying
    @Query("UPDATE JpaRollout r SET r.status = :newStatus WHERE r.status in :oldStatuses AND r.endAt < :nowInSeconds")
    void updateStatusByStatusAndRollout(@Param("newStatus") RolloutStatus newStatus,
                                        @Param("oldStatuses") List<RolloutStatus> oldStatuses,
                                        @Param("nowInSeconds") Long nowInSeconds);
}
