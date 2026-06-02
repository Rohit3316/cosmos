package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.eclipse.hawkbit.exception.ServerError;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelResponse;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtEcuModelMapper;
import org.eclipse.hawkbit.repository.jpa.SoftwareModuleRepository;
import org.eclipse.hawkbit.repository.jpa.VehicleRepository;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for {@link MgmtEcuResource} {@link RestController}.
 */
@Feature("Component Tests - Management API")
@Story("Ecu Model")
public class MgmtEcuModelResourceTest extends AbstractManagementApiIntegrationTest {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String TEST_ECU_MODEL_NAME = "TestEcuModel01";
    private static final String TEST_ECU_NODE_ID = "HeX32458";
    private static final long TENANT_ID = 1L;
    Random random = new SecureRandom();
    @MockBean
    private SoftwareModuleRepository softwareModuleRepository;
    @MockBean
    private VehicleRepository vehicleRepository;
    private List<EcuModel> savedEcuModel;

    @BeforeEach
    public void setUp() {
        testdataFactory.deleteAllEcuModels();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @Description("Ensure Adding New ECU Model name will return success with Id and other details in response")
    void givenNewEcuModelWhenAddThenAdded() throws Exception {

        MgmtEcuModelRequest request1 = buildTestEcuRequest();
        List<MgmtEcuModelRequest> requests = List.of(request1);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();
        List<MgmtEcuModelResponse> responses = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(requests.size(), responses.size());
        Assertions.assertEquals(requests.get(0).getEcuModelType(), responses.get(0).getEcuModelType());
        Assertions.assertEquals(requests.get(0).getEcuModelName(), responses.get(0).getEcuModelName());
        Assertions.assertEquals(requests.get(0).getEcuNodeId(), responses.get(0).getEcuNodeId());
    }

    @Test
    @Description("Ensure Adding Multiple ECU Models will return success with Id and other details in response")
    void givenMultipleEcuModelsWhenAddThenAllAdded() throws Exception {

        MgmtEcuModelRequest request1 = buildTestEcuRandomRequest();
        MgmtEcuModelRequest request2 = buildTestEcuRandomRequest();
        List<MgmtEcuModelRequest> requests = Arrays.asList(request1, request2);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn();
        List<MgmtEcuModelResponse> responses = objectMapper.readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
        Assertions.assertEquals(requests.size(), responses.size());
        Assertions.assertEquals(requests.get(0).getEcuModelType(), responses.get(0).getEcuModelType());
        Assertions.assertEquals(requests.get(0).getEcuModelName(), responses.get(0).getEcuModelName());
        Assertions.assertEquals(requests.get(0).getEcuNodeId(), responses.get(0).getEcuNodeId());
        Assertions.assertEquals(requests.get(1).getEcuModelType(), responses.get(1).getEcuModelType());
        Assertions.assertEquals(requests.get(1).getEcuModelName(), responses.get(1).getEcuModelName());
        Assertions.assertEquals(requests.get(1).getEcuNodeId(), responses.get(1).getEcuNodeId());
    }

    @Test
    @Description("Ensure Adding Same ECU Model names will throw an Exception")
    void givenSameEcuModelWhenAddThenError() throws Exception {

        MgmtEcuModelRequest request1 = buildTestEcuRequest();
        List<MgmtEcuModelRequest> requests = Arrays.asList(request1, request1);

        final MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().is4xxClientError()).andReturn();
        String errResponseString = mvcResult.getResponse().getContentAsString();
        JSONObject jsonObject = new JSONObject(errResponseString);
        Assertions.assertEquals(ServerError.ENTITY_ALREADY_EXISTS.toString(), jsonObject.getString("name"));
    }

