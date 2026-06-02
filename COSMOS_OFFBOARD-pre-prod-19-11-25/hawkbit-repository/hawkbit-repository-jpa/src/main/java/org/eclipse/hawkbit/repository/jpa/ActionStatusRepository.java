/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link ActionStatus} repository.
 */
@Transactional(readOnly = true)
public interface ActionStatusRepository extends BaseEntityRepository<JpaActionStatus, Long>, JpaSpecificationExecutor<JpaActionStatus> {

    /**
     * Counts {@link ActionStatus} entries of given {@link Action} in
     * repository.
     *
     * @param action to count status entries
     * @return number of actions in repository
     */
    Long countByAction(JpaAction action);

    /**
     * Counts {@link ActionStatus} entries of given {@link Action} in
     * repository.
     *
     * @param actionId of the action to count status entries for
     * @return number of actions in repository
     */
    long countByActionId(Long actionId);

    /**
     * Retrieves all {@link ActionStatus} entries from repository of given
     * ActionId.
     *
     * @param pageReq  parameters
     * @param actionId of the status entries
     * @return pages list of {@link ActionStatus} entries
     */
    Page<ActionStatus> findByActionId(Pageable pageReq, Long actionId);

    /**
     * Finds all status updates for the defined action and target including
     * {@link ActionStatus#getMessages()}.
     *
     * @param pageReq  for page configuration
     * @param target   to look for
     * @param actionId to look for
     * @return Page with found targets
     */
    @EntityGraph(value = "ActionStatus.withMessages", type = EntityGraphType.LOAD)
    Page<ActionStatus> getByActionId(Pageable pageReq, Long actionId);

    /**
     * Finds a filtered list of status messages for an action.
     *
     * @param pageable for page configuration
     * @param actionId for which to get the status messages
     * @param filter   is the SQL like pattern to use for filtering out or excluding
     *                 the messages
     * @return Page with found status messages.
     */
    @Query("SELECT message FROM JpaActionStatus actionstatus JOIN actionstatus.messages message WHERE actionstatus.action.id = :actionId AND message NOT LIKE :filter")
    Page<String> findMessagesByActionIdAndMessageNotLike(Pageable pageable, @Param("actionId") Long actionId,
                                                         @Param("filter") String filter);

    /**
     * Counts no of {@link ActionStatus}'s available for given {@link Rollout#getId()} and {@link DeviceActionStatus}
     *
     * @param rolloutId {@link Rollout#getId()}
     * @param status    the {@link DeviceActionStatus}
     * @return Long count of {@link ActionStatus}
     */
    @Query("SELECT count(actionStatus.id) FROM JpaActionStatus actionStatus JOIN actionStatus.action action WHERE action.rollout.id = :rolloutId and actionStatus.status = :status")
    Long countByRolloutIdAndStatus(@Param("rolloutId") Long rolloutId, @Param("status") DeviceActionStatus status);

    /**
     * Retrieves the latest {@link ActionStatus} for a given action.
     *
     * @param actionId the ID of the action
     * @return the latest {@link ActionStatus} for the given action
     */
    JpaActionStatus findFirstByActionIdOrderByOccurredAtDesc(Long actionId);

    /**
     * Retrieves a list of the latest {@link ActionStatus}
     * for a given action ID and controller ID, ordered by occurrence time descending.
     *
     * @param actionId     the ID of the action to filter by
     * @param controllerId the controller ID associated with the target
     * @return the latest {@link ActionStatus} for the given action ID and controller ID
     */
    @Query("SELECT s FROM JpaActionStatus s WHERE s.action.id = :actionId AND s.action.target.controllerId = :controllerId ORDER BY s.occurredAt DESC")
    List<JpaActionStatus> findStatusesByActionIdAndControllerId(
            @Param("actionId") Long actionId,
            @Param("controllerId") String controllerId);

    /**
     * Finds all {@link JpaActionStatus} entries for the given list of action IDs.
     *
     * @param actionIds list of action IDs to search for
     * @return list of {@link JpaActionStatus} entries associated with the provided action IDs
     */
    List<JpaActionStatus> findByActionIdIn(List<Long> actionIds);
}
