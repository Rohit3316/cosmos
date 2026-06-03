package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Step;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.springframework.test.web.servlet.ResultActions;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.sns.models.CdnUploadRequest;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
 class MgmtRolloutRetrievalTest extends AbstractManagementRolloutApiIntegrationTest{


    @Test
    @Description("Testing that the targets of rollout group can be retrieved")
    void retrieveTargetsFromRolloutGroup() throws Exception {
        // setup
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Long tenantId = 1L;
        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        rolloutManagement.freeze(rollout.getId());
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        // retrieve targets from the first rollout group with known ID
        mvc.perform(
                        get(BASE_URL + tenantId + ROLLOUT_GROUP_TARGETS_ENDPOINT, rollout.getId(), firstGroup.getId())
                                .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(10))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(10)));
    }

    @Test
    @Description("Testing that the targets of rollout group can be retrieved with rsql query param")
    void retrieveTargetsFromRolloutGroupWithQuery() throws Exception {
        final Long tenantId = 1L;
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(10,10);

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        final String targetInGroup = rolloutGroupManagement.findTargetsOfRolloutGroup(PAGE, firstGroup.getId()).getContent().get(0).getControllerId();

        // retrieve targets from the first rollout group with known ID
        mvc.perform(get(BASE_URL + tenantId + ROLLOUT_GROUP_TARGETS_ENDPOINT, rollout.getId(), firstGroup.getId()).accept(MediaType.APPLICATION_JSON).param("q", "controllerId==" + targetInGroup)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)));
    }

    @Test
    @Description("Testing that starting the rollout switches the state to starting and then to running")
    void startingRolloutSwitchesIntoRunningState() throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), targets, rollout, true);

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        // starting rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check rollout is in starting state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(STATUS, equalTo(STARTING)));

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // check rollout is in running state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(STATUS, equalTo(RUNNING)));
    }
    @Test
    @Description("Retrieves single rollout from management API including extra data that is delivered only for single rollout access.")
    void retrieveSingleRollout() throws Exception {
        List<Target> targets = testdataFactory.createTargets(20, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        retrieveAndVerifyRolloutInDraftState(rollout);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        retrieveAndVerifyRolloutInStarting(rollout);

        // We need to remove rollout group logic for this test to work
        retrieveAndVerifyRolloutInRunning(rollout);
    }


    void retrieveRolloutListFullRepresentation() throws Exception {
        List<Target> targets = testdataFactory.createTargets(20, ROLLOUT, ROLLOUT);
        // create a running rollout for the created targets
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        rolloutHandler.handleAll();
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // request the list of rollouts with full representation
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "?representation=full", TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)))
                .andExpect(jsonPath("content[0].id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath(CONTENT_0_NAME, equalTo(rollout.getName())))
                .andExpect(jsonPath("content[0].description", equalTo(rollout.getDescription())))
                .andExpect(jsonPath(CONTENT_0_STATUS, equalTo(RUNNING)))
                .andExpect(jsonPath("content[0].startAt", equalTo(rollout.getStartAt().intValue())))
                .andExpect(jsonPath("content[0].endAt", equalTo(rollout.getEndAt().intValue())))
                .andExpect(jsonPath("content[0].userAcceptanceRequired", equalTo(rollout.getUserAcceptanceRequired().getName())))
                .andExpect(jsonPath("content[0].connectivityType", equalTo(rollout.getConnectivityType().getName())))
                .andExpect(jsonPath("content[0].log.collectionRequired", equalTo(rollout.isLogCollectionRequired())))
                .andExpect(jsonPath("content[0].log.maxFailureVin", equalTo(rollout.getLogMaxFailureVin())))
                .andExpect(jsonPath("content[0].log.maxNumberOfFiles", equalTo(rollout.getLogMaxNumberOfFiles())))
                .andExpect(jsonPath("content[0].log.maxAllFileSize", equalTo(rollout.getLogMaxAllFileSize())))
                .andExpect(jsonPath("content[0].log.maxSuccessVin", equalTo(rollout.getLogMaxSuccessVin())))
                .andExpect(jsonPath("content[0].log.maxEachFileSize", equalTo(rollout.getLogMaxEachFileSize())))
                .andExpect(jsonPath("content[0].deploymentMetadata.downgradeAllowed", equalTo(rollout.getDowngradeAllowed().getValue())))
                .andExpect(jsonPath("content[0].deploymentMetadata.requiredMedia", equalTo(rollout.getRequiredMedia().getValue())))
                .andExpect(jsonPath("content[0].deploymentMetadata.requiredStateOfCharge", equalTo(Collections.emptyMap())))
                .andExpect(jsonPath("content[0].maxDownloadCellularDurationTimer", equalTo(rollout.getMaxDownloadCellularDurationTimer())))
                .andExpect(jsonPath("content[0].maxDownloadDurationTimer", equalTo(rollout.getMaxDownloadDurationTimer())))
                .andExpect(jsonPath("content[0].maxDownloadWifiDurationTimer", equalTo(rollout.getMaxDownloadWifiDurationTimer())))
                .andExpect(jsonPath("content[0].downloadRetryCount", equalTo(rollout.getDownloadRetryCount())))
                .andExpect(jsonPath("content[0].maxUpdateTime", equalTo(rollout.getMaxUpdateTime())))

                // Verifying the links in the Get All Rollouts API response
                .andExpect(jsonPath(CONTENT_0_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath("content[0]._links.start.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(START_URL))))
                .andExpect(jsonPath("content[0]._links.pause.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(PAUSE_URL))))
                .andExpect(jsonPath("content[0]._links.resume.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(RESUME_URL))))
                .andExpect(jsonPath("content[0]._links.groups.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath("content[0]._links.softwares.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath("content[0]._links.triggerNextGroup.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString("/groups/next"))))
                .andExpect(jsonPath("content[0]._links.supportPackages.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath("content[0]._links.cancel.href", allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString("/cancel"))))
                .andExpect(jsonPath("content[0]._links.delete.href", allOf(startsWith(HREF_ROLLOUT_PREFIX))))
                .andExpect(jsonPath("content[0]._links.freeze.href").doesNotExist());

    }

    @Test
    @Description("Testing that an already started rollout cannot be started again and returns bad request")
    void startingAlreadyStartedRolloutReturnsBadRequest() throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), targets, rollout, true);


        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        // starting rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Run here, because scheduler is disabled during tests
        rolloutHandler.handleAll();

        // starting rollout - already started should lead into bad request
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rollout.getId()).header(SESSION_ID, SESSION_ID_HEADER)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(DEBUG, notNullValue()));
    }


    @Test
    @Description("Mocks SNS to simulate successful ESP uploads and verifies rollout transitions to RUNNING without errors.")
    void givenESPDetails_WhenUploadedToCDNSuccessfully_ThenRolloutStarts(CapturedOutput output) throws Exception {
        Rollout rollout = prepareSupportPackageTestData(
                "ESPSetTarget",
                "ESPRolloutPrefix",
                "ESPRolloutName", TEST_ECU_NODE_ADDRESS_1, "ESPDistributionSetName",
                MgmtSupportPackageFileType.LICENSE
        );

        List<JpaEsp> espList = espRepository.findAll();
        espList.forEach(esp -> {
            PublishResponse publishResponse = PublishResponse.builder().messageId("test-message-id").build();
            CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(publishResponse);
            when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);
            when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);

            rolloutManagement.freeze(rollout.getId());
            esp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
            espRepository.save(esp);
            rolloutHandler.handleAll();
            rolloutManagement.start(rollout.getId());
            JpaEsp esp1 = espRepository.findById(esp.getId()).orElse(null);
            assert esp1 != null;
            esp1.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
            espRepository.save(esp1);

            rolloutHandler.handleAll();
            verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));


            Rollout rollout1 = rolloutManagement.get(rollout.getId()).get();
            assertEquals(RolloutStatus.RUNNING, rollout1.getStatus());
            rolloutGroupRepository.findByRolloutAndStatusNotIn((JpaRollout) rollout1, EnumSet.of(RolloutGroupStatus.RUNNING)).forEach(rolloutGroup -> {
                assertEquals(RolloutGroupStatus.RUNNING, rolloutGroup.getStatus());
            });


        });
    }

    @Test
    @Description("Given an uploaded RSP, when the rollout starts, then the system should avoid attempting to upload the RSP again.")
    void givenUploadedRspWhenRolloutStartsThenAvoidRspUpload() throws Exception {
        SupportPackageTestData rspTestData = new SupportPackageTestData();
        rspTestData.setTargetPrefix(TARGET_PREFIX);
        rspTestData.setRolloutPrefix(ROLLOUT_PREFIX);
        rspTestData.setRolloutName(NEW_ROLLOUT);
        rspTestData.setFileType(MgmtSupportPackageFileType.PROXI_SIGNATURE);
        rspTestData.setDistributionSetName(DISTRIBUTION_SET_NAME);

        var rspTestDataMap = prepareTestDataForCreatingSupportPackageTest(rspTestData);
        Rollout rollout = (Rollout) rspTestDataMap.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest rspRequestBody = (MgmtBaseSupportPackageCreateRequest) rspTestDataMap.get("requestBody");
        invokeCreateSupportPackageApi(rollout.getId(), rspRequestBody);

        // Perform the request to retrieve all support packages for the rollout and verify the response
        rolloutHandler.handleAll();
        rolloutManagement.freeze(rollout.getId());
        List<JpaRsp> rspSupportPackages = rspRepository.findAll();
        rspSupportPackages.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
            rspRepository.save(rsp);
        });
        rolloutHandler.handleAll();
        List<JpaRsp> rspSupportPackages1 = rspRepository.findAll();
        rspSupportPackages1.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());
            rspRepository.save(rsp);
        });
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }
    @Test
    @Description("Verifying the repository entry for the rollout and group while unfreezing the rollout")
    void verifyRepositoryEntryForRolloutAndGroup() {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));

        rolloutManagement.freeze(rollout.getId());
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        rolloutHandler.handleAll();
        rolloutManagement.unfreeze(rollout.getId());
        Rollout savedRollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.DRAFT, savedRollout.getStatus());
        rolloutGroupManagement.findByRollout(PAGE, savedRollout.getId()).get().forEach(group -> {
            assertEquals(RolloutGroupStatus.DRAFT, group.getStatus());
        });
    }

    @Test
    @Description("Given a rollout and rollout group in ready state, when unfreezing the rollout, then it should be successful.")
    void givenRolloutAndGroupInReadyState_whenUnfreezingRollout_thenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.READY, rollout.getStatus());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }


    @Description("Create a rollout and add devices without software being associated; it should be in READY state")
    @Test
    void givenRolloutIsCreated_whenDevicesAdded_thenStatusShouldChangeToReady() throws Exception {
        var rolloutName = "newRollout";
        var fileUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(fileUrl);
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        changeArtifactStatus(artifactsRepository.findById(artifacts.getArtifactId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.SCHEDULED, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, "newTarget", "newTargets");

        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(Target::getControllerId).toList(), rollout.getId());
        invokeRolloutFreezeApi(rollout.getId());
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        rolloutHandler.handleAll();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        assertEquals(RolloutStatus.READY, rollout.getStatus());
    }

    @Step
    private void retrieveAndVerifyRolloutInStarting(final Rollout rollout) throws Exception {
        rolloutManagement.start(rollout.getId());


        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ROLLOUT))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(STARTING)));
    }
    @Step
    private void retrieveAndVerifyRolloutInRunning(final Rollout rollout) throws Exception {
        rolloutHandler.handleAll();
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ROLLOUT))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(RUNNING)));
    }

    @Step
    private void retrieveAndVerifyRolloutInDraftState(final Rollout rollout) throws Exception {
        ResultActions result=  mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(rollout.getName())))
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(rollout.getDescription())))
                .andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)))
                .andExpect(jsonPath(JSON_PATH_START_AT, equalTo(rollout.getStartAt().intValue())))
                .andExpect(jsonPath(JSON_PATH_END_AT, equalTo(rollout.getEndAt().intValue())))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, equalTo(rollout.getUserAcceptanceRequired().getName())))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, equalTo(rollout.getConnectivityType().getName())))
                .andExpect(jsonPath(JSON_PATH_LOG_COLLECTION_REQUIRED, equalTo(rollout.isLogCollectionRequired())))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo(rollout.getDowngradeAllowed().getValue())))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo(rollout.getRequiredMedia().getValue())))
                .andExpect(jsonPath("$.deploymentMetadata.requiredStateOfCharge", equalTo(Collections.emptyMap())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, equalTo(rollout.getMaxDownloadCellularDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, equalTo(rollout.getMaxDownloadDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, equalTo(rollout.getMaxDownloadWifiDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, equalTo(rollout.getDownloadRetryCount())))
                .andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, equalTo(rollout.getMaxUpdateTime())))
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(START_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(PAUSE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(RESUME_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))));
                if (Boolean.TRUE.equals(rollout.isLogCollectionRequired())) {
                    result.andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN, equalTo(rollout.getLogMaxFailureVin())))
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES, equalTo(rollout.getLogMaxNumberOfFiles())))
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE, equalTo(rollout.getLogMaxAllFileSize())))
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN, equalTo(rollout.getLogMaxSuccessVin())))
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE, equalTo(rollout.getLogMaxEachFileSize())));
                } else {
                    result.andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN).doesNotExist())
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES).doesNotExist())
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE).doesNotExist())
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN).doesNotExist())
                            .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE).doesNotExist());
                }
    }

}
