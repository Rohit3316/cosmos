/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.eclipse.hawkbit.exception.ServerError;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link MgmtDistributionSetTypeResource}.
 */
@Feature("Component Tests - Management API")
@Story("Distribution Set Type Resource")
public class MgmtDistributionSetTypeResourceTest extends AbstractManagementApiIntegrationTest {


    private static final String TEST_NAME_123 = "TESTNAME123";
    private static final String DESC_123 = "Desc123";
    private static final String TEST_123 = "test123";
    private static final String COL_12 = "col12";
    private static final String DESC_1234 = "Desc1234";
    private static final String JSON_PATH_CONTENT_KEY = "$.content.[?(@.key=='";
    private static final String UPLOAD_TESTER = "uploadTester";
    private static final String DISTRIBUTIONSET_TYPES_URL = "http://localhost/management/v1/tenants/1/distributionset-types/";
    private static final String JSON_PATH_TOTAL = "$.total";
    private static final String ZZZZZ = "zzzzz";
    private static final String TEST_KEY_1 = "testkey1";
    private static final String TEST_KEY_2 = "testkey2";
    private static final String TEST_KEY_3 = "testkey3";
    private static final String FIRST_ITEM_NAME_PATH = "[0].name";

    private static final String NAME_OF_THE_KEY_IN_THE_ARRAY = "[0].key";
    private static final String DESCRIPTION_OF_FIRST_OBJECT_IN_ARRAY = "[0].description";
    private static final String SOFTWARE_MODULE_MANDATORY_ENDPOINT = "/{dstID}/softwaremodule-types/mandatory";
    private static final String JSON_PATH_ID = "{\"id\":";
    private static final String SOFTWARE_MODULE_OPTIONAL_ENDPOINT = "/{dstID}/softwaremodule-types/optional";
    private static final String SOFTWARE_MODULE_TYPE_PREFIX = "smType_";
    private static final String TEST_TYPE = "testType";
    private static final String JSON_PATH_NAME = "$.name";
    private static final String TEST_TYPE_2 = "testType2";
    private static final String JSON_PATH_DESCRIPTION = "$.description";
    private static final String JSON_PATH_CREATED_BY = "$.createdBy";
    private static final String JSON_PATH_CREATED_AT = "$.createdAt";
    private static final String JSON_PATH_LAST_MODIFIED_BY = "$.lastModifiedBy";
    private static final String JSON_PATH_LAST_MODIFIED_AT = "$.lastModifiedAt";
    private static final String DST_ID_URL = "/{dstId}";
    private static final String JSON_PATH_DELETED = "$.deleted";
    private static final String SOFTWAREMODULE_TYPES_OPTIONAL_PATH = "/softwaremodule-types/optional/";

