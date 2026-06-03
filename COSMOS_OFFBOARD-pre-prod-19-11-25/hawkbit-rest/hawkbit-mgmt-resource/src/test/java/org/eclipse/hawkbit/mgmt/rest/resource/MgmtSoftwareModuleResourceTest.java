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
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.ecu.dto.EcuModels;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignEcuModelRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtEcuModelMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtTargetResourceTest.JAKARTA_VALIDATION_VALIDATION_EXCEPTION;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MgmtSoftwareModuleResource} {@link RestController}.
 */
@Slf4j
@Feature("Component Tests - Management API")
@Story("Software Module Resource")
@TestPropertySource(properties = {"hawkbit.server.security.dos.maxArtifactSize=100000",
        "hawkbit.server.security.dos.maxArtifactStorage=500000"})
class MgmtSoftwareModuleResourceTest extends AbstractManagementApiIntegrationTest {

    public static final String SM_INSTALLER_TYPE_0 = "0";
    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_ECU_MODEL_NAME = "TestEcuModel01";

    private static final String TEST_ECU_NODE_ID = "HeX32458";

    private static final String TENANT_ID = "1";

    private static final String KNOWN_SW_NAME = "name1";
    private static final String VALUE = "value";
    private static final String KEY = "key";
    private static final String KNOWN_SW_VERSION = "version1";
    private static final String KNOWN_SW_DESCRIPTION = "description1";
    private static final String KNOWN_SW_VENDOR = "vendor1";
    private static final String SM_UPDATE_TESTER = "smUpdateTester";
    private static final String JSON_PATH_NAME = "$.name";
    private static final String JSON_PATH_DELETED = "$.deleted";
    private static final String BASE_SOFTWARE_MODULES_URL = "http://localhost/management/v1/tenants/1/softwaremodules";
    private static final String SEPARATOR = "/";
    private static final String QUOTA = "quota";
    private static final String QUOTA1 = "quota1";
    private static final String FORMAT_KEY = "theformat";
    private static final String CREATE_SOFTWARE_MODULE_URL = "/management/v1/tenants/{tenantId}/softwaremodules";
    private static final String FILE_1 = "file1";
    private static final String SOFTWARE_MODULE_DESCRIPTION = "Softwaremodule size is wrong";
    private static final String NAME = ")].name";
    private static final String NAME_VALUE = "name";
    private static final String NAME_SHOULD_NOT_BE_CHANGED = "nameShouldNotBeChanged";
    private static final String JSON_PATH_CONTENT_ID = "$.content.[?(@.id==";
    private static final String DESCRIPTION = ")].description";
    private static final String VENDOR = ")].vendor";
    private static final String TYPE = ")].type";
    private static final String CREATED_BY = ")].createdBy";
    private static final String UPLOAD_TESTER = "uploadTester";
    private static final String LINKS_SELF_HREF = ")]._links.self.href";
    private static final String GET_SOFTWAREMODULES_ID_VERSION = "/management/v1/tenants/1/softwaremodules/{swId}/version";
    private static final String SOFTWARE_MODULE_ID = "/{softwareModuleId}";

    private static final String KNOWN_TARGET_ID = "knownTargetId";
    private static final String CONTROLLER_ID = "knownTargetIdNew";
    private static final String ROLLOUT = "rollout";
    private static final String DESCRIPTION_STRING = "description";

    private static final String SOFTWARE_MODULE_URL = "/management/v1/tenants/{tenantId}/softwaremodules/{smId}";
    private static final String CREATED_AT = ")].createdAt";
    private static final String ARTIFACTS = ")].artifacts";
    private static final String JSON_PATH_TOTAL = "$.total";
    private static final String JSON_PATH_CONTENT = "$.content";
    private static final String KNOWN_VALUE = "knownValue";
    private static final String ETAG = "ETag";
    private static final String KNOWN_KEY = "knownKey";
    private static final String KNOWN_VALUE_1 = "knownValue1";
    private static final String VALUE_FOR_UPDATE = "valueForUpdate";
    private static final String NAME_3 = "name3";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_ECU_MODEL = "sp_ecu_model";
    private static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private ArtifactsRepository artifactsRepository;
    @MockBean
    private SnsAsyncClient snsAsyncClient;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;

    @Autowired
    JdbcTemplate jdbcTemplate;
    Random randomNumber = new SecureRandom();
    private List<EcuModel> savedEcuModel;

    private static ClientAndServer mockServer;

