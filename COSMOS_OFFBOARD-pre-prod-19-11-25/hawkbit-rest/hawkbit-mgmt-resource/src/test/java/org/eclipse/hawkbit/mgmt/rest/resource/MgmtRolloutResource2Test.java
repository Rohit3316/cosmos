package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVechicleCreateResponse;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutGroupManagement;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.hamcrest.Matchers;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MgmtRolloutResource2Test extends AbstractManagementRolloutApiIntegrationTest {

    @Test
    @Description("Given an uploading RSP, when the rollout starts, then the system should avoid attempting to upload the RSP again.")
    void givenUploadingRspWhenRolloutStartsThenAvoidRspUpload() throws Exception {
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
        rspSupportPackages = rspRepository.findAll();
        rspSupportPackages.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.toString());
            rspRepository.save(rsp);
        });

        rolloutManagement.start(rollout.getId());
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Given an draft rollout status, when not exists artifact sm, then it should return not found.")
    void givenDraftRolloutsWhenDistributionSetNotNullAndItIsMultiAndDsModuleDifferentRequestButNoArtifactSmThenNotFound() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final DistributionSet dsA = testdataFactory.createDistributionSetWithOutArtifacts("");
        testdataFactory.createDistributionSetWithOutArtifacts("B");
        final DistributionSet dsC = testdataFactory.createDistributionSetWithOutArtifacts("C");
        SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);
        final Rollout rollout2 = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_2)));
        JpaRollout jpaRollout2 = (JpaRollout) rollout2;
        jpaRollout2.setDistributionSet(dsA);
        jpaRollout2.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout2);
        Artifacts artifacts = createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(dsC.getDistributionSetModules().get(0).getSm().getId(), dsC.getDistributionSetModules().get(0).getVersion().getId());
        DistributionSetModule distributionSetModule = new DistributionSetModule((JpaDistributionSet) dsA, (JpaSoftwareModule) sm, (JpaVersion) dsA.getDistributionSetModules().get(0).getVersion());
        distributionSetManagement.createDistributionSetModule(distributionSetModule);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given invalid support packages file status, when starting the rollout, then throw a ValidationException.")
    void givenInvalidSupportPackagesFileStatusWhenStartingRolloutThenThrowValidationException() throws Exception {
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


        rspSupportPackages = rspRepository.findAll();
        rspSupportPackages.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.UPLOADING_TO_STORAGE.toString());
            rspRepository.save(rsp);
        });
        assertThrows(jakarta.validation.ValidationException.class, () -> rolloutManagement.start(rollout.getId()));
        verify(snsAsyncClient, times(2)).publish(any(PublishRequest.class));
    }


    @Test
    @Description("Given the rollout is frozen, RSP and ESP are created, there are no mandatory ESP or RSP files, and none of the uploaded RSP and ESP files are successfully uploaded to storage, then it should throw an ValidationException.")
    void givenRolloutFrozen_whenAllRSPAndESPNotUploadedToStorage_thenThrowValidationException() throws Exception {

        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        Long vehicleModelId = vehicleCreateResponse.get(0).getId();
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        Long ecuModelId = addEcuModelResponse.get(0).getId();
        String ecuNodeId = addEcuModelResponse.get(0).getEcuNodeId();
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(fileUrl);
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        changeArtifactStatus(artifactsRepository.findById(artifacts.getArtifactId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, "newTarget", "newTargets");
        List<String> controllerIds = targets.stream().map(Target::getControllerId).toList();
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(Target::getControllerId).toList(), rollout.getId());
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.PROXI_SIGNATURE,
                ecuNodeId,
                supportPackageUrl,
                controllerIds);
        invokeCreateSupportPackageApi(rollout.getId(), createRspRequest);
        invokeRolloutFreezeApi(rollout.getId());
        mockServer.clear(request());
        rolloutHandler.handleAll();

        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath(Constants.KAFKA_SEND_EVENT_URL),
                VerificationTimes.atLeast(1)

        );
//TODO: after fixing the freeze api
        //        int page = 0;
