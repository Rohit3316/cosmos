package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
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
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactModuleLinkRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetTypeRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutExecutor;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.TargetTagRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.VersionRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

abstract class AbstractManagementRolloutApiIntegrationTest extends AbstractManagementApiIntegrationTest {

    public static final String ROLLOUT_2 = "rollout2";
    public static final String ROLLOUT_1 = "rollout1";
    public static final String INVALID_ROLLOUT_NAME = "rollout 5";
    public static final String BASE_V1_REQUEST_MAPPING_TENANT = "/management/v1/tenants/{tenantId}";
    protected static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    protected static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";
    protected static final String KAFKA_ROLLOUT_ERROR_ENDPOINT = "/kafka/rollouterror";
    protected static final String JSON_PATH_CONTENT = "$.content";
    protected static final String ROLLOUT_ID_SM_ENDPOINT = "/{rolloutId}/softwares";
    protected static final String DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX = "_default_group_1";
    protected static final String ROLLOUT_GROUP_DESCRIPTION_SUFFIX = "_groupDesc_1";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_CANCELED = "$.totalTargetsPerStatus.canceled";
    protected static final String HREF_ROLLOUT_PREFIX = "http://localhost/management/v1/tenants/1/rollouts/";
    protected static final String TENANT_ID = "1";
    protected static final String ROLLOUT = "rollout";
    protected static final String XLS_CONTENT_TYPE = "application/vnd.ms-excel";
    protected static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    protected static final String XLSX_FILE_NAME = "vins.xlsx";
    protected static final String CSV_FILE_NAME = "vins.csv";
    protected static final String TEXT_CSV_CONTENT_TYPE = "text/csv";
    protected static final String XLS_FILE_NAME = "vins.xls";
    protected static final String PDF_FILE_NAME = "vins.pdf";
    protected static final String APPLICATION_PDF_CONTENT_TYPE = "application/pdf";
    protected static final Long VALID_ROLLOUT_START_DATE = Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond();
    protected static final Long VALID_ROLLOUT_END_DATE = Instant.now().plus(49, ChronoUnit.HOURS).getEpochSecond();
    protected static final Integer DEPLOYMENT_ESTIMATED_UPDATE_TIME = 1000;
    protected static final Long MAX_PACKAGE_SIZE_IN_BYTES = 10000L;
    protected static final Long INVALID_ROLLOUT_END_DATE = Instant.now().plusSeconds(120).getEpochSecond();
    protected static final String SESSION_ID = "session-id";
    protected static final String DEBUG = "debug";
    protected static final String TARGET = "target";
    protected static final String JSON_PATH_NAME = "$.name";
    protected static final String JSON_PATH_DEBUG = "$.debug";
    protected static final String ROLLOUT_1_DESC = "rollout1Desc";
    protected static final String JSON_PATH_TOTAL = "$.total";
    protected static final String CONTENT_0_NAME = "content[0].name";
    protected static final String CONTENT_0_STATUS = "content[0].status";
    protected static final String CONTENT_0_TOTAL_TARGETS_PER_STATUS = "content[0].totalTargetsPerStatus";
    protected static final String RUNNING = "running";
    protected static final String CONTENT_0_LINKS_SELF_HREF = "content[0]._links.self.href";
    protected static final String SUPPORT_PACKAGES_URL = "/support-packages";
    protected static final String JSON_PATH_STATUS = "$.status";
    protected static final String JSON_PATH_LOG_LEVEL = "$.log.level";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_RUNNING = "$.totalTargetsPerStatus.running";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_NOTSTARTED = "$.totalTargetsPerStatus.notstarted";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_SCHEDULED = "$.totalTargetsPerStatus.scheduled";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_CANCELLED = "$.totalTargetsPerStatus.cancelled";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_FINISHED = "$.totalTargetsPerStatus.finished";
    protected static final String JSON_PATH_TOTAL_TARGETS_PER_STATUS_ERROR = "$.totalTargetsPerStatus.error";
    protected static final String JSON_PATH_TOTAL_GROUPS = "$.totalGroups";
    protected static final String STARTING = "starting";
    protected static final String DRAFT = "draft";
    protected static final String BUMLUX = "bumlux";
    protected static final String JSON_PATH_LAST_MODIFIED_BY = "$.lastModifiedBy";
    protected static final String JSON_PATH_LAST_MODIFIED_AT = "$.lastModifiedAt";
    protected static final String JSON_PATH_CREATED_BY = "$.createdBy";
    protected static final String JSON_PATH_CREATED_AT = "$.createdAt";
    protected static final String JSON_PATH_TOTAL_TARGETS = "$.totalTargets";
    protected static final String JSON_PATH_LINKS_SELF_HREF = "$._links.self.href";
    protected static final String JSON_PATH_TARGET_FILTER_QUERY = "$.targetFilterQuery";
    protected static final String ROLLOUT_ID_GROUPS_URL = "/{rolloutId}/groups";
    protected static final String READY = "ready";
    protected static final String JSON_PATH_CONTENT_0_STATUS = "$.content[0].status";
    protected static final String CONFIRMATION_REQUIRED = "confirmationRequired";
    protected static final String CREATING_RIOLLOUT_GROUPS_URL = "/{rolloutId}/groups/{groupId}";
    protected static final String ROLLOUT_ID_URL = "/{rolloutId}";
    protected static final String STATUS = "status";
    protected static final String ROLLOUT_ID_CANCEL_URL = "/{rolloutId}/action/cancel";
    protected static final String ROLLOUT_GROUP_TARGETS_ENDPOINT = "/rollouts/{rolloutId}/groups/{groupId}/targets";
    protected static final String BASE_URL = "/management/v1/tenants/";
    protected static final String ROLLOUT_DELETE = "rolloutDelete";
    protected static final String ROLLOUT_3 = "rollout3";
    protected static final String OTHER_1 = "other1";
    protected static final String JSON_PATH_CONTENT_0_NAME = "$.content[0].name";
    protected static final String JSON_PATH_CONTENT_0_TOTAL_TARGETS_PER_STATUS = "$.content[0].totalTargetsPerStatus";
    protected static final String WIFI_PREFERRED = "wifi_preferred";
    protected static final String NAME_ROLLOUT_1 = "name==rollout1";
    protected final static String START_ROLLOUT_URL = "/{rolloutId}/action/start";
    protected static final String JSON_PATH_DESCRIPTION = "$.description";
    protected static final String JSON_PATH_START_AT = "$.startAt";
    protected static final String JSON_PATH_END_AT = "$.endAt";
    protected static final String JSON_PATH_USER_ACCEPTANCE_REQUIRED = "$.userAcceptanceRequired";
    protected static final String JSON_PATH_CONNECTIVITY_TYPE = "$.connectivityType";
    protected static final String JSON_PATH_LOG_COLLECTION_REQUIRED = "$.log.collectionRequired";
    protected static final String JSON_PATH_LOG_MAX_FAILURE_VIN = "$.log.maxFailureVin";
    protected static final String JSON_PATH_LOG_MAX_NUMBER_OF_FILES = "$.log.maxNumberOfFiles";
    protected static final String JSON_PATH_LOG_MAX_ALL_FILE_SIZE = "$.log.maxAllFileSize";
    protected static final String JSON_PATH_LOG_MAX_SUCCESS_VIN = "$.log.maxSuccessVin";
    protected static final String JSON_PATH_LOG_MAX_EACH_FILE_SIZE = "$.log.maxEachFileSize";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED = "$.deploymentMetadata.downgradeAllowed";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA = "$.deploymentMetadata.requiredMedia";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_PERCENTAGE";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMPERATURE";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMP_METRIC";
    protected static final String JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER = "$.maxDownloadCellularDurationTimer";
    protected static final String JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER = "$.maxDownloadDurationTimer";
    protected static final String JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER = "$.maxDownloadWifiDurationTimer";
    protected static final String JSON_PATH_DOWNLOAD_RETRY_COUNT = "$.downloadRetryCount";
    protected static final String JSON_PATH_MAX_UPDATE_TIME = "$.maxUpdateTime";
    protected static final String JSON_PATH_DEPLOYMENT_ESTIMATED_UPDATE_TIME = "$.deploymentMetadata.estimatedUpdateTime";
    protected static final String JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES = "$.maxPackageSize";
    protected static final String JSON_PATH_LINKS_START_HREF = "$._links.start.href";
    protected static final String START_URL = "/start";
    protected static final String PAUSE_URL = "/pause";
    protected static final String JSON_PATH_LINKS_PAUSE_HREF = "$._links.pause.href";
    protected static final String RESUME_URL = "/resume";
    protected static final String JSON_PATH_LINKS_RESUME_HREF = "$._links.resume.href";
    protected static final String JSON_PATH_LINKS_GROUPS_HREF = "$._links.groups.href";
    protected static final String GROUPS_URL = "/groups";
    protected static final String CAMPAIGN_FOR_HPC_UPDATE = "Campaign for HPC Update";
    protected static final String JSON_PATH_CONTENT1 = "$.content[";
    protected static final String HPC_UPDATE_1 = "HPC_UPDATE1";
    protected static final String RANDOM = "random";
    protected static final String JSON_PATH_MESSAGE = "$.message";
    protected static final String HPC_UPDATE_23 = "HPC_UPDATE23";
    protected static final String ROLLOUT_ID_ACTION_UNFREEZE_URL = "/{rolloutId}/action/unfreeze";
    protected static final String ROLLOUT_MANAGEMENT = "rolloutManagement";
    protected static final String ROLLOUT_GROUP_MANAGEMENT = "rolloutGroupManagement";
    protected static final String ROLLOUT_0010 = "rollout0010";
    protected static final String CONTROLLER_ID1 = "controller1";
    protected static final String CONTROLLER_ID2 = "controller2";
    protected static final String CONTROLLER_ID3 = "controller3";
    protected static final String CONTENT_TYPE = "text/csv";
    protected static final String ORIGINAL_FILENAME = "vins.csv";
    protected static final String TARGET_DEVICES = "targetDevices";
    protected static final String CELLULAR = "cellular";
    protected static final String DESCRIPTION = "description";
    protected static final String CONTROLLER_ID = "controllerId";
    protected static final String SP_TARGET_FILTER_QUERY = "sp_target_filter_query";
    protected static final String SP_TARGET_TAG = "sp_target_tag";
    protected static final String SP_TARGET = "sp_target";
    protected static final String SP_VEHICLE_ECU = "sp_vehicle_ecu";
    protected static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    protected static final String SP_ACTION = "sp_action";
    protected static final String SP_ROLLOUTGROUP = "sp_rolloutgroup";
    protected static final String SP_RSP_ROLLOUT = "sp_rsp_rollout";
    protected static final String SP_ESP_ECU_ROLLOUT = "sp_esp_ecu_rollout";
    protected static final String SP_ECU_MODEL = "sp_ecu_model";
    protected static final String SP_ROLLOUT = "sp_rollout";
    protected static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    protected static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String SP_ARTIFACTS = "sp_artifacts";
    protected static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    protected static final String SP_TARGET_INVENTORY = "sp_target_inventory";
    protected static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    protected static final String SP_RSP = "sp_rsp";
    protected static final String SP_ESP = "sp_esp";
    protected static final String TARGET_PREFIX = "RSPSetTarget02";
    protected static final String ROLLOUT_PREFIX = "RolloutPrefixSupportkg01";
    protected static final String ROLLOUT_NAME = "RSPRolloutPkg02";
    protected static final String DISTRIBUTION_SET_NAME = "RSPDistributionSetName02";
    protected static final String FOTA = "FOTA";
    protected static final String AOTA = "AOTA";
    protected static final String INSTALL = "INSTALL";
    protected static final String UNINSTALLANY = "UNINSTALLANY";
    protected static final String UNINSTALLSPECIFIC = "UNINSTALLSPECIFIC";
    protected static final Integer MAX_UPDATE_TIME = 1800;
    protected static final String MANDATORY_RSP_KEY = "rollout.mandatory.support-package.rsp";
    protected static final String MANDATORY_ESP_KEY = "rollout.mandatory.support-package.esp";
    protected static final String LATEST_FIRMWARE_UPDATE = "This file contains the latest firmware update.";
    protected static final String RELEASE_NOTES_URL = "https://example.com/release-notes";
    protected static final String REQUEST_BODY = "requestBody";
    protected static final String SET_TARGET = "SetTarget";
    protected static final String JSON_PATH_STARTTYPE = "$.startType";
    protected static final String JSON_PATH_PRIORITY = "$.priority";
    protected static final String NEW_ROLLOUT = "newRollout";
    protected static final String FILE_URL = "http://localhost:%s/some-file";
    protected static final String TEST_ECU_NODE_ADDRESS_1 = "30 A0";
    protected static final Long MAX_PACKAGE_FILE_SIZE = 104857600L;
    protected static final String JSON_PATH_MAX_PACKAGE_FILE_SIZE = "$.maxPackageSize";
    public static final String JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME = "$.deploymentMetadata.estimatedUpdateTime";
    protected static ClientAndServer mockServer;
    protected static String port;
    protected final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    protected String version = "2.0.0";
    @Autowired
    protected RolloutManagement rolloutManagement;
    @Autowired
    protected SupportPackageManagement supportPackageManagement;
    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    protected ArtifactUrlHandlerProperties artifactUrlHandlerProperties;
    @Autowired
    protected ArtifactModuleLinkRepository artifactModuleLinkRepository;
    @Autowired
    protected RolloutGroupManagement rolloutGroupManagement;
    @Autowired
    protected RolloutTestApprovalStrategy approvalStrategy;
    @Autowired
    protected BeanFactory beanFactory;
    @Autowired
    protected RolloutRepository rolloutRepository;
    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    protected TargetTagRepository targetTagRepository;
    @Autowired
    protected MgmtRolloutResource rolloutResource;
    @Autowired
    protected RolloutManagement rolloutManagementService;
    @Autowired
    protected JpaRolloutExecutor rolloutExecutor;
    @MockBean
    protected SnsServiceFactory snsServiceFactory;
    @Autowired
    protected RolloutTargetGroupRepository rolloutTargetGroupRepository;
    @Autowired
    protected ActionRepository actionRepository;


