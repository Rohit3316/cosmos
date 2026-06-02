/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Iterables;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.FilterParams;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.builder.TargetUpdate;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ActionUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTagCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Tag;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.fail;


@Feature("Component Tests - Repository")
@Story("Target Management")
class TargetManagementTest extends AbstractJpaIntegrationTest {

    private static final String TARGET_FILTER_WILD_QUERY = "name==*";
    private static final String KEY_FILTER_WILD_QUERY = "key==*";
    private static final String CONTROLLER_FILTER_WILD_QUERY = "controllerId==";
    private static final String ASSIGNED_TARGET_ERROR_MSG = "Assigned targets are wrong";
    private static final String COUNT_TARGET_ERROR_MSG = "Target count is wrong";
    private static final String TAG_ERROR_MSG = "Tag size is wrong";
    private static final String TOTAL_TARGETS = "Total targets";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private static final String TESTVIN="testvin";
    private static final String VIN = "19UYA31581L010500";
    private static final String SERIAL_NUMBER = "serialNumber";
    private static final String CONTROLLERID = "controllerId";
    private static final String WHITESPACE_ERROR = "target with whitespaces in controller id should not be created";
    private static final String VALUE_1_FILTER_QUERY = "-value1";
    private static final Logger LOG= LoggerFactory.getLogger(TargetManagementTest.class);
    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1)})
    void nonExistingEntityAccessReturnsNotPresent() {
        final Target target = testdataFactory.createTarget();
        assertThat(targetManagement.getByControllerID(NOT_EXIST_ID)).isNotPresent();
        assertThat(targetManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(targetManagement.getMetaDataByControllerId(target.getControllerId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specified on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetTagCreatedEvent.class, count = 1)})
    void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final TargetTag tag = targetTagManagement.create(entityFactory.tag().create().name("A"));
        final Target target = testdataFactory.createTarget();

        verifyThrownExceptionBy(
                () -> targetManagement.assignTag(Collections.singletonList(target.getControllerId()), NOT_EXIST_IDL),
                TargetTag.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.assignTag(Collections.singletonList(NOT_EXIST_ID), tag.getId()),
                Target.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.findByTag(PAGE, NOT_EXIST_IDL), TargetTag.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.findByRsqlAndTag(PAGE, TARGET_FILTER_WILD_QUERY, NOT_EXIST_IDL), TargetTag.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.countByAssignedDistributionSet(NOT_EXIST_IDL),
                DistributionSet.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.countByInstalledDistributionSet(NOT_EXIST_IDL),
                DistributionSet.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.existsByInstalledOrAssignedDistributionSet(NOT_EXIST_IDL),
                DistributionSet.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.countByTargetFilterQuery(NOT_EXIST_IDL), TargetFilterQuery.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.countByRsqlAndNonDSAndCompatible(NOT_EXIST_IDL, TARGET_FILTER_WILD_QUERY),
                DistributionSet.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.deleteByControllerID(NOT_EXIST_ID), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.delete(Collections.singletonList(NOT_EXIST_IDL)), Target.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> targetManagement.findByTargetFilterQueryAndNonDSAndCompatible(PAGE, NOT_EXIST_IDL, TARGET_FILTER_WILD_QUERY),
                DistributionSet.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.findByInRolloutGroupWithoutAction(PAGE, NOT_EXIST_IDL),
                RolloutGroup.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.findByAssignedDistributionSet(PAGE, NOT_EXIST_IDL),
                DistributionSet.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> targetManagement.findByAssignedDistributionSetAndRsql(PAGE, NOT_EXIST_IDL, TARGET_FILTER_WILD_QUERY),
                DistributionSet.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.findByInstalledDistributionSet(PAGE, NOT_EXIST_IDL),
                DistributionSet.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> targetManagement.findByInstalledDistributionSetAndRsql(PAGE, NOT_EXIST_IDL, TARGET_FILTER_WILD_QUERY),
                DistributionSet.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement
                .toggleTagAssignment(Collections.singletonList(target.getControllerId()), NOT_EXIST_ID), TargetTag.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> targetManagement.toggleTagAssignment(Collections.singletonList(NOT_EXIST_ID), tag.getName()),
                Target.class.getSimpleName());

        verifyThrownExceptionBy(() -> targetManagement.unAssignTag(NOT_EXIST_ID, tag.getId()), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.unAssignTag(target.getControllerId(), NOT_EXIST_IDL),
                TargetTag.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.update(entityFactory.target().update(NOT_EXIST_ID)), Target.class.getSimpleName());

        final String targetMeta = NAME + testdataFactory.getRandomInt();
        final String targetKey = NAME + testdataFactory.getRandomInt();
        verifyThrownExceptionBy(() -> targetManagement.createMetaData(NOT_EXIST_ID,
                Collections.singletonList(entityFactory.generateTargetMetadata(targetMeta, targetMeta))), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.deleteMetaData(NOT_EXIST_ID, targetKey), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.deleteMetaData(target.getControllerId(), NOT_EXIST_ID),
                TargetMetadata.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.getMetaDataByControllerId(NOT_EXIST_ID, targetKey), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.findMetaDataByControllerId(PAGE, NOT_EXIST_ID), Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.findMetaDataByControllerIdAndRsql(PAGE, NOT_EXIST_ID, KEY_FILTER_WILD_QUERY),
                Target.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> targetManagement.updateMetadata(NOT_EXIST_ID, entityFactory.generateTargetMetadata(targetKey, targetKey)),
                Target.class.getSimpleName());
        verifyThrownExceptionBy(() -> targetManagement.updateMetadata(target.getControllerId(),
                entityFactory.generateTargetMetadata(NOT_EXIST_ID, targetKey)), TargetMetadata.class.getSimpleName());
    }

    @Test
    @Description("Ensures that retrieving the target security is only permitted with the necessary permissions.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1)})
    void getTargetSecurityTokenOnlyWithCorrectPermission() throws Exception {
        final String securityToken = NAME + testdataFactory.getRandomInt();
        final Target createdTarget = targetManagement
                .create(entityFactory.target().create().controllerId("targetWithSecurityToken")
                        .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt())
                        .securityToken(securityToken).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN));

        // retrieve security token only with READ_TARGET_SEC_TOKEN permission
        final String securityTokenWithReadPermission = WithSpringAuthorityRule.runAs(
                WithSpringAuthorityRule.withUser("OnlyTargetReadPermission", false, SpPermission.READ_TARGET_SEC_TOKEN),
                createdTarget::getSecurityToken);

        // retrieve security token as system code execution
        final String securityTokenAsSystemCode = systemSecurityContext.runAsSystem(createdTarget::getSecurityToken);

        // retrieve security token without any permissions
        final String securityTokenWithoutPermission = WithSpringAuthorityRule
                .runAs(WithSpringAuthorityRule.withUser("NoPermission", false), createdTarget::getSecurityToken);

        assertThat(createdTarget.getSecurityToken()).isEqualTo(securityToken);
        assertThat(securityTokenWithReadPermission).isNotNull();
        assertThat(securityTokenAsSystemCode).isNotNull();

        assertThat(securityTokenWithoutPermission).isNull();
    }

    @Test
    @Description("Ensures that targets cannot be created e.g. in plug'n play scenarios when tenant does not exists.")
    @WithUser(tenantId = "tenantWhichDoesNotExists", allSpPermissions = true, autoCreateTenant = false)
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class)})
    void createTargetForTenantWhichDoesNotExistThrowsTenantNotExistException() {
        Assertions.assertThrows(TenantNotExistException.class, () -> {
            targetManagement.create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID))
                    .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()));
            fail("should not be possible as the serialNumber and Name does not exist");
        });
    }

    @Test
    @Description("Verify that a target with same controller ID than another device cannot be created.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1)})
    void createTargetThatViolatesUniqueConstraintFails() {
        final String vin = VIN + testdataFactory.getRandomInt();
        final String name = NAME + testdataFactory.getRandomInt();
        final String serialNumber = NAME + testdataFactory.getRandomInt();
        final String vehicleModelName = NAME + testdataFactory.getRandomInt();
        targetManagement.create(entityFactory.target().create().controllerId(vin).name(name)
                .serialNumber(serialNumber).vehicleModelId(testdataFactory.createVehicle(vehicleModelName).getId()).vin(VIN));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(vin)
                        .name(name).serialNumber(serialNumber).vehicleModelId(testdataFactory.createVehicle(vehicleModelName).getId())));
    }

    @Test
    @Description("Verify that a target with with invalid properties cannot be created or updated")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class)})
    void createAndUpdateTargetWithInvalidFields() {
        final Target target = testdataFactory.createTarget();

        createTargetWithInvalidControllerId();
        createAndUpdateTargetWithInvalidDescription(target);
        createAndUpdateTargetWithInvalidName(target);
        createAndUpdateTargetWithInvalidSecurityToken(target);
    }

    @Step
    private void createAndUpdateTargetWithInvalidDescription(final Target target) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long description should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID))
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid description should not be created").isThrownBy(() -> targetManagement
                        .create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID)).description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long description should not be updated")
                .isThrownBy(() -> targetManagement.update(entityFactory.target().update(target.getControllerId())
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid description should not be updated").isThrownBy(() -> targetManagement.update(
                        entityFactory.target().update(target.getControllerId()).description(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateTargetWithInvalidName(final Target target) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long name should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID))
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid name should not be created").isThrownBy(() -> targetManagement
                        .create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID)).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long name should not be updated")
                .isThrownBy(() -> targetManagement.update(entityFactory.target().update(target.getControllerId())
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid name should not be updated").isThrownBy(() -> targetManagement
                        .update(entityFactory.target().update(target.getControllerId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too short name should not be updated").isThrownBy(() -> targetManagement
                        .update(entityFactory.target().update(target.getControllerId()).name("")));

    }

    @Step
    private void createAndUpdateTargetWithInvalidSecurityToken(final Target target) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long token should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID))
                        .securityToken(RandomStringUtils.randomAlphanumeric(Target.SECURITY_TOKEN_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid token should not be created").isThrownBy(() -> targetManagement
                        .create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID)).securityToken(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long token should not be updated")
                .isThrownBy(() -> targetManagement.update(entityFactory.target().update(target.getControllerId())
                        .securityToken(RandomStringUtils.randomAlphanumeric(Target.SECURITY_TOKEN_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid token should not be updated").isThrownBy(() -> targetManagement.update(
                        entityFactory.target().update(target.getControllerId()).securityToken(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too short token should not be updated").isThrownBy(() -> targetManagement
                        .update(entityFactory.target().update(target.getControllerId()).securityToken("")));
    }
    @Step
    private void createTargetWithInvalidControllerId() {
        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with empty controller id should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId("")));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with null controller id should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(null)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with too long controller id should not be created")
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create()
                        .controllerId(RandomStringUtils.randomAlphanumeric(Target.CONTROLLER_ID_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("target with invalid controller id should not be created").isThrownBy(
                        () -> targetManagement.create(entityFactory.target().create().controllerId(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId(" ")));

        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId("a b")));

        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId("     ")));

        assertThatExceptionOfType(ConstraintViolationException.class).as(WHITESPACE_ERROR)
                .isThrownBy(() -> targetManagement.create(entityFactory.target().create().controllerId("aaa   bbb")));

    }

    @Test
    @Description("Ensures that targets can assigned and unassigned to a target tag. Not exists target will be ignored for the assignment.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 4),
            @Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 5)})
    void assignAndUnassignTargetsToTag() {
        final List<String> assignTarget = new ArrayList<>();
        Map<String, String> randomValues = generateRandomValues();
        final String targetId = randomValues.get(CONTROLLERID);
        assignTarget.add(
                targetManagement.create(entityFactory.target().create().controllerId(targetId)
                        .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN)).getControllerId());
        assignTarget.add(targetManagement.create(entityFactory.target().create().controllerId(targetId + "4")
                .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN)).getControllerId());
        assignTarget.add(targetManagement.create(entityFactory.target().create().controllerId(targetId + "5")
                .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN)).getControllerId());
        assignTarget.add(targetManagement.create(entityFactory.target().create().controllerId(targetId + "6")
                .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN)).getControllerId());

        final String tag = NAME + testdataFactory.getRandomInt();
        final TargetTag targetTag = targetTagManagement.create(entityFactory.tag().create().name(tag));

        final List<Target> assignedTargets = targetManagement.assignTag(assignTarget, targetTag.getId());
        assertThat(assignedTargets).as(ASSIGNED_TARGET_ERROR_MSG).hasSize(4);
        assignedTargets.forEach(target -> assertThat(
                targetTagManagement.findByTarget(PAGE, target.getControllerId()).getNumberOfElements()).isEqualTo(1));

        final TargetTag findTargetTag = targetTagManagement.getByName(tag).orElseThrow(IllegalStateException::new);
        assertThat(assignedTargets).as(ASSIGNED_TARGET_ERROR_MSG)
                .hasSize(targetManagement.findByTag(PAGE, targetTag.getId()).getNumberOfElements());

        final Target unAssignTarget = targetManagement.unAssignTag(targetId, findTargetTag.getId());
        assertThat(unAssignTarget.getControllerId()).as("Controller id is wrong").isEqualTo(targetId);
        assertThat(targetTagManagement.findByTarget(PAGE, unAssignTarget.getControllerId())).as(TAG_ERROR_MSG)
                .isEmpty();
        targetTagManagement.getByName(tag).orElseThrow(NoSuchElementException::new);
        assertThat(targetManagement.findByTag(PAGE, targetTag.getId())).as(ASSIGNED_TARGET_ERROR_MSG).hasSize(3);
        assertThat(targetManagement.findByRsqlAndTag(PAGE, CONTROLLER_FILTER_WILD_QUERY + targetId, targetTag.getId()))
                .as(ASSIGNED_TARGET_ERROR_MSG).isEmpty();
        assertThat(targetManagement.findByRsqlAndTag(PAGE, CONTROLLER_FILTER_WILD_QUERY + targetId + "4", targetTag.getId()))
                .as(ASSIGNED_TARGET_ERROR_MSG).hasSize(1);

    }

    @Test
    @Description("Ensures that targets can deleted e.g. test all cascades")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 12),
            @Expect(type = TargetDeletedEvent.class, count = 12), @Expect(type = TargetUpdatedEvent.class, count = 6)})
    void deleteAndCreateTargets() {
        Target target = targetManagement.create(entityFactory.target().create().controllerId(generateRandomValues().get(CONTROLLERID))
                .name(NAME + testdataFactory.getRandomInt()).serialNumber(NAME + testdataFactory.getRandomInt()).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN));
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isEqualTo(1);
        targetManagement.delete(Collections.singletonList(target.getId()));
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isZero();

        Map<String, String> randomValues = generateRandomValues();
        target = createTargetWithAttributes(randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), VIN);
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isEqualTo(1);
        assertThat(targetManagement.existsByControllerId(randomValues.get(CONTROLLERID))).isTrue();
        targetManagement.delete(Collections.singletonList(target.getId()));
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isZero();
        assertThat(targetManagement.existsByControllerId(randomValues.get(CONTROLLERID))).isFalse();

        final List<Long> targets = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            target = targetManagement.create(entityFactory.target().create().controllerId("" + i).name("" + i)
                    .serialNumber("" + i).vehicleModelId(testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId()).vin(VIN));
            targets.add(target.getId());
            targets.add(createTargetWithAttributes("" + (i * i + 1000), randomValues.get(NAME) + (i * i + 1000), randomValues.get(SERIAL_NUMBER) + (i * i + 1000), generateRandomValues().get(TESTVIN) + (i * i + 1000)).getId());
        }
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isEqualTo(10);
        targetManagement.delete(targets);
        assertThat(targetManagement.count()).as(COUNT_TARGET_ERROR_MSG).isZero();
    }

    private Target createTargetWithAttributes(final String controllerId, String name, String serialNumber, String vin) {
        final Map<String, String> testData = new HashMap<>();
        testData.put("test1", "testdata1");

        targetManagement.create(entityFactory.target().create().controllerId(controllerId).name(name).serialNumber(serialNumber).vehicleModelId(testdataFactory.createVehicle(name).getId()).vin(vin));
        final Target target = controllerManagement.updateControllerAttributesWithSoftware(controllerId, testData, null, null);

        assertThat(targetManagement.getControllerAttributes(controllerId)).as("Controller Attributes are wrong")
                .isEqualTo(testData);
        return target;
    }

    @Test
    @Description("Finds a target by given ID and checks if all data is in the response (including the data defined as lazy).")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 6)})
    void findTargetByControllerIDWithDetails() {
        final String newlyDsAssignErrorMsg = "For newly created distributions sets the assigned target count should be zero";
        final String newlyDsInstallErrorMsg = "For newly created distributions sets the installed target count should be zero";
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final DistributionSet testDs1 = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());
        final DistributionSet testDs2 = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());

        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId()))
                .as(newlyDsAssignErrorMsg).isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId()))
                .as(newlyDsInstallErrorMsg).isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as("Exists assigned or installed query should return false for new distribution sets").isFalse();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId()))
                .as(newlyDsAssignErrorMsg).isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId()))
                .as(newlyDsInstallErrorMsg).isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as(newlyDsAssignErrorMsg).isFalse();

        final long current = Instant.now().getEpochSecond();
        Map<String, String> randomValues = generateRandomValues();
        Target target = createTargetWithAttributes(randomValues.get(CONTROLLERID),randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), VIN);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), 1L);

        final DistributionSetAssignmentResult result = assignDistributionSet(testDs1.getId(), randomValues.get(CONTROLLERID));

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(getFirstAssignedActionId(result)).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assignDistributionSet(testDs2.getId(), randomValues.get(CONTROLLERID));

        target = targetManagement.getByControllerID(randomValues.get(CONTROLLERID)).orElseThrow(IllegalStateException::new);
        // read data
        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isEqualTo(1);
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as(COUNT_TARGET_ERROR_MSG).isTrue();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isEqualTo(1);
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as(COUNT_TARGET_ERROR_MSG).isTrue();
        assertThat(target.getLastTargetQuery()).as("Target query is not work").isGreaterThanOrEqualTo(current);

        final DistributionSet assignedDs = deploymentManagement.getAssignedDistributionSet(randomValues.get(CONTROLLERID))
                .orElseThrow(NoSuchElementException::new);
        assertThat(assignedDs).as("Assigned ds size is wrong").isEqualTo(testDs2);

        final DistributionSet installedDs = deploymentManagement.getInstalledDistributionSet(randomValues.get(CONTROLLERID))
                .orElseThrow(NoSuchElementException::new);
        assertThat(installedDs).as("Installed ds is wrong").isEqualTo(testDs1);
    }

    @Test
    @Description("Finds a target by given vin and checks if all data is in the response (including the data defined as lazy).")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 2),
            @Expect(type = TargetCreatedEvent.class, count = 1), @Expect(type = TargetUpdatedEvent.class, count = 5),
            @Expect(type = ActionCreatedEvent.class, count = 2), @Expect(type = ActionUpdatedEvent.class, count = 1),
            @Expect(type = TargetAssignDistributionSetEvent.class, count = 2),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 6),
            @Expect(type = TargetAttributesRequestedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1),
            @Expect(type = ArtifactsCreatedEvent.class, count = 6)})
    void givenTargetExistsWhenFindingByVinThenReturnsDetails() {
        final String newlyDsAssignErrorMsg = "For newly created distributions sets the assigned target count should be zero";
        final String newlyDsInstallErrorMsg = "For newly created distributions sets the installed target count should be zero";
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final DistributionSet testDs1 = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());
        final DistributionSet testDs2 = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt());

        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId()))
                .as(newlyDsAssignErrorMsg).isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId()))
                .as(newlyDsInstallErrorMsg).isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as("Exists assigned or installed query should return false for new distribution sets").isFalse();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId()))
                .as(newlyDsAssignErrorMsg).isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId()))
                .as(newlyDsInstallErrorMsg).isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as(newlyDsAssignErrorMsg).isFalse();

        final long current = Instant.now().getEpochSecond();
        Map<String, String> randomValues = generateRandomValues();
        Target target = createTargetWithAttributes(randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), VIN);
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), 1L);

        final DistributionSetAssignmentResult result = assignDistributionSet(testDs1.getId(), randomValues.get(CONTROLLERID));

        controllerManagement.addUpdateActionStatus(
                entityFactory.actionStatus().create(getFirstAssignedActionId(result)).status(DeviceActionStatus.FINISHED_NOT_EXECUTED), null);
        assignDistributionSet(testDs2.getId(), randomValues.get(CONTROLLERID));

        target = targetManagement.getByVin(VIN).orElseThrow(IllegalStateException::new);
        // read data
        assertThat(targetManagement.countByAssignedDistributionSet(testDs1.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isZero();
        assertThat(targetManagement.countByInstalledDistributionSet(testDs1.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isEqualTo(1);
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs1.getId()))
                .as(COUNT_TARGET_ERROR_MSG).isTrue();
        assertThat(targetManagement.countByAssignedDistributionSet(testDs2.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isEqualTo(1);
        assertThat(targetManagement.countByInstalledDistributionSet(testDs2.getId())).as(COUNT_TARGET_ERROR_MSG)
                .isZero();
        assertThat(targetManagement.existsByInstalledOrAssignedDistributionSet(testDs2.getId()))
                .as(COUNT_TARGET_ERROR_MSG).isTrue();
        assertThat(target.getLastTargetQuery()).as("Target query is not work").isGreaterThanOrEqualTo(current);

        final DistributionSet assignedDs = deploymentManagement.getAssignedDistributionSet(randomValues.get(CONTROLLERID))
                .orElseThrow(NoSuchElementException::new);
        assertThat(assignedDs).as("Assigned ds size is wrong").isEqualTo(testDs2);

        final DistributionSet installedDs = deploymentManagement.getInstalledDistributionSet(randomValues.get(CONTROLLERID))
                .orElseThrow(NoSuchElementException::new);
        assertThat(installedDs).as("Installed ds is wrong").isEqualTo(testDs1);
    }


    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if the targets with the same controller ID are created twice.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 5)})
    void createMultipleTargetsDuplicate() {

        Map<String, String> randomValues = generateRandomValues();
        testdataFactory.createTargets(5, randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), DESCRIPTION);
        try {
            testdataFactory.createTargets(5, randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), DESCRIPTION);
            fail("Targets already exists");
        } catch (final EntityAlreadyExistsException e) {
            LOG.error("Entity already exists", e);
        }
    }

    @Test
    @Description("Checks if the EntityAlreadyExistsException is thrown if a single target with the same controller ID are created twice.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1)})
    void createTargetDuplicate() {
        Map<String, String> randomValues = generateRandomValues();
        Long vehicleModelId = testdataFactory.createVehicle(randomValues.get(NAME)).getId();
        targetManagement.create(entityFactory.target().create().controllerId(randomValues.get(CONTROLLERID)).name(randomValues.get(NAME)).serialNumber(randomValues.get(SERIAL_NUMBER)).vehicleModelId(vehicleModelId).vin(VIN));
        try {
            targetManagement.create(entityFactory.target().create().controllerId(randomValues.get(CONTROLLERID)).name(randomValues.get(NAME)).serialNumber(randomValues.get(SERIAL_NUMBER)).vehicleModelId(vehicleModelId).vin(VIN));
            fail("Target already exists");
        } catch (final EntityAlreadyExistsException e) {
            LOG.error("Entity already exists", e);
        }
    }

    /**
     * verifies, that all {@link TargetTag} of parameter. NOTE: it's accepted
     * that the target have additional tags assigned to them which are not
     * contained within parameter tags.
     *
     * @param strict  if true, the given targets MUST contain EXACTLY ALL given
     *                tags, AND NO OTHERS. If false, the given targets MUST contain
     *                ALL given tags, BUT MAY CONTAIN FURTHER ONE
     * @param targets targets to be verified
     * @param tags    are contained within tags of all targets.
     */
    private void checkTargetHasTags(final boolean strict, final Iterable<Target> targets, final TargetTag... tags) {
        _target:
        for (final Target tl : targets) {
            for (final Tag tt : targetTagManagement.findByTarget(PAGE, tl.getControllerId())) {
                for (final Tag tag : tags) {
                    if (tag.getName().equals(tt.getName())) {
                        continue _target;
                    }
                }
                if (strict) {
                    fail("Target does not contain all tags");
                }
            }
            fail("Target does not contain any tags or the expected tag was not found");
        }
    }

    private void checkTargetHasNotTags(final Iterable<Target> targets, final TargetTag... tags) {
        for (final Target tl : targets) {
            targetManagement.getByControllerID(tl.getControllerId()).get();

            for (final Tag tag : tags) {
                for (final Tag tt : targetTagManagement.findByTarget(PAGE, tl.getControllerId())) {
                    if (tag.getName().equals(tt.getName())) {
                        fail("Target should have no tags");
                    }
                }
            }
        }
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Creates and updates a target and verifies the changes in the repository.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetUpdatedEvent.class, count = 1)})
    void singleTargetIsInsertedIntoRepo() throws Exception {

        final String myCtrlID = generateRandomValues().get(CONTROLLERID);

        Target savedTarget = testdataFactory.createTarget(myCtrlID);
        assertThat(savedTarget).as("The target should not be null").isNotNull();
        final long createdAt = savedTarget.getCreatedAt();
        long modifiedAt = savedTarget.getLastModifiedAt();

        assertThat(createdAt).as("CreatedAt compared with modifiedAt").isEqualTo(modifiedAt);

        Awaitility.await().until(() -> Instant.now().getEpochSecond() > createdAt + 1 );

        savedTarget = targetManagement.update(
                entityFactory.target().update(savedTarget.getControllerId()).description(DESCRIPTION));
        assertThat(createdAt).as("CreatedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        assertThat(modifiedAt).as("ModifiedAt compared with saved modifiedAt")
                .isNotEqualTo(savedTarget.getLastModifiedAt());
        modifiedAt = savedTarget.getLastModifiedAt();

        final Target foundTarget = targetManagement.getByControllerID(savedTarget.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(foundTarget).as("The target should not be null").isNotNull();
        assertThat(myCtrlID).as("ControllerId compared with saved controllerId")
                .isEqualTo(foundTarget.getControllerId());
        assertThat(savedTarget).as("Target compared with saved target").isEqualTo(foundTarget);
        assertThat(createdAt).as("CreatedAt compared with saved createdAt").isEqualTo(foundTarget.getCreatedAt());
        assertThat(modifiedAt).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(foundTarget.getLastModifiedAt());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Create multiple targets as bulk operation and delete them in bulk.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 101),
            @Expect(type = TargetUpdatedEvent.class, count = 100),
            @Expect(type = TargetDeletedEvent.class, count = 51)})
    void bulkTargetCreationAndDelete() throws InterruptedException {
        Map<String, String> randomValues = generateRandomValues();
        List<Target> firstList = testdataFactory.createTargets(100, randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), DESCRIPTION);

        final Target extra = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID));

        final Iterable<JpaTarget> allFound = targetRepository.findAll();

        assertThat(Long.valueOf(firstList.size())).as("List size of targets")
                .isEqualTo(firstList.spliterator().getExactSizeIfKnown());
        assertThat(Long.valueOf(firstList.size() + 1)).as("LastModifiedAt compared with saved lastModifiedAt")
                .isEqualTo(allFound.spliterator().getExactSizeIfKnown());

        TestdataFactory.waitForSeconds(2);
        // change the objects and save to again to trigger a change on
        // lastModifiedAt
        firstList = firstList.stream()
                .map(t -> targetManagement.update(
                        entityFactory.target().update(t.getControllerId()).name(t.getName().concat("\t" + DESCRIPTION))))
                .collect(Collectors.toList());

        // verify that all entries are found
        _founds:
        for (final Target foundTarget : allFound) {
            for (final Target changedTarget : firstList) {
                if (changedTarget.getControllerId().equals(foundTarget.getControllerId())) {
                    assertThat(changedTarget.getDescription())
                            .as("Description of changed target compared with description saved target")
                            .isEqualTo(foundTarget.getDescription());
                    assertThat(changedTarget.getName()).as("Name of changed target starts with name of saved target")
                            .startsWith(foundTarget.getName());
                    assertThat(changedTarget.getName()).as("Name of changed target ends with 'changed'")
                            .endsWith(DESCRIPTION);
                    assertThat(changedTarget.getCreatedAt()).as("CreatedAt compared with saved createdAt")
                            .isEqualTo(foundTarget.getCreatedAt());
                    assertThat(changedTarget.getLastModifiedAt()).as("LastModifiedAt compared with saved createdAt")
                            .isNotEqualTo(changedTarget.getCreatedAt());
                    continue _founds;
                }
            }

            if (!foundTarget.getControllerId().equals(extra.getControllerId())) {
                fail("The controllerId of the found target is not equal to the controllerId of the saved target");
            }
        }

        targetManagement.deleteByControllerID(extra.getControllerId());

        final int numberToDelete = 50;
        final Collection<Target> targetsToDelete = firstList.subList(0, numberToDelete);
        final Target[] deletedTargets = Iterables.toArray(targetsToDelete, Target.class);
        final List<Long> targetsIdsToDelete = targetsToDelete.stream().map(Target::getId).collect(Collectors.toList());

        targetManagement.delete(targetsIdsToDelete);

        final List<Target> targetsLeft = targetManagement.findAll(PageRequest.of(0, 200)).getContent();
        assertThat(firstList.spliterator().getExactSizeIfKnown() - numberToDelete).as("Size of split list")
                .isEqualTo(targetsLeft.spliterator().getExactSizeIfKnown());

        assertThat(targetsLeft).as("Not all undeleted found").doesNotContain(deletedTargets);
    }

    @Test
    @Description("Tests the assignment of tags to the a single target.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 2),
            @Expect(type = TargetTagCreatedEvent.class, count = 7),
            @Expect(type = TargetUpdatedEvent.class, count = 7)})
    void targetTagAssignment() {
        final Target t1 = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID));
        final int noT2Tags = 4;
        final int noT1Tags = 3;
        final List<TargetTag> t1Tags = testdataFactory.createTargetTags(noT1Tags, NAME + testdataFactory.getRandomInt());

        t1Tags.forEach(tag -> targetManagement.assignTag(Collections.singletonList(t1.getControllerId()), tag.getId()));

        final String controllerId = generateRandomValues().get(CONTROLLERID);
        final Target t2 = testdataFactory.createTarget(controllerId, controllerId, controllerId, testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        final List<TargetTag> t2Tags = testdataFactory.createTargetTags(noT2Tags, NAME + testdataFactory.getRandomInt());
        t2Tags.forEach(tag -> targetManagement.assignTag(Collections.singletonList(t2.getControllerId()), tag.getId()));

        final Target t11 = targetManagement.getByControllerID(t1.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(targetTagManagement.findByTarget(PAGE, t11.getControllerId()).getContent()).as(TAG_ERROR_MSG)
                .hasSize(noT1Tags).containsAll(t1Tags);
        assertThat(targetTagManagement.findByTarget(PAGE, t11.getControllerId()).getContent()).as(TAG_ERROR_MSG)
                .hasSize(noT1Tags).doesNotContain(Iterables.toArray(t2Tags, TargetTag.class));

        final Target t21 = targetManagement.getByControllerID(t2.getControllerId())
                .orElseThrow(IllegalStateException::new);
        assertThat(targetTagManagement.findByTarget(PAGE, t21.getControllerId()).getContent()).as(TAG_ERROR_MSG)
                .hasSize(noT2Tags).containsAll(t2Tags);
        assertThat(targetTagManagement.findByTarget(PAGE, t21.getControllerId()).getContent()).as(TAG_ERROR_MSG)
                .hasSize(noT2Tags).doesNotContain(Iterables.toArray(t1Tags, TargetTag.class));
    }

    @Test
    @Description("Tests the assignment of tags to multiple targets.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetTagCreatedEvent.class, count = 4),
            @Expect(type = TargetUpdatedEvent.class, count = 80)})
    void targetTagBulkAssignments() {
        final String targetA = NAME + testdataFactory.getRandomInt();
        final String targetB = NAME + testdataFactory.getRandomInt();
        final String targetC = NAME + testdataFactory.getRandomInt();
        final List<Target> tagATargets = testdataFactory.createTargets(10, targetA, DESCRIPTION);
        final List<Target> tagBTargets = testdataFactory.createTargets(10, targetB, DESCRIPTION);
        final List<Target> tagCTargets = testdataFactory.createTargets(10, targetC, DESCRIPTION);

        final String targetAB = NAME + testdataFactory.getRandomInt();
        final List<Target> tagABTargets = testdataFactory.createTargets(10, targetAB, DESCRIPTION);
        final String targetABC = NAME + testdataFactory.getRandomInt();
        final List<Target> tagABCTargets = testdataFactory.createTargets(10, targetABC, DESCRIPTION);

        final TargetTag tagA = targetTagManagement.create(entityFactory.tag().create().name("A"));
        final TargetTag tagB = targetTagManagement.create(entityFactory.tag().create().name("B"));
        final TargetTag tagC = targetTagManagement.create(entityFactory.tag().create().name("C"));
        targetTagManagement.create(entityFactory.tag().create().name("X"));

        // doing different assignments
        toggleTagAssignment(tagATargets, tagA);
        toggleTagAssignment(tagBTargets, tagB);
        toggleTagAssignment(tagCTargets, tagC);

        toggleTagAssignment(tagABTargets, tagA);
        toggleTagAssignment(tagABTargets, tagB);

        toggleTagAssignment(tagABCTargets, tagA);
        toggleTagAssignment(tagABCTargets, tagB);
        toggleTagAssignment(tagABCTargets, tagC);

        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "X")))
                .as(COUNT_TARGET_ERROR_MSG).isZero();

        // search for targets with tag tagA
        final List<Target> targetWithTagA = new ArrayList<>();
        final List<Target> targetWithTagB = new ArrayList<>();
        final List<Target> targetWithTagC = new ArrayList<>();

        // storing target lists to enable easy evaluation
        Iterables.addAll(targetWithTagA, tagATargets);
        Iterables.addAll(targetWithTagA, tagABTargets);
        Iterables.addAll(targetWithTagA, tagABCTargets);

        Iterables.addAll(targetWithTagB, tagBTargets);
        Iterables.addAll(targetWithTagB, tagABTargets);
        Iterables.addAll(targetWithTagB, tagABCTargets);

        Iterables.addAll(targetWithTagC, tagCTargets);
        Iterables.addAll(targetWithTagC, tagABCTargets);

        // check the target lists as returned by assignTag
        checkTargetHasTags(false, targetWithTagA, tagA);
        checkTargetHasTags(false, targetWithTagB, tagB);
        checkTargetHasTags(false, targetWithTagC, tagC);

        checkTargetHasNotTags(tagATargets, tagB, tagC);
        checkTargetHasNotTags(tagBTargets, tagA, tagC);
        checkTargetHasNotTags(tagCTargets, tagA, tagB);

        // check again target lists refreshed from DB
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "A")))
                .as(COUNT_TARGET_ERROR_MSG).isEqualTo(targetWithTagA.size());
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "B")))
                .as(COUNT_TARGET_ERROR_MSG).isEqualTo(targetWithTagB.size());
        assertThat(targetManagement.countByFilters(new FilterParams(null, null, null, null, Boolean.FALSE, "C")))
                .as(COUNT_TARGET_ERROR_MSG).isEqualTo(targetWithTagC.size());
    }

    @Test
    @Description("Tests the unassigment of tags to multiple targets.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 3),
            @Expect(type = TargetCreatedEvent.class, count = 109),
            @Expect(type = TargetUpdatedEvent.class, count = 227)})
    void targetTagBulkUnassignments() {
        final TargetTag targTagA = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));
        final TargetTag targTagB = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));
        final TargetTag targTagC = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));


        Map<String, String> randomValues = generateRandomValues();

        final List<Target> targAs = testdataFactory.createTargets(25, randomValues.get(CONTROLLERID) + "-A", randomValues.get(NAME) + "-A", randomValues.get(SERIAL_NUMBER)  + "-A", DESCRIPTION);
        final List<Target> targBs = testdataFactory.createTargets(20, randomValues.get(CONTROLLERID) + "-B", randomValues.get(NAME)  + "-B", randomValues.get(SERIAL_NUMBER) + "-B", DESCRIPTION);
        final List<Target> targCs = testdataFactory.createTargets(15, randomValues.get(CONTROLLERID) + "-C", randomValues.get(NAME)  + "-C", randomValues.get(SERIAL_NUMBER) + "-C", DESCRIPTION);

        final List<Target> targABs = testdataFactory.createTargets(12, randomValues.get(CONTROLLERID) + "-AB", randomValues.get(NAME)  + "-AB", randomValues.get(SERIAL_NUMBER) + "-AB", DESCRIPTION);
        final List<Target> targACs = testdataFactory.createTargets(13, randomValues.get(CONTROLLERID) + "-AC", randomValues.get(NAME)  + "-AC", randomValues.get(SERIAL_NUMBER) + "-AC", DESCRIPTION);
        final List<Target> targBCs = testdataFactory.createTargets(7, randomValues.get(CONTROLLERID) + "-BC", randomValues.get(NAME)  + "-BC", randomValues.get(SERIAL_NUMBER) + "-BC", DESCRIPTION);
        final List<Target> targABCs = testdataFactory.createTargets(17, randomValues.get(CONTROLLERID) + "-ABC", randomValues.get(NAME)  + "-ABC", randomValues.get(SERIAL_NUMBER) + "-ABC", DESCRIPTION);

        toggleTagAssignment(targAs, targTagA);
        toggleTagAssignment(targABs, targTagA);
        toggleTagAssignment(targACs, targTagA);
        toggleTagAssignment(targABCs, targTagA);

        toggleTagAssignment(targBs, targTagB);
        toggleTagAssignment(targABs, targTagB);
        toggleTagAssignment(targBCs, targTagB);
        toggleTagAssignment(targABCs, targTagB);

        toggleTagAssignment(targCs, targTagC);
        toggleTagAssignment(targACs, targTagC);
        toggleTagAssignment(targBCs, targTagC);
        toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA);
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targACs, targTagA, targTagC);
        checkTargetHasTags(true, targBCs, targTagB, targTagC);
        checkTargetHasTags(true, targABCs, targTagA, targTagB, targTagC);

        toggleTagAssignment(targCs, targTagC);
        toggleTagAssignment(targACs, targTagC);
        toggleTagAssignment(targBCs, targTagC);
        toggleTagAssignment(targABCs, targTagC);

        checkTargetHasTags(true, targAs, targTagA); // 0
        checkTargetHasTags(true, targBs, targTagB);
        checkTargetHasTags(true, targABs, targTagA, targTagB);
        checkTargetHasTags(true, targBCs, targTagB);
        checkTargetHasTags(true, targACs, targTagA);

        checkTargetHasNotTags(targCs, targTagC);
        checkTargetHasNotTags(targACs, targTagC);
        checkTargetHasNotTags(targBCs, targTagC);
        checkTargetHasNotTags(targABCs, targTagC);
    }

    @Test
    @Description("Test that NO TAG functionality which gives all targets with no tag assigned.")
    @ExpectEvents({@Expect(type = TargetTagCreatedEvent.class, count = 1),
            @Expect(type = TargetCreatedEvent.class, count = 50),
            @Expect(type = TargetUpdatedEvent.class, count = 25)})
    void findTargetsWithNoTag() {
        Map<String, String> randomValues = generateRandomValues();

        final TargetTag targTagA = targetTagManagement.create(entityFactory.tag().create().name(NAME + testdataFactory.getRandomInt()));

        final List<Target> targAs = testdataFactory.createTargets(25, randomValues.get("targetId"), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), DESCRIPTION);
        toggleTagAssignment(targAs, targTagA);

        final String vin2 = VIN + testdataFactory.getRandomInt();
        final String serialNumber2 = SERIAL_NUMBER + testdataFactory.getRandomInt();
        final String name2 = NAME + testdataFactory.getRandomInt();
        final String targetId2 = vin2 + "_" + serialNumber2;

        testdataFactory.createTargets(25, targetId2, name2, serialNumber2, DESCRIPTION);

        final String[] tagNames = null;
        final List<Target> targetsListWithNoTag = targetManagement
                .findByFilters(PAGE, new FilterParams(null, null, null, null, Boolean.TRUE, tagNames)).getContent();

        assertThat(targetManagement.count()).as(TOTAL_TARGETS).isEqualTo(50L);
        assertThat(targetsListWithNoTag).as("Targets with no tag").hasSize(25);

    }

    @Test
    @Description("Tests the a target can be read with only the read target permission")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 1),
            @Expect(type = TargetPollEvent.class, count = 1)})
    void targetCanBeReadWithOnlyReadTargetPermissionTrue() throws Exception {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final String knownTargetControllerId = generateRandomValues().get(CONTROLLERID);
        final Vehicle vehicle = testdataFactory.createVehicle("DCross");
        controllerManagement.findOrRegisterTargetIfItDoesNotExist(knownTargetControllerId,NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), vehicle.getId());

        WithSpringAuthorityRule.runAs(WithSpringAuthorityRule.withUser("bumlux", "READ_TARGET"), () -> {
            final Target findTargetByControllerID = targetManagement.getByControllerID(knownTargetControllerId)
                    .orElseThrow(IllegalStateException::new);
            assertThat(findTargetByControllerID).isNotNull();
            assertThat(findTargetByControllerID.getPollStatus()).isNotNull();
            return null;
        });
    }

    @Test
    @Description("Test that RSQL filter finds targets with tags or specific ids.")
    void findTargetsWithTagOrId() {
        final String targetAName = NAME + testdataFactory.getRandomInt();
        final String vinB = VIN + testdataFactory.getRandomInt();
        final String serialNumberB = SERIAL_NUMBER + testdataFactory.getRandomInt();
        final String nameB = NAME + testdataFactory.getRandomInt();
        String controllerIdB = vinB + "_" + serialNumberB;
        final String rsqlFilter = "tag==" + targetAName + ",id==" + controllerIdB + "-00001,id==" + controllerIdB + "-00008";
        final TargetTag targTagA = targetTagManagement.create(entityFactory.tag().create().name(targetAName));
        Map<String, String> randomValues = generateRandomValues();
        final List<String> targAs = testdataFactory.createTargets(25, randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), DESCRIPTION).stream()
                .map(Target::getControllerId).collect(Collectors.toList());
        targetManagement.toggleTagAssignment(targAs, targTagA.getName());


        testdataFactory.createTargets(25, nameB, serialNumberB, controllerIdB, DESCRIPTION);

        final Slice<Target> foundTargets = targetManagement.findByRsql(PAGE, rsqlFilter);
        final long foundTargetsCount = targetManagement.countByRsql(rsqlFilter);

        assertThat(targetManagement.count()).as(TOTAL_TARGETS).isEqualTo(50L);
        assertThat(foundTargets.getNumberOfElements()).as("Targets in RSQL filter").isEqualTo(foundTargetsCount)
                .isEqualTo(25L);
    }

    @Test
    @Description("Verify that the find all targets by ids method contains the entities that we are looking for")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 12)})
    void verifyFindTargetAllById() {
        final List<Long> searchIds = Arrays.asList(testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(),
                        testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN).getId(),
                testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(),
                        testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN).getId(),
                testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(),
                        testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN).getId());
        for (int i = 0; i < 9; i++) {
            Map<String, String> randomValues = generateRandomValues();
            testdataFactory.createTarget(randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), testdataFactory.createVehicle(randomValues.get(NAME)).getId(),VIN);
        }

        final List<Target> foundDs = targetManagement.get(searchIds);

        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(Target::getId).collect(Collectors.toList());
        assertThat(collect).containsAll(searchIds);
    }

    @Test
    @Description("Verify that the flag for requesting controller attributes is set correctly.")
    void verifyRequestControllerAttributes() {
        Map<String, String> randomValues = generateRandomValues();
        final Target target = createTargetWithAttributes(randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), VIN);

        assertThat(targetManagement.findByControllerAttributesRequested(PAGE)).isEmpty();
        assertThat(targetManagement.isControllerAttributesRequested(randomValues.get(CONTROLLERID))).isFalse();

        targetManagement.requestControllerAttributes(randomValues.get(CONTROLLERID));
        final Target updated = targetManagement.getByControllerID(randomValues.get(CONTROLLERID)).get();

        assertThat(target.isRequestControllerAttributes()).isFalse();
        assertThat(targetManagement.findByControllerAttributesRequested(PAGE).getContent()).contains(updated);
        assertThat(targetManagement.isControllerAttributesRequested(randomValues.get(CONTROLLERID))).isTrue();

    }

    @Test
    @Description("Checks that metadata for a target can be created.")
    void createTargetMetadata() {
        final String knownKey = generateRandomValues().get(CONTROLLERID);
        final String knownValue = generateRandomValues().get(CONTROLLERID);

        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID));
        final JpaTargetMetadata createdMetadata = insertTargetMetadata(knownKey, knownValue, target);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getTarget().getControllerId()).isEqualTo(target.getControllerId());
        assertThat(createdMetadata.getTarget().getId()).isEqualTo(target.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    private JpaTargetMetadata insertTargetMetadata(final String knownKey, final String knownValue,
                                                   final Target target) {
        final JpaTargetMetadata metadata = new JpaTargetMetadata(knownKey, knownValue, target);
        return (JpaTargetMetadata) targetManagement
                .createMetaData(target.getControllerId(), Collections.singletonList(metadata)).get(0);
    }

    @Test
    @Description("Verifies the enforcement of the metadata quota per target.")
    void createTargetMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final Target target1 = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(),
                testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerTarget();
        for (int i = 0; i < maxMetaData; ++i) {
            assertThat(insertTargetMetadata("k" + i, "v" + i, target1)).isNotNull();
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> insertTargetMetadata("k" + maxMetaData, "v" + maxMetaData, target1));

        // add multiple meta data entries at once
        final Target target2 = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), generateRandomValues().get(NAME), generateRandomValues().get(NAME),
                testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        final List<MetaData> metaData2 = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            metaData2.add(new JpaTargetMetadata("k" + i, "v" + i, target2));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.createMetaData(target2.getControllerId(), metaData2));

        // add some meta data entries
        final Target target3 = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID));
        final int firstHalf = Math.round(maxMetaData / 2F);
        for (int i = 0; i < firstHalf; ++i) {
            insertTargetMetadata("k" + i, "v" + i, target3);
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<MetaData> metaData3 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            metaData3.add(new JpaTargetMetadata("kk" + i, "vv" + i, target3));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> targetManagement.createMetaData(target3.getControllerId(), metaData3));

    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a target can be updated.")
    void updateTargetMetadata() throws InterruptedException {
        final String knownKey = NAME + testdataFactory.getRandomInt();
        final String knownValue = NAME + testdataFactory.getRandomInt();
        final String knownUpdateValue = NAME + testdataFactory.getRandomInt();

        // create a target
        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID));
        TestdataFactory.waitForSeconds(2);
        // initial opt lock revision must be zero
        assertThat(target.getOptLockRevision()).isEqualTo(1);

        // create target meta data entry
        insertTargetMetadata(knownKey, knownValue, target);

        Target changedLockRevisionTarget = targetManagement.get(target.getId())
                .orElseThrow(NoSuchElementException::new);
        TestdataFactory.waitForSeconds(2);
        assertThat(changedLockRevisionTarget.getOptLockRevision()).isEqualTo(2);

        // update the target metadata
        final JpaTargetMetadata updated = (JpaTargetMetadata) targetManagement.updateMetadata(target.getControllerId(),
                entityFactory.generateTargetMetadata(knownKey, knownUpdateValue));
        // we are updating the target meta data so also modifying the base
        // software module so opt lock revision must be three
        changedLockRevisionTarget = targetManagement.get(target.getId()).orElseThrow(NoSuchElementException::new);
        assertThat(changedLockRevisionTarget.getOptLockRevision()).isEqualTo(3);
        assertThat(changedLockRevisionTarget.getLastModifiedAt()).isPositive();

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getTarget().getControllerId()).isEqualTo(target.getControllerId());
        assertThat(updated.getTarget().getId()).isEqualTo(target.getId());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that target type for a target can be created, updated and unassigned.")
    void createAndUpdateTargetTypeInTarget() {
        // create a target type
        final List<TargetType> targetTypes = testdataFactory.createTargetTypes(NAME + testdataFactory.getRandomInt(), 2);
        assertThat(targetTypes).hasSize(2);
        // create a target
        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), generateRandomValues().get(NAME), generateRandomValues().get(NAME),
                targetTypes.get(0).getId(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType().getId()).isEqualTo(targetTypes.get(0).getId());

        // update the target type
        final TargetUpdate targetUpdate = entityFactory.target().update(target.getControllerId())
                .targetType(targetTypes.get(1).getId());
        targetManagement.update(targetUpdate);

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType().getId()).isEqualTo(targetTypes.get(1).getId());

        // unassign the target type
        targetManagement.unAssignType(target.getControllerId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound2 = targetRepository.findById(target.getId());
        assertThat(targetFound2).isPresent();
        assertThat(targetFound2.get().getOptLockRevision()).isEqualTo(3);
        assertThat(targetFound2.get().getTargetType()).isNull();
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that target type to a target can be assigned.")
    void assignTargetTypeInTarget() {
        // create a target
        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), NAME + testdataFactory.getRandomInt());
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType()).isNull();

        // create a target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType(NAME + testdataFactory.getRandomInt());
        assertThat(targetType).isNotNull();

        // assign target type to target
        targetManagement.assignType(targetFound.get().getControllerId(), targetType.getId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType().getId()).isEqualTo(targetType.getId());
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Tests the assignment of types to multiple targets.")
    @ExpectEvents({@Expect(type = TargetCreatedEvent.class, count = 20),
            @Expect(type = TargetTypeCreatedEvent.class, count = 2),
            @Expect(type = TargetUpdatedEvent.class, count = 29), @Expect(type = TargetDeletedEvent.class, count = 1)})
    void targetTypeBulkAssignments() {
        final String vinA = VIN + testdataFactory.getRandomInt();
        final String serialNumberA = SERIAL_NUMBER + testdataFactory.getRandomInt();
        final String nameA = NAME + testdataFactory.getRandomInt();
        final String knownControllerIdA = vinA + "_" + serialNumberA;

        final String vinB = VIN + testdataFactory.getRandomInt();
        final String serialNumberB = SERIAL_NUMBER + testdataFactory.getRandomInt();
        final String nameB = NAME + testdataFactory.getRandomInt();
        final String knownControllerIdB = vinB + "_" + serialNumberB;

        final List<Target> typeATargets = testdataFactory.createTargets(10, knownControllerIdA, nameA, serialNumberA, DESCRIPTION);
        final List<Target> typeBTargets = testdataFactory.createTargets(10, knownControllerIdB, nameB, serialNumberB, DESCRIPTION);

        // create a target type
        final TargetType typeA = testdataFactory.createTargetType("A", Collections.singletonList(standardDsType));
        final TargetType typeB = testdataFactory.createTargetType("B", Collections.singletonList(standardDsType));

        // assign target type to target
        TargetTypeAssignmentResult resultA = initiateTypeAssignment(typeATargets, typeA);
        TargetTypeAssignmentResult resultB = initiateTypeAssignment(typeBTargets, typeB);
        assertThat(resultA.getAssigned()).isEqualTo(10);
        assertThat(resultB.getAssigned()).isEqualTo(10);
        checkTargetsHaveType(typeATargets, typeA);
        checkTargetsHaveType(typeBTargets, typeB);

        // double assignment does not unassign
        resultA = initiateTypeAssignment(typeATargets, typeA);
        resultB = initiateTypeAssignment(typeBTargets, typeB);
        assertThat(resultA.getAssigned()).isZero();
        assertThat(resultB.getAssigned()).isZero();
        assertThat(resultA.getAlreadyAssigned()).isEqualTo(10);
        assertThat(resultB.getAlreadyAssigned()).isEqualTo(10);
        checkTargetsHaveType(typeATargets, typeA);
        checkTargetsHaveType(typeBTargets, typeB);

        // verify that type assignment does not throw an error if target list
        // includes an unknown id
        targetManagement.deleteByControllerID(typeATargets.get(0).getControllerId());
        final TargetTypeAssignmentResult resultC = initiateTypeAssignment(typeATargets, typeB);
        assertThat(resultC.getAssigned()).isEqualTo(9);
        assertThat(resultC.getAlreadyAssigned()).isZero();
        checkTargetsHaveType(typeATargets, typeB);
    }

    private void checkTargetsHaveType(final List<Target> targets, final TargetType type) {
        final List<JpaTarget> foundTargets = targetRepository
                .findAllById(targets.stream().map(Identifiable::getId).collect(Collectors.toList()));
        for (final Target target : foundTargets) {
            if (!type.getName().equals(type.getName())) {
                fail(String.format("Target %s is not of type %s.", target, type));
            }
        }
    }

    @Test
    @Description("Queries and loads the metadata related to a given target.")
    void findAllTargetMetadataByControllerId() {
        // create targets
        final String vin1 = VIN + testdataFactory.getRandomInt();
        final String name1 = NAME + testdataFactory.getRandomInt();
        final String serialNumber1 = NAME + testdataFactory.getRandomInt();
        final String controllerId1 = vin1 + "_" + serialNumber1;

        final Target target1 = createTargetWithMetadata(controllerId1, name1, serialNumber1, 10);
        final String vin2 = VIN + testdataFactory.getRandomInt();
        final String name2 = NAME + testdataFactory.getRandomInt();
        final String serialNumber2 = NAME + testdataFactory.getRandomInt();
        final String controllerId2 = vin2 + "_" + serialNumber2;
        final Target target2 = createTargetWithMetadata(controllerId2, name2, serialNumber2, 8);

        final Page<TargetMetadata> metadataOfTarget1 = targetManagement
                .findMetaDataByControllerId(PageRequest.of(0, 100), target1.getControllerId());

        final Page<TargetMetadata> metadataOfTarget2 = targetManagement
                .findMetaDataByControllerId(PageRequest.of(0, 100), target2.getControllerId());

        assertThat(metadataOfTarget1.getNumberOfElements()).isEqualTo(10);
        assertThat(metadataOfTarget1.getTotalElements()).isEqualTo(10);

        assertThat(metadataOfTarget2.getNumberOfElements()).isEqualTo(8);
        assertThat(metadataOfTarget2.getTotalElements()).isEqualTo(8);
    }

    private Target createTargetWithMetadata(final String controllerId, String name, String serialNumber, final int count) {
        final Target target = testdataFactory.createTarget(controllerId, name, serialNumber, testdataFactory.createVehicle(name).getId(),VIN);

        for (int index = 1; index <= count; index++) {
            insertTargetMetadata("key" + index, controllerId + "-value" + index, target);
        }

        return target;
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that target type is not assigned to target if invalid.")
    void assignInvalidTargetTypeToTarget() {
        // create a target
        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), generateRandomValues().get(NAME));
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType()).isNull();

        // assign target type to target
        assertThatExceptionOfType(ConstraintViolationException.class).as("target type with id=null cannot be assigned")
                .isThrownBy(() -> targetManagement.assignType(targetFound.get().getControllerId(), null));

        assertThatExceptionOfType(EntityNotFoundException.class)
                .as("target type with id that does not exists cannot be assigned")
                .isThrownBy(() -> targetManagement.assignType(targetFound.get().getControllerId(), 114L));

        // opt lock revision is not changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(1);
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that target type can be unassigned from target.")
    void unAssignTargetTypeFromTarget() {
        // create a target type
        final TargetType targetType = testdataFactory.findOrCreateTargetType(NAME + testdataFactory.getRandomInt());
        assertThat(targetType).isNotNull();
        // create a target
        final Target target = testdataFactory.createTarget(generateRandomValues().get(CONTROLLERID), generateRandomValues().get(NAME), generateRandomValues().get(SERIAL_NUMBER),
                targetType.getId(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        // initial opt lock revision must be one
        final Optional<JpaTarget> targetFound = targetRepository.findById(target.getId());
        assertThat(targetFound).isPresent();
        assertThat(targetFound.get().getOptLockRevision()).isEqualTo(1);
        assertThat(targetFound.get().getTargetType().getName()).isEqualTo(targetType.getName());

        // un-assign target type from target
        targetManagement.unAssignType(targetFound.get().getControllerId());

        // opt lock revision must be changed
        final Optional<JpaTarget> targetFound1 = targetRepository.findById(target.getId());
        assertThat(targetFound1).isPresent();
        assertThat(targetFound1.get().getOptLockRevision()).isEqualTo(2);
        assertThat(targetFound1.get().getTargetType()).isNull();
    }

    @Test
    @Description("Test that RSQL filter finds targets with metadata and/or controllerId.")
    void findTargetsByRsqlWithMetadata() {
        Map<String, String> randomValues1 = generateRandomValues();
        Map<String, String> randomValues2 = generateRandomValues();

        createTargetWithMetadata(randomValues1.get(CONTROLLERID), randomValues1.get(NAME), randomValues1.get(SERIAL_NUMBER), 2);
        createTargetWithMetadata(randomValues2.get(CONTROLLERID), randomValues2.get(NAME), randomValues2.get(SERIAL_NUMBER), 2);

        final String rsqlAndControllerIdFilter = "id==" + randomValues1.get(CONTROLLERID) + " and metadata.key1==" + randomValues1.get(CONTROLLERID) + VALUE_1_FILTER_QUERY;
        final String rsqlAndControllerIdWithWrongKeyFilter = "id==* and metadata.unknown==value1";
        final String rsqlAndControllerIdNotEqualFilter = "id==* and metadata.key2!=" + randomValues1.get(CONTROLLERID) + "-value2";
        final String rsqlOrControllerIdFilter = "id==" + randomValues1.get(CONTROLLERID) + " or metadata.key1==*value1";
        final String rsqlOrControllerIdWithWrongKeyFilter = "id==" + randomValues2.get(CONTROLLERID) + " or metadata.unknown==value1";
        final String rsqlOrControllerIdNotEqualFilter = "id==" + randomValues1.get(CONTROLLERID) + " or metadata.key1!=" + randomValues1.get(CONTROLLERID) + VALUE_1_FILTER_QUERY;

        assertThat(targetManagement.count()).as(TOTAL_TARGETS).isEqualTo(2);
        validateFoundTargetsByRsql(rsqlAndControllerIdFilter, randomValues1.get(CONTROLLERID));
        validateFoundTargetsByRsql(rsqlAndControllerIdWithWrongKeyFilter);
        validateFoundTargetsByRsql(rsqlAndControllerIdNotEqualFilter, randomValues2.get(CONTROLLERID));
        validateFoundTargetsByRsql(rsqlOrControllerIdFilter, randomValues1.get(CONTROLLERID), randomValues2.get(CONTROLLERID));
        validateFoundTargetsByRsql(rsqlOrControllerIdWithWrongKeyFilter, randomValues2.get(CONTROLLERID));
        validateFoundTargetsByRsql(rsqlOrControllerIdNotEqualFilter, randomValues1.get(CONTROLLERID), randomValues2.get(CONTROLLERID));
    }

    @Test
    @Description("Target matches filter.")
    void matchesFilter() {
        Map<String, String> randomValues = generateRandomValues();
        final Target target = createTargetWithMetadata(randomValues.get(CONTROLLERID), randomValues.get(NAME), randomValues.get(SERIAL_NUMBER), 2);
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final String filter = "metadata.key1==" + randomValues.get(CONTROLLERID) + VALUE_1_FILTER_QUERY;

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(target.getControllerId(),
                ds.getId(), filter)).isTrue();
    }

    @Test
    @Description("Target does not matches filter.")
    void matchesFilterWrongFilter() {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds = testdataFactory.createDistributionSet();
        final String filter = "metadata.key==not_existing";

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(target.getControllerId(),
                ds.getId(), filter)).isFalse();
    }

    @Test
    @Description("Target matches filter but DS already assigned.")
    void matchesFilterDsAssigned() {
        final Target target = testdataFactory.createTarget();
        final DistributionSet ds1 = testdataFactory.createDistributionSet();
        final DistributionSet ds2 = testdataFactory.createDistributionSet();
        assignDistributionSet(ds1, target);
        assignDistributionSet(ds2, target);

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(target.getControllerId(),
                ds1.getId(), TARGET_FILTER_WILD_QUERY)).isFalse();
    }

    @Test
    @Description("Target matches filter for DS with wrong type.")
    void matchesFilterWrongType() {
        final TargetType type = testdataFactory.createTargetType("type", Collections.emptyList());
        final String controllerID = generateRandomValues().get(CONTROLLERID);
        final Target target = testdataFactory.createTarget(controllerID, NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), type.getId(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN);
        final DistributionSet ds = testdataFactory.createDistributionSet();

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(target.getControllerId(),
                ds.getId(), TARGET_FILTER_WILD_QUERY)).isFalse();
    }

    @Test
    @Description("Target matches filter that is invalid.")
    void matchesFilterInvalidFilter() {
        final String target = testdataFactory.createTarget().getControllerId();
        final Long ds = testdataFactory.createDistributionSet().getId();

        assertThatExceptionOfType(RSQLParameterSyntaxException.class).isThrownBy(() -> targetManagement
                .isTargetMatchingQueryAndDSNotAssignedAndCompatible(target, ds, "invalid_syntax"));
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class).isThrownBy(() -> targetManagement
                .isTargetMatchingQueryAndDSNotAssignedAndCompatible(target, ds, "invalid_field==1"));
    }

    @Test
    @Description("Target matches filter for not existing target.")
    void matchesFilterTargetNotExists() {
        final DistributionSet ds = testdataFactory.createDistributionSet();

        assertThat(targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(generateRandomValues().get(CONTROLLERID), ds.getId(),
                TARGET_FILTER_WILD_QUERY)).isFalse();
    }

    @Test
    @Description("Target matches filter for not existing DS.")
    void matchesFilterDsNotExists() {
        final String target = testdataFactory.createTarget().getControllerId();

        assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(
                () -> targetManagement.isTargetMatchingQueryAndDSNotAssignedAndCompatible(target, 123, TARGET_FILTER_WILD_QUERY));
    }

    @Test
    @Description("Verify, for a given list of controllerIds, return lists of registered and unregistered controllerIds")
    void givenListOfControllerIdsWhenValidateControllerIdsThenReturnMapOfRegisteredAndUnregisteredControllerIds(){
        List<String> targets = new ArrayList<>();
        var registeredTarget = testdataFactory.createTarget("testVin1_testSerialNum1").getControllerId();
        var unregisteredTarget = "testVin2_testSerialNum2";
        targets.add(registeredTarget);
        targets.add(unregisteredTarget);

        Map<String, List<String>> valid = targetManagement.separateRegisteredAndUnregisteredControllerIds(targets);
        var regList = valid.get("registered");
        var unregList = valid.get("unregistered");

        Assertions.assertEquals(1, regList.size());
        Assertions.assertEquals(1, unregList.size());
        Assertions.assertEquals(registeredTarget, regList.get(0)) ;
        Assertions.assertEquals(unregisteredTarget, unregList.get(0)) ;
    }

    private void validateFoundTargetsByRsql(final String rsqlFilter, final String... controllerIds) {
        final Slice<Target> foundTargetsByMetadataAndControllerId = targetManagement.findByRsql(PAGE, rsqlFilter);
        final long foundTargetsByMetadataAndControllerIdCount = targetManagement.countByRsql(rsqlFilter);

        assertThat(foundTargetsByMetadataAndControllerId.getNumberOfElements())
                .as("Targets count in RSQL filter is wrong").isEqualTo(foundTargetsByMetadataAndControllerIdCount)
                .isEqualTo(controllerIds.length);
        assertThat(foundTargetsByMetadataAndControllerId.getContent().stream().map(Target::getControllerId))
                .as("Targets found by RSQL filter have wrong controller ids").containsExactlyInAnyOrder(controllerIds);
    }

    private Map<String, String> generateRandomValues() {
        Map<String, String> values = new HashMap<>();

        String vin = VIN + testdataFactory.getRandomInt();
        String serialNumber = SERIAL_NUMBER + testdataFactory.getRandomInt();
        String name = NAME + testdataFactory.getRandomInt();
        String controllerId = vin + "_" + serialNumber;

        values.put(VIN, vin);
        values.put(SERIAL_NUMBER, serialNumber);
        values.put(NAME, name);
        values.put(CONTROLLERID, controllerId);

        return values;
    }
}