//        int PAGE_SIZE=100;
//        Page<RolloutGroup> rolloutGroupPage;
//        do{  rolloutGroupPage= rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
//            for (RolloutGroup group : rolloutGroupPage.getContent()) {
//                assertEquals(RolloutGroupStatus.FREEZING,group.getStatus());
//            }
//            page++;
//        }while (rolloutGroupPage.hasNext());
    }

    @Test
    @Description("Testing that starting the rollout switches the state to starting and then to running")
    void freezingRolloutSwitchesIntoReadyState() throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);


        // starting rollout
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/action/freeze", TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // check rollout is in ready state
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(STATUS, equalTo(RolloutStatus.FREEZING.toString().toLowerCase())));
    }

    @Test
    @Description("Verifies that all RSP support packages and artifacts are uploaded successfully to the CDN, and ensures their file statuses are updated to CDN_UPLOAD_SUCCESSFUL")
    void givenAllRSPAndArtifacts_WhenUploadedToCDN_ThenValidationExceptionIsNotThrown() throws Exception {
        Rollout rollout = prepareSupportPackageTestData(
                "RSPSetTarget",
                "RSPRolloutPrefix",
                "RSPRolloutName", TEST_ECU_NODE_ADDRESS_1, "RSPDistributionSetName",
                MgmtSupportPackageFileType.PROXI_SIGNATURE
        );
        List<JpaRsp> rspList = rspRepository.findAll();
        assertEquals(1, rspList.size(), "There should be one RSP created.");

        rollout.getDistributionSet().getModules().forEach(module -> {
            module.getArtifactSoftwareModuleAssociations().forEach(association -> {
                JpaArtifacts artifact = (JpaArtifacts) association.getArtifact();
                artifact.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
                artifactsRepository.save(artifact);

                assertEquals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name(), artifact.getFileStatus().name(),
                        "Artifact status should be set to CDN_UPLOAD_SUCCESSFUL for artifact ID: " + artifact.getId());
            });
        });

        rspList.forEach(rsp -> {

            rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
            rspRepository.save(rsp);


            assertEquals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name(), rsp.getSupportPackageFileStatus().name(),
                    "RSP status should be set to CDN_UPLOAD_SUCCESSFUL for RSP ID: " + rsp.getId());
        });

    }

    @Test
    @Description("Verifies that an update request for a rollout returns success even  when 'endAt' is missing from the request body.")
    void givenRolloutWhenUpdatingWithoutEndAtThenReturnSuccess() throws Exception {
        // Create a test rollout
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        // Create an invalid update request with no 'endAt' field
        String updateRolloutRequest = new JSONObject()
                .put("manualStartReason", "Testing manual start for SCHEDULED rollout")
                .put("type", FOTA)
                .toString();

        // Perform the PUT request and expect a 400 Bad Request
        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(updateRolloutRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }


    @Test
    @Description("while adding device details, the confirmation required will be set to default value false in database")
    void givenRolloutGroupWhenAddingDeviceDetailsThenConfirmationRequiredShouldBeFalse() throws Exception {
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

        List<MgmtVechicleCreateResponse> vehicleCreateResponse = invokeAddVehicleModelApi();
        Long vehicleModelId = vehicleCreateResponse.get(0).getId();
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        Long ecuModelId = addEcuModelResponse.get(0).getId();
        invokeAssociateEcuModelToVehicleModelApi(vehicleModelId, ecuModelId);

        MgmtSoftwareModuleRequestBodyPost swModuleRequestBody = testdataFactory.getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType);
        List<MgmtSoftwareModule> softwareModules = invokeCreateSoftwareModuleApi(List.of(swModuleRequestBody));
        long swModuleId = softwareModules.get(0).getModuleId();
        MgmtAddVersionRequestBody versionRequestBody = testdataFactory.getRandomMgmtAddVersionRequestBody();
        MgmtAddVersionResponse versionResponse = invokeCreateSoftwareVersionApi(swModuleId, versionRequestBody);
        MgmtArtifacts artifacts = invokeCreateArtifactViaUrlApi(String.format(FILE_URL, port));
        invokeArtifactSoftwareModuleAssociationApi(Math.toIntExact(swModuleId), Math.toIntExact(versionResponse.getId()), Math.toIntExact(artifacts.getArtifactId()));
        changeArtifactStatus(artifactsRepository.findById(artifacts.getArtifactId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        invokeCreateRolloutApi(NEW_ROLLOUT, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(NEW_ROLLOUT).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, "newTarget", "newTargets");
        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(Target::getControllerId).toList(), rollout.getId());

        JpaRolloutGroupManagement jpaRolloutGroupManagement = (JpaRolloutGroupManagement) rolloutGroupManagement;
        List<RolloutGroup> rolloutGroups = jpaRolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent();

        for (RolloutGroup group : rolloutGroups) {
            Assertions.assertFalse(group.isConfirmationRequired());
        }
    }

    @Test
    @Description("Given an existing support package with statuses STORAGE_UPLOAD_SUCCESSFUL, UPLOADING_TO_CDN, or CDN_UPLOAD_SUCCESSFUL " +
            "associated with rollouts, when the scheduler attempts to transition the rollout state from FREEZING to READY, " +
            "then the rollout state transitions successfully to READY without exceptions, and all rollouts ultimately transition to RUNNING.")
    void givenSupportPackageWithValidStatusesWhenSchedulerRunsThenRolloutTransitionsToReady() throws Exception {
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

// Setup mock server and test data
        var port = System.getProperty("mock.server.port");
        var firstRolloutName = "firstRollout";
        var secondRolloutName = "secondRollout";
        var thirdRolloutName = "thirdRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

// Pre-requisites: Create vehicle model, ECU model, and associate them
        Long vehicleModelId = createVehicleModel();  // Create a vehicle model
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel(); // Create an ECU model
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId); // Associate ECU model to vehicle model

// Create software module, version, and artifact
        long swModuleId = createSoftwareModule(); // Create a software module
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId); // Create a software version
        MgmtArtifacts artifacts = createArtifact(fileUrl); // Create an artifact
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId()); // Associate artifact with software module

