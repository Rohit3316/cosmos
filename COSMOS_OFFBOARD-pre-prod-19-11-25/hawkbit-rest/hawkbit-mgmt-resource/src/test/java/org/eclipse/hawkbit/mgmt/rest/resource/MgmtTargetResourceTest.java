/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.JsonNode;
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.hamcrest.Matchers;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cosmos.models.mgmt.MgmtRestConstants.TENANT_API_V1_BASE_URL;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 */
@Feature("Component Tests - Management API")
@Story("Target Resource")
class MgmtTargetResourceTest extends AbstractManagementApiIntegrationTest {

    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    public static final String KNOWN_TARGET_ID_1 = "knownTargetId1";
    public static final String JSON_PATH_MODULES_TYPE = "$.modules.[?(@.type=='";
    public static final String LOCALHOST_URL = "http://localhost";
    private static final String TARGET_DESCRIPTION_TEST = "created in test";
    private static final String JSON_PATH_ROOT = "$";
    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_CONTROLLERID = ".controllerId";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_DESCRIPTION = ".description";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;
    private static final String JSON_PATH_FIELD_LAST_REQUEST_AT = ".lastControllerRequestAt";
    private static final String JSON_PATH_FIELD_TARGET_TYPE = ".targetType";
    private static final String JSON_PATH_ROOT_FIELD_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ROOT_FIELD_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_CONTROLLERID = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTROLLERID;
    private static final String JSON_PATH_ROOT_FIELD_DESCRIPTION = JSON_PATH_ROOT + JSON_PATH_FIELD_DESCRIPTION;
    private static final String JSON_PATH_LAST_REQUEST_AT = JSON_PATH_ROOT + JSON_PATH_FIELD_LAST_REQUEST_AT;
    private static final String JSON_PATH_TYPE = JSON_PATH_ROOT + JSON_PATH_FIELD_TARGET_TYPE;
    private static final String KNOWN_TARGET_ID = "knownTargetId";
    private static final String TENANT_ID = "{tenantId}";
    private static final String STATUS_URL = "/status";
    private static final String CONTENT_0_ID = "content.[0].id";
    private static final String CONTENT_0_TYPE = "content.[0].type";
    private static final String CONTENT_0_MESSAGES = "content.[0].messages";
    private static final String CONTENT_0_REPORTED_AT = "content.[0].reportedAt";
    private static final String CONTENT_1_TYPE = "content.[1].type";
    private static final String GENERIC_FEEDBACK = "/generic-feedback";
    private static final String POLLING_ID_ASC = "polling_id:ASC";
    private static final String TOTAL = "total";
    private static final String CONTROLLER_ID = "/{controllerId}";
    private static final String SECURITY_TOKEN = "securityToken";
    private static final String DCROSS = "Dcross";
    private static final String CONTENT_CONTROLLER_ID = "$.content.[?(@.controllerId=='";
    private static final String HOST = "127.0.0.1";
    private static final String CONTROLLER_ID_ACTIONS_Q = "/{controllerId}/actions?q=";
    private static final String PENDING = "pending";
    private static final String CONTROLLER_ID_ACTIONS_ACTION_ID = "/{controllerId}/actions/{actionId}";
    private static final String DESCRIPTION_STRING = "description";
    private static final String JSON_PATH_CONTROLLER_ID = "$.controllerId";
    private static final String JSON_PATH_NAME = "$.name";
    private static final String JSON_PATH_SERIAL_NUMBER = "$.serialNumber";
    private static final String VEHICLE_MODEL_ID = "vehicleModelId";
    private static final String NAME_NOT_MODIFY = "nameNotModify";
    private static final String SERIAL_NUM_NOT_MODIFY = "serialNumNotModify";
    private static final String TEST_123_FOOBAR = "amqp://test123/foobar";
    private static final String TARGETS_URL = "/targets/";
    private static final String LINKS_SELF_HREF = "')]._links.self.href";
    private static final String JSON_PATH_CONTENT_NAME = "$.content.[?(@.name=='";
    private static final String NAME = "')].name";
    private static final String DESCRIPTION = "')].description";
    private static final String CONTROLLER_ID1 = "')].controllerId";
    private static final String SERIAL_NUMBER = "')].serialNumber";
    private static final String CREATED_BY = "')].createdBy";
    private static final String BUMLUX = "bumlux";
    private static final String REGISTERED = "registered";
    private static final String UPDATE_STATUS = "')].updateStatus";
    private static final String LAST_CONTROLLER_REQUEST_AT = "')].lastControllerRequestAt";
    private static final String SOME_NAME = "someName";
    private static final String SOME_SERIAL_NUM = "someSerialNum";
    private static final String ID = "')].id";
    private static final String VENDOR = "')].vendor";
    private static final String TYPE = "')].type";
    private static final String CONTROLLER_ID2 = "[0].controllerId";
    private static final String NAME1 = "[0].name";
    private static final String SERIAL_NUMBER1 = "[0].serialNumber";
    private static final String SESSION_ID = "session-id";
    private static final String TESTNAME_1 = "testname1";
    private static final String TOKEN = "token";
    private static final String TESTID_1 = "testid1";
    private static final String TESTNAME_2 = "testname2";
    private static final String TESTID_2 = "testid2";
    private static final String TESTNAME_3 = "testname3";
    private static final String TESTID_3 = "testid3";
    private static final String CREATED_BY1 = "[1].createdBy";
    private static final String UPDATE = "update";
    private static final String STATUS = "status";

    private static final String TESTVIN = "testvin";
    private static final String FORCE_TYPE = "forceType";
    private static final String MAINTENANCE_WINDOW = "maintenanceWindow";
    private static final String LINKS_SELF_HREF1 = "_links.self.href";
    private static final String LINKS_DISTRIBUTIONSET_HREF = "_links.distributionset.href";
    private static final String LINKS_STATUS_HREF = "_links.status.href";
    private static final String CANCEL = "cancel";
    private static final String ID_ASC = "ID:ASC";
    private static final String CONTENT_1_ID = "content.[1].id";
    private static final String STATUS1 = "content.[0].status";
    private static final String LINKS_SELF_HREF2 = "content.[1]._links.self.href";
    private static final String UPDATE_STRING = "Update Server: cancel obsolete action due to new update";
    private static final String RUNNING = "running";
    private static final String REPORTED_AT = "content.[1].reportedAt";
    private static final String DISTRIBUTIONSET_URL = "/distributionset";
    private static final String ID1 = "{\"id\"";
    private static final String ASSIGNED_EXPRESSION = "alreadyAssigned";
    private static final String ASSIGNED = "assigned";
    private static final String KNOWN_TARGET_ID_DISTRIBUTIONSET = "/knownTargetId1/distributionset";
    private static final String SCHEDULE = "schedule";
    private static final String DURATION = "duration";
    private static final String TIMEZONE = "timezone";
    private static final String VALUE = "value";
    private static final String MANAGEMENT_V_1_TENANTS = "/management/v1/tenants/";
    private static final String TARGET_ID_WITH_METADATA = "targetIdWithMetadata";
    private static final String KNOWN_KEY = "knownKey";
    private static final String KNOWN_VALUE = "knownValue";
    private static final String TARGETS_CONTROLLER_ID_METADATA_KEY_URL = "/targets/{controllerId}/metadata/{key}";
    private static final String TARGETS_CONTROLLER_ID_DISTRIBUTIONSET_URL = "/targets/{controllerId}/distributionset";
    private static final String TARGETS_CONTROLLER_ID_ACTIONS_URL = "/targets/{controllerId}/actions";
    private static final String TARGET_WITHOUT_TYPE = "targetWithoutType";
    private static final String TARGET_OF_TYPE_1 = "targetOfType1";
    private static final String TARGET_OF_TYPE_2 = "targetOfType2";
    private static final String TARGETTYPE = "targettype";
    private static final String TESTTARGET = "testtarget";
    private static final String TARGETCONTROLLER = "targetcontroller";
    private static final String TEST_SERIAL_NUM = "testSerialNum";
    private static final String TARGET_TYPE = "/target-type";
    private static final String INITIATOR = "initiator";
    private static final String REMARK = "remark";
    private static final String CONTENT_0_AUTO_CONFIRM_ACTIVE = "content.[0].autoConfirmActive";
    private static final String LAST_STATUS_CODE = "lastStatusCode";
    private static final String DETAIL_STATUS = "detailStatus";
    public static final String ROLLOUT_1 = "rollout1";
    public static final String UPDATED_DESCRIPTION = "Updated Description";
    public static final String UPDATED_NAME = "Updated Name";
    public static final String UPDATED_ADDRESS = "Updated Address";
    public static final String ADDRESS = "address";
    public static final String JAKARTA_VALIDATION_VALIDATION_EXCEPTION = "jakarta.validation.ValidationException";
    public static final String JSON_PATH_MESSAGE = "$.message";
    public static final String SERIAL_NUM_1 = "serialNum1";
    public static final String ID_1_SERIAL_NUM_1 = "id1_serialNum1";
    public static final String ID_2_SERIAL_NUM_2 = "id2_serialNum2";
    public static final String ID_3_SERIAL_NUM_3 = "id3_serialNum3";
    public static final String CONTENT_0_LINKS_SELF_HREF = "content.[0]._links.self.href";
    public static final String TARGET_ID = "knownTargetId_knownTargetId";
    public static final String FORCED = "forced";
    public static final String ID_1_SRNO_1 = "id1_srno1";
    public static final String ID_2_SRNO_2 = "id2_srno2";
    public static final String ID_3_SRNO_3 = "id3_srno3";
    public static final String CONTROLLER_ID3 = "targetcontroller_testSerialNum";
    public static final String TENANT = "tenant";
    public static final String UPDATE_TENANT_PATH = "/updateTenant";
    private static ClientAndServer mockServer;
    static final String KNOWN_CONTROLLER_ID = TARGET_ID;
    private static final String CANCELING = "canceling";
    @Autowired
    private JpaProperties jpaProperties;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;

    @Autowired
    private ArtifactsRepository artifactsRepository;

