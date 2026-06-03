/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
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
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.exception.StopRolloutException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class testing the invalidation of a {@link DistributionSet} while the
 * handle rollouts is ongoing.
 */
@Feature("Component Tests - Repository")
@Story("Concurrent Distribution Set invalidation")
@ContextConfiguration(classes = ConcurrentDistributionSetInvalidationTest.Config.class)
@TestPropertySource(properties = {"hawkbit.server.repository.dsInvalidationLockTimeout=1"})
public class ConcurrentDistributionSetInvalidationTest extends AbstractManagementApiIntegrationTest {

    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

    private static ClientAndServer mockServer;

    @Autowired
    private RolloutRepository rolloutRepository;

    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;

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
    public void tearDown() throws Exception {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_action","sp_rollout");
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
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
    @Description("Verify that a large rollout causes a timeout when trying to invalidate a distribution set")
    void verifyInvalidateDistributionSetWithLargeRolloutThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final Rollout rollout = createRollout(distributionSet);
        rolloutHandler.handleAll();
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        final String tenant = tenantAware.getCurrentTenant();

        // run in new Thread so that the invalidation can be executed in
        // parallel
        new Thread(() -> systemSecurityContext.runAsSystemAsTenant(() -> {
            rolloutHandler.handleAll();
            return 0;
        }, tenant)).start();

        // wait until at least one RolloutGroup is created, as this means that
        // the thread has started and has acquired the lock
        Awaitility.await().atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(100))
                .until(() -> tenantAware.runAsTenant(tenant, () -> systemSecurityContext
                        .runAsSystem(() -> rolloutGroupManagement.findByRollout(PAGE, rollout.getId()).getSize() > 0)));

        assertThatExceptionOfType(StopRolloutException.class)
                .as("Invalidation of distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true), 1L));
    }

    private Rollout createRollout(DistributionSet distributionSet) {
        List<Target> targets = testdataFactory.createTargets(
                quotaManagement.getMaxTargetsPerRolloutGroup() * quotaManagement.getMaxRolloutGroupsPerRollout(),
                "invalidateDS");
        final Rollout rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("rollout", MgmtRolloutStartType.MANUAL)));
        associateRolloutWithDependencies(distributionSet, targets, rollout, true);

        return rollout;
    }

    @Configuration
    static class Config {

        /**
         * Creates a {@link RolloutGroupRepository} bean that is slow during saving already created Groups.
         * This gives this test more time to succeed
         */
        @Bean
        @Primary
        RolloutGroupRepository slowRolloutGroupRepository(final RolloutGroupRepository groupRepo) {
            final RolloutGroupRepository slowGroupRepo = mock(RolloutGroupRepository.class, delegatesTo(groupRepo));

            doAnswer(invocation -> {
                final JpaRolloutGroup group = invocation.getArgument(0);
                if (group.getId() == null) {
                    return groupRepo.save(group);
                }
                TimeUnit.SECONDS.sleep(2);
                return groupRepo.save(group);
            }).when(slowGroupRepo).save(any());

            return slowGroupRepo;
        }
    }



}