    @MockBean
    @Qualifier("fileDownloadRestTemplate")
    protected RestTemplate restTemplate;

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
    @Mock
    protected CdnUploadSnsService cdnUploadSnsService;
    @Mock
    protected CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    protected S3FileTransferSnsService s3FileTransferSnsService;
    @Mock
    protected S3FileDeleteSnsService s3FileDeleteSnsService;
    @MockBean
    protected SnsAsyncClient snsAsyncClient;
    @Autowired
    protected ArtifactsRepository artifactsRepository;
    @Autowired
    protected RspRepository rspRepository;
    @Autowired
    protected EspRepository espRepository;
    @Autowired
    protected TenantMetaDataRepository tenantMetaDataRepository;
    @Autowired
    ObjectMapper objectMapper;
    @MockBean
    MgmtS3Service s3Service;
    @Autowired
    DistributionSetRepository distributionSetRepository;
    @Autowired
    DistributionSetTypeRepository distributionSetTypeRepository;
    @Autowired
    VersionRepository versionRepository;
    @Autowired
    SoftwareModuleRepository softwareModuleRepository;

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    protected static void mockPublishVehicleStatus() {
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    protected static String createBasicGroupJson(String groupName,
                                                 int targetPercentage,
                                                 int errorConditionExpression,
                                                 int successConditionExpression) {

        return createBasicGroupJson(groupName, targetPercentage, errorConditionExpression, successConditionExpression, null);
    }

    protected static String createBasicGroupJson(String groupName,
                                                 int targetPercentage,
                                                 int errorConditionExpression,
                                                 int successConditionExpression,
                                                 String targetFilter) {

        String targetFilterJson = (targetFilter != null && !targetFilter.isEmpty())
                ? String.format(", \"targetFilter\": \"%s\"", targetFilter)
                : "";

        return String.format("{ \"errorCondition\": { \"action\": \"THRESHOLD\", \"expression\": %d }, \"successCondition\": { \"action\": \"THRESHOLD\", \"expression\": %d }, \"name\": \"%s\", \"targetPercentage\": %d%s }",
                errorConditionExpression, successConditionExpression, groupName, targetPercentage, targetFilterJson);
    }

    @BeforeEach
    void setup() throws Exception {
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        port = System.getProperty("mock.server.port");
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_TARGET_FILTER_QUERY, SP_TARGET_TAG, SP_TARGET, SP_VEHICLE_ECU,
                SP_VEHICLE_MODEL, SP_ACTION, SP_ROLLOUTGROUP, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT, SP_ECU_MODEL, SP_ROLLOUT,
                SP_DISTRIBUTION_SET, SP_ARTIFACT_SOFTWARE_MODULE,
                SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_RSP, SP_ESP);

        MockitoAnnotations.openMocks(this); // Initialize mocks
        approvalStrategy.setApprovalNeeded(false);

        mockServer.when(HttpRequest.request().withMethod("POST").withPath(Constants.KAFKA_SEND_EVENT_URL)).respond(HttpResponse.response().withStatusCode(201));

        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
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
        if (tenantMetaDataRepository.findAll().isEmpty()) {
            JpaTenantMetaData jpaTenantMetaData = new JpaTenantMetaData();
            jpaTenantMetaData.setTenant(tenantAware.getCurrentTenant());
            jpaTenantMetaData.setDefaultDsType((JpaDistributionSetType) standardDsType);
            tenantMetaDataRepository.save(jpaTenantMetaData);
        }
    }