    static final long TENANT_ID = 1L;

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types GET requests.")
    void getDistributionSetTypes() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour(COL_12));
        testType = distributionSetTypeManagement.update(entityFactory.distributionSetType().update(testType.getId()).description(DESC_1234));

        // 4 types overall (2 hawkbit tenant default, 1 test default and 1
        // generated in this test)
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_CONTENT_KEY + standardDsType.getKey() + "')].name", contains(standardDsType.getName()))).andExpect(jsonPath(JSON_PATH_CONTENT_KEY + standardDsType.getKey() + "')].description", contains(standardDsType.getDescription()))).andExpect(jsonPath(JSON_PATH_CONTENT_KEY + standardDsType.getKey() + "')].key", contains(standardDsType.getKey()))).andExpect(jsonPath("$.content.[?(@.key=='test123')].id", contains(testType.getId().intValue()))).andExpect(jsonPath("$.content.[?(@.key=='test123')].name", contains(TEST_NAME_123))).andExpect(jsonPath("$.content.[?(@.key=='test123')].description", contains(DESC_1234))).andExpect(jsonPath("$.content.[?(@.key=='test123')].colour", contains(COL_12))).andExpect(jsonPath("$.content.[?(@.key=='test123')].createdBy", contains(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[?(@.key=='test123')].createdAt", contains((int) testType.getCreatedAt()))).andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedBy", contains(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[?(@.key=='test123')].lastModifiedAt", contains((int) testType.getLastModifiedAt()))).andExpect(jsonPath("$.content.[?(@.key=='test123')].key", contains(TEST_123))).andExpect(jsonPath("$.content.[?(@.key=='test123')]._links.self.href", contains(DISTRIBUTIONSET_TYPES_URL + testType.getId()))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(5)));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types GET requests with sorting by KEY.")
    void getDistributionSetTypesSortedByKey() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(ZZZZZ).name(TEST_NAME_123).description(DESC_123).colour(COL_12));
        testType = distributionSetTypeManagement.update(entityFactory.distributionSetType().update(testType.getId()).description(DESC_1234));

        // descending
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:DESC")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content.[0].id", equalTo(testType.getId().intValue()))).andExpect(jsonPath("$.content.[0].name", equalTo(TEST_NAME_123))).andExpect(jsonPath("$.content.[0].description", equalTo(DESC_1234))).andExpect(jsonPath("$.content.[0].colour", equalTo(COL_12))).andExpect(jsonPath("$.content.[0].createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[0].createdAt", equalTo((int) testType.getCreatedAt()))).andExpect(jsonPath("$.content.[0].lastModifiedBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[0].lastModifiedAt", equalTo((int) testType.getLastModifiedAt()))).andExpect(jsonPath("$.content.[0].key", equalTo(ZZZZZ))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(DEFAULT_DS_TYPES + 1)));

        // ascending
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).accept(MediaType.APPLICATION_JSON).param(MgmtRestConstants.REQUEST_PARAMETER_SORTING, "KEY:ASC")).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content.[4].id", equalTo(testType.getId().intValue()))).andExpect(jsonPath("$.content.[4].name", equalTo(TEST_NAME_123))).andExpect(jsonPath("$.content.[4].description", equalTo(DESC_1234))).andExpect(jsonPath("$.content.[4].colour", equalTo(COL_12))).andExpect(jsonPath("$.content.[4].createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[4].createdAt", equalTo((int) testType.getCreatedAt()))).andExpect(jsonPath("$.content.[4].lastModifiedBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("$.content.[4].lastModifiedAt", equalTo((int) testType.getLastModifiedAt()))).andExpect(jsonPath("$.content.[4].key", equalTo(ZZZZZ))).andExpect(jsonPath(JSON_PATH_TOTAL, equalTo(DEFAULT_DS_TYPES + 1)));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types POST requests.")
    void createDistributionSetTypes() throws Exception {

        final List<DistributionSetType> types = createTestDistributionSetTestTypes();

        final MvcResult mvcResult = runPostDistributionSetType(types);

        verifyCreatedDistributionSetTypes(mvcResult);
    }

    @Step
    private void verifyCreatedDistributionSetTypes(final MvcResult mvcResult) throws UnsupportedEncodingException {
        final DistributionSetType created1 = distributionSetTypeManagement.getByKey(TEST_KEY_1).get();
        final DistributionSetType created2 = distributionSetTypeManagement.getByKey(TEST_KEY_2).get();
        final DistributionSetType created3 = distributionSetTypeManagement.getByKey(TEST_KEY_3).get();

        // Verify TYPE_KEY is lowercase
        assertThat(created1.getKey()).isEqualTo(TEST_KEY_1.toLowerCase());
        assertThat(created2.getKey()).isEqualTo(TEST_KEY_2.toLowerCase());
        assertThat(created3.getKey()).isEqualTo(TEST_KEY_3.toLowerCase());

        // Verify NAME is uppercase
        assertThat(created1.getName()).isEqualTo(created1.getName().toUpperCase());
        assertThat(created2.getName()).isEqualTo(created2.getName().toUpperCase());
        assertThat(created3.getName()).isEqualTo(created3.getName().toUpperCase());

        assertThat(created1.getMandatoryModuleTypes()).containsOnly(osType);
        assertThat(created1.getOptionalModuleTypes()).containsOnly(runtimeType);
        assertThat(created2.getOptionalModuleTypes()).containsOnly(osType, runtimeType, appType);
        assertThat(created3.getMandatoryModuleTypes()).containsOnly(osType, runtimeType);

        assertThat(JsonPath.compile("[0]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString()).hasToString(DISTRIBUTIONSET_TYPES_URL + created1.getId());
        assertThat(JsonPath.compile("[1]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString()).hasToString(DISTRIBUTIONSET_TYPES_URL + created2.getId());
        assertThat(JsonPath.compile("[2]_links.self.href").read(mvcResult.getResponse().getContentAsString()).toString()).hasToString(DISTRIBUTIONSET_TYPES_URL + created3.getId());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(7);
    }

    @Step
    private MvcResult runPostDistributionSetType(final List<DistributionSetType> types) throws Exception {
        return mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.distributionSetTypes(types)).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(FIRST_ITEM_NAME_PATH, equalTo("TESTNAME1"))).andExpect(jsonPath(NAME_OF_THE_KEY_IN_THE_ARRAY, equalTo(TEST_KEY_1))).andExpect(jsonPath(DESCRIPTION_OF_FIRST_OBJECT_IN_ARRAY, equalTo("Desc1"))).andExpect(jsonPath("[0].colour", equalTo("col"))).andExpect(jsonPath("[0].createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[1].name", equalTo("TESTNAME2"))).andExpect(jsonPath("[1].key", equalTo(TEST_KEY_2))).andExpect(jsonPath("[1].description", equalTo("Desc2"))).andExpect(jsonPath("[1].createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[2].name", equalTo("TESTNAME3"))).andExpect(jsonPath("[2].key", equalTo(TEST_KEY_3))).andExpect(jsonPath("[2].description", equalTo("Desc3"))).andExpect(jsonPath("[2].createdBy", equalTo(UPLOAD_TESTER))).andExpect(jsonPath("[2].createdAt", not(equalTo(0)))).andReturn();
    }

    @Step
    private List<DistributionSetType> createTestDistributionSetTestTypes() {
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);

        return Arrays.asList(entityFactory.distributionSetType().create().key(TEST_KEY_1).name("TestName1").description("Desc1").colour("col").mandatory(Collections.singletonList(osType.getId())).optional(Collections.singletonList(runtimeType.getId())).build(), entityFactory.distributionSetType().create().key(TEST_KEY_2).name("TestName2").description("Desc2").colour("col").optional(Arrays.asList(runtimeType.getId(), osType.getId(), appType.getId())).build(), entityFactory.distributionSetType().create().key(TEST_KEY_3).name("TestName3").description("Desc3").colour("col").mandatory(Arrays.asList(runtimeType.getId(), osType.getId())).build());
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/mandatory POST requests.")
    void addMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour(COL_12));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        TestdataFactory.waitForSeconds(2);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_MANDATORY_ENDPOINT, TENANT_ID, testType.getId()).content(JSON_PATH_ID + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(UPLOAD_TESTER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/optional POST requests.")
    void addOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour(COL_12));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        TestdataFactory.waitForSeconds(2);

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_OPTIONAL_ENDPOINT, TENANT_ID, testType.getId()).content(JSON_PATH_ID + osType.getId() + "}").contentType(MediaType.APPLICATION_JSON))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(UPLOAD_TESTER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(osType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();

    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Verifies quota enforcement for /management/v1/distributionset-types/{ID}/softwaremodule-types/optional POST requests.")
    void assignModuleTypesToDistributionSetTypeUntilQuotaExceeded() throws Exception {

        // create software module types
        final int maxSoftwareModuleTypes = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        final List<Long> moduleTypeIds = Lists.newArrayList();
        for (int i = 0; i < maxSoftwareModuleTypes + 1; ++i) {
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name(SOFTWARE_MODULE_TYPE_PREFIX + i).description(SOFTWARE_MODULE_TYPE_PREFIX + i).maxAssignments(1).colour("blue").key(SOFTWARE_MODULE_TYPE_PREFIX + i);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // verify quota enforcement for optional module types

        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_TYPE).name(TEST_TYPE).description(TEST_TYPE).colour(COL_12));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {

            mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_OPTIONAL_ENDPOINT, TENANT_ID, testType.getId()).content(JSON_PATH_ID + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_OPTIONAL_ENDPOINT, TENANT_ID, testType.getId()).content(JSON_PATH_ID + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")

                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.QUOTA_EXCEEDED.name()))).andExpect(jsonPath("$.message", equalTo("Quota exceeded: Cannot assign 1 more SoftwareModuleType entities to DistributionSetType '95'. The maximum is 5.")));

        // verify quota enforcement for mandatory module types

        final DistributionSetType testType2 = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_TYPE_2).name(TEST_TYPE_2).description(TEST_TYPE_2).colour(COL_12));
        assertThat(testType2.getOptLockRevision()).isEqualTo(1);

        for (int i = 0; i < moduleTypeIds.size() - 1; ++i) {

            mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_MANDATORY_ENDPOINT, TENANT_ID, testType2.getId()).content(JSON_PATH_ID + moduleTypeIds.get(i) + "}").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        }

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_MANDATORY_ENDPOINT, TENANT_ID, testType2.getId()).content(JSON_PATH_ID + moduleTypeIds.get(moduleTypeIds.size() - 1) + "}")

                .contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isForbidden()).andExpect(jsonPath(JSON_PATH_NAME, equalTo(ServerError.QUOTA_EXCEEDED.name()))).andExpect(jsonPath("$.debug", notNullValue()));

    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/mandatory GET requests.")
    void getMandatoryModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();


        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_MANDATORY_ENDPOINT, TENANT_ID, testType.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(FIRST_ITEM_NAME_PATH, equalTo(osType.getName()))).andExpect(jsonPath(DESCRIPTION_OF_FIRST_OBJECT_IN_ARRAY, equalTo(osType.getDescription()))).andExpect(jsonPath("[0].maxAssignments", equalTo(1))).andExpect(jsonPath(NAME_OF_THE_KEY_IN_THE_ARRAY, equalTo("os")));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/optional GET requests.")
    void getOptionalModulesOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + SOFTWARE_MODULE_OPTIONAL_ENDPOINT, TENANT_ID, testType.getId())

                .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(FIRST_ITEM_NAME_PATH, equalTo(appType.getName()))).andExpect(jsonPath(DESCRIPTION_OF_FIRST_OBJECT_IN_ARRAY, equalTo(appType.getDescription()))).andExpect(jsonPath(NAME_OF_THE_KEY_IN_THE_ARRAY, equalTo("application")));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/mandatory/{ID} GET requests.")
    void getMandatoryModuleOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{dstID}/softwaremodule-types/mandatory/{smtId}", TENANT_ID, testType.getId(), osType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(osType.getId().intValue()))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(osType.getName()))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(osType.getDescription()))).andExpect(jsonPath(JSON_PATH_CREATED_BY, equalTo(osType.getCreatedBy()))).andExpect(jsonPath(JSON_PATH_CREATED_AT, equalTo((int) osType.getCreatedAt()))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(osType.getLastModifiedBy()))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT, equalTo((int) osType.getLastModifiedAt())));
    }

    private DistributionSetType generateTestType() {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour("col").mandatory(Collections.singletonList(osType.getId())).optional(Collections.singletonList(appType.getId())));
        assertThat(testType.getOptLockRevision()).isEqualTo(1);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
        return testType;
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/optional/{ID} GET requests.")
    void getOptionalModuleOfDistributionSetType() throws Exception {
        final DistributionSetType testType = generateTestType();

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{dstID}/softwaremodule-types/optional/{smtId}", TENANT_ID, testType.getId(), appType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(appType.getId().intValue()))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(appType.getName()))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(appType.getDescription()))).andExpect(jsonPath(JSON_PATH_CREATED_BY, equalTo(appType.getCreatedBy()))).andExpect(jsonPath(JSON_PATH_CREATED_AT, equalTo((int) appType.getCreatedAt()))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(appType.getLastModifiedBy()))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_AT, equalTo((int) appType.getLastModifiedAt())));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/mandatory/{ID} DELETE requests.")
    void removeMandatoryModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        TestdataFactory.waitForSeconds(2);

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{dstID}/softwaremodule-types/mandatory/{smtId}", TENANT_ID, testType.getId(), osType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(UPLOAD_TESTER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).containsExactly(appType);
        assertThat(testType.getMandatoryModuleTypes()).isEmpty();
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID}/softwaremodule-types/optional/{ID} DELETE requests.")
    void removeOptionalModuleToDistributionSetType() throws Exception {
        DistributionSetType testType = generateTestType();

        TestdataFactory.waitForSeconds(2);

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{dstID}/softwaremodule-types/optional/{smtId}", TENANT_ID, testType.getId(), appType.getId()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        testType = distributionSetTypeManagement.get(testType.getId()).get();
        assertThat(testType.getLastModifiedBy()).isEqualTo(UPLOAD_TESTER);
        assertThat(testType.getOptLockRevision()).isEqualTo(2);
        assertThat(testType.getOptionalModuleTypes()).isEmpty();
        assertThat(testType.getMandatoryModuleTypes()).containsExactly(osType);
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID} GET requests.")
    void getDistributionSetType() throws Exception {

        DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123));
        testType = distributionSetTypeManagement.update(entityFactory.distributionSetType().update(testType.getId()).description(DESC_1234));


        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + DST_ID_URL, TENANT_ID, testType.getId()).accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE)).andExpect(jsonPath(JSON_PATH_NAME, equalTo(TEST_NAME_123))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo(DESC_1234))).andExpect(jsonPath("$.colour").doesNotExist()).andExpect(jsonPath(JSON_PATH_CREATED_BY, equalTo(UPLOAD_TESTER))).andExpect(jsonPath(JSON_PATH_CREATED_AT, equalTo((int) testType.getCreatedAt()))).andExpect(jsonPath(JSON_PATH_LAST_MODIFIED_BY, equalTo(UPLOAD_TESTER))).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(testType.isDeleted())));
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/DistributionSetTypes/{ID} DELETE requests (hard delete scenario).")
    void deleteDistributionSetTypeUnused() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour(COL_12));

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{dsId}", TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @Description("Ensures that DS type deletion request to API on an entity that does not exist results in NOT_FOUND.")
    void deleteDistributionSetTypeThatDoesNotExistLeadsToNotFound() throws Exception {
        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/1234", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
    }

    @Test
    @WithUser(principal = UPLOAD_TESTER, allSpPermissions = true)
    @Description("Checks the correct behaviour of /management/v1/DistributionSetTypes/{ID} DELETE requests (soft delete scenario).")
    void deleteDistributionSetTypeUsed() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour(COL_12));

        distributionSetManagement.create(entityFactory.distributionSet().create().name("sdfsd").description("dsfsdf").version("1").type(testType));

        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES + 1);
        assertThat(distributionSetManagement.count()).isEqualTo(1);

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + DST_ID_URL, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + DST_ID_URL, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + DST_ID_URL, TENANT_ID, testType.getId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(true)));


        assertThat(distributionSetManagement.count()).isEqualTo(1);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(DEFAULT_DS_TYPES);
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/distributionset-types/{ID} PUT requests.")
    void updateDistributionSetTypeColourDescriptionAndNameUntouched() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour("col"));

        final String body = new JSONObject().put("id", testType.getId()).put("description", "foobardesc").put("colour", "updatedColour").put("name", "nameShouldNotBeChanged").toString();

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/{smId}", TENANT_ID, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(testType.getId().intValue()))).andExpect(jsonPath(JSON_PATH_DESCRIPTION, equalTo("foobardesc"))).andExpect(jsonPath("$.colour", equalTo("updatedColour"))).andExpect(jsonPath(JSON_PATH_NAME, equalTo(TEST_NAME_123))).andReturn();
    }

    @Test
    @Description("Tests the update of the deletion flag. It is verfied that the distribution set type can't be marked as deleted through update operation.")
    void updateDistributionSetTypeDeletedFlag() throws Exception {
        final DistributionSetType testType = distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).colour("col"));

        final String body = new JSONObject().put("id", testType.getId()).put("deleted", true).toString();


        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + DST_ID_URL, TENANT_ID, testType.getId()).content(body).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("$.id", equalTo(testType.getId().intValue()))).andExpect(jsonPath(JSON_PATH_DELETED, equalTo(false)));
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/distributionset-types GET requests with paging.")
    void getDistributionSetTypesWithoutAddtionalRequestParameters() throws Exception {

        // 4 types overall (3 hawkbit tenant default, 1 test default
        final int types = 4;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(types))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(types)));
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/distributionset-types GET requests with paging.")
    void getDistributionSetTypesWithPagingLimitRequestParameter() throws Exception {

        final int types = DEFAULT_DS_TYPES;
        final int limitSize = 1;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Checks the correct behaviour of /management/v1/distributionset-types GET requests with paging.")
    void getDistributionSetTypesWithPagingLimitAndOffsetRequestParameter() throws Exception {

        final int types = DEFAULT_DS_TYPES;
        final int offsetParam = 2;
        final int expectedSize = types - offsetParam;
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam)).param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(types))).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(types))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize))).andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @Description("Ensures that the server is behaving as expected on invalid requests (wrong media type, wrong ID etc.).")
    void invalidRequestsOnDistributionSetTypesResource() throws Exception {
        final SoftwareModuleType testSmType = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key(TEST_123).name(TEST_NAME_123).maxAssignments(1));

        // DST does not exist
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/12345678", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/12345678/softwaremodule-types/mandatory", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/12345678/softwaremodule-types/optional", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/12345678", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        // Module types incorrect
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/mandatory", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/mandatory/565765656", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/optional/565765656", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/mandatory/" + osType.getId(), TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/mandatory/" + testSmType.getId(), TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + SOFTWAREMODULE_TYPES_OPTIONAL_PATH + osType.getId(), TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + SOFTWAREMODULE_TYPES_OPTIONAL_PATH + appType.getId(), TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + SOFTWAREMODULE_TYPES_OPTIONAL_PATH + testSmType.getId(), TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());


        // Modules types at creation time invalid

        final DistributionSetType testNewType = entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123).description(DESC_123).colour("col").mandatory(Collections.singletonList(osType.getId())).optional(Collections.emptyList()).build();

        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.distributionSetTypes(Collections.singletonList(testNewType))).contentType(MediaType.APPLICATION_OCTET_STREAM)).andDo(MockMvcResultPrinter.print()).andExpect(status().isUnsupportedMediaType());

        // bad request - no content
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // bad request - bad content
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).content("sdfjsdlkjfskdjf".getBytes()).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // Missing mandatory field name
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).content("[{\"description\":\"Desc123\",\"key\":\"test123\"}]").contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        final DistributionSetType toLongName = entityFactory.distributionSetType().create().key(TEST_123).name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1)).build();
        mvc.perform(post(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID).content(JsonBuilder.distributionSetTypes(Collections.singletonList(toLongName))).contentType(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());

        // not allowed methods
        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(delete(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING, TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());

        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + "/softwaremodule-types/mandatory", TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
        mvc.perform(put(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "/" + standardDsType.getId() + SOFTWAREMODULE_TYPES_OPTIONAL_PATH + osType.getId(), TENANT_ID))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isMethodNotAllowed());
    }

    @Test
    @Description("Search erquest of software module types.")
    void searchDistributionSetTypeRsql() throws Exception {
        distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key(TEST_123).name(TEST_NAME_123));
        distributionSetTypeManagement.create(entityFactory.distributionSetType().create().key("test1234").name("TestName1234"));

        final String rsqlFindLikeDs1OrDs2 = "name==TestName123,name==TestName1234";

        mvc.perform(get(MgmtRestConstants.DISTRIBUTIONSETTYPE_V1_REQUEST_MAPPING + "?q=" + rsqlFindLikeDs1OrDs2, TENANT_ID)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk()).andExpect(jsonPath("size", equalTo(2))).andExpect(jsonPath("total", equalTo(2))).andExpect(jsonPath("content[0].name", equalTo(TEST_NAME_123))).andExpect(jsonPath("content[1].name", equalTo("TESTNAME1234")));
    }

}