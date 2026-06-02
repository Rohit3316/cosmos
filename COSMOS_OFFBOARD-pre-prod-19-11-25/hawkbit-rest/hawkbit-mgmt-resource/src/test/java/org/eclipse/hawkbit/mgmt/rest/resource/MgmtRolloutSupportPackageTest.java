package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.qameta.allure.Description;
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
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.sns.models.CdnUploadRequest;
import org.eclipse.hawkbit.repository.jpa.model.AbstractBaseSupportPackage;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import jakarta.validation.ValidationException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MgmtRolloutSupportPackageTest extends AbstractManagementRolloutApiIntegrationTest {

    private static Stream<Arguments> getAllSupportPackagesPaginationArguments() {
        return Stream.of(
                // Offset, Limit, Size of the page, Total, sortParam, rsqlParam
                Arguments.of(0, 5, 10, 15),
                Arguments.of(5, 5, 5, 15),
                Arguments.of(0, 10, 15, 15),
                Arguments.of(5, 10, 5, 15),
                Arguments.of(0, 15, 15, 15),
                Arguments.of(5, 15, 5, 15),
                Arguments.of(10, 5, null, null),
                Arguments.of(10, 10, null, null),
                Arguments.of(10, 15, null, null),
                Arguments.of(0, 0, 15, 15),
                Arguments.of(5, null, 5, 15),
                Arguments.of(null, 100, 15, 15),
                Arguments.of("invalid", 5, null, null),
                Arguments.of(5, "invalid", null, null),
                Arguments.of(-1, 5, 10, 15),
                Arguments.of(5, -1, 5, 15)
        );
    }


    @Value("${hawkbit.artifact.url.cdn.host}")
    private String cdnHost;
    @Value("${hawkbit.artifact.url.cdn.rootDirectory}")
    private String cdnRootDirectory;

    @Test
    @Description("When delete device API is called with valid device details, the device details should be removed from the rollout successfully.")
    void givenValidDeviceDetails_whenDeleteDeviceApiCalled_thenDeviceDetailsAreRemovedSuccessfully() throws Exception {
        // Given: Prepare groups and their target devices
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3));
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        // When: Perform the DELETE request to remove device details
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Then: Verify the changes post-deletion
        List<JpaRolloutGroup> rolloutGroupsAfterDeletion = rolloutGroupRepository.findByRolloutId(rollout.getId());
        assertEquals(0, rolloutGroupsAfterDeletion.size(), "After deletion, no rollout groups should remain associated.");

        long targetCountAfterDeletion = targetManagement.count();
        assertEquals(3, targetCountAfterDeletion, "The total number of targets should remain unchanged.");

        assertEquals(0, rolloutTargetGroupRepository.count(), "No targets should remain associated with any groups post-deletion.");

        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).get();
        assertEquals(0, jpaRollout.getTotalTargets(), "Rollout should have 0 total targets after deletion.");
        assertEquals(0, jpaRollout.getRolloutGroupsCreated(), "Rollout should have 0 rollout groups created after deletion.");
        assertEquals(0, jpaRollout.getRolloutGroups().size(), "Rollout should have no groups associated post-deletion.");
    }

    @Test
    @Description("Validates that all ESPs are successfully uploaded to storage and ensures the rollout starts.")
    void givenAllESPsUploadedToStorage_WhenRolloutStarts_ThenNoValidationExceptionIsThrown() throws Exception {
        Rollout rollout = prepareSupportPackageTestData(
                "ESPSetTarget",
                "ESPRolloutPrefix",
                "ESPRolloutName", TEST_ECU_NODE_ADDRESS_1, "ESPDistributionSetName",
                MgmtSupportPackageFileType.LICENSE
        );

        List<JpaEsp> espList = espRepository.findAll();
        espList.forEach(esp -> {

            esp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name());
            espRepository.save(esp);

            assertEquals(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.name(), esp.getSupportPackageFileStatus().name(),
                    "ESP status should be set to STORAGE_UPLOAD_SUCCESSFUL for ESP ID: " + esp.getId());

            rolloutManagement.freeze(rollout.getId());
            rolloutHandler.handleAll();
            rolloutManagement.start(rollout.getId());
            rolloutHandler.handleAll();
            verify(snsAsyncClient, times(3)).publish(any(PublishRequest.class));

        });
    }

    @Test
    @Description("Validate various scenarios for deleting device details including invalid tenant, invalid rollout ID, empty or null target devices, and invalid rollout status.")
    void givenVariousValidationScenarios_whenRemovingDeviceDetails_thenAppropriateResponsesReturned() throws Exception {
        // Given: Prepare groups and their target devices
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        Path validFilePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1));
        MockMultipartFile validFile = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(validFilePath));

        // When & Then: Invalid tenant
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1000000L, rollout.getId())
                        .file(validFile)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("TenantMetaData with given identifier {1000000} does not exist."));

        // When & Then: Invalid rollout ID
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, 10000L)
                        .file(validFile)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Rollout with given identifier {10000} does not exist."));

        // When & Then: Null target devices
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(TARGET_DEVICES, null)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The target devices file must be provided and cannot be empty."));

        // When & Then: Empty target devices
        Path emptyFilePath = generateTargetDevicesFile(List.of("", "", ""));
        MockMultipartFile emptyFile = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(emptyFilePath));
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(emptyFile)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No target devices found in the uploaded file."));

        // When & Then: Completely empty file
        emptyFilePath = generateTargetDevicesFile(Collections.emptyList());
        emptyFile = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(emptyFilePath));
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(emptyFile)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("The target devices file must be provided and cannot be empty."));

        // Given: Freeze the rollout
        rolloutManagement.freeze(rollout.getId());

        // When & Then: Rollout should be in draft status while removing device details
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(validFile)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(String.format("The requested operation can be performed only when the Rollout is in status %s but the current state of the rollout is %s", RolloutStatus.DRAFT, RolloutStatus.FREEZING)));
    }

    @Test
    @Description("Given a rollout with support packages, when SNS publish fails during rollout start, then RSP is not uploaded to CDN.")
    void givenRolloutWithSupportPackagesWhenSnsPublishFailsThenRSPNotUploadedToCdn() throws Exception {
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

        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.failedFuture(new RuntimeException("Sns publish failed"));
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);

        List<JpaRsp> rspSupportPackages = rspRepository.findAll();
        rspSupportPackages.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
            rspRepository.save(rsp);
        });
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        for (Rsp rspSupportPackage : rspRepository.findAll()) {
            assertNotEquals(FileTransferStatus.UPLOADING_TO_CDN, rspSupportPackage.getSupportPackageFileStatus());
            assertEquals(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL, rspSupportPackage.getSupportPackageFileStatus());
        }
        verify(snsAsyncClient, times(3)).publish(any(PublishRequest.class));
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Testing that rollout paged list is limited by the query param limit")
    void retrieveRolloutGroupsForSpecificRollout(final boolean confirmationFlowEnabled, final boolean confirmationRequired) throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        if (confirmationFlowEnabled) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());

        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        // retrieve rollout groups from created rollout

        mvc.perform(
                        get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_GROUPS_URL, TENANT_ID, rollout.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                //  We need to develop multiple rollout groups for a rollout
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_0_STATUS, equalTo(READY)));
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, false), Arguments.of(true, true), Arguments.of(false, true));
    }

    @Test
    @Description("Given a rollout with groups and targets, when attempting to remove device details with no matching target IDs, then a ValidationException should be thrown.")
    void givenRolloutWithGroups_whenRemovingDeviceDetailsWithNoMatchingTargetIds_thenThrowValidationException() throws JsonProcessingException {

        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);
        var controllerIdList = List.of(CONTROLLER_ID);
        // When: Attempting to remove device details with no matching target IDs
        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> rolloutManagement.deleteDeviceDetails(rollout, controllerIdList, true)
        );

        // Then: A ValidationException is thrown with an appropriate error message
        assertNotNull(exception, "ValidationException should be thrown.");
        assertEquals("Targets are not associated with the provided rollout", exception.getMessage(),
                "Error message should match the expected validation message.");
    }

    @Test
    @Description("Given a rollout with valid groups and targets, when a file with no registered controller IDs is uploaded, then a bad request response should be returned.")
    void givenFileWithNoRegisteredControllerIds_whenRemovingDeviceDetails_thenBadRequest() throws Exception {
        // Given: A rollout with associated groups and targets
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        // Generate the target devices file containing no valid registered controller IDs
        Path filePath = generateTargetDevicesFile(List.of("randomDevice"));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        // When: A DELETE request is made with a file containing invalid target devices
        ResultActions resultActions = mvc.perform(
                MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file1)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON)
        );

        // Then: A bad request response should be returned
        resultActions
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        // Optionally, assert the error message if part of the response body
        MvcResult mvcResult = resultActions.andReturn();
        String responseContent = mvcResult.getResponse().getContentAsString();
        assertTrue(responseContent.contains("No registered target devices found in the file."),
                "The response should indicate that no registered controller IDs were found.");
    }

    @Test
    @Description("When the delete device API is called with valid device details with duplicates, only the specified device should be removed from the rollout.")
    void givenValidDeviceDetails_whenDeleteDeviceApiCalledWithDuplicates_thenOnlyOneDeviceIsRemoved() throws Exception {

        // Prepare groups and their target devices
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        // Generate the target devices file and mock a multipart request
        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID1, CONTROLLER_ID1));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        // Perform the DELETE request to remove device details
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file1)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Verify the number of targets after the deletion
        long targetCountAfterDeletion = targetManagement.count();
        assertEquals(3, targetCountAfterDeletion, "The number of targets should remain the same if no targets were deleted.");

        assertEquals(2, rolloutTargetGroupRepository.count(), "Target should no longer be associated with any group after deletion.");
        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).get();
        assertEquals(2, jpaRollout.getTotalTargets());

    }

    @Test
    @Description("Given a rollout is frozen, When there are no mandatory ESP or RSP uploaded, Then the state of the rollout and rollout group transitions to READY.")
    void givenFrozenRollout_whenNoMandatoryEspRspAndNoESPAndRSPUploaded_thenTransitionToReady() throws Exception {
        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var rolloutEndDate = Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond();

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

        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(Target::getControllerId).toList(), rollout.getId());
        invokeRolloutFreezeApi(rollout.getId());
        rolloutHandler.handleAll();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, rollout.getStatus());
        int page = 0;
        int PAGE_SIZE = 100;
        Page<RolloutGroup> rolloutGroupPage;
        do {
            rolloutGroupPage = rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
            for (RolloutGroup group : rolloutGroupPage.getContent()) {
                assertEquals(RolloutGroupStatus.READY, group.getStatus());
            }
            page++;
        } while (rolloutGroupPage.hasNext());
    }

    @Test
    @Description("Ensures that a ValidationException is thrown when attempting to start a rollout" +
            " without all RSP support packages and artifacts being successfully uploaded to the CDN.")
    void givenIncompleteRSP_WhenRolloutStarts_ThenValidationExceptionIsThrown() throws Exception {
        Rollout rollout = prepareSupportPackageTestData(
                "RSPSetTarget",
                "RSPRolloutPrefix",
                "RSPRolloutName", TEST_ECU_NODE_ADDRESS_1, "RSPDistributionSetName",
                MgmtSupportPackageFileType.PROXI_SIGNATURE
        );
        long rolloutId = rollout.getId();

        rolloutManagement.freeze(rollout.getId());
        List<JpaRsp> rspList = rspRepository.findAll();
        rspList.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
            rspRepository.save(rsp);
        });
        rollout = rolloutManagement.get(rolloutId).orElse(null);
        assertNotNull(rollout);
        rollout.getDistributionSet().getModules().forEach(module -> {
            module.getArtifactSoftwareModuleAssociations().forEach(association -> {
                JpaArtifacts artifact = (JpaArtifacts) association.getArtifact();
                artifact.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
                artifactsRepository.save(artifact);
            });
        });
        rolloutHandler.handleAll();
        rspList = rspRepository.findAll();
        rspList.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.name());
            rspRepository.save(rsp);
        });
        rollout = rolloutManagement.get(rolloutId).orElse(null);
        assertNotNull(rollout);
        rollout.getDistributionSet().getModules().forEach(module -> {
            module.getArtifactSoftwareModuleAssociations().forEach(association -> {
                JpaArtifacts artifact = (JpaArtifacts) association.getArtifact();
                artifact.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.name());
                artifactsRepository.save(artifact);
            });
        });

        rolloutManagement.start(rolloutId);
        rolloutHandler.handleAll();
        assertEquals(RolloutStatus.STARTING, rolloutManagement.get(rolloutId).get().getStatus());
        assertEquals(RolloutGroupStatus.READY, rolloutGroupManagement.findByRollout(PageRequest.of(0, 10), rolloutId).get().findFirst().get().getStatus());

    }

    @Test
    @Description("Testing that rolloutgroup paged list with rsql parameter")
    void retrieveRolloutGroupsForSpecificRolloutWithRSQLParam() throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());

        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        // retrieve rollout groups from created rollout

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_GROUPS_URL, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout1" + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_0_NAME, equalTo("rollout1" + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_0_TOTAL_TARGETS_PER_STATUS).doesNotExist());

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_GROUPS_URL, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout1*"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)));
    }

    @Test
    @Description("Given a rollout with support packages, when starting the rollout with SNS publish success, then RSP is uploading to CDN.")
    void givenRolloutWithSupportPackagesWhenStartingRolloutThenRSPUploadingToCdn() throws Exception {
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
        rolloutManagement.freeze(rollout.getId());

        // Mocking the STORAGE_UPLOADED status for RSP support packages
        List<JpaRsp> rspSupportPackages = rspRepository.findAll();
        rspSupportPackages.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
            rspRepository.save(rsp);
        });
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        for (Rsp rspSupportPackage : rspRepository.findAll()) {
            assertEquals(FileTransferStatus.UPLOADING_TO_CDN, rspSupportPackage.getSupportPackageFileStatus());
        }
        verify(snsAsyncClient, times(3)).publish(any(PublishRequest.class));
    }

    @Test
    @Description("Test to ensure that deleting a rollout in RUNNING state fails.")
    void deleteRolloutWhenRunningStateShouldFail() throws Exception {
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, "rolloutDeleteRunning", "rolloutDeleteRunning");
        final DistributionSet dsA = testdataFactory.createDistributionSet();

        // Create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, dsA, targets);

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(STARTING)));

        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultHandlers.print()).andExpect(status().isBadRequest()); // Expecting 400 Bad Request

    }

    @Test
    @Description("Given a valid support package for a rollout, " +
            "when it is fetched by its ID, " +
            "then the response should include the appropriate download links.")
    void givenSupportPackageIdWhenFetchingSupportPackageThenDownloadLinksArePresentInResponse() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        List<AbstractBaseSupportPackage> supportPackages = Stream.concat(
                rspRepository.findAll().stream(),
                espRepository.findAll().stream()
        ).toList();
        for (AbstractBaseSupportPackage supportPackage : supportPackages) {
            String expectedCdnDirectory = artifactUrlHandlerProperties.getCdn().getDirectory()
                    .replace("{tenant}", supportPackage.getTenant().toLowerCase())
                    .replace("{type}", supportPackage.getFileType().getCategory())
                    .replace("{SHA256}", supportPackage.getSha256Hash().replaceAll("(.{2})", "$1/"));

            String expectedDownloadUrl = artifactUrlHandlerProperties.getProtocols().get("download-cdn-http").getRef()
                    .replace("{hawkbit.artifact.url.cdn.host}", cdnHost)
                    .replace("{hawkbit.artifact.url.cdn.rootDirectory}", cdnRootDirectory)
                    .replace("{artifactFileName}", supportPackage.getFileName())
                    .replace("{hawkbit.artifact.url.cdn.directory}", expectedCdnDirectory);

            String expectedHttpUrl = "https://mgmt-api.host.com/management/v1/rollouts/" + rollout.getId() +
                    "/support-packages/" + supportPackage.getId() + "/download";

            invokeGetSupportPackageByIdApi(rollout.getId(), supportPackage.getId(), supportPackage.getFileType().getCategory())
                    .andExpect(jsonPath("$.supportPackageId").value(supportPackage.getId()))
                    .andExpect(jsonPath("$._links.download.href").value(expectedDownloadUrl))
                    .andExpect(jsonPath("$._links.download-http.href").value(expectedHttpUrl));
        }
    }

    @Test
    @Description("Given a rollout is created with associated support packages and dependencies. Pagination parameters such as offset and limit " +
            "are applied to the API request, when the API is called to retrieve support packages for the rollout using the " +
            "provided pagination parameters, then the API returns the correct paginated results based on the offset and limit, " +
            "ensuring the response matches the expected size and total count.")
    void givenRolloutWithSupportPackagesWhenRetrievingSupportPackagesBasedOnRolloutThenPaginatedResultsAreReturned() throws Exception {

        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

// Setup mock server and test data
        var port = System.getProperty("mock.server.port");
        var rolloutName = "rollout";
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

// Create and verify the rollout
        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId()); // Associate software module and distribution set with the rollout
        addDeviceDetails(controllerIds, rollout.getId(), 1); // Add device details to the rollout

