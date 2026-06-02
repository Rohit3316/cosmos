package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.qameta.allure.Description;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtCreateEcuModelResponse;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryIndividualDeviceRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutDeployment;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutLogRequest;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutResponseBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutUpdateRequest;
import org.cosmos.models.mgmt.rollout.dto.RetryMultipleDevicesRequest;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.sns.models.CdnUploadRequest;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModel;
import org.eclipse.hawkbit.repository.jpa.model.JpaEcuModelType;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory.SupportPackageTestData;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.ValidationException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE;
import static org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.RETRY_ROLLOUT_ALL_VEHICLES;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
class MgmtNewRolloutTest extends AbstractManagementRolloutApiIntegrationTest {

    @Autowired
    private HandleRolloutSchedulerService handleRolloutSchedulerService;

    @Test
    @Description("Testing that rollout cannot be created with the invalid userAcceptanceRequired.")
    void createRolloutWithInvalidUserAcceptanceRequiredFails() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), "TEST", MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), VALID_ROLLOUT_START_DATE, INVALID_ROLLOUT_END_DATE, null, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER).content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'userAcceptanceRequired': 'TEST'. Accepted values are: [YES, NO].")));
    }

    @Test
    @Description("Testing rollout creation with valid vehicle log level")
    void createRolloutWithValidVehicleLogLevel() throws Exception {
        testdataFactory.createTargets(10, TARGET, ROLLOUT);

        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout(HPC_UPDATE_1, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null, 3);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).header(SESSION_ID, SESSION_ID_HEADER).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.log.level").value(3));

    }

    @Test
    @Description("Testing rollout creation with invalid vehicle log level should fail")
    void givenInvalidVehicleLogLevelWhenCreatingRolloutThenShouldFail() throws Exception {
        testdataFactory.createTargets(10, TARGET, ROLLOUT);

        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout(HPC_UPDATE_1, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null, 10);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(rollout)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid Vehicle Log Level"));

    }

    @Test
    @Description("Testing rollout creation without providing vehicle log level should set default from tenant configuration")
    void givenNoVehicleLogLevelWhenCreatingRolloutThenDefaultTenantValueShouldBeUsed() throws Exception {
        testdataFactory.createTargets(10, TARGET, ROLLOUT);

        JSONObject deploymentLog = JsonBuilder.createDeploymentLog(true, 5, 5, 50, 5, 5);
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        final String rollout = JsonBuilder.rollout(HPC_UPDATE_1, CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 1, deploymentLog, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(rollout)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.log.level").value(4)); // default tenant config value

    }

    @Test
    @Description("Given a rollout is created with an  Past start date, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidPastStartDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        // Create targets for the rollout
        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        // Set a past date as the start date
        LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
        long startDate = pastDate.atZone(ZoneId.of("UTC")).toEpochSecond();
        // Create a rollout with an invalid start date (null) and perform the POST request
        final String rollout = JsonBuilder.rollout("HPC_UPDATE5", CAMPAIGN_FOR_HPC_UPDATE, MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.SCHEDULED.getName(), MgmtRolloutUserAcceptanceRequired.YES.getName(), MgmtRolloutConnectivityType.CELLULAR.getName(), startDate, VALID_ROLLOUT_END_DATE, null, 1, null, 0, MAX_UPDATE_TIME, 0, 0, null, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID).content(rollout).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a rollout is created with a Invalid Name which contains space, when the request is made, then it should return a bad request status.")
    void givenRolloutCreatedWithInvalidNameWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        testdataFactory.createTargets(10, TARGET, ROLLOUT);

        String rollout = JsonBuilder.rollout(INVALID_ROLLOUT_NAME, "desc", MgmtRolloutPriority.REGULAR.getPriority(), MgmtRolloutStartType.MANUAL.getName(), "YES", MgmtRolloutConnectivityType.WIFI_PREFERRED.getName(), VALID_ROLLOUT_START_DATE, INVALID_ROLLOUT_END_DATE, null, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .header(SESSION_ID, SESSION_ID_HEADER).content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Rollout name must not contain space")));
    }

    @Test
    @Description("Given a rollout with values, when it is created, then it should have all the expected detailed links.")
    void givenRolloutWithValuesWhenCreatedThenShouldHaveExpectedDetailedLinks() throws Exception {
        testdataFactory.createTargets(20, TARGET, ROLLOUT);
        MgmtRolloutRestRequestBody rolloutRequest = new MgmtRolloutRestRequestBody();
        rolloutRequest.setName(HPC_UPDATE_1);
        rolloutRequest.setEndDate(VALID_ROLLOUT_END_DATE);
        rolloutRequest.setStartType(MgmtRolloutStartType.AUTO);
        rolloutRequest.setDeploymentMetadata(MgmtRolloutDeployment.builder().estimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME).build());
        rolloutRequest.setType(MgmtRolloutType.FOTA);


        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$._links").exists())

                // Verifying the links in the Create Rollout API response
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_DELETE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_CANCEL_HREF).doesNotExist())
                .andReturn();
    }

    @Test
    @Description("Given a rollout with deploymentMetadata, when it is updated without touching deploymentMetadata, then the deploymentMetadata remains unchanged.")
    void givenDeploymentMetadataInRollout_whenRolloutIsUpdatedWithoutDeploymentMetadata_thenDeploymentMetadataIsPreserved() throws Exception {
        final int amountTargets = 20;

        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        JSONObject requiredStateOfCharge = new JSONObject()
                .put("BATTERY_PERCENTAGE", "80")
                .put("BATTERY_TEMPERATURE", "35")
                .put("BATTERY_TEMP_METRIC", "56");

        JSONObject deploymentMetadata = new JSONObject()
                .put("requiredStateOfCharge", requiredStateOfCharge)
                .put("requiredMedia", "0")
                .put("downgradeAllowed", "1")
                .put("estimatedUpdateTime", DEPLOYMENT_ESTIMATED_UPDATE_TIME);

        String initialUpdateRequest = new JSONObject()
                .put("name", "Updated Name")
                .put("description", "Campaign for HPC Update")
                .put("priority", "regular")
                .put("startType", "scheduled")
                .put("userAcceptanceRequired", "yes")
                .put("connectivityType", "cellular")
                .put("deploymentMetadata", deploymentMetadata)
                .put("type", FOTA)
                .toString();

        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(initialUpdateRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        String partialUpdateRequest = new JSONObject()
                .put("description", "Campaign for HPC Update")
                .put("priority", "urgent")
                .put("startType", "scheduled")
                .put("userAcceptanceRequired", "no")
                .put("connectivityType", "CELLULAR")
                .put("maxUpdateTime", 1700)
                .put("maxDownloadDurationTimer", 4)
                .put("type", FOTA)
                .toString();

        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(partialUpdateRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, containsString(deploymentMetadata.getString("downgradeAllowed"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, containsString(deploymentMetadata.getString("requiredMedia"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME, equalTo(deploymentMetadata.getInt("estimatedUpdateTime")))).andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE, containsString(requiredStateOfCharge.getString("BATTERY_PERCENTAGE"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE, containsString(requiredStateOfCharge.getString("BATTERY_TEMPERATURE"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC, containsString(requiredStateOfCharge.getString("BATTERY_TEMP_METRIC"))));
    }


    @Test
    @Description("Verifies that an update request returns all the required links in the response.")
    void givenRolloutWhenUpdatedThenShouldHaveExpectedDetailedLinks() throws Exception {
        final int amountTargets = 20;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);
        JSONObject requiredStateOfCharge = new JSONObject()
                .put("BATTERY_PERCENTAGE", "80")
                .put("BATTERY_TEMPERATURE", "35")
                .put("BATTERY_TEMP_METRIC", "56");
        JSONObject deploymentMetadata = new JSONObject()
                .put("requiredStateOfCharge", requiredStateOfCharge)
                .put("requiredMedia", "0")
                .put("downgradeAllowed", "1");

        String updateRolloutRequest = new JSONObject()
                .put("name", "Updated Name")
                .put("description", "Campaign for HPC Update")
                .put("priority", "regular")
                .put("startType", "scheduled")
                .put("userAcceptanceRequired", "yes")
                .put("connectivityType", "cellular")
                .put("deploymentMetadata", deploymentMetadata)
                .put("type", FOTA)
                .toString();

        // Perform the PUT request and expect a 200 OK status
        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(updateRolloutRequest)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$._links").exists())

                // Verifying the links in the Update Rollout API response
                .andExpect(jsonPath(JSON_PATH_LINKS_SELF_HREF, startsWith(HREF_ROLLOUT_PREFIX)))
                .andExpect(jsonPath(JSON_PATH_LINKS_SOFTWARES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SOFTWARES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_SUPPORT_PACKAGES_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(SUPPORT_PACKAGES_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_FREEZE_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(FREEZE_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_GROUPS_HREF, allOf(startsWith(HREF_ROLLOUT_PREFIX), containsString(GROUPS_URL))))
                .andExpect(jsonPath(JSON_PATH_LINKS_START_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_PAUSE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_RESUME_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_TRIGGER_NEXT_GROUP_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_DELETE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_CANCEL_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, containsString(deploymentMetadata.getString("downgradeAllowed"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, containsString(deploymentMetadata.getString("requiredMedia"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_PERCENTAGE, containsString(requiredStateOfCharge.getString("BATTERY_PERCENTAGE"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMPERATURE, containsString(requiredStateOfCharge.getString("BATTERY_TEMPERATURE"))))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_STATE_OF_CHARGE_BATTERY_TEMP_METRIC, containsString(requiredStateOfCharge.getString("BATTERY_TEMP_METRIC"))))
                .andReturn();
    }

    @Test
    @Description("Given a rollout is updated successfully, when it is retrieved using the GET endpoint, then the returned JSON should exactly match the one from the update response.")
    void givenUpdateRolloutWhenRetrievedThenResponseMatchesUpdate() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        // Prepare request body
        JSONObject logObject = new JSONObject()
                .put("collectionRequired", true)
                .put("maxSuccessVin", 10)
                .put("maxFailureVin", 5)
                .put("maxEachFileSize", 1048576)
                .put("maxAllFileSize", 5242880)
                .put("maxNumberOfFiles", 3);

        JSONObject requiredStateOfCharge = new JSONObject()
                .put("BATTERY_PERCENTAGE", "80")
                .put("BATTERY_TEMPERATURE", "35")
                .put("BATTERY_TEMPERATURE", "56");

        JSONObject deploymentMetadata = new JSONObject()
                .put("requiredStateOfCharge", requiredStateOfCharge)
                .put("requiredMedia", "0")
                .put("downgradeAllowed", "1");

        JSONObject updateRequest = new JSONObject()
                .put("name", "Updated Name")
                .put("description", "Campaign for HPC Update")
                .put("priority", "regular")
                .put("startType", "scheduled")
                .put("userAcceptanceRequired", "yes")
                .put("connectivityType", "cellular")
                .put("forcedTime", 1234)
                .put("weight", 1)
                .put("startAt", System.currentTimeMillis() / 1000 + 3600)
                .put("endAt", System.currentTimeMillis() / 1000 + 180000)
                .put("downloadRetryCount", 2)
                .put("maxWifiDownloadDurationTimer", 1)
                .put("maxCellularDownloadDurationTimer", 2)
                .put("maxDownloadDurationTimer", 3)
                .put("maxDownloadWifiDurationTimer", 3)
                .put("maxDownloadCellularDurationTimer", 3)
                .put("maxUpdateTime", 1234)
                .put("log", logObject)
                .put("deploymentMetadata", deploymentMetadata)
                .put("type", FOTA);

        // Send PUT request
        mvc.perform(put(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(updateRequest.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        MvcResult getResult1 = mvc.perform(get(HREF_ROLLOUT_PREFIX + rollout.getId())
                        .content(updateRequest.toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();


        String expectedResponse = new JSONObject(getResult1.getResponse().getContentAsString()).toString(2);

        JSONObject responseJson = new JSONObject(expectedResponse);
        long rolloutId = responseJson.getLong("id");

        MvcResult getResult = mvc.perform(get(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rolloutId)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andReturn();

        String actualResponse = new JSONObject(getResult.getResponse().getContentAsString()).toString(2);

        assertEquals(expectedResponse, actualResponse);
    }


    @Test
    @Description("Testing that a rollout with status CANCELLING cannot be updated and returns a BadRequest response with the correct error message.")
    void givenRolloutWithStatusCancellingWhenUpdatedThenReturnsIsBadRequest() throws Exception {

        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setStatus(RolloutStatus.CANCELING);
        jpaRollout.setType(MgmtRolloutType.FOTA);
        rolloutRepository.save(jpaRollout);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(new ObjectMapper().writeValueAsString(createRolloutUpdateRequest()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot update rollout details for FINISHED/CANCELED/FINISHING/CANCELING state, current state: " + RolloutStatus.CANCELING));
    }

    @Test
    @Description("Testing that a rollout with status FINISHING cannot be updated and returns a BadRequest response with the correct error message.")
    void givenRolloutWithStatusFinishingWhenUpdatedThenReturnsIsBadRequest() throws Exception {

        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));

        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setStatus(RolloutStatus.FINISHING);
        jpaRollout.setType(MgmtRolloutType.FOTA);
        rolloutRepository.save(jpaRollout);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .header(SESSION_ID, SESSION_ID_HEADER)
                        .content(new ObjectMapper().writeValueAsString(createRolloutUpdateRequest()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot update rollout details for FINISHED/CANCELED/FINISHING/CANCELING state, current state: " + RolloutStatus.FINISHING));
    }


    private MgmtRolloutUpdateRequest createRolloutUpdateRequest() {
        MgmtRolloutUpdateRequest rolloutRequest = new MgmtRolloutUpdateRequest();
        rolloutRequest.setDescription("Test rollout update");
        rolloutRequest.setForcedTime(System.currentTimeMillis());
        rolloutRequest.setWeight(5);

        rolloutRequest.setStartAt(System.currentTimeMillis() + 10000);
        rolloutRequest.setEndAt(System.currentTimeMillis() + 20000);

        // Setting up Log details
        MgmtRolloutLogRequest logRequest = new MgmtRolloutLogRequest();
        logRequest.setCollectionRequired(false);
        logRequest.setMaxSuccessVin(10);
        logRequest.setMaxFailureVin(5);
        logRequest.setMaxEachFileSize(1048576);
        logRequest.setMaxAllFileSize(10485760);
        logRequest.setMaxNumberOfFiles(10);
        rolloutRequest.setLog(logRequest);
        rolloutRequest.setDownloadRetryCount(3);
        rolloutRequest.setMaxDownloadDurationTimer(600);
        rolloutRequest.setMaxDownloadWifiDurationTimer(300);
        rolloutRequest.setMaxDownloadCellularDurationTimer(900);

        // Setting up Deployment Metadata
        MgmtRolloutDeployment deploymentMetadata = new MgmtRolloutDeployment();
        deploymentMetadata.setRequiredMedia(null);
        deploymentMetadata.setDowngradeAllowed(MgmtRolloutDowngradeAllowed.NO);
        Map<MgmtRolloutRequiredStateOfCharge, String> requiredStateOfChargeMap = new HashMap<>();
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE, "63%");
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE, "88 C");
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC, "72 C");

        deploymentMetadata.setRequiredStateOfCharge(requiredStateOfChargeMap);
        rolloutRequest.setDeploymentMetadata(deploymentMetadata);

        rolloutRequest.setUserAcceptanceRequired(null);
        rolloutRequest.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        rolloutRequest.setMaxUpdateTime(3600);
        rolloutRequest.setType(MgmtRolloutType.FOTA);
        return rolloutRequest;
    }

    private MgmtRolloutUpdateRequest createRolloutUpdateRequestWithDetails() {
        MgmtRolloutUpdateRequest rolloutRequest = new MgmtRolloutUpdateRequest();
        // Setting up Log details
        MgmtRolloutLogRequest logRequest = new MgmtRolloutLogRequest();
        logRequest.setCollectionRequired(false);
        logRequest.setMaxSuccessVin(100);
        logRequest.setMaxFailureVin(20);
        logRequest.setMaxEachFileSize(209715);
        logRequest.setMaxAllFileSize(1048576);
        logRequest.setMaxNumberOfFiles(5);
        rolloutRequest.setLog(logRequest);
        rolloutRequest.setDownloadRetryCount(5);
        rolloutRequest.setMaxDownloadDurationTimer(6);
        rolloutRequest.setMaxDownloadWifiDurationTimer(3);
        rolloutRequest.setMaxDownloadCellularDurationTimer(3);

        // Setting up Deployment Metadata
        MgmtRolloutDeployment deploymentMetadata = new MgmtRolloutDeployment();
        deploymentMetadata.setRequiredMedia(MgmtRolloutRequiredMedia.FROM_CDN);
        deploymentMetadata.setDowngradeAllowed(MgmtRolloutDowngradeAllowed.NO);
        Map<MgmtRolloutRequiredStateOfCharge, String> requiredStateOfChargeMap = new HashMap<>();
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_PERCENTAGE, "63%");
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_TEMPERATURE, "88 C");
        requiredStateOfChargeMap.put(MgmtRolloutRequiredStateOfCharge.BATTERY_TEMP_METRIC, "72 C");

        deploymentMetadata.setRequiredStateOfCharge(null);
        rolloutRequest.setDeploymentMetadata(deploymentMetadata);

        rolloutRequest.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        rolloutRequest.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        rolloutRequest.setMaxUpdateTime(1800);
        return rolloutRequest;
    }

    private MgmtRolloutUpdateRequest createSampleRolloutUpdateRequest(MgmtRolloutStartType... startType) {
        MgmtRolloutUpdateRequest rolloutRequest = new MgmtRolloutUpdateRequest();
        rolloutRequest.setDescription("Test rollout update");
        rolloutRequest.setPriority(MgmtRolloutPriority.REGULAR);
        rolloutRequest.setStartType(startType.length > 0 ? startType[0] : MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        rolloutRequest.setStartAt(startType[0].equals(MgmtRolloutStartType.SCHEDULED) ? Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond() : null);
        rolloutRequest.setEndAt(Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond());
        rolloutRequest.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        rolloutRequest.setMaxPackageSize(MAX_PACKAGE_FILE_SIZE);
        MgmtRolloutDeployment deploymentMetadata = new MgmtRolloutDeployment();
        deploymentMetadata.setRequiredMedia(MgmtRolloutRequiredMedia.FROM_CDN);
        deploymentMetadata.setDowngradeAllowed(MgmtRolloutDowngradeAllowed.NO);
        deploymentMetadata.setRequiredStateOfCharge(null);
        deploymentMetadata.setEstimatedUpdateTime(100);
        rolloutRequest.setDeploymentMetadata(deploymentMetadata);
        rolloutRequest.setType(MgmtRolloutType.FOTA);
        rolloutRequest.setVehicleLogLevel(5);
        return rolloutRequest;
    }

    private MgmtRolloutRestRequestBody createRolloutCreateRequest() {
        MgmtRolloutRestRequestBody rolloutRequest = new MgmtRolloutRestRequestBody();
        rolloutRequest.setName(HPC_UPDATE_1);
        rolloutRequest.setPriority(MgmtRolloutPriority.REGULAR);
        rolloutRequest.setStartType(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        rolloutRequest.setStartDate(Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond());
        rolloutRequest.setEndDate(Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond());
        rolloutRequest.setDeploymentMetadata(MgmtRolloutDeployment.builder().estimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME)
                .build());
        //rolloutRequest.setDeploymentEstimatedUpdateTime(DEPLOYMENT_ESTIMATED_UPDATE_TIME);
        rolloutRequest.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        rolloutRequest.setType(MgmtRolloutType.FOTA);

        return rolloutRequest;
    }

    @Test
    @Description("If rollout groups are not created, calling addDeviceDetails returns ok request")
    void givenRolloutGroupsNotCreatedWhenAddDeviceDetailsApiThenOk() throws Exception {
        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        DistributionSet ds = testdataFactory.createDistributionSet();
        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);
        List<Target> targets = testdataFactory.createTargets(3, ROLLOUT, ROLLOUT);
        Path filePath = generateTargetDevicesFile(List.of(targets.get(0).getControllerId(), targets.get(1).getControllerId(), targets.get(2).getControllerId()));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));

        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
    }

    @Test
    @Description("If target devices are already associated with the rollout, calling addDeviceDetails returns a bad request")
    void givenRolloutGroupsAlreadyCreatedWhenAddDeviceDetailsApiThenBadRequest() throws Exception {
        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        DistributionSet ds = testdataFactory.createDistributionSet();
        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        List<Target> targets = testdataFactory.createTargets(3, ROLLOUT, ROLLOUT);
        Path filePath = generateTargetDevicesFile(List.of(targets.get(0).getControllerId(), targets.get(1).getControllerId(), targets.get(2).getControllerId()));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest()).andExpect(jsonPath(JSON_PATH_MESSAGE, equalTo("No new registered target devices found in the file"))).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @Description("If few target devices already associated with the rollout, calling addDeviceDetails with new devices returns a ok request with list of newly added registered device and duplicate devices.")
    void givenRolloutGroupsAlreadyCreatedWhenAddDeviceDetailsApiThenOk() throws Exception {
        Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT_1)));
        DistributionSet ds = testdataFactory.createDistributionSet();
        associateRolloutWithDistributionSet(ds, (JpaRollout) rollout);

        List<Target> targets = testdataFactory.createTargets(3, ROLLOUT, ROLLOUT);
        Path filePath = generateTargetDevicesFile(List.of(targets.get(0).getControllerId(), targets.get(1).getControllerId(), targets.get(2).getControllerId()));
        MockMultipartFile file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        Target target = testdataFactory.createTarget("rollout3");
        filePath = generateTargetDevicesFile(List.of(targets.get(0).getControllerId(), targets.get(1).getControllerId(), targets.get(2).getControllerId(), target.getControllerId()));
        file1 = new MockMultipartFile(TARGET_DEVICES, ORIGINAL_FILENAME, CONTENT_TYPE, Files.newInputStream(filePath));
        mvc.perform(MockMvcRequestBuilders.multipart(MgmtRestConstants.ADD_DEVICE_DETAILS_REQUEST_MAPPING, 1L, rollout.getId()).file(file1).contentType(MediaType.MULTIPART_FORM_DATA).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.groups[0].targetDevices[0]").value("rollout3")).andExpect(jsonPath("$.duplicateTargetDevices[0]").value(targets.get(0).getControllerId())).andExpect(jsonPath("$.duplicateTargetDevices[1]").value(targets.get(1).getControllerId())).andExpect(jsonPath("$.duplicateTargetDevices[2]").value(targets.get(2).getControllerId()));
    }


    @Test
    @Description("testing all the params must update when rollout is in draft state ")
    void givenRolloutInDraftState() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(rolloutRequest.getDescription())))
                .andExpect(jsonPath(JSON_PATH_STATUS, equalTo(DRAFT)))
                .andExpect(jsonPath(JSON_PATH_START_AT, equalTo(rolloutRequest.getStartAt().intValue())))
                .andExpect(jsonPath(JSON_PATH_END_AT, equalTo(rolloutRequest.getEndAt().intValue())))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, equalTo(rolloutRequest.getUserAcceptanceRequired().getName())))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, equalTo(rolloutRequest.getConnectivityType().getName())))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, equalTo(rolloutRequest.getStartType().getName())))
                .andExpect(jsonPath(JSON_PATH_PRIORITY, equalTo(rolloutRequest.getPriority().getPriority())))
                .andExpect(jsonPath(JSON_PATH_MAX_PACKAGE_FILE_SIZE).exists())
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_DOWNGRADE_ALLOWED, equalTo(rolloutRequest.getDeploymentMetadata().getDowngradeAllowed().getValue())))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_REQUIRED_MEDIA, equalTo(rolloutRequest.getDeploymentMetadata().getRequiredMedia().getValue())))
                .andExpect(jsonPath(JSON_PATH_DEPLOYMENT_METADATA_ESTIMATED_UPDATE_TIME, equalTo(rolloutRequest.getDeploymentMetadata().getEstimatedUpdateTime())))
                .andExpect(jsonPath(JSON_PATH_LOG_LEVEL).exists());

    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with invalid type 'ABCD', then return Bad Request error")
    void givenRolloutInDraftStateWhenUpdatingWithInvalidTypeThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        ObjectNode node = objectMapper.valueToTree(rolloutRequest);
        node.put("type", "ABCD");
        String invalidJson = objectMapper.writeValueAsString(node);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(invalidJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'type': 'ABCD'. Accepted values are: [FOTA, AOTA].")));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with invalid type '', then return Bad Request error")
    void givenRolloutInDraftStateWhenUpdatingWithEmptyTypeThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        ObjectNode node = objectMapper.valueToTree(rolloutRequest);
        node.put("type", "");
        String invalidJson = objectMapper.writeValueAsString(node);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(invalidJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'type': ''. Accepted values are: [FOTA, AOTA].")));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with invalid type null, then return Bad Request error")
    void givenRolloutInDraftStateWhenUpdatingWithNullTypeThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        ObjectNode node = objectMapper.valueToTree(rolloutRequest);
        node.putNull("type");
        String invalidJson = objectMapper.writeValueAsString(node);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(invalidJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parameters[0]").value("type cannot be null"))
                .andExpect(jsonPath("$.message").value("Some Parameters are missing"));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating without the 'type' field, then return Bad Request with 'type cannot be null'")
    void givenRolloutInDraftStateWhenUpdatingWithoutTypeThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        ObjectNode node = objectMapper.valueToTree(rolloutRequest);
        node.remove("type");
        String invalidJson = objectMapper.writeValueAsString(node);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(invalidJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.parameters[0]").value("type cannot be null"))
                .andExpect(jsonPath("$.message").value("Some Parameters are missing"));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with type 'AOTA' but without 'updateAction', then return Bad Request indicating that 'updateAction' is mandatory")
    void givenRolloutInDraftStateWhenUpdatingWithAOTAWithoutUpdateActionThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setType(MgmtRolloutType.AOTA);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Update Action is mandatory with type as AOTA and should be one of INSTALL/UNINSTALLANY/UNINSTALLSPECIFIC")));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with type 'AOTA' and a valid 'updateAction', then the update succeeds with HTTP 200 OK")
    void givenRolloutInDraftStateWhenUpdatingWithAOTAAndValidUpdateActionThenReturnOk() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setType(MgmtRolloutType.AOTA);
        rolloutRequest.setUpdateAction(MgmtUpdateAction.INSTALL);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with type 'AOTA' and 'updateAction' as 'UNINSTALLANY', then the update succeeds with HTTP 200 OK")
    void givenRolloutInDraftStateWhenUpdatingWithAOTAAndUninstallAnyUpdateActionThenReturnOk() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setType(MgmtRolloutType.AOTA);
        rolloutRequest.setUpdateAction(MgmtUpdateAction.UNINSTALLANY);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with type 'AOTA' and 'updateAction' as 'UNINSTALLSPECIFIC' without specifying versions, then return Bad Request indicating that 'updateActionUninstallVersion' is mandatory")
    void givenRolloutInDraftStateWhenUpdatingWithAOTAAndUninstallSpecificWithoutVersionsThenReturnBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setType(MgmtRolloutType.AOTA);
        rolloutRequest.setUpdateAction(MgmtUpdateAction.UNINSTALLSPECIFIC);
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Update Action Uninstall Version is mandatory with updateAction as UNINSTALLSPECIFIC")));
    }

    @Test
    @Description("Given a rollout in DRAFT state, when updating with type 'AOTA' and 'updateAction' as 'UNINSTALLSPECIFIC' with valid 'updateActionUninstallVersion', then the update succeeds with HTTP 200 OK")
    void givenRolloutInDraftStateWhenUpdatingWithAOTAAndUninstallSpecificWithVersionsThenReturnOk() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setType(MgmtRolloutType.AOTA);
        rolloutRequest.setUpdateAction(MgmtUpdateAction.UNINSTALLSPECIFIC);
        rolloutRequest.setUpdateActionUninstallVersion(List.of("68541061AA000000000000000000007670181252000006002JY0000"));
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());
    }


    @Test
    @Description("Ensure only end date and log level is updated when rollout is not in draft state")
    void givenRolloutNotInDraftStateOnlyEndDateUpdated_success() throws Exception {
        // Create a rollout with a non-draft state (e.g., IN_PROGRESS)ents
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_2,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));
        rolloutManagement.freeze(rollout.getId()); // Setting rollout status to non-draft

        // Creating update request with only the end date changed
        MgmtRolloutUpdateRequest rolloutRequest = createRolloutUpdateRequestWithDetails();
        rolloutRequest.setEndAt(Instant.now().plus(49, ChronoUnit.HOURS).getEpochSecond()); //  end date is updated
        rolloutRequest.setVehicleLogLevel(6);  //log level is updated
        rolloutRequest.setEndAt(Instant.now().plus(49, ChronoUnit.HOURS).getEpochSecond()); // Only end date is updated
        rolloutRequest.setType(MgmtRolloutType.FOTA);

        // Performing the update request
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_END_AT, equalTo(rolloutRequest.getEndAt().intValue())))
                .andExpect(jsonPath(JSON_PATH_LOG_LEVEL).value(6));
    }

    @Test
    @Description("Ensure validation exception is thrown when trying to update fields other than end date when rollout is not in draft state")
    void givenRolloutNotInDraftState_whenOtherFieldsUpdated_thenValidationException() throws Exception {
        // Create a rollout with a non-draft state (e.g., IN_PROGRESS)
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_2,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));
        rolloutManagement.freeze(rollout.getId()); // Setting rollout status to non-draft

        // Creating update request with fields other than the end date changed

        MgmtRolloutUpdateRequest rolloutRequest = createRolloutUpdateRequest();
        rolloutRequest.setEndAt(rollout.getEndAt());
        rolloutRequest.setDescription("New Description"); // Attempting to update description

        // Performing the update request
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))

                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Only End Date and Log Level can be updated, current state: " + RolloutStatus.FREEZING)));
    }


    @Test
    @Description("Given a rollout is updated with an invalid or past end date, when the request is made, then it should return a bad request status.")
    void givenRolloutUpdatedWithInvalidOrPastEndDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(ROLLOUT, MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT), rollout, true);

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.MANUAL);
        rolloutRequest.setEndAt(Instant.now().minus(3, ChronoUnit.DAYS).getEpochSecond());

        // Perform the POST request and verify the response
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("End Date (in UTC Time Zone) must be at least 2 days ahead in the future")));
    }

    @Test
    @Description("Given a rollout is updated with an invalid or past start date, when the request is made, then it should return a bad request status.")
    void givenRolloutUpdatedWithInvalidOrPastStartDateWhenRequestIsMadeThenReturnsBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), testdataFactory.createTargets(3, ROLLOUT, ROLLOUT));

        MgmtRolloutUpdateRequest rolloutRequest = createSampleRolloutUpdateRequest(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setStartType(MgmtRolloutStartType.SCHEDULED);
        rolloutRequest.setStartAt(Instant.now().minus(3, ChronoUnit.DAYS).getEpochSecond());


        // Perform the POST request and verify the response
        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Start Date cannot be in the past")));
    }

    @Test
    @Description("Given a rollout with a specific start type and priority, when the rollout is created, then the response should reflect the same start type and priority.")
    void givenRolloutWithStartTypeAndPriorityWhenCreatedThenResponseShouldReflectSame() throws Exception {
        MgmtRolloutRestRequestBody rolloutRequest = createRolloutCreateRequest();

        MvcResult result = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(objectMapper.writeValueAsString(rolloutRequest))
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, equalTo(rolloutRequest.getPriority().getPriority())))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, equalTo(rolloutRequest.getStartType().getName())))
                .andReturn();
        String responseBody = result.getResponse().getContentAsString();
        MgmtRolloutRestRequestBody response = objectMapper.readValue(responseBody, MgmtRolloutRestRequestBody.class);
        assertEquals(rolloutRequest.getPriority(), response.getPriority());
        assertEquals(rolloutRequest.getStartType(), response.getStartType());
    }


    @Test
    @Description("Given an invalid device action state, when canceling a device action, then it should return a bad request status.")
    void givenInvalidDeviceStatusWhenCancelActionThenSuccess() throws Exception {
        mockServer.when(request().withMethod("HEAD").withPath("/some-file"))
                .respond(HttpResponse.response().withHeader("Content-Length", "12345").withStatusCode(200));

        var port = System.getProperty("mock.server.port");
        var rolloutName = "newRollout";
        var fileUrl = "http://localhost:" + port + "/some-file";
        var supportPackageUrl = "https://docs.aws.amazon.com/it_it/whitepapers/latest/overview-aws-cloud-adoption-framework/overview-aws-cloud-adoption-framework.pdf";
        var rolloutEndDate = Instant.now().plus(49, ChronoUnit.HOURS).getEpochSecond();

        Long vehicleModelId = createVehicleModel();
        List<MgmtCreateEcuModelResponse> ecuModelResponses = createEcuModel();
        String ecuNodeId = getEcuNodeId(ecuModelResponses.get(0));
        Long ecuModelId = ecuModelResponses.get(0).getId();
        associateEcuModelToVehicleModel(vehicleModelId, ecuModelId);

        long swModuleId = createSoftwareModule();
        MgmtAddVersionResponse versionResponse = createSoftwareVersion(swModuleId);
        MgmtArtifacts artifacts = createArtifact(fileUrl);
        associateArtifactWithSoftwareModule(swModuleId, versionResponse.getId(), artifacts.getArtifactId());

        invokeCreateRolloutApi(rolloutName, rolloutEndDate, MgmtRolloutStartType.AUTO, FOTA, null, null);
        Rollout rollout = rolloutManagement.getByName(rolloutName).orElse(null);
        assertNotNull(rollout);

        List<MgmtTarget> targets = createTargets(vehicleModelId, 1);
        List<String> controllerIds = getControllerIds(targets);

        associateSoftwareModuleWithRollout(swModuleId, versionResponse.getId(), rollout.getId());
        MgmtAddDeviceDetailsResponse groupsResponse = addDeviceDetails(controllerIds, rollout.getId(), 1);


        MgmtSupportPackage rspResponse = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.WHATS_NEW, "", List.of());
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp1Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_CERTIFICATE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        MgmtSupportPackage esp2Response = createSupportPackage(rollout.getId(), supportPackageUrl, MgmtSupportPackageFileType.ADA_LICENSE, ecuNodeId, controllerIds);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL);

        mockServer.clear(request());
        invokeRolloutFreezeApi(rollout.getId());
        handleRollout();
        handleRollout();
        updateFileStatus(RSP, rspResponse.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp1Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        updateFileStatus(ESP, esp2Response.getSupportPackageId(), FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
        handleRollout();
        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        handleRollout();

        verifyActionStatusAndTransition(groupsResponse, rollout.getId());
        rolloutHandler.handleAll();

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), targets.get(0).getControllerId(), true);
        actions.get().setStatus(DeviceActionStatus.FINISHED_SUCCESS);
        actionRepository.save(actions.get());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), targets.get(0).getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE,
                        equalTo("Cannot cancel this Device because it is not in RUNNING, PAUSED or DD_SENT state. Current status:finished_success")));

        final Optional<JpaAction> updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), targets.get(0).getControllerId(), true);
        assertEquals(DeviceActionStatus.FINISHED_SUCCESS, updatedactions.get().getStatus());
    }

    @Test
    @Description("Given device action state as Running, when canceling a device action, then it should return 202 status.")
    void givenDeviceResumedWhenCancelActionThenSuccess() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true);
        actions.get().setStatus(DeviceActionStatus.RUNNING);
        actionRepository.save(actions.get());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        final Optional<JpaAction> updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, false);
        assertEquals(DeviceActionStatus.CANCELED, updatedactions.get().getStatus());
    }

    @Test
    @Description("Given device action state as Paused, when canceling a device action, then it should return 202 status.")
    void givenDevicePausedWhenCancelActionThenSuccess() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true);
        actions.get().setStatus(DeviceActionStatus.PAUSED);
        actionRepository.save(actions.get());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        final Optional<JpaAction> updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, false);
        assertEquals(DeviceActionStatus.CANCELED, updatedactions.get().getStatus());
    }

    private Rollout prepareSupportPackageTestData(
            String targetPrefix,
            String rolloutPrefix,
            String rolloutName,
            String distributionSetName,
            String ecuNodeAddress,
            MgmtSupportPackageFileType fileType,
            List<Target> targets
    ) throws Exception {
        SupportPackageTestData customTestData = new SupportPackageTestData();
        customTestData.setTargetPrefix(targetPrefix);
        customTestData.setRolloutPrefix(rolloutPrefix);
        customTestData.setRolloutName(rolloutName);
        customTestData.setDistributionSetName(distributionSetName);
        customTestData.setFileType(fileType);
        customTestData.setEcuNodeAddress(ecuNodeAddress);

        var testData = prepareTestDataForCreatingSupportPackageTest(customTestData, targets);
        Rollout rollout = (Rollout) testData.get(ROLLOUT);
        MgmtBaseSupportPackageCreateRequest requestBody = (MgmtBaseSupportPackageCreateRequest) testData.get(REQUEST_BODY);

        createSupportPackage(rollout.getId(), requestBody).andExpect(status().isCreated());
        return rollout;
    }

    private Map<String, Object> prepareTestDataForCreatingSupportPackageTest(SupportPackageTestData testData, List<Target> targets) throws Exception {
        //Create ECU Model
        List<EcuModel> ecuModels = new ArrayList<>();
        if (!testData.getEcuNodeAddress().isEmpty()) {
            ecuModels = testdataFactory.addNewEcuModels(createEcuModel(testData.getEcuNodeAddress()));
        }
        assignEcuModelsToVehicle(targets, ecuModels);

        // Create rollout including the created targets with the provided prefix
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);


        // Create request body using the provided parameters
        List<String> vins = targets.stream().map(Target::getControllerId).toList();
        MgmtBaseSupportPackageCreateRequest requestBody = MgmtFileUrlSupportPackageCreateRequest.builder()
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

    private ResultActions createSupportPackage(Long rolloutId, MgmtBaseSupportPackageCreateRequest requestBody) throws Exception {
        return createSupportPackage(rolloutId, objectMapper.writeValueAsString(requestBody));
    }

    private ResultActions createSupportPackage(Long rolloutId, String requestBody) throws Exception {
        return mvc.perform(post(MgmtRestConstants.ROLLOUT_SUPPORTPACKAGE_FILEURL_V1_REQUEST_MAPPING_TENANT_TARGETS, 1, rolloutId).contentType("application/json").content(requestBody));
    }


    @Test
    @Description("Given device action state as DD_SENT, when canceling a device action, then it should return 202 status.")
    void givenDeviceDDSentWhenCancelActionThenSuccess() throws Exception {
        final int amountTargets = 2;
        final List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT);
        Rollout rollout = prepareSupportPackageTestData(
                "ESPSetTarget",
                "ESPRolloutPrefix",
                "ESPRolloutName", TEST_ECU_NODE_ADDRESS_1, "ESPDistributionSetName",
                MgmtSupportPackageFileType.LICENSE,
                targets
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
        });

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), targets.get(0).getControllerId(), true);
        actions.get().setStatus(DeviceActionStatus.DD_SENT);
        actionRepository.save(actions.get());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_DEVICE_ACTION_CANCEL_V1_MAPPING_TENANT, TENANT_ID, rollout.getId(), targets.get(0).getControllerId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isAccepted());

        final Optional<JpaAction> updatedactions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), targets.get(0).getControllerId(), false);
        assertEquals(DeviceActionStatus.CANCELED, updatedactions.get().getStatus());
    }

    @Test
    @Description("When deleting a group with an invalid rollout, status, or non-existing group, it should return Not Found or Bad Request based on the error type.")
    void givenInvalidOrNonExistingRolloutOrGroupRequestOrStatusWhenDeletingGroupThenReturnsNotFoundOrBadRequest() throws Exception {
        final int amountTargets = 8;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);
        RolloutGroup rolloutGroup = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().get(0);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, 101L, rolloutGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId(), 100L)
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        final Rollout rollout2 = createRolloutWithDependencies(ROLLOUT_2, testdataFactory.createDistributionSet(), targets);
        final RolloutGroup secondGroup = rolloutGroupManagement.findByRollout(PageRequest.of(0, 1, Sort.Direction.ASC, "id"), rollout2.getId()).getContent().get(0);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId(), secondGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout = rolloutRepository.findById(jpaRollout.getId()).orElseThrow();
        jpaRollout.setStatus(RolloutStatus.FREEZING);
        rolloutRepository.save(jpaRollout);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, jpaRollout.getId(), rolloutGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        jpaRollout = rolloutRepository.findById(jpaRollout.getId()).orElseThrow();
        jpaRollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(jpaRollout);

        JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) rolloutGroup;
        jpaRolloutGroup.setStatus(RolloutGroupStatus.FREEZING);
        rolloutGroupRepository.save(jpaRolloutGroup);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId(), jpaRolloutGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Given a valid rollout without targets deletion request, when deleting a group, it as then it should return a SUCCESS status")
    void givenRolloutWithoutTargetsDeletionRequestWhenDeletingGroupThenReturnsOk() throws Exception {
        final int amountTargets = 8;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);
        List<Long> targetIds = targets.stream().map(Target::getId).collect(Collectors.toList());
        targetManagement.delete(targetIds);
        RolloutGroup rolloutGroup = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().get(0);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId(), rolloutGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given a rollout with some target devices already associated, when calling addDeviceDetails with new devices, then it should return an OK response.")
    void givenRolloutWithAssociatedTargetsWhenAddingNewDevicesThenReturnsOk() throws Exception {
        final int noT1Tags = 3;
        final int amountTargets = 3;
        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT_1, testdataFactory.createDistributionSet(), targets);
        final List<TargetTag> t1Tags = testdataFactory.createTargetTags(noT1Tags, "NAME" + testdataFactory.getRandomInt());
        t1Tags.forEach(tag -> targetManagement.assignTag(Collections.singletonList(targets.get(0).getControllerId()), tag.getId()));
        RolloutGroup rolloutGroup = rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getContent().get(0);
        mvc.perform(delete(MgmtRestConstants.ROLLOUT_GROUPID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId(), rolloutGroup.getId())
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given rollout state as Paused, when canceling a rollout, then it should return Success.")
    void givenRolloutPausedWhenCancelRolloutThenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        rolloutHandler.handleAll();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rollout state as Paused, when canceling a rollout, then it should return Success.")
    void givenRolloutRunningWhenCancelRolloutThenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rollout state as Starting, when canceling a rollout, then it should return Bad request.")
    void givenInvalidStateWhenCancelRolloutThenBadRequest() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE,
                        equalTo("The requested operation can be performed only when the Rollout is in one of the statuses [RUNNING, PAUSED] but the current state of the rollout is PAUSING")));

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSING, updatedRollout.getStatus());
    }

    @Test
    @Description("Given case insensitive enums, when creating or updating rollout, then return success")
    void givenCaseInsensitiveEnumsWhenCreatingOrUpdatingRolloutThenReturnSuccess() throws Exception {

        // Mixedcase
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("regular")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("manual")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("yes")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("wifi_preferred"))).andReturn();
        MgmtRolloutResponseBody mgmtRolloutResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtRolloutResponseBody.class);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + mgmtRolloutResponseBody.getRolloutId(), TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("regular")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("manual")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("yes")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("wifi_preferred")));

        // Uppercase
        rollout = JsonBuilder.rollout(ROLLOUT_2, "desc", "CRITICAL", "SCHEDULED", "NO", "CELLULAR", VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        mvcResult = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("critical")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("scheduled")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("no")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("cellular"))).andReturn();
        mgmtRolloutResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtRolloutResponseBody.class);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + mgmtRolloutResponseBody.getRolloutId(), TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("critical")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("scheduled")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("no")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("cellular")));

        // Lowercase
        rollout = JsonBuilder.rollout(ROLLOUT, "desc", "urgent", "auto", "no", "wifi_only", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        mvcResult = mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("urgent")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("auto")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("no")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("wifi_only"))).andReturn();
        mgmtRolloutResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtRolloutResponseBody.class);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/" + mgmtRolloutResponseBody.getRolloutId(), TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_PRIORITY, containsString("urgent")))
                .andExpect(jsonPath(JSON_PATH_STARTTYPE, containsString("auto")))
                .andExpect(jsonPath(JSON_PATH_USER_ACCEPTANCE_REQUIRED, containsString("no")))
                .andExpect(jsonPath(JSON_PATH_CONNECTIVITY_TYPE, containsString("wifi_only")));
    }

    @Test
    @Description("Given rollout state as Running with three rollout groups, one in QUEUED state, one in RUNNING state and other is in PAUSED state, then it should return Success.")
    void givenRolloutWithThreeGroupsWhenOneQueuedOneRunningAndOnePausedThenSuccess() throws Exception {
        Rollout rollout = createRolloutwithTargetsAndRolloutGroups(3, 3);

        // Set the first rollout group to QUEUED state
        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.QUEUED);
        rolloutGroupRepository.save(firstGroup);

        // Set the second rollout group to RUNNING state
        JpaRolloutGroup secondGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(1);
        secondGroup.setStatus(RolloutGroupStatus.RUNNING);
        rolloutGroupRepository.save(secondGroup);

        // Set the third rollout group to PAUSED state
        JpaRolloutGroup thirdGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(2);
        thirdGroup.setStatus(RolloutGroupStatus.PAUSED);
        rolloutGroupRepository.save(thirdGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        // Assertions for the first group in QUEUED state
        RolloutGroup updatedFirstGroup = rolloutGroupRepository.findById(firstGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.CANCELED, updatedFirstGroup.getStatus());

        // Assertions for the second group in RUNNING state
        RolloutGroup updatedSecondGroup = rolloutGroupRepository.findById(secondGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.CANCELED, updatedSecondGroup.getStatus());

        // Assertions for the third group in PAUSED state
        RolloutGroup updatedThirdGroup = rolloutGroupRepository.findById(thirdGroup.getId()).orElseThrow();
        assertEquals(RolloutGroupStatus.CANCELED, updatedThirdGroup.getStatus());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rollout state as Running, when canceling a rollout, then it should return Success.")
    void givenRolloutRunningWhenRolloutInCancelingThenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rollout state as Running, when canceling a rollout, then it should return Success.")
    void givenRolloutPausedWhenRolloutInCancelingThenSuccess() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        rolloutHandler.handleAll();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());
    }

    @Test
    @Description("Given rolloutGroup as FINISHED When rollout is in RUNNING then call API and scheduler and rollout status should be in CANCELING")
    void givenInvalidGroupStateWhenRolloutRunningThenRolloutInCanceling() throws Exception {
        Rollout rollout = createRolloutWithDependencies();

        JpaRolloutGroup firstGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        firstGroup.setStatus(RolloutGroupStatus.FINISHED);
        rolloutGroupRepository.save(firstGroup);

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();

        Rollout updatedRolloutAPI = rolloutRepository.findById(rollout.getId()).orElseThrow();

        assertEquals(RolloutStatus.CANCELING, updatedRolloutAPI.getStatus());
    }

    @Test
    @Description("Given a new rollout in DRAFT state with associated targets, when deleting the rollout, then it should be deleted successfully along with its dependencies.")
    void givenCreateNewRollout_whenRolloutIsInDraft_thenRolloutDeleteSuccessful() throws Exception {
        final int amountTargets = 20;

        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        mvc.perform(get(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        rolloutHandler.handleAll();
        mvc.perform(get(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Given a new rollout in FREEZING state with associated targets, when deleting the rollout, then it should fail to delete the rollout.")
    void givenCreateNewRollout_whenRolloutIsInFreezing_thenRolloutDeleteFailed() throws Exception {
        final int amountTargets = 20;

        List<Target> targets = testdataFactory.createTargets(amountTargets, ROLLOUT, ROLLOUT);
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), targets);

        mvc.perform(get(MgmtRestConstants.ROLLOUT_ID_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        rolloutManagement.freeze(rollout.getId());

        mvc.perform(delete(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT + "/{rolloutId}", TENANT_ID, rollout.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Testing RetryMultipleDevice functionality for MgmtRolloutResource.")
    void testRetryMultipleDevice() throws Exception {
        // Arrange: Create targets and a rollout
        Rollout rollout = createRolloutWithDependencies();
        Optional<JpaRollout> rollout1 = rolloutRepository.findById(rollout.getId());
        RetryMultipleDevicesRequest request = new RetryMultipleDevicesRequest();
        request.setDescription("Retry operation for failed devices");
        request.setRetryMode(RetryMode.ALL_CANCELED_VEHICLES); // or another valid enum value
        request.setStartType(MgmtRolloutStartType.SCHEDULED); // or another valid enum value
        request.setStartDate(1697040000L);
        request.setEndDate(1697126400L);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rollout1.get().getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())

                // Assert: Verify the response
                .andExpect(status().isOk());
    }

    @Test
    @Description("Testing RetryMultipleDevice with Vehicle Log Level for MgmtRolloutResource.")
    void givenMultipleDevicesFinishedWhenRetryMultipleDevicesWithValidVehicleLogLevelThenReturnSuccessful() throws Exception {
        // Arrange: Create rollout and related entities
        Rollout rollout = createRolloutWithDependencies();
        Optional<JpaRollout> rolloutOptional = rolloutRepository.findById(rollout.getId());
        assertTrue(rolloutOptional.isPresent(), "Rollout should exist in DB after creation");

        RetryMultipleDevicesRequest request = new RetryMultipleDevicesRequest();
        request.setDescription("Retry operation for failed devices with custom vehicle log level");
        request.setRetryMode(RetryMode.ALL_FAILED_VEHICLES);
        request.setStartType(MgmtRolloutStartType.SCHEDULED);
        request.setStartDate(Instant.now().getEpochSecond());
        request.setEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        request.setVehicleLogLevel(4); // <-- NEW field under test

        String jsonRequest = objectMapper.writeValueAsString(request);

        // Act: Perform PUT request
        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rolloutOptional.get().getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Assert: Verify rollout actions updated with correct log level
        List<JpaAction> updatedActions = actionRepository.findActionByRolloutIdAndActive(rolloutOptional.get().getId(), true);
        assertFalse(updatedActions.isEmpty(), "Updated actions should not be empty");

        for (JpaAction action : updatedActions) {
            assertEquals(4, action.getVehicleLogLevel(), "Vehicle log level should match request input (4)");
        }

        // Also verify rollout persisted correctly
        JpaRollout updatedRollout = rolloutRepository.findById(rolloutOptional.get().getId()).orElseThrow();
        assertEquals(RetryMode.ALL_FAILED_VEHICLES, updatedRollout.getLastRetryMode(), "Retry mode should match request input");
    }

    @Test
    @Description("Given invalid vehicle log level when retrying multiple devices then ValidationException should be thrown")
    void givenInvalidVehicleLogLevelWhenRetryMultipleDevicesThenValidationExceptionThrown() throws Exception {
        // Given: an existing rollout with dependencies
        Rollout rollout = createRolloutWithDependencies();
        Optional<JpaRollout> rolloutOptional = rolloutRepository.findById(rollout.getId());
        assertTrue(rolloutOptional.isPresent(), "Rollout should " +
                "exist in DB after creation");

        RetryMultipleDevicesRequest request = new RetryMultipleDevicesRequest();
        request.setDescription("Retry operation with invalid vehicle log level");
        request.setRetryMode(RetryMode.ALL_FAILED_VEHICLES);
        request.setStartType(MgmtRolloutStartType.SCHEDULED);
        request.setStartDate(Instant.now().getEpochSecond());
        request.setEndDate(Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond());
        request.setVehicleLogLevel(9);

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then: performing the retry call should result in a ValidationException
        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rolloutOptional.get().getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException().getMessage().contains("Invalid Vehicle Log Level"),
                        "Exception message should mention invalid vehicle log level"
                ));
    }


    @Test
    @Description("Given invalid vehicle log level for individual and full rollout retry then ValidationException should be thrown")
    void givenInvalidVehicleLogLevelWhenRetryAcrossEndpointsThenValidationExceptionThrown() throws Exception {

        Rollout rolloutIndividual = setupRolloutForIndividualRetry();
        String invalidIndividualJson = objectMapper.writeValueAsString(buildInvalidIndividualRetryRequest());

        performInvalidRetryAndAssert(
                put(MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE, TENANT_ID, rolloutIndividual.getId(), CONTROLLER_ID),
                invalidIndividualJson
        );

        Rollout rolloutFull = setupRolloutForFullRetry();
        String invalidFullJson = objectMapper.writeValueAsString(buildInvalidFullRetryRequest());

        performInvalidRetryAndAssert(
                put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rolloutFull.getId()),
                invalidFullJson
        );
    }

    private Rollout setupRolloutForIndividualRetry() {
        Rollout rollout = createRolloutWithDependencies(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(CONTROLLER_ID)
        );
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Mark existing action finished
        actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true)
                .ifPresent(a -> {
                    a.setActive(false);
                    a.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
                    actionRepository.save(a);
                });

        return rollout;
    }

    private Rollout setupRolloutForFullRetry() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        handleRollout();

        // Enable full rollout retry feature
        String bodyActivate = new JSONObject().put("value", true).toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        return rollout;
    }

    private MgmtRetryIndividualDeviceRequestBody buildInvalidIndividualRetryRequest() {
        MgmtRetryIndividualDeviceRequestBody req = new MgmtRetryIndividualDeviceRequestBody();
        req.setDescription("Retry operation with invalid vehicle log level");
        req.setRetryMode(RetryMode.INDIVIDUAL_CANCELED_VEHICLES);
        req.setStartType(MgmtRolloutStartType.SCHEDULED);
        req.setStartDate(VALID_ROLLOUT_START_DATE);
        req.setEndDate(VALID_ROLLOUT_END_DATE);
        req.setVehicleLogLevel(9);
        return req;
    }

    private MgmtRetryFullRolloutRequestBody buildInvalidFullRetryRequest() {
        MgmtRetryFullRolloutRequestBody req = new MgmtRetryFullRolloutRequestBody();
        req.setDescription("Retry full rollout with invalid vehicle log level");
        req.setStartType(MgmtRolloutStartType.SCHEDULED);
        req.setStartDate(VALID_ROLLOUT_START_DATE);
        req.setEndDate(VALID_ROLLOUT_END_DATE);
        req.setVehicleLogLevel(9);
        return req;
    }

    private void performInvalidRetryAndAssert(MockHttpServletRequestBuilder requestBuilder, String requestJson) throws Exception {
        mvc.perform(requestBuilder
                        .content(requestJson)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(
                        result.getResolvedException().getMessage().contains("Invalid Vehicle Log Level"),
                        "Expected exception message to mention invalid vehicle log level"
                ));
    }


    @Test
    @Description("Negative test: RetryMultipleDevice request with missing required fields should return BadRequest.")
    void testRetryMultipleDevice_Negative_MissingRetryMode() throws Exception {
        // Arrange: Create targets and a rollout
        Rollout rollout = createRolloutWithDependencies();
        Optional<JpaRollout> rollout1 = rolloutRepository.findById(rollout.getId());
        RetryMultipleDevicesRequest request = new RetryMultipleDevicesRequest();
        request.setDescription("Missing retryMode field");
        // request.setRetryMode(null); // Intentionally not set
        request.setStartType(MgmtRolloutStartType.SCHEDULED);
        request.setStartDate(1697040000L);
        request.setEndDate(1697126400L);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rollout1.get().getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Testing RetryIndividualDevice with given Rollout in draft state then retry device return exception")
    void givenRolloutInDraftStatusWhenRetryIndividualDeviceThenThrowException() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        Optional<JpaRollout> rollout1 = rolloutRepository.findById(rollout.getId());
        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setDescription("Retry operation for failed devices");
        request.setRetryMode(RetryMode.INDIVIDUAL_CANCELED_VEHICLES); // or another valid enum value
        request.setStartType(MgmtRolloutStartType.SCHEDULED); // or another valid enum value
        request.setStartDate(1697040000L);
        request.setEndDate(1697126400L);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE, TENANT_ID, rollout1.get().getId(), CONTROLLER_ID)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @Description("Testing RetryIndividualDevice with given Rollout in running state and individual device in finished then return Successful")
    void givenRolloutInRunningStatusWhenRetryFinishedIndividualDeviceThenReturnSuccessful() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rollout.getId(), CONTROLLER_ID, true);
        actions.get().setActive(false);
        actions.get().setStatus(DeviceActionStatus.FINISHED_SUCCESS);
        actionRepository.save(actions.get());

        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setDescription("Retry operation for failed devices");
        request.setRetryMode(RetryMode.INDIVIDUAL_CANCELED_VEHICLES); // or another valid enum value
        request.setStartType(MgmtRolloutStartType.SCHEDULED); // or another valid enum value
        request.setStartDate(1697040000L);
        request.setEndDate(1697126400L);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given rollout in RUNNING state when retrying an individual device with valid log level then return Successful")
    void givenRolloutInRunningStateWhenRetryIndividualDeviceWithValidLogLevelThenReturnSuccessful() throws Exception {
        // Given rollout and dependencies
        final Rollout rollout = createRolloutWithDependencies(
                ROLLOUT,
                testdataFactory.createDistributionSet(),
                testdataFactory.createTargets(CONTROLLER_ID)
        );
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        // Given an existing finished device action
        final Optional<JpaAction> action = actionRepository.getActionByRolloutIdAndControllerId(
                rollout.getId(), CONTROLLER_ID, true);
        action.get().setActive(false);
        action.get().setStatus(DeviceActionStatus.FINISHED_SUCCESS);
        actionRepository.save(action.get());

        // Prepare retry request with valid log level
        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setDescription("Retry operation with valid log level");
        request.setRetryMode(RetryMode.INDIVIDUAL_CANCELED_VEHICLES);
        request.setStartType(MgmtRolloutStartType.SCHEDULED);
        request.setStartDate(VALID_ROLLOUT_START_DATE);
        request.setEndDate(VALID_ROLLOUT_END_DATE);
        request.setVehicleLogLevel(4); // valid log level

        String jsonRequest = objectMapper.writeValueAsString(request);

        // When & Then
        mvc.perform(put(MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Verify log level was updated
        JpaAction updatedAction = actionRepository.findById(action.get().getId()).orElseThrow();
        assertEquals(4, updatedAction.getVehicleLogLevel());
    }

    @Test
    @Description("Testing RetryIndividualDevice with given Rollout in finished state and individual device in finished then return Successful")
    void givenRolloutInFinishedStatusWhenRetryFinishedIndividualDeviceThenReturnSuccessful() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        List<JpaAction> actions = actionRepository.findByRolloutIdAndRolloutGroupId(rollout.getId(), rolloutGroup.getId(), true);
        for (JpaAction action : actions) {
            action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
            action.setActive(false);
            actionRepository.save(action);
        }

        handleRollout();//running to finishing
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHING, updatedRollout.getStatus());
        handleRollout();//finishing to finished

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHED, updatedRollout.getStatus());

        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setDescription("Retry operation for failed devices");
        request.setRetryMode(RetryMode.INDIVIDUAL_CANCELED_VEHICLES); // or another valid enum value
        request.setStartType(MgmtRolloutStartType.MANUAL); // or another valid enum value
        request.setEndDate(Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond());

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_INDIVIDUAL_DEVICE, TENANT_ID, rollout.getId(), CONTROLLER_ID)
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());

    }

    @Test
    @Description("test for retryMultipleDevices API should return 200 OK with action,rolloutTargetgroups updated accordingly.")
    void givenFailedRollout_whenRetrySchedulerRuns_thenActionsAreFlagged() throws Exception {

        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutId(rollout.getId()).get(0);
        List<JpaAction> actions = actionRepository.findByRolloutIdAndRolloutGroupId(rollout.getId(), rolloutGroup.getId(), true);
        for (JpaAction action : actions) {
            action.setStatus(DeviceActionStatus.FINISHED_FAILURE);
            action.setActive(false);
            actionRepository.save(action);
        }

        handleRollout();//running to finishing
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHING, updatedRollout.getStatus());
        handleRollout();//finishing to finished

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FINISHED, updatedRollout.getStatus());

        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setDescription("Retry operation for failed devices");
        request.setRetryMode(RetryMode.ALL_FAILED_VEHICLES); // or another valid enum value
        request.setStartType(MgmtRolloutStartType.MANUAL); // or another valid enum value
        request.setEndDate(Instant.now().plus(3, ChronoUnit.DAYS).getEpochSecond());

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rollout.getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        handleRolloutSchedulerService.handleRetryingRollout((JpaRollout) updatedRollout);

        // Verify the Rollout's final status
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow(); // Re-fetch from DB
        assertEquals(RolloutStatus.RETRY, updatedRollout.getStatus(), "Scheduler handler must transition status to RETRY.");

    }

    @Test
    @Description("test that retryMultipleDevices API returns 400 BAD REQUEST if the rollout status is not FINISHED.")
    void givenRunningRollout_whenRetryMultipleDevicesApiIsCalled_thenReturnsBadRequest() throws Exception {
        final Rollout rollout = createRolloutWithDependencies(ROLLOUT, testdataFactory.createDistributionSet(), testdataFactory.createTargets(CONTROLLER_ID));
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        updateRolloutGroupsToReadyState(rollout.getId());
        rolloutManagement.start(rollout.getId());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();

        MgmtRetryIndividualDeviceRequestBody request = new MgmtRetryIndividualDeviceRequestBody();
        request.setRetryMode(RetryMode.ALL_FAILED_VEHICLES);
        request.setStartType(MgmtRolloutStartType.MANUAL);

        String jsonRequest = objectMapper.writeValueAsString(request);

        mvc.perform(put(MgmtRestConstants.RETRY_MULTIPLE_DEVICES, TENANT_ID, rollout.getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }
    @Test
    @Description("Retry FULL Rollout when Rollout is in FINISHED/CANCELED,Rollout goes to RETRYING state")
    void given_CancelRollout_WhenRetryFullRollout_ThenRolloutINRetrying() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);

        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(RetryMode.FULL, updatedRollout.getLastRetryMode());

    }

    @Test
    @Description("When Retry Full Rollout is attempted with a start date for a non-SCHEDULED rollout start type it throws Bad Exception")
    void given_RetryFullRollout_WhenStartDateIsNotValid_ThenThrowException() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.AUTO, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);


        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start Date can be provided only for SCHEDULED rollout and the current rollout startType is " + rollout.getStartType()));


    }

    @Test
    @Description("When a Retry Full Rollout is attempted with a null end date it throws Bad Exception")
    void given_RetryFullRollout_WhenEndDateIsNotValid_ThenThrowException() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.AUTO, null, null);
        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date cannot be null"));
    }

    @Test
    @Description("Given VehicleLogLevel when Retry Full Rollout then the value is persisted in the Rollout table")
    void givenVehicleLogLevelWhenRetryFullRolloutThenPersistedInRollout() throws Exception {
        // Given: create a rollout and cancel it so it becomes eligible for retry
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout canceledRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, canceledRollout.getStatus());

        // Enable full rollout retry feature
        String bodyActivate = new JSONObject().put("value", true).toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        // When: Retry full rollout with a specific vehicle log level
        MgmtRetryFullRolloutRequestBody requestBody = new MgmtRetryFullRolloutRequestBody();
        requestBody.setDescription("Retry Full Rollout with vehicle log level");
        requestBody.setStartType(MgmtRolloutStartType.SCHEDULED);
        requestBody.setStartDate(VALID_ROLLOUT_START_DATE);
        requestBody.setEndDate(VALID_ROLLOUT_END_DATE);
        requestBody.setVehicleLogLevel(5);
        String jsonRequest = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(jsonRequest)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // Then: Verify rollout status and log level updated correctly
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(5, updatedRollout.getVehicleLogLevel());
    }


    @Test
    @Description("This test verifies that a rollout can be successfully cloned irrespective of status, and the cloned rollout has the expected name, description, and status")
    void givenRollout_WhenRolloutIsCreated_ThenRolloutCanBeCloned() throws Exception {
        Rollout originalRollout = createRolloutWithDependencies();
        Rollout rollout = rolloutRepository.findById(originalRollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, rollout.getStatus());

        MgmtCloneRolloutRequestBody requestBody = cloneRolloutRequestBody("cloneRollout1", "This is cloned rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);
        String jsonObject = objectMapper.writeValueAsString(requestBody);

        MvcResult mvcResult = mvc.perform(post(MgmtRestConstants.CLONE_ROLLOUT, TENANT_ID, originalRollout.getId())
                        .content(jsonObject).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated()).andReturn();
        MgmtRolloutResponseBody mgmtRolloutResponseBody = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), MgmtRolloutResponseBody.class);
        Rollout clonedRollout = rolloutRepository.findById(mgmtRolloutResponseBody.getRolloutId()).orElseThrow();
        assertEquals("cloneRollout1", clonedRollout.getName());
        assertEquals("This is cloned rollout", clonedRollout.getDescription());
        assertEquals(RolloutStatus.CLONING, clonedRollout.getStatus());
    }

    @Test
    @Description("Throws BadRequest when cloning a rollout with AUTO start type and a start date, which is only allowed for SCHEDULED start type.")
    void giveCloneRollout_WhenSartDateIsInvalid_ThenThrowException() throws Exception {
        Rollout originalRollout = createRolloutWithDependencies();
        Rollout rollout = rolloutRepository.findById(originalRollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RUNNING, rollout.getStatus());

        MgmtCloneRolloutRequestBody requestBody = cloneRolloutRequestBody("cloneRollout1", "This is cloned rollout", MgmtRolloutStartType.AUTO, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);
        String jsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(post(MgmtRestConstants.CLONE_ROLLOUT, TENANT_ID, originalRollout.getId())
                        .content(jsonObject).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Start Date can be provided only for SCHEDULED rollout and the current rollout startType is " + originalRollout.getStartType()));
    }

    @Test
    @Description("Throws BadRequest when cloning a canceled rollout with null end date.")
    void givenCanceledRollout_WhenEndDateIsNull_ThenThrowException() throws Exception {
        Rollout originalRollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, originalRollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(originalRollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        MgmtCloneRolloutRequestBody requestBody = cloneRolloutRequestBody("cloneRollout1", "This is cloned rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, null);
        String jsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(post(MgmtRestConstants.CLONE_ROLLOUT, TENANT_ID, originalRollout.getId())
                        .content(jsonObject).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("End date cannot be null"));
    }

    @Test
    @Description("Throws a Bad Request when cloning a rollout with same name as an existing rollout.")
    void givenCloneRollout_WhenRolloutNameExists_ThenThrowException() throws Exception {
        Rollout rollout = createRolloutWithDependencies();
        mvc.perform(put(MgmtRestConstants.ROLLOUT_PAUSE_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.PAUSING, updatedRollout.getStatus());

        MgmtCloneRolloutRequestBody requestBody = cloneRolloutRequestBody("cloneRollout1", "This is cloned rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);
        String jsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(post(MgmtRestConstants.CLONE_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(jsonObject).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated()).andReturn();

        mvc.perform(post(MgmtRestConstants.CLONE_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(jsonObject).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("The given entity already exists in database"));


    }


    @Test
    @Description("Given null rollout type with mixed case enums, when creating rollout, then return Bad Request error")
    void givenNullRolloutTypeWithMixedCaseEnumsWhenCreatingRolloutThenReturnBadRequest() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, null, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Rollout type should be FOTA or AOTA")));
    }

    @Test
    @Description("Given empty rollout type with mixed case enums, when creating rollout, then return Bad Request error")
    void givenEmptyRolloutTypeWithMixedCaseEnumsWhenCreatingRolloutThenReturnBadRequest() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, null, null, null);

        JSONObject rolloutJson = new JSONObject(rollout);
        rolloutJson.put("type", "");
        String updatedRollout = rolloutJson.toString();
        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(updatedRollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'type': ''. Accepted values are: [FOTA, AOTA].")));
    }

    @Test
    @Description("Given rollout type FOTA with mixed case enums, when creating rollout, then return Created status")
    void givenRolloutTypeFOTAWithMixedCaseEnumsWhenCreatingRolloutThenReturnCreated() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, FOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                .content(rollout).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Given rollout type AOTA with mixed case enums and missing UpdateAction, when creating rollout, then return Bad Request error")
    void givenRolloutTypeAOTAWithMissingUpdateActionWhenCreatingRolloutThenReturnBadRequest() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Update Action is mandatory with type as AOTA and should be one of INSTALL/UNINSTALLANY/UNINSTALLSPECIFIC")));
    }

    @Test
    @Description("Given rollout type AOTA with mixed case enums and valid UpdateAction INSTALL, when creating rollout, then return Created status")
    void givenRolloutTypeAOTAWithValidUpdateActionINSTALLWhenCreatingRolloutThenReturnCreated() throws Exception {

        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, INSTALL, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                .content(rollout).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Given rollout type AOTA with mixed case enums and valid UpdateAction UNINSTALLANY, when creating rollout, then return Created status")
    void givenRolloutTypeAOTAWithValidUpdateActionUNINSTALLANYWhenCreatingRolloutThenReturnCreated() throws
            Exception {

        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, UNINSTALLANY, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                .content(rollout).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Given rollout type AOTA with UpdateAction UNINSTALLSPECIFIC and missing versions to uninstall, when creating rollout, then return Bad Request error")
    void givenRolloutTypeAOTAWithUNINSTALLSPECIFICAndMissingVersionsWhenCreatingRolloutThenReturnBadRequest() throws
            Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, UNINSTALLSPECIFIC, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Update Action Uninstall Version is mandatory with updateAction as UNINSTALLSPECIFIC")));
    }

    @Test
    @Description("Given rollout type AOTA with mixed case enums and valid UpdateAction UNINSTALLSPECIFIC with specified versions, when creating rollout, then return Created status")
    void givenRolloutTypeAOTAWithValidUpdateActionUNINSTALLSPECIFICAndVersionsWhenCreatingRolloutThenReturnCreated
            () throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0, new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%").put(BATTERY_TEMPERATURE.name(), "78 C").put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);
        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD", null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2, null, AOTA, UNINSTALLSPECIFIC, Arrays.asList("SCOMO_12345:1.0.0", "SCOMO_12345:1.0.1", "SCOMO_67890:2.5.3"));

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                .content(rollout).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());
    }

    @Test
    @Description("Given invalid rollout type ABC, when creating rollout, then return Bad Request error")
    void givenInvalidRolloutTypeABCWhenCreatingRolloutThenReturnBadRequest() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0,
                new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%")
                        .put(BATTERY_TEMPERATURE.name(), "78 C")
                        .put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD",
                null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2,
                null, "ABC", null, null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'type': 'ABC'. Accepted values are: [FOTA, AOTA].")));
    }

    @Test
    @Description("Given rollout type AOTA with invalid UpdateAction XYZ, when creating rollout, then return Bad Request error")
    void givenRolloutTypeAOTAWithInvalidUpdateActionXYZWhenCreatingRolloutThenReturnBadRequest() throws Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0,
                new JSONObject().put(BATTERY_PERCENTAGE.name(), "60%")
                        .put(BATTERY_TEMPERATURE.name(), "78 C")
                        .put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD",
                null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2,
                null, AOTA, "XYZ", null);

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_MESSAGE, containsString("Invalid value for field 'updateAction'")));
    }

    @Test
    @Description("Given rollout type AOTA with UpdateAction UNINSTALLSPECIFIC and single uninstall version, when creating rollout, then return Created status")
    void givenRolloutTypeAOTAWithUNINSTALLSPECIFICAndSingleVersionWhenCreatingRolloutThenReturnCreated() throws
            Exception {
        JSONObject deploymentMetaData = JsonBuilder.createDeploymentMetaData(0, 0,
                new JSONObject()
                        .put(BATTERY_PERCENTAGE.name(), "60%")
                        .put(BATTERY_TEMPERATURE.name(), "78 C")
                        .put(BATTERY_TEMP_METRIC.name(), "NA"), 1000);

        String rollout = JsonBuilder.rollout(ROLLOUT_1, "desc", "ReGuLaR", "MaNuAl", "YeS", "WiFi_PrEfErReD",
                null, VALID_ROLLOUT_END_DATE, deploymentMetaData, 5, null, 3, MAX_UPDATE_TIME, 1, 2,
                null, AOTA, UNINSTALLSPECIFIC, List.of("68541061AA000000000000000000007670181252000006002JY0000"));

        mvc.perform(post(MgmtRestConstants.ROLLOUT_V1_REQUEST_MAPPING_TENANT, TENANT_ID)
                        .content(rollout).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }

    @Test
    @Description("given rollout start time is in the past greater than 1 day, when the rollout is freezing, then rollout be in freezing state")
    void givenStartAtInPastWhenRolloutIsFreezingThenRolloutStuckInFreezing() throws Exception {
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);

        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(RetryMode.FULL, updatedRollout.getLastRetryMode());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRY, updatedRollout.getStatus());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());

        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        jpaRollout.setStartAt(Instant.now().minus(2, ChronoUnit.DAYS).getEpochSecond());
        rolloutRepository.save(jpaRollout);

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());
    }

    @Test
    @Description("given rollout start time is in the past greater than 1 day, when the rollout is Ready, then rollout be in ready state")
    void givenStartAtInPastWhenRolloutIsReadyThenRolloutStuckInFreezing() throws Exception {
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.SCHEDULED, VALID_ROLLOUT_START_DATE, VALID_ROLLOUT_END_DATE);

        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(RetryMode.FULL, updatedRollout.getLastRetryMode());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRY, updatedRollout.getStatus());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());

        invokeRolloutFreezeApi(updatedRollout.getId());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.READY, updatedRollout.getStatus());

        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        jpaRollout.setStartAt(Instant.now().minus(2, ChronoUnit.DAYS).getEpochSecond());
        rolloutRepository.save(jpaRollout);

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.READY, updatedRollout.getStatus());
    }

    @Test
    @Description("given artifact which has association with SCOMO and its version as PURGED, when rollout goes to freezing since all the artifacts are PURGED, then it should be in freezing state")
    void givenPurgedArtifactWhenFreezingRolloutThenRolloutStuckInFreezing() throws Exception{
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        MgmtArtifacts artifacts = (MgmtArtifacts) setup.get("artifacts");
        JpaArtifacts jpaArtifacts = artifactsRepository.findById(artifacts.getArtifactId()).orElseThrow();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.MANUAL, null, VALID_ROLLOUT_END_DATE);

        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(RetryMode.FULL, updatedRollout.getLastRetryMode());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRY, updatedRollout.getStatus());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());

        invokePurgeArtifactsApi(jpaArtifacts);

        JpaArtifacts updatedArtifact = artifactsRepository.findById(jpaArtifacts.getId()).orElseThrow();
        assertEquals(ArtifactsStatus.PURGED.name(), updatedArtifact.getArtifactStatus());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());
    }

    @Test
    @Description("given artifact signature expiration in the past or current, when artifact status is ACTIVE, then rollout be in freezing state")
    void givenPastExpirationForArtifactWhenFreezingRolloutThenRolloutStuckInFreezing() throws Exception{
        Map<String, Object> setup = createAndTransitionRolloutToRunning();
        Rollout rollout = (Rollout) setup.get("rollout");
        MgmtArtifacts artifacts = (MgmtArtifacts) setup.get("artifacts");
        JpaArtifacts jpaArtifacts = artifactsRepository.findById(artifacts.getArtifactId()).orElseThrow();

        mvc.perform(put(MgmtRestConstants.ROLLOUT_CANCEL_V1_REQUEST_MAPPING_TENANT, TENANT_ID, rollout.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        handleRollout();
        Rollout updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.CANCELED, updatedRollout.getStatus());

        String bodyActivate = new JSONObject().put("value", true).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, TENANT_ID, RETRY_ROLLOUT_ALL_VEHICLES)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        MgmtRetryFullRolloutRequestBody requestBody = retryFullRolloutCreateRequestBody("Retry operation for Full Rollout", MgmtRolloutStartType.MANUAL, null, VALID_ROLLOUT_END_DATE);

        String JsonObject = objectMapper.writeValueAsString(requestBody);

        mvc.perform(put(MgmtRestConstants.RETRY_FULL_ROLLOUT, TENANT_ID, rollout.getId())
                        .content(JsonObject)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRYING, updatedRollout.getStatus());
        assertEquals(RetryMode.FULL, updatedRollout.getLastRetryMode());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.RETRY, updatedRollout.getStatus());

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());

        jpaArtifacts.setExpiryDate(Instant.now().minus(1, ChronoUnit.DAYS).getEpochSecond());
        artifactsRepository.save(jpaArtifacts);

        handleRollout();

        updatedRollout = rolloutRepository.findById(rollout.getId()).orElseThrow();
        assertEquals(RolloutStatus.FREEZING, updatedRollout.getStatus());
    }
}