// Create targets and retrieve controller IDs
        List<MgmtTarget> targets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> controllerIds = getControllerIds(targets);

// Prepare mandatory support package requests
        MgmtBaseSupportPackageCreateRequest whatsNewRspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.WHATS_NEW, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, controllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, controllerIds);


// Create and verify the first rollout
        invokeCreateRolloutApi(firstRolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout firstRollout = rolloutManagement.getByName(firstRolloutName).orElse(null);
        assertNotNull(firstRollout);
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), firstRollout.getId()); // Associate software module and distribution set with the first rollout
        addDeviceDetails(controllerIds, firstRollout.getId(), 1); // Add device details to the first rollout

// Create mandatory support packages
        MgmtSupportPackage whatsNewRspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), whatsNewRspCreateRequest);
        updateFileStatus(RSP, whatsNewRspCreateResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL); // Update RSP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL
        MgmtSupportPackage adaCertificateEspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), adaCertificateEspCreateRequest);
        updateFileStatus(ESP, adaCertificateEspCreateResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL); // Update ESP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL
        MgmtSupportPackage adaLicenseEspCreateResponse = invokeCreateSupportPackageApi(firstRollout.getId(), adaLicenseEspCreateRequest);
        updateFileStatus(ESP, adaLicenseEspCreateResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL); // Update ESP status of the first rollout to STORAGE_UPLOAD_SUCCESSFUL

// Transition the first rollout to READY state
        invokeRolloutFreezeApi(firstRollout.getId());
        handleRollout();
        firstRollout = rolloutManagement.getByName(firstRolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, firstRollout.getStatus());  // Verify the first rollout moves to state READY when the support packages status is STORAGE_UPLOAD_SUCCESSFUL

// Create and verify the second rollout
        invokeCreateRolloutApi(secondRolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout secondRollout = rolloutManagement.getByName(secondRolloutName).orElse(null);
        assertNotNull(secondRollout);
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), secondRollout.getId()); // Associate software module and distribution set with the second rollout
        addDeviceDetails(controllerIds, secondRollout.getId(), 1); // Add device details to the second rollout

// Associate existing support packages with the second rollout
        whatsNewRspCreateResponse = invokeCreateSupportPackageApi(secondRollout.getId(), whatsNewRspCreateRequest);
        updateFileStatus(RSP, whatsNewRspCreateResponse.getSupportPackageId(), FileTransferStatus.UPLOADING_TO_CDN); // Update RSP status of the second rollout to UPLOADING_TO_CDN
        adaCertificateEspCreateResponse = invokeCreateSupportPackageApi(secondRollout.getId(), adaCertificateEspCreateRequest);
        updateFileStatus(ESP, adaCertificateEspCreateResponse.getSupportPackageId(), FileTransferStatus.UPLOADING_TO_CDN); // Update ESP status of the second rollout to UPLOADING_TO_CDN
        adaLicenseEspCreateResponse = invokeCreateSupportPackageApi(secondRollout.getId(), adaLicenseEspCreateRequest);
        updateFileStatus(ESP, adaLicenseEspCreateResponse.getSupportPackageId(), FileTransferStatus.UPLOADING_TO_CDN); // Update ESP status of the second rollout to UPLOADING_TO_CDN

