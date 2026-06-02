/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test cancel action from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Cancel Action Resource")
class DdiCancelActionTest extends AbstractDDiApiIntegrationTest {


    private static final String ACTIONS_BASE_URL = "/actions/";
    private static final String CANCELED_BASE_URL = "/canceled";
    private static final String JSON_PATH_CANCEL_ACTION = "$.cancelAction.stopId";
    private static final String DEPLOYED_BASE_URL = "/deployed";
    private static final String JSON_PATH_CONFIG_POLLING_SLEEP = "$.config.polling.sleep";
    private static final String TIME = "00:01:00";
    private static final String DOT_ID = "$.id";
    private static final String TEST3 = "test3";
    private static final String TEST4 = "test4";
    private static final String TEST5 = "test5";
    private static final String TEST11 = "TEST11";
    private static final String VALUE_5120 = "5120";
    private static final Long SIZE = 123L;
    private static final String SHA_256 = "SHA_256";
    private static final String DESCRIPTION = "description";
    private static final String FORCED = "forced";
    private static final String JSON_PATH_LINKS_CANCEL_ACTION_HREF = "$._links.cancelAction.href";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF = "$._links.deploymentBase.href";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_DOWNLOAD_HREF = "$.deployment.download";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_UPDATE_HREF = "$.deployment.update";
    private static final String MESSAGE = "message";
    private static final String VALUE_34534543 = "34534543";
    private static final String VALUE_12345 = "12345";
    private static final String X250 = "X250";
    private static final String CANCELED_ACTIONS_URL = "/actions/1/canceled";
    private static final String DEVICE_CONTROLLERS_BASE_URL = "/device/v1/controllers/";
    private static final String API_DEVICE_V1_CONTROLLER_PATH = "/device/v1/controllers/{controller}";
    private static final String DEVICE_V1_CONTROLLERS_BASE_URL = "http://localhost/device/v1/controllers/";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_ARTIFACTS = "sp_artifacts";
    private static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_ECU_MODEL = "sp_ecu_model";
    private static final String SP_TARGET = "sp_target";
    private static final String SP_ACTION = "sp_action";
    private static final String SP_DISTRIBUTION_SET = "sp_distribution_set";

