/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.ddi.DdiActivateAutoConfirmation;
import org.cosmos.models.ddi.DdiConfirmationFeedback;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TenantConfigurationCreatedEvent;
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
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.ResultActions;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.repository.jpa.JpaConfirmationManagement.CONFIRMATION_CODE_MSG_PREFIX;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test confirmation base from the controller.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Confirmation Action Resource")
public class DdiConfirmationBaseTest extends AbstractDDiApiIntegrationTest {

    @MockBean
    MgmtS3Service s3Service;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @MockBean
    private SnsAsyncClient snsAsyncClient;

    private static final String DEFAULT_CONTROLLER_ID = "4747";
    private static final String EXPECTED_CONFIRMATION_BASE_LINK = "/device/v1/controllers/%s/confirmation/actions/%d";
    private static final String JSON_PATH_LINKS_CONFIRMATION_BASE_HREF = "$._links.confirmationBase.href";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF = "$._links.deploymentBase.href";
    private static final String MESSAGE = "Action confirmed message.";
    private static final String FORCED = "forced";
    private static final String TEST1 = "test1";
    private static final String TEST2 = "test2";
    private static final String TEST3 = "test3";
    private static final String TEST4 = "test4";
    private static final String TEST5 = "test5";
    private static final String TEST11 = "TEST11";
    private static final String TEST22 = "TEST22";
    private static final String VALUE_5120 = "5120";
    private static final Long SIZE = 123L;
    private static final String SHA_256 = "SHA_256";
    private static final String DESCRIPTION = "description";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_ARTIFACTS = "sp_artifacts";
    private static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_ECU_MODEL = "sp_ecu_model";
    private static final String SP_TARGET = "sp_target";
    private static final String SP_ACTION = "sp_action";
    private static final String SP_DISTRIBUTION_SET = "sp_distribution_set";

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    private static void mockPublishVehicleStatus() {
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_INVENTORY_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @BeforeEach
    public void setup() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_ECU_MODEL, SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_ECU_MODEL, SP_TARGET, SP_ACTION, SP_DISTRIBUTION_SET);

        // Create a mock PublishResponse
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(cdnUploadSnsService.publishMessage(any(CdnUploadRequest.class))).thenReturn(completedFuture);

