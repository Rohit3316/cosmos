package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@Slf4j
 class MgmtRetrieveRolloutTargetsAndGroups2Test extends AbstractManagementRolloutApiIntegrationTest{
    @Test
    @Description("The relation between deploy group and rollout should be validated.")
    void deployGroupsShouldValidateRelationWithRollout() throws Exception {
        // setup
        final int amountTargets = 8;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout1 = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);

        final Rollout rollout2 = createRolloutWithDependencies(ROLLOUT_2, testdataFactory.createDistributionSet(), targets);


        rolloutManagement.freeze(rollout1.getId());
        rolloutManagement.freeze(rollout2.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout1.getId());
        rolloutManagement.start(rollout2.getId());
        rolloutHandler.handleAll();

        final RolloutGroup firstGroup = rolloutGroupManagement.findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout1.getId()).getContent().get(0);

        // make request for firstGroupId and the rolloutId of the second rollout (the one with no groups)
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + CREATING_RIOLLOUT_GROUPS_URL, TENANT_ID, rollout2.getId(), firstGroup.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }
    @Test
    @Description("Testing that the list of rollout groups can be requested with representation mode 'full'.")
    void retrieveRolloutGroupsFullRepresentation() throws Exception {

        List<Target> targets = testdataFactory.createTargets(20, ROLLOUT, ROLLOUT);

        // create a running rollout for the created targets
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        rolloutHandler.handleAll();
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // retrieve the rollout groups of the created rollout
        // filter for the first group by RSQL
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_GROUPS_URL, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "name==rollout" + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX)
                        .param("representation", "full")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_0_NAME, equalTo("rollout" + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX)))
                .andExpect(jsonPath(CONTENT_0_TOTAL_TARGETS_PER_STATUS).exists())
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.running", equalTo(20)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.notstarted", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.scheduled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.canceled", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.finished", equalTo(0)))
                .andExpect(jsonPath("content[0].totalTargetsPerStatus.error", equalTo(0)));
    }

    @Test
    @Description("Test device details deletion while handling ESP unlinking or removal based on usage")
    void givenDeviceDetailsWhenDeleteDeviceApiCalledThenEspIsUnlinkedOrDeleted() throws Exception {
        // Ensure a clean test state (avoid duplicate entries)
        espRepository.deleteAll();
        rolloutRepository.deleteAll();
        rolloutGroupRepository.deleteAll();
        targetTagRepository.deleteAll();

        // Create ESP package and assign it to the rollout
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix("SetTarget01");
        customTestData.setRolloutPrefix("RolloutPrefixSupportkgESP01");
        customTestData.setRolloutName("ESProllout04");
        customTestData.setEcuNodeAddress(TEST_ECU_NODE_ADDRESS_1);
        customTestData.setDistributionSetName("ESPDistributionSetName02");

        var testData = prepareTestDataForCreatingESPTest(customTestData);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);

        // Ensure ESP package does not already exist before creation
        if (espRepository.findAll().isEmpty()) {
            invokeCreateSupportPackageApi(rollout.getId(), requestBody);
        }

        assertEquals(1, espRepository.findAll().size(), "ESP package should be created.");
        DistributionSet ds = testdataFactory.createDistributionSet();

        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();
        rollout = rolloutRepository.findById(rollout.getId()).orElseThrow(() -> new RuntimeException("Rollout not found"));
        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);
        associateArtifactWithSoftwareModule(softwareModule, version);
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3));
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        // When: Perform DELETE request to remove device details with deleteEsp = true
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        List<JpaEsp> remainingEspPackages = espRepository.findAll();
        if (remainingEspPackages.isEmpty()) {
            log.debug("ESP package was deleted successfully.");
        } else {
            log.debug("ESP package was unlinked but not deleted because it is used elsewhere.");
        }

        List<JpaRolloutGroup> rolloutGroupsAfterDeletion = rolloutGroupRepository.findByRolloutId(rollout.getId());
        assertEquals(0, rolloutGroupsAfterDeletion.size(), "After deletion, no rollout groups should remain.");
        assertEquals(0, rolloutTargetGroupRepository.count(), "No targets should remain associated with any groups.");
        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).orElse(null);
        assert jpaRollout != null;
        assertEquals(0, jpaRollout.getTotalTargets(), "Rollout should have 0 total targets after deletion.");
        assertEquals(0, jpaRollout.getRolloutGroupsCreated(), "Rollout should have 0 rollout groups after deletion.");
        assertEquals(0, jpaRollout.getRolloutGroups().size(), "Rollout should have no associated groups.");
    }
    @Test
    @Description("When delete device API is called with valid device details associated with multiple rollouts, then the device details should be removed from the correct rollout.")
    void givenValidDeviceDetailsAssociatedWithMultipleRollouts_whenDeleteDeviceApiCalled_thenDevicesAreRemoved() throws Exception {
        // Given: Prepare groups and their target devices
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        // Given: A target associated with another rollout
        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";
        final Rollout dummyRollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT_1,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        // Verify that the target is associated with different rollouts
        assertEquals(4, rolloutGroupRepository.count(), "Total rollout groups should be 4.");
        assertEquals(6, rolloutTargetGroupRepository.count(), "Targets should be associated with multiple rollouts.");

        // When: Perform the DELETE request to remove device details for the first rollout
        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3));
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Then: Verify no rollout groups are associated with the first rollout after deletion
        List<JpaRolloutGroup> rolloutGroupsAfterDeletion = rolloutGroupRepository.findByRolloutId(rollout.getId());
        assertEquals(0, rolloutGroupsAfterDeletion.size(), "After deletion, no rollout groups should be associated with the first rollout.");

        // Verify the target association with the dummy rollout
        List<JpaRolloutGroup> rolloutGroupsAfterDeletionForDummyRollout = rolloutGroupRepository.findByRolloutId(dummyRollout.getId());
        assertNotNull(rolloutGroupsAfterDeletionForDummyRollout, "Rollout groups should not be null.");
        assertEquals(2, rolloutGroupsAfterDeletionForDummyRollout.size(), "The number of groups should match the expected count.");
        assertEquals(2, rolloutGroupsAfterDeletionForDummyRollout.get(0).getRolloutTargetGroup().size(), "Group 1 should have the expected number of targets.");
        assertEquals(1, rolloutGroupsAfterDeletionForDummyRollout.get(1).getRolloutTargetGroup().size(), "Group 2 should have the expected number of targets.");

        // Verify the number of targets after deletion (no deletion in target management)
        long targetCountAfterDeletion = targetManagement.count();
        assertEquals(3, targetCountAfterDeletion, "The number of targets should remain the same if no targets were deleted.");

        // Verify that the deleted target is no longer in the first rollout
        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).get();
        assertEquals(0, jpaRollout.getTotalTargets(), "First rollout should have 0 total targets after deletion.");
        assertEquals(0, jpaRollout.getRolloutGroupsCreated(), "First rollout should have 0 groups created.");
        assertEquals(0, jpaRollout.getRolloutGroups().size(), "First rollout should have no groups associated.");

        // Verify the second rollout still has the targets associated
        JpaRollout jpaRollout1 = rolloutRepository.findById(dummyRollout.getId()).get();
        assertEquals(3, jpaRollout1.getTotalTargets(), "Second rollout should have 3 total targets.");
        assertEquals(2, jpaRollout1.getRolloutGroupsCreated(), "Second rollout should have 2 groups created.");
        assertEquals(2, jpaRollout1.getRolloutGroups().size(), "Second rollout should have 2 groups associated.");
    }

    private Map<String, Object> prepareTestDataForCreatingESPTest(SupportPackageTestData testData) throws Exception {

        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        List<EcuModel> ecuModels = new ArrayList<>();
        //Create ECU Model
        if(!testData.getEcuNodeAddress().isEmpty()) {
            ecuModels = testdataFactory.addNewEcuModels(createEcuModel(testData.getEcuNodeAddress()));
        }
        assignEcuModelsToVehicle(targets, ecuModels);
        final Rollout rollout = createRolloutWithDependenciesForEsp(ROLLOUT, testdataFactory.createDistributionSet(), targets);
        List<String> vins = targets.stream().map(Target::getControllerId).toList();

        MgmtBaseSupportPackageCreateRequest requestBody  =  MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(testData.getFileUrl())
                .fileName(testData.getTestFileName())
                .fileType(testData.getFileType())
                .sha256(testData.getSha256())
                .fileVersion(testData.getVersion())
                .controllerIds(vins)
                .ecuNodeAddress(testData.getEcuNodeAddress())
                .fileContentDescription(LATEST_FIRMWARE_UPDATE)
                .fileInfoUrl(RELEASE_NOTES_URL)
                .fileMetadata(testData.getFileMetadata())
                .build();
        return Map.of(ROLLOUT, rollout, "requestBody", requestBody);
    }
    private @NotNull Rollout createRolloutWithDependenciesForEsp(String rolloutName, DistributionSet ds, List<Target> targets) {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();
        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroups(targets,rollout,groupsDetailsJson);
        return rollout;
    }


}
