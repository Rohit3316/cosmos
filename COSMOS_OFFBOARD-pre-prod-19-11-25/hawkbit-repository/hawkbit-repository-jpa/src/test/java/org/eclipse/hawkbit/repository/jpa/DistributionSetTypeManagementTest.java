/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Sets;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.RandomStringUtils;
import org.assertj.core.util.Lists;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleTypeCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.RandomGenerator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link DistributionSetManagement} tests.
 */
@Feature("Component Tests - Repository")
@Story("DistributionSet Management")
public class DistributionSetTypeManagementTest extends AbstractJpaIntegrationTest {

    public static final String TARGET_FILTER_WILD_QUERY = "name==*";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String VERSION = "version";

    @Test
    @Description("Verifies that management get access react as specfied on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 0)})
    public void nonExistingEntityAccessReturnsNotPresent() {
        assertThat(distributionSetTypeManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetTypeManagement.getByKey(NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetTypeManagement.getByName(NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 0),
            @Expect(type = DistributionSetTypeCreatedEvent.class, count = 1)})
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {

        final List<Long> softwareModuleTypes = Collections.singletonList(osType.getId());

        final String dsTypeKey = NAME + testdataFactory.getRandomInt();

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(NOT_EXIST_IDL,
                softwareModuleTypes), DistributionSetType.class.getSimpleName());
        final List<Long> notExistingSwModuleTypeIds = Collections.singletonList(NOT_EXIST_IDL);
        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(
                        testdataFactory.findOrCreateDistributionSetType(dsTypeKey, dsTypeKey).getId(), notExistingSwModuleTypeIds),
                SoftwareModuleType.class.getSimpleName());

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(NOT_EXIST_IDL,
                softwareModuleTypes), DistributionSetType.class.getSimpleName());
        verifyThrownExceptionBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(
                        testdataFactory.findOrCreateDistributionSetType(dsTypeKey, dsTypeKey).getId(), notExistingSwModuleTypeIds),
                SoftwareModuleType.class.getSimpleName());

        verifyThrownExceptionBy(() -> distributionSetTypeManagement.delete(NOT_EXIST_IDL), DistributionSetType.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> distributionSetTypeManagement.update(entityFactory.distributionSetType().update(NOT_EXIST_IDL)),
                DistributionSet.class.getSimpleName());
    }

    @Test
    @Description("Verify that a DistributionSet with invalid properties cannot be created or updated")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class, count = 0),
            @Expect(type = ArtifactsCreatedEvent.class, count = 3)})
    public void createAndUpdateDistributionSetWithInvalidFields() {
        final DistributionSet set = testdataFactory.createDistributionSet();

        createAndUpdateDistributionSetWithInvalidDescription(set);
        createAndUpdateDistributionSetWithInvalidName(set);
        createAndUpdateDistributionSetWithInvalidVersion(set);
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set invalid description text should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid description should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).description(INVALID_TEXT_HTML)));

    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().version("a")
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        final String version = VERSION + testdataFactory.getRandomInt();
        assertThatExceptionOfType(ConstraintViolationException.class).as("set with invalid name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version(version).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version(version).name("")));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with null name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version(version).name(null)));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with invalid name should not be updated")
                .isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short name should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name("")));
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {

        final String name = NAME + testdataFactory.getRandomInt();

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name(name)
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name(name).version(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name(name).version("")));

        assertThatExceptionOfType(ConstraintViolationException.class).as("set with null version should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name(name).version(null)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with invalid version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("set with too short version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version("")));
    }

    @Test
    @Description("Tests the successful module update of unused distribution set type which is in fact allowed.")
    public void updateUnassignedDistributionSetTypeModules() {
        final String dsTypeKey = NAME + testdataFactory.getRandomInt();
        final DistributionSetType updatableType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(dsTypeKey).name(NAME + testdataFactory.getRandomInt()));
        assertThat(distributionSetTypeManagement.getByKey(dsTypeKey).get().getMandatoryModuleTypes()).isEmpty();

        // add OS
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Sets.newHashSet(osType.getId()));
        assertThat(distributionSetTypeManagement.getByKey(dsTypeKey).get().getMandatoryModuleTypes())
                .containsOnly(osType);

        // add JVM
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(updatableType.getId(),
                Sets.newHashSet(runtimeType.getId()));
        assertThat(distributionSetTypeManagement.getByKey(dsTypeKey).get().getMandatoryModuleTypes())
                .containsOnly(osType, runtimeType);

        // remove OS
        distributionSetTypeManagement.unassignSoftwareModuleType(updatableType.getId(), osType.getId());
        assertThat(distributionSetTypeManagement.getByKey(dsTypeKey).get().getMandatoryModuleTypes())
                .containsOnly(runtimeType);
    }

    @Test
    @Description("Verifies that the quota for software module types per distribution set type is enforced as expected.")
    public void quotaMaxSoftwareModuleTypes() {

        final int quota = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        // create software module types
        final List<Long> moduleTypeIds = Lists.newArrayList();
        for (int i = 0; i < quota + 1; ++i) {
            String smNane = NAME + testdataFactory.getRandomInt();
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name(smNane)
                    .description(smNane).maxAssignments(1).colour("blue").key(smNane);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }

        // assign all types at once
        final String dsName1 = NAME + testdataFactory.getRandomInt();
        final DistributionSetType dsType1 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(dsName1).name(dsName1));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(
                () -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType1.getId(), moduleTypeIds));
        assertThatExceptionOfType(AssignmentQuotaExceededException.class).isThrownBy(
                () -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType1.getId(), moduleTypeIds));

        // assign as many mandatory modules as possible
        final String dsName2 = NAME + testdataFactory.getRandomInt();
        final DistributionSetType dsType2 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(dsName2).name(dsName2));
        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2.getId(),
                moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType2.getId())).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType2.getId()).get().getMandatoryModuleTypes().size())
                .isEqualTo(quota);
        // assign one more to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(dsType2.getId(),
                        Collections.singletonList(moduleTypeIds.get(quota))));

        // assign as many optional modules as possible
        final String dsName3 = NAME + testdataFactory.getRandomInt();
        final DistributionSetType dsType3 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(dsName3).name(dsName3));
        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3.getId(),
                moduleTypeIds.subList(0, quota));
        assertThat(distributionSetTypeManagement.get(dsType3.getId())).isNotEmpty();
        assertThat(distributionSetTypeManagement.get(dsType3.getId()).get().getOptionalModuleTypes().size())
                .isEqualTo(quota);
        // assign one more to trigger the quota exceeded error
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType3.getId(),
                        Collections.singletonList(moduleTypeIds.get(quota))));

    }

    @Test
    @Description("Tests the successful update of used distribution set type meta data which is in fact allowed.")
    public void updateAssignedDistributionSetTypeMetaData() {
        final String type = NAME + testdataFactory.getRandomInt();
        final String color = NAME + testdataFactory.getRandomInt();
        final DistributionSetType nonUpdatableType = createDistributionSetTypeUsedByDs(type, color);

        distributionSetTypeManagement.update(
                entityFactory.distributionSetType().update(nonUpdatableType.getId()).description(DESCRIPTION));

        assertThat(distributionSetTypeManagement.getByKey(type).get().getDescription())
                .isEqualTo(DESCRIPTION);
        assertThat(distributionSetTypeManagement.getByKey(type).get().getColour()).isEqualTo(color);
    }

    @Test
    @Description("Tests the unsuccessful update of used distribution set type (module addition).")
    public void addModuleToAssignedDistributionSetTypeFails() {
        final String type = NAME + testdataFactory.getRandomInt();
        final String color = NAME + testdataFactory.getRandomInt();
        final DistributionSetType nonUpdatableType = createDistributionSetTypeUsedByDs(type, color);

        assertThatThrownBy(() -> distributionSetTypeManagement
                .assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(), Sets.newHashSet(osType.getId())))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    private DistributionSetType createDistributionSetTypeUsedByDs(final String type, final String color) {
        final DistributionSetType nonUpdatableType = distributionSetTypeManagement.create(entityFactory
                .distributionSetType().create().key(type).name(NAME + testdataFactory.getRandomInt()).colour(color));
        assertThat(distributionSetTypeManagement.getByKey(type).get().getMandatoryModuleTypes()).isEmpty();
        distributionSetManagement.create(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt())
                .version(VERSION + testdataFactory.getRandomInt()).type(nonUpdatableType.getKey()));
        return nonUpdatableType;
    }

    @Test
    @Description("Tests the unsuccessful update of used distribution set type (module removal).")
    public void removeModuleToAssignedDistributionSetTypeFails() {
        final String type = NAME + testdataFactory.getRandomInt();
        DistributionSetType nonUpdatableType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(type).name(NAME + testdataFactory.getRandomInt()));
        assertThat(distributionSetTypeManagement.getByKey(type).get().getMandatoryModuleTypes()).isEmpty();

        nonUpdatableType = distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(nonUpdatableType.getId(),
                Sets.newHashSet(osType.getId()));
        distributionSetManagement.create(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt())
                .version(VERSION + testdataFactory.getRandomInt()).type(nonUpdatableType.getKey()));

        final Long typeId = nonUpdatableType.getId();
        assertThatThrownBy(() -> distributionSetTypeManagement.unassignSoftwareModuleType(typeId, osType.getId()))
                .isInstanceOf(EntityReadOnlyException.class);
    }

    @Test
    @Description("Tests the successfull deletion of unused (hard delete) distribution set types.")
    public void deleteUnassignedDistributionSetType() {
        final JpaDistributionSetType hardDelete = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));

        assertThat(distributionSetTypeRepository.findAll()).contains(hardDelete);
        distributionSetTypeManagement.delete(hardDelete.getId());

        assertThat(distributionSetTypeRepository.findAll()).doesNotContain(hardDelete);
    }

    @Test
    @Description("Tests the successfully deletion of used (soft delete) distribution set types.")
    public void deleteAssignedDistributionSetType() {
        final int existing = (int) distributionSetTypeManagement.count();
        final String softDeleteKey = NAME + testdataFactory.getRandomInt();
        final JpaDistributionSetType toBeDeleted = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(softDeleteKey).name(NAME + testdataFactory.getRandomInt()));

        assertThat(distributionSetTypeRepository.findAll()).contains(toBeDeleted);
        distributionSetManagement.create(
                entityFactory.distributionSet().create().name(softDeleteKey).version(VERSION + testdataFactory.getRandomInt()).type(toBeDeleted.getKey()));

        distributionSetTypeManagement.delete(toBeDeleted.getId());
        final Optional<DistributionSetType> softDeleted = distributionSetTypeManagement.getByKey(softDeleteKey);
        assertThat(softDeleted).isPresent();
        assertThat(softDeleted.get().isDeleted()).isTrue();
        assertThat(distributionSetTypeManagement.findAll(PAGE)).hasSize(existing);
        assertThat(distributionSetTypeManagement.findByRsql(PAGE, TARGET_FILTER_WILD_QUERY)).hasSize(existing);
        assertThat(distributionSetTypeManagement.count()).isEqualTo(existing);
    }

    @Test
    @Description("Verifies that when no SoftwareModules are assigned to a Distribution then the DistributionSet is not complete.")
    public void shouldFailWhenDistributionSetHasNoSoftwareModulesAssigned() {

        final JpaDistributionSetType jpaDistributionSetType = (JpaDistributionSetType) distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));

        final List<SoftwareModule> softwareModules = new ArrayList<>();

        final DistributionSet distributionSet = testdataFactory.createDistributionSet(NAME + testdataFactory.getRandomInt(), VERSION + testdataFactory.getRandomInt(),
                jpaDistributionSetType, softwareModules);

        assertThat(jpaDistributionSetType.checkComplete(distributionSet)).isFalse();
    }

    @Test
    @Description("Update DS type adds mandatory modules")
    void givenDsIdWithMandatoryModulesWhenUpdateThenSuccess() {
        final int quota = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        final List<Long> moduleTypeIds = Lists.newArrayList();
        for (int i = 0; i < quota + 1; ++i) {
            final String smType = NAME + testdataFactory.getRandomInt();
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name(smType)
                    .description(smType).maxAssignments(1).colour("blue").key(smType);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }
        final DistributionSetType dsType1 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));
        distributionSetTypeManagement.update(
                entityFactory.distributionSetType().update(dsType1.getId()).description(DESCRIPTION).mandatory(moduleTypeIds));
        assertThat(distributionSetTypeManagement.get(dsType1.getId()).get().getMandatoryModuleTypes())
                .hasSize(quota + 1);
    }

    @Test
    @Description("Update Optional Modules for a given ds type id")
    void givenDsIdWithOptionalModulesWhenUpdateThenSuccess() {
        final int quota = quotaManagement.getMaxSoftwareModuleTypesPerDistributionSetType();
        final List<Long> moduleTypeIds = Lists.newArrayList();
        for (int i = 0; i < quota + 1; ++i) {
            final String smType = NAME + testdataFactory.getRandomInt();
            final SoftwareModuleTypeCreate smCreate = entityFactory.softwareModuleType().create().name(smType)
                    .description(smType).maxAssignments(1).colour("blue").key(smType);
            moduleTypeIds.add(softwareModuleTypeManagement.create(smCreate).getId());
        }
        final String dstOpt = NAME + testdataFactory.getRandomInt();
        DistributionSetType dsType1 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(dstOpt).name(dstOpt));
        distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(dsType1.getId(),
                moduleTypeIds.subList(0, quota));
        distributionSetTypeManagement.update(
                entityFactory.distributionSetType().update(dsType1.getId()).description(DESCRIPTION).optional(moduleTypeIds));
        if (distributionSetTypeManagement.getByKey(dstOpt).isPresent()) {
            assertFalse(distributionSetTypeManagement.getByKey(dstOpt).get().getOptionalModuleTypes().isEmpty());
        }
    }

    @Test
    @Description("Delete DS Types for list of ids")
    void givenDsIdsWhenDeleteThenSuccess() {
        List<DistributionSetType> distributionSetTypeList = createDistributionSetTypes();
        Collection<Long> ids = new ArrayList<>();
        distributionSetTypeList.forEach(dsType -> ids.add(dsType.getId()));
        assertThat(distributionSetTypeManagement.exists(distributionSetTypeList.get(0).getId())).isTrue();
        distributionSetTypeManagement.delete(ids);
        assertThat(distributionSetTypeRepository.findAllById(ids)).isEmpty();
    }

    @Test
    @Description("when delete ds type and not found throws exception")
    void givenDsIdsNotPresentWhenDeleteThenException() {
        long id1 = RandomGenerator.getRandom().nextLong();
        long id2 = RandomGenerator.getRandom().nextLong();
        Collection<Long> ids = new ArrayList<>();
        ids.add(id1);
        ids.add(id2);
        assertThrows(EntityNotFoundException.class, () -> distributionSetTypeManagement.delete(ids));
    }

    private List<DistributionSetType> createDistributionSetTypes() {
        DistributionSetType dsType1 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));
        DistributionSetType dsType2 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));
        DistributionSetType dsType3 = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));
        List<DistributionSetType> distributionSetTypeList = new ArrayList<>();
        distributionSetTypeList.add(dsType1);
        distributionSetTypeList.add(dsType2);
        distributionSetTypeList.add(dsType3);
        return distributionSetTypeList;
    }
}
