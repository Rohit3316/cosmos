/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.google.common.collect.Lists;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.assertj.core.api.ThrowableAssert;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutUpdateRequest;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.builder.RolloutGroupCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

import jakarta.validation.ValidationException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Junit tests for RolloutManagement.
 */
@Feature("Component Tests - Repository")
@Story("Rollout Management")
class RolloutManagementTest extends AbstractManagementApiIntegrationTest {
    public static final String STATE = " state";
    public static final String TARGET_FILTER_VIN_QUERY = "controllerId==";
    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String CONTROLLER_ID = "controllerId";
    private static final String NAME = "name";
    private static final Integer WEIGHT = 12;
    private static final String DESCRIPTION = "description";
    public static final String ROLLOUT = "rollout";
    protected static final String CONTROLLER_ID1 = "controller1";
    protected static final String CONTROLLER_ID2 = "controller2";
    protected static final String CONTROLLER_ID3 = "controller3";
    protected static final String CONTROLLER_ID4 = "controller4";
    protected static final String CONTROLLER_ID5 = "controller5";
    protected static final String CONTROLLER_ID6 = "controller6";
    protected static final String CONTROLLER_ID7 = "controller7";
    protected static final String CONTROLLER_ID8 = "controller8";
    protected static final String CONTROLLER_ID9 = "controller9";
    protected static final String CONTROLLER_ID10 = "controller10";

    protected static final String VIN = "19UYA31581L000129";