    @BeforeAll
    static void mockPublishRolloutStatus() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_ROLLOUT_STATUS_ENDPOINT)).
                respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_rollout");
    }

    private static byte[] randomBytes(final int len) {
        return RandomStringUtils.randomAlphanumeric(len).getBytes();
    }

    @BeforeEach
    public void assertPreparationOfRepo() throws Exception {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_SOFTWARE_ECU_MODEL, SP_ECU_MODEL, SP_ARTIFACT_SOFTWARE_MODULE,
                SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_VEHICLE_MODEL
        );
        assertThat(softwareModuleManagement.findAll(PAGE)).as("no softwaremodule should be founded").isEmpty();
        PublishResponse mockPublishResponse = PublishResponse.builder().messageId("mockMessageId").build();
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Tests the update of software module metadata. It is verfied that only the selected fields for the update are really updated and the modification values are filled (i.e. updated by and at).")
    @WithUser(principal = SM_UPDATE_TESTER, allSpPermissions = true)
    void updateSoftwareModuleOnlyDescriptionAndVendorNameUntouched() throws Exception {


        final String updateVendor = "newVendor1";
        final String updateDescription = "newDescription1";

        final SoftwareModule sm = softwareModuleManagement.create(entityFactory.softwareModule()
                .create()
                .type(osType)
                .name(KNOWN_SW_NAME)
                .version(KNOWN_SW_VERSION)
                .description(KNOWN_SW_DESCRIPTION)
                .vendor(KNOWN_SW_VENDOR)
                .format(format)
                .swInstallerType(swInstallerType));

        assertThat(sm.getName()).as("Wrong name of the software module").isEqualTo(KNOWN_SW_NAME);

        final String body = new JSONObject().put("vendor", updateVendor)
                .put(DESCRIPTION_STRING, updateDescription)
                .put(NAME_VALUE, NAME_SHOULD_NOT_BE_CHANGED)
                .toString();

        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        mvc.perform(put(SOFTWARE_MODULE_URL, TENANT_ID, sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.vendor", equalTo(updateVendor)))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(SM_UPDATE_TESTER)))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(sm.getLastModifiedAt()))))
                .andExpect(jsonPath("$.description", equalTo(updateDescription)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(KNOWN_SW_NAME)))
                .andReturn();

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(updatedSm.getName()).isEqualTo(KNOWN_SW_NAME);
        assertThat(updatedSm.getVendor()).isEqualTo(updateVendor);
        assertThat(updatedSm.getLastModifiedBy()).isEqualTo(SM_UPDATE_TESTER);
        assertThat(updatedSm.getDescription()).isEqualTo(updateDescription);

    }

    @Test
    @Description("Tests the update of software module metadata. It is verfied that only the selected fields for the update are really updated and the modification values are filled (i.e. updated by and at).")
    @WithUser(principal = SM_UPDATE_TESTER, allSpPermissions = true)
    void updateSoftwareModuleOnlyDescriptionAndVendorNameUntouchedWithScomoId() throws Exception {


        final String updateVendor = "newVendor1";
        final String updateDescription = "newDescription1";

        final SoftwareModule sm = softwareModuleManagement.create(entityFactory.softwareModule()
                .create()
                .type(osType)
                .name(KNOWN_SW_NAME)
                .version(KNOWN_SW_VERSION)
                .description(KNOWN_SW_DESCRIPTION)
                .vendor(KNOWN_SW_VENDOR)
                .format(format)
                .swInstallerType(swInstallerType));

        assertThat(sm.getName()).as("Wrong name of the software module").isEqualTo(KNOWN_SW_NAME);

        final String body = new JSONObject().put("vendor", updateVendor)
                .put(DESCRIPTION_STRING, updateDescription)
                .put(NAME_VALUE, NAME_SHOULD_NOT_BE_CHANGED)
                .toString();

        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofMillis(100))
                .pollInterval(10L, TimeUnit.MILLISECONDS)
                .until(() -> sm.getLastModifiedAt() > 0L && sm.getLastModifiedBy() != null);

        mvc.perform(put(MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.vendor", equalTo(updateVendor)))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(SM_UPDATE_TESTER)))
                .andExpect(jsonPath("$.lastModifiedAt", not(equalTo(sm.getLastModifiedAt()))))
                .andExpect(jsonPath("$.description", equalTo(updateDescription)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(KNOWN_SW_NAME)))
                .andReturn();

        final SoftwareModule updatedSm = softwareModuleManagement.get(sm.getId()).get();
        assertThat(updatedSm.getName()).isEqualTo(KNOWN_SW_NAME);
        assertThat(updatedSm.getVendor()).isEqualTo(updateVendor);
        assertThat(updatedSm.getLastModifiedBy()).isEqualTo(SM_UPDATE_TESTER);
        assertThat(updatedSm.getDescription()).isEqualTo(updateDescription);

    }

    @Test
    @Description("Tests the update of the deletion flag. It is verfied that the software module can't be marked as deleted through update operation.")
    @WithUser(principal = SM_UPDATE_TESTER, allSpPermissions = true)
    void updateSoftwareModuleDeletedFlag() throws Exception {


        final SoftwareModule sm = softwareModuleManagement.create(
                entityFactory.softwareModule().create().type(osType).name(KNOWN_SW_NAME).version(KNOWN_SW_VERSION)
                        .format(format).swInstallerType(swInstallerType));

        assertThat(sm.isDeleted()).as("Created software module should not be deleted").isFalse();

        final String body = new JSONObject().put("deleted", true).toString();

        // ensures that we are not to fast so that last modified is not set correctly
        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(1, TimeUnit.SECONDS)
                .until(() -> sm.getLastModifiedAt() > 0 && sm.getLastModifiedBy() != null);

        mvc.perform(put(SOFTWARE_MODULE_URL, TENANT_ID, sm.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(sm.getId().intValue())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(SM_UPDATE_TESTER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo((int) sm.getLastModifiedAt())))
                .andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));


        assertThat(sm.getLastModifiedBy()).isEqualTo(SM_UPDATE_TESTER);
        assertThat(sm.getLastModifiedAt()).isEqualTo(sm.getLastModifiedAt());
        assertThat(sm.isDeleted()).isFalse();
    }


    @Test
    @Description("Trying to create a SM from already marked as deleted type - should get as response 400 Bad Request")
    void createSMFromAlreadyMarkedAsDeletedType() throws Exception {
        final String SM_TYPE = "SOMETYPE"; // Convert to lowercase to match new logic 
        final SoftwareModule sm = testdataFactory.createSoftwareModule(SM_TYPE);

        final DistributionSetType type = testdataFactory.findOrCreateDistributionSetType(
                "testKey", "testType", Collections.singletonList(sm.getType()),
                Collections.singletonList(sm.getType()));

        final DistributionSet ds = testdataFactory.createDistributionSet(NAME_VALUE, "version", type, Collections.singletonList(sm));
        final Target target = testdataFactory.createTarget("test");

        assignDistributionSet(ds, target);

        // Delete sm type
        softwareModuleTypeManagement.delete(sm.getType().getId());

        // Check if it is marked as deleted
        final Optional<SoftwareModuleType> opt = softwareModuleTypeManagement.getByKey(SM_TYPE.toLowerCase()); // Use lowercase
        if (opt.isEmpty()) {
            throw new AssertionError("The Optional object of software module type should not be empty!");
        }

        final SoftwareModuleType smType = opt.get();
        Assert.isTrue(smType.isDeleted(), "Software Module Type not marked as deleted!");

        // Check if we'll get a bad request when trying to create a module from the deleted type
        final MvcResult mvcResult = mvc.perform(post(CREATE_SOFTWARE_MODULE_URL, TENANT_ID)
                        .content("[{\"description\":\"someDescription\",\"key\":\"someTestKey\", \"type\":\"" + SM_TYPE + "\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andReturn();

        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertEquals(JAKARTA_VALIDATION_VALIDATION_EXCEPTION, exceptionInfo.getName());
        assertTrue(exceptionInfo.getMessage().contains("Software Module Type already deleted"));
    }

    @Test
    @Description("Verifies that the system refuses unsupported request types and answers as defined to them, e.g. NOT FOUND on a non existing resource. Or a HTTP POST for updating a resource results in METHOD NOT ALLOWED etc.")
    void invalidRequestsOnSoftwaremodulesResource() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final List<SoftwareModule> modules = Arrays.asList(sm);

        // SM does not exist
        mvc.perform(get("/management/v1/tenants/1/softwaremodules/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete("/management/v1/tenants/1/softwaremodules/12345678"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post(BASE_SOFTWARE_MODULES_URL).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(BASE_SOFTWARE_MODULES_URL).content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post(BASE_SOFTWARE_MODULES_URL)
                        .content("[{\"description\":\"Desc123\",\"key\":\"test123\", \"type\":\"os\"}]")
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final SoftwareModule toLongName = entityFactory.softwareModule().create().type(osType)
                .name(RandomStringUtils.randomAlphanumeric(129)).format(format).version(MgmtRestConstants.DEFAULT_REQUEST_BODY_SM_VERSION)
                .swInstallerType(swInstallerType).build();
        mvc.perform(post(BASE_SOFTWARE_MODULES_URL).content(JsonBuilder.softwareModules(Arrays.asList(toLongName)))
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post(BASE_SOFTWARE_MODULES_URL).content(JsonBuilder.softwareModules(modules))
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        final SoftwareModule swm = entityFactory.softwareModule().create().name("encryptedModule").type(osType)
                .version(MgmtRestConstants.DEFAULT_REQUEST_BODY_SM_VERSION).vendor("vendor").description(DESCRIPTION_STRING).format(format)
                .swInstallerType(swInstallerType).encrypted(true).build();
        // artifact decryption is not supported
        mvc.perform(
                        post(BASE_SOFTWARE_MODULES_URL).content(JsonBuilder.softwareModules(Collections.singletonList(swm)))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put(BASE_SOFTWARE_MODULES_URL)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(BASE_SOFTWARE_MODULES_URL)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Test of modules retrieval without any parameters. Will return all modules in the system as defined by standard page size.")
    void getSoftwareModulesWithoutAddtionalRequestParameters() throws Exception {
        final int modules = 5;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, TENANT_ID))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(modules)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit parameter. Will return all modules in the system as defined by given page size.")
    void detSoftwareModulesWithPagingLimitRequestParameter() throws Exception {
        final int modules = 5;
        final int limitSize = 1;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, TENANT_ID).param(
                        MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Test of modules retrieval with paging limit offset parameters. Will return all modules in the system as defined by given page size starting from given offset.")
    void getSoftwareModulesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int modules = 5;
        final int offsetParam = 2;
        final int expectedSize = modules - offsetParam;
        createSoftwareModulesAlphabetical(modules);
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, TENANT_ID).param(
                                MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(modules)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(modules)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Test retrieval of all software modules the user has access to.")
    void getSoftwareModules() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();
        final SoftwareModule app = testdataFactory.createSoftwareModuleApp();
        Version version1 = testdataFactory.createVersionForSoftwareModule(os);
        Version version2 = testdataFactory.createVersionForSoftwareModule(app);
        associateArtifactAndSoftwareModule(os, version1);

        MvcResult mvcResult = mvc.perform(get(CREATE_SOFTWARE_MODULE_URL, TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + NAME, contains(os.getName())))
                .andExpect(
                        jsonPath(JSON_PATH_CONTENT_ID + os.getId() + DESCRIPTION, contains(os.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + VENDOR, contains(os.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + TYPE, contains("os")))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + CREATED_BY, contains(UPLOAD_TESTER)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + CREATED_AT, contains((int) os.getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + LINKS_SELF_HREF,
                        contains(BASE_SOFTWARE_MODULES_URL + SEPARATOR + os.getId())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].id").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].filename").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].fileType").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].description").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].signatureExpiryDate").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].size").exists())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].softwareModules").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + ARTIFACTS + "[0].hashes.sha256").exists())


                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + NAME, contains(app.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + DESCRIPTION,
                        contains(app.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + VENDOR, contains(app.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + TYPE, contains("application")))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + CREATED_BY, contains(UPLOAD_TESTER)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + CREATED_AT, contains((int) app.getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].id").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].filename").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].fileType").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].description").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].signatureExpiryDate").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].size").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].softwareModules").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app.getId() + ARTIFACTS + "[0].hashes.sha256").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + LINKS_SELF_HREF,
                        contains(BASE_SOFTWARE_MODULES_URL + SEPARATOR + os.getId()))).andReturn();

        PagedList<MgmtSoftwareModule> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        //assert for no ecuModels since there are no associations made
        assertEquals(0, response.getContent().get(0).getEcuModels().size());

        assertThat(softwareModuleManagement.findAll(PAGE)).as(SOFTWARE_MODULE_DESCRIPTION).hasSize(2);
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Test retrieval of all software modules the user has access to.")
    void givenSoftwareModeWithEcuAssociatedEcuModelsWhenGetReturnsSuccess() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();
        //create and associate ECU Models to given software Module
        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();
        testdataFactory.assignEcuSoftwareModules(os.getId(), List.of(ecuId));

        MvcResult mvcResult = mvc.perform(get(CREATE_SOFTWARE_MODULE_URL, TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + NAME, contains(os.getName())))
                .andExpect(
                        jsonPath(JSON_PATH_CONTENT_ID + os.getId() + DESCRIPTION, contains(os.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + VENDOR, contains(os.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + TYPE, contains("os")))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + CREATED_BY, contains(UPLOAD_TESTER)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + CREATED_AT, contains((int) os.getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os.getId() + LINKS_SELF_HREF,
                        contains(BASE_SOFTWARE_MODULES_URL + SEPARATOR + os.getId()))).andReturn();

        PagedList<MgmtSoftwareModule> response = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        //assert for ecuModels for which associations are made
        List<MgmtEcuModelResponse> ecuModels = response.getContent().get(0).getEcuModels();
        assertEquals(1, response.getContent().get(0).getEcuModels().size());
        assertEquals(ecuId, ecuModels.get(0).getId());

        assertThat(softwareModuleManagement.findAll(PAGE)).as(SOFTWARE_MODULE_DESCRIPTION).hasSize(1);
    }

    @Test
    @Description("Test the various filter parameters, e.g. filter by name or type of the module.")
    void getSoftwareModulesWithFilterParameters() throws Exception {
        final SoftwareModule os1 = testdataFactory.createSoftwareModuleOs("1");
        final SoftwareModule app1 = testdataFactory.createSoftwareModuleApp("1");
        testdataFactory.createSoftwareModuleOs("2");
        final SoftwareModule app2 = testdataFactory.createSoftwareModuleApp("2");

        assertThat(softwareModuleManagement.findAll(PAGE)).hasSize(4);

        // only by name, only one exists per name
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "?q=name==" + os1.getName(), 1L).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os1.getId() + NAME, contains(os1.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os1.getId() + DESCRIPTION,
                        contains(os1.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os1.getId() + VENDOR, contains(os1.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + os1.getId() + TYPE, contains("os")))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)));

        // by type, 2 software modules per type exists
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "?q=type==" + Constants.SMT_DEFAULT_APP_KEY, 1L)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + NAME, contains(app1.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + DESCRIPTION,
                        contains(app1.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + VENDOR, contains(app1.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + TYPE,
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app2.getId() + NAME, contains(app2.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app2.getId() + DESCRIPTION,
                        contains(app2.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app2.getId() + VENDOR, contains(app2.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app2.getId() + TYPE,
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(2))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(2)));

        // by type and version=2.0.0 -> only one result
        mvc.perform(get(
                        MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "?q=type==" + Constants.SMT_DEFAULT_APP_KEY + ";version==" + app1.getVersion(), 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + NAME, contains(app1.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + DESCRIPTION,
                        contains(app1.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + VENDOR, contains(app1.getVendor())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_ID + app1.getId() + TYPE,
                        contains(Constants.SMT_DEFAULT_APP_KEY)))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)));
    }

    @Test
    @Description("Verifies that the system answers as defined in case of a wrong filter parameter syntax. Expected result: BAD REQUEST with error description.")
    void getSoftwareModulesWithSyntaxErrorFilterParameter() throws Exception {
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "?q=wrongFIQLSyntax", 1L).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.REST_RSQL_SEARCH_PARAM_SYNTAX.name())));
    }

    @Test
    @Description("Verifies that the system answers as defined in case of a non existing field used in filter. Expected result: BAD REQUEST with error description.")
    void getSoftwareModulesWithUnknownFieldErrorFilterParameter() throws Exception {
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + "?q=wrongField==abc", 1L).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.REST_RSQL_PARAM_INVALID_FIELD.name())));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Tests GET request on /management/v1/tenants/{tenantId}/softwaremodules/{smId}.")
    void getSoftwareModule() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_ID_V1_REQUEST_MAPPING, 1L, os.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(os.getName())))
                .andExpect(jsonPath("$.description", equalTo(os.getDescription())))
                .andExpect(jsonPath("$.vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$.type", equalTo(os.getType().getKey())))
                .andExpect(jsonPath(JSON_PATH_DELETED, equalTo(os.isDeleted())))
                .andExpect(jsonPath("$.createdBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.createdAt", equalTo((int) os.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo(BASE_SOFTWARE_MODULES_URL + SEPARATOR + os.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/management/v1/tenants/1/softwaremodule-types/" + osType.getId())));

        assertThat(softwareModuleManagement.findAll(PAGE)).as(SOFTWARE_MODULE_DESCRIPTION).hasSize(1);
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Tests GET request on /management/v1/tenants/{tenantId}/scomos/{scomoId}.")
    void getSoftwareModuleWithScomoId() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(get(MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING, 1L, os.getName()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(os.getName())))
                .andExpect(jsonPath("$.description", equalTo(os.getDescription())))
                .andExpect(jsonPath("$.vendor", equalTo(os.getVendor())))
                .andExpect(jsonPath("$.type", equalTo(os.getType().getKey())))
                .andExpect(jsonPath(JSON_PATH_DELETED, equalTo(os.isDeleted())))
                .andExpect(jsonPath("$.createdBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.createdAt", equalTo((int) os.getCreatedAt())))
                .andExpect(jsonPath("$._links.metadata.href",
                        equalTo(BASE_SOFTWARE_MODULES_URL + SEPARATOR + os.getId()
                                + "/metadata?offset=0&limit=50")))
                .andExpect(jsonPath("$._links.type.href",
                        equalTo("http://localhost/management/v1/tenants/1/softwaremodule-types/" + osType.getId())));

        assertThat(softwareModuleManagement.findAll(PAGE)).as(SOFTWARE_MODULE_DESCRIPTION).hasSize(1);
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Verifies that the create request actually results in the creation of the modules in the repository.")
    void createSoftwareModules() throws Exception {
        final SoftwareModule os = entityFactory.softwareModule()
                .create()
                .name(KNOWN_SW_NAME)
                .type(osType)
                .version(KNOWN_SW_VERSION)
                .vendor(KNOWN_SW_VENDOR)
                .description(KNOWN_SW_DESCRIPTION)
                .format(format)
                .swInstallerType(swInstallerType)
                .build();
        final SoftwareModule ah = entityFactory.softwareModule()
                .create()
                .name(NAME_3)
                .type(appType)
                .version("version3")
                .vendor("vendor3")
                .description("description3")
                .format(format)
                .swInstallerType(swInstallerType)
                .build();

        final List<SoftwareModule> modules = Arrays.asList(os, ah);

        final long current = Instant.now().getEpochSecond();

        final MvcResult mvcResult = mvc.perform(
                        post(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON_VALUE)
                                .content(JsonBuilder.softwareModules(modules))
                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo(KNOWN_SW_NAME)))
                .andExpect(jsonPath("[0].description", equalTo(KNOWN_SW_DESCRIPTION)))
                .andExpect(jsonPath("[0].vendor", equalTo(KNOWN_SW_VENDOR)))
                .andExpect(jsonPath("[0].type", equalTo("os")))
                .andExpect(jsonPath("[0].createdBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("[1].name", equalTo(NAME_3)))
                .andExpect(jsonPath("[1].description", equalTo("description3")))
                .andExpect(jsonPath("[1].vendor", equalTo("vendor3")))
                .andExpect(jsonPath("[1].type", equalTo("application")))
                .andExpect(jsonPath("[1].createdBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andReturn();

        final SoftwareModule osCreated = softwareModuleManagement.getByNameAndVersionAndType(KNOWN_SW_NAME, KNOWN_SW_VERSION,
                osType.getId()).get();
        final SoftwareModule appCreated = softwareModuleManagement.getByNameAndVersionAndType(NAME_3, "version3",
                appType.getId()).get();

        assertThat(JsonPath.compile("[0]._links.self.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains invalid self href")
                .isEqualTo(BASE_SOFTWARE_MODULES_URL + SEPARATOR + osCreated.getId());

        assertThat(JsonPath.compile("[1]._links.self.href")
                .read(mvcResult.getResponse().getContentAsString())
                .toString()).as("Response contains links self href")
                .isEqualTo(BASE_SOFTWARE_MODULES_URL + SEPARATOR + appCreated.getId());

        assertThat(softwareModuleManagement.findAll(PAGE)).as("Wrong softwaremodule size").hasSize(2);
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getName()).as(
                "Softwaremoudle name is wrong").isEqualTo(os.getName());
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedBy()).as(
                "Softwaremoudle created by is wrong").isEqualTo(UPLOAD_TESTER);
        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent().get(0).getCreatedAt()).as(
                "Softwaremoudle created at is wrong").isGreaterThanOrEqualTo(current);
        assertThat(softwareModuleManagement.findByType(PAGE, appType.getId()).getContent().get(0).getName()).as(
                "Softwaremoudle name is wrong").isEqualTo(ah.getName());
    }

    @Test
    @Description("Verifies successfull deletion of software modules that are not in use, i.e. assigned to a DS.")
    void deleteUnassignedSoftwareModule() throws Exception {

    }

    @Test
    @Description("Verifies deletion of a software module which is associated with distribution set throws error")
    void givenSmWithDsAssociationWhenDeleteSmThenThrowError() throws Exception {

    }

    @Test
    @Description("Verifies successful deletion of a software module which is not associated with distribution set")
    void givenSmWithoutDsAssociationWhenDeleteSmThenSuccess() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();


        mvc.perform(delete(SOFTWARE_MODULE_URL, TENANT_ID, sm.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    @Description("Verifies the successful creation of metadata and the enforcement of the meta data quota.")
    void createMetadata() throws Exception {

        final String knownKey1 = "knownKey1";
        final String knownValue1 = KNOWN_VALUE_1;
        final String knownKey2 = "knownKey2";
        final String knownValue2 = KNOWN_VALUE_1;

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put(KEY, knownKey1).put(VALUE, knownValue1));
        metaData1.put(new JSONObject().put(KEY, knownKey2).put(VALUE, knownValue2).put("targetVisible", true));

        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].key", equalTo(knownKey1)))
                .andExpect(jsonPath("[0].value", equalTo(knownValue1)))
                .andExpect(jsonPath("[0].targetVisible", equalTo(false)))
                .andExpect(jsonPath("[1].key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1].value", equalTo(knownValue2)))
                .andExpect(jsonPath("[1].targetVisible", equalTo(true)));

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey1))
                .as("Metadata key is wrong").get().extracting(SoftwareModuleMetadata::getValue).isEqualTo(knownValue1);
        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey2))
                .as("Metadata key is wrong").get().extracting(SoftwareModuleMetadata::getValue).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put(KEY, knownKey1 + i).put(VALUE, knownValue1 + i));
        }

        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, Integer.MAX_VALUE), sm.getId()).getTotalElements())
                .isEqualTo(metaData1.length());

    }

    @Test
    @Description("Verifies the successful update of metadata based on given key.")
    void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;
        final String updateValue = VALUE_FOR_UPDATE;

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        final JSONObject jsonObject = new JSONObject().put(KEY, knownKey)
                .put(VALUE, updateValue)
                .put("targetVisible", true);

        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING, TENANT_ID, sm.getId(), knownKey).accept(
                        MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(KEY, equalTo(knownKey)))
                .andExpect(jsonPath(VALUE, equalTo(updateValue)));

        final SoftwareModuleMetadata assertDS = softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(),
                knownKey).get();
        assertThat(assertDS.getValue()).as("Metadata is wrong").isEqualTo(updateValue);
        assertThat(assertDS.isTargetVisible()).as("target visible is wrong").isTrue();
    }

    @Test
    @Description("Verifies the successful deletion of metadata entry.")
    void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING, TENANT_ID, sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that module metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteModuleMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;

        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(sm.getId()).key(knownKey).value(knownValue));

        mvc.perform(delete("/management/v1/tenants/{tenantId}/softwaremodules/{swId}/metadata/XXX", TENANT_ID, sm.getId(), knownKey))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_METADATA_ID_V1_REQUEST_MAPPING, TENANT_ID, "1234", knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(sm.getId(), knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that module deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteSoftwareModuleThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ID_V1_REQUEST_MAPPING, 1, "1234"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that module deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteSoftwareModuleThatDoesNotExistLeadsToNotFoundWithScomoId() throws Exception {
        mvc.perform(delete(MgmtRestConstants.SCOMO_ID_V1_REQUEST_MAPPING, 1, "Scomo-test"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies the successful search of a metadata entry based on value.")
    void searchSoftwareModuleMetadataRsql() throws Exception {
        final int totalMetadata = 10;
        final String knownKeyPrefix = KNOWN_KEY;
        final String knownValuePrefix = KNOWN_VALUE;
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();

        for (int index = 0; index < totalMetadata; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                    .create(sm.getId())
                    .key(knownKeyPrefix + index)
                    .value(knownValuePrefix + index));
        }

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_METADATA_V1_REQUEST_MAPPING + "?q=" + rsqlSearchValue1, 1, sm.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo(KNOWN_VALUE_1)));
    }

    private void createSoftwareModulesAlphabetical(final int amount) {
        char character = 'a';
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            softwareModuleManagement.create(entityFactory.softwareModule().create().type(osType).name(str)
                    .description(str).vendor(str).version(str).format(format).swInstallerType(swInstallerType));
            character++;
        }
    }

    @Test
    @Description("Create two version with same name and number for two different software modules.")
    void createTwoVersionsWithTwoDifferentSoftwareModules() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();
        final SoftwareModule app = testdataFactory.createSoftwareModuleApp();


        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, os.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"someTestKey\", \"number\":" + 1 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());


        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, app.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"someTestKey\", \"number\":" + 1 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());

    }


    @Test
    @Description("Create two version with the same name and the same software module id, this should cause a conflict")
    void createSameVersionWithSameName() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, os.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"sameName\", \"number\":" + 1 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());


        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, os.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"sameName\", \"number\":" + 2 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());

    }

    @Test
    @Description("Create two version with the same name and the same software module id, this should cause a conflict")
    void createSameVersionWithSameNumber() throws Exception {
        final SoftwareModule os = testdataFactory.createSoftwareModuleOs();

        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, os.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"name1\", \"number\":" + 1 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());


        mvc.perform(post(GET_SOFTWAREMODULES_ID_VERSION, os.getId()).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"someDescription\",\"name\":\"name2\", \"number\":" + 1 + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());

    }

    @Test
    @Description("Software Module id not present throws not found exeception")
    void givenSoftwareModuleAndEcuModelListWhenAssignSoftwareModuleIdNotFound() throws Exception {

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(2L, 3L)).build();
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, -1L).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Description("Ecu List is empty throws bad request")
    void givenSoftwareModuleAndEcuModelListWhenAssignBadRequest() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);
        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(new ArrayList<EcuModels>()).build();
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("One or more EcuModels are not found in table throws bad request")
    void givenSwModuleAndEcuModelListWhenAssignEcuModelNotfoundThrowsBadRequest() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);
        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(2L, 3L)).build();
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ecu Models associated for a given Software module id")
    void givenSwModuleAndEcuModelListWhenAssignSuccess() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = new MgmtEcuModelRequest();
        request1.setEcuModelType(EcuModelTypeEnum.OM.toString());
        request1.setEcuModelName(TEST_ECU_MODEL_NAME + randomNumber.nextInt(10000));
        request1.setEcuNodeId(TEST_ECU_NODE_ID + randomNumber.nextInt(10000));
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();
        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ecu Models association removed for a given Software module id")
    void givenSwModuleAndEcuModelListWhenRemoveAssociationThenSuccess() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ecu Models association removal for a given Software module id fails with invalid ecuModelId(s)")
    void givenSwModuleAndInvalidEcuModelListWhenRemoveAssociationThenError() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(randomNumber.nextLong(1000), null)).build();
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ignore non associated Ecu Models and remove only associated ECU Models from the given list for a given Software module id")
    void givenSwModuleAndEMsWhenRemoveAssociationThenSuccessOnlyAssociatedEMsRemoved() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        MgmtEcuModelRequest request2 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1, request2)));

        Long ecuId = savedEcuModel.get(0).getId();
        Long ecuId2 = savedEcuModel.get(1).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, ecuId2)).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        SoftwareModule softwareModule = softwareModuleManagement.getSoftwareModuleById(sm.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, sm.getId()));

        List<EcuModel> ecuModels = new ArrayList<>(softwareModule.getSoftwareEcuModels());

        assertThat(ecuModels, hasSize(0));
    }

    @Test
    @Description("Ecu Models association removal throws error for invalid Software module id")
    void givenInvalidSwModuleAndEcuModelListWhenRemoveAssociationThenError() throws Exception {

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, randomNumber.nextLong(10000)
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ecu Models association removal throws error for empty ECU List for given Software module id")
    void givenSwModuleAndEmptyEMWhenRemoveAssociationThenError() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);


        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(Collections.emptyList()).build();
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Remove Association fails with one or more ECU models does not exist from given ECU list for a given Software module id")
    void givenSwModuleAndNonExistingEMsWhenRemoveAssociationThenError() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, randomNumber.nextLong(10000))).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ecu Models association removal throws error for a given Software module id which is part of active rollout")
    void givenSwModuleWithActiveRolloutAndEMsWhenRemoveAssociationThenError() throws Exception {

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        //DS associted with this SM
        DistributionSet ds = testdataFactory.createDistributionSet("RolloutDsToRemoveAssociations");
        SoftwareModule sm1 = ds.getDistributionSetModules().get(0).getSm();
        testdataFactory.assignEcuSoftwareModules(sm1.getId(), List.of(ecuId));
        Long vehicleModel = testdataFactory.createVehicle("NES 2.0").getId();

        Target target = testdataFactory.createTarget(KNOWN_TARGET_ID, KNOWN_TARGET_ID, KNOWN_TARGET_ID, vehicleModel);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        // Run rollout
        rolloutManagement.start(rollout.getId());
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm1.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Attempt to remove ECU Model association for a software module linked to a distribution set, expecting validation error.")
    void givenSwModuleAssociatedWithDistributionSetWhenDeleteEcuModelAssociationThenValidationException() throws Exception {
        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        MgmtEcuModelRequest request2 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1, request2)));

        Long ecuId1 = savedEcuModel.get(0).getId();
        Long ecuId2 = savedEcuModel.get(1).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId1, null)).build();
        //DS associted with this SM
        DistributionSet ds = testdataFactory.createDistributionSet("DSAsssignSWM");
        SoftwareModule sm1 = ds.getDistributionSetModules().get(0).getSm();
        testdataFactory.assignEcuSoftwareModules(sm1.getId(), List.of(ecuId1, ecuId2));
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm1.getId()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest()) // Assuming ValidationException translates to a 400 Bad Request
                .andExpect(result -> assertEquals("Cannot unlink ecu model ID because the software module is associated with a distribution set.", result.getResolvedException().getMessage()));
    }

    @Test
    @Description("Software Module id not present throws not found exeception")
    void givenSoftwareModuleAndEcuModelListWhenAssignSoftwareModuleIdNotFoundWithScomoId() throws Exception {

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(2L, 3L)).build();
        mvc.perform(put(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, "Not-present").content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Description("Ecu List is empty throws bad request")
    void givenSoftwareModuleAndEcuModelListWhenAssignBadRequest_SCOMO() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);
        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(new ArrayList<EcuModels>()).build();
        mvc.perform(put(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ecu Models associated for a given Software module id")
    void givenSwModuleAndEcuModelListWhenAssignSuccess_SCOMO() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = new MgmtEcuModelRequest();
        request1.setEcuModelType(EcuModelTypeEnum.OM.toString());
        request1.setEcuModelName(TEST_ECU_MODEL_NAME + randomNumber.nextInt(10000));
        request1.setEcuNodeId(TEST_ECU_NODE_ID + randomNumber.nextInt(10000));
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();
        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        mvc.perform(put(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ecu Models association removed for a given Software module id")
    void givenSwModuleAndEcuModelListWhenRemoveAssociationThenSuccessWithScomoId() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ecu Models association removal for a given Software module id fails with invalid ecuModelId(s)")
    void givenSwModuleAndInvalidEcuModelListWhenRemoveAssociationThenErrorWithScomoId() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA1, QUOTA1, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(randomNumber.nextLong(1000), null)).build();
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ignore non associated Ecu Models and remove only associated ECU Models from the given list for a given Software module id")
    void givenSwModuleAndEMsWhenRemoveAssociationThenSuccessOnlyAssociatedEMsRemovedWithScomoId() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        MgmtEcuModelRequest request2 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1, request2)));

        Long ecuId = savedEcuModel.get(0).getId();
        Long ecuId2 = savedEcuModel.get(1).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, ecuId2)).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        SoftwareModule softwareModule = softwareModuleManagement.getSoftwareModuleById(sm.getId())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, sm.getId()));

        List<EcuModel> ecuModels = new ArrayList<>(softwareModule.getSoftwareEcuModels());

        assertThat(ecuModels, hasSize(0));
    }

    @Test
    @Description("Ecu Models association removal throws error for empty ECU List for given Software module id")
    void givenSwModuleAndEmptyEMWhenRemoveAssociationThenErrorWithScomoId() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);


        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(Collections.emptyList()).build();
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Remove Association fails with one or more ECU models does not exist from given ECU list for a given Software module id")
    void givenSwModuleAndNonExistingEMsWhenRemoveAssociationThenErrorWithScomoId() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModule(QUOTA, QUOTA, false, FORMAT_KEY, SM_INSTALLER_TYPE_0);

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, randomNumber.nextLong(10000))).build();
        testdataFactory.assignEcuSoftwareModules(sm.getId(), List.of(ecuId));
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ecu Models association removal throws error for a given Software module id which is part of active rollout")
    void givenSwModuleWithActiveRolloutAndEMsWhenRemoveAssociationThenErrorWithScomoId() throws Exception {

        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));

        Long ecuId = savedEcuModel.get(0).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId, null)).build();
        //DS associted with this SM
        DistributionSet ds = testdataFactory.createDistributionSet("RolloutDsToRemoveAssociations");
        SoftwareModule sm1 = ds.getDistributionSetModules().get(0).getSm();
        testdataFactory.assignEcuSoftwareModules(sm1.getId(), List.of(ecuId));
        Long vehicleModel = testdataFactory.createVehicle("NES 2.0").getId();

        Target target = testdataFactory.createTarget(KNOWN_TARGET_ID, KNOWN_TARGET_ID, KNOWN_TARGET_ID, vehicleModel);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        // Run rollout
        rolloutManagement.start(rollout.getId());
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm1.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Attempt to remove ECU Model association for a software module linked to a distribution set, expecting validation error.")
    void givenSwModuleAssociatedWithDistributionSetWhenDeleteEcuModelAssociationThenValidationExceptionWithScomoId() throws Exception {
        MgmtEcuModelRequest request1 = getTestMgmtEcuModelRequest();
        MgmtEcuModelRequest request2 = getTestMgmtEcuModelRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(List.of(request1, request2)));

        Long ecuId1 = savedEcuModel.get(0).getId();
        Long ecuId2 = savedEcuModel.get(1).getId();

        MgmtAssignEcuModelRequestBody request = MgmtAssignEcuModelRequestBody.builder().ecuModels(getEcuModelAssignmentRequest(ecuId1, null)).build();
        //DS associted with this SM
        DistributionSet ds = testdataFactory.createDistributionSet("DSAsssignSWM");
        SoftwareModule sm1 = ds.getDistributionSetModules().get(0).getSm();
        testdataFactory.assignEcuSoftwareModules(sm1.getId(), List.of(ecuId1, ecuId2));
        mvc.perform(delete(MgmtRestConstants.SCOMO_ECU_MODEL_V1_REQUEST_MAPPING, TENANT_ID, sm1.getName()
                ).content(objectMapper.writeValueAsString(request))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest()) // Assuming ValidationException translates to a 400 Bad Request
                .andExpect(result -> assertEquals("Cannot unlink ecu model ID because the software module is associated with a distribution set.", result.getResolvedException().getMessage()));
    }

    private List<EcuModels> getEcuModelAssignmentRequest(Long param1, Long param2) {
        List<EcuModels> test = new ArrayList<>();
        EcuModels id1 = new EcuModels();
        id1.setEcuModelId(param1);
        test.add(id1);
        if (param2 != null) {
            EcuModels id2 = new EcuModels();
            id2.setEcuModelId(param2);
            test.add(id2);
        }
        return test;
    }

    private MgmtEcuModelRequest getTestMgmtEcuModelRequest() {
        return MgmtEcuModelRequest.builder()
                .ecuModelType(EcuModelTypeEnum.SU.toString())
                .ecuModelName(TEST_ECU_MODEL_NAME + randomNumber.nextInt(10000))
                .ecuNodeId(TEST_ECU_NODE_ID + randomNumber.nextInt(10000))
                .build();
    }

    @Test
    @Description("Validates the DELETE request based on a software module's association with a Distribution Set and Rollout")
    void givenSoftwareModuleIsAssociatedToDsAndRolloutInAnyStateThenDeleteSMFail() throws Exception {
        DistributionSet ds1 = testdataFactory.createDistributionSet("NewRolloutDsToDeleteSoftwareModule");
        SoftwareModule sm2 = ds1.getDistributionSetModules().get(0).getSm();
        Long vehicleModel = testdataFactory.createVehicle("NewNES 2.0").getId();

        Target target = testdataFactory.createTarget(CONTROLLER_ID, CONTROLLER_ID, CONTROLLER_ID, vehicleModel);
        // Create Rollout with ds with ACTIVE status
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds1, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        // Run rollout
        rolloutManagement.start(rollout.getId());
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_ID, TENANT_ID, sm2.getId()
        )).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Validates that the response contains all associations when a software module association request is made.")
    void givenSoftwareModuleAssociationRequest_WhenAssociatingWithRollout_ThenResponseShouldContainAllAssociations() throws Exception {

        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));

        // Associating 1 software module and version with the rollout
        SoftwareModule softwareModuleOs = testdataFactory.createSoftwareModuleOs("test1");
        Version versionForSoftwareModule = testdataFactory.createVersionForSoftwareModule(softwareModuleOs);
        MgmtSoftwareModuleRequest mgmtSoftwareModuleRequest = buildSoftwareModuleRequest(softwareModuleOs.getId(), versionForSoftwareModule.getId());
        Artifacts artifacts = associateArtifactAndSoftwareModule(softwareModuleOs, versionForSoftwareModule);
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(mgmtSoftwareModuleRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.associatedModules", hasSize(1)))
                .andExpect(jsonPath("$.associatedModules[0].softwareVersionTargetId").value(versionForSoftwareModule.getId()))
                .andExpect(jsonPath("$.associatedModules[0].moduleId").value(softwareModuleOs.getId()));

        // Associating another software module and version with the rollout, the response consists of two associations
        SoftwareModule softwareModuleOs1 = testdataFactory.createSoftwareModuleOs("test2");
        Version versionForSoftwareModule1 = testdataFactory.createVersionForSoftwareModule(softwareModuleOs1);
        mgmtSoftwareModuleRequest = buildSoftwareModuleRequest(softwareModuleOs1.getId(), versionForSoftwareModule1.getId());
        Artifacts artifacts1 = associateArtifactAndSoftwareModule(softwareModuleOs1, versionForSoftwareModule1);
        changeArtifactStatus(artifactsRepository.findById(artifacts1.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(mgmtSoftwareModuleRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.associatedModules", hasSize(2)))
                .andExpect(jsonPath("$.associatedModules[?(@.softwareVersionTargetId == %s && @.moduleId == %s)]", versionForSoftwareModule.getId(), softwareModuleOs.getId()).exists())
                .andExpect(jsonPath("$.associatedModules[?(@.softwareVersionTargetId == %s && @.moduleId == %s)]", versionForSoftwareModule1.getId(), softwareModuleOs1.getId()).exists());


        SoftwareModule softwareModuleOs2 = testdataFactory.createSoftwareModuleOs("test3");
        Version versionForSoftwareModule2 = testdataFactory.createVersionForSoftwareModule(softwareModuleOs2);
        mgmtSoftwareModuleRequest = buildSoftwareModuleRequest(softwareModuleOs2.getId(), versionForSoftwareModule2.getId());
        Artifacts artifacts2 = associateArtifactAndSoftwareModule(softwareModuleOs2, versionForSoftwareModule2);
        changeArtifactStatus(artifactsRepository.findById(artifacts2.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(mgmtSoftwareModuleRequest))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.associatedModules", hasSize(3)));
    }


    /**
     * Creates an artifact and associates it with the given software module and version.
     *
     * @param softwareModule the software module to associate the artifact with
     * @param version        the version to associate the artifact with
     */
    private Artifacts associateArtifactAndSoftwareModule(SoftwareModule softwareModule, Version version) {
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(FILE_1, FileType.FULL, DESCRIPTION_STRING, "123", "SHA_256", 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) artifact)
                .softwareModule((JpaSoftwareModule) softwareModule)
                .sourceVersion((JpaVersion) version)
                .targetVersion((JpaVersion) version)
                .build();
        testdataFactory.createArtifactsSoftwareModuleAssociation(Set.of(association));
        return artifact;
    }

    @Test
    @Description("Validates that a 404 Not Found error is returned when associating an artifact from a different tenant with a rollout.")
    void givenArtifactFromDifferentTenantWhenAssociatingWithRolloutThenShouldReturnNotFound() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        SoftwareModule softwareModuleOs = testdataFactory.createSoftwareModuleOs("test1");
        Version versionForSoftwareModule = testdataFactory.createVersionForSoftwareModule(softwareModuleOs);
        MgmtSoftwareModuleRequest mgmtSoftwareModuleRequest = buildSoftwareModuleRequest(softwareModuleOs.getId(), versionForSoftwareModule.getId());
        Artifacts artifacts = associateArtifactAndSoftwareModule(softwareModuleOs, versionForSoftwareModule);
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        JpaArtifacts jpaArtifact = artifactsRepository.findById(artifacts.getId()).get();
        jpaArtifact.setTenant("DEFAULT123");
        artifactsRepository.save(jpaArtifact);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_ASSOCIATE_V1_REQUEST_MAPPING_TENANT, "123L", rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(List.of(mgmtSoftwareModuleRequest)))).andExpect(status().isBadRequest());
    }


    @Test
    @Description("Given a software module with a specific name already exists, when attempting to create another with the same name, then a validation exception should be thrown.")
    void givenSoftwareModuleWithDuplicateName_WhenCreate_ThenValidationException() throws Exception {
        final String duplicateName = "uniqueModuleName";
        // Create the first module directly in the database
        SoftwareModule s=testdataFactory.createSoftwareModule(duplicateName, "vendorA", false, "formatA", SM_INSTALLER_TYPE_0);

        // Prepare the duplicate module request
        final MgmtSoftwareModuleRequestBodyPost duplicateModule = MgmtSoftwareModuleRequestBodyPost.builder()
                .name("vendorA"+duplicateName)
                .vendor("vendorA")
                .description("Second module")
                .format("formatA")
                .type("typeA")
                .swInstallerType(SM_INSTALLER_TYPE_0)
                .build();

        // Attempt to create the duplicate via REST API and verify the response
        mvc.perform(post(CREATE_SOFTWARE_MODULE_URL, TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(List.of(duplicateModule))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString(
                        "Software Module with name '" + "vendorA"+duplicateName + "' already exists for tenant ")));
    }



}
