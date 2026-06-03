/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroup;
import org.eclipse.hawkbit.repository.jpa.model.RolloutTargetGroupId;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Spring data repository for {@link RolloutTargetGroup}.
 *
 */
@Transactional(readOnly = true)
public interface RolloutTargetGroupRepository
        extends CrudRepository<RolloutTargetGroup, RolloutTargetGroupId>, JpaSpecificationExecutor<RolloutTargetGroup> {

    /**
     * Counts all entries that have the specified rolloutGroup
     * 
     * @param rolloutGroup
     *            the group to filter for
     * @return count of targets in the group
     */
    Long countByRolloutGroup(JpaRolloutGroup rolloutGroup);

    /**
     * Retrieves all the targetId's that are associated to the specified rolloutGroup
     */
    @Query("SELECT rtg.target.id " +
            "FROM RolloutTargetGroup rtg " +
            "JOIN rtg.rolloutGroup rg " +
            "WHERE rg.id IN :rolloutGroupIdList")
    List<Long> getTargetIdsByRolloutGroupIds(@Param("rolloutGroupIdList") List<Long> rolloutGroupIdList);

    /**
     * Retrieves all the target controllerIds that are associated to the specified rolloutGroups
     */
    @Query("SELECT rtg.target.controllerId " +
            "FROM RolloutTargetGroup rtg " +
            "JOIN rtg.rolloutGroup rg " +
            "WHERE rg.id IN :rolloutGroupId")
    List<String> getTargetControllerIdsByRolloutGroupId(@Param("rolloutGroupId") List<Long> rolloutGroupIds);

    /**
     * Deletes the association between the given target IDs and the specified rollout group.
     * <p>
     * This query removes the entries from the {@link RolloutTargetGroup} table where the target ID matches any of the
     * provided target IDs and the associated rollout group matches the provided {@link JpaRolloutGroup}.
     * </p>
     *
     * @param targetIds A list of target IDs to be deleted from the association.
     * @param rolloutGroup The rollout group from which the target IDs will be removed.
     */
    @Modifying
    @Transactional
    @Query("delete from RolloutTargetGroup rtg where rtg.target.id in ?1 and rtg.rolloutGroup = ?2")
    void deleteTargetIdAndRolloutGroupAssociation(List<Long> targetIds, JpaRolloutGroup rolloutGroup);

    /**
     * Retrieves a list of {@link MgmtRolloutGroupTarget} DTOs representing associations between rollout group IDs and target IDs.
     * <p>
     * This query selects the rollout group ID and corresponding target ID from the {@link RolloutTargetGroup} entity,
     * joining with the {@link Target} entity. Only rollout group IDs that match the provided list are considered.
     * </p>
     *
     * @param groupIds the list of rollout group IDs to filter by; must not be {@code null} or empty.
     * @return a list of {@link MgmtRolloutGroupTarget} containing the rollout group ID and associated target ID.
     */
    @Query("SELECT new org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupTarget(rtg.rolloutGroup.id, t.id) " +
            "FROM RolloutTargetGroup rtg JOIN rtg.target t WHERE rtg.rolloutGroup.id IN :groupIds")
    List<MgmtRolloutGroupTarget> getTargetsGroupedByRolloutGroupId(@Param("groupIds") List<Long> groupIds);

    /**
     * Finds a RolloutTargetGroup by the given rollout group ID and target ID.
     *
     * @param rolloutGroupId the ID of the rollout group
     * @param targetId       the ID of the target
     * @return an Optional containing the found RolloutTargetGroup, or empty if not found
     */
    @Query(value = "SELECT * FROM sp_rollouttargetgroup rtg " + "WHERE rtg.rolloutgroup_id = ?1 " + "AND rtg.target_id = ?2", nativeQuery = true)
    Optional<RolloutTargetGroup> findByRolloutGroupAndTarget(Long rolloutGroupId, Long targetId);


    @Query("SELECT rtg FROM RolloutTargetGroup rtg " +
            "WHERE rtg.target.id = :targetId AND rtg.rolloutGroup.id = :rolloutGroupId")
    Optional<RolloutTargetGroup> findByTargetIdAndRolloutGroupId(@Param("targetId") Long targetId, @Param("rolloutGroupId") Long rolloutGroupId);
}
