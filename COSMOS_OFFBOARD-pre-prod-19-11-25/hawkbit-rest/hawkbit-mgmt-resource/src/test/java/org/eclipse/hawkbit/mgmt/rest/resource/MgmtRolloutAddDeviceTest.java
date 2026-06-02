package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
 class MgmtRolloutAddDeviceTest extends AbstractManagementRolloutApiIntegrationTest{

    @Test
    @Description("When all the valid device details are provided then device details are added successfully")
    void givenDeviceDetailsWhenAddDeviceDetailsApiThenSuccess(CapturedOutput out) throws Exception {
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        Assertions.assertTrue(out.getAll().contains("Created " + 2 + " RolloutGroups for Rollout"));

        //Add another set of groups to the rollout
        String groupsDetailsJson2 = "[" +
                createBasicGroupJson("test-group03", 100, 20, 50) + "," +
                createBasicGroupJson("test-group04", 75, 40, 50) + "]";


        Target newTarget = testdataFactory.createTarget(CONTROLLER_ID);
        createRolloutGroups(List.of(newTarget), rollout, groupsDetailsJson2);

        Assertions.assertTrue(out.getAll().contains("Created " + 1 + " RolloutGroups for Rollout"));
    }

    @Test
    @Description("If the rollout is in invalid status, then return a bad request")
    void givenInvalidRolloutStatusWhenAddDeviceDetailsApiThenFailure() throws Exception {
        // Store the original autowired instances
        RolloutManagement originalRolloutManagement = (RolloutManagement) ReflectionTestUtils.getField(rolloutResource, ROLLOUT_MANAGEMENT);

        Rollout rollout = mock(JpaRollout.class);
        RolloutManagement tempRolloutManagement = mock(RolloutManagement.class);
        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, tempRolloutManagement);
        when(rollout.getId()).thenReturn(2L);
        when(rollout.getDistributionSet()).thenReturn(testdataFactory.createDistributionSet());
        when(rollout.getStatus()).thenReturn(RolloutStatus.FINISHED);
        when(rollout.getName()).thenReturn(ROLLOUT_0010);
        when(tempRolloutManagement.get(anyLong())).thenReturn(Optional.of(rollout));

        Path filePath = generateTargetDevicesFile(List.of());
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Adding device details in the rollout status - FINISHED is not allowed"))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));


        // Restore the original autowired instances
        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, originalRolloutManagement);
    }

    @Test
    @Description("if the target devices file is empty, then return a bad request")
    void givenEmptyTargetDevicesFileWhenAddDeviceDetailsApiThenFailure() throws Exception {

        // Store the original autowired instances
        RolloutManagement originalRolloutManagement = (RolloutManagement) ReflectionTestUtils.getField(rolloutResource, ROLLOUT_MANAGEMENT);

        Rollout rollout = mock(JpaRollout.class);
        RolloutManagement tempRolloutManagement = mock(RolloutManagement.class);

        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, tempRolloutManagement);

        when(rollout.getId()).thenReturn(2L);
        when(rollout.getDistributionSet()).thenReturn(testdataFactory.createDistributionSet());
        when(rollout.getStatus()).thenReturn(RolloutStatus.DRAFT);
        when(rollout.getName()).thenReturn(ROLLOUT_0010);
        when(tempRolloutManagement.get(anyLong())).thenReturn(Optional.of(rollout));

        Path filePath = generateTargetDevicesFile(List.of());
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("The target devices file must be provided and cannot be empty."))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // Restore the original autowired instances
        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, originalRolloutManagement);
    }

    @Test
    @Description("if target device file contains no controllerId, then return a bad request")
    void givenTargetDevicesFileWithNoControllerIdsWhenAddDeviceDetailsApiThenFailure() throws Exception {
        // Store the original autowired instances
        RolloutManagement originalRolloutManagement = (RolloutManagement) ReflectionTestUtils.getField(rolloutResource, ROLLOUT_MANAGEMENT);

        Rollout rollout = mock(JpaRollout.class);
        RolloutManagement tempRolloutManagement = mock(RolloutManagement.class);

        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, tempRolloutManagement);

        when(rollout.getId()).thenReturn(2L);
        when(rollout.getDistributionSet()).thenReturn(testdataFactory.createDistributionSet());
        when(rollout.getStatus()).thenReturn(RolloutStatus.DRAFT);
        when(rollout.getName()).thenReturn(ROLLOUT_0010);
        when(tempRolloutManagement.get(anyLong())).thenReturn(Optional.of(rollout));

        Path filePath = generateTargetDevicesFile(List.of("", "", ""));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("No Target devices found in the file"))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // Restore the original autowired instances
        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, originalRolloutManagement);
    }

    @Test
    @Description("if target device file contains no registered controllerId, then return a bad request")
    void givenNoRegisteredTargetsWhenAddDeviceDetailsApiThenFailure() throws Exception {
        // Store the original autowired instances
        RolloutManagement originalRolloutManagement = (RolloutManagement) ReflectionTestUtils.getField(rolloutResource, ROLLOUT_MANAGEMENT);

        Rollout rollout = mock(JpaRollout.class);
        RolloutManagement tempRolloutManagement = mock(RolloutManagement.class);

        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, tempRolloutManagement);

        when(rollout.getId()).thenReturn(2L);
        when(rollout.getDistributionSet()).thenReturn(testdataFactory.createDistributionSet());
        when(rollout.getStatus()).thenReturn(RolloutStatus.DRAFT);
        when(rollout.getName()).thenReturn(ROLLOUT_0010);
        when(tempRolloutManagement.get(anyLong())).thenReturn(Optional.of(rollout));

        Path filePath = generateTargetDevicesFile(List.of("randomControllerId1", "randomControllerId2"));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("No new registered target devices found in the file"))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        // Restore the original autowired instances
        ReflectionTestUtils.setField(rolloutResource, ROLLOUT_MANAGEMENT, originalRolloutManagement);
    }

    @Test
    @Description("If no distribution set is associated with the rollout, then return a bad request")
    void givenNoDistributionSetWhenAddDeviceDetailsApiThenFailure() throws Exception {
        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Distribution Set is not present in the rollout"))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @Description("if the provided rollout does not exist then return a bad request")
    void givenRolloutDoesNotExistWhenAddDeviceDetailsApiThenFailure() throws Exception {
        Rollout rollout = mock(JpaRollout.class);
        when(rollout.getId()).thenReturn(2L);

        Path filePath = generateTargetDevicesFile(List.of());
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Rollout with given identifier {2} does not exist."))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }


    @ParameterizedTest
    @MethodSource("provideGroupDetailsJsonAndExpectedResult")
    @Description("Test only groups with atleast one or more targets are created ")
    public void givenAddDeviceDetailsWhenGroupDetailsThenOnlyGroupsWithAtLeastOneTargetsAreCreated(String groupsDetailsJson, int expectedResult, int iteration, CapturedOutput out){
        Rollout rollout=null;
        for (int i=0; i<iteration; i++) {
          List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1+i, CONTROLLER_ID2+i, CONTROLLER_ID3+i);
          if(Objects.isNull(rollout)){
              rollout= createRolloutWithDependenciesAndGroups(
                      ROLLOUT,
                      testdataFactory.createDistributionSet(),
                      targets,
                      groupsDetailsJson, true
              );
          }else {
              Assertions.assertNotNull(rollout);
              createRolloutGroups(targets, rollout, groupsDetailsJson);
          }
          Assertions.assertNotNull(rollout);
          Assertions.assertTrue(out.getAll().contains("Created " + expectedResult + " RolloutGroups for Rollout"));
          Assertions.assertEquals(1, rolloutGroupRepository.findByRolloutId(rollout.getId()).stream().filter(rg -> Objects.isNull(rg.getParent())).count());
      }
    }



    private static Stream<Arguments> provideGroupDetailsJsonAndExpectedResult() {
        return Stream.of(
                //Targets are part of both the groups, 2 groups are created
                Arguments.of("[" +
                        createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                        createBasicGroupJson("test-group02", 75, 40, 50) + "]", 2,1),

                //targets are part of only one group, only 1 group is created
                Arguments.of("[" +
                        createBasicGroupJson("test-group01", 100, 20, 50) + "," +
                        createBasicGroupJson("test-group02", 75, 40, 50) + "]", 1,1),

                //All targets are part of only two group, only 2 group is created
                Arguments.of("[" +
                        createBasicGroupJson("test-group01", 50, 20, 50, "vin==controller1") + "," +
                        createBasicGroupJson("test-group02", 100, 20, 50) + "," +
                        createBasicGroupJson("test-group03", 50, 20, 50) + "," +
                        createBasicGroupJson("test-group04", 70, 20, 50) + "," +
                        createBasicGroupJson("test-group05", 20, 20, 50) + "," +
                        createBasicGroupJson("test-group06", 75, 40, 50) + "]", 2,2)
        );
    }

    @Description("Test only groups with atleast one or more targets are created on adding more targets")
    @Test
    void givenAddDeviceDetailsWhenAddingMoreTargetsThenOnlyGroupsWithTargetsCreated(CapturedOutput out){
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "," +
                createBasicGroupJson("test-group02", 75, 40, 50) + "]";

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        Assertions.assertTrue(out.getAll().contains("Created " + 2 + " RolloutGroups for Rollout"));

        //Add another set of groups to the rollout
        String groupsDetailsJson2 = "[" +
                createBasicGroupJson("test-group03", 100, 20, 50) + "," +
                createBasicGroupJson("test-group04", 75, 40, 50) + "]";


        Target newTarget = testdataFactory.createTarget(CONTROLLER_ID);
        createRolloutGroups(List.of(newTarget), rollout, groupsDetailsJson2);

        Assertions.assertTrue(out.getAll().contains("Created " + 1 + " RolloutGroups for Rollout"));

    }

    @Test
    @Description("Given rollout, when creating groups then default group has null parentID, others link to a rollout group")
    void givenRolloutWhenCreatingGroupsThenOnlyFirstGroupHasNullParent() throws Exception {
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "]" ;

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        Assertions.assertNull(firstGroup.getParent());

        String groupsDetailsJson2 = "[" +
                createBasicGroupJson("test-group02", 100, 20, 50) + "," +
                createBasicGroupJson("test-group03", 75, 40, 50) + "]";

        Target newTarget = testdataFactory.createTarget(CONTROLLER_ID);
        createRolloutGroups(List.of(newTarget), rollout, groupsDetailsJson2);

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);

        Assertions.assertEquals(firstGroup.getId(), secondGroup.getParent().getId());
        Assertions.assertEquals(secondGroup.getId(), thirdGroup.getParent().getId());
    }


    @Test
    @Description("Given delete Device Details API is called with valid device details, when single device is removed, then the parent ID of the group should be remapped.")
    void givenValidDeviceDetailsWhenSingleDeviceIsRemovedThenParentIdRemapped() throws Exception {
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID,CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 25, 40, 50) + "," +
                createBasicGroupJson("test-group03", 25, 20, 50) + "," +
                createBasicGroupJson("test-group04", 25, 40, 50) + "]" ;

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1));
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        Assertions.assertNull(firstGroup.getParent());

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        Assertions.assertEquals(firstGroup.getId(), secondGroup.getParent().getId());
    }

    @Test
    @Description("Given delete Device Details API is called with valid device details, when list of devices are removed, then the parent ID of the group should be remapped.")
    void givenValidDeviceDetailsWhenListOfDevicesRemovedThenParentIdRemapped() throws Exception {
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID,CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 25, 20, 50) + "," +
                createBasicGroupJson("test-group02", 25, 40, 50) + "," +
                createBasicGroupJson("test-group03", 25, 20, 50) + "," +
                createBasicGroupJson("test-group04", 25, 40, 50) + "]" ;

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                targets,
                groupsDetailsJson, true
        );

        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1, CONTROLLER_ID2));
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.DELETE,
                                MgmtRestConstants.DELETE_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId())
                        .file(file)
                        .param("deleteEsp", String.valueOf(true))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        Assertions.assertNull(firstGroup.getParent());

        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        Assertions.assertEquals(firstGroup.getId(), secondGroup.getParent().getId());
    }
}
