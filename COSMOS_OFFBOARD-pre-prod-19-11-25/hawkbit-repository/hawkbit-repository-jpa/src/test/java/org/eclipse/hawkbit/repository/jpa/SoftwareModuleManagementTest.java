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
import io.qameta.allure.Story;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.RandomUtils;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.AssignedSoftwareModule;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Feature("Component Tests - Repository")
@Story("Software Module Management")
public class SoftwareModuleManagementTest extends AbstractJpaIntegrationTest {

    public static final String SM_INSTALLER_TYPE_0 = "0";
    public static final String TARGET_FILTER_WILD_QUERY = "name==*";

    private static final String NAME = "name";

    private static final String DESCRIPTION = "description";

    private static final String VERSION = "version";

    public static final String STRING_NAME_EQUALS = "name==*";

    public static final String FOUND = "found";
    public static final String DELETED = "deleted";
    public static final String FOUND_QUERY = "%found%";
    protected static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String SP_ARTIFACTS = "sp_artifacts";
    protected static final String SP_SOFTWARE_ECU_MODEL = "sp_software_ecu_model";
    protected static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    protected static final String SP_ECU_MODEL = "sp_ecu_model";
    protected static final String SP_TARGET = "sp_target";
    protected static final String SP_ACTION = "sp_action";
    protected static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    private final String FILE_SIZE = "5120";
    private final String SHA_256 = "SHA_256";
    protected static final String Version1 = "1.0.1";
    protected static final String Version12 = "1.0.2";
    protected static final String Version172 = "1.7.2";
    protected static final String ORACLE = "oracle";
    protected static final String TEST1 = "test1";
    protected static final String TEST2 = "test2";
    protected static final String A = "a";
    protected static final String B = "b";
    protected static final String KEY = "key";
    protected static final String JRE = "-jre";
    protected static final String AGENT_HUB = "agent-hub";
    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setup() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_ARTIFACT_SOFTWARE_MODULE, SP_ARTIFACTS, SP_SOFTWARE_ECU_MODEL, SP_SOFTWARE_VERSIONS, SP_BASE_SOFTWARE_MODULE, SP_ECU_MODEL, SP_TARGET, SP_ACTION, SP_DISTRIBUTION_SET);
    }

    @Test
    @Description("Verifies that management get access reacts as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = SoftwareModuleCreatedEvent.class, count = 1)})
    public void nonExistingEntityAccessReturnsNotPresent() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.get(1234L)).isNotPresent();

        assertThat(softwareModuleManagement.getByNameAndVersionAndType(NOT_EXIST_ID, NOT_EXIST_ID, osType.getId()))
                .isNotPresent();

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(module.getId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verifies that management queries react as specfied on calls for non existing entities "
            + " by means of throwing EntityNotFoundException.")
    @ExpectEvents({@Expect(type = SoftwareModuleCreatedEvent.class, count = 1)})
    public void entityQueriesReferringToNotExistingEntitiesThrowsException() {
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp();
        final String name = NAME + testdataFactory.getRandomInt();

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.create(Collections
                        .singletonList(entityFactory.softwareModule().create().name(name).type(NOT_EXIST_ID))),
                SoftwareModuleType.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> softwareModuleManagement
                        .create(entityFactory.softwareModule().create().name(name).type(NOT_EXIST_ID)),
                SoftwareModuleType.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetaData(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key(name).value(name)),
                SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.createMetaData(Collections.singletonList(
                        entityFactory.softwareModuleMetadata().create(NOT_EXIST_IDL).key(name).value(name))),
                SoftwareModule.class.getSimpleName());

        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(NOT_EXIST_IDL), SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(() -> softwareModuleManagement.delete(Collections.singletonList(NOT_EXIST_IDL)),
                SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetaData(NOT_EXIST_IDL, name), SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(() -> softwareModuleManagement.deleteMetaData(module.getId(), NOT_EXIST_ID),
                "SoftwareModuleMetadata");

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetaData(
                        entityFactory.softwareModuleMetadata().update(NOT_EXIST_IDL, name).value(name)),
                SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(
                () -> softwareModuleManagement.updateMetaData(
                        entityFactory.softwareModuleMetadata().update(module.getId(), NOT_EXIST_ID).value(name)),
                "SoftwareModuleMetadata");

        verifyThrownExceptionBy(() -> softwareModuleManagement.findByAssignedTo(PAGE, NOT_EXIST_IDL),
                "DistributionSet");

        verifyThrownExceptionBy(() -> softwareModuleManagement.getByNameAndVersionAndType(name, name, NOT_EXIST_IDL),
                SoftwareModuleType.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.getMetaDataBySoftwareModuleId(NOT_EXIST_IDL, NOT_EXIST_ID),
                SoftwareModule.class.getSimpleName());

        verifyThrownExceptionBy(() -> softwareModuleManagement.findMetaDataBySoftwareModuleId(PAGE, NOT_EXIST_IDL),
                SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(() -> softwareModuleManagement.findMetaDataByRsql(PAGE, NOT_EXIST_IDL, STRING_NAME_EQUALS),
                SoftwareModule.class.getSimpleName());
        verifyThrownExceptionBy(() -> softwareModuleManagement.findByType(PAGE, NOT_EXIST_IDL), SoftwareModule.class.getSimpleName());

        verifyThrownExceptionBy(
                () -> softwareModuleManagement.update(entityFactory.softwareModule().update(NOT_EXIST_IDL)),
                SoftwareModule.class.getSimpleName());
    }

    @Test
    @Description("Calling update without changing fields results in no recorded change in the repository including unchanged audit fields.")
    public void updateNothingResultsInUnchangedRepository() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()));

        assertThat(updated.getOptLockRevision())
                .as("Expected version number of updated entity to be equal to created version")
                .isEqualTo(ah.getOptLockRevision());
    }

    @Test
    @Description("Calling update for changed fields results in change in the repository.")
    public void updateSoftwareModuleFieldsToNewValue() {
        final SoftwareModule ah = testdataFactory.createSoftwareModuleOs();

        final SoftwareModule updated = softwareModuleManagement
                .update(entityFactory.softwareModule().update(ah.getId()).description(DESCRIPTION).vendor(DESCRIPTION));

        assertThat(updated.getOptLockRevision()).as("Expected version number of updated entitity is")
                .isEqualTo(ah.getOptLockRevision() + 1);
        assertThat(updated.getDescription()).as("Updated description is").isEqualTo(DESCRIPTION);
        assertThat(updated.getVendor()).as("Updated vendor is").isEqualTo(DESCRIPTION);
    }

    @Test
    @Description("Create Software Module call fails when called for existing entity.")
    public void createModuleCallFailsForExistingModule() {
        testdataFactory.createSoftwareModuleOs();
        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .as("Should not have worked as module already exists.")
                .isThrownBy(testdataFactory::createSoftwareModuleOs);
    }

    @Test
    @Description("searched for software modules based on the various filter options, e.g. name,desc,type, version.")
    public void findSoftwareModuleByFilters() {

        final SoftwareModule ah = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name(AGENT_HUB).version(Version1)
                        .format(format).swInstallerType(swInstallerType));
        final SoftwareModule jvm = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(runtimeType).name(ORACLE + JRE).version(Version172)
                        .format(format).swInstallerType(swInstallerType));
        final String Version2 = VERSION + testdataFactory.getRandomInt();
        final String name2 = "poky";
        final SoftwareModule os = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(osType).name(name2).version(Version2)
                        .format(format).swInstallerType(swInstallerType));
        final SoftwareModule ah2 = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(appType).name(AGENT_HUB+"2").version(Version12)
                        .format(format).swInstallerType(swInstallerType));

        Version v = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(os.getId()).number(testdataFactory.getRandomInt()));

        Version v2 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(jvm.getId()).number(testdataFactory.getRandomInt()));
        Version v3 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(ah2.getId()).number(testdataFactory.getRandomInt()));

        Map<Long, Long> map = Map.of(
                os.getId(), v.getId(),
                jvm.getId(), v2.getId(),
                ah2.getId(), v3.getId()
        );

        JpaDistributionSet ds = (JpaDistributionSet) distributionSetManagement
                .create(entityFactory.distributionSet().create().name("ds-1").version(Version1).type(standardDsType)
                        .modules(map));

        final JpaTarget target = (JpaTarget) testdataFactory.createTarget();
        ds = (JpaDistributionSet) assignSet(target, ds).getDistributionSet();

        // standard searches
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, name2, osType.getId()).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, name2, osType.getId()).getContent().get(0))
                .isEqualTo(os);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ORACLE, runtimeType.getId()).getContent())
                .hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ORACLE, runtimeType.getId()).getContent().get(0))
                .isEqualTo(jvm);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, ":" + Version1, appType.getId()).getContent()).hasSize(1)
                .first().isEqualTo(ah);
        final String searchVersion = ":1.0";
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, searchVersion, appType.getId()).getContent()).hasSize(2);

        // no we search with on entity marked as deleted
        softwareModuleManagement.delete(
                softwareModuleRepository.findByAssignedToAndType(PAGE, ds, appType).getContent().get(0).getId());

        assertThat(softwareModuleManagement.findByTextAndType(PAGE, searchVersion, appType.getId()).getContent()).hasSize(1);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, searchVersion, appType.getId()).getContent().get(0))
                .isEqualTo(ah);
        assertThat(softwareModuleManagement.findByTextAndType(PAGE, null, null))
                .hasSize(3);

    }

    private Action assignSet(final JpaTarget target, final JpaDistributionSet ds) {
        assignDistributionSet(ds.getId(), target.getControllerId());
        assertThat(targetManagement.getByControllerID(target.getControllerId()).get().getUpdateStatus())
                .isEqualTo(TargetUpdateStatus.PENDING);
        final Optional<DistributionSet> assignedDistributionSet = deploymentManagement
                .getAssignedDistributionSet(target.getControllerId());
        assertThat(assignedDistributionSet).contains(ds);
        final Action action = actionRepository.findByTargetAndDistributionSet(PAGE, target, ds, true).getContent().get(0);
        assertThat(action).isNotNull();
        return action;
    }

    @Test
    @Description("Searches for software modules based on a list of IDs.")
    public void findSoftwareModulesById() {

        final List<Long> modules = Arrays.asList(testdataFactory.createSoftwareModuleOs().getId(),
                testdataFactory.createSoftwareModuleApp().getId(), 624355263L);

        assertThat(softwareModuleManagement.get(modules)).hasSize(2);
    }

    @Test
    @Description("Searches for software modules by type.")
    public void findSoftwareModulesByType() {
        // found in test
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());
        final SoftwareModule two = testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());
        // ignored
        softwareModuleManagement.delete(testdataFactory.createSoftwareModuleOs(DELETED).getId());
        testdataFactory.createSoftwareModuleApp();

        assertThat(softwareModuleManagement.findByType(PAGE, osType.getId()).getContent())
                .as("Expected to find the following number of modules:").hasSize(2).as("with the following elements")
                .contains(two, one);
    }

    @Test
    @Description("Counts all software modules in the repsitory that are not marked as deleted.")
    public void countSoftwareModulesAll() {
        // found in test
        testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());
        testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleOs(DELETED);
        // ignored
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Expected to find the following number of modules:")
                .isEqualTo(2);
    }

    @Test
    @Description("Deletes an artifact, which is not assigned to a Distribution Set")
    public void hardDeleteOfNotAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with Artifacts
        final SoftwareModule unassignedModule = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), VERSION + testdataFactory.getRandomInt());
        final Version version = testdataFactory.createVersion(unassignedModule.getId(), "version", 1);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) unassignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) unassignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        // [STEP2]: Delete unassigned SoftwareModule
        softwareModuleManagement.delete(unassignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModule is deleted
        assertThat(softwareModuleRepository.findAll()).isEmpty();
        assertThat(softwareModuleManagement.get(unassignedModule.getId())).isNotPresent();

        // verify: binary data of artifact is deleted
        artifactsRepository.deleteById(artifact1.getId());
        artifactsRepository.deleteById(artifact2.getId());

        // verify: metadata of artifact is deleted
        assertThat(artifactsRepository.findById(artifact1.getId())).isNotPresent();
        assertThat(artifactsRepository.findById(artifact2.getId())).isNotPresent();
    }

    @Test
    @Description("Deletes an artifact, which is assigned to a DistributionSet")
    public void softDeleteOfAssignedArtifact() {

        // [STEP1]: Create SoftwareModuleX with ArtifactX
        SoftwareModule assignedModule = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), VERSION + testdataFactory.getRandomInt());

        // [STEP2]: Assign SoftwareModule to DistributionSet
        testdataFactory.createDistributionSet(Sets.newHashSet(assignedModule));

        // [STEP3]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The module should be flagged as deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleManagement.findByRsql(PAGE, STRING_NAME_EQUALS)).isEmpty();
        assertThat(softwareModuleManagement.count()).isZero();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        //binary data is deleted
        final Version version = testdataFactory.createVersion(assignedModule.getId(), "version", 1);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) assignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) assignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactsRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactsRepository.findById(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an artifact, which has been assigned to a rolled out DistributionSet in the past")
    public void softDeleteOfHistoricalAssignedArtifact() {

        // Init target
        final Target target = testdataFactory.createTarget();

        // [STEP1]: Create SoftwareModuleX and include the new ArtifactX
        SoftwareModule assignedModule = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), VERSION + testdataFactory.getRandomInt());

        // [STEP2]: Assign SoftwareModule to DistributionSet
        final DistributionSet disSet = testdataFactory.createDistributionSet(Sets.newHashSet(assignedModule));

        // [STEP3]: Assign DistributionSet to a Device
        assignDistributionSet(disSet, Collections.singletonList(target));

        // [STEP4]: Delete the DistributionSet
        distributionSetManagement.delete(disSet.getId());

        // [STEP5]: Delete the assigned SoftwareModule
        softwareModuleManagement.delete(assignedModule.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: assignedModule is marked as deleted
        assignedModule = softwareModuleManagement.get(assignedModule.getId()).get();
        assertTrue(assignedModule.isDeleted(), "The found module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(1);

        // verify: binary data is deleted
        final Version version = testdataFactory.createVersion(assignedModule.getId(), "version", 1);
        Artifacts artifact1 = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        Artifacts artifact2 = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact1).softwareModule((JpaSoftwareModule) assignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifact2).softwareModule((JpaSoftwareModule) assignedModule).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        associationList.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);
        assertArtifactNull(artifact1, artifact2);

        // verify: artifact meta data is still available
        assertThat(artifactsRepository.findById(artifact1.getId())).isNotNull();
        assertThat(artifactsRepository.findById(artifact2.getId())).isNotNull();
    }

    @Test
    @Description("Delete an software module with an artifact, which is also used by another software module.")
    public void deleteSoftwareModulesWithSharedArtifact() {

        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = RandomUtils.nextBytes(artifactSize);

        final String version1 = VERSION + testdataFactory.getRandomInt();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), version1);
        Version version = testdataFactory.createVersion(moduleX.getId(), NAME + testdataFactory.getRandomInt());

        moduleX = softwareModuleManagement.get(moduleX.getId()).get();

        Artifacts artifactX = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifactX).softwareModule((JpaSoftwareModule) moduleX).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        // [STEP3]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), version1);
        Version version2 = testdataFactory.createVersion(moduleY.getId(), NAME + testdataFactory.getRandomInt());

        Artifacts artifactY = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifactY).softwareModule((JpaSoftwareModule) moduleY).sourceVersion((JpaVersion) version2).targetVersion((JpaVersion) version2).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [VERIFY EXPECTED RESULT]:
        // verify: SoftwareModuleX is deleted, and ModuelY still exists
        assertThat(softwareModuleRepository.findAll()).hasSize(1);
        assertThat(softwareModuleManagement.get(moduleX.getId())).isNotPresent();
        assertThat(softwareModuleManagement.get(moduleY.getId())).isPresent();
        // verify: binary data of artifact is not deleted
        assertArtifactNotNull(artifactY);

        // verify: meta data of artifactX is not deleted
        assertThat(artifactsRepository.findById(artifactX.getId())).isPresent();

        // verify: meta data of artifactY is not deleted
        assertThat(artifactsRepository.findById(artifactY.getId())).isPresent();

    }

    @Test
    @Description("Delete two assigned softwaremodules which share an artifact.")
    public void deleteMultipleSoftwareModulesWhichShareAnArtifact() throws IOException {

        // Init artifact binary data, target and DistributionSets
        final int artifactSize = 1024;
        final byte[] source = RandomUtils.nextBytes(artifactSize);
        final Target target = testdataFactory.createTarget();

        final String version1 = VERSION + testdataFactory.getRandomInt();
        final String artifactFilename = NAME + testdataFactory.getRandomInt();

        // [STEP1]: Create SoftwareModuleX and add a new ArtifactX
        SoftwareModule moduleX = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), version1);

        Version version = testdataFactory.createVersion(moduleX.getId(), NAME + testdataFactory.getRandomInt());

        moduleX = softwareModuleManagement.get(moduleX.getId()).get();

        Artifacts artifactX = testdataFactory.createArtifactsWithExpiryDate(TEST1, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifactX).softwareModule((JpaSoftwareModule) moduleX).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList = new HashSet<>();
        associationList.add(association1);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList);

        // [STEP2]: Create SoftwareModuleY and add the same ArtifactX
        SoftwareModule moduleY = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), version1);

        moduleY = softwareModuleManagement.get(moduleY.getId()).get();

        Artifacts artifactY = testdataFactory.createArtifactsWithExpiryDate(TEST2, FileType.DELTA, DESCRIPTION, FILE_SIZE, SHA_256, 123L, FileTransferStatus.UPLOADING_TO_STORAGE.toString());
        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder().artifact((JpaArtifacts) artifactY).softwareModule((JpaSoftwareModule) moduleY).sourceVersion((JpaVersion) version).targetVersion((JpaVersion) version).build();
        Set<ArtifactSoftwareModuleAssociation> associationList2 = new HashSet<>();
        associationList2.add(association2);
        testdataFactory.createArtifactsSoftwareModuleAssociation(associationList2);

        // [STEP3]: Assign SoftwareModuleX to DistributionSetX and to target
        final DistributionSet disSetX = testdataFactory.createDistributionSet(Sets.newHashSet(moduleX), NAME + testdataFactory.getRandomInt());
        assignDistributionSet(disSetX, Collections.singletonList(target));

        // [STEP4]: Assign SoftwareModuleY to DistributionSet and to target
        final DistributionSet disSetY = testdataFactory.createDistributionSet(Sets.newHashSet(moduleY), NAME + testdataFactory.getRandomInt());
        assignDistributionSet(disSetY, Collections.singletonList(target));

        // [STEP5]: Delete SoftwareModuleX
        softwareModuleManagement.delete(moduleX.getId());

        // [STEP6]: Delete SoftwareModuleY
        softwareModuleManagement.delete(moduleY.getId());

        // [VERIFY EXPECTED RESULT]:
        moduleX = softwareModuleManagement.get(moduleX.getId()).get();
        moduleY = softwareModuleManagement.get(moduleY.getId()).get();

        // verify: SoftwareModuleX and SoftwareModule are marked as deleted
        assertThat(moduleX).isNotNull();
        assertThat(moduleY).isNotNull();
        assertTrue(moduleX.isDeleted(), "The module should be flagged deleted");
        assertTrue(moduleY.isDeleted(), "The module should be flagged deleted");
        assertThat(softwareModuleManagement.findAll(PAGE)).isEmpty();
        assertThat(softwareModuleRepository.findAll()).hasSize(2);

        // verify: binary data of artifact is deleted
        assertArtifactNull(artifactX, artifactY);

        // verify: meta data of artifactX and artifactY is not deleted
        assertThat(artifactsRepository.findById(artifactY.getId())).isNotNull();
    }

    private SoftwareModule createSoftwareModule(final SoftwareModuleType type, final String name, String version) {
        SoftwareModule softwareModule = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name).version(version).description("description of artifact " + name).format(format)
                .swInstallerType(swInstallerType));

        return softwareModule;
    }

    private void assertArtifactNotNull(final Artifacts... results) {
        for (final Artifacts result : results) {
            assertThat(result.getId()).isNotNull();
            assertThat(artifactsRepository.getArtifactsById(result.getId()))
                    .isNotNull();
        }
    }

    private void assertArtifactNull(final Artifacts... results) {
        for (final Artifacts result : results) {
            Optional<Artifacts> artifact = artifactsManagement.getArtifactsById(result.getId());
            Optional<Artifacts> forcedEmpty = artifact.filter(a -> false);
            assertThat(forcedEmpty).isEmpty();
        }
    }

    @Test
    @Description("Test verifies that results are returned based on given filter parameters and in the specified order.")
    public void findSoftwareModuleOrderByDistributionModuleNameAscModuleVersionAsc() {
        final String typeKey = NAME + testdataFactory.getRandomInt();
        // test meta data
        final SoftwareModuleType testType = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(typeKey).name(typeKey).maxAssignments(100));
        DistributionSetType testDsType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(KEY).name(NAME + testdataFactory.getRandomInt()));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(osType.getId()));
        testDsType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(testType.getId()));

        final String formatKey = NAME + testdataFactory.getRandomInt();

        // found in test
        final SoftwareModule unassigned = testdataFactory.createSoftwareModule(typeKey, NAME + testdataFactory.getRandomInt() + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule one = testdataFactory.createSoftwareModule(typeKey, A + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule two = testdataFactory.createSoftwareModule(typeKey, B + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule differentName = testdataFactory.createSoftwareModule(typeKey, NAME + testdataFactory.getRandomInt(), false, formatKey, SM_INSTALLER_TYPE_0);

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule(typeKey, DELETED, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());

        Version v = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(one.getId()).number(testdataFactory.getRandomInt()));

        Version v2 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(two.getId()).number(testdataFactory.getRandomInt()));
        Version v3 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(deleted.getId()).number(testdataFactory.getRandomInt()));
        Version v4 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(four.getId()).number(testdataFactory.getRandomInt()));
        Version v5 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(differentName.getId()).number(testdataFactory.getRandomInt()));

        Map<Long, Long> map = Map.of(
                one.getId(), v.getId(),
                two.getId(), v2.getId(),
                deleted.getId(), v3.getId(),
                four.getId(), v4.getId(),
                differentName.getId(), v5.getId()
        );

        final DistributionSet set = distributionSetManagement
                .create(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()).type(testDsType).modules(map));
        softwareModuleManagement.delete(deleted.getId());

        // with filter on name, version and module type
        assertThat(softwareModuleManagement.findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE,
                set.getId(), FOUND_QUERY, testType.getId()).getContent())
                .as("Found modules with given name, given module type and the assigned ones first")
                .containsExactly(new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                        new AssignedSoftwareModule(unassigned, false));

        // with filter on name, version and module type, sorting defined by
        // Pagerequest
        assertThat(softwareModuleManagement.findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, NAME)), set.getId(), FOUND_QUERY, testType.getId())
                .getContent()).as(
                        "Found modules with given name, given module type, the assigned ones first, ordered by name DESC")
                .containsExactly(new AssignedSoftwareModule(two, true), new AssignedSoftwareModule(one, true),
                        new AssignedSoftwareModule(unassigned, false));

        // with filter on module type only
        assertThat(softwareModuleManagement
                .findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE, set.getId(), null, testType.getId())
                .getContent()).as("Found modules with given module type and the assigned ones first").containsExactly(
                new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(unassigned, false));

        // without any filter
        assertThat(softwareModuleManagement
                .findAllOrderBySetAssignmentAndModuleNameAscModuleVersionAsc(PAGE, set.getId(), null, null)
                .getContent()).as("Found modules with the assigned ones first").containsExactly(
                new AssignedSoftwareModule(one, true), new AssignedSoftwareModule(two, true),
                new AssignedSoftwareModule(differentName, true), new AssignedSoftwareModule(four, true),
                new AssignedSoftwareModule(unassigned, false));
    }

    @Test
    @Description("Checks that number of modules is returned as expected based on given filters.")
    public void countSoftwareModuleByFilters() {

        final String typeName = NAME + testdataFactory.getRandomInt();
        // test meta data
        final SoftwareModuleType testType = softwareModuleTypeManagement
                .create(entityFactory.softwareModuleType().create().key(typeName).name(typeName).maxAssignments(100));
        DistributionSetType testDsType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key(NAME + testdataFactory.getRandomInt()).name(NAME + testdataFactory.getRandomInt()));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(osType.getId()));
        testDsType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(testDsType.getId(),
                Collections.singletonList(testType.getId()));

        final String formatKey = NAME + testdataFactory.getRandomInt();
        // found in test
        testdataFactory.createSoftwareModule(typeName, NAME + testdataFactory.getRandomInt() + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule one = testdataFactory.createSoftwareModule(typeName, NAME + testdataFactory.getRandomInt() + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule two = testdataFactory.createSoftwareModule(typeName, NAME + testdataFactory.getRandomInt() + FOUND, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule differentName = testdataFactory.createSoftwareModule(typeName, NAME + testdataFactory.getRandomInt(), false, formatKey, SM_INSTALLER_TYPE_0);

        // ignored
        final SoftwareModule deleted = testdataFactory.createSoftwareModule(typeName, DELETED, false, formatKey, SM_INSTALLER_TYPE_0);
        final SoftwareModule four = testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());

        Version v = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(one.getId()).number(testdataFactory.getRandomInt()));

        Version v2 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(two.getId()).number(testdataFactory.getRandomInt()));
        Version v3 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(deleted.getId()).number(testdataFactory.getRandomInt()));
        Version v4 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(four.getId()).number(testdataFactory.getRandomInt()));
        Version v5 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(differentName.getId()).number(testdataFactory.getRandomInt()));

        Map<Long, Long> map = Map.of(
                one.getId(), v.getId(),
                two.getId(), v2.getId(),
                deleted.getId(), v3.getId(),
                four.getId(), v4.getId(),
                differentName.getId(), v5.getId()
        );

        distributionSetManagement
                .create(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()).type(testDsType).modules(map));
        softwareModuleManagement.delete(deleted.getId());

        // test
        assertThat(softwareModuleManagement.countByTextAndType(FOUND_QUERY, testType.getId()))
                .as("Number of modules with given name or version and type").isEqualTo(3);
        assertThat(softwareModuleManagement.countByTextAndType(null, testType.getId()))
                .as("Number of modules with given type").isEqualTo(4);
        assertThat(softwareModuleManagement.countByTextAndType(null, null)).as("Number of modules overall")
                .isEqualTo(5);
    }

    @Test
    @Description("Verfies that all undeleted software modules are found in the repository.")
    public void countSoftwareModuleTypesAll() {
        testdataFactory.createSoftwareModuleOs();

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();
        testdataFactory.createDistributionSet(Collections.singletonList(deleted));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.count()).as("Number of undeleted modules").isEqualTo(1);
        assertThat(softwareModuleRepository.count()).as("Number of all modules").isEqualTo(2);
    }

    @Test
    @Description("Verfies that software modules are returned that are assigned to given DS.")
    public void findSoftwareModuleByAssignedTo() {
        // test modules
        final SoftwareModule one = testdataFactory.createSoftwareModuleOs();
        testdataFactory.createSoftwareModuleOs(NAME + testdataFactory.getRandomInt());

        // one soft deleted
        final SoftwareModule deleted = testdataFactory.createSoftwareModuleApp();

        Version v = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(one.getId()).number(testdataFactory.getRandomInt()));

        Version v2 = versionManagement.create(entityFactory.version().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION)
                .softwareModuleId(deleted.getId()).number(testdataFactory.getRandomInt()));

        Map<Long, Long> map = Map.of(
                one.getId(), v.getId(),
                deleted.getId(), v2.getId()
        );

        final DistributionSet set = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()).modules(map));
        softwareModuleManagement.delete(deleted.getId());

        assertThat(softwareModuleManagement.findByAssignedTo(PAGE, set.getId()).getContent())
                .as("Found this number of modules").hasSize(2);
    }

    @Test
    @Description("Checks that metadata for a software module can be created.")
    public void createSoftwareModuleMetadata() {

        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();

        final String knownKey2 = NAME + testdataFactory.getRandomInt();
        final String knownValue2 = NAME + testdataFactory.getRandomInt();

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        TestdataFactory.waitForSeconds(1);
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        final SoftwareModuleMetadataCreate swMetadata1 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey1).value(knownValue1);

        final SoftwareModuleMetadataCreate swMetadata2 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey2).value(knownValue2);

        final List<SoftwareModuleMetadata> softwareModuleMetadata = softwareModuleManagement
                .createMetaData(Arrays.asList(swMetadata1, swMetadata2));

        final SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);

        assertThat(softwareModuleMetadata).hasSize(2);
        assertThat(softwareModuleMetadata.get(0)).isNotNull();
        assertThat(softwareModuleMetadata.get(0).getValue()).isEqualTo(knownValue1);
        assertThat(((JpaSoftwareModuleMetadata) softwareModuleMetadata.get(0)).getId().getKey()).isEqualTo(knownKey1);
        assertThat(softwareModuleMetadata.get(0).getEntityId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Verifies the enforcement of the metadata quota per software module.")
    public void createSoftwareModuleMetadataUntilQuotaIsExceeded() {

        // add meta data one by one
        final SoftwareModule module = testdataFactory.createSoftwareModuleApp(NAME + testdataFactory.getRandomInt());
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerSoftwareModule();
        for (int i = 0; i < maxMetaData; ++i) {
            softwareModuleManagement.createMetaData(
                    entityFactory.softwareModuleMetadata().create(module.getId()).key("k" + i).value("v" + i));
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(module.getId()).key("k" + maxMetaData).value("v" + maxMetaData)));

        // add multiple meta data entries at once
        final SoftwareModule module2 = testdataFactory.createSoftwareModuleApp(NAME + testdataFactory.getRandomInt());
        final List<SoftwareModuleMetadataCreate> create = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            create.add(entityFactory.softwareModuleMetadata().create(module2.getId()).key("k" + i).value("v" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(create));

        // add some meta data entries
        final SoftwareModule module3 = testdataFactory.createSoftwareModuleApp(NAME + testdataFactory.getRandomInt());
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            softwareModuleManagement.createMetaData(
                    entityFactory.softwareModuleMetadata().create(module3.getId()).key("k" + i).value("v" + i));
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<SoftwareModuleMetadataCreate> create2 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            create2.add(entityFactory.softwareModuleMetadata().create(module3.getId()).key("kk" + i).value("vv" + i));
        }
        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(create2));

    }

    @Test
    @Description("Checks that metadata for a software module cannot be created for an existing key.")
    public void createSoftwareModuleMetadataFailsIfKeyExists() {

        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1)
                .value(knownValue1).targetVisible(true));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(ah.getId()).key(knownKey1).value(knownValue1).targetVisible(true)))
                .withMessageContaining("Metadata").withMessageContaining(knownKey1);

        final String knownKey2 = NAME + testdataFactory.getRandomInt();

        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey2)
                .value(knownValue1).targetVisible(false));

        assertThatExceptionOfType(EntityAlreadyExistsException.class)
                .isThrownBy(() -> softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata()
                        .create(ah.getId()).key(knownKey2).value(knownValue1).targetVisible(true)))
                .withMessageContaining("Metadata").withMessageContaining(knownKey2);
    }

    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a software module can be updated.")
    public void updateSoftwareModuleMetadata() {
        final String knownKey = NAME + testdataFactory.getRandomInt();
        final String knownValue = NAME + testdataFactory.getRandomInt();
        final String knownUpdateValue = NAME + testdataFactory.getRandomInt();

        // create a base software module
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        TestdataFactory.waitForSeconds(1);
        // initial opt lock revision must be 1
        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        // create an software module meta data entry
        final SoftwareModuleMetadata softwareModuleMetadata = softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey).value(knownValue));
        assertThat(softwareModuleMetadata.isTargetVisible()).isFalse();
        assertThat(softwareModuleMetadata.getValue()).isEqualTo(knownValue);

        // base software module should have now the opt lock revision one
        // because we are modifying the base software module
        SoftwareModule changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(2);
        TestdataFactory.waitForSeconds(1);
        // update the software module metadata
        final SoftwareModuleMetadata updated = softwareModuleManagement.updateMetaData(entityFactory
                .softwareModuleMetadata().update(ah.getId(), knownKey).value(knownUpdateValue).targetVisible(true));

        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be two
        changedLockRevisionModule = softwareModuleManagement.get(ah.getId()).get();
        assertThat(changedLockRevisionModule.getOptLockRevision()).isEqualTo(3);

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.isTargetVisible()).isTrue();
        assertThat(((JpaSoftwareModuleMetadata) updated).getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getEntityId()).isEqualTo(ah.getId());
    }

    @Test
    @Description("Verifies that existing metadata can be deleted.")
    public void deleteSoftwareModuleMetadata() {
        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();

        SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1).value(knownValue1));

        ah = softwareModuleManagement.get(ah.getId()).get();

        assertThat(
                softwareModuleManagement.findMetaDataBySoftwareModuleId(PageRequest.of(0, 10), ah.getId()).getContent())
                .as("Contains the created metadata element")
                .containsExactly(new JpaSoftwareModuleMetadata(knownKey1, ah, knownValue1));

        softwareModuleManagement.deleteMetaData(ah.getId(), knownKey1);
        assertThat(
                softwareModuleManagement.findMetaDataBySoftwareModuleId(PageRequest.of(0, 10), ah.getId()).getContent())
                .as("Metadata elements are").isEmpty();
    }

    @Test
    @Description("Verifies that non existing metadata find results in exception.")
    public void findSoftwareModuleMetadataFailsIfEntryDoesNotExist() {
        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();

        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1).value(knownValue1));

        assertThat(softwareModuleManagement.getMetaDataBySoftwareModuleId(ah.getId(), "doesnotexist")).isNotPresent();
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    public void findAllSoftwareModuleMetadataBySwId() {

        final SoftwareModule sw1 = testdataFactory.createSoftwareModuleApp();
        final int metadataCountSw1 = 8;

        final SoftwareModule sw2 = testdataFactory.createSoftwareModuleOs();
        final int metadataCountSw2 = 10;

        for (int index = 0; index < metadataCountSw1; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(sw1.getId())
                    .key(NAME + testdataFactory.getRandomInt()).value(NAME + testdataFactory.getRandomInt()).targetVisible(true));
        }

        for (int index = 0; index < metadataCountSw2; index++) {
            softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(sw2.getId())
                    .key(NAME + testdataFactory.getRandomInt()).value(NAME + testdataFactory.getRandomInt()).targetVisible(false));
        }

        Page<SoftwareModuleMetadata> metadataSw1 = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, 100), sw1.getId());

        Page<SoftwareModuleMetadata> metadataSw2 = softwareModuleManagement
                .findMetaDataBySoftwareModuleId(PageRequest.of(0, 100), sw2.getId());

        assertThat(metadataSw1.getNumberOfElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw1.getTotalElements()).isEqualTo(metadataCountSw1);

        assertThat(metadataSw2.getNumberOfElements()).isEqualTo(metadataCountSw2);
        assertThat(metadataSw2.getTotalElements()).isEqualTo(metadataCountSw2);

        metadataSw1 = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PageRequest.of(0, 100),
                sw1.getId());

        metadataSw2 = softwareModuleManagement.findMetaDataBySoftwareModuleIdAndTargetVisible(PageRequest.of(0, 100),
                sw2.getId());

        assertThat(metadataSw1.getNumberOfElements()).isEqualTo(metadataCountSw1);
        assertThat(metadataSw1.getTotalElements()).isEqualTo(metadataCountSw1);

        assertThat(metadataSw2.getNumberOfElements()).isZero();
        assertThat(metadataSw2.getTotalElements()).isZero();
    }

    @Test
    @Description("Distribution set Id not found for assigning software modules")
    void givenDistributionSetIdWhenNotExistThrowNotFound() {
        assertThrows(EntityNotFoundException.class, () -> softwareModuleManagement.countByAssignedTo(77));
    }

    @Test
    @Description("Returns count of software modules assigned to distribution set ")
    void givenDistributionSetWhenAssignReturnCount() {
        SoftwareModule assignedModule = createSoftwareModule(osType, NAME + testdataFactory.getRandomInt(), VERSION + testdataFactory.getRandomInt());
        DistributionSet distributionSet = testdataFactory.createDistributionSet(Sets.newHashSet(assignedModule));
        assertThat(softwareModuleManagement.countByAssignedTo(distributionSet.getId())).isEqualTo(1);
    }

    @Test
    @Description("For empty list of meta data, create metadata returns empty list")
    void givenEmptyListWhenCreateMetaDataReturnEmptyList() {
        final List<SoftwareModuleMetadataCreate> create = new ArrayList<>();
        assertThat(softwareModuleManagement.createMetaData(create)).isEmpty();
    }

    @Test
    @Description("Software Module Metadata already exists")
    void givenSwModuleMetadataWhenCreateMetadataThrowAlreadyExists() {
        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();
        final String knownKey2 = NAME + testdataFactory.getRandomInt();
        final String knownValue2 = NAME + testdataFactory.getRandomInt();
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();

        assertThat(ah.getOptLockRevision()).isEqualTo(1);

        final SoftwareModuleMetadataCreate swMetadata1 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey1).value(knownValue1);

        final SoftwareModuleMetadataCreate swMetadata2 = entityFactory.softwareModuleMetadata().create(ah.getId())
                .key(knownKey2).value(knownValue2);

        softwareModuleManagement.createMetaData(Arrays.asList(swMetadata1, swMetadata2));

        assertThrows(EntityAlreadyExistsException.class, () -> softwareModuleManagement
                .createMetaData(Arrays.asList(swMetadata1, swMetadata2)));
    }

    @Test
    @Description("Software Module Id not found for counting Metadata")
    void givenSoftwareModuleIdWhenNotExistThrowNotFound() {
        assertThrows(EntityNotFoundException.class, () -> softwareModuleManagement.countMetaDataBySoftwareModuleId(77));
    }

    @Test
    @Description("Returns count of meta data for a given software Module ")
    void givenSoftwareModuleWhenAssignReturnCount() {
        final String knownKey1 = NAME + testdataFactory.getRandomInt();
        final String knownValue1 = NAME + testdataFactory.getRandomInt();
        final SoftwareModule ah = testdataFactory.createSoftwareModuleApp();
        softwareModuleManagement.createMetaData(
                entityFactory.softwareModuleMetadata().create(ah.getId()).key(knownKey1).value(knownValue1));

        assertThat(softwareModuleManagement.countMetaDataBySoftwareModuleId(ah.getId())).isEqualTo(1);
    }

    @Test
    @Description("Software Module Id if found returns true")
    void givenSoftwareModuleIdWhenExistReturnTrue() {
        assertFalse(softwareModuleManagement.exists(40));
    }
}
