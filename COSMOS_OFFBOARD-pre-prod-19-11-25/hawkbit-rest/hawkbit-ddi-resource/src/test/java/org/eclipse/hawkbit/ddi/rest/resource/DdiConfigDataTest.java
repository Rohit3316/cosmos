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
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.hateoas.MediaTypes;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.eclipse.hawkbit.repository.RepositoryConstants.INVENTORY_HASH_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test config data from the controller.
 */
@ActiveProfiles({"im", "test"})
@Feature("Component Tests - Direct Device Integration API")
@Story("Config Data Resource")
class DdiConfigDataTest extends AbstractDDiApiIntegrationTest {

    private static final String TIME = "00:01:00";
    private static final String JAVA_PATH_CONFIG_POLLING_SLEEP = "$.config.polling.sleep";
    private static final String JSON_PATH_LINKS_INVENTORY_HREF = "$._links.inventory.href";
    private static final String DEVICE_V_1_CONTROLLERS_INVENTORY_BASE_URL = "http://localhost/device/v1/controllers/4712/inventory";
    private static final String JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF = "$._links.deploymentBase.href";
    private static final String DIFFERENT_INVENTORY_HASH = "differentInventoryHash";
    private static final String JSON_PATH_LINKS_INSTALLED_BASE_HREF = "$._links.installedBase.href";
    private static final String VALUE_1231231236 = "1231231236";
    private static final String VALUE_T1237813XX = "T1237813xx";
    private static final String VALUE_4712 = "4712";
    private static final String HASH = "hash";
    @MockBean
    private SnsAsyncClient snsAsyncClient;

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
    void setup() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate,
                SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_VERSIONS,
                SP_SOFTWARE_ECU_MODEL, SP_VEHICLE_MODEL, SP_TARGET_INVENTORY,
                SP_BASE_SOFTWARE_MODULE, SP_RSP_ROLLOUT, SP_ESP_ECU_ROLLOUT,
                SP_ESP, SP_RSP, SP_TARGET, SP_SOFTWARE_ECU_MODEL,
                SP_BASE_SOFTWARE_MODULE, SP_ROLLOUT
        );
        MockitoAnnotations.initMocks(this);
        PublishResponse mockPublishResponse = PublishResponse.builder()
                .messageId("mockMessageId")
                .build();
        // Create a completed CompletableFuture with the mock response
        CompletableFuture<PublishResponse> completedFuture = CompletableFuture.completedFuture(mockPublishResponse);
        when(snsAsyncClient.publish(any(PublishRequest.class))).thenReturn(completedFuture);
    }

    @Autowired
    private ActionRepository actionRepository;

    @Description("Verify that deployment link is not present when device action status is PAUSED.")
    @Test
    public void givenDeviceActionIsPausedWwhenFetchingInventoryHashThenShouldNotContainDeploymentLink() throws Exception {
        final String inventoryHash = "testInventoryHash";
        final DistributionSet ds = testdataFactory.createDistributionSet("");
        final Target savedTarget = testdataFactory.createTarget(VALUE_4712);
        assignDistributionSet(ds.getId(), savedTarget.getControllerId());

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        updateAction.setStatus(DeviceActionStatus.PAUSED);
        actionRepository.save((JpaAction) updateAction);

        mvc.perform(get(DdiRestConstants.GET_INVENTORY_HASH_PATH, VALUE_4712)
                        .param(HASH, inventoryHash))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_INSTALLED_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF).doesNotExist());
    }

    @Test
    @Description("We verify that the config data (i.e. device attributes like serial number, hardware revision etc.) "
            + "are requested each time the inventoryHash is empty / updated.")
    @SuppressWarnings("squid:S2925")
    void requestConfigDataIfEmptyOrUpdatedInventoryHash() throws Exception {
        final Target savedTarget = setupRolloutAndGetTarget();

        final String inventoryHash = "testInventoryHash";
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, inventoryHash))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON_VALUE))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, equalTo(
                        "http://localhost/device/v1/controllers/"+savedTarget.getControllerId()+"/inventory")));
        Thread.sleep(1200); // is required: otherwise processing the next line is

        // initial
        final Map<String, String> attributes = new HashMap<>();
        attributes.put(INVENTORY_HASH_KEY, inventoryHash);
        controllerManagement.updateControllerAttributes(savedTarget.getControllerId(), attributes, null);

        // ConfigData link should present for different inventoryHash value
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, DIFFERENT_INVENTORY_HASH))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, equalTo(
                        "http://localhost/device/v1/controllers/"+savedTarget.getControllerId()+"/inventory")));

        // ConfigData link shouldn't present for same inventoryHash value
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, inventoryHash))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF).doesNotExist());

        final DistributionSet ds = testdataFactory.createDistributionSet("");

        assignDistributionSet(ds.getId(), savedTarget.getControllerId());

        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        updateAction.setStatus(DeviceActionStatus.RUNNING);
        actionRepository.save((JpaAction) updateAction);

        // DeploymentBase link should present for same inventoryHash value with an active action.
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, inventoryHash))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF,
                        startsWith(deploymentBaseLink(savedTarget.getControllerId(), updateAction.getId().toString()))))
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF).doesNotExist());

        postDeploymentFeedback(savedTarget.getControllerId(), updateAction.getId(),
                getJsonFinishedSuccessDeploymentActionFeedback(), status().isOk());

        final Action updateActionForNextTarget = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, inventoryHash))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF,
                        startsWith(deploymentBaseLink(savedTarget.getControllerId(), updateActionForNextTarget.getId().toString()))))
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF).doesNotExist());

        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, DIFFERENT_INVENTORY_HASH))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath(JSON_PATH_LINKS_INSTALLED_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF, equalTo(
                        "http://localhost/device/v1/controllers/"+savedTarget.getControllerId()+"/inventory")));

        final Action activeAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);

        activeAction.setStatus(DeviceActionStatus.CANCELING);
        actionRepository.save((JpaAction) activeAction);

        // ConfigData link should present for different inventoryHash value with cancelled link
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, DIFFERENT_INVENTORY_HASH))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath("canceled", equalTo(true)))
                .andExpect(jsonPath("$._links.deploymentBase.href").doesNotExist())
                .andExpect(jsonPath("$._links.inventory.href", equalTo(
                        "http://localhost/device/v1/controllers/"+savedTarget.getControllerId()+"/inventory")));

        // Cancelled link should present without configData and deploymentBase link's. for same inventoryHash
        mvc.perform(
                        get(DdiRestConstants.GET_INVENTORY_HASH_PATH, savedTarget.getControllerId()).param(HASH, inventoryHash))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaTypes.HAL_JSON))
                .andExpect(jsonPath(JAVA_PATH_CONFIG_POLLING_SLEEP, equalTo(TIME)))
                .andExpect(jsonPath("canceled", equalTo(true)))
                .andExpect(jsonPath(JSON_PATH_LINKS_DEPLOYMENT_BASE_HREF).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_LINKS_INVENTORY_HREF).doesNotExist());
    }

    @Test
    @Description("When posting deployment feedback with a mixed case execution status, the feedback should be accepted successfully.")
    void givenMixedCaseExecutionStatusWhenPostingFeedbackThenItIsAccepted() throws Exception {
        final Target savedTarget = setupRolloutAndGetTarget();
        final Action updateAction = deploymentManagement.findActiveActionsByTarget(PAGE, savedTarget.getControllerId())
                .getContent().get(0);
        String jsonActionFeedback = getJsonActionFeedback(DdiStatus.ExecutionStatus.DD_ACCEPTED, null,
                Collections.singletonList(DdiStatus.ExecutionStatus.DD_ACCEPTED.getName()));

        // Mixed-case execution status
        String request = jsonActionFeedback.replace(DdiStatus.ExecutionStatus.DD_ACCEPTED.getName(), "Dd_AcCePtEd");
        postDeploymentFeedback(savedTarget.getControllerId(), updateAction.getId(), request, status().isOk());
    }

}
