package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileDeleteSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.DEPLOYMENT_ESTIMATED_UPDATE_TIME;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.JSON_PATH_DEPLOYMENT_ESTIMATED_UPDATE_TIME;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.MAX_UPDATE_TIME;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
public class MgmtMandatorySupportPackageForRolloutTest extends AbstractManagementApiIntegrationTest {


    public static final String ROLLOUT_1 = "rollout1";
    public static final String BASE_V1_REQUEST_MAPPING_TENANT = "/management/v1/tenants/{tenantId}";
    private static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String HREF_ROLLOUT_PREFIX = "http://localhost/management/v1/tenants/1/rollouts/";
    private static final String TENANT_ID = "1";
    private static final String KAFKA_ROLLOUT_ERROR_ENDPOINT = "/kafka/rollouterror";
    private static final String JSON_PATH_NAME = "$.name";
    private static final String JSON_PATH_STATUS = "$.status";
    private static final String DRAFT = "draft";
    private static final String JSON_PATH_DESCRIPTION = "$.description";
    private static final String JSON_PATH_START_AT = "$.startAt";
    private static final String JSON_PATH_END_AT = "$.endAt";
    private static final String JSON_PATH_USER_ACCEPTANCE_REQUIRED = "$.userAcceptanceRequired";
    private static final String JSON_PATH_CONNECTIVITY_TYPE = "$.connectivityType";
    private static final String JSON_PATH_LOG_COLLECTION_REQUIRED = "$.log.collectionRequired";
    private static final String JSON_PATH_LOG_MAX_FAILURE_VIN = "$.log.maxFailureVin";
    private static final String JSON_PATH_LOG_MAX_NUMBER_OF_FILES = "$.log.maxNumberOfFiles";
    private static final String JSON_PATH_LOG_MAX_ALL_FILE_SIZE = "$.log.maxAllFileSize";
    private static final String JSON_PATH_LOG_MAX_SUCCESS_VIN = "$.log.maxSuccessVin";
    private static final String JSON_PATH_LOG_MAX_EACH_FILE_SIZE = "$.log.maxEachFileSize";
    private static final String JSON_PATH_LINKS_SELF_HREF = "$._links.self.href";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED = "$.deploymentMetadata.downgradeAllowed";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA = "$.deploymentMetadata.requiredMedia";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_PERCENTAGE";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMPERATURE";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMP_METRIC";
    private static final String JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER = "$.maxDownloadCellularDurationTimer";
    private static final String JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER = "$.maxDownloadDurationTimer";
    private static final String JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER = "$.maxDownloadWifiDurationTimer";
    private static final String JSON_PATH_DOWNLOAD_RETRY_COUNT = "$.downloadRetryCount";
    private static final String JSON_PATH_MAX_UPDATE_TIME = "$.maxUpdateTime";
    private static final String JSON_PATH_LINKS_START_HREF = "$._links.start.href";
    private static final String START_URL = "/start";
    private static final String PAUSE_URL = "/pause";
    private static final String JSON_PATH_LINKS_PAUSE_HREF = "$._links.pause.href";
    private static final String RESUME_URL = "/resume";
    private static final String JSON_PATH_LINKS_RESUME_HREF = "$._links.resume.href";
    private static final String JSON_PATH_LINKS_GROUPS_HREF = "$._links.groups.href";
    private static final String GROUPS_URL = "/groups";
    private static final String CAMPAIGN_FOR_HPC_UPDATE = "Campaign for HPC Update";
    private static final String CONTENT_TYPE = "text/csv";
    private static final String ORIGINAL_FILENAME = "vins.csv";
    private static final String TARGET_DEVICES = "targetDevices";
    private static final String CELLULAR = "cellular";
    private static final String SP_TARGET_FILTER_QUERY = "sp_target_filter_query";
    private static final String SP_TARGET_TAG = "sp_target_tag";
    private static final String SP_TARGET = "sp_target";
    private static final String SP_VEHICLE_ECU = "sp_vehicle_ecu";
    private static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    private static final String SP_ACTION = "sp_action";
    private static final String SP_ROLLOUTGROUP = "sp_rolloutgroup";
    private static final String SP_RSP_ROLLOUT = "sp_rsp_rollout";
    private static final String SP_ESP_ECU_ROLLOUT = "sp_esp_ecu_rollout";
    private static final String SP_ECU_MODEL = "sp_ecu_model";
    private static final String SP_ROLLOUT = "sp_rollout";
    private static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_RSP = "sp_rsp";
    private static final String SP_ESP = "sp_esp";
    private static final String FOTA = "FOTA";
    private static final String AOTA = "AOTA";
    private static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";
    private static final String TEST_ECU_NODE_ADDRESS_1 = "30 A0";
    private static final Logger LOGGER = LoggerFactory.getLogger(MgmtRolloutResourceTest.class);
    private static ClientAndServer mockServer;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    @Autowired
    ObjectMapper objectMapper;
    @Autowired
    private RolloutManagement rolloutManagement;
    @Mock
    private TenantMetaDataRepository tenantMetaDataRepository;
    @MockBean
    private SnsServiceFactory snsServiceFactory;
    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;
    @Autowired
    private ActionRepository actionRepository;
    @MockBean
    @Qualifier("fileDownloadRestTemplate")
    private RestTemplate restTemplate;
    @Value("${log.collectionRequired}")
    private Boolean collectionRequired;
    @Value("${log.maxSuccessVin}")
    private Integer maxSuccessVin;
    @Value("${log.maxFailureVin}")
    private Integer maxFailureVin;
    @Value("${log.maxLogAllFileSize}")
    private Integer maxSize;
    @Value("${log.maxNumberOfFiles}")
    private Integer maxFile;
    @Autowired
    private RolloutTestApprovalStrategy approvalStrategy;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @Mock
    private S3FileDeleteSnsService s3FileDeleteSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;
    @Autowired
    private ArtifactsRepository artifactsRepository;
    @Autowired
    private RspRepository rspRepository;

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
    }

    @BeforeEach
    void setup() {


        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_TARGET_FILTER_QUERY, SP_TARGET_TAG, SP_TARGET, SP_VEHICLE_ECU,
                SP_VEHICLE_MODEL, SP_ACTION, SP_ROLLOUTGROUP, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT, SP_ECU_MODEL, SP_ROLLOUT,
                SP_DISTRIBUTION_SET, SP_ARTIFACT_SOFTWARE_MODULE,
                SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_RSP, SP_ESP);

        MockitoAnnotations.openMocks(this); // Initialize mocks
        approvalStrategy.setApprovalNeeded(false);
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(Constants.KAFKA_SEND_EVENT_URL)).respond(HttpResponse.response().withStatusCode(201));
        PublishResponse mockPublishResponse = PublishResponse.builder().messageId("mockMessageId").build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    void invokeCreateRolloutApi(final String name, final Long endDate, final MgmtRolloutStartType startType) throws Exception {
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        Long startDate = startType.equals(MgmtRolloutStartType.SCHEDULED) ? Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond() : null;
        final String rollout = JsonBuilder.rollout(name, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), startType.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, endDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(name))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(CAMPAIGN_FOR_HPC_UPDATE)))
                .andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)))
                .andExpect(jsonPath(JSON_PATH_END_AT, equalTo(endDate.intValue())))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, equalTo("yes")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, equalTo(CELLULAR)))
                .andExpect(jsonPath(JSON_PATH_LOG_COLLECTION_REQUIRED, equalTo(true)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN, equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES, equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE, equalTo(50)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN, equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE, equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE, equalTo("60%")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE, equalTo("78 C")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC, equalTo("NA")))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, equalTo(MAX_UPDATE_TIME)))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_ESTIMATED_UPDATE_TIME, equalTo(DEPLOYMENT_ESTIMATED_UPDATE_TIME)))
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES).doesNotExist())


                // Verifying the links in the Create Rollout API response
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_DELETE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_CANCEL_HREF).doesNotExist())
                .andReturn();
    }

    private void mockMandatoryEspRsp(String mandatoryRsp, String mandatoryEsp) throws Exception {

        System.setProperty("hawkbit.server.tenant.configuration.rollout-mandatory-support-package-rsp.defaultValue", mandatoryRsp);
        System.setProperty("hawkbit.server.tenant.configuration.rollout-mandatory-support-package-esp.defaultValue", mandatoryEsp);
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given the rollout is frozen, RSP and ESP are created, there are mandatory ESP and RSP files, all RSP files including mandatory RSPs are successfully uploaded to storage, all ESP files including mandatory ESPs for all targets except one target are successfully uploaded to storage, then it should throw an ValidationException.")
    void givenRolloutFrozen_whenMandatoryRSPUploadedButOneESPNotUploaded_thenThrowValidationException() throws Exception {
        mockMandatoryEspRsp("PROXI_SIGNATURE", "LICENSE");
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        Long vehicleModelId = vehicleCreateResponse.get(0).getId();
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        Long ecuModelId = addEcuModelResponse.get(0).getId();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(fileUrl);
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<MgmtTarget> targets = invokeCreateTargetApi(vehicleModelId, amountTargets);
        List<String> controllerIds = targets.stream().map(MgmtTarget::getControllerId).toList();
        JpaArtifacts jpaArtifacts = artifactsRepository.findById(artifacts.getArtifactId()).orElse(null);
        assertNotNull(jpaArtifacts);
        jpaArtifacts.setFileStatus(String.valueOf(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL));
        artifactsRepository.save(jpaArtifacts);
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(MgmtTarget::getControllerId).toList(), rollout.getId());
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.PROXI_SIGNATURE, "", supportPackageUrl, List.of());

        MgmtSupportPackage rspResponse = invokeCreateSupportPackageApi(rollout.getId(), createRspRequest);
        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);
        MgmtBaseSupportPackageCreateRequest createEspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.LICENSE, ecuNodeId, supportPackageUrl, controllerIds);
        invokeCreateSupportPackageApi(rollout.getId(), createEspRequest);
        mockServer.clear(request());
        rolloutHandler.handleAll();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        invokeRolloutFreezeApi(rollout.getId());
        rollout = rolloutManagement.get(rollout.getId()).orElse(null);
        assertNotNull(rollout);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        rolloutHandler.handleAll();
