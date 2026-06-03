package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.util.Base64;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.cosmos.models.ddi.DdiDeploymentDescriptor;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper;
import org.eclipse.hawkbit.repository.jpa.model.ActionArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.persistence.IdClass;
import jakarta.validation.ConstraintViolationException;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DdiDeploymentBaseNewTest extends AbstractDDiApiIntegrationTest {

    @MockBean
    private SnsAsyncClient snsAsyncClient;

    @BeforeAll
    static void initializeMockServer() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        setupMockEndpoints();
    }

    @AfterAll
    static void shutdownMockServer() {
        mockServer.stop();
    }

    private static void setupMockEndpoints() {
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response().withStatusCode(200));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_INVENTORY_ENDPOINT))
                .respond(HttpResponse.response().withStatusCode(200));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_DD_ARTIFACT_EXPIRY_URL))
                .respond(HttpResponse.response().withStatusCode(200));
    }

    @BeforeEach
    void setupTestEnvironment() throws IOException {
        clearDatabaseTables();
        MockitoAnnotations.initMocks(this);
        setupMockSnsClient();
        setupCertificatesAndKeysForDDGeneration();
    }

    private void clearDatabaseTables() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY,
                SP_BASE_SOFTWARE_MODULE, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_SOFTWARE_ECU_MODEL,
                SP_BASE_SOFTWARE_MODULE, SP_ROLLOUT, SP_ACTION_ARTIFACT
        );
    }

    private void setupMockSnsClient() {
        PublishResponse mockResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    private String extractDeploymentBaseLink(MvcResult mvcResult) throws Exception {
        return objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("_links").path("deploymentBase").path("href").asText();
    }

    private void associateAdditionalArtifactWithSoftwareModule(long softwareModuleId) throws Exception {
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(softwareModuleId);
        MgmtArtifacts artifact = createArtifact(String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort()));
        associateArtifactWithSoftwareModule(softwareModuleId, versionResponse.getId(), artifact.getArtifactId());
    }

    @Test
    @Description("Given a basic rollout setup with one action and one artifact detail, and successful DD generation for the target, " +
            "when the DD link is inspected, the right artifact and action mapping is saved correctly, duplicate mappings are prevented, " +
            "and cascading deletions are verified.")
    void givenActionAndArtifactDetailsWhenInspectingDDLinkThenMappingIsStoredSuccessfully() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(1);

        Target target = ((List<Target>) rolloutDetails.get("targets")).get(0);
        String ecuNodeName = (String) rolloutDetails.get("ecuNodeId");
        Rollout rollout = (Rollout) rolloutDetails.get("rollout");
        Long artifactId = (Long) rolloutDetails.get("artifactId");
        JpaArtifacts artifacts = artifactsRepository.findById(artifactId).get();
        long softwareModuleId = (long) rolloutDetails.get("softwareModuleId");
        JpaSoftwareModule softwareModule = softwareModuleRepository.findById(softwareModuleId).get();

        String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeName, softwareModule.getName());
        invokePollForUpdatesApi(target.getControllerId());
        invokePushInventoryApi(inventoryRequest, target.getControllerId());
        MvcResult mvcResult = invokePollForUpdatesApi(target.getControllerId());
        String inspectDDLink = extractDeploymentBaseLink(mvcResult);

        MockHttpServletRequestBuilder inspectRequest = MockMvcRequestBuilders.get(inspectDDLink);
        invokeInspectDDApi(inspectRequest);
        assertEquals(1, actionArtifactRepository.count());

        // Ensure duplicate mappings are not added
        invokeInspectDDApi(inspectRequest);
        assertEquals(1, actionArtifactRepository.count());

        invokePurgeArtifactsApi(artifacts);
        for (JpaActionArtifact jpaActionArtifact : actionArtifactRepository.findAll()) {
            Action action = actionRepository.getActionById(jpaActionArtifact.getAction().getId(), false).get();
            assertEquals(DeviceActionStatus.CANCELED, action.getStatus());
        }

        // Validate cascading deletions
        assertEquals(1, actionArtifactRepository.count());
        artifactsRepository.deleteById(artifactId);
        assertEquals(0, actionArtifactRepository.count());

    }

    @Test
    @Description("Given a basic rollout setup with multiple actions and one artifact detail, and successful DD generation for the target, " +
            "when the DD link is inspected, the right artifact and action mappings are saved correctly, duplicate mappings are prevented, " +
            "and cascading deletions are verified.")
    void givenMultipleActionsAndArtifactDetailsWhenInspectingDDLinkThenMappingsAreStoredSuccessfully() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(3);
        String ecuNodeName = (String) rolloutDetails.get("ecuNodeId");
        long softwareModuleId = (long) rolloutDetails.get("softwareModuleId");
        Long artifactId = (Long) rolloutDetails.get("artifactId");
        Rollout rollout = (Rollout) rolloutDetails.get("rollout");
        JpaArtifacts artifacts = artifactsRepository.findById(artifactId).get();
        int expectedMappings = 1;

        for (Target target : (List<Target>) rolloutDetails.get("targets")) {
            JpaSoftwareModule softwareModule = softwareModuleRepository.findById(softwareModuleId).get();
            String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeName, softwareModule.getName());

            invokePollForUpdatesApi(target.getControllerId());
            invokePushInventoryApi(inventoryRequest, target.getControllerId());
            MvcResult mvcResult = invokePollForUpdatesApi(target.getControllerId());
            String inspectDDLink = extractDeploymentBaseLink(mvcResult);

            MockHttpServletRequestBuilder inspectRequest = MockMvcRequestBuilders.get(inspectDDLink);
            invokeInspectDDApi(inspectRequest);
            assertEquals(expectedMappings++, actionArtifactRepository.count());
        }

        for (Target target : (List<Target>) rolloutDetails.get("targets")) {
            assertEquals(--expectedMappings, actionArtifactRepository.count());
            Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                    .getContent().get(0);
            actionRepository.deleteById(action.getId());
        }

        invokePurgeArtifactsApi(artifacts);
        for (JpaActionArtifact jpaActionArtifact : actionArtifactRepository.findAll()) {
            Action action = actionRepository.getActionById(jpaActionArtifact.getAction().getId(), true).get();
            assertEquals(DeviceActionStatus.CANCELED, action.getStatus());
        }
    }

    @Test
    @Description("Given a basic rollout setup with one action and multiple artifact details, and successful DD generation for the target, " +
            "when the DD link is inspected, the right artifact (only one) and action mapping is saved correctly, duplicate mappings are prevented, " +
            "and cascading deletions are verified.")
    void givenActionsAndMultipleArtifactDetailsWhenInspectingDDLinkThenMappingsAreStoredSuccessfully() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(1);
        Target target = ((List<Target>) rolloutDetails.get("targets")).get(0);
        String ecuNodeId = (String) rolloutDetails.get("ecuNodeId");
        Long artifactId = (Long) rolloutDetails.get("artifactId");
        Rollout rollout = (Rollout) rolloutDetails.get("rollout");
        JpaArtifacts artifacts = artifactsRepository.findById(artifactId).get();

        long softwareModuleId = (long) rolloutDetails.get("softwareModuleId");

        // Associating multiple artifacts with the software module
        associateAdditionalArtifactWithSoftwareModule(softwareModuleId);

        JpaSoftwareModule softwareModule = softwareModuleRepository.findById(softwareModuleId).get();
        String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeId, softwareModule.getName());
        invokePollForUpdatesApi(target.getControllerId());
        invokePushInventoryApi(inventoryRequest, target.getControllerId());
        MvcResult mvcResult = invokePollForUpdatesApi(target.getControllerId());
        String inspectDDLink = extractDeploymentBaseLink(mvcResult);

        MockHttpServletRequestBuilder inspectRequest = MockMvcRequestBuilders.get(inspectDDLink);
        invokeInspectDDApi(inspectRequest);
        assertEquals(1, actionArtifactRepository.count());

        // Ensure duplicate mappings are not added
        invokeInspectDDApi(inspectRequest);
        assertEquals(1, actionArtifactRepository.count());

        invokePurgeArtifactsApi(artifacts);
        for (JpaActionArtifact jpaActionArtifact : actionArtifactRepository.findAll()) {
            Action action = actionRepository.getActionById(jpaActionArtifact.getAction().getId(), false).get();
            assertEquals(DeviceActionStatus.CANCELED, action.getStatus());
        }

        // Validate cascading deletions
        assertEquals(1, actionArtifactRepository.count());
        artifactsRepository.deleteById(artifactId);
        assertEquals(0, actionArtifactRepository.count());

    }

    @Test
    @Description("Given a basic rollout setup is completed with invalid actions and artifact details, and DD generation succeeds for the target." +
            "when the DD link is inspected then throw Reference Integrity Violation Exception")
    void givenInvalidActionsAndArtifactDetailsWhenInspectingDDLinkThenThrowReferenceIntegrityViolationException() {
        JpaArtifacts invalidArtifact = new JpaArtifacts();
        invalidArtifact.setId(1000L);

        JpaAction invalidAction = new JpaAction();
        invalidAction.setId(1000L);

        JpaActionArtifact invalidMapping = new JpaActionArtifact();
        invalidMapping.setArtifact(invalidArtifact);
        invalidMapping.setAction(invalidAction);

        assertThrows(ConstraintViolationException.class, () -> actionArtifactRepository.save(invalidMapping));

    }

    @Test
    @Description("Given a JpaActionArtifact mapping class is initialized and validated for basic scenarios." +
            "when constructors, equality, hashCode, getters, setters, and IdClass mapping are tested." +
            "then all validations should pass successfully.")
    void givenJpaActionArtifactMappingClassWhenValidatingBasicScenariosThenItShouldPass() {
        // Testing constructor initialization
        JpaAction action = new JpaAction();
        JpaArtifacts artifact = new JpaArtifacts();
        JpaActionArtifact actionArtifact = new JpaActionArtifact(action, artifact);

        assertEquals(action, actionArtifact.getAction());
        assertEquals(artifact, actionArtifact.getArtifact());
        JpaActionArtifact jpaActionArtifact = new JpaActionArtifact(action, artifact);

        // Testing equality
        assertEquals(actionArtifact, jpaActionArtifact);
        assertEquals(actionArtifact.hashCode(), jpaActionArtifact.hashCode());

        // Testing getters and setters
        actionArtifact = new JpaActionArtifact();
        actionArtifact.setAction(action);
        actionArtifact.setArtifact(artifact);
        assertEquals(action, actionArtifact.getAction());
        assertEquals(artifact, actionArtifact.getArtifact());

        // Test ID class mapping
        assertNotNull(JpaActionArtifact.class.getAnnotation(IdClass.class));
        assertEquals(ActionArtifact.class, JpaActionArtifact.class.getAnnotation(IdClass.class).value());

        // Testing equality
        action.setId(1L);
        artifact.setId(2L);
        ActionArtifact id1 = new ActionArtifact(action, artifact);
        ActionArtifact id2 = new ActionArtifact(action, artifact);

        assertEquals(id1, id2);
        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    @Description("Given a rollout with multiple actions assigned to different targets, " +
            "when each target completes its action with a terminal status (FINISHED_SUCCESS, " +
            "FINISHED_FAILURE, or CANCELED) and the poll for updates API is called, " +
            "then the deployment base link should only be present for actions that are not completed, " +
            "and should be absent for actions in any completed state.")
    void givenRolloutWithMultipleTargetsWhenActionsCompleteWithTerminalStatusThenDeploymentBaseLinkIsAbsentForCompletedActions() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(3);
        List<Target> targets = (List<Target>) rolloutDetails.get("targets");
        String ecuNodeId = (String) rolloutDetails.get("ecuNodeId");
        long softwareModuleId = (long) rolloutDetails.get("softwareModuleId");
        JpaSoftwareModule softwareModule = softwareModuleRepository.findById(softwareModuleId).get();

// Setup: Ensure all actions are RUNNING and deployment base link is present
        for (Target target : targets) {
            String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeId, softwareModule.getName());
            invokePollForUpdatesApi(target.getControllerId());
            invokePushInventoryApi(inventoryRequest, target.getControllerId());
            Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
            assertEquals(DeviceActionStatus.RUNNING, action.getStatus());
            assertDeploymentBaseLinkPresence(invokePollForUpdatesApi(target.getControllerId()), true);
        }

