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
import org.apache.commons.lang3.RandomStringUtils;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.rollout.RolloutScheduler;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@Feature("Unit Tests - Rollout Scheduler")
@Story("Rollout scheduler for handling all status and end date completion")
public class RolloutSchedulerTest extends AbstractManagementApiIntegrationTest {

    private static final String NAME = "name_";

    private static ClientAndServer mockServer;
    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

    @Autowired
    private RolloutManagement rolloutManagement;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private TestdataFactory testdataFactory;
    private RolloutScheduler rolloutScheduler;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private ActionRepository actionRepository;

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

    @BeforeEach
    public void init() {
        rolloutScheduler = new RolloutScheduler(systemManagement, rolloutHandler, systemSecurityContext);
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_action","sp_rolloutgroup","sp_rollout");
    }

    @Test
    @Description("Ensures rollout with PAUSED state moves to FINISHING when end date is reached.")
    void endDateCompletedRollout() {

        final String randomString = RandomStringUtils.randomAlphanumeric(5);
        List<Target> targets = testdataFactory.createTargets(10, randomString + "-testTarget-");

        final String prefixRolloutRunning = randomString + "1";
        Rollout rollout = createRolloutWithDependencies(prefixRolloutRunning, testdataFactory.createDistributionSet(), targets);
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        // STARTING state
        rolloutManagement.start(rollout.getId());

        // RUNNING state
        rolloutHandler.handleAll();

        // PAUSED state
        rolloutManagement.pauseRollout(rollout.getId());
        JpaRollout jpaRollout = rolloutRepository.findById(rollout.getId()).get();
        jpaRollout.setEndAt(Instant.now().minusSeconds(200).getEpochSecond());
        rolloutRepository.save(jpaRollout);
        rolloutScheduler.runningRolloutEndScheduler();
        rollout=rolloutRepository.getRolloutById(rollout.getId()).get();
        assertEquals( RolloutStatus.FINISHING,rollout.getStatus());
        var rolloutGroups=rolloutGroupRepository.findByRolloutId(rollout.getId());
        rolloutGroups.forEach(rolloutGroup -> {assertEquals(RolloutGroupStatus.FINISHING, rolloutGroup.getStatus());});
        var actions=actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true);
        actions.forEach(action -> {
            if(!action.getStatus().equals(DeviceActionStatus.RUNNING)){
                assertEquals(DeviceActionStatus.FINISHED_NOT_EXECUTED,action.getStatus());
            }
        });

    }

}
