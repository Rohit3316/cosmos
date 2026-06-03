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
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.assertj.core.api.Condition;
import java.util.concurrent.CompletableFuture;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiPackage;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.sns.models.CdnDeleteRequest;
import org.cosmos.sns.models.CdnUploadRequest;
import org.cosmos.sns.models.S3FileTransferRequest;
import org.cosmos.sns.services.impl.CdnDeleteSnsService;
import org.cosmos.sns.services.impl.CdnUploadSnsService;
import org.cosmos.sns.services.impl.S3FileTransferSnsService;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.MgmtS3Service;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.hateoas.MediaTypes;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import static org.hamcrest.core.IsNull.notNullValue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Test deployment base from the controller.
 */
/**
 * @deprecated This test case is currently deprecated as the feature is currently deprecated and will be enabled once the feature is enabled.
 */
@Feature("Component Tests - Direct Device Integration API")
@Story("Deployment Action Resource")
@Disabled
@Deprecated
public class DdiDeploymentBaseTest extends AbstractDDiApiIntegrationTest {

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
    private static final String DS_METADATA_VALUE = "dsMetadataValue";
    private static final String FORCED = "forced";
    private static final String DS_METADATA_KEY = "dsMetadataKey";
    private static final String DEFAULT_CONTROLLER_ID = "4712";
    private static final String TIME = "00:01:00";
    private static final String JSON_PATH_CONFIG_POLLING_SLEEP = "$.config.polling.sleep";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF = "$._links.deploymentBase.href";
    private static final String JSON_PATH_LINKS_LOG_COLLECTION_HREF = "$._links.logCollection.href";
    private static final String WITH_VALUE = "withValue";
    private static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    private static final String SP_ARTIFACTS = "sp_artifacts";
    private static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    private static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    private static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    private static final String SP_ECU_MODEL = "sp_ecu_model";
    private static final String SP_TARGET = "sp_target";
    private static final String SP_ACTION = "sp_action";
    private static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    private static final String META_DATA_VISIBLE = "metaDataVisible";
    private static final String ATTEMPT = "attempt";
    private static final String TEST1 = "test1";
    private static final String TEST11 = "TEST11";
    private static final String TEST13 = "TEST13";
    private static final String VALUE_5120 = "5120";
    private static final String VALUE_4713 = "4713";
    private static final Long SIZE = 123L;
    private static final String SHA_256 = "SHA_256";
    private static final String DESCRIPTION = "description";
    private static final String STATUS_TEST_01 = "StatusTest01";
    private static final String STATUS_TEST_02 = "StatusTest02";
    private static final List<String> ERROR_CODE = Arrays.asList("ERR_XXXXXXXXX", "ERR_XXXXXXXX01");

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    private static void mockPublishVehicleStatus() {
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_VEHICLE_STATUS_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_INVENTORY_ENDPOINT)).respond(HttpResponse.response().withStatusCode(200));
    }

    @BeforeEach
    public void setup() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_ECU_MODEL,
                SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_ECU_MODEL,
                SP_TARGET, SP_ACTION, SP_DISTRIBUTION_SET
        );
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
    @Description("Ensure that the deployment resource is available as CBOR")
    public void deploymentResourceCbor() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        assignDistributionSet(distributionSet.getId(), target.getName());
        Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        SoftwareModule sm1 = action.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = action.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = action.getDistributionSet().getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(distributionSet), TEST11);

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifactSignature = testdataFactory.createArtifactsWithExpiryDate("test1.signature", FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        associationList.add(association3);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        // get deployment base
        performGet(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), status().isOk(),
                target.getControllerId(), action.getId().toString());

        var target1 = setupRolloutAndGetTarget();
        final Action savedAction = deploymentManagement.findActionsByTarget(target1.getControllerId(), PAGE).getContent().get(0);

        final byte[] feedback = jsonToCbor(getJsonCanceledCancelActionFeedback());
              postDeploymentFeedback(MediaType.parseMediaType(DdiRestConstants.MEDIA_TYPE_CBOR), target1.getControllerId(),
                      savedAction.getId(), feedback, status().isOk());
    }

    @Test
    @Description("Ensures that artifacts are found, when software module exists.")
    public void artifactsExists() throws Exception {
        final Target target = testdataFactory.createTarget();
        final DistributionSet distributionSet = testdataFactory.createDistributionSet("");

        SoftwareModule sm1 = distributionSet.getModules().stream().toList().get(0);
        SoftwareModule sm2 = distributionSet.getModules().stream().toList().get(1);
        SoftwareModule sm3 = distributionSet.getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(distributionSet), TEST13);

        Artifacts artifact3= testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);


        assignDistributionSet(distributionSet.getId(), target.getName());
    }

    @Test
    @Description("Forced deployment to a controller. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentForceAction() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true, false);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true, false);
        final Version version = testdataFactory.createVersion(getOsModule(ds), TEST11);
        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifactSignature = testdataFactory.createArtifactsWithExpiryDate("test1.signature", FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> targetsAssignedToDs = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                MgmtRolloutUserAcceptanceRequired.NO).getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);

        assignDistributionSet(ds2, targetsAssignedToDs).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test
        final long current = Instant.now().getEpochSecond();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(),
                DEFAULT_CONTROLLER_ID).andExpect(jsonPath(JSON_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF,
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(Instant.now().getEpochSecond());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        SoftwareModule sm1 = action.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = action.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = action.getDistributionSet().getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(findDistributionSetByAction), TEST13);

        Artifacts artifact3= testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);
        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), FORCED, FORCED);

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId());
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(DeviceActionStatus.DD_SENT);
    }

    @Test
    @Description("Attempt/soft deployment to a controller. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentAttemptAction() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true, false);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true, false);
        final String visibleMetadataOsKey = META_DATA_VISIBLE;
        final String visibleMetadataOsValue = WITH_VALUE;
        final Version version = testdataFactory.createVersion(getOsModule(ds), TEST11);

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifactSignature = testdataFactory.createArtifactsWithExpiryDate("test1.signature", FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key(visibleMetadataOsKey).value(visibleMetadataOsValue).targetVisible(true));
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(getOsModule(ds))
                .key("metaDataNotVisible").value(WITH_VALUE).targetVisible(false));

        createDistributionSetMetadata(ds.getId(), entityFactory.generateDsMetadata(DS_METADATA_KEY, DS_METADATA_VALUE));

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(), MgmtRolloutUserAcceptanceRequired.YES)
                .getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionsAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        SoftwareModule sm1 = action.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = action.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = action.getDistributionSet().getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(ds), TEST13);


        Artifacts artifact3= testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);
        assertThat(uaction.getDistributionSet().getName()).isEqualTo(ds.getName());
        assertThat(uaction.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(uaction.getDistributionSet().getOptLockRevision()).isEqualTo(2);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = Instant.now().getEpochSecond();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(),
                DEFAULT_CONTROLLER_ID).andExpect(jsonPath(JSON_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF,
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(Instant.now().getEpochSecond());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();



        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, visibleMetadataOsKey,
                visibleMetadataOsValue, artifact, artifactSignature, action.getId(), ATTEMPT, ATTEMPT,
                getOsModule(findDistributionSetByAction), DS_METADATA_KEY , DS_METADATA_VALUE);

        // Retrieved is reported
        final List<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(DeviceActionStatus.DD_SENT);
    }

    @Test
    @Description("Attempt/soft deployment to a controller including automated switch to hard. Checks if the resource response payload for a given deployment is as expected.")
    public void deploymentAutoForceAction() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        // Prepare test data
        final DistributionSet ds = testdataFactory.createDistributionSet("", true, false);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true, false);

        Artifacts artifact = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifactSignature = testdataFactory.createArtifactsWithExpiryDate("test1.signature", FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        final Version version = testdataFactory.createVersion(getOsModule(ds), TEST11);
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact).softwareModule((JpaSoftwareModule) ds.findFirstModuleByType(osType).orElseThrow(NoSuchElementException::new)).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        final Target savedTarget = createTargetAndAssertNoActiveActions();

        final List<Target> saved = assignDistributionSet(ds.getId(), savedTarget.getControllerId(),
                MgmtRolloutUserAcceptanceRequired.NO).getAssignedEntity().stream().map(Action::getTarget).collect(Collectors.toList());
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(1);

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        SoftwareModule sm1 = action.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = action.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = action.getDistributionSet().getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(ds), TEST13);

        Artifacts artifact3= testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        assertThat(deploymentManagement.countActionsAll()).isEqualTo(1);
        assignDistributionSet(ds2, saved).getAssignedEntity();
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final Action uaction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        assertThat(uaction.getDistributionSet()).isEqualTo(ds);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).hasSize(2);

        // Run test

        final long current = Instant.now().getEpochSecond();
        performGet(CONTROLLER_BASE, MediaTypes.HAL_JSON, status().isOk(),
                DEFAULT_CONTROLLER_ID).andExpect(jsonPath(JSON_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF,
                        startsWith(deploymentBaseLink(DEFAULT_CONTROLLER_ID, uaction.getId().toString()))));
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isGreaterThanOrEqualTo(current);
        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getLastTargetQuery())
                .isLessThanOrEqualTo(Instant.now().getEpochSecond());
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(2);

        final DistributionSet findDistributionSetByAction = distributionSetManagement.getByAction(action.getId()).get();

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaType.APPLICATION_JSON, ds, artifact,
                artifactSignature, action.getId(),
                findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), FORCED, FORCED);

        getAndVerifyDeploymentBasePayload(DEFAULT_CONTROLLER_ID, MediaTypes.HAL_JSON, ds, artifact, artifactSignature,
                action.getId(), findDistributionSetByAction.findFirstModuleByType(osType).get().getId(), FORCED, FORCED);

        // Retrieved is reported
        final Iterable<ActionStatus> actionStatusMessages = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 100, Direction.DESC, "id"), uaction.getId()).getContent();
        assertThat(actionStatusMessages).hasSize(2);
        final ActionStatus actionStatusMessage = actionStatusMessages.iterator().next();
        assertThat(actionStatusMessage.getStatus()).isEqualTo(DeviceActionStatus.DD_SENT);
    }

    private void getAndVerifyDeploymentBasePayload(final String controllerId, final MediaType mediaType,
                                                   final DistributionSet ds, final String visibleMetadataOsKey, final String visibleMetadataOsValue,
                                                   final Artifacts artifact, final Artifacts artifactSignature, final Long actionId, final String downloadType,
                                                   final String updateType, final Long osModuleId, final String dsMetadataKey, String dsMetadataValue) throws Exception {


        getAndVerifyDeploymentBasePayload(controllerId, mediaType, ds, artifact, artifactSignature, actionId,
                osModuleId, downloadType, updateType)
                .andExpect(jsonPath("$.deployment.chunks[?(@.part=='os')].chunkMetadata.*", notNullValue()))
                .andExpect(jsonPath("$.deployment.ddiMetadata.dsMetadataKey")
                        .value(dsMetadataValue));
    }

    @Test
    @Description("Test various invalid access attempts to the deployment resource und the expected behaviour of the server.")
    public void badDeploymentAction() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        // not allowed methods
        mvc.perform(post(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, DEFAULT_CONTROLLER_ID, "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        // non existing target
        mvc.perform(MockMvcRequestBuilders.get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH,  "not-existing", "1"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // no deployment
        mvc.perform(
                        MockMvcRequestBuilders.get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH,  DEFAULT_CONTROLLER_ID, "1"))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // wrong media type
        final List<Target> toAssign = Collections.singletonList(target);
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");

        final Long actionId = getFirstAssignedActionId(assignDistributionSet(savedSet, toAssign));

        final Action action = deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())
                .getContent().get(0);

        SoftwareModule sm1 = action.getDistributionSet().getModules().stream().toList().get(0);
        SoftwareModule sm2 = action.getDistributionSet().getModules().stream().toList().get(1);
        SoftwareModule sm3 = action.getDistributionSet().getModules().stream().toList().get(2);
        final Version version1 = testdataFactory.createVersion(getOsModule(savedSet), TEST13);

        Artifacts artifact3= testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact4 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact5 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, VALUE_5120, SHA_256, SIZE, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association3 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact3).softwareModule((JpaSoftwareModule) sm1).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association4 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact4).softwareModule((JpaSoftwareModule) sm2).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        JpaArtifactSoftwareModuleAssociationEntity association5 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact5).softwareModule((JpaSoftwareModule) sm3).sourceVersion((JpaVersion) version1).targetVersion((JpaVersion) version1).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association3);
        associationList2.add(association4);
        associationList2.add(association5);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        mvc.perform(MockMvcRequestBuilders.get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH, DEFAULT_CONTROLLER_ID,
        actionId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders
                        .get(DdiRestConstants.CONTROLLER_BASE_DEPLOYMENT_ACTION_PATH,  DEFAULT_CONTROLLER_ID, actionId)
                        .accept(MediaType.APPLICATION_ATOM_XML)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotAcceptable());
    }

    @Test
    @Description("The server protects itself against to many feedback upload attempts. The test verifies that "
            + "it is not possible to exceed the configured maximum number of feedback uploads.")
    public void tooMuchDeploymentActionFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final String feedback = getJsonRejectedDeploymentActionFeedback();
        // assign distribution set creates an action status, so only 99 left
        for (int i = 0; i < 99; i++) {
            postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isOk());
        }

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isForbidden());
    }

    @Test
    @Description("The server protects itself against too large feedback bodies. The test verifies that "
            + "it is not possible to exceed the configured maximum number of feedback details.")
    public void tooMuchDeploymentActionMessagesInFeedback() throws Exception {
        final Target target = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), DEFAULT_CONTROLLER_ID);
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final List<String> messages = new ArrayList<>();
        for (int i = 0; i < quotaManagement.getMaxMessagesPerActionStatus() + 1; i++) {
            messages.add(String.valueOf(i));
        }

        final String feedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS, new DdiDownload(70, new DdiPackage(2,4)),
                null, messages);
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(), feedback, status().isBadRequest());
    }

    @Test
    @Description("Multiple uploads of deployment status feedback to the server.")
    public void multipleDeploymentActionFeedback() throws Exception {
        testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);
        testdataFactory.createTarget(VALUE_4713, VALUE_4713, VALUE_4713, testdataFactory.createVehicle("NES 2.0").getId());
        testdataFactory.createTarget("4714", "4714", "4714", testdataFactory.createVehicle("X250").getId());

        final DistributionSet ds1 = testdataFactory.createDistributionSet("1", true, false);
        final DistributionSet ds2 = testdataFactory.createDistributionSet("2", true, false);
        final DistributionSet ds3 = testdataFactory.createDistributionSet("3", true, false);

        final Long actionId1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), DEFAULT_CONTROLLER_ID));
        final Long actionId2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), DEFAULT_CONTROLLER_ID));
        final Long actionId3 = getFirstAssignedActionId(assignDistributionSet(ds3.getId(), DEFAULT_CONTROLLER_ID));

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 3, Optional.empty());
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.UNKNOWN)).hasSize(2);

        // action1 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId1, getJsonCanceledDeploymentActionFeedback(),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 2, Optional.of(ds1));
        assertStatusMessagesCount(4);

        // action2 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId2, getJsonCanceledDeploymentActionFeedback(),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.PENDING, 1, Optional.of(ds2));
        assertStatusMessagesCount(5);

        // action3 done
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId3, getJsonCanceledDeploymentActionFeedback(),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.of(ds3), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds3));
        assertStatusMessagesCount(6);

    }

    @Test
    @Description("Verifies that an update action is correctly set to error if the controller provides error feedback.")
    public void rootRsSingleDeploymentActionWithErrorFeedback() throws Exception {
        DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        assertThat(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.UNKNOWN);
        assignDistributionSet(ds, Collections.singletonList(savedTarget));
        final Action action = deploymentManagement.findActionsByDistributionSet(PAGE, ds.getId()).getContent().get(0);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action.getId(),
                getJsonActionFeedback(DdiStatus.ExecutionStatus.ERROR_RESPONSE_CODE, null,
                        Collections.singletonList("Error message")),
                status().isOk());

        findTargetAndAssertUpdateStatus(Optional.empty(), TargetUpdateStatus.ERROR, 0, Optional.empty());
        assertThat(deploymentManagement.countActionsByTarget(DEFAULT_CONTROLLER_ID)).isEqualTo(1);
        assertTargetCountByStatus(0, 1, 0);

        // redo
        ds = distributionSetManagement.getWithDetails(ds.getId()).get();
        assignDistributionSet(ds,
                Collections.singletonList(targetManagement.getByControllerID(DEFAULT_CONTROLLER_ID).get()));
        final Action action2 = deploymentManagement.findActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID).getContent()
                .get(0);
        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, action2.getId(), getJsonCanceledCancelActionFeedback(),
                status().isOk());
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.IN_SYNC, 0, Optional.of(ds));
        assertTargetCountByStatus(0, 0, 1);
        assertThat(deploymentManagement.findInActiveActionsByTarget(PAGE, DEFAULT_CONTROLLER_ID)).hasSize(2);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(4);
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(DeviceActionStatus.ERROR_RESPONSE_CODE));
        assertThat(deploymentManagement.findActionStatusByAction(PAGE, action2.getId()).getContent()).haveAtLeast(1,
                new ActionStatusCondition(DeviceActionStatus.FINISHED_SUCCESS));

    }

    @Test
    @Description("Verifies that the controller can provided as much feedback entries as necessary as long as it is in the configured limits.")
    public void rootRsSingleDeploymentActionFeedback() throws Exception {
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(ds,
                Collections.singletonList(testdataFactory.createTarget(DEFAULT_CONTROLLER_ID))));
        findTargetAndAssertUpdateStatus(Optional.of(ds), TargetUpdateStatus.PENDING, 1, Optional.empty());

        // Now valid Feedback
        for (int i = 0; i < 4; i++) {
            postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonRejectedDeploymentActionFeedback(),
                    status().isOk());
            assertActionStatusCount(i + 2, i);

        }

        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(6, 6, 0, 0, 0, 0);
        assertTargetCountByStatus(1, 0, 0);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonCanceledDeploymentActionFeedback(),
                status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(7, 6, 0, 0, 0, 1);
        assertTargetCountByStatus(1, 0, 0);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonRejectedDeploymentActionFeedback(),
                status().isOk()).andExpect(jsonPath(JSON_PATH_LINKS_LOG_COLLECTION_HREF).doesNotExist());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.PENDING, 1);
        assertActionStatusCount(8, 6, 0, 1, 0, 1);

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, actionId, getJsonCanceledDeploymentActionFeedback(),
                status().isOk());
        assertStatusAndActiveActionsCount(TargetUpdateStatus.IN_SYNC, 0);
        assertActionStatusCount(9, 6, 0, 1, 1, 1);
        assertTargetCountByStatus(0, 0, 1);

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, ds.getId())).hasSize(1);
        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, ds.getId())).hasSize(1);
    }

    @Test
    @Description("Various forbidden request attempts on the feedback resource. Ensures correct answering behaviour as expected to these kind of errors.")
    public void badDeploymentActionFeedback() throws Exception {
        final DistributionSet savedSet = testdataFactory.createDistributionSet("");
        final DistributionSet savedSet2 = testdataFactory.createDistributionSet("1");

        // target does not exist

        postDeploymentFeedback(DEFAULT_CONTROLLER_ID, 1234L, getJsonCanceledDeploymentActionFeedback(),
                status().isNotFound());

        final Target savedTarget = testdataFactory.createTarget(DEFAULT_CONTROLLER_ID);

        assignDistributionSet(savedSet, Collections.singletonList(savedTarget)).getAssignedEntity().iterator().next();
        assignDistributionSet(savedSet2, Collections.singletonList(testdataFactory.createTarget(VALUE_4713, VALUE_4713, VALUE_4713, testdataFactory.createVehicle("Dcross").getId())));

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        // not allowed methods
        mvc.perform(MockMvcRequestBuilders.get(DdiRestConstants.BASE_DEPLOYMENT_ACTION_FEEDBACK_PATH,
                        DEFAULT_CONTROLLER_ID, "2")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(put(DdiRestConstants.BASE_DEPLOYMENT_ACTION_FEEDBACK_PATH,  DEFAULT_CONTROLLER_ID, "2"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Ensures that an invalid id in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void invalidIdInFeedbackReturnsBadRequest() throws Exception {
        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String invalidFeedback = "{\"id\":\"AAAA\",\"status\":{\"execution\":\"proceeding\",\"result\":{\"finished\":\"none\",\"progress\":{\"cnt\":2,\"of\":5}},\"details\":\"details\"]}}";
        postDeploymentFeedback("1080", action.getId(), invalidFeedback, status().isBadRequest());
    }

    @Test
    @Description("Ensures that a missing feedback result in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void missingResultAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), "1080");
        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        final String missingResultInFeedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.DD_ACCEPTED, null,
                Collections.singletonList("test"));
        postDeploymentFeedback("1080", action.getId(), missingResultInFeedback, status().isBadRequest());
    }

    @Test
    @Description("Ensures that a missing finished result in feedback body returns a bad request.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 1),
            @Expect(type = ActionCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 1)})
    public void missingFinishedAttributeInFeedbackReturnsBadRequest() throws Exception {

        final Target target = testdataFactory.createTarget("1080");
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        assignDistributionSet(ds.getId(), "1080");

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        final String missingFinishedResultInFeedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.DD_ACCEPTED,
                null,
                Collections.singletonList("test"));

        postDeploymentFeedback("1080", action.getId(), missingFinishedResultInFeedback, status().isBadRequest())
                .andExpect(jsonPath("$.*", hasSize(3)))
                .andExpect(jsonPath("$.name", equalTo("REST_BODY_NOT_READABLE")));
    }

    @Test
    @Description("Ensures that a feedback body with finished result that contains failure status, " +
            "insert's error codes into action status errorCodes.")
    public void validateFailureFeedbackWithErrorCode() throws Exception {

        final Target target = testdataFactory.createTarget(STATUS_TEST_01);
        final DistributionSet ds = testdataFactory.createDistributionSet(STATUS_TEST_01);
        assignDistributionSet(ds.getId(), STATUS_TEST_01);

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);
        List<DdiStatus> ddiStatuses = List.of(getDdiStatusWithErrorList(DdiStatus.ExecutionStatus.ERROR_RESPONSE_CODE, null, MESSAGE, 1737348213000L));
        String feedbackAsString = getJsonActionFeedbackObjectToSting(ddiStatuses);

        postDeploymentFeedback(STATUS_TEST_01, action.getId(), feedbackAsString, status().isOk());

        final ActionStatus failureActionStatus = deploymentManagement
                .findActionStatusByAction(PageRequest.of(1, 1), action.getId()).toList().get(0);
        assertNotNull(failureActionStatus.getErrorCode());
    }

    @Test
    @Description("Ensures that a feedback body with finished result that contains success status, " +
            "won't insert's error codes into action status errorCodes.")
    public void validateSuccessFeedbackWithErrorCode() throws Exception {

        final Target target = testdataFactory.createTarget(STATUS_TEST_02);
        final DistributionSet ds = testdataFactory.createDistributionSet(STATUS_TEST_02);
        assignDistributionSet(ds.getId(), STATUS_TEST_02);

        final Action action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).getContent()
                .get(0);

        // Validate feedback won't insert error code on success
        final String successFinishedResultInFeedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.DD_ACCEPTED,
                null, null, Collections.singletonList("Success"), ERROR_CODE);

        postDeploymentFeedback(STATUS_TEST_02, action.getId(), successFinishedResultInFeedback, status().isOk());

        final ActionStatus successActionStatus = deploymentManagement
                .findActionStatusByAction(PageRequest.of(0, 1), action.getId()).toList().get(0);
        assertNull(successActionStatus.getErrorCode());
    }

    private void assertActionStatusCount(final int actionStatusCount, final int minActionStatusCountInPage) {
        final Target target = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.PENDING);

        assertTargetCountByStatus(1, 0, 0);

        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId())).hasSize(1);
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(actionStatusCount);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(minActionStatusCountInPage,
                new ActionStatusCondition(DeviceActionStatus.RUNNING));
    }

    private Target createTargetAndAssertNoActiveActions() {
        final Target savedTarget = testdataFactory.createTarget(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())).isEmpty();
        assertThat(deploymentManagement.countActionsAll()).isZero();
        assertThat(deploymentManagement.countActionStatusAll()).isZero();
        return savedTarget;
    }

    private void assertStatusMessagesCount(final int actionStatusMessagesCount) {
        final Iterable<ActionStatus> actionStatusMessages;
        actionStatusMessages = deploymentManagement.findActionStatusAll(PageRequest.of(0, 100, Direction.DESC, "id"))
                .getContent();
        assertThat(actionStatusMessages).hasSize(actionStatusMessagesCount);
        assertThat(actionStatusMessages).haveAtLeast(1, new ActionStatusCondition(DeviceActionStatus.FINISHED_SUCCESS));
    }

    private void findTargetAndAssertUpdateStatus(final Optional<DistributionSet> ds,
                                                 final TargetUpdateStatus updateStatus, final int activeActions,
                                                 final Optional<DistributionSet> installedDs) {
        final Target myT = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(myT.getUpdateStatus()).isEqualTo(updateStatus);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, myT.getControllerId())).hasSize(activeActions);
        assertThat(deploymentManagement.getAssignedDistributionSet(myT.getControllerId())).isEqualTo(ds);
        assertThat(deploymentManagement.getInstalledDistributionSet(myT.getControllerId())).isEqualTo(installedDs);
    }

    private void assertTargetCountByStatus(final int pending, final int error, final int inSync) {
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.PENDING))
                .hasSize(pending);
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.ERROR)).hasSize(error);
        assertThat(targetManagement.findByUpdateStatus(PageRequest.of(0, 10), TargetUpdateStatus.IN_SYNC))
                .hasSize(inSync);
    }

    private void assertActionStatusCount(final int total, final int running, final int install, final int warning, final int finished,
                                         final int canceled) {
        assertThat(deploymentManagement.countActionStatusAll()).isEqualTo(total);
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(running,
                new ActionStatusCondition(DeviceActionStatus.RUNNING));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(canceled,
                new ActionStatusCondition(DeviceActionStatus.CANCELED));
        assertThat(deploymentManagement.findActionStatusAll(PAGE).getContent()).haveAtLeast(finished,
                new ActionStatusCondition(DeviceActionStatus.FINISHING_SUCCESS));
    }

    private void assertStatusAndActiveActionsCount(final TargetUpdateStatus status, final int activeActions) {
        final Target target = targetManagement.getByControllerID(DdiDeploymentBaseTest.DEFAULT_CONTROLLER_ID).get();
        assertThat(target.getUpdateStatus()).isEqualTo(status);
        assertThat(deploymentManagement.findActiveActionsByTarget(PAGE, target.getControllerId()))
                .hasSize(activeActions);
    }

    private static class ActionStatusCondition extends Condition<ActionStatus> {
        private final DeviceActionStatus status;

        public ActionStatusCondition(final DeviceActionStatus status) {
            this.status = status;
        }

        @Override
        public boolean matches(final ActionStatus value) {
            return value.getStatus() == status;
        }
    }
}
