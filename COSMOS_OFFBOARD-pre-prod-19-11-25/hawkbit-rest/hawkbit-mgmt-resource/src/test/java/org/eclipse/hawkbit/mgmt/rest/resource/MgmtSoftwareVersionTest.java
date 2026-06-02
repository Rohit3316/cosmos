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
import com.jayway.jsonpath.JsonPath;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;


import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link MgmtSoftwareModuleTypeResource}.
 */
@Feature("Component Tests - Management API")
@Story("Software Version Resource")
public class MgmtSoftwareVersionTest extends AbstractManagementApiIntegrationTest {

    private static final String VERSION_URL = "/version/";
    private static final String TENANT_ID = "1";
    private static final String TEST_2 = "test2";
    private static final String VERSION = "version";
    private static final String VENDOR = "vendor";
    private static final String DESCRIPTION = "description";
    private static final String VERDEL = "VERDEL";
    private static final String GREEN = "green";
    private static final String VERDELFAIL1_ENTITYNAME = "VerDelFail1";
    private static final String VERSION1 = "version1";
    private static final String DESCRIPTION1 = "description1";
    private static final String VENDOR1 = "vendor1";
    private static final String TEST_MODULE_NOT_PRESENT = "TestModuleNotPresent";
    private static final String VERSION_SW = "version sw";
    private static final String TEST_3 = "test3";

