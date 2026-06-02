package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.vehicle.dto.EcuModelAssignmentRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleResponse;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtEcuModelMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtVehicleMapper;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import jakarta.validation.constraints.NotNull;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;


import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MgmtVehicleResource} {@link RestController}.
 */
@Feature("Component Tests - Management API")
@Story("Vehicle")
public class MgmtVehicleResourceTest extends AbstractManagementApiIntegrationTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String TEST_VM_01 = "TestVM01";
    private static final String STLAB = "STLAB";

    private static final long tenantId = 1L;
    private static final String KNOWN_TARGET_ID = "knownTargetId";
    public static final String CONTROLLER_ID1 = "knownTargetId1";
    public static final String CONTROLLER_ID2 = "knownTargetId2";
    static Random random = new SecureRandom();
    private List<Vehicle> savedVehicle;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private VehicleManagement vehicleManagement;

    protected static ObjectMapper getMapper() {
        return objectMapper;
    }

    private static ClientAndServer mockServer;
    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

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
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target", "sp_vehicle_ecu", "sp_vehicle_model", "sp_rollout");
    }

    @BeforeEach
    void init() throws Exception {
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Ensure Adding New Vehicle Model name will return success with Id and other details in response")
    void givenNewVmWhenAddThenAdded() throws Exception {

        MgmtVehicleRequest vehicleRequest1 = buildTestVehicleRequest();
        List<MgmtVehicleRequest> vehicleRequests = List.of(vehicleRequest1);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(getMapper().writeValueAsString(vehicleRequests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();
        List<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(mgmtVehicleResponses.size(), vehicleRequests.size());
        Assertions.assertEquals(vehicleRequests.get(0).getName(), mgmtVehicleResponses.get(0).getName());
    }

    @Test
    @Description("Ensure Adding Multiple Vehicle Model names will return success with Id and other details in response")
    void givenMultipleVmWhenAddThenAllAdded() throws Exception {
        MgmtVehicleRequest vehicleRequest1 = buildTestVehicleRandomRequest();
        MgmtVehicleRequest vehicleRequest2 = buildTestVehicleRandomRequest();
        List<MgmtVehicleRequest> vehicleRequests = Arrays.asList(vehicleRequest1, vehicleRequest2);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(getMapper().writeValueAsString(vehicleRequests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();
        List<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(mgmtVehicleResponses.size(), vehicleRequests.size());
        Assertions.assertEquals(vehicleRequests.get(0).getName(), mgmtVehicleResponses.get(0).getName());
        Assertions.assertEquals(vehicleRequests.get(1).getName(), mgmtVehicleResponses.get(1).getName());
    }

    @Test
    @Description("Ensure Adding Same Vehicle Model names will return throw Exception")
    void givenSameVmWhenAddThenError() throws Exception {

        MgmtVehicleRequest vehicleRequest1 = buildTestVehicleRequest();
        MgmtVehicleRequest vehicleRequest2 = buildTestVehicleRequest();
        List<MgmtVehicleRequest> vehicleRequests = Arrays.asList(vehicleRequest1, vehicleRequest2);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(getMapper().writeValueAsString(vehicleRequests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is4xxClientError()).andReturn();
        String errResponseString = mvcResult.getResponse().getContentAsString();
        JSONObject jsonObject = new JSONObject(errResponseString);
        Assertions.assertEquals("One or More Vehicle Model/s Name already exists", jsonObject.optString("message"));
    }

    @Test
    @Description("Ensure Adding Invalid Vehicle Model names will return throw exception")
    void givenInvalidVmWhenAddThenError() throws Exception {

        MgmtVehicleRequest vehicleRequest1 = buildTestVehicleRandomRequest();
        vehicleRequest1.setName("");
        List<MgmtVehicleRequest> vehicleRequests = List.of(vehicleRequest1);

        mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING).content(getMapper().writeValueAsString(vehicleRequests))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
        MgmtVehicleRequest vehicleRequest2 = buildTestVehicleRandomRequest();
        vehicleRequest2.setName(null);
        List<MgmtVehicleRequest> vehicleRequests2 = List.of(vehicleRequest2);

        mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING).content(getMapper().writeValueAsString(vehicleRequests2))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure updating Vehicle Model for given Vehicle Model ID returns success")
    void givenVmWhenUpdateThenVmUpdated() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRandomRequest();
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId()).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Ensure updating Vehicle Model for an invalid Vehicle Model ID returns a Bad Request with the expected error message")
    void givenInvalidVmIdWhenUpdateThenBadRequest() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRandomRequest();

        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        "invalid-id")
                        .content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This API accepts a Long value for the path variable.")); // Custom error message
    }


    @Test
    @Description("Ensure updating Vehicle Model with invalid name for given Vehicle Model ID returns bad request ")
    void givenInvalidVmNameWhenUpdateThenError() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRandomRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRandomRequest();
        String testVmName = null;
        updatedVehicleRequest.setName(testVmName);
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId()).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
        testVmName = "";
        updatedVehicleRequest.setName(testVmName);
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId()).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure updating Vehicle Model for non existing Vehicle Model ID returns not found")
    void givenVmNameWhenUpdateNonExistingVmThenError() throws Exception {
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRequest();
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        random.nextInt(10000)).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure updating Vehicle Model for invalid type of Vehicle Model ID returns bad request")
    void givenInvalidTypeVmIdWhenUpdateThenError() throws Exception {
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRequest();
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        "test" + random.nextInt(10000)).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure updating Vehicle Model for given Vehicle Model ID when associated with target returns bad request")
    void givenVmAssociatedWithTargetWhenUpdateThenThrowsError() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        MgmtVehicleRequest updatedVehicleRequest = buildTestVehicleRandomRequest();
        testdataFactory.createTarget(KNOWN_TARGET_ID, KNOWN_TARGET_ID, KNOWN_TARGET_ID,
                savedVehicle.get(0).getId(),KNOWN_TARGET_ID);
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId()).content(getMapper().writeValueAsString(updatedVehicleRequest))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure deleting Vehicle Model for given Vehicle Model ID returns success")
    void givenVmWhenDeleteThenVmDeleted() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                savedVehicle.get(0).getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    @Description("Ensure deleting Vehicle Model for non existing Vehicle Model ID returns not found")
    void givenVmWhenDeleteNonExistingThenError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        random.nextInt(10000))).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure deleting Vehicle Model for invalid type of Vehicle Model ID returns bad request")
    void givenInvalidTypeVmIdWhenDeleteThenError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        TEST_VM_01 + random.nextInt(10000))).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Few or all of the ECU Models does not exist")
    void givenVehicleModelAndEcuModelListWhenAssignEcuModelNotFound() throws Exception {
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        List<EcuModelAssignmentRequest> ecuModelAssignmentRequestList = getEcuModelAssignmentRequest(ecuModels);
        EcuModelAssignmentRequest request = new EcuModelAssignmentRequest();
        request.setEcuModelId(9L);
        ecuModelAssignmentRequestList.add(request);
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        2L ).content(getMapper().writeValueAsString(ecuModelAssignmentRequestList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }


    @Test
    @Description("Ensure assigning a non-existent ECU Model returns a Bad Request due to invalid path variable type")
    void givenInvalidVehicleModelIdWhenAssignEcuModelThenBadRequest() throws Exception {
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        List<EcuModelAssignmentRequest> ecuModelAssignmentRequestList = getEcuModelAssignmentRequest(ecuModels);
        EcuModelAssignmentRequest request = new EcuModelAssignmentRequest();
        request.setEcuModelId(ecuModels.get(0).getId());
        ecuModelAssignmentRequestList.add(request);

        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, "invalid-id")
                        .content(getMapper().writeValueAsString(ecuModelAssignmentRequestList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This API accepts a Long value for the path variable."));
    }


    @Test
    @Description("Ecu List is empty throws bad request")
    void givenVehicleModelAndEmptlyEcuModelListWhenAssignBadRequest() throws Exception {
        List<EcuModelAssignmentRequest> ecuModelAssignmentList = new ArrayList<>();
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING,
                        2L ).content(getMapper().writeValueAsString(ecuModelAssignmentList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Vehicle Model Id not present")
    void givenVehicleModelAndEcuModelListWhenAssignVehicleModelNotFound() throws Exception {
        EcuModelAssignmentRequest ecuModelAssignment = new EcuModelAssignmentRequest();
        ecuModelAssignment.setEcuModelId(3L);
        List<EcuModelAssignmentRequest> ecuModelAssignmentList = Stream.of(ecuModelAssignment).toList();
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        288L).content(getMapper().writeValueAsString(ecuModelAssignmentList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ecu Models associated for a given vehicle model id")
    void givenVehicleModelAndEcuModelListWhenAssignSuccess() throws Exception {
        Long vehicleModel = testdataFactory.createVehicle("STLA").getId();
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        vehicleModel).content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Few or all of the ECU Models does not exist")
    void givenVehicleModelAndEcuModelListWhenDeleteEcuModelNotFound() throws Exception {
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        List<EcuModelAssignmentRequest> ecuModelAssignmentRequestList = getEcuModelAssignmentRequest(ecuModels);
        EcuModelAssignmentRequest request = new EcuModelAssignmentRequest();
        request.setEcuModelId(9L);
        ecuModelAssignmentRequestList.add(request);
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        2L).content(getMapper().writeValueAsString(ecuModelAssignmentRequestList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ecu List is empty throws bad request")
    void givenVehicleModelAndEmptlyEcuModelListWhenDeleteBadRequest() throws Exception {
        List<EcuModelAssignmentRequest> ecuModelAssignmentList = new ArrayList<>();
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        2L).content(getMapper().writeValueAsString(ecuModelAssignmentList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Vehicle Model Id not present")
    void givenVehicleModelAndEcuModelListWhenDeleteVehicleModelNotFound() throws Exception {
        EcuModelAssignmentRequest ecuModelAssignment = new EcuModelAssignmentRequest();
        ecuModelAssignment.setEcuModelId(3L);
        List<EcuModelAssignmentRequest> ecuModelAssignmentList = Stream.of(ecuModelAssignment).toList();
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        288L).content(getMapper().writeValueAsString(ecuModelAssignmentList))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Vehicle model id is associated with target which is part of any rollout")
    void givenVehicleModelAndEcuModelListWhenDeleteThrowErrorForRollout() throws Exception {
        Long vehicleModel = testdataFactory.createVehicle("NES 2.0").getId();
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        vehicleModel).content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        Target target = testdataFactory.createTarget(KNOWN_TARGET_ID, KNOWN_TARGET_ID, KNOWN_TARGET_ID, vehicleModel,KNOWN_TARGET_ID);

        final Rollout rollout = createRolloutWithDependencies("rollout", List.of(target));

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();


        rolloutManagement.start(rollout.getId());
        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        vehicleModel).content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Association between vehicle model id and ecu model ids is removed")
    void givenVehicleModelAndEcuModelListWhenDeleteSuccess() throws Exception {
        Vehicle vehicleModel = testdataFactory.createVehicle("X250");
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));


        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, vehicleModel.getId())
                        .content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,  vehicleModel.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        Assertions.assertFalse(testdataFactory.vehicleEcuExists(vehicleModel, ecuModels.stream().map(EcuModel::getId).collect(Collectors.toList())));
    }


    @Test
    @Description("Ensure vehicle model associated with a target cannot be deleted")
    void givenVehicleModelWithAssociatedTargetWhenDeleteThenFail() throws Exception {
        Long vehicleModelId = testdataFactory.createVehicle("X251").getId();
        testdataFactory.createTarget(CONTROLLER_ID1, CONTROLLER_ID1, CONTROLLER_ID1, vehicleModelId,CONTROLLER_ID1);

        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, vehicleModelId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure to delete the vehicle model which is not associated with any target.")
    void givenVehicleModelWithAssociatedTargetWhenDeleteThenFailAndNotAssociatedWhenDeleteSuccess() throws Exception {
        Long vehicleModelId = testdataFactory.createVehicle("X252").getId();
        Long vehicleModelId1 = testdataFactory.createVehicle("X253").getId();
        testdataFactory.createTarget(CONTROLLER_ID2, CONTROLLER_ID2, CONTROLLER_ID2, vehicleModelId, CONTROLLER_ID2);

        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, vehicleModelId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(delete(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, vehicleModelId1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Ensures that created Vehicle is able to retrieve with Vehicle Model Id.")
    void givenVehicleModelIdWhenGetVehicleModelThenReturnVehicleModelWithEmptyEcuModels() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedVehicle.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        List<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(1, mgmtVehicleResponses.size());
        Assertions.assertTrue(mgmtVehicleResponses.get(0).getEcuModels().isEmpty());
        Assertions.assertEquals(vehicleRequest.getName(), mgmtVehicleResponses.get(0).getName());
    }

    @Test
    @Description("Ensures that created Vehicle is able to retrieve with Ecu Models using Vehicle Model Id.")
    void givenVehicleModelIdWhenGetVehicleModelThenReturnVehicleModelWithNonEmptyEcuModels() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        List<EcuModel> ecuModels = testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(getEcuModelRequest()));
        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ECU_V1_REQUEST_MAPPING, 
                        savedVehicle.get(0).getId()).content(getMapper().writeValueAsString(getEcuModelAssignmentRequest(ecuModels)))
                        .contentType(MediaType.APPLICATION_JSON_VALUE)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedVehicle.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        List<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(1, mgmtVehicleResponses.size());
        Assertions.assertEquals(2, mgmtVehicleResponses.get(0).getEcuModels().size());
        Assertions.assertEquals(vehicleRequest.getName(), mgmtVehicleResponses.get(0).getName());
    }

    @Test
    @Description("Ensures that Created Vehicle is not able to retrieve with different Vehicle Model Id.")
    void givenVehicleModelIdWhenGetVehicleModelThenReturnVehicleNotFound() throws Exception {
        MgmtVehicleRequest vehicleRequest = buildTestVehicleRandomRequest();
        savedVehicle = testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest)));
        mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedVehicle.get(0).getId() + 1))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that All Vehicle Models are able to retrieve.")
    void givenEMWhenGetAllVehicleModelsThenReturnListOfVehicleModels() throws Exception {
        MgmtVehicleRequest vehicleRequest1 = buildTestVehicleRandomRequest();
        testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest1)));
        MgmtVehicleRequest vehicleRequest2 = buildTestVehicleRandomRequest();
        testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest2)));
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").value(2))
                .andReturn();
        PagedList<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(2, mgmtVehicleResponses.getContent().size());
    }

    @Test
    @Description("Ensure filtering Vehicle Models using rsqlParam works correctly")
    void givenRsqlParamWhenGetAllVehicleModelsThenFilterResults() throws Exception {
        // Reset the ID sequence to start from 1
        jdbcTemplate.execute("ALTER TABLE sp_vehicle_model ALTER COLUMN id RESTART WITH 1;");
        // Create test data with known IDs
        MgmtVehicleRequest vehicleRequest1 = MgmtVehicleRequest.builder().name("Vehicle1").build();
        MgmtVehicleRequest vehicleRequest2 = MgmtVehicleRequest.builder().name("Vehicle2").build();
        testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest1)));
        testdataFactory.addNewVehicleModels(List.of(MgmtVehicleMapper.formVehicleRequest(vehicleRequest2)));

        // Perform the GET request with the rsqlParam
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0") // Add paging offset
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10") // Add paging limit
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::ASC") // Add sorting
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "id==1") // Add rsqlParam
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.total").value(1)) // Validate the total count
                .andReturn();

        // Parse the response
        PagedList<MgmtVehicleResponse> mgmtVehicleResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        // Assertions
        Assertions.assertEquals(1, mgmtVehicleResponses.getContent().size());
        Assertions.assertTrue(mgmtVehicleResponses.getContent().stream()
                .allMatch(vehicle -> vehicle.getId() == 1L));
    }

    private List<MgmtEcuModelRequest> getEcuModelRequest() {
        MgmtEcuModelRequest ecuModel1 = new MgmtEcuModelRequest();
        MgmtEcuModelRequest ecuModel2 = new MgmtEcuModelRequest();
        ecuModel1.setEcuModelType(EcuModelTypeEnum.BT.toString());
        ecuModel1.setEcuModelName("R2" + random.nextInt(10000));
        ecuModel1.setEcuNodeId("R2PM" + random.nextInt(10000));
        ecuModel2.setEcuModelType(EcuModelTypeEnum.BTD.toString());
        ecuModel2.setEcuModelName("HPC" + random.nextInt(10000));
        ecuModel2.setEcuNodeId("RTCU" + random.nextInt(10000));
        return Stream.of(ecuModel1, ecuModel2).toList();
    }

    private List<EcuModelAssignmentRequest> getEcuModelAssignmentRequest(List<EcuModel> ecuModels) {
        EcuModelAssignmentRequest ecuModelAssignment1 = new EcuModelAssignmentRequest();
        EcuModelAssignmentRequest ecuModelAssignment2 = new EcuModelAssignmentRequest();
        ecuModelAssignment1.setEcuModelId(ecuModels.get(0).getId());
        ecuModelAssignment2.setEcuModelId(ecuModels.get(1).getId());
        List<EcuModelAssignmentRequest> ecuModelAssignmentRequests = new ArrayList<>();
        ecuModelAssignmentRequests.add(ecuModelAssignment1);
        ecuModelAssignmentRequests.add(ecuModelAssignment2);
        return ecuModelAssignmentRequests;
    }

    private static MgmtVehicleRequest buildTestVehicleRequest() {
        return MgmtVehicleRequest.builder().name(TEST_VM_01).build();
    }

    private static MgmtVehicleRequest buildTestVehicleRandomRequest() {
        return MgmtVehicleRequest.builder()
                .name(TEST_VM_01 + random.nextLong(10000)).build();
    }

    /**
     * Creates a new rollout and its dependencies, including software modules, versions, and associations.
     *
     * @param rolloutName the name of the rollout to create
     * @param targets     the list of targets to associate with the rollout
     * @return the created Rollout object
     */
    private @NotNull Rollout createRolloutWithDependencies(String rolloutName, List<Target> targets) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        // Create new software module and version
        SoftwareModule softwareModule = testdataFactory.createSoftwareModuleOs();
        Version version = testdataFactory.createVersion(softwareModule.getId(), "Test", 12);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroup(targets, rollout);
        return rollout;
    }

    @ParameterizedTest
    @MethodSource("provideSuccessfulCreateRequests")
    @Description("Verify successful creation of Vehicle Models with various ERC configurations")
    void testVehicleModelCreateSuccess(List<MgmtVehicleRequest> requests,
                                       Consumer<List<MgmtVehicleResponse>> assertions) throws Exception {

        MvcResult mvcResult =
                mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                                .content(getMapper().writeValueAsString(requests))
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andDo(MockMvcResultPrinter.print())
                        .andExpect(status().isCreated())
                        .andReturn();

        List<MgmtVehicleResponse> responses =
                getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                        new TypeReference<>() {});

        assertions.accept(responses);
    }

    private static Stream<Arguments> provideSuccessfulCreateRequests() {
        MgmtVehicleRequest req1 = buildTestVehicleRequest();
        req1.setErcType(STLAB);

        MgmtVehicleRequest req2 = buildTestVehicleRequest(); // no ERC

        MgmtVehicleRequest req3 = buildTestVehicleRandomRequest();
        req3.setErcType(STLAB);
        MgmtVehicleRequest req4 = buildTestVehicleRandomRequest();
        req4.setErcType(STLAB);

        return Stream.of(
                // Single with ERC
                Arguments.of(List.of(req1), (Consumer<List<MgmtVehicleResponse>>) (resp) -> {
                    Assertions.assertEquals(1, resp.size());
                    Assertions.assertEquals(req1.getName(), resp.get(0).getName());
                    Assertions.assertEquals(req1.getErcType(), resp.get(0).getErcType());
                }),

                // Single without ERC
                Arguments.of(List.of(req2), (Consumer<List<MgmtVehicleResponse>>) (resp) -> {
                    Assertions.assertEquals(1, resp.size());
                    Assertions.assertEquals(req2.getName(), resp.get(0).getName());
                    Assertions.assertNull(resp.get(0).getErcType());
                }),

                // Multiple with ERC
                Arguments.of(List.of(req3, req4), (Consumer<List<MgmtVehicleResponse>>) (resp) -> {
                    Assertions.assertEquals(2, resp.size());
                    Assertions.assertEquals(req3.getErcType(), resp.get(0).getErcType());
                    Assertions.assertEquals(req4.getErcType(), resp.get(1).getErcType());
                })
        );
    }

    @ParameterizedTest
    @MethodSource("provideErrorCreateRequests")
    @Description("Verify error responses when invalid Vehicle Model creation requests are sent")
    void testVehicleModelCreateErrors(List<MgmtVehicleRequest> requests,
                                      Consumer<JSONObject> assertions) throws Exception {

        MvcResult mvcResult =
                mvc.perform(post(MgmtRestConstants.VEHICLEMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                                .content(getMapper().writeValueAsString(requests))
                                .contentType(MediaType.APPLICATION_JSON_VALUE))
                        .andDo(MockMvcResultPrinter.print())
                        .andExpect(status().is4xxClientError())
                        .andReturn();

        JSONObject errorJson = new JSONObject(mvcResult.getResponse().getContentAsString());
        assertions.accept(errorJson);
    }

    private static Stream<Arguments> provideErrorCreateRequests() {

        MgmtVehicleRequest dup1 = buildTestVehicleRequest();
        dup1.setErcType(STLAB);

        MgmtVehicleRequest dup2 = buildTestVehicleRequest();
        dup2.setErcType(STLAB);

        MgmtVehicleRequest invalid = buildTestVehicleRequest();
        invalid.setErcType("ST");

        return Stream.of(
                Arguments.of(List.of(dup1, dup2),
                        (Consumer<JSONObject>) (json) -> {
                            Assertions.assertEquals("One or More Vehicle Model/s Name already exists", json.optString("message"));
                        }),

                Arguments.of(List.of(invalid),
                        (Consumer<JSONObject>) (json) -> {
                            Assertions.assertEquals("Cannot add Vehicle Model: specified ERC type does not exist.", json.optString("message"));
                        })
        );
    }

    @Test
    @Description("Verify updating Vehicle Model including ERC type")
    void testVehicleModelUpdate() throws Exception {

        MgmtVehicleRequest initial = buildTestVehicleRequest();

        savedVehicle = testdataFactory.addNewVehicleModels(
                List.of(MgmtVehicleMapper.formVehicleRequest(initial))
        );

        Assertions.assertNull(savedVehicle.get(0).getErcType());

        MgmtVehicleRequest updated = buildTestVehicleRandomRequest();
        updated.setErcType(STLAB);

        mvc.perform(put(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId())
                        .content(getMapper().writeValueAsString(updated))
                        .contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.VEHICLEMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING,
                        savedVehicle.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        List<MgmtVehicleResponse> responses =
                getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                        new TypeReference<>() {});

        Assertions.assertEquals(updated.getName(), responses.get(0).getName());
        Assertions.assertEquals(updated.getErcType(), responses.get(0).getErcType());
    }

}
