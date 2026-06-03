/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.s3.S3MultipartFileUpload;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import software.amazon.awssdk.core.SdkResponse;
import software.amazon.awssdk.http.SdkHttpResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;

import java.io.InputStream;
import java.time.Duration;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cosmos.models.mgmt.MgmtRestConstants.DEPLOYMENT_LOG_V1_REQUEST_MAPPING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the {@link MgmtActionRestApi}.
 */
@Feature("Component Tests - Management API")
@Story("Action Resource")
class MgmtActionResourceTest extends AbstractManagementApiIntegrationTest {

    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    private static final String JSON_PATH_ROOT = "$";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String LOCALHOST = "http://localhost";
    private static final String TENANT_ID = "{tenantId}";
    private static final String KNOWN_TARGET_ID = "knownTargetId";
    private static final String TOTAL = "total";
    private static final String PENDING = "pending";
    private static final String CONTENT_STATUS = "content[0].status";
    private static final String RUNNING = "running";
    private static final String DISTRIBUTION_NAME = "content.[0]._links.distributionset.name";
    private static final String TARGET_NAME = "content.[0]._links.target.name";
    private static final String TARGET_ID = "targetId";
    private static final String ID_ASC = "ID:ASC";
    private static final String UPDATE = "update";
    private static final String DETAIL_STATUS = "content.[1].detailStatus";
    private static final String CONTENT_ID = "content.[0].id";
    private static final String CONTENT_TYPE = "content.[0].type";
    private static final String CANCEL = "cancel";
    private static final String STATUS = "content.[0].status";
    private static final String HREF = "content.[0]._links.self.href";
    private static final String CONTENT_DETAIL_STATUS = "content.[0].detailStatus";
    private static final String TARGET = "3W2234F45240S";
    private static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;
    private static final String JSON_PATH_ACTION_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String ROLLOUT_1 = "rollout1";
    private static final String ROLLOUT_NAME = "rollout";
    private static final Long TARGET_NOT_FOUND = 2000L;
    private static final Long ACTION_NOT_FOUND = 2000L;

    private static ClientAndServer mockServer;
    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;
    @MockBean
    private S3Client s3Client;
    @MockBean
    private S3MultipartFileUpload s3MultipartFileUpload;

