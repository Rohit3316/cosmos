package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.FileDownloadFailureException;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.cosmos.models.mgmt.MgmtRestConstants.BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES;
import static org.cosmos.models.mgmt.MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * This class is responsible for testing the functionality of managing   ESP and RSP support packages
 * using the Management API. It covers positive and negative  scenarios
 * <p>
 * The class uses JUnit 5 for testing and Spring MVC Test for making HTTP requests. It also utilizes
 * Mockito for mocking dependencies and H2 database for storing test data.
 * <p>
 * The class contains multiple test methods, each annotated with the @Test annotation, to test
 * different scenarios. Each test method is described using the @Description annotation to provide
 * a clear understanding of the expected behavior.
 */
@Story("Support Package Resource")
@Slf4j
 class MgmtSupportPackageResourceTest extends AbstractSupportPackageManagementApiIntegrationTest {

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

    @Test
    @Description("Test for associating new rollout with existing package")
    @SuppressWarnings("unchecked")
    void shouldAssociateTheECURolloutWhenSupportPackageExists() throws Exception {
        SupportPackageTestData customTestDataForFirstReq = new SupportPackageTestData();
        customTestDataForFirstReq.setTargetPrefix("FirstSetTarget");
        customTestDataForFirstReq.setRolloutPrefix("FirstRolloutPrefix");
        customTestDataForFirstReq.setRolloutName("FirstRolloutName");
        customTestDataForFirstReq.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestDataForFirstReq.setDistributionSetName("FirstDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestDataForFirstReq);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvc.perform(post(ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rollout.getId()).contentType("application/json").content(objectMapper.writeValueAsString(requestBody))).andExpect(status().isCreated());
        Esp esp = espRepository.findAll().get(0);
        assertEquals(1, espRepository.findAll().size());
        assertEquals(10, espRepository.findAll().get(0).getEspEcuRollouts().size());

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("secondSetTarget");
        customTestData.setRolloutPrefix("secondRolloutPrefix");
        customTestData.setRolloutName("secondRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_2);
        customTestData.setDistributionSetName("secondDistributionSetName");
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_2);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());
        assertEquals(2, espRepository.findAll().size());
        assertEquals(esp, espRepository.findAll().get(0));
        assertEquals(20, espEcuRolloutRepository.findAll().size());
    }

    @Test
    @Description("Test to ensure a 400 Bad Request is returned when attempting to associate controller IDs to a different ESP file of the same file type for the same rollout")
    void givenControllerIdsAlreadyAssociatedWithDifferentEspFileOfSameFileTypeWhenUploadingEspFileThenReturnBadRequest() throws Exception {
        SupportPackageTestData customTestDataForFirstReq = new SupportPackageTestData();
        customTestDataForFirstReq.setTargetPrefix("FirstSetTarget");
        customTestDataForFirstReq.setRolloutPrefix("FirstRolloutPrefix");
        customTestDataForFirstReq.setRolloutName("FirstRolloutName");
        customTestDataForFirstReq.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestDataForFirstReq.setDistributionSetName("FirstDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestDataForFirstReq);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvc.perform(post(ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rollout.getId()).contentType("application/json").content(objectMapper.writeValueAsString(requestBody))).andExpect(status().isCreated());

        requestBody.setFileName("NewFileName");
        requestBody.setSha256("d6e2b3c99e3ad6d32b61aa162b8f86dfaa89dc57b8c9dc58f7b2e7c693d9b72a");

        mvc.perform(post(ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rollout.getId()).contentType("application/json").content(objectMapper.writeValueAsString(requestBody))).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verifies that when adding new devices along with devices already associated with ESP, only the new devices are associated successfully, and existing associations remain unchanged. This ensures that new devices are not blocked by previously associated devices.")
    void givenNewDevicesAndExistingEspAssociationsWhenAddEspThenNewDevicesAreAssociatedCorrectly() throws Exception {
        SupportPackageTestData customTestDataForFirstReq = new SupportPackageTestData();
        customTestDataForFirstReq.setTargetPrefix("FirstSetTarget");
        customTestDataForFirstReq.setRolloutPrefix("FirstRolloutPrefix");
        customTestDataForFirstReq.setRolloutName("FirstRolloutName");
        customTestDataForFirstReq.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestDataForFirstReq.setDistributionSetName("FirstDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestDataForFirstReq);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        List<String> controllerIds = new ArrayList<>(requestBody.getControllerIds());
        String lastControllerId = controllerIds.remove(controllerIds.size() - 1);
        requestBody.setControllerIds(controllerIds);
        mvc.perform(post(ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rollout.getId()).contentType("application/json").content(objectMapper.writeValueAsString(requestBody))).andExpect(status().isCreated());
        controllerIds.add(lastControllerId);
        requestBody.setControllerIds(Collections.singletonList(lastControllerId));
        mvc.perform(post(ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rollout.getId()).contentType("application/json").content(objectMapper.writeValueAsString(requestBody))).andExpect(status().isCreated());
    }

    @Test
    @Description("Test create supportPackage with a multipart file")
    @SuppressWarnings("unchecked")
    void givenSupportPackageAsFileWhenCreateIsCalledThenFileIsDirectlyUploadedToS3() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());
        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.WHATS_NEW.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA
        ).andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print());
    }

    @Test
    @Description("Test creating supportPackage with an empty multipart file throws exception")
    @SuppressWarnings("unchecked")
    void givenEmptySupportPackageAsFileWhenCreateIsCalledThenError() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        MockMultipartFile emptyFile = new MockMultipartFile("file", "testFileName", MediaType.TEXT_PLAIN_VALUE, new byte[0]);

        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());

        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                emptyFile,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.WHATS_NEW.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File cannot be empty"));
    }


    @Test
    @Description("Test invalid file metadata throws exception on creating supportPackage with a multipart file")
    @SuppressWarnings("unchecked")
    void givenInvalidFileMetadataFormatWhenCreateSupportPackageAsFileIsCalledThenError() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());

        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.WHATS_NEW.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                "Invalid metadata format")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid metadata format"));
    }

    @Test
    @Description("Test no Ecu Node Address in ESP throws exception on creating supportPackage with a multipart file")
    @SuppressWarnings("unchecked")
    void givenNoECUNodeAddressInESPWhenCreateSupportPackageAsFileIsCalledThenError() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());

        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.ADA_CERTIFICATE.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                "",
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ecu node address is not provided in the request"));
    }

    @Test
    @Description("Test no controllerIds in ESP throws exception on creating supportPackage with a multipart file")
    @SuppressWarnings("unchecked")
    void givenNoControllerIdsInESPWhenCreateSupportPackageAsFileIsCalledThenError() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());

        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.ADA_CERTIFICATE.toString(),
                requestBody.getFileVersion(),
                List.of(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("ControllerId is not provided in the request"));
    }

    @Test
    @Description("Test invalid filetype throws exception on creating supportPackage with a multipart file")
    @SuppressWarnings("unchecked")
    void givenInvalidFileTypeWhenCreateSupportPackageAsFileIsCalledThenError() throws Exception {

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, new SupportPackageTestData());
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());

        invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                "InvalidFileType",
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for field 'fileType': 'InvalidFileType'. Accepted values are: [VARIANT_CODING, LICENSE, UDS_FLOW, ECU_SCRIPT, BASELINE_INVENTORY, INSTALLATION_ROLLBACK_PLAN, DTC_BLACKLIST, RULE_ENGINE_CONFIG, PROXI, PROXI_SIGNATURE, ADA_CERTIFICATE, ADA_LICENSE, WHATS_NEW, UDS_GLOBAL_PRE_INSTALL, UDS_GLOBAL_POST_INSTALL]."));
    }


    @Test
    @Description("Test for creating a new ESP support package with invalid file type")
    void shouldNotCreateNewSupportPackageWithInvalidFileTypeTest() throws Exception {
        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix(INVALID_FILE_TYPE);
        supportPackageTestData.setRolloutPrefix(INVALID_FILE_TYPE);
        supportPackageTestData.setRolloutName(INVALID_FILE_TYPE);
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        supportPackageTestData.setDistributionSetName(INVALID_FILE_TYPE);
        SupportPackageTestData customTestData = new SupportPackageTestData();// Invalid file type
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace("LICENSE", "INVALID ");
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyString).andExpect(status().isBadRequest()); // Expect 400 Bad Request
    }

    @Test
    @Description("Test for creating a new ESP support package with missing file URL")
    void shouldNotCreateNewSupportPackageWithMissingFileUrlTest() throws Exception {

        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix(MISSING_FILE_URL);
        supportPackageTestData.setRolloutPrefix(MISSING_FILE_URL);
        supportPackageTestData.setRolloutName(MISSING_FILE_URL);
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        supportPackageTestData.setDistributionSetName(MISSING_FILE_URL);

        supportPackageTestData.setFileUrl(null); // Missing file URL
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, supportPackageTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest()); // Expect 400 Bad Request
    }

    @Test
    @Description("Test for creating a new ESP support package with invalid SHA-256 hash")
    void shouldNotCreateNewSupportPackageWithInvalidSha256HashTest() throws Exception {

        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix(INVALID_SHA_256);
        supportPackageTestData.setRolloutPrefix(INVALID_SHA_256);
        supportPackageTestData.setRolloutName(INVALID_SHA_256);
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);

        supportPackageTestData.setDistributionSetName(INVALID_SHA_256);
        supportPackageTestData.setSha256(INVALID_SHA_256); // Invalid SHA-256 hash
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, supportPackageTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest()); // Expect 400 Bad Request
    }


    @Test
    @Description("Verify unlink support package for invalid rollout throws error")
    void givenInvalidRolloutWhenDeletingThenThrowsError() throws Exception {
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES,
                        1, RandomUtils.nextInt()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Verify unlink support package for rollout with no package association throws error")
    void givenValidRolloutWithNoPackageWhenDeletingThenThrowsError() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("RSPDeleteTarget");
        customTestData.setRolloutPrefix(RSP_DELETE_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_DELETE_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName(RSP_DELETE_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isNotFound());
    }

    /**
     * This test case verifies that the API successfully returns all support packages (ESP and RSP) for a given valid rollout ID.
     *
     * @throws Exception
     */
    @Test
    @Description("This test case verifies that the API successfully returns all support packages (ESP and RSP) for a given valid rollout ID.")
    @SuppressWarnings("unchecked")
    void testGetAllSupportPackagesWithValidRolloutId() throws Exception {
        // Prepare and create the ESP support package
        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("SetTarget06");
        espTestData.setRolloutPrefix("RolloutPrefixSupportkg");
        espTestData.setRolloutName("Rollout06");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        espTestData.setDistributionSetName("DistributionSetName07");


        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, espTestData);
        Rollout espRollout = (Rollout) espTestDataMap.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), espRequestBody).andExpect(status().isCreated());

        // Prepare and create the RSP support package
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix("RSPSetTarget07");
        rspTestData.setRolloutPrefix("RolloutPrefixSupportkg08");
        rspTestData.setRolloutName("RSRolloutPrefixSupportkg08");
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName("RSPDistributionSetName08");
        rspTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_2);
        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, rspTestData);
        Rollout rspRollout = (Rollout) rspTestDataMap.get(ROLLOUT_2);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rspRollout.getId(), rspRequestBody).andExpect(status().isCreated());

        // Perform the request to retrieve all support packages for the rollout and verify the response
        Long tenantId = 1L;
        mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + espRollout.getId() + "/support-packages").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    /**
     * This test case verifies that the API throws EntityNotFoundException when an invalid rollout ID is provided.
     *
     * @throws Exception
     */
    @Test
    @Description("This test case verifies that the API throws EntityNotFoundException when an invalid rollout ID is provided.")
    void testGetSupportPackagesWithInvalidRolloutId() throws Exception {

        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("SetTarget05");
        espTestData.setRolloutPrefix("RolloutPrefixSupportkg04");
        espTestData.setRolloutName("RolloutSupportkg04");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        espTestData.setDistributionSetName("DistributionSetName06");
        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, espTestData);
        Rollout rollout = (Rollout) espTestDataMap.get(ROLLOUT);// Invalid rollout ID
        Long tenantId = 1L;

        mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_V1_REQUEST_MAPPING_TENANT_TARGETS, tenantId, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(result -> assertInstanceOf(EntityNotFoundException.class, result.getResolvedException()));


    }


    @Test
    @Description("Validate delete support packages of a rollout throws exception on packages not found")
    @SuppressWarnings("unchecked")
    void givenRolloutIdWithoutPackagesWhenDeleteSupportPackageThenEntityNotFoundException() throws Exception {
        reset(s3Client);
        // Create ESP support package
        SupportPackageTestData customEspData = new SupportPackageTestData();
        customEspData.setTargetPrefix("SetSpTarget");
        customEspData.setRolloutPrefix("SpRolloutPrefix");
        customEspData.setRolloutName("SpRolloutName");
        customEspData.setFileType(MgmtSupportPackageFileType.LICENSE);
        customEspData.setDistributionSetName("SpDistributionSetName");
        var testEspData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customEspData);

        Rollout rollout = (Rollout) testEspData.get(ROLLOUT);
        assignEcuModelsToVehicle((List<Target>) testEspData.get(TARGETS), (List<EcuModel>) testEspData.get(ECU_MODELS));
        MgmtBaseSupportPackageCreateRequest requestEspBody = (MgmtBaseSupportPackageCreateRequest) testEspData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestEspBody).andExpect(status().isCreated());

        // Create RSP support package
        SupportPackageTestData customRspData = new SupportPackageTestData();
        customRspData.setTargetPrefix("SetSpTarget");
        customRspData.setRolloutPrefix("SpRolloutPrefix");
        customRspData.setRolloutName("SpRolloutName");
        customRspData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customRspData.setDistributionSetName("SpDistributionSetName");


        MgmtBaseSupportPackageCreateRequest requestRspBody =  MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl( customRspData.getFileUrl())
                .fileName(customRspData.getTestFileName())
                .fileType(customRspData.getFileType())
                .sha256(customRspData.getSha256())
                .fileVersion(customRspData.getVersion())
                .controllerIds(requestEspBody.getControllerIds())
                .ecuNodeAddress(customRspData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(customRspData.getFileMetadata())
                .build();
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestRspBody).andExpect(status().isCreated());

        List<JpaEsp> esp = espRepository.findAll();
        List<JpaRsp> rsp = rspRepository.findAll();
        assertEquals(1, esp.size());
        assertEquals(1, rsp.size());
        assertEquals(espEcuRolloutRepository.findAll().get(0).getSupportPackage(), esp.get(0));
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rsp.get(0));

        //Delete Support Package
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isOk());

        //Delete without Support Packages
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Support Packages not found for the rollout ID: " + rollout.getId()));
    }

    @Test
    @Description("Validate delete support packages of a rollout throws exception on rollout not found")
    void givenInvalidRolloutIdWhenDeleteSupportPackageThenEntityNotFoundException() throws Exception {
        //Delete Support Package
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, 1)).andExpect(status().isNotFound()).andExpect(jsonPath("$.message").value("Rollout with given identifier {1} does not exist."));
    }

    private void mockIsFileAvailableOnS3(boolean isAvailable) {
        if (isAvailable) {

            when(s3Service.isValidGetUrl(any(URL.class))).thenReturn(isAvailable);
        } else {
            doThrow(new FileDownloadFailureException("File is unavailable for download")).when(s3Service).isValidGetUrl(any(URL.class));
        }

    }

    /**
     * This test case verifies that a request for downloading an existing support-pkg returns a successful response
     *
     * @throws Exception
     */
    @ParameterizedTest
    @ValueSource(booleans = {true})
    @Description("Ensures that a request for downloading an existing support-pkg returns a successful response")
    void givenSupportPackageAvailabilityShouldRespondAccordingly(boolean isFileAvailableOnS3) throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3SupportPkgObjectName(anyString(), anyString(), anyString(), anyString())).thenCallRealMethod();
        Long tenantId = 1L;
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix("TargetUnique123");
        rspTestData.setRolloutPrefix("PrefixUnique123");
        rspTestData.setRolloutName("RolloutUnique123");
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName("DistributionUnique123");


        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, rspTestData);
        Rollout rspRollout = (Rollout) rspTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get(REQUEST_BODY);

        invokeCreateSupportPackageApiAndReturnHttpResponse(rspRollout.getId(), rspRequestBody).andExpect(status().isCreated());
        mockIsFileAvailableOnS3(isFileAvailableOnS3);
        var result = mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + rspRollout.getId() + SUPPORT_PACKAGES_URL + rspRepository.findAll().get(0).getId() + "/download").param(TYPE_QUERY_PARAM, "rsp").contentType(MediaType.APPLICATION_JSON));
        if (isFileAvailableOnS3) {
            result.andExpect(status().is(302));
            var url = result.andReturn().getResponse().getHeader("Location");
            assertThat(url).isNotNull();
        } else {
            result.andExpect(status().isForbidden());
        }

    }

    /**
     * This test case verifies that the API returns a 404 Not Found response when a request is made to download a non-existing support package.
     *
     * @throws Exception
     */
    @Test
    @Description("Ensures that a request for downloading a non-existing support-pkg returns a 404 Not Found response")
    void givenNonExistentSupportPackageShouldReturnNotFound() throws Exception {
        Long tenantId = 1L;
        Long nonExistentSupportPackageId = 999L; // Use an ID that doesn't exist in the repository

        var result = mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + nonExistentSupportPackageId + SUPPORT_PACKAGES_URL + nonExistentSupportPackageId + "/download").param(TYPE_QUERY_PARAM, "rsp").contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    private void testS3Integration(final BaseSupportPackage supportPackage) {
        testS3Integration(supportPackage, 1);
    }

    private void testS3Integration(final BaseSupportPackage supportPackage, int noOfTimes) {
        final String keyPath = artifactUrlHandlerProperties.getS3().getDirectory().replace(TENANT, tenantAware.getCurrentTenant()).replace(TYPE, supportPackage.getFileType().getCategory()).replace(SHA256, supportPackage.getSha256Hash());
        verify(s3Client, times(noOfTimes)).deleteObject(DeleteObjectRequest.builder().bucket(supportPackageBucketName).key(keyPath + supportPackage.getFileName()).build());
    }

    @Test
    @Description("Given case insensitive file type, when creating support package for rollout, then return success")
    @SuppressWarnings("unchecked")
    void givenCaseInsensitiveFileTypeWhenCreatingSupportPackageForRolloutThenReturnSuccess() throws Exception {
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix("TargetUnique123");
        rspTestData.setRolloutPrefix("PrefixUnique123");
        rspTestData.setRolloutName("RolloutUnique123");
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName("DistributionUnique123");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, rspTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace("PROXI_SIGNATURE", "PrOxI_sIgNaTuRe");
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyString).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType", equalTo("PROXI_SIGNATURE")));

        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("TargetUnique1266");
        espTestData.setRolloutPrefix("PrefixUnique1266");
        espTestData.setRolloutName("RolloutUnique1266");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_2);
        espTestData.setFileType(MgmtSupportPackageFileType.ADA_CERTIFICATE);
        espTestData.setDistributionSetName("DistributionUnique123");
        testData = prepareTestDataForCreatingSupportPackageTest("RolloutUnique1266", espTestData);
        rollout = (Rollout) testData.get("RolloutUnique1266");
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        reqBodyString = objectMapper.writeValueAsString(requestBody).replace("ADA_CERTIFICATE", "AdA_cErTiFiCaTe");
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyString).andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileType", equalTo("ADA_CERTIFICATE")));
    }

    @Test
    @Description("Given file name with multiple white spaces, when creating an RSP, then all the white spaces in filename are trimmed")
    void givenFileNameWithWhiteSpaceWhenCreatingRSPThenSpaceTrimmed() throws Exception{
        SupportPackageTestData customTestRspData = new SupportPackageTestData();
        customTestRspData.setTargetPrefix("RSPDeleteTarget");
        customTestRspData.setTestFileName("test     file  name    03");
        customTestRspData.setRolloutPrefix(RSP_DELETE_ROLLOUT_PREFIX);
        customTestRspData.setRolloutName(RSP_DELETE_ROLLOUT_NAME);
        customTestRspData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestRspData.setDistributionSetName(RSP_DELETE_DISTRIBUTION_SET_NAME);

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestRspData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());
        MvcResult mvcResult = invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.WHATS_NEW.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA
        ).andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print()).andReturn();
        MgmtSupportPackage rsp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        JpaRsp jpaRsp = rspRepository.findById(rsp.getSupportPackageId()).get();
        assertEquals("testfilename03", jpaRsp.getFileName()  );
    }

    @Test
    @Description("Given file name with multiple white spaces, when creating an ESP, then all the white spaces in filename are trimmed")
    void givenFileNameWithWhiteSpaceWhenCreatingESPThenSpaceTrimmed() throws Exception{
        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("SetTarget01");
        espTestData.setRolloutPrefix("RolloutPrefixSupportkg06");
        espTestData.setTestFileName("test     file  name    04");
        espTestData.setRolloutName("Rollout06");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        espTestData.setDistributionSetName("DistributionSetName03");

        Map<String, Object> testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, espTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        doNothing().when(s3MultipartFileUpload).uploadFileToS3Multipart(any(), any());
        MvcResult mvcResult = invokeCreateSupportPackageWithFileApi(1L, rollout.getId(),
                TEST_MOCKED_MULTIPART_FILE,
                requestBody.getFileName(),
                MgmtSupportPackageFileType.WHATS_NEW.toString(),
                requestBody.getFileVersion(),
                requestBody.getControllerIds(),
                TEST_SHA256,
                requestBody.getEcuNodeAddress(),
                requestBody.getFileContentDescription(),
                requestBody.getFileInfoUrl(),
                TEST_SUPPORT_PACKAGE_FILE_METADATA
        ).andExpect(status().isCreated()).andDo(MockMvcResultPrinter.print()).andReturn();
        MgmtSupportPackage rsp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        JpaRsp jpaRsp = rspRepository.findById(rsp.getSupportPackageId()).get();
        assertEquals("testfilename04", jpaRsp.getFileName()  );
    }

}