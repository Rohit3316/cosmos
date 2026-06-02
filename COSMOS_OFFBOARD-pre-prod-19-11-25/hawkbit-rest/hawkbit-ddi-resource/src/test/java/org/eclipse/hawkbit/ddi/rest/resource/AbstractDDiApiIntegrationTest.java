/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.dataformat.cbor.CBORGenerator;
import com.fasterxml.jackson.dataformat.cbor.CBORParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.IntStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.ddi.DdiActionFeedback;
import org.cosmos.models.ddi.DdiActionFeedbacks;
import org.cosmos.models.ddi.DdiConfirmationFeedback;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiSignature;
import org.cosmos.models.ddi.DdiSignatureType;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.ddi.DdiUserAcceptanceMessage;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.EcuModels;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutCondition;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroup;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignEcuModelRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtArtifactsResource;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactModuleLinkRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.GeneralFeedbackRepository;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaSigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.jpa.service.RedisCacheService;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetSoftware;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.hamcrest.CoreMatchers;
import jakarta.validation.constraints.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.GetParameterRequest;
import software.amazon.awssdk.services.ssm.model.GetParameterResponse;

import static org.cosmos.models.ddi.DdiRestConstants.ESP;
import static org.cosmos.models.ddi.DdiRestConstants.RSP;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_VEHICLES;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ContextConfiguration(classes = {DdiApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestChannelBinderConfiguration.class})
@TestPropertySource(locations = "classpath:/ddi-test.properties")
public abstract class AbstractDDiApiIntegrationTest extends AbstractRestIntegrationTest {

    protected static final String LOCAL_HOST = "http://localhost";
    protected static final String HTTP_LOCALHOST = LOCAL_HOST + ":80/";
    protected static final String MOCK_ARTIFACT_FILE_URI = "http://localhost:%d/file-url";
    protected static final String DEVICE_V1_TENANTS = "device/v1";
    protected static final String TENANT_ID = "1";
    protected static final String CONTROLLER_BASE = "/device/v1/controllers/{controllerId}";
    protected static final String SOFTWARE_MODULE_ARTIFACTS = CONTROLLER_BASE
            + "/softwaremodules/{softwareModuleId}/artifacts";
    protected static final String DEPLOYMENT_BASE = CONTROLLER_BASE + "/actions/{actionId}/deployed";
    protected static final String DEPLOYMENT_FEEDBACK = DEPLOYMENT_BASE + "/feedback";
    protected static final String ALL_FEEDBACK = DEPLOYMENT_BASE + "/allFeedback";
    protected static final String DEPLOYMENT_LOGS = DEPLOYMENT_BASE + "/logs";
    protected static final String CANCEL_ACTION = CONTROLLER_BASE + "/actions/{actionId}";
    protected static final String CANCEL_FEEDBACK = CANCEL_ACTION + "/cancelled";
    protected static final String INSTALLED_BASE = CONTROLLER_BASE + "/actions/{actionId}/installed";
    protected static final String CONFIRMATION_BASE = CONTROLLER_BASE + "/confirmation";
    protected static final String CONFIRMATION_BASE_ACTION = CONTROLLER_BASE + "/confirmation/actions/{actionId}";
    protected static final String CONTROLLERS_URL = "/controllers/";
    protected static final String INVENTORY = "/inventory";
    protected static final int ARTIFACT_SIZE = 5 * 1024;
    protected static final String SESSION_ID_HEADER = "536ffe3d6jh6";
    protected static final String DEPLOYED = "$.deployment";
    protected static final String CORRLEATION_ID = "correlationId";
    protected static final String SOFTWAREMODULES = "/softwaremodules/";
    protected static final String ARTIFACTS = "/artifacts/";
    protected static final String URL = "http://localhost/";
    protected static final String ACTIONS = "/actions/";
    protected static final String SUCCESS = "success";
    protected static final String FINISHED_SUCCESS = "finished_success";
    protected static final String FINISHED_FAILURE = "finished_failure";
    protected static final String TABLE_SP_VEHICLE_MODEL = "sp_vehicle_model";
    protected static final String TABLE_SP_TARGET_INVENTORY = "sp_target_inventory";
    protected static final String TABLE_SP_RSP_ROLLOUT = "sp_rsp_rollout";
    protected static final String TABLE_SP_ESP_ECU_ROLLOUT = "sp_esp_ecu_rollout";
    protected static final String TABLE_SP_ESP = "sp_esp";
    protected static final String TABLE_SP_RSP = "sp_rsp";
    protected static final String REJECTED = "rejected";
    protected static final String DOWNLOAD = "download";
    protected static final String INSTALL = "install";
    protected static final String CANCELED = "canceled";
    protected static final String FOTA = "FOTA";
    protected static final String AOTA = "AOTA";
    protected static final String USER_SCHEDULED = "user_scheduled";
    protected static final String DOWNLOAD_IN_PROGRESS = "download_in_progress";
    protected static final String DOWNLOAD_STARTED = "download_started";
    protected static final String DD_SENT = "dd_sent";
    protected static final String DOWNLOAD_COMPLETED = "download_completed";
    protected static final String USER_ACCEPTED = "user_accepted";
    protected static final String CONFIRMATION = "$.confirmation";
    protected static final String DOT_DOWNLOAD = ".download";
    protected static final String UPDATE = ".update";
    protected static final String ARTIFACT_FILE_TYPE_FULL = "FULL";
    protected static final String ARTIFACT_FILE_TYPE_DELTA = "DELTA";
    protected static final String JSON_PATH_LINKS_SOFTWARES_HREF = "$._links.softwares.href";
    protected static final String JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF = "$._links.supportPackages.href";
    protected static final String JSON_PATH_LINKS_FREEZE_HREF = "$._links.freeze.href";
    protected static final String SOFTWARES_URL = "/softwares";
    protected static final String SUPPORT_PACKAGES_URL = "/support-packages";
    protected static final String FREEZE_URL = "/action/freeze";
    protected static final String SEPERATOR = "/";
    protected static final ObjectMapper objectMapper = new ObjectMapper();
    protected static final String JSON_PATH_STATUS = "$.status";
    protected static final String JSON_PATH_NAME = "$.name";
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
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME= "$.deploymentMetadata.estimatedUpdateTime";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_PERCENTAGE";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMPERATURE";
    protected static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC = "$.deploymentMetadata.requiredStateOfCharge.BATTERY_TEMP_METRIC";
    protected static final String JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER = "$.maxDownloadCellularDurationTimer";
    protected static final String JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER = "$.maxDownloadDurationTimer";
    protected static final String JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER = "$.maxDownloadWifiDurationTimer";
    protected static final String JSON_PATH_DOWNLOAD_RETRY_COUNT = "$.downloadRetryCount";
    protected static final String JSON_PATH_MAX_UPDATE_TIME = "$.maxUpdateTime";
    protected static final String JSON_PATH_LINKS_GROUPS_HREF = "$._links.groups.href";
    protected static final String JSON_PATH_LINKS_SELF_HREF = "$._links.self.href";
    protected static final String HREF_ROLLOUT_PREFIX = "http://localhost/management/v1/tenants/1/rollouts/";
    protected static final String GROUPS_URL = "/groups";
    protected static final String CAMPAIGN_FOR_HPC_UPDATE = "Campaign for HPC Update";
    protected static final String JSON_PATH_DESCRIPTION = "$.description";
    protected static final String DRAFT = "draft";
    protected static final String CELLULAR = "cellular";
    protected static final String ORIGINAL_FILENAME = "origFilename.csv";
    protected static final String NONE = "none";
    protected static final Integer DEPLOYMENT_ESTIMATED_UPDATE_TIME = 1000;
    protected static final Integer MAX_UPDATE_TIME = 1800;
    protected static ClientAndServer mockServer;
    protected static Target target;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Mock
    protected S3FileTransferSnsService s3FileTransferSnsService;
    @Autowired
    protected ArtifactsRepository artifactsRepository;
    @Autowired
    protected ArtifactModuleLinkRepository artifactModuleLinkRepository;
    @Autowired
    protected EspRepository espRepository;
    @Autowired
    protected RspRepository rspRepository;
    @Autowired
    MgmtArtifactsResource mgmtArtifactsResource;
    @Autowired
    GeneralFeedbackRepository generalFeedbackRepository;
    @Autowired
    protected ActionStatusRepository actionStatusRepository;
    @Autowired
    protected ActionRepository actionRepository;
    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    protected RolloutRepository rolloutRepository;
    @Autowired
    protected SoftwareModuleRepository softwareModuleRepository;
    @Autowired
    protected ActionArtifactRepository actionArtifactRepository;
    @MockBean
    protected PKIManagement pkiManagement;
    @MockBean
    protected S3Client s3Client;
    @MockBean
    protected SsmClient ssmClient;
    @MockBean
    protected RedisCacheService redisCacheService;
    @MockBean
    protected S3MultipartFileUpload s3MultipartFileUpload;
    @Autowired
    protected DdiSignatureService ddiSignatureService;

    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_LOGS_MAX_LOG_ALL_FILE_SIZE = "$.deploymentDescription.deploymentMetadata.logs.maxLogAllFileSize";
    protected static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    protected static final String KAFKA_INVENTORY_ENDPOINT = "/kafka/inventory";
    protected static final String KAFKA_DD_ARTIFACT_EXPIRY_URL = "/kafka/artifact/error";
    protected static final String TARGET_COMPLETED_INSTALLATION_MSG = "Target completed installation.";
    protected static final String TARGET_PROCEEDING_INSTALLATION_MSG = "Target proceeding installation.";
    protected static final String TARGET_SCHEDULED_INSTALLATION_MSG = "Target scheduled installation.";
    protected static final String ENCODED_INVENTORY_HASH = "MEUCIQDl/3WHVfpYaZS6M5QqcBWdS7MqP1XUhYJgOWmvDJ8X/gIgFUkIyiZxSgPyeYN8Lc2nVyJkmFx9rQcY87bJ41uAWbk=";
    protected static final Long VALID_ROLLOUT_END_DATE = Instant.now().plus(52, ChronoUnit.HOURS).getEpochSecond();
    protected static final Long VALID_ROLLOUT_START_DATE = Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond();

    protected static final String VEHICLE_MODEL_NAME = "STLA-Brain";
    protected static final String CONTROLLER = "controller";
    protected static final String CONTROLLER_BASE_URL = "/controllers/";
    protected static final String DEFAULT_TENANT_URL = "/default-tenant/";
    protected static final String JSON_PATH_CONFIG_POLLING_SLEEP = "$.config.polling.sleep";
    protected static final String TIME = "00:01:00";
    protected static final String VALUE = "00:02:00";
    protected static final String JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF = "$._links.deploymentBase.href";
    protected static final String NAME = "If-None-Match";
    protected static final String JSON_PATH_LINKS_INSTALLED_BASE_HREF = "$._links.installedBase.href";


