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
import org.eclipse.hawkbit.repository.event.remote.entity.AbstractRolloutGroupEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RemoteEntityEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutGroupUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test the remote entity events.
 */
@Feature("Component Tests - Repository")
@Story("Test RolloutGroupCreatedEvent and RolloutGroupUpdatedEvent")
public class RolloutGroupEventTest extends AbstractRemoteEntityEventTest<RolloutGroup> {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

    private static ClientAndServer mockServer;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

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

    @AfterEach
    public void tearDown() {
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

    @Test
    @Description("Verifies that the rollout group entity reloading by remote created event works")
    void testRolloutGroupCreatedEvent() {
        final RolloutGroupCreatedEvent createdEvent = (RolloutGroupCreatedEvent) assertAndCreateRemoteEvent(
                RolloutGroupCreatedEvent.class);
        assertThat(createdEvent.getRolloutId()).isNotNull();
    }

    @Test
    @Description("Verifies that the rollout group entity reloading by remote updated event works")
    void testRolloutGroupUpdatedEvent() {
        assertAndCreateRemoteEvent(RolloutGroupUpdatedEvent.class);
    }

    @Override
    protected int getConstructorParamCount() {
        return 3;
    }

    @Override
    protected Object[] getConstructorParams(final RolloutGroup baseEntity) {
        return new Object[]{baseEntity, 1L, "Node"};
    }

    @Override
    protected RemoteEntityEvent<?> assertEntity(final RolloutGroup baseEntity, final RemoteEntityEvent<?> e) {
        final AbstractRolloutGroupEvent event = (AbstractRolloutGroupEvent) e;

        assertThat(event.getEntity()).isPresent().get().isSameAs(baseEntity);
        assertThat(event.getRolloutId()).isEqualTo(1L);

        AbstractRolloutGroupEvent underTestCreatedEvent = createProtoStuffEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);

        underTestCreatedEvent = createJacksonEvent(event);
        assertThat(underTestCreatedEvent.getEntity()).isPresent().get().isEqualTo(baseEntity);
        assertThat(underTestCreatedEvent.getRolloutId()).isEqualTo(1L);

        return underTestCreatedEvent;
    }

    @Override
    protected RolloutGroup createEntity() {
        final Rollout entity = createRolloutWithDependencies("rollout1", testdataFactory.createDistributionSet(), testdataFactory.createTargets("controller"));

        // Freeze the rollout
        rolloutManagement.freeze(entity.getId());
        rolloutHandler.handleAll();
        // This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(entity.getId());

        rolloutManagement.start(entity.getId());

        return rolloutGroupManagement.findByRollout(AbstractIntegrationTest.PAGE, entity.getId()).getContent().get(0);
    }




}