    @Test
    @Description("Get not found when the id has not been inserted")
    public void getSoftwareVersion() throws Exception {
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, 123, 15000)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, 123, 1500)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Create and Delete an existing software version")
    public void deleteSoftwareVersion() throws Exception {

        SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(TEST_2).description(TEST_2).colour("blue").maxAssignments(1).key("2"));

        SoftwareModuleFormat format = softwareModuleFormatManagement
                .create(entityFactory.softwareModuleFormat().create().name("name2").description("desc").key("2"));

        SoftwareModule module = softwareModuleManagement.create(entityFactory.softwareModule().create().name("TEST_2")
                .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        Version result = versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(DESCRIPTION));

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(
                        delete(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When software module id is not present for the software version then not found")
    public void whenSoftwareModuleIdNotExistForVersionThenNotFound() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(VERDEL).description("test").colour(GREEN).maxAssignments(1).key("5"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("VerDelFail")
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        final Version result = versionManagement.create(entityFactory.version().create().number(14)

                .softwareModuleId(module.getId()).name(TEST_2).description(DESCRIPTION));

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, 14,result.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

    }

    @Test
    @Description("When version id not attached to artifact then delete")
    public void whenVersionIdDoesExistsForArtifactThenBadRequest() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(VERDEL).description("test").colour(GREEN).maxAssignments(1).key("5"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("VerDelPass")
                        .version("version2").type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description("description2").vendor("vendor2").swInstallerType(swInstallerType.getName()));
        final Version swVersion = versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(DESCRIPTION));

        mvc.perform(
                        get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), swVersion.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), swVersion.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(
                        get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), swVersion.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When software module id not present thne Software Version cannot be created")
    public void whenModuleIdNotPresentThenSoftwareVersionCannotBeCreated() throws Exception {
        MgmtAddVersionRequestBody requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName("TRW12");
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        int softwareModuleId = 10;
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, softwareModuleId)

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound()).andReturn();

    }

    @Test
    @Description("When software module id present thne create Software Version")
    public void whenModuleIdPresentThenCreateSoftwareVersion() throws Exception {
        SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(VERDEL).description("test").colour(GREEN).maxAssignments(1).key("5"));

        SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name(VERDELFAIL1_ENTITYNAME)
                        .version(VERSION1).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION1).vendor(VENDOR1).swInstallerType(swInstallerType.getName()));
        MgmtAddVersionRequestBody requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName("TRW12");
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, module.getId())

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }

    @Test
    @Description("When software version name or number present then throw already exist error")
    public void whenNameOrNumberPresentThenDontCreateSoftwareVersion() throws Exception {
        SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(VERDEL).description("test").colour(GREEN).maxAssignments(1).key("5"));

        SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name(VERDELFAIL1_ENTITYNAME)
                        .version(VERSION1).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION1).vendor(VENDOR1).swInstallerType(swInstallerType.getName()));

        versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(VERSION_SW));

        SoftwareModule module2 = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("VerDelFail2")
                        .version("version2").type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description("description2").vendor("vendor2").swInstallerType(swInstallerType.getName()));

        versionManagement.create(entityFactory.version().create().number(2)
                .softwareModuleId(module2.getId()).name(TEST_3).description(VERSION_SW));

        MgmtAddVersionRequestBody requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName(TEST_2);
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, 1L, module.getId())

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());

        requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName(TEST_3);
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(11);
        requestBodyJson = objectMapper.writeValueAsString(requestBody);
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, 1L, module.getId())

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isConflict());

        requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName(TEST_3);
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        requestBodyJson = objectMapper.writeValueAsString(requestBody);
        mvc.perform(post(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, module.getId())

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated());
    }

    @Test
    @Description("For a software module id this will fetch all the software versions")
    public void givenSoftwareModuleIdWhenExistsThenReturnVersions() throws Exception {
        final SoftwareModule sm = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createVersion(sm.getId(), "ABVERS1");
        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, sm.getId().intValue()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(hasSize(1)));

    }

    @Test
    @Description("Software module id not found")
    public void givenVersionWhenSoftwareModuleIdNotPresentThrowNotFound() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name("GETSWID").description("SWTEST").colour(GREEN).maxAssignments(1).key("6"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("SWNOTFOUND")
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        final Version result = versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(VERSION_SW));

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, 111, result.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When software module id is not present for the software version then not found")
    public void whenSoftwareModuleIdNotExistForVersionThenNotFoundForDelete() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name(VERDEL).description("test").colour(GREEN).maxAssignments(1).key("5"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("VerDelFail")
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));
        final SoftwareModule module1 = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("SwNotForVer")
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        final Version result = versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(DESCRIPTION));

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), result.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        mvc.perform(
                        delete(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module1.getId().intValue(), result.getId()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When given version not found then throw not found")
    public void givenVersionWhenNotExisnThenNotFound() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name("SWVER").description("test").colour(GREEN).maxAssignments(1).key("5"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("SWVERNOTFOUND")
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        versionManagement.create(entityFactory.version().create().number(11)
                .softwareModuleId(module.getId()).name(TEST_2).description(DESCRIPTION));

        mvc.perform(delete(MgmtRestConstants.SOFTWAREMODULE_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue(), 99))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When given software module id not found then throw not found")
    public void givensoftwareModuleIdWhenNotExisnThenNotFound() throws Exception {

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, 33)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }
    @Test
    @Description("When given software module id not found then throw not found")
    public void givenSwVersionWhenNotExisnThenNotFound() throws Exception {

        final SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name("SWVER").description("test").colour(GREEN).maxAssignments(1).key("5"));

        final SoftwareModule module = softwareModuleManagement
                .create(entityFactory.softwareModule().create().name("SWVERNOTFOUND")
                        // .maxAssignments(1)
                        .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                        .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        mvc.perform(get(MgmtRestConstants.SOFTWAREMODULE_VERSION_V1_REQUEST_MAPPING, TENANT_ID, module.getId().intValue()))

                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

    @Test
    @Description("When scomo Id not present or null then Software Version cannot be created")
    public void whenScomoIdNotPresentNullThenSoftwareVersionCannotBeCreated() throws Exception {
        MgmtAddVersionRequestBody requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName("TRW12");
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        String scomoId = "TRW34";
        // Test with non-existing scomoId
        mvc.perform(post(MgmtRestConstants.SCOMO_VERSION_V1_REQUEST_MAPPING, TENANT_ID, scomoId)

                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound()).andReturn();
        // Get scomo not found exception
        mvc.perform(get(MgmtRestConstants.SCOMO_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, "SCOMO1", 1500)).andDo(MockMvcResultPrinter.print())
                .andExpect(status().isNotFound());

    }

    @Test
    @Description("Create, Delete and Get an existing software version")
    public void givenSwVersionForScomoWhenDeleteVersionThenSuccess() throws Exception {

        SoftwareModuleType type = softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                .name("TEST_6").description(TEST_2).colour("blue").maxAssignments(1).key("89"));

        SoftwareModuleFormat format;
        java.util.Optional<SoftwareModuleFormat> existingFormat = softwareModuleFormatManagement.getByName("qnx");
        if (existingFormat.isPresent()) {
            format = existingFormat.get();
        } else {
            format = softwareModuleFormatManagement.create(entityFactory.softwareModuleFormat().create().name("qnx").description("desc").key("89"));
        }

        SoftwareModule module = softwareModuleManagement.create(entityFactory.softwareModule().create().name("HPC10ROWDEV680********************")
                .version(VERSION).type(type.getKey()).format(format.getKey()).encrypted(false)
                .description(DESCRIPTION).vendor(VENDOR).swInstallerType(swInstallerType.getName()));

        MgmtAddVersionRequestBody requestBody = new MgmtAddVersionRequestBody();
        requestBody.setName("TRW12");
        requestBody.setDescription(TEST_MODULE_NOT_PRESENT);
        requestBody.setNumber(2);
        ObjectMapper objectMapper = new ObjectMapper();
        String requestBodyJson = objectMapper.writeValueAsString(requestBody);
        //create a version for the module based on scomo name
        final MvcResult result = mvc.perform(post(MgmtRestConstants.SCOMO_VERSION_V1_REQUEST_MAPPING, TENANT_ID, module.getName())
                        .contentType(MediaType.APPLICATION_JSON).content(requestBodyJson))
                .andDo(MockMvcResultPrinter.print())
                .andExpect(status().isCreated()).andReturn();
        // Extract ID from the response
        String responseContent = result.getResponse().getContentAsString();
        String versionId = JsonPath.parse(responseContent).read("$.id", String.class);

        // Get the version by ID and scomo name
        mvc.perform(get(MgmtRestConstants.SCOMO_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getName(), versionId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());
        // Delete the version by ID and scomo name
        mvc.perform(delete(MgmtRestConstants.SCOMO_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getName(), versionId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        // Verify that the version is deleted
        mvc.perform(get(MgmtRestConstants.SCOMO_VERSION_ID_V1_REQUEST_MAPPING, TENANT_ID, module.getName(), versionId))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isNotFound());

    }

}