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
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterVinListRequestBody;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.rest.util.MockMvcResultPrinter.print;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC Tests against the MgmtTargetResource.
 */
@Feature("Component Tests - Management API")
@Story("Target Filter Query Resource")
public class MgmtTargetFilterQueryResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String JSON_PATH_ROOT = "$";

    // fields, attributes
    private static final String JSON_PATH_FIELD_ID = ".id";
    private static final String JSON_PATH_FIELD_NAME = ".name";
    private static final String JSON_PATH_FIELD_QUERY = ".query";
    private static final String JSON_PATH_FIELD_CONFIRMATION_REQUIRED = ".confirmationRequired";
    private static final String JSON_PATH_FIELD_CONTENT = ".content";
    // target
    // $.field
    static final String JSON_PATH_PAGED_LIST_CONTENT = JSON_PATH_ROOT + JSON_PATH_FIELD_CONTENT;
    private static final String JSON_PATH_FIELD_SIZE = ".size";
    static final String JSON_PATH_PAGED_LIST_SIZE = JSON_PATH_ROOT + JSON_PATH_FIELD_SIZE;
    private static final String JSON_PATH_FIELD_TOTAL = ".total";
    static final String JSON_PATH_PAGED_LIST_TOTAL = JSON_PATH_ROOT + JSON_PATH_FIELD_TOTAL;
    private static final String JSON_PATH_FIELD_AUTO_ASSIGN_DS = ".autoAssignDistributionSet";
    private static final String JSON_PATH_FIELD_AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED = ".autoAssignUserAcceptanceRequired";
    private static final String JSON_PATH_FIELD_EXCEPTION_CLASS = ".name";
    private static final String JSON_PATH_FIELD_ERROR_CODE = ".debug";
    private static final String JSON_PATH_NAME = JSON_PATH_ROOT + JSON_PATH_FIELD_NAME;
    private static final String JSON_PATH_ID = JSON_PATH_ROOT + JSON_PATH_FIELD_ID;
    private static final String JSON_PATH_QUERY = JSON_PATH_ROOT + JSON_PATH_FIELD_QUERY;
    private static final String JSON_PATH_CONFIRMATION_REQUIRED = JSON_PATH_ROOT
            + JSON_PATH_FIELD_CONFIRMATION_REQUIRED;
    private static final String JSON_PATH_AUTO_ASSIGN_DS = JSON_PATH_ROOT + JSON_PATH_FIELD_AUTO_ASSIGN_DS;
    private static final String JSON_PATH_AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED = JSON_PATH_ROOT
            + JSON_PATH_FIELD_AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED;
    private static final String JSON_PATH_EXCEPTION_CLASS = JSON_PATH_ROOT + JSON_PATH_FIELD_EXCEPTION_CLASS;
    private static final String JSON_PATH_ERROR_CODE = JSON_PATH_ROOT + JSON_PATH_FIELD_ERROR_CODE;
    private static final Long TEST_TENANT_ID= 1L;
    private static final String TARGET_FILTER_ENDPOINT = MgmtRestConstants.TARGET_FILTER_V1_REQUEST_MAPPING_TENANT.replace("{tenantId}", String.valueOf(TEST_TENANT_ID));
    private static final String NAME_FILTER_QUERY_TEST = "name==test";
    private static final String JSON_PATH_CONTENT_NAME = "$.content.[?(@.name=='";
    private static final String NAME = "')].name";
    private static final String QUERY = "')].query";
    private static final String DISTRIBUTIONSET = "/distributionset";
    private static final String ID = "{\"id\":";
    private static final String TARGET_FILTER_QUERY_ID_DISTRIBUTIONSET_URL = "/{targetFilterQueryId}/distributionset";
    private static final String VIN_LIST = "/vinList";

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of(Arguments.of(true, false), Arguments.of(true, true), Arguments.of(false, true),
                Arguments.of(true, null), Arguments.of(false, null));
    }

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model");
    }

    @Test
    @Description("Ensures that deletion is executed if permitted.")
    public void deleteTargetFilterQueryReturnsOK() throws Exception {
        final String filterName = "filter_01";
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery(filterName, "name==test_01");

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterQuery.getId()))
                .andExpect(status().isOk());

        assertThat(targetFilterQueryManagement.get(filterQuery.getId())).isNotPresent();
    }

    @Test
    @Description("Ensures that deletion is refused with not found if target does not exist.")
    public void deleteTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, notExistingId))
                .andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that update is refused with not found if target does not exist.")
    public void updateTargetWhichDoesNotExistsLeadsToEntityNotFound() throws Exception {
        final String notExistingId = "4395";
        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, notExistingId).content("{}")
                .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isNotFound());
    }

    @Test
    @Description("Ensures that update request is reflected by repository.")
    public void updateTargetFilterQueryQuery() throws Exception {
        final String filterName = "filter_02";
        final String filterQuery = "name==test_02";
        final String filterQuery2 = "name==test_02_changed";
        final String body = new JSONObject().put("query", filterQuery2).toString();

        // prepare
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(filterName, filterQuery);

        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(filterQuery2)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(filterName)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist());

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.get(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery2);
        assertThat(tfqCheck.getName()).isEqualTo(filterName);
    }

    @Test
    @Description("Ensures that update request is reflected by repository.")
    public void updateTargetFilterQueryName() throws Exception {
        final String filterName = "filter_03";
        final String filterName2 = "filter_03_changed";
        final String filterQuery = "name==test_03";
        final String body = new JSONObject().put("name", filterName2).toString();

        // prepare
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(filterName).query(filterQuery));

        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isOk())
                .andExpect(jsonPath(JSON_PATH_ID, equalTo(tfq.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(filterQuery)))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(filterName2)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist());

        final TargetFilterQuery tfqCheck = targetFilterQueryManagement.get(tfq.getId()).get();
        assertThat(tfqCheck.getQuery()).isEqualTo(filterQuery);
        assertThat(tfqCheck.getName()).isEqualTo(filterName2);
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format.")
    public void getTargetFilterQueryWithoutAdditionalRequestParameters() throws Exception {
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";

        createSingleTargetFilterQuery(idA, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idB, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idC, NAME_FILTER_QUERY_TEST);

        mvc.perform(get(TARGET_FILTER_ENDPOINT)).andExpect(status().isOk()).andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(knownTargetAmount)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + NAME, contains(idA)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + QUERY, contains(NAME_FILTER_QUERY_TEST)))
                // idB
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + NAME, contains(idB)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idB + QUERY, contains(NAME_FILTER_QUERY_TEST)))
                // idC
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + NAME, contains(idC)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + QUERY, contains(NAME_FILTER_QUERY_TEST)));
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format in size reduced by given limit parameter.")
    public void getTargetWithPagingLimitRequestParameter() throws Exception {
        final int limitSize = 1;
        final int knownTargetAmount = 3;
        final String idA = "a";
        final String idB = "b";
        final String idC = "c";

        createSingleTargetFilterQuery(idA, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idB, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idC, NAME_FILTER_QUERY_TEST);

        mvc.perform(get(TARGET_FILTER_ENDPOINT)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + NAME, contains(idA)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idA + QUERY, contains(NAME_FILTER_QUERY_TEST)));
    }

    @Test
    @Description("Ensures that request returns list of filters in defined format in size reduced by given limit and offset parameter.")
    public void getTargetWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int knownTargetAmount = 5;
        final int offsetParam = 2;
        final int expectedSize = knownTargetAmount - offsetParam;
        final String idC = "c";
        final String idD = "d";
        final String idE = "e";

        createSingleTargetFilterQuery("a", NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery("b", NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idC, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idD, NAME_FILTER_QUERY_TEST);
        createSingleTargetFilterQuery(idE, NAME_FILTER_QUERY_TEST);

        mvc.perform(get(TARGET_FILTER_ENDPOINT)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(knownTargetAmount)))
                .andExpect(status().isOk()).andDo(print())
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_TOTAL, equalTo(knownTargetAmount)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)))
                // idA
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + NAME, contains(idC)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idC + QUERY, contains(NAME_FILTER_QUERY_TEST)))
                // idB
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + NAME, contains(idD)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idD + QUERY, contains(NAME_FILTER_QUERY_TEST)))
                // idC
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + NAME, contains(idE)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_NAME + idE + QUERY, contains(NAME_FILTER_QUERY_TEST)));
    }

    @Test
    @Description("Ensures that a single target filter query can be retrieved via its id.")
    public void getSingleTarget() throws Exception {
        // create first a target which can be retrieved by rest interface
        final String knownQuery = "name==test01";
        final String knownName = "someName";
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        final String hrefPrefix = "http://localhost" + TARGET_FILTER_ENDPOINT + "/"
                + tfq.getId();
        // test
        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(knownName)))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(knownQuery)))
                .andExpect(jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist())
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + DISTRIBUTIONSET)));
    }

    @Test
    @Description("Ensures that the retrieval of a non-existing target filter query results in a HTTP Not found error (404).")
    public void getSingleTargetNoExistsResponseNotFound() throws Exception {
        final String targetIdNotExists = "546546";
        // test

        final MvcResult mvcResult = mvc
                .perform(get(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, targetIdNotExists))
                .andExpect(status().isNotFound()).andReturn();

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.ENTITY_NOT_EXISTS.name());
    }

    @Test
    @Description("Ensures that the creation of a target filter query based on an invalid request payload results in a HTTP Bad Request error (400).")
    public void createTargetFilterQueryWithBadPayloadBadRequest() throws Exception {
        final String notJson = "abc";

        final MvcResult mvcResult = mvc
                .perform(post(TARGET_FILTER_ENDPOINT).content(notJson)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetFilterQueryManagement.count()).isEqualTo(0);

        // verify response json exception message
        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.REST_BODY_NOT_READABLE.name());
        assertThat(exceptionInfo.getMessage()).isEqualTo("The given request body is not well formed");
    }

    @Test
    @Description("Ensures that the creation of a target filter query based on an invalid RSQL query results in a HTTP Bad Request error (400).")
    public void createTargetFilterWithInvalidQuery() throws Exception {
        final String invalidQuery = "name=abc";
        final String body = new JSONObject().put("query", invalidQuery).put("name", "invalidFilter").toString();

        mvc.perform(post(TARGET_FILTER_ENDPOINT).content(body)
                .contentType(MediaType.APPLICATION_JSON)).andDo(print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetFilterQueryManagement.count()).isEqualTo(0);
    }

    @Test
    @Description("Ensures that the assignment of an auto-assign distribution set results in a HTTP Forbidden error (403) "
            + "if the (existing) query addresses too many targets.")
    public void setAutoAssignDistributionSetOnFilterQueryThatExceedsQuota() throws Exception {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target");

        // create the filter query and the distribution set
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("1", "controllerId==target*");

        mvc.perform(
                        post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID,  filterQuery.getId())
                                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(print()).andExpect(status().isForbidden())
                .andExpect(
                        jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(ServerError.QUOTA_EXCEEDED.name())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, notNullValue()));
    }

    @Test
    @Description("Ensures that the update of a target filter query results in a HTTP Forbidden error (403) "
            + "if the updated query addresses too many targets.")
    public void updateTargetFilterQueryWithQueryThatExceedsQuota() throws Exception {

        // create targets
        final int maxTargets = quotaManagement.getMaxTargetsPerAutoAssignment();
        testdataFactory.createTargets(maxTargets + 1, "target");

        // create the filter query and the distribution set
        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery filterQuery = createSingleTargetFilterQuery("1", "controllerId==target1");

        // assign the auto-assign distribution set, this should work
        mvc.perform(
                        post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterQuery.getId())
                                .content("{\"id\":" + set.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(print()).andExpect(status().isCreated());

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.get(filterQuery.getId()).get();

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.YES);

        // update the query of the filter query to trigger a quota hit
        mvc.perform(put(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterQuery.getId())
                        .content("{\"query\":\"controllerId==target*\"}").contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isForbidden())
                .andExpect(
                        jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(ServerError.QUOTA_EXCEEDED.name())))
                .andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.QUOTA_EXCEEDED.name())));

    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Ensures that the distribution set auto-assignment works as intended with distribution set, user acceptance required and confirmation validation")
    public void setAutoAssignDistributionSetToTargetFilterQuery(final boolean confirmationFlowActive,
                                                                final Boolean confirmationRequired) throws Exception {
        final String knownQuery = "name==test05";
        final String knownName = "filter05";

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);

        verifyAutoAssignmentWithoutUserAcceptanceRequired(tfq, set, confirmationRequired);

        verifyAutoAssignmentWithUserAcceptanceNotRequired(tfq, set, confirmationRequired);

        verifyAutoAssignmentWithUserAcceptanceRequired(tfq, set, confirmationRequired);

        verifyAutoAssignmentWithUnknownUserAcceptanceRequired(tfq, set);

        verifyAutoAssignmentWithIncompleteDs(tfq);

        verifyAutoAssignmentWithSoftDeletedDs(tfq);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @Description("Verify the confirmation required flag will be set based on the feature state")
    void verifyConfirmationStateIfNotProvided(final boolean confirmationFlowActive) throws Exception {
        final String knownQuery = "name==test05";
        final String knownName = "filter05";

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        final DistributionSet set = testdataFactory.createDistributionSet();
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);

        // do not provide something about the confirmation
        verifyAutoAssignmentByUserAcceptanceRequired(tfq, set, null, null);

        assertThat(targetFilterQueryManagement.get(tfq.getId())).hasValueSatisfying(filter ->
                assertThat(filter.isConfirmationRequired()).isEqualTo(confirmationFlowActive)
        );
    }

    @Step
    private void verifyAutoAssignmentWithoutUserAcceptanceRequired(final TargetFilterQuery tfq, final DistributionSet set,
                                                                   final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByUserAcceptanceRequired(tfq, set, null, confirmationRequired);
    }

    @Step
    private void verifyAutoAssignmentWithUserAcceptanceNotRequired(final TargetFilterQuery tfq, final DistributionSet set,
                                                                   final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByUserAcceptanceRequired(tfq, set, MgmtRolloutUserAcceptanceRequired.NO, confirmationRequired);
    }

    @Step
    private void verifyAutoAssignmentWithUserAcceptanceRequired(final TargetFilterQuery tfq, final DistributionSet set,
                                                                final Boolean confirmationRequired) throws Exception {
        verifyAutoAssignmentByUserAcceptanceRequired(tfq, set, MgmtRolloutUserAcceptanceRequired.YES, confirmationRequired);
    }

    private void verifyAutoAssignmentByUserAcceptanceRequired(final TargetFilterQuery tfq, final DistributionSet set,
                                                              final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final Boolean confirmationRequired) throws Exception {
        final String hrefPrefix = "http://localhost" + TARGET_FILTER_ENDPOINT + "/"
                + tfq.getId();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("id", set.getId());
        if (userAcceptanceRequired != null) {
            jsonObject.put("userAcceptanceRequired", userAcceptanceRequired.getName());
        }
        if (confirmationRequired != null) {
            jsonObject.put("confirmationRequired", confirmationRequired);
        }

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())

                        .content(jsonObject.toString()).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isCreated());

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.get(tfq.getId()).get();
        final MgmtRolloutUserAcceptanceRequired expectedUserAcceptanceRequired = userAcceptanceRequired != null ? userAcceptanceRequired : MgmtRolloutUserAcceptanceRequired.YES;

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignUserAcceptanceRequired())
                .isEqualTo(expectedUserAcceptanceRequired);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_ID_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())).andDo(print())
                .andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(tfq.getName())))
                .andExpect(jsonPath(JSON_PATH_QUERY, equalTo(tfq.getQuery())))
                .andExpect(isConfirmationFlowEnabled()
                        ? jsonPath(JSON_PATH_CONFIRMATION_REQUIRED,
                        equalTo(confirmationRequired == null || confirmationRequired))
                        : jsonPath(JSON_PATH_CONFIRMATION_REQUIRED).doesNotExist())
                .andExpect(jsonPath(JSON_PATH_AUTO_ASSIGN_DS, equalTo(set.getId().intValue())))
                .andExpect(jsonPath(JSON_PATH_AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED, equalTo(expectedUserAcceptanceRequired.getName())))
                .andExpect(jsonPath("$._links.self.href", equalTo(hrefPrefix)))
                .andExpect(jsonPath("$._links.autoAssignDS.href", equalTo(hrefPrefix + DISTRIBUTIONSET)));
    }

    @Step
    private void verifyAutoAssignmentWithUnknownUserAcceptanceRequired(final TargetFilterQuery tfq, final DistributionSet set)
            throws Exception {
        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())
                        .content("{\"id\":" + set.getId() + ", \"userAcceptanceRequired\":\"unknown\"}").contentType(MediaType.APPLICATION_JSON))

                .andDo(print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, notNullValue()));
    }

    @Step
    private void verifyAutoAssignmentWithIncompleteDs(final TargetFilterQuery tfq) throws Exception {
        final DistributionSet incompleteDistributionSet = distributionSetManagement
                .create(entityFactory.distributionSet().create().name("incomplete").version("1")
                        .type(testdataFactory.findOrCreateDefaultTestDsType()));

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())
                        .content("{\"id\":" + incompleteDistributionSet.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(print()).andExpect(status().isBadRequest())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS,
                        equalTo(ServerError.DS_INCOMPLETE.name())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, notNullValue()));
    }

    @Step
    private void verifyAutoAssignmentWithSoftDeletedDs(final TargetFilterQuery tfq) throws Exception {
        final DistributionSet softDeletedDs = testdataFactory.createDistributionSet("softDeleted");
        assignDistributionSet(softDeletedDs, testdataFactory.createTarget("forSoftDeletedDs"));
        distributionSetManagement.delete(softDeletedDs.getId());

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId())
                        .content("{\"id\":" + softDeletedDs.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(print()).andExpect(status().isNotFound())
                .andExpect(jsonPath(JSON_PATH_EXCEPTION_CLASS, equalTo(ServerError.ENTITY_NOT_EXISTS.name())))
                .andExpect(jsonPath(JSON_PATH_ERROR_CODE, notNullValue()));
    }

    @Test
    @Description("Ensures that the deletion of auto-assignment distribution set works as intended, deleting the auto-assignment user acceptance required as well")
    public void deleteAutoAssignDistributionSetOfTargetFilterQuery() throws Exception {

        final String knownQuery = "name==test06";
        final String knownName = "filter06";
        final String dsName = "testDS";

        final DistributionSet set = testdataFactory.createDistributionSet(dsName);
        final TargetFilterQuery tfq = createSingleTargetFilterQuery(knownName, knownQuery);
        targetFilterQueryManagement
                .updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(tfq.getId()).ds(set.getId()));

        final TargetFilterQuery updatedFilterQuery = targetFilterQueryManagement.get(tfq.getId()).get();

        assertThat(updatedFilterQuery.getAutoAssignDistributionSet()).isEqualTo(set);
        assertThat(updatedFilterQuery.getAutoAssignUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.YES);

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(dsName)));

        mvc.perform(delete(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId()))

                .andExpect(status().isNoContent());

        final TargetFilterQuery filterQueryWithDeletedDs = targetFilterQueryManagement.get(tfq.getId()).get();

        assertThat(filterQueryWithDeletedDs.getAutoAssignDistributionSet()).isNull();
        assertThat(filterQueryWithDeletedDs.getAutoAssignUserAcceptanceRequired()).isNull();

        mvc.perform(get(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, tfq.getId()))

                .andExpect(status().isNoContent());

    }

    @Test
    @Description("An auto assignment containing a weight is only accepted when weight is valide and multi assignment is on.")
    public void weightValidation() throws Exception {
        final Long filterId = createSingleTargetFilterQuery("filter1", "name==*").getId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        final String invalideWeightRequest = new JSONObject().put("id", dsId).put("weight", Action.WEIGHT_MIN - 1)
                .toString();
        final String valideWeightRequest = new JSONObject().put("id", dsId).put("weight", 45).toString();

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.name", equalTo(ServerError.MULTIASSIGNMENT_NOT_ENABLED.name())));
        enableMultiAssignments();
        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(invalideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.debug", notNullValue()));

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())

                .andExpect(status().isCreated());

        final List<TargetFilterQuery> filters = targetFilterQueryManagement.findAll(PAGE).getContent();
        assertThat(filters).hasSize(1);
        assertThat(filters.get(0).getAutoAssignWeight().get()).isEqualTo(45);
    }

    private TargetFilterQuery createSingleTargetFilterQuery(final String name, final String query) {
        return targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name(name).query(query));
    }

    @Test
    @Description("Ensures that status is not found if tenant is not available")
    public void targetFilterVinListTenantValidation() throws Exception {

        List<String> vinList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        vinList.add("Device01");
        MgmtTargetFilterVinListRequestBody targetFilterVinList = new MgmtTargetFilterVinListRequestBody();
        targetFilterVinList.setControllerIds(vinList);
        String requestBodyJson = objectMapper.writeValueAsString(targetFilterVinList);

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID)

                        .content(requestBodyJson).contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("Ensures that the creation of a target filter with empty vinList results in a HTTP Bad Request error (400).")
    public void targetFilterVinListWithEmptyVinListBadRequest() throws Exception {
        final String emptyVinPayload = "{\"tenant\":\"DEFAULT\",\"controllerIds\":[]}";

        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID).content(emptyVinPayload)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isBadRequest()).andReturn();

        assertThat(targetFilterQueryManagement.count()).isEqualTo(0);

        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.CONSTRAINT_VIOLATION.name());
        assertThat(exceptionInfo.getMessage()).isEqualTo("getByControllerID.controllerIDs must not be empty.");
    }

    @Test
    @Description("Target list not found")
    public void targetFilterVinList_targetNotFound() throws Exception {
        List<String> vinList = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        vinList.add("4F2YZ04153KM18431");
        vinList.add("1C3BF66P9HX758540");
        MgmtTargetFilterVinListRequestBody targetFilterVinList = new MgmtTargetFilterVinListRequestBody();
        targetFilterVinList.setControllerIds(vinList);
        String requestBodyJson = objectMapper.writeValueAsString(targetFilterVinList);
        final MvcResult mvcResult = mvc
                .perform(post(MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID).content(requestBodyJson)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isNotFound()).andReturn();
        assertThat(targetFilterQueryManagement.count()).isEqualTo(0);

        final ExceptionInfo exceptionInfo = ResourceUtility
                .convertException(mvcResult.getResponse().getContentAsString());
        assertThat(exceptionInfo.getName()).isEqualTo(ServerError.ENTITY_NOT_EXISTS.name());
        assertThat(exceptionInfo.getMessage()).isEqualTo("Targets with given identifiers {4F2YZ04153KM18431,1C3BF66P9HX758540} do not exist.");
    }

    @Test
    @Description("Ensures that the All VINs are invalid results in HTTP not found (404).")
    public void targetFilterVinListWithInvalidVins() throws Exception {
        final String invalidVinPayload = "{\"tenant\":\"DEFAULT\",\"controllerIds\":[\"NotVIN\",\"NotVIN\"]}";

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID).content(invalidVinPayload)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isNotFound()).andReturn();
    }

    @Test
    @Description("Ensures that the VINs include valid and invalid VINs results in HTTP status ok (200).")
    public void targetFilterVinListWithValidInvalidVins() throws Exception {
        final String knownControllerId = "knownControllerId";
        testdataFactory.createTarget(knownControllerId);
        final String vinPayload = "{\"tenant\":\"DEFAULT\",\"controllerIds\":[\"knownControllerId\",\"NotVIN\"]}";

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_VINLIST_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID).content(vinPayload)

                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print()).andExpect(status().isCreated()).andReturn();
    }

    @Test
    @Description("Given case insensitive enum, when creating target filter for rollout, then return success")
    public void givenCaseInsensitiveEnumWhenCreatingTargetFilterQueryForRolloutThenReturnSuccess() throws Exception {
        enableMultiAssignments();
        final Long filterId = createSingleTargetFilterQuery("filter1", "name==*").getId();
        final Long dsId = testdataFactory.createDistributionSet().getId();

        // Mixed-case User acceptance required
        String valideWeightRequest = new JSONObject().put("id", dsId).put("weight", 45)
                .put("userAcceptanceRequired", "YeS").toString();

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoAssignUserAcceptanceRequired",equalTo("yes")));

        // Upper case User acceptance required
        valideWeightRequest = new JSONObject().put("id", dsId).put("weight", 45)
                .put("userAcceptanceRequired", "YES").toString();

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoAssignUserAcceptanceRequired",equalTo("yes")));

        // Lower case User acceptance required
        valideWeightRequest = new JSONObject().put("id", dsId).put("weight", 45)
                .put("userAcceptanceRequired", "yes").toString();

        mvc.perform(post(MgmtRestConstants.TARGET_FILTER_DS_V1_REQUEST_MAPPING_TENANT, TEST_TENANT_ID, filterId).content(valideWeightRequest).contentType(MediaType.APPLICATION_JSON)).andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoAssignUserAcceptanceRequired",equalTo("yes")));
    }


}