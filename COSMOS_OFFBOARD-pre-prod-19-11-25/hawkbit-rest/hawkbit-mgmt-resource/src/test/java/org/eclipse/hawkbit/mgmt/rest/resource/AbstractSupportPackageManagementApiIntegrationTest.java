package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.ecu.dto.EcuModels;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignEcuModelRequestBody;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileDeleteSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.EcuModelRepository;
import org.eclipse.hawkbit.repository.jpa.EspEcuRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.VehicleRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.jdbc.JdbcTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public abstract class AbstractSupportPackageManagementApiIntegrationTest extends AbstractManagementApiIntegrationTest {

    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";
    protected static final String TEST_ECU_NODE_ADDRESS_1 = "30 A0";
    protected static final String TEST_ECU_NODE_ADDRESS_2 = "40 A0";
    protected static final String TEST_ECU_NODE_ADDRESS_3 = "50 A0";
    protected static final String ROLLOUT = "rollout";
    protected static final String TARGET = "target";
    protected static final String MOCK_ARTIFACT_FILE_URI = "http://localhost:%d/file-url";
    protected static final String TENANT = "{tenant}";
    protected static final String TYPE = "{type}";
    protected static final String SHA256 = "{SHA256}";
    protected static final String LATEST_FIRMWARE_UPDATE = "This file contains the latest firmware update.";
    protected static final String RELEASE_NOTES_URL = "https://example.com/release-notes";
    protected static final String REQUEST_BODY = "requestBody";
    protected static final String TARGETS = "targets";
    protected static final String ECU_MODELS = "ecuModels";
    protected static final String SET_TARGET = "SetTarget";
    protected static final String ROLLOUT_PREFIX = "RolloutPrefix";
    protected static final String ROLLOUT_NAME = "RolloutName";
    protected static final String DISTRIBUTION_SET_NAME = "DistributionSetName";
    protected static final String RANDOM_TARGET = "random-target-00001";
    protected static final String TARGET_00001 = "SetTarget-00001";
    protected static final String NOT_MATCH_WITH_ECU_NODE_ADDRESS = "ControllerIds do not match with ECU Node Address";
    public static final String RANDOM_TARGET_NOT_FOUND="Target with given identifier {random-target-00001} does not exist.";
    protected static final String NEW_CONTROLLER_ID = "new-controllerId";
    public static final String MOCK_MESSAGE_ID = "mockMessageId";
    public static final String CONTROLLER_ID = "controllerId";
    public static final String RSP_SET_TARGET = "RSPSetTarget";
    public static final String RSP_ROLLOUT_PREFIX = "RSPRolloutPrefix";
    public static final String RSP_ROLLOUT_NAME = "RSPRolloutName";
    public static final String RSP_DISTRIBUTION_SET_NAME = "RSPDistributionSetName";
    public static final String DIFFERENT_FILE = "different file";
    public static final String INVALID_FILE_TYPE = "invalidFileType";
    public static final String MISSING_FILE_URL = "missingFileUrl";
    public static final String INVALID_SHA_256 = "invalidSha256";
    public static final String RSP_DELETE_ROLLOUT_PREFIX = "RSPDeleteRolloutPrefix";
    public static final String RSP_DELETE_ROLLOUT_NAME = "RSPDeleteRolloutName";
    public static final String RSP_DELETE_DISTRIBUTION_SET_NAME = "RSPDeleteDistributionSetName";
    public static final String SUPPORT_PACKAGES_URL = "/support-packages/";
    public static final String SET_TEST_TARGET = "SetTestTarget";
    public static final String TEST_ROLLOUT_PREFIX = "TestRolloutPrefix";
    public static final String TEST_ROLLOUT_NAME = "TestRolloutName";
    public static final String TEST_DISTRIBUTION_SET_NAME = "TestDistributionSetName";
    public static final String ROLLOUT_1 = "rollout1";
    public static final String ROLLOUT_2 = "rollout2";
    protected static final String ROLLOUTS_URL = "/rollouts/";
    protected static final String TENANTS_V1_MANAGEMENT = "/management/v1/tenants/";
    protected static final String ESP = "ESP";
    protected static final String RSP = "RSP";
    public static final String TYPE_QUERY_PARAM = "type";
    protected static final String TEST_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
    protected static final String TEST_SUPPORT_PACKAGE_FILE_METADATA = "{\"size\":\"15MB\",\"createdBy\":\"admin\",\"releaseDate\":\"2024-06-30\"}";
    protected static final MockMultipartFile TEST_MOCKED_MULTIPART_FILE = new MockMultipartFile("file", "testFileName", MediaType.TEXT_PLAIN_VALUE, "TEST_CONTENT".getBytes());
    protected static final String VALID_CONTROLLER_ID = "1C4SJUFJ9NS100607";

    protected static ClientAndServer mockServer;
    @Autowired
    protected EntityFactory entityFactory;
    @MockBean
    MgmtS3Service s3Service;
    @Mock
    protected CdnUploadSnsService cdnUploadSnsService;
    @Mock
    protected CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    protected S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    protected SnsAsyncClient snsAsyncClient;
    @Mock
    protected S3FileDeleteSnsService s3FileDeleteSnsService;
    @MockBean
    protected S3MultipartFileUpload s3MultipartFileUpload;
    @Autowired
    EspEcuRolloutRepository espEcuRolloutRepository;

    @Autowired
    protected ArtifactsRepository artifactsRepository;

    @Autowired
    RspRepository rspRepository;
    @Autowired
    RspRolloutRepository rspRolloutRepository;
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    EcuModelRepository ecuModelRepository;
    @Autowired
    VehicleRepository vehicleRepository;
    @Autowired
    protected EspRepository espRepository;
    @Autowired
    protected RolloutRepository rolloutRepository;
    @Autowired
    protected DistributionSetRepository distributionSetRepository;
    @Autowired
    protected TargetRepository targetRepository;
    @Autowired
    protected ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    @Autowired
    protected S3Client s3Client;
    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;

    @Value("${log.collectionRequired}")
    protected Boolean collectionRequired;
    @Value("${log.maxSuccessVin}")
    protected Integer maxSuccessVin;
    @Value("${log.maxFailureVin}")
    protected Integer maxFailureVin;
    @Value("${log.maxLogAllFileSize}")
    protected Integer maxSize;
    @Value("${log.maxNumberOfFiles}")
    protected Integer maxFile;
    @Value("${cosmos.server.s3.support-package.bucket.name}")
    protected String supportPackageBucketName;

    @BeforeAll
    static void mockPublishRolloutStatus() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_ROLLOUT_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    @BeforeEach
    void setup() throws Exception {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                "sp_artifact_software_module", "sp_artifacts", "sp_software_versions", "sp_esp_ecu_rollout",
                "sp_software_ecu_model", "sp_ecu_model", "sp_vehicle_model", "sp_target_inventory",
                "sp_base_software_module", "sp_rsp_rollout", "sp_esp_ecu_rollout",
                "sp_esp", "sp_rsp", "sp_target", "sp_software_ecu_model",
                "sp_base_software_module", "sp_rollout", "sp_distribution_set"
        );

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    static String generateSha256Hash() throws NoSuchAlgorithmException {
        return generateSha256Hash("sampledata");
    }

    @AfterEach
    public void tearDown() {
        log.info("Cleaning up the database");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifact_software_module",
                "sp_artifacts",
                "sp_software_versions",
                "sp_software_ecu_model",
                "sp_target_inventory",
                "sp_target",
                "sp_esp_ecu_rollout",
                "sp_rsp_rollout",
                "sp_esp",
                "sp_rsp",
                "sp_vehicle_model",
                "sp_base_software_module",
                "sp_rollout",
                "sp_distribution_set",
                "sp_ecu_model");
    }

    protected Map<String, Object> prepareTestDataForCreatingSupportPackageTestWithoutVins(SupportPackageTestData testData) throws Exception {


        //Create ECU Model
        if (testData.getEcuNodeAddress() != null || testData.getEcuNodeAddress().isEmpty()) {
            testdataFactory.addNewEcuModels(createEcuModel(testData.getEcuNodeAddress()));
        }

        // Create rollout including the created targets with the provided prefix
        invokeCreateRolloutApi(testData.getRolloutName(), Instant.now().plus(49, ChronoUnit.HOURS).getEpochSecond());
        Rollout rollout=rolloutManagement.getByName(testData.getRolloutName()).orElse(null);
        // Create request body using the provided parameters
        MgmtBaseSupportPackageCreateRequest requestBody =  MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(testData.getFileUrl())
                .fileName(testData.getTestFileName())
                .fileType(testData.getFileType())
                .sha256(testData.getSha256())
                .fileVersion(testData.getVersion())
                .controllerIds(List.of())
                .ecuNodeAddress(testData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(testData.getFileMetadata())
                .build();

        PublishResponse publishResponse = PublishResponse.builder().messageId("test-message-id").build();
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(CompletableFuture.completedFuture(publishResponse));

        Assertions.assertNotNull(rollout);
        return Map.of(ROLLOUT, rollout, REQUEST_BODY, requestBody);
    }

    protected Map<String, Object> setupRolloutWithTargets(int noOfTargets) throws Exception {
        Map<String, Object> result = new HashMap<>();
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

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        List<Target> targets = testdataFactory.createTargets(noOfTargets);
        result.put("vehicleModelId", vehicleModelId);
        result.put("targets", targets);
        result.put("rollout", rollout);
        result.put("ecuNodeId", ecuNodeId);

        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        targets.forEach(target -> {
            try {
                associateEcuModelToVehicleModel(target.getVehicleModelId(), ecuModelResponses.get(0).getId());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        result.put("supportPackageUrl", esp1Response.getFileUrl());

        addDevice(controllerIds, rollout.getId());

        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        invokeRolloutStartApi(rollout.getId());

        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);

        JpaEsp esp1 = espRepository.findById(esp1Response.getSupportPackageId()).get();
        esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp1);

        JpaEsp esp2 = espRepository.findById(esp2Response.getSupportPackageId()).get();
        esp2.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp2);

        handleRollout();
        handleRollout();
        return result;
    }

    protected Long createVehicleModel() throws Exception {
        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        return vehicleCreateResponse.get(0).getId();
    }

    protected List<MgmtCreateEcuModelResponse> createEcuModel() throws Exception {
        return invokeAddEcuModelApi();

    }

    protected void associateEcuModelToSoftwareModule(long swModuleId, long ecuNodeId) throws Exception {
        invokeAssociateEcuModelToSoftwareModuleApi(swModuleId, ecuNodeId);
    }

    protected void associateArtifactWithSoftwareModule(long swModuleId, long versionId, long artifactId) throws Exception {
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionId), Math.toIntExact(artifactId));
        changeArtifactStatus(artifactsRepository.findById(artifactId).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
    }

    protected void invokeAssociateEcuModelToSoftwareModuleApi(long swModuleId, long ecuNodeId) throws Exception {
        EcuModels ecuModels = new EcuModels();
        ecuModels.setEcuModelId(ecuNodeId);
        MgmtAssignEcuModelRequestBody requestBody = MgmtAssignEcuModelRequestBody.builder().ecuModels(
                List.of(ecuModels)
        ).build();
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, swModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    protected void updateFileStatus(String packageType, long supportPackageId, FileTransferStatus status) {
        if (packageType.equals(RSP)) {
            JpaRsp rsp = rspRepository.findById(supportPackageId).orElse(null);
            assertNotNull(rsp);
            rsp.setFileStatus(status.toString());
            rspRepository.save(rsp);
        } else {
            JpaEsp esp = espRepository.findById(supportPackageId).orElse(null);
            assertNotNull(esp);
            esp.setFileStatus(status.toString());
            espRepository.save(esp);
        }
    }

    protected MgmtArtifacts createArtifact(String fileUrl) throws Exception {
        return invokeCreateArtifactViaUrlApi(fileUrl);
    }

    protected MgmtAddVersionResponse createSoftwareVersion(long swModuleId) throws Exception {
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        return invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
    }

    protected List<MgmtTarget> createTargets(Long vehicleModelId, int amountTargets) throws Exception {
        return invokeCreateTargetApi(vehicleModelId, amountTargets);
    }

    protected long createSoftwareModule() throws Exception {
        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        return softwareModules.get(0).getModuleId();
    }

    protected void associateEcuModelToVehicleModel(Long vehicleModelId, Long ecuModelId) throws Exception {
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);
    }

    protected void addDevice(List<String> controllerIds, long rolloutId) throws Exception {
        invokeAddDeviceApi(controllerIds, rolloutId);
    }

    protected List<String> getControllerIds(List<MgmtTarget> targets) {
        return targets.stream().map(MgmtTarget::getControllerId).toList();
    }

    protected void associateSoftwareModuleWithRollout(long swModuleId, long versionId, long rolloutId) throws Exception {
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionId);
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rolloutId);
    }

    protected String getEcuNodeId(MgmtCreateEcuModelResponse response) throws Exception {

        return response.getEcuNodeId();
    }

    protected MgmtSupportPackage createSupportPackage(long rolloutId, String supportPackageUrl, MgmtSupportPackageFileType fileType, String ecuNodeId, List<String> controllerIds) throws Exception {
        MgmtBaseSupportPackageCreateRequest createRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(fileType, ecuNodeId, supportPackageUrl, controllerIds);
        return invokeCreateSupportPackageApi(rolloutId, createRequest);
    }

    protected MgmtAddDeviceDetailsResponse addDeviceDetails(List<String> controllerIds, long rolloutId, int totalGroups) throws Exception {
        return invokeAddDeviceApi(controllerIds, rolloutId, totalGroups);
    }

    protected void handleRollout() {
        rolloutHandler.handleAll();
    }

}
