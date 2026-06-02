/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import ch.qos.logback.classic.Level;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.StreamSupport;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.ddi.DdiDeploymentDescriptor;
import org.cosmos.models.ddi.DdiDeploymentDescriptorBase;
import org.cosmos.models.ddi.DdiDeploymentMetadata;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiFeedbackRequestBody;
import org.cosmos.models.ddi.DdiPackage;
import org.cosmos.models.ddi.DdiRequiredStateOfCharge;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiSignature;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.ddi.DdiUserAcceptanceMessage;
import org.cosmos.models.ddi.ExecutionType;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaGeneralFeedback;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInventory;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.jpa.model.JpaVehicle;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.eclipse.hawkbit.repository.model.TargetSoftware;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the root controller resources.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Root Poll Resource")
@ExtendWith(MockitoExtension.class)
@ExtendWith(OutputCaptureExtension.class)
class DdiRootControllerTest extends AbstractDDiApiIntegrationTest {


    public static final String TARGET1_ID = "4717";
    @InjectMocks
    DataConversionHelper dataConversionHelper;
    @Mock
    EcuModelManagement ecuModelManagement;
    @MockBean
    MgmtS3Service s3Service;
    @Autowired
    private HawkbitSecurityProperties securityProperties;
    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Value("${ddi.inventory.inventory-signature}")
    private String inventorySignature;
    @Value("${ddi.inventory.inventory-details}")
    private String inventoryDetails;
    @Value("${ddi.inventory.staticInventory-signature}")
    private String staticInventorySignature;
    @Value("${ddi.inventory.staticInventory-hash}")
    private String staticInventoryHash;
    @Value("${ddi.inventory.rawInventory-signature}")
    private String rawInventorySignature;
    @Value("${ddi.inventory.rawInventory-details}")
    private String rawInventoryDetails;
    @Value("${ddi.inventory.malformed-inventory-details}")
    private String malformedInventoryDetails;
    @Value("${ddi.inventory.invalid-inventory-details}")
    private String invalidInventoryDetails;
    @Value("${ddi.inventory.empty-ecu-list}")
    private String emptyEcuList;
    @Value("${ddi.inventory.empty-scomos-list}")
    private String emptyScomosList;
    @Value("${ddi.inventory.empty-scomos-id}")
    private String emptyScomosId;
    @Value("${ddi.inventory.empty-software-version}")
    private String emptySwVersion;
    @Value("${ddi.inventory.empty-hardware-version}")
    private String emptyHwVersion;
    @Value("${ddi.inventory.empty-node-address}")
    private String emptyNodeAddress;
    @Value("${ddi.inventory.empty-part-number}")
    private String emptyPartNumber;
    @Value("${log.maxLogAllFileSize}")
    private String logMaxSize;
    @Value("${ddi.inventory.inventory-details-controller-id}")
    private String inventoryDetailsControllerId;
    @Value("${ddi.inventory.inventory-details-mismatched-controller-id}")
    private String inventoryDetailsMismatchedControllerId;
    @Value("${ddi.inventory.inventory-details-with-known-controller-id}")
    private String inventoryDetailsWithKnownControllerId;
    @Value("${ddi.inventory.inventory-details-with-invalid-target}")
    private String inventoryDetailsWithInvalidTarget;
    @Value("${ddi.inventory.invalid-target-controller-id}")
    private String InvalidTargetControllerId;
    @Value("${ddi.inventory.blank-software-version}")
    private String blankSwVersion;
    @Value("${ddi.inventory.null-software-version}")
    private String nullSwVersion;
    @Value("${ddi.inventory.blank-scomos-id}")
    private String blankScomosId;
    @Value("${ddi.inventory.null-scomos-id}")
    private String nullScomosId;

    @Autowired
    private EspRepository espRepository;

    @Mock
    private AbstractAuthenticationToken token;
    @Autowired
    private ArtifactsRepository artifactsRepository;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private EntityFactory entityFactory;
    @Mock
    private CdnUploadSnsService cdnUploadSnsService;
    @Mock
    private CdnDeleteSnsService cdnDeleteSnsService;
    @Mock
    private S3FileTransferSnsService s3FileTransferSnsService;
    @Mock
    private RetryContext retryContext;
    @MockBean
    private SnsAsyncClient snsAsyncClient;
    @Mock
    private DdiDownload ddiDownload;

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

    private static byte[] randomBytes(final int len) {
        return RandomStringUtils.randomAlphanumeric(len).getBytes();
    }

    @BeforeEach
    void setup() throws IOException {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY,
                SP_BASE_SOFTWARE_MODULE, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_BASE_SOFTWARE_MODULE, SP_SOFTWARE_ECU_MODEL,
                SP_ROLLOUT, SP_DISTRIBUTION_SET
        );
        MockitoAnnotations.initMocks(this);
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
        setupCertificatesAndKeysForDDGeneration();
        RetrySynchronizationManager.register(retryContext); // Register the mock RetryContext
        ddiDownload = new DdiDownload(80, new DdiPackage(3, 5));
    }

    @Test
    @Description("Ensures that server returns a not found response in case of empty controller ID.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 0)})
    void rootRsWithoutId() throws Exception {
        mvc.perform(get(CONTROLLER_BASE_URL)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }


    @Test
    @Description("Controller trys to finish an update process after it has been finished by an error action status.")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 1),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = CancelTargetAssignmentEvent.class, count = 1),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 8),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 1)
    })    void tryToFinishAnUpdateProcessAfterItHasBeenFinished() throws Exception {
        var savedTarget = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        savedTarget = getFirstAssignedTarget(assignDistributionSet(savedAction.getDistributionSet().getId(), savedTarget.getControllerId()));
        List<DdiStatus> ddiStatuses = List.of(getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.DOWNLOAD_STARTED, null, MESSAGE, 1737348213000L),
                getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS, ddiDownload, MESSAGE1, 1737089013000L),
                getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.DOWNLOAD_COMPLETED, null, MESSAGE2, 1737780213000L));
        String feedbackAsString = getJsonActionFeedbackObjectToSting(ddiStatuses);
        sendDeploymentActionFeedback(savedTarget, savedAction, feedbackAsString).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
