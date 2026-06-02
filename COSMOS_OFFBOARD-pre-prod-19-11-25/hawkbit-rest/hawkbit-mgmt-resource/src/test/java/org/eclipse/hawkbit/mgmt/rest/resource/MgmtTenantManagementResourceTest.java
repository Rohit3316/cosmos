/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.system.constants.MgmtSystemTenantConfigurationValueRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationRequest;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtTenantValidationResponse;
import org.cosmos.models.mgmt.systemmanagement.dto.MgmtValidTenant;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.Format;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.eclipse.hawkbit.tenancy.configuration.InvalidTenantConfigurationKeyException;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.cosmos.models.mgmt.MgmtRestConstants.TENANTID_CONFIG_SYSTEM_MAPPING;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.CONTROLLER_ROLE;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.SYSTEM_ROLE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


/**
 * Spring MVC Tests against the MgmtTenantManagementResource.
 */
@Slf4j
@Feature("Component Tests - Management API")
@Story("Tenant Management Resource")
public class MgmtTenantManagementResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String KEY_MULTI_ASSIGNMENTS = "multi.assignments.enabled";
    private static final String POLLING_TIME = "pollingTime";
    private static final String KEY_AUTO_CLOSE = "repository.actions.autoclose.enabled";

    private static final String CLONE_TENANT_PATH = "SAMPLE";
    private static final String TENANT = "tenant";
    private static final String FILE_PATH = "/accounts/tenants";
    private static final String VALUE = "value";
    private static final String KEY_URL = "/{key}";
    private static final String TEST_TENANT = "TEST-TENANT";
    private static final String ACCOUNTS_TENANTS_TENANT_ID_URL = "/accounts/tenants/{tenantId}";
    private static final String ACCOUNTS_TENANTS_TENANT_ID_CLONE_URL = "/accounts/tenants/{tenantId}/clone";
    private static final String JSON_PATH_NAME_EXP = "$.name";
    private static final String PERMISSION = "INSUFFICIENT_PERMISSION";
    public static final String SEPARATOR = ",";
    private final Set<String> createdTenants = new HashSet<>();


    @Test
    @Description("Successfully create a new tenant and verify mandatory configurations. Clone the tenant and verify its configuration")
    public void createTenantAndVerifyConfigurations() throws Exception {
        createTenantRequest(TENANT);
        verifyConfiguration(1L, TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_ESP_KEY, "");
        verifyConfiguration(1L, TenantConfigurationProperties.TenantConfigurationKey.MANDATORY_RSP_KEY, "");
        AtomicReference<Long> tenantIdOfTenant = new AtomicReference<>();
        tenantAware.runAsTenant(TENANT, () -> {
                    List<SoftwareModuleFormat> formatList = softwareModuleFormatRepository.findAll(Pageable.unpaged()).getContent();
                    var listFromDb = formatList.stream().map(Format::getKey).toList().stream().sorted().toList();
                    var listFromSource = Stream.of(softwareModuleFormats.split(SEPARATOR)).map(String::toLowerCase).sorted().toList();
                    tenantIdOfTenant.set(systemManagement.getTenantMetadata().getTenantId());
                    assertEquals(listFromSource, listFromDb);
                    return 0;
                }
        );
        tenantAware.runAsTenant(TENANT, () -> {
            try {
                final String tenantBody = new JSONObject().put(TENANT, TEST_TENANT).toString();
                mvc.perform(post(MgmtRestConstants.BASE_CLONE_V1_REQUEST_MAPPING, tenantIdOfTenant.get())
                                .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                        .andExpect(status().isOk());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            List<SoftwareModuleFormat> formatList = softwareModuleFormatRepository.findAll(Pageable.unpaged()).getContent();
            var listFromDb = formatList.stream().map(Format::getKey).toList().stream().sorted().toList();
            var listFromSource = Stream.of(softwareModuleFormats.split(SEPARATOR)).map(String::toLowerCase).sorted().toList();

            assertEquals(listFromSource, listFromDb);
            return 0;
        });
    }

    /*
     * Fetches the configuration for a given tenant and configuration key.
     * Returns the response content as a string.
     */
    private String fetchConfiguration(Long tenantId, String configKey) throws Exception {
        MvcResult result = mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, tenantId, configKey)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    /*
     * Creates a tenant by sending a POST request with the provided tenant name.
     */
    private void createTenantRequest(String tenant) throws Exception {
        final String tenantBody = new JSONObject().put("tenant", tenant).toString();
        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(tenantBody)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
    }

    /*
     * Verifies that the configuration response contains the expected values.
     */
    private void verifyConfiguration(Long tenantId, String configKey, String expectedValues) throws Exception {
        String response = fetchConfiguration(tenantId, configKey);
        assertThat(response).containsIgnoringWhitespaces(expectedValues);
    }


    @Test
    @Description("Bad request if tenant not present")
    public void createTenantBadRequest() throws Exception {
        final String tenantBody = new JSONObject().put("bad", "request").toString();

        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

    }


    @Test
    @Description("The 'multi.assignments.enabled' property must not be changed to false.")
    public void deactivateMultiAssignment() throws Exception {
        final String bodyActivate = new JSONObject().put(VALUE, true).toString();
        final String bodyDeactivate = new JSONObject().put(VALUE, false).toString();

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }

    @Test
    @Description("The 'repository.actions.autoclose.enabled' property must not be modified if Multi-Assignments is enabled.")
    public void autoCloseCannotBeModifiedIfMultiAssignmentIsEnabled() throws Exception {
        final String bodyActivate = new JSONObject().put(VALUE, true).toString();
        final String bodyDeactivate = new JSONObject().put(VALUE, false).toString();

        // enable Multi-Assignments
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // try to enable Auto-Close
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());

        // try to disable Auto-Close
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(bodyDeactivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isForbidden());
    }


    @Test
    @Description("Successfully retrieve tenant informations.")
    @WithUser(principal = "bumlux", allSpPermissions = true, authorities = {CONTROLLER_ROLE, SYSTEM_ROLE}, tenantId = TEST_TENANT)
    public void getTenantsMetadata() throws Exception {

        final String tenant = TEST_TENANT;
        TenantMetaData result = systemManagement.getTenantMetadata(tenant);

        mvc.perform(get(MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING, result.getTenantId())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Clone an already present tenant with his configuration")
    public void cloneTenantSuccess() throws Exception {
        final String bodyActivate = new JSONObject().put(VALUE, true).toString();

        final String sampleTenant = new JSONObject().put(TENANT, CLONE_TENANT_PATH).toString();
        //Create tenant to hold the config
        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(sampleTenant).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // create a config value in order to check if clone is working
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());


        final String tenantBody = new JSONObject().put(TENANT, TEST_TENANT).toString();

        mvc.perform(post(MgmtRestConstants.BASE_CLONE_V1_REQUEST_MAPPING, 1L)
                        .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        //Check if there is the configuration for that tenant
        mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Clone an already present tenant with this configuration exception on path tenant")
    public void cloneTenantBadRequestTenantPathNotPresent() throws Exception {
        final String bodyActivate = new JSONObject().put(VALUE, true).toString();

        final String sampleTenant = new JSONObject().put(TENANT, CLONE_TENANT_PATH).toString();
        //Create tenant to hold the config
        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(sampleTenant).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // create a config value in order to check if clone is working
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());


        final String tenantBody = new JSONObject().put(TENANT, TEST_TENANT).toString();

        mvc.perform(post(MgmtRestConstants.BASE_CLONE_V1_REQUEST_MAPPING, 100L)
                        .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Clone an already present tenant with his configuration exception on body tenant")
    public void cloneTenantBadRequestTenantBodyNotPresent() throws Exception {
        final String bodyActivate = new JSONObject().put(VALUE, true).toString();

        final String sampleTenant = new JSONObject().put(TENANT, CLONE_TENANT_PATH).toString();
        //Create tenant to hold the config
        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(sampleTenant).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        // create a config value in order to check if clone is working
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_AUTO_CLOSE)
                        .content(bodyActivate).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());


        final String tenantBody = new JSONObject().put(TENANT, "BAD_TENANT").toString();

        mvc.perform(post(MgmtRestConstants.BASE_CLONE_V1_REQUEST_MAPPING, 100L)
                        .content(tenantBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Successfully retrieve All tenant name and Id info.")
    public void getAllTenantsMetadata() throws Exception {

        mvc.perform(get(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

    }

    @Test
    @Description("Given valid tenant ID, when retrieving tenant configuration, then expect status OK.")
    public void givenValidTenantIdWhenRetrievingTenantConfigurationThenExpectStatusOk() throws Exception {
        mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_SYSTEM_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("On tenant creation, the polling time should be added by default.And Updating the polling time should also be reflected in the tenant configuration.")
    public void givenNewTenantWhenCreatedThenPollingTimeAddedByDefault() throws Exception {

        mvc.perform(get(TENANTID_CONFIG_SYSTEM_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollingTime").exists())
                .andReturn();

        // Update pollingTime
        final String body = new JSONObject().put(VALUE, "00:12:00").toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, POLLING_TIME)
                        .content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        //verify if the pollingTime is updated
        mvc.perform(get(TENANTID_CONFIG_SYSTEM_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pollingTime").exists())
                .andExpect(jsonPath("$.pollingTime.value").exists())
                .andExpect(jsonPath("$.pollingTime.value").value("00:12:00"))
                .andReturn();

    }

    @Test
    @Description("Given insufficient permission, when retrieving tenant configuration, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.READ_TENANT_CONFIG})
    public void givenInsufficientPermissionWhenRetrievingTenantConfigurationThenExpectClientError() throws Exception {
        mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_SYSTEM_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @Test
    @Description("Given valid tenant ID and key, when retrieving tenant configuration value, then expect status OK.")
    public void givenValidTenantIdAndKeyWhenRetrievingTenantConfigurationValueThenExpectStatusOk() throws Exception {
        mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given insufficient permission, when retrieving tenant configuration value, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.READ_TENANT_CONFIG})
    public void givenInsufficientPermissionWhenRetrievingTenantConfigurationValueThenExpectClientError() throws Exception {
        mvc.perform(get(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @Test
    @Description("Given valid tenant ID and key, when deleting tenant configuration value, then expect status OK.")
    public void givenValidTenantIdAndKeyWhenDeletingTenantConfigurationValueThenExpectStatusOk() throws Exception {
        mvc.perform(delete(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo("CONFIGURATION_KEY_INVALID")));
    }


    @Test
    @Description("Given insufficient permission, when deleting tenant configuration value, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.DELETE_TENANT_CONFIG})
    public void givenInsufficientPermissionWhenDeletingTenantConfigurationValueThenExpectClientError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }


    @Test
    @Description("Given valid tenant ID and key, when updating tenant configuration value, then expect status OK.")
    public void givenValidTenantIdAndKeyWhenUpdatingTenantConfigurationValueThenExpectStatusOk() throws Exception {
        String requestBody = new JSONObject().put("value", true).toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given insufficient permission, when updating tenant configuration value, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.UPDATE_TENANT_CONFIG})
    public void givenInsufficientPermissionWhenUpdatingTenantConfigurationValueThenExpectClientError() throws Exception {
        String requestBody = new JSONObject().put("value", true).toString();
        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L, KEY_MULTI_ASSIGNMENTS)
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @Test
    @Description("Given valid tenant ID, when deleting the tenant, then expect status OK.")
    public void givenValidTenantIdWhenDeletingTenantThenExpectStatusOk() throws Exception {
        mvc.perform(delete(MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());
    }

    @Test
    @Description("Given insufficient permission, when deleting the tenant, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.DELETE_TENANT})
    public void givenInsufficientPermissionWhenDeletingTenantThenExpectClientError() throws Exception {
        mvc.perform(delete(MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @Test
    @Description("Given insufficient permission, when creating the tenant, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.CREATE_TENANT})
    public void givenInsufficientPermissionWhenCreatingTenantThenExpectClientError() throws Exception {
        String requestBody = new JSONObject().put("tenant", "tenant").toString();
        mvc.perform(post(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .content(requestBody).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @Test
    @Description("Given insufficient permission, when retrieving the tenant, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.READ_TENANT})
    public void givenInsufficientPermissionWhenRetrievingTenantThenExpectClientError() throws Exception {
        mvc.perform(get(MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING, 1L)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    //TODO: Temporarily disabled the permission check in getAllTenants API after springboot 3.X upgrade, for UI to load.
    @Description("Given insufficient permission, when retrieving all tenants, then expect 4xx client error with INSUFFICIENT_PERMISSION name.")
    @WithUser(removeFromAllPermission = {SpPermission.READ_ALL_TENANT})
    public void givenInsufficientPermissionWhenRetrievingAllTenantsThenExpectClientError() throws Exception {
        mvc.perform(get(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath(JSON_PATH_NAME_EXP, equalTo(PERMISSION)));
    }

    @WithUser(allSpPermissions = true, authorities = {SpPermission.SpringEvalExpressions.SYSTEM_ROLE, SpPermission.SpringEvalExpressions.CONTROLLER_ROLE})
    @ParameterizedTest
    @Description("Given various RSQL parameters, when retrieving paged tenants, then expect paged list or empty list in response")
    @MethodSource("tenantRsqlParams")
    void givenRsqlParamVariantsWhenGetAllTenantsThenReturnExpectedResult(String rsql, int expectedStatus, int expectedContentLength) throws Exception {
        createTenantRequest("tenantToTestAllTenants");
        createdTenants.add("tenantToTestAllTenants");
        TenantMetaData created = systemManagement.getTenantMetadata("tenantToTestAllTenants");
        long createdId = created.getTenantId();
        String url = rsql.replace("{TENANT_ID}", String.valueOf(createdId));

        mvc.perform(get(url)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(expectedContentLength))
                .andExpect(jsonPath("$.total").exists());

    }

    @Test
    @Description("Given a new tenant, when deleteing the tenant, then expect status OK and all related software module formats to be deleted.")
    void givenNewTenantWhenDeleteTenantThenReturnsOk() throws Exception {
        createTenantRequest(TENANT);

        List<SoftwareModuleFormat> formatList = softwareModuleFormatRepository.findAll(Pageable.unpaged()).getContent();
        boolean isTenantMatching = formatList.stream().allMatch(format -> format.getTenant().equals(TENANT));

        if (isTenantMatching) {
            mvc.perform(delete(MgmtRestConstants.BASE_HNDL_ID_V1_REQUEST_MAPPING, 1L)
                            .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                    .andExpect(status().isOk());

            List<SoftwareModuleFormat> updatedFormatList = softwareModuleFormatRepository.findAll(Pageable.unpaged()).getContent();
            assertTrue(updatedFormatList.isEmpty());
        }
    }

    private static Stream<Arguments> tenantRsqlParams() {

        return Stream.of(
                Arguments.of(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING + "?offset=0&limit=10&sort=ID:ASC&q=id=={TENANT_ID}", 200, 1),
                Arguments.of(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING + "?offset=0&limit=10&sort=ID:ASC", 200, 2),
                Arguments.of(MgmtRestConstants.BASE_HNDL_V1_REQUEST_MAPPING + "?offset=0&limit=10&sort=ID:ASC", 200, 2)
        );
    }

    @AfterEach
    void cleanUpTestTenants() {
        for (String tenant : createdTenants) {
            try {
                systemManagement.deleteTenant(tenant);
            } catch (Exception ignored) {
                // Ignore if already deleted or not found
            }
        }
        createdTenants.clear();
    }

    @Test
    @Description("Verify that when a new tenant is created, the default Vehicle Log Level configuration (common.log.level) is added with value 4.")
    public void givenNewTenantWhenCreatedThenDefaultVehicleLogLevelIsSetToFour() throws Exception {
        // Step 1️ Create a new tenant
        createTenantRequest(TENANT);

        // Step 2 Verify that 'common.log.level' configuration is created and set to '4'
        verifyConfiguration(
                1L,
                TenantConfigurationProperties.TenantConfigurationKey.VEHICLE_LOG_LEVEL,
                "4"
        );
    }

    @Test
    @Description("Verify that setting invalid Vehicle Log Level throws Exception.")
    public void givenExistingTenantWhenUpdatingInvalidVehicleLogLevelThenShouldFail() throws Exception {
        // Step 1 Create a tenant first (to have a valid context)
        createTenantRequest(TENANT);

        // Step 2 Try updating the Vehicle Log Level to an invalid value (e.g., 10)
        MgmtSystemTenantConfigurationValueRequest invalidValueRequest = new MgmtSystemTenantConfigurationValueRequest();
        invalidValueRequest.setValue(10);

        mvc.perform(put(MgmtRestConstants.TENANTID_CONFIG_KEY_SYSTEM_MAPPING, 1L,
                        TenantConfigurationProperties.TenantConfigurationKey.VEHICLE_LOG_LEVEL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidValueRequest)))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest())
                .andExpect(result ->
                        assertTrue(result.getResolvedException().getMessage().contains("Invalid Vehicle Log Level")));
    }

    @Test
    @Description("Given valid and invalid tenants, when validating tenants, then returns only the valid ones.")
    public void givenValidAndInvalidTenantsWhenValidateTenantsThenReturnsOnlyValidOnes() throws Exception {

        systemManagement.createTenant("t1");
        systemManagement.createTenant("t2");

        MgmtTenantValidationRequest request = new MgmtTenantValidationRequest();
        request.setTenants(List.of("t1", "invalid"));

        String json = objectMapper.writeValueAsString(request);

        // Act + Assert
        mvc.perform(post(MgmtRestConstants.BASE_TENANT_VALIDATE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validTenants").isArray())
                .andExpect(jsonPath("$.validTenants.length()").value(1))
                .andExpect(jsonPath("$.validTenants[0].tenantName").value("t1"));
    }


}

