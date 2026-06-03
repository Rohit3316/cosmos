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
import org.assertj.core.api.Assertions;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidationCount;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.jdbc.JdbcTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

/**
 * Test class testing the functionality of invalidating a
 * {@link DistributionSet}
 */
@Feature("Component Tests - Repository")
@Story("Distribution set invalidation management")
class DistributionSetInvalidationManagementTest extends AbstractManagementApiIntegrationTest {

    private static ClientAndServer mockServer;
    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private TargetRepository targetRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private DistributionSetRepository distributionSetRepository;

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
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_filter_query","sp_target_tag",
                "sp_action", "sp_target", "sp_rollout", "sp_distribution_set");
    }

    @Test
    @Description("Verify invalidation of distribution sets that only removes distribution sets from auto assignments")
    void verifyInvalidateDistributionSetStopAutoAssignment() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAutoAssignment", "VINTEST179123S");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                false);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 0);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation, 1L);
        rolloutHandler.handleAll();

        Assertions.assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        Assertions.assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isNotIn(RolloutStatus.FINISHING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.getTargets()) {
            // if status is pending, the assignment has not been canceled
            Assertions.assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            Assertions.assertThat(actionRepository.findByTarget(target, true).size()).isEqualTo(1);
            Assertions.assertThat(actionRepository.findByTarget(target, true).get(0).getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
        }
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments and stops rollouts")
    void verifyInvalidateDistributionSetStopRollouts() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopRollouts", "VINTEST179128S");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 0, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation, 1L);
        rolloutHandler.handleAll();

        Assertions.assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        Assertions.assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isEqualTo(RolloutStatus.CANCELING);
        assertNoScheduledActionsExist(invalidationTestData.getRollout());
        for (final Target target : invalidationTestData.getTargets()) {
            // if status is pending, the assignment has not been canceled
            Assertions.assertThat(
                    targetRepository.findById(invalidationTestData.getTargets().get(0).getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            Assertions.assertThat(actionRepository.findByTarget(target, true).size()).isEqualTo(1);
            Assertions.assertThat(actionRepository.findByTarget(target, true).get(0).getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
        }
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and force cancels assignments")
    void verifyInvalidateDistributionSetStopAllAndForceCancel() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAllAndForceCancel", "VINTEST179129S");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.FORCE,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation, 1L);
        rolloutHandler.handleAll();

        Assertions.assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        Assertions.assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isEqualTo(RolloutStatus.CANCELING);
        assertNoScheduledActionsExist(invalidationTestData.getRollout());
        for (final Target target : invalidationTestData.getTargets()) {
            Assertions.assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.IN_SYNC);
            Assertions.assertThat(actionRepository.findByTarget(target, false).size()).isEqualTo(1);
            Assertions.assertThat(actionRepository.findByTarget(target, false).get(0).getStatus()).isEqualTo(DeviceActionStatus.CANCELED);
        }
    }

    private void assertNoScheduledActionsExist(final Rollout rollout) {
        Assertions.assertThat(
                        actionRepository.findByRolloutIdAndStatusAndActive(AbstractIntegrationTest.PAGE, rollout.getId(), DeviceActionStatus.USER_SCHEDULED, true).getTotalElements())
                .isZero();
    }

    @Test
    @Description("Verify invalidation of distribution sets that removes distribution sets from auto assignments, stops rollouts and cancels assignments")
    void verifyInvalidateDistributionSetStopAll() {
        final InvalidationTestData invalidationTestData = createInvalidationTestData(
                "verifyInvalidateDistributionSetStopAll", "VINTEST179125S");

        final DistributionSetInvalidation distributionSetInvalidation = new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.SOFT,
                true);
        final DistributionSetInvalidationCount distributionSetInvalidationCount = distributionSetInvalidationManagement
                .countEntitiesForInvalidation(distributionSetInvalidation);
        assertDistributionSetInvalidationCount(distributionSetInvalidationCount, 1, 5, 1);

        distributionSetInvalidationManagement.invalidateDistributionSet(distributionSetInvalidation, 1L);

        Assertions.assertThat(targetFilterQueryManagement.get(invalidationTestData.getTargetFilterQuery().getId()).get()
                .getAutoAssignDistributionSet()).isNull();
        Assertions.assertThat(rolloutRepository.findById(invalidationTestData.getRollout().getId()).get().getStatus())
                .isIn(RolloutStatus.CANCELING, RolloutStatus.FINISHED);
        for (final Target target : invalidationTestData.getTargets()) {
            Assertions.assertThat(targetRepository.findById(target.getId()).get().getUpdateStatus())
                    .isEqualTo(TargetUpdateStatus.PENDING);
            Assertions.assertThat(actionRepository.findByTarget(target, true).size()).isEqualTo(1);
            Assertions.assertThat(actionRepository.findByTarget(target, true).get(0).getStatus()).isEqualTo(DeviceActionStatus.CANCELING);
        }
    }

    @Test
    @Description("Verify that invalidating an incomplete distribution set throws an exception")
    void verifyInvalidateIncompleteDistributionSetThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true), 1L));
    }

    @Test
    @Description("Verify that invalidating an invalidated distribution set throws an exception")
    void verifyInvalidateInvalidatedDistributionSetThrowsException() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as("Invalid distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetInvalidationManagement.invalidateDistributionSet(
                        new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()),
                                CancelationType.SOFT, true), 1L));
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY and UPDATE_REPOSITORY is not allowed to invalidate a distribution set")
    @WithUser(authorities = {"READ_REPOSITORY", "UPDATE_REPOSITORY"})
    void verifyInvalidateWithReadAndUpdateRepoAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext
                .runAsSystem(() -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAuthority", "VINTEST179126S"));

        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception expected")
                .isThrownBy(() -> distributionSetInvalidationManagement
                        .invalidateDistributionSet(new DistributionSetInvalidation(
                                Collections.singletonList(invalidationTestData.getDistributionSet().getId()),
                                CancelationType.NONE, false), 1L));
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY, UPDATE_REPOSITORY and UPDATE_TARGET is allowed to invalidate a distribution set only without canceling rollouts")
    @WithUser(authorities = {"READ_REPOSITORY", "UPDATE_REPOSITORY", "UPDATE_TARGET"})
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext.runAsSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority", "VINTEST179127S"));

        assertThatExceptionOfType(InsufficientPermissionException.class)
                .as("Insufficient permission exception expected")
                .isThrownBy(() -> distributionSetInvalidationManagement
                        .invalidateDistributionSet(new DistributionSetInvalidation(
                                Collections.singletonList(invalidationTestData.getDistributionSet().getId()),
                                CancelationType.SOFT, true), 1L));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.NONE,
                false), 1L);
        Assertions.assertThat(
                distributionSetRepository.findById(invalidationTestData.getDistributionSet().getId()).get().isValid())
                .isFalse();
    }

    @Test
    @Description("Verify that a user that has authority READ_REPOSITORY, UPDATE_REPOSITORY, UPDATE_ROLLOUT and UPDATE_TARGET is allowed to invalidate a distribution")
    @WithUser(authorities = {"READ_REPOSITORY", "UPDATE_REPOSITORY", "UPDATE_TARGET", "UPDATE_ROLLOUT"})
    void verifyInvalidateWithReadAndUpdateRepoAndUpdateTargetAndUpdateRolloutAuthority() {
        final InvalidationTestData invalidationTestData = systemSecurityContext.runAsSystem(
                () -> createInvalidationTestData("verifyInvalidateWithUpdateRepoAndUpdateTargetAuthority", "VINTEST179124S"));

        distributionSetInvalidationManagement.invalidateDistributionSet(new DistributionSetInvalidation(
                Collections.singletonList(invalidationTestData.getDistributionSet().getId()), CancelationType.SOFT,
                true), 1L);
        Assertions.assertThat(
                distributionSetRepository.findById(invalidationTestData.getDistributionSet().getId()).get().isValid())
                .isFalse();
    }

    private InvalidationTestData createInvalidationTestData(final String testName, final String controllerId) {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final List<Target> targets = testdataFactory.createTargets(5, controllerId);
        assignDistributionSet(distributionSet, targets);
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.create(entityFactory.targetFilterQuery()
                .create().name(testName).query("name==*").autoAssignDistributionSet(distributionSet));
        Rollout rollout = createRolloutWithDependencies("rollout", distributionSet, targets);

        return new InvalidationTestData(distributionSet, targets, targetFilterQuery, rollout);
    }

    private void assertDistributionSetInvalidationCount(
            final DistributionSetInvalidationCount distributionSetInvalidationCount,
            final long expectedAutoAssignmentCount, final long expectedActionCount, final long expectedRolloutCount) {
        assertThat(distributionSetInvalidationCount.getAutoAssignmentCount()).isEqualTo(expectedAutoAssignmentCount);
        assertThat(distributionSetInvalidationCount.getActionCount()).isEqualTo(expectedActionCount);
        assertThat(distributionSetInvalidationCount.getRolloutsCount()).isEqualTo(expectedRolloutCount);
    }

    private static class InvalidationTestData {
        private final DistributionSet distributionSet;
        private final List<Target> targets;
        private final TargetFilterQuery targetFilterQuery;
        private final Rollout rollout;

        public InvalidationTestData(final DistributionSet distributionSet, final List<Target> targets,
                                    final TargetFilterQuery targetFilterQuery, final Rollout rollout) {
            super();
            this.distributionSet = distributionSet;
            this.targets = targets;
            this.targetFilterQuery = targetFilterQuery;
            this.rollout = rollout;
        }

        public DistributionSet getDistributionSet() {
            return distributionSet;
        }

        public List<Target> getTargets() {
            return targets;
        }

        public TargetFilterQuery getTargetFilterQuery() {
            return targetFilterQuery;
        }

        public Rollout getRollout() {
            return rollout;
        }
    }



}
