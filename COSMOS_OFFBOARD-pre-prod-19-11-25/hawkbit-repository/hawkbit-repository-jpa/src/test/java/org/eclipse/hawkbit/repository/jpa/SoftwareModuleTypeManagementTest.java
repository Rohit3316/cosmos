/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

@Feature("Component Tests - Repository")
@Story("Software Module Management")
public class SoftwareModuleTypeManagementTest extends AbstractJpaIntegrationTest {

    public static final String TARGET_FILTER_WILD_QUERY = "name==*";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String VERSION = "version";
    private static final Logger LOG = LoggerFactory.getLogger(SoftwareModuleTypeManagementTest.class);
    private static final String ENTITY_ALREADY_EXISTS = "Entity already exists";

    @Test
    @Description("Verifies that management get access reacts as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = SoftwareModuleCreatedEvent.class, count = 0)})
    public void nonExistingEntityAccessReturnsNotPresent() {

        assertThat(softwareModuleTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(softwareModuleTypeManagement.getByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(softwareModuleTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = SoftwareModuleCreatedEvent.class, count = 0)})
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        verifyThrownExceptionBy(() -> softwareModuleTypeManagement.delete(NOT_EXIST_IDL), SoftwareModuleType.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> softwareModuleTypeManagement.update(entityFactory.softwareModuleType().update(NOT_EXIST_IDL)),
                SoftwareModuleType.class.getSimpleName());
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepositoryForType() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        final SoftwareModuleType updated = softwareModuleTypeManagement
                .update(entityFactory.softwareModuleType().update(created.getId()).maxAssignments(1));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entitity to be equal to created version")
                .isEqualTo(created.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftareModuleTypeFieldsToNewValue() {
        final SoftwareModuleType created = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        final SoftwareModuleType updated = softwareModuleTypeManagement.update(
                entityFactory.softwareModuleType().update(created.getId()).description(DESCRIPTION).colour(DESCRIPTION).maxAssignments(1));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(created.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo(DESCRIPTION);
        assertThat(updated.getColour()).as("Updated vendor is").isEqualTo(DESCRIPTION);
    }

    @Test
    @Description("Create Software Module Types call fails when called for existing entities.")
    public void createModuleTypesCallFailsForExistingTypes() {
        final List<SoftwareModuleTypeCreate> created = Arrays.asList(
                entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1),
                entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        softwareModuleTypeManagement.create(created);
        try {
            softwareModuleTypeManagement.create(created);
            fail("Should not have worked as module already exists.");
        } catch (final EntityAlreadyExistsException e) {
            LOG.error(ENTITY_ALREADY_EXISTS, e);
        }
    }

    @Test
    @Description("Tests the successful deletion of software module types. Both unused (hard delete) and used ones (soft delete).")
    public void deleteAssignedAndUnassignedSoftwareModuleTypes() {
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);

        SoftwareModuleType type = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        // delete unassigned
        softwareModuleTypeManagement.delete(type.getId());
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeRepository.findAll()).hasSize(3).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType);

        type = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(4).contains(osType, runtimeType, appType, type);

        softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(type).name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()).format(format).swInstallerType(swInstallerType));

        // delete assigned
        softwareModuleTypeManagement.delete(type.getId());
        assertThat(softwareModuleTypeManagement.findAll(PAGE)).hasSize(3).contains(osType, runtimeType, appType);
        assertThat(softwareModuleTypeManagement.findByRsql(PAGE, TARGET_FILTER_WILD_QUERY)).hasSize(3).contains(osType, runtimeType,
                appType);
        assertThat(softwareModuleTypeManagement.count()).isEqualTo(3);

        assertThat(softwareModuleTypeRepository.findAll()).hasSize(4).contains((JpaSoftwareModuleType) osType,
                (JpaSoftwareModuleType) runtimeType, (JpaSoftwareModuleType) appType,
                softwareModuleTypeRepository.findById(type.getId()).get());
    }

    @Test
    @Description("Checks that software module typeis found based on given name.")
    public void findSoftwareModuleTypeByName() {
        testdataFactory.createSoftwareModuleOs();
        final String name = "NAME" + testdataFactory.getRandomInt();
        final SoftwareModuleType found = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(name).name(name).maxAssignments(1));
        softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1));

        assertThat(softwareModuleTypeManagement.getByName(name).get()).as(DESCRIPTION).isEqualTo(found);
    }

    @Test
    @Description("Verfies that it is not possible to create a type that alrady exists.")
    public void createSoftwareModuleTypeFailsWithExistingEntity() {
        final String type = NAME + testdataFactory.getRandomInt();
        final String name = NAME + testdataFactory.getRandomInt();
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key(type).name(name).maxAssignments(1));
        try {
            softwareModuleTypeManagement
                    .create(entityFactory.softwareModuleType().create().key(type).name(name).maxAssignments(1));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {
            LOG.error(ENTITY_ALREADY_EXISTS, e);
        }
    }

    @Test
    @Description("Verfies that it is not possible to create a list of types where one already exists.")
    public void createSoftwareModuleTypesFailsWithExistingEntity() {
        final String type = NAME + testdataFactory.getRandomInt();
        final String name = NAME + testdataFactory.getRandomInt();
        softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create().key(type).name(name).maxAssignments(1));
        try {
            softwareModuleTypeManagement
                    .create(Arrays.asList(entityFactory.softwareModuleType().create().key(type).name(name).maxAssignments(1),
                            entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1)));
            fail("should not have worked as module type already exists");
        } catch (final EntityAlreadyExistsException e) {
            LOG.error(ENTITY_ALREADY_EXISTS, e);
        }
    }


    @Test
    @Description("Verfies that multiple types are created as requested.")
    public void createMultipleSoftwareModuleTypes() {
        final List<SoftwareModuleType> created = softwareModuleTypeManagement
                .create(Arrays.asList(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1),
                        entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1)));

        assertThat(created.size()).as(DESCRIPTION).isEqualTo(2);
        assertThat(softwareModuleTypeManagement.count()).as(DESCRIPTION).isEqualTo(5);
    }

    @Test
    @Description("Deletes list of software module types")
    public void givenIdsWhenDeleteSwModuleTypesThenSuccess() {
        final List<SoftwareModuleType> created = softwareModuleTypeManagement
                .create(Arrays.asList(entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1),
                        entityFactory.softwareModuleType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()).maxAssignments(1)));
        Collection<Long> ids = new ArrayList<>();
        created.forEach(type -> ids.add(type.getId()));
        softwareModuleTypeManagement.delete(ids);
        assertFalse(softwareModuleTypeManagement.exists(created.get(0).getId()));
        assertThat(softwareModuleTypeManagement.count()).as("Number of types in repository").isEqualTo(3);
    }

    @Test
    @Description("Throws not found when ids not exist in db")
    public void givenIdsNotPresentInDbWhenDeleteSwModuleTypesThenException() {
        Collection<Long> ids = new ArrayList<>();
        ids.add(1013211L);
        ids.add(1234556L);
        assertThrows(EntityNotFoundException.class, () -> softwareModuleTypeManagement.delete(ids));
    }
}
