/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Component Tests - Repository")
@Story("Target Management Searches")
class TargetManagementSearchTest extends AbstractJpaIntegrationTest {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String NAME_FILTER_WILD_QUERY = "name==*";
    private static final String ASSIGNED_STATUS = "assigned";
    private static final String INSTALLED_STATUS = "installed";
    private static final String DESCRIPTION_FILTER_WILD_QUERY = "description==*";
    private static final String PENDING_UPDATE_STATUS_QUERY = "updatestatus==pending";
    private static final String UNKNOWN_UPDATE_STATUS_QUERY = "updatestatus==unknown";
    private static final String INSTALLED_DS_NAME_QUERY = "installedds.name==";
    private static final String ASSIGNED_DS_NAME_QUERY = "assignedds.name==";
    private static final String TARGET_TAG = "tag==";
    private static final String AND = " and ";
    private static final String OR = " or ";

    @Test
    @Description("Tests different parameter combinations for target search operations. "
            + "That includes both the test itself, as a count operation with the same filters "
            + "and query definitions by RSQL (named and un-named).")
    void targetSearchWithVariousFilterCombinations() {
        final TargetTag targTagX = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));
        final TargetTag targTagY = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));
        final TargetTag targTagZ = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));
        final TargetTag targTagW = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));

        final DistributionSet setA = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());
        final DistributionSet setB = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());

        final TargetType targetTypeX = testdataFactory.createTargetType(NAME + testdataFactory.getRandomInt(),
                Collections.singletonList(setB.getType()));

        final DistributionSet installedSet = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());

        final Long lastTargetQueryNotOverdue = Instant.now().getEpochSecond();
        final Long lastTargetQueryAlwaysOverdue = 0L;
        final Long lastTargetNull = null;

        final String targetDsAIdPref = NAME + testdataFactory.getRandomInt();
        List<Target> targAs = testdataFactory.createTargets(100, targetDsAIdPref,
                targetDsAIdPref.concat(" " + DESCRIPTION), lastTargetQueryNotOverdue);
        targAs = toggleTagAssignment(targAs, targTagX).getAssignedEntity();

        final String targSpecial = NAME + testdataFactory.getRandomInt();
        final Target targSpecialName = targetManagement
                .update(entityFactory.target().update(targAs.get(0).getControllerId()).name(targSpecial));

        final String targetDsBIdPref = NAME + testdataFactory.getRandomInt();
        List<Target> targBs = testdataFactory.createTargets(100, targetDsBIdPref,
                targetDsBIdPref.concat(" " + DESCRIPTION), lastTargetQueryAlwaysOverdue);

        targBs = toggleTagAssignment(targBs, targTagY).getAssignedEntity();
        targBs = toggleTagAssignment(targBs, targTagW).getAssignedEntity();

        final String targetDsCIdPref = NAME + testdataFactory.getRandomInt();
        List<Target> targCs = testdataFactory.createTargets(100, targetDsCIdPref,
                targetDsCIdPref.concat(" " + DESCRIPTION), lastTargetQueryAlwaysOverdue);

        targCs = toggleTagAssignment(targCs, targTagZ).getAssignedEntity();
        targCs = toggleTagAssignment(targCs, targTagW).getAssignedEntity();

        final String targetDsDIdPref = NAME + testdataFactory.getRandomInt();
        final List<Target> targDs = testdataFactory.createTargets(100, targetDsDIdPref,
                targetDsDIdPref.concat(" " + DESCRIPTION), lastTargetNull);

        final String targetDsEIdPref = NAME + testdataFactory.getRandomInt();
        final List<Target> targEs = testdataFactory.createTargetsWithType(100, targetDsEIdPref, targetTypeX);

        final String assignedC = targCs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedC);
        final String assignedA = targAs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedA);
        final String assignedB = targBs.iterator().next().getControllerId();
        assignDistributionSet(setA.getId(), assignedB);
        final String assignedE = targEs.iterator().next().getControllerId();
        assignDistributionSet(setB.getId(), assignedE);
        final String installedC = targCs.iterator().next().getControllerId();
        final Long actionId = getFirstAssignedActionId(assignDistributionSet(installedSet.getId(), assignedC));

        // set one installed DS also
        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(actionId).status(DeviceActionStatus.FINISHED_NOT_EXECUTED).message("message"), null);
        assignDistributionSet(setA.getId(), installedC);

        final List<TargetUpdateStatus> unknown = List.of(TargetUpdateStatus.UNKNOWN);
        final List<TargetUpdateStatus> pending = List.of(TargetUpdateStatus.PENDING);
        final List<TargetUpdateStatus> both = Arrays.asList(TargetUpdateStatus.UNKNOWN, TargetUpdateStatus.PENDING);

        // get final updated version of targets
        targAs = targetManagement
                .getByControllerID(targAs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targBs = targetManagement
                .getByControllerID(targBs.stream().map(Target::getControllerId).collect(Collectors.toList()));
        targCs = targetManagement
                .getByControllerID(targCs.stream().map(Target::getControllerId).collect(Collectors.toList()));

        // try to find several targets with different filter settings
        verifyThat1TargetHasNameAndId(targSpecial, targSpecialName.getControllerId());
        verifyThatRepositoryContains500Targets();
        verifyThat200TargetsHaveTagD(targTagW, concat(targBs, targCs));
        verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(targTagY, targTagW, targBs, targetDsBIdPref);
        verifyThat1TargetHasTagHasDescOrNameAndDs(targTagW, setA, targetManagement.getByControllerID(assignedC).get(), targetDsCIdPref);
        verifyThat0TargetsWithTagAndDescOrNameHasDS(targTagW, setA, targetDsAIdPref);
        verifyThat0TargetsWithNameOrdescAndDSHaveTag(targTagX, setA, targetDsCIdPref);
        verifyThat3TargetsHaveDSAssigned(setA,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithDescOrNameHasDS(setA, targetManagement.getByControllerID(assignedA).get(), targetDsAIdPref);
        List<Target> expected = concat(targAs, targBs, targCs, targDs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat496TargetsAreInStatusUnknown(unknown, expected);
        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(targTagY, targTagW, unknown, expected);
        verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(setA, unknown);
        expected = concat(targAs);
        expected.remove(targetManagement.getByControllerID(assignedA).get());
        verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(unknown, expected, targetDsAIdPref);
        expected = concat(targBs);
        expected.remove(targetManagement.getByControllerID(assignedB).get());
        verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(targTagW, unknown, expected, targetDsBIdPref);
        verifyThat4TargetsAreInStatusPending(pending,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC, assignedE)));
        verifyThat3TargetsWithGivenDSAreInPending(setA, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(setA, pending,
                targetManagement.getByControllerID(assignedA).get(), targetDsAIdPref);
        verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerID(assignedB).get(), targetDsBIdPref);
        verifyThat2TargetsWithGivenTagAndDSIsInPending(targTagW, setA, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat2TargetsWithGivenTagAreInPending(targTagW, pending,
                targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(targTagW, both, concat(targBs, targCs));
        verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(installedSet, pending,
                targetManagement.getByControllerID(installedC).get());
        verifyThat1TargetHasTypeAndDSAssigned(targetTypeX, setB, targetManagement.getByControllerID(assignedE).get());
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(setA,
                targetManagement.getByControllerID(Arrays.asList(assignedA, assignedB, assignedC)));
        verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(installedSet,
                targetManagement.getByControllerID(Collections.singletonList(installedC)));
        verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(targetTypeX, targEs, targetDsEIdPref);
        verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(concat(targAs, targBs, targCs, targDs));

        expected = concat(targBs, targCs);
        expected.removeAll(targetManagement.getByControllerID(Arrays.asList(assignedB, assignedC)));
        verifyThat198TargetsAreInStatusUnknownAndOverdue(unknown, expected);
    }

    @Step
    private void verifyThat1TargetAIsInStatusPendingAndHasDSInstalled(final DistributionSet installedSet,
                                                                      final List<TargetUpdateStatus> pending, final Target expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, installedSet.getId(), Boolean.FALSE);
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + INSTALLED_DS_NAME_QUERY + installedSet.getName();
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat200targetsWithGivenTagAreInStatusPendingorUnknown(final TargetTag targTagW,
                                                                             final List<TargetUpdateStatus> both, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(both, null, null, null, Boolean.FALSE, targTagW.getName());
        final String query = "(" + PENDING_UPDATE_STATUS_QUERY + OR + UNKNOWN_UPDATE_STATUS_QUERY + ")" + AND + TARGET_TAG + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(200)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAreInPending(final TargetTag targTagW,
                                                            final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE,
                targTagW.getName());
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + TARGET_TAG + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(2)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat2TargetsWithGivenTagAndDSIsInPending(final TargetTag targTagW, final DistributionSet setA,
                                                                final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY
                + setA.getName() + ")" + AND + TARGET_TAG + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(2)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndTagAndDSIsInPending(final TargetTag targTagW,
                                                                            final DistributionSet setA, final List<TargetUpdateStatus> pending, final Target expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(pending, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY
                + setA.getName() + ")" + AND + "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText
                + "*)" + AND + TARGET_TAG + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithGivenNameOrDescAndDSIsInPending(final DistributionSet setA,
                                                                      final List<TargetUpdateStatus> pending, final Target expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(pending, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE);
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY
                + setA.getName() + ")" + AND + "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsWithGivenDSAreInPending(final DistributionSet setA,
                                                           final List<TargetUpdateStatus> pending, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, setA.getId(), Boolean.FALSE);
        final String query = PENDING_UPDATE_STATUS_QUERY + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY
                + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(3)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat4TargetsAreInStatusPending(final List<TargetUpdateStatus> pending,
                                                      final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(pending, null, null, null, Boolean.FALSE);

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(4)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, PENDING_UPDATE_STATUS_QUERY).getContent());
    }

    @Step
    private void verifyThat99TargetsWithGivenNameOrDescAndTagAreInStatusUnknown(final TargetTag targTagW,
                                                                                final List<TargetUpdateStatus> unknown, final List<Target> expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%" + searchText + "%", null, Boolean.FALSE,
                targTagW.getName());
        final String query = UNKNOWN_UPDATE_STATUS_QUERY + AND + "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + TARGET_TAG
                + targTagW.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(99)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat99TargetsWithNameOrDescriptionAreInGivenStatus(final List<TargetUpdateStatus> unknown,
                                                                          final List<Target> expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(unknown, null, "%" + searchText + "%", null, Boolean.FALSE);
        final String query = UNKNOWN_UPDATE_STATUS_QUERY + AND + "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(99)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat0TargetsAreInStatusUnknownAndHaveDSAssigned(final DistributionSet setA,
                                                                       final List<TargetUpdateStatus> unknown) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, setA.getId(), Boolean.FALSE);
        final String query = UNKNOWN_UPDATE_STATUS_QUERY + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(0)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndHaveGivenTags(final TargetTag targTagY,
                                                                        final TargetTag targTagW, final List<TargetUpdateStatus> unknown, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE, targTagY.getName(),
                targTagW.getName());
        final String query = UNKNOWN_UPDATE_STATUS_QUERY + AND + "(" + TARGET_TAG + targTagY.getName() + OR + TARGET_TAG + targTagW.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(198)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat496TargetsAreInStatusUnknown(final List<TargetUpdateStatus> unknown,
                                                        final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, null, null, null, Boolean.FALSE);

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(496)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, UNKNOWN_UPDATE_STATUS_QUERY).getContent());
    }

    @Step
    private void verifyThat198TargetsAreInStatusUnknownAndOverdue(final List<TargetUpdateStatus> unknown,
                                                                  final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(unknown, Boolean.TRUE, null, null, Boolean.FALSE);
        // be careful: simple filters are concatenated using AND-gating
        final String query = "lastcontrollerrequestat=le=${overdue_ts};updatestatus==unknown";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(198)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetWithDescOrNameHasDS(final DistributionSet setA, final Target expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(null, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE);
        final String query = "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName()
                + OR + INSTALLED_DS_NAME_QUERY + setA.getName() + ")";

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat3TargetsHaveDSAssigned(final DistributionSet setA, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, setA.getId(), Boolean.FALSE);
        final String query = ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY + setA.getName();

        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(3)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat0TargetsWithNameOrdescAndDSHaveTag(final TargetTag targTagX, final DistributionSet setA, final String searchText) {
        final FilterParams filterParams = new FilterParams(null, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE,
                targTagX.getName());
        final String query = "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + TARGET_TAG + targTagX.getName()
                + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(0)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat0TargetsWithTagAndDescOrNameHasDS(final TargetTag targTagW, final DistributionSet setA, final String searchText) {
        final FilterParams filterParams = new FilterParams(null, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + TARGET_TAG + targTagW.getName()
                + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(0)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .hasSize(targetManagement.findByRsql(PAGE, query).getContent().size());
    }

    @Step
    private void verifyThat1TargetHasTagHasDescOrNameAndDs(final TargetTag targTagW, final DistributionSet setA,
                                                           final Target expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(null, null, "%" + searchText + "%", setA.getId(), Boolean.FALSE,
                targTagW.getName());
        final String query = "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + TARGET_TAG + targTagW.getName()
                + AND + "(" + ASSIGNED_DS_NAME_QUERY + setA.getName() + OR + INSTALLED_DS_NAME_QUERY + setA.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected).containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThat1TargetHasNameAndId(final String name, final String controllerId) {
        final FilterParams filterParamsByName = new FilterParams(null, null, name, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(PAGE, filterParamsByName).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParamsByName)));

        final FilterParams filterParamsByControllerId = new FilterParams(null, null, controllerId, null, Boolean.FALSE);
        assertThat(targetManagement.findByFilters(PAGE, filterParamsByControllerId).getContent())
                .hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParamsByControllerId)));
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTagAssigned(final TargetTag targTagY,
                                                                         final TargetTag targTagW, final List<Target> expected, final String searchText) {
        final FilterParams filterParams = new FilterParams(null, null, "%" + searchText + "%", null, Boolean.FALSE,
                targTagY.getName(), targTagW.getName());
        final String query = "(" + NAME_FILTER_WILD_QUERY + searchText + "*" + OR + DESCRIPTION_FILTER_WILD_QUERY + searchText + "*)" + AND + "(" + TARGET_TAG + targTagY.getName() + OR +
                TARGET_TAG + targTagW.getName() + ")";
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(100)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @SafeVarargs
    private final List<Target> concat(final List<Target>... targets) {
        final List<Target> result = new ArrayList<>();
        Arrays.asList(targets).forEach(result::addAll);
        return result;
    }

    @Step
    private void verifyThat200TargetsHaveTagD(final TargetTag targTagD, final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, null, null, null, Boolean.FALSE, targTagD.getName());
        final String query = TARGET_TAG + targTagD.getName();
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent())
                .hasSize(200)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected)
                .containsAll(targetManagement.findByRsql(PAGE, query).getContent());
    }

    @Step
    private void verifyThatRepositoryContains500Targets() {
        final FilterParams filterParams = new FilterParams(null, null, null, null, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(500)
                .hasSize(Ints.saturatedCast(targetManagement.count()))
                .containsAll(targetManagement.findAll(PAGE).getContent());
    }

    @Step
    private void verifyThat1TargetHasTypeAndDSAssigned(final TargetType type, final DistributionSet set,
                                                       final Target expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.FALSE, type.getId());
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(1)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsExactly(expected);
    }

    @Step
    private void verifyThatTargetsHasNoTypeAndDSAssignedOrInstalled(final DistributionSet set,
                                                                    final List<Target> expected) {
        final FilterParams filterParams = new FilterParams(null, set.getId(), Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(expected.size())
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)))
                .containsAll(expected);
    }

    @Step
    private void verifyThat100TargetsContainsGivenTextAndHaveTypeAssigned(final TargetType targetType,
                                                                          final List<Target> expected, final String searchText) {
        final FilterParams filterParams = new FilterParams("%" + searchText + "%", null, Boolean.FALSE, targetType.getId());
        final List<Target> filteredTargets = targetManagement.findByFilters(PAGE, filterParams).getContent();
        assertThat(filteredTargets).as(DESCRIPTION).hasSize(100)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams)));
        // Comparing the controller ids, as one of the targets was modified, so
        // a 1:1
        // comparison of the objects is not possible
        assertThat(filteredTargets.stream().map(Target::getControllerId).collect(Collectors.toList()))
                .containsAll(expected.stream().map(Target::getControllerId).collect(Collectors.toList()));
    }

    @Step
    private void verifyThat400TargetsContainsGivenTextAndHaveNoTypeAssigned(final List<Target> expected) {
        final FilterParams filterParams = new FilterParams("%" + NAME + "%", null, Boolean.TRUE, null);
        assertThat(targetManagement.findByFilters(PAGE, filterParams).getContent()).hasSize(400)
                .hasSize(Ints.saturatedCast(targetManagement.countByFilters(filterParams))).containsAll(expected);
    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithVariousFilterCombinationsAndOrderByDistributionSet() {

        final List<Target> notAssigned = testdataFactory.createTargets(3, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
        List<Target> targAssigned = testdataFactory.createTargets(3, ASSIGNED_STATUS, DESCRIPTION);
        List<Target> targInstalled = testdataFactory.createTargets(3, INSTALLED_STATUS, DESCRIPTION);

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, DeviceActionStatus.FINISHED_NOT_EXECUTED, Collections.singletonList(INSTALLED_STATUS))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final Slice<Target> result = targetManagement.findByFilterOrderByLinkedDistributionSet(PAGE, ds.getId(),
                new FilterParams(null, null, null, null, Boolean.FALSE));

        final Comparator<TenantAwareBaseEntity> byId = (e1, e2) -> Long.compare(e1.getId(), e2.getId());

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        Collections.sort(targInstalled, byId);
        Collections.sort(targAssigned, byId);
        Collections.sort(notAssigned, byId);
        expected.addAll(targInstalled);
        expected.addAll(targAssigned);
        expected.addAll(notAssigned);

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));
    }

    @Test
    @Description("Tests the correct order of targets based on selected distribution set and sort parameter. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithOrderByDistributionSetAndSortParam() {

        final List<Target> notAssigned = testdataFactory.createTargets(3, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
        List<Target> targAssigned = testdataFactory.createTargets(3, ASSIGNED_STATUS, DESCRIPTION);
        List<Target> targInstalled = testdataFactory.createTargets(3, INSTALLED_STATUS, DESCRIPTION);

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, DeviceActionStatus.FINISHED_NOT_EXECUTED, Collections.singletonList(INSTALLED_STATUS))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final List<Target> targetsOrderedByDistAndName = targetManagement
                .findByFilterOrderByLinkedDistributionSet(PageRequest.of(0, 500, Sort.by(Direction.DESC, "name")),
                        ds.getId(), new FilterParams(null, null, null, null, Boolean.FALSE))
                .getContent();
        assertThat(targetsOrderedByDistAndName).hasSize(9);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 0, targInstalled, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 1, targInstalled, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 2, targInstalled, 0);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 3, targAssigned, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 4, targAssigned, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 5, targAssigned, 0);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 6, notAssigned, 2);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 7, notAssigned, 1);
        assertThatTargetNameEquals(targetsOrderedByDistAndName, 8, notAssigned, 0);
    }

    private void assertThatTargetNameEquals(final List<Target> targets1, final int index1, final List<Target> targets2,
                                            final int index2) {
        assertThat(targets1.get(index1).getName()).isEqualTo(targets2.get(index2).getName());
    }

    @Test
    @Description("Tests the correct order of targets with applied overdue filter based on selected distribution set. The system expects to have an order based on installed, assigned DS.")
    void targetSearchWithOverdueFilterAndOrderByDistributionSet() {

        final Long lastTargetQueryAlwaysOverdue = 0L;
        final Long lastTargetQueryNotOverdue = Instant.now().getEpochSecond();
        final Long lastTargetNull = null;

        final Long[] overdueMix = {lastTargetQueryAlwaysOverdue, lastTargetQueryNotOverdue,
                lastTargetQueryAlwaysOverdue, lastTargetNull, lastTargetQueryAlwaysOverdue};

        final List<Target> notAssigned = Lists.newArrayListWithExpectedSize(overdueMix.length);
        List<Target> targAssigned = Lists.newArrayListWithExpectedSize(overdueMix.length);
        List<Target> targInstalled = Lists.newArrayListWithExpectedSize(overdueMix.length);

        for (int i = 0; i < overdueMix.length; i++) {
            String notTarget = NAME + testdataFactory.getRandomInt();
            notAssigned.add(targetManagement
                    .create(entityFactory.target().create().controllerId(notTarget)
                            .name("not" + i).serialNumber(notTarget).vehicleModelId(testdataFactory.createVehicle(notTarget).getId()).lastTargetQuery(overdueMix[i]).vin(notTarget)));
            String assignedTargets = ASSIGNED_STATUS + testdataFactory.getRandomInt();
            targAssigned.add(targetManagement.create(
                    entityFactory.target().create().controllerId(assignedTargets)
                            .name(assignedTargets).serialNumber(assignedTargets).vehicleModelId(testdataFactory.createVehicle(assignedTargets).getId()).vin(assignedTargets).lastTargetQuery(overdueMix[i])));
            String installedTargets = INSTALLED_STATUS + testdataFactory.getRandomInt();
            targInstalled.add(targetManagement.create(
                    entityFactory.target().create().controllerId(installedTargets)
                            .name(installedTargets).serialNumber(installedTargets).vehicleModelId(testdataFactory.createVehicle(installedTargets).getId()).vin(installedTargets).lastTargetQuery(overdueMix[i])));
        }

        final DistributionSet ds = testdataFactory.createDistributionSet("a");

        targAssigned = assignDistributionSet(ds, targAssigned).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = assignDistributionSet(ds, targInstalled).getAssignedEntity().stream().map(Action::getTarget)
                .collect(Collectors.toList());
        targInstalled = testdataFactory
                .sendUpdateActionStatusToTargets(targInstalled, DeviceActionStatus.FINISHED_NOT_EXECUTED, Collections.singletonList(INSTALLED_STATUS))
                .stream().map(Action::getTarget).collect(Collectors.toList());

        final Slice<Target> result = targetManagement.findByFilterOrderByLinkedDistributionSet(PAGE, ds.getId(),
                new FilterParams(null, Boolean.TRUE, null, null, Boolean.FALSE));

        final Comparator<TenantAwareBaseEntity> byId = (e1, e2) -> Long.compare(e1.getId(), e2.getId());

        assertThat(result.getNumberOfElements()).isEqualTo(9);
        final List<Target> expected = new ArrayList<>();
        expected.addAll(targInstalled.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));
        expected.addAll(targAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));
        expected.addAll(notAssigned.stream().sorted(byId)
                .filter(item -> lastTargetQueryAlwaysOverdue.equals(item.getLastTargetQuery()))
                .collect(Collectors.toList()));

        assertThat(result.getContent()).usingElementComparator(controllerIdComparator())
                .containsExactly(expected.toArray(new Target[0]));

    }

    @Test
    @Description("Verfies that targets with given assigned DS are returned from repository.")
    void findTargetByAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        testdataFactory.createTargets(10, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
        List<Target> assignedtargets = testdataFactory.createTargets(10, NAME + testdataFactory.getRandomInt(), DESCRIPTION);

        assignDistributionSet(assignedSet, assignedtargets);

        // get final updated version of targets
        assignedtargets = targetManagement
                .getByControllerID(assignedtargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(targetManagement.findByAssignedDistributionSet(PAGE, assignedSet.getId()))
                .containsAll(assignedtargets).hasSize(10);

    }

    @Test
    @Description("Verifies that targets without given assigned DS are returned from repository.")
    void findTargetWithoutAssignedDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_FILTER_WILD_QUERY));
        final List<Target> unassignedTargets = testdataFactory.createTargets(12, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
        final List<Target> assignedTargets = testdataFactory.createTargets(10, NAME + testdataFactory.getRandomInt(), DESCRIPTION);

        assignDistributionSet(assignedSet, assignedTargets);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatible(PAGE, assignedSet.getId(), tfq.getQuery()).getContent();
        assertThat(result).hasSize(unassignedTargets.size())
                .containsAll(unassignedTargets);

    }

    @Test
    @Description("Verifies that targets with given installed DS are returned from repository.")
    void findTargetByInstalledDistributionSet() {
        final DistributionSet assignedSet = testdataFactory.createDistributionSet("");
        final DistributionSet installedSet = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());
        testdataFactory.createTargets(10, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
        List<Target> installedtargets = testdataFactory.createTargets(10, NAME + testdataFactory.getRandomInt(), DESCRIPTION);

        // set on installed and assign another one
        assignDistributionSet(installedSet, installedtargets).getAssignedEntity().forEach(action -> controllerManagement
                .addUpdateActionStatus(entityFactory.actionStatus().create(action.getId()).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null));
        assignDistributionSet(assignedSet, installedtargets);

        // get final updated version of targets
        installedtargets = targetManagement
                .getByControllerID(installedtargets.stream().map(Target::getControllerId).collect(Collectors.toList()));

        assertThat(targetManagement.findByInstalledDistributionSet(PAGE, installedSet.getId()))
                .containsAll(installedtargets).hasSize(10);

    }

    @Test
    @Description("Verifies that all compatible targets are returned from repository.")
    void shouldFindAllTargetsCompatibleWithDS() {
        final DistributionSet testDs = testdataFactory.createDistributionSet();
        final TargetType targetType = testdataFactory.createTargetType(NAME + testdataFactory.getRandomInt(),
                Collections.singletonList(testDs.getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_FILTER_WILD_QUERY));
        final List<Target> targets = testdataFactory.createTargets(20, NAME + testdataFactory.getRandomInt());
        final List<Target> targetWithCompatibleTypes = testdataFactory.createTargetsWithType(20, NAME + testdataFactory.getRandomInt(),
                targetType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatible(PAGE, testDs.getId(), tfq.getQuery()).getContent();

        assertThat(result).hasSize(targets.size() + targetWithCompatibleTypes.size())
                .containsAll(targetWithCompatibleTypes).containsAll(targets);
    }

    @Test
    @Description("Verifies that incompatible targets are not returned from repository.")
    void shouldNotFindTargetsIncompatibleWithDS() {
        final DistributionSetType dsType = testdataFactory.findOrCreateDistributionSetType(NAME + testdataFactory.getRandomInt(),
                NAME + testdataFactory.getRandomInt());
        final DistributionSet testDs = createDistSetWithType(dsType);
        final TargetType compatibleTargetType = testdataFactory.createTargetType(NAME + testdataFactory.getRandomInt(),
                Collections.singletonList(dsType));
        final TargetType incompatibleTargetType = testdataFactory.createTargetType(NAME + testdataFactory.getRandomInt(),
                Collections.singletonList(testdataFactory.createDistributionSet().getType()));
        final TargetFilterQuery tfq = targetFilterQueryManagement
                .create(entityFactory.targetFilterQuery().create().name(NAME + testdataFactory.getRandomInt()).query(NAME_FILTER_WILD_QUERY));

        final List<Target> targetsWithOutType = testdataFactory.createTargets(20, NAME + testdataFactory.getRandomInt());
        final List<Target> targetsWithCompatibleType = testdataFactory.createTargetsWithType(20, NAME + testdataFactory.getRandomInt(),
                compatibleTargetType);
        final List<Target> targetsWithIncompatibleType = testdataFactory.createTargetsWithType(20, NAME + testdataFactory.getRandomInt(),
                incompatibleTargetType);

        final List<Target> testTargets = new ArrayList<>();
        testTargets.addAll(targetsWithOutType);
        testTargets.addAll(targetsWithCompatibleType);

        final List<Target> result = targetManagement
                .findByTargetFilterQueryAndNonDSAndCompatible(PAGE, testDs.getId(), tfq.getQuery()).getContent();

        assertThat(result).hasSize(testTargets.size()).containsExactlyInAnyOrderElementsOf(testTargets)
                .doesNotContainAnyElementsOf(targetsWithIncompatibleType);
    }

    private DistributionSet createDistSetWithType(final DistributionSetType type) {
        final DistributionSetCreate dsCreate = entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt()).version("1.0")
                .type(type);
        return distributionSetManagement.create(dsCreate);
    }

    @Test
    @Description("Verifies that targets with given target type are returned from repository.")
    public void findTargetByTargetType() {
        final TargetType testType = testdataFactory.createTargetType(NAME + testdataFactory.getRandomInt(),
                Collections.singletonList(standardDsType));
        final List<Target> unassigned = testdataFactory.createTargets(9, NAME + testdataFactory.getRandomInt());
        final List<Target> assigned = testdataFactory.createTargetsWithType(11, NAME + testdataFactory.getRandomInt(), testType);

        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, false, testType.getId())))
                .containsAll(assigned).hasSize(11);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, false, testType.getId())))
                .isEqualTo(11);

        assertThat(targetManagement.findByFilters(PAGE, new FilterParams(null, null, true, null)))
                .containsAll(unassigned).hasSize(9);
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, true, null)))
                .isEqualTo(9);

    }

}
