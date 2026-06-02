package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MgmtRolloutPauseAndResumeTest extends AbstractManagementRolloutApiIntegrationTest {

    @Test
    @Description("Given single valid device action state, when pausing a device action, then it will update the rollout status to Pausing")
    void givenSingleValidDeviceActionStateWhenPausingDeviceActionThenRolloutGoesToPausing() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        Optional<JpaRollout> updatedrollout = rolloutRepository.getRolloutById(rollout.getId());
        assertEquals(RolloutStatus.PAUSING, updatedrollout.get().getStatus());
    }

    @Test
    @Description("Testing that pausing the rollout switches the state to paused")
    void pausingRolloutSwitchesIntoPausedState() throws Exception {

        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        rolloutHandler.handleAll();

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.PAUSED, updatedRollout.getStatus());
    }

    @Test
    @Description("Given Invalid Rollout status, when pausing the rollout, then return Bad request")
    void givenInvalidRolloutStateWhenPauseRolloutThenBadRequest() throws Exception {

        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELING, updatedRollout.getStatus());

    }

    @Test
    @Description("Testing that resuming the rollout switches the state to running")
    void givenRolloutRunningWhenResumingRolloutThenSwitchesToRunningState() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        // pausing rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/action/pause", TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        rolloutHandler.handleAll();

        // resume rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/action/resume", TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        rolloutHandler.handleAll();

        // check rollout is in running state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(STATUS, equalTo("running")));

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());
    }

    @Test
    @Description("Verify pause and resume functionality for rollout groups and actions associated with a rollout.")
    void pauseAndResumeRolloutWhenRolloutIsInRunningState() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.PAUSING, updatedRollout.getStatus());

        handleRollout();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        rolloutHandler.handleAll();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());
    }

    @Test
    @Description("Verify partial pause and resume functionality for rollout groups and actions associated with a rollout.")
    void verifyPartialPauseAndResumeForRollout() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        List<JpaRolloutGroup> groups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        List<JpaAction> actions = StreamSupport
                .stream(actionRepository.findAll().spliterator(), false)
                .toList();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Rollout updatedRolloutAPI = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.PAUSING, updatedRolloutAPI.getStatus());

        rolloutHandler.handleAll();

        List<JpaAction> updatedActions = StreamSupport
                .stream(actionRepository.findAll().spliterator(), false)
                .toList();
        List<JpaRolloutGroup> updatedGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.PAUSED, updatedRollout.getStatus());
        assertTrue(updatedGroups.stream().allMatch(group -> RolloutGroupStatus.PAUSED.equals(group.getStatus())));
        assertTrue(updatedActions.stream().allMatch(action -> DeviceActionStatus.PAUSED.equals(action.getStatus())));

        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        rolloutHandler.handleAll();

        List<JpaAction> updatedActionsForResume = StreamSupport
                .stream(actionRepository.findAll().spliterator(), false)
                .toList();
        List<JpaRolloutGroup> updatedGroupsForResume = rolloutGroupRepository.findByRolloutId(rollout.getId());
        Rollout updatedRolloutForResume = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.RUNNING, updatedRolloutForResume.getStatus());
        assertTrue(updatedGroupsForResume.stream().allMatch(group -> RolloutGroupStatus.RUNNING.equals(group.getStatus())));
        assertTrue(updatedActionsForResume.stream().allMatch(action -> DeviceActionStatus.RUNNING.equals(action.getStatus())));

    }

    @Test
    @Description("Given an valid device action state, when resuming a device action, then it will update the rollout status to Resuming.")
    void givenSingleValidDeviceActionStateWhenResumingDeviceActionThenRolloutGoesToResuming() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        Optional<JpaRollout> updatedrollout = rolloutRepository.getRolloutById(rollout.getId());
        assertEquals(RolloutStatus.RESUMING, updatedrollout.get().getStatus());
    }

    @Test
    @Description("Given a valid device action state, when pausing or resuming an action among multiple actions, then the action status is updated accordingly" )
    void givenValidStateWhenPausingOrResumingThenUpdateSingleActionStatus() throws Exception {
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 10, 20, 10) + "," +
                createBasicGroupJson("test-group02", 30, 20, 30) + "," +
                createBasicGroupJson("test-group03", 30, 40, 10) + "]" ;

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();


        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        List<JpaAction> actions = actionRepository.findByRolloutIdAndRolloutGroupId(rollout.getId(), rolloutGroup.getId(), true);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID1)
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        Rollout updatedRolloutForPause = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRolloutForPause.getStatus());

        final Optional<JpaAction> updatedFirstActionForPause = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID1, true);
        assertEquals(DeviceActionStatus.PAUSED, updatedFirstActionForPause.get().getStatus());

        final Optional<JpaAction> updatedSecondActionForPause = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID2, true);
        assertEquals(DeviceActionStatus.RUNNING, updatedSecondActionForPause.get().getStatus());

        final Optional<JpaAction> updatedThirdActionForPause = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID3, true);
        assertEquals(DeviceActionStatus.RUNNING, updatedThirdActionForPause.get().getStatus());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID1)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        Rollout updatedRolloutForResume = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSED, updatedRolloutForResume.getStatus());

        final Optional<JpaAction> updatedFirstActionForResume = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID1, true);
        assertEquals(DeviceActionStatus.RUNNING, updatedFirstActionForResume.get().getStatus());

        final Optional<JpaAction> updatedSecondActionForResume = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID2, true);
        assertEquals(DeviceActionStatus.PAUSED, updatedSecondActionForResume.get().getStatus());

        final Optional<JpaAction> updatedThirdActionForResume = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID3, true);
        assertEquals(DeviceActionStatus.PAUSED, updatedThirdActionForResume.get().getStatus());
    }

    @Test
    @Description("Given an invalid device action state, when pausing a device action, then it should return a bad request status.")
    void givenInvalidDeviceActionState_whenPausingDeviceAction_thenReturnsBadRequest() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true);
        actions.get().setStatus(DeviceActionStatus.FINISHED_SUCCESS);
        actionRepository.save(actions.get());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE,
                        equalTo("Device is not in RUNNING state")));

        final Optional<JpaAction> updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true);
        assertEquals(DeviceActionStatus.FINISHED_SUCCESS, updatedactions.get().getStatus());
    }

    @Test
    @Description("Given an invalid device action state, when Resuming a device action, then it should return a bad request status.")
    void givenInvalidDeviceActionState_whenResumingDeviceAction_thenReturnsBadRequest() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID), rollout, true);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final JpaAction actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true).get();
        actions.setStatus(DeviceActionStatus.FINISHING_SUCCESS);
        actionRepository.save(actions);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE,
                        equalTo("Device is not in PAUSED state")));

        final JpaAction updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true).get();
        assertEquals(DeviceActionStatus.FINISHING_SUCCESS, updatedactions.getStatus());
    }

    @Test
    @Description("Testing that resuming a rollout which is not started leads to bad request")
    void resumingNotStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        // resume not yet started rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/action/resume", TENANT_ID, rollout.getId()).header(SESSION_ID, SESSION_ID_HEADER)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.ROLLOUT_ILLEGAL_STATE.name()))).andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()));
    }

    @Test
    @Description("Given an rollout in ready status, when Pausing an action, then it should return bad request.")
    void givenRolloutInReadyStateWhenPausingAnActionThenReturnsBadRequest() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_PAUSE_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Device is not in RUNNING state")));
    }

    @Test
    @Description("Given an rollout in ready status, when Resuming an action, then it should return bad request.")
    void givenRolloutInReadyStateWhenResumingAnActionThenReturnsBadRequest() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_RESUME_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE,
                        equalTo("Device is not in PAUSED state")));
    }

    @Test
    @Description("Testing a rollout for pause and resume for transition to Running and Paused through scheduler")
    void givenRolloutInRunningWhenPauseAndResumeThroughSchedulerThenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Rollout pausedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSED, pausedRollout.getStatus());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rolloutGroup as PAUSED When rollout is in RUNNING then call API and scheduler and rollout status should be in PAUSING")
    void givenInvalidGroupStatusForRolloutWhenPausingSchedulerRunsThenRolloutInPausing() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(3, 3);

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.PAUSED);
        rolloutGroupRepository.save(firstGroup);

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        secondGroup.setStatus(RolloutGroupStatus.DRAFT);
        rolloutGroupRepository.save(secondGroup);

        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);
        thirdGroup.setStatus(RolloutGroupStatus.FREEZING);
        rolloutGroupRepository.save(thirdGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());//running -> pausing
        handleRollout();//pausing ->paused

        JpaRolloutGroup updatedRolloutGroup = rolloutGroupRepository.findById(firstGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.PAUSED, updatedRolloutGroup.getStatus());

        JpaRolloutGroup updatedSecondRolloutGroup = rolloutGroupRepository.findById(secondGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.DRAFT, updatedSecondRolloutGroup.getStatus());

        JpaRolloutGroup updatedThirdRolloutGroup = rolloutGroupRepository.findById(thirdGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.FREEZING, updatedThirdRolloutGroup.getStatus());

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rolloutGroup as PAUSED When rollout is in PAUSED then call API and scheduler and rollout status should be in RESUMING")
    void givenInvalidGroupStatusForRolloutWhenResumingSchedulerRunsThenRolloutInResuming() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(3, 3);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(firstGroup);

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        secondGroup.setStatus(RolloutGroupStatus.CANCELED);
        rolloutGroupRepository.save(secondGroup);

        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);
        thirdGroup.setStatus(RolloutGroupStatus.FINISHED);
        rolloutGroupRepository.save(thirdGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        JpaRolloutGroup updatedRolloutGroup = rolloutGroupRepository.findById(firstGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.RUNNING, updatedRolloutGroup.getStatus());

        updatedRolloutGroup = rolloutGroupRepository.findById(secondGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.CANCELED, updatedRolloutGroup.getStatus());

        updatedRolloutGroup = rolloutGroupRepository.findById(thirdGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.FINISHED, updatedRolloutGroup.getStatus());

        Rollout updatedRolloutAPI = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.RESUMING, updatedRolloutAPI.getStatus());
    }

    @Test
    @Description("Given Rollout and Rollout groups are in FINISHING state,when the Scheduler runs then the Rollout and Rollout groups should be in FINISHED state")
    void givenRolloutAndGroupsInFinishingStateWhenSchedulerRunsThenTransitionToFinished() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        List<JpaAction> actions = actionRepository.findByRolloutIdAndRolloutGroupId(rollout.getId(), rolloutGroup.getId(), true);
        for (JpaAction action : actions) {
            action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
            actionRepository.save(action);
        }

        handleRollout();//running to finishing
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHING, updatedRollout.getStatus());
        handleRollout();//finishing to finished

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHED, updatedRollout.getStatus());
    }

    @Test
    @Description("Given Invalid Rollout group status, when the scheduler runs then then Rollout will be in FINISHING state")
    void givenInvalidRolloutGroupsWhenSchedulerRunsThenRolloutInFinishingState() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        handleRollout();//running to finishing

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        rolloutGroup.setStatus(RolloutGroupStatus.CANCELED);
        rolloutGroupRepository.save(rolloutGroup);

        handleRollout();//finishing to finished

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHING, updatedRollout.getStatus());

    }

    @Test
    @Description("Given Rollout and Rollout groups are in Paused,Canceled,Finished,Queued state,when the Scheduler runs then the Rollout and Rollout groups should be in Running  state")
    void givenValidRolloutGroupsForRolloutWhenResumingSchedulerRunsThenReturnSuccess() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(4, 3);


        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(post(MgmtRestConstants.ROLLOUT_NEXT_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());//Running -> pausing

        handleRollout();//pausing ->paused


        Rollout pausedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSED, pausedRollout.getStatus());

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.PAUSED);
        rolloutGroupRepository.save(firstGroup);

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        secondGroup.setStatus(RolloutGroupStatus.FINISHED);
        rolloutGroupRepository.save(secondGroup);

        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);
        thirdGroup.setStatus(RolloutGroupStatus.QUEUED);
        rolloutGroupRepository.save(thirdGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());//paused -> resuming

        handleRollout();//resuming -> running

        RolloutGroup updatedRolloutGroup = rolloutGroupRepository.findById(firstGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.RUNNING, updatedRolloutGroup.getStatus());

        RolloutGroup updatedSecondRolloutGroup = rolloutGroupRepository.findById(secondGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.FINISHED, updatedSecondRolloutGroup.getStatus());

        RolloutGroup updatedThirdRolloutGroup = rolloutGroupRepository.findById(thirdGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.QUEUED, updatedThirdRolloutGroup.getStatus());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given Rollout in Running and valid Rollout groups as Canceled,Finished,Queued when scheduler Runs then Rollout should be in Paused state")
    void givenValidRolloutGroupsForRolloutWhenPausingSchedulerRunsThenReturnSuccess() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(4, 4);

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, updatedRollout.getStatus());


        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(firstGroup);

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        secondGroup.setStatus(RolloutGroupStatus.FINISHED);
        rolloutGroupRepository.save(secondGroup);

        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);
        thirdGroup.setStatus(RolloutGroupStatus.CANCELED);
        rolloutGroupRepository.save(thirdGroup);

        JpaRolloutGroup fourthGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(3);
        fourthGroup.setStatus(RolloutGroupStatus.QUEUED);
        rolloutGroupRepository.save(fourthGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());//running -> pausing
        handleRollout();//pausing ->paused

        JpaRolloutGroup updatedRolloutGroup = rolloutGroupRepository.findById(firstGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.PAUSED, updatedRolloutGroup.getStatus());

        JpaRolloutGroup updatedSecondRolloutGroup = rolloutGroupRepository.findById(secondGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.FINISHED, updatedSecondRolloutGroup.getStatus());

        JpaRolloutGroup updatedThirdRolloutGroup = rolloutGroupRepository.findById(thirdGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.CANCELED, updatedThirdRolloutGroup.getStatus());

        JpaRolloutGroup updatedFourthRolloutGroup = rolloutGroupRepository.findById(fourthGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.QUEUED, updatedFourthRolloutGroup.getStatus());

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSED, updatedRollout.getStatus());
    }
}