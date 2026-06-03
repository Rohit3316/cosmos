/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutCondition;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroup;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.cosmos.models.mgmt.vehicle.dto.EcuModelAssignmentRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RepositoryApplicationConfiguration;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import  org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.AbstractRestIntegrationTest;
import org.eclipse.hawkbit.rest.RestConfiguration;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.hamcrest.CoreMatchers;
import jakarta.validation.constraints.NotNull;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mock;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.cosmos.models.mgmt.MgmtRestConstants.DEPLOYMENT_LOG_V1_REQUEST_MAPPING;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.MAX_PACKAGE_SIZE_IN_BYTES;
import static org.eclipse.hawkbit.mgmt.rest.resource.AbstractManagementRolloutApiIntegrationTest.MAX_UPDATE_TIME;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ContextConfiguration(classes = {MgmtApiConfiguration.class, RestConfiguration.class,
        RepositoryApplicationConfiguration.class, TestConfiguration.class, TestChannelBinderConfiguration.class})
@TestPropertySource(locations = "classpath:/mgmt-test.properties")
public abstract class AbstractManagementApiIntegrationTest extends AbstractRestIntegrationTest {

    public static final ObjectMapper objectMapper = new ObjectMapper();
    public static final int TENANT_ID = 1;
    public static final String RSP = "rsp";
    public static final String ESP = "esp";
    public static final String ROLLOUT_2 = "rollout2";
    public static final String ROLLOUT_1 = "rollout1";
    protected static final String TEST_ECU_NODE_ADDRESS = "30 A0";
    protected static final String SESSION_ID_HEADER = "536ffe3d6jh6";
    protected static final String TARGETS = "targets";
    protected static final String ECU_MODELS = "ecuModels";
    protected static final String JSON_PATH_LINKS_SOFTWARES_HREF = "$._links.softwares.href";
    protected static final String JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF = "$._links.supportPackages.href";
    protected static final String JSON_PATH_LINKS_FREEZE_HREF = "$._links.freeze.href";
    protected static final String SOFTWARES_URL = "/softwares";
    protected static final String SUPPORT_PACKAGES_URL = "/support-packages";
    protected static final String FREEZE_URL = "/action/freeze";
    protected static final String CANCEL_URL = "/cancel";
    protected static final String JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF = "$._links.triggerNextGroup.href";
    protected static final String JSON_PATH_LINKS_DELETE_HREF = "$._links.delete.href";
    protected static final String JSON_PATH_LINKS_CANCEL_HREF = "$._links.cancel.href";
    protected static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String REG_EX_ID = ".[?(@.id==";
    private static final String REG_EX_CONTROLLER_ID = ".[?(@.controllerId=='";
    private static final String REG_EX_CONTENT_ID = "$.content.[?(@.id==";
    private static final String REG_EX_ID_PATH_FOR_DATE = "$.[?(@.id==";
    private static final String REG_EX_CONTROLLER_ID_PATH_FOR_DATE = "$.[?(@.controllerId=='";
    private static final String DESCRIPTION = "description";
    private static final String REG_EX_ENTITY_ID = "$.[?(@.id=='";
    private static final String JSON_PATH_STATUS = "$.status";
    private static final String JSON_PATH_NAME = "$.name";
    private static final String REQUEST_BODY = "requestBody";
    private static final String HREF_ROLLOUT_PREFIX = "http://localhost/management/v1/tenants/1/rollouts/";
    private static final String TARGET = "target";
    protected static final String FOTA = "FOTA";
    protected static final String AOTA = "AOTA";
    private static final String DRAFT = "draft";
    private static final String JSON_PATH_LINKS_SELF_HREF = "$._links.self.href";
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
    private static final String JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED = "$.deploymentMetadata.downgradeAllowed";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA = "$.deploymentMetadata.requiredMedia";
    private static final String JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME = "$.deploymentMetadata.estimatedUpdateTime";
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
    private static final String ROLLOUT = "rollout";
    private static final String ARTIFACTS_ENDPOINT = MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING;
    private static final String FILE_NAME = "filename";
    private static final String FILE_TYPE = "fileType";
    private static final String SIGNATURE_EXPIRY_DATE = "signatureExpiryDate";
    private static final String DELTA = "DELTA";
    protected static ClientAndServer mockServer;
    @Autowired
    protected RspRepository rspRepository;
    @Autowired
    protected EspRepository espRepository;
    @Autowired
    protected ActionRepository actionRepository;
    @Value("${initConfig.softwareModuleFormat}")
    String softwareModuleFormats;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    SoftwareModuleFormatManagement softwareModuleFormatRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private ArtifactsRepository artifactsRepository;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;

