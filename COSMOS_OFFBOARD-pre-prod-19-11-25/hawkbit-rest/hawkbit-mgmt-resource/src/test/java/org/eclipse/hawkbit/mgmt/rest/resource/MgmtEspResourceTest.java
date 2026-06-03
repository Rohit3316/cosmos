package org.eclipse.hawkbit.mgmt.rest.resource;


import io.qameta.allure.Description;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.Collections;

import static org.cosmos.models.mgmt.MgmtRestConstants.BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class MgmtEspResourceTest extends AbstractSupportPackageManagementApiIntegrationTest {
    @Test
    @Description("This test case verifies that the API throws an EntityNotFoundException when an invalid ESP support package ID is provided.")
    @SuppressWarnings("unchecked")
    void testGetESPSupportPackageWithInvalidPkgId() throws Exception {
        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("SetTarget01");
        espTestData.setRolloutPrefix("RolloutPrefixSupportkg06");
        espTestData.setRolloutName("Rollout06");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        espTestData.setDistributionSetName("DistributionSetName03");
        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, espTestData);
        Rollout espRollout = (Rollout) espTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), espRequestBody).andExpect(status().isCreated());

        Long tenantId = 1L;
        Long invalidPkgId = 100L;

        mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + espRollout.getId() + SUPPORT_PACKAGES_URL + "Esp/" + invalidPkgId).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given ESP packages in storage, when downloading, then return appropriate response")
    @SuppressWarnings("unchecked")
    void givenEspPackagesInStorage_whenDownloading_thenReturnAppropriateResponse() throws Exception {

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtFileUrlSupportPackageCreateRequest requestBody = (MgmtFileUrlSupportPackageCreateRequest) testData.get(REQUEST_BODY);

        // Mock valid S3 URL generation
        when(s3Service.generatePresignedUrl(any(), any(), any())).thenReturn(new URL(requestBody.getFileUrl()));
        when(s3Service.isValidGetUrl(any())).thenReturn(true);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        String contentAsString = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn().
                getResponse().getContentAsString();
        MgmtSupportPackage esp = objectMapper.readValue(contentAsString, MgmtSupportPackage.class);
        assertEquals(1, espRepository.findAll().size());
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS,
                1, rollout.getId(), esp.getSupportPackageId()).queryParam(TYPE_QUERY_PARAM, ESP)).andExpect(status().is3xxRedirection());

    }


    @Test
    @Description("Test for creating a new ESP support package throws bad request error if the vehicle " +
            "is not found in the database")
    void givenVehicleNotFoundInTheDbWhenCreatingEspThenThrowBadRequest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        targetRepository.save(new JpaTarget(NEW_CONTROLLER_ID, "test", "test", 995L, CONTROLLER_ID));
        var vinsToReplace = NEW_CONTROLLER_ID;
        var existingVin = TARGET_00001;
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace(existingVin, vinsToReplace);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyString).andExpect(status().isBadRequest()).andExpect(result -> {
            String errorMessage = result.getResponse().getContentAsString();
            assertTrue(errorMessage.contains(NOT_MATCH_WITH_ECU_NODE_ADDRESS));
        });
    }

    @Test
    @Description("Given ESP packages in system but missing on storage (S3) or incorrect pre-signed S3 URL generated, " +
            "when there is an error downloading, then return NOT_FOUND")
    @SuppressWarnings("unchecked")
    void givenEspPackagesInStorage_whenErrorDownloading_thenReturnNotFound() throws Exception {
        // Mock valid S3 URL generation
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        String contentAsString = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn().
                getResponse().getContentAsString();
        MgmtSupportPackage esp = objectMapper.readValue(contentAsString, MgmtSupportPackage.class);
        assertEquals(1, espRepository.findAll().size());
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
        mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS,
                1, rollout.getId(), esp.getSupportPackageId()).queryParam(TYPE_QUERY_PARAM, ESP)).andExpect(status().isNotFound());

    }

    @Test
    @Description("Verify unlink of rollouts with associated ESP support packages")
    @SuppressWarnings("unchecked")
    void givenRolloutWithESPPackageWhenDeletingThenDeleteAssociationAndMarkAsDeleted() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("ESPUnlinkDeleteTarget");
        customTestData.setRolloutPrefix("ESPUnlinkDeleteRolloutPrefix");
        customTestData.setRolloutName("ESPUnlinkDeleteRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("ESPUnlinkDeleteDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());

        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isOk());
    }

    @Test
    @Description("Test for creating a new ESP support package")
    @SuppressWarnings("unchecked")
    void shouldCreateNewESPTest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(1, espRepository.findAll().size());
        assertEquals(10, espRepository.findAll().get(0).getEspEcuRollouts().size());
        assertEquals(espEcuRolloutRepository.findAll().get(0).getSupportPackage(), espRepository.findAll().get(0));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));

    }

    @Test
    @Description("Test for creating a new ESP support package throws bad request error if the controllerIds are " +
            "not associated with provided ECU Node Address")
    void givenRandomVinsWhenCreatingEspThenThrowBadRequest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        //add a new target to the repository that is not associated with the provided ECU Node Address
        targetRepository.save(new JpaTarget(RANDOM_TARGET, "test", "test", 999L, CONTROLLER_ID));
        var vinsToReplace = RANDOM_TARGET;
        var existingVin = TARGET_00001;
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace(existingVin, vinsToReplace);
        var createRequestObject = objectMapper.readValue(reqBodyString, MgmtFileUrlSupportPackageCreateRequest.class);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), createRequestObject).andExpect(status().isBadRequest()).andExpect(result -> {
            String errorMessage = result.getResponse().getContentAsString();
            assertTrue(errorMessage.contains(NOT_MATCH_WITH_ECU_NODE_ADDRESS));

        });
    }

    @Test
    @Description("Test for creating a new ESP support package ensuring different SHA-256 with existing file type throws error for existing rollout")
    @SuppressWarnings("unchecked")
    void givenEspSupportPackageWithExistingFileTypeAndDifferentSha256WhenCreatingForExistingRolloutThenThrowError() throws Exception {
        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix("replaceTarget");
        supportPackageTestData.setRolloutPrefix("replaceRolloutPrefix");
        supportPackageTestData.setRolloutName("replaceRolloutName");
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        supportPackageTestData.setDistributionSetName("replaceDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, supportPackageTestData);
        var rollout = (Rollout) testData.get(ROLLOUT);
        var requestBody = (MgmtFileUrlSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());
        assertEquals(1, espRepository.findAll().size());

        MgmtBaseSupportPackageCreateRequest newRequestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(requestBody.getFileUrl())
                .fileName(requestBody.getFileName())
                .fileType(requestBody.getFileType())
                .sha256(generateSha256Hash(DIFFERENT_FILE))
                .fileVersion(requestBody.getFileVersion())
                .controllerIds(requestBody.getControllerIds())
                .ecuNodeAddress(requestBody.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(requestBody.getFileMetadata())
                .build();

        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), newRequestBody).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The device of this fileType already exists for the rollout Id")));;
    }


    @Test
    @Description("Test for creating a new ESP support package with empty VINs")
    void shouldNotCreateESPWhenVinsAreEmpty() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("emptyVins");
        customTestData.setRolloutPrefix("emptyVinsRolloutPrefix");
        customTestData.setRolloutName("emptyVinsRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("emptyVinsDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        var vinsToReplace = "[]";
        var existingVin = "[ \"emptyVins-00000\", \"emptyVins-00001\", \"emptyVins-00002\", \"emptyVins-00003\", \"emptyVins-00004\", \"emptyVins-00005\", \"emptyVins-00006\", \"emptyVins-00007\", \"emptyVins-00008\", \"emptyVins-00009\" ]";
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace(existingVin, vinsToReplace);
        var reqBodyObj = objectMapper.readValue(reqBodyString, MgmtBaseSupportPackageCreateRequest.class);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyObj).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Validate delete support packages of a rollout is success on esp packages delete")
    @SuppressWarnings("unchecked")
    void givenRolloutIdHavingEspWhenDeleteSupportPackageThenSuccess() throws Exception {
        reset(s3Client);
        // Create ESP support package
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("SetEspDeleteTarget");
        customTestData.setRolloutPrefix("EspDeleteRolloutPrefix");
        customTestData.setRolloutName("EspDeleteRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("EspDeleteDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());
        List<JpaEsp> esp = espRepository.findAll();
        assertEquals(1, esp.size());
        assertEquals(espEcuRolloutRepository.findAll().get(0).getSupportPackage(), esp.get(0));

        //Delete Support Package
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isOk());
        assertEquals(0, espRepository.findAll().size());
    }

    @Test
    @Description("This test case verifies that the API successfully returns all ESP support packages for a specific rollout.")
    @SuppressWarnings("unchecked")
    void testGetESPSupportPackageWithValidPkgId() throws Exception {
        SupportPackageTestData espTestData = new SupportPackageTestData();
        espTestData.setTargetPrefix("SetTarget04");
        espTestData.setRolloutPrefix("RolloutPrefixSupportkg05");
        espTestData.setRolloutName("Rollout05");
        espTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        espTestData.setDistributionSetName("DistributionSetName04");
        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, espTestData);
        Rollout espRollout = (Rollout) espTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), espRequestBody).andExpect(status().isCreated());

        Long tenantId = 1L;
        mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + espRollout.getId() + SUPPORT_PACKAGES_URL + "Esp/" + espRepository.findAll().get(0).getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @Description("Test for creating a new ESP support package with ECU Node Address")
    @SuppressWarnings("unchecked")
    void shouldCreateESPWithEcuNodeAddress() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setEcuNodeAddress("ecunodeaddress");
        customTestData.setFileType(MgmtSupportPackageFileType.ECU_SCRIPT);
        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());
    }

    @Test
    @Description("Test for creating a new ESP support package with invalid target filter")
    @SuppressWarnings("unchecked")
    void shouldNotCreateESPWhenReqContainVinDoNotMatchTargetFilter() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("targetFilter");
        customTestData.setRolloutPrefix("targetFilterRolloutPrefix");
        customTestData.setRolloutName("targetFilterRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("targetFilterDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        var vinsToReplace = RANDOM_TARGET;
        var existingVin = "targetFilter-00000";
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace(existingVin, vinsToReplace);
        testdataFactory.createTarget(RANDOM_TARGET);
        var reqBodyObj = objectMapper.readValue(reqBodyString, MgmtFileUrlSupportPackageCreateRequest.class);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyObj).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Test for creating a new ESP support package throws bad request error if one or more controllerIds are " +
            "associated with a different ECU Node Address")
    void givenOneVinAssociatedWithDifferentEcuNodeAddWhenCreatingEspThenThrowBadRequest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TARGET);
        customTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        customTestData.setRolloutName(ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        //Create a new vehicle and associate it wuth a different ECU Node Address
        JpaEcuModel ecuModel1 = new JpaEcuModel();
        JpaEcuModelType ecuModelType1 = new JpaEcuModelType("OM1");
        ecuModelType1.setId(97L);
        ecuModel1.setId(95L);
        ecuModel1.setEcuModelType(ecuModelType1);
        ecuModel1.setEcuModelName("Test ECU Model");
        ecuModel1.setEcuNodeId("different-ecu-node-address");
        Set<EcuModel> ecuModels = new HashSet<>();
        ecuModels.add(ecuModel1);

        JpaVehicle v1 = new JpaVehicle();
        v1.setId(11L);
        v1.setVehicleEcu(ecuModels);
        v1.setName("Test vehicle");
        var vehicle = vehicleRepository.save(v1);
        targetRepository.save(new JpaTarget(NEW_CONTROLLER_ID, "test", "test", vehicle.getId(), CONTROLLER_ID));
        var vinsToReplace = NEW_CONTROLLER_ID;
        var existingVin = TARGET_00001;
        var reqBodyString = objectMapper.writeValueAsString(requestBody).replace(existingVin, vinsToReplace);
        var reqBodyObj = objectMapper.readValue(reqBodyString, MgmtFileUrlSupportPackageCreateRequest.class);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), reqBodyObj).andExpect(status().isBadRequest()).andExpect(result -> {
            String errorMessage = result.getResponse().getContentAsString();
            assertTrue(errorMessage.contains(NOT_MATCH_WITH_ECU_NODE_ADDRESS));
        });
    }

    @Test
    @Description("Test for creating a ESP support package without ECU Node Address returns bad request")
    void testCreatingEspWithoutNodeAddressPresent() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setEcuNodeAddress("");

        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Test for creating a new ESP support package with Empty device list in Rollout Throw Bad Request")
    void createESPWithEmptyDeviceThrowsBadRequest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);

        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);

        // Create targets once and reuse them
        List<Target> targets = testdataFactory.createTargets(customTestData.getAmountTargets(), customTestData.getTargetPrefix(), customTestData.getRolloutPrefix());

        //Create ECU Model
        if (customTestData.getEcuNodeAddress() != null || customTestData.getEcuNodeAddress().isEmpty()) {
            testdataFactory.addNewEcuModels(createEcuModel(customTestData.getEcuNodeAddress()));
        }
        var ds = testdataFactory.createDistributionSet();
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));

        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        // Create request body using the provided parameters
        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();

        MgmtBaseSupportPackageCreateRequest requestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(customTestData.getFileUrl())
                .fileName(customTestData.getTestFileName())
                .fileType(customTestData.getFileType())
                .sha256(customTestData.getSha256())
                .fileVersion(customTestData.getVersion())
                .controllerIds(controllerIds)
                .ecuNodeAddress(customTestData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(customTestData.getFileMetadata())
                .build();

        var testData = Map.of(ROLLOUT, rollout, REQUEST_BODY, requestBody);

        Rollout rollout1 = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody1 = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout1.getId(), requestBody1).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Test for creating a new ESP support package with Empty List It Should Throw Bad Request")
    void shouldCreateESPWithEMPTYLIST() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setEcuNodeAddress("ECUNodeAddress");
        customTestData.setFileType(MgmtSupportPackageFileType.ECU_SCRIPT);
        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTestWithoutVins(customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given existing ESP for different rollout, when linking, then associate existing ESP to rollout and not reupload")
    @SuppressWarnings("unchecked")
    void givenExistingEspForDifferentRolloutWhenLinkingThenAssociateExistingEspToRolloutAndNotReupload() throws Exception {
        SupportPackageTestData customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET, TEST_ROLLOUT_PREFIX, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_1, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.ECU_SCRIPT);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        MvcResult mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        MgmtSupportPackage esp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);
        JpaEsp jpaEsp = espRepository.findById(esp.getSupportPackageId()).get();
        jpaEsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(jpaEsp);

        // Associating same sha256 with same rollout
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The device of this fileType already exists for the rollout Id")));

        // Associating the same sha256 with different rollout
        customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET + 1, TEST_ROLLOUT_PREFIX + 1, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_2, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.ECU_SCRIPT);
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_1);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        esp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        // Verifying ID is same
        assertEquals(jpaEsp.getId(), esp.getSupportPackageId());

        jpaEsp = espRepository.findById(esp.getSupportPackageId()).get();
        assertEquals(jpaEsp.getSupportPackageFileStatus().toString(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());

        jpaEsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        espRepository.save(jpaEsp);

        // Associating the same sha256 with different rollout
        customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET + 2, TEST_ROLLOUT_PREFIX + 2, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_3, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.ECU_SCRIPT);
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_2);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        esp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        // Verifying ID is same
        assertEquals(jpaEsp.getId(), esp.getSupportPackageId());

        jpaEsp = espRepository.findById(esp.getSupportPackageId()).get();
        assertEquals(jpaEsp.getSupportPackageFileStatus().toString(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
    }

    @Test
    @Description("Test for creating a new ESP support package ensuring existing SHA-256 with different file type throws error for existing rollout")
    @SuppressWarnings("unchecked")
    void givenEspSupportPackageWithExistingSha256AndDifferentFileTypeWhenCreatingForExistingRolloutThenThrowError() throws Exception {
        // Prepare and create the first ESP support package
        SupportPackageTestData espTestData = testdataFactory.getSupportPackageTestData("SetTarget01", "RolloutPrefix01", "Rollout01", TEST_ECU_NODE_ADDRESS_1, "DistributionSetName01", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.ADA_LICENSE);

        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, espTestData);
        Rollout espRollout = (Rollout) espTestDataMap.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), espRequestBody).andExpect(status().isCreated());

        // Prepare and attempt to create a second ESP support package with the same SHA-256 but different file type
        SupportPackageTestData duplicateEspTestData = testdataFactory.getSupportPackageTestData("SetTarget02", "RolloutPrefix02", "Rollout02",
                TEST_ECU_NODE_ADDRESS_1, "DistributionSetName01", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.ADA_CERTIFICATE);

        testdataFactory.createTargets(1, duplicateEspTestData.getTargetPrefix(), duplicateEspTestData.getRolloutPrefix());
        MgmtBaseSupportPackageCreateRequest requestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(duplicateEspTestData.getFileUrl())
                .fileName(duplicateEspTestData.getTestFileName())
                .fileType(duplicateEspTestData.getFileType())
                .sha256(duplicateEspTestData.getSha256())
                .fileVersion(duplicateEspTestData.getVersion())
                .controllerIds(List.of("SetTarget01-00006"))
                .ecuNodeAddress(duplicateEspTestData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(duplicateEspTestData.getFileMetadata())
                .build();

        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), requestBody)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The given entity already exists in database"));
    }

    @Test
    @Description("Test for creating a new ESP support package ensuring existing SHA-256 with different file type for a new rollout throws error ")
    @SuppressWarnings("unchecked")
    void givenEspSupportPackageWithExistingSha256AndDifferentFileTypeWhenCreatingForNewRolloutThenThrowError() throws Exception {
        // Prepare and create the first ESP support package
        SupportPackageTestData espTestData = testdataFactory.getSupportPackageTestData("SetTarget01", "RolloutPrefix01", "Rollout01", TEST_ECU_NODE_ADDRESS_1, "DistributionSetName01", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.ADA_LICENSE);

        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, espTestData);
        Rollout espRollout = (Rollout) espTestDataMap.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(espRollout.getId(), espRequestBody).andExpect(status().isCreated());

        // Prepare and attempt to create a second ESP support package with the same SHA-256 but different file type

        SupportPackageTestData duplicateEspTestData = testdataFactory.getSupportPackageTestData("SetTarget02", "RolloutPrefix02", "Rollout02", TEST_ECU_NODE_ADDRESS_2, "DistributionSetName02", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.ADA_CERTIFICATE);

        var duplicateEspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, duplicateEspTestData);
        Rollout duplicateEspRollout = (Rollout) duplicateEspTestDataMap.get(ROLLOUT_2);
        MgmtBaseSupportPackageCreateRequest duplicateEspRequestBody = (MgmtBaseSupportPackageCreateRequest) duplicateEspTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) duplicateEspTestDataMap.get(TARGETS), (List<EcuModel>) duplicateEspTestDataMap.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(duplicateEspRollout.getId(), duplicateEspRequestBody)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The given entity already exists in database"));

    }

    @Test
    @Description("Created a rollout and the status is running and then created a new device and associated ESP's to it when the new device is not part of any rolloutGroup of the give rollout")
    void givenNewTargetWhenRolloutIsInRunningThenAppropriateResponse() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(1);

        String ecuNodeId = (String) rolloutDetails.get("ecuNodeId");
        Rollout rollout = (Rollout) rolloutDetails.get("rollout");
        Long vehicleModelId = (Long) rolloutDetails.get("vehicleModelId");
        String supportPackageUrl = (String) rolloutDetails.get("supportPackageUrl");

        Optional<JpaRollout> updatesRollout = rolloutRepository.getRolloutById(rollout.getId());
        assertEquals(RolloutStatus.RUNNING, updatesRollout.get().getStatus());

        List<MgmtTarget> targets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> secondControllerIds = getControllerIds(targets);

        MgmtBaseSupportPackageCreateRequest ecuScriptEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ECU_SCRIPT, ecuNodeId, supportPackageUrl, secondControllerIds);

        // Sending the new target which is not part of any rolloutGroup of the given RUNNING rollout then should return isCreated()
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout.get().getId(), ecuScriptEspCreateRequest)
                .andExpect(status().isCreated());

        //create a new ESP support package for a rollout with an existing device of the same file type results in a BadRequest response, with an appropriate error message.
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout.get().getId(), ecuScriptEspCreateRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The device of this fileType already exists for the rollout Id: " + rollout.getId() + ", Controller IDs: " + secondControllerIds));

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Optional<JpaRollout> updatesRollout2 = rolloutRepository.getRolloutById(rollout.getId());
        assertEquals(RolloutStatus.PAUSED, updatesRollout2.get().getStatus());

        MgmtBaseSupportPackageCreateRequest udsFlowEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, secondControllerIds);

        // Sending the new target which is not part of any rolloutGroup of the given PAUSED rollout then should return isCreated()
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout2.get().getId(), udsFlowEspCreateRequest)
                .andExpect(status().isCreated());

        List<MgmtTarget> thirdTarget = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> thirdControllerIds = getControllerIds(thirdTarget);

        // Combine secondControllerIds and thirdControllerIds into one list such that we can pass one device which is associated and one is not
        List<String> combinedControllerIds = new ArrayList<>(secondControllerIds);
        combinedControllerIds.addAll(thirdControllerIds);

        MgmtBaseSupportPackageCreateRequest secondUdsFlowEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, combinedControllerIds);

        // Sending the list of targets in which one is associated and one is not then return badRequest
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout2.get().getId(), secondUdsFlowEspCreateRequest)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The device of this fileType already exists for the rollout Id: " + rollout.getId() + ", Controller IDs: " + secondControllerIds));
    }

    @Test
    @Description("Created a rollout and the status is running and associated ESP's to it when sorting and filtering the support packages then should return the appropriate response")
    void givenRolloutRunningWhenSortingAndFilteringThenAppropriateResponse() throws Exception {
        Map<String, Object> rolloutDetails = setupRolloutWithTargets(1);

        String ecuNodeId = (String) rolloutDetails.get("ecuNodeId");
        Rollout rollout = (Rollout) rolloutDetails.get("rollout");
        String supportPackageUrl = (String) rolloutDetails.get("supportPackageUrl");
        List<Target> targets = (List<Target>) rolloutDetails.get("targets");

        Optional<JpaRollout> updatesRollout = rolloutRepository.getRolloutById(rollout.getId());
        assertEquals(RolloutStatus.RUNNING, updatesRollout.get().getStatus());

        assertEquals(2, espRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().size());

        List<String> controllerId = Collections.singletonList(targets.get(0).getControllerId());

        MgmtBaseSupportPackageCreateRequest udsFlowEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, controllerId);
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout.get().getId(), udsFlowEspCreateRequest)
                .andExpect(status().isCreated());


        MgmtBaseSupportPackageCreateRequest ecuScriptEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ECU_SCRIPT, ecuNodeId, supportPackageUrl, controllerId);
        invokeCreateSupportPackageApiAndReturnHttpResponse(updatesRollout.get().getId(), ecuScriptEspCreateRequest)
                .andExpect(status().isCreated());

        assertEquals(4, espRepository.findAll().size());

        invokeGetAllSupportPackagesApi(updatesRollout.get().getId(), 0,4, "id:asc", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.content").isArray());

        invokeGetAllSupportPackagesApi(updatesRollout.get().getId(), 0,4, null, "id==2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.size").value(1));

        invokeGetAllSupportPackagesApi(updatesRollout.get().getId(), 0,4, null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.size").value(5));

        invokeGetAllSupportPackagesApi(updatesRollout.get().getId(), 0,4, "sha256:desc", null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.size").value(5));

    }

    @Test
    @Description("Test for creating a new ESP support package ensuring existing SHA-256 with existing file type throws error for existing rollout")
    @SuppressWarnings("unchecked")
    void givenEspSupportPackageWithExistingFileTypeAndExistingSha256WhenCreatingForExistingRolloutThenThrowError() throws Exception {
        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix("replaceTarget");
        supportPackageTestData.setRolloutPrefix("replaceRolloutPrefix");
        supportPackageTestData.setRolloutName("replaceRolloutName");
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        supportPackageTestData.setDistributionSetName("replaceDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, supportPackageTestData);
        var rollout = (Rollout) testData.get(ROLLOUT);
        var requestBody = (MgmtFileUrlSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated());
        assertEquals(1, espRepository.findAll().size());

        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The device of this fileType already exists for the rollout Id")));;
    }
}