// Simulate completion of actions with different terminal statuses
        Action firstAction = deploymentManagement.findActionsByTarget(targets.get(0).getControllerId(), PAGE).getContent().get(0);
        sendFinishedSuccessDeploymentActionFeedback(targets.get(0), firstAction); // FINISHED_SUCCESS

        Action secondAction = deploymentManagement.findActionsByTarget(targets.get(1).getControllerId(), PAGE).getContent().get(0);
        sendFinishedFailureDeploymentActionFeedback(targets.get(1), secondAction); // FINISHED_FAILURE

        Action thirdAction = deploymentManagement.findActionsByTarget(targets.get(2).getControllerId(), PAGE).getContent().get(0);
        sendCanceledAcceptDeploymentActionFeedback(targets.get(2), thirdAction); // CANCELED

// Verify: After completion, deployment base link should be absent for all completed actions
        for (Target target : targets) {
            assertDeploymentBaseLinkPresence(invokePollForUpdatesApi(target.getControllerId()), false);
        }
    }

    @Test
    @Description("Given a DdiDeploymentDescriptor, when encodeDeploymentDescriptor is called, " +
            "then it should return the expected Base64-encoded SHA-256 hash of the descriptor JSON.")
    void givenDeploymentDescriptorWhenEncodingThenReturnsExpectedBase64Sha256Hash() throws Exception {
        // Given
        DdiDeploymentDescriptor descriptor = new DdiDeploymentDescriptor();
        descriptor.setActionId(1L);
        descriptor.setDescription("Test Deployment");
        descriptor.setRolloutName("Test Rollout");
        String json = new ObjectMapper().writeValueAsString(descriptor);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String base64 = Base64.encode(bytes).toString();

        // When
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(base64.getBytes(StandardCharsets.UTF_8));
        String expected = Base64.encode(hash).toString();

        String actual = DdiApiHelper.encodeDeploymentDescriptor(descriptor);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @Description("Given: A rollout is created with a maximum package size limit, and associated actions, targets, and support packages are set up. " +
            "The file sizes of artifacts and support packages are manipulated to exceed the allowed package size.  " +
            "When: The deployment descriptor is inspected for a target after the rollout is started and the package size " +
            "exceeds the configured maximum.  " +
            "Then: The system throws a MaxPackageSizeExceededException, returning a Bad Request response with an " +
            "appropriate error message. All related actions are subsequently canceled.")
    void givenRolloutWithMaxPackageSizeLimitWhenPackageSizeExceedsLimitThenThrowMaxPackageSizeExceededException() throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();

        long swModuleId = createSoftwareModule();
        String swModuleName = softwareModuleRepository.findById(swModuleId).get().getName();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"),1000);
        final String rolloutRequest = JsonBuilder.rollout(rolloutName, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), null, rolloutEndDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, 5L, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rolloutRequest).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
        Rollout rollout = getRolloutByName(rolloutName);
        assertNotNull(rollout);
        assertEquals(5L, rollout.getMaxPackageSize());

        Target target = createTargets(VALID_CONTROLLER_ID);
        List<String> controllerIds = List.of(target.getControllerId());
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        associateEcuModelToVehicleModel(target.getVehicleModelId(), addEcuModelResponse.get(0).getId());
        List<Long> supportPackages = createAndSaveSupportPackages(rollout.getId(), ecuNodeId, supportPackageUrl, controllerIds);
        addDevice(controllerIds, rollout.getId());


        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout(rolloutName, RolloutStatus.READY);
        startRollout(rollout.getId());

        JpaEsp esp = espRepository.findById(supportPackages.get(0)).get();
        esp.setFileSize(2L);
        esp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp);
        JpaEsp esp1 = espRepository.findById(supportPackages.get(1)).get();
        esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        esp1.setFileSize(1L);
        espRepository.save(esp1);
        JpaRsp rsp = rspRepository.findById(supportPackages.get(2)).get();
        rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rsp.setFileSize(1L);
        rspRepository.save(rsp);
        JpaArtifacts jpaArtifact = artifactsRepository.findById(artifacts.getArtifactId()).get();
        jpaArtifact.setFileSize(2L);
        artifactsRepository.save(jpaArtifact);

        handleRollout(rolloutName, RolloutStatus.RUNNING);

        String inventoryRequest = buildDefaultInventoryPushRequest(target.getControllerId(), ecuNodeId, swModuleName);
        invokePollForUpdatesApi(target.getControllerId());
        invokePushInventoryApi(inventoryRequest, target.getControllerId());
        MvcResult mvcResult = invokePollForUpdatesApi(target.getControllerId());
        String inspectDDLink = extractDeploymentBaseLink(mvcResult);

        MockHttpServletRequestBuilder inspectRequest = MockMvcRequestBuilders.get(inspectDDLink);
        mvc.perform(inspectRequest
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", containsString("PackageSizeLimitExceededException")))
                .andExpect(jsonPath("$.message", containsString("Maximum package size exceeded for rollout")));

        handleRollout();
        handleRollout();
        actionRepository.findAll().forEach(jpaAction ->
                assertEquals(DeviceActionStatus.CANCELED, jpaAction.getStatus())
        );
        rollout = getRolloutByName(rolloutName);
        assertEquals(RolloutStatus.CANCELED, rollout.getStatus());
    }

    @Test
    @Description("Given: A rollout is created with a maximum package size limit, and associated actions, targets, and support packages are set up. " +
            "The file sizes of artifacts and support packages are manipulated to remain within the allowed package size.  " +
            "When: The deployment descriptor is inspected for a target after the rollout is started and the package size " +
            "is within the configured maximum.  " +
            "Then: The system process the action normally without errors or interruptions and generates the deployment descriptor.")
    void givenRolloutWithMaxPackageSizeLimitWhenPackageSizeIsWithinLimitThenDeploymentDescriptorIsGeneratedSuccessfully() throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

        long swModuleId = createSoftwareModule();
        associateEcuModelToSoftwareModule(swModuleId, ecuModelResponses.get(0).getId());
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"),1000);
        final String rolloutRequest = JsonBuilder.rollout(rolloutName, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), null, rolloutEndDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, 5L, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rolloutRequest).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());

        Rollout rollout = getRolloutByName(rolloutName);
        assertNotNull(rollout);
        assertNotNull(rollout.getMaxPackageSize());

        List<Target> targets = createTargets(vehicleModelId, 1).stream().
                map(mgmtTarget -> (Target) new JpaTarget(mgmtTarget.getControllerId(),
                        mgmtTarget.getName(), mgmtTarget.getSerialNumber(),
                        mgmtTarget.getVehicleModelId(), mgmtTarget.getControllerId())).toList();

        String target = targets.get(0).getControllerId();
        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelResponses.get(0).getId());
        List<Long> supportPackages = createAndSaveSupportPackages(rollout.getId(), ecuNodeId, supportPackageUrl, controllerIds);
        addDevice(controllerIds, rollout.getId());


        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout(rolloutName, RolloutStatus.READY);
        startRollout(rollout.getId());

        JpaEsp esp = espRepository.findById(supportPackages.get(0)).get();
        esp.setFileSize(2L);
        esp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp);
        JpaEsp esp1 = espRepository.findById(supportPackages.get(1)).get();
        esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        esp1.setFileSize(1L);
        espRepository.save(esp1);
        JpaRsp rsp = rspRepository.findById(supportPackages.get(2)).get();
        rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rsp.setFileSize(1L);
        rspRepository.save(rsp);
        JpaArtifacts jpaArtifact = artifactsRepository.findById(artifacts.getArtifactId()).get();
        jpaArtifact.setFileSize(1L);
        artifactsRepository.save(jpaArtifact);

        handleRollout(rolloutName, RolloutStatus.RUNNING);

        JpaSoftwareModule softwareModule = softwareModuleRepository.findById(swModuleId).get();
        String inventoryRequest = buildDefaultInventoryPushRequest(target, ecuNodeId, softwareModule.getName());
        invokePollForUpdatesApi(target);
        invokePushInventoryApi(inventoryRequest, target);
        MvcResult mvcResult = invokePollForUpdatesApi(target);
        String inspectDDLink = extractDeploymentBaseLink(mvcResult);

        MockHttpServletRequestBuilder inspectRequest = MockMvcRequestBuilders.get(inspectDDLink);
        mvc.perform(inspectRequest
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        handleRollout();
        handleRollout();
        actionRepository.findAll().forEach(jpaAction ->
                assertEquals(DeviceActionStatus.DD_SENT, jpaAction.getStatus())
        );
        rollout = getRolloutByName(rolloutName);
        assertEquals(RolloutStatus.RUNNING, rollout.getStatus());

    }

}