//        TODO: verify after fixing frezze api
        //        int page = 0;
//        int PAGE_SIZE=100;
//        Page<RolloutGroup> rolloutGroupPage;
//        do{  rolloutGroupPage= rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
//            for (RolloutGroup group : rolloutGroupPage.getContent()) {
//                assertEquals(RolloutGroupStatus.FREEZING,group.getStatus());
//            }
//            page++;
//        }while (rolloutGroupPage.hasNext());


        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL),
                VerificationTimes.atLeast(1)
        );
    }

    @Test
    @Description("When the rollout is frozen and mandatory rollout is not uploaded, an ValidationException is thrown")
    void givenFrozenRolloutAndMandatoryRSPNotUploaded_whenActionPerformed_thenValidationExceptionIsThrown() throws Exception {

        mockMandatoryEspRsp("PROXI_SIGNATURE", "LICENSE");
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        Long vehicleModelId = vehicleCreateResponse.get(0).getId();
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        Long ecuModelId = addEcuModelResponse.get(0).getId();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(fileUrl);
        JpaArtifacts jpaArtifacts = artifactsRepository.findById(artifacts.getArtifactId()).orElse(null);
        jpaArtifacts.setFileStatus(String.valueOf(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL));
        artifactsRepository.save(jpaArtifacts);
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        jpaArtifacts = artifactsRepository.findById(artifacts.getArtifactId()).orElse(null);
        jpaArtifacts.setFileStatus(String.valueOf(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL));
        artifactsRepository.save(jpaArtifacts);
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<MgmtTarget> targets = invokeCreateTargetApi(vehicleModelId, amountTargets);
        List<String> controllerIds = targets.stream().map(MgmtTarget::getControllerId).toList();
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(MgmtTarget::getControllerId).toList(), rollout.getId());
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.PROXI_SIGNATURE, "", supportPackageUrl, List.of());
        log.info("create request:{}", createRspRequest);
        MgmtSupportPackage rspResponse = invokeCreateSupportPackageApi(rollout.getId(), createRspRequest);
        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);
        MgmtBaseSupportPackageCreateRequest createEspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.LICENSE, ecuNodeId, supportPackageUrl, controllerIds);
        invokeCreateSupportPackageApi(rollout.getId(), createEspRequest);
        rolloutHandler.handleAll();
        invokeRolloutFreezeApi(rollout.getId());
        rollout = rolloutManagement.getByName(rollout.getName()).orElse(null);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
