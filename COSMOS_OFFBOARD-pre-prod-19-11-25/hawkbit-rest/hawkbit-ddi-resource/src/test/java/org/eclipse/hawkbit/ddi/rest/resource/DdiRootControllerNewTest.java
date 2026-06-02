package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import jakarta.validation.ValidationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiPackage;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiSignature;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.s3.exception.S3Exception;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.DeploymentLogRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetSoftware;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static com.jayway.jsonpath.internal.path.PathCompiler.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.cosmos.models.ddi.DdiRestConstants.ESP;
import static org.cosmos.models.ddi.DdiRestConstants.RSP;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
public class DdiRootControllerNewTest extends AbstractDDiApiIntegrationTest {


    public static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ESTIMATED_UPDATE_TIME = "$.deploymentDescription.deploymentMetadata.estimatedUpdateTime";
    @InjectMocks
    DataConversionHelper dataConversionHelper;
    @Mock
    EcuModelManagement ecuModelManagement;
    @MockBean
    MgmtS3Service s3Service;
    @Autowired
    private HawkbitSecurityProperties securityProperties;
    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Value("${ddi.inventory.inventory-signature}")
    private String inventorySignature;
    @Value("${ddi.inventory.inventory-details}")
    private String inventoryDetails;
    @Value("${ddi.inventory.staticInventory-signature}")
    private String staticInventorySignature;
    @Value("${ddi.inventory.staticInventory-hash}")
    private String staticInventoryHash;
    @Value("${ddi.inventory.rawInventory-signature}")
    private String rawInventorySignature;
    @Value("${ddi.inventory.rawInventory-details}")
    private String rawInventoryDetails;
    @Value("${ddi.inventory.malformed-inventory-details}")
    private String malformedInventoryDetails;
    @Value("${ddi.inventory.invalid-inventory-details}")
    private String invalidInventoryDetails;
    @Value("${ddi.inventory.empty-ecu-list}")
    private String emptyEcuList;
    @Value("${ddi.inventory.empty-scomos-list}")
    private String emptyScomosList;
    @Value("${ddi.inventory.empty-scomos-id}")
    private String emptyScomosId;
    @Value("${ddi.inventory.empty-software-version}")
    private String emptySwVersion;
    @Value("${ddi.inventory.empty-hardware-version}")
    private String emptyHwVersion;
    @Value("${ddi.inventory.empty-node-address}")
    private String emptyNodeAddress;
    @Value("${ddi.inventory.empty-part-number}")
    private String emptyPartNumber;
    @Value("${log.maxLogAllFileSize}")
    private String logMaxSize;
    @Value("${ddi.inventory.inventory-details-controller-id}")
    private String inventoryDetailsControllerId;
    @Value("${ddi.inventory.inventory-details-mismatched-controller-id}")
    private String inventoryDetailsMismatchedControllerId;
    @Value("${ddi.inventory.inventory-details-with-known-controller-id}")
    private String inventoryDetailsWithKnownControllerId;
    @Value("${ddi.inventory.inventory-details-with-invalid-target}")
    private String inventoryDetailsWithInvalidTarget;
    @Value("${ddi.inventory.invalid-target-controller-id}")
    private String InvalidTargetControllerId;

    @Autowired
    private EspRepository espRepository;

    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private DeploymentLogRepository deploymentLogRepository;
    @Autowired
    private EntityFactory entityFactory;
    @MockBean
    private SnsAsyncClient snsAsyncClient;

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
    void setup() throws IOException {
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
        setupCertificatesAndKeysForDDGeneration();
    }

