/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.action.dto.DeviceActionStatusTimestampResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutApprovalDecision;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryIndividualDeviceRequestBody;
import org.cosmos.models.mgmt.rollout.dto.RetryMultipleDevicesRequest;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * RolloutManagement to control rollouts e.g. like creating, starting, resuming
 * and pausing rollouts. This service secures all the functionality based on the
 * {@link PreAuthorize} annotation on methods.
 */
public interface RolloutManagement {

    /**
     * Counts all {@link Rollout}s in the repository that are not marked as deleted.
     *
     * @return number of rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long count();

    /**
     * Count rollouts by given text in name or description.
     *
     * @param searchText name or description
     * @return total count rollouts for specified filter text.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countByFilters(@NotEmpty String searchText);

    /**
     * Counts all {@link Rollout}s for a specific {@link DistributionSet} that
     * are stoppable
     *
     * @param setId the distribution set
     * @return the count
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    long countByDistributionSetIdAndRolloutIsStoppable(long setId);

    /**
     * Triggers all the groups of a rollout for processing even success threshold
     * isn't met yet. Current running groups will not change their status.
     *
     * @param rolloutId the rollout to be paused.
     * @throws EntityNotFoundException      if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_SYSTEM_ADMIN + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.HAS_AUTH_TENANT_UPDATE)
    void startAllGroups(long rolloutId);

    /**
     * Adds provided target filter query to the rollout
     *
     * @param rollout           target rollout
     * @param targetFilterQuery filter query to assign to the rollout
     * @return updated rollout
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    Rollout addTargetFilterQuery(final Rollout rollout, String targetFilterQuery);

    /**
     * Retrieves all rollouts.
     *
     * @param pageable the page request to sort and limit the result
     * @param deleted  flag if deleted rollouts should be included
     * @return a page of found rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findAll(@NotNull Pageable pageable, boolean deleted);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param pageable the page request to sort and limit the result
     * @param deleted  flag if deleted rollouts should be included
     * @return a list of rollouts with details of targets count for different
     * statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rollout> findAllWithDetailedStatus(@NotNull Pageable pageable, boolean deleted);

    /**
     * Retrieves all rollouts found by the given specification.
     *
     * @param pageable  the page request to sort and limit the result
     * @param rsqlParam the specification to filter rollouts
     * @param deleted   flag if deleted rollouts should be included
     * @return a page of found rollouts
     * @throws RSQLParameterUnsupportedFieldException if a field in the RSQL string is used but not provided by the
     *                                                given {@code fieldNameProvider}
     * @throws RSQLParameterSyntaxException           if the RSQL syntax is wrong
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Page<Rollout> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam, boolean deleted);

    /**
     * Finds rollouts by given text in name or description.
     *
     * @param pageable   the page request to sort and limit the result
     * @param searchText search text which matches name or description of rollout
     * @param deleted    flag if deleted rollouts should be included
     * @return the founded rollout or {@code null} if rollout with given ID does
     * not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Slice<Rollout> findByFiltersWithDetailedStatus(@NotNull Pageable pageable, @NotEmpty String searchText, boolean deleted);

    /**
     * Find rollouts which are still active and needs to be handled.
     *
     * @return a list of active rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Long> findActiveRollouts();

    /**
     * Find rollouts which are still active and ends now.
     *
     * @return a list of active rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findByActiveRolloutsEndAtNow();

    /**
     * Retrieves a specific rollout by its ID.
     *
     * @param rolloutId the ID of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given ID does
     * not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> get(long rolloutId);

    /**
     * Retrieves a specific rollout by its name.
     *
     * @param rolloutName the name of the rollout to retrieve
     * @return the founded rollout or {@code null} if rollout with given name
     * does not exists
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> getByName(@NotEmpty String rolloutName);

    /**
     * Get count of targets in different status in rollout.
     *
     * @param rolloutId rollout id
     * @return rollout details of targets count for different statuses
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Optional<Rollout> getWithDetailedStatus(long rolloutId);

    /**
     * Checks if rollout with given ID exists.
     *
     * @param rolloutId rollout id
     * @return <code>true</code> if rollout exists
     */
    boolean exists(long rolloutId);