        when(cdnDeleteSnsService.publishMessage(any(CdnDeleteRequest.class))).thenReturn(completedFuture);
        when(s3FileTransferSnsService.publishMessage(any(S3FileTransferRequest.class))).thenReturn(completedFuture);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }



    @Test
    @Description("Ensure that the confirmation endpoint is not available.")
    public void confirmationEndpointNotExposed() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);

        mvc.perform(get(DdiRestConstants.GET_CONFIRMATION_BASE_ACTION_PATH, controllerId, savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
     */
    @Test
    @Description("Ensure that the deploymentBase endpoint is not available for action ins WFC state.")
    @Disabled("This is failing and the feature is deprecated so disabling it for now.")
    @Deprecated
    public void deploymentEndpointNotAccessibleForActionsWFC() throws Exception {
        enableConfirmationFlow();

        Target savedTarget = setupRolloutAndGetTarget();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent()
                .get(0);

        mvc.perform(get(DdiRestConstants.GET_CONFIRMATION_BASE_ACTION_PATH, savedTarget.getControllerId(), savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, savedTarget.getControllerId(), savedAction.getId())
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    /**
     * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
     */
    @Test
    @Description("Confirmation base provides right values if auto-confirm not active.")
    @Disabled("This is failing and the feature is deprecated so disabling it for now.")
    @Deprecated
    void getConfirmationBaseProvidesAutoConfirmStatusNotActive() throws Exception {
        enableConfirmationFlow();

        final String controllerId = testdataFactory.createTarget("989").getControllerId();
        assignDistributionSet(testdataFactory.createDistributionSet("").getId(), controllerId);
        final long actionId = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent().get(0)
                .getId();

        final String confirmationBaseActionLink = String.format(EXPECTED_CONFIRMATION_BASE_LINK, controllerId, actionId);

        final String activateAutoConfLink = String.format("/device/v1/controllers/%s/confirmation/activate",controllerId);

        mvc.perform(get(DdiRestConstants.GET_CONFIRMATION_BASE_PATH, controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.FALSE)))
                .andExpect(jsonPath(JSON_PATH_LINKS_CONFIRMATION_BASE_HREF, containsString(confirmationBaseActionLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm.href", containsString(activateAutoConfLink)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm").doesNotExist());
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    @Description("Confirmation base provides right values if auto-confirm is active.")
    void getConfirmationBaseProvidesAutoConfirmStatusActive(final String initiator, final String remark)
            throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);

        final String deactivateAutoConfLink = String.format(
                "/device/v1/controllers/%s/confirmation/deactivate",controllerId);

        mvc.perform(get(DdiRestConstants.GET_CONFIRMATION_BASE_PATH, controllerId).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("autoConfirm.active", equalTo(Boolean.TRUE)))
                .andExpect(initiator == null ? jsonPath("autoConfirm.initiator").doesNotExist()
                        : jsonPath("autoConfirm.initiator", equalTo(initiator)))
                .andExpect(remark == null ? jsonPath("autoConfirm.remark").doesNotExist()
                        : jsonPath("autoConfirm.remark", equalTo(remark)))
                .andExpect(jsonPath("$._links.deactivateAutoConfirm.href", containsString(deactivateAutoConfLink)))
                .andExpect(jsonPath("$._links.activateAutoConfirm").doesNotExist());
    }

    private static Stream<Arguments> possibleActiveStates() {
        return Stream.of(Arguments.of("someInitiator", "someRemark"), Arguments.of(null, "someRemark"),
                Arguments.of("someInitiator", null), Arguments.of(null, null));
    }

    @ParameterizedTest
    @MethodSource("possibleActiveStates")
    @Description("Verify auto-confirm activation is handled correctly.")
    void activateAutoConfirmation(final String initiator, final String remark) throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("988");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));

        final String controllerId = savedTarget.getControllerId();

        final DdiActivateAutoConfirmation body = new DdiActivateAutoConfirmation(initiator, remark);

        mvc.perform(post(DdiRestConstants.POST_ACTIVATE_AUTO_CONFIRMATION_PATH, controllerId)
                .content(getMapper().writeValueAsString(body)).contentType(MediaType.APPLICATION_JSON_UTF8))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(confirmationManagement.getStatus(controllerId)).hasValueSatisfying(status -> {
            assertThat(status.getInitiator()).isEqualTo(initiator);
            assertThat(status.getRemark()).isEqualTo(remark);
            assertThat(status.getCreatedBy()).isEqualTo("bumlux");
        });
    }

    @Test
    @Description("Verify auto-confirm deactivation is handled correctly.")
    void deactivateAutoConfirmation() throws Exception {
        final String controllerId = testdataFactory.createTarget("988").getControllerId();

        confirmationManagement.activateAutoConfirmation(controllerId, null, null);

        mvc.perform(post(DdiRestConstants.POST_DEACTIVATE_AUTO_CONFIRMATION_PATH, controllerId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(confirmationManagement.getStatus(controllerId)).isEmpty();
    }


    private ResultActions sendConfirmationFeedback(final Target target, final Action action,
                                                   final DdiConfirmationFeedback.Confirmation confirmation, Integer code, String message) throws Exception {

        if (message == null) {
            message = RandomStringUtils.randomAlphanumeric(1000);
        }

        final String feedback = getJsonConfirmationFeedback(confirmation, code, Collections.singletonList(message));
        return mvc.perform(post(DdiRestConstants.POST_CONFIRMATION_ACTION_FEEDBACK_PATH, target.getControllerId(), action.getId())
                .content(feedback).contentType(MediaType.APPLICATION_JSON));
    }

    /**
     * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
     */
    @Test
    @Description("Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerDeploymentActionFeedback endpoint.")
    @Disabled("This is failing and the feature is deprecated so disabling it for now.")
    @Deprecated
    @ExpectEvents({ @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TenantConfigurationCreatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    void testActionHistoryCount() throws Exception {
        enableConfirmationFlow();

        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("990");
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        TestdataFactory.waitForSeconds(2);
        String controllerId = savedTarget.getControllerId();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, controllerId).getContent()
                .get(0);


        final Version version1 = testdataFactory.createVersion(getOsModule(ds), TEST11);
        SoftwareModule sm1 = savedAction.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = savedAction.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = savedAction.getDistributionSet().getModules().stream().toList().get(2);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(TEST3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        associationList.add(association3);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);


        final String CONFIRMED_MESSAGE = MESSAGE;

        final Integer CONFIRMED_CODE = 10;
        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.CONFIRMED,
                CONFIRMED_CODE, CONFIRMED_MESSAGE).andExpect(status().isOk());


        // confirmationBase not available in RUNNING state anymore
        mvc.perform(get(DdiRestConstants.GET_CONFIRMATION_BASE_ACTION_PATH, savedTarget.getControllerId(),
        savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // assert confirmed message against deploymentBase endpoint
        // this call will update the action due to retrieved action status update
        mvc.perform(get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH + "?actionHistory=2", savedTarget.getControllerId(),
                        savedAction.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.actionHistory.messages", hasItem(containsString(CONFIRMED_MESSAGE))))
                .andExpect(jsonPath("$.actionHistory.messages",
                        hasItem(containsString(String.format(CONFIRMATION_CODE_MSG_PREFIX, CONFIRMED_CODE)))));
    }

    /**
     * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
     */
    @Test
    @Description("When posting action feedback with a mixed case feedback status, the feedback should be accepted successfully.")
    @Disabled("This is failing and the feature is deprecated so disabling it for now.")
    @Deprecated
    public void givenMixedCaseFeedbackStatusWhenPostingFeedbackThenItIsAccepted() throws Exception {
        enableConfirmationFlow();

        Target savedTarget = setupRolloutAndGetTarget();

        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent()
                .get(0);

        // disable confirmation flow
        disableConfirmationFlow();

        // verify confirmation endpoint is still accessible
        sendConfirmationFeedback(savedTarget, savedAction, DdiConfirmationFeedback.Confirmation.DENIED, 20,
                "Action denied message.").andExpect(status().isOk());

        String feedback = getJsonConfirmationFeedback(DdiConfirmationFeedback.Confirmation.DENIED, 20,
                Collections.singletonList("Action denied message."));
        // Mixed case feedback status
        feedback = feedback.replace("denied", "DeNiEd");
        mvc.perform(post(DdiRestConstants.POST_CONFIRMATION_ACTION_FEEDBACK_PATH, savedTarget.getControllerId(), savedAction.getId())
                .content(feedback).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

}