    @Test
    @Description("Ensure Adding Ecu Models without node id will throw exception")
    void givenEMWithMissingNodeIdWhenAddThenError() throws Exception {

        MgmtEcuModelRequest request1 = MgmtEcuModelRequest.builder()
                .ecuModelType(EcuModelTypeEnum.BT.toString())
                .ecuModelName(TEST_ECU_MODEL_NAME).build();
        List<MgmtEcuModelRequest> requests = List.of(request1);

        mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        MgmtEcuModelRequest request2 = new MgmtEcuModelRequest();
        List<MgmtEcuModelRequest> requests2 = List.of(request2);

        mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests2)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure updating Ecu Model for given ECU Model ID returns success")
    void givenEmWhenUpdateThenEmUpdated() throws Exception {
        MgmtEcuModelRequest request1 = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));
        MgmtEcuModelRequest updatedEcuModelRequest = MgmtEcuModelRequest.builder()
                .ecuModelType(EcuModelTypeEnum.OM.toString())
                .ecuModelName("TestUpdate" + random.nextInt(10000))
                .ecuNodeId(TEST_ECU_NODE_ID + random.nextInt(10000))
                .build();
        mvc.perform(put(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId())
                        .content(objectMapper.writeValueAsString(updatedEcuModelRequest)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    @Description("Ensure updating ECU Model with invalid ECU Model type for given ECU Model ID returns bad request ")
    void givenInvalidEmNameWhenUpdateThenError() throws Exception {
        MgmtEcuModelRequest request = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request)));
        MgmtEcuModelRequest updatedEcuModelRequest = MgmtEcuModelRequest.builder().ecuModelName(null).build();
        mvc.perform(put(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId())
                        .content(objectMapper.writeValueAsString(updatedEcuModelRequest)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        updatedEcuModelRequest.setEcuModelName("");
        mvc.perform(put(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId())
                        .content(objectMapper.writeValueAsString(updatedEcuModelRequest)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure updating ECU Model for non existing ECU Model ID returns not found")
    void givenEmWhenUpdateNonExistingEmThenError() throws Exception {
        MgmtEcuModelRequest updatedEcuModelRequest = buildTestEcuRandomRequest();
        mvc.perform(put(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, random.nextInt(10000))
                        .content(objectMapper.writeValueAsString(updatedEcuModelRequest)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure updating ECU Model for invalid type of ECU Model ID returns bad request")
    void givenInvalidTypeEmIdWhenUpdateThenError() throws Exception {
        MgmtEcuModelRequest updatedEcuModelRequest = buildTestEcuRandomRequest();
        mvc.perform(put(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, random.nextInt(10000))
                        .content(objectMapper.writeValueAsString(updatedEcuModelRequest)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure deleting ECU Model for given ECU Model ID returns success")
    void givenEmWhenDeleteThenEmDeleted() throws Exception {
        MgmtEcuModelRequest request = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request)));
        mvc.perform(delete(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    @Description("Ensure deleting ECU Model for non existing ECU Model ID returns not found")
    void givenEmWhenDeleteNonExistingThenError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, random.nextInt(10000)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure deleting ECU Model for invalid type of ECU Model ID returns bad request")
    void givenInvalidTypeEmIdWhenDeleteThenError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, TEST_ECU_MODEL_NAME + random.nextInt(10000)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure deleting ECU Model with existing association with Vehicle Model returns appropriate error message")
    void givenEcuModelWithAssociationWithVehicleModelWhenDeleteThenReturnError() throws Exception {
        MgmtEcuModelRequest request = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request)));
        when(vehicleRepository.existsByVehicleEcuId(savedEcuModel.get(0).getId())).thenReturn(true);

        mvc.perform(delete(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("EcuModel cannot be deleted as it has associations with VehicleModel")));
    }

    @Test
    @Description("Ensure deleting ECU Model with existing association with Software Module returns appropriate error message")
    void givenEcuModelWithAssociationWithSoftwareModuleWhenDeleteThenReturnError() throws Exception {
        MgmtEcuModelRequest request = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request)));
        when(softwareModuleRepository.countBySoftwareEcuModels(savedEcuModel.get(0).getId())).thenReturn(2L);
        mvc.perform(delete(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId()))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("EcuModel cannot be deleted as it has associations with SoftwareModule")));
    }

    @Test
    @Description("Ensure fetching Ecu Model for given ECU Model ID returns success")
    void givenEmIdWhenGetThenGetEmForGivenEMId() throws Exception {
        MgmtEcuModelRequest request1 = buildTestEcuRandomRequest();
        savedEcuModel = testdataFactory.addNewEcuModels(List.of(MgmtEcuModelMapper.toEcuModelRequest(request1)));
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, savedEcuModel.get(0).getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        JSONArray jsonResponse = new JSONArray(mvcResult.getResponse().getContentAsString());
        Assertions.assertEquals(savedEcuModel.get(0).getId(), jsonResponse.getJSONObject(0).getLong("ecuModelId"));
    }

    @Test
    @Description("Ensure fetching ECU Model for non existing ECU Model ID returns not found")
    void givenEmIdWhenGetNonExistingEmThenNotFoundError() throws Exception {
        mvc.perform(get(MgmtRestConstants.ECUMODEL_ID_V1_NO_TENANT_REQUEST_MAPPING, random.nextInt(10000)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure fetching All Ecu Models returns success")
    void givenEMWhenGetAllThenGetAllEcuModels() throws Exception {
        MgmtEcuModelRequest request1 = buildTestEcuRandomRequest();
        MgmtEcuModelRequest request2 = buildTestEcuRandomRequest();
        List<MgmtEcuModelRequest> requests = Arrays.asList(request1, request2);
        testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(requests));
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andReturn();
        JSONObject jsonResponse = new JSONObject(mvcResult.getResponse().getContentAsString());
        JSONArray contentArray = jsonResponse.getJSONArray("content");
        Assertions.assertEquals(requests.size(), contentArray.length());
    }

    @Test
    @Description("Ensure fetching ECU Model for non existing ECU Models returns not found")
    void givenEmWhenGetNonExistingEmThenNotFoundError() throws Exception {
        mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensure Adding Ecu Models without model Type will throw exception")
    void givenEMWithMissingModelTypeWhenAddThenError() throws Exception {

        MgmtEcuModelRequest request1 = MgmtEcuModelRequest.builder()
                .ecuNodeId(TEST_ECU_NODE_ID)
                .ecuModelName(TEST_ECU_MODEL_NAME).build();
        List<MgmtEcuModelRequest> requests = List.of(request1);

        mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensure Adding Ecu Models with model Type that doesnt exist will throw exception")
    void givenEMWithModelTypeNotPresentWhenAddThenError() throws Exception {

        MgmtEcuModelRequest request1 = MgmtEcuModelRequest.builder()
                .ecuModelType("test")
                .ecuNodeId(TEST_ECU_NODE_ID)
                .ecuModelName(TEST_ECU_MODEL_NAME).build();
        List<MgmtEcuModelRequest> requests = List.of(request1);

        mvc.perform(post(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .content(objectMapper.writeValueAsString(requests)).contentType(MediaType.APPLICATION_JSON_VALUE))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }


    private MgmtEcuModelRequest buildTestEcuRequest() {
        return MgmtEcuModelRequest.builder()
                .ecuModelType(EcuModelTypeEnum.SU.toString())
                .ecuModelName(TEST_ECU_MODEL_NAME).ecuNodeId(TEST_ECU_NODE_ID).build();
    }

    private MgmtEcuModelRequest buildTestEcuRandomRequest() {
        return MgmtEcuModelRequest.builder()
                .ecuModelType(EcuModelTypeEnum.BT.toString())
                .ecuModelName(TEST_ECU_MODEL_NAME + random.nextLong(10000))
                .ecuNodeId(TEST_ECU_NODE_ID + random.nextLong(10000)).build();
    }

    @Test
    @Description("Ensure filtering ECU Models using rsqlParam works correctly")
    void givenEcuModelsWhenGetAllEcuModelsWithRsqlParamsThenFilterResults() throws Exception {
        // Create and save ECU models
        List<MgmtEcuModelRequest> requests = Arrays.asList(
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest()
        );
        testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(requests));

        // Perform GET request
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0") // Add paging offset
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10") // Add paging limit
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::ASC")) // Add sorting
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andReturn();

        // Parse the response
        PagedList<MgmtEcuModelResponse> mgmtEcuModelResponses = getMapper().readValue(mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });
    }

    @Test
    @Description("Ensure ECU Models are sorted in ascending order by ID")
    void givenEcuModelsWhenGetAllEcuModelsWithSortAscParamThenFilterResults() throws Exception {
        // Create and save ECU models
        List<MgmtEcuModelRequest> requests = Arrays.asList(
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest()
        );
        testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(requests));

        // Perform GET request
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::ASC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response
        PagedList<MgmtEcuModelResponse> response = getMapper().readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        List<Long> ids = response.getContent().stream()
                .map(MgmtEcuModelResponse::getId)
                .toList();

        List<Long> sorted = new ArrayList<>(ids);
        Collections.sort(sorted);

        Assertions.assertEquals(sorted, ids);
    }

    @Test
    @Description("Ensure ECU Models are sorted in descending order by ID")
    void givenEcuModelsWhenGetAllEcuModelsWithSortDescParamThenFilterResults() throws Exception {
        // Create and save ECU models
        List<MgmtEcuModelRequest> requests = List.of(
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest()
        );
        testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(requests));

        // Perform GET request
        final MvcResult mvcResult = mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::DESC"))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        // Parse the response
        PagedList<MgmtEcuModelResponse> response = getMapper().readValue(
                mvcResult.getResponse().getContentAsString(),
                new TypeReference<>() {
                });

        List<Long> ids = response.getContent().stream()
                .map(MgmtEcuModelResponse::getId)
                .toList();

        List<Long> sorted = new ArrayList<>(ids);
        sorted.sort(Collections.reverseOrder());

        Assertions.assertEquals(sorted, ids);
    }

    @Test
    @Description("Ensure RSQL handling returns appropriate status codes for malformed and non-matching filters")
    void givenEcuModelsWhenGetAllEcuModelsWithInvalidRsqlParamsThenReturnError() throws Exception {
        // Create and save ECU models
        List<MgmtEcuModelRequest> requests = List.of(
                buildTestEcuRandomRequest(),
                buildTestEcuRandomRequest()
        );
        testdataFactory.addNewEcuModels(MgmtEcuModelMapper.fromEcuModelRequests(requests));

        // Perform GET request with a filter that doesn't match any ECU Model
        mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::ASC")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "id==10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // Perform GET request with a filter that contains malformed rsql
        mvc.perform(get(MgmtRestConstants.ECUMODEL_V1_NO_TENANT_REQUEST_MAPPING)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, "0")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, "10")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "id::ASC")
                        .param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "id==") // malformed RSQL
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }
}