    /**
     * Pauses a device action which is in running.
     *
     * @param rolloutId
     * @param controllerId
     * @throws EntityNotFoundException if rolloutId or controllerId with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void pauseDeviceAction(long rolloutId, String controllerId);

    /**
     * Handles the cancel request for canceling a deviceAction.
     *
     * @param rolloutId
     * @param controllerId
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void cancelDeviceAction(long rolloutId, String controllerId);

    /**
     * Pauses a rollout which is currently running. The Rollout switches
     * {@link RolloutStatus#PAUSED}. {@link RolloutGroup}s which are currently
     * running will be untouched. {@link RolloutGroup}s which are
     * {@link RolloutGroupStatus#SCHEDULED} will not be started and keep in
     * {@link RolloutGroupStatus#SCHEDULED} state until the rollout is
     * {@link RolloutManagement#resumeRollout(long)}}.
     * <p>
     * Switching the rollout status to {@link RolloutStatus#PAUSED} is
     * sufficient because the checkRunningRollouts will not check
     * this rollout anymore.
     *
     * @param rolloutId the rollout to be paused.
     * @throws EntityNotFoundException      if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}.
     *                                      Only running rollouts can be paused.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void pauseRollout(long rolloutId);

    /**
     * Cancels a rollout which is currently running. The Rollout switches to
     * {@link RolloutStatus#CANCELING}. This stopping rollout will be changed to {@link RolloutStatus#FINISHED}
     * when scheduler's execute stopping rollouts, where {@link RolloutGroup}s
     * which are {@link RolloutGroupStatus#RUNNING} will be canceled.
     * {@link RolloutGroup}s which are {@link RolloutGroupStatus#SCHEDULED} will be deleted.
     *
     * @param rolloutId the rollout to be canceled.
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}.
     *                                      Only running rollouts can be canceled/stopped.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE + SpringEvalExpressions.HAS_AUTH_OR +
            SpringEvalExpressions.IS_CONTROLLER)
    void cancelRollout(long rolloutId);

    /**
     * Resumes a paused rollout. The rollout switches back to
     * {@link RolloutStatus#RUNNING} state which is then picked up again by the checkRunningRollouts.
     *
     * @param rolloutId the rollout to be resumed
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#PAUSED}. Only
     *                                      paused rollouts can be resumed.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void resumeRollout(long rolloutId);

    /**
     * Approves or denies a created rollout being in state
     * {@link RolloutStatus#WAITING_FOR_APPROVAL}. If the rollout is approved,
     * it switches state to {@link RolloutStatus#DRAFT}, otherwise it switches
     * to state {@link RolloutStatus#APPROVAL_DENIED}
     *
     * @param rolloutId the rollout to be approved or denied.
     * @param decision  decision whether a rollout is approved or denied.
     * @return approved or denied rollout
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in
     *                                      {@link RolloutStatus#WAITING_FOR_APPROVAL}. Only rollouts
     *                                      waiting for approval can be acted upon.
     */
    @Deprecated
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_APPROVE)
    Rollout approveOrDeny(long rolloutId, MgmtRolloutApprovalDecision decision);

    /**
     * Approves or denies a created rollout being in state
     * {@link RolloutStatus#WAITING_FOR_APPROVAL}. If the rollout is approved,
     * it switches state to {@link RolloutStatus#DRAFT}, otherwise it switches
     * to state {@link RolloutStatus#APPROVAL_DENIED}
     *
     * @param rolloutId the rollout to be approved or denied.
     * @param decision  decision whether a rollout is approved or denied.
     * @param remark    user remark on approve / deny decision
     * @return approved or denied rollout
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in
     *                                      {@link RolloutStatus#WAITING_FOR_APPROVAL}. Only rollouts
     *                                      waiting for approveOrDeny can be acted upon.
     */
    @Deprecated
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_APPROVE)
    Rollout approveOrDeny(long rolloutId, MgmtRolloutApprovalDecision decision, String remark);

    /**
     * Freeze a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#DRAFT} state. The Rollout will be set into the
     * {@link RolloutStatus#READY} state.
     *
     * @param rolloutId the rollout to be ready
     * @return ready rollout
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#DRAFT}. Only
     *                                      ready rollouts can be moved to ready.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    Rollout freeze(long rolloutId);

    /**
     * Starts a rollout which has been created. The rollout must be in
     * {@link RolloutStatus#DRAFT} state. The Rollout will be set into the
     * {@link RolloutStatus#STARTING} state. The RolloutScheduler will ensure
     * all actions are created and the first group is started. The rollout
     * itself will be then also in {@link RolloutStatus#RUNNING}.
     *
     * @param rolloutId the rollout to be started
     * @return started rollout
     * @throws EntityNotFoundException      if rollout with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#DRAFT}. Only
     *                                      ready rollouts can be started.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    Rollout start(long rolloutId);

    /**
     * Update rollout details.
     *
     * @param update rollout to be updated
     * @return Rollout updated rollout
     * @throws EntityNotFoundException if rollout or DS with given IDs do not exist
     * @throws EntityReadOnlyException if rollout is in soft deleted state, i.e. only kept as
     *                                 reference
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    Rollout update(@NotNull @Valid RolloutUpdate update);

    /**
     * Deletes a rollout. A rollout might be deleted asynchronously by
     * indicating the rollout by {@link RolloutStatus#DELETING}
     *
     * @param rolloutId the ID of the rollout to be deleted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void delete(long rolloutId);

    /**
     * Cancels all rollouts that refer to the given {@link DistributionSet}.
     * This is called when a distribution set is invalidated and the cancel
     * rollouts option is activated.
     *
     * @param set the {@link DistributionSet} for that the rollouts should be
     *            canceled
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    void cancelRolloutsForDistributionSet(DistributionSet set);

    /**
     * Triggers next group of a rollout for processing even success threshold
     * isn't met yet. Current running groups will not change their status.
     *
     * @param rolloutId the rollout to be paused.
     * @throws EntityNotFoundException      if rollout or group with given ID does not exist
     * @throws RolloutIllegalStateException if given rollout is not in {@link RolloutStatus#RUNNING}.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    void triggerNextGroup(long rolloutId);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    boolean isTenantDistributionSoftwareDowngradeEnabled();

    /**
     * Finds the unfinished rollouts associated with the provided target
     *
     * @param targetId the targetId
     * @return list of rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findNotFinishedStatusRolloutByTargetId(Target targetId);


    /**
     * Finds all the rollouts associated with the provided target
     *
     * @param targetId the targetId
     * @return list of rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findAllRolloutByTargetId(Target targetId);

    /**
     * Finds the unfinished rollouts associated with the provided targets
     *
     * @param targets the targetId
     * @return list of rollouts
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findAllRolloutByTargetIds(List<Target> targets);


    /**
     * Ensures that the rollout associated with the given distribution set is in an inactive state.
     * If the distribution set is already part of an active rollout, this method will validate that
     * the rollout is in one of the following stages: CREATING, DRAFT, DELETING or DELETED.
     * <p>
     * If the rollout is not in one of these stages, an exception will be thrown to prevent the
     * unlinking process from proceeding.
     * <p>
     * This method is protected by the {@code HAS_AUTH_ROLLOUT_MANAGEMENT_READ} permission, which
     * ensures that only users with the appropriate authorization can execute it.
     *
     * @param distributionSetId the ID of the distribution set to validate
     * @throws ValidationException if the rollout is not in an inactive stage or is not in one of the allowed stages
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    void ensureRolloutIsInactiveOrThrow(long distributionSetId, String operationType);

    /**
     * Finds all software modules associated with a given distribution set.
     *
     * @param distributionSetId the ID of the distribution set for which to retrieve the software modules
     * @return a list of associations between software modules and the distribution set matching the provided ID
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findByDistributionSetId(Long distributionSetId);

    /**
     * Creates a new Rollout based on the provided {@link Rollout} object.
     *
     * <p>This method is secured and requires the user to have the necessary
     * authorization to manage rollouts, specifically the 'create' permission.</p>
     *
     * <p>The provided {@link Rollout} object is validated before proceeding, and must not be null.
     * Any validation errors will result in an appropriate exception.</p>
     *
     * @param rollout the {@link Rollout} object to be created, which must not be null and must pass validation
     * @return the created {@link Rollout} object after it has been persisted
     * @throws IllegalArgumentException                        if the {@link Rollout} object is null
     * @throws jakarta.validation.ConstraintViolationException if the provided {@link Rollout} object fails validation
     * @PreAuthorize ensures that only authorized users with the {@code HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE}
     * permission can invoke this method.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    Rollout create(@NotNull @Valid Rollout rollout);


    /**
     * Unfreezes a specified rollout, converts the rollout and group state from READY to DRAFT.
     *
     * <p>This method changes the status of the rollout to allow further actions.
     * It requires the user to have the appropriate authorization to manage rollouts.
     *
     * @param rolloutId the ID of the rollout to unfreeze, must not be null
     * @throws EntityNotFoundException      if the rollout with the given ID does not exist
     * @throws RolloutIllegalStateException if the rollout is not in a state that can be unfrozen
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void unfreeze(@NotNull @Valid Long rolloutId);

    /**
     * Expects a list of registered controller ids & grouping conditions, creates the rollout groups and assign the targets to it.
     *
     * @param rollout                 the rollout
     * @param registeredControllerIds list of controller ids that are registered with cosmos
     * @param groupsBody              json body with grouping condition/details
     * @return a list of {@link AssociatedTargetsToRolloutGroup}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_CREATE)
    List<AssociatedTargetsToRolloutGroup> addDeviceDetails(@NotNull @Valid Rollout rollout, @NotNull @Valid List<String> registeredControllerIds, String groupsBody);

    /**
     * Updates rollout groups for newly registered controller IDs.
     * <p>
     * This method modifies the existing rollout groups by incorporating newly registered controller IDs.
     * It ensures that the provided controller IDs are valid and updates the associated targets in the rollout groups.
     * </p>
     *
     * @param rollout                           The rollout entity to update. Must not be {@code null} and must be valid.
     * @param groupsBody                        JSON body containing grouping conditions/details. Can be {@code null}.
     * @param allAssignedTargetsToRolloutGroups A list of already assigned targets to rollout groups. Must not be {@code null}.
     * @return A list of {@link AssociatedTargetsToRolloutGroup} representing the updated rollout groups.
     * @throws ValidationException If any of the parameters are invalid or if the operation violates any constraints.
     */

    List<AssociatedTargetsToRolloutGroup> updateRolloutGroupsForNewlyRegisteredControllerIds(
            @NotNull @Valid Rollout rollout,
            String groupsBody,
            List<AssociatedTargetsToRolloutGroup> allAssignedTargetsToRolloutGroups);

    /**
     * Validates mandatory ESP (Enterprise Service Platform) configurations for registered controllers.
     * <p>
     * This method ensures that the provided rollout and list of registered controller IDs meet the
     * mandatory ESP requirements. If any validation fails, an appropriate exception is thrown.
     * </p>
     *
     * @param rollout                 The rollout entity to validate. Must not be {@code null}.
     * @param registeredControllerIds A list of registered controller IDs to validate. Must not be {@code null} or empty.
     * @throws ValidationException If the rollout or registered controller IDs are invalid or if the mandatory ESP requirements are not met.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    void validateMandatoryEspForRegisteredControllers(Rollout rollout, List<String> registeredControllerIds);

    /**
     * Removes device details associated with a given rollout.
     *
     * @param rollout                 The rollout entity for which device details will be removed. Must not be null and must be valid.
     * @param registeredControllerIds A list of registered controller IDs to be removed. Must not be null or empty.
     * @param deleteEsp               A flag indicating whether to delete ESP-related data. Must not be null.
     * @throws ValidationException If any of the parameters are invalid or if the operation violates any constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void deleteDeviceDetails(
            @NotNull @Valid Rollout rollout,
            @NotNull @Valid List<String> registeredControllerIds,
            @NotNull Boolean deleteEsp);

    /**
     * Find a Rollout by distributionSets.
     *
     * @param distributionSets the Ilist of distributionSets
     * @return the found entity, or null if not found
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<Rollout> findByDistributionSetIdAndStatus(List<DistributionSet> distributionSets, List<RolloutStatus> status);

    /**
     * Updates a specified rollout by associating it with a new distribution set.
     *
     * <p>This method modifies the relationship between the given rollout and the provided
     * distribution set. The user must have appropriate authorization to perform this operation.
     *
     * @param rollout         the rollout entity to update, must not be null
     * @param distributionSet the distribution set to associate with the rollout, must not be null
     * @return the updated rollout entity
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    Rollout updateRolloutDistributionSet(Rollout rollout, DistributionSet distributionSet);

    /**
     * Validates the association of software modules with a specific version for a given tenant and rollout.
     *
     * @param softwareModuleRequests
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void validateSoftwareModuleAssociation(List<MgmtSoftwareModuleRequest> softwareModuleRequests);

    /**
     * Associates software modules with a specific version for a given tenant and rollout.
     * <p>
     * This method associates one or more software modules with a specified version, as detailed in the provided
     * {@link MgmtSoftwareModuleRequest}. The operation is restricted to users with the appropriate
     * authority, enforced by the {@code @PreAuthorize} annotation. Users must have the {@code HAS_AUTH_UPDATE_REPOSITORY}
     * authority to invoke this method successfully.
     * <p>
     * The association is performed only if the input parameters are valid and the business rules are satisfied.
     * For example, the rollout must exist and be in the appropriate state for association.
     *
     * @param tenantId               the ID of the tenant to which the software modules will be associated.
     *                               This parameter must not be {@code null}.
     * @param rollout                the rollout entity to which the software modules will be associated.
     *                               This parameter must not be {@code null}.
     * @param softwareModuleRequests the model containing the software modules to be associated.
     *                               This parameter must not be {@code null}.
     * @throws IllegalArgumentException if any of the input parameters are invalid or if the association cannot
     *                                  be performed due to business rule violations.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void associateSoftwareModulesToVersion(Long tenantId, Rollout rollout, List<MgmtSoftwareModuleRequest> softwareModuleRequests);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void resumeDeviceAction(long rolloutID, String controllerId);

    /**
     * get list of controller ids associated with Rollout
     *
     * @param rolloutId to get the list of devices
     * @return list of controller ids
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    List<String> getControllerIdsByRolloutId(long rolloutId);


    /**
     * Updates the specified rollout with the provided rollout request.
     * This method is secured and requires the user to have the necessary
     * authorization to manage rollouts, specifically the 'update' permission.
     *
     * @param rollout        the existing rollout to be updated, must not be null
     * @param rolloutRequest the new rollout details to update, must not be null
     * @return the updated {@link Rollout} object after it has been persisted
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    Rollout updateRollout(Rollout rollout, Rollout rolloutRequest);

    /**
     * Deletes the specified rollout group and target group from the given rollout and rollout group.
     * <p>
     * This method checks if the group can be deleted based on certain conditions, and if valid,
     * performs the deletion asynchronously by marking the rollout status as {@link RolloutStatus#DELETING}.
     *
     * @param rollout      the rollout from which the group should be deleted
     * @param rolloutGroup the group to be deleted from the specified rollout
     * @throws IllegalArgumentException if the group cannot be deleted due to invalid state or other conditions.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_DELETE)
    void deleteRolloutGroupTargets(Rollout rollout, RolloutGroup rolloutGroup);

    /**
     * Calculates and updates the total number of targets for a given rollout.
     * <p>
     * This method retrieves all {@link RolloutGroup} entities associated with the rollout,
     * sums the number of targets for each group by querying the database,
     * and updates the total target count in the rollout entity.
     * </p>
     * <p>
     * The update is performed in a single transaction to ensure data consistency.
     * If any error occurs during the process, the transaction will be rolled back.
     * </p>
     *
     * @param rollout The {@link Rollout} entity whose total targets need to be updated.
     * @throws IllegalArgumentException if the provided rollout is null.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_REPOSITORY)
    void updateTotalTargetsForRollout(Rollout rollout);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_UPDATE)
    long handleEndRollouts();

    /**
     * Retrieves the action status timestamps for a specific rollout and controller within a tenant.
     * <p>
     * This endpoint allows clients to fetch the status history of a device action, identified by its rollout ID and controller ID,
     * for a given tenant. The response contains a list of status timestamp objects, each representing a status change event for the action.
     * </p>
     *
     * @param rolloutId    the unique identifier of the rollout; must not be {@code null}
     * @param controllerId the unique identifier of the controller (device); must not be {@code null} or empty
     * @return
     * @throws EntityNotFoundException if the specified action or controller does not exist for the tenant
     * @throws ValidationException     if the action does not belong to the specified controller or tenant
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    List<DeviceActionStatusTimestampResponse> fetchActionStatuses(long rolloutId, String controllerId);

    /**
     * Retrieves the target actions for a specific rollout and controller within a tenant.
     * <p>
     * This endpoint allows clients to fetch the actions associated with a device, identified by its rollout ID and controller ID,
     * for a given tenant. The response contains a list of target objects, each representing an action that can be performed on the device.
     * </p>
     *
     * @param rolloutId the unique identifier of the rollout; must not be {@code null}
     * @return a list of {@link MgmtTarget} objects representing the actions available for the specified rollout and controller
     * @throws EntityNotFoundException if the specified rollout or controller does not exist for the tenant
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_READ)
    Map<Target, Action> fetchRolloutTargetActions(long rolloutId);


    /**
     * Retries multiple devices in a rollout based on the provided request details.
     * <p>
     * This method handles the retry operation for multiple devices in a rollout. It validates
     * the retry mode and ensures that the operation is allowed for the tenant. If valid, it
     * triggers the retry operation for the specified devices.
     * </p>
     *
     * @param tenantId                    the ID of the tenant
     * @param rolloutId                   the ID of the rollout
     * @param retryMultipleDevicesRequest the request object containing retry details
     * @throws IllegalArgumentException if the retry mode is not enabled for the tenant
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void retryMultipleDevices(Long tenantId, Long rolloutId, RetryMultipleDevicesRequest retryMultipleDevicesRequest);

    /**
     * Retries individual devices in a rollout based on the provided request details.
     * <p>
     * This method handles the retry operation for individual device in a rollout. It validates
     * the retry mode and ensures that the operation is allowed for the tenant. If valid, it
     * triggers the retry operation for the specific device.
     * </p>
     *
     * @param tenantId                         the ID of the tenant
     * @param rolloutId                        the ID of the rollout
     * @param controllerId                     the controllerId of the target
     * @param retryIndividualDeviceRequestBody the request object containing retry details
     * @throws IllegalArgumentException if the retry mode is not enabled for the tenant
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void retryIndividualDevice(Long rolloutId, String controllerId,
                               MgmtRetryIndividualDeviceRequestBody retryIndividualDeviceRequestBody);


    /**
     * Retries the entire rollout for all associated devices based on the provided request details.
     * <p>
     * This method handles the retry operation for a full rollout. It validates the request and triggers
     * the retry process for all devices included in the rollout.
     * </p>
     *
     * @param rolloutId               the ID of the rollout to retry
     * @param retryFullRolloutRequest the request object containing details for the full rollout retry
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    void retryFullRollout(Long rolloutId, MgmtRetryFullRolloutRequestBody retryFullRolloutRequest);


    /**
     * Clones an existing rollout based on the provided request details.
     * <p>
     * This method creates a new rollout by cloning the configuration of an existing rollout.
     * It validates the request and sets up the new rollout according to the specified parameters.
     * </p>
     *
     * @param RolloutId      the ID of the rollout to be cloned
     * @param rolloutRequest the request object containing details for the cloned rollout
     * @return the newly created cloned {@link Rollout} object
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_ROLLOUT_MANAGEMENT_HANDLE)
    Rollout cloneRollout(Long RolloutId, MgmtCloneRolloutRequestBody rolloutRequest);


}