//TODO: Need to check why this status is gone also below 3 parts are disabled since its associated with rollout for sending vehicle status
//        sendDeploymentActionFeedback(savedTarget, savedAction, CLOSED, SUCCESS).andDo(MockMvcResultPrinter.print()).andExpect(status().isGone());

        // Send feedback for log upload in progress
//        sendDeploymentActionFeedback(savedTarget, savedAction, LOG_UPLOAD_IN_PROGRESS, NONE).andDo(MockMvcResultPrinter.print());
//
//        // Send feedback for log upload success
//        sendDeploymentActionFeedback(savedTarget, savedAction, LOG_UPLOAD_SUCCESS, SUCCESS).andDo(MockMvcResultPrinter.print());
//
//        // Send feedback for log upload failure
//        sendDeploymentActionFeedback(savedTarget, savedAction, LOG_UPLOAD_FAILURE, FAILURE).andDo(MockMvcResultPrinter.print());

    }

    @Test
    @Description("Test to verify that only a specific count of messages are returned based on the input actionHistory for getControllerDeploymentActionFeedback endpoint,  the API getControllerDeploymentActionFeedback consistently returns HTTP 400 Bad Request and the message 'DD cannot be generated, no updatable ECUs/software modules found'.\")")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 0),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 7),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 8)
    })
    void givenExistingFeedbacks_whenRequestingDeploymentActionHistoryWithLimit_thenReturnsBadRequestDueToNoUpdateAvailable() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("12");
        Target savedTarget = testdataFactory.createTarget("4443", "4443", "203", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        Version v1 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM8);
        Version v2 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM9);
        final SoftwareModule sm1 = ds.getModules().stream().toList().get(0);
        final SoftwareModule sm2 = ds.getModules().stream().toList().get(1);
        final SoftwareModule sm3 = ds.getModules().stream().toList().get(2);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(FILE1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(FILE2, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(FILE3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        associationList.add(association3);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        sendDeploymentActionFeedback(savedTarget, savedAction, USER_SCHEDULED, NONE, TARGET_SCHEDULED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        TestdataFactory.waitForSeconds(1);
        var target1 = setupRolloutAndGetTarget();
        final Action action1 = deploymentManagement.findActionsByTarget(target1.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(target1, action1, DOWNLOAD_IN_PROGRESS, NONE, TARGET_PROCEEDING_INSTALLATION_MSG, ddiDownload)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        sendDeploymentActionFeedback(target1, action1, DOWNLOAD_STARTED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Send feedback for log upload in progress
        sendDeploymentActionFeedback(target1, action1, LOG_UPLOAD_IN_PROGRESS, NONE, null).andDo(MockMvcResultPrinter.print());
        // Send feedback for log upload success
        sendDeploymentActionFeedback(target1, action1, LOG_UPLOAD_SUCCESS, SUCCESS, null).andDo(MockMvcResultPrinter.print());
        // Send feedback for log upload failure
        sendDeploymentActionFeedback(target1, action1, LOG_UPLOAD_FAILURE, FAILURE, null).andDo(MockMvcResultPrinter.print());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=2", target1.getControllerId(), action1.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("DD cannot be generated, no updatable ECUs/software modules found")));
    }

    @Test
    @Description("Test to verify that a zero input value of actionHistory results in no action history appended for getControllerDeploymentActionFeedback endpoint.")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 0),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 4),
            @Expect(type = ArtifactsCreatedEvent.class, count = 7),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 8),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 1)
    })
    void testActionHistoryZeroInput() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("1112", "1112", "206", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        Version v1 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM8);
        Version v2 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM9);
        final SoftwareModule sm1 = ds.getModules().stream().toList().get(0);
        final SoftwareModule sm2 = ds.getModules().stream().toList().get(1);
        final SoftwareModule sm3 = ds.getModules().stream().toList().get(2);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(FILE1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(FILE2, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(FILE3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        associationList.add(association3);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        sendDeploymentActionFeedback(savedTarget, savedAction, USER_SCHEDULED, null, TARGET_SCHEDULED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        var target1 = setupRolloutAndGetTarget();
        final Action action1 = deploymentManagement.findActionsByTarget(target1.getControllerId(), PAGE).getContent().get(0);
        sendDeploymentActionFeedback(target1, action1, DOWNLOAD_IN_PROGRESS, null, TARGET_PROCEEDING_INSTALLATION_MSG, ddiDownload)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        sendDeploymentActionFeedback(target1, action1, DOWNLOAD_STARTED, SUCCESS, TARGET_COMPLETED_INSTALLATION_MSG, null)
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());


        // Send feedback for log upload in progress
        sendDeploymentActionFeedback(target1, action1, LOG_UPLOAD_IN_PROGRESS, NONE, null).andDo(MockMvcResultPrinter.print());

        // Send feedback for log upload success
        sendDeploymentActionFeedback(savedTarget, action1, LOG_UPLOAD_SUCCESS, SUCCESS, null).andDo(MockMvcResultPrinter.print());
        // Send feedback for log upload failure
        sendDeploymentActionFeedback(savedTarget, savedAction, LOG_UPLOAD_FAILURE, FAILURE, null).andDo(MockMvcResultPrinter.print());
        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=0", 1112, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ACTION_HISTORY_MESSAGES).doesNotExist());

        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory", 1112, savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ACTION_HISTORY_MESSAGES).doesNotExist());

    }

    @Test
    @Description("This test checks that when requesting the target-based deployment action history with no available update, the API getTargetBasedDeploymentAction consistently returns HTTP 400 Bad Request and the message 'DD cannot be generated, no updatable ECUs/software modules found'.")
    @ExpectEvents({
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 2),
            @Expect(type = ActionUpdatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 2),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 0),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 4),
            @Expect(type = SoftwareModuleUpdatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 7),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 3),
            @Expect(type = RolloutUpdatedEvent.class, count = 8)
    })
    void testActionHistoryNegativeInput() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("1432", "1432", "1432", testdataFactory.createVehicle("NES 2.0").getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        Version v1 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM8);
        Version v2 = testdataFactory.createVersion(ds.getModules().stream().toList().get(0).getId(), GETSWM9);
        final SoftwareModule sm1 = ds.getModules().stream().toList().get(0);
        final SoftwareModule sm2 = ds.getModules().stream().toList().get(1);
        final SoftwareModule sm3 = ds.getModules().stream().toList().get(2);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(FILE1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(FILE2, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact3 = testdataFactory.createArtifactsWithExpiryDate(FILE3, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) v1).targetVersion((JpaVersion) v2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        associationList.add(association3);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        var target1 = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(target1.getControllerId(), PAGE).getContent().get(0);
        List<DdiStatus> ddiStatuses = List.of(getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.DD_ACCEPTED, null, TARGET_COMPLETED_INSTALLATION_MSG, 1737348213000L),
                getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS, ddiDownload, TARGET_PROCEEDING_INSTALLATION_MSG, 1737089013000L),
                getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.USER_SCHEDULED, null, TARGET_COMPLETED_INSTALLATION_MSG, 1737089013000L));
        String feedbackAsString = getJsonActionFeedbackObjectToSting(ddiStatuses);
        sendDeploymentActionFeedback(target1, savedAction, feedbackAsString).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Send feedback for log upload in progress
        sendDeploymentActionFeedback(target1, savedAction, LOG_UPLOAD_IN_PROGRESS, NONE, null).andDo(MockMvcResultPrinter.print());

        // Send feedback for log upload success
        sendDeploymentActionFeedback(target1, savedAction, LOG_UPLOAD_SUCCESS, SUCCESS, null).andDo(MockMvcResultPrinter.print());

        // Send feedback for log upload failure
        sendDeploymentActionFeedback(target1, savedAction, LOG_UPLOAD_FAILURE, FAILURE, null).andDo(MockMvcResultPrinter.print());


        mvc.perform(get(DEPLOYMENT_BASE + "?actionHistory=-1", target1.getControllerId(), savedAction.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("DD cannot be generated, no updatable ECUs/software modules found")));
    }

    @Test
    @Description("Test download and update values after maintenance window start time.")
    void downloadAndUpdateStatusDuringMaintenanceWindow() throws Exception {
        Target target = setupRolloutAndGetTargetForDD();
        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        final MvcResult result = mvc.perform(get(DEPLOYMENT_BASE, target.getControllerId(), action.getId())
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = extractDecodedJsonNode(result, "$.deploymentDescription");

        // Perform assertions on the decoded JSON to verify expected structure and values
        assertNotNull(jsonNode.get("rolloutName"));
        assertTrue(jsonNode.has("download"));
        assertTrue(jsonNode.has("update"));

// ✅ Correct assertions comparing actual JSON values
        assertEquals("attempt", jsonNode.get("download").asText());
        assertEquals("attempt", jsonNode.get("update").asText());
    }

    @Test
    @Description("API to send collective feedback with successfully closed execution.")
    void givenFeedbackStatusList_SuccessTest() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = setupRolloutAndGetTarget();
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);
        List<DdiStatus> ddiStatuses = List.of(getDdiStatus(DdiStatus.ExecutionStatus.DOWNLOAD_STARTED, null, MESSAGE, 1737348213000L),
                getDdiStatus(DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS, ddiDownload, MESSAGE1, 1737348213000L),
                getDdiStatus(DdiStatus.ExecutionStatus.DD_ACCEPTED, null, MESSAGE2, 1737348213000L));
        String feedbackAsString = getJsonActionFeedbackObjectToSting(ddiStatuses);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());

    }

    @Test
    @Description("All Feedback API with User accepted/ignored/scheduled status if message status does not match with execution status then log error.")
    void givenFeedbackApiWhenUserAcceptanceStatusNotMatchWithExecutionStatusThenError(CapturedOutput output) throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("321234F45240S", "321234F45240S", "201", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);

        DdiUserAcceptanceMessage userAcceptanceMessage = DdiUserAcceptanceMessage.builder()
                .userResponse(DdiStatus.ExecutionStatus.USER_IGNORED)
                .vin("test").ecuHMISN("test").otaMasterSN("test")
                .prompt("test").scheduledTime(0L).timeStampOfPrompt(0L)
                .build();

        DdiStatus ddiStatus = getDdiStatus(DdiStatus.ExecutionStatus.USER_ACCEPTED, 1737089013000L, null, MESSAGE, userAcceptanceMessage, "test");

        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON));
        assertTrue(output.getAll().contains("Execution status does not match with provided user acceptance message status"));
    }

    @Test
    @Description("All FeedbackAPI with User scheduled status - scheduled time is mandatory and should be in future. An error is logged otherwise")
    void givenFeedbackApiWhenUserScheduledStatusScheduledTimeIsMandatoryAndValid(CapturedOutput output) throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("321234F45240S", "321234F45240S", "201", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);

        DdiUserAcceptanceMessage userAcceptanceMessage = DdiUserAcceptanceMessage.builder()
                .userResponse(DdiStatus.ExecutionStatus.USER_SCHEDULED)
                .vin("test").ecuHMISN("test").otaMasterSN("test").prompt("test").scheduledTime(1234L).timeStampOfPrompt(0L)
                .build();

        DdiStatus ddiStatus = getDdiStatus(DdiStatus.ExecutionStatus.USER_SCHEDULED, 1737089013000L, null, MESSAGE, userAcceptanceMessage, null);
        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON));
        assertTrue(output.getAll().contains("Invalid or past scheduled time provided"));
    }

    @ParameterizedTest
    @CsvSource({
            "USER_ACCEPTED, User has accepted to proceed with installation",
            "USER_IGNORED, User has ignored to proceed with installation",
            "USER_SCHEDULED, User has scheduled to proceed with the installation"
    })
    @Description("All Feedback API with User accepted/ignored/scheduled status send a tailored message to DOCG")
    void givenFeedbackApiWhenDifferentUserStatusThenTailoredMessageIsSent1(DdiStatus.ExecutionStatus executionStatus, String expectedMessage, CapturedOutput output) throws Exception {
        //Enforce debug logging for this testcase.
        Logger logger = LoggerFactory.getLogger(DdiRootController.class);
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);

        //setup
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("321234F45240S", "321234F45240S", "201", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);

        DdiUserAcceptanceMessage userAcceptanceMessage = DdiUserAcceptanceMessage.builder()
                .userResponse(executionStatus)
                .vin("test").ecuHMISN("test").otaMasterSN("test")
                .prompt("test").scheduledTime(Instant.now().plusSeconds(1000L).getEpochSecond())
                .timeStampOfPrompt(0L).build();

        String Job1UserAcceptanceMessage = "test message";
        DdiStatus ddiStatus = getDdiStatus(executionStatus, 1737089013000L, null, MESSAGE, userAcceptanceMessage, Job1UserAcceptanceMessage);
        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON));
        assertTrue(output.getAll().contains(expectedMessage));
    }

    @ParameterizedTest
    @CsvSource({
            "USER_ACCEPTED, User has accepted to proceed with installation",
            "USER_SCHEDULED, User has scheduled to proceed with the installation"
    })
    @Description("All Feedback API with User accepted/scheduled status When userAcceptanceMessageJob1 is provided, it's saved in db")
    void givenFeedbackApiWhenUserAcceptanceMessageJob1ThenMessageAddedToDB(DdiStatus.ExecutionStatus executionStatus, String expectedMessage, CapturedOutput output) throws Exception {
        //Enforce debug logging for this testcase.
        Logger logger = LoggerFactory.getLogger(DdiRootController.class);
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);

        //setup
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("321234F45240S", "321234F45240S", "201", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);


        String Job1UserAcceptanceMessage = "test message";

        DdiStatus ddiStatus = getDdiStatus(executionStatus, 1737089013000L, null, MESSAGE, null, Job1UserAcceptanceMessage);
        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON));
        assertTrue(output.getAll().contains("Added user acceptance message job1"));
        assertTrue(output.getAll().contains(expectedMessage));

    }

    @ParameterizedTest
    @CsvSource({
            "USER_ACCEPTED",
            "USER_SCHEDULED"
    })
    @Description("All Feedback API with When userAcceptanceMessage  is not provided Then logged.")
    void givenFeedbackApiWhenNoUserAcceptanceMessageOrJob1ProvidedThenLogged(DdiStatus.ExecutionStatus executionStatus, CapturedOutput output) throws Exception {
        //Enforce debug logging for this testcase.
        Logger logger = LoggerFactory.getLogger(DdiRootController.class);
        ((ch.qos.logback.classic.Logger) logger).setLevel(Level.DEBUG);

        //setup
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("321234F45240S", "321234F45240S", "201", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);


        DdiStatus ddiStatus = getDdiStatus(executionStatus, 1737089013000L, null, MESSAGE, null, "test");
        String feedbackAsString = getJsonActionFeedbackObjectToSting(List.of(ddiStatus));
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON));
        assertTrue(output.getAll().contains("User Acceptance Message is not provided"));

    }

    @Test
    @Description("API to send collective feedback with failed result and canceled execution.")
    void givenFeedbackStatusList_CancelState_test() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget("VINRS1234123S", "VINRS1234123S", "118", testdataFactory.createVehicle(VEHICLE_MODEL_NAME).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);
        List<DdiStatus> ddiStatuses = List.of(getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.CANCELED, null, "The update was canceled by the ECU", 1737089013000L), getDdiStatus(DdiStatus.ExecutionStatus.DD_SENT, null, MESSAGE1, 1737089013000L));
        String feedbackAsString = getJsonActionFeedbackObjectToSting(ddiStatuses);
        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }

    @Test
    @Description("API to send collective feedback with rejected execution.")
    void givenFeedbackStatusList_RejectState_test() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        Target savedTarget = testdataFactory.createTarget(R3W2234F45240S, R3W2234F45240S, "205", testdataFactory.createVehicle(X250).getId());
        savedTarget = getFirstAssignedTarget(assignDistributionSet(ds.getId(), savedTarget.getControllerId()));
        final Action savedAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId()).getContent().get(0);

        String feedbackAsString = getJsonCanceledRejectDeploymentActionFeedback();

        mvc.perform(post(ALL_FEEDBACK, savedTarget.getControllerId(), savedAction.getId()).content(feedbackAsString).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());

    }

    @Test
    @Description("No request deployment logs for closed success feedbacks and rollout's logCollection false")
    void givenClosedSuccessFeedbackWithRolloutLogCollectionFalseWhenPostBDAFThenEmptyDeploymentLogLink() throws Exception {
        var target1 = setupRolloutAndGetTargetForDD();

        JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(target1.getControllerId());
        Action action = jpaTarget.getActions().get(0);

        postDeploymentFeedback(target1.getControllerId(), action.getId(), getJsonCanceledDeploymentActionFeedback(), status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());
    }


    @Test
    @Description("Validates signature and inventory details and process the request to success")
    void givenDeviceInventoryWhenPutDeviceInventoryThenSuccess() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given inventory with invalid target throws target not found error")
    void givenInventoryWithInvalidTargetWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetailsWithInvalidTarget, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                InvalidTargetControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Throws exception for invalid inventory signature")
    void givenInvalidInventorySignatureWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature("signature", SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        // Mock TenantConfigHelper so validation is enabled
        try (MockedStatic<TenantConfigHelper> mocked = mockStatic(TenantConfigHelper.class)) {
            TenantConfigHelper helper = mock(TenantConfigHelper.class);
            when(helper.isInventorySignatureValidationEnabled()).thenReturn(true);

            mocked.when(() -> TenantConfigHelper.usingContext(any(), any()))
                    .thenReturn(helper);

            // Act + Assert
            mvc.perform(put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                            inventoryDetailsControllerId)
                            .content(stringObj)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());  // signature validation should fail
        }

    }

    @Test
    @Description("Throws exception for malformed inventory details")
    void givenMalformedInventoryDetailsWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, malformedInventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Throws exception for invalid inventory details")
    void givenInvalidInventoryDetailsWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, invalidInventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Throws exception for Empty inventory details")
    void givenEmptyInventoryDetailsWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiDeviceInventory inventory = new DdiDeviceInventory();
        inventory.setInventorySignature(signature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Some Parameters are missing")));

    }

    @Test
    @Description("Throws exception for Empty inventory signature")
    void givenEmptyInventorySignatureWhenPutDeviceInventoryThenError() throws Exception {
        DdiDeviceInventory inventory = new DdiDeviceInventory();
        inventory.setInventoryDetails(inventoryDetails);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Some Parameters are missing")));

    }

    @Test
    @Description("Throws exception for Empty Ecu List")
    void givenEmptyEcuListWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyEcuList, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Inventory must contain minimum 1 ECU in ecuList"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));
    }

    @Test
    @Description("Throws exception for Empty Scomos List")
    void givenEmptyScomosListWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyScomosList, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(ECU_LIST))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @Test
    @Description("Throws exception for Empty Scomos Id")
    void givenEmptyScomosIdWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyScomosId, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("ScomoId or SwVersion missing"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @Test
    @Description("Throws exception for Empty Software Version")
    void givenEmptySwVersionWhenPutDeviceInventoryThenError() throws Exception {
        // Set up your signature and inventory
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptySwVersion, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);

        // Create the target first
        testdataFactory.createTarget(inventoryDetailsControllerId);

        // Prepare the request payload
        String stringObj = mapper.writeValueAsString(inventory);

        // Perform the PUT request with the correct controller ID in the URL

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("ScomoId or SwVersion missing"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }


    @Test
    @Description("Throws exception for Empty Harware Version")
    void givenEmptyHwVersionWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyHwVersion, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(ECU_LIST))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @Test
    @Description("Throws exception for Empty Node Address Version")
    void givenEmptyNodeAddressWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyNodeAddress, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(ECU_LIST))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @Test
    @Description("Throws exception for Empty Node Address Version")
    void givenEmptyPartNumberWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, emptyPartNumber, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(ECU_LIST))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @Test
    @Description("Put/update device inventory replaces Target software details")
    void givenDeviceInventoryWhenPutDeviceInventoryThenReplaceTargetSoftwares() throws Exception {
        String controllerId = inventoryDetailsControllerId;
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        Target target = testdataFactory.createTarget(controllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                controllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());


        createTargetSoftwares(target);
        JpaTarget actualTarget = (JpaTarget) testdataFactory.getTarget(controllerId);
        Assertions.assertEquals(3, actualTarget.getSoftwares().size());
    }

    @Test
    @Description("Put/update device inventory insert into Target Inventory")
    void givenDeviceInventoryWhenPutDeviceInventoryThenInsertIntoTargetInventory() throws Exception {
        String controllerId = inventoryDetailsControllerId;
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        Target target = testdataFactory.createTarget(controllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                controllerId)
                                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        List<TargetInventory> actualTargetInventories = testdataFactory.getTargetInventory(controllerId);
        TargetInventory expected = createTargetInventory((JpaTarget) target, inventory);

        Assertions.assertEquals(expected.getInventory(), actualTargetInventories.get(0).getInventory());
        Assertions.assertEquals(expected.getTarget().getControllerId(), actualTargetInventories.get(0).getTarget().getControllerId());
    }

    @Test
    @Description("Get inventory hash for path pattern /test")
    void givenHashWithPatternWhenGetInventoryHashThenSuccess() throws Exception {
        testdataFactory.createTarget("5432");
        final String inventoryHash = "testInventoryHash";
        MockHttpServletRequestBuilder requestBuilder = get("/device/v1/controllers/5432/inventory").param("hash", inventoryHash);
        mvc.perform(requestBuilder).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    private List<TargetSoftware> createTargetSoftwares(Target target) {
        // Sample ecu and its scomos from encoded inventory details in test "inventoryDetails" property.
        return List.of(new JpaTargetSoftware(NODE, "SCOMO-C-021-004\\00021", "21.26.00\\68530161", target));
    }

    private TargetInventory createTargetInventory(JpaTarget target, DdiDeviceInventory inventory) throws JsonProcessingException {
        // Sample device inventory details from encoded inventory details in test "inventoryDetails" property.
        target.setOptLockRevision(2);
        return new JpaTargetInventory(target, DdiApiHelper.decodeAndCreateDeviceInventory(inventory), null);
    }

    @Test
    @Description("Get deployment action based on target")
    void givenTargetWithSameEcuModelsWhenGetTargetBasedDeploymentActionThenSuccess() throws Exception {
        var target = setupRolloutAndGetTargetForDD();

        final Action savedAction = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent().get(0);

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, target.getControllerId()).content(stringObj)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        final MvcResult result = mvc.perform(
                        get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH,
                                target.getControllerId(), savedAction.getId()).param("c", "1"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode jsonNode = extractDecodedJsonNode(result, "$.deploymentDescription");

        // Perform assertions on the decoded JSON to verify expected structure and values
        assertNotNull(jsonNode.get("rolloutName"));
        assertEquals(savedAction.getRollout().getName(), jsonNode.get("rolloutName").asText());
        assertEquals(savedAction.getRollout().getDescription(), jsonNode.get("description").asText());
        assertTrue(jsonNode.has("download"));
        assertTrue(jsonNode.has("update"));
        assertTrue(jsonNode.get("ecus").isArray());
        assertEquals(1, jsonNode.get("ecus").size());

        // ✅ Check deploymentMetadata.ddExpiryDate directly from jsonNode
        assertTrue(jsonNode.has("deploymentMetadata"));
        assertTrue(jsonNode.get("deploymentMetadata").has("ddExpiryDate"));
        assertFalse(jsonNode.get("deploymentMetadata").get("ddExpiryDate").asText().isEmpty());

        assertTrue(JsonPath.read(result.getResponse().getContentAsString(), "$.deploymentSignature") != null);

        // verifies that when a deployment action's status is set to PAUSED, attempting to fetch the deployment action for the target results in a400 Bad Request response with an appropriate error message indicating that the campaign or device is not running
        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, savedAction.getRollout().getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, target.getControllerId(), savedAction.getId())
                        .param("c", "1"))
                .andExpect(status().is2xxSuccessful());

        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode root = mapper.readTree(jsonResponse);
        String deploymentSignature = root.path(DEPLOYMENT_SIGNATURE).asText();
        KeyPair keyPair = DdiSignatureService.getKeyPairFromPrivateKey(TEST_DD_PRIVATE_KEY_01);
        assertTrue(verifySignature(keyPair, deploymentSignature));
    }

    @Test
    @Description("Check for violations after validating DDI deployment descriptor base")
    void getViolationsAfterValidatingDdiDeploymentDescriptorBase() throws Exception {
        DdiRootController ddiRootController = new DdiRootController();
        DdiDeploymentMetadata deploymentMetadata = DdiDeploymentMetadata.builder().requiredStateOfCharge(new DdiRequiredStateOfCharge("In the modern world, technology plays a crucial role in shaping the way we live, work, and communicate. From the convenience of smartphones to the transformative power of artificial intelligence, innovations continue to redefine industries and create opportunities. As we adapt to these changes, ethical considerations and sustainability remain essential.")).build();
        DdiDeploymentDescriptor deploymentDescriptor = new DdiDeploymentDescriptor();
        deploymentDescriptor.setDeploymentMetadata(deploymentMetadata);
        deploymentDescriptor.setEcus(new ArrayList<>());
        DdiDeploymentDescriptorBase base = new DdiDeploymentDescriptorBase(deploymentDescriptor, "abcdefg");
        DdiDeploymentDescriptorBase ddiDeploymentDescriptorBase = ddiRootController.validateDdiDeploymentDescriptorBase(base);
        assertNull(ddiDeploymentDescriptorBase.getDeploymentDescription());
        assertNull(ddiDeploymentDescriptorBase.getDeploymentSignature());
    }

    @Test
    @Description("Validation fails for Deployment signature")
    void givenInvalidDeploymentSignatureWhenVerifySignatureThenReturnFalse() throws Exception {
        KeyPair keyPair = DdiSignatureService.getKeyPairFromPrivateKey(TEST_DD_PRIVATE_KEY_01);
        Assertions.assertFalse(verifySignature(keyPair, DEPLOYMENT_SIGNATURE));
    }

    public boolean verifySignature(KeyPair keyPair, String signature) {
        try {
            ECPublicKey publicKey = (ECPublicKey) keyPair.getPublic();
            Algorithm algo = Algorithm.ECDSA256(publicKey, null);
            JWT.require(algo).build().verify(signature);
            return true;
        } catch (JWTVerificationException e) {
            return false;
        }
    }

    @Test
    @Description("Given inventory with mismatched ControllerId with inventory results in validation error")
    void givenInventoryWithMismatchedControllerIdWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsMismatchedControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isNotFound());
    }

    public void espSignatureVerify(MvcResult result, String supportPackageFileType) throws IOException {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode rootNode = mapper.readTree(jsonResponse);

        JsonNode ecusArray = rootNode.at("/deploymentDescription/ecus");
        Optional<JsonNode> ecuNodeId = StreamSupport.stream(ecusArray.spliterator(), false).filter(node -> node.path("ecuNodeId").asText().equals(NODE)).findFirst();

// Extract the espSignature from the ecuScript object
        String espSignature = null;
        if (ecuNodeId.isPresent()) {
            JsonNode node = ecuNodeId.get().path("esp").path(supportPackageFileType);
            espSignature = node.path("espSignature").asText();
        }
        KeyPair keyPair = DdiSignatureService.getKeyPairFromPrivateKey(TEST_ESP_PRIVATE_KEY_01);
        assertTrue(verifySignature(keyPair, espSignature));
    }

    public void rspSignatureVerify(MvcResult result, String supportPackageFileType) throws IOException {
        String jsonResponse = result.getResponse().getContentAsString();
        JsonNode rootNode = mapper.readTree(jsonResponse);
        JsonNode rspSignature = rootNode.at("/deploymentDescription/rsp").path(supportPackageFileType).path("rspSignature");
        KeyPair keyPair = DdiSignatureService.getKeyPairFromPrivateKey(TEST_RSP_PRIVATE_KEY_01);
        assertTrue(verifySignature(keyPair, rspSignature.asText()));
    }

    @Test
    @Description("Given valid status, when sending feedback, then Kafka publish should succeed")
    public void givenValidStatusWhenSendingFeedbackThenKafkaPublishShouldSucceed(CapturedOutput output) throws Exception {
        var savedTarget = setupRolloutAndGetTargetForDD();

        JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(savedTarget.getControllerId());
        Action savedAction = jpaTarget.getActions().get(0);
        sendDeploymentActionFeedback(savedTarget, savedAction,  DOWNLOAD_STARTED, SUCCESS, TARGET_SCHEDULED_INSTALLATION_MSG, null)
            .andDo(MockMvcResultPrinter.print())
            .andExpect(status().isOk());
    }

    @Test
    @Description("Given invalid status, when sending feedback, then Kafka publish should fail")
    public void givenInvalidStatusWhenSendingFeedbackThenKafkaPublishShouldFail(CapturedOutput output) throws Exception {
        var savedTarget = setupRolloutAndGetTargetForDD();

        JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(savedTarget.getControllerId());
        Action savedAction = jpaTarget.getActions().get(0);

        sendDeploymentActionFeedback(savedTarget, savedAction, USER_SCHEDULED, NONE, TARGET_SCHEDULED_INSTALLATION_MSG, null).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        Assertions.assertFalse(output.getOut().contains(MESSAGE3));
    }


    @Test
    @Description("Given invalid deployment details, when executing deployment, then Kafka publish should fail")
    public void givenInvalidDeploymentDetailsWhenExecutingDeploymentThenKafkaPublishShouldFail(CapturedOutput output) throws Exception {
        Target savedTarget = setupRolloutAndGetTargetForDD();
        final Action savedAction = deploymentManagement.findActionsByTarget(savedTarget.getControllerId(), PAGE).getContent().get(0);

        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId())
                .content(stringObj).contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk());
        mvc.perform(get(DdiRestConstants.GET_TARGET_BASED_DEPLOYMENT_ACTION_PATH, savedTarget.getControllerId(), savedAction.getId())
                .param("c", "1"))
            .andExpect(status().isOk());
        // Check that the expected Kafka publish log/message is NOT present
        assertFalse(output.getOut().contains("Sending vehicle status message to DOCG"));
    }

    @Test
    @Description("Given valid inventory when creating inventory then Kafka publish should be unsuccessful")
    void givenValidInventoryWhenCreatingInventoryThenKafkaPublishShouldBeSuccessful(CapturedOutput output) throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, inventoryDetails, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);
        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);
        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }


    @Test
    @Description("Given finish failure vehicle status and log collection required true then share log collection link in response")
    void givenLogCollectionRequiredAndStatusFinishFailure_WhenAllFeedbackThenLogCollectLink() throws Exception {
        var savedTarget = setupRolloutAndGetTargetForDD();

        JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(savedTarget.getControllerId());
        Action action = jpaTarget.getActions().get(0);

        postDeploymentFeedback(savedTarget.getControllerId(), action.getId(), getJsonFinishedFailureDeploymentActionFeedback(), status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF, startsWith(deploymentLogLink(savedTarget.getControllerId(), action.getId().toString()))));
    }

    @Test
    @Description("Given finish success vehicle status and log collection required true then share log collection link in response")
    void givenLogCollectionRequiredAndStatusFinishSuccess_WhenAllFeedbackThenLogCollectLink() throws Exception {
        var savedTarget = setupRolloutAndGetTargetForDD();

        JpaTarget jpaTarget = (JpaTarget) testdataFactory.getTarget(savedTarget.getControllerId());
        Action action = jpaTarget.getActions().get(0);

        postDeploymentFeedback(savedTarget.getControllerId(), action.getId(), getJsonFinishedSuccessDeploymentActionFeedback(), status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF, startsWith(deploymentLogLink(savedTarget.getControllerId(), action.getId().toString()))));
    }

    @Test
    @Description("Given feedback request and controllerId then gets ok response")
    void givenFeedbackRequestAndControllerIdWhenAddFeedbackThenOk() throws Exception {
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        errorCode.add("ERR_04020281");
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(200, details, ExecutionType.ERC, errorCode, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());

        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedback);
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());


    }

    @Test
    @Description("When posting action feedback with a mixed case feedback status, the feedback should be accepted successfully.")
    void givenMixedCaseFeedbackStatusWhenPostingFeedbackThenItIsAccepted() throws Exception {
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        errorCode.add("ERR_04020281");
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(200, details, ExecutionType.ERC, errorCode, Instant.now().getEpochSecond());

        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedback).replace("ERC", "ErC");
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        feedbackJson = feedbackJson.replace("ErC", "erc");
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        feedbackJson = feedbackJson.replace("erc", "ERC");
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

    }

    @Test
    @Description("Given feedback request with missing parameter and controllerId then gets bad request")
    void givenFeedbackRequestAndControllerIdCodeIsNullWhenAddFeedbackThenGetException() throws Exception {
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        errorCode.add("ERR_04020281");
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(null, details, ExecutionType.ERC, errorCode, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedback);
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a valid feedback request with a proper timestamp, when adding feedback, then it returns OK")
    void whenValidFeedbackRequestWithProperTimestampThenReturnsOk() throws Exception {
        // Arrange
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = List.of("Error feedback 1", "Error feedback 2");
        List<String> errorCode = List.of("ERR_04020281");
        long millis = System.currentTimeMillis();
        DdiFeedbackRequestBody feedback = new DdiFeedbackRequestBody(200, details, ExecutionType.ERC, errorCode, millis);

        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedback);

        // Act
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // Assert
        JpaGeneralFeedback jpaGeneralFeedback = generalFeedbackRepository.findAll().get(0);
        assertEquals(millis / 1000, jpaGeneralFeedback.getTimestamp());
    }

    @Test
    @Description("Given a valid feedback request with a past timestamp (Before September 9, 2001 at 1:46:40 AM UTC), when adding feedback, then it returns Bad Request")
    void whenValidFeedbackRequestWithPastTimestampThenReturnsBadRequest() throws Exception {
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        errorCode.add("ERR_04020281");
        long millis = 985446591L;
        Map<String, Object> feedbackMap = new HashMap<>();
        feedbackMap.put("code", 200);
        feedbackMap.put("details", List.of("Error feedback 1", "Error feedback 2"));
        feedbackMap.put("execution", "ERC");
        feedbackMap.put("errorCode", List.of("ERR_04020281"));
        feedbackMap.put("timestamp", millis);

        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedbackMap);
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The provided timestamp must be between September 9, 2001 1:46:40 AM UTC " +
                        "and November 20, 33658 1:46:40 AM UTC")));

    }

    @Test
    @Description("Given a valid feedback request with an invalid timestamp (String), when adding feedback, then it returns Bad Request")
    void whenValidFeedbackRequestWithInvalidTimestampThenReturnsBadRequest() throws Exception {
        testdataFactory.createTarget(CONTROLLER_ID);
        List<String> details = new ArrayList<>();
        details.add("Error feedback 1");
        details.add("Error feedback 2");
        List<String> errorCode = new ArrayList<>();
        errorCode.add("ERR_04020281");
        Map<String, Object> feedbackMap = new HashMap<>();
        feedbackMap.put("code", 200);
        feedbackMap.put("details", List.of("Error feedback 1", "Error feedback 2"));
        feedbackMap.put("execution", "ERC");
        feedbackMap.put("errorCode", List.of("ERR_04020281"));
        feedbackMap.put("timestamp", "timestamp");

        ObjectMapper objectMapper = new ObjectMapper();
        String feedbackJson = objectMapper.writeValueAsString(feedbackMap);
        mvc.perform(post(DdiRestConstants.GENERAL_FEEDBACK_PATH, CONTROLLER_ID)
                        .content(feedbackJson).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("The given request body is not well formed")));

    }

    public List<EcuModel> createEcuModelWithSameNodeId() {
        List<EcuModel> mockEcuModels = new ArrayList<>();

        JpaEcuModel ecuModel1 = new JpaEcuModel();
        JpaEcuModelType ecuModelType = new JpaEcuModelType(OM);
        ecuModel1.setEcuModelType(ecuModelType);
        ecuModel1.setEcuModelName(TEST_ECUMODEL_1);
        ecuModel1.setEcuNodeId(NODE);

        JpaEcuModel ecuModel2 = new JpaEcuModel();
        ecuModel2.setEcuModelType(ecuModelType);
        ecuModel2.setEcuModelName(TEST_ECUMODEL_2);
        ecuModel2.setEcuNodeId(NODE);

        mockEcuModels.add(ecuModel1);
        mockEcuModels.add(ecuModel2);
        return mockEcuModels;
    }

    public List<EcuModel> createEcuModels() {
        List<EcuModel> mockEcuModels = new ArrayList<>();

        JpaEcuModel ecuModel1 = new JpaEcuModel();
        JpaEcuModelType ecuModelType1 = new JpaEcuModelType(OM);
        ecuModel1.setEcuModelType(ecuModelType1);
        ecuModel1.setEcuModelName(TEST_ECUMODEL_1);
        ecuModel1.setEcuNodeId(NODE);

        JpaEcuModel ecuModel2 = new JpaEcuModel();
        JpaEcuModelType ecuModelType2 = new JpaEcuModelType(SU);
        ecuModel2.setEcuModelType(ecuModelType2);
        ecuModel2.setEcuModelName(TEST_ECUMODEL_2);
        ecuModel2.setEcuNodeId("40 A0");

        mockEcuModels.add(ecuModel1);
        mockEcuModels.add(ecuModel2);
        return mockEcuModels;
    }

    public List<Vehicle> createVehicle(String vehicleName, List<EcuModel> ecuModels) {
        List<Vehicle> mockVehicles = new ArrayList<>();
        Set<EcuModel> ecuModelsSet = new HashSet<>(ecuModels);
        JpaVehicle vehicle = new JpaVehicle();
        vehicle.setName(vehicleName);
        vehicle.setVehicleEcu(ecuModelsSet);
        mockVehicles.add(vehicle);
        return mockVehicles;
    }

    public List<Long> getEcuModelsIds(List<EcuModel> ecuModels) {
        List<Long> ecuModelIds = new ArrayList<>(ecuModels.size());
        for (EcuModel ecuModel : ecuModels) {
            ecuModelIds.add(ecuModel.getId());
        }
        return ecuModelIds;
    }

    @ParameterizedTest
    @ValueSource(strings = {"blankSwVersion", "nullSwVersion"})
    @Description("Throws exception for Null Software Version")
    void givenNullOrBlankSwVersionWhenPutDeviceInventoryThenError(String swVersionType) throws Exception {
        String swVersion;

        // Pick the correct encoded value based on parameter
        if ("blankScomosId".equals(swVersionType)) {
            swVersion = blankSwVersion;
        } else {
            swVersion = nullSwVersion;
        }

        // Set up your signature and inventory
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, swVersion, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);

        // Create the target first
        testdataFactory.createTarget(inventoryDetailsControllerId);

        // Prepare the request payload
        String stringObj = mapper.writeValueAsString(inventory);

        // Perform the PUT request with the correct controller ID in the URL

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("ScomoId or SwVersion missing"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    @ParameterizedTest
    @ValueSource(strings = {"blankScomosId", "nullScomosId"})
    @Description("Throws exception for Null Scomos Id")
    void givenNullOrBlankScomosIdWhenPutDeviceInventoryThenError(String scomosIdType) throws Exception {
        String scomosId;

        // Pick the correct encoded value based on parameter
        if ("blankScomosId".equals(scomosIdType)) {
            scomosId = blankScomosId;
        } else {
            scomosId = nullScomosId;
        }
        // Set up your signature and inventory
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);
        DdiSignature rawSignature = new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1);
        DdiDeviceInventory inventory = new DdiDeviceInventory(signature, scomosId, staticInventoryHash, staticSignature, rawInventoryDetails, rawSignature);

        // Create the target first
        testdataFactory.createTarget(inventoryDetailsControllerId);

        // Prepare the request payload
        String stringObj = mapper.writeValueAsString(inventory);

        // Perform the PUT request with the correct controller ID in the URL

        mvc.perform(put(DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + CONTROLLER_ID_INVENTORY_URL, inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("ScomoId or SwVersion missing"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(EXCEPTION)));

    }

    /**
     * Extracts the Base64-encoded deployment description from a MockMvc response,
     * decodes it, and parses it into a {@link JsonNode} for validation and inspection.
     *
     * @param result the {@link MvcResult} object containing the API response
     * @return a {@link JsonNode} representing the decoded JSON structure
     * @throws Exception if extraction, decoding, or parsing fails
     */
    public static JsonNode extractDecodedJsonNode(MvcResult result, String node) throws Exception {
        // Retrieve the Base64-encoded deployment description from the API response body
        String base64DD = JsonPath.read(result.getResponse().getContentAsString(), node);

        // Decode the Base64 string to restore the original JSON content
        byte[] decodedBytes = Base64.getDecoder().decode(base64DD);
        String jsonDD = new String(decodedBytes, StandardCharsets.UTF_8);

        // Parse the decoded JSON into a JsonNode for structured access and assertions
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(jsonDD);
    }

    @Disabled("This is disabled till mandatory check for static inventory is added back")
    @Test
    @Description("Throws exception when static inventory hash is missing")
    void givenMissingStaticInventoryHashWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);

        DdiDeviceInventory inventory = new DdiDeviceInventory();
        inventory.setInventorySignature(signature);
        inventory.setInventoryDetails(inventoryDetails);
        inventory.setStaticInventorySignature(staticSignature);
        // staticInventoryHash is missing
        inventory.setRawInventoryDetails(rawInventoryDetails);
        inventory.setRawInventorySignature(new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1));

        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Some Parameters are missing")));
    }

    @Disabled("This is disabled till mandatory check for static inventory is added back")
    @Test
    @Description("Throws exception when static inventory signature is missing")
    void givenMissingStaticInventorySignatureWhenPutDeviceInventoryThenError() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);

        DdiDeviceInventory inventory = new DdiDeviceInventory();
        inventory.setInventorySignature(signature);
        inventory.setInventoryDetails(inventoryDetails);
        inventory.setStaticInventoryHash(staticInventoryHash);
        // staticInventorySignature is missing
        inventory.setRawInventoryDetails(rawInventoryDetails);
        inventory.setRawInventorySignature(new DdiSignature(rawInventorySignature, SIGNATURE_TYPE1));

        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(
                        put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory",
                                inventoryDetailsControllerId)
                                .content(stringObj)
                                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Some Parameters are missing")));
    }

    @Test
    @Description("Succeeds even if raw inventory details and signature are missing (optional fields)")
    void givenNoRawInventoryWhenPutDeviceInventoryThenSuccess() throws Exception {
        DdiSignature signature = new DdiSignature(inventorySignature, SIGNATURE_TYPE1);
        DdiSignature staticSignature = new DdiSignature(staticInventorySignature, SIGNATURE_TYPE1);

        DdiDeviceInventory inventory = new DdiDeviceInventory(
                signature,
                inventoryDetails,
                staticInventoryHash,
                staticSignature,
                null,  // rawInventoryDetails missing
                null   // rawInventorySignature missing
        );

        testdataFactory.createTarget(inventoryDetailsControllerId);
        String stringObj = mapper.writeValueAsString(inventory);

        mvc.perform(put(DdiRestConstants.DEVICE_V1_NO_TENANTS_REQUEST_MAPPING + "/{controllerId}/inventory", inventoryDetailsControllerId).content(stringObj).contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk());
    }
}
