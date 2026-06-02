package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
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
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import jakarta.validation.constraints.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class DdiDeploymentFeedbackTest extends AbstractDDiApiIntegrationTest {

    private static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String KAFKA_INVENTORY_ENDPOINT = "/kafka/inventory";
    private static final String ORIGINAL_FILENAME = "origFilename.csv";
    private static final String SIGNATURE_TYPE1 = "SHA256withECC";
    private static final String FINISH_SUCCESS = "Finish success";
    private static final String FINISH_FAILURE = "Finish failure";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_ARTIFACTS = "sp_artifacts";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    private static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    private static final String SP_TARGET_INVENTORY = "sp_target_inventory";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_RSP_ROLLOUT = "sp_rsp_rollout";
    private static final String SP_ESP_ECU_ROLLOUT = "sp_esp_ecu_rollout";
    private static final String SP_ESP = "sp_esp";
    private static final String SP_RSP = "sp_rsp";
    private static final String SP_TARGET = "sp_target";
    private static final String SP_ROLLOUT = "sp_rollout";
    private static final String VALID_CONTROLLER_ID = "1C4SJUFJ9NS100607";

    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private ArtifactsRepository artifactsRepository;
    @Autowired
    private RspRepository rspRepository;
    @Autowired
    private ObjectMapper objectMapper;
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
    @Autowired
    private EspRepository espRepository;
    @Autowired
    private ActionRepository actionRepository;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
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


    /**
     * Creates a mock PublishResponse object for testing purposes.
     *
     * @return a mock PublishResponse object
     */
    private PublishResponse createMockPublishResponse() {
        return PublishResponse.builder()
                .messageId("MOCK_MESSAGE_ID")
                .build();
    }

    /**
     * Mocks the behavior of the publish method to return the provided mock response.
     *
     * @param mockPublishResponse the mock response to be returned by the publish method
     */
    private void mockPublishBehavior(PublishResponse mockPublishResponse) {
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        lenient().when(s3FileTransferSnsService.publishMessage(any())).thenReturn(completedFuture);
    }

    /**
     * Invokes the API to add a new ECU model.
     *
     * @return a list of MgmtCreateEcuModelResponse objects representing the added ECU models
     * @throws Exception if an error occurs during the API invocation
     */

    @Test
    @Description("Given rollout with log collection is not required for finished success action status, when sending deployment action feedback with inventory mismatch, then return only the inventory link in the response")
    void givenRolloutWithLogCollectionNotRequiredForFinishedSuccessStatusWhenInventoryMismatchOccursThenReturnOnlyInventoryLink() throws Exception {
        Target target = setupRolloutWithDependencies(false);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        sendFinishedSuccessDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href").doesNotExist());
    }

    @Test
    @Description("Given rollout with log collection is not required for finished failure action status, when sending deployment action feedback with inventory mismatch, then return only the inventory link in the response")
    void givenRolloutWithLogCollectionNotRequiredForFinishedFailureStatusWhenInventoryMismatchOccursThenReturnOnlyInventoryLink() throws Exception {
        Target target = setupRolloutWithDependencies(false);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        sendFinishedFailureDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href").doesNotExist());
    }

    @Test
    @Description("Given rollout with log collection is required for finished success action status, when sending deployment action feedback with inventory mismatch, then return both inventory link and log collection link in the response")
    void givenLogCollectionRequiredForFinishedSuccessStatusWhenInventoryMismatchThenReturnInventoryLinkAndLogCollectionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        sendFinishedSuccessDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))));
    }

    @Test
    @Description("Given rollout with log collection is required for finished failure action status, when sending deployment action feedback with inventory mismatch, then return both inventory link and log collection link in the response")
    void givenLogCollectionRequiredForFinishedFailureStatusWhenInventoryMismatchThenReturnInventoryLinkAndLogCollectionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        sendFinishedFailureDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))));
    }

    @Test
    @Description("Given rollout with log collection is required for download action status, when sending deployment action feedback with inventory mismatch, then doesn't return any link in the response")
    void givenLogCollectionRequiredForNotFinishedStatusWhenInventoryMismatchThenReturnNoLinks() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        sendCanceledDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk()).andExpect(jsonPath("$._links.inventory.href").doesNotExist())
                .andExpect(jsonPath("$._links.logCollection.href").doesNotExist());
    }

    @Test
    @Description("Given rollout with log collection is required for finished failure action status, when sending deployment action feedback with no inventory mismatch, then return only log collection link in the response")
    void givenLogCollectionRequiredForFinishedFailureStatusWhenNoInventoryMismatchThenReturnOnlyLogCollectionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        invokePushDeviceInventoryApi(target.getControllerId(), inventoryDetails);
        sendDeploymentActionFeedback(target, inventorySignature, actions.get(0), "finished_failure", List.of(FINISH_FAILURE), Instant.now().getEpochSecond()).andExpect(status().isOk())
                .andExpect(status().isOk()).andExpect(jsonPath("$._links.inventory.href").doesNotExist())
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))));

    }

    @Test
    @Description("Given rollout with log collection is required for finished success action status, when sending deployment action feedback with no inventory mismatch, then return only log collection link in the response")
    void givenLogCollectionRequiredForFinishedSuccessStatusWhenNoInventoryMismatchThenReturnOnlyLogCollectionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        invokePushDeviceInventoryApi(target.getControllerId(), inventoryDetails);
        sendDeploymentActionFeedback(target, inventorySignature, actions.get(0), "finished_success", List.of(FINISH_SUCCESS), Instant.now().getEpochSecond()).andExpect(status().isOk())
                .andExpect(status().isOk()).andExpect(jsonPath("$._links.inventory.href").doesNotExist())
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))));
    }


    private Path generateTargetDevicesFile(List<String> controllerIds) throws IOException {
        Path filePath = Files.createTempFile("controllerIds", ".csv");
        Files.writeString(filePath, String.join("\n", controllerIds), StandardOpenOption.WRITE);
        return filePath;
    }


    void invokeCreateRolloutApi(final String name, final Long endDate, final MgmtRolloutStartType startType, boolean logCollectionRequired) throws Exception {
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(logCollectionRequired, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"),1000);
        Long startDate = startType.equals(MgmtRolloutStartType.SCHEDULED) ? Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond() : null;
        final String rollout = JsonBuilder.rollout(name, "description", MgmtRolloutPriority.REGULAR.getPriority(), startType.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, endDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, 1L)
                        .content(rollout)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }


    private void invokePushDeviceInventoryApi(String controllerId, String inventoryDetails) throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory", controllerId)
                        .content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    /**
     * Sets up a rollout with its dependencies.
     *
     * @param logCollectionRequired boolean indicating if log collection is required
     * @return a target object representing the created target
     * @throws Exception if an error occurs during setup
     */
    @NotNull
    private Target setupRolloutWithDependencies(boolean logCollectionRequired) throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();

        long swModuleId = createSoftwareModule();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        createRollout(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, logCollectionRequired);
        Rollout rollout = getRolloutByName(rolloutName);
        assertNotNull(rollout);

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
        esp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp);
        JpaEsp esp1 = espRepository.findById(supportPackages.get(1)).get();
        esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp1);
        JpaRsp rsp = rspRepository.findById(supportPackages.get(2)).get();
        rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);

        handleRollout(rolloutName, RolloutStatus.RUNNING);
        return target;
    }


    /**
     * Creates a new ECU model.
     *
     * @return the ID of the created ECU model
     * @throws Exception if an error occurs during the creation of the ECU model
     */
    private Long createECUModel() throws Exception {
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        return addEcuModelResponse.get(0).getId();
    }

    /**
     * Creates a new rollout with the specified parameters.
     *
     * @param rolloutName           the name of the rollout
     * @param rolloutEndDate        the end date of the rollout
     * @param startType             the start type of the rollout
     * @param logCollectionRequired whether log collection is required
     * @throws Exception if an error occurs during the creation of the rollout
     */
    private void createRollout(String rolloutName, Long rolloutEndDate, MgmtRolloutStartType startType, boolean logCollectionRequired) throws Exception {
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, startType, logCollectionRequired);
    }

    private ResultActions sendDeploymentActionFeedback(final Target target, final String inventoryHash, final Action action, final String execution, final List<String> messages, final
    Long timestamp) throws Exception {
        final String feedback = getJsonActionFeedbackWithTimestamp(DdiStatus.ExecutionStatus.valueOf(execution.toUpperCase()), messages, inventoryHash, timestamp);
        return sendDeploymentActionFeedback(target, action, feedback);

    }

    @Test
    @Description("when sending finished success feedback and device is not cancel then return success and not return cancel action link")
    void givenActionForFinishedSuccessStatusWhenRolloutAndActionIsNotCanceledThenNotReturnCancelActionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);

        sendFinishedSuccessDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))))
                .andExpect(jsonPath("$._links.cancelAction.href").doesNotExist());
    }

    @Test
    @Description("when sending finished failure feedback and device is not cancel then return success and not return cancel action link")
    void givenActionForFinishedFailureStatusWhenRolloutAndActionIsNotCanceledThenNotReturnCancelActionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);

        sendFinishedFailureDeploymentActionFeedback(target, actions.get(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$._links.inventory.href", startsWith(pushInventoryLink(target.getControllerId()))))
                .andExpect(jsonPath("$._links.logCollection.href", startsWith(deploymentLogLink(target.getControllerId(), actions.get(0).getId().toString()))))
                .andExpect(jsonPath("$._links.cancelAction.href").doesNotExist());

    }

    @Test
    @Description("when sending finished success feedback and device is in cancel then return canceled as true")
    void givenActionForFinishedStatusWhenRolloutAndActionInCancelThenReturnCancelActionLink() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        // Test for canceled action
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, actions.get(0).getRollout().getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        actionStatusRepository.deleteAll();

        sendCanceledDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                        .andExpect(jsonPath("canceled").value(true));

    }

    @Test
    @Description("When sending Cancel_Accept FeedBack then return with cancel Action Link and Action will be set to Canceled")
    void givenActionForCancelAcceptStatusWhenRolloutAndActionInCancelThenReturnCanceledTrue() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);

        sendCanceledAcceptDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                .andExpect(jsonPath("canceled").value(true));
        JpaAction updatedAction = actionRepository.findById(actions.get(0).getId()).orElseThrow();
        Assertions.assertThat(updatedAction.getStatus()).isEqualTo(DeviceActionStatus.CANCELED);
    }

    @Test
    @Description("When sending CancelReject FeedBack then return Action will be set to DD_SENT state")
    void givenActionForCancelRejectStatusWhenRolloutAndActionInCancelThenReturnCanceledFalse() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);

        sendCanceledRejectDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                .andExpect(jsonPath(("canceled")).value(false));


        JpaAction updatedAction = actionRepository.findById(actions.get(0).getId()).orElseThrow();
        Assertions.assertThat(updatedAction.getStatus()).isEqualTo(DeviceActionStatus.DD_SENT);
    }

    @Test
    @Description("When sending CancelReject it returns success after sending CancelAccept it will set to Canceled and will not revert")
    void givenActionForCancelRejectSWhenRolloutAndActionInCancelThenReturnCanceledTrue() throws Exception {
        Target target = setupRolloutWithDependencies(true);
        List<JpaAction> actions = actionRepository.findByTarget(target, true);

        sendCanceledRejectDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                .andExpect(jsonPath(("canceled")).value(false));

        JpaAction updatedAction = actionRepository.findById(actions.get(0).getId()).orElseThrow();
        Assertions.assertThat(updatedAction.getStatus()).isEqualTo(DeviceActionStatus.DD_SENT);


        sendCanceledAcceptDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                .andExpect(jsonPath("canceled").value(true));
        updatedAction = actionRepository.findById(actions.get(0).getId()).orElseThrow();
        Assertions.assertThat(updatedAction.getStatus()).isEqualTo(DeviceActionStatus.CANCELED);

        sendCanceledRejectDeploymentActionFeedback(target, actions.get(0)).
                andExpect(status().isOk())
                .andExpect(jsonPath(("canceled")).value(true));


        updatedAction = actionRepository.findById(actions.get(0).getId()).orElseThrow();
        Assertions.assertThat(updatedAction.getStatus()).isEqualTo(DeviceActionStatus.CANCELED);
    }

    @Test
    @Description("Given a rollout with associated actions and controllers, " +
            "when fetching action status by action ID and controller ID, " +
            "then return OK for valid pairs, Bad Request for mismatched controller, " +
            "and Not Found for missing action status")
    void givenRolloutWithActionsAndControllersWhenFetchingActionStatusByActionIdAndControllerIdThenReturnAppropriateResponse() throws Exception {
        // Arrange: Setup two targets and their actions
        Map<String, Object> stringObjectMap = setupRolloutWithTargets(2);
        List<Target> targets = (List<Target>) stringObjectMap.get("targets");
        Target firstTarget = targets.get(0);
        JpaRollout rollout = (JpaRollout) stringObjectMap.get("rollout");
        DdiDownload download = new DdiDownload(70, new DdiPackage(2, 5));

        // Act: Send feedback for the first target through all action states
        List<JpaAction> actions = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), firstTarget.getControllerId(), true);
        JpaAction firstAction = actions.get(0);

        sendDeploymentActionFeedback(firstTarget, firstAction, DD_SENT, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(firstTarget, firstAction, DOWNLOAD_STARTED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(firstTarget, firstAction, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, download)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(firstTarget, firstAction, DOWNLOAD_COMPLETED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
//        sendFinishedSuccessDeploymentActionFeedback(firstTarget, firstAction)
//                .andExpect(status().isOk());

        // Assert: Validate action status history for the first target
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), firstTarget.getControllerId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").exists())
                .andExpect(jsonPath("$[*].timestamp").exists())
                .andExpect(jsonPath("$.length()").value(actionStatusRepository.countByActionId(firstAction.getId())))
                .andExpect(result -> {
                    List<Integer> timestamps = JsonPath.read(result.getResponse().getContentAsString(), "$[*].timestamp");
                    for (int i = 1; i < timestamps.size(); i++) {
                        Assertions.assertThat(timestamps.get(i - 1)).isGreaterThanOrEqualTo(timestamps.get(i));
                    }
                })
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    List<Map<String, Object>> statuses = JsonPath.read(content, "$");
                    assertDownloadField(statuses, 70, 2, 5);
                });

        // Assert: Invalid controllerId and rollout for action returns BadRequest
        invokeGetActionStatusByRolloutIdAndControllerId(100000L, "randomControllerId")
                .andExpect(status().isNotFound())
                        .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Rollout with given identifier {100000} does not exist.")));

        List<JpaAction> secondActionList = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), targets.get(1).getControllerId(), true);
        JpaAction secondAction = secondActionList.get(0);

        // Assert: Invalid tenantId for rollout returns NotFound
        mvc.perform(
                        get(MgmtRestConstants.ACTION_STATUS_V1_REQUEST_MAPPING_TENANT, 100000, rollout.getId(), firstTarget.getControllerId())
                                .accept(MediaType.APPLICATION_JSON)
                ).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Act: No feedback sent for second target yet, expect empty status history
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), targets.get(1).getControllerId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").doesNotExist())
                .andExpect(jsonPath("$[*].timestamp").doesNotExist())
                .andExpect(jsonPath("$[*].download").doesNotExist())
                .andExpect(jsonPath("$.length()").value(0));

        // Act: Send feedback for the second target through all action states
        Target secondTarget = targets.get(1);
        sendDeploymentActionFeedback(secondTarget, secondAction, DD_SENT, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(secondTarget, secondAction, DOWNLOAD_STARTED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(secondTarget, secondAction, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, download)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(secondTarget, secondAction, DOWNLOAD_COMPLETED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
//        sendFinishedFailureDeploymentActionFeedback(secondTarget, secondAction)
//                .andExpect(status().isOk());

        // Assert: Validate action status history for the second target
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), secondTarget.getControllerId())
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").exists())
                .andExpect(jsonPath("$[*].timestamp").exists())
                .andExpect(jsonPath("$.length()").value(actionStatusRepository.countByActionId(secondAction.getId())))
                .andExpect(result -> {
                    List<Integer> timestamps = JsonPath.read(result.getResponse().getContentAsString(), "$[*].timestamp");
                    for (int i = 1; i < timestamps.size(); i++) {
                        Assertions.assertThat(timestamps.get(i - 1)).isGreaterThanOrEqualTo(timestamps.get(i));
                    }
                })
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    List<Map<String, Object>> statuses = JsonPath.read(content, "$");
                    assertDownloadField(statuses, 70, 2, 5);
                });

        // Assert: Invalid rolloutId for action returns NotFound
        invokeGetActionStatusByRolloutIdAndControllerId(100000L, targets.get(1).getControllerId())
                .andExpect(status().isNotFound());
        // Assert: Invalid controllerId for action returns NotFound
        invokeGetActionStatusByRolloutIdAndControllerId(firstAction.getId(),"randomControllerId")
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("When DOWNLOAD_IN_PROGRESS status is reported with valid download info, then verify all attributes are correct")
    void givenRolloutWithActionsAndControllersWhenDownloadInProgressWithValidDownloadThenDownloadFieldIsCorrect() throws Exception {
        // Arrange: Setup rollout with targets and actions
        Map<String, Object> stringObjectMap = setupRolloutWithTargets(1);
        List<Target> targets = (List<Target>) stringObjectMap.get("targets");
        Target target = targets.get(0);
        JpaRollout rollout = (JpaRollout) stringObjectMap.get("rollout");

        List<JpaAction> actions = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), target.getControllerId(), true);
        JpaAction action = actions.get(0);

        // Act: Send feedback DOWNLOAD_IN_PROGRESS
        DdiDownload download = new DdiDownload(80, new DdiPackage(3, 6));
        sendDeploymentActionFeedback(target, action, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, download).andExpect(status().isOk());

        sendDeploymentActionFeedback(target, action, DOWNLOAD_COMPLETED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null).andExpect(status().isOk());

        // Act + Assert: Fetch statuses and verify the download field
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), target.getControllerId()).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").exists())
                .andExpect(jsonPath("$[*].timestamp").exists())
                .andExpect(jsonPath("$.length()").value(actionStatusRepository.countByActionId(action.getId())))
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString();
                    List<Map<String, Object>> statuses = JsonPath.read(content, "$");
                    assertDownloadField(statuses, 80, 3, 6);
                });
    }

    @Test
    @Description("When download is not present, the API should accept the request")
    void givenRolloutWithActionsAndControllersWhenDownloadInProgressWithoutDownloadThenDownloadFieldCanBeNull() throws Exception {
        // Arrange: Setup rollout with targets and actions
        Map<String, Object> stringObjectMap = setupRolloutWithTargets(1);
        List<Target> targets = (List<Target>) stringObjectMap.get("targets");
        Target target = targets.get(0);
        JpaRollout rollout = (JpaRollout) stringObjectMap.get("rollout");

        List<JpaAction> actions = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), target.getControllerId(), true);
        JpaAction action = actions.get(0);

        // Act: Send feedback DOWNLOAD_IN_PROGRESS
        sendDeploymentActionFeedback(target, action, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null).andExpect(status().isOk());

        sendDeploymentActionFeedback(target, action, DOWNLOAD_COMPLETED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null).andExpect(status().isOk());

        // Act + Assert: Fetch statuses and verify the download field
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), target.getControllerId()).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").exists())
                .andExpect(jsonPath("$[*].timestamp").exists())
                .andExpect(jsonPath("$[*].download").doesNotExist())
                .andExpect(jsonPath("$.length()").value(actionStatusRepository.countByActionId(action.getId())));
    }

    @Test
    @Description("When download percentage is 0 and 100, it should be accepted and correctly reflected in the response")
    void givenRolloutWithActionsAndControllersWhenDownloadPercentageAtEdgeThenAccepted() throws Exception {
        // Arrange: Setup rollout with targets and actions
        Map<String, Object> stringObjectMap = setupRolloutWithTargets(1);
        List<Target> targets = (List<Target>) stringObjectMap.get("targets");
        Target target = targets.get(0);
        JpaRollout rollout = (JpaRollout) stringObjectMap.get("rollout");

        List<JpaAction> actions = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), target.getControllerId(), true);
        JpaAction action = actions.get(0);

        // Act: Send feedback DOWNLOAD_IN_PROGRESS
        DdiDownload zeroPercent = new DdiDownload(0, new DdiPackage(3, 6));
        sendDeploymentActionFeedback(target, action, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, zeroPercent).andExpect(status().isOk());

        DdiDownload fullPercent = new DdiDownload(100, new DdiPackage(1, 2));
        sendDeploymentActionFeedback(target, action, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, fullPercent).andExpect(status().isOk());

        sendDeploymentActionFeedback(target, action, DOWNLOAD_COMPLETED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null).andExpect(status().isOk());

        // Act + Assert: Fetch statuses and verify the download field
        invokeGetActionStatusByRolloutIdAndControllerId(rollout.getId(), target.getControllerId()).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status").exists())
                .andExpect(jsonPath("$[*].timestamp").exists())
                .andExpect(jsonPath("$.length()").value(actionStatusRepository.countByActionId(action.getId())))
                .andExpect(result -> {
                    List<Map<String, Object>> statuses = JsonPath.read(result.getResponse().getContentAsString(), "$");
                    // Assert presence of both cases
                    Assertions.assertThat(statuses.stream()
                                    .filter(s -> "DOWNLOAD_IN_PROGRESS".equals(s.get("status")) && s.get("download") != null)
                                    .map(s -> ((Map<String, Object>) s.get("download")).get("percentage")))
                            .contains(0, 100);
                });
    }

    @Test
    @Description("When invalid download percentage is sent, the API should reject the request")
    void givenRolloutWithActionsAndControllersWhenDownloadPercentageInvalidThenRequestRejected() throws Exception {
        // Arrange: Setup rollout with targets and actions
        Map<String, Object> stringObjectMap = setupRolloutWithTargets(1);
        List<Target> targets = (List<Target>) stringObjectMap.get("targets");
        Target target = targets.get(0);
        JpaRollout rollout = (JpaRollout) stringObjectMap.get("rollout");

        List<JpaAction> actions = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rollout.getId(), target.getControllerId(), true);
        JpaAction action = actions.get(0);

        // Act: Send feedback DOWNLOAD_IN_PROGRESS
        DdiDownload invalidDownload = new DdiDownload(150, new DdiPackage(1, 3));
        sendDeploymentActionFeedback(target, action, DOWNLOAD_IN_PROGRESS, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, invalidDownload).andExpect(status().isBadRequest());
    }

    @Description("Validate the download attribute value")
    private void assertDownloadField(List<Map<String, Object>> statuses, int percentage, int cnt, int of) {
        for (Map<String, Object> statusEntry : statuses) {
            String status = (String) statusEntry.get("status");
            Object download = statusEntry.get("download");

            if ("DOWNLOAD_IN_PROGRESS".equals(status)) {
                if (download != null) {
                    Assertions.assertThat(download).isInstanceOf(Map.class);

                    Map<String, Object> downloadMap = (Map<String, Object>) download;
                    Assertions.assertThat(downloadMap).containsEntry("percentage", percentage);

                    Map<String, Object> pkg = (Map<String, Object>) downloadMap.get("package");
                    Assertions.assertThat(pkg).containsEntry("cnt", cnt).containsEntry("of", of);
                }
                // else: acceptable, since download is optional
            } else {
                Assertions.assertThat(download).isNull();
            }
        }
    }
}