    @BeforeEach
    public void setup() throws IOException {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_ECU_MODEL, SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_ECU_MODEL, SP_TARGET, SP_ACTION, SP_DISTRIBUTION_SET);
        setupCertificatesAndKeysForDDGeneration();
    }

    @Test
    @Description("Tests that the cancel action resource can be used with CBOR.")
    void cancelActionCbor() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        testdataFactory.createTarget();
        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        // check that we can get the cancel action as CBOR
        final byte[] result = mvc
                .perform(get(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + ACTIONS_BASE_URL
                        + cancelAction.getId() + CANCELED_BASE_URL, tenantAware.getCurrentTenant())
                        .accept(DdiRestConstants.MEDIA_TYPE_CBOR))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(DdiRestConstants.MEDIA_TYPE_CBOR)).andReturn().getResponse()
                .getContentAsByteArray();
        assertThat(JsonPathUtils.<String>evaluate(cborToJson(result), DOT_ID))
                .isEqualTo(String.valueOf(cancelAction.getId()));
        assertThat(JsonPathUtils.<String>evaluate(cborToJson(result), JSON_PATH_CANCEL_ACTION))
                .isEqualTo(String.valueOf(actionId));
    }

    @Test
    @Description("Test of the controller can continue a started update even after a cancel command if it so desires.")
    void rootRsCancelActionButContinueAnyway() throws Exception {
        // prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        // controller rejects cancellation
        mvc.perform(post(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + ACTIONS_BASE_URL
                        + cancelAction.getId() + CANCELED_BASE_URL, tenantAware.getCurrentTenant())
                        .content(getJsonPostRejectedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final long current = Instant.now().getEpochSecond();

        final Version version1 = testdataFactory.createVersion(getOsModule(ds), TEST11);
        SoftwareModule sm1 = ds.getModules().stream().toList().get(0);
        SoftwareModule sm2 = ds.getModules().stream().toList().get(1);
        SoftwareModule sm3 = ds.getModules().stream().toList().get(2);
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(TEST3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST4, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST5, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        // get update action anyway
        mvc.perform(get(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + ACTIONS_BASE_URL + actionId + DEPLOYED_BASE_URL,
                        tenantAware.getCurrentTenant()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResponse().getContentAsString().contains("DD cannot be generated, no updatable ECUs/software modules found")));

    }

    @Test
    @Description("Tests various bad requests and if the server handles them as expected.")
    void badCancelAction() throws Exception {

        // not allowed methods
        mvc.perform(patch(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + CANCELED_ACTIONS_URL)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + CANCELED_ACTIONS_URL)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(get(DEVICE_CONTROLLERS_BASE_URL + VALUE_34534543 + CANCELED_ACTIONS_URL).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        createCancelAction(VALUE_34534543, X250);

        // wrong media type
        mvc.perform(get(DEVICE_CONTROLLERS_BASE_URL + VALUE_34534543 + CANCELED_ACTIONS_URL)
                        .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());

    }

    private Action createCancelAction(final String targetid, final String vehicleModelName) {
        final DistributionSet ds = testdataFactory.createDistributionSet(targetid);
        final Target savedTarget = testdataFactory.createTarget(targetid, targetid, targetid, testdataFactory.createVehicle(vehicleModelName).getId());
        final List<Target> toAssign = new ArrayList<>();
        toAssign.add(savedTarget);
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds, toAssign));

        return deploymentManagement.cancelAction(actionId);
    }

    @Test
    @Description("Tests the feedback channel of the cancel operation.")
    @Disabled
    void rootRsCancelActionFeedback() throws Exception {

        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));

        // cancel action manually
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        // cancellation canceled -> should remove the action from active
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonPostCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(3);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(0);

        // cancellation rejected -> action still active until controller close
        // it
        // with finished or
        // error
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(0);
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonPostRejectedCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(0);
    }


    @Test
    @Description("Tests the feeback chanel of for multiple open cancel operations on the same target.")
    void multipleCancelActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("", true, false);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true, false);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true, false);

        final Target savedTarget = testdataFactory.createTarget();

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        final Long actionId2 = getFirstAssignedActionId(
                assignDistributionSet(ds2.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));
        final Long actionId3 = getFirstAssignedActionId(
                assignDistributionSet(ds3.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));

        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(3);

        // 3 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        final Action cancelAction = deploymentManagement.cancelAction(actionId);
        final Action cancelAction2 = deploymentManagement.cancelAction(actionId2);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(3);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())).isEqualTo(3);

        mvc.perform(get(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(DOT_ID, equalTo(String.valueOf(cancelAction.getId()))))
                .andExpect(jsonPath(JSON_PATH_CANCEL_ACTION, equalTo(String.valueOf(actionId))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(6);

        // now lets return feedback for the first cancelation
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonPostCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(7);

        // 1 update actions, 1 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())
                + actionRepository.countByTarget_ControllerIdAndActive(savedTarget.getControllerId(), false)).isEqualTo(3);
        mvc.perform(get(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction2.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOT_ID, equalTo(String.valueOf(cancelAction2.getId()))))
                .andExpect(jsonPath(JSON_PATH_CANCEL_ACTION, equalTo(String.valueOf(actionId2))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(8);

        // now lets return feedback for the second cancelation
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction2.getId())
                        .content(getJsonPostCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(9);
        assertThat(deploymentManagement.getAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds3);
        final Version version1 = testdataFactory.createVersion(getOsModule(ds), TEST11);
        SoftwareModule sm1 = ds3.getModules().stream().toList().get(0);
        SoftwareModule sm2 = ds3.getModules().stream().toList().get(1);
        SoftwareModule sm3 = ds3.getModules().stream().toList().get(2);
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(TEST3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST4, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST5, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        mvc.perform(
                        get(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + ACTIONS_BASE_URL + actionId3 + DEPLOYED_BASE_URL))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("DD cannot be generated, no updatable ECUs/software modules found")));

        // 1 update actions, 0 cancel actions
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action cancelAction3 = deploymentManagement.cancelAction(actionId3);

        // action is in cancelling state
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId()) +
                actionRepository.countByTarget_ControllerIdAndActive(savedTarget.getControllerId(), false)).isEqualTo(3);
        assertThat(deploymentManagement.getAssignedDistributionSet(TestdataFactory.DEFAULT_CONTROLLER_ID).get())
                .isEqualTo(ds3);

        mvc.perform(get(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction3.getId()).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath(DOT_ID, equalTo(String.valueOf(cancelAction3.getId()))))
                .andExpect(jsonPath(JSON_PATH_CANCEL_ACTION, equalTo(String.valueOf(actionId3))));
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(11);

        // now lets return feedback for the third cancelation
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction3.getId())
                        .content(getJsonPostCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(12);

        // final status
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsByTarget(savedTarget.getControllerId())+
                actionRepository.countByTarget_ControllerIdAndActive(savedTarget.getControllerId(), false)
        ).isEqualTo(3);
    }

    @Test
    @Description("Tests the feeback channel closing for too many feedbacks, i.e. denial of service prevention.")
    @Disabled
    void tooMuchCancelActionFeedback() throws Exception {
        testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(
                assignDistributionSet(ds.getId(), TestdataFactory.DEFAULT_CONTROLLER_ID));

        final Action cancelAction = deploymentManagement.cancelAction(actionId);

        final String feedback = getJsonPostCanceledDeploymentActionFeedback();
        // assignDistributionSet creates an ActionStatus and cancel action
        // stores an action status, so
        // only 97 action status left
        for (int i = 0; i < 98; i++) {
            mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId()).content(feedback)
                            .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk());
        }
    }

    @Test
    @Description("test the correct rejection of various invalid feedback requests")
    void badCancelActionFeedback() throws Exception {
        final Action cancelAction = createCancelAction(TestdataFactory.DEFAULT_CONTROLLER_ID, "STLA");
        createCancelAction("4715", "Dcross");

        // not allowed methods
        mvc.perform(put(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(DEVICE_CONTROLLERS_BASE_URL + TestdataFactory.DEFAULT_CONTROLLER_ID + ACTIONS_BASE_URL
                        + cancelAction.getId() + CANCELED_BASE_URL))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(patch(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // bad content type
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_ATOM_XML)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // bad body
        String invalidFeedback = "{\"status\":{\"execution\":\"546456456\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"none\"]}}";
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId()).content(invalidFeedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // non existing target
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, VALUE_12345, cancelAction.getId()).content(getJsonPostCanceledCancelActionFeedback())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // invalid action
        invalidFeedback = "{\"id\":\"sdfsdfsdfs\",\"status\":{\"execution\":\"closed\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"details\"]}}";
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId()).content(invalidFeedback)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // finaly, get it right :)
        mvc.perform(post(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH, TestdataFactory.DEFAULT_CONTROLLER_ID, cancelAction.getId())
                        .content(getJsonPostCanceledCancelActionFeedback()).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }
}
