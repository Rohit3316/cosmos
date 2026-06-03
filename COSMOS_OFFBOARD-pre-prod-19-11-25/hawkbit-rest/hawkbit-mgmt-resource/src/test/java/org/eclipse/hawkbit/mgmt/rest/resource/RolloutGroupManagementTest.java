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
import io.qameta.allure.Story;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetWithActionStatus;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpStatus;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@Feature("Component Tests - Repository")
@Story("Rollout Management")
class RolloutGroupManagementTest extends AbstractManagementRolloutApiIntegrationTest {
    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String LAST_ACTION_STATUS_CODE = "lastActionStatusCode";
    public static final String TARGET_MANAGEMENT = "targetManagement";
    public static final String ROLLOUT_TARGET_GROUP_REPOSITORY = "rolloutTargetGroupRepository";
    public static final String ROLLOUT_1 = "rollout1";
    public static final String GROUP_1 = "group1";
    @Autowired
    private ActionRepository actionRepository;
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatusSuccess();
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    private static void mockPublishVehicleStatusSuccess() {
        mockServer
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response()
                        .withStatusCode(HttpStatus.OK.value()));
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY,
                SP_BASE_SOFTWARE_MODULE, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_BASE_SOFTWARE_MODULE, SP_ROLLOUT
        );
    }

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final DeviceActionStatus actionStatus) {
        return Lists.newArrayList(actionRepository.findByRolloutIdAndStatusAndActive(PAGE, rollout.getId(), actionStatus, true));
    }

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 0)})
    void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(rolloutGroupManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(rolloutGroupManagement.getWithDetailedStatus(NOT_EXIST_IDL)).isNotPresent();

    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = RolloutCreatedEvent.class, count = 1)})
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));

        verifyThrownExceptionBy(() -> rolloutGroupManagement.countByRollout(NOT_EXIST_IDL), Rollout.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutGroupManagement.countTargetsOfRolloutsGroup(NOT_EXIST_IDL),
                RolloutGroup.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutWithDetailedStatus(PAGE, NOT_EXIST_IDL),
                Rollout.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(PAGE, NOT_EXIST_IDL),
                RolloutGroup.class.getSimpleName());
        verifyThrownExceptionBy(() -> rolloutGroupManagement.findByRolloutAndRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                Rollout.class.getSimpleName());

        verifyThrownExceptionBy(() -> rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, NOT_EXIST_IDL),
                RolloutGroup.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(PAGE, NOT_EXIST_IDL, "name==*"),
                RolloutGroup.class.getSimpleName());
    }

    @Test
    @Description("Verifies that the returned result considers the provided sort parameters.")
    void findAllTargetsOfRolloutGroupWithActionStatusConsidersSorting() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(4, 1);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        final RolloutGroup rolloutGroup = rolloutGroups.get(0);
        final List<Target> targets = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, rolloutGroup.getId())
                .getContent();
        Target targetCancelled = targets.get(0);
        final Action actionCancelled = deploymentManagement.findActionsByTarget(targetCancelled.getControllerId(), PAGE)
                .getContent().get(0);
        deploymentManagement.cancelAction(actionCancelled.getId());
        deploymentManagement.forceQuitAction(actionCancelled.getId());
        targetCancelled = reloadTarget(targetCancelled);
        final List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "status")),
                        rolloutGroup.getId())
                .getContent();
        assertThat(targetsWithActionStatus.get(0).getTarget()).isEqualTo(targetCancelled);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameDesc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameDesc, Direction.DESC);

        final List<TargetWithActionStatus> targetsWithActionStatusOrderedByNameAsc = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(PageRequest.of(0, 500, Sort.by(Direction.ASC, "name")),
                        rolloutGroup.getId())
                .getContent();
        assertThatListIsSortedByTargetName(targetsWithActionStatusOrderedByNameAsc, Direction.ASC);
    }

    @Test
    @Description("Verifies that the returned result considers sorting by action status code.")
    void findAllTargetsOfRolloutGroupWithActionStatusConsidersSortingByLastActionStatusCode() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(23, 5);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();
        final RolloutGroup rolloutGroup = rolloutGroups.get(0);
        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        final Target target0 = runningActions.get(0).getTarget();
        final Target target24 = CollectionUtils.lastElement(runningActions).getTarget();
        int i = 0;
        for (final Action action : runningActions) {
            controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.RUNNING).code(i++), null);
        }

        List<TargetWithActionStatus> targetsWithActionStatus = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.ASC, LAST_ACTION_STATUS_CODE)),
                        rolloutGroup.getId())
                .getContent();
        assertSortedListOfActionStatus(targetsWithActionStatus, target0, 0, target24, 4);
        assertThat(targetsWithActionStatus)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroup.getId()));

        targetsWithActionStatus = rolloutGroupManagement.findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, LAST_ACTION_STATUS_CODE)), rolloutGroup.getId())
                .getContent();
        assertSortedListOfActionStatus(targetsWithActionStatus, target24, 4, target0, 0);
    }

    private void assertSortedListOfActionStatus(final List<TargetWithActionStatus> targetsWithActionStatus,
                                                final Target first, final Integer firstStatusCode, final Target last, final Integer lastStatusCode) {
        assertTargetAndActionStatusCode(CollectionUtils.firstElement(targetsWithActionStatus), first, firstStatusCode);
        assertTargetAndActionStatusCode(CollectionUtils.lastElement(targetsWithActionStatus), last, lastStatusCode);
    }

    private void assertTargetAndActionStatusCode(final TargetWithActionStatus targetWithActionStatus,
                                                 final Target target, final Integer actionStatusCode) {
        assertThat(targetWithActionStatus.getTarget().getControllerId()).isEqualTo(target.getControllerId());
        assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
    }

    private void assertTargetNotNullAndActionStatusNullAndActionStatusCode(
            final List<TargetWithActionStatus> targetsWithActionStatus, final Integer actionStatusCode) {
        targetsWithActionStatus.forEach(targetWithActionStatus -> {
            assertThat(targetWithActionStatus.getTarget().getControllerId()).isNotNull();
            assertThat(targetWithActionStatus.getStatus()).isNull();
            assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
        });
    }

    private void assertTargetNotNullAndActionStatusAndActionStatusCode(
            final List<TargetWithActionStatus> targetsWithActionStatus, final DeviceActionStatus actionStatus,
            final Integer actionStatusCode) {
        targetsWithActionStatus.forEach(targetWithActionStatus -> {
            assertThat(targetWithActionStatus.getTarget().getControllerId()).isNotNull();
            assertThat(targetWithActionStatus.getStatus()).isEqualTo(actionStatus);
            assertThat(targetWithActionStatus.getLastActionStatusCode()).isEqualTo(actionStatusCode);
        });
    }

    @Test
    @Description("Verifies that Rollouts in different states are handled correctly.")
    void findAllTargetsOfRolloutGroupWithActionStatus() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(4, 1);
        final List<RolloutGroup> rolloutGroups = rolloutGroupManagement.findByRollout(PAGE, rollout.getId())
                .getContent();

        final List<Action> runningActions = findActionsByRolloutAndStatus(rollout, DeviceActionStatus.RUNNING);
        final RolloutGroup rolloutGroupRunning = runningActions.get(0).getRolloutGroup();
        for (final Action action : runningActions) {
            controllerManagement.addUpdateActionStatus(
                    entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.RUNNING).code(100), null);
        }

        // check query when action status code exists
        final List<TargetWithActionStatus> targetsWithActionStatusForRunningRG = rolloutGroupManagement
                .findAllTargetsOfRolloutGroupWithActionStatus(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, LAST_ACTION_STATUS_CODE)),
                        rolloutGroupRunning.getId())
                .getContent();
        assertThat(targetsWithActionStatusForRunningRG)
                .hasSize((int) rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroupRunning.getId()));
        assertTargetNotNullAndActionStatusAndActionStatusCode(targetsWithActionStatusForRunningRG,
                DeviceActionStatus.RUNNING, 100);
    }

    @Test
    @Description("Given valid rollout a rollout group is created")
    void givenRolloutWhenCreateGroupThenRolloutGroupsCreated() {
        JpaRollout rollout = (JpaRollout) testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("rollout5")));

        rollout.setStatus(RolloutStatus.DRAFT);
        RolloutGroup createdRollout = rolloutGroupManagement.createDefaultRolloutGroup(rollout);

        assertEquals(rollout.getName() + "_default_group_1", createdRollout.getName());
    }

    private void assertThatListIsSortedByTargetName(final List<TargetWithActionStatus> targets,
                                                    final Direction sortDirection) {
        String previousName = null;
        for (final TargetWithActionStatus targetWithActionStatus : targets) {
            final String actualName = targetWithActionStatus.getTarget().getName();
            if (previousName != null) {
                if (Direction.ASC == sortDirection) {
                    assertThat(actualName).isGreaterThanOrEqualTo(previousName);
                } else {
                    assertThat(actualName).isLessThanOrEqualTo(previousName);
                }
            }
            previousName = actualName;
        }
    }

    private Target reloadTarget(final Target targetCancelled) {
        return targetManagement.get(targetCancelled.getId()).orElseThrow();
    }

    protected static void verifyThrownExceptionBy(final ThrowableAssert.ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    private JpaRollout createTestRollout(String rolloutName){
        JpaRollout rollout = (JpaRollout) MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1));
        rollout.setName(rolloutName);
        rollout.setStatus(RolloutStatus.DRAFT);
        rollout.setTargetFilterQuery("tag=="+ rollout.getName());
        rollout.setDistributionSet(testdataFactory.createDistributionSet());

        return rollout;
    }

    private JpaRolloutGroup createTestRolloutGroup(String groupName, RolloutGroupConditions conditions, Rollout rollout){
        JpaRolloutGroup group =(JpaRolloutGroup) entityFactory.rolloutGroup().create()
                .name(groupName)
                .conditions(conditions)
                .targetPercentage(100f)
                .confirmationRequired(true)
                .description("")
                .build();
        group.setRollout(rollout);
        return group;
    }

}
