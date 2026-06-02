/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autoassign;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Test class for {@link AutoAssignChecker}.
 *
 */
@Feature("Component Tests - Repository")
@Story("Auto assign checker")
class AutoAssignCheckerIntTest extends AbstractJpaIntegrationTest {

    private static final String NAME = "name_";
    private static final String DESCRIPTION = " description";
    private static final String NAME_QUERY = "name==*";

    @Autowired
    private AutoAssignChecker autoAssignChecker;

    @Autowired
    private ActionRepository actionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    public void tearDown(){
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model");
    }

    @Test
    @Description("Verifies that a running action is auto canceled by a AutoAssignment which assigns another distribution-set.")
    void autoAssignDistributionSetAndAutoCloseOldActions() {

        tenantConfigurationManagement
                .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, true);

        try {
            final String knownControllerId = "controller12345";
            final DistributionSet firstDistributionSet = testdataFactory.createDistributionSet();
            final DistributionSet secondDistributionSet = testdataFactory.createDistributionSet("second");
            Target target = testdataFactory.createTarget(knownControllerId);
            final DistributionSetAssignmentResult assignmentResult = assignDistributionSet(firstDistributionSet.getId(),
                    knownControllerId);
            final Long manuallyAssignedActionId = getFirstAssignedActionId(assignmentResult);

            // target filter query that matches all targets
            final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement
                    .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_QUERY));
            targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                    .updateAutoAssign(targetFilterQuery.getId()).ds(secondDistributionSet.getId()));
            // Run the check
            autoAssignChecker.checkAllTargets();

            // verify that manually created action is canceled and action
            // created from AutoAssign is running
            final List<JpaAction> actionsByKnownTarget = actionRepository.findByTarget(target, false);
            actionsByKnownTarget.addAll(actionRepository.findByTarget(target, true));
            // should be 2 actions, one manually and one from the AutoAssign
            assertThat(actionsByKnownTarget).hasSize(2);
            // verify that manually assigned action is still running
            assertThat(actionRepository.findById(manuallyAssignedActionId).get().getStatus())
                    .isEqualTo(DeviceActionStatus.CANCELED);
            // verify that AutoAssign created action is running
            final Action rolloutCreatedAction = actionsByKnownTarget.stream()
                    .filter(action -> !action.getId().equals(manuallyAssignedActionId)).findAny().get();
            assertThat(rolloutCreatedAction.getStatus()).isEqualTo(DeviceActionStatus.RUNNING);
            assertThat(rolloutCreatedAction.getUserAcceptanceRequired()).isEqualTo(MgmtRolloutUserAcceptanceRequired.YES);
        } finally {
            tenantConfigurationManagement
                    .addOrUpdateConfiguration(TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, false);
        }
    }

    @Test
    @Description("Test auto assignment of a DS to filtered targets")
    void checkAutoAssign() {

        final DistributionSet setA = testdataFactory.createDistributionSet("dsA"); // will
                                                                                   // be
                                                                                   // auto
                                                                                   // assigned
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        // target filter query that matches all targets
        final TargetFilterQuery targetFilterQuery = targetFilterQueryManagement.updateAutoAssignDS(entityFactory
                .targetFilterQuery()
                .updateAutoAssign(targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_QUERY)).getId())
                .ds(setA.getId()));

        final String targetDsAIdPref = "targ";
        final List<Target> targets = testdataFactory.createTargets(25, targetDsAIdPref,
                targetDsAIdPref.concat(DESCRIPTION));
        final int targetsCount = targets.size();

        // assign set A to first 10 targets
        assignDistributionSet(setA, targets.subList(0, 10));
        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(0, 10), targetsCount);

        // assign set B to first 5 targets
        // they have now 2 DS in their action history and should not get updated
        // with dsA
        assignDistributionSet(setB, targets.subList(0, 5));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

        // assign set B to next 10 targets
        assignDistributionSet(setB, targets.subList(10, 20));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(10, 20), targetsCount);

        // Count the number of targets that will be assigned with setA
        assertThat(targetManagement.countByRsqlAndNonDSAndCompatible(setA.getId(), targetFilterQuery.getQuery()))
                .isEqualTo(15);

        // Run the check
        autoAssignChecker.checkAllTargets();

        verifyThatTargetsHaveDistributionSetAssignment(setA, targets.subList(5, 25), targetsCount);

        // first 5 should keep their dsB, because they already had the dsA once
        verifyThatTargetsHaveDistributionSetAssignment(setB, targets.subList(0, 5), targetsCount);

        verifyThatCreatedActionsAreInitiatedByCurrentUser(targetFilterQuery, setA, targets);
    }

    @Test
    @Description("Test auto assignment of a DS for a specific device")
    void checkAutoAssignmentForDevice() {

        final DistributionSet toAssignDs = testdataFactory.createDistributionSet();

        // target filter query that matches all targets
        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_QUERY)).getId())
                .ds(toAssignDs.getId()));

        final List<Target> targets = testdataFactory.createTargets(25);
        final int targetsCount = targets.size();

        // Run the check
        autoAssignChecker.checkSingleTarget(targets.get(0).getControllerId());

        verifyThatTargetsHaveDistributionSetAssignment(toAssignDs, targets.subList(0, 1), targetsCount);

        verifyThatTargetsNotHaveDistributionSetAssignment(toAssignDs, targets.subList(1, 25));
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Test auto assignment of a DS to filtered targets with different confirmation options")
    void checkAutoAssignWithConfirmationOptions(final boolean confirmationFlowActive, final boolean confirmationRequired,
                                                final DeviceActionStatus expectedStatus) {

        final DistributionSet distributionSet = testdataFactory.createDistributionSet("dsA");

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

      targetFilterQueryManagement.updateAutoAssignDS(entityFactory
                .targetFilterQuery()
                .updateAutoAssign(targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_QUERY)).getId())
                .ds(distributionSet.getId()).confirmationRequired(confirmationRequired));

        final String targetDsAIdPref = "targ";
        final List<Target> targets = testdataFactory.createTargets(20, targetDsAIdPref,
                targetDsAIdPref.concat(DESCRIPTION));

        // Run the check
        autoAssignChecker.checkAllTargets();

        verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(distributionSet, targets, expectedStatus);
    }

    @ParameterizedTest
    @MethodSource("confirmationOptions")
    @Description("Test auto assignment of a DS for a specific device with different confirmation options")
    void checkAutoAssignmentForDeviceWithConfirmationRequired(final boolean confirmationFlowActive,
                                                              final boolean confirmationRequired, final DeviceActionStatus expectedStatus) {

        final DistributionSet toAssignDs = testdataFactory.createDistributionSet();

        if (confirmationFlowActive) {
            enableConfirmationFlow();
        }

        // target filter query that matches all targets
        targetFilterQueryManagement.updateAutoAssignDS(entityFactory.targetFilterQuery()
                .updateAutoAssign(targetFilterQueryManagement
                        .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_QUERY)).getId())
                .ds(toAssignDs.getId()).confirmationRequired(confirmationRequired));

        final List<Target> targets = testdataFactory.createTargets(25);

        // Run the check
        autoAssignChecker.checkSingleTarget(targets.get(0).getControllerId());
        verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(toAssignDs, targets.subList(0, 1), expectedStatus);

        verifyThatTargetsNotHaveDistributionSetAssignment(toAssignDs, targets.subList(1, 25));
    }

    private static Stream<Arguments> confirmationOptions() {
        return Stream.of( //
                Arguments.of(true, false, DeviceActionStatus.RUNNING), //
                Arguments.of(false, true, DeviceActionStatus.RUNNING), //
                Arguments.of(false, false, DeviceActionStatus.RUNNING));
    }

    @Test
    @Description("Test auto assignment of an incomplete DS to filtered targets, that causes failures")
    void checkAutoAssignWithFailures() {

        // incomplete distribution set that will be assigned
        final DistributionSet setF = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("dsA").version("1").type(testdataFactory.findOrCreateDefaultTestDsType()));
        final DistributionSet setA = testdataFactory.createDistributionSet("dsAa");
        final DistributionSet setB = testdataFactory.createDistributionSet("dsB");

        final String targetDsAIdPref = "targA";
        final String targetDsFIdPref = "targB";

        // target filter query that matches first bunch of targets, that should
        // fail
        assertThatExceptionOfType(IncompleteDistributionSetException.class).isThrownBy(() -> {
            final Long filterId = targetFilterQueryManagement.create(
                    entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query("id==" + targetDsFIdPref + "*"))
                    .getId();
            targetFilterQueryManagement
                    .updateAutoAssignDS(entityFactory.targetFilterQuery().updateAutoAssign(filterId).ds(setF.getId()));
        });
        // target filter query that matches failed bunch of targets
        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("filterB")
                .query("id==" + targetDsAIdPref + "*").autoAssignDistributionSet(setA.getId()));

        final List<Target> targetsF = testdataFactory.createTargets(10, targetDsFIdPref,
                targetDsFIdPref.concat(DESCRIPTION));

        final List<Target> targetsA = testdataFactory.createTargets(10, targetDsAIdPref,
                targetDsAIdPref.concat(DESCRIPTION));

        final int targetsCount = targetsA.size() + targetsF.size();

        // assign set B to first 5 targets of fail group
        assignDistributionSet(setB, targetsF.subList(0, 5));
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // Run the check
        autoAssignChecker.checkAllTargets();

        // first 5 targets of the fail group should still have setB
        verifyThatTargetsHaveDistributionSetAssignment(setB, targetsF.subList(0, 5), targetsCount);

        // all targets of A group should have received setA
        verifyThatTargetsHaveDistributionSetAssignment(setA, targetsA, targetsCount);

    }

    /**
     * @param set
     *            the expected distribution set
     * @param targets
     *            the targets that should have it
     */
    @Step
    private void verifyThatTargetsHaveDistributionSetAssignment(final DistributionSet set, final List<Target> targets,
            final int count) {
        final List<Long> targetIds = targets.stream().map(Target::getId).collect(Collectors.toList());

        final Slice<Target> targetsAll = targetManagement.findAll(PAGE);
        assertThat(targetsAll).as("Count of targets").hasSize(count);

        for (final Target target : targetsAll) {
            if (targetIds.contains(target.getId())) {
                assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId()).get())
                        .as("assigned DS").isEqualTo(set);
            }
        }

    }

    @Step
    private void verifyThatTargetsHaveDistributionSetAssignedAndActionStatus(final DistributionSet set,
                                                                             final List<Target> targets, final DeviceActionStatus status) {
        final List<String> targetIds = targets.stream().map(Target::getControllerId).collect(Collectors.toList());
        final List<Target> targetsWithAssignedDS = targetManagement.findByAssignedDistributionSet(PAGE, set.getId())
                .getContent();
        assertThat(targetsWithAssignedDS).isNotEmpty();
        assertThat(targetsWithAssignedDS).allMatch(target -> targetIds.contains(target.getControllerId()));

        final List<Action> actionsByDs = deploymentManagement.findActionsByDistributionSet(PAGE, set.getId())
                .getContent();

        assertThat(actionsByDs).hasSize(targets.size());
        assertThat(actionsByDs).allMatch(action -> action.getStatus() == status);
    }

    @Step
    private void verifyThatTargetsNotHaveDistributionSetAssignment(final DistributionSet set,
            final List<Target> targets) {
        final List<Long> targetIds = targets.stream().map(Target::getId).collect(Collectors.toList());

        final Slice<Target> targetsAll = targetManagement.findAll(PAGE);

        for (final Target target : targetsAll) {
            if (targetIds.contains(target.getId())) {
                assertThat(deploymentManagement.getAssignedDistributionSet(target.getControllerId())).isEmpty();
            }
        }

    }

    @Step
    private void verifyThatCreatedActionsAreInitiatedByCurrentUser(final TargetFilterQuery targetFilterQuery,
            final DistributionSet distributionSet, final List<Target> targets) {
        final Set<String> targetIds = targets.stream().map(Target::getControllerId).collect(Collectors.toSet());

        actionRepository.findByDistributionSetId(Pageable.unpaged(), distributionSet.getId()).stream()
                .filter(a -> targetIds.contains(a.getTarget().getControllerId()))
                .forEach(a -> assertThat(a.getInitiatedBy())
                        .as("Action should be initiated by the user who initiated the auto assignment")
                        .isEqualTo(targetFilterQuery.getAutoAssignInitiatedBy()));
    }

    @Test
    @Description("Test auto assignment of a distribution set with NO and YES user acceptance required")
    void checkAutoAssignWithDifferentUserAcceptanceRequired() {
        final DistributionSet distributionSet = testdataFactory.createDistributionSet();
        final String targetDsAIdPref = "A";
        final String targetDsBIdPref = "B";

        final List<Target> targetsA = createTargetsAndAutoAssignDistSet(targetDsAIdPref, 5, distributionSet,
                MgmtRolloutUserAcceptanceRequired.NO);
        final List<Target> targetsB = createTargetsAndAutoAssignDistSet(targetDsBIdPref, 10, distributionSet,
                MgmtRolloutUserAcceptanceRequired.YES);

        final int targetsCount = targetsA.size() + targetsB.size();

        autoAssignChecker.checkAllTargets();

        verifyThatTargetsHaveDistributionSetAssignment(distributionSet, targetsA, targetsCount);
        verifyThatTargetsHaveDistributionSetAssignment(distributionSet, targetsB, targetsCount);

        verifyThatTargetsHaveAssignmentUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.NO, targetsA);
        verifyThatTargetsHaveAssignmentUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES, targetsB);
    }

    @Step
    private List<Target> createTargetsAndAutoAssignDistSet(final String prefix, final int targetCount,
            final DistributionSet distributionSet, final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {

        final List<Target> targets = testdataFactory.createTargets(targetCount, "target" + prefix,
                prefix.concat(DESCRIPTION));
        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("filter" + prefix).query("id==target" + prefix + "*")
                        .autoAssignDistributionSet(distributionSet).autoAssignUserAcceptanceRequired(userAcceptanceRequired));

        return targets;
    }

    @Step
    private void verifyThatTargetsHaveAssignmentUserAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final List<Target> targets) {
        final List<Action> actions = targets.stream().map(Target::getControllerId).flatMap(
                controllerId -> deploymentManagement.findActionsByTarget(controllerId, PAGE).getContent().stream())
                .collect(Collectors.toList());

        assertThat(actions).hasSize(targets.size());
        assertThat(actions).allMatch(action -> action.getUserAcceptanceRequired().equals(userAcceptanceRequired));
    }

    @Test
    @Description("An auto assignment target filter with weight creates actions with weights")
    void actionsWithWeightAreCreated() throws Exception {
        final int amountOfTargets = 5;
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final int weight = 32;
        enableMultiAssignments();

        targetFilterQueryManagement.create(entityFactory.targetFilterQuery().create().name("a").query(NAME_QUERY)
                .autoAssignDistributionSet(ds).autoAssignWeight(weight));
        testdataFactory.createTargets(amountOfTargets);
        autoAssignChecker.checkAllTargets();

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions).hasSize(amountOfTargets);
        assertThat(actions).allMatch(action -> action.getWeight().get() == weight);
    }

    @Test
    @Description("An auto assignment target filter without weight still works after multi assignment is enabled")
    void filterWithoutWeightWorksInMultiAssignmentMode() throws Exception {
        final int amountOfTargets = 5;
        final DistributionSet ds = testdataFactory.createDistributionSet();
        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("a").query(NAME_QUERY).autoAssignDistributionSet(ds));
        enableMultiAssignments();

        testdataFactory.createTargets(amountOfTargets);
        autoAssignChecker.checkAllTargets();

        final List<Action> actions = deploymentManagement.findActionsAll(PAGE).getContent();
        assertThat(actions).hasSize(amountOfTargets);
        assertThat(actions).allMatch(action -> !action.getWeight().isPresent());
    }

    @Test
    @Description("Verifies an auto assignment only creates actions for compatible targets")
    void checkAutoAssignmentWithIncompatibleTargets() {
        final int TARGET_COUNT = 5;

        final DistributionSet testDs = testdataFactory.createDistributionSet();
        final DistributionSetType incompatibleDsType1 = testdataFactory
                .findOrCreateDistributionSetType("incompatibleDsType1", "incompDsType1");
        final DistributionSetType incompatibleDsType2 = testdataFactory
                .findOrCreateDistributionSetType("incompatibleDsType2", "incompDsType2");
        final TargetFilterQuery testFilter = targetFilterQueryManagement.create(entityFactory.targetFilterQuery()
                .create().name("test-filter").query(NAME_QUERY).autoAssignDistributionSet(testDs));

        final TargetType incompatibleEmptyType = testdataFactory.createTargetType("incompatibleEmptyType",
                Collections.emptyList());
        final TargetType incompatibleSingleType = testdataFactory.createTargetType("incompatibleSingleType",
                Collections.singletonList(incompatibleDsType1));
        final TargetType incompatibleMultiType = testdataFactory.createTargetType("incompatibleMultiType",
                Arrays.asList(incompatibleDsType1, incompatibleDsType2));
        final TargetType compatibleSingleType = testdataFactory.createTargetType("compatibleSingleType",
                Collections.singletonList(testDs.getType()));
        final TargetType compatibleMultiType = testdataFactory.createTargetType("compatibleMultiType",
                Arrays.asList(testDs.getType(), incompatibleDsType1));

        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleEmpty", incompatibleEmptyType);
        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleSingle", incompatibleSingleType);
        testdataFactory.createTargetsWithType(TARGET_COUNT, "incompatibleMulti", incompatibleMultiType);

        final List<Target> compatibleTargetsSingleType = testdataFactory.createTargetsWithType(TARGET_COUNT,
                "compatibleSingle", compatibleSingleType);
        final List<Target> compatibleTargetsMultiType = testdataFactory.createTargetsWithType(TARGET_COUNT,
                "compatibleMulti", compatibleMultiType);
        final List<Target> compatibleTargetsWithoutType = testdataFactory.createTargets(TARGET_COUNT,
                "compatibleSingleNoType");

        final List<Long> compatibleTargets = Stream
                .of(compatibleTargetsSingleType, compatibleTargetsMultiType, compatibleTargetsWithoutType)
                .flatMap(Collection::stream).map(Target::getId).collect(Collectors.toList());
        final long compatibleCount = targetManagement.countByRsqlAndNonDSAndCompatible(testDs.getId(),
                testFilter.getQuery());
        assertThat(compatibleCount).isEqualTo(compatibleTargets.size());

        autoAssignChecker.checkAllTargets();

        final List<Action> actions = deploymentManagement.findActionsAll(Pageable.unpaged()).getContent();
        assertThat(actions).hasSize(compatibleTargets.size());
        final List<Long> actionTargets = actions.stream().map(a -> a.getTarget().getId()).collect(Collectors.toList());
        assertThat(actionTargets).containsExactlyInAnyOrderElementsOf(compatibleTargets);
    }
}