    private static ClientAndServer mockServer;
    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private RolloutTestApprovalStrategy approvalStrategy;
    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;
    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);


    private static Stream<Arguments> simpleRolloutsPossibilities() {
        return Stream.of(
                Arguments.of(true, false, DeviceActionStatus.RUNNING), //
                Arguments.of(false, true, DeviceActionStatus.RUNNING), //
                Arguments.of(false, false, DeviceActionStatus.RUNNING));//
    }

    private static Map<TotalTargetCountStatus.Status, Long> createInitStatusMap() {
        final Map<TotalTargetCountStatus.Status, Long> map = new HashMap<>();
        for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
            map.put(status, 0L);
        }
        return map;
    }

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    private static void mockPublishVehicleStatus() {
        mockServer
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
    }

    @BeforeEach
    void reset() throws Exception {
        this.approvalStrategy.setApprovalNeeded(false);
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_filter_query", "sp_target_tag", "sp_target", "sp_vehicle_ecu",
                "sp_vehicle_model", "sp_action", "sp_rolloutgroup", "sp_rollout", "sp_distribution_set", "sp_artifact_software_module",
                "sp_software_versions", "sp_base_software_module");
    }

    @Test
    @Description("Verifies that a running action with distribution-set (A) is not canceled by a rollout which tries to also assign a distribution-set (A)")
    void givenRunningActionWithSameDistributionSetWhenTryingToCancelThenRolloutIsNotCancelled() {
        // manually assign distribution set to target
        final String knownControllerId = CONTROLLER_ID + testdataFactory.getRandomInt();
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        Target target = testdataFactory.createTarget(knownControllerId);
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(),
                knownControllerId);
        final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

        // create rollout with the same distribution set already assigned
        // start rollout
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, knownDistributionSet, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();


        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // verify that manually created action is still running and action
        // created from rollout is finished
        final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                .getContent();
        // should be 2 actions, one manually and one from the rollout
        assertThat(actionsByKnownTarget).hasSize(2);
        // verify that manually assigned action is still running
        assertThat(deploymentManagement.findAction(manuallyAssignedActionId).get().getStatus())
                .isEqualTo(DeviceActionStatus.RUNNING);
        // verify that rollout management created action is finished because is
        // duplicate assignment
        final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
        assertThat(rolloutCreatedAction.getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
    }

    @ParameterizedTest
    @MethodSource("simpleRolloutsPossibilities")
    @Description("Verifies that action states are correctly initialized after starting a rollout with different options in regard to the confirmation.")
    void runRolloutWithConfirmationFlagAndCoonfirmationFlowOptions(final boolean confirmationFlowActive,
                                                                   final boolean confirmationRequired, final DeviceActionStatus expectedStatus) {
        // manually assign distribution set to target
        final String knownControllerId = CONTROLLER_ID + testdataFactory.getRandomInt();
        Target target1 = testdataFactory.createTarget(knownControllerId);

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // create rollout with the same distribution set already assigned
        // start rollout
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), List.of(target1), false);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // verify that manually created action is still running and action
        // created from rollout is finished
        final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                .getContent();
        assertThat(actionsByKnownTarget).hasSize(1);
        assertThat(actionsByKnownTarget.get(0).getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
    }

    @Test
    @Description("Verifies that a running action is auto canceled by a rollout which assigns another distribution-set.")
    void givenNewDistributionSetWhenAssignedThenAutoClosesActiveActions() {
        tenantConfigurationManagement
                .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            // manually assign distribution set to target
            final String knownControllerId = CONTROLLER_ID + testdataFactory.getRandomInt();
            final DistributionSet firstDistributionSet = testdataFactory.createDistributionSet();
            Target target = testdataFactory.createTarget(knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(firstDistributionSet.getId(),
                    knownControllerId);
            final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

            // create rollout with the same distribution set already assigned
            // start rollout
            final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), List.of(target), false);
            rolloutManagement.freeze(rollout.getId());
            rolloutHandler.handleAll();

            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();

            // verify that manually created action is canceled and action
            // created from rollout is running
            final List<Action> actionsByKnownTarget = deploymentManagement.findActionsByTarget(knownControllerId, PAGE)
                    .getContent();
            // should be 2 actions, one manually and one from the rollout
            assertThat(actionsByKnownTarget).hasSize(2);
            // verify that manually assigned action is still running
            assertThat(deploymentManagement.findAction(manuallyAssignedActionId).get().getStatus())
                    .isEqualTo(DeviceActionStatus.RUNNING);
            // verify that rollout management created action is running
            final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                    .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
            assertThat(rolloutCreatedAction.getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
        } finally {
            tenantConfigurationManagement
                    .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }

    }

    @Test
    @Description("Verifies that management get access reacts as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class)})
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(rolloutManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(rolloutManagement.getByName(NOT_EXIST_ID)).isNotPresent();
        assertThat(rolloutManagement.getWithDetailedStatus(NOT_EXIST_IDL)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = RolloutDeletedEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 0),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = RolloutCreatedEvent.class, count = 1), @Expect(type = RolloutUpdatedEvent.class, count = 8),
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 4),
            @Expect(type = ActionCreatedEvent.class, count = 1)})
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID), true);
        startRollout(rollout);

        verifyThrownExceptionBy(() -> rolloutManagement.delete(NOT_EXIST_IDL), Rollout.class.getSimpleName());

        verifyThrownExceptionBy(() -> rolloutManagement.pauseRollout(NOT_EXIST_IDL), Rollout.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutManagement.resumeRollout(NOT_EXIST_IDL), Rollout.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutManagement.start(NOT_EXIST_IDL), Rollout.class.getSimpleName());

        verifyThrownExceptionBy(() -> rolloutManagement.update(entityFactory.rollout().update(NOT_EXIST_IDL)),
                Rollout.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutManagement.triggerNextGroup(NOT_EXIST_IDL), Rollout.class.getSimpleName());
    }

    protected static void verifyThrownExceptionBy(final ThrowableAssert.ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    @Test
    @Description("Verifying that the rollout is created correctly, executing the filter and split up the targets in the correct group size.")
    void creatingRolloutIsCorrectPersisted() {
        final int amountGroups = 1;
        final String knownControllerId = CONTROLLER_ID + testdataFactory.getRandomInt();
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        Target target = testdataFactory.createTarget(knownControllerId, knownControllerId, knownControllerId, testdataFactory.createVehicle("X250999").getId(), VIN);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, knownDistributionSet, List.of(target), false);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // verify the split of the target and targetGroup
        final Page<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId());
        // we have total of #amountTargetsForRollout in rollouts split in
        // group size #groupSize
        assertThat(rolloutGroups).hasSize(amountGroups);
    }

    @Test
    @Description("Verifying that when the rollout is started the actions for all targets in the rollout is created and the state of the first group is running as well as the corresponding actions")
    void givenRolloutStart_whenFirstGroupSet_thenFirstGroupAndActionsInRunningStateOthersScheduled() {
        final int amountTargetsForRollout = 1;
        final int amountGroups = 1;
        mockServer.reset();
        final String knownControllerId = CONTROLLER_ID + testdataFactory.getRandomInt();
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        Target target = testdataFactory.createTarget(knownControllerId, knownControllerId, knownControllerId, testdataFactory.createVehicle("X250999").getId(), VIN);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, knownDistributionSet, List.of(target), false);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        // verify first group is running
        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 1, Sort.by(Direction.ASC, "id")), rollout.getId())
                .getContent().get(0);
        assertThat(firstGroup.getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        // verify other groups are scheduled
        final List<RolloutGroup> scheduledGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(1, 100, Sort.by(Direction.ASC, "id")), rollout.getId())
                .getContent();
        scheduledGroups.forEach(group -> assertThat(group.getStatus())
                .as("group which should be in scheduled state is in " + group.getStatus() + STATE)
                .isEqualTo(RolloutGroupStatus.QUEUED));
        // verify that the first group actions has been started and are in state
        // running
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        assertThat(runningActions).hasSize(amountTargetsForRollout / amountGroups)
                .as("Created actions are initiated by rollout creator")
                .allMatch(a -> a.getInitiatedBy().equals(rollout.getCreatedBy()));
        // the rest targets are only scheduled
        assertThat(findActionsByRolloutAndStatus(rollout, DeviceActionStatus.USER_SCHEDULED))
                .hasSize(amountTargetsForRollout - (amountTargetsForRollout / amountGroups));

    }

    @Test
    @Description("Verifying that a finish condition of a group is hit the next group of the rollout is also started")
    void checkRunningRolloutsDoesNotStartNextGroupIfFinishConditionIsNotHit() {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID), false);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        // finish one action should be sufficient due the finish condition is at
        // 50%
        final JpaAction action = (JpaAction) runningActions.get(0);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.FINISHED_SUCCESS), null);

        // verify that now the first and the second group are in running state
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 2, Sort.by(Direction.ASC, "id")), rollout.getId())
                .getContent();
        runningRolloutGroups.forEach(group -> assertThat(group.getStatus())
                .as("group should be in running state because it should be started but it is in " + group.getStatus()
                        + STATE)
                .isEqualTo(RolloutGroupStatus.RUNNING));

        // verify that the other groups are still in schedule state
        final List<RolloutGroup> scheduledRolloutGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(2, 10, Sort.by(Direction.ASC, "id")), rollout.getId())
                .getContent();
        scheduledRolloutGroups.forEach(group -> assertThat(group.getStatus())
                .as("group should be in scheduled state because it should not be started but it is in "
                        + group.getStatus() + STATE)
                .isEqualTo(RolloutGroupStatus.QUEUED));
    }

    @Test
    @Description("Verifying that next group is started when targets of the group have been deleted.")
    void givenRunningRollouts_whenTargetsDeleted_thenNextGroupStarts() {

        final Rollout createdRollout = createAndStartRollout(ROLLOUT, CONTROLLER_ID, false);

        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatusAndActive(PAGE,
                createdRollout.getId(), DeviceActionStatus.RUNNING, true);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        finishAction(runningActions.get(0));
        targetManagement.delete(Collections.singleton(runningActions.get(0).getTarget().getId()));
        rolloutHandler.handleAll();
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(PAGE, createdRollout.getId()).getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHING);

    }

    @Step("Finish three actions of the rollout group and delete two targets")
    private void finishActionAndDeleteTargetsOfFirstRunningGroup(final Rollout createdRollout) {
        // finish group one by finishing targets and deleting targets
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatusAndActive(PAGE,
                createdRollout.getId(), DeviceActionStatus.RUNNING, true);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        finishAction(runningActions.get(0));
        finishAction(runningActions.get(1));
        finishAction(runningActions.get(2));
        targetManagement.delete(
                Arrays.asList(runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));
    }

    @Step("Check the status of the rollout groups, second group should be in running status")
    private void checkSecondGroupStatusIsRunning(final Rollout createdRollout) {
        rolloutHandler.handleAll();
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")), createdRollout.getId())
                .getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.QUEUED);
    }

    @Step("Finish one action of the rollout group and delete four targets")
    private void finishActionAndDeleteTargetsOfSecondRunningGroup(final Rollout createdRollout) {
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatusAndActive(PAGE,
                createdRollout.getId(), DeviceActionStatus.RUNNING, true);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        finishAction(runningActions.get(0));
        targetManagement.delete(
                Arrays.asList(runningActions.get(1).getTarget().getId(), runningActions.get(2).getTarget().getId(),
                        runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));

    }

    @Step("Delete all targets of the rollout group")
    private void deleteAllTargetsFromThirdGroup(final Rollout createdRollout) {
        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatusAndActive(PAGE,
                createdRollout.getId(), DeviceActionStatus.USER_SCHEDULED, true);
        final List<JpaAction> runningActions = runningActionsSlice.getContent();
        targetManagement.delete(Arrays.asList(runningActions.get(0).getTarget().getId(),
                runningActions.get(1).getTarget().getId(), runningActions.get(2).getTarget().getId(),
                runningActions.get(3).getTarget().getId(), runningActions.get(4).getTarget().getId()));
    }

    @Step("Check the status of the rollout groups and the rollout")
    private void verifyRolloutAndAllGroupsAreFinished(final Rollout createdRollout) {
        rolloutHandler.handleAll();
        final List<RolloutGroup> runningRolloutGroups = rolloutGroupManagement
                .findByRollout(PAGE, createdRollout.getId()).getContent();
        assertThat(runningRolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(runningRolloutGroups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.FINISHED);
        assertThat(reloadRollout(createdRollout).getStatus()).isEqualTo(RolloutStatus.FINISHED);

    }

    private void finishAction(final Action action) {
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.FINISHED_SUCCESS), null);
    }

    @Test
    @Description("Verifies that when an action fails, the rollout group also fails, causing the rollout to enter the 'finished' status")
    void givenErrorInAction_whenOccurs_thenGroupFailsAndRolloutEnds() {

        final Rollout createdRollout = createAndStartRollout(ROLLOUT, CONTROLLER_ID, false);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = new ArrayList<>();
        runningActions.addAll(findActionsByRolloutAndStatus(createdRollout, DeviceActionStatus.RUNNING));
        runningActions.addAll(actionRepository.findByRolloutIdAndStatusAndActive(PAGE, createdRollout.getId(), DeviceActionStatus.RUNNING, false).getContent());

        // finish actions with error
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.ERROR_RESPONSE_CODE), null);
        }
        rolloutHandler.handleAll();
        final Rollout rollout = reloadRollout(createdRollout);
        // the rollout itself should be in finished based on the error action and on the error of rollout group
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.FINISHING);

        // the first rollout group should be in error state
        final List<RolloutGroup> errorGroup = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 1, Sort.by(Direction.ASC, "id")), createdRollout.getId())
                .getContent();
        assertThat(errorGroup).hasSize(1);
        assertThat(errorGroup.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHING);

        // all other groups should still be in scheduled state
        final List<RolloutGroup> scheduleGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(1, 100, Sort.by(Direction.ASC, "id")), createdRollout.getId())
                .getContent();
        scheduleGroups.forEach(group -> assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.QUEUED));
    }


    @Test
    @Description("Verify that a paused rollout due to an error action resumes if there are queued or running groups, otherwise moves to finished.")
    void givenErrorAction_whenPausesRollout_andNoQueuedOrRunningGroups_thenFinishes() {

        final Rollout createdRollout = createAndStartRollout(ROLLOUT, CONTROLLER_ID, false);

        // set both actions in error state so error condition is hit and error
        // action is executed
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, DeviceActionStatus.RUNNING);
        // finish actions with error
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.ERROR_RESPONSE_CODE), null);
        }

        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId()).getContent();
        for (RolloutGroup group : rolloutGroups) {
            JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) group;
            jpaRolloutGroup.setStatus(RolloutGroupStatus.PAUSED);
            rolloutGroupRepository.save(jpaRolloutGroup);
        }
        rolloutHandler.handleAll();

        final Rollout rollout = reloadRollout(createdRollout);

        // the rollout itself should be in finished status
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.FINISHING);
    }


    @Test
    @Description("Verify that the targets have the right status during the rollout.")
    void givenRollout_whenCountingTargetStatuses_thenCorrectCountsReturned() {

        final int amountTargets = 8;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");

        Rollout createdRollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), targets, false);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        // 8 targets are not started
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        rolloutHandler.handleAll();

        // 8 targets are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 0L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, DeviceActionStatus.FINISHED_SUCCESS);
        rolloutHandler.handleAll();

        // 0 targets are ready, 8 are finished and 0 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    @Test
    @Description("Verify that the targets have the right status during a download_only rollout.")
    void givenDownloadOnlyRollout_whenCountingTargetStatuses_thenCorrectCountsReturned() {

        final int amountTargets = 8;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");

        Rollout createdRollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), targets, false);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        // 8 targets are not started
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        rolloutHandler.handleAll();

        // 8 targets are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 0L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, DeviceActionStatus.FINISHED_SUCCESS);
        rolloutHandler.handleAll();

        // 0 targets are ready, 8 are finished and 0 are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.FINISHED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when an error emerges.")
    void givenRolloutWithError_whenCountingTargetStatuses_thenCorrectCountsReturned() {

        final int amountTargets = 8;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");

        Rollout createdRollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), targets, false);

        // targets have not started
        Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        // 8 targets are not started
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        rolloutManagement.start(createdRollout.getId());

        rolloutHandler.handleAll();

        // 8 targets are running
        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.SCHEDULED, 0L);
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

        changeStatusForAllRunningActions(createdRollout, DeviceActionStatus.ERROR_RESPONSE_CODE);
        rolloutHandler.handleAll();

        validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.ERROR, 8L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    @Test
    @Description("Verify that the targets have the right status during the rollout when receiving the status of rollout groups.")
    void givenRollout_whenCountingTargetGroupStatuses_thenCorrectCountsReturned() {

        final int amountTargets = 8;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 55, 40, 55) + "]";


        createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);
        Rollout createdRollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        createdRollout = reloadRollout(createdRollout);
        List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId())
                .getContent();

        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, (long) rolloutGroups.get(0).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(0), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, (long) rolloutGroups.get(1).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(1), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, (long) rolloutGroups.get(2).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(2), expectedTargetCountStatus);

        rolloutManagement.start(createdRollout.getId());
        rolloutHandler.handleAll();

        createdRollout = reloadRollout(createdRollout);
        rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId())
                .getContent();

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, (long) rolloutGroups.get(0).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(0), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, (long) rolloutGroups.get(1).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(1), expectedTargetCountStatus);

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, (long) rolloutGroups.get(2).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(2), expectedTargetCountStatus);

        changeStatusForAllRunningActions(createdRollout, DeviceActionStatus.FINISHED_SUCCESS);
        rolloutHandler.handleAll();

        createdRollout = reloadRollout(createdRollout);
        rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId())
                .getContent();

        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, (long) rolloutGroups.get(0).getTotalTargets());
        validateRolloutGroupActionStatus(rolloutGroups.get(0), expectedTargetCountStatus);

    }

    @Test
    @Description("Verify that target actions of rollout get canceled when a manuel distribution sets assignment is done.")
    void givenRollout_whenManualDsAssignment_thenTargetsGetAssigned() {

        final int amountTargets = 8;
        final List<Target> createTargets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 55, 40, 55) + "]";


        createRolloutAndGroups(ROLLOUT, knownDistributionSet, createTargets, groupsDetailsJson, false);
        Rollout createdRollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        startRollout(createdRollout);

        // 2 targets are running
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, DeviceActionStatus.RUNNING);
        assertThat(runningActions.size()).isEqualTo(2);

        // 2 targets are in the group and the DS has been assigned
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId())
                .getContent();
        final Page<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE,
                rolloutGroups.get(0).getId());
        final List<Target> targetList = targets.getContent();

        final List<Target> targetToCancel = new ArrayList<>();
        targetToCancel.add(targetList.get(0));
        targetToCancel.add(targetList.get(1));

        final DistributionSet dsForCancelTest = testdataFactory.createDistributionSet("dsForTest");
        assignDistributionSet(dsForCancelTest, targetToCancel);

        final Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 2L);
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 6L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);
    }

    @Test
    @Description("Verify that target actions of a rollout get cancelled when another rollout with same targets gets started.")
    void givenOtherRollout_whenAssignsDistributionSet_thenTargetsGetAssigned() {

        final int amountTargets = 8;
        final List<Target> createTargetsOne = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet knownDistributionSetOne = testdataFactory.createDistributionSet();

        String groupsDetailsJsonOne = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 55, 40, 55) + "]";

        createRolloutAndGroups(ROLLOUT_1, knownDistributionSetOne, createTargetsOne, groupsDetailsJsonOne, false);
        Rollout rolloutOne = rolloutRepository.findByDistributionSetId(knownDistributionSetOne.getId()).get(0);

        final DistributionSet knownDistributionSetTwo = testdataFactory.createDistributionSet();

        String groupsDetailsJsonTwo = "[" +
                createBasicGroupJson("test-group03", 25, 20, 50) + "," +
                createBasicGroupJson("test-group04", 55, 40, 55) + "]";

        createRolloutAndGroups(ROLLOUT_2, knownDistributionSetTwo, createTargetsOne, groupsDetailsJsonTwo, false);
        Rollout rolloutTwo = rolloutRepository.findByDistributionSetId(knownDistributionSetTwo.getId()).get(0);

        changeStatusForAllRunningActions(rolloutOne, DeviceActionStatus.FINISHED_SUCCESS);
        rolloutHandler.handleAll();
        // Verify that 8 targets are not started
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);

        rolloutManagement.freeze(rolloutTwo.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rolloutTwo.getId());
        rolloutHandler.handleAll();

        // Verify that 8 targets are not started
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, 8L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify that error status of DistributionSet installation during rollout can get rerun with second rollout so that all targets have some DistributionSet installed at the end.")
    void givenFirstRolloutEndsWithErrors_whenStartingSecondRollout_thenItStartsSuccessfully() {

        final int amountTargets = 8;
        final List<Target> createTargetsOne = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet distributionSetOne = testdataFactory.createDistributionSet();

        String groupsDetailsJsonOne = "[" +
                createBasicGroupJson("test-group01", 100, 1, 100) + "]";


        createRolloutAndGroups(ROLLOUT_1, distributionSetOne, createTargetsOne, groupsDetailsJsonOne, false);
        Rollout rolloutOne = rolloutRepository.findByDistributionSetId(distributionSetOne.getId()).get(0);

        startRollout(rolloutOne);

        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.ERROR_RESPONSE_CODE, 4);
        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.FINISHED_SUCCESS, 4);
        rolloutHandler.handleAll();

        // 9 targets are finished and 6 have error
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 4L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 4L);
        validateRolloutActionStatus(rolloutOne.getId(), expectedTargetCountStatus);
        rolloutOne = reloadRollout(rolloutOne);
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.FINISHING);

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rolloutOne.getId()).get(0);
        rolloutGroup.setStatus(RolloutGroupStatus.FINISHING);
        rolloutGroupRepository.save(rolloutGroup);

        rolloutHandler.handleAll();
        // rollout is finished
        rolloutOne = reloadRollout(rolloutOne);
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.FINISHED);


        final List<Target> createTargetsTwo = testdataFactory.createTargets(amountTargets, "trf", "trf");
        final DistributionSet distributionSetTwo = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 100, 1, 100) + "]";


        createRolloutAndGroups(ROLLOUT, distributionSetTwo, createTargetsTwo, groupsDetailsJson, false);
        Rollout rolloutTwo = rolloutRepository.findByDistributionSetId(distributionSetTwo.getId()).get(0);

        startRollout(rolloutTwo);

        // 8 error targets are now running
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 8L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 0L);
        validateRolloutActionStatus(rolloutTwo.getId(), expectedTargetCountStatus);
        changeStatusForAllRunningActions(rolloutTwo, DeviceActionStatus.FINISHED_SUCCESS);
        final Page<Target> targetPage = targetManagement.findByUpdateStatus(PAGE, TargetUpdateStatus.IN_SYNC);
        final List<Target> targetList = targetPage.getContent();
        assertThat(targetList.size()).isEqualTo(0);
        targetList.stream().map(Target::getControllerId).map(deploymentManagement::getAssignedDistributionSet)
                .forEach(d -> assertThat(d).contains(distributionSetOne));
    }

    @Test
    @Description("Verify that the rollout moves to the next group when the success condition was achieved and the error condition was not exceeded.")
    void givenSuccessConditionMetAndErrorConditionNotExceeded_whenCheckingRolloutStatus_thenRolloutProceeds() {

        final int amountTargets = 10;
        final List<Target> createTargetsOne = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet distributionSetOne = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 50, 1, 100) + "," +
                createBasicGroupJson("test-group02", 50, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT_1, distributionSetOne, createTargetsOne, groupsDetailsJson, false);
        Rollout rolloutOne = rolloutRepository.findByDistributionSetId(distributionSetOne.getId()).get(0);

        startRollout(rolloutOne);

        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.ERROR_RESPONSE_CODE, 2);
        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.FINISHED_SUCCESS, 3);
        rolloutHandler.handleAll();
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rolloutOne.getId())
                .getContent();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.NOTSTARTED, 3L);
        validateRolloutGroupActionStatus(rolloutGroups.get(1), expectedTargetCountStatus);

    }

    @Test
    @Description("Verify that the rollout does not move to the next group when the success condition was not achieved.")
    void givenSuccessConditionNotMet_whenCheckingRolloutStatus_thenRolloutDoesNotProceed() {

        final int amountTargets = 20;
        final List<Target> createTargetsOne = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet distributionSetOne = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 100, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT_1, distributionSetOne, createTargetsOne, groupsDetailsJson, false);
        Rollout rolloutOne = rolloutRepository.findByDistributionSetId(distributionSetOne.getId()).get(0);

        rolloutManagement.freeze(rolloutOne.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rolloutOne.getId());
        rolloutHandler.handleAll();


        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.RUNNING, 12);
        rolloutHandler.handleAll();
        final List<RolloutGroup> rolloutGruops = rolloutGroupManagement.findByRollout(PAGE, rolloutOne.getId())
                .getContent();
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 20L);
        validateRolloutGroupActionStatus(rolloutGruops.get(0), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify that the rollout pauses when the error condition was exceeded.")
    void errorConditionExceeded() {

        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3, CONTROLLER_ID4, CONTROLLER_ID5);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 50, 20, 80) + "," +
                createBasicGroupJson("test-group02", 50, 40, 60) + "]";

        Rollout rolloutOne = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, false
        );

        startRollout(rolloutOne);
        changeStatusForRunningActions(rolloutOne, DeviceActionStatus.ERROR_RESPONSE_CODE, 2);
        rolloutHandler.handleAll();
        rolloutOne = reloadRollout(rolloutOne);
        assertThat(rolloutOne.getStatus()).isEqualTo(RolloutStatus.PAUSING);
    }

    @Test
    @Description("Verify that all rollouts are return with expected target statuses.")
    void givenRollouts_whenFetchingWithDetailedStatus_thenReturnCorrectDetails() {

        Rollout rolloutA = createAndStartRollout("rollout1", "controllerId1", false);
        Rollout rolloutB = createAndStartRollout("rollout2", "controllerId2", false);

        changeStatusForAllRunningActions(rolloutB, DeviceActionStatus.FINISHED_SUCCESS);
        rolloutHandler.handleAll();

        Rollout rolloutC = createAndStartRollout("rollout3", "controllerId3", false);

        changeStatusForAllRunningActions(rolloutC, DeviceActionStatus.ERROR_RESPONSE_CODE);
        rolloutHandler.handleAll();

        final Slice<Rollout> rolloutPage = rolloutManagement
                .findAllWithDetailedStatus(new OffsetBasedPageRequest(0, 100, Sort.by(Direction.ASC, "name")), false);
        final List<Rollout> rolloutList = rolloutPage.getContent();

        // validate rolloutA -> 6 running and 6 ready
        Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 1L);
        validateRolloutActionStatus(rolloutList.get(0).getId(), expectedTargetCountStatus);

        // validate rolloutB -> 5 running and 5 finished
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.FINISHED, 1L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 0L);
        validateRolloutActionStatus(rolloutList.get(1).getId(), expectedTargetCountStatus);

        // validate rolloutC -> 5 running and 5 error
        expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.ERROR, 1L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 0L);
        validateRolloutActionStatus(rolloutList.get(2).getId(), expectedTargetCountStatus);
    }

    @Test
    @Description("Verify the count of existing rollouts.")
    void givenRollouts_whenCounting_thenReturnCorrectCount() {

        for (int i = 1; i <= 10; i++) {
            String rolloutName = NAME + testdataFactory.getRandomInt();
            testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));
        }
        final Long count = rolloutManagement.count();
        assertThat(count).isEqualTo(10L);
    }


    @Test
    @Description("Verify that the filtering and sorting ascending for rollout is working correctly.")
    void findRolloutByFilters() {

        for (int i = 1; i <= 5; i++) {
            testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(NAME + i)));
        }
        for (int i = 1; i <= 8; i++) {
            final String randomRollout = String.valueOf(testdataFactory.getRandomInt());
            testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(randomRollout)));

        }

        final Slice<Rollout> rollout = rolloutManagement.findByFiltersWithDetailedStatus(
                new OffsetBasedPageRequest(0, 100, Sort.by(Direction.ASC, "name")), NAME + "%", false);
        final List<Rollout> rolloutList = rollout.getContent();
        assertThat(rolloutList.size()).isEqualTo(5);
        int i = 1;
        for (final Rollout r : rolloutList) {
            assertThat(r.getName()).isEqualTo(NAME + i);
            i++;
        }
    }

    @Test
    @Description("Verify that the expected rollout is found by name.")
    void findRolloutByName() {

        final String rolloutName = "Rollout137";
        Rollout rolloutCreated = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        final Rollout rolloutFound = rolloutManagement.getByName(rolloutName).get();
        assertThat(rolloutCreated).isEqualTo(rolloutFound);

    }

    @Test
    @Description("Verify that the percent count is acting like aspected when targets move to the status finished or error.")
    void givenRunningGroup_whenCalculatingFinishedPercent_thenReturnCorrectValue() {

        final int amountOtherTargets = 5;
        final String rolloutName = "MyRollout";
        List<Target> targets = testdataFactory.createTargets(amountOtherTargets, rolloutName + "-", rolloutName);

        Rollout myRollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, false);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);

        rolloutHandler.handleAll();

        final String rsqlParam = TARGET_FILTER_VIN_QUERY + "*MyRoll*";
        rolloutHandler.handleAll();
        rolloutManagement.freeze(myRollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(myRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        changeStatusForRunningActions(myRollout, DeviceActionStatus.FINISHED_SUCCESS, 2);
        rolloutHandler.handleAll();
        myRollout = reloadRollout(myRollout);

        final Optional<JpaRolloutGroup> rolloutGroup = rolloutGroupRepository.findById(
                rolloutGroupManagement.findByRollout(PAGE, myRollout.getId()).getContent().get(0).getId()
        );

        float percent;
        percent = validateRolloutStatusCount(rolloutGroup.get());
        assertThat(percent).isEqualTo(40);

        changeStatusForRunningActions(myRollout, DeviceActionStatus.FINISHED_SUCCESS, 3);
        rolloutHandler.handleAll();

        percent = validateRolloutStatusCount(rolloutGroup.get());
        assertThat(percent).isEqualTo(100);

        changeStatusForRunningActions(myRollout, DeviceActionStatus.FINISHED_SUCCESS, 0);
        changeStatusForAllRunningActions(myRollout, DeviceActionStatus.ERROR_RESPONSE_CODE);
        rolloutHandler.handleAll();

        percent = validateRolloutStatusCount(rolloutGroup.get());
        assertThat(percent).isEqualTo(100);
    }

    private float validateRolloutStatusCount(JpaRolloutGroup jpaRolloutGroup) {
        List<TotalTargetCountActionStatus> rolloutStatusCountItems;
        TotalTargetCountStatus totalTargetCountStatus;
        float percent;
        rolloutStatusCountItems = actionRepository.getStatusCountByRolloutGroupId(jpaRolloutGroup.getId(), true);
        rolloutStatusCountItems.addAll(actionRepository.getStatusCountByRolloutGroupId(jpaRolloutGroup.getId(), false));
        totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems,
                (long) jpaRolloutGroup.getTotalTargets(), jpaRolloutGroup.getRollout().getUserAcceptanceRequired());
        jpaRolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        percent = jpaRolloutGroup.getTotalTargetCountStatus().getFinishedPercent();
        return percent;
    }

    @Test
    @Description("Verify that the expected targets are returned for the rollout groups.")
    void givenRsqlParam_whenFindingRolloutGroupTargets_thenReturnMatchingResults() {

        final int amountOtherTargets = 5;
        final String rolloutName = "MyRollout";
        List<Target> targets = testdataFactory.createTargets(amountOtherTargets, rolloutName + "-", rolloutName);

        Rollout myRollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, false);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);

        rolloutHandler.handleAll();

        final String rsqlParam = TARGET_FILTER_VIN_QUERY + "*MyRoll*";
        rolloutHandler.handleAll();
        rolloutManagement.freeze(myRollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(myRollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        final Condition<String> targetBelongsInRollout = new Condition<>(s -> s.startsWith(rolloutName),
                "Target belongs into rollout");

        myRollout = reloadRollout(myRollout);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, myRollout.getId())
                .getContent();

        Page<Target> targetPage = rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(
                new OffsetBasedPageRequest(0, 100), rolloutGroups.get(0).getId(), rsqlParam);
        final List<Target> targetlistGroup1 = targetPage.getContent();
        assertThat(targetlistGroup1.size()).isEqualTo(5);
        assertThat(targetlistGroup1.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .are(targetBelongsInRollout);

        targetPage = rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(new OffsetBasedPageRequest(0, 100),
                rolloutGroups.get(0).getId(), rsqlParam);
        final List<Target> targetlistGroup2 = targetPage.getContent();
        assertThat(targetlistGroup2.size()).isEqualTo(5);
        assertThat(targetlistGroup2.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .are(targetBelongsInRollout);
    }

    @Test
    @Description("Verify the creation of a Rollout without targets doensn't throw an Exception.")
    void givenNonMatchingTargets_whenCreatingRollout_thenRolloutIsNotCreated() {
        final String rolloutName = "rolloutTest3";
        testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));
        assertThat(rolloutRepository.findByName(rolloutName)).isPresent();
    }

    @Test
    @Description("Verify the creation of a Rollout with the same name throws an Exception.")
    void createDuplicateRollout() {
        final int amountTargetsForRollout = 10;
        final String rolloutName = NAME + testdataFactory.getRandomInt();

        testdataFactory.createTargets(amountTargetsForRollout, "dup-ro-", ROLLOUT);


        testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName))))
                .withMessageContaining("already exists in database");
    }

    @Test
    @Description("Verify the creation and the start of a Rollout with more groups than targets.")
    void givenEmptyGroups_whenCreatingAndStartingRollout_thenFails() {

        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group2", 55, 40, 55) + "]";

        //Action
        List<AssociatedTargetsToRolloutGroup> createdGroups = createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);

        Rollout rollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        List<RolloutGroup> groups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent();

        assertThat(groups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(0).getTotalTargets()).isNotZero();
        assertThat(groups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isNotZero();
        assertThat(groups.get(2).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(groups.get(1).getTotalTargets()).isNotZero();

        rolloutManagement.start(rollout.getId());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        awaitRunningState(rollout.getId());

        rollout = getRollout(rollout.getId());
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
    }


    @Test
    @Description("Verify the creation and the start of a rollout.")
    void givenValidInput_whenCreatingAndStartingRollout_thenSucceeds() {

        final int amountTargetsForRollout = 50;
        final String rolloutName = NAME + testdataFactory.getRandomInt();
        List<Target> targets = testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        Rollout myRollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, false);

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);

        rolloutHandler.handleAll();

        final Long myRolloutId = myRollout.getId();
        myRollout = getRollout(myRolloutId);
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);

        rolloutManagement.freeze(myRolloutId);
        rolloutHandler.handleAll();
        rolloutManagement.start(myRolloutId);

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        awaitRunningState(myRolloutId);

        myRollout = reloadRollout(myRollout);
        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
        final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus = createInitStatusMap();
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.RUNNING, 10L);
        expectedTargetCountStatus.put(TotalTargetCountStatus.Status.SCHEDULED, 40L);
    }

    @Test
    @Description("Verify that a rollout can be created if the 'max targets per rollout group' quota is violated, cause  RolloutHelper.verifyRolloutGroupParameter(1, quotaManagement); it is hardcode.")
    void givenQuotaViolation_whenCreatingRollout_thenFails() {

        final int amountTargets = quotaManagement.getMaxTargetsPerRolloutGroup();;
        final List<Target> createTargetsOne = testdataFactory.createTargets(amountTargets + 1, "trg", "trg");
        final DistributionSet knownDistributionSetOne = testdataFactory.createDistributionSet();

        String groupsDetailsJsonOne = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 55, 40, 55) + "]";

        createRolloutAndGroups(ROLLOUT_1, knownDistributionSetOne, createTargetsOne, groupsDetailsJsonOne, false);
    }

    @Test
    @Description("Verify that a rollout can be created based on group definitions if the 'max targets per rollout group' quota is violated for one of the groups.")
    void givenQuotaViolation_whenCreatingRolloutWithGroupDefinitions_thenFails() {

        final int maxTargets = quotaManagement.getMaxTargetsPerRolloutGroup();

        final int amountTargetsForRollout = maxTargets * 2 + 2;
        final String rolloutName = NAME + testdataFactory.getRandomInt();
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);
        rolloutManagement.create(
                MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));
    }

    private Rollout reloadRollout(final Rollout r) {
        return getRollout(r.getId());
    }

    private Rollout getRollout(final Long myRolloutId) {
        return rolloutManagement.get(myRolloutId).orElseThrow(NoSuchElementException::new);
    }

    @Test
    @Description("Verify the creation of a rollout with a groups definition.")
    void givenGroupDefinition_whenCreatingRollout_thenSuccess() throws Exception {
        final int amountTargets = 8;
        final List<Target> createTargets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 50, 1, 100) + "]";


        createRolloutAndGroups(ROLLOUT, knownDistributionSet, createTargets, groupsDetailsJson, false);
        Rollout createdRollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(createdRollout.getId());
        rolloutHandler.handleAll();

        // 4 targets are running
        final List<Action> runningActions = findActionsByRolloutAndStatus(createdRollout, DeviceActionStatus.RUNNING);
        assertThat(runningActions.size()).isEqualTo(4);

        // 2 targets are in the group and the DS has been assigned
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId())
                .getContent();
        final Page<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE,
                rolloutGroups.get(0).getId());
        final List<Target> targetList = targets.getContent();

        final List<Target> targetToCancel = new ArrayList<>();
        targetToCancel.add(targetList.get(0));
        targetToCancel.add(targetList.get(1));

        final DistributionSet dsForCancelTest = testdataFactory.createDistributionSet("dsForTest");
        assignDistributionSet(dsForCancelTest, targetToCancel);

        final Map<TotalTargetCountStatus.Status, Long> validationMap = createInitStatusMap();
        validationMap.put(TotalTargetCountStatus.Status.RUNNING, 4L);
        validationMap.put(TotalTargetCountStatus.Status.NOTSTARTED, 4L);
        validateRolloutActionStatus(createdRollout.getId(), validationMap);

    }

    @Test
    @Description("Verify rollout execution with advanced group definition and confirmation flow active.")
    void givenGroupDefinitionAndConfirmationFlowActive_whenCreatingRollout_thenSuccess() {
        final int amountTargets = 20;
        final int amountTargetsForGroup = 10;
        final List<Target> createTargets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 50, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT, knownDistributionSet, createTargets, groupsDetailsJson, false);
        Rollout createdRollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        assertThat(getRollout(createdRollout.getId())).satisfies(rollout -> {
            assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);
            for (final RolloutGroup group : rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent()) {
                assertThat(group.getStatus()).isEqualTo(RolloutGroupStatus.DRAFT);
            }
            assertThat(rollout.getTotalTargets()).isEqualTo(amountTargets);
        });
        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        // verify created rollout groups
        List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId()).getContent();
        assertThat(rolloutGroups.size()).isEqualTo(2);

        assertThat(rolloutGroups.get(0).getTotalTargets()).isEqualTo(amountTargetsForGroup);
        assertThat(rolloutGroups.get(1).getTotalTargets()).isEqualTo(amountTargetsForGroup);
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.READY);
        assertThat(rolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.READY);

        // start rollout
        rolloutManagement.start(createdRollout.getId());
        rolloutHandler.handleAll();
        rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId()).getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.QUEUED);

        // cancel execution of all action of group 1 to trigger second group
        forceQuitAllActionsOfRolloutGroup(rolloutGroups.get(0).getId());

        rolloutHandler.handleAll();

        rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, createdRollout.getId()).getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.FINISHING);
        assertThat(rolloutGroups.get(1).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
    }

    private void forceQuitAllActionsOfRolloutGroup(final long rolloutGroupId) {
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, rolloutGroupId)
                .getContent();
        targets.forEach(target ->
                deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId()).getContent().stream().map(Identifiable::getId)
                        .forEach(actionId -> {
                            deploymentManagement.cancelAction(actionId);
                            deploymentManagement.forceQuitAction(actionId);
                        })
        );
    }

    @Test
    @Description("Verify rollout creation doen't fail if group definition does not address all targets")
    void givenGroupsNotMatchingTargets_whenCreatingRollout_thenFails() {
        final String rolloutName = NAME + testdataFactory.getRandomInt();
        final int percentTargetsInGroup1 = 20;
        final int percentTargetsInGroup2 = 50;

        final List<RolloutGroupCreate> rolloutGroups = new ArrayList<>(2);
        rolloutGroups.add(generateRolloutGroup(0, percentTargetsInGroup1, null));
        rolloutGroups.add(generateRolloutGroup(1, percentTargetsInGroup2, null));

        assertThatCode(() -> rolloutManagement.create(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName))))
                .doesNotThrowAnyException();

    }

    @Test
    @Description("Verify rollout creation fails if group definition specifies illegal target percentage")
    void givenIllegalPercentage_whenCreatingRollout_thenFails() {
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 120, 20, 50) + "," +
                createBasicGroupJson("test-group2", -34, 40, 50) + "]";

        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false))
                .withMessageContaining("Target percentage has to be between 1 and 100");

    }

    @Test
    @Description("Verify rollout creation")
    void givenValidData_whenCreatingRollout_thenSucceeds() {
        final String rolloutName = "rolloutTest5";
        Rollout myRollout = rolloutManagement.create(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        assertThat(myRollout).isNotNull();
        assertThat(myRollout.getName()).isEqualTo(rolloutName);
    }

    @Test
    @Description("Verify the start of a Rollout does not work during creation phase.")
    void createAndStartRolloutDuringCreationFails() {
        final int amountTargetsForRollout = 3;
        final String rolloutName = "rolloutTestGC";
        testdataFactory.createTargets(amountTargetsForRollout, rolloutName + "-", rolloutName);

        Rollout myRollout = rolloutManagement.create(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));
        myRollout = getRollout(myRollout.getId());

        assertThat(myRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);

        final Long rolloutId = myRollout.getId();
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.start(rolloutId))
                .withMessageContaining("can only be started in state ready");
    }

    @Test
    @Description("Creating a rollout with approval role or approval engine disabled results in the rollout being in "
            + "READY state.")
    void createdRolloutWithApprovalRoleOrApprovalDisabledTransitionsToInDraftState() {
        approvalStrategy.setApprovalNeeded(false);
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);
    }


    @Test
    @Description("Attempting to freeze a rollout without any devices added throws a RolloutIllegalStateException.")
    void givenRolloutCreatedWithoutDevices_whenFreezeAttempted_thenThrowException() {
        approvalStrategy.setApprovalNeeded(true);
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        assertThat(rollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);
        assertThrows(RolloutIllegalStateException.class, () -> rolloutManagement.freeze(rollout.getId()));
    }


    @Test
    @Description("Verifies that when attempting to delete a Rollout that has never started, its status is set to DELETING instead of being immediately removed. Also ensures that all associated rollout groups and target groups are deleted.")
    @ExpectEvents({@Expect(type = RolloutDeletedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 0),
            @Expect(type = TargetCreatedEvent.class, count = 0),
            @Expect(type = RolloutUpdatedEvent.class, count = 1),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 0),
            @Expect(type = RolloutGroupDeletedEvent.class, count = 0),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 0),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 0),
            @Expect(type = RolloutCreatedEvent.class, count = 1)})
    void givenRolloutNeverStarted_whenDeleted_thenStatusSetToDeleting() {
        final Rollout createdRollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));

        // test
        rolloutManagement.delete(createdRollout.getId());

        // verify
        final Optional<JpaRollout> deletedRollout = rolloutRepository.findById(createdRollout.getId());
        Rollout rolloutDeliting = (Rollout) deletedRollout.get();
        assertThat(rolloutDeliting.getStatus()).isEqualTo(RolloutStatus.DELETING);

        rolloutHandler.handleAll();
        assertThat(rolloutGroupRepository.count()).isZero();
        assertThat(rolloutTargetGroupRepository.count()).isZero();
    }

    @Test
    @ExpectEvents({
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 0),
            @Expect(type = RolloutGroupCreatedEvent.class, count = 0),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 0),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 8),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 4),
    })
    void givenRolloutStartedBefore_whenSoftDeleteAttempted_thenFails() {
        enableMultiAssignments();
        List<Target> targets = testdataFactory.createTargets(2);
        Rollout createdRollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, true);
        startRollout(createdRollout);
        // verify we have running actions
        assertThat(actionRepository.findByRolloutIdAndStatusAndActive(PAGE, createdRollout.getId(), DeviceActionStatus.RUNNING, true)
                .getNumberOfElements()).isEqualTo(2);


        // Try to delete the started rollout and expect a ResponseStatusException with 400 BAD_REQUEST
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> rolloutManagement.delete(createdRollout.getId()))
                .withMessageContaining("Rollout can be deleted only in DRAFT");


        // Verify that the status of the rollout has not been changed to deleted
        final JpaRollout nonDeletedRollout = rolloutRepository.findById(createdRollout.getId()).get();
        assertThat(nonDeletedRollout.getStatus()).isEqualTo(RolloutStatus.RUNNING);
    }


    @Test
    @Description("Creating a rollout without weight value when multi assignment in enabled.")
    void weightNotRequiredInMultiAssignmentMode() {
        enableMultiAssignments();
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        assertThat(rollout).isNotNull();
    }

    @Test
    @Description("Creating a rollout with a weight causes an error when multi assignment in disabled.")
    void givenMultiAssignmentModeNotEnabled_whenCheckingWeightRequirement_thenNotAllowed() {

        MgmtRolloutRestRequestBody rolloutRestRequestBody = testdataFactory.buildDefaultRolloutRequest("test");

        Rollout rollout = MgmtRolloutMapper.fromRequest(rolloutRestRequestBody);
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setWeight(WEIGHT);

        testdataFactory.addNewRollout(jpaRollout);

        Optional<JpaRollout> saved = rolloutRepository.findById(jpaRollout.getId());
        Assertions.assertThat(saved).isPresent();
        Assertions.assertThat(saved.get().getWeight().get()).isEqualTo(WEIGHT);
    }


    @Test
    @Description("Verifies that returned result considers provided sort parameter.")
    void givenSortingCriteria_whenFindingAllRollouts_thenSortedCorrectly() {
        final String randomString = RandomStringUtils.randomAlphanumeric(5);
        List<Target> targets = testdataFactory.createTargets(10, randomString + "-testTarget-");

        final String prefixRolloutRunning = randomString + "1";
        Rollout rolloutRunning = createRolloutWithDependencies(prefixRolloutRunning, testdataFactory.createDistributionSet(), targets, false);
        // Let the executor handle created Rollout
        rolloutManagement.freeze(rolloutRunning.getId());
        rolloutHandler.handleAll();
        // start the rollout, so it has active running actions and a group which
        // has been started
        rolloutManagement.start(rolloutRunning.getId());
        rolloutHandler.handleAll();
        rolloutRunning = reloadRollout(rolloutRunning);

        final String prefixRolloutReady = randomString + "2";

        Rollout rolloutReady = createRolloutWithDependencies(prefixRolloutReady + "-testRollout", testdataFactory.createDistributionSet(), targets, false);
        // Let the executor handle created Rollout
        rolloutManagement.freeze(rolloutReady.getId());
        rolloutHandler.handleAll();

        rolloutReady = reloadRollout(rolloutReady);

        final List<Rollout> rolloutsOrderedByStatus = rolloutManagement
                .findAll(PageRequest.of(0, 500, Sort.by(Direction.ASC, "status")), false).getContent();
        assertThat(rolloutsOrderedByStatus).containsSubsequence(List.of(rolloutReady, rolloutRunning));

        final List<Rollout> rolloutsOrderedByName = rolloutManagement
                .findAll(PageRequest.of(0, 500, Sort.by(Direction.ASC, "name")), false).getContent();
        assertThat(rolloutsOrderedByName).containsSubsequence(List.of(rolloutRunning, rolloutReady));
    }

    @Test
    @Description("Weight is validated and saved to the Rollout.")
    void givenValidWeight_whenSaving_thenWeightIsValidatedAndSaved() throws Exception {
        final String targetPrefix = RandomStringUtils.randomAlphanumeric(8);

        testdataFactory.createTargets(4, targetPrefix);

        final Rollout createdRollout1 = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        final Rollout createdRollout2 = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test1")));

        MgmtRolloutUpdateRequest rolloutRequest1 = new MgmtRolloutUpdateRequest();
        rolloutRequest1.setWeight(Action.WEIGHT_MAX);
        rolloutRequest1.setType(MgmtRolloutType.FOTA);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, createdRollout1.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());


        MgmtRolloutUpdateRequest rolloutRequest2 = new MgmtRolloutUpdateRequest();
        rolloutRequest2.setWeight(Action.WEIGHT_MIN);
        rolloutRequest2.setType(MgmtRolloutType.FOTA);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, createdRollout2.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        enableMultiAssignments();

        assertThat(rolloutRepository.findById(createdRollout1.getId()).get().getWeight()).get()
                .isEqualTo(Action.WEIGHT_MAX);
        assertThat(rolloutRepository.findById(createdRollout2.getId()).get().getWeight()).get()
                .isEqualTo(Action.WEIGHT_MIN);
    }

    @Test
    @Description("A Rollout with weight creates actions with weights")
    void givenActionsWithWeight_whenCreated_thenAreSuccessfullyCreated() throws Exception {
        final int amountOfTargets = 5;
        final int weight = 99;
        enableMultiAssignments();
        List<Target> targets = testdataFactory.createTargets(amountOfTargets);
        Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, false);

        MgmtRolloutUpdateRequest rolloutRequest = new MgmtRolloutUpdateRequest();
        rolloutRequest.setWeight(weight);
        rolloutRequest.setType(MgmtRolloutType.FOTA);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        enableMultiAssignments();
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();

        assertThat(actions) //
                .hasSize(amountOfTargets) //
                .allMatch(action -> action.getWeight().get() == weight);
    }

    @Test
    @Description("Rollout can be created without weight in single assignment and be started in multi assignment")
    void createInSingleStartInMultiassigMode() {
        final int amountOfTargets = 5;
        List<Target> targets = testdataFactory.createTargets(amountOfTargets);
        Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets, false);

        enableMultiAssignments();
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions).hasSize(amountOfTargets).allMatch(action -> !action.getWeight().isPresent());
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to create a rollout with an invalidated distribution set.")
    void createRolloutWithInvalidDistributionSet() {
        final Rollout rollout = testdataFactory.addNewRollout(
                MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        final DistributionSet invalidDistributionSet = testdataFactory.createAndInvalidateDistributionSet();

        final RolloutUpdate updatedRollout = entityFactory.rollout().update(rollout.getId()).set(invalidDistributionSet.getId());
        assertThrows(InvalidDistributionSetException.class, () -> rolloutManagement.update(updatedRollout));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to create a rollout with an incomplete distribution set.")
    void createRolloutWithIncompleteDistributionSet() {
        final Rollout rollout = testdataFactory.addNewRollout(
                MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        final DistributionSet incompleteDistributionSet = testdataFactory.createIncompleteDistributionSet();

        final RolloutUpdate updatedRollout = entityFactory.rollout().update(rollout.getId()).set(incompleteDistributionSet.getId());
        assertThrows(IncompleteDistributionSetException.class, () -> rolloutManagement.update(updatedRollout));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update a rollout with an invalidated distribution set.")
    void updateRolloutWithInvalidDistributionSet() {
        testdataFactory.createTarget();
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        final DistributionSet invalidDistributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception").isThrownBy(() -> rolloutManagement
                        .update(entityFactory.rollout().update(rollout.getId()).set(invalidDistributionSet.getId())));
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update a rollout with an incomplete distribution set.")
    void updateRolloutWithIncompleteDistributionSet() {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        final DistributionSet incompleteDistributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception").isThrownBy(() -> rolloutManagement.update(
                        entityFactory.rollout().update(rollout.getId()).set(incompleteDistributionSet.getId())));
    }

    @Test
    @Description("Verify the that only compatible targets are part of a Rollout.")
    void createAndStartRolloutWithTargetTypes() {
        final String rolloutName = "rolloutTestCompatibility";

        final DistributionSet testDs = testdataFactory.createDistributionSet("test-ds");
        final TargetType incompatibleTargetType = testdataFactory.createTargetType("incompatible-type",
                Collections.emptyList());
        final TargetType compatibleTargetType = testdataFactory.createTargetType("compatible-type",
                Collections.singletonList(testDs.getType()));

        final List<Target> incompatibleTargets = testdataFactory.createTargetsWithType(10, "incompatible",
                incompatibleTargetType);
        final List<Target> targetsWithoutType = testdataFactory.createTargets(10, "testTarget-");
        final List<Target> targets = testdataFactory.createTargetsWithType(10, "compatibleTarget-",
                compatibleTargetType);
        targets.addAll(targetsWithoutType);


        final Rollout createdRollout = createRolloutWithDependencies(rolloutName, testDs, targets, false);

        // Let the executor handle created Rollout
        rolloutHandler.handleAll();

        final Rollout testRollout = reloadRollout(createdRollout);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement
                .findByRollout(Pageable.unpaged(), testRollout.getId()).getContent();

        assertThat(testRollout.getStatus()).isEqualTo(RolloutStatus.DRAFT);
        assertThat(testRollout.getTotalTargets()).isEqualTo(targets.size());
        assertThat(rolloutGroups).hasSize(1);
        assertThat(rolloutGroups.get(0).getTotalTargets()).isEqualTo(targets.size());

        final List<Target> rolloutGroupTargets = rolloutGroupManagement
                .findTargetsOfRolloutGroup(Pageable.unpaged(), rolloutGroups.get(0).getId()).getContent();

        assertThat(rolloutGroupTargets).hasSize(targets.size()).allSatisfy(target -> {
            Target expectedTarget = targets.stream().filter(t -> t.getId().equals(target.getId())).findFirst().orElse(null);
            assertThat(expectedTarget).isNotNull();
            assertThat(target.getId()).isEqualTo(expectedTarget.getId());
            assertThat(target.getName()).isEqualTo(expectedTarget.getName());
        }).doesNotContainAnyElementsOf(incompatibleTargets);
    }

    private RolloutGroupCreate generateRolloutGroup(final int index, final Integer percentage,
                                                    final String targetFilter) {
        return generateRolloutGroup(index, percentage, targetFilter, false);
    }

    private RolloutGroupCreate generateRolloutGroup(final int index, final Integer percentage,
                                                    final String targetFilter, final boolean confirmationRequired) {
        return entityFactory.rolloutGroup().create().name("Group" + index).description("Group" + index + "desc")
                .targetPercentage(Float.valueOf(percentage)).targetFilterQuery(targetFilter)
                .confirmationRequired(confirmationRequired);
    }

    private void validateRolloutGroupActionStatus(final RolloutGroup rolloutGroup,
                                                  final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroup;

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = new ArrayList<>();
        rolloutStatusCountItems.addAll(actionRepository.getStatusCountByRolloutGroupId(jpaRolloutGroup.getId(), true));
        rolloutStatusCountItems.addAll(actionRepository.getStatusCountByRolloutGroupId(jpaRolloutGroup.getId(), false));

        TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                rolloutStatusCountItems,
                (long) jpaRolloutGroup.getTotalTargets(),
                jpaRolloutGroup.getRollout().getUserAcceptanceRequired()
        );
        jpaRolloutGroup.setTotalTargetCountStatus(totalTargetCountStatus);
        validateStatus(totalTargetCountStatus, expectedTargetCountStatus);
    }

    private void validateRolloutActionStatus(final Long rolloutId,
                                             final Map<TotalTargetCountStatus.Status, Long> expectedTargetCountStatus) {
        Optional<JpaRollout> rolloutOpt = rolloutRepository.findById(rolloutId);
        JpaRollout rollout = rolloutOpt.get();
        List<TotalTargetCountActionStatus> statusCountItems = new ArrayList<>();
        statusCountItems.addAll(actionRepository.getStatusCountByRolloutId(rolloutId, true));
        statusCountItems.addAll(actionRepository.getStatusCountByRolloutId(rolloutId, false));

        TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(
                statusCountItems,
                rollout.getTotalTargets(),
                rollout.getUserAcceptanceRequired()
        );
        rollout.setTotalTargetCountStatus(totalTargetCountStatus);
        validateStatus(totalTargetCountStatus, expectedTargetCountStatus);
    }

    private void validateStatus(final TotalTargetCountStatus totalTargetCountStatus,
                                final Map<TotalTargetCountStatus.Status, Long> expectedTotalCountStates) {
        for (final Map.Entry<TotalTargetCountStatus.Status, Long> entry : expectedTotalCountStates.entrySet()) {
            final Long countReady = totalTargetCountStatus.getTotalTargetCountByStatus(entry.getKey());
            assertThat(countReady).as("targets in status " + entry.getKey()).isEqualTo(entry.getValue());
        }
    }

    private int changeStatusForAllRunningActions(final Rollout rollout, final DeviceActionStatus status) {
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        for (final Action action : runningActions) {
            controllerManagement
                    .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(status), null);
        }
        return runningActions.size();
    }

    private int changeStatusForRunningActions(final Rollout rollout, final DeviceActionStatus status,
                                              final int amountOfTargetsToGetChanged) {
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        assertThat(runningActions.size()).isGreaterThanOrEqualTo(amountOfTargetsToGetChanged);
        for (int i = 0; i < amountOfTargetsToGetChanged; i++) {
            controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(runningActions.get(i).getId()).status(status), null);
        }
        return runningActions.size();
    }

    private void awaitRunningState(final Long myRolloutId) {
        Awaitility.await().atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(500)).with()
                .until(() -> WithSpringAuthorityRule
                        .runAsPrivileged(
                                () -> rolloutManagement.get(myRolloutId).orElseThrow(NoSuchElementException::new))
                        .getStatus().equals(RolloutStatus.RUNNING));
    }

    @Test
    @Description("Verifying that next group is started on manual trigger next group.")
    void givenRunningRollouts_whenManuallyTriggeringNextGroup_thenSuccess() {

        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 20, 1, 100) + "," +
                createBasicGroupJson("test-group02", 20, 1, 100) + "," +
                createBasicGroupJson("test-group03", 20, 1, 100) + "," +
                createBasicGroupJson("test-group04", 20, 1, 100) + "," +
                createBasicGroupJson("test-group05", 20, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);

        Rollout createdRollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);
        startRollout(createdRollout);
        // triggers next group
        rolloutManagement.triggerNextGroup(createdRollout.getId());

        // second group should in running state
        List<RolloutGroup> rolloutGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")), createdRollout.getId())
                .getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(1).getStatus()).isIn(RolloutGroupStatus.QUEUED, RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(2).getStatus()).isIn(RolloutGroupStatus.QUEUED, RolloutGroupStatus.RUNNING);

        // triggers next group
        rolloutManagement.triggerNextGroup(createdRollout.getId());
        rolloutHandler.handleAll();
        // third group should be in running state
        rolloutGroups = rolloutGroupManagement
                .findByRollout(new OffsetBasedPageRequest(0, 10, Sort.by(Direction.ASC, "id")), createdRollout.getId())
                .getContent();
        assertThat(rolloutGroups.get(0).getStatus()).isEqualTo(RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(1).getStatus()).isIn(RolloutGroupStatus.QUEUED, RolloutGroupStatus.RUNNING);
        assertThat(rolloutGroups.get(2).getStatus()).isIn(RolloutGroupStatus.QUEUED, RolloutGroupStatus.RUNNING);
    }

    @Test
    void givenRolloutAndTargetFilterQueryWhenAddTargetFilterQueryThenAssociateWithRollout() {

        var rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("rollout1")));
        var targetFilterQuery = "tag==rollout01";

        assertNull(rollout.getTargetFilterQuery());
        var updatedRollout = rolloutManagement.addTargetFilterQuery(rollout, targetFilterQuery);

        assertEquals(targetFilterQuery, updatedRollout.getTargetFilterQuery());
    }


    @Test
    @Description("Trigger next rollout group if rollout is in wrong state")
    void givenRolloutInWrongState_whenTriggeringNextGroup_thenThrowException() {

        final String errorMessage = "Rollout is not in running state";

        final Rollout createdRollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), List.of(testdataFactory.createTarget()), false);

        // check CREATING state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRollout.getId()))
                .withMessageContaining(errorMessage);
        rolloutManagement.freeze(createdRollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(createdRollout.getId());
        // check STARTING state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRollout.getId()))
                .withMessageContaining(errorMessage);

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();
        final Rollout rollout = reloadRollout(createdRollout);

        rolloutManagement.pauseRollout(rollout.getId());

        // check STOPPED state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRollout.getId()))
                .withMessageContaining(errorMessage);

        final Slice<JpaAction> runningActionsSlice = actionRepository.findByRolloutIdAndStatusAndActive(PAGE,
                createdRollout.getId(), DeviceActionStatus.RUNNING, true);
        runningActionsSlice.getContent().forEach(this::finishAction);

        // check FINISHED state
        assertThatExceptionOfType(RolloutIllegalStateException.class)
                .isThrownBy(() -> rolloutManagement.triggerNextGroup(createdRollout.getId()))
                .withMessageContaining(errorMessage);
    }

    @Test
    @Description("Verifies that 2 groups are created with 3 targets in 1 and 1 target in 2nd group.")
    void givenGroupingDetailsAddDeviceDetailsCreatesGroupsAndAddTargetsWithProvidedGroupingDetails() {
        //setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group2", 75, 40, 50) + "]";

        //Action
        List<AssociatedTargetsToRolloutGroup> createdGroups = createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);

        //Verify
        //Based on the grouping conditions/details 2 groups -
        // 3 target in 1st group, 1 targets in 2nd group are created
        assertEquals(2, createdGroups.size());
        assertEquals("test-group01", createdGroups.get(0).getRolloutGroup().getName());
        assertEquals(75, createdGroups.get(0).getRolloutGroup().getTargetPercentage());
        assertEquals(3, createdGroups.get(0).getRolloutGroup().getTotalTargets());
        assertEquals("test-group2", createdGroups.get(1).getRolloutGroup().getName());
        assertEquals(1, createdGroups.get(1).getRolloutGroup().getTotalTargets());
        assertEquals(75, createdGroups.get(1).getRolloutGroup().getTargetPercentage());
    }

    @Test
    @Description("Verifies that 2 groups are created with all 4 targets in 1st and 0 targets in 2nd group.")
    void givenGroupingDetailsAddDeviceDetailsCreatesGroupsAndAddTargetsWithProvidedPercentage() {
        //setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 100, 20, 50) + "," +
                createBasicGroupJson("test-group2", 25, 40, 50) + "]";


        //Action
        List<AssociatedTargetsToRolloutGroup> createdGroups = createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);

        //Verify
        //Based on the grouping conditions/details 1 group(Because second group will be empty) -
        // 4 target in 1st group, 0 targets in 2nd group are created
        assertEquals(1, createdGroups.size());
        assertEquals("test-group01", createdGroups.get(0).getRolloutGroup().getName());
        assertEquals(100, createdGroups.get(0).getRolloutGroup().getTargetPercentage());
        assertEquals(4, createdGroups.get(0).getRolloutGroup().getTotalTargets());
    }

    @Test
    @Description("Verifies that 4 groups are created, 4th is a default group with remaining targets in it")
    void givenGroupingDetailsWithRemainingTargetsAddDeviceDetailsCreatesADefaultGroupAddRemainingTargets() {
        //setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group2", 55, 40, 55) + "]";

        //Action
        List<AssociatedTargetsToRolloutGroup> createdGroups = createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson, false);

        //Verify
        //Based on the grouping conditions/details 3 groups -
        // 1 target in 1st group, 2 targets in 2nd group and 1 target in 3rd a default group are created
        assertEquals(3, createdGroups.size());
        assertEquals("test-group01", createdGroups.get(0).getRolloutGroup().getName());
        assertEquals(25, createdGroups.get(0).getRolloutGroup().getTargetPercentage());
        assertEquals(1, createdGroups.get(0).getRolloutGroup().getTotalTargets());
        assertEquals("test-group2", createdGroups.get(1).getRolloutGroup().getName());
        assertEquals(55, createdGroups.get(1).getRolloutGroup().getTargetPercentage());
        assertEquals(2, createdGroups.get(1).getRolloutGroup().getTotalTargets());
        assertEquals(ROLLOUT + "_default_group_3", createdGroups.get(2).getRolloutGroup().getName());
        assertEquals(100, createdGroups.get(2).getRolloutGroup().getTargetPercentage());
        assertEquals(1, createdGroups.get(2).getRolloutGroup().getTotalTargets());
    }

    @ParameterizedTest
    @MethodSource("groupDetailsJson")
    @Description("Verifies that default group is created with default conditions and all targets are added to it.")
    void givenNoGroupingDetailsAddDeviceDetailsCreatesADefaultGroupWithTargets(String groupDetailsJson) {
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        List<AssociatedTargetsToRolloutGroup> createdGroups = createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupDetailsJson, false);

        assertEquals(1, createdGroups.size());
        assertEquals(ROLLOUT + "_default_group_1", createdGroups.get(0).getRolloutGroup().getName());
        assertEquals(100, createdGroups.get(0).getRolloutGroup().getTargetPercentage());
        assertEquals(4, createdGroups.get(0).getRolloutGroup().getTotalTargets());
    }

    private static Stream<String> groupDetailsJson() {
        return Stream.of(
                null,
                ""
        );
    }

    @Test
    @Description("Verifies that default group is created with default conditions and all targets are added to it.")
    void givenInvalidGroupsDetailsJsonAddDeviceDetailsThrowsAValidationException() {
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        assertThrows(ValidationException.class, () -> createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, "invalidJson", false));
    }

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final DeviceActionStatus actionStatus) {
        return Lists.newArrayList(actionRepository.findByRolloutIdAndStatusAndActive(PAGE, rollout.getId(), actionStatus, true));
    }


    /**
     * Creates a rollout and groups based on the provided grouping conditions and associates them
     * with the given rollout.
     *
     * @param rolloutName       name of the rollout
     * @param ds                distribution set
     * @param targets           list of targets to be grouped
     * @param groupsDetailsJson conditions for creating groups and assigning targets
     * @return list of groups created and associated targets to the groups
     */
    private @NotNull List<AssociatedTargetsToRolloutGroup> createRolloutAndGroups(String rolloutName, DistributionSet ds, List<Target> targets, String groupsDetailsJson, Boolean isAssociateArtifactWithSoftwareModule) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        if (isAssociateArtifactWithSoftwareModule) {
            // Create artifact and associate with software module
            associateArtifactWithSoftwareModule(softwareModule, version);
        }

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        return rolloutManagement.get(rollout.getId()).map(savedRollout ->
                rolloutManagement.addDeviceDetails(savedRollout, targets.stream().map(Target::getControllerId).toList(), groupsDetailsJson)
        ).orElse(Collections.emptyList());
    }


    /**
     * Create a basic rollout group json with provided values.
     *
     * @param groupName                  name of the group
     * @param targetPercentage           target percentage
     * @param errorConditionExpression   error condition expression
     * @param successConditionExpression success condition expression
     * @return json body.
     */
    private String createBasicGroupJson(String groupName,
                                        int targetPercentage,
                                        int errorConditionExpression,
                                        int successConditionExpression) {

        return String.format("{ \"errorCondition\": { \"action\": \"THRESHOLD\", \"expression\": %d }, \"successCondition\": { \"action\": \"THRESHOLD\", \"expression\": %d }, \"name\": \"%s\", \"targetPercentage\": %d }",
                errorConditionExpression, successConditionExpression, groupName, targetPercentage);
    }

    private Rollout createAndStartRollout(String name, String controllerId, Boolean isAssociateArtifactSoftwareModule) {
        Rollout rollout = createRolloutWithDependencies(name, testdataFactory.createDistributionSet(), testdataFactory.createTargets(controllerId), isAssociateArtifactSoftwareModule);
        startRollout(rollout);
        return rollout;
    }

    private void startRollout(Rollout rollout) {
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
    }
}