/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutDeployment;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
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
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.verify.VerificationTimes;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.eclipse.hawkbit.mgmt.rest.resource.MgmtTargetResourceTest.JAKARTA_VALIDATION_VALIDATION_EXCEPTION;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for covering the {@link MgmtRolloutResource}.
 */
@Slf4j
@Feature("Component Tests - Management API")
@Story("Rollout Resource")
@ExtendWith(OutputCaptureExtension.class)
class MgmtRolloutResourceTest extends AbstractManagementRolloutApiIntegrationTest {


    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, false), Arguments.of(true, true), Arguments.of(false, true));
    }

    @Test
    @Description("Testing that creating rollout with wrong body returns bad request")
    void givenInvalidBodyWhenCreatingRolloutThenReturnBadRequest() throws Exception {

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).header(SESSION_ID, SESSION_ID_HEADER).content("invalid body").contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(DEBUG, notNullValue()));
    }

    @Test
    @Description("Testing that creating rollout with insufficient permission returns forbidden")
    @WithUser(allSpPermissions = true, removeFromAllPermission = "CREATE_ROLLOUT")
    void givenInsufficientPermissionWhenCreatingRolloutThenReturnForbidden() throws Exception {
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(JsonBuilder.rollout(ROLLOUT_1, "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null)).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().is(403)).andReturn();
    }

    @Test
    @Description("Testing that rollout can be created")
    void givenValidRequestWhenCreatingRolloutThenRolloutIsCreated() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);
    }

    @Test
    @Description("Testing that rollout can be created with end date is in future")
    void givenValidEndDateWhenCreatingRolloutThenRolloutIsCreated() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);

    }

    @Test
    @Description("Testing that rollout cannot be created with the end date same as creation date.")
    void givenInvalidEndDateWhenCreatingRolloutThenCreationFails() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), VALID_ROLLOUT_START_DATE, INVALID_ROLLOUT_END_DATE, null, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        MvcResult result = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER).content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION)))
                .andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue())).andReturn();
        String jsonResponse = result.getResponse().getContentAsString();

        assertTrue(jsonResponse.contains("End Date should be greater than Start Date") || jsonResponse.contains("'End Date (in UTC Time Zone)' must be at least one day ahead in the future"), "The error message does not match any of the expected options.");
    }

    @Test
    @Description("Given a rollout creation request, which may or may not include the maxPackageSize parameter," +
            "When the rollout creation API is called with this request," +
            "Then the rollout should be successfully created, and the response should not contain the maxPackageSizeInBytes " +
            "field if it was not provided in the request. The test also verifies that the created rollout exists in the repository.")
    void givenRolloutRequestWithOrWithoutMaxPackageSizeWhenCreatingRolloutThenRolloutIsCreated() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rolloutWithoutMaxPackageSize = JsonBuilder.rollout(
                ROLLOUT_1, "desc", MgmtRolloutPriority.REGULAR.getPriority(),
                MgmtRolloutStartType.SCHEDULED.getName(),
                MgmtRolloutUserAcceptanceRequired.YES.getName(),
                MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(),
                VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE,
                deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        MvcResult resultWithoutMax = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(rolloutWithoutMaxPackageSize)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES).doesNotExist())
                .andReturn();

        String responseWithoutMax = resultWithoutMax.getResponse().getContentAsString();
        long rolloutIdWithoutMax = new ObjectMapper().readTree(responseWithoutMax).get("id").asLong();
        JpaRollout jpaRolloutWithoutMax = rolloutRepository.getRolloutById(rolloutIdWithoutMax).orElse(null);
        assertNotNull(jpaRolloutWithoutMax, "JpaRollout should not be null");
        assertNull(jpaRolloutWithoutMax.getMaxPackageSize(), "maxPackageSize should be null when not provided");

        String rolloutWithMaxPackageSize = JsonBuilder.rollout(
                ROLLOUT_2, "desc", MgmtRolloutPriority.REGULAR.getPriority(),
                MgmtRolloutStartType.SCHEDULED.getName(),
                MgmtRolloutUserAcceptanceRequired.YES.getName(),
                MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(),
                VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE,
                deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, 5L, FOTA, null, null);

        MvcResult resultWithMax = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(rolloutWithMaxPackageSize)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES, equalTo(5)))
                .andReturn();

        String responseWithMax = resultWithMax.getResponse().getContentAsString();
        long rolloutIdWithMax = new ObjectMapper().readTree(responseWithMax).get("id").asLong();
        JpaRollout jpaRolloutWithMax = rolloutRepository.getRolloutById(rolloutIdWithMax).orElse(null);
        assertNotNull(jpaRolloutWithMax, "JpaRollout should not be null");
        assertEquals(5, jpaRolloutWithMax.getMaxPackageSize(), "maxPackageSize should match the provided value");
    }

    @Test
    @Description("Testing the empty list is returned if no rollout exists")
    void givenNoRolloutWhenQueriedThenReturnsEmptyList() throws Exception {
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(0))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(0)));
    }


    @Test
    @Description("Ensures the response for retrieving a single rollout contains all required fields as per the specification.")
    void givenRolloutWhenRetrievedThenResponseContainsAllRequiredFields() throws Exception {
        List<Target> targets = testdataFactory.createTargets(20, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);
        rolloutHandler.handleAll();

        // Perform the GET request
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.createdBy").exists())
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.lastModifiedBy").exists())
                .andExpect(jsonPath("$.lastModifiedAt").exists())
                .andExpect(jsonPath("$.name").exists())
                .andExpect(jsonPath("$.description").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.userAcceptanceRequired").exists())
                .andExpect(jsonPath("$.startAt").exists())
                .andExpect(jsonPath("$.endAt").exists())
                .andExpect(jsonPath("$.connectivityType").exists())
                .andExpect(jsonPath("$.downloadRetryCount").exists())
                .andExpect(jsonPath("$.maxDownloadDurationTimer").exists())
                .andExpect(jsonPath("$.maxDownloadWifiDurationTimer").exists())
                .andExpect(jsonPath("$.maxDownloadCellularDurationTimer").exists())
                .andExpect(jsonPath("$.maxUpdateTime").exists())
                .andExpect(jsonPath("$.log.collectionRequired", equalTo(rollout.isLogCollectionRequired())))
                .andExpect(jsonPath("$.log.level").exists())
                .andExpect(jsonPath("$.log.maxFailureVin").doesNotExist())
                .andExpect(jsonPath("$.log.maxAllFileSize").doesNotExist())
                .andExpect(jsonPath("$.log.maxEachFileSize").doesNotExist())
                .andExpect(jsonPath("$.log.maxSuccessVin").doesNotExist())
                .andExpect(jsonPath("$.deploymentMetadata.downgradeAllowed").exists())
                .andExpect(jsonPath("$.deploymentMetadata.requiredMedia").exists())
                .andExpect(jsonPath("$.deploymentMetadata.requiredStateOfCharge").exists())
                .andExpect(jsonPath("$.modules").isArray())
                .andExpect(jsonPath("$.modules[0].moduleId").exists())
                .andExpect(jsonPath("$.modules[0].softwareVersionTargetId").exists())
                .andExpect(jsonPath("$.totalTargets").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.running").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.notstarted").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.scheduled").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.canceled").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.finished").exists())
                .andExpect(jsonPath("$.totalTargetsPerStatus.error").exists())
                .andExpect(jsonPath("$.totalGroups").exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_DELETE_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_CANCEL_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF).exists())
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF).exists());

    }


    @Test
    @Description("Create rollout from management API excluding Device and Distribution set mapping throws exception while freezing rollout")
    void givenRolloutWithoutDeviceWhenCreatedThenThrowsException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        retrieveAndVerifyRolloutInDraftState(rollout);
        Assertions.assertThrows(RolloutIllegalStateException.class, () -> rolloutManagement.freeze(rollout.getId()));
    }


    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Description("Verify the confirmation required flag is not part of the rollout parent entity")
    void givenRolloutWhenVerifiedThenConfirmationFlagIsNotPartOfEntity(final boolean confirmationFlowActive) throws Exception {
        testdataFactory.createTargets(20, ROLLOUT, ROLLOUT);

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath("$.confirmationRequired").doesNotExist())
                .andExpect(jsonPath("$._links.supportPackages.href", equalTo(HREF_ROLLOUT_PREFIX + rollout.getId().intValue() + SUPPORT_PACKAGES_URL + "?offset=0&limit=50")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Description("Verify the confirmation required flag will be set based on the feature state")
    void givenRolloutWhenStateNotProvidedThenVerifyDefaultConfirmationState(final boolean confirmationFlowActive) throws Exception {
        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);

        final List<Rollout> content = rolloutManagement.findAll(PAGE, false).getContent();
        assertThat(content).hasSizeGreaterThan(0).allSatisfy(rollout -> assertThat(rolloutGroupManagement.findByRollout(PAGE, rollout.getId())).describedAs("Confirmation required flag depends on feature active.").allMatch(group -> group.isConfirmationRequired() == confirmationFlowActive));
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
    private void retrieveAndVerifyRolloutInStarting(final Rollout rollout) throws Exception {
        rolloutManagement.start(rollout.getId());


        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ROLLOUT))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(STARTING)));
    }

    @Step
    private void retrieveAndVerifyRolloutInDraft(final Rollout rollout) throws Exception {
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(ROLLOUT_1))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(BUMLUX))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT, not(equalTo(0))));
    }

    @Step
    private void retrieveAndVerifyRolloutInDraftState(final Rollout rollout) throws Exception {
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON))
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
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo(rollout.getDowngradeAllowed().getValue())))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo(rollout.getRequiredMedia().getValue())))
                .andExpect(jsonPath("$.deploymentMetadata.requiredStateOfCharge", equalTo(Collections.emptyMap())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, equalTo(rollout.getMaxDownloadCellularDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, equalTo(rollout.getMaxDownloadDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, equalTo(rollout.getMaxDownloadWifiDurationTimer())))
                .andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, equalTo(rollout.getDownloadRetryCount())))
                .andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, equalTo(rollout.getMaxUpdateTime())))

                // Verifying the links in the Get Rollout API response
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(START_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(PAUSE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(RESUME_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith("/groups/next"))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(SUPPORT_PACKAGES_URL + "?offset=0&limit=50"))))
                .andExpect(jsonPath(JSON_PATH_LINKS_DELETE_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_CANCEL_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), endsWith(CANCEL_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF).doesNotExist());
    }

    @Test
    @Description("Testing that rollout paged list contains rollouts")
    void givenRolloutPagedListWhenRetrievedThenContainsAllRollouts() throws Exception {

        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // setup - create 2 rollouts
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);
        invokeCreateRolloutApi(ROLLOUT_2, VALID_ROLLOUT_END_DATE);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(2))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(2)));
    }

    @Test
    void givenRolloutWithConnectivityTypeWhenRetrievedThenResponseContainsAllFields() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);
        invokeCreateRolloutApi(ROLLOUT_2, VALID_ROLLOUT_END_DATE);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "?limit=1", TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content[0].connectivityType", equalTo(CELLULAR)));
    }

    @Test
    @Description("Testing representation mode of rollout paged list")
    void givenRolloutPagedListWhenRetrievedThenRepresentationModeIsCorrect() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // setup - create 2 rollouts
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);
        invokeCreateRolloutApi(ROLLOUT_2, VALID_ROLLOUT_END_DATE);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "?representation=full", TENANT_ID).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.content[0]._links").exists());
    }

    @Test
    @Description("Testing that rollout paged list is limited by the query param limit")
    void givenRolloutPagedListWhenQueriedThenItIsLimitedToQueryParam() throws Exception {

        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // setup - create 2 rollouts
        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);
        invokeCreateRolloutApi(ROLLOUT_2, VALID_ROLLOUT_END_DATE);

        // Run here, because Scheduler is disabled during tests
        rolloutHandler.handleAll();

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "?limit=1", TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT1, hasSize(1))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(2)));
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Testing that a single rollout group can be retrieved")
    void givenRolloutGroupIdWhenRetrievedThenReturnSingleRolloutGroup(final boolean confirmationFlowEnabled, final boolean confirmationRequired) throws Exception {
        // setup
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        if (confirmationFlowEnabled) {
            enableConfirmationFlow();
        }

        // create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        final RolloutGroup firstGroup = rolloutGroupManagement
                .findByRollout(PageRequest.of(0, 1, Direction.ASC, "id"), rollout.getId()).getContent().get(0);

        givenRolloutWhenVerifyingThenRolloutGroupShouldBeInDraft(rollout, firstGroup);
        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());

        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());
        givenRolloutWhenVerifyingThenRolloutGroupShouldBeInReady(rollout, firstGroup);
        retrieveAndVerifyRolloutGroupInRunningAndScheduled(rollout, firstGroup,
                confirmationRequired);
    }

    @Step
    private void retrieveAndVerifyRolloutGroupInRunningAndScheduled(final Rollout rollout,
                                                                    final RolloutGroup firstGroup,
                                                                    final boolean confirmationRequired) throws Exception {
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + CREATING_RIOLLOUT_GROUPS_URL, TENANT_ID, rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                //  We need to enable after multiple rollout groups functionality is ready
                .andExpect(jsonPath(STATUS, equalTo(RUNNING)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_RUNNING, equalTo(20)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_NOTSTARTED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_SCHEDULED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_CANCELED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_FINISHED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_ERROR, equalTo(0)));
    }

    @Step
    private void givenRolloutWhenVerifyingThenRolloutGroupShouldBeInReady(final Rollout rollout, final RolloutGroup firstGroup)
            throws Exception {
        rolloutHandler.handleAll();
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + CREATING_RIOLLOUT_GROUPS_URL, TENANT_ID, rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(STATUS, equalTo(READY)))
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(BUMLUX)))
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT, not(equalTo(0))))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS, equalTo(20)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_RUNNING, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_NOTSTARTED, equalTo(20)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_SCHEDULED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_CANCELED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_FINISHED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_ERROR, equalTo(0)))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath(CONFIRMATION_REQUIRED).exists()
                        : jsonPath(CONFIRMATION_REQUIRED).doesNotExist());
    }

    @Step
    private void givenRolloutWhenVerifyingThenRolloutGroupShouldBeInDraft(final Rollout rollout, final RolloutGroup firstGroup)
            throws Exception {
        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + CREATING_RIOLLOUT_GROUPS_URL, TENANT_ID, rollout.getId(), firstGroup.getId())
                        .accept(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("id", equalTo(firstGroup.getId().intValue())))
                .andExpect(isConfirmationFlowEnabled() ? jsonPath(CONFIRMATION_REQUIRED).exists()
                        : jsonPath(CONFIRMATION_REQUIRED).doesNotExist())
                .andExpect(jsonPath(STATUS, equalTo(DRAFT))).andExpect(jsonPath("name", equalTo(rollout.getName() + DEFAULT_ROLLOUT_GROUP_NAME_SUFFIX)))
                .andExpect(jsonPath(DESCRIPTION, equalTo(rollout.getName() + ROLLOUT_GROUP_DESCRIPTION_SUFFIX)))
                .andExpect(jsonPath(JSON_PATH_TARGET_FILTER_QUERY, equalTo("")))
                .andExpect(jsonPath("$.targetPercentage", equalTo(100.0)))
                .andExpect(jsonPath(JSON_PATH_CREATED_BY, equalTo(BUMLUX)))
                .andExpect(jsonPath(JSON_PATH_CREATED_AT, not(equalTo(0))))
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(BUMLUX)))
                .andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT, not(equalTo(0))))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS, equalTo(20)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_RUNNING, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_NOTSTARTED, equalTo(20)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_SCHEDULED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_CANCELED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_FINISHED, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_TOTAL_TARGETS_PER_STATUS_ERROR, equalTo(0)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, equalTo(
                        HREF_ROLLOUT_PREFIX + rollout.getId() + "/groups/" + firstGroup.getId().intValue())));
    }

    @Test
    @Description("Deletion of a rollout with no support packages")
    void givenRolloutWithoutSupportPackages_whenDeleteRollout_thenRolloutIsDeleted() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT_DELETE, ROLLOUT_DELETE);

        // Create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_DELETE)));

        // Perform the delete operation for the rollout and expect an EntityNotFoundException
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());


    }


    List<EcuModel> createEcuModel(String ecuNodeAddress) {
        List<EcuModel> mockEcuModels = new ArrayList<>();

        JpaEcuModel ecuModel1 = new JpaEcuModel();
        JpaEcuModelType ecuModelType1 = new JpaEcuModelType("OM");
        ecuModel1.setEcuModelType(ecuModelType1);
        ecuModel1.setEcuModelName("Test ECU Model 1");
        ecuModel1.setEcuNodeId(ecuNodeAddress);
        mockEcuModels.add(ecuModel1);
        return mockEcuModels;
    }


    @Test
    @Description("This test case verifies that the API successfully unlinks all support packages and deletes a specific rollout.")
    void givenRollout_whenUnlinkSupportPackagesAndDelete_thenSuccess() throws Exception {
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
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }

    @Test
    @Description("Verifies that rollout with other then READY status will fail to start when manual start type is used")
    void givenDraftRolloutWhenStartedThenThrowsError() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE1221", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, 10000L, FOTA, null, null);

        MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES, equalTo(10000)))
                .andReturn();
        JSONObject jsonObject = new JSONObject(mvcResult.getResponse().getContentAsString());
        long rolloutId = jsonObject.getLong("id");
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rolloutId)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Verifies that rollout with other then MANUAL start type will fail to start when rollout manual is started manually.")
    void givenScheduledRolloutWhenStartedManuallyThenThrowsError() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE12", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);


        MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated()).andReturn();
        JSONObject jsonObject = new JSONObject(mvcResult.getResponse().getContentAsString());
        long rolloutId = jsonObject.getLong("id");
        mvc.perform(put(MgmtRestConstants.ROLLOUT_START_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Check if assigned \"software downgrade enabled\" distribution is allowed based on tenant configuration, while creating rollout")
    void givenDowngradeDisableTenant_whenValidateDistributionAssignment_thenRolloutAssigned() throws Exception {

        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, ROLLOUT_1_DESC, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), 5L, 3L, null, 3, null, null, MAX_UPDATE_TIME, 50, 5, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(JAKARTA_VALIDATION_VALIDATION_EXCEPTION))).andExpect(jsonPath(JSON_PATH_DEBUG, notNullValue()));
    }

    @Test
    @Description("Testing that rollout can be created with end-date")
    void givenRollout_whenCreateWithEndDate_thenSuccess() throws Exception {
        final Long startAt = VALID_ROLLOUT_START_DATE;
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create rollout with end date less than current date then throws error
        String rollout = JsonBuilder.rollout("rollout21", "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), startAt, 3L, null, 5, null, null, MAX_UPDATE_TIME, 50, 5, null, FOTA, null, null);


        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // Create rollout with end date less than start time then throws error

        rollout = JsonBuilder.rollout("rollout21", "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), startAt, 3L, null, 5, null, null, MAX_UPDATE_TIME, 50, 5, null, FOTA, null, null);


        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        invokeCreateRolloutApi(ROLLOUT_1, VALID_ROLLOUT_END_DATE);

    }

    @Test
    @Description("Trigger next rollout group and verify If its running and then set endDate of Rollout to past and run the scheduler to verify if the rollout is moved to finishing")
    void givenRollout_whenTriggerNextGroup_thenSuccess() throws Exception {
        // setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 33, 1, 100) + "," +
                createBasicGroupJson("test-group02", 33, 1, 100) + "," +
                createBasicGroupJson("test-group03", 33, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson);

        // Retrieve the newly created rollout
        Rollout rollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        // Freeze and start the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/groups/next", TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Verify initial group statuses
        final List<RolloutGroupStatus> groupStatus = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().stream().map(RolloutGroup::getStatus).collect(Collectors.toList());
        assertThat(groupStatus).containsExactly(RolloutGroupStatus.RUNNING, RolloutGroupStatus.RUNNING, RolloutGroupStatus.QUEUED, RolloutGroupStatus.QUEUED);

        // Expire the rollout by setting end time to the past
        var pastTimeStamp = Instant.now().atZone(ZoneId.of("UTC")).minusDays(1).toEpochSecond();
        JpaRollout jpaRollout = rolloutRepository.getRolloutById(rollout.getId()).get();
        jpaRollout.setEndAt(pastTimeStamp);
        rolloutRepository.save(jpaRollout);

        // Handle expired rollout
        rolloutHandler.handleEnd();

        // Verify rollout and groups are in FINISHING state
        rollout = rolloutRepository.getRolloutById(rollout.getId()).get();
        assertEquals(RolloutStatus.FINISHING, rollout.getStatus());
        var rolloutGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        rolloutGroups.forEach(rolloutGroup -> {
            assertEquals(RolloutGroupStatus.FINISHING, rolloutGroup.getStatus());
        });

        // Verify actions that are not running are marked as FINISHED_NOT_EXECUTED
        var actions = actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true);
        actions.forEach(action -> {
            if (!action.getStatus().equals(DeviceActionStatus.RUNNING)) {
                assertEquals(DeviceActionStatus.FINISHED_NOT_EXECUTED, action.getStatus());
            }
        });

        // The status of expired rollout updated to FINISHED
        rolloutHandler.handleAll();
        assertEquals(RolloutStatus.FINISHED, rolloutManagement.get(rollout.getId()).get().getStatus());

        // Verify group statuses are FINISHED
        var updatedGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        updatedGroups.forEach(group -> {
            assertEquals(RolloutGroupStatus.FINISHED, group.getStatus());
        });
    }

    @Test
    @Description("Verify start all the rollout groups API moves all the groups to RUNNING at a time")
    void givenRolloutWhenTriggerStartAllGroupsThenSuccess() throws Exception {
        // setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group001", 80, 100, 100) + "," +
                createBasicGroupJson("test-group002", 20, 100, 100) + "]";

        createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson);

        // Retrieve the newly created rollout
        Rollout rollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        // Verify Start All rollout Groups throws error when the rollout is not RUNNING
        mvc.perform(post(MgmtRestConstants.ROLLOUT_GROUP_START_ALL_V1_REQUEST_MAPPING_TENANT, TENANT_ID,
                        rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", containsString("ROLLOUT_ILLEGAL_STATE")))
                .andExpect(jsonPath("$.message", containsString("The requested operation can be performed only when the Rollout is in status RUNNING but the current state of the rollout is DRAFT")));


        // Freeze and start the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Verify initial group statuses
        final List<RolloutGroupStatus> groupStatus = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().stream().map(RolloutGroup::getStatus).collect(Collectors.toList());
        assertThat(groupStatus).containsExactly(RolloutGroupStatus.RUNNING, RolloutGroupStatus.QUEUED);

        // Start All rollout Groups
        mvc.perform(post(MgmtRestConstants.ROLLOUT_GROUP_START_ALL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print())
                .andReturn();

        // Verify all the groups are running
        var rolloutGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        rolloutGroups.forEach(rolloutGroup -> {
            assertEquals(RolloutGroupStatus.RUNNING, rolloutGroup.getStatus());
        });

        // Verify Start All rollout Groups throws error when there are no groups in QUEUED state to start
        mvc.perform(post(MgmtRestConstants.ROLLOUT_GROUP_START_ALL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("No rollout groups found to perform the requested operation which are in status QUEUED")));
    }

    /**
     * Creates a rollout and groups based on the provided grouping conditions and associates them
     * with the given rollout.
     *
     * @param rolloutName       name of the rollout
     * @param ds                distribution set
     * @param targets           list of targets to be grouped
     * @param groupsDetailsJson conditions for creating groups and assigning targets
     * @return list of groups created and associated targets to the groups
     */
    private List<AssociatedTargetsToRolloutGroup> createRolloutAndGroups(String rolloutName, DistributionSet ds, List<Target> targets, String groupsDetailsJson) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        // Create new software module and version
        SoftwareModule softwareModule = ds.getDistributionSetModules().get(0).getSm();
        Version version = ds.getDistributionSetModules().get(0).getVersion();

        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        return rolloutManagement.get(rollout.getId()).map(savedRollout ->
                rolloutManagement.addDeviceDetails(savedRollout, targets.stream().map(Target::getControllerId).toList(), groupsDetailsJson)
        ).orElse(Collections.emptyList());
    }

    @Test
    @Description("Trigger next rollout group if rollout is in wrong state")
    void givenRollout_whenTriggerNextGroupWrongState_thenError() throws Exception {
        final String randomString = RandomStringUtils.randomAlphanumeric(5);
        List<Target> targets = testdataFactory.createTargets(10, randomString + "-testTarget-");

        final String prefixRolloutRunning = randomString + "1";
        Rollout rollout = createRolloutWithDependencies(prefixRolloutRunning, testdataFactory.createDistributionSet(), targets);
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // STARTING state
        rolloutManagement.start(rollout.getId());
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        // RUNNING state
        rolloutHandler.handleAll();

        // PAUSED state
        rolloutManagement.pauseRollout(rollout.getId());
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        rolloutHandler.handleAll();

        rolloutManagement.resumeRollout(rollout.getId());

        // last group already running
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

        rolloutHandler.handleAll();

        // FINISHED state
        setTargetsStatus(targets, DeviceActionStatus.FINISHING_SUCCESS);
        rolloutHandler.handleAll();
        triggerNextGroupAndExpect(rollout, status().isBadRequest());

    }

    private void triggerNextGroupAndExpect(final Rollout rollout, final ResultMatcher expect) throws Exception {
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/groups/next", TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(expect);
    }

    private void setTargetsStatus(final List<Target> targets, final DeviceActionStatus status) {
        for (final Target target : targets) {
            final Long action = deploymentManagement.findActionsByTarget(target.getControllerId(), PAGE).toList().get(0).getId();
            controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(action).status(status).message("test"), null);
        }
    }

    @Test
    @Description("Given a rollout is created, when starting it manually, then it returns bad request.")
    void givenRollout_whenStartManual_thenBadRequestReturned() throws Exception {
        List<Target> targets = testdataFactory.createTargets(2);
        Rollout rollout = createRolloutWithDependencies("rollout1", testdataFactory.createDistributionSet(), targets);

        // Ensure the rollout remains in draft state even after multiple scheduler passes
        for (int i = 0; i < 5; i++) {
            rolloutHandler.handleAll();
            Thread.sleep(2000);
        }
        String response = mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).param(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, NAME_ROLLOUT_1).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andReturn().getResponse().getContentAsString();

        // Manually start the rollout using the start API and confirm it transitions to the starting state
        String rolloutId = new ObjectMapper().readTree(response).path("content").get(0).path("id").toString();

        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + START_ROLLOUT_URL, TENANT_ID, rolloutId)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Test to ensure that deleting a rollout in DRAFT status is successful.")
    void givenRolloutInDraftWhenDeleteThenSuccess() throws Exception {
        final int amountTargets = 10;

        testdataFactory.createTargets(amountTargets, ROLLOUT_DELETE, ROLLOUT_DELETE);

        // Create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)));

        // Perform the delete operation for the rollout and expect an EntityNotFoundException
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }


    @Test
    @Description("Test to ensure that deleting a rollout in DRAFT status also deletes associated distribution sets.")
    void givenRolloutInDraftWhenDeleteThenDistributionSetDeleted() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT_DELETE, ROLLOUT_DELETE);
        final DistributionSet dsA = testdataFactory.createDistributionSet("set01");

        // Create rollout including the created targets with prefix 'rollout'
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        mvc.perform(get(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + rollout.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id", equalTo(rollout.getId().intValue()))).andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)));
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_URL, TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Verify the distribution set was deleted
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSET_V1_REQUEST_MAPPING + "/" + dsA.getId(), TENANT_ID).accept(MediaType.APPLICATION_JSON));
    }

    @Test
    @Description("Given valid rollout data, When the rollout is created via POST request, Then it should return status CREATED and the correct response body")
    void givenValidRolloutData_whenCreatingRollout_thenRolloutIsCreatedSuccessfully() throws Exception {

        // Given: Setup the initial conditions
        Long startDate = VALID_ROLLOUT_START_DATE;
        Long endDate = VALID_ROLLOUT_END_DATE;

        // Create necessary test data
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Build the deployment log and metadata objects
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        // Create rollout JSON using builder pattern or helper method
        final String rollout = JsonBuilder.rollout(HPC_UPDATE_1, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, endDate, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        // When: Perform the action
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))

                // Then: Verify the results
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).
                andExpect(jsonPath(JSON_PATH_NAME, equalTo(HPC_UPDATE_1))).
                andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(CAMPAIGN_FOR_HPC_UPDATE))).
                andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT))).
                andExpect(jsonPath(JSON_PATH_START_AT, equalTo(startDate.intValue()))).
                andExpect(jsonPath(JSON_PATH_END_AT, equalTo(endDate.intValue()))).
                andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, equalTo("yes"))).
                andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, equalTo(CELLULAR))).
                andExpect(jsonPath(JSON_PATH_LOG_COLLECTION_REQUIRED, equalTo(true))).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN, equalTo(5))).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES, equalTo(5))).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE, equalTo(50))).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN, equalTo(5))).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE, equalTo(5))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo("0"))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo("0"))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE, equalTo("60%"))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE, equalTo("78 C"))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC, equalTo("NA"))).
                andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, equalTo(0))).
                andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, equalTo(0))).
                andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, equalTo(0))).
                andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, equalTo(1))).
                andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, equalTo(MAX_UPDATE_TIME))).
                andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX))).
                andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL)))).
                andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL)))).
                andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL)))).
                andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL)))).
                andExpect(jsonPath(JSON_PATH_DEPLOYMENT_ESTIMATED_UPDATE_TIME, equalTo(DEPLOYMENT_ESTIMATED_UPDATE_TIME)));
    }

    @Test
    @Description("Given a rollout is created with a null or empty name, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithNullOrEmptyNameWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with a null name and perform the POST request
        String rollout = JsonBuilder.rollout(null, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // Create a rollout with an empty name and perform the POST request
        rollout = JsonBuilder.rollout("", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid user acceptance required value, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidUserAcceptanceRequiredWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid user acceptance required value and perform the POST request
        final String rollout = JsonBuilder.rollout(HPC_UPDATE_1, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), RANDOM, MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid tenant ID, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidTenantIdWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with a valid payload but an invalid tenant ID and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE2", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, 10000L).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a rollout is created with an invalid priority value, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidPriorityWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid priority value and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE3", CAMPAIGN_FOR_HPC_UPDATE, RANDOM, MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid start type, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidStartTypeWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid start type and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE4", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), RANDOM, MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid start date, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidStartDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid start date (null) and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE5", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), null, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }


    @Test
    @Description("Given a rollout is created with an invalid end date, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidEndDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid end date and perform the POST request
        String rollout = JsonBuilder.rollout("HPC_UPDATE6", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, INVALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // Create a rollout with a null end date and perform the POST request
        rollout = JsonBuilder.rollout("HPC_UPDATE7", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, null, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid connectivity type, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidConnectivityTypeWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid connectivity type and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE8", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), RANDOM, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid required state of charge, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidRequiredStateOfChargeWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create deployment metadata with invalid required state of charge
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put("sss", "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        // Create a rollout with the invalid required state of charge and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE9", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), RANDOM, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid required media, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidRequiredMediaWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create deployment metadata with invalid required media
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(1, 2, new JSONObject().put(BATTERY_TEMPERATURE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        // Create a rollout with the invalid required media and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE10", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), RANDOM, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with an invalid downgrade allowed, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidDowngradeAllowedWhenRequestIsMadeThenReturnsBadRequest() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create deployment metadata with invalid downgrade allowed
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(2, 1, new JSONObject().put(BATTERY_TEMPERATURE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        // Create a rollout with the invalid downgrade allowed and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE11", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), RANDOM, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created, when the request is made, then the max each file size should be populated to the default value 209715.")
    void givenRolloutCreatedWhenRequestIsMadeThenMaxEachFileSizeIsPopulatedToDefaultValue() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with the specified parameters
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE12", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).
                andExpect(status().isCreated()).
                andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE).doesNotExist());
    }

    @Test
    @Description("Given a rollout is created with an invalid log max file size, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidLogMaxFileSizeWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create deployment log with invalid log max file size
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 15);

        // Create a rollout with the invalid log max file size and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE13", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, deploymentLog, 0, MAX_UPDATE_TIME, 500, 1, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Log Max Each File Size (in Kilo Bytes) must be less than or equal to Log Max All File Size (in Kilo Bytes) / Log Max Number of Files")));
    }

    @Test
    @Description("Given a rollout is created with an invalid download duration timer, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidDownloadDurationTimerWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create a rollout with the invalid download duration timer
        final String rollout = JsonBuilder.rollout("HPC_UPDATE14", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 1, MAX_UPDATE_TIME, 0, 2, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Max download duration timer should be greater than either max wifi or cellular download duration timer")));
    }

    @Test
    @Description("Given a rollout is created with an invalid download wifi duration timer, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidDownloadWifiDurationTimerWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create a rollout with the invalid download duration timer
        final String rollout = JsonBuilder.rollout("HPC_UPDATE15", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 1, MAX_UPDATE_TIME, 2, 0, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Max download duration timer should be greater than either max wifi or cellular download duration timer")));
    }

    @Test
    @Description("Given a rollout is created with an invalid sum of download duration timers, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidSumDownloadDurationTimersWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create a rollout with the invalid sum of download duration timers
        final String rollout = JsonBuilder.rollout("HPC_UPDATE16", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 15, MAX_UPDATE_TIME, 2, 9, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Max download duration timer should be less than sum of wifi and cellular download duration timer")));
    }

    @Test
    @Description("Given a rollout is created with an end date less than one day ahead, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithEndDateLessThanOneDayAheadWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create a rollout with an end date less than one day ahead

        Long endDate = Instant.now().plus(6, ChronoUnit.HOURS).getEpochSecond();
        final String rollout = JsonBuilder.rollout("HPC_UPDATE17", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, endDate, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        // Perform the POST request and verify the response

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Start Date can be provided only for SCHEDULED rollout and the current rollout startType is MANUAL")));
    }

    @Test
    @Description("Given a rollout is created with an invalid or past end date, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidOrPastEndDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create a rollout with an invalid or past end date
        final String rollout = JsonBuilder.rollout("HPC_UPDATE18",
                CAMPAIGN_FOR_HPC_UPDATE,
                MgmtRolloutPriority.REGULAR.getPriority(),
                MgmtRolloutStartType.MANUAL.getName(),
                MgmtRolloutUserAcceptanceRequired.YES.getName(),
                MgmtRolloutConnectivityType.CELLULAR.getName(),
                null,
                Instant.now().minusSeconds(1).getEpochSecond(),
                null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("End Date (in UTC Time Zone) must be at least 2 days ahead in the future")));
    }

    @Test
    @Description("Given a duplicate rollout name for the same tenant, when creating the rollout, then it should return conflict status")
    void givenDuplicateRolloutNameForSameTenant_whenCreatingRollout_thenConflictStatus() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid user acceptance required value and perform the POST request
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE19", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
        final String rollout1 = JsonBuilder.rollout("HPC_UPDATE19", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout1).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isConflict());
    }

    @Test
    @Description("Given a rollout, when creating it, then it should be stored properly")
    void givenRollout_whenCreating_thenStoredProperly() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create a rollout with an invalid user acceptance required value and perform the POST request
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE21", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        String response = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long rolloutId = new ObjectMapper().readTree(response).path("id").asLong();
        Optional<JpaRollout> savedRollout = rolloutRepository.findById(rolloutId);
        assertTrue(savedRollout.isPresent());
    }

    @Test
    @Description("Given a rollout with max number of files set to zero, when the request is made, then it should return a bad request status.")
    void givenRolloutWithMaxNumberOfFilesZero_whenRequestIsMade_thenReturnsBadRequest() throws Exception {


        // Create necessary test data
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Build the deployment log and metadata objects
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 0, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        final String rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout with max update time less than minimum threshold value, when creating the rollout, then it should return bad request status")
    void givenRolloutWithMaxUpdateTimeLessThanMinThreshold_whenCreatingRollout_thenReturnsBadRequest() throws Exception {


        // Create necessary test data
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Build the deployment log and metadata objects
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        final String rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, 200, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout with max update time greater than maximum threshold value, when creating the rollout, then it should return bad request status")
    void givenRolloutWithMaxUpdateTimeGreaterThanMaxThreshold_whenCreatingRollout_thenReturnsBadRequest() throws Exception {


        // Create necessary test data
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Build the deployment log and metadata objects
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        final String rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, 2000, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout with negative values, when the request is made, then it should return a bad request status.")
    void givenRolloutWithNegativeValues_whenRequestIsMade_thenReturnsBadRequest() throws Exception {


        // Create necessary test data
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Build the deployment log and metadata objects
        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        String rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, -3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, -2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, -MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, -5, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, -1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        deploymentLog = JsonBuilder.createDeploymentLog(true, -5, 1, 50, 5, 5);

        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        deploymentLog = JsonBuilder.createDeploymentLog(true, 5, -1, 50, 5, 5);

        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, -50, 5, 5);

        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, 50, -5, 5);

        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 1, 50, 5, -5);

        rollout = JsonBuilder.rollout(HPC_UPDATE_23, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, null, MAX_UPDATE_TIME, 2, 3, null, FOTA, null, null);

        // Perform the POST request and verify the response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout with default values, when it is created, then it should have all the expected default values.")
    void givenRolloutWithDefaultValues_whenCreated_thenShouldHaveExpectedDefaultValues() throws Exception {

        // Given: Setup the initial conditions
        Long startDate = VALID_ROLLOUT_START_DATE;
        Long endDate = VALID_ROLLOUT_END_DATE;

        // Create necessary test data for targets
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        // Create rollout JSON using builder pattern or helper method
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE24", null, null, MgmtRolloutStartType.SCHEDULED.getName(), null, null, startDate, endDate, deploymentMetaData, null, null, null, null, null, null, null, FOTA, null, null);

        // When: Perform the action by making a POST request to create a rollout
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))

                // Then: Verify the results
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo("HPC_UPDATE24")))
                .andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)))
                .andExpect(jsonPath(JSON_PATH_END_AT, equalTo(endDate.intValue())))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, equalTo("yes")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, equalTo(WIFI_PREFERRED)))
                .andExpect(jsonPath(JSON_PATH_LOG_COLLECTION_REQUIRED, equalTo(false)))
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_FAILURE_VIN).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_NUMBER_OF_FILES).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_ALL_FILE_SIZE).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_SUCCESS_VIN).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LOG_MAX_EACH_FILE_SIZE).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo("0")))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_CELLULAR_DURATION_TIMER, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_DURATION_TIMER, equalTo(6)))
                .andExpect(jsonPath(JSON_PATH_MAX_DOWNLOAD_WIFI_DURATION_TIMER, equalTo(3)))
                .andExpect(jsonPath(JSON_PATH_DOWNLOAD_RETRY_COUNT, equalTo(5)))
                .andExpect(jsonPath(JSON_PATH_MAX_UPDATE_TIME, equalTo(MAX_UPDATE_TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_ESTIMATED_UPDATE_TIME, equalTo(DEPLOYMENT_ESTIMATED_UPDATE_TIME)))
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_SIZE_IN_BYTES).doesNotExist());
    }

    @Test
    @Description("Given duplicate rollout names for different tenants, when creating the rollout, then it should return created status")
    void givenTheDuplicateRolloutNamesForDifferentTenants_whenCreatingRollout_thenReturnsCreatedStatus() throws Exception {


        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        TenantMetaData tenant = systemManagement.createTenant("test1");
        // Create a rollout with an invalid user acceptance required value and perform the POST request
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout("HPC_UPDATE50", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
        MgmtRolloutRestRequestBody rolloutRequest = new MgmtRolloutRestRequestBody();
        rolloutRequest.setStartType(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setName("HPC_UPDATE50");
        rolloutRequest.setStartDate(VALID_ROLLOUT_START_DATE);
        rolloutRequest.setEndDate(VALID_ROLLOUT_END_DATE);
        rolloutRequest.setDeploymentMetadata(MgmtRolloutDeployment.builder().estimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME).build());
        rolloutRequest.setType(MgmtRolloutType.FOTA);
        systemSecurityContext.runAsSystemAsTenant(() -> {
            assertDoesNotThrow(() -> rolloutManagement.create(MgmtRolloutMapper.fromRequest(rolloutRequest)));
            return null;
        }, tenant.getTenant());
    }


    @Test
    @Description("Given an invalid tenant, when unfreezing a rollout, then it should return a not found status.")
    void givenInvalidTenant_whenUnfreezingRollout_thenReturnsNotFound() throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, 100000L, 1).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an invalid rollout ID, when unfreezing a rollout, then it should return a not found status.")
    void givenInvalidRolloutId_whenUnfreezingRollout_thenReturnsNotFound() throws Exception {
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, 10000L).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an invalid rollout state, when unfreezing a rollout, then it should return a bad request status.")
    void givenInvalidRolloutState_whenUnfreezingRollout_thenReturnsBadRequest() throws Exception {
        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("The requested operation can be performed only when the Rollout is in one of the statuses [READY, FREEZING] but the current state of the rollout is DRAFT")));
    }

    @Test
    @Description("Given an invalid rollout group state, when unfreezing a rollout, then it should return a bad request status.")
    void givenInvalidRolloutGroupState_whenUnfreezingRollout_thenReturnsBadRequest(CapturedOutput out) throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        Assertions.assertTrue(out.getAll().contains("Created " + 1 + " RolloutGroups for Rollout"));
        Rollout reloadedRollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.RUNNING, reloadedRollout.getStatus());
        JpaRollout jpaRollout = (JpaRollout) reloadedRollout;
        jpaRollout.setStatus(RolloutStatus.READY);
        rolloutRepository.save(jpaRollout);

        Rollout finalRollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.READY, finalRollout.getStatus());

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        assertEquals(RolloutGroupStatus.RUNNING, firstGroup.getStatus());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Rollout groups must be in the READY state to proceed, but one or more groups are not")));
    }

    @Test
    @Description("Give Rollout status Draft and Freezing, when unfreezing, then return")
    void givenUnFreezeRolloutWhenRolloutInDraftAndReadyStatusesThenOk(CapturedOutput out) throws Exception {
        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 75, 20, 50) + "]" ;

        Rollout rollout = createRolloutWithDependenciesAndGroups(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(CONTROLLER_ID),
                groupsDetailsJson,
                true
        );

        Assertions.assertTrue(out.getAll().contains("Created " + 1 + " RolloutGroups for Rollout"));
        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);

        assertEquals(RolloutGroupStatus.DRAFT, firstGroup.getStatus());

        assertEquals(RolloutStatus.DRAFT, rollout.getStatus());
        rolloutManagement.freeze(rollout.getId());
        rollout = rolloutManagement.get(rollout.getId()).get();
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId())
                .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).get();
        assertEquals(RolloutStatus.DRAFT, jpaRollout.getStatus());

        jpaRollout.setStatus(RolloutStatus.READY);
        rolloutRepository.save(jpaRollout);

        firstGroup.setStatus(RolloutGroupStatus.READY);
        rolloutGroupRepository.save(firstGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        JpaRollout jpaRollout1 = rolloutRepository.findById(rollout.getId()).get();
        assertEquals(RolloutStatus.DRAFT, jpaRollout1.getStatus());
    }


    @Test
    @Description("Given an empty rollout group, when unfreezing a rollout, then it should return a bad request status.")
    void givenEmptyRolloutGroup_whenUnfreezingRollout_thenReturnsBadRequest() throws Exception {
        JpaRollout jpaRollout = new JpaRollout();
        jpaRollout.setName("Test");
        jpaRollout.setStartType(MgmtRolloutStartType.SCHEDULED);
        jpaRollout.setStartAt(VALID_ROLLOUT_START_DATE);
        jpaRollout.setEndAt(VALID_ROLLOUT_END_DATE);
        jpaRollout.setStatus(RolloutStatus.READY);
        jpaRollout.setDeploymentEstimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME);
        jpaRollout.setType(MgmtRolloutType.FOTA);
        Rollout rollout = rolloutManagement.create(jpaRollout);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_ACTION_UNFREEZE_URL, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("Rollout must have at least one group to proceed")));
    }


    @Test
    @Description("Given an empty action field in the request, when associating software modules not found, then it should throw a ValidationException with a specific error message.")
    void givenEmptyActionFieldWhenAssociatingSoftwareModulesThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(4L, 3L);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(String.format("Software Module %s Target Version %s is not assigned with any artifact.", module.getModuleId(), module.getSoftwareVersionTargetId()))));
    }

    @Test
    @Description("Given a null modules list, then it should throw a ValidationException with a specific error message.")
    void givenNullModulesListThenThrowsValidationException() throws Exception {
        String requestBody = "[]";
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, 1).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("The request must contain at least one SoftwareModuleRequest.")));
    }

    @Test
    @Description("Given an empty modules list, then it should throw a ValidationException with a specific error message.")
    void givenEmptyModulesListThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(null);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID,
                        rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a module with a null moduleId, then it should throw a ValidationException with a specific error message.")
    void givenModuleWithNullModuleIdThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        MgmtSoftwareModuleRequest module = MgmtSoftwareModuleRequest.builder().softwareVersionTargetId(3L).build();
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("moduleId is mandatory for each module.")));
    }

    @Test
    @Description("Given a module with a null softwareVersionTargetId, then it should throw a ValidationException with a specific error message.")
    void givenModuleWithNullSoftwareVersionTargetIdThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        MgmtSoftwareModuleRequest module = MgmtSoftwareModuleRequest.builder().softwareVersionTargetId(4L).build();
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("moduleId is mandatory for each module.")));
    }

    @Test
    @Description("Given a module with a invalid moduleId, then it should throw a ValidationException with a specific error message.")
    void givenModuleWithNonPositiveModuleIdThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(-1L, 3L);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("moduleId must be a positive integer.")));
    }

    @Test
    @Description("Given a module with an invalid softwareVersionTargetId, then it should throw a ValidationException with a specific error message.")
    void givenModuleWithNonPositiveSoftwareVersionTargetIdThenThrowsValidationException() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT)));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(4L, -1L);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("softwareVersionTargetId must be a positive integer.")));
    }

    @Test
    @Description("Given a non-existent rollout ID, when associating software modules, then it should return a Not Found status.")
    void givenNonExistentRolloutIdWhenAssociatingSoftwareModulesThenReturnsNotFoundStatus() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        rolloutRepository.save(jpaRollout);
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(4L, 3L);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, 100).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isNotFound());
    }

    @Test
    @Description("Given an active rollout status, when associating software modules, then it should throw a ValidationException with a message indicating the operation is only allowed in DRAFT status.")
    void givenReadyRolloutStatusWhenAssociatingSoftwareModulesThenThrowsValidationException() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.READY);
        rolloutRepository.save(jpaRollout);
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(sm.getId(), dsA.getDistributionSetModules().get(0).getVersion().getId());
        createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID,
                        rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString(String.format("The requested operation can be performed only when the Rollout is in status %s but the current state of the rollout is %s", RolloutStatus.DRAFT, RolloutStatus.READY))));
    }

    @Test
    @Description("Given an active rollout status, when attempting to associate a non-existent software module, then it should return a NotFound status.")
    void givenRolloutStatusWhenAssociatingNonExistentSoftwareModuleThenReturnsNotFound() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        rolloutRepository.save(jpaRollout);
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(4L, 3L);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that an existing DistributionSet is reused for a single rollout when one is already linked to it.")
    void givenDraftRolloutWhenDistributionSetExistsThenReusesItSuccessfullyForSingleRollout() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();

        // Create a DistributionSet
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();

        // Create and link the Rollout with the DistributionSet
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        // Create and associate artifacts with SoftwareModule
        Artifacts artifacts = createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        // Prepare the MgmtSoftwareModuleRequest
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(dsA.getDistributionSetModules().get(0).getSm().getId(), dsA.getDistributionSetModules().get(0).getVersion().getId());
        DistributionSetModule distributionSetModule = new DistributionSetModule((JpaDistributionSet) dsA, (JpaSoftwareModule) sm, (JpaVersion) dsA.getDistributionSetModules().get(0).getVersion());
        distributionSetManagement.createDistributionSetModule(distributionSetModule);

        // Create the request body
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);

        // Perform the POST request and validate response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == HttpStatus.OK.value()) {
                        assertThat(status).isEqualTo(HttpStatus.OK.value());
                    } else if (status == HttpStatus.BAD_REQUEST.value()) {
                        assertThat(result.getResponse().getContentAsString())
                                .contains("Timeout reached while waiting for Distribution Set creation");
                    }
                });
    }


    @Test
    @Description("Given an active rollout status, when attempting to associate a non-existent software module, then it should return not found.")
    void givenDraftRolloutStatusWhenAssociatingNonExistentSoftwareModuleAndNotArtifactThenNotFound() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);

        Rollout rollout1 = createRollout(ROLLOUT_1, RolloutStatus.DRAFT);
        SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        final List<Version> version = Collections.singletonList(testdataFactory.createVersion(sm.getId(), "Test", 12));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(sm.getId(), version.get(0).getId());
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout1.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given an active rollout status, when attempting to associate a non-existent software module, then it should return bad request.")
    void givenDraftRolloutStatusWhenMoreRolloutThenIsOk() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        Rollout rollout1 = createRollout(ROLLOUT_1, RolloutStatus.DRAFT);
        SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        final List<Version> version = Collections.singletonList(testdataFactory.createVersion(sm.getId(), "Test", 12));
        createAndAssociateArtifactWithSoftwareModule(sm, version.get(0));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(sm.getId(), version.get(0).getId());
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout1.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given an active rollout status, when associating an existing software module with an artifact, it should return OK.")
    void givenDraftRolloutStatusWhenAssociatingExistentSoftwareModuleAndNotArtifactThenIsOk() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        Rollout rollout1 = createRollout(ROLLOUT_1, RolloutStatus.DRAFT);
        SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        final List<Version> version = Collections.singletonList(testdataFactory.createVersion(sm.getId(), "Test", 12));
        Artifacts artifacts = createAndAssociateArtifactWithSoftwareModule(sm, version.get(0));
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(sm.getId(), version.get(0).getId());
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout1.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isOk());
    }

    @Test
    @Description("Given an active rollout status, when associating an existing software module with an artifact but not uploaded to cdn, it should return bad request.")
    void givenDraftRolloutStatusWhenAssociatingExistentSoftwareModuleAndArtifactNotUploadedThenIsBadRequest() throws Exception {
        final int amountTargets = 10;
        testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        Rollout rollout1 = createRollout(ROLLOUT_1, RolloutStatus.DRAFT);
        SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        final List<Version> version = Collections.singletonList(testdataFactory.createVersion(sm.getId(), "Test", 12));
        createAndAssociateArtifactWithSoftwareModule(sm, version.get(0));
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(sm.getId(), version.get(0).getId());
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout1.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Ensures that a new DistributionSet is created and associated with a rollout when no DistributionSet is linked to it.")
    void givenDraftRolloutWhenNoDistributionSetExistsThenCreatesNewDistributionSet() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();

        // Create a DistributionSet
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();

        // Create and link the Rollout with the DistributionSet
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        // Create and associate artifacts with SoftwareModule
        Artifacts artifacts = createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());
        changeArtifactStatus(artifactsRepository.findById(artifacts.getId()).get(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        // Prepare the MgmtSoftwareModuleRequest
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(dsA.getDistributionSetModules().get(0).getSm().getId(), dsA.getDistributionSetModules().get(0).getVersion().getId());
        DistributionSetModule distributionSetModule = new DistributionSetModule((JpaDistributionSet) dsA, (JpaSoftwareModule) sm, (JpaVersion) dsA.getDistributionSetModules().get(0).getVersion());
        distributionSetManagement.createDistributionSetModule(distributionSetModule);

        // Create the request body
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);

        // Perform the POST request and validate response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == HttpStatus.OK.value()) {
                        assertThat(status).isEqualTo(HttpStatus.OK.value());
                    } else if (status == HttpStatus.BAD_REQUEST.value()) {
                        assertThat(result.getResponse().getContentAsString())
                                .contains("Timeout reached while waiting for Distribution Set creation");
                    }
                });
    }


    @Test
    @Description("Ensures that the response body contains the required fields.")
    void givenValidAssociationRequestThenResponseHasRequiredFields() throws Exception {
        // Mock external service methods
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();

        // Create a DistributionSet
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();

        // Create and link the Rollout with the DistributionSet
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        // Create and associate artifacts with SoftwareModule
        createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());

        // Prepare the MgmtSoftwareModuleRequest
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(dsA.getDistributionSetModules().get(0).getSm().getId(), dsA.getDistributionSetModules().get(0).getVersion().getId());
        DistributionSetModule distributionSetModule = new DistributionSetModule((JpaDistributionSet) dsA, (JpaSoftwareModule) sm, (JpaVersion) dsA.getDistributionSetModules().get(0).getVersion());
        distributionSetManagement.createDistributionSetModule(distributionSetModule);

        // Create the request body
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);

        // Perform the POST request and validate response
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    if (status == HttpStatus.OK.value()) {
                        assertThat(status).isEqualTo(HttpStatus.OK.value());
                    } else if (status == HttpStatus.BAD_REQUEST.value()) {
                        assertThat(Objects.requireNonNull(result.getResolvedException()).getMessage())
                                .contains(String.format("Artifact for the module %s on target version %s is not uploaded to CDN", request.get(0).getModuleId(), request.get(0).getSoftwareVersionTargetId()));
                    }
                })
                .andExpect(result -> {
                    String responseBody = result.getResponse().getContentAsString();
                    Assertions.assertNotNull(responseBody, "Response body should not be null.");
                });
    }


    @Test
    @Description("Given an draft rollout status, with multi rollouts with same distrSet, then it should return success.")
    void givenDraftRolloutsWhenDistributionSetNotNullAndItIsMultiAndDsModuleDifferentRequestButNoArtifactSmThenSucess() throws Exception {
        when(s3Service.generatePresignedUrl(anyString(), anyString(), anyLong())).thenCallRealMethod();
        when(s3Service.buildS3ObjectName(anyString(), anyString(), anyString())).thenCallRealMethod();
        final DistributionSet dsA = testdataFactory.createDistributionSet("");
        final DistributionSet dsC = testdataFactory.createDistributionSet("C");
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
        createAndAssociateArtifactWithSoftwareModule(sm, dsA.getDistributionSetModules().get(0).getVersion());
        createAndAssociateArtifactWithSoftwareModule(dsC.getDistributionSetModules().get(0).getSm(), dsC.getDistributionSetModules().get(0).getVersion());
        MgmtSoftwareModuleRequest module = createMgmtSoftwareModuleRequest(dsC.getDistributionSetModules().get(0).getSm().getId(), dsC.getDistributionSetModules().get(0).getVersion().getId());
        DistributionSetModule distributionSetModule = new DistributionSetModule((JpaDistributionSet) dsA, (JpaSoftwareModule) sm, (JpaVersion) dsA.getDistributionSetModules().get(0).getVersion());
        distributionSetManagement.createDistributionSetModule(distributionSetModule);
        List<MgmtSoftwareModuleRequest> request = createSoftwareModuleAssociationModelRequest(module);
        String requestBody = objectMapper.writeValueAsString(request);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId()).contentType(MediaType.APPLICATION_JSON).content(requestBody)).andExpect(result -> {
            int status = result.getResponse().getStatus();
            if (status == HttpStatus.OK.value()) {
                assertThat(status).isEqualTo(HttpStatus.OK.value());
            } else {
                assertThat(status).isEqualTo(HttpStatus.BAD_REQUEST.value());
            }
        });

    }

    @Test
    @Description("Ensures that a ValidationException is thrown when attempting to start a rollout " +
            "without all artifacts being successfully uploaded to the CDN, even if RSPs are fully uploaded")
    void givenIncompleteArtifactUploads_WhenRolloutStarts_ThenValidationExceptionIsThrown() throws Exception {
        // Step 1: Prepare RSP test data
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

        rollout.getDistributionSet().getModules().forEach(module -> {
            module.getArtifactSoftwareModuleAssociations().forEach(association -> {
                JpaArtifacts artifact = (JpaArtifacts) association.getArtifact();
                artifact.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
                artifactsRepository.save(artifact);
            });
        });

        rolloutHandler.handleAll();

        rolloutManagement.start(rollout.getId());
        List<JpaRsp> jpaRsps = rspRepository.findAll();
        jpaRsps.forEach(rsp -> {
            rsp.setFileStatus(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name());
            rspRepository.save(rsp);
        });
        Rollout rollout1 = rolloutManagement.get(rolloutId).orElse(null);
        rollout1.getDistributionSet().getModules().forEach(module -> {
            module.getArtifactSoftwareModuleAssociations().forEach(association -> {
                JpaArtifacts artifact = (JpaArtifacts) association.getArtifact();
                artifact.setFileStatus(FileTransferStatus.UPLOADING_TO_CDN.name());
                artifactsRepository.save(artifact);
            });
        });
        rolloutHandler.handleAll();

        assertEquals(RolloutStatus.STARTING, rolloutManagement.get(rolloutId).get().getStatus());
        assertEquals(RolloutGroupStatus.READY, rolloutGroupManagement.findByRollout(PageRequest.of(0, 10), rolloutId).get().findFirst().get().getStatus());


    }


    @Test
    @Description("Ensures that software modules can be unlinked from a Distribution Set for a rollout in the draft state.")
    void givenDraftRolloutWithLinkedSoftwareModulesThenUnlinkSuccessfully() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("DS_A");
        final SoftwareModule sm = dsA.getDistributionSetModules().get(0).getSm();
        final Version version = dsA.getDistributionSetModules().get(0).getVersion();

        final Rollout rollout = testdataFactory.addNewRollout(
                MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1))
        );
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        createAndAssociateArtifactWithSoftwareModule(sm, version);
        List<MgmtSoftwareModuleRequest> unlinkRequests = createSoftwareModuleAssociationModelRequest(
                createMgmtSoftwareModuleRequest(sm.getId(), version.getId())
        );

        String requestBody = objectMapper.writeValueAsString(unlinkRequests);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
        List<IDistributionSetModule> linkedModules = distributionSetManagement.findDsModuleBySoftwareModuleIdAndTargetVersionId(sm.getId(), version.getId());
        Assertions.assertTrue(linkedModules.isEmpty(), "The software module should be unlinked from the Distribution Set.");
    }

    @Description("Create a rollout and add devices without software being associated; it should throw an error")
    @Test
    void givenRolloutWithoutAssociatedSoftware_whenAddingDevices_thenShouldThrowError() throws Exception {
        var rolloutName = "newRollout";
        var rolloutEndDate = Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond();

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);
        final int amountTargets = 10;
        List<Target> targets = testdataFactory.createTargets(amountTargets, "newTarget", "newTargets");


        mvc.perform(put(MgmtRestConstants.ROLLOUT_FREEZE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())).andReturn().getResponse().getContentAsString();

        Path filePath = generateTargetDevicesFile(targets.stream().map(Target::getControllerId).toList());
        MockMultipartFile file = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        var content = mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError()).andReturn().getResponse().getContentAsString();


    }


    @Test
    @Description("Ensures that unlink operation fails when software module ID or version ID is null.")
    void givenNullSoftwareModuleOrVersionInRequestThenValidationException() throws Exception {
        final DistributionSet dsA = testdataFactory.createDistributionSet("DS_A");
        final Rollout rollout = testdataFactory.addNewRollout(
                MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1))
        );
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setDistributionSet(dsA);
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        List<MgmtSoftwareModuleRequest> unlinkRequests = createSoftwareModuleAssociationModelRequest(
                createMgmtSoftwareModuleRequest(null, null) // Invalid request
        );
        List<MgmtSoftwareModuleRequest> emptyUnlinkRequest = Collections.emptyList();
        List<MgmtSoftwareModuleRequest> nullUnlinkRequest = null;

        String requestBody = objectMapper.writeValueAsString(unlinkRequests);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        Assertions.assertTrue(
                                result.getResponse().getContentAsString().contains("softwareVersionTargetId is mandatory for each module."),
                                "Expected validation exception for null Software Module ID/Version ID."
                        ));


        requestBody = objectMapper.writeValueAsString(emptyUnlinkRequest);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        Assertions.assertTrue(
                                result.getResponse().getContentAsString().contains("Software Module ID/Software Version Target ID cannot be Empty or Null"),
                                "Expected validation exception for null Software Module ID/Version ID."
                        ));
        requestBody = objectMapper.writeValueAsString(nullUnlinkRequest);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + ROLLOUT_ID_SM_ENDPOINT, TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());

    }

    @Test
    @Description("Given the rollout is frozen, RSP and ESP are created, there are no mandatory ESP or RSP files, all RSP files are successfully uploaded to storage, but none of the ESP files are successfully uploaded to storage, then it should throw an ValidationException.")
    void givenRolloutFrozen_whenAllRSPUploadedButESPNotUploadedToStorage_thenThrowValidationException() throws Exception {

        mockServer.when(HttpRequest.request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));
        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond(); // Must be at least 2 days ahead

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
        List<MgmtTarget> targets = invokeCreateTargetApi(vehicleModelId, amountTargets);
        List<String> controllerIds = targets.stream().map(MgmtTarget::getControllerId).toList();

        MgmtSoftwareModuleRequest swModuleRequest = testdataFactory.getAssociateSwModuleWithRolloutRequestBody(swModuleId, versionResponse.getId());
        invokeRolloutSoftwareAssociateApi(List.of(swModuleRequest), rollout.getId());
        invokeAddDeviceApi(targets.stream().map(MgmtTarget::getControllerId).toList(), rollout.getId());
        MgmtBaseSupportPackageCreateRequest createRspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.PROXI_SIGNATURE, "someNodeId", supportPackageUrl, List.of());

        log.info("Create rsp Request:{}", createRspRequest);
        MgmtSupportPackage rspResponse = invokeCreateSupportPackageApi(rollout.getId(), createRspRequest);
        JpaRsp rsp = rspRepository.findById(rspResponse.getSupportPackageId()).get();
        rsp.setFileStatus(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL.toString());
        rspRepository.save(rsp);
        MgmtBaseSupportPackageCreateRequest createEspRequest = testdataFactory.getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType.LICENSE, ecuNodeId, supportPackageUrl, controllerIds);
        invokeCreateSupportPackageApi(rollout.getId(), createEspRequest);
        // Clear all previous requests recorded by MockServer
        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        rolloutHandler.handleAll();
        rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertEquals(RolloutStatus.FREEZING, rollout.getStatus());
        mockServer.verify(
                request()
                        .withMethod("POST")
                        .withPath("/kafka/sendEvent"),
                VerificationTimes.atLeast(1)
        );
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
    @Description("When the delete device API is called with valid device details, only the specified device should be removed from the rollout.")
    void givenValidDeviceDetails_whenDeleteDeviceApiCalled_thenOnlyOneDeviceIsRemoved() throws Exception {

        // Prepare groups and their target devices
        List<Target> targets = testdataFactory.createTargets(CONTROLLER_ID1, CONTROLLER_ID2, CONTROLLER_ID3);
        final Rollout rollout = prepareRolloutAndGroupDetails(targets);

        // Generate the target devices file and mock a multipart request
        Path filePath = generateTargetDevicesFile(List.of(CONTROLLER_ID1));
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
    @Description("Verifies that an update request for a rollout succeeds when all required fields are present in the request body, and the 'name' field is not updated.")
    void givenRolloutWhenUpdatingWithValidRequestThenNameIsNotUpdated() throws Exception {
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        String originalName = rollout.getName();
        String updateRolloutRequest = new JSONObject()
                .put("name", "Updated Name")
                .put("description", "Campaign for HPC Update")
                .put("priority", "regular")
                .put("startType", "scheduled")
                .put("userAcceptanceRequired", "yes")
                .put("connectivityType", "cellular")
                .put("startDate", VALID_ROLLOUT_START_DATE)
                .put("endAt", VALID_ROLLOUT_END_DATE)
                .put("type", FOTA)
                .toString();

        // Perform the PUT request and expect a 200 OK status
        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(updateRolloutRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Campaign for HPC Update"))
                .andExpect(jsonPath("$.userAcceptanceRequired").value("yes"))
                .andExpect(jsonPath("$.connectivityType").value("cellular"))
                .andExpect(jsonPath("$.endAt").value(VALID_ROLLOUT_END_DATE))
                .andExpect(jsonPath("$.name").value(originalName));
    }


    @Test
    @Description("Given a rollout with a null name, when the POST endpoint is called, then the response should be 400 Bad Request with a message indicating the name is required.")
    void givenRolloutWithNullName_whenCreatingRollout_thenBadRequestWithNameRequiredMessage() throws Exception {
        final String rollout = JsonBuilder.rollout(null, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Rollout name must not be null")));
    }

    @Test
    @Description("Given a rollout with a empty name, when the POST endpoint is called, then the response should be 400 Bad Request with a message indicating the name is required.")
    void givenRolloutWithEmptyName_whenCreatingRollout_thenBadRequestWithNameRequiredMessage() throws Exception {
        final String rollout = JsonBuilder.rollout("", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Rollout name must not be empty")));
    }

    @Test
    @Description("Given a rollout with a null EdnDate, when the POST endpoint is called, then the response should be 400 Bad Request with a message indicating the name is required.")
    void givenRolloutWithNullEndDate_whenCreatingRollout_thenBadRequestWithNameRequiredMessage() throws Exception {
        final String rollout = JsonBuilder.rollout(ROLLOUT_NAME, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, null, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("End date cannot be null")));
    }

    @Test
    @Description("Given a rollout with a null deploymentEstimatedUpdateTime, when the POST endpoint is called, then the response should be 400 Bad Request with a message indicating the name is required.")
    void givenRolloutWithNullDeploymentEstimatedUpdateTime_whenCreatingRollout_thenBadRequestWithNameRequiredMessage() throws Exception {
        final String rollout = JsonBuilder.rollout(ROLLOUT_NAME, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, null, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("End date cannot be null")));
    }

    @ParameterizedTest
    @EnumSource(value = RolloutStatus.class, names = {"STARTING", "RUNNING", "PAUSING", "PAUSED", "RESUMING", "RETRYING"})
    @Description("Verify that expired rollouts with active statuses transition correctly to finishing and finished states")
    void givenActiveRolloutWhenExpiredThenTransitionsToFinished(RolloutStatus status) throws Exception {
        // setup
        final DistributionSet knownDistributionSet = testdataFactory.createDistributionSet();
        List<Target> targets = testdataFactory.createTargets(
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt(),
                CONTROLLER_ID + testdataFactory.getRandomInt());

        String groupsDetailsJson = "[" +
                createBasicGroupJson("test-group01", 33, 1, 100) + "," +
                createBasicGroupJson("test-group02", 33, 1, 100) + "," +
                createBasicGroupJson("test-group03", 33, 1, 100) + "]";

        createRolloutAndGroups(ROLLOUT, knownDistributionSet, targets, groupsDetailsJson);

        // Retrieve the newly created rollout
        Rollout rollout = rolloutRepository.findByDistributionSetId(knownDistributionSet.getId()).get(0);

        // Freeze and start the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}/groups/next", TENANT_ID, rollout.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Verify initial group statuses
        final List<RolloutGroupStatus> groupStatus = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().stream().map(RolloutGroup::getStatus).collect(Collectors.toList());
        assertThat(groupStatus).containsExactly(RolloutGroupStatus.RUNNING, RolloutGroupStatus.RUNNING, RolloutGroupStatus.QUEUED, RolloutGroupStatus.QUEUED);

        // Set rollout expired and active status
        var pastTimeStamp = Instant.now().atZone(ZoneId.of("UTC")).minusDays(1).toEpochSecond();
        JpaRollout jpaRollout = rolloutRepository.getRolloutById(rollout.getId()).get();
        jpaRollout.setEndAt(pastTimeStamp);
        jpaRollout.setStatus(status);
        rolloutRepository.save(jpaRollout);

        // Handle expired rollout
        rolloutHandler.handleEnd();

        // Verify rollout and groups are in FINISHING state
        rollout = rolloutRepository.getRolloutById(rollout.getId()).get();
        assertEquals(RolloutStatus.FINISHING, rollout.getStatus());
        var rolloutGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        rolloutGroups.forEach(rolloutGroup -> {
            assertEquals(RolloutGroupStatus.FINISHING, rolloutGroup.getStatus());
        });
        ResultActions result = invokeGetTargetsActionsByRolloutIdApi(rollout.getId());
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(targets.size())));


        // Verify actions that are not running are marked as FINISHED_NOT_EXECUTED
        var actions = actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true);
        actions.forEach(action -> {
            if (!action.getStatus().equals(DeviceActionStatus.RUNNING)) {
                assertEquals(DeviceActionStatus.FINISHED_NOT_EXECUTED, action.getStatus());
            }
        });

        // The status of expired rollout updated to FINISHED
        rolloutHandler.handleAll();
        assertEquals(RolloutStatus.FINISHED, rolloutManagement.get(rollout.getId()).get().getStatus());

        // Verify group statuses are FINISHED
        var updatedGroups = rolloutGroupRepository.findByRolloutId(rollout.getId());
        updatedGroups.forEach(group -> {
            assertEquals(RolloutGroupStatus.FINISHED, group.getStatus());
        });
    }
}
