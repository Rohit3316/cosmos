package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.sns.models.S3FileDeletionRequest;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.cosmos.models.mgmt.MgmtRestConstants.BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MgmtRspResourceTest extends AbstractSupportPackageManagementApiIntegrationTest {

    @Test
    @Description("Test for associating new rollout with existing package")
    void shouldAssociateThRSPRolloutWhenSupportPackageExists() throws Exception {
        SupportPackageTestData customTestDataForFirstReq = new SupportPackageTestData();
        customTestDataForFirstReq.setTargetPrefix("RSPFirstSetTarget");
        customTestDataForFirstReq.setRolloutPrefix("RSPFirstRolloutPrefix");
        customTestDataForFirstReq.setRolloutName("RSPFirstRolloutName");
        customTestDataForFirstReq.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestDataForFirstReq.setDistributionSetName("RSPFirstDistributionSetName");
        customTestDataForFirstReq.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestDataForFirstReq);
        Rollout rollout = (Rollout) testData.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        Rsp rsp = rspRepository.findAll().get(0);

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("RSPSecondSetTarget");
        customTestData.setRolloutPrefix("RSPSecondRolloutPrefix");
        customTestData.setRolloutName("RSPSecondRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_2);
        customTestData.setDistributionSetName("RSPSecondDistributionSetName");
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_2);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(2, rspRepository.findAll().size());
        assertEquals(rsp, rspRepository.findAll().get(0));
        assertEquals(2, rspRolloutRepository.findAll().size());
    }

    @Test
    @Description("Test for creating a new RSP support package throws error if the rollout is not in DRAFT state")
    void givenRolloutNotInDraftStateWhenCreatingRSPThenThrowError() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verify unlink of rollouts with associated RSP support packages when SNS message publishing fails, then throw internal server error")
    void givenRolloutWithRspPackageWhenDeletingSnsFailureThenThrowInternalServerError() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("RSPUnlinkDeleteTarget");
        customTestData.setRolloutPrefix("RSPUnlinkDeleteRolloutPrefix");
        customTestData.setRolloutName("RSPUnlinkDeleteRolloutName");
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName("RSPUnlinkDeleteDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class))).thenThrow(new CompletionException(new RuntimeException("Failed to publish message to SNS")));
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(CompletableFuture.failedFuture(new RuntimeException("Failed to publish message to SNS")));
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isInternalServerError());
    }

    @Test
    @Description("This test case verifies that the API successfully returns all RSP support packages for a specific rollout.")
    void testGetRSPSupportPackageByValidPkgId() throws Exception {
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix("RSPSetTarget02");
        rspTestData.setRolloutPrefix("RolloutPrefixSupportkg01");
        rspTestData.setRolloutName("RSPRolloutPkg02");
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName("RSPDistributionSetName02");


        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, rspTestData);
        Rollout rspRollout = (Rollout) rspTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rspRollout.getId(), rspRequestBody);

        // Perform the request to retrieve all support packages for the rollout and verify the response
        Long tenantId = 1L;
        mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + rspRollout.getId() + SUPPORT_PACKAGES_URL + "Rsp/" + rspRepository.findAll().get(0).getId()).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @Description("Test for creating a new RSP support package")
    void shouldCreateNewRSPTest() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rspRepository.findAll().get(0));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));

    }

    @Test
    @Description("This test case verifies that the API throws an EntityNotFoundException when an invalid RSP support package ID is provided.")
    void testGetRSPSupportPackageWithInvalidPkgId() throws Exception {
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix("RSPSetTarget03");
        rspTestData.setRolloutPrefix("RolloutPrefixSupportkg09");
        rspTestData.setRolloutName("RolloutPrefixSupportkg");
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName("RSPDistributionSetName01");


        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, rspTestData);
        Rollout rspRollout = (Rollout) rspTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rspRollout.getId(), rspRequestBody);

        Long tenantId = 1L;
        Long invalidPkgId = 100L;

        mvc.perform(get(TENANTS_V1_MANAGEMENT + tenantId + ROLLOUTS_URL + rspRollout.getId() + SUPPORT_PACKAGES_URL + "Rsp/" + invalidPkgId).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    @Test
    @Description("Verify unlink of rollouts with associated ESP and RSP support packages")
    @SuppressWarnings("unchecked")
    void givenRolloutWithESPRSPPackageWhenDeletingThenDeleteAssociation() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("ESPDeleteTarget");
        customTestData.setRolloutPrefix("ESPDeleteRolloutPrefix");
        customTestData.setRolloutName("ESPDeleteRolloutName");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("ESPDeleteDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        //Create Rsp for the same Rollout
        SupportPackageTestData customTestRspData = new SupportPackageTestData();
        customTestRspData.setTargetPrefix("RSPDeleteTarget");
        customTestRspData.setRolloutPrefix(RSP_DELETE_ROLLOUT_PREFIX);
        customTestRspData.setRolloutName(RSP_DELETE_ROLLOUT_NAME);
        customTestRspData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestRspData.setDistributionSetName(RSP_DELETE_DISTRIBUTION_SET_NAME);

        MgmtBaseSupportPackageCreateRequest rspRequest = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(customTestRspData.getFileUrl())
                .fileName(customTestRspData.getTestFileName())
                .fileType(customTestRspData.getFileType())
                .sha256(customTestRspData.getSha256())
                .fileVersion(customTestRspData.getVersion())
                .controllerIds(requestBody.getControllerIds())
                .ecuNodeAddress(customTestRspData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(customTestRspData.getFileMetadata())
                .build();


        invokeCreateSupportPackageApi(rollout.getId(), rspRequest);

        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isOk());
    }

    @Test
    @Description("Test for creating a new RSP support package withOUT ECU Node Address")
    @SuppressWarnings("unchecked")
    void shouldCreateRSPWithOutEcuNodeAddress() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.ADA_LICENSE);
        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
    }

    @Test
    @Description("Test for creating a new ESP/RSP support package throws error if the rollout is CANCELLING or CANCELLED or FINISHING or FINISHED or DELETING state")
    void givenRolloutInCancelledStateWhenCreatingRSPThenThrowError() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        rolloutManagement.delete(rollout.getId());
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Validate delete support packages of a rollout is success on rsp packages delete")
    void givenRolloutIdHavingRspWhenDeleteSupportPackageThenSuccess() throws Exception {
        reset(s3Client);
        // Create RSP support package
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("SetRspDeleteTarget");
        customTestData.setRolloutPrefix(RSP_DELETE_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_DELETE_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);

        customTestData.setDistributionSetName(RSP_DELETE_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        List<JpaRsp> rsp = rspRepository.findAll();
        assertEquals(1, rsp.size());
        assertEquals(1, rsp.get(0).getRspRollouts().size());
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rsp.get(0));

        //Delete Support Package
        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class))).thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES, 1, rollout.getId())).andExpect(status().isOk());
        assertEquals(0, rspRepository.findAll().size());
    }

    @Test
    @Description("Test for verifying WHATS_NEW RSP")
    void verifyingWhatsNewEnum() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.WHATS_NEW);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        List<JpaRsp> rspList = rspRepository.findAll();
        assertEquals(1, rspList.size());
        Rsp savedRsp = rspList.get(0);
        assertEquals("WHATS_NEW", savedRsp.getRollout().getName());
        assertEquals(MgmtSupportPackageFileType.WHATS_NEW, savedRsp.getFileType());

        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rspRepository.findAll().get(0));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Test for verifying UDS_GLOBAL_PRE_INSTALL RSP")
    void givenRspWhenUdsGlobalPreInstallAvailableThenEnumSuccess() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.UDS_GLOBAL_PRE_INSTALL);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        List<JpaRsp> rspList = rspRepository.findAll();
        assertEquals(1, rspList.size());
        Rsp savedRsp = rspList.get(0);
        assertEquals("UDS_GLOBAL_PRE_INSTALL", savedRsp.getRollout().getName());
        assertEquals(MgmtSupportPackageFileType.UDS_GLOBAL_PRE_INSTALL, savedRsp.getFileType());

        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rspRepository.findAll().get(0));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Test for verifying UDS_GLOBAL_POST_INSTALL RSP")
    void givenRspWhenUdsGlobalPostInstallAvailableThenEnumSuccess() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.UDS_GLOBAL_POST_INSTALL);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        List<JpaRsp> rspList = rspRepository.findAll();
        assertEquals(1, rspList.size());
        Rsp savedRsp = rspList.get(0);
        assertEquals("UDS_GLOBAL_POST_INSTALL", savedRsp.getRollout().getName());
        assertEquals(MgmtSupportPackageFileType.UDS_GLOBAL_POST_INSTALL, savedRsp.getFileType());

        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        assertEquals(rspRolloutRepository.findAll().get(0).getSupportPackage(), rspRepository.findAll().get(0));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Test for replacing existing ESP support package when SHA-256 is different")
    void givenAddRspWhenSha256IsDifferentThenReplaceError() throws Exception {
        SupportPackageTestData supportPackageTestData = new SupportPackageTestData();
        supportPackageTestData.setTargetPrefix("RspReplaceTarget");
        supportPackageTestData.setRolloutPrefix("RSPReplaceRolloutPrefix");
        supportPackageTestData.setRolloutName("RspReplaceRolloutName");
        supportPackageTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        supportPackageTestData.setDistributionSetName("RSPReplaceDistributionSetName");
        supportPackageTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, supportPackageTestData);
        var rollout = (Rollout) testData.get(ROLLOUT);
        var requestBody = (MgmtFileUrlSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(1, rspRepository.findAll().size());
        var newRequestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(requestBody.getFileUrl())
                .fileName(requestBody.getFileName())
                .fileType(requestBody.getFileType())
                .sha256(generateSha256Hash(DIFFERENT_FILE))
                .fileVersion(requestBody.getFileVersion())
                .controllerIds(requestBody.getControllerIds())
                .ecuNodeAddress(requestBody.getEcuNodeAddress())
                .fileContentDescription(requestBody.getFileContentDescription())
                .fileInfoUrl(requestBody.getFileInfoUrl())
                .fileMetadata(requestBody.getFileMetadata())
                .build();

        ResultActions resultActions = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), newRequestBody);
        resultActions.andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given RSP packages in system but missing on storage (S3) or incorrect pre-signed S3 URL generated, " +
            "when there is an error downloading, then return NOT_FOUND")
    void givenRspPackagesInStorage_whenErrorDownloading_thenReturnNotFound() throws Exception {

        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.WHATS_NEW);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        MgmtSupportPackage rsp = invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
        mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS,
                1, rollout.getId(), rsp.getSupportPackageId()).queryParam(TYPE_QUERY_PARAM, RSP)).andExpect(status().isNotFound());

    }

    @Test
    @Description("Verify unlink of rollouts with associated RSP support packages")
    void givenRolloutWithRspPackageWhenDeletingThenDeleteAssociation() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("RSPUnlinkDeleteTarget");
        customTestData.setRolloutPrefix("RSPUnlinkDeleteRolloutPrefix");
        customTestData.setRolloutName("RSPUnlinkDeleteRolloutName");
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName("RSPUnlinkDeleteDistributionSetName");
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);

        when(s3FileDeleteSnsService.publishMessage(any(S3FileDeletionRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId(MOCK_MESSAGE_ID).build()));
        mvc.perform(delete(BASE_TENANT_ROLLOUT_SUPPORT_PACKAGES,
                        1, rollout.getId()))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given RSP packages in storage, when downloading, then return appropriate response")
    void givenRspPackagesInStorage_whenDownloading_thenReturnAppropriateResponse() throws Exception {

        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(RSP_SET_TARGET);
        customTestData.setRolloutPrefix(RSP_ROLLOUT_PREFIX);
        customTestData.setRolloutName(RSP_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.WHATS_NEW);
        customTestData.setDistributionSetName(RSP_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtFileUrlSupportPackageCreateRequest requestBody = (MgmtFileUrlSupportPackageCreateRequest) testData.get(REQUEST_BODY);

        // Mock valid S3 URL generation
        when(s3Service.generatePresignedUrl(any(), any(), any())).thenReturn(new URL(requestBody.getFileUrl()));
        when(s3Service.isValidGetUrl(any())).thenReturn(true);


        MgmtSupportPackage rsp = invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        assertEquals(1, rspRepository.findAll().size());
        assertEquals(1, rspRepository.findAll().get(0).getRspRollouts().size());
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_DOWNLOAD_V1_REQUEST_MAPPING_TENANT_TARGETS,
                1, rollout.getId(), rsp.getSupportPackageId()).queryParam(TYPE_QUERY_PARAM, RSP)).andExpect(status().is3xxRedirection());

    }

    @Test
    @Description("Test for creating a new RSP support package with empty VIN list")
    void shouldCreateRSPWithEmptyVinList() throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(SET_TEST_TARGET);
        customTestData.setRolloutPrefix(TEST_ROLLOUT_PREFIX);
        customTestData.setRolloutName(TEST_ROLLOUT_NAME);
        customTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        customTestData.setDistributionSetName(TEST_DISTRIBUTION_SET_NAME);
        var testData = prepareTestDataForCreatingSupportPackageTestWithoutVins(customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        invokeCreateSupportPackageApi(rollout.getId(), requestBody);
    }

    @Test
    @Description("Given existing RSP for different rollout, when linking, then associate existing RSP to rollout and not reupload")
    @SuppressWarnings("unchecked")
    void givenExistingRspForDifferentRolloutWhenLinkingThenAssociateExistingRspToRolloutAndNotReupload() throws Exception {
        SupportPackageTestData customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET, TEST_ROLLOUT_PREFIX, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_1, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.PROXI_SIGNATURE);
        var testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT, customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        MvcResult mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        MgmtSupportPackage rsp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);
        JpaRsp jpaRsp = rspRepository.findById(rsp.getSupportPackageId()).get();
        jpaRsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(jpaRsp);

        invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString(String.format("Rollout %d is already associated with RSP file of the file type %s", rollout.getId(), requestBody.getFileType()))));

        // Associating the same sha256 with different rollout
        customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET + 1, TEST_ROLLOUT_PREFIX + 1, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_2, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.ECU_SCRIPT);
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_1);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        rsp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        // Verifying ID is same
        assertEquals(jpaRsp.getId(), rsp.getSupportPackageId());

        jpaRsp = rspRepository.findById(rsp.getSupportPackageId()).get();
        assertEquals(jpaRsp.getSupportPackageFileStatus().toString(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());

        jpaRsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(jpaRsp);
        // Associating the same sha256 with different rollout
        customTestData = testdataFactory.getSupportPackageTestData(SET_TEST_TARGET + 2, TEST_ROLLOUT_PREFIX + 2, TEST_ROLLOUT_NAME, TEST_ECU_NODE_ADDRESS_3, TEST_DISTRIBUTION_SET_NAME, TEST_SHA256, MgmtSupportPackageFileType.ECU_SCRIPT);
        testData = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, customTestData);
        rollout = (Rollout) testData.get(ROLLOUT_2);
        requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) testData.get(TARGETS), (List<EcuModel>) testData.get(ECU_MODELS));
        mvcResult = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), requestBody).andExpect(status().isCreated()).andReturn();
        rsp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtSupportPackage.class);

        // Verifying ID is same
        assertEquals(jpaRsp.getId(), rsp.getSupportPackageId());

        jpaRsp = rspRepository.findById(rsp.getSupportPackageId()).get();
        assertEquals(jpaRsp.getSupportPackageFileStatus().toString(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());

    }

    @Test
    @Description("Test for creating a new RSP support package ensuring existing SHA-256 with different file type throws error")
    void givenRspSupportPackageWithExistingSha256AndDifferentFileTypeWhenCreatingForNewRolloutThenThrowError() throws Exception {
        // Prepare and create the first RSP support package
        SupportPackageTestData rspTestData = testdataFactory.getSupportPackageTestData("SetTarget01", "RolloutPrefix01", "Rollout01", TEST_ECU_NODE_ADDRESS_1, "DistributionSetName01", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.PROXI_SIGNATURE);

        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, rspTestData);
        Rollout rspRollout = (Rollout) rspTestDataMap.get(ROLLOUT_1);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(rspRollout.getId(), rspRequestBody).andExpect(status().isCreated());

        // Prepare and attempt to create a second RSP support package with the same SHA-256 but different file type

        SupportPackageTestData duplicateRspTestData = testdataFactory.getSupportPackageTestData("SetTarget02", "RolloutPrefix02", "Rollout02", TEST_ECU_NODE_ADDRESS_2, "DistributionSetName02", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.PROXI);


        var duplicateRspTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_2, duplicateRspTestData);
        Rollout duplicateRspRollout = (Rollout) duplicateRspTestDataMap.get(ROLLOUT_2);
        MgmtBaseSupportPackageCreateRequest duplicateRspRequestBody = (MgmtBaseSupportPackageCreateRequest) duplicateRspTestDataMap.get(REQUEST_BODY);
        invokeCreateSupportPackageApiAndReturnHttpResponse(duplicateRspRollout.getId(), duplicateRspRequestBody)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The given entity already exists in database"));
    }

}
