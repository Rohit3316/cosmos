/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

import java.net.URISyntaxException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;

import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.ddi.*;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TargetTestData;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.jpa.domain.Specification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS;
import static org.eclipse.hawkbit.repository.jpa.configuration.Constants.TX_RT_MAX;
import static org.eclipse.hawkbit.repository.test.util.TestdataFactory.DEFAULT_CONTROLLER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Component Tests - Repository")
@Story("Controller Management")
class ControllerManagementTest extends AbstractJpaIntegrationTest {

    @Autowired
    private RepositoryProperties repositoryProperties;

    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;


    private static final String PRINCIPLE = "controller";
    private static final String CONTROLLER_ID = "controllerId";
    private static final String NAME = "name";
    private static final String FILE = "file";
    private static final String DESCRIPTION = "Controller Attributes are wrong";
    private static final String TEST_DATA_1 = "testdata1";
    private static final String TEST_DATA_20 = "testdata20";
    private static final String TEST_DATA_12 = "testdata12";
    private static final String V0 = "v0";
    private static final String V1 = "v1";
    private static final String V2 = "v2";
    private static final String V3 = "v3";
    private static final String V4 = "v4";
    private static final String K0 = "k0";
    private static final String K1 = "k1";
    private static final String K2 = "k2";
    private static final String K3 = "k3";
    private static final String K4 = "k4";
    private static final String T1 = "t1";
    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String V1_MODIFIED = "v1_modified";
    private static final String V1_MODIFIED_AGAIN = "v1_modified_again";
    private static final String AA = "AA";
    private static final String DS1 = "ds1";
    private static final String MSG = "msg";



    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1)})
    void nonExistingEntityAccessReturnsNotPresent() {
        final Target target = testdataFactory.createTarget();
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        assertThat(controllerManagement.findActionWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getByControllerId(NOT_EXIST_ID)).isNotPresent();
        assertThat(controllerManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(controllerManagement.getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(),
                module.getId())).isNotPresent();

        assertThat(controllerManagement.findActiveActionWithHighestWeight(NOT_EXIST_ID)).isNotPresent();

    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1)})
    void entityQueriesReferringToNotExistingEntitiesThrowsException() throws URISyntaxException {
        final Target target = testdataFactory.createTarget();
        final SoftwareModule module = testdataFactory.createSoftwareModuleOs();

        verifyThrownExceptionBy(() -> controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(DeviceActionStatus.FINISHED_SUCCESS)), Action.class.getSimpleName());

        verifyThrownExceptionBy(() -> controllerManagement.addInformationalActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(DeviceActionStatus.RUNNING)), Action.class.getSimpleName());

        verifyThrownExceptionBy(() -> controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(NOT_EXIST_IDL).status(DeviceActionStatus.FINISHED_SUCCESS), null), Action.class.getSimpleName());

        verifyThrownExceptionBy(() -> controllerManagement
                        .getActionForDownloadByTargetAndSoftwareModule(target.getControllerId(), NOT_EXIST_IDL),
                "SoftwareModule");

        verifyThrownExceptionBy(
                () -> controllerManagement.getActionForDownloadByTargetAndSoftwareModule(NOT_EXIST_ID, module.getId()),
                Target.class.getSimpleName());

        verifyThrownExceptionBy(() -> controllerManagement.findActionStatusByAction(PAGE, NOT_EXIST_IDL), Action.class.getSimpleName());

        verifyThrownExceptionBy(() -> controllerManagement.registerRetrieved(NOT_EXIST_IDL, "test message"), Action.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> controllerManagement.updateControllerAttributesWithSoftware(NOT_EXIST_ID, Maps.newHashMap(), null, null), Target.class.getSimpleName());
    }

    @Test
    @Description("Controller confirms successful update with FINISHED status.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerConfirmsUpdateWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.DD_SENT,
                DeviceActionStatus.FINISHED_NOT_EXECUTED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller confirmation fails with invalid messages.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerConfirmationFailsWithInvalidMessages() {
        final Long actionId = createTargetAndAssignDs();

        simulateIntermediateStatusOnUpdate(actionId);

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(entityFactory.actionStatus()
                        .create(actionId).status(DeviceActionStatus.FINISHED_SUCCESS).message(INVALID_TEXT_HTML), null));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> controllerManagement.addUpdateActionStatus(
                        entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_SUCCESS)
                                .messages(Arrays.asList("this is valid.", INVALID_TEXT_HTML)), null));

        assertThat(actionStatusRepository.count()).isEqualTo(6);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(6);
    }

    @Test
    @Description("Controller confirms successful update with FINISHED status on a action that is on canceling. "
            + "Reason: The decision to ignore the cancellation is in fact up to the controller.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerConfirmsUpdateWithFinishedAndIgnoresCancellationWithThat() {
        final Long actionId = createTargetAndAssignDs();
        deploymentManagement.cancelAction(actionId);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.CANCELING,
                DeviceActionStatus.FINISHED_NOT_EXECUTED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(3);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(3);
    }

    @Test
    @Description("Update server rejects cancellation feedback if action is not in CANCELING state.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void cancellationFeedbackRejectedIfActionIsNotInCanceling() {
        final Long actionId = createTargetAndAssignDs();

        assertThatExceptionOfType(CancelActionNotAllowedException.class)
                .as("Expected " + CancelActionNotAllowedException.class.getName())
                .isThrownBy(() -> controllerManagement.addCancelActionStatus(
                        entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_SUCCESS)));

        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.RUNNING,
                DeviceActionStatus.RUNNING, true);

        assertThat(actionStatusRepository.count()).isEqualTo(1);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(1);
    }

    @Test
    @Description("Controller confirms action cancellation with FINISHED status.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerConfirmsActionCancellationWithFinished() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_SUCCESS));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.CANCELED,
                DeviceActionStatus.FINISHED_SUCCESS, false);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller confirms action cancellation with FINISHED status.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerConfirmsActionCancellationWithCanceled() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.CANCELED));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.CANCELED,
                DeviceActionStatus.CANCELED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller rejects action cancellation with CANCEL_REJECTED status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerRejectsActionCancellationWithReject() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement.addCancelActionStatus(
                entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.CANCELED_REJECT));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.RUNNING,
                DeviceActionStatus.CANCELED_REJECT, true);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Test
    @Description("Controller rejects action cancellation with ERROR status. Action goes back to RUNNING status as it expects "
            + "that the controller will continue the original update.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerRejectsActionCancellationWithError() {
        final Long actionId = createTargetAndAssignDs();

        deploymentManagement.cancelAction(actionId);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.CANCELING, true);

        simulateIntermediateStatusOnCancellation(actionId);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.ERROR_RESPONSE_CODE));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.RUNNING,
                DeviceActionStatus.ERROR_RESPONSE_CODE, true);

        assertThat(actionStatusRepository.count()).isEqualTo(7);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(7);
    }

    @Step
    private Long createTargetAndAssignDs() {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        testdataFactory.createTarget();
        assignDistributionSet(dsId, DEFAULT_CONTROLLER_ID);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        return deploymentManagement.findActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID).getContent().get(0).getId();
    }

    @Step
    private Long assignDs(final Long dsId, final String defaultControllerId, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        assignDistributionSet(dsId, defaultControllerId, userAcceptanceRequired);
        assertThat(targetManagement.getByControllerID(defaultControllerId).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);

        final Long id = deploymentManagement.findActiveActionsByTarget(PAGE, defaultControllerId).getContent().get(0)
                .getId();
        assertThat(id).isNotNull();
        return id;
    }

    @Step
    private void simulateIntermediateStatusOnCancellation(final Long actionId) {
        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.RUNNING));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.RUNNING, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.DOWNLOAD_IN_PROGRESS));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.DOWNLOAD_IN_PROGRESS, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.DOWNLOAD_IN_PROGRESS));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.DOWNLOAD_IN_PROGRESS, true);

        controllerManagement
                .addCancelActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.DD_SENT));
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.CANCELING,
                DeviceActionStatus.DD_SENT, true);

    }

    @Step
    private void simulateIntermediateStatusOnUpdate(final Long actionId) {
        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.RUNNING);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.DOWNLOAD_IN_PROGRESS);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.DOWNLOAD_COMPLETED);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.DD_SENT);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.CANCELED_ACCEPT);
    }

    private void addUpdateActionStatusAndAssert(final Long actionId, final DeviceActionStatus actionStatus) {
        addUpdateActionStatusAndAssert(actionId, actionStatus, null);
    }

    private void addUpdateActionStatusAndAssert(final Long actionId, final DeviceActionStatus actionStatus,
                                                final Integer code) {
        final ActionStatusCreate status = entityFactory.actionStatus().create(actionId).status(actionStatus);
        if (code != null) {
            status.code(code.intValue());
        }
        final DdiDownload ddiDownload = new DdiDownload(70, new DdiPackage(10,10));
        final DdiStatus ddiStatus = new DdiStatus(DdiStatus.ExecutionStatus.valueOf(actionStatus.name()),
                ddiDownload,
                code,
                Collections.singletonList("testMessage"),
                System.currentTimeMillis());
        controllerManagement
                .addUpdateActionStatus(status, ddiStatus);
        DeviceActionStatus currentActionStatus = actionRepository.findById(actionId).get().getStatus();
        DeviceActionStatus expectedMainStatus =
                (actionStatus == DeviceActionStatus.CANCELED && !currentActionStatus.equals(DeviceActionStatus.CANCELED)) ? DeviceActionStatus.CANCELED
                        : (actionStatus == DeviceActionStatus.DD_SENT && !currentActionStatus.equals(DeviceActionStatus.CANCELED)) ? DeviceActionStatus.DD_SENT
                        : currentActionStatus;
        boolean actionActive = !currentActionStatus.equals(DeviceActionStatus.CANCELED);
        assertActionStatus(
                actionId,
                DEFAULT_CONTROLLER_ID,
                TargetUpdateStatus.PENDING,
                expectedMainStatus,
                actionStatus,
                actionActive
        );

    }

    private void assertActionStatus(final Long actionId, final String controllerId,
                                    final TargetUpdateStatus expectedTargetUpdateStatus, final DeviceActionStatus expectedActionActionStatus,
                                    final DeviceActionStatus expectedActionStatus, final boolean actionActive) {
        final TargetUpdateStatus targetStatus = targetManagement.getByControllerID(controllerId).get()
                .getUpdateStatus();
        assertThat(targetStatus).isEqualTo(expectedTargetUpdateStatus);
        final Action action = actionRepository.findById(actionId).get();
        assertThat(action.getStatus()).isEqualTo(expectedActionActionStatus);
        assertThat(action.isActive()).isEqualTo(actionActive);
        final List<ActionStatus> actionStatusList = controllerManagement.findActionStatusByAction(PAGE, actionId)
                .getContent();
        assertThat(actionStatusList.get(actionStatusList.size() - 1).getStatus()).isEqualTo(expectedActionStatus);
        if (actionActive) {
            assertThat(controllerManagement.findActiveActionWithHighestWeight(controllerId).get().getId())
                    .isEqualTo(actionId);
        }
    }


    @Test
    @Description("Update controller throws exception which does not exist")
    @ExpectEvents(@Expect(type = TargetCreatedEvent.class, count = 0))
    void findOrRegisterTargetIfItDoesNotExistFalse() {
        ddiSecurityProperties.setCreateTargetOnRun(false);
        final Vehicle vehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when updating target")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId()));
        assertThat(targetRepository.count()).as("target should not exist").isEqualTo(0L);
    }

    @Test
    @Description("Register a controller with name which does not exist and update its name")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 0)})
    void findOrRegisterTargetIfItDoesNotExistWithName() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final Vehicle vehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId());
        final Target sameTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA,
                NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId());
        assertThat(target.getId()).as("Target should be the equals").isEqualTo(sameTarget.getId());
        assertThat(target.getName()).as("Target name should be same").isEqualTo(sameTarget.getName());
        assertThat(targetRepository.count()).as("Only 1 target should be registered").isEqualTo(1L);
    }

    @Test
    @Description("Tries to register a target with an invalid controller id")
    void findOrRegisterTargetIfItDoesNotExistThrowsExceptionForInvalidControllerIdParam() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        testdataFactory.createVehicle("Dcross").getId();
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with null as controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(null, null, null, null));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with empty controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("", null, null, null));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with empty controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(" ", null, null, null));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("register target with too long controllerId should fail")
                .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(
                        RandomStringUtils.randomAlphabetic(Target.CONTROLLER_ID_MAX_SIZE + 1),
                        RandomStringUtils.randomAlphabetic(Target.CONTROLLER_ID_MAX_SIZE + 1),
                        RandomStringUtils.randomAlphabetic(Target.CONTROLLER_ID_MAX_SIZE + 1),
                        1L));
    }

    @Test
    @Description("Register a controller which does not exist, when a ConcurrencyFailureException is raised, the "
            + "exception is rethrown after max retries")
    void findOrRegisterTargetIfItDoesNotExistThrowsExceptionAfterMaxRetries() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        when(mockTargetRepository.findOne((Specification<JpaTarget>) any())).thenThrow(ConcurrencyFailureException.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        try {
            assertThatExceptionOfType(ConcurrencyFailureException.class)
                    .as("Expected an ConcurrencyFailureException to be thrown!")
                    .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), 1L));

            verify(mockTargetRepository, times(TX_RT_MAX)).findOne((Specification<JpaTarget>) any());
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Register a controller which does not exist, when a ConcurrencyFailureException is raised, the "
            + "exception is not rethrown when the max retries are not yet reached")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1)})
    void findOrRegisterTargetIfItDoesNotExistDoesNotThrowExceptionBeforeMaxRetries() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);
        final Target target = testdataFactory.createTarget();

        when(mockTargetRepository.findOne((Specification<JpaTarget>) any())).thenThrow(ConcurrencyFailureException.class)
                .thenThrow(ConcurrencyFailureException.class).thenReturn(Optional.of((JpaTarget) target));
        when(mockTargetRepository.save(any())).thenReturn(target);

        try {
            final Target targetFromControllerManagement = controllerManagement
                    .findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), target.getControllerId(),
                            target.getControllerId(), 1L);
            verify(mockTargetRepository, times(3)).findOne((Specification<JpaTarget>) any());
            verify(mockTargetRepository, times(1)).save(any());
            assertThat(target).isEqualTo(targetFromControllerManagement);
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Register a controller which does not exist, then update the controller twice, first time by providing a name property and second time without a new name")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 3), @Expect(type = TargetUpdatedEvent.class, count = 0)})
    void findOrRegisterTargetIfItDoesNotExistDoesUpdateNameOnExistingTargetProperly() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final String controllerId = "12345";
        final String targetName = "UpdatedName";
        final String targetSerialNumber = "serialNumber";
        final Vehicle newVehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        final Target newTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, controllerId, targetSerialNumber, newVehicle.getId());
        assertThat(newTarget.getName()).isEqualTo(controllerId);

        final Target firstTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                targetName, targetSerialNumber, newVehicle.getId());
        assertThat(firstTimeUpdatedTarget.getName()).isEqualTo(controllerId);

        // Name should not change to default (name=targetId) if target is
        // updated without new name provided
        final Target secondTimeUpdatedTarget = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                targetName, targetSerialNumber, newVehicle.getId());
        assertThat(secondTimeUpdatedTarget.getName()).isEqualTo(controllerId);
    }

    @Test
    @Description("Register a controller which does not exist, if a EntityAlreadyExistsException is raised, the "
            + "exception is rethrown and no further retries will be attempted")
    void findOrRegisterTargetIfItDoesNotExistDoesntRetryWhenEntityAlreadyExistsException() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        testdataFactory.createVehicle("Dcross").getId();
        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        when(mockTargetRepository.findOne((Specification<JpaTarget>) any())).thenReturn(Optional.empty());
        when(mockTargetRepository.save(any())).thenThrow(EntityAlreadyExistsException.class);
    }

    @Test
    @Description("Retry is aborted when an unchecked exception is thrown and the exception should also be "
            + "rethrown")
    void recoverFindOrRegisterTargetIfItDoesNotExistIsNotInvokedForOtherExceptions() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final TargetRepository mockTargetRepository = Mockito.mock(TargetRepository.class);
        ((JpaControllerManagement) controllerManagement).setTargetRepository(mockTargetRepository);

        when(mockTargetRepository.findOne((Specification<JpaTarget>) any())).thenThrow(RuntimeException.class);

        try {
            assertThatExceptionOfType(RuntimeException.class).as("Expected a RuntimeException to be thrown!")
                    .isThrownBy(() -> controllerManagement.findOrRegisterTargetIfItDoesNotExist("aControllerId",
                            NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), 1L));
            verify(mockTargetRepository, times(1)).findOne((Specification<JpaTarget>) any());
        } finally {
            // revert
            ((JpaControllerManagement) controllerManagement).setTargetRepository(targetRepository);
        }
    }

    @Test
    @Description("Verify that targetVisible metadata is returned from repository")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 6),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void findTargetVisibleMetaDataBySoftwareModuleId() throws InterruptedException {
        final DistributionSet set = testdataFactory.createDistributionSet();
        testdataFactory.addSoftwareModuleMetadata(set);

        final Map<Long, List<SoftwareModuleInfo>> result = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(set.getModules().stream()
                        .map(SoftwareModule::getId).collect(Collectors.toList()));

        assertThat(result).hasSize(3);
        result.forEach((key, value) -> assertThat(value).hasSize(1));
    }

    @Test
    @Description("Verify that controller registration does not result in a TargetPollEvent if feature is disabled")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 0)})
    void targetPollEventNotSendIfDisabled() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        repositoryProperties.setPublishTargetPollEvent(false);
        final Vehicle vehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        assertThat(controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId())).isNotNull();
        repositoryProperties.setPublishTargetPollEvent(true);
    }

    @Test
    @Description("Controller tries to finish an update process after it has been finished by an error action status.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 2),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void tryToFinishWithErrorUpdateProcessMoreThanOnce() {
        final Long actionId = createTargetAndAssignDs();

        // test and verify
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.RUNNING), null);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.PENDING, DeviceActionStatus.RUNNING,
                DeviceActionStatus.RUNNING, true);

        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.ERROR_RESPONSE_CODE), null);
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, DeviceActionStatus.ERROR_RESPONSE_CODE,
                DeviceActionStatus.ERROR_RESPONSE_CODE, false);

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, DeviceActionStatus.ERROR_RESPONSE_CODE,
                DeviceActionStatus.FINISHED_NOT_EXECUTED, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedback and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.ERROR, DeviceActionStatus.ERROR_RESPONSE_CODE,
                DeviceActionStatus.FINISHED_NOT_EXECUTED, false);

        assertThat(actionStatusRepository.count()).isEqualTo(5);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(5);

    }

    @Test
    @Description("Controller tries to finish an update process after it has been finished by an FINISHED action status.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 3),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void tryToFinishUpdateProcessMoreThanOnce() {
        Long actionId = prepareFinishedUpdate().getId();

        // try with disabled late feedback
        repositoryProperties.setRejectActionStatusForClosedAction(true);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.FINISHED_FAILURE,
                DeviceActionStatus.FINISHED_NOT_EXECUTED, false);

        // try with enabled late feedback - should not make a difference as it
        // only allows intermediate feedback and not multiple close
        repositoryProperties.setRejectActionStatusForClosedAction(false);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_SUCCESS), null);

        // test
        assertActionStatus(actionId, DEFAULT_CONTROLLER_ID, TargetUpdateStatus.IN_SYNC, DeviceActionStatus.FINISHED_SUCCESS,
                DeviceActionStatus.FINISHED_SUCCESS, false);

        assertThat(actionStatusRepository.count()).isEqualTo(6);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, actionId).getNumberOfElements()).isEqualTo(6);

    }

    @Test
    @Description("Controller tries to send an update feedback after it has been finished which is reject as the repository is "
            + "configured to reject that.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void sendUpdatesForFinishUpdateProcessDroppedIfDisabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(true);

        Action action = prepareFinishedUpdate();

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.RUNNING), null);

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        assertThat(actionRepository.findById(action.getId()))
                .hasValueSatisfying(a -> assertThat(a.getStatus()).isEqualTo(DeviceActionStatus.FINISHED_FAILURE));
        assertThat(actionStatusRepository.count()).isEqualTo(5);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, action.getId()).getNumberOfElements())
                .isEqualTo(5);
    }

    @Test
    @Description("Controller tries to send an update feedback after it has been finished which is accepted as the repository is "
            + "configured to accept them.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1),
            @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void sendUpdatesForFinishUpdateProcessAcceptedIfEnabled() {
        repositoryProperties.setRejectActionStatusForClosedAction(false);

        Action action = prepareFinishedUpdate();
        action = controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.RUNNING), null);

        // nothing changed as "feedback after close" is disabled
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.IN_SYNC);

        // however, additional action status has been stored
        assertThat(actionStatusRepository.findAll(PAGE).getNumberOfElements()).isEqualTo(5);
        assertThat(controllerManagement.findActionStatusByAction(PAGE, action.getId()).getNumberOfElements())
                .isEqualTo(5);
    }

    @Test
    @Description("Ensures that target attribute update is reflected by the repository.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 3)})
    void updateTargetAttributes() throws Exception {
        final String controllerId = "test123";
        final Target target = testdataFactory.createTarget(controllerId);

        WithSpringAuthorityRule.runAs(WithSpringAuthorityRule.withController(PRINCIPLE, CONTROLLER_ROLE_ANONYMOUS), () -> {
            addAttributeAndVerify(controllerId);
            addSecondAttributeAndVerify(controllerId);
            updateAttributeAndVerify(controllerId);
            return null;
        });

        // verify that audit information has not changed
        final Target targetVerify = targetManagement.getByControllerID(controllerId).get();
        assertThat(targetVerify.getCreatedBy()).isEqualTo(target.getCreatedBy());
        assertThat(targetVerify.getCreatedAt()).isEqualTo(target.getCreatedAt());
        assertThat(targetVerify.getLastModifiedBy()).isEqualTo(target.getLastModifiedBy());
        assertThat(targetVerify.getLastModifiedAt()).isEqualTo(target.getLastModifiedAt());
    }

    @Step
    private void addAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(1);
        testData.put(NAME + 2, TEST_DATA_1);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, testData, null, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as(DESCRIPTION)
                .isEqualTo(testData);
    }

    @Step
    private void addSecondAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(2);
        testData.put(NAME + 1, TEST_DATA_20);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, testData, null, null);

        testData.put(NAME + 2, TEST_DATA_1);
        assertThat(targetManagement.getControllerAttributes(controllerId)).as(DESCRIPTION)
                .isEqualTo(testData);
    }

    @Step
    private void updateAttributeAndVerify(final String controllerId) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(2);
        testData.put(NAME + 2, TEST_DATA_12);

        controllerManagement.updateControllerAttributesWithSoftware(controllerId, testData, null, null);

        testData.put(NAME + 1, TEST_DATA_20);
        assertThat(targetManagement.getControllerAttributes(controllerId)).as(DESCRIPTION)
                .isEqualTo(testData);
    }

    @Test
    @Description("Ensures that target attributes can be updated using different update modes.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 4)})
    void updateTargetAttributesWithDifferentUpdateModes() {

        final String controllerId = "testCtrl";
        testdataFactory.createTarget(controllerId);

        // no update mode
        updateTargetAttributesWithoutUpdateMode(controllerId);

        // update mode REPLACE
        updateTargetAttributesWithUpdateModeReplace(controllerId);

        // update mode MERGE
        updateTargetAttributesWithUpdateModeMerge(controllerId);

        // update mode REMOVE
        updateTargetAttributesWithUpdateModeRemove(controllerId);

    }

    @Step
    private void updateTargetAttributesWithUpdateModeRemove(final String controllerId) {

        final int previousSize = targetManagement.getControllerAttributes(controllerId).size();

        // update the attributes using update mode REMOVE
        final Map<String, String> removeAttributes = new HashMap<>();
        removeAttributes.put(K1, FOO);
        removeAttributes.put(K3, BAR);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, removeAttributes, null, UpdateMode.REMOVE);

        // verify attribute removal
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSize(previousSize - 2);
        assertThat(updatedAttributes).doesNotContainKeys(K1, K3);

    }

    @Step
    private void updateTargetAttributesWithUpdateModeMerge(final String controllerId) {
        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode MERGE
        final Map<String, String> mergeAttributes = new HashMap<>();
        mergeAttributes.put(K1, V1_MODIFIED_AGAIN);
        mergeAttributes.put(K4, V4);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, mergeAttributes, null, UpdateMode.MERGE);

        // verify attribute merge
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSize(4);
        assertThat(updatedAttributes).containsAllEntriesOf(mergeAttributes);
        assertThat(updatedAttributes.get(K1)).isEqualTo(V1_MODIFIED_AGAIN);
        attributes.keySet().forEach(assertThat(updatedAttributes)::containsKey);
    }

    @Step
    private void updateTargetAttributesWithUpdateModeReplace(final String controllerId) {

        // get the current attributes
        final HashMap<String, String> attributes = new HashMap<>(
                targetManagement.getControllerAttributes(controllerId));

        // update the attributes using update mode REPLACE
        final Map<String, String> replacementAttributes = new HashMap<>();
        replacementAttributes.put(K1, V1_MODIFIED);
        replacementAttributes.put(K2, V2);
        replacementAttributes.put(K3, V3);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, replacementAttributes, null, UpdateMode.REPLACE);

        // verify attribute replacement
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSameSizeAs(replacementAttributes);
        assertThat(updatedAttributes).containsAllEntriesOf(replacementAttributes);
        assertThat(updatedAttributes.get(K1)).isEqualTo(V1_MODIFIED);
        attributes.entrySet().forEach(assertThat(updatedAttributes)::doesNotContain);
    }

    @Step
    private void updateTargetAttributesWithoutUpdateMode(final String controllerId) {

        // set the initial attributes
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(K0, V0);
        attributes.put(K1, V1);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, attributes, null, null);

        // verify initial attributes
        final Map<String, String> updatedAttributes = targetManagement.getControllerAttributes(controllerId);
        assertThat(updatedAttributes).hasSameSizeAs(attributes);
        assertThat(updatedAttributes).containsAllEntriesOf(attributes);
    }

    @Test
    @Description("Ensures that target attribute update fails if quota hits.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 2)})
    void updateTargetAttributesFailsIfTooManyEntries() throws Exception {
        final String controllerId = "test123";
        final int allowedAttributes = quotaManagement.getMaxAttributeEntriesPerTarget();
        testdataFactory.createTarget(controllerId);

        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> WithSpringAuthorityRule
                .runAs(WithSpringAuthorityRule.withController(PRINCIPLE, CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, allowedAttributes + 1, "key", "value");
                    return null;
                })).withMessageContaining("" + allowedAttributes);

        // verify that no attributes have been written
        assertThat(targetManagement.getControllerAttributes(controllerId)).isEmpty();

        // Write allowed number of attributes twice with same key should result
        // in update but work
        WithSpringAuthorityRule.runAs(WithSpringAuthorityRule.withController(PRINCIPLE, CONTROLLER_ROLE_ANONYMOUS), () -> {
            writeAttributes(controllerId, allowedAttributes, "key", "value1");
            writeAttributes(controllerId, allowedAttributes, "key", "value2");
            return null;
        });
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

        // Now rite one more
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> WithSpringAuthorityRule
                .runAs(WithSpringAuthorityRule.withController(PRINCIPLE, CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeAttributes(controllerId, 1, "additional", "value1");
                    return null;
                })).withMessageContaining("" + allowedAttributes);
        assertThat(targetManagement.getControllerAttributes(controllerId)).hasSize(10);

    }

    private void writeAttributes(final String controllerId, final int allowedAttributes, final String keyPrefix,
                                 final String valuePrefix) {
        final Map<String, String> testData = Maps.newHashMapWithExpectedSize(allowedAttributes);
        for (int i = 0; i < allowedAttributes; i++) {
            testData.put(keyPrefix + i, valuePrefix);
        }
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, testData, null, null);
    }

    @Test
    @Description("Checks if invalid values of attribute-key and attribute-value are handled correctly")
    void updateTargetAttributesFailsForInvalidAttributes() {
        final String controllerId = "targetId123";
        testdataFactory.createTarget(controllerId);

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributesWithSoftware(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_VALID), null, null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key too long and value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributesWithSoftware(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_TOO_LONG, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG), null, null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with value too long should not be created")
                .isThrownBy(() -> controllerManagement.updateControllerAttributesWithSoftware(controllerId,
                        Collections.singletonMap(TargetTestData.ATTRIBUTE_KEY_VALID, TargetTestData.ATTRIBUTE_VALUE_TOO_LONG), null, null));

        assertThatExceptionOfType(InvalidTargetAttributeException.class)
                .as("Attribute with key NULL should not be created").isThrownBy(() -> controllerManagement
                        .updateControllerAttributesWithSoftware(controllerId,
                                Collections.singletonMap(null, TargetTestData.ATTRIBUTE_VALUE_VALID), null, null));
    }

    @Test
    @Description("Controller providing status entries fails if providing more than permitted by quota.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void controllerProvidesIntermediateFeedbackFailsIfQuotaHit() {
        final int allowStatusEntries = 10;
        final Long actionId = createTargetAndAssignDs();

        // Fails as one entry is already in there from the assignment
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(() -> WithSpringAuthorityRule
                .runAs(WithSpringAuthorityRule.withController(PRINCIPLE, CONTROLLER_ROLE_ANONYMOUS), () -> {
                    writeStatus(actionId, allowStatusEntries);
                    return null;
                })).withMessageContaining("" + allowStatusEntries);

    }

    private void writeStatus(final Long actionId, final int allowedStatusEntries) {
        for (int i = 0; i < allowedStatusEntries; i++) {
            controllerManagement.addInformationalActionStatus(
                    entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.RUNNING).message("test" + i));
        }
    }

    @Test
    @Description("Test to verify the storage and retrieval of action history.")
    void findMessagesByActionStatusId() throws InterruptedException {
        final DistributionSet testDs = testdataFactory.createDistributionSet("1");
        final List<Target> testTarget = testdataFactory.createTargets(1);

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(testDs, testTarget));
        TestdataFactory.waitForSeconds(1);
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(DeviceActionStatus.RUNNING).occurredAt(Instant.now().getEpochSecond())
                .messages(Lists.newArrayList("proceeding message 1")), null);
        TestdataFactory.waitForSeconds(1);
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId)
                .status(DeviceActionStatus.RUNNING).occurredAt(Instant.now().getEpochSecond())
                .messages(Lists.newArrayList("proceeding message 2")), null);
        final List<String> messages = controllerManagement.getActionHistoryMessages(actionId, 2);

        assertThat(deploymentManagement.findActionStatusByAction(PAGE, actionId).getTotalElements())
                .as("Two action-states in total").isEqualTo(3L);
        assertThat(messages.get(0)).as("Message of action-status").isEqualTo("proceeding message 2");
        assertThat(messages.get(1)).as("Message of action-status").isEqualTo("proceeding message 1");
    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of status entries per action is enforced.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = ArtifactsCreatedEvent.class, count = 6)})
    void addActionStatusUpdatesUntilQuotaIsExceeded() {

        // any distribution set assignment causes 1 status entity to be created
        final int maxStatusEntries = quotaManagement.getMaxStatusEntriesPerAction() - 1;

        // test for informational status
        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet(DS1), testdataFactory.createTargets(1, T1)));
        assertThat(actionId1).isNotNull();
        // test for update status (and mixed case)
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(
                testdataFactory.createDistributionSet("ds2"), testdataFactory.createTargets(1, "t2")));
        assertThat(actionId2).isNotEqualTo(actionId1);

    }

    @Test
    @Description("Verifies that the quota specifying the maximum number of messages per action status is enforced.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void createActionStatusWithTooManyMessages() {

        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(testdataFactory.createDistributionSet(DS1), testdataFactory.createTargets(1)));
        assertThat(actionId).isNotNull();

        final List<String> messages = Lists.newArrayList();
        IntStream.range(0, maxMessages).forEach(i -> messages.add(i, MSG));

    }

    @Test
    @Description("Verifies that quota is enforced for UpdateActionStatus events for FORCED assignments.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void quotaExceededExceptionWhenControllerReportsTooManyUpdateActionStatusMessagesForForced() {
        final int maxMessages = quotaManagement.getMaxMessagesPerActionStatus();
        final Long actionId = createTargetAndAssignDs();
        assertThat(actionId).isNotNull();

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many DOWNLOADED updateActionStatus updates").isThrownBy(
                        () -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement.addUpdateActionStatus(
                                entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.DOWNLOAD_COMPLETED), null)));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many ERROR updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.ERROR_RESPONSE_CODE), null)));

        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .as("No QuotaExceededException thrown for too many FINISHED updateActionStatus updates")
                .isThrownBy(() -> IntStream.range(0, maxMessages).forEach(i -> controllerManagement
                        .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null)));
    }

    @Test
    @Description("Verify that the attaching externalRef to an action is properly stored")
    void updatedExternalRefOnActionIsReallyUpdated() {
        final List<String> allExternalRef = new ArrayList<>();
        final List<Long> allActionId = new ArrayList<>();
        final int numberOfActions = 3;
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        for (int i = 0; i < numberOfActions; i++) {
            final String knownControllerId = CONTROLLER_ID + i;
            final String knownExternalRef = "externalRefId" + i;

            testdataFactory.createTarget(knownControllerId, knownControllerId, knownControllerId, testdataFactory.createVehicle(knownControllerId).getId(),knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(),
                    knownControllerId);
            final Long actionId = getFirstAssignedActionId(assignmentResult);
            controllerManagement.updateActionExternalRef(actionId, knownExternalRef);

            allExternalRef.add(knownExternalRef);
            allActionId.add(actionId);
        }

        final List<Action> foundAction = controllerManagement.getActiveActionsByExternalRef(allExternalRef);
        assertThat(foundAction).isNotNull();
        for (int i = 0; i < numberOfActions; i++) {
            assertThat(foundAction.get(i).getId()).isEqualTo(allActionId.get(i));
        }
    }

    @Test
    @Description("Verify that getting a single action using externalRef works")
    void getActionUsingSingleExternalRef() {

        final String knownExternalRef = "externalRefId";
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();

        // GIVEN
        testdataFactory.createTarget(CONTROLLER_ID);
        final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(knownDistributionSet.getId(),
                CONTROLLER_ID);
        final Long actionId = getFirstAssignedActionId(assignmentResult);
        controllerManagement.updateActionExternalRef(actionId, knownExternalRef);

        // WHEN
        final Optional<Action> foundAction = controllerManagement.getActionByExternalRef(knownExternalRef);

        // THEN
        assertThat(foundAction).isPresent();
        assertThat(foundAction.get().getId()).isEqualTo(actionId);
    }

    @Test
    @Description("Verify that a null externalRef cannot be assigned to an action")
    void externalRefCannotBeNull() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("No ConstraintViolationException thrown when a null externalRef was set on an action")
                .isThrownBy(() -> controllerManagement.updateActionExternalRef(1L, null));
    }

    @Step
    private void finishDownloadOnlyUpdateAndSendUpdateActionStatus(final Long actionId, final DeviceActionStatus status) {
        // finishing action
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.DOWNLOAD_COMPLETED), null);

        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(status), null);
        assertThat(actionRepository.activeActionExistsForControllerId(DEFAULT_CONTROLLER_ID)).isFalse();
    }

    @Step
    private void addUpdateActionStatus(final Long actionId, final String controllerId, final DeviceActionStatus actionStatus) {
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(actionId).status(actionStatus), null);
        assertActionStatus(actionId, controllerId, TargetUpdateStatus.IN_SYNC, actionStatus, actionStatus, false);
    }

    @Test
    @Description("Actions are exposed according to thier weight in multi assignment mode.")
    void actionsAreExposedAccordingToTheirWeight() {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long actionWeightNull = assignDistributionSet(ds.getId(), targetId).getAssignedEntity().get(0).getId();
        enableMultiAssignments();
        final Long actionWeight500old = assignDistributionSet(ds.getId(), targetId, 500).getAssignedEntity().get(0)
                .getId();
        final Long actionWeight500new = assignDistributionSet(ds.getId(), targetId, 500).getAssignedEntity().get(0)
                .getId();
        final Long actionWeight1000 = assignDistributionSet(ds.getId(), targetId, 1000).getAssignedEntity().get(0)
                .getId();

        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeightNull);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeightNull).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight1000);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight1000).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight500old);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight500old).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId).get().getId())
                .isEqualTo(actionWeight500new);
        controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(actionWeight500new).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assertThat(controllerManagement.findActiveActionWithHighestWeight(targetId)).isEmpty();
    }

    @Test
    @Description("Delete a target on requested target deletion from client side")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1), @Expect(type = TargetDeletedEvent.class, count = 1)})
    void deleteTargetWithValidThingId() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final Vehicle vehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId());
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        controllerManagement.deleteExistingTarget(target.getControllerId());

        assertThat(targetRepository.count()).as("target should not exist anymore").isEqualTo(0L);
    }

    @Test
    @Description("Delete a target with a non existing thingId")
    @ExpectEvents({@Expect(type = TargetDeletedEvent.class, count = 0)})
    void deleteTargetWithInvalidThingId() {
        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget("BB"));
        assertThat(targetRepository.count()).as("target should not exist").isEqualTo(0L);
    }

    @Test
    @Description("Delete a target after it has been deleted already")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1), @Expect(type = TargetDeletedEvent.class, count = 1)})
    void deleteTargetAfterItWasDeleted() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final Vehicle vehicle = testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt());
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(AA, NAME + testdataFactory.getRandomInt(), CONTROLLER_ID + testdataFactory.getRandomInt(), vehicle.getId());
        assertThat(target).as("target should not be null").isNotNull();
        assertThat(targetRepository.count()).as("target exists and is ready for deletion").isEqualTo(1L);

        controllerManagement.deleteExistingTarget(target.getControllerId());
        assertThat(targetRepository.count()).as("target should not exist anymore").isEqualTo(0L);

        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("No EntityNotFoundException thrown when deleting a non-existing target")
                .isThrownBy(() -> controllerManagement.deleteExistingTarget(target.getControllerId()));
    }

    @Test
    @Description("When action status code is provided in feedback it is also stored in the action field lastActionStatusCode")
    void lastActionStatusCodeIsSet() {
        final Long actionId = createTargetAndAssignDs();

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.RUNNING, 10);
        assertLastActionStatusCodeInAction(actionId, 10);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.RUNNING);
        assertLastActionStatusCodeInAction(actionId, null);

        addUpdateActionStatusAndAssert(actionId, DeviceActionStatus.RUNNING, 20);
        assertLastActionStatusCodeInAction(actionId, 20);

    }

    @Test
    @Description("When Execution is ERC and error code is empty in feedback then it throws exception")
    void givenExecutionIsErcAndErrorCodeIsEmptyWhenAddFeedbackThenThrowsException() {
        Target target = testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(200, details, ExecutionType.ERC, errorCode, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> controllerManagement.addFeedbackByControllerId(target, feedback))
                .withMessageContaining("ErrorCode is mandatory when execution is ERC");
    }

    @Test
    @Description("When Execution is ERC and error code is null in feedback then it throws exception")
    void givenExecutionIsErcAndErrorCodeIsNullWhenAddFeedbackThenThrowsException() {
        Target target = testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(200, details, ExecutionType.ERC, null, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        assertThatExceptionOfType(ValidationException.class)
                .isThrownBy(() -> controllerManagement.addFeedbackByControllerId(target, feedback))
                .withMessageContaining("ErrorCode is mandatory when execution is ERC");
    }

    private void assertLastActionStatusCodeInAction(final Long actionId, final Integer expectedLastActionStatusCode) {
        final Optional<Action> action = actionRepository.getActionById(actionId, true);
        assertThat(action).isPresent();
        assertThat(action.get().getLastActionStatusCode()).isEqualTo(Optional.ofNullable(expectedLastActionStatusCode));
    }
}
