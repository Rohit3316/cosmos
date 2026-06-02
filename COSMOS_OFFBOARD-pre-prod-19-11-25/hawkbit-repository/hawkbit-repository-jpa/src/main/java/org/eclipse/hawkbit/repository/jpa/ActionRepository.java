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

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link Action} repository.
 *
 */
@Transactional(readOnly = true)
public interface ActionRepository extends BaseEntityRepository<JpaAction, Long>, JpaSpecificationExecutor<JpaAction> {
    /**
     * Retrieves an Action with all lazy attributes.
     *
     * @param actionId the ID of the action
     * @return the found {@link Action}
     */
    @EntityGraph(value = "Action.all", type = EntityGraphType.LOAD)
    @Query("SELECT a FROM JpaAction a WHERE a.id = :actionId AND a.active = :active")
    Optional<Action> getActionById(Long actionId, boolean active);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link DistributionSet}.
     *
     * @param pageable page parameters
     * @param dsId     the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    Page<Action> findByDistributionSetId(Pageable pageable, Long dsId);

    /**
     * Retrieves all active {@link Action}s which are referring the given
     * {@link DistributionSet}.
     *
     * @param set the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    List<Action> findByDistributionSetAndActiveIsTrue(DistributionSet set);

    /**
     * Retrieves all active {@link Action}s which are referring the given
     * {@link DistributionSet} and are not in the given state
     *
     * @param set    the {@link DistributionSet} on which will be filtered
     * @param status the state the actions should not have
     * @return the found {@link Action}s
     */
    List<Action> findByDistributionSetAndActiveIsTrueAndStatusIsNot(DistributionSet set, DeviceActionStatus status);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link Target}.
     *
     * @param pageable     page parameters
     * @param controllerId the target to find assigned actions
     * @return the found {@link Action}s
     */
    Slice<Action> findByTargetControllerIdAndActive(Pageable pageable, String controllerId, boolean active);

    /**
     * Retrieves all {@link Action}s which are active and referring to the given
     * {@link Target} order by ID ascending.
     *
     * @param target the target to find assigned actions
     * @param active the action active flag
     * @return the found {@link Action}s
     */
    List<Action> findByTargetAndActiveOrderByIdAsc(JpaTarget target, boolean active);

    /**
     * Retrieves a page of active {@link Action}s for the given target controller ID,
     * where each action has a non-null weight and its status is not in the provided list.
     * The results are ordered by descending weight (highest first), and for actions with
     * the same weight, by ascending ID (oldest first).
     * <p>
     * Loads the associated {@link DistributionSet} using the "Action.ds" entity graph.
     *
     * @param pageable     pagination information
     * @param controllerId the controller ID of the target to filter actions
     * @param status       a list of {@link DeviceActionStatus} values to exclude from the results
     * @return a page of matching {@link Action}s
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    Page<Action> findByTargetControllerIdAndActiveIsTrueAndWeightIsNotNullAndStatusNotInOrderByWeightDescIdAsc(Pageable pageable,
                                                                                                               String controllerId,
                                                                                                               List<DeviceActionStatus> status);

    /**
     * Retrieves a page of active {@link Action}s for the given target controller ID,
     * where each action has a null weight and its status is not in the provided list.
     * The results are ordered by ascending ID (oldest first).
     * <p>
     * Loads the associated {@link DistributionSet} using the "Action.ds" entity graph.
     *
     * @param pageable     pagination information
     * @param controllerId the controller ID of the target to filter actions
     * @param status       a list of {@link DeviceActionStatus} values to exclude from the results
     * @return a page of matching {@link Action}s
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    Page<Action> findByTargetControllerIdAndActiveIsTrueAndWeightIsNullAndStatusNotInOrderByIdAsc(Pageable pageable,
                                                                                                  String controllerId,
                                                                                                  List<DeviceActionStatus> status);

    /**
     * Checks if an active action exists for given
     * {@link Target#getControllerId()}.
     *
     * @param controllerId of target to check
     * @return <code>true</code> if an active action for the target exists.
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a JOIN a.target t WHERE t.controllerId=:controllerId AND a.active=1")
    boolean activeActionExistsForControllerId(@Param("controllerId") String controllerId);

    /**
     * Check if any active actions with given action status and given controller
     * ID exist.
     *
     * @param controllerId  of the target to check for actions
     * @param currentStatus of the active action to look for
     * @return <code>true</code> if one or more active actions for the given
     * controllerId and action status are found
     */
    boolean existsByTargetControllerIdAndStatusAndActiveIsTrue(String controllerId, DeviceActionStatus currentStatus);

    /**
     * Retrieves latest {@link Action} for given target and
     * {@link SoftwareModule}.
     *
     * @param targetId to search for
     * @param moduleId to search for
     * @return action if there is one with assigned target and module is part of
     * assigned {@link DistributionSet}.
     */
    @Query("Select a " +
            "from JpaAction a " +
            "join a.distributionSet ds " +
            "join ds.modules modul " +
            "join modul.sm som " +
            "where a.target.controllerId = :target and som.id = :module AND a.active = :active order by a.id desc")
    List<Action> findActionByTargetAndSoftwareModule(@Param("target") String targetId, @Param("module") Long moduleId,
                                                     boolean active);

    /**
     * Retrieves the latest finished {@link Action} for given target and {@link DistributionSet}.
     *
     * @param targetId the action belongs to
     * @param dsId     of the ds that is assigned to the target
     * @param status   of the action
     * @return action if there is one with assigned target and assigned
     * {@link DistributionSet}.
     */
    Optional<Action> findFirstByTargetIdAndDistributionSetIdAndStatusInAndActiveIsTrueOrderByIdDesc(@Param("target") long targetId,
                                                                                                    @Param("ds") Long dsId, @Param("status") List<DeviceActionStatus> status);

    /**
     * Retrieves all {@link Action}s which are referring the given
     * {@link DistributionSet} and {@link Target}.
     *
     * @param pageable page parameters
     * @param target   is the assigned target
     * @param ds       the {@link DistributionSet} on which will be filtered
     * @return the found {@link Action}s
     */
    @Query("Select a from JpaAction a where a.target = :target and a.distributionSet = :ds AND a.active = :active")
    Page<JpaAction> findByTargetAndDistributionSet(Pageable pageable, @Param("target") JpaTarget target,
                                                   @Param("ds") JpaDistributionSet ds, boolean active);

    /**
     * Retrieves all {@link Action}s of a specific target, without pagination
     * ordered by action ID.
     *
     * @param target to search for
     * @return a list of actions according to the searched target
     */
    @Query("Select a from JpaAction a where a.target = :target and a.active = :active order by a.id")
    List<JpaAction> findByTarget(@Param("target") Target target, boolean active);

    /**
     * Retrieves all {@link Action}s of a specific target and given active flag
     * ordered by action ID. Loads also the lazy {@link Action#getDistributionSet()}
     * field.
     *
     * @param pageable     page parameters
     * @param controllerId to search for
     * @param active       {@code true} for all actions which are currently active,
     *                     {@code false} for inactive
     * @return a list of actions
     */
    @EntityGraph(value = "Action.ds", type = EntityGraphType.LOAD)
    @Query("Select a from JpaAction a where a.target.controllerId = :controllerId and a.active = :active")
    Page<Action> findByActiveAndTarget(Pageable pageable, @Param("controllerId") String controllerId,
                                       @Param("active") boolean active);

    /**
     * Switches the status of actions from one specific status into another, only if
     * the actions are in a specific status. This should be a atomar operation.
     *
     * @param statusToSet   the new status the actions should get
     * @param targetIds     the IDs of the targets of the actions which are affected
     * @param active        the active flag of the actions which should be affected
     * @param currentStatus the current status of the actions which are affected
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :statusToSet WHERE a.target IN :targetsIds AND a.active = :active AND a.status = :currentStatus AND a.distributionSet.requiredMigrationStep = false")
    void switchStatus(@Param("statusToSet") DeviceActionStatus statusToSet, @Param("targetsIds") List<Long> targetIds,
                      @Param("active") boolean active, @Param("currentStatus") DeviceActionStatus currentStatus);

    /**
     *
     * Retrieves all active {@link Action}s by given controllerId filtered by a
     * status
     *
     * @param controllerId the IDs of targets for the actions
     * @param status       the current status of the actions
     * @return the found list of {@link Action}
     */
    @Query("SELECT a FROM JpaAction a WHERE a.target.controllerId = :controllerId AND a.active = true AND a.status = :status")
    List<JpaAction> findByTargetIdAndIsActiveAndActionStatus(@Param("controllerId") String controllerId,
                                                             @Param("status") DeviceActionStatus status);

    /**
     *
     * Retrieves all IDs for {@link Action}s referring to the given target IDs,
     * active flag, current status and distribution set not requiring migration
     * step.
     *
     * @param targetIds     the IDs of targets for the actions
     * @param active        flag to indicate active/inactive actions
     * @param currentStatus the current status of the actions
     * @return the found list of {@link Action} IDs
     */
    @Query("SELECT a.id FROM JpaAction a WHERE a.target IN :targetsIds AND a.active = :active AND a.status = :currentStatus AND a.distributionSet.requiredMigrationStep = false")
    List<Long> findByTargetIdInAndIsActiveAndActionStatusAndDistributionSetNotRequiredMigrationStep(
            @Param("targetsIds") List<Long> targetIds, @Param("active") boolean active,
            @Param("currentStatus") DeviceActionStatus currentStatus);

    /**
     * Retrieves all {@link Action}s that matches the queried externalRefs.
     *
     * @param externalRefs for which the actions need to be found
     * @param active       flag to indicate active/inactive actions
     * @return list of actions
     */
    List<Action> findByExternalRefInAndActive(@Param("externalRefs") List<String> externalRefs,
                                              @Param("active") boolean active);

    /**
     * Retrieves an {@link Action} that matches the queried externalRef.
     *
     * @param externalRef of the action. See {@link Action#getExternalRef()}
     * @return the found {@link Action}
     */
    Optional<Action> findByExternalRef(@Param("externalRef") String externalRef);

    /**
     * Switches the status of actions from one specific status into another for
     * given actions IDs, active flag and current status
     *
     * @param statusToSet   the new status the actions should get
     * @param actionIds     the IDs of the actions which are affected
     * @param active        flag to indicate active/inactive actions
     * @param currentStatus the current status of the actions
     * @return the amount of updated actions
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :statusToSet WHERE a.id IN :actionIds AND a.active = :active AND a.status = :currentStatus")
    int switchStatusForActionIdInAndIsActiveAndActionStatus(@Param("statusToSet") DeviceActionStatus statusToSet,
                                                            @Param("actionIds") List<Long> actionIds, @Param("active") boolean active,
                                                            @Param("currentStatus") DeviceActionStatus currentStatus);

    /**
     *
     * Retrieves all {@link Action}s which are active and referring to the given
     * target Ids and distribution set not requiring migration step.
     *
     * @param targetIds the IDs of targets for the actions
     * @param notStatus the status which the actions should not have
     * @return the found list of {@link Action}s
     */
    @EntityGraph(attributePaths = {"target"}, type = EntityGraphType.LOAD)
    @Query("SELECT a FROM JpaAction a WHERE a.active = true AND a.distributionSet.requiredMigrationStep = false AND a.target IN ?1 AND a.status != ?2")
    List<JpaAction> findByActiveAndTargetIdInAndActionStatusNotEqualToAndDistributionSetNotRequiredMigrationStep(
            Collection<Long> targetIds, DeviceActionStatus notStatus);

    /**
     *
     * Retrieves all {@link Action}s which are active and referring to the given
     * target Ids and distribution set not requiring migration step.
     *
     * @param targetIds the IDs of targets for the actions
     * @return the found list of {@link Action}s
     */
    @EntityGraph(attributePaths = {"target"}, type = EntityGraphType.LOAD)
    @Query("SELECT a FROM JpaAction a WHERE a.active = true AND a.distributionSet.requiredMigrationStep = false AND a.target IN ?1")
    List<JpaAction> findByActiveAndTargetIdInAndDistributionSetNotRequiredMigrationStep(Collection<Long> targetIds);

    /**
     * Counts all {@link Action}s referring to the given target.
     *
     * @param controllerId the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTarget_ControllerIdAndActive(String controllerId, boolean active);

    /**
     * Counts all {@link Action}s referring to the given targetId.
     *
     * @param targetId the target to count the {@link Action}s
     * @return the count of actions referring to the given target
     */
    Long countByTargetIdAndActive(Long targetId, boolean active);

    /**
     * Counts all {@link Action}s referring to the given DistributionSet.
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetId(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet.
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrue(Long distributionSet);

    /**
     * Counts all active {@link Action}s referring to the given DistributionSet
     * that are not in a given state.
     *
     * @param distributionSet DistributionSet to count the {@link Action}s from
     * @param status          the state the actions should not have
     * @return the count of actions referring to the given distributionSet
     */
    Long countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(Long distributionSet, DeviceActionStatus status);

    /**
     * Counts all actions referring to a given rollout and rolloutgroup which
     * are currently not in the given status. An in-clause statement does not
     * work with the spring-data, so this is specific usecase regarding to the
     * rollout-management to find out actions which are not in specific states.
     *
     * @param rollout      the rollout the actions are belong to
     * @param rolloutGroup the rolloutgroup the actions are belong to
     * @param statuses     the list of statuses the action should not have
     * @return the count of actions referring the rollout and rolloutgroup and
     * are not in given states
     */
    Long countByRolloutAndRolloutGroupAndStatusNotInAndActive(JpaRollout rollout, JpaRolloutGroup rolloutGroup,
                                                              List<DeviceActionStatus> statuses, boolean active);

    /**
     * Counts all actions referring to a given rollout and rolloutgroup.
     *
     * @param rollout      the rollout the actions belong to
     * @param rolloutGroup the rolloutgroup the actions belong to
     * @return the count of actions referring to a rollout and rolloutgroup
     */
    Long countByRolloutAndRolloutGroupAndActive(JpaRollout rollout, JpaRolloutGroup rolloutGroup, boolean active);

    /**
     * Counts all actions referring to a given rollout, rolloutgroup and status.
     *
     * @param rolloutId      the ID of rollout the actions belong to
     * @param rolloutGroupId the ID rolloutgroup the actions belong to
     * @param status         the status the actions should have
     * @return the count of actions referring to a rollout, rolloutgroup and are
     * in a given status
     */
    Long countByRolloutIdAndRolloutGroupIdAndStatus(Long rolloutId, Long rolloutGroupId, DeviceActionStatus status);

    /**
     * Counts all actions referring to a given rollout, rollout group, and a list of statuses.
     *
     * @param rolloutId      the ID of the rollout the actions belong to
     * @param rolloutGroupId the ID of the rollout group the actions belong to
     * @param statusList     the list of statuses the actions should have
     * @return the count of actions referring to a rollout, rollout group, and are in the given statuses
     */
    Long countByRolloutIdAndRolloutGroupIdAndStatusIn(Long rolloutId, Long rolloutGroupId, List<DeviceActionStatus> statusList);


    /**
     * Counts all actions referring to a given rollout and status.
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @param status    the status the actions should have
     * @return the count of actions referring to a rollout and are in a given
     * status
     */
    Long countByRolloutIdAndStatusAndActive(Long rolloutId, DeviceActionStatus status, boolean active);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @return {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId AND a.active = :active")
    boolean existsByRolloutId(@Param("rolloutId") Long rolloutId, boolean active);

    /**
     * Returns {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     *
     * @param rolloutId the ID of the rollout the actions belong to
     * @param status    the action is not to be in
     * @return {@code true} if actions for the given rollout exists, otherwise
     * {@code false}
     */
    @Query("SELECT CASE WHEN COUNT(a)>0 THEN 'true' ELSE 'false' END FROM JpaAction a WHERE a.rollout.id=:rolloutId AND a.status != :status AND a.active = :active")
    boolean existsByRolloutIdAndStatusNotIn(@Param("rolloutId") Long rolloutId, @Param("status") DeviceActionStatus status, boolean active);

    /**
     * Retrieving all actions referring to a given rollout with a specific action as
     * parent reference and a specific status.
     * <p>
     * Finding all actions of a specific rolloutgroup parent relation.
     *
     * @param pageable           page parameters
     * @param rollout            the rollout the actions belong to
     * @param rolloutGroupParent the parent rolloutgroup the actions should reference
     * @param actionStatus       the status the actions have
     * @return the actions referring a specific rollout and a specific parent
     * rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = {"target", "target.autoConfirmationStatus", "rolloutGroup"}, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIdAndStatusAndActive(Pageable pageable, Long rollout,
                                                                          Long rolloutGroupParent, DeviceActionStatus actionStatus,
                                                                          boolean active);

    /**
     * Retrieving all actions referring to the first group of a rollout.
     *
     * @param pageable     page parameters
     * @param rollout      the rollout the actions belong to
     * @param actionStatus the status the actions have
     * @return the actions referring a specific rollout and a specific parent
     * rolloutgroup in a specific status
     */
    @EntityGraph(attributePaths = {"target", "target.autoConfirmationStatus",
            "rolloutGroup"}, type = EntityGraphType.LOAD)
    Page<Action> findByRolloutIdAndRolloutGroupParentIsNullAndStatusAndActive(Pageable pageable, Long rollout,
                                                                              DeviceActionStatus actionStatus,
                                                                              boolean active);

    /**
     * Retrieves all actions for a specific rollout and in a specific status.
     *
     * @param pageable     page parameters
     * @param rolloutId    the rollout the actions belong to
     * @param actionStatus the status of the actions
     * @return the actions referring a specific rollout an in a specific status
     */
    Page<JpaAction> findByRolloutIdAndStatusAndActive(Pageable pageable, Long rolloutId, DeviceActionStatus actionStatus, boolean active);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout.
     *
     * @param rolloutId id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus( a.rollout.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id IN ?1 and a.active = ?2 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutId(List<Long> rolloutId, boolean active);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout.
     *
     * @param rolloutId id of {@link Rollout}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus( a.rollout.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rollout.id = ?1 and a.active = ?2 GROUP BY a.rollout.id,a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutId(Long rolloutId, boolean active);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout group.
     *
     * @param rolloutGroupId id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id = ?1 and a.active = ?2 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupId(Long rolloutGroupId, boolean active);

    /**
     * Get list of objects which has details of status and count of targets in
     * each status in specified rollout group.
     *
     * @param rolloutGroupId list of id of {@link RolloutGroup}
     * @return list of objects with status and target count
     */
    @Query("SELECT NEW org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus(a.rolloutGroup.id, a.status , COUNT(a.id)) FROM JpaAction a WHERE a.rolloutGroup.id IN ?1 and a.active = ?2 GROUP BY a.rolloutGroup.id, a.status")
    List<TotalTargetCountActionStatus> getStatusCountByRolloutGroupId(List<Long> rolloutGroupId, boolean active);

    /**
     * Deletes all actions with the given IDs.
     *
     * @param actionIDs the IDs of the actions to be deleted.
     */
    @Modifying
    @Transactional
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("DELETE FROM JpaAction a WHERE a.id IN ?1")
    void deleteByIdIn(Collection<Long> actionIDs);

    /**
     * Updates the externalRef of an action by its actionId.
     *
     * @param actionId    for which the externalRef is being updated.
     * @param externalRef value of the external reference for the given action id.
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.externalRef = :externalRef WHERE a.id = :actionId")
    void updateExternalRef(@Param("actionId") Long actionId, @Param("externalRef") String externalRef);

    /**
     * Retrieves the actionId's that are associated with the given target and rolloutId
     *
     * @param targetId
     * @param rolloutId
     * @return
     */
    @Query("SELECT a FROM JpaAction a WHERE a.target.id IN :targetId and a.rollout.id = :rolloutId AND a.active = :active")
    List<JpaAction> getActionIdsByTargetIdAndRolloutId(@Param("targetId") List<Long> targetId, long rolloutId, boolean active);

    /**
     * Retrives the actions that are associated to the target and rolloutGroup
     *
     * @param targetId
     * @param rolloutGroupId
     * @param pageable
     * @return
     */
    Page<JpaAction> findByTargetIdInAndRolloutGroupIdIn(List<Long> targetId, List<Long> rolloutGroupId, Pageable pageable);

    @Query("SELECT a FROM JpaAction a WHERE a.rolloutGroup.id = :rolloutGroupId AND a.active = :active")
    List<JpaAction> getActionsByRolloutGroupId(@Param("rolloutGroupId") Long rolloutGroupId, boolean active);

    @Query("SELECT a FROM JpaAction a WHERE a.target.controllerId = :controllerId AND a.rollout.id = :rolloutId and a.active = :active")
    Optional<JpaAction> getActionByRolloutIdAndControllerId(@Param("rolloutId") long rolloutId, @Param("controllerId") String controllerId, boolean active);

    List<JpaAction> findByRolloutIdAndTargetControllerIdAndActive(long rolloutId, String controllerId, boolean active);

    /**
     * get the actions associated to the given rolloutId and controllerId also based on the status
     *
     * @param rolloutId
     * @param controllerId
     * @param status
     */
    @Query("SELECT a FROM JpaAction a WHERE a.target.controllerId = :controllerId AND a.rollout.id = :rolloutId AND a.status = :status AND a.active = :active")
    Optional<JpaAction> getActionByRolloutIdAndControllerIdByStatus(@Param("rolloutId") long rolloutId, @Param("controllerId") String controllerId, @Param("status") DeviceActionStatus status, boolean active);

    /**
     * Retrieves all {@link JpaAction} entities that belong to a specific rollout and rollout group.
     *
     * @param rolloutId      the ID of the rollout
     * @param rolloutGroupId the ID of the rollout group
     * @return a list of {@link JpaAction} entities that match the given rollout and rollout group IDs
     */
    @Query("SELECT a FROM JpaAction a WHERE a.rollout.id = :rolloutId AND a.rolloutGroup.id = :rolloutGroupId AND a.active = :active")
    List<JpaAction> findByRolloutIdAndRolloutGroupId(long rolloutId, long rolloutGroupId, boolean active);

    /**
     * Retrieves all {@link JpaAction} entities that belong to a specific rollout and rollout group.
     *
     * @param rolloutId the ID of the rollout
     * @param rolloutGroupId the ID of the rollout group
     * @return a list of {@link JpaAction} entities that match the given rollout and rollout group IDs
     */
    @Query("SELECT a FROM JpaAction a WHERE a.rollout.id = :rolloutId AND a.rolloutGroup.id = :rolloutGroupId")
    List<JpaAction> findByRolloutIdAndRolloutGroupId(long rolloutId, long rolloutGroupId);

    @Query("SELECT jas FROM JpaActionStatus jas WHERE jas.action.id = :actionId")
    List<JpaActionStatus> getActionStatusByActionId(@Param("actionId") long actionId);

    /**
     * Retrieves a paginated list of {@link JpaAction} entities that belong to the specified rollout groups.
     *
     * @param rolloutGroupId a list of rollout group IDs to filter the actions
     * @param pageable       the pagination information
     * @return a {@link Page} of {@link JpaAction} entities matching the given rollout group IDs
     */
    Page<JpaAction> findByRolloutGroupIdInAndActive(List<Long> rolloutGroupId, Pageable pageable, boolean active);

    List<JpaAction> findActionByRolloutIdAndActive(Long rolloutId, boolean active);


    /**
     * Updates the status of active actions for expired rollouts.
     * <p>
     * This method updates the status of actions whose current status is in the provided list of old statuses
     * and whose associated rollout has already expired (based on the `endAt` timestamp).
     * </p>
     *
     * @param newStatus   The new status to set for the actions.
     * @param oldStatuses A list of statuses to filter the actions that need to be updated.
     * @param now         The current timestamp in seconds, used to determine expired rollouts.
     */
    @Modifying
    @Transactional
    @Query("UPDATE JpaAction a SET a.status = :newStatus WHERE a.status in :oldStatuses AND a.rollout.endAt < :now")
    void updateActiveActionStatusForExpiredRollout(@Param("newStatus") DeviceActionStatus newStatus,
                                                   @Param("oldStatuses") List<DeviceActionStatus> oldStatuses, @Param("now") Long now);

    /**
     *
     * Retrieves first active {@link Action}s by given controllerId and actionId
     *
     * @param controllerId the IDs of targets for the actions
     * @param actionId     the current ID of the actions
     * @return the found list of {@link Action}
     */
    @Query("SELECT a FROM JpaAction a WHERE a.target.controllerId = :controllerId AND a.id = :actionId AND a.active = :active")
    Optional<Action> findFirstByTargetIdAndActionId(@Param("controllerId") String controllerId,
                                                    @Param("actionId") Long actionId,
                                                    boolean active);

    /**
     * Retrieves all {@link JpaAction}s that match the specified target IDs and rollout ID.
     *
     * @param targetIds the list of target IDs to filter the actions
     * @param rolloutId the ID of the rollout to filter the actions
     * @return a list of {@link JpaAction} entities that match the given target IDs and rollout ID
     */
    @Query("SELECT a FROM JpaAction a WHERE a.rollout.id = :rolloutId AND a.target.id IN :targetIds AND a.active = :active")
    List<JpaAction> findByRolloutIdAndTargetIdIn(@Param("targetIds") List<Long> targetIds, @Param("rolloutId") Long rolloutId, boolean active);



    /**
     * Retrieves all active {@link JpaAction} entities associated with a specific rollout ID and status,
     * including their related {@link RolloutGroup} entities.
     *
     * @param rolloutId the ID of the rollout to filter the actions
     * @param status    the status to filter the actions
     * @return a list of active {@link JpaAction} entities matching the given rollout ID and status,
     * with their associated {@link RolloutGroup} entities eagerly loaded
     */

    @Query(value = "SELECT a.*, g.* " + "FROM sp_action a " + "JOIN sp_rolloutgroup g ON a.rolloutgroup = g.id " + "WHERE a.rollout = ?1 " + "AND a.status = ?2 " +
            "AND a.active = TRUE", nativeQuery = true)
    List<JpaAction> getActionsAndGroupsByRolloutAndStatus(long rolloutId, Integer status);

    /**
     * Deactivates the action with the specified action ID by setting its active status to FALSE.
     *
     * @param actionId the ID of the action to be deactivated
     */
    @Modifying
    @Query(value = "UPDATE sp_action SET active = FALSE WHERE id = ?1", nativeQuery = true)
    void updateActiveStatus(@Param("actionId") Long actionId);


    /**
     * Retrieves an {@link JpaAction} by its ID, including its associated RolloutGroup and Target entities.
     *
     * @param actionId the ID of the action to retrieve
     * @return an {@link Optional} containing the found {@link JpaAction} with its associations, or empty if not found
     */
    @Query("SELECT a FROM JpaAction a " + "JOIN FETCH a.rolloutGroup rg " + "JOIN FETCH a.target t " + "WHERE a.id = :actionId")
    Optional<JpaAction> findByIdWithAssociations(@Param("actionId") Long actionId);


}