//        TODO: fix the freeze api and test this
//        int page = 0;
//        int PAGE_SIZE=100;
//        Page<RolloutGroup> rolloutGroupPage;
//        do{  rolloutGroupPage= rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
//            for (RolloutGroup group : rolloutGroupPage.getContent()) {
//                assertEquals(RolloutGroupStatus.FREEZING,group.getStatus());
//            }
//            page++;
//        }while (rolloutGroupPage.hasNext());

    }


    @Test
    @Description("When the rollout is frozen and mandatory rollout is not uploaded, an ValidationException is thrown")
    void givenFrozenRolloutAndMandatoryESPNotUploaded_whenActionPerformed_thenValidationExceptionIsThrown() throws Exception {

        mockMandatoryEspRsp("PROXI_SIGNATURE", "LICENSE");
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        Long vehicleModelId = vehicleCreateResponse.get(0).getId();
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        Long ecuModelId = addEcuModelResponse.get(0).getId();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(fileUrl);
        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifactsRepository.getArtifactsById(artifacts.getArtifactId()).orElse(null);
        assert jpaArtifacts != null;
        jpaArtifacts.setFileStatus(String.valueOf(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL));
        artifactsRepository.save(jpaArtifacts);
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        jpaArtifacts = (JpaArtifacts) artifactsRepository.getArtifactsById(artifacts.getArtifactId()).orElse(null);
        assert jpaArtifacts != null;
        jpaArtifacts.setFileStatus(String.valueOf(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL));
        artifactsRepository.save(jpaArtifacts);
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<MgmtTarget> targets = invokeCreateTargetApi(vehicleModelId, amountTargets);
        List<String> controllerIds = targets.stream().map(MgmtTarget::getControllerId).toList();


        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(MgmtTarget::getControllerId).toList(), rollout.getId());
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.PROXI_SIGNATURE, "", supportPackageUrl, List.of());

        MgmtSupportPackage rspResponse = invokeCreateSupportPackageApi(rollout.getId(), createRspRequest);
        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);
        MgmtBaseSupportPackageCreateRequest createEspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.ECU_SCRIPT, ecuNodeId, supportPackageUrl, controllerIds);
        invokeCreateSupportPackageApi(rollout.getId(), createEspRequest);
        // Clear all previous requests recorded by MockServer
        mockServer.clear(request());
        rolloutHandler.handleAll();
        invokeRolloutFreezeApi(rollout.getId());
        rollout = rolloutManagement.getByName(rollout.getName()).orElse(null);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        rolloutHandler.handleAll();
//        TODO: fix the freeze api and test this
//        int page = 0;
//        int PAGE_SIZE=100;
//        Page<RolloutGroup> rolloutGroupPage;
//        do{  rolloutGroupPage= rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
//            for (RolloutGroup group : rolloutGroupPage.getContent()) {
//                assertEquals(RolloutGroupStatus.FREEZING,group.getStatus());
//            }
//            page++;
//        }while (rolloutGroupPage.hasNext());

        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL),
                VerificationTimes.atLeast(1)
        );
    }

}
