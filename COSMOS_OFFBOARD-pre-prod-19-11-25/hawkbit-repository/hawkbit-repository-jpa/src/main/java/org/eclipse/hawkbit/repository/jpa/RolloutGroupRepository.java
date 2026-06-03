/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;


import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * The repository interface for the {@link RolloutGroup} model.
 */
@Transactional(readOnly = true)
public interface RolloutGroupRepository
        extends BaseEntityRepository<JpaRolloutGroup, Long>, JpaSpecificationExecutor<JpaRolloutGroup> {

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in the
     * order of creating them. ID ASC.
     *
     * @param rollout
     *            the rollout the rolloutgroups belong to
     * @return the rollout groups belonging to a rollout ordered by ID ASC.
     */
    List<JpaRolloutGroup> findByRolloutOrderByIdAsc(JpaRollout rollout);

    /**
     * Retrieves all {@link RolloutGroup} referring a specific rollout in a
     * specific {@link RolloutGroupStatus}.
     *
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param status
     *            the status of the rollout groups
     * @return the rollout groups belonging to a rollout in a specific status
     */
    List<JpaRolloutGroup> findByRolloutAndStatus(Rollout rollout, RolloutGroupStatus status);

    /**
     * Counts all {@link RolloutGroup} referring a specific rollout in specific
     * {@link RolloutGroupStatus}s. An in-clause statement does not work with
     * the spring-data, so this is specific usecase regarding to the
     * rollout-management to find out rolloutgroups which are in specific
     * states.
     *
     * @param rolloutId
     *            the ID of the rollout the rolloutgroup belong to
     * @param rolloutGroupStatus1
     *            the status of the rollout groups
     * @param rolloutGroupStatus2
     *            the status of the rollout groups
     * @return the count of rollout groups belonging to a rollout in specific
     *         status
     */
    @Query("SELECT COUNT(r.id) FROM JpaRolloutGroup r WHERE r.rollout.id = :rolloutId and (r.status = :status1 or r.status = :status2)")
    Long countByRolloutIdAndStatusOrStatus(@Param("rolloutId") long rolloutId,
            @Param("status1") RolloutGroupStatus rolloutGroupStatus1,
            @Param("status2") RolloutGroupStatus rolloutGroupStatus2);

    /**
     *
     * Counts all rollout-groups refering to a given {@link Rollout} by its ID
     * and groups which not having the given status.
     *
     * @param rolloutId
     *            the ID of the rollout refering the groups
     * @param status1
     *            the status which the groups should not have
     * @param status2
     *            the status which the groups should not have
     * @param status2
     *            the status which the groups should not have
     * @return count of rollout-groups referning a rollout and not in the given
     *         states
     */
    long countByRolloutIdAndStatusNotAndStatusNotAndStatusNot(@Param("rolloutId") long rolloutId,
                                                              @Param("status1") RolloutGroupStatus status1,
                                                              @Param("status2") RolloutGroupStatus status2,
                                                              @Param("status3") RolloutGroupStatus status3);

    /**
     * Retrieves all {@link RolloutGroup} for a specific parent in a specific
     * status. Retrieves the child rolloutgroup for a specific status.
     *
     * @param rolloutGroupId
     *            the rolloutgroupId to find the parents
     * @param status
     *            the status of the rolloutgroups
     * @return The child {@link RolloutGroup}s in a specific status
     */
    @Query("SELECT g FROM JpaRolloutGroup g WHERE g.parent.id=:rolloutGroupId and g.status=:status")
    List<JpaRolloutGroup> findByParentIdAndStatus(@Param("rolloutGroupId") long rolloutGroupId,
            @Param("status") RolloutGroupStatus status);

    /**
     * Retrieves all {@link JpaRolloutGroup} entities that have the specified parent ID.
     *
     * @param rolloutGroupId the ID of the parent rollout group
     * @return a list of {@link JpaRolloutGroup} entities that have the specified parent ID
     */
    @Query("SELECT g FROM JpaRolloutGroup g WHERE g.parent.id=:rolloutGroupId")
    List<JpaRolloutGroup> findByParentId(@Param("rolloutGroupId") long rolloutGroupId);

    /**
     * Updates all {@link RolloutGroup#getStatus()} of children for given
     * parent.
     *
     * @param parent
     *            the parent rolloutgroup
     * @param status
     *            the status of the rolloutgroups
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaRolloutGroup g SET g.status = :status WHERE g.parent = :parent")
    void setStatusForChildren(@Param("status") RolloutGroupStatus status, @Param("parent") RolloutGroup parent);

    /**
     * Retrieves all {@link RolloutGroup} for a specific rollout and status not
     * having ordered by ID DESC, latest top.
     *
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param notStatus
     *            the status which the rolloutgroup should not have
     * @return rolloutgroup referring to a rollout and not having a specific
     *         status ordered by ID DESC.
     */
    List<JpaRolloutGroup> findByRolloutAndStatusNotOrderByIdDesc(JpaRollout rollout, RolloutGroupStatus notStatus);

    /**
     * Retrieves all {@link RolloutGroup}s for a specific rollout and status not
     * having.
     *
     * @param rollout
     *            the rollout the rolloutgroup belong to
     * @param status
     *            the status which the rolloutgroup should not have
     * @return rolloutgroup referring to a rollout and not having a specific
     *         status.
     */
    List<JpaRolloutGroup> findByRolloutAndStatusNotIn(JpaRollout rollout, Collection<RolloutGroupStatus> status);

    /**
     * Retrieves all {@link RolloutGroup} for a specific rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to find the rollout groups
     * @param page
     *            the page request to sort, limit the result
     * @return a page of found {@link RolloutGroup} or {@code empty}.
     */
    Page<JpaRolloutGroup> findByRolloutId(Long rolloutId, Pageable page);

    /**
     * Counts all {@link RolloutGroup} for a specific rollout.
     *
     * @param rolloutId
     *            the ID of the rollout to find the rollout groups
     *
     * @return the amount of found {@link RolloutGroup}s.
     */
    long countByRolloutId(Long rolloutId);

    @Modifying
    @Query("DELETE FROM JpaRolloutGroup g where g.id in :rolloutGroupIds")
    void deleteByIds(@Param("rolloutGroupIds") List<Long> rolloutGroups);


    /**
     * Retrieves all {@link JpaRolloutGroup} entities for a specific rollout by its ID.
     *
     * @param rolloutId the ID of the rollout to find the rollout groups
     * @return a list of {@link JpaRolloutGroup} entities belonging to the specified rollout
     */
    List<JpaRolloutGroup> findByRolloutId(Long rolloutId);

    /**
     * Retrieves all rollout groups associated with the given rollout and target IDs.
     * <p>
     * This query finds all the {@link JpaRolloutGroup} entries that are associated with the provided list of target IDs
     * and a specific rollout ID. It joins the {@link JpaRollout} and {@link JpaRolloutGroup} tables based on the target
     * ID and rollout ID.
     * </p>
     *
     * @param rolloutId The ID of the rollout for which the groups are being fetched.
     * @param targetIds A list of target IDs to filter the rollout groups by.
     * @return A list of {@link JpaRolloutGroup} that match the given target IDs and rollout ID.
     */
    @Query("SELECT DISTINCT rg FROM JpaRollout r " +
            "JOIN r.rolloutGroups rg " +
            "JOIN rg.rolloutTargetGroup rtg " +
            "WHERE rtg.target.id IN :targetIds " +
            "AND r.id = :rolloutId")
    List<JpaRolloutGroup> findAllRolloutGroupsByTargetIds(@Param("rolloutId") Long rolloutId, @Param("targetIds") List<Long> targetIds);

    /**
     * find RolloutGroup by its name and associated to rolloutId
     * @param name name of rollout group
     * @param rolloutId associated rolloutId
     * @return matched RolloutGroup
     */
    Optional<RolloutGroup> findByNameAndRolloutId(String name, Long rolloutId);

    /**
     * Retrieves the first {@link JpaRolloutGroup} for a specific rollout ID, ordered by ID in descending order.
     * @param rolloutId the ID of the rollout to find the rollout group
     * @return an Optional containing the first {@link JpaRolloutGroup} for the specified rollout ID, or empty if none found
     */
    @Query("SELECT rg FROM JpaRolloutGroup rg WHERE rg.rollout.id = :rolloutId ORDER BY rg.id DESC")
    List<JpaRolloutGroup> findTopByRolloutIdOrderByIdDesc(@Param("rolloutId") Long rolloutId, Pageable pageable);

    /**
     * Updates the status of all {@link JpaRolloutGroup} entities to a new status if their current status
     * is in the provided list of old statuses and their associated rollout has expired.
     *
     * @param newStatus     The new status to set for the rollout groups.
     * @param oldStatuses   A list of old statuses to filter the rollout groups.
     * @param nowInSeconds  The current time in seconds, used to check if the rollout has expired.
     */
    @Modifying
    @Query("UPDATE JpaRolloutGroup rg SET rg.status = :newStatus WHERE rg.status in :oldStatuses AND rg.rollout.endAt < :nowInSeconds")
    void updateStatusByStatusAndRollout(@Param("newStatus") RolloutGroupStatus newStatus,
                                        @Param("oldStatuses") List<RolloutGroupStatus> oldStatuses,
                                        @Param("nowInSeconds") Long nowInSeconds);

    /**
     * Retrieves all {@link JpaRolloutGroup} entities associated with the given rollout
     * that have a status included in the provided list of statuses.
     *
     * @param rollout the {@link JpaRollout} entity for which the rollout groups are retrieved.
     * @param statuses a list of {@link RolloutGroupStatus} values to filter the rollout groups.
     * @return a list of {@link JpaRolloutGroup} entities matching the rollout and any of the specified statuses.
     */
    List<JpaRolloutGroup> findByRolloutAndStatusIn(JpaRollout rollout, List<RolloutGroupStatus> statuses);

    /**
     *
     * @param rolloutId
     * @param newStatus
     * @param oldStatus
     * @return
     */
    @Modifying
    @Query(value = "UPDATE sp_rolloutgroup " + "SET status = ?2 " + "WHERE rollout = ?1 " + "AND status = ?3", nativeQuery = true)
    int updateGroupsStatusToOneStatus(Long rolloutId, Integer newStatus, Integer oldStatus);


}
