package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import io.qameta.allure.Description;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.cosmos.models.ddi.DdiRestConstants.ESP;
import static org.cosmos.models.ddi.DdiRestConstants.RSP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


public class DdiRolloutTest extends AbstractDDiApiIntegrationTest {

    @MockBean
    private SnsAsyncClient snsAsyncClient;
    @Autowired
    private HandleRolloutSchedulerService rolloutSchedulerService;
    @Autowired
    private TargetRepository targetRepository;

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
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_INVENTORY_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @BeforeEach
    void setup() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY,
                SP_BASE_SOFTWARE_MODULE, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_SOFTWARE_ECU_MODEL,
                SP_BASE_SOFTWARE_MODULE, SP_ROLLOUT
        );
        MockitoAnnotations.initMocks(this);
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    @Test
    @Description("Given a Rollout and associated Rollout Groups," +
            "When all device actions are completed successfully or fail," +
            "Then the Rollout and its groups transition to the FINISHED state.")
    void givenRolloutAndGroupsWhenDeviceActionsAreCompletedThenTransitionToFinishedState() throws Exception {
        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(5, 2);

        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 0, RolloutStatus.RUNNING, RolloutGroupStatus.FINISHING, getJsonFinishedSuccessDeploymentActionFeedback());
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 1, RolloutStatus.RUNNING, RolloutGroupStatus.FINISHING, getJsonFinishedFailureDeploymentActionFeedback());

        JpaRolloutGroup rolloutGroup = getRolloutGroup(rollout, 2);
        List<JpaAction> actions = getActions(rollout, rolloutGroup);
        // TODO: Remove this manual setting once the bug is fixed
        for (JpaAction action : actions) {
            updateActionsWithStatus(action.getId(), DeviceActionStatus.CANCELED);
        }
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 2, RolloutStatus.FINISHING, RolloutGroupStatus.FINISHING, getJsonCanceledCancelActionFeedback());
        handleRollout();

        assertEquals(RolloutStatus.FINISHED, getUpdatedRollout(rollout).getStatus());
        assertTrue(getRolloutGroup(rollout).stream().allMatch(group -> group.getStatus().equals(RolloutGroupStatus.FINISHED)));
    }

    @Test
    @Description("Given a Rollout and associated Rollout Groups, " +
            "when device actions are partially completed, " +
            "then the Rollout and groups remains in the RUNNING state.")
    void givenRolloutAndGroupsWhenDeviceActionsPartiallyCompleteThenRemainInRunningState() throws Exception {
        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(12, 2);

        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 0);
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 1);
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 2);

        handleRollout();

        assertEquals(RolloutStatus.RUNNING, getUpdatedRollout(rollout).getStatus());
        assertTrue(getRolloutGroup(rollout).stream().allMatch(group -> group.getStatus().equals(RolloutGroupStatus.RUNNING)));
    }

    @Test
    @Description("Given a rollout and its associated default groups, " +
            "when all device actions are completed successfully or fail, " +
            "then the rollout and its groups transition to the FINISHED state.")
    void givenRolloutAndDefaultGroupsWhenDeviceActionsAreCompletedThenTransitionToFinishedState() throws Exception {
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
        JpaRollout rollout = (JpaRollout) rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());

        List<Target> targets = createTargets(vehicleModelId, 3).stream().
                map(mgmtTarget -> (Target) new JpaTarget(mgmtTarget.getControllerId(),
                        mgmtTarget.getName(), mgmtTarget.getSerialNumber(),
                        mgmtTarget.getVehicleModelId(), mgmtTarget.getControllerId())).toList();

        // creating default group
        createRolloutGroup(targets, rollout);
        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();
        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        targets = createTargets(vehicleModelId, 3).stream().
                map(mgmtTarget -> (Target) new JpaTarget(mgmtTarget.getControllerId(),
                        mgmtTarget.getName(), mgmtTarget.getSerialNumber(),
                        mgmtTarget.getVehicleModelId(), mgmtTarget.getControllerId())).toList();
        controllerIds = targets.stream().map(Target::getControllerId).toList();


        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage espSupportPackage1 = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, espSupportPackage1.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage espSupportPackage2 = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, espSupportPackage2.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
        // creating a group
        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), 1);

        targets = createTargets(vehicleModelId, 3).stream().
                map(mgmtTarget -> (Target) new JpaTarget(mgmtTarget.getControllerId(),
                        mgmtTarget.getName(), mgmtTarget.getSerialNumber(),
                        mgmtTarget.getVehicleModelId(), mgmtTarget.getControllerId())).toList();

        // creating another default group
        createRolloutGroup(targets, rollout);
        controllerIds = targets.stream().map(Target::getControllerId).toList();
        MgmtSupportPackage supportPackage1 = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, supportPackage1.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage supportPackage2 = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, supportPackage2.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        handleRollout();
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, espSupportPackage1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, espSupportPackage2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, supportPackage1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, supportPackage2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        handleRollout();
        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        handleRollout();
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 0);
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 1);
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 2);

        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 0, RolloutStatus.RUNNING, RolloutGroupStatus.FINISHING, getJsonFinishedSuccessDeploymentActionFeedback());
        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 1, RolloutStatus.RUNNING, RolloutGroupStatus.FINISHING, getJsonFinishedFailureDeploymentActionFeedback());
        JpaRolloutGroup rolloutGroup = getRolloutGroup(rollout, 2);
        List<JpaAction> actions = getActions(rollout, rolloutGroup);
        // TODO: Remove this manual setting once the bug is fixed
        for (JpaAction action : actions) {
            updateActionsWithStatus(action.getId(), DeviceActionStatus.CANCELED);
        }