    private Target testTarget;


    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, true), Arguments.of(true, false), Arguments.of(false, true), Arguments.of(false, false), Arguments.of(true, null), Arguments.of(false, null));
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(Arguments.of("someInitiator", "someRemark"), Arguments.of(null, "someRemark"), Arguments.of("someInitiator", null), Arguments.of(null, null));
    }

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    private static void mockPublishVehicleStatus() {
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    @BeforeEach
    void setUp() throws Exception {
        testTarget = testdataFactory.createTarget(KNOWN_CONTROLLER_ID, KNOWN_TARGET_ID, KNOWN_TARGET_ID, testdataFactory.createVehicle("NES 2.0").getId(), KNOWN_TARGET_ID);
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_tag", "sp_target", "sp_vehicle_model",
                "sp_action", "sp_rollout", "sp_distribution_set", "sp_artifact_software_module",
                "sp_software_versions", "sp_base_software_module");
    }

    @Test
    @Description("Ensures that actions list is in expected order.")
    void getActionStatusReturnsCorrectType() throws Exception {
        final int limitSize = 2;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();
        assertThat(actions).hasSize(2);
        updateActionStatus(actions.get(0), DeviceActionStatus.FINISHED_SUCCESS, null, "test");
        final Long tenantId = 1L;
        final PageRequest pageRequest = PageRequest.of(0, 1000, Direction.ASC, ActionFields.ID.getFieldName());
//        final Action action = actionRepository.findByTargetControllerIdAndActive(pageRequest, KNOWN_CONTROLLER_ID, false).getContent().get(0);

        final ActionStatus status = deploymentManagement.findActionStatusByAction(PAGE, actions.get(1).getId()).getContent().stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId())).collect(Collectors.toList()).get(0);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actions.get(1).getId())
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:DESC"))
                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)))
                .andExpect(jsonPath("content.[0].id", equalTo(status.getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].messages", hasSize(1)))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) status.getCreatedAt())));

    }

    @Test
    @Description("Ensures that security token is not returned if user does not have READ_TARGET_SEC_TOKEN permission.")
    @WithUser(allSpPermissions = false, authorities = {SpPermission.READ_TARGET, SpPermission.CREATE_TARGET})
    void securityTokenIsNotInResponseIfMissingPermission() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken").doesNotExist());

    }

    @Test
    @Description("Ensures that security token is returned if user does have READ_TARGET_SEC_TOKEN permission.")
    @WithUser(allSpPermissions = false, authorities = {SpPermission.READ_TARGET, SpPermission.CREATE_TARGET, SpPermission.READ_TARGET_SEC_TOKEN})
    void securityTokenIsInResponseWithCorrectPermission() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("securityToken", equalTo(testTarget.getSecurityToken())));

    }

    @Test
    @Description("Ensures that that IP address is in result as stored in the repository.")
    void addressAndIpAddressInTargetResult() throws Exception {
        // prepare targets with IP
        final String knownControllerId1 = "0815";
        final String knownControllerId2 = "4711";
        final Long tenantId = 1L;

        final int amountTargets = 1;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");
        createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), targets);


        testTarget = testdataFactory.createTarget(knownControllerId1, knownControllerId1, knownControllerId1, testdataFactory.createVehicle("X250").getId(),TESTVIN);
        testTarget = testdataFactory.createTarget(knownControllerId2, knownControllerId2, knownControllerId2, testdataFactory.createVehicle("Dcross").getId(),TESTVIN);

        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))
                .andExpect(status().isOk());

        ResultActions asd = mvc.perform(
                        get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))
                )
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(3)))
                .andExpect(jsonPath("size", equalTo(3)))
                .andExpect(jsonPath("$.content[?(@.controllerId=='"+ knownControllerId1 + "')]").exists())
                .andExpect(jsonPath("$.content[?(@.controllerId=='knownTargetId_knownTargetId')]").exists())
                .andExpect(jsonPath("$.content[?(@.controllerId=='" + targets.get(0).getControllerId() + "')]").exists());

    }

    private void createTarget(final String controllerId) {
        targetManagement.create(entityFactory.target().create().name(controllerId).controllerId(controllerId + "_" + controllerId).serialNumber(controllerId));
    }

    @Test
    @Description("Ensures that actions history is returned as defined by filter status==pending,status==finished.")
    void searchActionsRsql() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");

        assignDistributionSet(dsA, Collections.singletonList(testTarget));

        final String rsqlPendingStatus = "status==pending";
        final String rsqlFinishedStatus = "status==finished";
        final Long tenantId = 1L;
        final String rsqlPendingOrFinishedStatus = rsqlFinishedStatus + "," + rsqlPendingStatus;
        // pending status one result
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING + "?q=" + rsqlPendingStatus, tenantId, testTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("running")));

        // finished status none result
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING + "?q=" + rsqlFinishedStatus, tenantId, testTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(0))).andExpect(jsonPath("size", equalTo(0)));

        // pending or finished status one result
        mvc.perform(get(
                        MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING + "?q=" + rsqlPendingOrFinishedStatus, tenantId, testTarget.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("content[0].status", equalTo("running")));

    }

    @Test
    @Description("Ensures that a deletion of an active action results in cancelation triggered.")
    void cancelActionOK() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();
        final Long tenantId = 1L;
        // test - cancel the active action
        mvc.perform(
                        delete(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId,
                                tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                        .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNoContent());

        final Action action = deploymentManagement.findAction(
                deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId()).get();

        // still active because in CANCELING state and waiting for controller
        // feedback
        assertThat(action.isActive()).isTrue();

        // action has not been canceled confirmed from controller, so DS
        // remains assigned until
        // confirmation
        assertThat(deploymentManagement.getAssignedDistributionSet(tA.getControllerId())).isPresent();
        assertThat(deploymentManagement.getInstalledDistributionSet(tA.getControllerId())).isNotPresent();
    }

    @Test
    @Description("Ensures that method not allowed is returned if cancellation is triggered on already canceled action.")
    void cancelAndCancelActionIsNotAllowed() throws Exception {
        // prepare test
        final Target tA = createTargetAndStartAction();
        final Long tenantId = 1L;
        // cancel the active action
        deploymentManagement.cancelAction(deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().stream().filter(Action::isCancelingOrCanceled).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);

        // test - cancel an cancel action returns forbidden
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId,
                        testTarget.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Force Quit an Action, which is already canceled. Expected Result is an HTTP response code 204.")
    void forceQuitAnCanceledActionReturnsOk() throws Exception {

        final Target tA = createTargetAndStartAction();
        final Long tenantId = 1L;
        // cancel the active action
        deploymentManagement.cancelAction(deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().get(0).getId());

        // find the current active action
        final List<Action> cancelActions = deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE).getContent().stream().filter(Action::isCancelingOrCanceled).collect(Collectors.toList());
        assertThat(cancelActions).hasSize(1);
        assertThat(cancelActions.get(0).isCancelingOrCanceled()).isTrue();

        // test - force quit: Canceled actions should return 204
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING + "?force=true", tenantId,
                        tA.getControllerId(), cancelActions.get(0).getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNoContent());

    }

    @Test
    @Description("Force Quit an Action, which is not canceled. Expected Result is an HTTP response code 405.")
    void forceQuitAnNotCanceledActionReturnsMethodNotAllowed() throws Exception {

        final Target tA = createTargetAndStartAction();
        final Long tenantId = 1L;
        // test - cancel an cancel action returns forbidden
        mvc.perform(
                        delete(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING + "?force=true", tenantId,
                                tA.getControllerId(), deploymentManagement.findActionsByTarget(tA.getControllerId(), PAGE)
                                        .getContent().get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }


    @Test
    @Description("Ensures that deletion is executed if permitted.")
    void deleteTargetReturnsOK() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + KNOWN_CONTROLLER_ID)).andExpect(status().isOk());

        assertThat(targetManagement.getByControllerID(KNOWN_CONTROLLER_ID)).isNotPresent();
    }

    @Test
    @Description("Ensures that deletion is refused with not found if target does not exist.")
    void deleteTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdDelete";
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId))
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ensures that update is refused with not found if target does not exist.")
    void updateTargetWhichDoesNotExistsLeadsToNotFound() throws Exception {
        final String knownControllerId = "knownControllerIdUpdate";
        final Long tenantId = 1L;
        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId).content("{}")
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetDescription() throws Exception {

        final String knownNewDescription = "a new desc updated over rest";
        final String knownNameNotModify = KNOWN_TARGET_ID;
        final String knownSerialNumNotModify = KNOWN_TARGET_ID;
        final Long tenantId = 1L;
        final String body = new JSONObject().put(DESCRIPTION_STRING, knownNewDescription).toString();

        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).content(body)

                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTROLLER_ID, equalTo(KNOWN_CONTROLLER_ID)))
                .andExpect(jsonPath("$.description", equalTo(knownNewDescription)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownNameNotModify)))
                .andExpect(jsonPath(JSON_PATH_SERIAL_NUMBER, equalTo(knownSerialNumNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(KNOWN_CONTROLLER_ID).get();
        assertThat(findTargetByControllerID.getDescription()).isEqualTo(knownNewDescription);
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        assertThat(findTargetByControllerID.getSerialNumber()).isEqualTo(knownSerialNumNotModify);
    }

    @Test
    @Description("vehicle model update for target ")
    void givenVehicleModelIdWhenUpdateTargetSuccess() throws Exception {
        final String knownNameNotModify = KNOWN_TARGET_ID;
        final String knownSerialNumNotModify = KNOWN_TARGET_ID;
        final Long tenantId = 1L;
        final String body = new JSONObject().put(VEHICLE_MODEL_ID, testTarget.getVehicleModelId()).toString();


        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).content(body)

                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTROLLER_ID, equalTo(KNOWN_CONTROLLER_ID)))
                .andExpect(jsonPath("$.vehicleModelId", equalTo(testTarget.getVehicleModelId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownNameNotModify)))
                .andExpect(jsonPath(JSON_PATH_SERIAL_NUMBER, equalTo(knownSerialNumNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(KNOWN_CONTROLLER_ID).get();
        assertThat(findTargetByControllerID.getVehicleModelId()).isEqualTo(testTarget.getVehicleModelId());
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        assertThat(findTargetByControllerID.getSerialNumber()).isEqualTo(knownSerialNumNotModify);
    }

    @Test
    @Description("When Target is part of current rollout, it is not updated, as only description update is allowed")
    void givenTargetPartOfRolloutWhenUpdateTargetThrowException() throws Exception {
        final Long tenantId = 1L;
        final String body = new JSONObject().put(VEHICLE_MODEL_ID, testTarget.getVehicleModelId()).toString();
        List<Target> targets = testdataFactory.createTargets(20, ROLLOUT_1, ROLLOUT_1);
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        DistributionSet ds = testdataFactory.createDistributionSet();
        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroup(targets, rollout);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT, tenantId, rollout.getId())).andDo(MockMvcResultPrinter.print());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }

    @Test
    @Description("When Target is part of rollout in any state, description is updates")
    void givenTargetPartOfRolloutInAnyStateWhenUpdateTargetUpdatesDescription() throws Exception {
        final Long tenantId = 1L;

        // Create a request body with description, name, and address
        final String body = new JSONObject().put(DESCRIPTION_STRING, UPDATED_DESCRIPTION).put("name", UPDATED_NAME).put(ADDRESS, UPDATED_ADDRESS).toString();


        DistributionSet ds = testdataFactory.createDistributionSet("");
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("rollout1")));

        // Perform the update request
        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());


        // Verify that only the description was updated
        Target updatedTarget = targetManagement.getByControllerID(KNOWN_CONTROLLER_ID).orElseThrow(() -> new EntityNotFoundException("Target not found"));

        assertEquals(UPDATED_DESCRIPTION, updatedTarget.getDescription());
        assertNotEquals(UPDATED_NAME, updatedTarget.getName());
    }


    @Test
    @Description("When Target is not part of rollout in any state, description is updates")
    void givenTargetIsNotPartOfAnyRolloutWhenUpdateTargetUpdatesDescription() throws Exception {
        final Long tenantId = 1L;
        final String body = new JSONObject().put(DESCRIPTION_STRING, UPDATED_DESCRIPTION).put("name", UPDATED_NAME).put(ADDRESS, UPDATED_ADDRESS).toString();

        DistributionSet ds = testdataFactory.createDistributionSet("");

        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)

                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // Verify that only the description was updated
        Target updatedTarget = targetManagement.getByControllerID(KNOWN_CONTROLLER_ID).orElseThrow(() -> new EntityNotFoundException("Target not found"));

        assertEquals(UPDATED_DESCRIPTION, updatedTarget.getDescription());
        assertNotEquals(UPDATED_NAME, updatedTarget.getName());

    }

    @Test
    @Description("Ensures that target update request fails is updated value fails against a constraint.")
    void updateTargetDescriptionFailsIfInvalidLength() throws Exception {
        final String knownControllerId = "123";
        final String knownNewDescription = RandomStringUtils.randomAlphabetic(513);
        final String knownNameNotModify = NAME_NOT_MODIFY;
        final String knownSerialNumNotModify = SERIAL_NUM_NOT_MODIFY;
        final Long tenantId = 1L;
        final String body = new JSONObject().put(DESCRIPTION_STRING, knownNewDescription).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify).serialNumber(knownSerialNumNotModify).vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).description("old description").vin(TESTVIN));

        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId).content(body)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());


        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getDescription()).isEqualTo("old description");
    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetSecurityToken() throws Exception {
        final String knownControllerId = "123";
        final String knownNewToken = "6567576565";
        final Long tenantId = 1L;
        final String knownNameNotModify = NAME_NOT_MODIFY;
        final String knownSerialNumNotModify = SERIAL_NUM_NOT_MODIFY;
        final String body = new JSONObject().put(SECURITY_TOKEN, knownNewToken).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify).serialNumber(knownSerialNumNotModify).vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).vin(TESTVIN));

        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ensures that target update request is reflected by repository.")
    void updateTargetAddress() throws Exception {
        final String knownControllerId = "123";
        final String knownNewAddress = TEST_123_FOOBAR;
        final String knownNameNotModify = NAME_NOT_MODIFY;
        final String knownSerialNumNotModify = SERIAL_NUM_NOT_MODIFY;
        final Long tenantId = 1L;
        final String body = new JSONObject().put(ADDRESS, knownNewAddress).toString();

        // prepare
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify).serialNumber(knownSerialNumNotModify).vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).vin(TESTVIN));

        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId).content(body)

                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTROLLER_ID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownNameNotModify)))
                .andExpect(jsonPath(JSON_PATH_SERIAL_NUMBER, equalTo(knownSerialNumNotModify)));

        final Target findTargetByControllerID = targetManagement.getByControllerID(knownControllerId).get();
        assertThat(findTargetByControllerID.getName()).isEqualTo(knownNameNotModify);
        assertThat(findTargetByControllerID.getSerialNumber()).isEqualTo(knownSerialNumNotModify);
    }

    @Test
    @Description("Ensures that when targetType value of -1 is provided the target type is unassigned from the target when updating multiple fields in target object.")
    void updateTargetNameAndUnnasignTargetType() throws Exception {
        final String knownControllerId = KNOWN_TARGET_ID_1;
        final String knownNewAddress = TEST_123_FOOBAR;
        final String knownNameNotModify = KNOWN_TARGET_ID_1;
        final String knownSerialNumNotModify = KNOWN_TARGET_ID_1;
        final Long unnasignTargetTypeValue = -1L;
        final Long tenantId = 1L;
        final TargetType targetType = targetTypeManagement.create(entityFactory.targetType().create().name("targettype1").description("targettypedes1"));

        final String body = new JSONObject().put("targetType", unnasignTargetTypeValue).put("name", "controllerNewName").toString();


        // Create a target with the created TargetType
        targetManagement.create(entityFactory.target().create().controllerId(knownControllerId).name(knownNameNotModify).serialNumber(knownSerialNumNotModify).targetType(targetType.getId()).vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).vin(TESTVIN));

        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId)

                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_CONTROLLER_ID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownNameNotModify)))
                .andExpect(jsonPath(JSON_PATH_SERIAL_NUMBER, equalTo(knownSerialNumNotModify)))
                .andExpect(jsonPath("$.targetType").exists());

        // Perform the update request
        mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId).content(body)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.controllerId", equalTo(knownControllerId)))
                .andExpect(jsonPath("$.serialNumber", equalTo(knownSerialNumNotModify)))
                .andExpect(jsonPath("$.targetType").exists()); // Expecting targetType to exist, which will make the test fail

    }


    @Test
    @Description("Ensures that target query returns list of targets in defined format.")
    void getTargetWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 4;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";
        final Long tenantId = 1L;
        final String linksHrefPrefix = TENANT_API_V1_BASE_URL + tenantId + TARGETS_URL;
        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + LINKS_SELF_HREF, contains(linksHrefPrefix + idA + "_" + idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + NAME, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + DESCRIPTION, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + CONTROLLER_ID1, contains(idA + "_" + idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + SERIAL_NUMBER, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + UPDATE_STATUS, contains(REGISTERED))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + LAST_CONTROLLER_REQUEST_AT, notNullValue()))
                // idB
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + LINKS_SELF_HREF, contains(linksHrefPrefix + idB + "_" + idB))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + NAME, contains(idB))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + DESCRIPTION, contains(idB))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + CONTROLLER_ID1, contains(idB + "_" + idB))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + SERIAL_NUMBER, contains(idB))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + UPDATE_STATUS, contains(REGISTERED))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + LAST_CONTROLLER_REQUEST_AT, notNullValue()))
                // idC
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + LINKS_SELF_HREF, contains(linksHrefPrefix + idC + "_" + idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + NAME, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + DESCRIPTION, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + CONTROLLER_ID1, contains(idC + "_" + idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + SERIAL_NUMBER, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + UPDATE_STATUS, contains(REGISTERED))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + LAST_CONTROLLER_REQUEST_AT, notNullValue()));
    }

    @Test
    @Description("Ensures that target query returns list of targets in defined format in size reduced by given limit parameter.")
    void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int knownTargetAmount = 3;
        final int limitSize = 1;
        createTargetsAlphabetical(knownTargetAmount);
        final String idA = "a";
        final Long tenantId = 1L;
        final String linksHrefPrefix = TENANT_API_V1_BASE_URL + tenantId + TARGETS_URL;

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + LINKS_SELF_HREF, contains(linksHrefPrefix + idA + "_" + idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + NAME, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + DESCRIPTION, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + CONTROLLER_ID1, contains(idA + "_" + idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + SERIAL_NUMBER, contains(idA))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + UPDATE_STATUS, contains(REGISTERED)));
    }

    @Test
    @Description("Ensures that target query returns list of targets in defined format in size reduced by given limit and offset parameter.")
    void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 6;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";
        final Long tenantId = 1L;
        final String linksHrefPrefix = TENANT_API_V1_BASE_URL + tenantId + TARGETS_URL;

        createTargetsAlphabetical(knownTargetAmount);
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam)).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount))).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize))).andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + LINKS_SELF_HREF, contains(linksHrefPrefix + idC + "_" + idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + NAME, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + DESCRIPTION, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + CONTROLLER_ID1, contains(idC + "_" + idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + SERIAL_NUMBER, contains(idC))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + UPDATE_STATUS, contains(REGISTERED)))
                // idB
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + LINKS_SELF_HREF, contains(linksHrefPrefix + idD + "_" + idD))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + NAME, contains(idD))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + DESCRIPTION, contains(idD))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + CONTROLLER_ID1, contains(idD + "_" + idD))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + SERIAL_NUMBER, contains(idD))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + UPDATE_STATUS, contains(REGISTERED)))
                // idC
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + LINKS_SELF_HREF, contains(linksHrefPrefix + idE + "_" + idE))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + NAME, contains(idE))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + DESCRIPTION, contains(idE))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + CONTROLLER_ID1, contains(idE + "_" + idE))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + SERIAL_NUMBER, contains(idE))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + CREATED_BY, contains(BUMLUX))).andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + UPDATE_STATUS, contains(REGISTERED)));
    }

    @Test
    @Description("Ensures that the get request for a target works.")
    void testGetTargetApi() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = SOME_NAME;
        final String knownSerialNum = SOME_SERIAL_NUM;
        final Long tenantId = 1L;
        final Target target = createSingleTarget(knownControllerId, knownName, knownSerialNum);
        final String hrefPrefix = TENANT_API_V1_BASE_URL + tenantId + TARGETS_URL + knownControllerId + "/";
        // test
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo(knownControllerId)))
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_DESCRIPTION, equalTo(TARGET_DESCRIPTION_TEST)))
                .andExpect(jsonPath(JSON_PATH_LAST_REQUEST_AT, equalTo(target.getLastTargetQuery().intValue())))
                .andExpect(jsonPath("$.pollStatus", hasKey("lastRequestAt")))
                .andExpect(jsonPath("$.pollStatus", hasKey("nextExpectedRequestAt")))
                .andExpect(jsonPath("$._links.actions.href",
                        equalTo(hrefPrefix + "actions" + "?offset=0&limit=50&sort=id%3ADESC")));
    }

    @Test
    @Description("Ensures that target get request returns a not found if the target does not exits.")
    void getSingleTargetNoExistsResponseNotFound() throws Exception {

        final String targetIdNotExists = "bubu";
        final Long tenantId = 1L;
        // test
        final MvcResult mvcResult = mvc
                .perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, targetIdNotExists))
                .andExpect(status().isNotFound()).andReturn();


        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.ENTITY_NOT_EXISTS.name());
    }

    @Test
    @Description("Ensures that get request for asigned distribution sets returns no count if no distribution set has been assigned.")
    void getAssignedDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = SOME_NAME;
        final String knownSerialNum = SOME_SERIAL_NUM;
        final Long tenantId = 1L;
        createSingleTarget(knownControllerId, knownName, knownSerialNum);

        // test
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ASSIGN_DSET_V1_REQUEST_MAPPING, tenantId, knownControllerId))
                .andExpect(status().isNoContent()).andExpect(content().string(""));


    }

    @Test
    @Description("Ensures that the get request for asigned distribution sets works.")
    void getAssignedDistributionSetOfTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = SOME_NAME;
        final String knownSerialNum = SOME_SERIAL_NUM;
        final Long tenantId = 1L;
        createSingleTarget(knownControllerId, knownName, knownSerialNum);
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), knownControllerId);

        // test

        final SoftwareModule os = ds.findFirstModuleByType(osType).get();
        final SoftwareModule jvm = ds.findFirstModuleByType(runtimeType).get();
        final SoftwareModule bApp = ds.findFirstModuleByType(appType).get();
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ASSIGN_DSET_V1_REQUEST_MAPPING, tenantId, knownControllerId))

                .andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_ID, equalTo(ds.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_NAME, equalTo(ds.getName())))
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_DESCRIPTION, equalTo(ds.getDescription())))
                // os
                .andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + ID, contains(os.getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + NAME, contains(os.getName()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + DESCRIPTION, contains(os.getDescription()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + VENDOR, contains(os.getVendor()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + osType.getKey() + TYPE, contains("os")))
                // jvm
                .andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + ID, contains(jvm.getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + NAME, contains(jvm.getName()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + DESCRIPTION, contains(jvm.getDescription()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + VENDOR, contains(jvm.getVendor()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + runtimeType.getKey() + TYPE, contains("runtime")))
                // baseApp
                .andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + ID, contains(bApp.getId().intValue()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + NAME, contains(bApp.getName()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + DESCRIPTION, contains(bApp.getDescription()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + VENDOR, contains(bApp.getVendor()))).andExpect(jsonPath(JSON_PATH_MODULES_TYPE + appType.getKey() + TYPE, contains("application")));

    }

    @Test
    @Description("Ensures that get request for installed distribution sets returns no count if no distribution set has been installed.")
    void getInstalledDistributionSetOfTargetIsEmpty() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownControllerId = "1";
        final String knownName = SOME_NAME;
        final Long tenantId = 1L;
        final String knownSerialNum = SOME_SERIAL_NUM;
        createSingleTarget(knownControllerId, knownName, knownSerialNum);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_INSTALL_V1_REQUEST_MAPPING, tenantId, knownControllerId))
                .andExpect(status().isNoContent()).andExpect(content().string(""));

    }

    @Test
    @Description("Ensures that a target creation with empty name and a controllerId that exceeds the name length limitation is successful and the name gets truncated.")
    void createTargetWithLongControllerId() throws Exception {
        final String randomName = RandomStringUtils.randomAlphanumeric(JpaTarget.NAME_MAX_SIZE);
        //Max length of controllerId is 256, since we are concatenating controllerId + serial number now, creating a random sting of 128 characters
        final String randomSerialNum = RandomStringUtils.randomAlphanumeric(JpaTarget.SERIAL_NUMBER_MAX_SIZE / 2);
        final String randomControllerId = RandomStringUtils.randomAlphanumeric(127) + "_" + randomSerialNum;

        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(randomControllerId).name(randomName).serialNumber(randomSerialNum).vehicleModelId(testdataFactory.createVehicle("X50").getId()).vin(randomControllerId.split("_")[0]).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(jsonPath(CONTROLLER_ID2, equalTo(randomControllerId))).andExpect(jsonPath(NAME1, equalTo(randomName))).andExpect(jsonPath(SERIAL_NUMBER1, equalTo(randomSerialNum)));


        assertThat(targetManagement.getByControllerID(randomControllerId).get().getName()).isEqualTo(randomName);
    }

    @Test
    @Description("Given correct controller id format when create target then success")
    void givenCorrectControllerIdFormatWhenCreateTargetThenSuccess() throws Exception {
        final String randomName = SOME_NAME;
        final String randomSerialNum = RandomStringUtils.randomAlphanumeric(15);
        final String randomControllerId = RandomStringUtils.randomAlphanumeric(15) + "_" + randomSerialNum;

        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(randomControllerId).name(randomName).serialNumber(randomSerialNum).vehicleModelId(testdataFactory.createVehicle("X50").getId()).vin(randomControllerId.split("_")[0]).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andExpect(jsonPath(CONTROLLER_ID2, equalTo(randomControllerId))).andExpect(jsonPath(NAME1, equalTo(randomName))).andExpect(jsonPath(SERIAL_NUMBER1, equalTo(randomSerialNum)));


        assertThat(targetManagement.getByControllerID(randomControllerId).get().getName()).isEqualTo(randomName);
    }

    @Deprecated
    @Disabled("Controller Id will not be provided in request by DOCG so this test is invalid")
    @Test
    @Description("Given a  wrong controller id format when create target then fails")
    void givenWrongControllerIdFormatWhenCreateTargetThenFails() throws Exception {
        final String randomName = SOME_NAME;
        final String randomSerialNum = RandomStringUtils.randomAlphanumeric(15);
        final String randomControllerId = RandomStringUtils.randomAlphanumeric(15);

        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(randomControllerId).name(randomName).serialNumber(randomSerialNum).vehicleModelId(testdataFactory.createVehicle("X50").getId()).vin(TESTVIN).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())

                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Provided Controller ID format is invalid")));
    }

    @Deprecated
    @Disabled("Controller Id will not be provided in request by DOCG so this test is invalid")
    @Test
    @Description("Given a  wrong controller id format when create target then fails")
    void givenAnotherWrongControllerIdFormatWhenCreateTargetThenFails() throws Exception {
        final String randomName = SOME_NAME;
        final String randomSerialNum = RandomStringUtils.randomAlphanumeric(15);
        final String randomControllerId = RandomStringUtils.randomAlphanumeric(15) + "_" + randomSerialNum + "_" + randomSerialNum;

        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(randomControllerId).name(randomName).serialNumber(randomSerialNum).vehicleModelId(testdataFactory.createVehicle("X50").getId()).vin(TESTVIN).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);


        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Provided Controller ID format is invalid")));
    }

    @Deprecated
    @Disabled("Controller Id will not be provided in request by DOCG so this test is invalid")
    @Test
    @Description("Given the serial number in the controller ID does not match the actual serial number, when creating a target, then it fails.")
    void givenSerialNumInControllerIdDoesNotMatchActualSerialNumWhenCreateTargetThenFails() throws Exception {
        final String randomName = SOME_NAME;
        final String randomSerialNum = RandomStringUtils.randomAlphanumeric(15);
        final String randomControllerId = RandomStringUtils.randomAlphanumeric(15) + "_" + randomSerialNum + "1234";

        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(randomControllerId).name(randomName).serialNumber(randomSerialNum).vehicleModelId(testdataFactory.createVehicle("X50").getId()).vin(randomControllerId.split("_")[0]).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);


        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Provided serial number does not match with the one in controller ID")));
    }

    @Test
    @Description("Ensures that post request for creating a target with no payload returns a bad request.")
    void createTargetWithoutPayloadBadRequest() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))
                .andExpect(status().isOk());
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId)))
                        .header("session-id", SESSION_ID_HEADER)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();


        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getDebug()).isNotNull();
        assertThat(exceptionInfo.getMessage()).isEqualTo(ServerError.REST_BODY_NOT_READABLE.getMessage());
    }

    @Test
    @Description("Ensures that post request for creating a target with invalid payload returns a bad request.")
    void createTargetWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))
                .andExpect(status().isOk());
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId)))
                        .header("session-id", SESSION_ID_HEADER)
                        .content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.REST_BODY_NOT_READABLE.name());
        assertThat(exceptionInfo.getDebug()).isNotNull();
    }

    @Test
    @Description("Verifies that a mandatory properties of new targets are validated as not null.")
    void createTargetWithMissingMandatoryPropertyBadRequest() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))
                .andExpect(status().isOk());
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId))).content("[{\"name\":\"id1\"}]")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();
        assertThat(mvcResult.getResolvedException()).isNotNull();
        assertThat(mvcResult.getResolvedException().getClass()).isNotNull();
        Class<? extends Throwable> t = mvcResult.getResolvedException().getClass();
        assertThat(t.getName()).isEqualTo(ConstraintViolationException.class.getName());
        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getDebug()).isNotEmpty();
    }

    @Test
    @Description("Verfies that a  properties of new targets are validated as in allowed size range.")
    void createTargetWithInvalidPropertyBadRequest() throws Exception {
        final Long tenantId = 1L;
        final var serialNumber = "serialNum1";
        final Target test1 = entityFactory.target().create().controllerId("id1_serialNum1")
                .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).serialNumber("serialNum1").build();
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))

                .andExpect(status().isOk());
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))
                        .content(JsonBuilder.targets(Collections.singletonList(test1), true))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetManagement.count()).isZero();
        assertThat(mvcResult.getResolvedException()).isNotNull();
        assertThat(mvcResult.getResolvedException().getClass()).isNotNull();
        Class<? extends Throwable> t = mvcResult.getResolvedException().getClass();
        assertThat(t.getName()).isEqualTo(ConstraintViolationException.class.getName());
        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());

        // Conflict Resolution - COSMOS-133
        assertThat(exceptionInfo.getDebug()).isNotEmpty();
    }

    @Test
    @Description("Ensures that a post request for creating multiple targets works.")
    void createTargetsListReturnsSuccessful() throws Exception {
        final Long tenantId = 1L;
        final Target test1 = entityFactory.target().create().controllerId(ID_1_SERIAL_NUM_1).name(TESTNAME_1).serialNumber(SERIAL_NUM_1).securityToken(TOKEN).description(TESTID_1).vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).vin(ID_1_SERIAL_NUM_1.split("_")[0]).build();
        final Target test2 = entityFactory.target().create().controllerId(ID_2_SERIAL_NUM_2).name(TESTNAME_2).serialNumber("serialNum2").description(TESTID_2).vehicleModelId(testdataFactory.createVehicle("X250").getId()).vin(ID_2_SERIAL_NUM_2.split("_")[0]).build();
        final Target test3 = entityFactory.target().create().controllerId(ID_3_SERIAL_NUM_3).name(TESTNAME_3).serialNumber("serialNum3").description(TESTID_3).vehicleModelId(testdataFactory.createVehicle("STLA-Brain").getId()).vin(ID_3_SERIAL_NUM_3.split("_")[0]).build();

        final List<Target> targets = Arrays.asList(test1, test2, test3);

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), tenantId).content(JsonBuilder.targets(targets, true))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("testname1")))
                .andExpect(jsonPath("[0].controllerId", equalTo("id1_serialNum1")))
                .andExpect(jsonPath("[0].serialNumber", equalTo("serialNum1")))
                .andExpect(jsonPath("[0].description", equalTo("testid1")))
                .andExpect(jsonPath("[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[0].securityToken", equalTo("token")))
                .andExpect(jsonPath("[1].name", equalTo("testname2")))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].controllerId", equalTo("id2_serialNum2")))
                .andExpect(jsonPath("[1].serialNumber", equalTo("serialNum2")))
                .andExpect(jsonPath("[1].description", equalTo("testid2")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[2].name", equalTo("testname3")))
                .andExpect(jsonPath("[2].controllerId", equalTo("id3_serialNum3")))
                .andExpect(jsonPath("[2].serialNumber", equalTo("serialNum3")))
                .andExpect(jsonPath("[2].description", equalTo("testid3")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].createdBy", equalTo("bumlux"))).andReturn();


        assertThat(
                JsonPath.compile("[0]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                .hasToString(TENANT_API_V1_BASE_URL + tenantId + "/targets/id1_serialNum1");
        assertThat(
                JsonPath.compile("[1]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                .hasToString(TENANT_API_V1_BASE_URL + tenantId + "/targets/id2_serialNum2");
        assertThat(
                JsonPath.compile("[2]._links.self.href").read(mvcResult.getResponse().getContentAsString()).toString())
                .hasToString(TENANT_API_V1_BASE_URL + tenantId + "/targets/id3_serialNum3");

        final Target t1 = assertTarget(ID_1_SERIAL_NUM_1, TESTNAME_1, TESTID_1);
        assertThat(t1.getSecurityToken()).isEqualTo(TOKEN);

        assertTarget(ID_2_SERIAL_NUM_2, TESTNAME_2, TESTID_2);
        assertTarget(ID_3_SERIAL_NUM_3, TESTNAME_3, TESTID_3);
    }

    private Target assertTarget(final String controllerId, final String name, final String description) {
        final Optional<Target> target1 = targetManagement.getByControllerID(controllerId);
        assertThat(target1).isPresent();
        final Target t = target1.get();
        assertThat(t.getName()).isEqualTo(name);
        assertThat(t.getDescription()).isEqualTo(description);
        return t;
    }

    @Test
    @Description("Ensures that a post request for creating one target within a list works.")
    void createTargetsSingleEntryListReturnsSuccessful() throws Exception {
        final String knownName = KNOWN_TARGET_ID_1;
        final String knownSerialNum = KNOWN_TARGET_ID_1;
        final String knownControllerId = "knownTargetId1_" + knownSerialNum;
        final String knownDescription = "someDescription";
        final Long tenantId = 1L;
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownSerialNum, knownDescription, knownControllerId.split("_")[0]);

        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andExpect(status().isOk());
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId))
                ).content(createTargetsJson)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());


        final Slice<Target> findTargetsAll = targetManagement.findAll(PageRequest.of(0, 100));
        final Target target = findTargetsAll.getContent().get(0);
        assertThat(targetManagement.count()).isEqualTo(1);
        assertThat(target.getControllerId()).isEqualTo(knownControllerId);
        assertThat(target.getName()).isEqualTo(knownName);
        assertThat(target.getDescription()).isEqualTo(knownDescription);
    }

    @Test
    @Description("Ensures that a post request for creating the same target again leads to a conflict response.")
    void createTargetsSingleEntryListDoubleReturnConflict() throws Exception {
        final String knownName = SOME_NAME;
        final String knownSerialNum = SOME_SERIAL_NUM;
        final String knownControllerId = "controllerId1_" + knownSerialNum;
        final String knownDescription = "someDescription";
        final Long tenantId = 1L;
        final String createTargetsJson = getCreateTargetsListJsonString(knownControllerId, knownName, knownSerialNum, knownDescription,knownControllerId.split("_")[0]);

        // create a target first to provoke a already exists error
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()))
                .andExpect(status().isOk());
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId))
                ).content(createTargetsJson)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is2xxSuccessful());

        // create another one to retrieve the entity already exists exception
        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).header(SESSION_ID, SESSION_ID_HEADER).content(createTargetsJson).contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(HttpStatus.CONFLICT.value())).andReturn();

        // verify only one entry
        assertThat(targetManagement.count()).isEqualTo(1);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.ENTITY_ALREADY_EXISTS.name());
        assertThat(exceptionInfo.getDebug()).isNotNull();
        assertThat(exceptionInfo.getMessage()).isEqualTo(ServerError.ENTITY_ALREADY_EXISTS.getMessage());
    }

    @Test
    @Description("Ensures that the get request for action of a target returns no actions if nothing has happened.")
    void getActionWithEmptyResult() throws Exception {
        final Long tenantId = 1L;

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(0))).andExpect(jsonPath("content", hasSize(0)))
                .andExpect(jsonPath("total", equalTo(0)));

    }

    @Test
    @Description("Ensures that the expected response is returned for update action.")
    void getUpdateAction() throws Exception {
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("status", equalTo(actions.get(1).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("forceType", equalTo("no")))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId(), tenantId))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))));

    }

    @Test
    @Description("Ensures that the expected response is returned for update action with maintenance window.")
    void getUpdateActionWithMaintenanceWindow() throws Exception {
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actions.get(1).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("status", equalTo(actions.get(1).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("forceType", equalTo("no")))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo((int) actions.get(1).getMaintenanceWindowStartTime().get().toInstant().getEpochSecond())))
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))))
                .andExpect(jsonPath("_links.distributionset.href",
                        equalTo(generateActionDsLink(actions.get(1).getDistributionSet().getId(), tenantId))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))));

    }

    @Test
    @Description("Ensures that the expected response is returned when update action was cancelled.")
    void getCancelAction() throws Exception {
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("status", equalTo(actions.get(0).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("maintenanceWindow").doesNotExist())
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))))
                .andExpect(jsonPath("_links.canceledaction.href",
                        equalTo(generateCanceledactionreferenceLink(KNOWN_CONTROLLER_ID, actions.get(0), tenantId))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))));

    }

    @Test
    @Description("Ensures that the expected response is returned when update action with maintenance window was cancelled.")
    void getCancelActionWithMaintenanceWindow() throws Exception {
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actions.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("forceType", equalTo("no")))
                .andExpect(jsonPath("status", equalTo(actions.get(0).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("maintenanceWindow.nextStartAt",
                        equalTo((int) actions.get(0).getMaintenanceWindowStartTime().get().toInstant().getEpochSecond())))
                .andExpect(jsonPath("_links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))))
                .andExpect(jsonPath("_links.canceledaction.href",
                        equalTo(generateCanceledactionreferenceLink(KNOWN_CONTROLLER_ID, actions.get(0), tenantId))))
                .andExpect(jsonPath("_links.status.href",
                        equalTo(generateStatusreferenceLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))));

    }


    @Test
    @Description("Ensures that the expected response of geting actions of a target is returned.")
    void getMultipleActions() throws Exception {
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].status", equalTo(actions.get(1).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].status", equalTo(actions.get(0).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Ensures that the expected response of getting actions with maintenance window of a target is returned.")
    void getMultipleActionsWithMaintenanceWindow() throws Exception {
        final String schedule = getTestSchedule(10);
        final String duration = getTestDuration(10);
        final String timezone = getTestTimeZone();
        final long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(schedule, duration, timezone);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].status", equalTo(actions.get(1).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[1].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[1].maintenanceWindow.nextStartAt",
                        equalTo((int) actions.get(1).getMaintenanceWindowStartTime().get().toInstant().getEpochSecond())))
                .andExpect(jsonPath("content.[1]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))))
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].status", equalTo(actions.get(0).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0].maintenanceWindow.schedule", equalTo(schedule)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.duration", equalTo(duration)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.timezone", equalTo(timezone)))
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt",
                        equalTo((int) actions.get(0).getMaintenanceWindowStartTime().get().toInstant().getEpochSecond())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Verifies that the API returns the status list with expected content.")
    void getMultipleActionStatus() throws Exception {
        final Long tenantId = 1L;
        final Action action = generateTargetWithTwoUpdatesWithOneOverride().get(0);
        // retrieve list in default descending order for actionstaus entries
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent().stream().sorted((e1, e2) -> Long.compare(e2.getId(), e1.getId())).collect(Collectors.toList());

        // sort is default descending order, latest status first
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo(CANCELING)))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo((int) actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Verifies that the API returns the status list with expected content sorted in descending order of reportedAt field.")
    void getMultipleActionStatusSortedByReportedAtDescendingOrder() throws Exception {
        final Long tenantId = 1L;
        final Action action = generateTargetWithTwoUpdatesWithOneOverride().get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent().stream().sorted(Comparator.comparingLong(Identifiable::getId)).collect(Collectors.toList());


        // descending order
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, action.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                        "REPORTEDAT:DESC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo(CANCELING)))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo("running")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo((int) actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Verifies that the API returns the status list with expected content sorted in ascending order of reportedAt field.")
    void getMultipleActionStatusSortedByReportedAtAscendingOrder() throws Exception {
        final Long tenantId = 1L;
        final Action action = generateTargetWithTwoUpdatesWithOneOverride().get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent().stream().sorted(Comparator.comparingLong(Identifiable::getId)).collect(Collectors.toList());

        // ascending order
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, action.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING,
                        "REPORTEDAT:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[1].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[1].type", equalTo(CANCELING)))
                .andExpect(jsonPath("content.[1].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[1].reportedAt", equalTo((int) actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Verifies that the API returns the status list with expected content split into two pages.")
    void getMultipleActionStatusWithPagingLimitRequestParameter() throws Exception {
        final String knownTargetId = TARGET_ID;
        final Long tenantId = 1L;

        final Action action = generateTargetWithTwoUpdatesWithOneOverride().get(0);
        final List<ActionStatus> actionStatus = deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent().stream().sorted((e1, e2) -> Long.compare(e1.getId(), e2.getId())).collect(Collectors.toList());

        // Page 1
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, action.getId()).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT,
                        String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo(CANCELING)))
                .andExpect(jsonPath("content.[0].messages",
                        hasItem("Update Server: cancel obsolete action due to new update")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) actionStatus.get(1).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // Page 2
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, action.getId())
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actionStatus.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].type", equalTo("running")))
                .andExpect(jsonPath("content.[0].reportedAt", equalTo((int) actionStatus.get(0).getCreatedAt())))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

    }

    @Test
    @Description("Verifies getting multiple actions with the paging request parameter.")
    void getMultipleActionsWithPagingLimitRequestParameter() throws Exception {
        final Long tenantId = 1L;
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();

        // page 1: one entry
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(0).getId().intValue())))
                .andExpect(jsonPath("content.[0].status", equalTo(actions.get(0).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(0).getId(), tenantId))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

        // page 2: one entry
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(1))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "ID:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("content.[0].id", equalTo(actions.get(1).getId().intValue())))
                .andExpect(jsonPath("content.[0].status", equalTo(actions.get(1).getStatus().toString().toLowerCase())))
                .andExpect(jsonPath("content.[0]._links.self.href",
                        equalTo(generateActionSelfLink(KNOWN_CONTROLLER_ID, actions.get(1).getId(), tenantId))))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));

    }

    private String generateActionSelfLink(final String knownTargetId, final Long actionId, final Long tenantId) {
        return LOCALHOST_URL + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId;
    }

    private String generateActionDsLink(final Long dsId, final Long tenantId) {
        return LOCALHOST_URL + MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + dsId;
    }

    private String generateCanceledactionreferenceLink(final String knownTargetId, final Action action, final Long tenantId) {
        return LOCALHOST_URL + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + action.getId();
    }

    private String generateStatusreferenceLink(final String knownTargetId, final Long actionId, final Long tenantId) {
        return LOCALHOST_URL + MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/" + actionId + "/" + MgmtRestConstants.TARGET_V1_ACTION_STATUS + "?offset=0&limit=50&sort=id%3ADESC";
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverride() {
        return generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(null, null, null);
    }

    private List<Action> generateTargetWithTwoUpdatesWithOneOverrideWithMaintenanceWindow(final String schedule, final String duration, final String timezone) {

        final Iterator<DistributionSet> sets = testdataFactory.createDistributionSets(2).iterator();
        final DistributionSet one = sets.next();
        final DistributionSet two = sets.next();

        // Update
        if (schedule == null) {
            final List<Target> updatedTargets = assignDistributionSet(one, Collections.singletonList(testTarget)).getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 2 seconds to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofSeconds(10)).atLeast(1, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSet(two, updatedTargets);
        } else {
            final List<Target> updatedTargets = assignDistributionSetWithMaintenanceWindow(one.getId(), testTarget.getControllerId(), schedule, duration, timezone).getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
            // 2nd update
            // sleep 2 Seconds to ensure that we can sort by reportedAt
            Awaitility.await().atMost(Duration.ofSeconds(10)).atLeast(1, TimeUnit.SECONDS).pollInterval(2, TimeUnit.SECONDS).until(() -> updatedTargets.stream().allMatch(t -> t.getLastModifiedAt() > 0L));
            assignDistributionSetWithMaintenanceWindow(two.getId(), updatedTargets.get(0).getControllerId(), schedule, duration, timezone);
        }

        // two updates, one cancelation
        final List<Action> actions = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE).getContent();

        assertThat(actions).hasSize(2);
        return actions;
    }

    @Test
    @Description("Verfies that an action is switched from soft to forced if requested by management API")
    void updateAction() throws Exception {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(set.getId(), testTarget.getControllerId(), MgmtRolloutUserAcceptanceRequired.YES));
        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.YES);

        Optional<Action> action = deploymentManagement.findAction(actionId);

        final String body = new JSONObject().put("userAcceptanceRequired", "no").toString();
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), actionId).content(body)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("id", equalTo(actionId.intValue())))
                .andExpect(jsonPath(STATUS, equalTo(action.get().getStatus().toString().toLowerCase()))).andExpect(jsonPath(FORCE_TYPE, equalTo("no")))
                .andExpect(jsonPath(LINKS_SELF_HREF1,
                        equalTo(generateActionSelfLink(testTarget.getControllerId(), actionId, tenantId))))
                .andExpect(jsonPath(LINKS_DISTRIBUTIONSET_HREF,
                        equalTo(TENANT_API_V1_BASE_URL + tenantId + "/distributionsets/" + set.getId())))
                .andExpect(jsonPath(LINKS_STATUS_HREF,
                        equalTo(generateStatusreferenceLink(testTarget.getControllerId(), actionId, tenantId))));

        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.NO);
    }

    @Test
    @Description("Verfies that a DS to target assignment is reflected by the repository and that repeating " + "the assignment does not change the target.")
    void assignDistributionSetToTarget() throws Exception {
        final Long tenantId = 1L;
        final DistributionSet set = testdataFactory.createDistributionSet("one");

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath("assigned", equalTo(1))).andExpect(jsonPath("alreadyAssigned", equalTo(0)))
                .andExpect(jsonPath("total", equalTo(1)));


        assertThat(deploymentManagement.getAssignedDistributionSet(testTarget.getControllerId()).get()).isEqualTo(set);
        testTarget = targetManagement.getByControllerID(testTarget.getControllerId()).get();

        // repeating DS assignment leads again to OK
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath("assigned", equalTo(0))).andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));


        // ...but does not change the target
        assertThat(targetManagement.getByControllerID(testTarget.getControllerId()).get()).isEqualTo(testTarget);
    }

    /**
     * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
     */
    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Ensures that confirmation option is considered in assignment request.")
    @Disabled("This feature is deprecated so disabling it for now.")
    @Deprecated
    void assignDistributionSetToTargetWithConfirmationOptions(final boolean confirmationFlowActive, final Boolean confirmationRequired) throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final JSONObject jsonPayload = new JSONObject();
        jsonPayload.put("id", set.getId());
        if (confirmationRequired != null) {
            jsonPayload.put("confirmationRequired", confirmationRequired);
        }

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + testTarget.getControllerId() + DISTRIBUTIONSET_URL).content(jsonPayload.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andExpect(jsonPath(ASSIGNED, equalTo(1))).andExpect(jsonPath(ASSIGNED_EXPRESSION, equalTo(0))).andExpect(jsonPath(TOTAL, equalTo(1)));

        assertThat(deploymentManagement.getAssignedDistributionSet(testTarget.getControllerId()).get()).isEqualTo(set);

        assertThat(deploymentManagement.findActionsByDistributionSet(PAGE, set.getId()).getContent()).hasSize(1).allMatch(action -> {
            if (!confirmationFlowActive) {
                return !action.isWaitingConfirmation();
            }
            return confirmationRequired == null ? action.isWaitingConfirmation() : confirmationRequired == action.isWaitingConfirmation();
        });
    }

    @Test
    @Description("Verfies that an offline DS to target assignment is reflected by the repository and that repeating " + "the assignment does not change the target.")
    void offlineAssignDistributionSetToTarget() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + testTarget.getControllerId() + "/distributionset?offline=true").content(ID1 + ":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(ASSIGNED, equalTo(1))).andExpect(jsonPath(ASSIGNED_EXPRESSION, equalTo(0))).andExpect(jsonPath(TOTAL, equalTo(1)));

        assertThat(deploymentManagement.getAssignedDistributionSet(testTarget.getControllerId()).get()).isEqualTo(set);
        assertThat(deploymentManagement.getInstalledDistributionSet(testTarget.getControllerId()).get()).isEqualTo(set);
        testTarget = targetManagement.getByControllerID(testTarget.getControllerId()).get();
        assertThat(testTarget.getUpdateStatus()).isEqualTo(TargetUpdateStatus.IN_SYNC);

        // repeating DS assignment leads again to OK
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING + "?offline=true", tenantId, testTarget.getControllerId()).content("{\"id\":" + set.getId() + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("assigned", equalTo(0))).andExpect(jsonPath("alreadyAssigned", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1)));


        // ...but does not change the target
        assertThat(targetManagement.getByControllerID(testTarget.getControllerId()).get()).isEqualTo(testTarget);
    }

    @Test
    @Description("Assigns distribution set to target with only maintenance schedule.")
    void assignDistributionSetToTargetWithMaintenanceWindowStartTimeOnly() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        final String body = new JSONObject().put("id", set.getId()).put("type", FORCED).put(MAINTENANCE_WINDOW, new JSONObject().put(SCHEDULE, getTestSchedule(0))).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Assigns distribution set to target with only maintenance window duration.")
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeOnly() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        final String body = new JSONObject().put("id", set.getId()).put("type", FORCED).put(MAINTENANCE_WINDOW, new JSONObject().put(DURATION, getTestDuration(10))).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content(body).contentType(MediaTypes.HAL_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Assigns distribution set to target with valid maintenance window.")
    void assignDistributionSetToTargetWithValidMaintenanceWindow() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        final String body = new JSONObject().put("id", set.getId()).put("type", FORCED).put("forcetime", "0").put(MAINTENANCE_WINDOW, new JSONObject().put(SCHEDULE, getTestSchedule(10)).put(DURATION, getTestDuration(10)).put(TIMEZONE, getTestTimeZone())).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

    }

    @Test
    @Description("Assigns distribution set to target with maintenance window next execution start (should be ignored, calculated automaticaly based on schedule, duration and timezone)")
    void assignDistributionSetToTargetWithMaintenanceWindowNextExecutionStart() throws Exception {

        final Rollout rollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), testdataFactory.createTargets("controllerId"));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final DistributionSet set = rollout.getDistributionSet();

        final long nextExecutionStart = Instant.now().getEpochSecond();
        final Long tenantId = 1L;
        final String body = new JSONObject().put("id", set.getId()).put(MAINTENANCE_WINDOW, new JSONObject().put(SCHEDULE, getTestSchedule(10)).put(DURATION, getTestDuration(10)).put(TIMEZONE, getTestTimeZone()).put("nextStartAt", String.valueOf(nextExecutionStart))).toString();
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());


        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath("content.[0].maintenanceWindow.nextStartAt", not(nextExecutionStart)));

    }

    @Test
    @Description("Assigns distribution set to target with last maintenance window scheduled before current time.")
    void assignDistributionSetToTargetWithMaintenanceWindowEndTimeBeforeStartTime() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;
        final String body = new JSONObject().put("id", set.getId()).put("type", FORCED).put(MAINTENANCE_WINDOW, new JSONObject().put(SCHEDULE, getTestSchedule(-30)).put(DURATION, getTestDuration(5)).put(TIMEZONE, getTestTimeZone())).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    void invalidRequestsOnAssignDistributionSetToTarget() throws Exception {

        final DistributionSet set = testdataFactory.createDistributionSet("one");
        final Long tenantId = 1L;

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))
                        + KNOWN_TARGET_ID_DISTRIBUTIONSET)
                        .content(ID1 + ":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/knownTargetId_knownTargetId/distributionset").content(ID1 + ":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/knownTargetId_knownTargetId/distributionset").content(ID1 + ":12345678}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/knownTargetId_knownTargetId/distributionset/assign").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/knownTargetId_knownTargetId/distributionset/assign").content(ID1 + ":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

    }

    @Test
    void invalidRequestsOnActionResource() throws Exception {
        final String knownTargetId = KNOWN_TARGET_ID_1;
        final Long tenantId = 1L;
        // target does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId + "/" + MgmtRestConstants.TARGET_V1_ACTIONS)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, "knownTargetId_knownTargetId", actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // action does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, "knownTargetId_knownTargetId", "12321")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, "knownTargetId_knownTargetId", actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // Invalid content
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, "knownTargetId_knownTargetId", actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

    }

    @Test
    void invalidRequestsOnActionStatusResource() throws Exception {
        final Long tenantId = 1L;
        // target does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + KNOWN_CONTROLLER_ID + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/1/status")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        final List<Action> actions = generateTargetWithTwoUpdatesWithOneOverride();
        final Long actionId = actions.get(0).getId();

        // should work now
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());


        // action does not exist
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + KNOWN_CONTROLLER_ID + "/" + MgmtRestConstants.TARGET_V1_ACTIONS + "/12321/status")).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // not allowed methods
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ACTION_STATUS_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID, actionId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    void getControllerAttributesViaTargetResourceReturnsAttributesWithOk() throws Exception {
        final Map<String, String> knownControllerAttrs = new HashMap<>();
        knownControllerAttrs.put("a", "1");
        knownControllerAttrs.put("b", "2");
        final Long tenantId = 1L;
        controllerManagement.updateControllerAttributes(KNOWN_CONTROLLER_ID, knownControllerAttrs, null);

        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_CNTRL_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is2xxSuccessful())
                .andExpect(jsonPath("$.a", equalTo("1"))).andExpect(jsonPath("$.b", equalTo("2")));

    }

    @Test
    void getControllerEmptyAttributesReturnsNoContent() throws Exception {
        // create target with attributes
        final String knownTargetId = "targetIdWithAttributes";
        final Long tenantId = 1L;
        // test query target over rest resource
        mvc.perform(get(MgmtRestConstants.TARGET_CNTRL_V1_REQUEST_MAPPING, tenantId, knownTargetId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is(HttpStatus.NO_CONTENT.value()));

    }

    @Step
    private void verifyAttributeUpdateCanBeRequested(final String knownTargetId) throws Exception {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("requestAttributes", true).toString();
        final Long tenantId = 1L;
        jsonObject.put(VEHICLE_MODEL_ID, testdataFactory.createVehicle("Test1").getId()).toString();
        final String body = String.valueOf(jsonObject);
        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId).content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(targetManagement.isControllerAttributesRequested(knownTargetId)).isTrue();
    }

    @Step
    private void verifyRequestAttributesAttributeIsOptional(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put(DESCRIPTION_STRING, "verify attribute can be missing").toString();
        final Long tenantId = 1L;

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId).content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Step
    private void verifyResettingRequestAttributesIsNotAllowed(final String knownTargetId) throws Exception {
        final String body = new JSONObject().put("requestAttributes", false).toString();
        final Long tenantId = 1L;

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + knownTargetId).content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        assertThat(targetManagement.isControllerAttributesRequested(knownTargetId)).isTrue();
    }

    @Test
    void searchTargetsUsingRsqlQuery() throws Exception {
        final int amountTargets = 10;
        createTargetsAlphabetical(amountTargets);
        final Long tenantId = 1L;

        final String rsqlFindAOrB = "controllerId==a_a,controllerId==b_b";

        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "?q=" + rsqlFindAOrB)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2))).andExpect(jsonPath(TOTAL, equalTo(2))).andExpect(jsonPath("content[0].controllerId", equalTo("a_a"))).andExpect(jsonPath("content[1].controllerId", equalTo("b_b")));
    }


    private String getCreateTargetsListJsonString(final String controllerId, final String name, final String serialNum, final String description, final String vin) {
        return "[{\"name\":\"" + name + "\",\"controllerId\":\"" + controllerId + "\",\"serialNumber\":\"" + serialNum + "\",\"description\":\"" + description + "\", \"vehicleModelId\":\"" + testTarget.getVehicleModelId() + "\", \"vin\":\"" + vin +"\"}]";
    }

    private Target createSingleTarget(final String controllerId, final String name, final String serialNumber) {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final Vehicle vehicle = testdataFactory.createVehicle(DCROSS);
        targetManagement.create(entityFactory.target().create().controllerId(controllerId).name(name).serialNumber(serialNumber).description(TARGET_DESCRIPTION_TEST).vehicleModelId(vehicle.getId()).vin(TESTVIN));
        return controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId, name, serialNumber, vehicle.getId());
    }

    /**
     * Creating targets with the given amount by setting name, id etc from the
     * alphabet [a-z] using ASCII.
     *
     * @param amount The number of targets to create
     */
    private void createTargetsAlphabetical(final int amount) {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        char character = 'a';
        for (int index = 1; index < amount; index++) {
            final String str = String.valueOf(character);
            final Vehicle vehicle = testdataFactory.createVehicle(str);
            targetManagement.create(entityFactory.target().create().controllerId(str + "_" + str).name(str).serialNumber(str).description(str).vehicleModelId(vehicle.getId()).vin(str));
            controllerManagement.findOrRegisterTargetIfItDoesNotExist(str + "_" + str, str, str, vehicle.getId());
            character++;
        }
    }

    /**
     * helper method to create a target and start an action on it.
     *
     * @return The targetid of the created target.
     */
    private Target createTargetAndStartAction() {
        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        // assign a distribution set so we get an active update action
        assignDistributionSet(dsA, Collections.singletonList(testTarget));
        // verify active action
        final Slice<Action> actionsByTarget = deploymentManagement.findActionsByTarget(testTarget.getControllerId(), PAGE);
        assertThat(actionsByTarget.getContent()).hasSize(1);
        return targetManagement.getByControllerID(testTarget.getControllerId()).get();
    }

    @Test
    @Description("Ensures that the metadata creation through API is reflected by the repository.")
    void createMetadata() throws Exception {
        final String knownKey1 = "known.key.1";
        final String knownKey2 = "knownKey2";

        final String knownValue1 = "knownValue1";
        final String knownValue2 = "knownValue2";
        final Long tenantId = 1L;
        final JSONArray metaData1 = new JSONArray();
        metaData1.put(new JSONObject().put("key", knownKey1).put("value", knownValue1));
        metaData1.put(new JSONObject().put("key", knownKey2).put("value", knownValue2));

        mvc.perform(post(MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(metaData1.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].key", equalTo(knownKey1)))
                .andExpect(jsonPath("[0].value", equalTo(knownValue1)))
                .andExpect(jsonPath("[1].key", equalTo(knownKey2)))
                .andExpect(jsonPath("[1].value", equalTo(knownValue2)));


        final TargetMetadata metaKey1 = targetManagement.getMetaDataByControllerId(KNOWN_CONTROLLER_ID, knownKey1).get();
        final TargetMetadata metaKey2 = targetManagement.getMetaDataByControllerId(KNOWN_CONTROLLER_ID, knownKey2).get();

        assertThat(metaKey1.getValue()).isEqualTo(knownValue1);
        assertThat(metaKey2.getValue()).isEqualTo(knownValue2);

        // verify quota enforcement
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerTarget();

        final JSONArray metaData2 = new JSONArray();
        for (int i = 0; i < maxMetaData - metaData1.length() + 1; ++i) {
            metaData2.put(new JSONObject().put("key", knownKey1 + i).put(VALUE, knownValue1 + i));
        }

        mvc.perform(post(MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID).accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON).content(metaData2.toString()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden());


        // verify that the number of meta data entries has not changed
        // (we cannot use the PAGE constant here as it tries to sort by ID)
        assertThat(targetManagement.findMetaDataByControllerId(PageRequest.of(0, Integer.MAX_VALUE), KNOWN_CONTROLLER_ID).getTotalElements()).isEqualTo(metaData1.length());

    }

    @Test
    @Description("Ensures that a metadata update through API is reflected by the repository.")
    void updateMetadata() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;
        final Long tenantId = 1L;
        // prepare and create metadata for update
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;
        final String updateValue = "valueForUpdate";

        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        final JSONObject jsonObject = new JSONObject().put("key", knownKey).put(VALUE, updateValue);

        mvc.perform(put(MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING, tenantId, knownControllerId, knownKey)
                        .accept(MediaType.APPLICATION_JSON).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonObject.toString())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(updateValue)));


        final TargetMetadata updatedTargetMetadata = targetManagement.getMetaDataByControllerId(knownControllerId, knownKey).get();
        assertThat(updatedTargetMetadata.getValue()).isEqualTo(updateValue);

    }

    private void setupTargetWithMetadata(final String knownControllerId, final String knownKey, final String knownValue) {
        testdataFactory.createTarget(knownControllerId, knownControllerId, knownControllerId, testdataFactory.createVehicle("X250").getId(),knownControllerId);
        targetManagement.createMetaData(knownControllerId, Collections.singletonList(entityFactory.generateTargetMetadata(knownKey, knownValue)));
    }

    @Test
    @Description("Ensures that a metadata entry deletion through API is reflected by the repository.")
    void deleteMetadata() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;

        // prepare and create metadata for deletion
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;
        final Long tenantId = 1L;
        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(delete(MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING, tenantId, knownControllerId, knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());


        assertThat(targetManagement.getMetaDataByControllerId(knownControllerId, knownKey)).isNotPresent();
    }

    @Test
    @Description("Ensures that target metadata deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteMetadataThatDoesNotExistLeadsToNotFound() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;

        // prepare and create metadata for deletion
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;
        final Long tenantId = 1L;
        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(delete(MANAGEMENT_V_1_TENANTS + tenantId + "/targets/{controllerId}/metadata/XXX", knownControllerId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MANAGEMENT_V_1_TENANTS + tenantId + "/targets/1234/metadata/{key}", knownKey)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        assertThat(targetManagement.getMetaDataByControllerId(knownControllerId, knownKey)).isPresent();
    }

    @Test
    @Description("Ensures that a metadata entry selection through API reflectes the repository content.")
    void getSingleMetadata() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;

        // prepare and create metadata for deletion
        final String knownKey = KNOWN_KEY;
        final String knownValue = KNOWN_VALUE;
        final Long tenantId = 1L;
        setupTargetWithMetadata(knownControllerId, knownKey, knownValue);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_METADATA_KEY_V1_REQUEST_MAPPING, tenantId, knownControllerId, knownKey))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("key", equalTo(knownKey))).andExpect(jsonPath("value", equalTo(knownValue)));

    }

    @Test
    @Description("Ensures that a metadata entry paged list selection through API reflectes the repository content.")
    void getPagedListOfMetadata() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;

        final int totalMetadata = 10;
        final int limitParam = 5;
        final String offsetParam = "0";
        final String knownKeyPrefix = KNOWN_KEY;
        final String knownValuePrefix = KNOWN_VALUE;
        final Long tenantId = 1L;
        setupTargetWithMetadata(knownControllerId, knownKeyPrefix, knownValuePrefix, totalMetadata);

        mvc.perform(get(MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING + "?offset=" + offsetParam + "&limit=" + limitParam, tenantId,
                        knownControllerId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("size", equalTo(limitParam))).andExpect(jsonPath("total", equalTo(totalMetadata)))
                .andExpect(jsonPath("content[0].key", equalTo("knownKey0")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue0")));


    }

    private void setupTargetWithMetadata(final String knownControllerId, final String knownKeyPrefix, final String knownValuePrefix, final int totalMetadata) {
        testdataFactory.createTarget(knownControllerId);

        final List<MetaData> targetMetadataEntries = new LinkedList<>();
        for (int index = 0; index < totalMetadata; index++) {
            targetMetadataEntries.add(entityFactory.generateTargetMetadata(knownKeyPrefix + index, knownValuePrefix + index));
        }
        targetManagement.createMetaData(knownControllerId, targetMetadataEntries);
    }

    @Test
    @Description("Ensures that a target metadata filtered query with value==knownValue1 parameter returns only the metadata entries with that value.")
    void searchDistributionSetMetadataRsql() throws Exception {
        final String knownControllerId = TARGET_ID_WITH_METADATA;

        final int totalMetadata = 10;
        final String knownKeyPrefix = KNOWN_KEY;
        final String knownValuePrefix = KNOWN_VALUE;
        final Long tenantId = 1L;
        setupTargetWithMetadata(knownControllerId, knownKeyPrefix, knownValuePrefix, totalMetadata);

        final String rsqlSearchValue1 = "value==knownValue1";

        mvc.perform(get(MgmtRestConstants.TARGET_ID_METADATA_V1_REQUEST_MAPPING + "?q=" + rsqlSearchValue1, tenantId, knownControllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(1)))
                .andExpect(jsonPath("total", equalTo(1))).andExpect(jsonPath("content[0].key", equalTo("knownKey1")))
                .andExpect(jsonPath("content[0].value", equalTo("knownValue1")));

    }

    @Test
    @Description("A request for assigning multiple DS to a target results in a Bad Request when multiassignment in disabled.")
    void multiassignmentRequestNotAllowedIfDisabled() throws Exception {
        final List<Long> dsIds = testdataFactory.createDistributionSets(2).stream().map(DistributionSet::getId).collect(Collectors.toList());

        final JSONArray body = new JSONArray();
        final Long tenantId = 1L;
        dsIds.forEach(id -> body.put(getAssignmentObject(id, MgmtRolloutUserAcceptanceRequired.NO, 67)));

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Passing an array in assignment request is allowed if multiassignment is disabled and array size in 1.")
    void multiassignmentRequestAllowedIfDisabledButHasSizeOne() throws Exception {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long tenantId = 1L;
        final JSONArray body = new JSONArray().put(getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO));
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());

    }

    @Test
    @Description("Identical assignments in a single request are removed when multiassignment in disabled.")
    void identicalAssignmentInRequestAreRemovedIfMultiassignmentsDisabled() throws Exception {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final Long tenantId = 1L;
        final JSONObject assignment = getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO);
        final JSONArray body = new JSONArray().put(assignment).put(assignment);

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath("total", equalTo(1)));

    }

    @Test
    @Description("Assign multiple DSs to a target in one request with multiassignments enabled.")
    void multiAssignment() throws Exception {
        final List<Long> dsIds = testdataFactory.createDistributionSets(2).stream().map(DistributionSet::getId).collect(Collectors.toList());

        final JSONArray body = new JSONArray();
        final Long tenantId = 1L;
        dsIds.forEach(id -> body.put(getAssignmentObject(id, MgmtRolloutUserAcceptanceRequired.NO, 76)));

        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(body.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath("total", equalTo(2)));

    }

    @Test
    @Description("An assignment request containing a weight is only accepted when weight is valid and multi assignment is on.")
    void weightValidation() throws Exception {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 98;
        final Long tenantId = 1L;
        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO, weight);
        final JSONObject bodyInvalid = getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO, Action.WEIGHT_MIN - 1);

        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(bodyInvalid.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.debug", notNullValue()));

        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(bodyValid.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());


        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("An assignment request containing a valid weight when multi assignment is off.")
    void weightWithSingleAssignment() throws Exception {
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO, 98);
        final Long tenantId = 1L;
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId())
                .header("session-id", SESSION_ID_HEADER).content(bodyValid.toString())


                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.debug", notNullValue()));
    }

    @Test
    @Description("An assignment request containing a valid weight when multi assignment is on.")
    void weightWithMultiAssignment() throws Exception {
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int weight = 98;
        final Long tenantId = 1L;
        final JSONObject bodyValid = getAssignmentObject(dsId, MgmtRolloutUserAcceptanceRequired.NO, weight);

        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.TARGET_ID_ASSIGN_DS_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId()).content(bodyValid.toString())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());


        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).get().collect(Collectors.toList());
        assertThat(actions).size().isEqualTo(1);
        assertThat(actions.get(0).getWeight()).get().isEqualTo(weight);
    }

    @Test
    @Description("Get weight of action")
    void getActionWeight() throws Exception {
        String targetId = testTarget.getControllerId();
        final Long dsId = testdataFactory.createDistributionSet().getId();
        final int customWeightHigh = 800;
        final int customWeightLow = 300;
        assignDistributionSet(dsId, targetId);
        enableMultiAssignments();
        assignDistributionSet(dsId, targetId, customWeightHigh);
        assignDistributionSet(dsId, targetId, customWeightLow);
        final Long tenantId = 1L;
        // POSTGRESQL sets null values at the end, not the beginning
        if (Database.POSTGRESQL.equals(jpaProperties.getDatabase())) {
            mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, targetId)
                            .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "WEIGHT:ASC"))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                    .andExpect(jsonPath("content.[0].weight", equalTo(customWeightLow)))
                    .andExpect(jsonPath("content.[1].weight", equalTo(customWeightHigh)))
                    .andExpect(jsonPath("content.[2].weight").doesNotExist());
        } else {
            mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, targetId)
                            .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "WEIGHT:ASC"))
                    .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                    .andExpect(jsonPath("content.[0].weight").doesNotExist())
                    .andExpect(jsonPath("content.[1].weight", equalTo(customWeightLow)))
                    .andExpect(jsonPath("content.[2].weight", equalTo(customWeightHigh)));
        }

    }

    @Test
    @Description("An action provides information of the rollout it was created for (if any).")
    void getActionWithRolloutInfo() throws Exception {

        // setup
        final int amountTargets = 10;
        final Long tenantId = 1L;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, "trg", "trg");


        Rollout rollout = createRolloutWithDependencies("rollout", testdataFactory.createDistributionSet(), targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // get all actions for the first target
        final Target target = targets.get(0);
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTIONS_V1_REQUEST_MAPPING, tenantId, target.getControllerId())).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andExpect(jsonPath("content.[0].rollout", equalTo(rollout.getId().intValue()))).andExpect(jsonPath("content.[0].rolloutName", equalTo(rollout.getName())));

        final Slice<Action> action = deploymentManagement.findActionsByTarget(target.getControllerId(), PageRequest.of(0, 100));
        assertThat(action.getContent()).hasSize(1);
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, target.getControllerId(), action.getContent().get(0).getId())).andExpect(status().isOk()).andDo(MockMvcResultPrinter.print()).andExpect(jsonPath("$.rollout", equalTo(rollout.getId().intValue()))).andExpect(jsonPath("$.rolloutName", equalTo(rollout.getName()))).andExpect(jsonPath("$._links.rollout.href", containsString(MANAGEMENT_V_1_TENANTS + tenantId + "/rollouts/" + rollout.getId().intValue())));
    }

    @Test
    @Description("Ensures that a post request for creating targets with target type works.")
    void createTargetsWithTargetType() throws Exception {
        final TargetType type1 = testdataFactory.createTargetType("typeWithDs", Collections.singletonList(standardDsType));
        final TargetType type2 = testdataFactory.createTargetType("typeWithOutDs", Collections.singletonList(standardDsType));
        final Long tenantId = 1L;
        final Target test1 = entityFactory.target().create().controllerId(ID_1_SRNO_1).name(TARGET_WITHOUT_TYPE).serialNumber("srno1").vehicleModelId(testdataFactory.createVehicle(DCROSS).getId()).securityToken(TOKEN).description(TESTID_1).vin(ID_1_SRNO_1.split("_")[0]).build();
        final Target test2 = entityFactory.target().create().controllerId(ID_2_SRNO_2).name(TARGET_OF_TYPE_1).serialNumber("srno2").vehicleModelId(testdataFactory.createVehicle("Dcross2").getId()).targetType(type1.getId()).description(TESTID_2).vin(ID_2_SRNO_2.split("_")[0]).build();
        final Target test3 = entityFactory.target().create().controllerId(ID_3_SRNO_3).name(TARGET_OF_TYPE_2).serialNumber("srno3").vehicleModelId(testdataFactory.createVehicle("Dcross1").getId()).targetType(type2.getId()).description(TESTID_3).vin(ID_3_SRNO_3.split("_")[0]).build();
        final String hrefType1 = "http://localhost/management/v1/tenants/1/target-types/" + type1.getId();

        final List<Target> targets = Arrays.asList(test1, test2, test3);

        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING, tenantId).content(JsonBuilder.targets(targets, true))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("[0].name", equalTo("targetWithoutType")))
                .andExpect(jsonPath("[0].controllerId", equalTo("id1_srno1")))
                .andExpect(jsonPath("[0].description", equalTo("testid1")))
                .andExpect(jsonPath("[0].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[0].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[0].securityToken", equalTo("token")))
                .andExpect(jsonPath("[0].targetType").doesNotExist())
                .andExpect(jsonPath("[0].serialNumber", equalTo("srno1")))
                .andExpect(jsonPath("[1].name", equalTo("targetOfType1")))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].controllerId", equalTo("id2_srno2")))
                .andExpect(jsonPath("[1].description", equalTo("testid2")))
                .andExpect(jsonPath("[1].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[1].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[1].targetType", equalTo(type1.getId().intValue())))
                .andExpect(jsonPath("[1].serialNumber", equalTo("srno2")))
                .andExpect(jsonPath("[2].name", equalTo("targetOfType2")))
                .andExpect(jsonPath("[2].controllerId", equalTo("id3_srno3")))
                .andExpect(jsonPath("[2].description", equalTo("testid3")))
                .andExpect(jsonPath("[2].createdAt", not(equalTo(0))))
                .andExpect(jsonPath("[2].createdBy", equalTo("bumlux")))
                .andExpect(jsonPath("[2].serialNumber", equalTo("srno3")))
                .andExpect(jsonPath("[2].targetType", equalTo(type2.getId().intValue()))).andReturn();

        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, test2.getControllerId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_NAME, equalTo(TARGET_OF_TYPE_1)))
                .andExpect(jsonPath(JSON_PATH_CONTROLLERID, equalTo(ID_2_SRNO_2)))
                .andExpect(jsonPath(JSON_PATH_TYPE, equalTo(type1.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_ROOT_FIELD_DESCRIPTION, equalTo(TESTID_2)))
                .andExpect(jsonPath("$._links.targetType.href", equalTo(hrefType1))).andReturn();

        final Target target1 = assertTarget(ID_1_SRNO_1, TARGET_WITHOUT_TYPE, TESTID_1);
        assertThat(target1.getTargetType()).isNull();
        assertThat(target1.getSecurityToken()).isEqualTo(TOKEN);

        final Target target2 = assertTarget(ID_2_SRNO_2, TARGET_OF_TYPE_1, TESTID_2);
        assertThat(target2.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithDs");

        final Target target3 = assertTarget(ID_3_SRNO_3, TARGET_OF_TYPE_2, TESTID_3);
        assertThat(target3.getTargetType()).extracting(TargetType::getName).isEqualTo("typeWithOutDs");
    }

    @Test
    @Description("Ensures that a post request for creating target with target type works.")
    void createTargetWithExistingTargetType() throws Exception {
        // create target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes(TARGETTYPE, 1);
        assertThat(targetTypes).hasSize(1);
        final Long tenantId = 1L;
        final Target target = entityFactory.target().create().controllerId(CONTROLLER_ID3).name(TESTTARGET).serialNumber(TEST_SERIAL_NUM).vehicleModelId(testdataFactory.createVehicle("TEST").getId()).targetType(targetTypes.get(0).getId()).vin(CONTROLLER_ID3.split("_")[0]).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false);

        // test query target over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isCreated()).andExpect(jsonPath(CONTROLLER_ID2, equalTo(CONTROLLER_ID3))).andExpect(jsonPath("[0].targetType", equalTo(targetTypes.get(0).getId().intValue())));

        assertThat(targetManagement.getByControllerID(CONTROLLER_ID3).get().getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());
    }

    @Test
    @Description("Ensures that a PUT request for updating the description of a target type works and that no other fields are updated.")
    void updateTargetTypeDescription() throws Exception {
        // Create target types
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes(TARGETTYPE, 2);
        assertThat(targetTypes).hasSize(2);

        final String controllerId = TARGETCONTROLLER;
        final Target target = testdataFactory.createTarget(controllerId, TESTTARGET, TEST_SERIAL_NUM, targetTypes.get(0).getId(), testdataFactory.createVehicle(DCROSS).getId(), TESTVIN);
        final Long tenantId = 1L;
        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // Update the description of the target type over the REST resource
        final String newDescription = "Updated target type description";
        final String body = new JSONObject().put(DESCRIPTION_STRING, newDescription).toString();

        // Perform the PUT request
        MvcResult result = mvc.perform(put(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, controllerId)
                        .content(body)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(MockMvcResultPrinter.print()) // Print the response for debugging

                .andReturn();

        // Get the response content
        String responseContent = result.getResponse().getContentAsString();

        // Perform the GET request to verify the update
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, controllerId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    @Description("Ensures that a post request for creating targets with unknown target type fails.")
    void addingNonExistingTargetTypeInTargetShouldFail() throws Exception {
        final long unknownTargetTypeId = 999;
        final String errorMsg = String.format("TargetType with given identifier {%s} does not exist.", unknownTargetTypeId);
        final Long tenantId = 1L;
        final Optional<TargetType> targetType = targetTypeManagement.get(unknownTargetTypeId);
        assertThat(targetType).isNotPresent();

        final String controllerId = "targetcontroller_targetSerialNum";
        final Target target = entityFactory.target().create().controllerId(controllerId).serialNumber("targetSerialNum").name(TESTTARGET).build();

        final String targetList = JsonBuilder.targets(Collections.singletonList(target), false, unknownTargetTypeId);

        // post target over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))).content(targetList).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound()).andExpect(jsonPath("message", Matchers.containsString(errorMsg)));
    }

    @Test
    @Description("Ensures that a post request for assign target type to target works.")
    void assignTargetTypeToTarget() throws Exception {
        // create target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType(TARGETTYPE);
        assertThat(targetType).isNotNull();
        final Long tenantId = 1L;
        // create target
        assertThat(testTarget).isNotNull();

        // assign target type over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                        .content("{\"id\":" + targetType.getId() + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());


        assertThat(targetManagement.getByControllerID(KNOWN_CONTROLLER_ID).get().getTargetType().getId()).isEqualTo(targetType.getId());
    }

    @Test
    @Description("Ensures that a post request for assign a invalid target type to target fails.")
    void assignInvalidTargetTypeToTargetFails() throws Exception {
        // Invalid target type ID
        final long invalidTargetTypeId = 999;
        final Long tenantId = 1L;
        // create target
        final String targetControllerId = TARGETCONTROLLER;
        assertThat(testTarget).isNotNull();

        // assign invalid target type over rest resource
        mvc.perform(post(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, targetControllerId)
                        .content("{\"id\":" + invalidTargetTypeId + "}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // verify response json exception message if body does not include id
        // field
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, targetControllerId)
                        .content("{\"unknownfield\":" + invalidTargetTypeId + "}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(mvcResult.getResolvedException()).isNotNull();
        assertThat(mvcResult.getResolvedException().getClass()).isNotNull();
        Class<? extends Throwable> t = mvcResult.getResolvedException().getClass();
        assertThat(t.getName()).isEqualTo(ConstraintViolationException.class.getName());
        final ExceptionInfo exceptionInfo = ResourceUtility.convertException(mvcResult.getResponse().getContentAsString());

        // Conflict Resolution - COSMOS-133
        assertThat(exceptionInfo.getDebug()).isNotNull();
        assertThat(exceptionInfo.getMessage()).contains("targetTypeId");
    }

    @Test
    @Description("Ensures that a delete request for unassign target type from target works.")
    void unassignTargetTypeFromTarget() throws Exception {
        // create target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes(TARGETTYPE, 1);
        assertThat(targetTypes).hasSize(1);
        final Long tenantId = 1L;
        final String targetControllerId = TARGETCONTROLLER;
        final Target target = testdataFactory.createTarget(targetControllerId, TESTTARGET, TEST_SERIAL_NUM, targetTypes.get(0).getId(), testdataFactory.createVehicle(DCROSS).getId(), TESTVIN);

        assertThat(target).isNotNull();
        assertThat(target.getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // unassign target type over rest resource
        mvc.perform(delete(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, targetControllerId)
                .contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());


        assertThat(targetManagement.getByControllerID(targetControllerId).get().getTargetType()).isNull();
    }

    @Test
    void invalidRequestsOnTargetTypeResource() throws Exception {
        final String knownTargetId = "targetId";
        testdataFactory.createTargetType(TARGETTYPE, Collections.emptyList());
        final Long tenantId = 1L;
        // GET is not allowed
        mvc.perform(get(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // PUT is not allowed
        mvc.perform(put(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());


        // POST does not exist with path parameter targettype
        mvc.perform(post(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))

                + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING + "/123", knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // DELETE does not exist with path parameter targettype
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId))

                + MgmtRestConstants.TARGET_TARGET_TYPE_V1_REQUEST_MAPPING + "/123", knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // Invalid content
        mvc.perform(post(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // Bad request if id field is missing
        mvc.perform(post(MgmtRestConstants.TARGET_MNG_V1_REQUEST_MAPPING, tenantId, knownTargetId).content("{\"unknownfield\":123}").contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    void getAutoConfirmActive(final String initiator, final String remark) throws Exception {
        confirmationManagement.activateAutoConfirmation(KNOWN_CONTROLLER_ID, initiator, remark);
        final Long tenantId = 1L;
        // GET with all possible responses
        mvc.perform(get(MgmtRestConstants.TARGET_ID_CONFIRM_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(Boolean.TRUE)))
                .andExpect(initiator == null ? jsonPath("initiator").doesNotExist()
                        : jsonPath("initiator", equalTo(initiator)))
                .andExpect(remark == null ? jsonPath("remark").doesNotExist() : jsonPath("remark", equalTo(remark)))
                .andExpect(jsonPath("_links.deactivate").exists())
                .andExpect(jsonPath("_links.activate").doesNotExist());

    }

    @Test
    void getAutoConfirmStateFromTargetsEndpoint() throws Exception {
        final Long tenantId = 1L;
        // GET if active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(CONTENT_0_AUTO_CONFIRM_ACTIVE).doesNotExist());

        enableConfirmationFlow();

        // GET if not active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(CONTENT_0_AUTO_CONFIRM_ACTIVE, equalTo(Boolean.FALSE)));

        confirmationManagement.activateAutoConfirmation(KNOWN_CONTROLLER_ID, "test", REMARK);

        // GET if active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(CONTENT_0_AUTO_CONFIRM_ACTIVE, equalTo(Boolean.TRUE)));
    }

    @Test
    void getAutoConfirmNotActive() throws Exception {
        final String knownTargetId = KNOWN_TARGET_ID_1;
        final Long tenantId = 1L;
        // GET for not existing target
        mvc.perform(get(MgmtRestConstants.TARGET_ID_CONFIRM_V1_REQUEST_MAPPING, tenantId, knownTargetId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // GET for auto-confirm not active
        mvc.perform(get(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace("{tenantId}", String.valueOf(tenantId))
                                + "/{knownTargetId}/" + MgmtRestConstants.TARGET_V1_AUTO_CONFIRM,
                        "knownTargetId_knownTargetId")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("active", equalTo(Boolean.FALSE))).andExpect(jsonPath("initiator").doesNotExist())
                .andExpect(jsonPath("remark").doesNotExist()).andExpect(jsonPath("_links.activate").exists());

    }

    @Test
    void autoConfirmStateReferenceOnTarget() throws Exception {
        final Long tenantId = 1L;
        // GET with confirmation flow not active should not expose
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").doesNotExist())
                .andExpect(jsonPath("_links.autoConfirm").doesNotExist());


        enableConfirmationFlow();

        // GET with confirmation flow active should expose
        mvc.perform(get(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirmActive").exists()).andExpect(jsonPath("_links.autoConfirm").exists());

    }

    @Test
    @Description("Verifies that the status code that was reported in the last action status update is correctly exposed via the action.")
    void lastActionStatusCode() throws Exception {

        // prepare test
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Action action = getFirstAssignedAction(assignDistributionSet(dsA, Collections.singletonList(testTarget)));
        final Long tenantId = 1L;

        // no status update yet -> no status code
        mvc.perform(get(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), action.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("lastStatusCode").doesNotExist());

    }

    private Action updateActionStatus(final Action action, final DeviceActionStatus status, final Integer statusCode) {
        return updateActionStatus(action, status, statusCode, null);
    }

    private Action updateActionStatus(final Action action, final DeviceActionStatus status, final Integer statusCode, final String message) {

        assertThat(action).isNotNull();
        assertThat(status).isNotNull();

        final ActionStatusCreate actionStatus = entityFactory.actionStatus().create(action.getId());
        actionStatus.status(status);
        if (statusCode != null) {
            actionStatus.code(statusCode);
        }
        if (message != null) {
            actionStatus.message(message);
        }

        return controllerManagement.addUpdateActionStatus(actionStatus, null);
    }

    @Test
    @Description("When Target is part of rollout, delete operation fails with a validation error")
    void givenTargetPartOfRolloutWhenDeleteItFailsWithValidationError() throws Exception {
        final Long tenantId = 1L;
        DistributionSet ds = testdataFactory.createDistributionSet("");
        createRolloutWithDependencies("rollout1", ds, List.of(testTarget));
        createRolloutWithDependencies("rollout2", ds, List.of(testTarget));

        String expectedErrorMessage = String.format(
                "Unable to delete %s, since it is part of Rollouts [%s]",
                testTarget.getName(),
                "rollout1, rollout2"
        );

        // Perform the delete operation and expect a validation error
        mvc.perform(delete(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + KNOWN_CONTROLLER_ID)).andExpect(status().isBadRequest()).andExpect(result -> assertEquals(expectedErrorMessage, result.getResolvedException().getMessage()));
    }

    @Test
    @Description("When Target is not part of any rollout, delete operation is successful")
    void givenTargetNotPartOfAnyRolloutWhenDeleteIsSuccessful() throws Exception {
        final Long tenantId = 1L;

        // Create a request body with description, name, and address
        final String body = new JSONObject()
                .put("description", "Updated Description")
                .put("name", "Updated Name")
                .put("address", "Updated Address")
                .toString();

        DistributionSet ds = testdataFactory.createDistributionSet("");

        // Perform the delete operation and expect a validation error
        mvc.perform(delete(MgmtRestConstants.TARGET_ID_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID))
                .andExpect(status().isOk());

    }

    @Test
    @Description("When Target is not part of any active rollout, update tenant operation returns success")
    void givenTargetNotPartOfActiveRolloutthenUpdateTenantSuccess() throws Exception {
        final Long tenantId = 1L;

        // Create a request body with description, name, and address
        final String body = new JSONObject().put(TENANT, "EMEA").toString();

        mvc.perform(put(MgmtRestConstants.TARGET_ID_UPDATETENANT_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                .contentType(MediaType.APPLICATION_JSON).content(body));


    }

    @Test
    @Description("When current tenant and requested tenant are same, update tenant operation should fail")
    void givenTenantAndRequestedTenantAreSameUpdateTenantShouldFail() throws Exception {
        final Long tenantId = 1L;

        // Create a request body with description, name, and address
        final String body = new JSONObject().put(TENANT, "DEFAULT").toString();

        mvc.perform(put(MgmtRestConstants.TARGET_ID_UPDATETENANT_V1_REQUEST_MAPPING, tenantId, KNOWN_CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON) // Specify content type as JSON

                        .content(body) // Add request body
                        .accept(MediaType.APPLICATION_JSON)) // Specify JSON response format
                .andExpect(status().isBadRequest()) // Check for 400 Bad Request
                .andExpect(result -> assertEquals("New tenant must be different from the current tenant", result.getResolvedException().getMessage())); // Validate exception message

    }

    @Test
    @Description("update tenant with all the validation returns success")
    void updateTenantForTargetwithAllValidationRetrunSuccess() throws Exception {
        final Long tenantId = 1L;
        final String newTenant = "EMEA";

        // Create a request body with description, name, and address
        final String body = new JSONObject().put(TENANT, newTenant).toString();

        mvc.perform(put(MgmtRestConstants.TARGET_V1_REQUEST_MAPPING.replace(TENANT_ID, String.valueOf(tenantId)) + "/" + KNOWN_CONTROLLER_ID + UPDATE_TENANT_PATH).contentType(MediaType.APPLICATION_JSON).content(body).accept(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());

    }

    @Test
    @Description("Given case insensitive enum, when updating action id for target, then return success")
    void givenCaseInsensitiveEnumWhenUpdatingActionIdForTargetThenReturnSuccess() throws Exception {
        final DistributionSet set = testdataFactory.createDistributionSet();
        final Long tenantId = 1L;
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(set.getId(), testTarget.getControllerId(), MgmtRolloutUserAcceptanceRequired.YES));
        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.YES);

        String body = new JSONObject().put("userAcceptanceRequired", "No").toString();
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), actionId).content(body)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(FORCE_TYPE, equalTo("no")));

        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.NO);

        body = new JSONObject().put("userAcceptanceRequired", "NO").toString();
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), actionId).content(body)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(FORCE_TYPE, equalTo("no")));

        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.NO);

        body = new JSONObject().put("userAcceptanceRequired", "no").toString();
        mvc.perform(put(MgmtRestConstants.TARGET_ID_ACTION_ID_V1_REQUEST_MAPPING, tenantId, testTarget.getControllerId(), actionId).content(body)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(FORCE_TYPE, equalTo("no")));

        assertThat(deploymentManagement.findAction(actionId).get().getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.NO);
    }

    @Test
    @Description("When a target is added, then targetId is shared in response as first field.")
    void givenTargetRequestWhenCreateTargetThenReturnReponseWithTargetIdOnTop() throws Exception {
        long vehicleModelId = invokeAddVehicleModelApi().get(0).getId();
        String response = invokeCreateTargetApiAndReturnResponseAsString(vehicleModelId, 2);
        JsonNode rootNode = objectMapper.readTree(response);
        Assertions.assertEquals(2, rootNode.size());
        JsonNode firstResponse = rootNode.get(0);
        String firstKey = firstResponse.fieldNames().next();
        Assertions.assertEquals("targetId", firstKey);
        JsonNode secondResponse = rootNode.get(1);
        firstKey = secondResponse.fieldNames().next();
        Assertions.assertEquals("targetId", firstKey);
    }

    @Test
    void givenUnknownVinWhenGetRolloutHistoryThen404() throws Exception {
        final Long tenantId = 1L;
        mvc.perform(get("/management/v1/tenants/{tenantId}/targets/{vin}/history", tenantId, "unknown-vin"))
                .andExpect(status().isNotFound());
    }

    @Test
    void givenVinWithNoRolloutsWhenGetRolloutHistoryThenEmptyList() throws Exception {
        final Long tenantId = 1L;
        String vin = "vinNoRollouts";
        testdataFactory.createTarget(vin, vin, vin, testdataFactory.createVehicle("ModelX").getId(), vin);
        mvc.perform(get(MgmtRestConstants.TARGET_ID_FETCH_VIN_HISTORY, tenantId, vin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void givenVinWithRolloutsWhenGetRolloutHistoryThenOk() throws Exception {
        final Long tenantId = 1L;
        String vin = "vinWithRolloutsSec";
        Target target = testdataFactory.createTarget(vin, vin, vin, testdataFactory.createVehicle("ModelSec").getId(), vin);
        DistributionSet ds = testdataFactory.createDistributionSet("");
        createRolloutWithDependencies("rollout-single", ds, List.of(target));
        mvc.perform(get(MgmtRestConstants.TARGET_ID_FETCH_VIN_HISTORY, tenantId, vin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scomoid").value("application"))
                .andExpect(jsonPath("$[0].targetVersion").value("sample3"))
                .andExpect(jsonPath("$[0].latestStatus").exists())
                .andExpect(jsonPath("$[0].message").exists())
                .andExpect(jsonPath("$[0].latestStatusDate").exists())
                .andExpect(jsonPath("$[0].rolloutName").exists())
                .andExpect(jsonPath("$[0].rolloutStartDate").exists())
                .andExpect(jsonPath("$[0].rolloutEndDate").exists());
    }


    @Test
    void givenVinWithMultipleRolloutsWhenGetRolloutHistoryThenOk() throws Exception {
        final Long tenantId = 1L;
        String vin = "vinWithMultipleRollouts";
        Target target = testdataFactory.createTarget(vin, vin, vin, testdataFactory.createVehicle("ModelMulti").getId(), vin);
        DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");
        createRolloutWithDependencies("rollout1", ds1, List.of(target));
        createRolloutWithDependencies("rollout2", ds2, List.of(target));
        createRolloutWithDependencies("rollout3", ds3, List.of(target));
        mvc.perform(get(MgmtRestConstants.TARGET_ID_FETCH_VIN_HISTORY, tenantId, vin))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.scomoid=='ds1application' && @.targetVersion=='sample3')]").exists())
                .andExpect(jsonPath("$[?(@.scomoid=='ds2 Firmware' && @.targetVersion=='sample1')]").exists())
                .andExpect(jsonPath("$[?(@.scomoid=='ds2app runtime' && @.targetVersion=='sample2')]").exists())
                .andExpect(jsonPath("$[?(@.scomoid=='ds1app runtime' && @.targetVersion=='sample2')]").exists())
                .andExpect(jsonPath("$[?(@.scomoid=='ds1 Firmware' && @.targetVersion=='sample1')]").exists())
        ;
;
    }

}
