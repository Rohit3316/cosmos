/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link MgmtSoftwareModuleTypeResource}.
 *
 */
@Feature("Component Tests - Management API")
@Story("Software Module Type Resource")
public class MgmtSoftwareModuleTypeResourceTest extends AbstractManagementApiIntegrationTest {

    private static final String TENANT_ID = "1";
    private static final String GET_SOFTWAREMODULE_TYPE_URL = "/management/v1/tenants/{tenantId}/softwaremodule-types";
    private static final String JSON_PATH_CONTENT_KEY = "$.content.[?(@.key=='";
    private static final String NAME = "')].name";
    private static final String DESCRIPTION = "')].description";
    private static final String MAX_ASSIGNMENTS = "')].maxAssignments";
    private static final String KEY = "')].key";
    private static final String TEST_NAME_123 = "TESTNAME123";
    private static final String DESC_1234 = "Desc1234";
    private static final String COLOUR = "colour";
    private static final String UPLOAD_TESTER = "uploadTester";
    private static final String TEST_123 = "test123";
    private static final String JSON_PATH_TOTAL = "$.total";
    private static final String SOFTWAREMODULE_TYPES_SMT_ID_URL = "/management/v1/tenants/{tenantId}/softwaremodule-types/{smtId}";
    private static final String JSON_PATH_DELETED = "$.deleted";

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types GET requests.")
    public void getSoftwareModuleTypes() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + osType.getKey() + NAME, contains(osType.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + osType.getKey() + DESCRIPTION,
                        contains(osType.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + osType.getKey() + "')].colour").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + osType.getKey() + MAX_ASSIGNMENTS, contains(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + osType.getKey() + KEY, contains("os")))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + runtimeType.getKey() + NAME,
                        contains(runtimeType.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + runtimeType.getKey() + DESCRIPTION,
                        contains(runtimeType.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + runtimeType.getKey() + MAX_ASSIGNMENTS, contains(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + runtimeType.getKey() + KEY, contains("runtime")))
                .andExpect(
                        jsonPath(JSON_PATH_CONTENT_KEY + appType.getKey() + NAME, contains(appType.getName())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + appType.getKey() + DESCRIPTION,
                        contains(appType.getDescription())))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + appType.getKey() + "')].colour").doesNotExist())
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + appType.getKey() + MAX_ASSIGNMENTS,
                        contains(1)))
                .andExpect(jsonPath(JSON_PATH_CONTENT_KEY + appType.getKey() + KEY, contains("application")))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].id", contains(testType.getId().intValue())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].name", contains(TEST_NAME_123)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].description", contains(DESC_1234)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].colour", contains(COLOUR)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdBy", contains(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].createdAt", contains((int) testType.getCreatedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedBy", contains(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedAt",
                        contains((int) testType.getLastModifiedAt())))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].maxAssignments", contains(2)))
                .andExpect(jsonPath("$.content.[?(@.key=='test123')].key", contains(TEST_123)))
                .andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(4)));
    }

    private SoftwareModuleType createTestType() {
        SoftwareModuleType testType = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .key(TEST_123).name(TEST_NAME_123).description("Desc123").colour(COLOUR).maxAssignments(5));
        testType = softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(testType.getId()).description(DESC_1234).maxAssignments(2));
        return testType;
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types GET requests with sorting by MAXASSIGNMENTS field.")
    public void getSoftwareModuleTypesSortedByMaxAssignments() throws Exception {

        // ascending
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON)

                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "MAXASSIGNMENTS:ASC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(3)));

        // descending
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON)

                        .param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "MAXASSIGNMENTS:DESC"))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(3)));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types POST requests when max assignment is smaller than 1")
    public void createSoftwareModuleTypesInvalidAssignmentBadRequest() throws Exception {

        final List<SoftwareModuleType> types = new ArrayList<>();
        types.add(entityFactory.softwareModuleType().create().key("test-1").name("TestName-1").maxAssignments(-1)
                .build());

        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.softwareModuleTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());

        types.clear();
        types.add(entityFactory.softwareModuleType().create().key("test0").name("TestName0").maxAssignments(0).build());

        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.softwareModuleTypes(types))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated());

    }


    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types/{ID} GET requests.")
    public void getSoftwareModuleType() throws Exception {
        final SoftwareModuleType testType = createTestType();

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId()).accept(MediaType.APPLICATION_JSON))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                // Verify that NAME is uppercase
                .andExpect(jsonPath("$.name", equalTo(TEST_NAME_123.toUpperCase())))
                // Verify that KEY is lowercase
                .andExpect(jsonPath("$.key", equalTo(testType.getKey().toLowerCase())))
                .andExpect(jsonPath("$.name", equalTo(TEST_NAME_123)))
                .andExpect(jsonPath("$.description", equalTo(DESC_1234)))
                .andExpect(jsonPath("$.colour", equalTo(COLOUR)))
                .andExpect(jsonPath("$.maxAssignments", equalTo(2)))
                .andExpect(jsonPath("$.createdBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.createdAt", equalTo((int) testType.getCreatedAt())))
                .andExpect(jsonPath("$.lastModifiedBy", equalTo(UPLOAD_TESTER)))
                .andExpect(jsonPath("$.lastModifiedAt", equalTo((int) testType.getLastModifiedAt())))
                .andExpect(jsonPath(JSON_PATH_DELETED, equalTo(testType.isDeleted())));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types/{ID} DELETE requests (hard delete scenario).")
    public void deleteSoftwareModuleTypeUnused() throws Exception {
        final SoftwareModuleType testType = createTestType();

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk());

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    @Test
    @Description("Ensures that module type deletion request to API on an entity that does not exist results in NOT_FOUND.")
    public void deleteSoftwareModuleTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, "1234")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types/{ID} DELETE requests (soft delete scenario).")
    public void deleteSoftwareModuleTypeUsed() throws Exception {
        final SoftwareModuleType testType = createTestType();
        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(testType).name("name").version("version").format(format).swInstallerType(swInstallerType));

        assertThat(softwareModuleTypeManagement.count()).isEqualTo(4);

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(false)));

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("$.deleted", equalTo(true)));


        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/tenants/{tenantId}/softwaremodule-types/{ID} PUT requests.")
    public void updateSoftwareModuleTypeColourDescriptionAndNameUntouched() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc")
                .put(COLOUR, "updatedColour").put("name", "nameShouldNotBeChanged").put("maxAssignments", "5").toString();

        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, testType.getId()).content(body)
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.id", equalTo(testType.getId().intValue())))
                .andExpect(jsonPath("$.description", equalTo("foobardesc")))
                .andExpect(jsonPath("$.colour", equalTo("updatedColour")))
                .andExpect(jsonPath("$.name", equalTo(TEST_NAME_123)))
                .andExpect(jsonPath("$.maxAssignments", equalTo(5))).andReturn();

    }


    @Test
    @Description("Checks the correct behaviour of /management/v1/softwaremodule-types GET requests with paging.")
    public void getSoftwareModuleTypesWithoutAddtionalRequestParameters() throws Exception {
        final int types = 3;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/softwaremodule-types GET requests with paging.")
    public void getSoftwareModuleTypesWithPagingLimitRequestParameter() throws Exception {
        final int types = 3;
        final int limitSize = 1;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/softwaremodule-types GET requests with paging.")
    public void getSoftwareModuleTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final int types = 3;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @Description("Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).")
    public void invalidRequestsOnSoftwaremoduleTypesResource() throws Exception {
        final SoftwareModuleType testType = createTestType();

        final List<SoftwareModuleType> types = Collections.singletonList(testType);

        // SM does not exist
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, "12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_ID_V1_REQUEST_MAPPING, TENANT_ID, "12345678")).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        // bad request - no content
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content("sdfjsdlkjfskdjf".getBytes())
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content(

                                "[{\"description\":\"Desc123\",\"id\":9223372036854775807,\"key\":\"test123\",\"maxAssignments\":5}]")
                        .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isBadRequest());

        final SoftwareModuleType toLongName = entityFactory.softwareModuleType().create().key(TEST_123)
                .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).build();
        mvc.perform(
                        post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.softwareModuleTypes(Collections.singletonList(toLongName)))

                                .contentType(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // unsupported media type
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.softwareModuleTypes(types))

                        .contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isUnsupportedMediaType());

        // not allowed methods
        mvc.perform(put(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print())

                .andExpect(status().isMethodNotAllowed());

    }

    @Test
    @Description("Search erquest of software module types.")
    public void searchSoftwareModuleTypeRsql() throws Exception {
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key(TEST_123)
                .name(TEST_NAME_123).description("Desc123").maxAssignments(5));
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key("test1234")
                .name("TestName1234").description(DESC_1234).maxAssignments(5));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULETYPE_V1_REQUEST_MAPPING + "?q=" + rsqlFindLikeDs1OrDs2, 1L)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2)))
                .andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo(TEST_NAME_123)))
                .andExpect(jsonPath("content[1].name", equalTo("TESTNAME1234")));

    }
}