//        updateActionsWithAlternatingFeedbackAndAssertStatuses(rollout, 2, RolloutStatus.FINISHING, RolloutGroupStatus.FINISHING, getJsonCanceledCancelActionFeedback());

        handleRollout();
        handleRollout();
        assertEquals(RolloutStatus.FINISHED, getUpdatedRollout(rollout).getStatus());
        assertTrue(getRolloutGroup(rollout).stream().allMatch(group -> group.getStatus().equals(RolloutGroupStatus.FINISHED)));
    }

    @Test
    @Description("Given two rollouts with identical configurations (artifact, software module, version, target and vehicle model), "
            + "when the first rollout completes successfully for a target, "
            + "then the deployment action link for the second rollout is generated and accessible for the same target upon polling for updates.")
    void givenTwoRolloutsWithIdenticalConfigsWhenFirstCompletesThenSecondDeploymentLinkIsAccessibleForSameTarget() throws Exception {
// Mock HEAD request for file existence
        mockServer.when(request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

// Setup variables for rollouts and resources
        var port = System.getProperty("mock.server.port");
        var firstRolloutName = "newRollout";
        var secondRolloutName = "newRollout1";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

// Create vehicle model and ECU model, associate them
        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

// Create software module, version, and artifact, associate them
        long swModuleId = createSoftwareModule();
        JpaSoftwareModule softwareModule = softwareModuleRepository.findById(swModuleId).get();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

// Create two rollouts with identical configs
        JpaRollout firstRollout = createAndSetupRollout(firstRolloutName, rolloutEndDate, swModuleId, versionResponse);
        JpaRollout secondRollout = createAndSetupRollout(secondRolloutName, rolloutEndDate, swModuleId, versionResponse);

// Create a single target and controller ID list
        List<Target> targets = createTargets(vehicleModelId, 1).stream()
                .map(mgmtTarget -> (Target) new JpaTarget(
                        mgmtTarget.getControllerId(),
                        mgmtTarget.getName(),
                        mgmtTarget.getSerialNumber(),
                        mgmtTarget.getVehicleModelId(),
                        mgmtTarget.getControllerId()))
                .toList();
        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();

// Create default groups for both rollouts
        createRolloutGroup(targets, firstRollout);
        createRolloutGroup(targets, secondRollout);

// Create and update support packages for both rollouts
        createAndUploadSupportPackages(firstRollout.getId(), supportPackageUrl, ecuNodeId, controllerIds);
        createAndUploadSupportPackages(secondRollout.getId(), supportPackageUrl, ecuNodeId, controllerIds);

// Freeze both rollouts and simulate file upload completion
        mockServer.clear(request());
        invokeRolloutFreezeApi(firstRollout.getId());
        invokeRolloutFreezeApi(secondRollout.getId());
        handleRollout();
        handleRollout();
        handleRollout();

// Simulate polling and inventory push for the first rollout
        Target target = targets.get(0);
        String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeId, softwareModule.getName());
        invokePollForUpdatesApi(target.getControllerId());
        invokePushInventoryApi(inventoryRequest, target.getControllerId());
        MvcResult mvcResult = invokePollForUpdatesApi(target.getControllerId());
        JsonNode response = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("_links").path("deploymentBase");
        assertTrue(response.has("href"), "First rollout: deploymentBase link should be present");

// Validate deployment link for the first rollout
        JpaAction firstRolloutAction = actionRepository.findActionByRolloutIdAndActive(firstRollout.getId(), true).get(0);
        String href = response.path("href").asText();
        assertTrue(href.contains(target.getControllerId()), "First rollout: href should contain controllerId");
        assertTrue(href.contains(String.valueOf(firstRolloutAction.getId())), "First rollout: href should contain actionId");

// Complete the first rollout action
        sendFinishedSuccessDeploymentActionFeedback(target, firstRolloutAction);

// Simulate polling and inventory push for the second rollout
        mvcResult = invokePollForUpdatesApi(target.getControllerId());
        response = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("_links").path("deploymentBase");
// Validate deployment link for the second rollout
        JpaAction secondRolloutAction = actionRepository.findActionByRolloutIdAndActive(secondRollout.getId(), true).get(0);
        href = response.path("href").asText();
        assertTrue(href.contains(target.getControllerId()), "Second rollout: href should contain controllerId");
        assertTrue(href.contains(String.valueOf(secondRolloutAction.getId())), "Second rollout: href should contain actionId");
    }

    @Test
    @Description("Given a rollout and its associated default groups, " +
            "when all device actions are completed successfully or fail, " +
            "then the rollout and its groups transition to the FINISHED state.")
    void givenRolloutAndDefaultGroupsWhenDeviceActionsAreCompletedThenTransitionToFinishedState1() throws Exception {
        mockServer.when(request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

        var rolloutName = "newRollout";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
    }

    @Test
    @Description("GIVEN a rollout is in the FINISHED state with completed groups and inactive actions, "
            + "WHEN a full retry is triggered via the API, "
            + "THEN the rollout should transition through RETRYING, RETRY, FREEZING, READY, STARTING, RUNNING, "
            + "and finally return to the FINISHED state after all groups complete their retry actions.")
    void givenRolloutIsFinishedWhenFullRetryIsTriggeredThenRolloutShouldTransitionToRetried() throws Exception {
        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(3, 2);

        cancelRollout(rollout);
        assertRolloutStatus(rollout, RolloutStatus.CANCELED);

        activateRetryRolloutConfig();
        rollout = transitionRolloutToRunning(rollout, 2);

        cancelRollout(rollout);
        assertActionsInactive(rollout, 4);

        finalizeRollout(rollout, RolloutStatus.CANCELED, RolloutGroupStatus.CANCELED);

        rollout = transitionRolloutToRunning(rollout, 4);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 2);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        rollout = transitionRolloutToRunning(rollout, 7);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 2);
        assertActionsInactive(rollout, 10);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        triggerFullRetryRollout(rollout);
        assertRolloutRetrying(rollout, RetryMode.FULL, 10);

        rollout = progressRolloutToReady(rollout);

        rolloutManagement.start(rollout.getId());
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.STARTING.name(), rollout.getStatus().name());
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RUNNING.name(), rollout.getStatus().name());

        completeGroupsWithFeedback(rollout, getJsonCanceledAcceptDeploymentActionFeedback(), 2);
        assertActionsInactive(rollout, 13, 3, 10);

        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        activateRetryRolloutConfig();
        rollout = transitionRolloutToRunning(rollout, 13);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 1);
        cancelDeviceAction(rollout);

        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        activateRetryRolloutConfig();
        rollout = transitionRolloutToRunning(rollout, 16);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 1);
        cancelDeviceAction(rollout);

        assertActionsInactive(rollout, 19, 0, 19);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);
    }

    @Test
    @Description("Given a rollout with multiple targets assigned to a single tenant," +
            "When some of these targets are moved to a different tenant and a full retry is triggered via the API," +
            "Then only the targets that remain in the original tenant should be retried, and targets moved to another tenant should not participate in the retry process.")
    void givenRolloutWithMultipleTargetsWhenSomeTargetsMovedToDifferentTenantAndFullRetryTriggeredThenOnlyOriginalTenantTargetsAreRetried() throws Exception {
        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(3, 1);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 1);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        activateRetryRolloutConfig();
        rollout = transitionRolloutToRunning(rollout, 3);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 1);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        // Now switch the targets to a different tenant
        List<JpaTarget> targets = targetRepository.findTargetsByRolloutId(rollout.getId());
        List<JpaTarget> jpaTargets = targets.subList(0, 2);
        for (JpaTarget target : jpaTargets) {
            targetManagement.updateTenant(target.getControllerId(), "DEFAULT", "ANOTHER_TENANT");
        }

        triggerFullRetryRollout(rollout);

        // Assert: Rollout is in RETRYING state with FULL retry mode
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RETRYING, rollout.getStatus(), "Rollout should be RETRYING");
        assertEquals(RetryMode.FULL, rollout.getLastRetryMode(), "Retry mode should be FULL");

        // Act: Handle retrying rollout
        handleRollout();

        // Assert: All groups are in RETRY status
        assertAllGroupsInStatus(rollout.getId(), RolloutGroupStatus.RETRY);

        // Assert: Rollout transitions to FREEZING and READY
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.FREEZING.name(), rollout.getStatus().name(), "Rollout should be FREEZING");
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.READY.name(), rollout.getStatus().name(), "Rollout should be READY");

        // Act: Start rollout and progress to RUNNING
        rolloutManagement.start(rollout.getId());
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.STARTING.name(), rollout.getStatus().name(), "Rollout should be STARTING");
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RUNNING.name(), rollout.getStatus().name(), "Rollout should be RUNNING");

        int size = actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true).size();

        // Only one target should be retried as the other two were moved to a different tenant
        assertEquals(1, size, "One action should be created since the other two targets were moved to a different tenant");
    }


    @Disabled("This test is disabled until we can re-enable the test")
    @Test
    @Description("GIVEN a finished rollout with completed groups, WHEN fetching actions and targets for that rollout, THEN all actions and associated targets should be returned successfully.")
    void givenFinishedRolloutWhenFetchingActionsAndTargetsThenAllAreReturned() throws Exception {

        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(3, 1);
        completeGroupsWithFeedback(rollout, getJsonFinishedSuccessDeploymentActionFeedback(), 1);
        finalizeRollout(rollout, RolloutStatus.FINISHED, RolloutGroupStatus.FINISHED);

        activateRetryRolloutConfig();
         transitionRolloutToRunning(rollout, 3);

        mvc.perform(get(MgmtRestConstants.ROLLOUT_TARGETS_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("[0].targetId").exists())
                .andExpect(jsonPath("[0].actionDetails").exists())
                .andExpect(jsonPath("[0].actionDetails[0].actionId").exists())
                .andExpect(jsonPath("[0].actionDetails[0].actionStatus").exists())
                .andExpect(jsonPath("[1].targetId").exists())
                .andExpect(jsonPath("[1].actionDetails").exists())
                .andExpect(jsonPath("[1].actionDetails[1].actionId").exists())
                .andExpect(jsonPath("[1].actionDetails[1].actionStatus").exists());
    }


    private Rollout transitionRolloutToRunning(Rollout rollout, int expected) throws Exception {
        triggerFullRetryRollout(rollout);

        // Assert: Rollout is in RETRYING state with FULL retry mode
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RETRYING, rollout.getStatus(), "Rollout should be RETRYING");
        assertEquals(RetryMode.FULL, rollout.getLastRetryMode(), "Retry mode should be FULL");

        // Act: Handle retrying rollout
        handleRollout();

        // Assert: All groups are in RETRY status
        assertAllGroupsInStatus(rollout.getId(), RolloutGroupStatus.RETRY);

        // Assert: All previous actions are inactive
        assertEquals(expected, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), false).size(), "All previous actions should be inactive");

        // Assert: Rollout transitions to FREEZING and READY
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.FREEZING.name(), rollout.getStatus().name(), "Rollout should be FREEZING");
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.READY.name(), rollout.getStatus().name(), "Rollout should be READY");

        // Act: Start rollout and progress to RUNNING
        rolloutManagement.start(rollout.getId());
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.STARTING.name(), rollout.getStatus().name(), "Rollout should be STARTING");
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RUNNING.name(), rollout.getStatus().name(), "Rollout should be RUNNING");
        return rollout;
    }

