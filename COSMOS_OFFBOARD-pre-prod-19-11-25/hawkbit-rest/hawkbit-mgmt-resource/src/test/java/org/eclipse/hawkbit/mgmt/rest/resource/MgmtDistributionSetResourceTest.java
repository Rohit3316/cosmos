/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPost;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtGetVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssignments;
import org.cosmos.models.mgmt.softwaremodule.dto.VersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.util.TestCommonConstants;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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


@Feature("Component Tests - Management API")
@Story("Distribution Set Resource")
public class MgmtDistributionSetResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String TOTAL_ROLLOUTS_PER_STATUS = "totalRolloutsPerStatus";
    private static final String MAINTENANCE_WINDOW = "maintenanceWindow";
    private static final String TOTAL = "$.total";
    private static final String ALREADY_ASSIGNED = "$.alreadyAssigned";
    private static final String ASSIGNED = "$.assigned";
    private static final String TARGET_ASSIGNED_WITH_DS = "Five targets in repository have DS assigned";
    private static final String KNOWN_TARGET_ID_2 = TestCommonConstants.KNOWN_TARGET_ID + "2";
    private static final String KNOWN_TARGET_ID_3 = TestCommonConstants.KNOWN_TARGET_ID + "3";
    private static final String NAME_Y = "name==y";
    private static final String CLOSE_BRACKETS_ID = "')].id";
    private static final String JSON_PATH_MODULES_TYPE = "$.content.[0].modules.[?(@.type=='";
    private static final String JSON_PATH_DELETED = "$.deleted";
    private static final String JSON_PATH_MODULE_TYPE = "$.modules.[?(@.type=='";
    private static final String JSON_PATH_MODULE_TYPE_0 = "[0].modules.[?(@.type=='";
    private static final String JSON_PATH_MODULE_TYPE_1 = "[1].modules.[?(@.type=='";
    private static final String UPLOAD_TESTER = "uploadTester";
    private static final String JSON_PATH_MODULE_TYPE_2 = "[2].modules.[?(@.type=='";
    private static final String VALUE = "value";
    private static final String DELTA = "DELTA";
    private static final String TEST_FILE_DESCRIPTION = "New file description";
    private static final String NAME = "name";
    private static final String DISTRIBUTIONSETS_DS_TARGETS_URL = "/distributionsets/{ds}/targets";
    private static final String TENANTS_URL = "/management/v1/tenants/";
    private static final String TARGETS = "targets";
    private static final String DESCRIPTION = "description";
    private static final String QUERY = "name==targets*";
    private static final String TOTAL_ACTIONS_PER_STATUS = "totalActionsPerStatus";
    private static final String TOTAL_AUTO_ASSIGNMENTS = "totalAutoAssignments";
    private static final String DS_STATISTICS_TARGETS_URL = "/{ds}/statistics/targets";
    private static final String VERSION = "398,88";
    private static final String JUPITER = "Jupiter";
    private static final String JSON_PATH_MESSAGE = "$.message";
    private static final String DELETE_SP_ARTIFACT_SOFTWARE_MODULE = "DELETE FROM sp_artifact_software_module";
    private static final String DELETE_SP_SOFTWARE_VERSIONS = "DELETE FROM sp_software_versions";
    private static final String DELETE_SP_ARTIFACTS = "DELETE FROM sp_artifacts";
    private static final String DELETE_SP_DS_MODULE = "DELETE FROM sp_ds_module";
    private static final String DELETE_SP_BASE_SOFTWARE_MODULE = "DELETE FROM sp_base_software_module";
    private static final String DELETE_SP_ROLLOUT = "DELETE FROM sp_rollout";
    private static final String DELETE_SP_ACTION = "DELETE FROM sp_action";
    private static final String DELETE_SP_TARGET = "DELETE FROM sp_target";
    private static final String DELETE_SP_DISTRIBUTION_SET = "DELETE FROM sp_distribution_set";
    private static final String JSON_PATH_REQUIRED_MIGRATION_STEP = "$.requiredMigrationStep";
    private static final String REQUIRED_MIGRATION_STEP = "requiredMigrationStep";
    private static final String UPDATED_NAME = "Updated Name";
    private static final String UPDATED_DESCRIPTION = "Updated Description";
    private static final String ROLLOUT = "rollout";
    private static final String KNOWN_KEY = "knownKey";
    private static final String KNOWN_VALUE = "knownValue";

    private static final String VIN= "19UYA31581L100499";

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;

    @TempDir
    static Path tempDir;
    private static ClientAndServer mockServer;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true), Arguments.of(false, false), Arguments.of(true, null), Arguments.of(false, null));
    }

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
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

    @BeforeEach
    void reset() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_artifact_software_module", "sp_software_versions", "sp_artifacts", "sp_ds_module", "sp_base_software_module", "sp_rollout", "sp_action", "sp_target", "sp_distribution_set");

        MockitoAnnotations.openMocks(this); // Initialize mocks

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);

        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_action", "sp_rollout", "sp_target", "sp_vehicle_model", "sp_artifact_software_module", "sp_software_versions", "sp_artifacts", "sp_base_software_module", "sp_distribution_set");
    }

    @Test
    @Description("This test verifies the deletion of a assigned Software Module of a Distribution Set can not be achieved when that Distribution Set has been assigned or installed to a target.")
    void deleteFailureWhenDistributionSetInUse() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        SoftwareModule sm = set.findFirstModuleByType(osType).get();
        final Long tenantId = 1L;

        // create targets and assign DisSet to target
        final long forceTime = Instant.now().getEpochSecond();
        final String[] knownTargetIds = new String[]{"target1", "target2", "target3"};
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIds) {
            targetManagement.create(entityFactory.target().create().controllerId(targetId).name(targetId).serialNumber(targetId).vehicleModelId(testdataFactory.createVehicle(targetId).getId()).vin(VIN));
            list.put(new JSONObject().put("id", targetId).put("type", "timeforced").put("forcetime", forceTime).put(MAINTENANCE_WINDOW, new JSONObject().put(TestCommonConstants.SCHEDULE, getTestSchedule(100)).put(TestCommonConstants.DURATION, getTestDuration(10)).put(TestCommonConstants.TIMEZONE, getTestTimeZone())));
        }

        // assign already one target to DS
        assignDistributionSet(set.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, set.getId()).content(list.toString()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        // try to delete the Software Module from DistSet that has been assigned
        // to the target.
        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_DELETE_SOFTWAREMODULES_V1_REQUEST_MAPPING, tenantId, set.getId(), sm.getId()).param(TestCommonConstants.VERSION, sm.getDsmRelation().get(0).getVersion().getId().toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());
    }

    @Test
    @Description("This test verifies the deletion of a assigned Software Module of a Distribution Set can be achieved when that Distribution Set has not been assigned or installed to a target.")
    void deleteWhenDistributionSetNotInUse() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        final Long tenantId = 1L;
        SoftwareModule sm = set.findFirstModuleByType(osType).get();

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_DELETE_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), set.getId(), sm.getId()).param(TestCommonConstants.VERSION, sm.getDsmRelation().get(0).getVersion().getId().toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }


    @Test
    @Description("This test verifies the call of all Software Modules that are assigned to a Distribution Set through the RESTful API.")
    public void getSoftwaremodules() throws Exception {
        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSetWithOutArtifacts("SMTest");
        final Long tenantId = 1L;
        Version swVersion = set.getDistributionSetModules().get(0).getVersion();
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate("file1", FileType.FULL, "description", "123", "SHA_256", 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) set.getModules().stream().toList().get(0)).sourceVersion((JpaVersion) swVersion).targetVersion((JpaVersion) swVersion).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), set.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(set.getModules().size())));

    }

    @Test
    @Description("This test verifies the removal of Software Modules of a Distribution Set through the RESTful API.")
    void unassignSoftwaremoduleFromDistributionSet() throws Exception {

        // Create DistributionSet with three software modules
        final DistributionSet set = testdataFactory.createDistributionSetWithOutArtifacts("Venus");
        final Long tenantId = 1L;
        int amountOfSM = set.getModules().size();
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + TestCommonConstants.SOFTWARE_MODULE, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(amountOfSM)));
        // test the removal of all software modules one by one
        for (final Iterator<SoftwareModule> iter = set.getModules().iterator(); iter.hasNext(); ) {
            final Long smId = iter.next().getId();

            MvcResult result = mvc.perform(get("/management/v1/tenants/1/softwaremodules/" + smId + "/version", String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();

            MockHttpServletResponse res = result.getResponse();
            String contentAsString = res.getContentAsString();

            ObjectMapper objectMapper = new ObjectMapper();
            MgmtGetVersionResponse yourObject = objectMapper.readValue(contentAsString, MgmtGetVersionResponse.class);
            for (VersionResponse version : yourObject.getVersion()) {
                mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + set.getId() + "/softwaremodules/" + smId, String.valueOf(tenantId)).param(TestCommonConstants.VERSION, String.valueOf(version.getId()))).andExpect(status().isOk());
            }
        }
    }

    @Test
    @Description("Ensures that multi target assignment through API is reflected by the repository.")
    void assignMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING_TENANT.replace(TestCommonConstants.TENANT_ID, String.valueOf(tenantId)) + MgmtRestConstants.PATH_SEPARATOR + createdDs.getId() + TestCommonConstants.TARGETS).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk()).andExpect(jsonPath(ASSIGNED, equalTo(knownTargetIds.length - 1))).andExpect(jsonPath(ALREADY_ASSIGNED, equalTo(1))).andExpect(jsonPath(TOTAL, equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent()).as(TARGET_ASSIGNED_WITH_DS).hasSize(5);
    }

    @Test
    @Description("Ensures that targets can be assigned even if the specified controller IDs are in different case (e.g. 'TARGET1' instead of 'target1'.")
    void assignTargetsToDistributionSetIgnoreCase() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        // prepare targets
        final String[] knownTargetIds = new String[]{"64-da-a0-02-43-8b", "Trg1", "target2", "target4"};
        final Long tenantId = 1L;
        final String[] knownTargetIdDifferentCase = new String[]{"64-DA-A0-02-43-8b", "TRG1", "TarGET2", "target4"};
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId, targetId, targetId, testdataFactory.createVehicle(targetId).getId());
        }
        final JSONArray list = new JSONArray();
        for (final String targetId : knownTargetIdDifferentCase) {
            list.put(new JSONObject().put("id", targetId).put("type", TestCommonConstants.FORCED_UPDATE_TYPE));
        }

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk());
        // we just need to make sure that no error 500 is returned
    }

    @Test
    @Description("Ensures that multi target assignment is protected by our getMaxTargetDistributionSetAssignmentsPerManualAssignment quota.")
    void assignMultipleTargetsToDistributionSetUntilQuotaIsExceeded() throws Exception {
        final int maxActions = quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment();
        final List<Target> targets = testdataFactory.createTargets(maxActions + 1);
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;

        final JSONArray payload = new JSONArray();
        targets.forEach(trg -> {
            try {
                payload.put(new JSONObject().put("id", trg.getId()));
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        });

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, ds.getId()).contentType(MediaType.APPLICATION_JSON).content(payload.toString())).andExpect(status().isForbidden());

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId()).getContent()).isEmpty();
    }

    @Test
    @Description("Ensures that the 'max actions per target' quota is enforced if the distribution set assignment of a target is changed permanently")
    void changeDistributionSetAssignmentForTargetUntilQuotaIsExceeded() throws Exception {

        // create one target
        final Target testTarget = testdataFactory.createTarget("trg1");
        final int maxActions = quotaManagement.getMaxActionsPerTarget();

        // create a set of distribution sets
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        final DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");
        final Long tenantId = 1L;

        IntStream.range(0, maxActions).forEach(i ->
                // toggle the distribution set
                assignDistributionSet(i % 2 == 0 ? ds1 : ds2, testTarget)
        );

        // assign our test target to another distribution set and verify that
        // the 'max actions per target' quota is exceeded
        final String json = new JSONArray().put(new JSONObject().put("id", testTarget.getControllerId())).toString();
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, ds3.getId()).contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isForbidden());
    }

    @Test
    @Description("Ensures that offline reported multi target assignment through API is reflected by the repository.")
    void offlineAssignmentOfMultipleTargetsToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5);
        final JSONArray list = new JSONArray();
        final Long tenantId = 1L;
        targets.forEach(target -> {
            try {
                list.put(new JSONObject().put("id", target.getControllerId()));
            } catch (final JSONException e) {
                e.printStackTrace();
            }
        });

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), targets.get(0).getControllerId());

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT + "?offline=true", tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk()).andExpect(jsonPath(ASSIGNED, equalTo(targets.size() - 1))).andExpect(jsonPath(ALREADY_ASSIGNED, equalTo(1))).andExpect(jsonPath(TOTAL, equalTo(targets.size())));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent()).as(TARGET_ASSIGNED_WITH_DS).hasSize(5);

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, createdDs.getId()).getContent()).hasSize(4);
    }

    @Test
    @Description("Assigns multiple targets to distribution set with only maintenance schedule.")
    void assignMultipleTargetsToDistributionSetWithMaintenanceWindowStartOnly() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(0), null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with only maintenance window duration.")
    public void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndOnly() throws Exception {

        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(null, getTestDuration(10), null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with valid maintenance window.")
    void assignMultipleTargetsToDistributionSetWithValidMaintenanceWindow() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(10), getTestDuration(10), getTestTimeZone(), null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with last maintenance window scheduled before current time.")
    void assignMultipleTargetsToDistributionSetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {

        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(getTestSchedule(-30), getTestDuration(5), getTestTimeZone(), null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Assigns multiple targets to distribution set with and without maintenance window.")
    void assignMultipleTargetsToDistributionSetWithAndWithoutMaintenanceWindow() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = new JSONArray();
        final Long tenantId = 1L;
        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId, targetId, targetId, testdataFactory.createVehicle(targetId).getId(),VIN);
            if (Integer.parseInt(targetId) % 2 == 0) {
                list.put(new JSONObject().put("id", Long.valueOf(targetId)).put(MAINTENANCE_WINDOW, new JSONObject().put(TestCommonConstants.SCHEDULE, getTestSchedule(10)).put(TestCommonConstants.DURATION, getTestDuration(5)).put(TestCommonConstants.TIMEZONE, getTestTimeZone())));
            } else {
                list.put(new JSONObject().put("id", Long.valueOf(targetId)));
            }
        }
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk());
    }

    @Test
    @Description("Assigning distribution set to the list of targets with a non-existing one leads to successful assignment of valid targets, while not found targets are silently ignored.")
    void assignNotExistingTargetToDistributionSet() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        final String[] knownTargetIds = new String[]{"1", "2", "3"};
        final JSONArray assignTargetJson = createTargetAndJsonArray(null, null, null, TestCommonConstants.FORCED_UPDATE_TYPE, null, knownTargetIds);
        assignDistributionSet(createdDs.getId(), knownTargetIds[0]);
        final Long tenantId = 1L;
        assignTargetJson.put(new JSONObject().put("id", "notexistingtarget").put("type", TestCommonConstants.FORCED_UPDATE_TYPE));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(assignTargetJson.toString())).andExpect(status().isOk()).andExpect(jsonPath(ALREADY_ASSIGNED, equalTo(1))).andExpect(jsonPath(ASSIGNED, equalTo(2))).andExpect(jsonPath(TOTAL, equalTo(3)));
    }

    private JSONArray createTargetAndJsonArray(
            final String schedule, final String duration, final String timezone,
            final String type, final Boolean confirmationRequired, final String... targetIds) throws Exception {

        final JSONArray result = new JSONArray();

        for (final String targetId : targetIds) {
            createTargetWithVehicle(targetId);

            final JSONObject targetJsonObject = createTargetJsonObject(targetId, type, schedule, duration, timezone, confirmationRequired);
            result.put(targetJsonObject);
        }

        return result;
    }

    private void createTargetWithVehicle(final String targetId) throws Exception {
        testdataFactory.createTarget(targetId, targetId, targetId, testdataFactory.createVehicle(targetId).getId(),VIN);
    }

    private JSONObject createTargetJsonObject(
            final String targetId, final String type, final String schedule,
            final String duration, final String timezone, final Boolean confirmationRequired) throws JSONException {

        final JSONObject targetJsonObject = new JSONObject();
        targetJsonObject.put("id", Long.valueOf(targetId));

        if (type != null) {
            targetJsonObject.put("type", type);
        }

        if (schedule != null || duration != null || timezone != null) {
            targetJsonObject.put(MAINTENANCE_WINDOW, createMaintenanceJsonObject(schedule, duration, timezone));
        }

        if (confirmationRequired != null) {
            targetJsonObject.put("confirmationRequired", confirmationRequired);
        }

        return targetJsonObject;
    }

    private JSONObject createMaintenanceJsonObject(final String schedule, final String duration, final String timezone) throws JSONException {
        final JSONObject maintenanceJsonObject = new JSONObject();

        if (schedule != null) {
            maintenanceJsonObject.put(TestCommonConstants.SCHEDULE, schedule);
        }
        if (duration != null) {
            maintenanceJsonObject.put(TestCommonConstants.DURATION, duration);
        }
        if (timezone != null) {
            maintenanceJsonObject.put(TestCommonConstants.TIMEZONE, timezone);
        }

        return maintenanceJsonObject;
    }


    @Test
    @Description("Ensures that assigned targets of DS are returned as reflected by the repository.")
    void getAssignedTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        testdataFactory.createTarget(knownTargetId);
        final Long tenantId = 1L;
        assignDistributionSet(createdDs.getId(), knownTargetId);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_TARGETS_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId())).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(1))).andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    @Test
    @Description("Ensures that assigned targets of DS are returned as persisted in the repository.")
    void getAssignedTargetsOfDistributionSetIsEmpty() throws Exception {
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_TARGETS_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId())).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(0))).andExpect(jsonPath(TOTAL, equalTo(0)));
    }

    @Test
    @Description("Ensures that installed targets of DS are returned as persisted in the repository.")
    void getInstalledTargetsOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownTargetId = "knownTargetId1";
        final Long tenantId = 1L;
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Target createTarget = testdataFactory.createTarget(knownTargetId);
        // create some dummy targets which are not assigned or installed
        testdataFactory.createTarget(KNOWN_TARGET_ID_2, KNOWN_TARGET_ID_2, KNOWN_TARGET_ID_2, testdataFactory.createVehicle(KNOWN_TARGET_ID_2).getId(),VIN);
        testdataFactory.createTarget(KNOWN_TARGET_ID_3, KNOWN_TARGET_ID_3, KNOWN_TARGET_ID_3, testdataFactory.createVehicle(KNOWN_TARGET_ID_3).getId(),VIN);
        // assign knownTargetId to distribution set
        assignDistributionSet(createdDs.getId(), knownTargetId);
        // make it in install state
        testdataFactory.sendUpdateActionStatusToTargets(Collections.singletonList(createTarget), DeviceActionStatus.FINISHED_NOT_EXECUTED, Collections.singletonList("some message"));

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_INSTALLED_TARGETS_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId())).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(1))).andExpect(jsonPath("$.content[0].controllerId", equalTo(knownTargetId)));
    }

    @Test
    @Description("Ensures that target filters with auto assign DS are returned as persisted in the repository.")
    void getAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        // prepare distribution set
        final String knownFilterName = "a";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(knownFilterName).query(NAME_Y).autoAssignDistributionSet(createdDs.getId()));

        // create some dummy target filter queries
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("b").query(NAME_Y));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("c").query(NAME_Y));

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGN_TARGET_FILTER_V1_REQUEST_MAPPING, 1L, createdDs.getId())).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(1))).andExpect(jsonPath("$.content[0].name", equalTo(knownFilterName)));
    }

    @Test
    @Description("Ensures that an error is returned when the query is invalid.")
    void getAutoAssignTargetFiltersOfDSWithInvalidFilter() throws Exception {
        // prepare distribution set
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String invalidQuery = "unknownField=le=42";
        final Long tenantId = 1L;

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGN_TARGET_FILTER_V1_REQUEST_MAPPING, tenantId, createdDs.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, invalidQuery)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that target filters with auto assign DS are returned according to the query.")
    void getMultipleAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==" + filterNamePrefix + "*";
        final Long tenantId = 1L;

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGN_TARGET_FILTER_V1_REQUEST_MAPPING, tenantId, createdDs.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query)).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(2))).andExpect(jsonPath("$.content[0].name", equalTo(filterNamePrefix + "1"))).andExpect(jsonPath("$.content[1].name", equalTo(filterNamePrefix + "2")));
    }

    @Test
    @Description("Ensures that no target filters are returned according to the non matching query.")
    void getEmptyAutoAssignTargetFiltersOfDistributionSet() throws Exception {
        final Long tenantId = 1L;
        final String filterNamePrefix = "filter-";
        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final String query = "name==doesNotExist";

        prepareTestFilters(filterNamePrefix, createdDs);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGN_TARGET_FILTER_V1_REQUEST_MAPPING, tenantId, createdDs.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, query)).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(0)));
    }

    private void prepareTestFilters(final String filterNamePrefix, final DistributionSet createdDs) {
        // create target filter queries that should be found
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "1").query(NAME_Y).autoAssignDistributionSet(createdDs.getId()));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "2").query(NAME_Y).autoAssignDistributionSet(createdDs.getId()));
        // create some dummy target filter queries
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "b").query(NAME_Y));
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(filterNamePrefix + "c").query(NAME_Y));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging properties.")
    void getDistributionSetsWithoutAddtionalRequestParameters() throws Exception {
        final int sets = 5;
        final Long tenantId = 1L;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(sets))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(sets)));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging results with paging limit parameter.")
    public void getDistributionSetsWithPagingLimitRequestParameter() throws Exception {
        final int sets = 5;
        final int limitSize = 1;
        final Long tenantId = 1L;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Ensures that DS in repository are listed with proper paging results with paging limit and offset parameter.")
    public void getDistributionSetsWithPagingLimitAndOffsetRequestParameter() throws Exception {

        final int sets = 5;
        final int offsetParam = 2;
        final int expectedSize = sets - offsetParam;
        final Long tenantId = 1L;
        createDistributionSetsAlphabetical(sets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam)).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(sets))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(sets))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Ensures that multiple DS requested are listed with expected payload.")
    public void getDistributionSets() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();

        DistributionSet set = testdataFactory.createDistributionSet("one");
        set = distributionSetManagement.update(entityFactory.distributionSet().update(set.getId()).version(TestCommonConstants.ANOTHER_VERSION).requiredMigrationStep(true));

        // load also lazy stuff
        set = distributionSetManagement.getWithDetails(set.getId()).get();

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);
        final Long tenantId = 1L;

        // perform request
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content.[0]._links.self.href", equalTo("http://localhost/management/v1/tenants/1/distributionsets/" + set.getId()))).andExpect(jsonPath("$.content.[0].id", equalTo(set.getId().intValue()))).andExpect(jsonPath("$.content.[0].name", equalTo(set.getName()))).andExpect(jsonPath("$.content.[0].requiredMigrationStep", equalTo(Boolean.TRUE))).andExpect(jsonPath("$.content.[0].description", equalTo(set.getDescription()))).andExpect(jsonPath("$.content.[0].type", equalTo(set.getType().getKey()))).andExpect(jsonPath("$.content.[0].createdBy", equalTo(set.getCreatedBy()))).andExpect(jsonPath("$.content.[0].createdAt", equalTo((int) set.getCreatedAt()))).andExpect(jsonPath("$.content.[0].complete", equalTo(Boolean.TRUE))).andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(set.getLastModifiedBy()))).andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo((int) set.getLastModifiedAt()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + CLOSE_BRACKETS_ID, contains(set.findFirstModuleByType(runtimeType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + CLOSE_BRACKETS_ID, contains(set.findFirstModuleByType(appType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + CLOSE_BRACKETS_ID, contains(getOsModule(set).intValue())));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Ensures that single DS requested by ID is listed with expected payload.")
    void getDistributionSet() throws Exception {
        final DistributionSet set = testdataFactory.createUpdatedDistributionSet();
        final Long tenantId = 1L;

        // perform request
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$._links.self.href", equalTo("http://localhost/management/v1/tenants/1/distributionsets/" + set.getId()))).andExpect(jsonPath("$.id", equalTo(set.getId().intValue()))).andExpect(jsonPath("$.name", equalTo(set.getName()))).andExpect(jsonPath("$.type", equalTo(set.getType().getKey()))).andExpect(jsonPath("$.description", equalTo(set.getDescription()))).andExpect(jsonPath("$.requiredMigrationStep", equalTo(set.isRequiredMigrationStep()))).andExpect(jsonPath("$.createdBy", equalTo(set.getCreatedBy()))).andExpect(jsonPath("$.complete", equalTo(Boolean.TRUE))).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(set.isDeleted()))).andExpect(jsonPath("$.createdAt", equalTo((int) set.getCreatedAt()))).andExpect(jsonPath("$.lastModifiedBy", equalTo(set.getLastModifiedBy()))).andExpect(jsonPath("$.lastModifiedAt", equalTo((int) set.getLastModifiedAt()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE + runtimeType.getKey() + CLOSE_BRACKETS_ID, contains(set.findFirstModuleByType(runtimeType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE + appType.getKey() + CLOSE_BRACKETS_ID, contains(set.findFirstModuleByType(appType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE + osType.getKey() + CLOSE_BRACKETS_ID, contains(getOsModule(set).intValue())));


    }

    @Step
    private MvcResult executeMgmtTargetPost(final DistributionSet one, final DistributionSet two, final DistributionSet three) throws Exception {
        final Long tenantId = 1L;
        return mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).content(JsonBuilder.distributionSets(Arrays.asList(one, two, three))).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("[0]name", equalTo(one.getName()))).andExpect(jsonPath("[0]description", equalTo(one.getDescription()))).andExpect(jsonPath("[0]type", equalTo(standardDsType.getKey()))).andExpect(jsonPath("[0]createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[0]version", equalTo(one.getVersion()))).andExpect(jsonPath("[0]complete", equalTo(Boolean.TRUE))).andExpect(jsonPath("[0]requiredMigrationStep", equalTo(one.isRequiredMigrationStep()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_0 + runtimeType.getKey() + CLOSE_BRACKETS_ID, contains(one.findFirstModuleByType(runtimeType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_0 + appType.getKey() + CLOSE_BRACKETS_ID, contains(one.findFirstModuleByType(appType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_0 + osType.getKey() + CLOSE_BRACKETS_ID, contains(one.findFirstModuleByType(osType).get().getId().intValue()))).andExpect(jsonPath("[1]name", equalTo(two.getName()))).andExpect(jsonPath("[1]description", equalTo(two.getDescription()))).andExpect(jsonPath("[1]complete", equalTo(Boolean.TRUE))).andExpect(jsonPath("[1]type", equalTo(standardDsType.getKey()))).andExpect(jsonPath("[1]createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[1]version", equalTo(two.getVersion()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_1 + runtimeType.getKey() + CLOSE_BRACKETS_ID, contains(two.findFirstModuleByType(runtimeType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_1 + appType.getKey() + CLOSE_BRACKETS_ID, contains(two.findFirstModuleByType(appType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_1 + osType.getKey() + CLOSE_BRACKETS_ID, contains(two.findFirstModuleByType(osType).get().getId().intValue()))).andExpect(jsonPath("[1]requiredMigrationStep", equalTo(two.isRequiredMigrationStep()))).andExpect(jsonPath("[2]name", equalTo(three.getName()))).andExpect(jsonPath("[2]description", equalTo(three.getDescription()))).andExpect(jsonPath("[2]complete", equalTo(Boolean.TRUE))).andExpect(jsonPath("[2]type", equalTo(standardDsType.getKey()))).andExpect(jsonPath("[2]createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[2]version", equalTo(three.getVersion()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_2 + runtimeType.getKey() + CLOSE_BRACKETS_ID, contains(three.findFirstModuleByType(runtimeType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_2 + appType.getKey() + CLOSE_BRACKETS_ID, contains(three.findFirstModuleByType(appType).get().getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULE_TYPE_2 + osType.getKey() + CLOSE_BRACKETS_ID, contains(three.findFirstModuleByType(osType).get().getId().intValue()))).andExpect(jsonPath("[2]requiredMigrationStep", equalTo(three.isRequiredMigrationStep()))).andReturn();
    }

    @Test
    @Description("Ensures that DS deletion request to API is reflected by the repository.")
    void deleteUnassignedistributionSet() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);

        // perform request
        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check repository content
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();
        assertThat(distributionSetManagement.count()).isZero();
    }

    @Test
    @Description("Ensures that DS deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteDistributionSetThatDoesNotExistLeadsToNotFound() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, "1234")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that assigned DS deletion request to API is reflected by the repository by means of deleted flag set.")
    public void deleteAssignedDistributionSet() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        testdataFactory.createTarget("test");
        assignDistributionSet(set.getId(), "test");


        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(true)));

        // check repository content
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();
    }

    @Test
    @Description("Ensures that DS property update request to API is reflected by the repository.")
    public void updateDistributionSet() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;

        assertThat(distributionSetManagement.count()).isEqualTo(1);

        final String body = new JSONObject().put(TestCommonConstants.VERSION, TestCommonConstants.ANOTHER_VERSION).put(REQUIRED_MIGRATION_STEP, true).put("deleted", true).toString();

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId()).content(body).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("$.requiredMigrationStep", equalTo(true))).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));


        final DistributionSet setupdated = distributionSetManagement.get(set.getId()).get();

        assertThat(setupdated.isRequiredMigrationStep()).isTrue();
        assertThat(setupdated.getVersion()).isEqualTo(TestCommonConstants.ANOTHER_VERSION);
        assertThat(setupdated.getName()).isEqualTo(set.getName());
        assertThat(setupdated.isDeleted()).isFalse();
    }

    @Test
    @Description("Ensures that DS property update on requiredMigrationStep fails if DS is assigned to a target.")
    public void updateRequiredMigrationStepFailsIfDistributionSetisInUse() throws Exception {

        // prepare test data
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).isEmpty();

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        assignDistributionSet(set.getId(), testdataFactory.createTarget().getControllerId());

        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, set.getId()).content("{\"version\":\"anotherVersion\",\"requiredMigrationStep\":\"true\"}").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        final DistributionSet setupdated = distributionSetManagement.get(set.getId()).get();

        assertThat(setupdated.isRequiredMigrationStep()).isFalse();
        assertThat(setupdated.getVersion()).isEqualTo(set.getVersion());
        assertThat(setupdated.getName()).isEqualTo(set.getName());
    }

    @Test
    @Description("Ensures that the server reacts properly to invalid requests (URI, Media Type, Methods) with correct reponses.")
    void invalidRequestsOnDistributionSetsResource() throws Exception {
        final Long tenantId = 1L;
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        final List<DistributionSet> sets = new ArrayList<>();
        sets.add(set);

        // SM does not exist
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, "12345678")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, "12345678")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).content("sdfjsdlkjfskdjf".getBytes()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        final DistributionSet missingName = entityFactory.distributionSet().create().build();
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).content(JsonBuilder.distributionSets(Collections.singletonList(missingName))).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        final DistributionSet toLongName = testdataFactory.generateDistributionSet(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1));
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).content(JsonBuilder.distributionSets(Collections.singletonList(toLongName))).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).content(JsonBuilder.distributionSets(sets)).contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Ensures that the metadata creation through API is reflected by the repository.")
    void createMetadata() throws Exception {
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;

        final String knownKey1 = "known.key.1.1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";

        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put(VALUE, knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put(VALUE, knownValue2));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_METADATA_V1_REQUEST_MAPPING, String.valueOf(tenantId), testDS.getId()).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(metaData1.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("[0]key", equalTo(knownKey1))).andExpect(jsonPath("[0]value", equalTo(knownValue1))).andExpect(jsonPath("[1]key", equalTo(knownKey2))).andExpect(jsonPath("[1]value", equalTo(knownValue2)));

        final DistributionSetMetadata metaKey1 = distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), knownKey1).get();
        final DistributionSetMetadata metaKey2 = distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), knownKey2).get();

        assertThat(metaKey1.getValue()).isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put(VALUE, knownValue1 + i));
        }

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_METADATA_V1_REQUEST_MAPPING, String.valueOf(tenantId), testDS.getId()).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(metaData2.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());

        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(distributionSetManagement.findMetaDataByDistributionSetId(PageRequest.of(0, Integer.MAX_VALUE), testDS.getId()).getTotalElements()).isEqualTo(metaData1.length());

    }

    @Test
    @Description("Ensures that a metadata update through API is reflected by the repository.")
    void updateMetadata() throws Exception {
        // prepare and create metadata for update
        final String updateValue = "valueForUpdate";

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(TestCommonConstants.KNOWN_KEY, TestCommonConstants.KNOWN_VALUE));
        final Long tenantId = 1L;

        final JSONObject jsonObject = new JSONObject().put("key", TestCommonConstants.KNOWN_KEY).put(VALUE, updateValue);

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_DELETE_METADATA_V1_REQUEST_MAPPING, tenantId, testDS.getId(), TestCommonConstants.KNOWN_KEY).accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON).content(jsonObject.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("key", equalTo(TestCommonConstants.KNOWN_KEY))).andExpect(jsonPath(VALUE, equalTo(updateValue)));

        final DistributionSetMetadata assertDS = distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), TestCommonConstants.KNOWN_KEY).get();
        assertThat(assertDS.getValue()).isEqualTo(updateValue);

    }

    @Test
    @Description("Ensures that a metadata entry deletion through API is reflected by the repository.")
    void deleteMetadata() throws Exception {
        // prepare and create metadata for deletion
        final Long tenantId = 1L;

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(TestCommonConstants.KNOWN_KEY, TestCommonConstants.KNOWN_VALUE));

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_DELETE_METADATA_V1_REQUEST_MAPPING, String.valueOf(tenantId), testDS.getId(), TestCommonConstants.KNOWN_KEY)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), TestCommonConstants.KNOWN_KEY)).isNotPresent();
    }

    @Test
    @Description("Ensures that DS metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        // prepare and create metadata for deletion
        final Long tenantId = 1L;

        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(TestCommonConstants.KNOWN_KEY, TestCommonConstants.KNOWN_VALUE));

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_DELETE_METADATA_V1_REQUEST_MAPPING + "/XXX", String.valueOf(tenantId), testDS.getId(), TestCommonConstants.KNOWN_KEY)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/1234/metadata/{key}", String.valueOf(tenantId), TestCommonConstants.KNOWN_KEY)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(distributionSetManagement.getMetaDataByDistributionSetId(testDS.getId(), TestCommonConstants.KNOWN_KEY)).isPresent();
    }

    @Test
    @Description("Ensures that a metadata entry selection through API reflectes the repository content.")
    public void getSingleMetadata() throws Exception {

        final Long tenantId = 1L;
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(TestCommonConstants.KNOWN_KEY, TestCommonConstants.KNOWN_VALUE));

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + TestCommonConstants.DS_METADATA_KEY_URL, String.valueOf(tenantId), testDS.getId(), TestCommonConstants.KNOWN_KEY)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("key", equalTo(TestCommonConstants.KNOWN_KEY))).andExpect(jsonPath(VALUE, equalTo(TestCommonConstants.KNOWN_VALUE)));
    }

    @Test
    @Description("Ensures that a metadata entry paged list selection through API reflectes the repository content.")
    void getPagedListofMetadata() throws Exception {
        final Long tenantId = 1L;
        final int totalMetadata = 10;
        final int limitParam = 5;
        final String offsetParam = "0";
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(TestCommonConstants.KNOWN_KEY + index, TestCommonConstants.KNOWN_VALUE + index));
        }

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_CREATE_METADATA_V1_REQUEST_MAPPING + "?offset=" + offsetParam + "&limit=" + limitParam, String.valueOf(tenantId), testDS.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(limitParam))).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(totalMetadata))).andExpect(jsonPath("content[0].key", equalTo("knownKey0"))).andExpect(jsonPath("content[0].value", equalTo("knownValue0")));

    }

    @Test
    @Description("Ensures that a DS search with query parameters returns the expected result.")
    void searchDistributionSetRsql() throws Exception {
        final String dsSuffix = "test";
        final int amount = 10;
        final Long tenantId = 1L;
        testdataFactory.createDistributionSets(dsSuffix, amount);
        testdataFactory.createDistributionSet("DS1test");
        testdataFactory.createDistributionSet("DS2test");

        final String rsqlFindLikeDs1OrDs2 = "name==DS1test,name==DS2test";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "?q=" + rsqlFindLikeDs1OrDs2, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2))).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(2))).andExpect(jsonPath("content[0].name", equalTo("DS1test"))).andExpect(jsonPath("content[1].name", equalTo("DS2test")));

    }

    @Test
    @Description("Ensures that a DS search with complete==true parameter returns only DS that are actually completely filled with mandatory modules.")
    void filterDistributionSetComplete() throws Exception {
        final int amount = 10;
        final Long tenantId = 1L;
        testdataFactory.createDistributionSets(amount);
        distributionSetManagement.create(entityFactory.distributionSet().create().name("incomplete").version("2").type("os"));

        final String rsqlFindLikeDs1OrDs2 = "complete==" + Boolean.TRUE;

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "?q=" + rsqlFindLikeDs1OrDs2, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(10))).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(10)));
    }

    @Test
    @Description("Ensures that a DS assigned target search with controllerId==1 parameter returns only the target with the given ID.")
    void searchDistributionSetAssignedTargetsRsql() throws Exception {
        // prepare distribution set
        final Set<DistributionSet> createDistributionSetsAlphabetical = createDistributionSetsAlphabetical(1);
        final DistributionSet createdDs = createDistributionSetsAlphabetical.iterator().next();
        // prepare targets
        final Collection<String> knownTargetIds = Arrays.asList("1", "2", "3", "4", "5");
        final Long tenantId = 1L;

        for (final String targetId : knownTargetIds) {
            testdataFactory.createTarget(targetId, targetId, targetId, testdataFactory.createVehicle(targetId).getId(), VIN);
        }

        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds.iterator().next());

        final String rsqlFindTargetId1 = "controllerId==1";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_TARGETS_V1_REQUEST_MAPPING_TENANT + "?q=" + rsqlFindTargetId1, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(1))).andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath("content[0].controllerId", equalTo("1")));
    }

    @Test
    @Description("Ensures that a DS metadata filtered query with value==knownValue1 parameter returns only the metadata entries with that value.")
    void searchDistributionSetMetadataRsql() throws Exception {
        final Long tenantId = 1L;
        final int totalMetadata = 10;
        final String knownKeyPrefix = KNOWN_KEY;
        final String knownValuePrefix = KNOWN_VALUE;
        final DistributionSet testDS = testdataFactory.createDistributionSet("one");
        for (int index = 0; index < totalMetadata; index++) {
            createDistributionSetMetadata(testDS.getId(), entityFactory.generateDsMetadata(knownKeyPrefix + index, knownValuePrefix + index));
        }

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_METADATA_V1_REQUEST_MAPPING + "?q=" + rsqlSearchValue1, String.valueOf(tenantId), testDS.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1))).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1"))).andExpect(jsonPath("content[0].value", equalTo("knownValue1")));
    }

    private Set<DistributionSet> createDistributionSetsAlphabetical(final int amount) {
        char character = 'a';
        final Set<DistributionSet> created = Sets.newHashSetWithExpectedSize(amount);
        for (int index = 0; index < amount; index++) {
            final String str = String.valueOf(character);
            created.add(testdataFactory.createDistributionSet(str));
            character++;
        }
        return created;
    }

    @Test
    @Description("Ensures that multi target assignment through API is reflected by the repository in the case of user acceptance required.")
    void assignMultipleTargetsToDistributionSetAsUserAcceptanceRequired() throws Exception {

        final DistributionSet createdDs = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        // prepare targets
        final String[] knownTargetIds = new String[]{"1", "2", "3", "4", "5"};
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, null, knownTargetIds);
        // assign already one target to DS
        assignDistributionSet(createdDs.getId(), knownTargetIds[0], MgmtRolloutUserAcceptanceRequired.YES);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId(), String.valueOf(tenantId), createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk()).andExpect(jsonPath(ASSIGNED, equalTo(knownTargetIds.length - 1))).andExpect(jsonPath(ALREADY_ASSIGNED, equalTo(1))).andExpect(jsonPath(TOTAL, equalTo(knownTargetIds.length)));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, createdDs.getId()).getContent()).as(TARGET_ASSIGNED_WITH_DS).hasSize(5);
    }


    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Ensures that confirmation option is considered in assignment request.")
    void assignTargetsToDistributionSetWithConfirmationOptions(final boolean confirmationFlowActive, final Boolean confirmationRequired) throws Exception {

        final Long tenantId = 1L;
        final DistributionSet createdDs = testdataFactory.createDistributionSet();

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // prepare targets
        final String targetId = "1";
        final JSONArray list = createTargetAndJsonArray(null, null, null, null, confirmationRequired, targetId);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, createdDs.getId()).contentType(MediaType.APPLICATION_JSON).content(list.toString())).andExpect(status().isOk()).andExpect(jsonPath(ASSIGNED, equalTo(1))).andExpect(jsonPath(ALREADY_ASSIGNED, equalTo(0))).andExpect(jsonPath(TOTAL, equalTo(1)));

    }

    @Test
    @Description("A request for assigning a target multiple times results in a Bad Request when multiassignment is disabled.")
    void multiassignmentRequestNotAllowedIfDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long tenantId = 1L;
        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.YES));
        body.put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.NO));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).content(body.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Identical assignments in a single request are removed when multiassignment is disabled.")
    void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long tenantId = 1L;
        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.NO));
        body.put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.NO));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).content(body.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(1)));

    }

    @Test
    @Description("Assigning targets multiple times to a DS in one request works in multiassignment mode.")
    void multiAssignment() throws Exception {
        final List<String> targetIds = testdataFactory.createTargets(2).stream().map(Target::getControllerId).collect(Collectors.toList());
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long tenantId = 1L;
        final JSONArray body = new JSONArray();
        body.put(getAssignmentObject(targetIds.get(0), MgmtRolloutUserAcceptanceRequired.NO, 56));
        body.put(getAssignmentObject(targetIds.get(0), MgmtRolloutUserAcceptanceRequired.NO, 78));
        body.put(getAssignmentObject(targetIds.get(1), MgmtRolloutUserAcceptanceRequired.NO, 67));
        body.put(getAssignmentObject(targetIds.get(1), MgmtRolloutUserAcceptanceRequired.YES, 34));

        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).content(body.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.TOTAL, equalTo(body.length())));

    }

    @Test
    @Description("An assignment request containing a weight is only accepted when weight is valide and multi assignment is on.")
    void weightValidation() throws Exception {
        final String targetId = testdataFactory.createTarget().getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 78;
        final Long tenantId = 1L;

        final JSONArray bodyValide = new JSONArray().put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.NO, weight));
        final JSONArray bodyInvalide = new JSONArray().put(getAssignmentObject(targetId, MgmtRolloutUserAcceptanceRequired.NO, Action.WEIGHT_MIN - 1));


        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).header("session-id", SESSION_ID_HEADER).content(bodyValide.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.debug", notNullValue()));
        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).content(bodyInvalide.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.debug", notNullValue()));

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_CREATE_ASSIGNED_TARGET_V1_REQUEST_MAPPING_TENANT, tenantId, dsId).content(bodyValide.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());


        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("Request to get the count of all Rollouts by status for specific Distribution set")
    public void statisticsForRolloutsCountByStatus() throws Exception {

        final Long tenantId = 1L;
        List<Target> targets = testdataFactory.createTargets(TARGETS, 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");


        Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds1, targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        createRolloutWithDependencies("rollout1", ds1, targets);
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/rollouts", String.valueOf(tenantId), ds1.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("totalRolloutsPerStatus.DRAFT", equalTo(1))).andExpect(jsonPath("totalRolloutsPerStatus.RUNNING", equalTo(1))).andExpect(jsonPath("totalRolloutsPerStatus.total", equalTo(2))).andExpect(jsonPath(TOTAL_ACTIONS_PER_STATUS).doesNotExist()).andExpect(jsonPath(TOTAL_AUTO_ASSIGNMENTS).doesNotExist());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/{ds}/statistics/rollouts", String.valueOf(tenantId), ds2.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL_ROLLOUTS_PER_STATUS).doesNotExist()).andExpect(jsonPath(TOTAL_ACTIONS_PER_STATUS).doesNotExist()).andExpect(jsonPath(TOTAL_AUTO_ASSIGNMENTS).doesNotExist());
    }

    @Test
    @Description("Request to get the count of all Actions by status for specific Distribution set")
    void statisticsForActionsCountByStatus() throws Exception {
        final Long tenantId = 1L;
        List<Target> targets = testdataFactory.createTargets(TARGETS, 4);

        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");


        Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds1, targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ACTIONS_V1_REQUEST_MAPPING, String.valueOf(tenantId), ds1.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("totalActionsPerStatus.RUNNING", equalTo(4))).andExpect(jsonPath("totalActionsPerStatus.total", equalTo(4))).andExpect(jsonPath("totalRolloutsPerStatus").doesNotExist()).andExpect(jsonPath("totalAutoAssignments").doesNotExist());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ACTIONS_V1_REQUEST_MAPPING, String.valueOf(tenantId), ds2.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("totalRolloutsPerStatus").doesNotExist()).andExpect(jsonPath("totalActionsPerStatus").doesNotExist()).andExpect(jsonPath("totalAutoAssignments").doesNotExist());

    }

    @Test
    @Description("Request to get the count of all Auto Assignments for specific Distribution set")
    void statisticsForAutoAssignmentsCount() throws Exception {
        final Long tenantId = 1L;
        testdataFactory.createTargets(TARGETS, 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("test filter 1").autoAssignDistributionSet(ds1.getId()).query(QUERY));

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("test filter 2").autoAssignDistributionSet(ds1.getId()).query(QUERY));
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGNMENTS_V1_REQUEST_MAPPING, tenantId, ds1.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("totalAutoAssignments", equalTo(2))).andExpect(jsonPath("totalRolloutsPerStatus").doesNotExist()).andExpect(jsonPath("totalActionsPerStatus").doesNotExist());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGNMENTS_V1_REQUEST_MAPPING, tenantId, ds2.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("totalRolloutsPerStatus").doesNotExist()).andExpect(jsonPath("totalActionsPerStatus").doesNotExist()).andExpect(jsonPath("totalAutoAssignments").doesNotExist());

    }

    @Test
    @Description("Request to get full Statistics for specific Distribution set")
    void statisticsForDistributionSet() throws Exception {
        final Long tenantId = 1L;
        List<Target> targets = testdataFactory.createTargets(TARGETS, 4);
        testdataFactory.createTargets("autoAssignments", 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("test filter 1").autoAssignDistributionSet(ds1.getId()).query(QUERY));


        Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds1, targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
                 //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_STATISTICS_V1_REQUEST_MAPPING, String.valueOf(tenantId), ds1.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL_AUTO_ASSIGNMENTS, equalTo(1))).andExpect(jsonPath("totalActionsPerStatus.RUNNING", equalTo(4))).andExpect(jsonPath("totalActionsPerStatus.total", equalTo(4))).andExpect(jsonPath("totalRolloutsPerStatus.RUNNING", equalTo(1))).andExpect(jsonPath("totalRolloutsPerStatus.total", equalTo(1)));

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_AUTO_ASSIGNMENTS_V1_REQUEST_MAPPING, String.valueOf(tenantId), ds2.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL_ROLLOUTS_PER_STATUS).doesNotExist()).andExpect(jsonPath(TOTAL_ACTIONS_PER_STATUS).doesNotExist()).andExpect(jsonPath(TOTAL_AUTO_ASSIGNMENTS).doesNotExist());
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments")
    void invalidateDistributionSet() throws Exception {
        final Long tenantId = 1L;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, "invalidateDS");
        assignDistributionSet(distributionSet, targets);
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("invalidateDistributionSet").query("name==*").autoAssignDistributionSet(distributionSet));


        Rollout rollout = createRolloutWithDependencies(ROLLOUT, distributionSet, targets);
        rolloutHandler.handleAll();
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("actionCancelationType", "soft");
        jsonObject.put("cancelRollouts", true);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, String.valueOf(tenantId), distributionSet.getId()).content(jsonObject.toString()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

        assertThat(targetFilterQueryManagement.get(targetFilterQuery.getId()).get().getAutoAssignDistributionSet()).isNull();
        assertThat(rolloutManagement.get(rollout.getId()).get().getStatus()).isIn(RolloutStatus.CANCELING, RolloutStatus.FINISHED);
        for (final Target target : targets) {
            assertThat(targetManagement.get(target.getId()).get().getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100)).getNumberOfElements()).isEqualTo(1);
            assertThat(deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100)).getContent().get(0).getStatus()).isEqualTo(DeviceActionStatus.CANCELING);
        }
    }

    @Test
    @Description("Throw exception if software version id is not present")
    void assignSoftwaremoduleToDistributionSet_SoftwareVersionIdNull() throws Exception {
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        final Long tenantId = 1L;
        // create Software Modules
        final List<Long> smIDs = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(), testdataFactory.createSoftwareModuleApp().getId());
        assignment1.setSoftwareVersionTargetId(null);
        assignment1.setId(smIDs.get(0));
        softwareModuleAssignment.add(assignment1);

        String jsonObject = new ObjectMapper().writeValueAsString(softwareModuleAssignment);
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonObject)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Throw exception if software module id is null")
    void assignSoftwaremoduleToDistributionSet_moduleIdNull() throws Exception {
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<MgmtSoftwareModuleAssignments>();

        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        final Long tenantId = 1L;
        assignment1.setSoftwareVersionTargetId("22");
        assignment1.setId(null);
        softwareModuleAssignment.add(assignment1);

        String jsonObject = new ObjectMapper().writeValueAsString(softwareModuleAssignment);
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));


        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonObject)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Throw exception if target version blank")
    public void assignSoftwaremoduleToDistributionSet_versionIdBlank() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        // create Software Modules
        final List<Long> smIDs = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(), testdataFactory.createSoftwareModuleApp().getId());
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId("");
        assignment1.setId(smIDs.get(0));
        softwareModuleAssignment.add(assignment1);

        String jsonObject = new ObjectMapper().writeValueAsString(softwareModuleAssignment);
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonObject)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("This test verifies the assignment of One Software Module and One Version to a Distribution Set through the RESTful API.")
    public void assignOneSoftwaremoduleOneSwVersionToDistributionSetWithoutEcoModel() throws Exception {

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);
        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + disSet.getId() + TestCommonConstants.SOFTWARE_MODULE, String.valueOf(tenantId))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));
        // create Software Modules
        final List<Version> version = Collections.singletonList(artifactSoftwareModuleAssociationList.get(0).getTargetVersion());
        String jsonBody = JsonBuilder.createSwModuleSwVersionJsonArray(version).toString();

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    /**
     * @deprecated Multi-assignment of different target versions of the same module to a distribution is deprecated.
     * This test verifies the assignment of one software module with multiple versions to a distribution set through the RESTful API.
     */
    @Deprecated
    /* Multi assignment of different target version of same module to a distribution is deprecated. */
    @Test
    @Description("This test verifies the assignment of one Software Modules and Multi Version to a Distribution Set through the RESTful API.")
    public void assignOneSoftwaremoduleMultiSwVersionToDistributionSet() throws Exception {

        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));
        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);
        final List<Version> version = Arrays.asList(artifactSoftwareModuleAssociationList.get(0).getTargetVersion(), artifactSoftwareModuleAssociationList.get(1).getTargetVersion());
        String jsonBody = JsonBuilder.createSwModuleSwVersionJsonArray(version).toString();

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("This test verifies the assignment of Multi Software Modules and Multi Version to a Distribution Set through the RESTful API.")
    public void assignMultiSoftwaremoduleMultiSwVersionToDistributionSetWithoutEcuModel() throws Exception {

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);

        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));
        // create Software Modules

        final List<Version> versions = Arrays.asList(artifactSoftwareModuleAssociationList.get(0).getTargetVersion(), artifactSoftwareModuleAssociationList.get(1).getTargetVersion());
        String jsonBody = JsonBuilder.createSwModuleSwVersionJsonArray(versions).toString();

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Throw exception if software version is not attached to any software modules artifacts.")
    public void assignSmSwVersionToDistributionSetThrowsNotFoundWhenSwVersionNotAttachedToSm() throws Exception {

        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);

        // create Software Modules
        Long smId = testdataFactory.createSoftwareModuleOs().getId();

        Version version = testdataFactory.createVersion(smId, "TestVersion");

        String jsonBody = "[{\"id\":" + smId + ",\"softwareVersionTargetId\":" + version.getId() + "}]";

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Throw exception if software version & software module not found.")
    void assignSmSwVersionToDistributionSetThrowsNotFoundWhenSwVersionAndToSmNotFound() throws Exception {
        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));
        // create Software Modules

        String jsonBody = "[{\"id\":16,\"softwareVersionTargetId\":38}]";

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("Throw exception if software version & software module already assigned to Distribution set.")
    public void assignSmSwVersionToDistributionSetAlreadyAssignedWithoutEcuModel() throws Exception {

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);
        final Long tenantId = 1L;
        // create DisSet
        final DistributionSet disSet = testdataFactory.createDistributionSetWithNoSoftwareModules(JUPITER, VERSION);
        // Test if size is 0
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TestCommonConstants.JSON_PATH_FOR_SIZE, equalTo(disSet.getModules().size())));
        // create Software Modules
        final List<Version> versions = Collections.singletonList(artifactSoftwareModuleAssociationList.get(0).getTargetVersion());
        String jsonBody = JsonBuilder.createSwModuleSwVersionJsonArray(versions).toString();

        // post assignment
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId(), String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // post assignment to check it is already assigned
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ASSIGNED_SOFTWAREMODULES_V1_REQUEST_MAPPING, String.valueOf(tenantId), disSet.getId()).contentType(MediaType.APPLICATION_JSON).content(jsonBody)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Throw exception if target version is not assigned with artifact")
    void givenModulesTargetVersionNotAssigntoArtifactWhenCreateDSThenThrowException() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId(artifactSoftwareModuleAssociationList.get(1).getSourceVersion().getId().toString());
        assignment1.setId(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId());
        softwareModuleAssignment.add(assignment1);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(createRequestForDistSet(softwareModuleAssignment))))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("No artifact association found for software module " + artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId() + " with target version " + artifactSoftwareModuleAssociationList.get(1).getSourceVersion().getId())));
    }

    @Test
    @Description("Creates DS when no modules are assigned to it")
    void givenEmptyModulesWhenCreateDSThenSuccess() throws Exception {
        final Long tenantId = 1L;
        MgmtDistributionSetRequestBodyPost requestBody = new MgmtDistributionSetRequestBodyPost();
        requestBody.setRequiredMigrationStep(false);
        requestBody.setName("DistList2");
        requestBody.setType("os");
        requestBody.setDescription("Test");
        requestBody.setVersion("1.0");
        List<MgmtDistributionSetRequestBodyPost> requestBodyList = List.of(requestBody);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(requestBodyList))).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Throw error when target version is blank with spaces")
    void givenModulesNoTargetVersionWhenCreateDSThenThrowException() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        SoftwareModule sm1 = testdataFactory.createSoftwareModuleOs();
        SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();

        final List<Version> version = Arrays.asList(testdataFactory.createVersion(sm1.getId(), "smV1", 12), testdataFactory.createVersion(sm2.getId(), "smV2", 13));
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId(" ");
        assignment1.setId(sm1.getId());
        softwareModuleAssignment.add(assignment1);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(createRequestForDistSet(softwareModuleAssignment)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Software Module Id / Software Version Target Id cannot be null")));
    }

    @Test
    @Description("Throw error when target version is null ")
    void givenModulesTargetVersionNullWhenCreateDSThenThrowException() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        SoftwareModule sm1 = testdataFactory.createSoftwareModuleOs();
        SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();

        final List<Version> version = Arrays.asList(testdataFactory.createVersion(sm1.getId(), "smV1", 12), testdataFactory.createVersion(sm2.getId(), "smV2", 13));
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId(null);
        assignment1.setId(sm1.getId());
        softwareModuleAssignment.add(assignment1);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(createRequestForDistSet(softwareModuleAssignment)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("The given request body is not well formed")));
    }

    @Test
    @Description("Create distribution set with software modules")
    void givenModulesWhenCreateDSThenSuccess() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(osType, NAME + testdataFactory.getRandomInt(), "1", 2, format, swInstallerType);
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        MgmtSoftwareModuleAssignments assignment2 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId(artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId().toString());
        assignment1.setId(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId());
        softwareModuleAssignment.add(assignment1);
        assignment2.setSoftwareVersionTargetId(artifactSoftwareModuleAssociationList.get(1).getTargetVersion().getId().toString());
        assignment2.setId(artifactSoftwareModuleAssociationList.get(1).getSoftwareModule().getId());
        softwareModuleAssignment.add(assignment1);
        softwareModuleAssignment.add(assignment2);
        MgmtDistributionSetRequestBodyPost requestBody = new MgmtDistributionSetRequestBodyPost();
        requestBody.setRequiredMigrationStep(false);
        requestBody.setName("DistList3");
        requestBody.setType("os");
        requestBody.setDescription("Create DS with software modules list");
        requestBody.setVersion("3.0");
        requestBody.setModules(List.of(assignment1, assignment2));
        List<MgmtDistributionSetRequestBodyPost> requestBodyList = List.of(requestBody);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(requestBodyList))).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Error when software module id is null")
    void givenModuleIdNullWhenCreateDSThenThrowException() throws Exception {
        final Long tenantId = 1L;
        List<MgmtSoftwareModuleAssignments> softwareModuleAssignment = new ArrayList<>();
        SoftwareModule sm1 = testdataFactory.createSoftwareModuleOs();
        SoftwareModule sm2 = testdataFactory.createSoftwareModuleApp();

        final List<Version> version = Arrays.asList(testdataFactory.createVersion(sm1.getId(), "smV1", 12), testdataFactory.createVersion(sm2.getId(), "smV2", 13));
        MgmtSoftwareModuleAssignments assignment1 = new MgmtSoftwareModuleAssignments();
        assignment1.setSoftwareVersionTargetId(version.get(0).getId().toString());
        assignment1.setId(null);
        softwareModuleAssignment.add(assignment1);
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING, String.valueOf(tenantId)).contentType(MediaType.APPLICATION_JSON).content(new ObjectMapper().writeValueAsString(createRequestForDistSet(softwareModuleAssignment)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Software Module Id / Software Version Target Id cannot be null")));
    }


    private List<MgmtDistributionSetRequestBodyPost> createRequestForDistSet(List<MgmtSoftwareModuleAssignments> softwareModuleAssignments) {
        MgmtDistributionSetRequestBodyPost requestBody = new MgmtDistributionSetRequestBodyPost();
        requestBody.setRequiredMigrationStep(false);
        requestBody.setName("DistList1");
        requestBody.setType("os");
        requestBody.setModules(Collections.singletonList(softwareModuleAssignments.get(0)));
        return List.of(requestBody);
    }

    private List<ArtifactSoftwareModuleAssociation> createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
                                                                                      final String version, final int numberArtifacts, final SoftwareModuleFormat format,
                                                                                      final SoftwareInstallerType swInstallerType) {
        Path tempFile = tempDir.resolve("test_fileName");

        SoftwareModule softwareModule1 = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name).version("1").description("description of artifact " + name).format(format)
                .swInstallerType(swInstallerType));

        SoftwareModule softwareModule2 = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name+"2").version("2").description("description of artifact2 " + name).format(format)
                .swInstallerType(swInstallerType));

        Version swVersion1 = testdataFactory.createVersion(softwareModule1.getId(), version + numberArtifacts);
        Version swVersion2 = testdataFactory.createVersion(softwareModule2.getId(), version + numberArtifacts);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION + "1", "1231", "1231");
        Artifacts file2 = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION + "2", "123", "123");

        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) swVersion1)
                .targetVersion((JpaVersion) swVersion1)
                .build();

        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file2)
                .softwareModule((JpaSoftwareModule) softwareModule2)
                .sourceVersion((JpaVersion) swVersion2)
                .targetVersion((JpaVersion) swVersion2)
                .build();

        List<Artifacts> artifacts = new ArrayList<>();
        artifacts.add(file);
        artifacts.add(file2);

        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association1);
        associationsSet.add(association2);

        artifactsManagement.createOrUpdateArtifactSoftwareModuleAssociation(associationsSet);

        return associationsSet.stream().toList();
    }

    @Test
    @Description("Ensures that full update is allowed when all associated rollouts are in CREATING, DRAFT, or DELETING status.")
    void updateDistributionSetWithAllowedStatuses() throws Exception {

        final Long tenantId = 1L;
        testdataFactory.createTargets(TARGETS, 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");

        final String body = new JSONObject()
                .put("name", UPDATED_NAME)
                .put(DESCRIPTION, UPDATED_DESCRIPTION)
                .put("version", "2.0")
                .put(REQUIRED_MIGRATION_STEP, true)
                .toString();

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, ds1.getId()).content(body).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).
                andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).
                andExpect(jsonPath(JSON_PATH_REQUIRED_MIGRATION_STEP, equalTo(true))).
                andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));

        final DistributionSet updatedSet = distributionSetManagement.get(ds1.getId()).get();
        assertThat(updatedSet.getName()).isEqualTo(UPDATED_NAME);
        assertThat(updatedSet.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
        assertThat(updatedSet.getVersion()).isEqualTo("2.0");
        assertThat(updatedSet.isRequiredMigrationStep()).isTrue();
    }


    @Test
    @Description("Test limited update of DistributionSet when rollouts are in non-allowed statuses like RUNNING")
    void updateDistributionSetWithRolloutsInRestrictedStatuses() throws Exception {

        final Long tenantId = 1L;
        List<Target> targets = testdataFactory.createTargets(TARGETS, 4);
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");


        Rollout rollout = createRolloutWithDependencies(ROLLOUT, ds1, targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final String body = new JSONObject()
                .put("name", UPDATED_NAME)
                .put(DESCRIPTION, UPDATED_DESCRIPTION)
                .put("version", "2.0")
                .put(REQUIRED_MIGRATION_STEP, false)
                .toString();

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, tenantId, ds1.getId())
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));

        final DistributionSet updatedSet = distributionSetManagement.get(ds1.getId()).get();
        assertThat(updatedSet.getName()).isEqualTo(ds1.getName());
        assertThat(updatedSet.getVersion()).isEqualTo(ds1.getVersion());
        assertThat(updatedSet.getDescription()).isEqualTo(UPDATED_DESCRIPTION);
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments")
    void givenCaseInsensitiveCancellationTypeWhenInvalidatingDsThenReturnSuccess() throws Exception {
        final Long tenantId = 1L;
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, "invalidateDS");
        assignDistributionSet(distributionSet, targets);

        createRolloutWithDependencies(ROLLOUT, distributionSet, targets);
        rolloutHandler.handleAll();
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("actionCancelationType", "SoFt"); // Providing mixed-case action cancelation type
        jsonObject.put("cancelRollouts", true);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, String.valueOf(tenantId), distributionSet.getId())
                .content(jsonObject.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        final DistributionSet distributionSet1 = testdataFactory.createDistributionSet();
        jsonObject.put("actionCancelationType", "force"); // Providing lowercase action cancelation type
        jsonObject.put("cancelRollouts", true);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, String.valueOf(tenantId), distributionSet1.getId())
                .content(jsonObject.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        final DistributionSet distributionSet2 = testdataFactory.createDistributionSet();
        jsonObject.put("actionCancelationType", "NONE"); // Providing uppercase action cancelation type
        jsonObject.put("cancelRollouts", true);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSET_ONE_V1_REQUEST_MAPPING, String.valueOf(tenantId), distributionSet2.getId())
                .content(jsonObject.toString())
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }


}