// Validate absence of support packages linked to the rollout
        invokeGetAllSupportPackagesApi(rollout.getId(), 0, 100, null, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message",
                        Matchers.equalTo("Support Packages not found for the rollout ID: " + rollout.getId())));

// Create all the available support packages to reproduce pagination
        for (MgmtSupportPackageFileType supportPackageType : MgmtSupportPackageFileType.values()) {
            String ecuNodeIdOrEmpty = supportPackageType.getCategory().equals(RSP) ? "" : ecuNodeId;
            List<String> controllerIdsOrEmpty = supportPackageType.getCategory().equals(RSP) ? List.of() : controllerIds;
            MgmtSupportPackage supportPackage = createSupportPackage(rollout.getId(), supportPackageUrl, supportPackageType, ecuNodeIdOrEmpty, controllerIdsOrEmpty);
            updateFileStatus(supportPackageType.getCategory(), supportPackage.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);
        }

// Transition the first rollout to READY state
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertEquals(RolloutStatus.READY, rollout.getStatus());
        mockServer.clear(HttpRequest.request());

        final Rollout savedRollout = rollout;
        getAllSupportPackagesPaginationArguments().forEach(arguments -> {
            try {
                Object offset = arguments.get()[0];
                Object limit = arguments.get()[1];
                Object expectedSize = arguments.get()[2];
                Object total = arguments.get()[3];
                Object sortParam = "id:ASC";

                if ((offset != null && offset.equals("invalid")) || (limit != null && limit.equals("invalid"))) {
                    invokeGetAllSupportPackagesApi(savedRollout.getId(), offset, limit, sortParam, null)
                            .andExpect(status().isBadRequest());
                } else if (expectedSize == null || total == null) {
// When no support packages are found for the given rollout ID based on the pagination parameters
                    invokeGetAllSupportPackagesApi(savedRollout.getId(), offset, limit, sortParam, null)
                            .andExpect(status().isNotFound())
                            .andExpect(jsonPath("$.message", Matchers.equalTo("Support Packages not found for the rollout ID: " + savedRollout.getId())));
                } else {
// When support packages are found for the given rollout ID based on the pagination parameters
                    invokeGetAllSupportPackagesApi(savedRollout.getId(), offset, limit, sortParam, null)
                            .andExpect(jsonPath("$.content", hasSize((Integer) expectedSize)))
                            .andExpect(jsonPath("$.size", Matchers.equalTo(expectedSize)))
                            .andExpect(jsonPath("$.total", Matchers.equalTo(total)));
                }
            } catch (Exception e) {
                throw new RuntimeException("Error during pagination test for offset: " + arguments.get()[0] + ", limit: " + arguments.get()[1], e);
            }
        });

    }
}