    protected static ObjectMapper getMapper() {
        return objectMapper;
    }

    protected static ResultMatcher applyBaseEntityMatcherOnArrayResult(final BaseEntity entity,
                                                                       final String arrayElement) {
        return mvcResult -> {
            jsonPath("$." + arrayElement + REG_EX_ID + entity.getId() + ")].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_ID + entity.getId() + ")].createdAt",
                    contains((int) entity.getCreatedAt())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_ID + entity.getId() + ")].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_ID + entity.getId() + ")].lastModifiedAt",
                    contains((int) entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTargetEntityMatcherOnArrayResult(final Target entity, final String arrayElement) {
        return mvcResult -> {
            jsonPath("$." + arrayElement + REG_EX_CONTROLLER_ID + entity.getControllerId() + "')].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_CONTROLLER_ID + entity.getControllerId() + "')].createdAt",
                    contains((int) entity.getCreatedAt())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_CONTROLLER_ID + entity.getControllerId() + "')].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("$." + arrayElement + REG_EX_CONTROLLER_ID + entity.getControllerId() + "')].lastModifiedAt",
                    contains((int) entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyBaseEntityMatcherOnPagedResult(final BaseEntity entity) {
        return applyBaseEntityMatcherOnArrayResult(entity, "content");
    }

    protected static ResultMatcher applyNamedEntityMatcherOnPagedResult(final NamedEntity entity) {
        return mvcResult -> {
            applyBaseEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath(REG_EX_CONTENT_ID + entity.getId() + ")].name", contains(entity.getName())).match(mvcResult);
            jsonPath(REG_EX_CONTENT_ID + entity.getId() + ")].description", contains(entity.getDescription()))
                    .match(mvcResult);
        };

    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnPagedResult(final NamedVersionedEntity entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath(REG_EX_CONTENT_ID + entity.getId() + ")].version", contains(entity.getVersion()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnPagedResult(final Tag entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity).match(mvcResult);
            jsonPath(REG_EX_CONTENT_ID + entity.getId() + ")].colour", contains(entity.getColour()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnPagedResult(final BaseEntity entity, final String link) {
        return mvcResult ->
                jsonPath(REG_EX_CONTENT_ID + entity.getId() + ")]._links.self.href", contains(link)).match(mvcResult);

    }

    protected static ResultMatcher applyBaseEntityMatcherOnArrayResult(final BaseEntity entity) {
        return mvcResult -> {
            jsonPath(REG_EX_ID_PATH_FOR_DATE + entity.getId() + ")].createdBy", contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath(REG_EX_ID_PATH_FOR_DATE + entity.getId() + ")].createdAt", contains((int) entity.getCreatedAt())).match(mvcResult);
            jsonPath(REG_EX_ID_PATH_FOR_DATE + entity.getId() + ")].lastModifiedBy", contains(entity.getLastModifiedBy()))
                    .match(mvcResult);
            jsonPath(REG_EX_ID_PATH_FOR_DATE + entity.getId() + ")].lastModifiedAt", contains((int) entity.getLastModifiedAt()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyTargetEntityMatcherOnArrayResult(final Target entity) {
        return mvcResult -> {
            jsonPath(REG_EX_CONTROLLER_ID_PATH_FOR_DATE + entity.getControllerId() + "')].createdBy",
                    contains(entity.getCreatedBy())).match(mvcResult);
            jsonPath(REG_EX_CONTROLLER_ID_PATH_FOR_DATE + entity.getControllerId() + "')].createdAt",
                    contains((int) entity.getCreatedAt())).match(mvcResult);
            jsonPath(REG_EX_CONTROLLER_ID_PATH_FOR_DATE + entity.getControllerId() + "')].lastModifiedBy",
                    contains(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath(REG_EX_CONTROLLER_ID_PATH_FOR_DATE + entity.getControllerId() + "')].lastModifiedAt",
                    contains((int) entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedEntityMatcherOnArrayResult(final NamedEntity entity) {
        return mvcResult -> {
            applyBaseEntityMatcherOnPagedResult(entity);

            jsonPath(REG_EX_ENTITY_ID + entity.getId() + "')].name", contains(entity.getName())).match(mvcResult);
            jsonPath(REG_EX_ENTITY_ID + entity.getId() + "')].description", contains(entity.getDescription()))
                    .match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnArrayResult(final NamedVersionedEntity entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity);

            jsonPath(REG_EX_ENTITY_ID + entity.getId() + "')].version", contains(entity.getVersion())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnArrayResult(final Tag entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnPagedResult(entity);

            jsonPath(REG_EX_ENTITY_ID + entity.getId() + "')].colour", contains(entity.getColour())).match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnArrayResult(final BaseEntity entity, final String link) {
        return mvcResult ->
                jsonPath(REG_EX_ENTITY_ID + entity.getId() + "')]._links.self.href", contains(link)).match(mvcResult);
    }

    protected static ResultMatcher applyBaseEntityMatcherOnSingleResult(final BaseEntity entity) {
        return mvcResult -> {
            jsonPath("createdBy", equalTo(entity.getCreatedBy())).match(mvcResult);
            jsonPath("createdAt", equalTo(entity.getCreatedAt())).match(mvcResult);
            jsonPath("lastModifiedBy", equalTo(entity.getLastModifiedBy())).match(mvcResult);
            jsonPath("lastModifiedAt", equalTo(entity.getLastModifiedAt())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedEntityMatcherOnSingleResult(final NamedEntity entity) {
        return mvcResult -> {
            applyBaseEntityMatcherOnSingleResult(entity);

            jsonPath("name", equalTo(entity.getName())).match(mvcResult);
            jsonPath("description", equalTo(entity.getDescription())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyNamedVersionedEntityMatcherOnSingleResult(final NamedVersionedEntity entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnSingleResult(entity);

            jsonPath("version", equalTo(entity.getVersion())).match(mvcResult);
        };
    }

    protected static ResultMatcher applyTagMatcherOnSingleResult(final Tag entity) {
        return mvcResult -> {
            applyNamedEntityMatcherOnSingleResult(entity);

            jsonPath("colour", equalTo(entity.getColour())).match(mvcResult);
        };
    }

    protected static ResultMatcher applySelfLinkMatcherOnSingleResult(final String link) {
        return mvcResult ->
                jsonPath("_links.self.href", equalTo(link)).match(mvcResult);
    }

    protected static JSONObject getAssignmentObject(final Object id, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        final JSONObject obj = new JSONObject();
        try {
            obj.put("id", id);
            obj.put("userAcceptanceRequired", userAcceptanceRequired.getName());
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        return obj;
    }

    protected static JSONObject getAssignmentObject(final Object id, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final int weight) {
        try {
            return getAssignmentObject(id, userAcceptanceRequired).put("weight", weight);
        } catch (final JSONException e) {
            e.printStackTrace();
        }
        return new JSONObject();
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

    protected static MockHttpServletRequestBuilder createArtifactRequest(MockMultipartFile file, String filename, String fileType, String description, String sha256, String signatureExpiryDate) {
        return MockMvcRequestBuilders.multipart(ARTIFACTS_ENDPOINT, TENANT_ID)
                .file(file)
                .param(FILE_NAME, filename)
                .param(FILE_TYPE, fileType)
                .param(DESCRIPTION, description)
                .param("sha256", sha256)
                .param(SIGNATURE_EXPIRY_DATE, signatureExpiryDate);
    }

    protected static MgmtArtifactsRequest createArtifactRequestUsingFileUrl(String sha256, String fileName, FileType fileType) {
        MgmtArtifactsRequest request = new MgmtArtifactsRequest();
        request.setFileURL("https://en.xiaoai.me/pages/text-empty-pdf-generator");
        request.setFilename(fileName);
        request.setFileType(fileType.name());
        request.setDescription(DESCRIPTION);
        request.setSignatureExpiryDate(1896514417L);
        request.setSha256(sha256);
        return request;
    }

    protected String deploymentLogDownloadLink(final String controllerId, final Long actionId, final Long deploymentLogId) {
        return "http://localhost" + DEPLOYMENT_LOG_V1_REQUEST_MAPPING.replace("{controllerId}", String.valueOf(controllerId)).replace("{actionId}", String.valueOf(actionId)) + "/"
                + deploymentLogId + "/download";
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

    protected void invokeAssociateEcuModelToVehicleModelApi(Long vehicleModelId, Long ecuModelId) throws Exception {
        var requestBody = testdataFactory.getAssociateEcuModelAndVehicleRequestBody(List.of(ecuModelId));
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, vehicleModelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk());
    }

    protected List<MgmtTarget> invokeCreateTargetApi(Long vehicleModelId, int totalTargets) throws Exception {
        String responseAsString = invokeCreateTargetApiAndReturnResponseAsString(vehicleModelId, totalTargets);
        return objectMapper.readValue(responseAsString, new TypeReference<>() {
        });
    }

    protected String invokeCreateTargetApiAndReturnResponseAsString(Long vehicleModelId, int totalTargets) throws Exception {
        List<MgmtTargetRequestBody> requestBody = testdataFactory.getCreateTargetRequestBody(vehicleModelId, totalTargets);
        MvcResult result = mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().is2xxSuccessful())
                .andReturn();

        return result.getResponse().getContentAsString();
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

    protected ResultActions invokeCreateSupportPackageWithFileApi(Long tenantId, Long rolloutId, MockMultipartFile file, String filename, String fileType,
                                                                  String fileVersion, List<String> controllerIds, String sha256, String ecuNodeAddress, String fileContentDescription,
                                                                  String fileInfoUrl, String fileMetadata) throws Exception {

        return mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS, tenantId, rolloutId)
                .file(file)
                .param("fileName", filename)
                .param("fileType", fileType)
                .param("fileVersion", fileVersion)
                .param("controllerIds", controllerIds == null || controllerIds.isEmpty() ? "" : String.valueOf(controllerIds))
                .param("sha256", sha256)
                .param("ecuNodeAddress", ecuNodeAddress)
                .param("fileContentDescription", fileContentDescription)
                .param("fileInfoUrl", fileInfoUrl)
                .param("fileMetadata", fileMetadata));
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
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        return mvc.perform(post(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, TENANT_ID, rolloutId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody));
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

    protected void invokeRolloutSoftwareAssociateApi(List<MgmtSoftwareModuleRequest> swAssociationRequestBody, long rolloutId) throws Exception {
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(swAssociationRequestBody)))
                .andReturn()
                .getResponse()
                .getContentAsString();

    }

    protected MgmtAddVersionResponse invokeCreateSoftwareVersionApi(long softwareModuleId, MgmtAddVersionRequestBody versionRequestBody) throws Exception {
        var content = mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, softwareModuleId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(versionRequestBody))).andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(content, MgmtAddVersionResponse.class);
    }

    protected void invokeCreateRolloutApi(final String name, final Long endDate) throws Exception {
        invokeCreateRolloutApi(name, endDate, MgmtRolloutStartType.MANUAL, FOTA, null, null);

    }

    protected void invokeRolloutFreezeApi(long rolloutId) throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)).andReturn().getResponse().getContentAsString();

    }

    protected void invokeRolloutStartApi(long rolloutId) throws Exception {
        var result = mvc.perform(put(MgmtRestConstants.ROLLOUT_START_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)).andReturn();
        log.info("result:{}", result.getResponse().getContentAsString());
    }

    void invokeCreateRolloutApi(final String name, final Long endDate, final MgmtRolloutStartType startType, String type, String updateAction, List<String> updateActionUninstallVersion) throws Exception {
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        Long startDate = startType.equals(MgmtRolloutStartType.SCHEDULED) ? Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond() : null;
        final String rollout = JsonBuilder.rollout(name, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), startType.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, endDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, MAX_PACKAGE_SIZE_IN_BYTES, type, updateAction, updateActionUninstallVersion);

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
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES, CoreMatchers.equalTo(10000)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))));

    }

    protected void invokeAddDeviceApi(List<String> controllerIds, long rolloutId) throws Exception {
        invokeAddDeviceApi(controllerIds, rolloutId, 0);

    }

    protected MgmtAddDeviceDetailsResponse invokeAddDeviceApi(List<String> controllerIds, long rolloutId, int totalGroups) throws Exception {
        Path filePath = generateTargetDevicesFile(controllerIds);
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        if (totalGroups > 0) {
            List<MgmtRolloutGroup> groupsDetails = IntStream.range(0, totalGroups)
                    .mapToObj(i -> {
                        var group = new MgmtRolloutGroup();
                        group.setName(RandomStringUtils.randomAlphanumeric(5));
                        group.setTargetPercentage((float) (100 / totalGroups));
                        group.setErrorCondition(new MgmtRolloutCondition(MgmtRolloutCondition.Condition.THRESHOLD, "20"));
                        group.setSuccessCondition(new MgmtRolloutCondition(MgmtRolloutCondition.Condition.THRESHOLD, "50"));
                        return group;
                    })
                    .collect(Collectors.toList());
            // Convert the list to JSON or any required format for the MockMultipartFile
            ObjectMapper objectMapper = new ObjectMapper();
            String groupsJsonArray = objectMapper.writeValueAsString(groupsDetails);
            var mvcresult = mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rolloutId)
                    .file(file)
                    .param("groups", groupsJsonArray)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON));

            // Verifying the links in the Add Device Details API response
            mvcresult.andExpect(jsonPath("$._links").doesNotExist());
            var content = mvcresult.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(content, MgmtAddDeviceDetailsResponse.class);
        } else {
            var mvcresult = mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rolloutId)
                    .file(file)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON));

            // Verifying the links in the Add Device Details API response
            mvcresult.andExpect(jsonPath("$._links").doesNotExist());
            var content = mvcresult.andReturn().getResponse().getContentAsString();
            return objectMapper.readValue(content, MgmtAddDeviceDetailsResponse.class);
        }

    }

    protected MgmtArtifacts invokeCreateArtifactViaUrlApi(String fileUrl) throws Exception {
        MgmtArtifactsRequest mgmtArtifactsRequest = testdataFactory.getRandomValidCreateArtifactsRequestBody(generateSha256Hash(RandomStringUtils.randomAlphanumeric(4)),
                fileUrl,
                "FULL",
                RandomStringUtils.randomAlphanumeric(5));

        String requestJson = objectMapper.writeValueAsString(mgmtArtifactsRequest);

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("MOCK_MESSAGE_ID")
                .build();

        // Mock the behavior of publishMessage to return a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        var mvcResult = mvc.perform(post(MgmtRestConstants.ARTIFACTS_V1_REQUEST_MAPPING_FILEURL, TENANT_ID).content(requestJson)
                .contentType(MediaType.APPLICATION_JSON));
        var content = mvcResult.andReturn().getResponse().getContentAsString();
        log.info("content:{}", content);

        mvcResult.andExpect(status().isCreated());
        return objectMapper.readValue(content, MgmtArtifacts.class);
    }

    protected Path generateTargetDevicesFile(List<String> controllerIds) throws IOException {
        Path filePath = Files.createTempFile("controllerIds", ".csv");
        Files.writeString(filePath, String.join("\n", controllerIds), StandardOpenOption.WRITE);
        return filePath;
    }

    protected @NotNull Rollout
    createRolloutWithDependencies(String rolloutName, DistributionSet ds, List<Target> targets) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        return associateRolloutWithDependencies(ds, targets, rollout, true);
    }

    protected @NotNull Rollout
    createRolloutWithDependencies(String rolloutName, DistributionSet ds, List<Target> targets, Boolean isAssociateArtifactSoftwareModule) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        return associateRolloutWithDependencies(ds, targets, rollout, isAssociateArtifactSoftwareModule);
    }

    protected Rollout associateRolloutWithDependencies(DistributionSet ds, List<Target> targets, Rollout rollout, Boolean isAssociateArtifactWithSoftwareModule) {
        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);
        if (isAssociateArtifactWithSoftwareModule) {
            // Create artifact and associate with software module
            associateArtifactWithSoftwareModule(softwareModule, version);
        }
        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroup(targets, rollout);
        return rollout;
    }

    protected void associateRolloutWithDistributionSet(DistributionSet ds, JpaRollout rollout) {
        rollout.setDistributionSet(ds);
        rolloutRepository.save(rollout);
    }

    /**
     * Creates an artifact and associates it with the given software module and version.
     *
     * @param softwareModule the software module to associate the artifact with
     * @param version        the version to associate the artifact with
     */
    protected void associateArtifactWithSoftwareModule(SoftwareModule softwareModule, Version version) {
        long expiryEpochSeconds = Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond();
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate("file1", FileType.FULL, DESCRIPTION, "123", "SHA_256", expiryEpochSeconds, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) version)
                .targetVersion((JpaVersion) version)
                .build();
        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));
        //Here we are mocking Artifacts to CDN_UPLOAD_SUCCESSFUL status.This can be removed once Lambda is Operational.
        Optional<Artifacts> artifacts = artifactsRepository.getArtifactsById(artifact.getId());
        if (artifacts.isPresent()) {
            JpaArtifacts jpaArtifacts = (JpaArtifacts) artifacts.get();
            jpaArtifacts.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
            artifactsRepository.save(jpaArtifacts);
            //Sleep is added to avoid JPAOptimisticlockException
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                log.error("Thread sleep interrupted: {}", e.getMessage(), e);

            }
        }


    }

    /**
     * Builds a software module request for the given software module and version IDs.
     *
     * @param softwareModuleId the ID of the software module
     * @param versionId        the ID of the version
     * @return the created MgmtSoftwareModuleRequest object
     */
    protected MgmtSoftwareModuleRequest buildSoftwareModuleRequest(Long softwareModuleId, Long versionId) {
        return MgmtSoftwareModuleRequest.builder()
                .moduleId(softwareModuleId)
                .softwareVersionTargetId(versionId)
                .build();
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

    protected void updateRolloutGroupsToReadyState(Long rolloutId) {
        rolloutGroupManagement.findByRollout(PAGE, rolloutId).forEach(rolloutGroup -> {
            JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroup;
            jpaRolloutGroup.setStatus(RolloutGroupStatus.READY);
            rolloutGroupRepository.save(jpaRolloutGroup);
        });
    }

    /**
     * Creates a new rollout and its dependencies, including software modules, versions, and associations.
     *
     * @param rolloutName       the name of the rollout to create
     * @param ds                the distribution set to associate with the rollout
     * @param targets           the list of targets to associate with the rollout
     * @param groupsDetailsJson the json string containing the rollout groups details
     * @return the created Rollout object
     */
    protected @NotNull Rollout createRolloutWithDependenciesAndGroups(String rolloutName, DistributionSet ds, List<Target> targets, String groupsDetailsJson, Boolean isAssociateArtifactWithSoftwareModule) {
        // Create new rollout

        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        if (isAssociateArtifactWithSoftwareModule) {
            // Create artifact and associate with software module
            associateArtifactWithSoftwareModule(softwareModule, version);
        }

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroups(targets, rollout, groupsDetailsJson);
        return rollout;
    }

    /**
     * Creates a rollout group and associates it with the given rollout.
     *
     * @param targets           the list of targets to associate with the rollout group
     * @param rollout           the rollout to associate the group with
     * @param groupsDetailsJson the json string containing the rollout groups details
     */

    protected void createRolloutGroups(List<Target> targets, Rollout rollout, String groupsDetailsJson) {
        rolloutManagement.get(rollout.getId()).ifPresent(savedRollout ->
                rolloutManagement.addDeviceDetails(savedRollout, targets.stream().map(Target::getControllerId).toList(), groupsDetailsJson)
        );
    }

    protected Rollout prepareSupportPackageTestData(
            String targetPrefix,
            String rolloutPrefix,
            String rolloutName,
            String distributionSetName,
            String ecuNodeAddress,
            MgmtSupportPackageFileType fileType
    ) throws Exception {
        TestdataFactory.SupportPackageTestData customTestData = new TestdataFactory.SupportPackageTestData();
        customTestData.setTargetPrefix(targetPrefix);
        customTestData.setRolloutPrefix(rolloutPrefix);
        customTestData.setRolloutName(rolloutName);
        customTestData.setDistributionSetName(distributionSetName);
        customTestData.setFileType(fileType);
        customTestData.setEcuNodeAddress(ecuNodeAddress);


        var testData = prepareTestDataForCreatingSupportPackageTest(customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        return rollout;
    }

    protected Map<String, Object> prepareTestDataForCreatingSupportPackageTest(String rolloutName, TestdataFactory.SupportPackageTestData testData) {
        // Create targets once and reuse them
        List<Target> targets = testdataFactory.createTargets(testData.getAmountTargets(), testData.getTargetPrefix(), testData.getRolloutPrefix());

        List<EcuModel> ecuModels = new ArrayList<>();
        //Create ECU Model
        if (!testData.getEcuNodeAddress().isEmpty()) {
            ecuModels = testdataFactory.addNewEcuModels(createEcuModel(testData.getEcuNodeAddress()));
        }

        // Create rollout including the created targets with the provided prefix
        Rollout rollout = createRolloutWithDependencies(rolloutName, testdataFactory.createDistributionSet(), targets);

        // Create request body using the provided parameters
        List<String> vins = targets.stream().map(Target::getControllerId).toList();

        MgmtFileUrlSupportPackageCreateRequest requestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(testData.getFileUrl())
                .fileName(testData.getTestFileName())
                .fileType(testData.getFileType())
                .sha256(testData.getSha256())
                .fileVersion(testData.getVersion())
                .controllerIds(vins)
                .ecuNodeAddress(testData.getEcuNodeAddress())
                .fileContentDescription("This file contains the latest firmware update.")
                .fileInfoUrl("https://example.com/release-notes")
                .fileMetadata(testData.getFileMetadata())
                .build();


        return Map.of(rolloutName, rollout, "requestBody", requestBody, "targets", targets, "ecuModels", ecuModels);
    }

    protected Map<String, Object> prepareTestDataForCreatingSupportPackageTest(TestdataFactory.SupportPackageTestData testData) throws JSONException {
        return prepareTestDataForCreatingSupportPackageTest(ROLLOUT, testData);

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

    protected void assignEcuModelsToVehicle(List<Target> targets, List<EcuModel> ecuModels) throws Exception {
        for (Target target : targets) {
            Long vehicleModelId = target.getVehicleModelId();
            mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING,
                            vehicleModelId).content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                            .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk());
        }
    }

    private List<EcuModelAssignmentRequest> getEcuModelAssignmentRequest(List<EcuModel> ecuModels) {
        EcuModelAssignmentRequest ecuModelAssignment = new EcuModelAssignmentRequest();
        ecuModelAssignment.setEcuModelId(ecuModels.get(0).getId());
        List<EcuModelAssignmentRequest> ecuModelAssignmentRequests = new ArrayList<>();
        ecuModelAssignmentRequests.add(ecuModelAssignment);
        return ecuModelAssignmentRequests;
    }

    /**
     * Invokes the API to retrieve a support package by its ID for a specific rollout and type.
     *
     * @param rolloutId The ID of the rollout associated with the support package.
     * @param packageId The ID of the support package to retrieve.
     * @param type      The type of the support package (e.g., ESP or RSP).
     * @return A {@link ResultActions} object containing the result of the API invocation.
     * @throws Exception If an error occurs during the API invocation.
     */
    protected ResultActions invokeGetSupportPackageByIdApi(Long rolloutId, Long packageId, String type) throws Exception {
        return mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_ID_V1_REQUEST_MAPPING_TENANT_TARGETS, TENANT_ID, rolloutId, type, packageId))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
    }

    /**
     * Invokes the API to retrieve all support packages for a given rollout with optional pagination parameters.
     *
     * @param rolloutId the ID of the rollout for which support packages are to be retrieved
     * @param offset    the starting point of the results (optional, can be null)
     * @param limit     the maximum number of results to return (optional, can be null)
     * @param rsqlParam the RSQL query parameter for filtering results (optional, can be null)
     * @param sortParam the sorting parameter for the results (optional, can be null)
     * @return the result of the API call as a {@link ResultActions} object
     * @throws Exception if an error occurs during the API invocation
     */
    protected ResultActions invokeGetAllSupportPackagesApi(long rolloutId, Object offset, Object limit, Object sortParam, Object rsqlParam) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS, TENANT_ID, rolloutId)
                .contentType(MediaType.APPLICATION_JSON);

        if (offset != null) {
            requestBuilder = requestBuilder.param("offset", String.valueOf(offset));
        }
        if (limit != null) {
            requestBuilder = requestBuilder.param("limit", String.valueOf(limit));
        }
        if (rsqlParam != null) {
            requestBuilder = requestBuilder.param("q", String.valueOf(rsqlParam));
        }
        if (sortParam != null) {
            requestBuilder = requestBuilder.param("sort", String.valueOf(sortParam));
        }

        return mvc.perform(requestBuilder);
    }

    /**
     * Invokes the API to retrieve targets associated with a specific rollout ID.
     *
     * @param rolloutId The ID of the rollout for which targets are to be retrieved.
     * @return A {@link ResultActions} object containing the result of the API invocation.
     * @throws Exception If an error occurs during the API invocation.
     */
    protected ResultActions invokeGetTargetsActionsByRolloutIdApi(Long rolloutId) throws Exception {
        return mvc.perform(get(MgmtRestConstants.ROLLOUT_TARGETS_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print());
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

}