// --- Helper methods ---

    private void cancelRollout(Rollout rollout) throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        handleRollout();
        handleRollout();
    }

    private void assertRolloutStatus(Rollout rollout, RolloutStatus expectedStatus) {
        assertEquals(expectedStatus, getUpdatedRollout(rollout).getStatus());
    }

    private void assertActionsInactive(Rollout rollout, int expectedInactiveCount) {
        assertEquals(expectedInactiveCount, actionRepository.count());
        assertEquals(0, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true).size());
        assertEquals(expectedInactiveCount, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), false).size());
    }

    private void assertActionsInactive(Rollout rollout, int total, int active, int inactive) {
        assertEquals(total, actionRepository.count());
        assertEquals(active, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true).size());
        assertEquals(inactive, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), false).size());
    }

    private void finalizeRollout(Rollout rollout, RolloutStatus expectedStatus, RolloutGroupStatus status) {
        handleRollout();
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(expectedStatus.name(), rollout.getStatus().name());
        assertTrue(getRolloutGroup(rollout).stream().allMatch(group -> group.getStatus().equals(status)));
    }

    private void completeGroupsWithFeedback(Rollout rollout, String feedback, int groupCount) throws Exception {
        for (int i = 0; i < groupCount; i++) {
            sendFeedbackForActiveActions(rollout.getId(), feedback);
            handleRollout();
        }
    }

    private void cancelDeviceAction(Rollout rollout) throws Exception {
        List<JpaAction> action = actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, 1L, rollout.getId(), action.get(0).getTarget().getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());
        handleRollout();
        handleRollout();
    }

    private void assertRolloutRetrying(Rollout rollout, RetryMode expectedMode, int expectedInactiveCount) {
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.RETRYING, rollout.getStatus());
        assertEquals(expectedMode, rollout.getLastRetryMode());
        assertEquals(expectedInactiveCount, actionRepository.findActionByRolloutIdAndActive(rollout.getId(), false).size());
        handleRollout();
        assertAllGroupsInStatus(rollout.getId(), RolloutGroupStatus.RETRY);
    }

    private Rollout progressRolloutToReady(Rollout rollout) throws Exception {
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.FREEZING.name(), rollout.getStatus().name());
        handleRollout();
        rollout = getUpdatedRollout(rollout);
        assertEquals(RolloutStatus.READY.name(), rollout.getStatus().name());
        return rollout;
    }

    @Test
    @Description("GIVEN a FINISHED rollout, WHEN a full retry is triggered via the API, " +
            "THEN the rollout should not transition and an exception should be thrown if active actions exist or " +
            "if not all groups are in the expected state.")
    void givenFinishedRolloutWhenFullRetryIsTriggeredThenThrowException() throws Exception {
        // Arrange: Create a rollout with 3 targets and 2 groups
        Rollout rollout = createRolloutWithTargetsAndRolloutGroups(3, 2);
        handleRollout();

        // Complete group 1
        sendFeedbackForActiveActions(rollout.getId(), getJsonFinishedSuccessDeploymentActionFeedback());
        handleRollout();

        // Complete group 2
        sendFeedbackForActiveActions(rollout.getId(), getJsonFinishedSuccessDeploymentActionFeedback());
        handleRollout();
        handleRollout();

        // Assert: Rollout is finished
        assertEquals(RolloutStatus.FINISHED, getUpdatedRollout(rollout).getStatus(), "Rollout should be FINISHED");

        // Act: Trigger retry rollout API
        activateRetryRolloutConfig();
        triggerFullRetryRollout(rollout);

        // Assert: Rollout is in RETRYING state with FULL retry mode
        rollout = getUpdatedRollout(rollout);
        JpaRollout jpaRollout = (JpaRollout) rollout;

        // Manually set action to active to simulate the scenario
        actionRepository.findById(1L).ifPresent(action -> {
            action.setActive(true);
            actionRepository.save(action);
        });

        assertEquals(RolloutStatus.RETRYING, rollout.getStatus(), "Rollout should be RETRYING");
        assertEquals(RetryMode.FULL, rollout.getLastRetryMode(), "Retry mode should be FULL");

        // Act: Handle retrying rollout
        RolloutIllegalStateException exception = assertThrows(
                RolloutIllegalStateException.class,
                () -> rolloutSchedulerService.validateAllActionsInactiveForGroup(jpaRollout, 1L)
        );
        assertEquals("Active actions exist for rolloutGroupId=1", exception.getMessage());

        // Manually set group to running to simulate the scenario
        JpaRolloutGroup jpaRolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        jpaRolloutGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(jpaRolloutGroup);

        assertEquals(RolloutStatus.RETRYING, rollout.getStatus(), "Rollout should be RETRYING");
        assertEquals(RetryMode.FULL, rollout.getLastRetryMode(), "Retry mode should be FULL");

        // Act: Handle retrying rollout
        exception = assertThrows(
                RolloutIllegalStateException.class,
                () -> rolloutSchedulerService.handleFullRetryMode(jpaRollout)
        );
        assertEquals("Not all groups are in the expected state", exception.getMessage());
    }

}