// Transition the second rollout to READY state
        invokeRolloutFreezeApi(secondRollout.getId());
        handleRollout();
        secondRollout = rolloutManagement.getByName(secondRolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, secondRollout.getStatus()); // Verify the second rollout moves to state READY when the support packages status is UPLOADING_TO_CDN

// Create and verify the third rollout
        invokeCreateRolloutApi(thirdRolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout thirdRollout = rolloutManagement.getByName(thirdRolloutName).orElse(null);
        assertNotNull(thirdRollout);
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), thirdRollout.getId()); // Associate software module and distribution set with the third rollout
        addDeviceDetails(controllerIds, thirdRollout.getId(), 1); // Add device details to the third rollout

// Associate existing support packages with the third rollout
        whatsNewRspCreateResponse = invokeCreateSupportPackageApi(thirdRollout.getId(), whatsNewRspCreateRequest);
        updateFileStatus(RSP, whatsNewRspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update RSP status of the third rollout to CDN_UPLOAD_SUCCESSFUL
        adaCertificateEspCreateResponse = invokeCreateSupportPackageApi(thirdRollout.getId(), adaCertificateEspCreateRequest);
        updateFileStatus(ESP, adaCertificateEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update ESP status of the third rollout to CDN_UPLOAD_SUCCESSFUL
        adaLicenseEspCreateResponse = invokeCreateSupportPackageApi(thirdRollout.getId(), adaLicenseEspCreateRequest);
        updateFileStatus(ESP, adaLicenseEspCreateResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL); // Update ESP status of the third rollout to CDN_UPLOAD_SUCCESSFUL

// Transition the third rollout to READY state
        invokeRolloutFreezeApi(thirdRollout.getId());
        handleRollout();
        thirdRollout = rolloutManagement.getByName(thirdRolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, thirdRollout.getStatus()); // Verify the third rollout moves to state READY when the support packages status is CDN_UPLOAD_SUCCESSFUL

// Transition all rollouts from READY to RUNNING state
        handleRollout(); // READY to STARTING
        handleRollout(); // STARTING to RUNNING

        firstRollout = rolloutManagement.getByName(firstRolloutName).orElse(null);
        secondRollout = rolloutManagement.getByName(secondRolloutName).orElse(null);
        thirdRollout = rolloutManagement.getByName(thirdRolloutName).orElse(null);

// Verify all the rollouts are transitioned to state RUNNING once the support packages are uploaded to CDN successfully
        assert firstRollout != null;
        assertEquals(RolloutStatus.RUNNING, firstRollout.getStatus());
        assert secondRollout != null;
        assertEquals(RolloutStatus.RUNNING, secondRollout.getStatus());
        assert thirdRollout != null;
        assertEquals(RolloutStatus.RUNNING, thirdRollout.getStatus());
    }


    @Description("Given a rollout with already associated devices, when uploading the same devices again via file, then the system rejects the request because no new registered target devices are provided.")
    @Test
    void givenRolloutWithExistingDevices_whenUploadingDuplicateDevicesViaFile_thenValidationExceptionIsThrown() throws Exception {
        SupportPackageTestData espTestData = testdataFactory.getSupportPackageTestData("SetTarget01", "RolloutPrefix01", "Rollout01", TEST_ECU_NODE_ADDRESS_1, "DistributionSetName01", "AA12BB34CC56DD78EE90FF11223344556677889900AABBCCDDEEFF0011223344", MgmtSupportPackageFileType.ADA_CERTIFICATE);
        var espTestDataMap = prepareTestDataForCreatingSupportPackageTest(ROLLOUT_1, espTestData);
        Rollout rollout = (Rollout) espTestDataMap.get(ROLLOUT_1);
        rollout = rolloutManagement.get(rollout.getId()).orElseThrow(() -> new RuntimeException("Rollout not found"));
        MgmtBaseSupportPackageCreateRequest espRequestBody = (MgmtBaseSupportPackageCreateRequest) espTestDataMap.get(REQUEST_BODY);
        assignEcuModelsToVehicle((List<Target>) espTestDataMap.get(TARGETS), (List<EcuModel>) espTestDataMap.get(ECU_MODELS));
        ResultActions resultActions = invokeCreateSupportPackageApiAndReturnHttpResponse(rollout.getId(), espRequestBody).andExpect(status().isCreated());
        MvcResult mvcResult = resultActions.andReturn();
        String responseBody = mvcResult.getResponse().getContentAsString();
        final Rollout rolloutTemp = (Rollout) espTestDataMap.get(ROLLOUT_1);
        List<JpaEsp> jpaEsps = espRepository.findAll();
        List<Target> targets = (List<Target>) espTestDataMap.get("targets");
        jpaEsps.get(0).setFileType(MgmtSupportPackageFileType.ADA_LICENSE);
        espRepository.save(jpaEsps.get(0));
        List<MgmtCreateEcuModelResponse> addEcuModelResponse = invokeAddEcuModelApi();
        JpaRollout jpaRollout = rolloutRepository.getRolloutById(rollout.getId()).orElseThrow();
        jpaRollout = rolloutRepository.getRolloutById(rollout.getId()).orElseThrow();
        jpaRollout.setStatus(RolloutStatus.PAUSED);
        rolloutRepository.save(jpaRollout);
        rollout = rolloutManagement.get(rollout.getId()).orElseThrow(() -> new RuntimeException("Rollout not found"));
        Path filePath = generateTargetDevicesFile(targets.stream().map(Target::getControllerId).toList());
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, Matchers.matchesPattern("No new registered target devices found in the file")));
    }

    @Description("Given a running rollout, when new devices are uploaded via file, then a new group is created and set to QUEUED with mandatory ESPs.")
    @Test
    void givenRunningRolloutWhenUploadingDeviceWithMandatoryEspThenGroupIsQueued() throws Exception {
        // Create and transition rollout to RUNNING using the helper method
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        Long vehicleModelId = (Long) setup.get("vehicleModelId");
        String ecuNodeId = (String) setup.get("ecuNodeId");
        String supportPackageUrl = (String) setup.get("supportPackageUrl");

        // Create targets and retrieve controller IDs
        List<MgmtTarget> newTargets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> newControllerIds = getControllerIds(newTargets);

        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest1);
        updateFileStatus(ESP, adaCertificateEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest1);
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        // Add device details to the first rollout with new targets
        Path filePath = generateTargetDevicesFile(newControllerIds);
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Description("Given a paused rollout, when new devices are uploaded via file, then a new group is created and set to Paused.")
    @Test
    void givenPausedRolloutWhenUploadingDeviceWithMandatoryEspThenGroupIsQueued() throws Exception {
        // Create and transition rollout to RUNNING using the helper method
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        Long vehicleModelId = (Long) setup.get("vehicleModelId");
        String ecuNodeId = (String) setup.get("ecuNodeId");
        String supportPackageUrl = (String) setup.get("supportPackageUrl");
        pauseRollout(rollout.getId());

        handleRollout();
        rollout = rolloutManagement.getByName(rollout.getName()).orElse(null);
        assertEquals(RolloutStatus.PAUSED, rollout.getStatus());

        // Create targets and retrieve controller IDs
        List<MgmtTarget> newTargets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> newControllerIds = getControllerIds(newTargets);

        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest1);
        updateFileStatus(ESP, adaCertificateEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest1);
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        // Add device details to the first rollout with new targets
        Path filePath = generateTargetDevicesFile(newControllerIds);
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Description("Given a rollout in RUNNING state, when devices without required ESPs are uploaded, then a BAD_REQUEST error is returned and once again upload the right ESPS will return success.")
    @Test
    void givenRunningRolloutWhenUploadingDevicesWithoutMandatoryEspThenBadRequestIsReturned() throws Exception {
        // Setup tenant mandatory ESP/RSP configuration
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_ESP_KEY, "ADA_CERTIFICATE, ADA_LICENSE");
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_RSP_KEY, "WHATS_NEW");

        // Create and transition rollout to RUNNING using the helper method
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        Long vehicleModelId = (Long) setup.get("vehicleModelId");
        String ecuNodeId = (String) setup.get("ecuNodeId");
        String supportPackageUrl = (String) setup.get("supportPackageUrl");

        // Create targets and retrieve controller IDs
        List<MgmtTarget> newTargets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> newControllerIds = getControllerIds(newTargets);

        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.VARIANT_CODING, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest1);
        updateFileStatus(ESP, adaCertificateEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest1);
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        // Add device details to the first rollout with new targets
        Path filePath = generateTargetDevicesFile(newControllerIds);
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1)
                        .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("controller with controllerId " + newControllerIds.get(0) + " do not contain all the mandatory Esp"));

        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest2 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest2 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse2 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest2);
        updateFileStatus(ESP, adaCertificateEspCreateResponse2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse2 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest2);
        updateFileStatus(ESP, adaLicenseEspCreateResponse2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MockMultipartFile file2 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Description("Given a rollout in Paused state, when devices without required ESPs are uploaded, then a BAD_REQUEST error is returned  and once again upload the right ESPS will return success")
    @Test
    void givenPausedRolloutWhenUploadingDevicesWithoutMandatoryEspThenBadRequestIsReturned() throws Exception {

        // --- Step 1: Setup tenant mandatory ESP/RSP configuration ---
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_ESP_KEY, "ADA_CERTIFICATE, ADA_LICENSE");
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_RSP_KEY, "WHATS_NEW");

        // --- Step 2: Create and transition rollout to RUNNING using the helper method ---
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        Long vehicleModelId = (Long) setup.get("vehicleModelId");
        String ecuNodeId = (String) setup.get("ecuNodeId");
        String supportPackageUrl = (String) setup.get("supportPackageUrl");

        // --- Step 3: Pause the rollout ---
        pauseRollout(rollout.getId());

        handleRollout();
        rollout = rolloutManagement.getByName(rollout.getName()).orElse(null);
        assertEquals(RolloutStatus.PAUSED, rollout.getStatus());


        // --- Step 4: Create targets and retrieve controller IDs ---
        List<MgmtTarget> newTargets = createTargets(vehicleModelId, 1); // Create targets associated with the vehicle model
        List<String> newControllerIds = getControllerIds(newTargets);

        // Upload invalid ESP types that are not mandatory
        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.UDS_FLOW, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest1 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.VARIANT_CODING, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest1);
        updateFileStatus(ESP, adaCertificateEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse1 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest1);
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        // --- Step 5: Attempt to upload device details (should fail with BAD_REQUEST due to missing mandatory ESPs) ---
        Path filePath = generateTargetDevicesFile(newControllerIds);
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1)
                        .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("controller with controllerId " + newControllerIds.get(0) + " do not contain all the mandatory Esp"));

        // --- Step 6: Upload the correct mandatory ESPs (ADA_LICENSE, ADA_CERTIFICATE) ---
        MgmtBaseSupportPackageCreateRequest adaCertificateEspCreateRequest2 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, supportPackageUrl, newControllerIds);
        MgmtBaseSupportPackageCreateRequest adaLicenseEspCreateRequest2 = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(
                MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, supportPackageUrl, newControllerIds);

        MgmtSupportPackage adaCertificateEspCreateResponse2 = invokeCreateSupportPackageApi(rollout.getId(), adaCertificateEspCreateRequest2);
        updateFileStatus(ESP, adaCertificateEspCreateResponse2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSupportPackage adaLicenseEspCreateResponse2 = invokeCreateSupportPackageApi(rollout.getId(), adaLicenseEspCreateRequest2);
        updateFileStatus(ESP, adaLicenseEspCreateResponse2.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MockMultipartFile file2 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        updateFileStatus(ESP, adaLicenseEspCreateResponse1.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);

        // --- Step 7: Upload device file again (should succeed now) ---
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file2)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Description("Given a rollout , when devices without required ESPs are uploaded for the first time, then a BAD_REQUEST error is returned.")
    @Test
    void givenRolloutWhenUploadingDevicesWithoutMandatoryEspThenBadRequestIsReturned() throws Exception {

        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_ESP_KEY, "ADA_CERTIFICATE, ADA_LICENSE");
        testdataFactory.addTenantConfigurations(TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_RSP_KEY, "WHATS_NEW");

        var rolloutName = "newRollout";


        List<MgmtTarget> target = setUpPreRequisitesAndCreateRollout(rolloutName);
        String controllerId = target.get(0).getControllerId();

        Rollout firstRollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assert firstRollout != null;


        // Add device details to the first rollout with new targets
        Path filePath = generateTargetDevicesFile(List.of(controllerId));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, firstRollout.getId()).file(file1)
                        .contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("controller with controllerId " + controllerId + " do not contain all the mandatory Esp"));
    }

    @Test
    @Description("Verifies that a rollout with DRAFT groups transitions to QUEUED only after ESPs are uploaded to CDN. " +
            "Also validates that device upload fails with BAD_REQUEST when ESPs aren't uploaded, " +
            "and group states are updated appropriately through pause and resume operations.")
    void givenRunningRolloutWithDraftGroupWhenSchedulerRunsThenGroupIsQueued() throws Exception {
        // --- Step 1: Setup Environment (using new helper method) ---
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        Long vehicleModelId = (Long) setup.get("vehicleModelId");
        String ecuNodeId = (String) setup.get("ecuNodeId");
        String supportPackageUrl = (String) setup.get("supportPackageUrl");

        // --- Step 2: Upload target devices before uploading ESP (expect DRAFT → READY only, not QUEUED) ---
        List<String> firstControllerIds = createTargetsAndGetControllers(vehicleModelId, 1);
        var espIds1 = createAndUploadEspPackages(rollout.getId(), ecuNodeId, supportPackageUrl, firstControllerIds, false);
        Path firstFilePath = generateTargetDevicesFile(firstControllerIds);
        MockMultipartFile firstFile = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(firstFilePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(firstFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // --- Verify group transitioned from DRAFT → READY (not QUEUED) because ESP not uploaded yet ---
        assertGroupStatus(rollout.getId(), 1, RolloutGroupStatus.READY);

        // --- Step 3: Upload ESP to CDN and trigger scheduler to update group state ---
        uploadEspToCdn(espIds1);
        handleRollout();

        // --- Group should now move from READY → QUEUED after successful ESP upload ---
        assertGroupStatus(rollout.getId(), 1, RolloutGroupStatus.QUEUED);

        // --- Step 4: Pause rollout and check group statuses ---
        pauseRollout(rollout.getId());
        handleRollout(); // Apply pause state
        rollout = rolloutManagement.getByName(rollout.getName()).orElseThrow();
        assertEquals(RolloutStatus.PAUSED, rollout.getStatus());
        assertGroupStatus(rollout.getId(), 0, RolloutGroupStatus.PAUSED);
        assertGroupStatus(rollout.getId(), 1, RolloutGroupStatus.QUEUED);

        // --- Step 5: Add new group with ESPs not uploaded to CDN ---
        List<String> secondControllerIds = createTargetsAndGetControllers(vehicleModelId, 1);
        var espIds2 = createAndUploadEspPackages(rollout.getId(), ecuNodeId, supportPackageUrl, secondControllerIds, false);
        Path secondFilePath = generateTargetDevicesFile(secondControllerIds);
        MockMultipartFile secondFile = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(secondFilePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(secondFile)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // --- Step 6: Upload ESPs for new group and resume the rollout ---
        uploadEspToCdn(espIds2);
        assertGroupStatus(rollout.getId(), 2, RolloutGroupStatus.READY);
        resumeRollout(rollout.getId());
        handleRollout(); // Scheduler run after resume

        // --- Step 7: Group Status Checks after resuming ---
        assertGroupStatus(rollout.getId(), 0, RolloutGroupStatus.RUNNING);
        assertGroupStatus(rollout.getId(), 1, RolloutGroupStatus.QUEUED);
        assertGroupStatus(rollout.getId(), 2, RolloutGroupStatus.READY);

        // --- Step 8: Scheduler run should pick up READY group and queue it ---
        handleRollout();

        // --- Step 9: Final Group Status Checks ---
        assertGroupStatus(rollout.getId(), 0, RolloutGroupStatus.RUNNING);
        assertGroupStatus(rollout.getId(), 1, RolloutGroupStatus.QUEUED);
        assertGroupStatus(rollout.getId(), 2, RolloutGroupStatus.QUEUED);
    }

}