    @Test
    @Description("Given valid deployment details, when executing deployment, then Kafka publish should succeed")
    void givenValidDeploymentDetailsWhenExecutingDeploymentThenKafkaPublishShouldSucceed(CapturedOutput output) throws Exception {

        var target = setupRolloutAndGetTargetForDD();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);


        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);


        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, target.getControllerId(), savedAction.getId())
                .param("c", "1")).andExpect(status().isOk());
        // Updated assertion for Kafka publish log
        assertTrue(output.getOut().contains("Publishing message of type [vehiclestatus]") ||
                   output.getOut().toLowerCase().contains("vehicle status message sent successfully to kafka service"));
    }

    @Test
    @Description("No request deployment logs for targets feedback above rollout's success controllerId limit")
    void givenMoreClosedSuccessFeedbackThanRolloutSuccessLimitWhenPostBaseDeploymentActionFeedbackThenEmptyLink() throws Exception {
        int rolloutSuccessLimit = 5;
        Map<String, Object> rolloutWithTargets = setupRolloutWithTargets(rolloutSuccessLimit + 1);
        List<Target> targets = (List<Target>) rolloutWithTargets.get("targets");

        for (int countOfTargets = 0; countOfTargets <= rolloutSuccessLimit; countOfTargets++) {
            String jpaTargetControllerId = targets.get(countOfTargets).getControllerId();
            JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(jpaTargetControllerId);
            Action action = jpaTarget.getActions().get(0);
            if (rolloutSuccessLimit == countOfTargets) {
                // Empty deployment log link for finished status feedbacks > 5.
                postDeploymentFeedback(jpaTargetControllerId, action.getId(), getJsonFinishedSuccessDeploymentActionFeedback(),
                        status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());
            } else {
                // Deployment log link present for finished status feedbacks <=5.
                postDeploymentFeedback(jpaTargetControllerId, action.getId(), getJsonFinishedSuccessDeploymentActionFeedback(),
                        status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF, startsWith(deploymentLogLink(jpaTargetControllerId, action.getId().toString()))));
            }

        }
    }

    @Test
    @Description("No request deployment logs for targets feedback above rollout's failure controllerId limit")
    void givenMoreFailureFeedbackThanRolloutSuccessLimitWhenPostBaseDeploymentActionFeedbackThenEmptyLink() throws Exception {
        int rolloutFailureLimit = 5;
        Map<String, Object> rolloutWithTargets = setupRolloutWithTargets(rolloutFailureLimit + 1);
        List<Target> targets = (List<Target>) rolloutWithTargets.get("targets");

        for (int countOfTargets = 0; countOfTargets <= rolloutFailureLimit; countOfTargets++) {
            String jpaTargetControllerId = targets.get(countOfTargets).getControllerId();
            JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(jpaTargetControllerId);
            Action action = jpaTarget.getActions().get(0);
            if (rolloutFailureLimit == countOfTargets) {
                // Empty deployment log link for finished status feedbacks > 5.
                postDeploymentFeedback(jpaTargetControllerId, action.getId(), getJsonFinishedFailureDeploymentActionFeedback(),
                        status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());
            } else {
                // Deployment log link present for finished status feedbacks <=5.
                postDeploymentFeedback(jpaTargetControllerId, action.getId(), getJsonFinishedFailureDeploymentActionFeedback(),
                        status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF, startsWith(deploymentLogLink(jpaTargetControllerId, action.getId().toString()))));
            }

        }
    }

    @Test
    @Description("Given the successful Installation Complete Finish success vehicle status should be sent to DOCG")
    void givenSuccessFullInstallationWhenRolloutStartsThenPublishVehicleStatusToDocg(CapturedOutput output) throws Exception {

        Target target = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);


        sendFinishedSuccessDeploymentActionFeedback(target, savedAction)
                .andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, startsWith(pushInventoryLink(target.getControllerId()))));
        // Updated assertion for Kafka publish log
        assertTrue(output.getOut().contains("Publishing message of type [vehiclestatus]") ||
                   output.getOut().toLowerCase().contains("vehicle status message sent successfully to kafka service"));
    }

    @Test
    @Description("Given a started rollout, when an intermediate state is sent as execution, it should be saved in the action status")
    void givenRolloutIsStartedWhenIntermediateStateIsSentAsExecutionThenItIsSavedInActionStatus() throws Exception {

        Target target = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        List<String> statuses = List.of("dd_accepted", "download_started", "download_in_progress", "download_completed");
        for (String status : statuses) {
            DdiDownload ddiDownload = null;
            if (status.equals("download_in_progress")) {
                ddiDownload = new DdiDownload(80, new DdiPackage(2, 4));
            }
            sendDeploymentActionFeedback(target, savedAction, status, null, ddiDownload).andExpect(status().isOk());
            JpaAction jpaAction = (JpaAction) actionRepository.getActionById(savedAction.getId(), true).orElseThrow(Exception::new);
            assertTrue(jpaAction.getActionStatus().stream().anyMatch(actionStatus -> actionStatus.getStatus().equals(DeviceActionStatus.valueOf(status.toUpperCase()))), "Expected status not found in any element");

        }
    }

    @Test
    @Description("Given the failed Installation and inventory is changed, vehicle status for failure should be sent to DOCG")
    void givenFailureInInstallationWithChangeInInventoryWhenRolloutStartsThenPublishVehicleStatusToDocg(CapturedOutput output) throws Exception {
        Target target = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        sendFinishedFailureDeploymentActionFeedback(target, savedAction).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, startsWith(pushInventoryLink(target.getControllerId()))));
        // Updated assertion for Kafka publish log
        assertTrue(output.getOut().contains("Publishing message of type [vehiclestatus]") ||
                   output.getOut().toLowerCase().contains("vehicle status message sent successfully to kafka service"));
    }

    @Test
    @Description("Given invalid fields ,API to send collective feedback sends validation error.")
    void givenFeedbackStatusListWhenInvalidFieldsThenThrowsValidationException() throws Exception {
        Target savedTarget = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);

        // Scenario 1: Code is null
        List<DdiStatus> ddiStatusesWithNoCode = List.of(
                getDdiStatusWithNoCode(DdiStatus.ExecutionStatus.DD_ACCEPTED, null, MESSAGE, 1737348213000L)
        );
        String feedbackAsStringWithNoCode = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoCode);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("Code cannot be null", result.getResolvedException().getMessage()));

        // Scenario 2: ErrorCode is null for ExecutionStatus as 'FINISHED_FAILURE'
        List<DdiStatus> ddiStatusesWithNoErrorCode = List.of(
                getDdiStatus(DdiStatus.ExecutionStatus.FINISHED_FAILURE, null, MESSAGE, 1737348213000L)
        );
        String feedbackAsStringWithNoErrorCode = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoErrorCode);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoErrorCode)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("ErrorCode cannot be null for execution statuses FINISHED_FAILURE/LOG_UPLOAD_FAILURE/ERROR_RESPONSE_CODE/CANCELED_ACCEPT/CANCELED_REJECT", result.getResolvedException().getMessage()));

        // Scenario 2.1: ErrorCode is null for ExecutionStatus as 'ERROR_RESPONSE_CODE'
        List<DdiStatus> ddiStatusesWithNoErrorCode2 = List.of(
                getDdiStatus(DdiStatus.ExecutionStatus.ERROR_RESPONSE_CODE, null, MESSAGE, 1737348213000L)
        );
        String feedbackAsStringWithNoErrorCode2 = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoErrorCode2);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoErrorCode2)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("ErrorCode cannot be null for execution statuses FINISHED_FAILURE/LOG_UPLOAD_FAILURE/ERROR_RESPONSE_CODE/CANCELED_ACCEPT/CANCELED_REJECT", result.getResolvedException().getMessage()));

        // Scenario 2.2: ErrorCode is empty for ExecutionStatus as 'ERROR_RESPONSE_CODE'
        List<DdiStatus> ddiStatusesWithNoErrorCode3 = List.of(
                getDdiStatus(DdiStatus.ExecutionStatus.ERROR_RESPONSE_CODE, null, MESSAGE, 1737348213000L)
        );
        ddiStatusesWithNoErrorCode3.get(0).setErrorCode(Collections.singletonList(" "));

        String feedbackAsStringWithNoErrorCode3 = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoErrorCode3);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoErrorCode3)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("ErrorCode cannot be null for execution statuses FINISHED_FAILURE/LOG_UPLOAD_FAILURE/ERROR_RESPONSE_CODE/CANCELED_ACCEPT/CANCELED_REJECT", result.getResolvedException().getMessage()));

        // Scenario 3: InventoryHash is null for ExecutionStatus as 'FINISHED_SUCCESS'
        List<DdiStatus> ddiStatusesWithNoInventoryHash = List.of(
                getDdiStatus(DdiStatus.ExecutionStatus.FINISHED_SUCCESS, null, MESSAGE, 1737348213000L)
        );
        String feedbackAsStringWithNoInventoryHash = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoInventoryHash);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoInventoryHash)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("InventoryHash cannot be null for execution statuses FINISHED_SUCCESS and FINISHED_FAILURE and CANCELED_ACCEPT", result.getResolvedException().getMessage()));

        // Scenario 4: UserAcceptanceMessageJob1 is null for ExecutionStatus as 'USER_ACCEPTED'
        List<DdiStatus> ddiStatusesWithNoUserAcceptanceMessage = List.of(
                getDdiStatus(DdiStatus.ExecutionStatus.USER_ACCEPTED, null, MESSAGE, 1737348213000L)
        );
        String feedbackAsStringWithNoUserAcceptanceMessage = getJsonActionFeedbackObjectToSting(ddiStatusesWithNoUserAcceptanceMessage);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsStringWithNoUserAcceptanceMessage)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("UserAcceptanceMessageJob1 is required for execution status USER_ACCEPTED", result.getResolvedException().getMessage()));

        // Scenario 5: InventoryHash cannot be null for ExecutionStatus as 'CANCELED_ACCEPT'
        DdiStatus ddiStatus = getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.CANCELED_ACCEPT, null, MESSAGE, 1737348213000L);
        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId())
                        .content(feedbackAsString)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ValidationException))
                .andExpect(result -> assertEquals("InventoryHash cannot be null for execution statuses FINISHED_SUCCESS and FINISHED_FAILURE and CANCELED_ACCEPT", result.getResolvedException().getMessage()));
    }

    @Test
    @Description("Given multiple Ecus with multiple softwareModules and multiple versions for Rollout Update,proper artifact is selected")
    void givenMultipleEcusWhenExecutingDeploymentThenSuccess(CapturedOutput output) throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        // Create the first ECU model and retrieve its node ID
        List<MgmtCreateEcuModelResponse> ecuModelResponses1 = createEcuModel();
        String ecuNodeId1 = getEcuNodeId(ecuModelResponses1.get(0));

        // Create the second ECU model and retrieve its node ID
        List<MgmtCreateEcuModelResponse> ecuModelResponses2 = createEcuModel();
        String ecuNodeId2 = getEcuNodeId(ecuModelResponses2.get(0));

        long swModuleId1 = createSoftwareModule();

        List<Long> ecuModelIds = List.of(ecuModelResponses1.get(0).getId(), ecuModelResponses2.get(0).getId());
        softwareModuleManagement.assignEcuModel(swModuleId1, Collections.singletonList(ecuModelIds.get(0)));

        // Fetch the SoftwareModule using its ID
        Optional<SoftwareModule> optionalSoftwareModule1 = softwareModuleManagement.getSoftwareModuleById(swModuleId1);

        String softwareModuleName1 = "";
        if (optionalSoftwareModule1.isPresent()) {
            // Retrieve the name of the SoftwareModule
            softwareModuleName1 = optionalSoftwareModule1.get().getName();

        } else {
            // Handle the case where the SoftwareModule is not found
            throw new IllegalArgumentException("Software Module not found for ID: " + swModuleId1);
        }


        MgmtAddVersionResponse versionResponse1 = createSoftwareVersion(swModuleId1);
        MgmtAddVersionResponse versionResponse2 = createSoftwareVersion(swModuleId1);
        MgmtAddVersionResponse versionResponse3 = createSoftwareVersion(swModuleId1);
        MgmtArtifacts artifact1 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact2 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact3 = createDeltaArtifact(fileUrl);
        associateArtifactWithSoftwareModuleVersions(swModuleId1, versionResponse1.getId(), versionResponse2.getId(), artifact1.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId1, versionResponse1.getId(), versionResponse3.getId(), artifact2.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId1, versionResponse2.getId(), versionResponse3.getId(), artifact3.getArtifactId());


        long swModuleId2 = createSoftwareModule();
        softwareModuleManagement.assignEcuModel(swModuleId2, Collections.singletonList(ecuModelIds.get(0)));
        // Fetch the SoftwareModule using its ID
        Optional<SoftwareModule> optionalSoftwareModule2 = softwareModuleManagement.getSoftwareModuleById(swModuleId2);

        String softwareModuleName2 = "";
        if (optionalSoftwareModule2.isPresent()) {
            // Retrieve the name of the SoftwareModule
            softwareModuleName2 = optionalSoftwareModule2.get().getName();

        } else {
            // Handle the case where the SoftwareModule is not found
            throw new IllegalArgumentException("Software Module not found for ID: " + swModuleId2);
        }
        MgmtAddVersionResponse versionResponse4 = createSoftwareVersion(swModuleId2);
        MgmtAddVersionResponse versionResponse5 = createSoftwareVersion(swModuleId2);
        MgmtAddVersionResponse versionResponse6 = createSoftwareVersion(swModuleId2);
        MgmtArtifacts artifact4 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact5 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact6 = createDeltaArtifact(fileUrl);
        associateArtifactWithSoftwareModuleVersions(swModuleId2, versionResponse4.getId(), versionResponse5.getId(), artifact4.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId2, versionResponse4.getId(), versionResponse6.getId(), artifact5.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId2, versionResponse5.getId(), versionResponse6.getId(), artifact6.getArtifactId());


        long swModuleId3 = createSoftwareModule();
        softwareModuleManagement.assignEcuModel(swModuleId3, Collections.singletonList(ecuModelIds.get(1)));
        // Fetch the SoftwareModule using its ID
        Optional<SoftwareModule> optionalSoftwareModule3 = softwareModuleManagement.getSoftwareModuleById(swModuleId3);

        String softwareModuleName3 = "";
        if (optionalSoftwareModule3.isPresent()) {
            // Retrieve the name of the SoftwareModule
            softwareModuleName3 = optionalSoftwareModule3.get().getName();

        } else {
            // Handle the case where the SoftwareModule is not found
            throw new IllegalArgumentException("Software Module not found for ID: " + swModuleId2);
        }
        MgmtAddVersionResponse versionResponse7 = createSoftwareVersion(swModuleId3);
        MgmtAddVersionResponse versionResponse8 = createSoftwareVersion(swModuleId3);
        MgmtAddVersionResponse versionResponse9 = createSoftwareVersion(swModuleId3);
        MgmtArtifacts artifact7 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact8 = createDeltaArtifact(fileUrl);
        MgmtArtifacts artifact9 = createDeltaArtifact(fileUrl);
        associateArtifactWithSoftwareModuleVersions(swModuleId3, versionResponse7.getId(), versionResponse8.getId(), artifact7.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId3, versionResponse7.getId(), versionResponse9.getId(), artifact8.getArtifactId());
        associateArtifactWithSoftwareModuleVersions(swModuleId3, versionResponse8.getId(), versionResponse9.getId(), artifact9.getArtifactId());


        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        Target target = createTargets(VALID_CONTROLLER_ID);
        List<String> controllerIds = List.of(target.getControllerId());
        associateSoftwareModuleWithRollout(swModuleId1, versionResponse3.getId(), rollout.getId());
        associateSoftwareModuleWithRollout(swModuleId2, versionResponse6.getId(), rollout.getId());
        associateSoftwareModuleWithRollout(swModuleId3, versionResponse9.getId(), rollout.getId());
        associateEcuModelToVehicleModel(target.getVehicleModelId(), ecuModelResponses1.get(0).getId());
        associateEcuModelToVehicleModel(target.getVehicleModelId(), ecuModelResponses2.get(0).getId());
        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId1, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId1, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp3Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId2, controllerIds);
        updateFileStatus(ESP, esp3Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp4Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.VARIANT_CODING, ecuNodeId2, controllerIds);
        updateFileStatus(ESP, esp4Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
        addDevice(controllerIds, rollout.getId());


        invokeRolloutFreezeApi(rollout.getId());
        handleRollout(rolloutName, RolloutStatus.READY);
        startRollout(rollout.getId());

        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);

        JpaEsp esp1 = espRepository.findById(esp1Response.getSupportPackageId()).get();
        esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp1);

        JpaEsp esp2 = espRepository.findById(esp2Response.getSupportPackageId()).get();
        esp2.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp2);

        JpaEsp esp3 = espRepository.findById(esp3Response.getSupportPackageId()).get();
        esp3.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp3);

        JpaEsp esp4 = espRepository.findById(esp4Response.getSupportPackageId()).get();
        esp4.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp4);


        handleRollout(rolloutName, RolloutStatus.RUNNING);
        String controllerId = target.getControllerId();

        Version targetVersion1 = versionManagement.getById(versionResponse1.getId())
                .orElseThrow(() -> new IllegalStateException("Version not found"));
        JpaTargetSoftware targetSoftware1 = new JpaTargetSoftware(ecuNodeId1, softwareModuleName1, targetVersion1.getName(), target);

        Version targetVersion2 = versionManagement.getById(versionResponse4.getId())
                .orElseThrow(() -> new IllegalStateException("Version not found"));
        JpaTargetSoftware targetSoftware2 = new JpaTargetSoftware(ecuNodeId1, softwareModuleName2, targetVersion2.getName(), target);

        Version targetVersion3 = versionManagement.getById(versionResponse7.getId())
                .orElseThrow(() -> new IllegalStateException("Version not found"));
        JpaTargetSoftware targetSoftware3 = new JpaTargetSoftware(ecuNodeId2, softwareModuleName3, targetVersion3.getName(), target);


        Set<TargetSoftware> targetSoftwares = Set.of(targetSoftware1, targetSoftware2, targetSoftware3);

        controllerManagement.updateTargetSoftware(controllerId, targetSoftwares);


        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        MvcResult result = mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, target.getControllerId(), savedAction.getId())
                        .param("c", "1"))
                .andExpect(status().isOk())
                .andReturn();

        Optional<Artifacts> optionalArtifact = artifactsManagement.getArtifactsById(artifact2.getArtifactId());
        String artifactName2 = "";

        if (optionalArtifact.isPresent()) {

            artifactName2 = optionalArtifact.get().getFileName();

        } else {

            throw new IllegalArgumentException("Artifact not found for ID: " + artifact2.getArtifactId());
        }
        optionalArtifact = artifactsManagement.getArtifactsById(artifact5.getArtifactId());
        String artifactName5 = "";

        if (optionalArtifact.isPresent()) {

            artifactName5 = optionalArtifact.get().getFileName();

        } else {

            throw new IllegalArgumentException("Artifact not found for ID: " + artifact5.getArtifactId());
        }

        optionalArtifact = artifactsManagement.getArtifactsById(artifact8.getArtifactId());
        String artifactName8 = "";

        if (optionalArtifact.isPresent()) {

            artifactName8 = optionalArtifact.get().getFileName();

        } else {

            throw new IllegalArgumentException("Artifact not found for ID: " + artifact7.getArtifactId());
        }

        // Parse JSON response

        // Print the response content
        System.out.println(result.getResponse().getContentAsString());

        String jsonResponse = result.getResponse().getContentAsString();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonResponse);

        // Navigate to deploymentDescription.ecus
        JsonNode ecusArray = rootNode.path("deploymentDescription").path("ecus");

        for (JsonNode ecuNode : ecusArray) {
            String ecuId = ecuNode.path("ecuNodeId").asText(); // Changed from ecuId
            JsonNode softwareArray = ecuNode.path("software");

            System.out.println("ECU: " + ecuId);

            // Add your expected values per ECU ID
            if (ecuId.equals(ecuNodeId1)) {
                List<String> expectedFilenames = List.of(artifactName2, artifactName5);
                for (JsonNode softwareNode : softwareArray) {
                    String artifactName = softwareNode.path("swArtifact").path("filename").asText();
                    assertTrue(expectedFilenames.contains(artifactName),
                            "Unexpected artifact for ECU_ID_1: " + artifactName);
                }
            } else if (ecuId.equals(ecuNodeId2)) {
                List<String> expectedFilenames = List.of(artifactName8);
                for (JsonNode softwareNode : softwareArray) {
                    String artifactName = softwareNode.path("swArtifact").path("filename").asText();
                    assertTrue(expectedFilenames.contains(artifactName),
                            "Unexpected artifact for ECU_ID_2: " + artifactName);
                }
            } else {
                fail("Unexpected ECU ID found in response: " + ecuId);
            }


        }
    }

    @Test
    @Description("Verify that the new field 'deploymentEstimatedUpdateTime' is included in the deployment description")
    void givenDeploymentActionWhenFetchingThenIncludesNewField() throws Exception {

        Target target = setupRolloutAndGetTargetForDD();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);


        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);


        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        MvcResult result = mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, target.getControllerId(), savedAction.getId())
                        .param("c", "1")).andExpect(status().isOk())
                .andReturn();
        JsonNode jsonNode = extractDecodedJsonNode(result, "$.deploymentDescription");
        assertEquals(savedAction.getRollout().getDeploymentEstimatedUpdateTime().intValue(), jsonNode.get("deploymentMetadata").get("estimatedUpdateTime").asInt());
    }

    @Test
    @Description("Given empty ECU list, when executing deployment, then error should be returned")
    void givenEmptyEcusWhenExecutingDeploymentThenError(CapturedOutput output) throws Exception {
        Target target = setupRolloutAndGetTarget();

        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, target.getControllerId(), savedAction.getId())
                        .param("c", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("DD cannot be generated, no updatable ECUs/software modules found")));


    }

    /**
     * The test sets up two rollouts for the same device, both in RUNNING state.
     * It polls for the deployment action twice before feedback, asserting the response is the same.
     * After sending feedback for the first action, it polls again and asserts the response is now for the next action.
     **/

    @Test
    @Description("Given a device in multiple running rollouts, when polling for deployment, then same DD and inventory link are returned for the first DD_SENT action until feedback is received, after which the next action is returned")
    void getPollForUpdates_whenMultipleRollouts_thenReturnsSameDDUntilFeedback() throws Exception {
        // Given: Setup two rollouts with the same target (device)
        Target target = setupRolloutAndGetTarget();
        String controllerId = target.getControllerId();
        // Create a second rollout with the same target
        String controllerId2 = "VINTEST179126S";
        Target target2 = setupRolloutAndGetTarget("secondRollout", controllerId2);

        // Use deploymentManagement to get the first action
        final Action firstAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);
        final Action secondAction = deploymentManagement.findActionsByTarget(target2.getControllerId(), PAGE).getContent().get(0);

        // When: Poll for updates multiple times
        MvcResult firstPollResult = invokePollForUpdatesApi(controllerId);
        String firstInventoryLink = JsonPath.read(firstPollResult.getResponse().getContentAsString(), "$._links.inventory.href");
        String firstDD = firstPollResult.getResponse().getContentAsString();

        MvcResult secondPollResult = invokePollForUpdatesApi(controllerId);
        String secondInventoryLink = JsonPath.read(secondPollResult.getResponse().getContentAsString(), "$._links.inventory.href");
        String secondDD = secondPollResult.getResponse().getContentAsString();

        // Then: The same inventory link and DD should be returned for the first action
        assertEquals(firstInventoryLink, secondInventoryLink, "Inventory link should be the same for repeated polls");
        assertEquals(firstDD, secondDD, "DD should be the same for repeated polls");

        // When: Send feedback for the first action
        sendDeploymentActionFeedback(target, firstAction, "dd_accepted", null, null).andExpect(status().isOk());
        sendFinishedSuccessDeploymentActionFeedback(target, firstAction);

        // When: Poll for updates again
        MvcResult thirdPollResult = invokePollForUpdatesApi(controllerId2);
        String thirdInventoryLink = JsonPath.read(thirdPollResult.getResponse().getContentAsString(), "$._links.inventory.href");
        String thirdDD = thirdPollResult.getResponse().getContentAsString();

        // Then: The next available action's inventory link and DD should be returned
        assertNotEquals(firstInventoryLink, thirdInventoryLink, "Inventory link should change after feedback is sent");
        assertNotEquals(firstDD, thirdDD, "DD should change after feedback is sent");
    }

    @Test
    @Description("Mandate Inventory Hash for Execution Status Canceled Accept")
    void givenInventoryHashWhenCanceledAcceptFeedBackThenReturnsOkWithInventoryAndCancelLinks() throws Exception {
        Target target = setupRolloutAndGetTarget();

        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        sendCanceledAcceptDeploymentActionFeedback(target, savedAction)
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("canceled").value(true));

    }

    @Test
    @Description("Given a device with a valid controller ID, when sending inventory details, then it should return a success response")
    void givenValidControllerIdWhenSendingInventoryDetailsThenReturnsSuccess() throws Exception {

        var target = setupRolloutAndGetTargetForDD();
        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);


        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get(DdiRestConstants.GET_INVENTORY_DETAILS, target.getControllerId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].actionId", equalTo(savedAction.getId().intValue())));

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get(DdiRestConstants.GET_INVENTORY_DETAILS, target.getControllerId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").value(2)) // Validate the total count
                .andReturn();


    }

    @Test
    @Description("Handles invalid controller ID and valid controller ID with no records in one test")
    void givenInvalidAndValidControllerIdWhenFetchingInventoryDetailsThenReturnNotFoundOrEmpty() throws Exception {
        // 1️⃣ Invalid controller ID → should return 404 NOT FOUND
        String invalidControllerId = "non-existent-controller";

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, invalidControllerId)
                        .content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        // 2️⃣ Valid controller ID with no inventory → should return 200 OK with empty content
        var target = setupRolloutAndGetTargetForDD();
        String controllerId = target.getControllerId();

        mvc.perform(get(DdiRestConstants.GET_INVENTORY_DETAILS, controllerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.total").value(0));

    }

    @Test
    @Description("Given: A deployment log file is available for upload and the necessary setup for deployment management, S3 bucket, " +
            "and metadata storage is in place.  When: The file is uploaded using the deployment log upload API endpoint.  " +
            "Then: The file is successfully sent to the configured S3 bucket, and the corresponding metadata for the deployment " +
            "log is stored in the database.")
    void givenFileUploadWhenUploadingDeploymentLogThenUploadAndMetadataStored() throws Exception {

        var target = setupRolloutAndGetTargetForDD();

        Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        MockMultipartFile file = new MockMultipartFile(FILE, FILE, MediaType.APPLICATION_OCTET_STREAM_VALUE, "test".getBytes());

        when(s3MultipartFileUpload.uploadFileToS3(any(), any(S3FileUpload.class))).thenReturn("1234");

        mvc.perform(
                        multipart(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, target.getControllerId(), action.getId())
                                .file(file)
                                .queryParam("filename", FILE)
                                .queryParam("isLastFile", "true")
                                .queryParam("sequence", "1")
                                .accept(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(s3MultipartFileUpload, times(1)).uploadFileToS3(any(), any(S3FileUpload.class));

        String response = mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", CoreMatchers.equalTo(1)))
                .andExpect(jsonPath("total", CoreMatchers.equalTo(1)))
                .andExpect(jsonPath("$.content[0].file_name", CoreMatchers.equalTo(FILE)))
                .andExpect(jsonPath("$.content[0].file_size", CoreMatchers.equalTo(Math.toIntExact(file.getSize()))))
                .andExpect(jsonPath("$.content[0].sequence", CoreMatchers.equalTo(1)))
                .andExpect(jsonPath("$.content[0].is_last_file", CoreMatchers.equalTo(true)))
                .andExpect(jsonPath("$.content[0].sha256_hash", CoreMatchers.equalTo("1234")))
                .andReturn().getResponse().getContentAsString();


        DeploymentLog deploymentLogById = deploymentLogRepository.findById(1L).get();
        assertThat(deploymentLogById.getFilePath()).isNotNull();
        assertEquals("/" + action.getTenant() + "/" + action.getRollout().getId() + "/" + action.getId() + "/", deploymentLogById.getFilePath());

        // Negative: Missing file parameter
        mvc.perform(
                        multipart(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, target.getControllerId(), action.getId())
                                .queryParam("filename", FILE)
                                .queryParam("isLastFile", "true")
                                .queryParam("sequence", "1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        // Negative: Missing file parameter
        mvc.perform(
                        multipart(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, target.getControllerId(), action.getId())
                                .file(file)
                                .queryParam("isLastFile", "true")
                                .queryParam("sequence", "1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        // Negative: Missing file parameter
        mvc.perform(
                        multipart(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, target.getControllerId(), action.getId())
                                .file(file)
                                .queryParam("filename", FILE)
                                .queryParam("sequence", "1")
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200) // Successful response status
                .build();
        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);

        doThrow(new S3Exception("S3 upload failed")).when(s3MultipartFileUpload).uploadFileToS3(any(), any(S3FileUpload.class));
        mvc.perform(
                        multipart(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH, target.getControllerId(), action.getId())
                                .file(file)
                                .queryParam("filename", FILE)
                                .queryParam("isLastFile", "true")
                                .queryParam("sequence", "2")
                                .accept(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("S3 upload failed"));

    }

    @Test
    @Description("Given valid inventory details, when fetching DD, then sha256 from deploymentSignature payload matches computed sha256 of Base64-encoded deployment description")
    void givenValidDetailsWhenFetchDDThenSha256fromDescriptionAndSignatureMatches() throws Exception {
        var target = setupRolloutAndGetTargetForDD();

        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        final MvcResult result = mvc.perform(
                        get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH,
                                target.getControllerId(), savedAction.getId()).param("c", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = extractDecodedJsonNode(result, "$.deploymentDescription");
        Assertions.assertNotNull(jsonNode);

        // Extract JWT and payload
        String jwt = JsonPath.read(result.getResponse().getContentAsString(), "$.deploymentSignature");
        Assertions.assertNotNull(jwt);

        String[] parts = jwt.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JsonNode payloadJson = new ObjectMapper().readTree(payload);
        String expectedSha256 = payloadJson.get("sha256").asText();
        Assertions.assertNotNull(expectedSha256);

        // Extract deployment descriptor JSON
        JsonNode deploymentDescriptorNode = extractDecodedJsonNode(result, "$.deploymentDescription");

        String deploymentJson = deploymentDescriptorNode.toString();

        // Step 1: Convert JSON string to bytes using UTF-8
        byte[] inputBytes = deploymentJson.getBytes(StandardCharsets.UTF_8);

        // Step 2: Encode the byte array using Base64.
        String base64EncodedDescriptorJson = Base64.getEncoder().encodeToString(inputBytes);

        // Step 3: Compute SHA-256 hash of the UTF-8 input
        // Output: 32-byte (256-bit) hash
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha256Bytes = digest.digest(base64EncodedDescriptorJson.getBytes(StandardCharsets.UTF_8));

        // Way 1: Two-step approach (Hex ➜ Base64)
        // Step 4: Represent hash as hexadecimal
        // Example (lowercase hex): dc03b05862e5f437d9274c018f6524f880b614a42040da3dac6cf87d89ed26f4
        String hexDigest = bytesToHex(sha256Bytes);

        // Step 5: Base64-encode the SHA-256 hash (RFC 4648 standard)
        String base64Digest = Base64.getEncoder().encodeToString(sha256Bytes);

        // Step 6: Assert the computed Base64 SHA-256 with the expected value
        assertEquals(expectedSha256, base64Digest);

        // Way 2: Direct Base64 encoding of the raw SHA-256 bytes
        // This skips the intermediate hex step.
        // Equivalent result as “Output Encoding = Base64” in online SHA tools
        String base64DigestDirect = Base64.getEncoder().encodeToString(sha256Bytes);

        assertEquals(expectedSha256, base64DigestDirect);

    }

    /**
     * Helper to convert bytes to lowercase hex string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Extracts the Base64-encoded deployment description from a MockMvc response,
     * decodes it, and parses it into a {@link JsonNode} for validation and inspection.
     *
     * @param result the {@link MvcResult} object containing the API response
     * @return a {@link JsonNode} representing the decoded JSON structure
     * @throws Exception if extraction, decoding, or parsing fails
     */
    public static JsonNode extractDecodedJsonNode(MvcResult result, String node) throws Exception {
        // Retrieve the Base64-encoded deployment description from the API response body
        String base64DD = JsonPath.read(result.getResponse().getContentAsString(), node);

        // Decode the Base64 string to restore the original JSON content
        byte[] decodedBytes = Base64.getDecoder().decode(base64DD);
        String jsonDD = new String(decodedBytes, StandardCharsets.UTF_8);

        // Parse the decoded JSON into a JsonNode for structured access and assertions
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonDD);
    }


}
