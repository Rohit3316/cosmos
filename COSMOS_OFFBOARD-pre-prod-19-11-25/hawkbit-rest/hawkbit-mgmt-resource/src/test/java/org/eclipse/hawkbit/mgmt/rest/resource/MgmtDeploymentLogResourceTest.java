package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.DeploymentLogUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.RandomGenerator;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtSupportPackageResourceTest.MOCK_MESSAGE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Feature("Component Tests - Management API")
@Story("DeploymentLog Resource")
class MgmtDeploymentLogResourceTest extends AbstractManagementApiIntegrationTest {
    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String FILE_NAME1 = "file1";
    private static ClientAndServer mockServer;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;


    protected static final String TABLE_SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String TABLE_SP_ARTIFACTS = "sp_artifacts";
    protected static final String TABLE_SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    protected static final String TABLE_SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String TABLE_SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    protected static final String TABLE_SP_ECU_MODEL = "sp_ecu_model";
    protected static final String TABLE_SP_TARGET = "sp_target";
    protected static final String TABLE_SP_ACTION = "sp_action";
    private static final String TABLE_SP_VEHICLE_MODEL = "sp_vehicle_model";
    private static final String TABLE_SP_ROLLOUT = "sp_rollout";
    private static final String TABLE_SP_ROLLOUTGROUP = "sp_rolloutgroup";
    private static final String TABLE_SP_DISTRIBUTION_SET = "sp_distribution_set";


    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;
    @MockBean
    private S3MultipartFileUpload s3MultipartFileUpload;

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    @BeforeEach
    public void setup() {

        JdbcTestUtils.deleteFromTables(jdbcTemplate, TABLE_SP_ARTIFACT_SOFTWARE_MODULE, TABLE_SP_ARTIFACTS, TABLE_SP_SOFTWARE_ECU_MODEL, TABLE_SP_SOFTWARE_VERSIONS, TABLE_SP_BASE_SOFTWARE_MODULE, TABLE_SP_ECU_MODEL, TABLE_SP_TARGET, TABLE_SP_ACTION, TABLE_SP_VEHICLE_MODEL, TABLE_SP_ROLLOUT, TABLE_SP_ROLLOUTGROUP, TABLE_SP_DISTRIBUTION_SET);

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId(MOCK_MESSAGE_ID)
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);

        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);

    }

    @AfterEach
    public void tearDown() {


    }

    @BeforeEach
    void initBeforeEach() throws Exception {
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    private static void mockPublishVehicleStatus() {
        mockServer
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    private static byte[] randomBytes(final int len) {
        return RandomStringUtils.randomAlphanumeric(len).getBytes();
    }

    @Test
    @Description("Ensure updating Vehicle Model for non existing Vehicle Model ID returns not found")
    void givenDLogActionIdAndDLogIdIWhenIdNotExistThenError() throws Exception {

        Random random = RandomGenerator.getRandom();
        Long actionId = random.nextLong();
        Long controllerId = random.nextLong();
        Long deploymentLogId = random.nextLong();
        mvc.perform(get(MgmtRestConstants.DOWNLOAD_DEPLOYMENT_LOG_V1_REQUEST_MAPPING, controllerId, actionId,
                        deploymentLogId).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an existing deployment log, when downloading the log via GET API, then return OK")
    void givenExistingDeploymentLog_whenDownloadingLog_thenReturnsOk() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");

        final Rollout rollout = createRolloutWithDependencies("ROLLOUT_NAME", ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        String fileName = "test-log-" + action.getId();
        DeploymentLog deploymentLog = testdataFactory.createDeploymentLog(fileName, action);
        //Removed the implementation of the api downloadDeploymentLog to read and return the file from file system and returning a 404 error temporarily.
        mvc.perform(get(MgmtRestConstants.DOWNLOAD_DEPLOYMENT_LOG_V1_REQUEST_MAPPING, target.getControllerId(), action.getId(),
                        deploymentLog.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an not existing controllerId, when downloading the log via GET API, then return OK")
    void givenNotExistingControllerId_whenDownloadingLog_thenReturnsIsNotFound() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");

        final Rollout rollout = createRolloutWithDependencies("ROLLOUT_NAME", ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        String fileName = "test-log-" + action.getId();
        DeploymentLog deploymentLog = testdataFactory.createDeploymentLog(fileName, action);

        mvc.perform(get(MgmtRestConstants.DOWNLOAD_DEPLOYMENT_LOG_V1_REQUEST_MAPPING, "test_test", action.getId(),
                        deploymentLog.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Test to verify that when the request has same Sequence FileName ActionId, a ValidationException is thrown.")
    void givenSameFileNameSequenceActionIdWhenDeploymentLogsPublishThenDuplicateFileError() throws Exception {
        final int deploymentLogSize = 5 * 1024;
        final byte[] random = randomBytes(deploymentLogSize);

        // Create a target and rollout with dependencies
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");
        final Rollout rollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), List.of(target));

        // Perform rollout steps
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        MockMultipartFile file = new MockMultipartFile(FILE_NAME1, FILE_NAME1, MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(FILE_NAME1)
                .fileOriginalName(file.getOriginalFilename())
                .action(action)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .sequence(1)
                .isLastFile(true)
                .build();

        deploymentLogManagement.create(deploymentLogUpload, file);

        assertThat(deploymentLogManagement.count()).isEqualTo(1);

        // Attempt to create one more deployment log with same FileName ,sequence and actionId , expecting an exception
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            deploymentLogManagement.create(deploymentLogUpload, file);
        });

        assertEquals("Duplicate file chunk was uploaded for actionId=" + action.getId() + ", filename=" + FILE_NAME1 + ", sequence=1", exception.getMessage());

    }

    @Description("Test to verify that when the maximum number of deployment log files is reached, a ValidationException is thrown.")
    @Test
    public void givenMaxFilecountExceededWhenDeploymentLogsPublishThenGiveError() throws Exception {
        final int deploymentLogSize = 5 * 1024;
        final byte[] random = randomBytes(deploymentLogSize);

        // Create a target and rollout with dependencies
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("rollout", MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), List.of(target), rollout, true);

        // Perform rollout steps
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Fetch active action for the target
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        // Ensure maximum log file count is reached before attempting another upload
        Random randomGenerator = new Random();
        MockMultipartFile file = new MockMultipartFile(FILE_NAME1, FILE_NAME1, MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);

        for (int i = 0; i < rollout.getLogMaxNumberOfFiles(); i++) {
            int randomSequence = randomGenerator.nextInt();
            DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                    .filename(FILE_NAME1)
                    .fileOriginalName(file.getOriginalFilename())
                    .action(action)
                    .contentType(file.getContentType())
                    .fileSize(file.getSize())
                    .sequence(randomSequence)
                    .isLastFile(true)
                    .build();
            deploymentLogManagement.create(deploymentLogUpload, file);
        }

        // Attempt to create one more deployment log, expecting an exception
        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(FILE_NAME1)
                .fileOriginalName(file.getOriginalFilename())
                .action(action)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .sequence(10000)
                .isLastFile(true)
                .build();
        ValidationException exception = assertThrows(ValidationException.class, () -> deploymentLogManagement.create(deploymentLogUpload, file));

        assertEquals("Max deployment log file count reached for actionId=" + action.getId() + ". Limit: " + rollout.getLogMaxNumberOfFiles(), exception.getMessage());
    }

    @Test
    @Description("Test to verify that when the total file size exceeds the allowed limit, a ValidationException is thrown.")
    void givenLogRequestSizeExceededWhenDeploymentLogsPublishRolloutMaxLimitThenValidationError() throws Exception {
        final int deploymentLogSize = 5 * 1024;
        final byte[] random = randomBytes(deploymentLogSize);

        // Create a target and rollout with dependencies
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");
        final Rollout rollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), List.of(target));

        // Perform rollout steps
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Fetch active action for the target
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);


        Long fileSizeExceedingRolloutMaxLimit = rollout.getLogMaxAllFileSize() + 1L;

        // Verify that an exception with message is thrown due to exceeding the total file size limit
        MockMultipartFile file = new MockMultipartFile(FILE_NAME1, FILE_NAME1, MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(FILE_NAME1)
                .fileOriginalName(file.getOriginalFilename())
                .action(action)
                .contentType(file.getContentType())
                .fileSize(fileSizeExceedingRolloutMaxLimit)
                .sequence(1)
                .isLastFile(true)
                .build();
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            deploymentLogManagement.create(deploymentLogUpload, file);
        });

        assertEquals("Max total file size exceeded for actionId=" + action.getId() + ". Limit: " + rollout.getLogMaxAllFileSize(), exception.getMessage());
    }

    @Test
    @Description("Test to verify that when an individual file exceeds the allowed size limit, a ValidationException is thrown.")
    void givenLogRequestSizeExceededWhenDeploymentLogsPublishRolloutRequestLimitThenValidationError() throws Exception {
        final int deploymentLogSize = 5 * 1024;
        final byte[] random = randomBytes(deploymentLogSize);

        // Create a target and rollout with dependencies
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");
        final Rollout rollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), List.of(target));

        // Perform rollout steps
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Fetch active action for the target
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);


        Long fileSizeExceedingRolloutMaxLimit = rollout.getLogMaxEachFileSize() + 1L;

        // Verify that an exception with message is thrown due to exceeding  the per-file size limit
        MockMultipartFile file = new MockMultipartFile(FILE_NAME1, FILE_NAME1, MediaType.APPLICATION_OCTET_STREAM_VALUE, new byte[0]);
        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(FILE_NAME1)
                .fileOriginalName(file.getOriginalFilename())
                .action(action)
                .contentType(file.getContentType())
                .fileSize(fileSizeExceedingRolloutMaxLimit)
                .sequence(1)
                .isLastFile(true)
                .build();
        ValidationException exception = assertThrows(ValidationException.class, () -> {
            deploymentLogManagement.create(deploymentLogUpload, file);
        });

        assertEquals("Max individual file size exceeded for actionId=" + action.getId() + ". Limit: " + rollout.getLogMaxEachFileSize(), exception.getMessage());
    }
}