    @NotNull
    protected Rollout prepareRolloutAndGroupDetails(List<Target> targets) {
        // Given: A rollout with associated groups and targets
        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";

        final Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        // Verify the initial state of rollout groups
        List<JpaRolloutGroup> rolloutGroupsBeforeDeletion = rolloutGroupRepository.findByRolloutId(rollout.getId());
        assertNotNull(rolloutGroupsBeforeDeletion, "Rollout groups should not be null.");
        assertEquals(2, rolloutGroupsBeforeDeletion.size(), "There should be 2 rollout groups associated with the rollout.");

        // Verify the target distribution across groups
        assertEquals(2, rolloutGroupsBeforeDeletion.get(0).getRolloutTargetGroup().size(), "Group 1 should have 2 targets.");
        assertEquals(1, rolloutGroupsBeforeDeletion.get(1).getRolloutTargetGroup().size(), "Group 2 should have 1 target.");

        // Verify the total target count in the system
        long initialTargetCount = targetManagement.count();
        assertEquals(3, initialTargetCount, "There should be 3 targets in the system initially.");

        // Verify the rollout is in DRAFT status
        assertEquals(RolloutStatus.DRAFT, rollout.getStatus(), "Rollout should be in DRAFT status initially.");
        return rollout;
    }

    protected Artifacts createAndAssociateArtifactWithSoftwareModule(SoftwareModule softwareModule, Version version) {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("file1", FileType.FULL, DESCRIPTION, "123", "SHA_256", 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifacts jpaArtifacts = (JpaArtifacts) artifact;
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact(jpaArtifacts).softwareModule((JpaSoftwareModule) softwareModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        return artifact;
    }

    protected List<MgmtSoftwareModuleRequest> createSoftwareModuleAssociationModelRequest(MgmtSoftwareModuleRequest module) {
        List<MgmtSoftwareModuleRequest> modulesList = new ArrayList<>();
        modulesList.add(module);

        return modulesList;
    }

    protected Rollout createRollout(String rolloutName, RolloutStatus rolloutStatus) {
        JpaRollout jpaRollout = new JpaRollout();
        jpaRollout.setName(rolloutName);
        jpaRollout.setStartType(MgmtRolloutStartType.SCHEDULED);
        jpaRollout.setStartAt(VALID_ROLLOUT_START_DATE);
        jpaRollout.setEndAt(VALID_ROLLOUT_END_DATE);
        jpaRollout.setStatus(rolloutStatus);
        jpaRollout.setDeploymentEstimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME);
        jpaRollout.setType(MgmtRolloutType.FOTA);
        return rolloutManagement.create(jpaRollout);
    }

    protected Rollout createRolloutWithDependencies() throws Exception {
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
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

        List<MgmtTarget> targets = createTargets(vehicleModelId, 1);
        List<String> controllerIds = getControllerIds(targets);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());

        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), 1);

        mockServer.clear(HttpRequest.request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        handleRollout();
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        handleRollout();
        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        handleRollout();

        return rollout;
    }

    protected Rollout createRolloutwithTargetsAndRolloutGroups(int noOfTargets, int noOfRolloutGroups) throws Exception {
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

        List<MgmtTarget> targets = createTargets(vehicleModelId, noOfTargets);
        List<String> controllerIds = getControllerIds(targets);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());

        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), noOfRolloutGroups);

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
        return rollout;
    }

    protected MgmtSoftwareModuleRequest createMgmtSoftwareModuleRequest(Long softwareModuleId, Long versionId) {
        MgmtSoftwareModuleRequest moduleRequest = MgmtSoftwareModuleRequest.builder().moduleId(softwareModuleId).softwareVersionTargetId(versionId).build();
        return moduleRequest;
    }

    protected Long createVehicleModel() throws Exception {
        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        return vehicleCreateResponse.get(0).getId();
    }

    protected List<MgmtCreateEcuModelResponse> createEcuModel() throws Exception {
        return invokeAddEcuModelApi();

    }

    protected String getEcuNodeId(MgmtCreateEcuModelResponse response) throws Exception {

        return response.getEcuNodeId();
    }

    protected void associateEcuModelToVehicleModel(Long vehicleModelId, Long ecuModelId) throws Exception {
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);
    }

    protected long createSoftwareModule() throws Exception {
        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        return softwareModules.get(0).getModuleId();
    }

    protected MgmtAddVersionResponse createSoftwareVersion(long swModuleId) throws Exception {
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        return invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
    }

    protected MgmtArtifacts createArtifact(String fileUrl) throws Exception {
        return invokeCreateArtifactViaUrlApi(fileUrl);
    }

    protected void associateArtifactWithSoftwareModule(long swModuleId, long versionId, long artifactId) throws Exception {
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionId), Math.toIntExact(artifactId));
        changeArtifactStatus(artifactsRepository.findById(artifactId).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
    }

    protected List<MgmtTarget> createTargets(Long vehicleModelId, int amountTargets) throws Exception {
        return invokeCreateTargetApi(vehicleModelId, amountTargets);
    }

    protected List<String> getControllerIds(List<MgmtTarget> targets) {
        return targets.stream().map(MgmtTarget::getControllerId).toList();
    }

    protected void associateSoftwareModuleWithRollout(long swModuleId, long versionId, long rolloutId) throws Exception {
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionId);
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rolloutId);
    }

    protected MgmtAddDeviceDetailsResponse addDeviceDetails(List<String> controllerIds, long rolloutId, int totalGroups) throws Exception {
        return invokeAddDeviceApi(controllerIds, rolloutId, totalGroups);
    }

    protected MgmtSupportPackage createSupportPackage(long rolloutId, String supportPackageUrl, MgmtSupportPackageFileType fileType, String ecuNodeId, List<String> controllerIds) throws Exception {
        MgmtBaseSupportPackageCreateRequest createRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(fileType, ecuNodeId, supportPackageUrl, controllerIds);
        return invokeCreateSupportPackageApi(rolloutId, createRequest);
    }

    protected void updateFileStatus(String packageType, long supportPackageId, FileTransferStatus status) {
        if (packageType.equalsIgnoreCase(RSP)) {
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

    protected void handleRollout() {
        rolloutHandler.handleAll();
    }

    protected void verifyRunningGroups(long rolloutId, MgmtAddDeviceDetailsResponse groupsResponse) {
        var runningGroups = rolloutGroupRepository.findByRolloutId(rolloutId).stream().filter(group -> group.getStatus().equals(RolloutGroupStatus.RUNNING)).toList();
        assertEquals(1, runningGroups.size());
        var firstRunningGroup = runningGroups.get(0);
        var actionsList = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, firstRunningGroup.getId(), true);
        makeAllActionSuccessFul(actionsList);
        handleRollout();
        assertEquals(RolloutGroupStatus.FINISHING, rolloutGroupManagement.get(firstRunningGroup.getId()).get().getStatus());
        verifyActionStatusAndTransition(groupsResponse, rolloutId);
        runningGroups = rolloutGroupRepository.findByParentId(firstRunningGroup.getId());
        assertTrue(runningGroups.stream().allMatch(group -> group.getStatus().equals(RolloutGroupStatus.RUNNING)));
        actionsList = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, runningGroups.get(0).getId(), true);
        makePartialActionsSuccessFul(actionsList, true);
        handleRollout();
        verifyActionStatusAndTransition(groupsResponse, rolloutId);
    }

    protected void makePartialActionsSuccessFul(List<JpaAction> actionList, boolean almostAll) {
        if (almostAll) {
            for (int i = 0; i < actionList.size() - 2; i++) {
                var action = actionList.get(i);
                action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
                actionRepository.save(action);
            }
        } else {
            var action = actionList.get(0);
            action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
            actionRepository.save(action);
        }

    }

    protected void makeAllActionSuccessFul(List<JpaAction> actionList) {
        for (JpaAction action : actionList) {
            action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
            actionRepository.save(action);
        }
    }

    protected void verifyActionStatusAndTransition(MgmtAddDeviceDetailsResponse groupsResponse, long rolloutId) {
        int totalGroup = groupsResponse.getTotalGroups();
        IntStream.range(0, groupsResponse.getTotalGroups()).boxed().forEach(i -> {
            var group = groupsResponse.getGroups().get(i);
            Long groupId = group.getId();
            var rolloutgroup = rolloutGroupManagement.get(groupId).orElse(null);
            assertNotNull(rolloutgroup);

            if (rolloutgroup.getStatus().equals(RolloutGroupStatus.RUNNING)) {
                List<JpaAction> actionList = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, groupId, true);
                long countOfFinishedActions = actionList.stream()
                        .filter(action -> action.getStatus().equals(DeviceActionStatus.FINISHED_SUCCESS)
                                || action.getStatus().equals(DeviceActionStatus.FINISHED_FAILURE)
                                || action.getStatus().equals(DeviceActionStatus.FINISHED_NOT_EXECUTED))
                        .count();
                int threshold = Integer.parseInt(rolloutgroup.getSuccessConditionExp());
                if (countOfFinishedActions == 0) {
                    assertTrue(actionList.stream().allMatch(action -> action.getStatus().equals(DeviceActionStatus.RUNNING)));
                } else if (((float) countOfFinishedActions / (float) totalGroup >= (float) threshold / 100F) && !rolloutGroupRepository.findByParentId(groupId).isEmpty()) {
                    Assertions.assertFalse(rolloutGroupRepository.findByParentIdAndStatus(groupId, RolloutGroupStatus.RUNNING).isEmpty());
                } else {
                    assertTrue(actionList.stream().anyMatch(action -> action.getStatus().equals(DeviceActionStatus.RUNNING)));
                }
            } else if (rolloutgroup.getStatus().equals(RolloutGroupStatus.FINISHING)) {

                List<JpaAction> actionList = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, groupId, true);
                assertTrue(actionList.stream().allMatch(action -> action.getStatus().equals(DeviceActionStatus.FINISHED_SUCCESS)
                        || action.getStatus().equals(DeviceActionStatus.FINISHED_FAILURE)
                        || action.getStatus().equals(DeviceActionStatus.FINISHED_NOT_EXECUTED)));
            } else {
                assertEquals(RolloutGroupStatus.QUEUED, rolloutgroup.getStatus());
                assertTrue(actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, groupId, true).isEmpty());
            }
        });

    }

    protected List<MgmtTarget> setUpPreRequisitesAndCreateRollout(String rolloutName) throws Exception {
        // Setup mock server and test data
        var port = System.getProperty("mock.server.port");
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        // Pre-requisites: Create vehicle model, ECU model, and associate them
        Long vehicleModelId = createVehicleModel();  // Create a vehicle model
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel(); // Create an ECU model
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId); // Associate ECU model to vehicle model

        // Create software module, version, and artifact
        long swModuleId = createSoftwareModule(); // Create a software module
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId); // Create a software version
        MgmtArtifacts artifacts = createArtifact(fileUrl); // Create an artifact
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId()); // Associate artifact with software module

        // Create targets and retrieve controller IDs
        List<MgmtTarget> targets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> controllerIds = getControllerIds(targets);

        // Prepare mandatory support package requests
        MgmtBaseSupportPackageCreateRequest whatsNewRspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.WHATS_NEW, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, controllerIds);

        // Create and verify the first rollout
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout firstRollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(firstRollout);

        // Create mandatory support packages
        MgmtSupportPackage whatsNewRspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), whatsNewRspCreateRequest);
        updateFileStatus(RSP, whatsNewRspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update RSP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL
        MgmtSupportPackage adaCertificateEspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), adaCertificateEspCreateRequest);
        updateFileStatus(ESP, adaCertificateEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update ESP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL
        MgmtSupportPackage adaLicenseEspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), adaLicenseEspCreateRequest);
        updateFileStatus(ESP, adaLicenseEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update ESP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), firstRollout.getId());// Associate software module and distribution set with the first rollout

        return targets;
    }

    protected Map<String, Object> createAndTransitionRolloutToRunning() throws Exception {
        // Setup mock server and test data
        Map<String, Object> result = new HashMap<>();
        var rolloutName = "newRollout";
        var port = System.getProperty("mock.server.port");
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        // Create vehicle model, ECU model, and associate them
        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

        // Create software module, version, and artifact
        long swModuleId = createSoftwareModule(); // Create a software module
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId); // Create a software version
        MgmtArtifacts artifacts = createArtifact(fileUrl); // Create an artifact
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId()); // Associate artifact with software module

        // Create targets and retrieve controller IDs
        List<MgmtTarget> targets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> controllerIds = getControllerIds(targets);

        // Prepare mandatory support package requests
        MgmtBaseSupportPackageCreateRequest whatsNewRspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.WHATS_NEW, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, controllerIds);

        // Create and verify the rollout
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        // Create mandatory support packages
        MgmtSupportPackage whatsNewRspCreateResponse = invokeCreateSupportPackageApi(rollout.getId(), whatsNewRspCreateRequest);
        updateFileStatus(RSP, whatsNewRspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaCertificateEspCreateResponse = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest);
        updateFileStatus(ESP, adaCertificateEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest);
        updateFileStatus(ESP, adaLicenseEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        addDeviceDetails(controllerIds, rollout.getId(), 1);

        // Transition the rollout to READY state
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, rollout.getStatus());
        handleRollout(); // READY to STARTING
        handleRollout(); // STARTING to RUNNING
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assert rollout != null;
        assertEquals(RolloutStatus.RUNNING, rollout.getStatus());

        result.put("vehicleModelId", vehicleModelId);
        result.put("targets", targets);
        result.put("rollout", rollout);
        result.put("ecuNodeId", ecuNodeId);
        result.put("supportPackageUrl", supportPackageUrl);
        result.put("controllerIds", controllerIds);
        result.put("artifacts", artifacts);
        return result;
    }

    protected List<String> createTargetsAndGetControllers(Long vehicleModelId, int count) throws Exception {
        return getControllerIds(createTargets(vehicleModelId, count));
    }

    protected List<Long> createAndUploadEspPackages(Long rolloutId, String ecuNodeId, String fileUrl, List<String> controllerIds, boolean uploadToCdn) throws Exception {
        var certPackage = createSupportPackage(rolloutId, fileUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        var licensePackage = createSupportPackage(rolloutId, fileUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);

        List<Long> supportPackageIds = List.of(certPackage.getSupportPackageId(), licensePackage.getSupportPackageId());

        if (uploadToCdn) {
            uploadEspToCdn(supportPackageIds);
        }

        return supportPackageIds;
    }

    protected void uploadEspToCdn(List<Long> supportPackageIds) {
        for (Long id : supportPackageIds) {
            updateFileStatus(ESP, id, FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        }
    }

    protected void assertGroupStatus(Long rolloutId, int groupIndex, RolloutGroupStatus expectedStatus) {
        JpaRolloutGroup group = rolloutGroupRepository.findByRolloutId(rolloutId).get(groupIndex);
        assertEquals(expectedStatus, group.getStatus());
    }

    protected void pauseRollout(Long rolloutId) throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected void resumeRollout(Long rolloutId) throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_RESUME_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    protected static Stream<Arguments> artifactReplacementTestArguments() {
        return Stream.of(
                Arguments.of(FileType.FULL, "testSMForFullAssociation" , null, "testTargetVersionForFullAssociation1"),
                Arguments.of(FileType.DELTA, "testSMForDeltaAssociation", "testSourceVersionForDeltaAssociation" , "testTargetVersionForDeltaAssociation1")
        );
    }

    protected void resetTableSequence(){
        jdbcTemplate.execute("ALTER TABLE sp_artifacts ALTER COLUMN id RESTART WITH 1;");
        jdbcTemplate.execute("ALTER TABLE sp_artifact_software_module ALTER COLUMN id RESTART WITH 1;");
        jdbcTemplate.execute("ALTER TABLE sp_software_versions ALTER COLUMN id RESTART WITH 1;");
        jdbcTemplate.execute("ALTER TABLE sp_base_software_module ALTER COLUMN id RESTART WITH 1;");
    }

    protected static Stream<Arguments> artifactAssociationUploadStatusArguments() {
        return Stream.of(
                Arguments.of(FileTransferStatus.UPLOADING_TO_STORAGE, 2, false, false, "Replace Artifact - Uploading to Storage"),
                Arguments.of(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL, 3, false, true, "Replace Artifact - Storage Upload Successful"),
                Arguments.of(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL, 4, true, true, "Replace Artifact - CDN Upload Successful")
        );
    }

    protected MgmtRetryFullRolloutRequestBody retryFullRolloutCreateRequestBody(String description, MgmtRolloutStartType startType, Long startDate , Long endDate) {
        MgmtRetryFullRolloutRequestBody retryRequest = new MgmtRetryFullRolloutRequestBody();
        retryRequest.setDescription(description);
        retryRequest.setStartType(startType);
        retryRequest.setStartDate(startDate);
        retryRequest.setEndDate(endDate);
        return retryRequest;
    }

    protected MgmtCloneRolloutRequestBody cloneRolloutRequestBody(String name, String description, MgmtRolloutStartType startType, Long startDate , Long endDate) {
        MgmtCloneRolloutRequestBody cloneRequest = new MgmtCloneRolloutRequestBody();
        cloneRequest.setName(name);
        cloneRequest.setDescription(description);
        cloneRequest.setStartType(startType);
        cloneRequest.setStartDate(startDate);
        cloneRequest.setEndDate(endDate);
        return cloneRequest;
    }
}
