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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.assertj.core.api.Assertions;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.event.remote.TargetTagDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetFilterQueryCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.rest.util.JsonBuilder;
import org.eclipse.hawkbit.rest.util.MockMvcResultPrinter;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.ResultActions;


import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Spring MVC Tests against the MgmtTargetTagResource.
 */
@Feature("Component Tests - Management API")
@Story("Target Tag Resource")
public class MgmtTargetTagResourceTest extends AbstractManagementApiIntegrationTest {

    private static final Long TEST_TENANT_ID = 1L;
    private static final String TARGET_TAGS_ENDPOINT = MgmtRestConstants.TARGET_TAG_V1_REQUEST_MAPPING_TENANT.replace("{tenantId}", String.valueOf(TEST_TENANT_ID));
    private static final String TARGETTAGS_ROOT_TENANT = "http://localhost" + TARGET_TAGS_ENDPOINT + "/";
    private static final String TARGETS_URL = "/targets";
    public static final String TARGETS_PATH = "/targets/";

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;

    private static ClientAndServer mockServer;
    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

    @AfterEach
    void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_filter_query", "sp_target_tag", "sp_target", "sp_vehicle_ecu",
                "sp_vehicle_model", "sp_action", "sp_rolloutgroup", "sp_rollout", "sp_distribution_set", "sp_artifact_software_module",
                "sp_software_versions", "sp_base_software_module");

    }

    @BeforeEach
    void init() throws Exception {
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @BeforeAll
    static void mockPublishRolloutStatus() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_ROLLOUT_STATUS_ENDPOINT)).
                respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    @Test
    @Description("Verfies that a paged result list of target tags reflects the content on the repository side.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 2)})
    void getTargetTags() throws Exception {
        final List<TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag assigned = tags.get(0);
        final TargetTag unassigned = tags.get(1);

        mvc.perform(get(TARGET_TAGS_ENDPOINT).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(assigned)).andExpect(applyTagMatcherOnPagedResult(unassigned))
                .andExpect(applySelfLinkMatcherOnPagedResult(assigned, TARGETTAGS_ROOT_TENANT + assigned.getId()))
                .andExpect(applySelfLinkMatcherOnPagedResult(unassigned, TARGETTAGS_ROOT_TENANT + unassigned.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

    }

    @Test
    @Description("Verfies that a paged result list of target tags reflects on the content of assigned tags for specific controller/target ID")
    void getTargetTagsByTargetId() throws Exception {
        final String controllerId1 = "controllerTestId1";
        final String controllerId2 = "controllerTestId2";
        testdataFactory.createTarget(controllerId1);
        testdataFactory.createTarget(controllerId2, controllerId2, controllerId2, testdataFactory.createVehicle("X250").getId(), controllerId2);

        final List<TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag tag1 = tags.get(0);
        final TargetTag tag2 = tags.get(1);

        targetManagement.toggleTagAssignment(List.of(controllerId1, controllerId2), tag1.getName());
        targetManagement.toggleTagAssignment(List.of(controllerId2), tag2.getName());

        mvc.perform(get(TARGET_TAGS_ENDPOINT)
                        .queryParam(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "target.controllerId==" + controllerId2)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(tag1))
                .andExpect(applyTagMatcherOnPagedResult(tag2))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, TARGETTAGS_ROOT_TENANT + tag1.getId()))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag2, TARGETTAGS_ROOT_TENANT + tag2.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(2)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(2)));

        mvc.perform(get(TARGET_TAGS_ENDPOINT)
                        .queryParam(MgmtRestConstants.REQUEST_PARAMETER_SEARCH, "target.controllerId==" + controllerId1)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(tag1))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, TARGETTAGS_ROOT_TENANT + tag1.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    @Test
    @Description("Verifies that a page result when listing tags reflects on the content in the repository when filtered by 2 fields - one tag field and one target field")
    void getTargetTagsFilteredByColorAndTargetId() throws Exception {
        final String controllerId1 = "controllerTestId1";
        final String controllerId2 = "controllerTestId2";
        testdataFactory.createTarget(controllerId1);
        testdataFactory.createTarget(controllerId2, controllerId2, controllerId2, testdataFactory.createVehicle("X250").getId(),controllerId2);

        final List<TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag tag1 = tags.get(0);
        final TargetTag tag2 = tags.get(1);

        targetManagement.toggleTagAssignment(List.of(controllerId1, controllerId2), tag1.getName());
        targetManagement.toggleTagAssignment(List.of(controllerId2), tag2.getName());

        // pass here q directly as a pure string because .queryParam method delimiters the parameters in q with ,
        // which is logical OR, we want AND here
        mvc.perform(get(TARGET_TAGS_ENDPOINT +
                        "?" + MgmtRestConstants.REQUEST_PARAMETER_SEARCH + "=target.controllerId==" + controllerId2 + ";colour==" + tag1.getColour())
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnPagedResult(tag1))
                .andExpect(applySelfLinkMatcherOnPagedResult(tag1, TARGETTAGS_ROOT_TENANT + tag1.getId()))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(1)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(1)));
    }

    @Test
    @Description("Verfies that a single result of a target tag reflects the content on the repository side.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 2)})
    void getTargetTag() throws Exception {
        final List<TargetTag> tags = testdataFactory.createTargetTags(2, "");
        final TargetTag assigned = tags.get(0);

        mvc.perform(get(TARGET_TAGS_ENDPOINT + "/" + assigned.getId())
                        .accept(MediaType.APPLICATION_JSON)).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
                .andExpect(applyTagMatcherOnSingleResult(assigned))
                .andExpect(applySelfLinkMatcherOnSingleResult(TARGETTAGS_ROOT_TENANT + assigned.getId()))
                .andExpect(jsonPath("_links.assignedTargets.href",
                        equalTo(TARGETTAGS_ROOT_TENANT + assigned.getId() + "/targets?offset=0&limit=50")));

    }

    @Test
    @Description("Verifies that created target tags are stored in the repository as send to the API.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 2)})
    void createTargetTags() throws Exception {
        final Tag tagOne = entityFactory.tag().create().colour("testcol1").description("its a test1").name("thetest1")
                .build();
        final Tag tagTwo = entityFactory.tag().create().colour("testcol2").description("its a test2").name("thetest2")
                .build();

        final ResultActions result = mvc
                .perform(post(TARGET_TAGS_ENDPOINT)
                        .content(JsonBuilder.tags(Arrays.asList(tagOne, tagTwo)))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag createdOne = targetTagManagement.findByRsql(PAGE, "name==thetest1").getContent().get(0);
        assertThat(createdOne.getName()).isEqualTo(tagOne.getName());
        assertThat(createdOne.getDescription()).isEqualTo(tagOne.getDescription());
        assertThat(createdOne.getColour()).isEqualTo(tagOne.getColour());
        final Tag createdTwo = targetTagManagement.findByRsql(PAGE, "name==thetest2").getContent().get(0);
        assertThat(createdTwo.getName()).isEqualTo(tagTwo.getName());
        assertThat(createdTwo.getDescription()).isEqualTo(tagTwo.getDescription());
        assertThat(createdTwo.getColour()).isEqualTo(tagTwo.getColour());

        result.andExpect(applyTagMatcherOnArrayResult(createdOne)).andExpect(applyTagMatcherOnArrayResult(createdTwo));
    }

    @Test
    @Description("Verifies that an updated target tag is stored in the repository as send to the API.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetTagUpdatedEvent.class, count = 1)})
    void updateTargetTag() throws Exception {
        final List<TargetTag> tags = testdataFactory.createTargetTags(1, "");
        final TargetTag original = tags.get(0);

        final Tag update = entityFactory.tag().create().name("updatedName").colour("updatedCol")
                .description("updatedDesc").build();

        final ResultActions result = mvc
                .perform(put(TARGET_TAGS_ENDPOINT + "/" + original.getId())
                        .content(JsonBuilder.tag(update)).contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final Tag updated = targetTagManagement.findByRsql(PAGE, "name==updatedName").getContent().get(0);
        assertThat(updated.getName()).isEqualTo(update.getName());
        assertThat(updated.getDescription()).isEqualTo(update.getDescription());
        assertThat(updated.getColour()).isEqualTo(update.getColour());

        result.andExpect(applyTagMatcherOnArrayResult(updated)).andExpect(applyTagMatcherOnArrayResult(updated));
    }

    @Test
    @Description("Verfies that the delete call is reflected by the repository.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetTagDeletedEvent.class, count = 1)})
    void deleteTargetTag() throws Exception {
        final List<TargetTag> tags = testdataFactory.createTargetTags(1, "");
        final TargetTag original = tags.get(0);

        mvc.perform(delete(TARGET_TAGS_ENDPOINT + "/" + original.getId()))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        assertThat(targetTagManagement.get(original.getId())).isNotPresent();
    }

    @Test
    @Description("Ensures that assigned targets to tag in repository are listed with proper paging results.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5), @Expect(type = TargetUpdatedEvent.class, count = 5)})
    void getAssignedTargets() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.toggleTagAssignment(targets.stream().map(Target::getControllerId).collect(Collectors.toList()),
                tag.getName());

        mvc.perform(get(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_URL))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(targetsAssigned)));
    }

    @Test
    @Description("Ensures that assigned DS to tag in repository are listed with proper paging results with paging limit parameter.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5), @Expect(type = TargetUpdatedEvent.class, count = 5)})
    void getAssignedTargetsWithPagingLimitRequestParameter() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final int limitSize = 1;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.toggleTagAssignment(targets.stream().map(Target::getControllerId).collect(Collectors.toList()),
                tag.getName());

        mvc.perform(get(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_URL)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(limitSize)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(limitSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(limitSize)));
    }

    @Test
    @Description("Ensures that assigned targets to tag in repository are listed with proper paging results with paging limit and offset parameter.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 5), @Expect(type = TargetUpdatedEvent.class, count = 5)})
    void getAssignedTargetsWithPagingLimitAndOffsetRequestParameter() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 5;
        final int offsetParam = 2;
        final int expectedSize = targetsAssigned - offsetParam;

        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        targetManagement.toggleTagAssignment(targets.stream().map(Target::getControllerId).collect(Collectors.toList()),
                tag.getName());

        mvc.perform(get(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_URL)
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, String.valueOf(offsetParam))
                        .param(MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, String.valueOf(targetsAssigned)))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_TOTAL, equalTo(targetsAssigned)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_SIZE, equalTo(expectedSize)))
                .andExpect(jsonPath(MgmtTargetResourceTest.JSON_PATH_PAGED_LIST_CONTENT, hasSize(expectedSize)));
    }

    @Test
    @Description("verfies that tag assignments done through toggle API command are correctly assigned or unassigned.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 4)})
    void toggleTagAssignment() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);

        ResultActions result = toggle(tag, targets);

        List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsAll(targets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0), "assignedTargets"))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1), "assignedTargets"));

        result = toggle(tag, targets);

        updated = targetManagement.findAll(PAGE).getContent();

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0), "unassignedTargets"))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1), "unassignedTargets"));

        assertThat(targetManagement.findByTag(PAGE, tag.getId())).isEmpty();
    }

    private ResultActions toggle(final TargetTag tag, final List<Target> targets) throws Exception {
        return mvc
                .perform(patch(TARGET_TAGS_ENDPOINT + "/" + tag.getId()
                        + TARGETS_URL)
                        .content(JsonBuilder.controllerIds(
                                targets.stream().map(Target::getControllerId).collect(Collectors.toList())))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));
    }

    @Test
    @Description("Verfies that tag assignments done through tag API command are correctly stored in the repository.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 2)})
    void assignTargets() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);

        final ResultActions result = mvc
                .perform(post(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_URL)
                        .content(JsonBuilder.controllerIds(
                                targets.stream().map(Target::getControllerId).collect(Collectors.toList())))
                        .contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultPrinter.print()).andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE));

        final List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsAll(targets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        result.andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(0)))
                .andExpect(applyTargetEntityMatcherOnArrayResult(updated.get(1)));
    }

    @Test
    @Description("Verfies that tag unassignments done through tag API command are correctly stored in the repository.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 2), @Expect(type = TargetUpdatedEvent.class, count = 3)})
    void unassignTarget() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        final Target assigned = targets.get(0);
        final Target unassigned = targets.get(1);

        targetManagement.toggleTagAssignment(targets.stream().map(Target::getControllerId).collect(Collectors.toList()),
                tag.getName());

        mvc.perform(delete(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_PATH
                + unassigned.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsOnly(assigned.getControllerId());
    }


    @Test
    @Description("Tag should not be able to remove from target, if target is already associated with any Rollout")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 2),
            @Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = RolloutCreatedEvent.class, count = 1),
            @Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = RolloutUpdatedEvent.class, count = 7),
            @Expect(type = TargetFilterQueryCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 1),
            @Expect(type = RolloutGroupUpdatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 4)})
    void unassignTagFromTargetWithRollout() throws Exception {

        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final int targetsAssigned = 2;
        final List<Target> targets = testdataFactory.createTargets(targetsAssigned);
        final Target targetWithRollout = targets.get(0);
        final Target targetWithoutRollout = targets.get(1);
        targetManagement.toggleTagAssignment(targets.stream().map(Target::getControllerId).collect(Collectors.toList()),
                tag.getName());
        final Rollout rollout = createRolloutWithDependencies("rollout", List.of(targetWithRollout));

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();


        rolloutManagement.start(rollout.getId());

        mvc.perform(delete(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_PATH
                + targetWithRollout.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isBadRequest());
        mvc.perform(delete(TARGET_TAGS_ENDPOINT + "/" + tag.getId() + TARGETS_PATH
                + targetWithoutRollout.getControllerId())).andDo(MockMvcResultPrinter.print()).andExpect(status().isOk());

        final List<Target> updated = targetManagement.findByTag(PAGE, tag.getId()).getContent();

        assertThat(updated.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsOnly(targetWithRollout.getControllerId());
    }

    @Test
    @Description("Verifies that an IllegalArgumentException is thrown when trying to unassign a tag that is not linked to the target")
    void unassignTagNotLinkedToTarget() throws Exception {
        final TargetTag tag = testdataFactory.createTargetTags(1, "").get(0);
        final Target target = testdataFactory.createTargets(1).get(0);
        Throwable thrown = Assertions.catchThrowable(() ->
            targetManagement.unAssignTag(target.getControllerId(), tag.getId())
        );
        Assertions.assertThat(thrown)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Tag ID is not linked to the Controller ID.");
    }

    /**
     * Creates a new rollout and its dependencies, including software modules, versions, and associations.
     *
     * @param rolloutName the name of the rollout to create
     * @param targets     the list of targets to associate with the rollout
     * @return the created Rollout object
     */
    private @NotNull Rollout createRolloutWithDependencies(String rolloutName, List<Target> targets) {
        // Create new rollout
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest(rolloutName)));

        // Create new software module and version
        SoftwareModule softwareModule = testdataFactory.createSoftwareModuleOs();
        Version version = testdataFactory.createVersion(softwareModule.getId(), "Test", 12);

        // Create artifact and associate with software module
        associateArtifactWithSoftwareModule(softwareModule, version);

        // Create and associate ds with rollout
        MgmtSoftwareModuleRequest moduleRequest = buildSoftwareModuleRequest(softwareModule.getId(), version.getId());
        rolloutManagement.associateSoftwareModulesToVersion(1L, rollout, List.of(moduleRequest));

        createRolloutGroup(targets, rollout);
        return rollout;
    }



}
