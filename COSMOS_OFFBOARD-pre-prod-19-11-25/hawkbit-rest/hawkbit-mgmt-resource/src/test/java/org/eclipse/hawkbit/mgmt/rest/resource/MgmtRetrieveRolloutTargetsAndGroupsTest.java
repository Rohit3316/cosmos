package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;

import jakarta.validation.ValidationException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class MgmtRetrieveRolloutTargetsAndGroupsTest extends AbstractManagementRolloutApiIntegrationTest{


    @Test
    @Description("Testing that rollout paged list with rsql parameter")
    void getRolloutWithRSQLParam() throws Exception {

        final int amountTargetsRollout1 = 25;
        final int amountTargetsRollout2 = 25;
        final int amountTargetsRollout3 = 25;
        final int amountTargetsOther = 25;
        List<Target> target1 = testdataFactory.createTargets(amountTargetsRollout1, ROLLOUT_1, ROLLOUT_1);
        List<Target> target2 = testdataFactory.createTargets(amountTargetsRollout2, ROLLOUT_2, ROLLOUT_2);
        List<Target> target3 = testdataFactory.createTargets(amountTargetsRollout3, ROLLOUT_3, ROLLOUT_3);
        List<Target> target4 = testdataFactory.createTargets(amountTargetsOther, OTHER_1, OTHER_1);

        createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), target1);
        final Rollout rollout2 = createRolloutWithDependencies(ROLLOUT_2, testdataFactory.createDistributionSet(), target2);
        createRolloutWithDependencies(ROLLOUT_3, testdataFactory.createDistributionSet(), target3);
        createRolloutWithDependencies(OTHER_1, testdataFactory.createDistributionSet(), target4);

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*2").accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1))).andExpect(jsonPath(JSON_PATH_CONTENT_0_NAME, equalTo(rollout2.getName())));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout*")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(3))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(3)));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==*1")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(2))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(2)));

    }



    @Test
    @Description("Given a rollout with associated groups and targets, when attempting to remove device details with invalid targets, then a ValidationException should be thrown.")
    void givenRolloutWithoutGroups_whenRemovingDeviceDetails_thenThrowValidationException() {

        // Given: A rollout with associated groups and targets
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        prepareRolloutAndGroupDetails(targets);

        // Given: A dummy rollout with  associated groups
        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";
        final Rollout dummyRollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT_1,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(CONTROLLER_ID),
                groupsDetailsJson, true
        );

        // When & Then: Attempting to remove device details for the dummy rollout throws ValidationException
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> rolloutManagement.deleteDeviceDetails(dummyRollout,
                        List.of(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3), true)
        );

        // Verify exception message
        assertNotNull(exception, "ValidationException should be thrown.");
        assertEquals("Targets are not associated with the provided rollout", exception.getMessage(),
                "The exception message should indicate that no groups are associated with the rollout.");
    }



    @Test
    @Description("Testing that the targets of rollout group can be retrieved")
    void retrieveTargetsFromRolloutGroup() throws Exception {
        // setup
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Long tenantId = 1L;
        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        rolloutManagement.freeze(rollout.getId());
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                        get(BASE_URL + tenantId + ROLLOUT_GROUP_TARGETS_ENDPOINT, rollout.getId(), firstGroup.getId())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(10))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(10)));
    }

    @Test
    @Description("Given a rollout is started in async mode, when the process begins, then actions should be created for the first rollout group and the rollout should switch to the running state.")
    void givenRolloutStartedAsync_whenProcessBegins_thenActionsCreatedForFirstGroupAndStateIsRunning() throws Exception {


        final int amountTargets = 50;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), targets, rollout, true);


        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        // starting rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // check if running
        awaitRunningState(rollout.getId());

        assertEquals(amountTargets, StreamSupport.stream(actionRepository.findAll().spliterator(),false).count());
    }

    private void awaitRunningState(final Long rolloutId) {
        Awaitility.await().atMost(Duration.ofMinutes(1)).pollInterval(Duration.ofMillis(100)).with().until(() -> WithSpringAuthorityRule.runAsPrivileged(() -> rolloutManagement.get(rolloutId).orElseThrow(NoSuchElementException::new)).getStatus().equals(RolloutStatus.RUNNING));
    }
    @Test
    @Description("Testing that canceling the rollout switches to canceled state")
    void cancelingRolloutSwitchesIntoCanceledState() throws Exception {

        // canceling rollout with entity not exist.
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_CANCEL_URL, TENANT_ID, 100)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), targets, rollout, true);

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        // starting rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // canceling rollout with status ready is a bad request.
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_CANCEL_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // canceling rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_CANCEL_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check rollout is in stopping state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath(STATUS, equalTo("canceling")));

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // check rollout is in finished state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(STATUS, equalTo("canceled")));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved after the rollout has been started")
    void retrieveTargetsFromRolloutGroupAfterRolloutIsStarted() throws Exception {
        // setup
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        rolloutManagement.freeze(rollout.getId());
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Long tenantId = 1L;

        final RolloutGroup firstGroup = rolloutGroupManagement.findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                        get(BASE_URL + tenantId + ROLLOUT_GROUP_TARGETS_ENDPOINT, rollout.getId(), firstGroup.getId())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(10))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(10)));
    }



    @Test
    @Description("Given a rollout, when it moves to RUNNING, then the rollout group transitions sequentially and gets finished")
    void givenRolloutWhenMovesToRunningThenRolloutGroupTransitions() throws Exception {

        mockServer.when(request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

        long swModuleId = createSoftwareModule();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        List<MgmtTarget> targets = createTargets(vehicleModelId, 10);
        List<String> controllerIds = getControllerIds(targets);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), 3);

        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        handleRollout();
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        handleRollout();
        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        handleRollout();

        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        verifyRunningGroups(rollout.getId(), groupsResponse);
    }



    List<EcuModel> createEcuModel(String ecuNodeAddress) {
        List<EcuModel> mockEcuModels = new ArrayList<>();

        JpaEcuModel ecuModel1 = new JpaEcuModel();
        JpaEcuModelType ecuModelType1 = new JpaEcuModelType("OM");
        ecuModel1.setEcuModelType(ecuModelType1);
        ecuModel1.setEcuModelName("Test ECU Model 1");
        ecuModel1.setEcuNodeId(ecuNodeAddress);
        mockEcuModels.add(ecuModel1);
        return mockEcuModels;
    }


}