    protected static final String FAILURE = "failure";
    protected static final String ETAG = "ETag";
    protected static final String LOG_UPLOAD_IN_PROGRESS = "LOG_UPLOAD_IN_PROGRESS";
    protected static final String LOG_UPLOAD_SUCCESS = "LOG_UPLOAD_SUCCESS";
    protected static final String LOG_UPLOAD_FAILURE = "LOG_UPLOAD_FAILURE";
    protected static final String JSON_PATH_ACTION_HISTORY_MESSAGES = "$.actionHistory.messages";
    protected static final String VALUE1 = "00:05:00";
    protected static final String CONTROLLER_ID = "VINTEST179126S";
    protected static final String CONTROLLER_ID1 = "VINTEST179100S";
    protected static final String TARGET_NAME = "VINTEST179101S";
    protected static final String CONTROLLER_ID2 = "VINTEST179102S";
    protected static final String CONTROLLER_ID3 = "RS1234F452432";
    protected static final String FORCED = "forced";
    protected static final String CONTROLLER_ID4 = "VINTEST1791235";
    protected static final String TEST_ECUMODEL_1 = "Test ECU Model 1";
    protected static final String TEST_ECUMODEL_2 = "Test ECU Model 2";
    protected static final String SU = "SU";
    protected static final String OM = "OM";
    protected static final String MESSAGE = "The update was downloaded by the ECU";
    protected static final String MESSAGE1 = "The update downloading by the ECU";
    protected static final String MESSAGE2 = "The update completed by the ECU";
    protected static final String JSON_PATH_LINKS_LOG_COLLECTION_HREF = "$._links.logCollection.href";
    protected static final String CONTROLLER_11 = "controller11";
    protected static final String SIGNATURE_TYPE = "signatureType";
    protected static final String JSON_PATH_MESSAGE = "$.message";
    protected static final String EXCEPTION = "jakarta.validation.ValidationException";
    protected static final String ECU_LIST = "Node Address/ Part Number/ Hardware version / Scomos are missing in ECU list";
    protected static final String FILENAME = "filename";
    protected static final String FILE = "file";
    protected static final String LOG = "TestLog";
    protected static final String SEQUENCE = "sequence";
    protected static final String BYTESIZE = "bytesize";
    protected static final String RANGE = "range";
    protected static final String LAST_CHUNK = "isLastChunk";
    protected static final String LAST_FILE = "isLastFile";
    protected static final String NODE = "30 A0";
    protected static final String X250 = "X250";
    protected static final String STLA = "STLA";
    protected static final String VALUE_202 = "202";
    protected static final String GETSWM8 = "GETSWM8";
    protected static final String GETSWM9 = "GETSWM9";
    protected static final String RTCU_MODULE_NAME1 = "RTCU10R1WD***********************";
    protected static final String RTCU_MODULE_NAME2 = "RTCU10ROWD***********************";
    protected static final String HPC_MODULE_NAME = "HPC10ROWDEV01********************";
    protected static final String VERSION = "3.0.2";
    protected static final String CONTROLLER_ACTION_DEPLOYMENT_PATH = "/{controllerId}/actions/{actionId}/deployed";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION = "$.deploymentDescription";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DOWNLOAD = "$.deploymentDescription.download";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_UPDATE = "$.deploymentDescription.update";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS = "$.deploymentDescription.ecus";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_ECU_MODEL_TYPE = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].ecuModelType";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_NAME = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swName";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_FILENAME = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.filename";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_TARGET_VERSION = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.targetVersion";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_SOURCE_VERSION = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.sourceVersion";
    protected static final String SOURCE_VERSION_1 = "60570118**52239958***********************0640324U400*****";
    protected static final String TARGET_VERSION_1 = "61570118**52239958***********************0640324U400*****";
    protected static final String TARGET_VERSION_2 = "50570118**52239958***********************0640324U316*****";
    protected static final String TARGET_VERSION_3 = "68541061AX00000000000000000008*670181252*133156002JY0000";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_SIZE = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.size";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_HASHES_SHA256 = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.hashes.sha256";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_HASHES_MD5 = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.hashes.md5";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_SOFTWARE_OS_SW_ARTIFACT_LINKS_DOWNLOAD_HTTP_HREF = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='OS')].swArtifact.links[?(@.rel=='download-http')].href";
    protected static final String TEST_FILE_DELTA_FOR_DD_1 = "testFileDeltaForDD1";
    protected static final String VERSION1 = "68541061AX00000000000000000008*670181252*133156002JY0000";
    protected static final String VERSION2 = "61570118**52239958***********************0640324U400*****";
    protected static final String VERSION3 = "68541061AX00000000000000000008*670181252*133156002JY0011";
    protected static final String VERSION4 = "68541061AX00000000000000000008*670181252*133156002JY00001";
    protected static final String DOWNLOAD_URL = "/download";
    protected static final String ARTIFACTS_URL = "/artifacts/";
    protected static final String SOFTWAREMODULES_BASE_URL = "/softwaremodules/";
    protected static final String TEST_FILE_FOR_DD_2 = "testFileForDD2";
    protected static final String TARGET_VERSION = "50570118**52239958***********************0640324U316*****";
    protected static final String TEST_FILE_FOR_DD_3 = "testFileForDD3";
    protected static final String SOURCE_VERSION = "60570118**52239958***********************0640324U400*****";
    protected static final String V1 = "v1";
    protected static final String TEST1 = "test1";
    protected static final String TEST2 = "test2";
    protected static final String TEST3 = "test3";
    protected static final String FILE1 = "file1";
    protected static final String FILE2 = "file2";
    protected static final String FILE3 = "file3";
    protected static final String SHA_256 = "SHA_256";
    protected static final String DESCRIPTION = "description";
    protected static final String VALUE_5120 = "5120";
    protected static final Long SIZE = 123L;
    protected static final String RS1234F45240S = "RS1234F45240S";
    protected static final String R3W2234F45240S = "3W2234F45240S";
    protected static final String VALUE_200 = "200";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_CONNECTIVITY_TYPE = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.connectivityType";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.downgradeAllowed";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.requiredStateOfCharge";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_END_DATE = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.endDate";
    protected static final String WIFI_PREFERRED = "WIFI_PREFERRED";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_DOWNLOAD_RETRY_COUNT = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.downloadRetryCount";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_MAX_DOWNLOAD_DURATION_TIMER = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.maxDownloadDurationTimer";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_MAX_WIFI_DOWNLOAD_DURATION_TIMER = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.maxWifiDownloadDurationTimer";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.maxCellularDownloadDurationTimer";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_MAX_UPDATE_TIME = JSON_PATH_DEPLOYMENT_DESCRIPTION + ".deploymentMetadata.maxUpdateTime";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_ESP = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].esp";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_ECU_MODEL_TYPE = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].ecuModelType";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_NAME = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swName";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_FILENAME = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.filename";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_TARGET_VERSION = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.targetVersion";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_SOURCE_VERSION = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.sourceVersion";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_SIZE = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.size";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_HASHES_SHA256 = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.hashes.sha256";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS_30A0_SOFTWARE_0_SW_ARTIFACT_LINKS_DOWNLOAD_HTTP_HREF = JSON_PATH_DEPLOYMENT_DESCRIPTION_ECUS + "[?(@.ecuNodeId=='30 A0')].software[0].swArtifact.links[?(@.rel=='download-http')].href";
    protected static final String UDS_FLOW = "UDS_FLOW";
    protected static final String STLA_1 = "STLA1";
    protected static final String VIN_2 = "controller12346";
    protected static final String VIN_1 = "controller12345";
    protected static final String ECU_NODE_ID = "40 A0";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE = "$.deploymentDescription.rsp.adaLicense";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE_FILE_TYPE = "$.deploymentDescription.rsp.adaLicense.fileType";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE_FILE_NAME = "$.deploymentDescription.rsp.adaLicense.fileName";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE_HASHES_SHA_256 = "$.deploymentDescription.rsp.adaLicense.hashes.sha256";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE_LINKS_REL_DOWNLOAD_HTTP_HREF = "$.deploymentDescription.rsp.adaLicense.links[?(@.rel=='download-http')].href";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_RSP_ADA_LICENSE_RSP_SIGNATURE = "$.deploymentDescription.rsp.adaLicense.rspSignature";
    protected static final String ADA_LICENSE = "ADA_LICENSE";
    protected static final String TENANT_NOT_EXISTS = "tenantDoesNotExists";
    protected static final String ADA_LICENSE_FILE_TYPE = "adaLicense";
    protected static final String MESSAGE3 = "Vehicle status message sent successfully to Kafka service";
    protected static final String JSON_PATH_LINKS_INVENTORY_HREF = "$._links.inventory.href";
    protected static final String ROLLOUT_1 = "rollout1";

    protected static final String ROLLOUT_LOGS = "rolloutLogs";
    protected static final String VALUE_4711 = "4711";
    protected static final String SIGNATURE_TYPE1 = "SHA256withECC";
    protected static final String CONTROLLER_ID_INVENTORY_URL = "/{controllerId}/inventory";
    protected static final String DEPLOYMENT_SIGNATURE = "deploymentSignature";
    protected static final String JSON_PATH_ECU_SOFTWARE_RUNTIME_NAME = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swName";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_FILENAME = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.filename";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_TARGET_VERSION = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.targetVersion";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_SOURCE_VERSION = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.sourceVersion";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_SIZE = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.size";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_SHA256_HASH = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.hashes.sha256";
    protected static final String JSON_PATH_ECU_RUNTIME_SW_ARTIFACT_DOWNLOAD_HTTP_LINK = "$.deploymentDescription.ecus[?(@.ecuNodeId=='40 A0')].software[?(@.swType=='runtime')].swArtifact.links[?(@.rel=='download-http')].href";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_LOGS_MAX_NUMBER_OF_FILES = "$.deploymentDescription.deploymentMetadata.logs.maxNumberOfFiles";
    protected static final String JSON_PATH_DEPLOYMENT_DESCRIPTION_DEPLOYMENT_METADATA_LOGS_MAX_LOG_EACH_FILE_SIZE = "$.deploymentDescription.deploymentMetadata.logs.maxLogEachFileSize";
    protected static final String FINISH_SUCCESS = "Finish success";
    protected static final String FINISH_FAILURE = "Finish failure";
    protected static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String SP_ARTIFACTS = "sp_artifacts";
    protected static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    protected static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    protected static final String SP_TARGET_INVENTORY = "sp_target_inventory";
    protected static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    protected static final String SP_RSP_ROLLOUT = "sp_rsp_rollout";
    protected static final String SP_ESP_ECU_ROLLOUT = "sp_esp_ecu_rollout";
    protected static final String SP_ESP = "sp_esp";
    protected static final String SP_RSP = "sp_rsp";
    protected static final String SP_TARGET = "sp_target";
    protected static final String SP_ROLLOUT = "sp_rollout";
    protected static final String SP_ACTION_ARTIFACT = "sp_action_artifact";
    protected static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    protected static final String VALID_CONTROLLER_ID = "1C4SJUFJ9NS100607";
    protected static final String inventoryHash = "ZsGZQ4bvMP4MRjYjEGMGkpCNnm79k0pOW9USF35ti3hxMRioagVS2rk1h9m4uXpK";
    protected static final List<String> errorCodes = List.of("ERR_001", "ERR_002");
    protected static final String JSON_DD_ROLLOUTNAME = "$.deploymentDescription.rolloutName";
    protected static final String JSON_DD_DESCRIPTION = "$.deploymentDescription.description";
    protected static final String JSON_DD_ACTIONID = "$.deploymentDescription.actionId";

    //These are the Sining certificate configuration for ECU ID issuers 01 - stla_otateam_test_pki_ds_subca_g1
    protected static final String TEST_ECU_ID_ISSUER_01 = "stla_otateam_test_pki_ds_subca_g1";
    protected static final String TEST_DD_PRIVATE_KEY_01 = "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIKFxdPyn6QZ1DFeIJe8q1LKNPXG0trhuNBb22hIamKYWoAoGCCqGSM49AwEHoUQDQgAE0F2C8bWyvM6f4L+toDvMwJis1Ck3BK61qJd17e4QfgvsDrubhTIcffzv/CiMDoYr5MDaqb30nhngvxOOCCJpyw==\n-----END EC PRIVATE KEY-----";
    protected static final String TEST_DD_SERVER_CERTIFICATE_01 = "-----BEGIN CERTIFICATE-----\nMIICOjCCAeCgAwIBAgIJAMNGCrznxnr+MAoGCCqGSM49BAMCMHYxCzAJBgNVBAYTAklUMQ4wDAYDVQQIDAVJdGFseTETMBEGA1UECgwKU3RlbGxhbnRpczEWMBQGA1UECwwNU1RMQSBPVEEgVGVhbTEqMCgGA1UEAwwhc3RsYV9vdGF0ZWFtX3Rlc3RfcGtpX2RzX3N1YmNhX2cxMB4XDTI0MDYwNzE0MDIzMFoXDTI2MDYwNzE0MDIzMFowZzELMAkGA1UEBhMCSVQxDjAMBgNVBAgMBUl0YWx5MRMwEQYDVQQKDApTdGVsbGFudGlzMRYwFAYDVQQLDA1TVExBIE9UQSBUZWFtMRswGQYDVQQDDBJjb3Ntb3MuZGV2LnNpZ24uZGQwWTATBgcqhkjOPQIBBggqhkjOPQMBBwNCAATQXYLxtbK8zp/gv62gO8zAmKzUKTcErrWol3Xt7hB+C+wOu5uFMhx9/O/8KIwOhivkwNqpvfSeGeC/E44IImnLo2YwZDAdBgNVHQ4EFgQUJWpm4EdN9tQT4OfleX24XWOZkkMwHwYDVR0jBBgwFoAUPzCY+y38Wo+Oi7zkz126H0GZj8cwEgYDVR0TAQH/BAgwBgEB/wIBAzAOBgNVHQ8BAf8EBAMCAYYwCgYIKoZIzj0EAwIDSAAwRQIgbkZT35kQbdfCXICv0RdyTXa9aelTQZE35Svt2Kob48sCIQDf6QuuStYQlh0wu4tY5ihWFfVs78cp3o+ck4ylvWubpw==\n-----END CERTIFICATE-----";
    protected static final String TEST_ESP_PRIVATE_KEY_01 = "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIDBvtEBL1bbd6srZ04CyKvVoO+Xfeli9DR2UCB8DRqvjoAoGCCqGSM49AwEHoUQDQgAEmNdt+8+ri03gNxYzQqDOMvvPDTNVOvwByOqJDYr1CJurS73AuSTasDLb2K4Lv3Fw6CDFfwdn3rihVcq4B/NQcw==\n-----END EC PRIVATE KEY-----";
    protected static final String TEST_ESP_SERVER_CERTIFICATE_01 = "-----BEGIN CERTIFICATE-----\nMIICOzCCAeGgAwIBAgIJAMNGCrznxnr/MAoGCCqGSM49BAMCMHYxCzAJBgNVBAYTAklUMQ4wDAYDVQQIDAVJdGFseTETMBEGA1UECgwKU3RlbGxhbnRpczEWMBQGA1UECwwNU1RMQSBPVEEgVGVhbTEqMCgGA1UEAwwhc3RsYV9vdGF0ZWFtX3Rlc3RfcGtpX2RzX3N1YmNhX2cxMB4XDTI0MDYwNzE0MDI1MVoXDTI2MDYwNzE0MDI1MVowaDELMAkGA1UEBhMCSVQxDjAMBgNVBAgMBUl0YWx5MRMwEQYDVQQKDApTdGVsbGFudGlzMRYwFAYDVQQLDA1TVExBIE9UQSBUZWFtMRwwGgYDVQQDDBNjb3Ntb3MuZGV2LnNpZ24uZXNwMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEmNdt+8+ri03gNxYzQqDOMvvPDTNVOvwByOqJDYr1CJurS73AuSTasDLb2K4Lv3Fw6CDFfwdn3rihVcq4B/NQc6NmMGQwHQYDVR0OBBYEFETQsaG3NnSZlEO/KLDhZStyC+i2MB8GA1UdIwQYMBaAFD8wmPst/FqPjou85M9duh9BmY/HMBIGA1UdEwEB/wQIMAYBAf8CAQMwDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA0gAMEUCIQDIf+Xoz+OQ8DFcYhvGZnWF8X53f3hnwWPLDgxq9DcxlgIgMBsq/NHdL50ZAjaOdKEhpecXMG7645FUKkI6G8wfrEA=\n-----END CERTIFICATE-----";
    protected static final String TEST_RSP_PRIVATE_KEY_01 = "-----BEGIN EC PARAMETERS-----\nBggqhkjOPQMBBw==\n-----END EC PARAMETERS-----\n-----BEGIN EC PRIVATE KEY-----\nMHcCAQEEIOgq4yO7NSdidIapO16K9XTYBtUDH+DdEiob95Dcnmp2oAoGCCqGSM49AwEHoUQDQgAEpWMvTxozclDoZvdjC+A9b4rmwuhlevkTO0ypyPIRCvRbQTkhkZl2fzCPVOhHXTRQ2j0uMpcGogvkp+9QjUuDNw==\n-----END EC PRIVATE KEY-----";
    protected static final String TEST_RSP_SERVER_CERTIFICATE_01 = "-----BEGIN CERTIFICATE-----\nMIICOzCCAeGgAwIBAgIJAMNGCrznxnsAMAoGCCqGSM49BAMCMHYxCzAJBgNVBAYTAklUMQ4wDAYDVQQIDAVJdGFseTETMBEGA1UECgwKU3RlbGxhbnRpczEWMBQGA1UECwwNU1RMQSBPVEEgVGVhbTEqMCgGA1UEAwwhc3RsYV9vdGF0ZWFtX3Rlc3RfcGtpX2RzX3N1YmNhX2cxMB4XDTI0MDYwNzE0MDMwMVoXDTI2MDYwNzE0MDMwMVowaDELMAkGA1UEBhMCSVQxDjAMBgNVBAgMBUl0YWx5MRMwEQYDVQQKDApTdGVsbGFudGlzMRYwFAYDVQQLDA1TVExBIE9UQSBUZWFtMRwwGgYDVQQDDBNjb3Ntb3MuZGV2LnNpZ24ucnNwMFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEpWMvTxozclDoZvdjC+A9b4rmwuhlevkTO0ypyPIRCvRbQTkhkZl2fzCPVOhHXTRQ2j0uMpcGogvkp+9QjUuDN6NmMGQwHQYDVR0OBBYEFIdwwO9gRsJBoA4/o7IC/Z46g4q6MB8GA1UdIwQYMBaAFD8wmPst/FqPjou85M9duh9BmY/HMBIGA1UdEwEB/wQIMAYBAf8CAQMwDgYDVR0PAQH/BAQDAgGGMAoGCCqGSM49BAMCA0gAMEUCIQCelR2tnU5WGk6N6W0QbU71V461ANmtSc3+7LeMh6CABgIgQzkHlf+XO0TTzFH8duKwVGmDcHBg1SaQ6KFn6kUfVHc=\n-----END CERTIFICATE-----";

    protected static final Logger log = LoggerFactory.getLogger(DdiRootControllerNewTest.class);
    protected final ObjectMapper mapper = new ObjectMapper();
    private static final String TARGET_DEVICES = "targetDevices";

    @Value("${ddi.inventory.inventory-signature}")
    private String inventorySignature;
    @Value("${ddi.inventory.staticInventory-signature}")
    private String staticInventorySignature;
    @Value("${ddi.inventory.staticInventory-hash}")
    private String staticInventoryHash;
    @Value("${ddi.inventory.rawInventory-signature}")
    private String rawInventorySignature;
    @Value("${ddi.inventory.rawInventory-details}")
    private String rawInventoryDetails;


    /**
     * Configuration for the DdiSignatureService test.
     * This configuration is used to generate signature for DD.
     */
    @org.springframework.boot.test.context.TestConfiguration
    static class DdiSignatureServiceTestConfig {
        @Bean
        public DdiSignatureService ddiSignatureService(@Autowired S3Client s3Client,
                                                       @Autowired SsmClient ssmClient,
                                                       @Autowired RedisCacheService redisCacheService) {
            return new DdiSignatureService(s3Client, ssmClient, redisCacheService);
        }
    }

    /**
     * Convert JSON to a CBOR equivalent.
     *
     * @param json JSON object to convert
     * @return Equivalent CBOR data
     * @throws IOException Invalid JSON input
     */
    protected static byte[] jsonToCbor(final String json) throws IOException {
        final JsonFactory jsonFactory = new JsonFactory();
        final JsonParser jsonParser = jsonFactory.createParser(json);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final CBORFactory cborFactory = new CBORFactory();
        final CBORGenerator cborGenerator = cborFactory.createGenerator(out);
        while (jsonParser.nextToken() != null) {
            cborGenerator.copyCurrentEvent(jsonParser);
        }
        cborGenerator.flush();
        return out.toByteArray();
    }

    /**
     * Convert CBOR to JSON equivalent.
     *
     * @param input CBOR data to convert
     * @return Equivalent JSON string
     * @throws IOException Invalid CBOR input
     */
    protected static String cborToJson(final byte[] input) throws IOException {
        final CBORFactory cborFactory = new CBORFactory();
        final CBORParser cborParser = cborFactory.createParser(input);
        final JsonFactory jsonFactory = new JsonFactory();
        final StringWriter stringWriter = new StringWriter();
        final JsonGenerator jsonGenerator = jsonFactory.createGenerator(stringWriter);
        while (cborParser.nextToken() != null) {
            jsonGenerator.copyCurrentEvent(cborParser);
        }
        jsonGenerator.flush();
        return stringWriter.toString();
    }

    private static String getDownloadAndUploadType(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        if (MgmtRolloutUserAcceptanceRequired.NO.equals(userAcceptanceRequired)) {
            return "forced";
        }
        return "attempt";
    }

    protected static ObjectMapper getMapper() {
        return objectMapper;
    }

    /**
     * Generates a SHA-256 hash for the given data.
     *
     * @param data The data to be hashed.
     * @return The SHA-256 hash of the data.
     * @throws NoSuchAlgorithmException If the SHA-256 algorithm is not available.
     */
    protected static String generateSha256Hash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return generateSha256Hash(bytes);
    }

    /**
     * Generates a SHA-256 hash from the given byte array.
     *
     * @param bytes The byte array to generate the hash from.
     * @return A string representing the SHA-256 hash of the input bytes.
     * Each byte is converted to its hexadecimal representation and concatenated together.
     */
    protected static String generateSha256Hash(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, TABLE_SP_VEHICLE_MODEL, TABLE_SP_TARGET_INVENTORY, TABLE_SP_RSP_ROLLOUT, TABLE_SP_ESP_ECU_ROLLOUT, TABLE_SP_ESP, TABLE_SP_RSP);
    }

    protected ResultActions postDeploymentFeedback(final String controllerId, final Long actionId, final String content,
                                                   final ResultMatcher statusMatcher) throws Exception {
        return postDeploymentFeedback(MediaType.APPLICATION_JSON_UTF8, controllerId, actionId, content.getBytes(),
                statusMatcher);
    }

    protected ResultActions postDeploymentFeedback(final MediaType mediaType, final String controllerId,
                                                   final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(ALL_FEEDBACK, controllerId, actionId)
                        .content(content).contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher);
    }

    protected ResultActions postCancelFeedback(final String controllerId, final Long actionId, final String content,
                                               final ResultMatcher statusMatcher) throws Exception {
        return postCancelFeedback(MediaType.APPLICATION_JSON_UTF8, controllerId, actionId, content.getBytes(),
                statusMatcher);
    }

    protected ResultActions postCancelFeedback(final MediaType mediaType, final String controllerId,
                                               final Long actionId, final byte[] content, final ResultMatcher statusMatcher) throws Exception {
        return mvc
                .perform(post(CANCEL_FEEDBACK, controllerId, actionId).content(content)
                        .contentType(mediaType).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher);
    }

    protected ResultActions performGet(final String url, final MediaType mediaType, final ResultMatcher statusMatcher,
                                       final String... values) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url, values).accept(mediaType))
                .andDo(MockMvcResultPrinter.print()).andExpect(statusMatcher)
                .andExpect(content().contentTypeCompatibleWith(mediaType));
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
                                                              final DistributionSet ds, final Artifacts artifact, final Artifacts artifactSignature, final Long actionId,
                                                              final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        final ResultActions resultActions = performGet(DEPLOYMENT_BASE, mediaType, status().isOk(), controllerId, actionId.toString());
        return verifyBasePayload(DEPLOYED, resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                downloadType, updateType);
    }

    protected ResultActions getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
                                                              final DistributionSet ds, final Artifacts artifact, final Artifacts artifactSignature, final Long actionId,
                                                              final Long osModuleId, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) throws Exception {
        return getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, getDownloadAndUploadType(userAcceptanceRequired), getDownloadAndUploadType(userAcceptanceRequired));
    }

    protected ResultActions getAndVerifyInstalledBasePayload(final String controllerId, final MediaType mediaType,
                                                             final DistributionSet ds, final Artifacts artifact, final Artifacts artifactSignature, final Long actionId,
                                                             final Long osModuleId, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) throws Exception {
        final ResultActions resultActions = performGet(INSTALLED_BASE, mediaType, status().isOk(), controllerId, actionId.toString());
        return verifyBasePayload(DEPLOYED, resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                getDownloadAndUploadType(userAcceptanceRequired), getDownloadAndUploadType(userAcceptanceRequired));
    }

    private ResultActions verifyBasePayload(final String prefix, final ResultActions resultActions, final String controllerId,
                                            final DistributionSet ds, final Artifacts artifact, final Artifacts artifactSignature, final Long actionId,
                                            final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        return resultActions.andExpect(jsonPath("$.id", equalTo(String.valueOf(actionId))))
                .andExpect(jsonPath(prefix + DOT_DOWNLOAD, equalTo(downloadType)))
                .andExpect(jsonPath(prefix + UPDATE, equalTo(updateType)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='jvm')].name",
                        contains(ds.findFirstModuleByType(runtimeType).get().getName())))
                //.andExpect(jsonPath(prefix + ".chunks[?(@.part=='jvm')].version",
                //        contains(ds.findFirstModuleByType(runtimeType).get().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].name",
                        contains(ds.findFirstModuleByType(osType).get().getName())))
                //.andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].version",
                //        contains(ds.findFirstModuleByType(osType).get().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].hashes.md5",
                        contains(artifact.getMd5Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[0].hashes.sha256",
                        contains(artifact.getSha256Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].size", contains(ARTIFACT_SIZE)))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].hashes.md5",
                        contains(artifactSignature.getMd5Hash())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='os')].artifacts[1].hashes.sha256",
                        contains(artifactSignature.getSha256Hash())))
                //.andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].version",
                //        contains(ds.findFirstModuleByType(appType).get().getVersion())))
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].metadata").doesNotExist())
                .andExpect(jsonPath(prefix + ".chunks[?(@.part=='bApp')].name")
                        .value(ds.findFirstModuleByType(appType).get().getName()));
    }

    protected String installedBaseLink(final String controllerId, final String actionId) {

        return URL + DEVICE_V1_TENANTS + CONTROLLERS_URL + controllerId
                + ACTIONS + actionId + DdiRestConstants.PATH_SEPARATOR + DdiRestConstants.INSTALLED_ACTION;
    }

    protected String deploymentBaseLink(final String controllerId, final String actionId) {
        return URL + DEVICE_V1_TENANTS + CONTROLLERS_URL + controllerId
                + ACTIONS + actionId + DdiRestConstants.PATH_SEPARATOR + DdiRestConstants.DEPLOYED_ACTION;
    }

    protected String deploymentLogLink(final String controllerId, final String actionId) {
        return URL + DEVICE_V1_TENANTS + CONTROLLERS_URL + controllerId + ACTIONS + actionId + DdiRestConstants.PATH_SEPARATOR + DdiRestConstants.DEPLOYED_ACTION + DdiRestConstants.PATH_SEPARATOR + DdiRestConstants.LOGS;
    }

    protected String cancelActionLink(final long actionId, final String controllerId) {
        return URL + DEVICE_V1_TENANTS + CONTROLLERS_URL + controllerId + ACTIONS + actionId + DdiRestConstants.PATH_SEPARATOR + DdiRestConstants.CANCELED;
    }

    protected String pushInventoryLink(final String controllerId) {
        return LOCAL_HOST + SEPERATOR + DEVICE_V1_TENANTS + CONTROLLERS_URL + controllerId + INVENTORY;
    }

    protected String getJsonRejectedCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(DdiStatus.ExecutionStatus.CANCELED_REJECT, null,
                Collections.singletonList(REJECTED));
    }

    protected String getJsonPostRejectedCancelActionFeedback() throws JsonProcessingException {
        return getJsonPostActionFeedback(DdiStatus.ExecutionStatus.CANCELED_REJECT, null,
                Collections.singletonList(REJECTED));
    }

    protected String getJsonRejectedDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(DdiStatus.ExecutionStatus.CANCELED_REJECT, null,
                Collections.singletonList(REJECTED));
    }

    protected String getJsonCanceledCancelActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(DdiStatus.ExecutionStatus.CANCELED, null,
                Collections.singletonList(CANCELED));
    }

    protected String getJsonPostCanceledCancelActionFeedback() throws JsonProcessingException {
        return getJsonPostActionFeedback(DdiStatus.ExecutionStatus.CANCELED, null,
                Collections.singletonList(CANCELED));
    }

    protected String getJsonCanceledDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonActionFeedback(DdiStatus.ExecutionStatus.CANCELED, null,
                Collections.singletonList(CANCELED));
    }

    protected String getJsonPostCanceledDeploymentActionFeedback() throws JsonProcessingException {
        return getJsonPostActionFeedback(DdiStatus.ExecutionStatus.CANCELED, null,
                Collections.singletonList(CANCELED));
    }

    protected String getJsonCanceledAcceptDeploymentActionFeedback() throws JsonProcessingException {
        DdiStatus ddiStatus = new DdiStatus(
                DdiStatus.ExecutionStatus.CANCELED_ACCEPT,
                null,
                500,
                Collections.singletonList("CANCELED_ACCEPT"),
                errorCodes,
                1737348213000L
        );
        ddiStatus.setInventoryHash(inventoryHash);
        return getJsonActionFeedbackObjectToSting(Collections.singletonList(ddiStatus));
    }

    protected String getJsonCanceledRejectDeploymentActionFeedback() throws JsonProcessingException {
        DdiStatus ddiStatus = new DdiStatus(
                DdiStatus.ExecutionStatus.CANCELED_REJECT,
                null,
                500,
                Collections.singletonList("CANCELED_REJECT"),
                errorCodes,
                1737348213000L
        );
        ddiStatus.setInventoryHash(inventoryHash);
        return getJsonActionFeedbackObjectToSting(Collections.singletonList(ddiStatus));
    }

    protected String getJsonFinishedFailureDeploymentActionFeedback() throws JsonProcessingException {
        DdiStatus ddiStatus = new DdiStatus(
                DdiStatus.ExecutionStatus.FINISHED_FAILURE,
                null,
                500,
                Collections.singletonList("FINISHED_FAILURE"),
                errorCodes,
                1737348213000L
        );
        ddiStatus.setInventoryHash(inventoryHash);
        return getJsonActionFeedbackObjectToSting(Collections.singletonList(ddiStatus));
    }

    protected String getJsonFinishedSuccessDeploymentActionFeedback() throws JsonProcessingException {
        DdiStatus ddiStatus = new DdiStatus(
                DdiStatus.ExecutionStatus.FINISHED_SUCCESS,
                null,
                200,
                Collections.singletonList("FINISHED_SUCCESS"),
                Collections.emptyList(),
                1737348213000L
        );
        ddiStatus.setInventoryHash(inventoryHash);
        return getJsonActionFeedbackObjectToSting(Collections.singletonList(ddiStatus));
    }

    protected String getJsonActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                           final DdiDownload ddiDownload) throws JsonProcessingException {
        return getJsonActionFeedback(executionStatus, ddiDownload,
                Collections.singletonList(RandomStringUtils.randomAlphanumeric(1000)));
    }

    protected String getJsonActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                           final DdiDownload ddiDownload, final List<String> messages) throws JsonProcessingException {
        return getJsonActionFeedback(executionStatus, ddiDownload, 200, messages);
    }

    protected String getJsonPostActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                               final DdiDownload ddiDownload, final List<String> messages) throws JsonProcessingException {
        return getJsonPostActionFeedback(executionStatus, ddiDownload, 200, messages);
    }

    protected String getJsonActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                           final DdiDownload ddiDownload, final Integer code, final List<String> messages) throws JsonProcessingException {
        final List<DdiStatus> ddiStatuses = List.of(new DdiStatus(executionStatus, ddiDownload, code,
                messages, 1737089013000L));
        return objectMapper.writeValueAsString(new DdiActionFeedbacks(System.currentTimeMillis(), ddiStatuses));
    }

    protected String getJsonPostActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                               final DdiDownload ddiDownload, final Integer code, final List<String> messages) throws JsonProcessingException {
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, ddiDownload, code,
                messages, 1737089013000L);
        return objectMapper.writeValueAsString(new DdiActionFeedback(System.currentTimeMillis(), ddiStatus));
    }

    protected String getJsonActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                           final List<String> messages, final String inventoryHash) throws JsonProcessingException {
        final List<DdiStatus> ddiStatuses = Arrays.asList(new DdiStatus(executionStatus, messages, ""),
                new DdiStatus(executionStatus, messages, ""));
        return objectMapper.writeValueAsString(new DdiActionFeedbacks(System.currentTimeMillis(), ddiStatuses));
    }

    protected String getJsonActionFeedbackWithTimestamp(final DdiStatus.ExecutionStatus executionStatus,
                                                        final List<String> messages, final String inventoryHash, Long timestamp) throws JsonProcessingException {
        final List<DdiStatus> ddiStatuses = List.of(new DdiStatus(executionStatus, messages, inventoryHash, timestamp));
        ddiStatuses.get(0).setCode(2);
        ddiStatuses.get(0).setErrorCode(errorCodes);
        return objectMapper.writeValueAsString(new DdiActionFeedbacks(System.currentTimeMillis(), ddiStatuses));
    }

    protected String getJsonActionFeedback(final DdiStatus.ExecutionStatus executionStatus,
                                           final DdiDownload ddiDownload,
                                           final Integer code, final List<String> messages,
                                           final List<String> errorCode) throws JsonProcessingException {
        final DdiStatus ddiStatus = new DdiStatus(executionStatus, ddiDownload, code,
                messages, errorCode);
        return objectMapper.writeValueAsString(new DdiActionFeedback(System.currentTimeMillis(), ddiStatus));
    }

    protected String getJsonConfirmationFeedback(final DdiConfirmationFeedback.Confirmation confirmation,
                                                 final Integer code, final List<String> messages) throws JsonProcessingException {
        return objectMapper.writeValueAsString(new DdiConfirmationFeedback(confirmation, code, messages));
    }

    protected ResultActions getAndVerifyConfirmationBasePayload(final String controllerId, final MediaType mediaType,
                                                                final DistributionSet ds, final Artifacts artifact, final Artifacts artifactSignature, final Long actionId,
                                                                final Long osModuleId, final String downloadType, final String updateType) throws Exception {
        final ResultActions resultActions = performGet(CONFIRMATION_BASE_ACTION, mediaType, status().isOk(), controllerId, actionId.toString());
        return verifyBasePayload(CONFIRMATION, resultActions, controllerId, ds, artifact, artifactSignature, actionId, osModuleId,
                downloadType, updateType);
    }

    protected String getJsonActionFeedbackObjectToSting(List<DdiStatus> ddiStatuses) throws JsonProcessingException {

        return objectMapper.writeValueAsString(new DdiActionFeedbacks(System.currentTimeMillis(), new ArrayList<>(ddiStatuses)));
    }

    protected DdiStatus getDdiStatus(DdiStatus.ExecutionStatus status, DdiDownload ddiDownload, String message, Long timestamp) {
        return new DdiStatus(status, ddiDownload, 400,
                Collections.singletonList(message), timestamp);
    }

    protected DdiStatus getDdiStatusWithNoCode(DdiStatus.ExecutionStatus status, DdiDownload ddiDownload, String message, Long timestamp) {
        return new DdiStatus(status, ddiDownload, null,
                Collections.singletonList(message), timestamp);
    }

    protected DdiStatus getDdiStatusWithNoDownload(DdiStatus.ExecutionStatus status, DdiDownload ddiDownload, String message, Long timestamp) {
        return new DdiStatus(status, null, 400,
                Collections.singletonList(message), timestamp);
    }

    protected DdiStatus getDdiStatusWithErrorList(DdiStatus.ExecutionStatus status, DdiDownload ddiDownload, String message, Long timestamp) {
        return new DdiStatus(status, ddiDownload, 400,
                Collections.singletonList(message), Arrays.asList("ERR_00020181", "ERR_0002021", "ERR_00020111"), timestamp);
    }

    protected DdiStatus getDdiStatus(DdiStatus.ExecutionStatus status, Long timestamp, DdiDownload ddiDownload, String message, DdiUserAcceptanceMessage userAcceptanceMessage, String userAcceptanceMessageJob1) {
        return new DdiStatus(status, timestamp, ddiDownload, 200,
                Collections.singletonList(message), userAcceptanceMessage, userAcceptanceMessageJob1);
    }

    protected MgmtAddVersionResponse invokeCreateSoftwareVersionApi(long softwareModuleId, MgmtAddVersionRequestBody versionRequestBody) throws Exception {
        var content = mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, 1L, softwareModuleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(versionRequestBody)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readValue(content, MgmtAddVersionResponse.class);
    }

    /**
     * Creates a new software version for the specified software module.
     *
     * @param swModuleId the ID of the software module
     * @return the response containing details of the created software version
     * @throws Exception if an error occurs during the creation of the software version
     */
    protected MgmtAddVersionResponse createSoftwareVersion(long swModuleId) throws Exception {
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        return invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
    }

    protected MgmtArtifacts invokeCreateArtifactViaUrlApi(String fileUrl, String fileType) throws Exception {

        mockServer.when(
                request()
                        .withMethod("HEAD")
                        .withPath("/file-url")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Length", "100")
        );
        MgmtArtifactsRequest mgmtArtifactsRequest = testdataFactory.getRandomValidCreateArtifactsRequestBody(generateSha256Hash(RandomStringUtils.randomAlphanumeric(4)),
                fileUrl,
                fileType,
                RandomStringUtils.randomAlphanumeric(5));

        String requestJson = objectMapper.writeValueAsString(mgmtArtifactsRequest);

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("MOCK_MESSAGE_ID")
                .build();

        // Mock the behavior of publishMessage to return a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        lenient().when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        var mvcResult = mvc.perform(post(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON));
        var content = mvcResult.andReturn().getResponse().getContentAsString();
        log.info("content:{}", content);

        mvcResult.andExpect(status().isCreated());
        return objectMapper.readValue(content, MgmtArtifacts.class);
    }

    protected MgmtArtifacts invokeCreateArtifactViaUrlApi(String fileUrl) throws Exception {

        return invokeCreateArtifactViaUrlApi(fileUrl, ARTIFACT_FILE_TYPE_FULL);
    }

    protected MgmtArtifacts invokeCreateDeltaArtifactViaUrlApi(String fileUrl) throws Exception {

        return invokeCreateArtifactViaUrlApi(fileUrl, ARTIFACT_FILE_TYPE_DELTA);
    }

    protected List<MgmtSoftwareModuleRequestBodyPost> getSoftwareModuleRequestBodyList(List<String> names, String version) {

        return names.stream().map(name -> testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType, name, version)).toList();
    }

    protected List<MgmtSoftwareModule> invokeCreateSoftwareModuleApi(List<MgmtSoftwareModuleRequestBodyPost> requestBody) throws Exception {
        String swAssociationRequestBody = objectMapper.writeValueAsString(requestBody);
        var content = mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swAssociationRequestBody))
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    public List<MgmtSoftwareModule> createSoftwareModulesForDD(final List<String> names, final String version) throws Exception {
        var softwareModuleRequestBodyList = getSoftwareModuleRequestBodyList(names, version);
        return invokeCreateSoftwareModuleApi(softwareModuleRequestBodyList);
    }

    /**
     * Creates a rollout with the specified parameters.
     *
     * @param name      the name of the rollout
     * @param endDate   the end date of the rollout
     * @param startType the start type of the rollout
     * @throws Exception if an error occurs during the creation of the rollout
     */

    void invokeCreateRolloutApi(final String name, final Long endDate, final MgmtRolloutStartType startType, String type, String updateAction, List<String> updateActionUninstallVersion) throws Exception {
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"),1000);
        Long startDate = startType.equals(MgmtRolloutStartType.SCHEDULED) ? Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond() : null;
        final String rollout = JsonBuilder.rollout(name, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), startType.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, endDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, type, updateAction, updateActionUninstallVersion);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_NAME, CoreMatchers.equalTo(name))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, CoreMatchers.equalTo(CAMPAIGN_FOR_HPC_UPDATE)))
                .andExpect(jsonPath(JSON_PATH_STATUS, CoreMatchers.equalTo(DRAFT)))
                .andExpect(jsonPath(JSON_PATH_END_AT, CoreMatchers.equalTo(endDate.intValue())))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, CoreMatchers.equalTo("yes")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, CoreMatchers.equalTo(CELLULAR)))
                .andExpect(jsonPath(JSON_PATH_LOG_COLLECTION_REQUIRED, CoreMatchers.equalTo(true)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN, CoreMatchers.equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES, CoreMatchers.equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE, CoreMatchers.equalTo(50)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN, CoreMatchers.equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE, CoreMatchers.equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, CoreMatchers.equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, CoreMatchers.equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME, CoreMatchers.equalTo(1000)))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE, CoreMatchers.equalTo("60%")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE, CoreMatchers.equalTo("78 C")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC, CoreMatchers.equalTo("NA")))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, CoreMatchers.equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, CoreMatchers.equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, CoreMatchers.equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, CoreMatchers.equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, CoreMatchers.equalTo(MAX_UPDATE_TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))));
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

    protected void invokeAssociateEcuModelToVehicleModelApi(Long vehicleModelId, Long ecuModelId) throws Exception {
        var requestBody = testdataFactory.getAssociateEcuModelAndVehicleRequestBody(List.of(ecuModelId));
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, vehicleModelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    protected List<MgmtVechicleCreateResponse> invokeAddVehicleModelApi() throws Exception {
        var requestBody = testdataFactory.getAddVehicleModelRequestBody();
        var content = mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().is2xxSuccessful())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }

    protected List<MgmtCreateEcuModelResponse> invokeAddEcuModelApi() throws Exception {
        var requestBody = testdataFactory.getCreateEcuModelRequestBody();
        var content = mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().is2xxSuccessful())
                .andReturn()
                .getResponse().getContentAsString();

        return objectMapper.readValue(content, new TypeReference<>() {
        });
    }


    protected Target createTargets(String controllerId) {
        return testdataFactory.createTarget(controllerId);
    }

    protected long createSoftwareModule() throws Exception {
        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        return softwareModules.get(0).getModuleId();
    }

    protected MgmtArtifacts createArtifact(String fileUrl) throws Exception {
        return invokeCreateArtifactViaUrlApi(fileUrl);
    }

    protected MgmtArtifacts createDeltaArtifact(String fileUrl) throws Exception {
        return invokeCreateDeltaArtifactViaUrlApi(fileUrl);
    }

    protected void associateArtifactWithSoftwareModule(long swModuleId, long versionId, long artifactId) throws Exception {
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionId), Math.toIntExact(artifactId));
        changeArtifactStatus(artifactsRepository.findById(artifactId).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
    }

    protected void associateEcuModelToSoftwareModule(long swModuleId, long ecuNodeId) throws Exception {
        invokeAssociateEcuModelToSoftwareModuleApi(swModuleId, ecuNodeId);
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

    protected void invokeArtifactSoftwareModuleAssociationApi(int softwareModuleId, int targetVersion, int artifactId) throws Exception {
        SoftwareModuleArtifactBindingRequest moduleArtifactBindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModuleId)
                .sourceVersion(List.of())
                .targetVersion(targetVersion)
                .build();
        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING, TENANT_ID, artifactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isOk());
    }

    protected void associateArtifactWithSoftwareModuleVersions(long swModuleId, long sourceVersionId, long targetVersionId, long artifactId) throws Exception {
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(sourceVersionId), Math.toIntExact(targetVersionId), Math.toIntExact(artifactId));
        changeArtifactStatus(artifactsRepository.findById(artifactId).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
    }

    protected void invokeArtifactSoftwareModuleAssociationApi(int softwareModuleId, int sourceVersion, int targetVersion, int artifactId) throws Exception {
        SoftwareModuleArtifactBindingRequest moduleArtifactBindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                .softwareModuleId(softwareModuleId)
                .sourceVersion(List.of(sourceVersion))
                .targetVersion(targetVersion)
                .build();
        mvc.perform(post(MgmtRestConstants.CREATE_ARTIFACTS_SM_ASSOCIATIONN_V1_REQUEST_MAPPING, TENANT_ID, artifactId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moduleArtifactBindingRequest)))
                .andExpect(status().isOk());
    }

    public void changeArtifactStatus(Artifacts artifact, FileTransferStatus fileTransferStatus) {
        JpaArtifacts jpaArtifact = (JpaArtifacts) artifact;
        jpaArtifact.setFileStatus(fileTransferStatus.name());
        artifactsRepository.save(jpaArtifact);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            throw new RuntimeException();
        }
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


    protected MgmtSupportPackage createSupportPackage(long rolloutId, String supportPackageUrl, MgmtSupportPackageFileType fileType, String ecuNodeId, List<String> controllerIds) throws Exception {
        MgmtBaseSupportPackageCreateRequest createRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(fileType, ecuNodeId, supportPackageUrl, controllerIds);
        return invokeCreateSupportPackageApi(rolloutId, createRequest);
    }

    protected MgmtSupportPackage invokeCreateSupportPackageApi(long rolloutId, MgmtBaseSupportPackageCreateRequest request) throws Exception {
        var mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rolloutId, request).andReturn();
        log.info("supportPcakage Response:{}", mvcResult.getResponse().getContentAsString());
        assertEquals(HttpStatus.CREATED.value(), mvcResult.getResponse().getStatus());
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);
    }

    protected ResultActions invokeCreateSupportPackageApiAndReturnHttpResponse(long rolloutId, MgmtBaseSupportPackageCreateRequest request) throws Exception {
        String requestJson = objectMapper.writeValueAsString(request);

        return invokeCreateSupportPackageApiAndReturnHttpResponse(rolloutId, requestJson);

    }

    protected ResultActions invokeCreateSupportPackageApiAndReturnHttpResponse(long rolloutId, String requestBody) throws Exception {
        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("MOCK_MESSAGE_ID")
                .build();

        // Mock the behavior of publishMessage to return a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        lenient().when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        return mvc.perform(post(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, TENANT_ID, rolloutId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
    }

    protected void addDevice(List<String> controllerIds, long rolloutId) throws Exception {
        invokeAddDeviceApi(controllerIds, rolloutId);
    }

    private void invokeAddDeviceApi(List<String> controllerIds, long rolloutId) throws Exception {
        Path filePath = generateTargetDevicesFile(controllerIds);
        MockMultipartFile file = new MockMultipartFile("targetDevices", ORIGINAL_FILENAME, "text/csv", Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rolloutId)
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


    /**
     * Generates a temporary CSV file containing the given list of controller IDs.
     *
     * @param controllerIds the list of controller IDs to be written to the file
     * @return the path to the generated temporary file
     * @throws IOException if an I/O error occurs
     */
    private Path generateTargetDevicesFile(List<String> controllerIds) throws IOException {
        Path filePath = Files.createTempFile("controllerIds", ".csv");
        Files.writeString(filePath, String.join("\n", controllerIds), StandardOpenOption.WRITE);
        return filePath;
    }


    /**
     * Creates and saves support packages for a given rollout.
     *
     * @param rolloutId         The ID of the rollout.
     * @param ecuNodeId         The ECU node ID.
     * @param supportPackageUrl The URL of the support package.
     * @param controllerIds     The list of controller IDs.
     * @return A list of support package IDs.
     * @throws Exception If an error occurs during the creation or saving of support packages.
     */
    protected List<Long> createAndSaveSupportPackages(long rolloutId, String ecuNodeId, String supportPackageUrl, List<String> controllerIds) throws Exception {
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.WHATS_NEW, "someNodeId", supportPackageUrl, List.of());
        log.info("Create rsp Request:{}", createRspRequest);
        MgmtSupportPackage rspResponse = invokeCreateSupportPackageApi(rolloutId, createRspRequest);
        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);

        MgmtBaseSupportPackageCreateRequest createEspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtSupportPackage espResponse = invokeCreateSupportPackageApi(rolloutId, createEspRequest);
        JpaEsp esp = espRepository.findById(espResponse.getSupportPackageId()).get();
        esp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp);

        MgmtBaseSupportPackageCreateRequest createEspRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtSupportPackage espResponse1 = invokeCreateSupportPackageApi(rolloutId, createEspRequest1);
        JpaEsp esp1 = espRepository.findById(espResponse1.getSupportPackageId()).get();
        esp1.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(esp1);

        List<Long> supportPackageIds = new ArrayList<>();
        supportPackageIds.add(espResponse1.getSupportPackageId());
        supportPackageIds.add(espResponse.getSupportPackageId());
        supportPackageIds.add(rspResponse.getSupportPackageId());
        return supportPackageIds;
    }


    /**
     * Associates a software module with a rollout.
     *
     * @param swModuleId the ID of the software module
     * @param versionId  the ID of the software module version
     * @param rolloutId  the ID of the rollout
     * @throws Exception if an error occurs during the association
     */
    protected void associateSoftwareModuleWithRollout(long swModuleId, long versionId, long rolloutId) throws Exception {
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionId);
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rolloutId);
    }


    /**
     * Associates a list of software modules with a rollout.
     *
     * @param swAssociationRequestBody the list of software module requests to associate
     * @param rolloutId                the ID of the rollout to associate the software modules with
     * @throws Exception if an error occurs during the association
     */
    private void invokeRolloutSoftwareAssociateApi(List<MgmtSoftwareModuleRequest> swAssociationRequestBody, long rolloutId) throws Exception {
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, 1L, rolloutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swAssociationRequestBody)))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }

    /**
     * Freezes the specified rollout.
     *
     * @param rolloutId the ID of the rollout to freeze
     * @throws Exception if an error occurs during the freeze operation
     */
    void invokeRolloutFreezeApi(long rolloutId) throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT, 1L, rolloutId)).andReturn().getResponse().getContentAsString();

    }

    /**
     * Starts the rollout process.
     *
     * @param rolloutId the ID of the rollout
     */
    protected void startRollout(long rolloutId) {
        rolloutManagement.start(rolloutId);
    }

    /**
     * Handles the rollout process and verifies the expected status.
     *
     * @param rolloutName    the name of the rollout
     * @param expectedStatus the expected status of the rollout
     */
    protected void handleRollout(String rolloutName, RolloutStatus expectedStatus) {
        rolloutHandler.handleAll();
        Rollout rollout = getRolloutByName(rolloutName);
        assertNotNull(rollout);
        assertEquals(expectedStatus, rollout.getStatus());
    }

    /**
     * Retrieves a Rollout object by its name.
     *
     * @param rolloutName the name of the rollout
     * @return the Rollout object with the specified name
     */
    protected Rollout getRolloutByName(String rolloutName) {
        return rolloutManagement.getByName(rolloutName).orElse(null);
    }

    protected ResultActions sendDeploymentActionFeedback(final Target target, final Action action, final String execution, String finished, String message, DdiDownload ddiDownload) throws Exception {
        if (finished == null) {
            finished = NONE;
        }
        if (message == null) {
            message = RandomStringUtils.randomAlphanumeric(1000);
        }

        final String feedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.valueOf(execution.toUpperCase()), ddiDownload, Collections.singletonList(message));
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected ResultActions sendDeploymentActionFeedback(final Target target, final Action action, final String execution, final String finished, final DdiDownload ddiDownload) throws Exception {
        return sendDeploymentActionFeedback(target, action, execution, finished, null, ddiDownload);
    }

    protected ResultActions sendDeploymentActionFeedback(final Target target, final String inventoryHash, final Action action, final String execution, final List<String> messages) throws Exception {
        final String feedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.valueOf(execution.toUpperCase()), messages, inventoryHash);
        return sendDeploymentActionFeedback(target, action, feedback);

    }

    protected ResultActions sendDeploymentActionFeedback(Target target, Action action, String feedback) throws Exception {
        return mvc.perform(post(ALL_FEEDBACK, target.getControllerId(), action.getId()).content(feedback).contentType(MediaType.APPLICATION_JSON));
    }


    protected ResultActions sendFinishedSuccessDeploymentActionFeedback(final Target target, final Action action) throws Exception {
        final String feedback = getJsonFinishedSuccessDeploymentActionFeedback();
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected ResultActions sendCanceledDeploymentActionFeedback(final Target target, final Action action) throws Exception {
        final String feedback = getJsonCanceledDeploymentActionFeedback();
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected ResultActions sendCanceledAcceptDeploymentActionFeedback(final Target target, final Action action) throws Exception {
        final String feedback = getJsonCanceledAcceptDeploymentActionFeedback();
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected ResultActions sendCanceledRejectDeploymentActionFeedback(final Target target, final Action action) throws Exception {
        final String feedback = getJsonCanceledRejectDeploymentActionFeedback();
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected ResultActions sendFinishedFailureDeploymentActionFeedback(final Target target, final Action action) throws Exception {
        final String feedback = getJsonFinishedFailureDeploymentActionFeedback();
        return sendDeploymentActionFeedback(target, action, feedback);
    }

    protected Target setupRolloutAndGetTarget() throws Exception {
        var rolloutName = "newRollout";
        return setupRolloutAndGetTarget(rolloutName, VALID_CONTROLLER_ID);
    }

    protected Target setupRolloutAndGetTarget(String rolloutName, String controllerId) throws Exception {
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));

        long swModuleId = createSoftwareModule();
        associateEcuModelToSoftwareModule(swModuleId, ecuModelResponses.get(0).getId());
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());
        List<Long> ecuModelIds = List.of(ecuModelResponses.get(0).getId());
        softwareModuleManagement.assignEcuModel(swModuleId, Collections.singletonList(ecuModelIds.get(0)));

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);


        Target target = createTargets(controllerId);
        List<String> controllerIds = List.of(target.getControllerId());
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        associateEcuModelToVehicleModel(target.getVehicleModelId(), ecuModelResponses.get(0).getId());
        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
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


        handleRollout(rolloutName, RolloutStatus.RUNNING);
        return target;
    }

    protected Map<String, Object> setupRolloutWithTargets(int noOfTargets) throws Exception {
        Map<String, Object> result = new HashMap<>();
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));

        long swModuleId = createSoftwareModule();
        associateEcuModelToSoftwareModule(swModuleId, ecuModelResponses.get(0).getId());
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        List<Target> targets = testdataFactory.createTargets(noOfTargets);
        result.put("targets", targets);
        result.put("rollout", rollout);
        result.put("ecuNodeId", ecuNodeId);
        result.put("softwareModuleId", swModuleId);
        result.put("artifactId", artifacts.getArtifactId());

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


        handleRollout(rolloutName, RolloutStatus.RUNNING);
        return result;
    }

    protected Target setupRolloutAndGetTargetForDD() throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = String.format(MOCK_ARTIFACT_FILE_URI, mockServer.getPort());
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";

        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));

        long swModuleId = createSoftwareModule();

        List<Long> ecuModelIds = List.of(ecuModelResponses.get(0).getId());
        softwareModuleManagement.assignEcuModel(swModuleId, Collections.singletonList(ecuModelIds.get(0)));

        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        Target target = createTargets(VALID_CONTROLLER_ID);
        List<String> controllerIds = List.of(target.getControllerId());
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        associateEcuModelToVehicleModel(target.getVehicleModelId(), ecuModelResponses.get(0).getId());
        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

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


        handleRollout(rolloutName, RolloutStatus.RUNNING);
        String controllerId = target.getControllerId();

        // Fetch the SoftwareModule using its ID
        Optional<SoftwareModule> optionalSoftwareModule = softwareModuleManagement.getSoftwareModuleById(swModuleId);

        String softwareModuleName = "";
        if (optionalSoftwareModule.isPresent()) {
            // Retrieve the name of the SoftwareModule
            softwareModuleName = optionalSoftwareModule.get().getName();

        } else {
            // Handle the case where the SoftwareModule is not found
            throw new IllegalArgumentException("Software Module not found for ID: " + swModuleId);
        }

        JpaTargetSoftware targetSoftware = new JpaTargetSoftware(ecuNodeId, softwareModuleName,
                String.valueOf(versionResponse.getId()), target);

        Set<TargetSoftware> targetSoftwares = Set.of(targetSoftware);

        controllerManagement.updateTargetSoftware(controllerId, targetSoftwares);
        return target;
    }

    /**
     * Creates a new rollout, vehicle model, ECU model, and associated artifacts,
     * targets, rollout groups, and support packages.
     * Then simulates rollout lifecycle (handling, freezing, updating statuses).
     *
     * @param noOfTargets       Number of targets to create
     * @param noOfRolloutGroups Number of rollout groups to create
     * @return The created {@link JpaRollout} entity
     * @throws Exception if any error occurs during setup
     */
    protected Rollout createRolloutWithTargetsAndRolloutGroups(int noOfTargets, int noOfRolloutGroups) throws Exception {
        // Mock file server HEAD response for artifact URL
        mockServer.when(request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response()
                        .withHeader("Content-Length", "12345")
                        .withStatusCode(200));

        String port = System.getProperty("mock.server.port");
        String rolloutName = "newRollout";
        String fileUrl = "http://localhost:" + port + "/some-file";
        String supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        long rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        // Create and associate vehicle model and ECU model
        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        MgmtCreateEcuModelResponse ecuModelResponse = ecuModelResponses.get(0);
        String ecuNodeId = getEcuNodeId(ecuModelResponse);
        Long ecuModelId = ecuModelResponse.getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

        // Create software module and version
        long swModuleId = createSoftwareModule();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        // Create and retrieve rollout
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        JpaRollout rollout = (JpaRollout) rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        // Create targets and associate to rollout groups
        List<MgmtTarget> targets = createTargets(vehicleModelId, noOfTargets);
        List<String> controllerIds = getControllerIds(targets);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());

        // Upload support packages (RSP, ESPs)
        MgmtSupportPackage rspResponse = createAndUploadSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        MgmtSupportPackage esp1Response = createAndUploadSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        MgmtSupportPackage esp2Response = createAndUploadSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), noOfRolloutGroups);

        // Clear mocks, freeze rollout, and simulate transitions
        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        handleRollout();
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        handleRollout();
        // Verify correct transition of actions and groups
        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        handleRollout();
        return rollout;
    }

    /**
     * Creates and uploads a support package, updates its upload status.
     */
    private MgmtSupportPackage createAndUploadSupportPackage(Long rolloutId, String url, MgmtSupportPackageFileType type, String ecuNodeId, List<String> controllerIds) throws Exception {
        MgmtSupportPackage supportPackage = createSupportPackage(rolloutId, url, type, ecuNodeId, controllerIds);
        updateFileStatus(type == MgmtSupportPackageFileType.WHATS_NEW ? RSP : ESP, supportPackage.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
        return supportPackage;
    }

    /**
     * Creates a list of targets for the given vehicle model.
     */
    protected List<MgmtTarget> createTargets(Long vehicleModelId, int amountTargets) throws Exception {
        return invokeCreateTargetApi(vehicleModelId, amountTargets);
    }

    /**
     * Calls API to create targets and parses the response into objects.
     */
    protected List<MgmtTarget> invokeCreateTargetApi(Long vehicleModelId, int totalTargets) throws Exception {
        String responseAsString = invokeCreateTargetApiAndReturnResponseAsString(vehicleModelId, totalTargets);
        return objectMapper.readValue(responseAsString, new TypeReference<>() {
        });
    }

    /**
     * Calls API to create targets and returns raw JSON response string.
     */
    protected String invokeCreateTargetApiAndReturnResponseAsString(Long vehicleModelId, int totalTargets) throws Exception {
        List<MgmtTargetRequestBody> requestBody = testdataFactory.getCreateTargetRequestBody(vehicleModelId, totalTargets);
        MvcResult result = mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    /**
     * Extracts the list of controller IDs from a list of targets.
     */
    protected List<String> getControllerIds(List<MgmtTarget> targets) {
        return targets.stream().map(MgmtTarget::getControllerId).toList();
    }

    /**
     * Verifies the action status and group transition logic after rollout steps.
     */
    protected void verifyActionStatusAndTransition(MgmtAddDeviceDetailsResponse groupsResponse, long rolloutId) {
        int totalGroup = groupsResponse.getTotalGroups();

        IntStream.range(0, totalGroup).forEach(i -> {
            var group = groupsResponse.getGroups().get(i);
            Long groupId = group.getId();
            var rolloutGroup = rolloutGroupManagement.get(groupId).orElse(null);
            assertNotNull(rolloutGroup);

            List<JpaAction> actionList = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutId, groupId, true);

            switch (rolloutGroup.getStatus()) {
                case RUNNING -> verifyRunningGroup(totalGroup, (JpaRolloutGroup) rolloutGroup, actionList, groupId);
                case FINISHING -> assertTrue(actionList.stream()
                        .allMatch(action -> isFinishedStatus(action.getStatus())));
                default -> {
                    assertEquals(RolloutGroupStatus.QUEUED, rolloutGroup.getStatus());
                    assertTrue(actionList.isEmpty());
                }
            }
        });

    }

    /**
     * Verifies actions for a group with RUNNING status.
     */
    private void verifyRunningGroup(int totalGroup, JpaRolloutGroup rolloutGroup, List<JpaAction> actionList, Long groupId) {
        long finishedActions = actionList.stream()
                .filter(action -> isFinishedStatus(action.getStatus()))
                .count();
        int threshold = Integer.parseInt(rolloutGroup.getSuccessConditionExp());

        if (finishedActions == 0) {
            assertTrue(actionList.stream().allMatch(action -> action.getStatus().equals(DeviceActionStatus.RUNNING)));
        } else if ((float) finishedActions / totalGroup >= (float) threshold / 100F && !rolloutGroupRepository.findByParentId(groupId).isEmpty()) {
            assertFalse(rolloutGroupRepository.findByParentIdAndStatus(groupId, RolloutGroupStatus.RUNNING).isEmpty());
        } else {
            assertTrue(actionList.stream().anyMatch(action -> action.getStatus().equals(DeviceActionStatus.RUNNING)));
        }
    }

    /**
     * Checks if the device action status is a finished status.
     */
    private boolean isFinishedStatus(DeviceActionStatus status) {
        return status == DeviceActionStatus.FINISHED_SUCCESS ||
                status == DeviceActionStatus.FINISHED_FAILURE ||
                status == DeviceActionStatus.FINISHED_NOT_EXECUTED;
    }

    /**
     * Adds devices and rollout groups to an existing rollout.
     */
    protected MgmtAddDeviceDetailsResponse addDeviceDetails(List<String> controllerIds, long rolloutId, int totalGroups) throws Exception {
        return invokeAddDeviceApi(controllerIds, rolloutId, totalGroups);
    }

    /**
     * Invokes API to add devices via a CSV file upload and rollout group creation.
     */
    protected MgmtAddDeviceDetailsResponse invokeAddDeviceApi(List<String> controllerIds, long rolloutId, int totalGroups) throws Exception {
        Path filePath = generateTargetDevicesFile(controllerIds);
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, "text/csv", Files.newInputStream(filePath));

        var requestBuilder = MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rolloutId)
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.APPLICATION_JSON);

        if (totalGroups > 0) {
            List<MgmtRolloutGroup> groups = IntStream.range(0, totalGroups)
                    .mapToObj(i -> {
                        MgmtRolloutGroup group = new MgmtRolloutGroup();
                        group.setName(RandomStringUtils.randomAlphanumeric(5));
                        group.setTargetPercentage((float) (100 / totalGroups));
                        group.setErrorCondition(new MgmtRolloutCondition(MgmtRolloutCondition.Condition.THRESHOLD, "20"));
                        group.setSuccessCondition(new MgmtRolloutCondition(MgmtRolloutCondition.Condition.THRESHOLD, "50"));
                        return group;
                    })
                    .toList();

            String groupsJson = objectMapper.writeValueAsString(groups);
            requestBuilder.param("groups", groupsJson);
        }

        MvcResult mvcResult = mvc.perform(requestBuilder)
                .andExpect(jsonPath("$._links").doesNotExist())
                .andReturn();
        return objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtAddDeviceDetailsResponse.class);

    }

    /**
     * Creates a rollout group and associates it with the given rollout.
     *
     * @param targets the list of targets to associate with the rollout group
     * @param rollout the rollout to associate the group with
     */
    protected void createRolloutGroup(List<Target> targets, Rollout rollout) {
        rolloutManagement.get(rollout.getId()).ifPresent(savedRollout ->
                rolloutManagement.addDeviceDetails(savedRollout, targets.stream().map(Target::getControllerId).toList(),
                        null)
        );
    }

    protected void handleRollout() {
        rolloutHandler.handleAll();
    }

    /**
     * Updates all actions in a rollout group to the given status, triggers rollout scheduler,
     * and asserts that rollout and group statuses match the expected values.
     *
     * @param rollout               The rollout to update.
     * @param groupIndex            Index of the rollout group within the rollout.
     * @param expectedRolloutStatus Expected status of the rollout after handling.
     * @param expectedGroupStatus   Expected status of the rollout group after handling.
     */
    protected void updateActionsWithAlternatingFeedbackAndAssertStatuses(Rollout rollout, int groupIndex, RolloutStatus expectedRolloutStatus, RolloutGroupStatus expectedGroupStatus, String requestBody) throws Exception {
        JpaRolloutGroup rolloutGroup = getRolloutGroup(rollout, groupIndex);
        List<JpaAction> actions = getActions(rollout, rolloutGroup);

        for (JpaAction action : actions) {
            sendAllFeedback(action, requestBody);
        }

        handleRollout();

        assertEquals(expectedRolloutStatus, getUpdatedRollout(rollout).getStatus());
        assertEquals(expectedGroupStatus, getUpdatedRolloutGroup(rolloutGroup).getStatus());
    }

    /**
     * Updates the statuses of actions in a rollout group with specific logic (alternate statuses),
     * triggers rollout scheduler, and asserts intermediate statuses.
     *
     * @param rollout    The rollout to process.
     * @param groupIndex Index of the rollout group within the rollout.
     */
    protected void updateActionsWithAlternatingFeedbackAndAssertStatuses(Rollout rollout, int groupIndex) throws Exception {
        JpaRolloutGroup rolloutGroup = getRolloutGroup(rollout, groupIndex);
        List<JpaAction> actions = getActions(rollout, rolloutGroup);

        updateActionsWithAlternatingFeedback(actions);

        handleRollout();

        assertEquals(RolloutStatus.RUNNING, getUpdatedRollout(rollout).getStatus());
        inferGroupStatus(actions, getUpdatedRolloutGroup(rolloutGroup));
    }

    /**
     * Updates the statuses of the provided actions with alternating feedback.
     *
     * @param actions List of actions to update.
     */
    protected void updateActionsWithAlternatingFeedback(List<JpaAction> actions) throws Exception {
        for (int i = 0; i < actions.size(); i++) {
            JpaAction action = actions.get(i);
            if (i == 0 || i % 2 != 0) {
                sendAllFeedback(action, getJsonFinishedSuccessDeploymentActionFeedback());
            } else {
                sendAllFeedback(action, getJsonRejectedCancelActionFeedback());
                // TODO: Remove this manual update once the bug is fixed (COSMOS-1686)
                updateActionsWithStatus(action.getId(), DeviceActionStatus.PAUSED);
            }
        }
    }

    /**
     * Manually updates the status of an action.
     * (Temporary workaround for known bug.)
     */
    protected void updateActionsWithStatus(Long actionId, DeviceActionStatus status) {
        actionRepository.getActionById(actionId, true)
                .ifPresent(action -> {
                    action.setStatus(status);
                    actionRepository.save((JpaAction) action);
                });
    }

    /**
     * Asserts the rollout group status based on the list of action statuses.
     * - If only 1 action exists -> expect FINISHING
     * - Otherwise -> expect RUNNING
     *
     * @param actions      List of actions.
     * @param rolloutGroup Rollout group to verify.
     */
    protected void inferGroupStatus(List<JpaAction> actions, JpaRolloutGroup rolloutGroup) {
        RolloutGroupStatus expectedStatus = (actions.size() == 1) ? RolloutGroupStatus.FINISHING : RolloutGroupStatus.RUNNING;
        assertEquals(getUpdatedRolloutGroup(rolloutGroup).getStatus(), expectedStatus);
    }

    /**
     * Sends feedback for a given action with the specified request body.
     */
    private void sendAllFeedback(JpaAction action, String requestBody) throws Exception {
        mvc.perform(post(ALL_FEEDBACK, action.getTarget().getControllerId(), action.getId())
                .content(requestBody)
                .contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * Helper to retrieve actions by rollout and rollout group.
     */
    protected List<JpaAction> getActions(Rollout rollout, JpaRolloutGroup rolloutGroup) {
        return actionRepository.findByRolloutIdAndRolloutGroupId(rollout.getId(), rolloutGroup.getId(), true);
    }

    /**
     * Helper to retrieve rollout group by rollout and index.
     */
    protected JpaRolloutGroup getRolloutGroup(Rollout rollout, int groupIndex) {
        return rolloutGroupRepository.findByRolloutId(rollout.getId()).get(groupIndex);
    }

    /**
     * Helper to retrieve rollout group by rollou.
     */
    protected List<JpaRolloutGroup> getRolloutGroup(Rollout rollout) {
        return rolloutGroupRepository.findByRolloutId(rollout.getId());
    }

    /**
     * Helper to retrieve updated rollout entity.
     */
    protected Rollout getUpdatedRollout(Rollout rollout) {
        return rolloutRepository.findById(rollout.getId()).orElseThrow();
    }

    /**
     * Helper to retrieve updated rollout group entity.
     */
    protected JpaRolloutGroup getUpdatedRolloutGroup(JpaRolloutGroup rolloutGroup) {
        return rolloutGroupRepository.findById(rolloutGroup.getId()).orElseThrow();
    }

    /**
     * Builds a default inventory push request string using the provided VIN, ECU node address, and SCOMO ID.
     *
     * @param vin            the vehicle identification number
     * @param ecuNodeAddress the address of the ECU node
     * @param scomoId        the SCOMO ID
     * @return a string representing the default inventory push request
     */
    protected String buildDefaultInventoryPushRequest(String vin, String ecuNodeAddress, String scomoId) throws JsonProcessingException {
        ObjectNode inventoryRequest = mapper.createObjectNode();
        inventoryRequest.put("date", "2024-12-09T14:46:30Z");
        inventoryRequest.put("error", "");
        inventoryRequest.put("vin", vin);
        inventoryRequest.put("proxyString", "Test");

        // Create ECU list
        ArrayNode ecuArray = mapper.createArrayNode();
        ObjectNode ecuDetails = mapper.createObjectNode();
        ecuDetails.put("nodeAddr", ecuNodeAddress);
        ecuDetails.put("partnumber", "68426102AE");
        ecuDetails.put("hwVersion", "4.0002.00000016");
        ecuDetails.put("hwSignature", "hwSignature");
        ecuDetails.put("hwSignatureType", "SHA256withRSA");
        ecuDetails.put("serialNumber", "0263 014 379-2021.51.01");

        // Add SCOMO list
        ArrayNode scomoArray = mapper.createArrayNode();
        ObjectNode scomoDetails = mapper.createObjectNode();
        scomoDetails.put("scomoId", scomoId);
        scomoDetails.put("swVersion", "60570118**52239958***********");
        scomoDetails.put("swSignature", "swSignature");
        scomoDetails.put("swSignatureType", "SHA256withECDSA");
        scomoDetails.put("swFingerPrint", "NWQgMmMgMjIgNDQgNTQgNDMg...");

        scomoArray.add(scomoDetails);
        ecuDetails.set("scomos", scomoArray);

        // Add DTC list
        ArrayNode dtcArray = mapper.createArrayNode();
        ObjectNode dtcDetails = mapper.createObjectNode();
        dtcDetails.put("dtcMask", "19020E");
        dtcDetails.put("dtcCode", "5902FF1702682F556D626C");
        dtcArray.add(dtcDetails);
        ecuDetails.set("DTC", dtcArray);

        ecuArray.add(ecuDetails);
        inventoryRequest.set("ecuList", ecuArray);

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory deviceInventory = new DdiDeviceInventory(signature, Base64.getEncoder().encodeToString(inventoryRequest.toPrettyString().getBytes()), staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        return objectMapper.writeValueAsString(deviceInventory);
    }

    /**
     * Invokes the API to poll for updates for a specific controller.
     *
     * @param controllerId the ID of the controller to poll for updates
     * @return the result of the API call as an {@link MvcResult}
     * @throws Exception if an error occurs during the API call
     */
    protected MvcResult invokePollForUpdatesApi(String controllerId) throws Exception {
        return mvc.perform(get(DdiRestConstants.GET_INVENTORY_HASH_PATH, controllerId)
                        .param("hash", inventorySignature)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andReturn();
    }

    /**
     * Invokes the API to push inventory data for a specific controller.
     *
     * @param inventoryPushRequest the inventory data to be pushed
     * @param controllerId         the ID of the controller to push the inventory to
     * @return the result of the API call as an {@link MvcResult}
     * @throws Exception if an error occurs during the API call
     */
    protected MvcResult invokePushInventoryApi(String inventoryPushRequest, String controllerId) throws Exception {
        return mvc.perform(put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                        controllerId)
                        .content(inventoryPushRequest).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()).andReturn();
    }

    /**
     * Invokes the API to inspect the Deployment Descriptor (DD) link.
     *
     * @param inspectDDRequestBuilder the request builder for the inspect DD API call
     * @return the result of the API call as an {@link MvcResult}
     * @throws Exception if an error occurs during the API call
     */
    protected MvcResult invokeInspectDDApi(MockHttpServletRequestBuilder inspectDDRequestBuilder) throws Exception {
        MvcResult mvcResult = mvc.perform(inspectDDRequestBuilder
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andReturn();
        validateDeploymentSignature(mvcResult);
        return mvcResult;
    }

    /**
     * Invokes the API to purge artifacts.
     *
     * @param artifacts the artifacts to be purged
     * @return the result of the API call as an {@link MvcResult}
     * @throws Exception if an error occurs during the API call
     */
    protected MvcResult invokePurgeArtifactsApi(Artifacts artifacts) throws Exception {
        return mvc.perform(delete(MgmtRestConstants.PURGE_ARTIFACTS_V1_REQUEST_MAPPING, 1, artifacts.getId()))
                .andExpect(status().isOk()).andReturn();
    }


    /**
     * Invokes the API to retrieve the status of an action for a specific target (controller).
     *
     * @param rolloutId    the ID of the action whose status is to be retrieved
     * @param controllerId the ID of the target (controller) associated with the action
     * @return the result of the API call as a {@link ResultActions} object
     * @throws Exception if an error occurs during the API call
     */
    @NotNull
    protected ResultActions invokeGetActionStatusByRolloutIdAndControllerId(Long rolloutId, String controllerId) throws Exception {
        return mvc.perform(
                get(MgmtRestConstants.ACTION_STATUS_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId, controllerId)
                        .accept(MediaType.APPLICATION_JSON)
        ).andDo(MockMvcResultPrinter.print());
    }

    /**
     * Asserts the presence or absence of the deployment base link in the given MVC result.
     *
     * @param mvcResult   the result of the MVC call containing the response to check
     * @param shouldExist true if the deployment base link is expected to be present, false otherwise
     * @throws Exception if an error occurs while parsing the response
     */
    protected void assertDeploymentBaseLinkPresence(MvcResult mvcResult, boolean shouldExist) throws Exception {
        boolean hasLink = objectMapper.readTree(mvcResult.getResponse().getContentAsString())
                .path("_links").path("deploymentBase").has("href");
        if (shouldExist) {
            assertTrue(hasLink);
        } else {
            assertFalse(hasLink);
        }
    }

    /**
     * Creates a new rollout with the specified parameters, associates a software module with it,
     * and returns the created {@link JpaRollout} entity.
     *
     * @param rolloutName     the name of the rollout to create
     * @param rolloutEndDate  the end date of the rollout (as epoch seconds)
     * @param swModuleId      the ID of the software module to associate
     * @param versionResponse the version response containing the version ID to associate
     * @return the created {@link JpaRollout} entity
     * @throws Exception if an error occurs during creation or association
     */
    protected JpaRollout createAndSetupRollout(String rolloutName, long rolloutEndDate, long swModuleId,
                                               MgmtAddVersionResponse versionResponse) throws Exception {
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        JpaRollout rollout = (JpaRollout) rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout, rolloutName + " should be created");
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        return rollout;
    }


    /**
     * Creates and uploads all required support packages for a given rollout, updating their file statuses to
     * {@link FileTransferStatus#CDN_UPLOAD_SUCCESSFUL} as appropriate.
     *
     * <p>This method creates three types of support packages:
     * <ul>
     *   <li>WHATS_NEW (RSP) - not associated with any ECU node or controller IDs</li>
     *   <li>ADA_CERTIFICATE (ESP) - associated with the provided ECU node and controller IDs</li>
     *   <li>ADA_LICENSE (ESP) - associated with the provided ECU node and controller IDs</li>
     * </ul>
     * After creation, it updates the file status of each package to indicate successful CDN upload.
     *
     * @param rolloutId         the ID of the rollout to associate the support packages with
     * @param supportPackageUrl the URL where the support package can be accessed
     * @param ecuNodeId         the ECU node ID to associate with the ESP support packages
     * @param controllerIds     list of controller IDs to associate with the ESP support packages
     * @throws Exception if an error occurs during creation or status update
     */
    protected void createAndUploadSupportPackages(Long rolloutId, String supportPackageUrl, String ecuNodeId, List<String> controllerIds) throws Exception {
        MgmtSupportPackage whatsNewRsp = createSupportPackage(rolloutId, supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        MgmtSupportPackage adaCertificateEsp = createSupportPackage(rolloutId, supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        MgmtSupportPackage adaLicenseEsp = createSupportPackage(rolloutId, supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);

        updateFileStatus(RSP, whatsNewRsp.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, adaCertificateEsp.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, adaLicenseEsp.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
    }

    /**
     * Sets up the necessary certificates and keys for DD generation.
     * This method configures the signing certificate paths, private keys, and other related settings
     * required for generating deployment descriptors.
     * This sets up a mocked configuration for signing certificates and private keys to not attempt
     * to connect to s3 or ssm.
     * <p>
     * Note: add this method to @BeforeEach method in the test class to ensure that the certificates and keys are set up for DD generation.
     *
     * @throws IOException if an error occurs while setting up the certificates and keys
     */
    protected void setupCertificatesAndKeysForDDGeneration() throws IOException {

        SigningCertificateConfiguration config = JpaSigningCertificateConfiguration.builder()
                .ddCertificatePath("test-bucket/test_cert.pem")
                .ddPrivateKeyPath("/test/dd-key")
                .espCertificatePath("test-bucket/test_cert.pem")
                .espPrivateKeyPath("/test/dd-key")
                .rspCertificatePath("test-bucket/test_cert.pem")
                .rspPrivateKeyPath("/test/dd-key")
                .intermediateCACertificatePath("test-bucket/test_cert.pem")
                .pki("TEST_PKI_01")
                .ecuIdIssuer(TEST_ECU_ID_ISSUER_01)
                .build();
        when(pkiManagement.getSigningCertificateConfiguration(anyString())).thenReturn(config);
        when(ssmClient.getParameter(any(GetParameterRequest.class)))
                .thenReturn(GetParameterResponse.builder().parameter(p -> p.value(TEST_DD_PRIVATE_KEY_01)).build());
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenAnswer(invocation -> {
                    InputStream certStream = new ByteArrayInputStream(TEST_DD_SERVER_CERTIFICATE_01.getBytes(StandardCharsets.UTF_8));
                    return new ResponseInputStream<>(
                            GetObjectResponse.builder().build(),
                            new BufferedInputStream(certStream)
                    );
                });
    }


    /**
     * Validates the deployment signature in the inspect DD response body.
     * <p>
     * This method ensures that the deployment signature contains a valid SHA-256 hash
     * of the base64-encoded deployment description. The validation process includes:
     * <ul>
     *   <li>Extracting the deployment signature and description from the response.</li>
     *   <li>Computing the SHA-256 hash of the base64-encoded deployment description.</li>
     *   <li>Decoding the signature to retrieve the expected SHA-256 hash.</li>
     *   <li>Comparing the computed hash with the expected hash from the signature and asserting equality.</li>
     * </ul>
     *
     * @param result the {@link MvcResult} containing the deployment descriptor response
     * @throws Exception if parsing, decoding, or validation fails
     */
    private void validateDeploymentSignature(MvcResult result) throws Exception {

        String inspectDDResponse = result.getResponse().getContentAsString();
        String deploymentSignature = objectMapper.readTree(inspectDDResponse)
                .path("deploymentSignature").asText();
        String deploymentDescriptionBase64 = objectMapper.readTree(inspectDDResponse)
                .path("deploymentDescription").asText();
        String deploymentDescription = decodeJsonFromBase64(deploymentDescriptionBase64);

        String jwtSignaturePayload = deploymentSignature.split("\\.")[1];

// Compute SHA-256 hash of the base64-encoded deploymentDescription
        String base64Description = Base64.getEncoder()
                .encodeToString(deploymentDescription.getBytes(StandardCharsets.UTF_8));
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] sha256Hash = digest.digest(base64Description.getBytes(StandardCharsets.UTF_8));
        String expectedBase64 = new String(Base64.getEncoder().encode(sha256Hash), StandardCharsets.UTF_8);

// Decode the second part of the signature and extract the sha256 field
        String decodedSignatureJson = new String(Base64.getUrlDecoder()
                .decode(jwtSignaturePayload), StandardCharsets.UTF_8);
        String actualBase64 = objectMapper.readTree(decodedSignatureJson).path("sha256").asText();
        assertEquals(actualBase64, expectedBase64);

        long actualExpiryTime = objectMapper.readTree(decodedSignatureJson).path("exp").asLong();
        SigningCertificateConfiguration signingCertificateConfiguration = pkiManagement.getSigningCertificateConfiguration("TestEcuIdIssuer");
        Date signingCertificateExpirationDate = ddiSignatureService.generateSignature(base64Description, DdiSignatureType.DD, signingCertificateConfiguration, null)
                .getSigningCertificateExpirationDate();
        long expectedExpiryTime = signingCertificateExpirationDate.toInstant().getEpochSecond();
        assertEquals(expectedExpiryTime, actualExpiryTime, "JWT expiry time should match signing certificate expiration time");
    }

    /**
     * Decodes a Base64-encoded string into its original JSON string representation.
     * <p>
     * This method takes a Base64-encoded string, decodes it into bytes using UTF-8,
     * and returns the resulting JSON string. It is configured to ignore unknown
     * properties during potential JSON deserialization.
     * </p>
     *
     * @param base64DD the Base64-encoded JSON string to decode
     * @return the decoded JSON string
     * @throws IllegalArgumentException if the input string is not valid Base64
     */
    public static String decodeJsonFromBase64(String base64DD) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        // Decode Base64 → JSON
        byte[] decodedBytes = Base64.getDecoder().decode(base64DD);
        return new String(decodedBytes, StandardCharsets.UTF_8);
    }

    protected MgmtRetryFullRolloutRequestBody retryFullRolloutCreateRequestBody(String description, MgmtRolloutStartType startType, Long startDate , Long endDate) {
        MgmtRetryFullRolloutRequestBody retryRequest = new MgmtRetryFullRolloutRequestBody();
        retryRequest.setDescription(description);
        retryRequest.setStartType(startType);
        retryRequest.setStartDate(startDate);
        retryRequest.setEndDate(endDate);
        return retryRequest;
    }

    protected void sendFeedbackForActiveActions(Long rolloutId, String json) throws Exception {
        for (JpaAction jpaAction : actionRepository.findActionByRolloutIdAndActive(rolloutId, true)) {
            mvc.perform(post(ALL_FEEDBACK, jpaAction.getTarget().getControllerId(), jpaAction.getId())
                            .content(json)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    protected void activateRetryRolloutConfig() throws Exception {
        String bodyActivate = new JSONObject().put("value", true).toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    protected void triggerFullRetryRollout(Rollout rollout) throws Exception {
        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody(
                "Retry operation for Full Rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);
        String jsonObject = objectMapper.writeValueAsString(requestBody);
        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(jsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    protected void assertAllGroupsInStatus(Long rolloutId, RolloutGroupStatus expectedStatus) {
        List<String> statuses = rolloutGroupRepository.findByRolloutId(rolloutId)
                .stream()
                .map(group -> group.getStatus().name())
                .toList();
        assertTrue(statuses.stream().allMatch(expectedStatus.name()::equals),
                "Not all groups are in " + expectedStatus + " status: " + statuses);
    }
}