    private static String generateActionLink(final String targetId, final Long actionId, final Long tenantId) {
        return LOCALHOST + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))
                + MgmtRestConstants.PATH_SEPARATOR + targetId + MgmtRestConstants.PATH_SEPARATOR + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private static String generateActionDeploymentLogsLink(final String controllerId, final Long actionId, final Long tenantId) {
        return LOCALHOST + DEPLOYMENT_LOG_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)).replace("{controllerId}", String.valueOf(controllerId))
                .replace("{actionId}", String.valueOf(actionId));
    }

    private static String generateTargetLink(final String targetId, final Long tenantId) {
        return LOCALHOST + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + MgmtRestConstants.PATH_SEPARATOR + targetId;
    }

    private static String generateDistributionSetLink(final Action action, final Long tenantId) {
        return LOCALHOST + MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/"
                + action.getDistributionSet().getId();
    }

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
        mockServer
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
    }

    @AfterEach
    void tearDown() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_tag", "sp_target", "sp_vehicle_model",
                "sp_action", "sp_rollout", "sp_distribution_set", "sp_artifact_software_module",
                "sp_software_versions", "sp_base_software_module");
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @BeforeEach
    void initSetup() throws Exception {
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Verifies that actions can be filtered based on rollout fields.")
    void filterActionsByRollout() throws Exception {

        // prepare test
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final Target target0 = testdataFactory.createTarget("t0");
        final Long tenantId = 1L;
        // manual assignment
        assignDistributionSet(ds, Collections.singletonList(target0));

        // rollout
        final Target target1 = testdataFactory.createTarget("t1", "t1", "t1", testdataFactory.createVehicle("X250").getId(),"vin1");

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target1));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final String rsqlRolloutName = "rollout.name==" + rollout.getName();
        final String rsqlRolloutId = "rollout.id==" + rollout.getId();

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)) + "?q=" + rsqlRolloutName)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL, equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath(TARGET_NAME, equalTo(target1.getName()))).andExpect(jsonPath(
                        DISTRIBUTION_NAME, equalTo(ds.getName() + ":" + ds.getVersion())));

        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)) + "?q=" + rsqlRolloutId)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_REPRESENTATION_MODE, MgmtRepresentationMode.FULL.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(TOTAL, equalTo(1)))
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath(TARGET_NAME, equalTo(target1.getName()))).andExpect(jsonPath(
                        DISTRIBUTION_NAME, equalTo(ds.getName() + ":" + ds.getVersion())));
    }

    @Test
    @Description("Verifies that a action is returned with deployment logs link.")
    void getActionByIdWithDeploymentLogs() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);
        String fileName = "test-log-" + action.getId();
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(S3FileUpload.class), any(InputStream.class));
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200) // Successful response status
                .build();
        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);
        testdataFactory.createDeploymentLog(fileName, action);

        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given a target with both active and inactive actions, " +
            "when fetching deployment logs, then only logs for the active action are returned " +
            "and not for the inactive one.")
    void givenTargetWithActiveAndInactiveActionsWhenFetchingDeploymentLogsThenReturnLogsOnlyForActiveAction() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");
        Target target1 = testdataFactory.createTarget("TestDeploymentLogVIN2");

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target, target1));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);
        final JpaAction action1 = (JpaAction) deploymentManagement.findActiveActionsByTarget(PAGE, target1.getControllerId())
                .getContent().get(0);

        // Cancel the action for target1, making it inactive
        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, 1L, rollout.getId(), action1.getTarget().getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        // Mock S3 interactions for deployment log creation
        String fileName = "test-log-" + action.getId();
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(S3FileUpload.class), any(InputStream.class));
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200)
                .build();
        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);

        testdataFactory.createDeploymentLog(fileName, action);

        // Assert: Only the active action returns logs, the inactive one returns 404
        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", equalTo(1)))
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].file_name", equalTo(fileName)));

        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, target1.getControllerId(), action1.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that the get request for actions returns an empty collection if no assignments have been done yet.")
    void getActionsWithEmptyResult() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId))))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath(TOTAL, equalTo(0)));

    }

    @Test
    @Description("Verifies that the actions resource is read-only.")
    void invalidRequestsOnActionResource() throws Exception {
        final Long tenantId = 1L;

        // not allowed methods
        mvc.perform(post(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(delete(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Verifies that NOT_FOUND is returned when there is no such action.")
    void requestActionThatDoesNotExistsLeadsToNotFound() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + 101)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that the get request for actions deployment logs returns an empty collection if no logs found.")
    void givenActionIdWhenGetActionsDeploymentLogsThenEmptyCollection() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget(TARGET, TARGET, "205", testdataFactory.createVehicle("X250").getId(),TARGET);
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, savedTarget.getControllerId(), savedAction.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0)))
                .andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath(TOTAL, equalTo(0)));

    }

    @Test
    @Description("Verifies that Not Found is returned when the target does not exist.")
    void givenNonExistingTarget_whenGettingDeploymentLogs_thenReturnsNotFound() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget(TARGET, TARGET, "205", testdataFactory.createVehicle("X250").getId(),TARGET);
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, TARGET_NOT_FOUND, savedAction.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that Not Found is returned when the action does not exist.")
    void givenNonExistingAction_whenGettingDeploymentLogs_thenReturnsNotFound() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget(TARGET, TARGET, "205", testdataFactory.createVehicle("X250").getId(),TARGET);
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, savedTarget.getControllerId(), ACTION_NOT_FOUND))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that Not Found is returned when both target and action do not exist.")
    void givenNonExistingTargetAndAction_whenGettingDeploymentLogs_thenReturnsNotFound() throws Exception {
        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, TARGET_NOT_FOUND, ACTION_NOT_FOUND))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that the get request with invalid action for deployment logs returns not found exception.")
    void givenInvalidActionIdWhenGetActionsDeploymentLogsThenNotFound() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget(TARGET, TARGET, "205", testdataFactory.createVehicle("X250").getId(),TARGET);
        getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        // Invalid action ID throws not found exception.
        mvc.perform(get(MgmtRestConstants.ACTION_V1_REQUEST_MAPPING_TENANT
                        .replace(TENANT_ID, "1") + "/1000" + "/" + MgmtRestConstants.DEPLOYMENT_LOGS))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verifies that the get request for actions deployment logs returns deployment logs.")
    void givenActionIdWhenGetActionsDeploymentLogsThenDeploymentLogs() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        String fileName = "test-log-" + action.getId();
        when(s3MultipartFileUpload.uploadFileToS3(any(), any(S3FileUpload.class))).thenReturn("1234");
        SdkHttpResponse sdkHttpResponse = SdkHttpResponse.builder()
                .statusCode(200) // Successful response status
                .build();
        SdkResponse sdkResponse = DeleteObjectResponse.builder().sdkHttpResponse(sdkHttpResponse).build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn((DeleteObjectResponse) sdkResponse);
        DeploymentLog deploymentLog = testdataFactory.createDeploymentLog(fileName, action);
        mvc.perform(get(MgmtRestConstants.ACTION_DEPLOYMENT_LOGS_MAPPING_TENANT, 1, target.getControllerId(), action.getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath(TOTAL, equalTo(1)))
                .andExpect(jsonPath("$.content[0].file_name", equalTo(fileName)))
                .andExpect(jsonPath("$.content[0].file_size", equalTo(Math.toIntExact(deploymentLog.getFileSize()))))
                .andExpect(jsonPath("$.content[0].sequence", equalTo(Math.toIntExact(deploymentLog.getSequence()))))
                .andExpect(jsonPath("$.content[0].byte_size").doesNotExist())
                .andExpect(jsonPath("$.content[0].byte_range").doesNotExist())
                .andExpect(jsonPath("$.content[0].is_last_chunk").doesNotExist())
                .andExpect(jsonPath("$.content[0].is_last_file", equalTo(deploymentLog.getIsLastFile())))
                .andExpect(jsonPath("$.content[0].sha256_hash", equalTo(deploymentLog.getSha256Hash())))
                .andExpect(jsonPath("$.content[0].file_path", equalTo( "/" + action.getTenant().toUpperCase() + "/" + action.getRollout().getId() + "/"
                        + action.getId() + "/")))
                .andExpect(jsonPath("$.content[0]._links.download.href",
                        equalTo(deploymentLogDownloadLink(target.getControllerId(), action.getId(), deploymentLog.getId()))));
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride(final String knownTargetId) {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(knownTargetId, null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(final String knownTargetId,
                                                                                          final String schedule, final String duration, final String timezone) {
        final Target target = testdataFactory.createTarget(knownTargetId);

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(target))
                    .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofMillis(100)).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets = assignDistributionSetWithMaintenanceWindow(one.getId(),
                    target.getControllerId(), schedule, duration, timezone).getAssignedEntity().stream()
                    .map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 10ms to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofMillis(100)).atLeast(5, TimeUnit.MILLISECONDS)
                    .pollInterval(10, TimeUnit.MILLISECONDS)
                    .until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule,
                    duration, timezone);
        }

        // two updates, one cancellation
        final List<Action> actions = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE)
                .getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }


    @Test
    @Description("Given multiple actions are assigned to a target, "
            + "When fetching all actions for that target via the API, "
            + "Then only the active actions should be returned in the response, "
            + "and after canceling each action, the count decreases accordingly.")
    void givenMultipleActionsWhenFetchingAllActionsThenOnlyActiveActionsAreReturned() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target target = testdataFactory.createTarget("TestDeploymentLogVIN1");

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Initial fetch: all actions should be active
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, 1, target.getControllerId())
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        List<JpaAction> actions = actionRepository.findByTarget(target, true);
        JpaAction action1 = actions.get(0);
        // Cancel the first action and verify only one active remains
        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, 1L, rollout.getId(), action1.getTarget().getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, 1, target.getControllerId()).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(0)));
    }


}
