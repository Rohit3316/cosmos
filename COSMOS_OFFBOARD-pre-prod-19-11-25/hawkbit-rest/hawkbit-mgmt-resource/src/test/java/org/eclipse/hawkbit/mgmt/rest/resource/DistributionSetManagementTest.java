/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;


import com.google.common.collect.Lists;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;
import java.util.UUID;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.api.Condition;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.event.remote.entity.ArtifactsCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.exception.AssignmentQuotaExceededException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.InvalidDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetMetadata;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter;
import org.eclipse.hawkbit.repository.model.DistributionSetFilter.DistributionSetFilterBuilder;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSoftwareVersionWrapper;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Statistic;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.test.matcher.Expect;
import org.eclipse.hawkbit.repository.test.matcher.ExpectEvents;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.repository.test.util.WithUser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ValidationException;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@link DistributionSetManagement} tests.
 */
@Feature("Component Tests - Repository")
@Story("DistributionSet Management")
class DistributionSetManagementTest extends AbstractManagementApiIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    public static final String KAFKA_VEHICLE_STATUS_ENDPOINT = "/kafka/vehiclestatus";
    public static final String TAG1_NAME = "Tag1";
    public static final String TARGET_FILTER_QUERY = "name==";
    public static final String TARGET_FILTER_WILD_QUERY = "name==*";
    public static final String DS_TAG_ERROR_MSG = "ds tag ds has wrong ds size";
    public static final String DS_ERROR_MSG = "Invalid distributionSet should throw an exception";
    private static final String NAME = "name";
    private static final String VERSION = "version";
    private static final String DELTA = "DELTA";
    private static final String TEST_FILE_DESCRIPTION = "New file description";
    private static final String DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE = "DELETE FROM sp_artifact_software_module";
    private static final String DELETE_FROM_SP_SOFTWARE_VERSIONS = "DELETE FROM sp_software_versions";
    private static final String DELETE_FROM_SP_ARTIFACTS = "DELETE FROM sp_artifacts";
    private static final String DELETE_FROM_SP_DS_MODULE = "DELETE FROM sp_ds_module";
    private static final String DELETE_FROM_SP_BASE_SOFTWARE_MODULE = "DELETE FROM sp_base_software_module";
    private static final String DELETE_FROM_SP_ROLLOUT = "DELETE FROM sp_rollout";
    private static final String DELETE_FROM_SP_DISTRIBUTION_SET = "DELETE FROM sp_distribution_set";
    private static final String DELETE_FROM_SP_TARGET = "DELETE FROM sp_target";
    private static final String ROLLOUT_NAME = "rollout1";
    @TempDir
    static Path tempDir;
    private static ClientAndServer mockServer;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    private DistributionSetRepository distributionSetRepository;
    @Autowired
    private DistributionSetTagRepository distributionSetTagRepository;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;

    @Autowired
    private ArtifactsRepository artifactsRepository;
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);

    @BeforeAll
    static void init() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockPublishVehicleStatus();
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }

    private static void mockPublishVehicleStatus() {
        mockServer
                .when(HttpRequest.request()
                        .withMethod("POST")
                        .withPath(KAFKA_VEHICLE_STATUS_ENDPOINT))
                .respond(HttpResponse.response()
                        .withStatusCode(200));
    }

    @BeforeEach
    void initSetup() throws Exception {
        System.setProperty(DEFAULT_RSP_KEY, "");
        System.setProperty(DEFAULT_ESP_KEY, "");
        mvc.perform(post("/actuator/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
    }
    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_target_filter_query", "sp_target_tag", "sp_target", "sp_vehicle_ecu",
                "sp_vehicle_model", "sp_action", "sp_rolloutgroup", "sp_rollout", "sp_distribution_set", "sp_artifact_software_module",
                "sp_software_versions", "sp_base_software_module","sp_esp","sp_esp_ecu_rollout","sp_rsp","sp_rsp_rollout","sp_ecu_model");
    }

    @Test
    @Description("Verifies that management get access react as specified on calls for non existing entities by means "
            + "of Optional not present.")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type= ArtifactsCreatedEvent.class, count = 3)})
    void nonExistingEntityAccessReturnsNotPresent() {
        final DistributionSet set = testdataFactory.createDistributionSet();
        assertThat(distributionSetManagement.get(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.getWithDetails(NOT_EXIST_IDL)).isNotPresent();
        assertThat(distributionSetManagement.getByNameAndVersion(NOT_EXIST_ID, NOT_EXIST_ID)).isNotPresent();
        assertThat(distributionSetManagement.getMetaDataByDistributionSetId(set.getId(), NOT_EXIST_ID)).isNotPresent();
    }

    @Test
    @Description("Verify that a DistributionSet with invalid properties cannot be created or updated")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 1),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 3),
            @Expect(type = DistributionSetUpdatedEvent.class),
            @Expect(type= ArtifactsCreatedEvent.class, count = 3)})
    void createAndUpdateDistributionSetWithInvalidFields() {
        final DistributionSet set = testdataFactory.createDistributionSet();

        createAndUpdateDistributionSetWithInvalidDescription(set);
        createAndUpdateDistributionSetWithInvalidName(set);
        createAndUpdateDistributionSetWithInvalidVersion(set);
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidDescription(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid description should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version("a").description(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long description should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .description(RandomStringUtils.randomAlphanumeric(513))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).description(INVALID_TEXT_HTML)));
    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidName(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().version("a")
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name("")));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters in name should not be created")
                .isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().version("a").name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long name should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .name(RandomStringUtils.randomAlphanumeric(NamedEntity.NAME_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with invalid characters should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name(INVALID_TEXT_HTML)));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short name should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).name("")));

    }

    @Step
    private void createAndUpdateDistributionSetWithInvalidVersion(final DistributionSet set) {

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be created")
                .isThrownBy(() -> distributionSetManagement.create(entityFactory.distributionSet().create().name("a")
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be created").isThrownBy(() -> distributionSetManagement
                        .create(entityFactory.distributionSet().create().name("a").version("")));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too long version should not be updated")
                .isThrownBy(() -> distributionSetManagement.update(entityFactory.distributionSet().update(set.getId())
                        .version(RandomStringUtils.randomAlphanumeric(NamedVersionedEntity.VERSION_MAX_SIZE + 1))));

        assertThatExceptionOfType(ConstraintViolationException.class)
                .as("entity with too short version should not be updated").isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(set.getId()).version("")));

    }

    @Test
    @Description("Ensures that it is not possible to create a DS that already exists (unique constraint is on name,version for DS).")
    void createDuplicateDistributionSetsFailsWithException() {
        testdataFactory.createDistributionSet("a");

        assertThatThrownBy(() -> testdataFactory.createDistributionSet("a"))
                .isInstanceOf(EntityAlreadyExistsException.class);
    }

    @Test
    @Description("Verifies that a DS is of default type if not specified explicitly at creation time.")
    void createDistributionSetWithImplicitType() {
        final DistributionSet set = distributionSetManagement
                .create(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()));

        assertThat(set.getType()).as("Type should be equal to default type of tenant")
                .isEqualTo(systemManagement.getTenantMetadata().getDefaultDsType());

    }

    @Test
    @Description("Verifies that a DS cannot be created if another DS with same name and version exists.")
    void createDistributionSetWithDuplicateNameAndVersionFails() {
        final String dsName = NAME + testdataFactory.getRandomInt();
        final String versionName = VERSION + testdataFactory.getRandomInt();
        distributionSetManagement.create(entityFactory.distributionSet().create().name(dsName).version(versionName));

        assertThatExceptionOfType(EntityAlreadyExistsException.class).isThrownBy(() -> distributionSetManagement
                .create(entityFactory.distributionSet().create().name(dsName).version(versionName)));

    }

    @Test
    @Description("Verifies that multiple DS are of default type if not specified explicitly at creation time.")
    void createMultipleDistributionSetsWithImplicitType() {

        final List<DistributionSetCreate> creates = Lists.newArrayListWithExpectedSize(10);
        for (int i = 0; i < 10; i++) {
            creates.add(entityFactory.distributionSet().create().name(NAME + testdataFactory.getRandomInt()).version(VERSION + testdataFactory.getRandomInt()));
        }

        final List<DistributionSet> sets = distributionSetManagement.create(creates);

        assertThat(sets).as("Type should be equal to default type of tenant").are(new Condition<DistributionSet>() {
            @Override
            public boolean matches(final DistributionSet value) {
                return value.getType().equals(systemManagement.getTenantMetadata().getDefaultDsType());
            }
        });

    }

    @Test
    @Description("Checks that metadata for a distribution set can be created.")
    void createDistributionSetMetadata() {
        final String knownKey = "dsMetaKnownKey";
        final String knownValue = "dsMetaKnownValue";

        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");

        final DistributionSetMetadata metadata = new JpaDistributionSetMetadata(knownKey, ds, knownValue);
        final JpaDistributionSetMetadata createdMetadata = (JpaDistributionSetMetadata) createDistributionSetMetadata(
                ds.getId(), metadata);

        assertThat(createdMetadata).isNotNull();
        assertThat(createdMetadata.getId().getKey()).isEqualTo(knownKey);
        assertThat(createdMetadata.getDistributionSet().getId()).isEqualTo(ds.getId());
        assertThat(createdMetadata.getValue()).isEqualTo(knownValue);
    }

    @Test
    @Description("Verifies the enforcement of the metadata quota per distribution set.")
    void createDistributionSetMetadataUntilQuotaIsExceeded() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        // add meta data one by one
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds1");
        final int maxMetaData = quotaManagement.getMaxMetaDataEntriesPerDistributionSet();
        for (int i = 0; i < maxMetaData; ++i) {
            assertThat((JpaDistributionSetMetadata) createDistributionSetMetadata(ds1.getId(),
                    new JpaDistributionSetMetadata("k" + i, ds1, "v" + i))).isNotNull();
        }

        // quota exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds1.getId(), createDistributionSetMetadata(ds1.getId(),
                        new JpaDistributionSetMetadata("k" + maxMetaData, ds1, "v" + maxMetaData))));

        // add multiple meta data entries at once
        final DistributionSet ds2 = testdataFactory.createDistributionSet("ds2");
        final List<MetaData> metaData2 = new ArrayList<>();
        for (int i = 0; i < maxMetaData + 1; ++i) {
            metaData2.add(new JpaDistributionSetMetadata("k" + i, ds2, "v" + i));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds2.getId(), metaData2));

        // add some meta data entries
        final DistributionSet ds3 = testdataFactory.createDistributionSet("ds3");
        final int firstHalf = Math.round((maxMetaData) / 2.f);
        for (int i = 0; i < firstHalf; ++i) {
            createDistributionSetMetadata(ds3.getId(), new JpaDistributionSetMetadata("k" + i, ds3, "v" + i));
        }
        // add too many data entries
        final int secondHalf = maxMetaData - firstHalf;
        final List<MetaData> metaData3 = new ArrayList<>();
        for (int i = 0; i < secondHalf + 1; ++i) {
            metaData3.add(new JpaDistributionSetMetadata("kk" + i, ds3, "vv" + i));
        }
        // verify quota is exceeded
        assertThatExceptionOfType(AssignmentQuotaExceededException.class)
                .isThrownBy(() -> createDistributionSetMetadata(ds3.getId(), metaData3));

    }

    @Test
    @Description("Ensures that distribution sets can assigned and unassigned to a  distribution set tag.")
    void assignAndUnassignDistributionSetToTag() {
        final List<Long> assignDS = Lists.newArrayListWithExpectedSize(4);
        for (int i = 0; i < 4; i++) {
            assignDS.add(testdataFactory.createDistributionSet("DS" + i, "1.0", Collections.emptyList()).getId());
        }

        final DistributionSetTag tag = distributionSetTagManagement
                .create(entityFactory.tag().create().name(TAG1_NAME));

        final List<DistributionSet> assignedDS = distributionSetManagement.assignTag(assignDS, tag.getId());
        assertThat(assignedDS.size()).as("assigned ds has wrong size").isEqualTo(4);
        assignedDS.stream().map(JpaDistributionSet.class::cast)
                .forEach(ds -> assertThat(ds.getTags().size()).as("ds has wrong tag size").isEqualTo(1));

        final DistributionSetTag findDistributionSetTag = getOrThrow(distributionSetTagManagement.getByName(TAG1_NAME));

        assertThat(assignedDS.size()).as("assigned ds has wrong size")
                .isEqualTo(distributionSetManagement.findByTag(PAGE, tag.getId()).getNumberOfElements());

        final JpaDistributionSet unAssignDS = (JpaDistributionSet) distributionSetManagement
                .unAssignTag(assignDS.get(0), findDistributionSetTag.getId());
        assertThat(unAssignDS.getId()).as("unassigned ds is wrong").isEqualTo(assignDS.get(0));
        assertThat(unAssignDS.getTags().size()).as("unassigned ds has wrong tag size").isZero();
        assertThat(distributionSetTagManagement.getByName(TAG1_NAME)).isPresent();
        assertThat(distributionSetManagement.findByTag(PAGE, tag.getId()).getNumberOfElements())
                .as(DS_TAG_ERROR_MSG).isEqualTo(3);

        assertThat(distributionSetManagement.findByRsqlAndTag(PAGE, TARGET_FILTER_QUERY + unAssignDS.getName(), tag.getId())
                .getNumberOfElements()).as(DS_TAG_ERROR_MSG).isZero();
        assertThat(distributionSetManagement.findByRsqlAndTag(PAGE, "name!=" + unAssignDS.getName(), tag.getId())
                .getNumberOfElements()).as(DS_TAG_ERROR_MSG).isEqualTo(3);
    }

    @Test
    @Description("Verifies that an exception is thrown when trying to update an invalid distribution set")
    void updateInvalidDistributionSet() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as(DS_ERROR_MSG).isThrownBy(() -> distributionSetManagement
                        .update(entityFactory.distributionSet().update(distributionSet.getId()).name("new_name")));
    }




    @Test
    @WithUser(allSpPermissions = true)
    @Description("Checks that metadata for a distribution set can be updated.")
    void updateDistributionSetMetadata() {
        final String knownKey = "myKnownKey";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "myNewUpdatedValue";

        // create a DS
        final DistributionSet ds = testdataFactory.createDistributionSet("testDs");
        // initial opt lock revision must be zero
        assertThat(ds.getOptLockRevision()).isEqualTo(1);

        TestdataFactory.waitForSeconds(1);
        // create an DS meta data entry
        createDistributionSetMetadata(ds.getId(), new JpaDistributionSetMetadata(knownKey, ds, knownValue));
        final DistributionSet changedLockRevisionDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(changedLockRevisionDS.getOptLockRevision()).isEqualTo(2);

        TestdataFactory.waitForSeconds(1);
        // update the DS metadata
        final JpaDistributionSetMetadata updated = (JpaDistributionSetMetadata) distributionSetManagement
                .updateMetaData(ds.getId(), entityFactory.generateDsMetadata(knownKey, knownUpdateValue));
        // we are updating the sw metadata so also modifying the base software
        // module so opt lock revision must be three
        final DistributionSet reloadedDS = getOrThrow(distributionSetManagement.get(ds.getId()));
        assertThat(reloadedDS.getOptLockRevision()).isEqualTo(3);
        assertThat(reloadedDS.getLastModifiedAt()).isPositive();

        // verify updated meta data contains the updated value
        assertThat(updated).isNotNull();
        assertThat(updated.getValue()).isEqualTo(knownUpdateValue);
        assertThat(updated.getId().getKey()).isEqualTo(knownKey);
        assertThat(updated.getDistributionSet().getId()).isEqualTo(ds.getId());
    }

    @Test
    @Description("Tests that a DS queue is possible where the result is ordered by the target assignment, i.e. assigned first in the list.")
    void findDistributionSetsAllOrderedByLinkTarget() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<DistributionSet> buildDistributionSets = testdataFactory.createDistributionSets("dsOrder", 10);

        final List<Target> buildTargetFixtures = testdataFactory.createTargets(5, "tOrder", "someDesc");

        final Iterator<DistributionSet> dsIterator = buildDistributionSets.iterator();
        final Iterator<Target> tIterator = buildTargetFixtures.iterator();
        final DistributionSet dsFirst = dsIterator.next();
        final DistributionSet dsSecond = dsIterator.next();
        final DistributionSet dsThree = dsIterator.next();
        final DistributionSet dsFour = dsIterator.next();
        final Target tFirst = tIterator.next();
        final Target tSecond = tIterator.next();

        // set assigned
        assignDistributionSet(dsSecond.getId(), tSecond.getControllerId());
        assignDistributionSet(dsThree.getId(), tFirst.getControllerId());
        // set installed
        testdataFactory.sendUpdateActionStatusToTargets(Collections.singleton(tSecond), DeviceActionStatus.FINISHED_NOT_EXECUTED,
                singletonList("some message"));

        assignDistributionSet(dsFour.getId(), tSecond.getControllerId());

        final DistributionSetFilter distributionSetFilter = new DistributionSetFilterBuilder().setIsDeleted(false)
                .setIsComplete(true).setSelectDSWithNoTag(Boolean.FALSE).build();

        // target first only has an assigned DS-three so check order correct
        final List<DistributionSet> tFirstPin = distributionSetManagement
                .findByDistributionSetFilterOrderByLinkedTarget(PAGE, distributionSetFilter, tFirst.getControllerId())
                .getContent();
        assertThat(tFirstPin).hasSize(10);
        // assigned
        assertThat(tFirstPin.get(0)).isEqualTo(dsThree);
        // remaining id:ASC
        assertThat(tFirstPin.get(1)).isEqualTo(dsFirst);
        assertThat(tFirstPin.get(2)).isEqualTo(dsSecond);
        assertThat(tFirstPin.get(3)).isEqualTo(dsFour);

        // target second has installed DS-2 and assigned DS-4 so check order
        // correct
        final List<DistributionSet> tSecondPin = distributionSetManagement
                .findByDistributionSetFilterOrderByLinkedTarget(PAGE, distributionSetFilter, tSecond.getControllerId())
                .getContent();
        assertThat(tSecondPin).hasSize(10);
        // installed
        assertThat(tSecondPin.get(0)).isEqualTo(dsSecond);
        // assigned
        assertThat(tSecondPin.get(1)).isEqualTo(dsFour);
        // remaining id:ASC
        assertThat(tSecondPin.get(2)).isEqualTo(dsFirst);
        assertThat(tSecondPin.get(3)).isEqualTo(dsThree);

        // target second has installed DS-2 and assigned DS-4 so check order
        // correct
        final List<DistributionSet> tSecondPinOrderedByName = distributionSetManagement
                .findByDistributionSetFilterOrderByLinkedTarget(
                        PageRequest.of(0, 500, Sort.by(Direction.DESC, VERSION)), distributionSetFilter,
                        tSecond.getControllerId())
                .getContent();
        assertThat(tSecondPinOrderedByName).hasSize(10);
        // installed
        assertThat(tSecondPinOrderedByName.get(0)).isEqualTo(buildDistributionSets.get(1));
        // assigned
        assertThat(tSecondPinOrderedByName.get(1)).isEqualTo(buildDistributionSets.get(3));
        // remaining version:DESC
        assertThat(tSecondPinOrderedByName.get(2)).isEqualTo(buildDistributionSets.get(9));
        assertThat(tSecondPinOrderedByName.get(3)).isEqualTo(buildDistributionSets.get(8));
        assertThat(tSecondPinOrderedByName.get(4)).isEqualTo(buildDistributionSets.get(7));
        assertThat(tSecondPinOrderedByName.get(5)).isEqualTo(buildDistributionSets.get(6));
        assertThat(tSecondPinOrderedByName.get(6)).isEqualTo(buildDistributionSets.get(5));
        assertThat(tSecondPinOrderedByName.get(7)).isEqualTo(buildDistributionSets.get(4));
        assertThat(tSecondPinOrderedByName.get(8)).isEqualTo(buildDistributionSets.get(2));
        assertThat(tSecondPinOrderedByName.get(9)).isEqualTo(buildDistributionSets.get(0));

    }

    @Test
    @Description("searches for distribution sets based on the various filter options, e.g. name, version, desc., tags.")
    void searchDistributionSetsOnFilters() {
        DistributionSetTag dsTagA = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-A"));
        final DistributionSetTag dsTagB = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-B"));
        final DistributionSetTag dsTagC = distributionSetTagManagement
                .create(entityFactory.tag().create().name("DistributionSetTag-C"));
        distributionSetTagManagement.create(entityFactory.tag().create().name("DistributionSetTag-D"));

        List<DistributionSet> dsGroup1 = testdataFactory.createDistributionSets("", 5);
        final String dsGroup2Prefix = "test";
        List<DistributionSet> dsGroup2 = testdataFactory.createDistributionSets(dsGroup2Prefix, 5);
        DistributionSet dsDeleted = testdataFactory.createDistributionSet("testDeleted");
        final DistributionSet dsInComplete = distributionSetManagement.create(entityFactory.distributionSet().create()
                .name("notcomplete").version("1").type(standardDsType.getKey()));

        DistributionSetType newType = distributionSetTypeManagement
                .create(entityFactory.distributionSetType().create().key("foo").name("bar").description("test"));

        distributionSetTypeManagement.assignMandatorySoftwareModuleTypes(newType.getId(),
                singletonList(osType.getId()));
        newType = distributionSetTypeManagement.assignOptionalSoftwareModuleTypes(newType.getId(),
                Arrays.asList(appType.getId(), runtimeType.getId()));

        Set<SoftwareModule> li = dsDeleted.getModules();

        final Random random = new SecureRandom();
        Map<Long, Long> map = new HashMap<>();
        for (SoftwareModule swId : li) {
            Version v = versionManagement.create(entityFactory.version().create().name("randomname" + random).description("test")
                    .softwareModuleId(swId.getId()).number(random.nextInt(3000)));
            map.put(swId.getId(), v.getId());
        }

        final DistributionSet dsNewType = distributionSetManagement.create(
                entityFactory.distributionSet().create().name("newtype").version("1").type(newType.getKey()).modules(map));

        assignDistributionSet(dsDeleted, testdataFactory.createTargets(5));
        distributionSetManagement.delete(dsDeleted.getId());
        dsDeleted = getOrThrow(distributionSetManagement.get(dsDeleted.getId()));

        dsGroup1 = toggleTagAssignment(dsGroup1, dsTagA).getAssignedEntity();
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup1 = toggleTagAssignment(dsGroup1, dsTagB).getAssignedEntity();
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));
        dsGroup2 = toggleTagAssignment(dsGroup2, dsTagA).getAssignedEntity();
        dsTagA = getOrThrow(distributionSetTagRepository.findByNameEquals(dsTagA.getName()));

        final List<DistributionSet> allDistributionSets = Stream
                .of(dsGroup1, dsGroup2, Arrays.asList(dsDeleted, dsInComplete, dsNewType)).flatMap(Collection::stream)
                .collect(Collectors.toList());
        final List<DistributionSet> dsGroup1WithGroup2 = Stream.of(dsGroup1, dsGroup2).flatMap(Collection::stream)
                .collect(Collectors.toList());
        final int sizeOfAllDistributionSets = allDistributionSets.size();

        // check setup
        assertThat(distributionSetRepository.findAll()).hasSize(sizeOfAllDistributionSets);

        validateFindAll(allDistributionSets);
        validateDeleted(dsDeleted, sizeOfAllDistributionSets - 1);
        validateCompleted(dsInComplete, sizeOfAllDistributionSets - 1);
        validateType(newType, dsNewType, sizeOfAllDistributionSets - 1);
        validateSearchText(allDistributionSets, dsGroup2Prefix);
        validateTags(dsTagA, dsTagB, dsTagC, dsGroup1WithGroup2, dsGroup1);
        validateDeletedAndCompleted(dsGroup1WithGroup2, dsNewType, dsDeleted);
        validateDeletedAndCompletedAndType(dsGroup1WithGroup2, dsDeleted, newType, dsNewType);
        validateDeletedAndCompletedAndTypeAndSearchText(dsGroup2, newType, dsGroup2Prefix);
        validateDeletedAndCompletedAndTypeAndSearchText(dsGroup1WithGroup2, dsDeleted, dsInComplete, dsNewType, newType,
                ":1");
        validateDeletedAndCompletedAndTypeAndSearchTextAndTag(dsGroup2, dsTagA, dsGroup2Prefix);
    }

    @Step
    private void validateFindAll(final List<DistributionSet> expectedDistributionsets) {

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder(), expectedDistributionsets);
    }

    @Step
    private void validateDeleted(final DistributionSet deletedDistributionSet, final int notDeletedSize) {

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE),
                singletonList(deletedDistributionSet));

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE), notDeletedSize, deletedDistributionSet);
    }

    @Step
    private void validateCompleted(final DistributionSet dsIncomplete, final int completedSize) {

        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE), completedSize, dsIncomplete);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE), singletonList(dsIncomplete));
    }

    @Step
    private void validateType(final DistributionSetType newType, final DistributionSet dsNewType,
                              final int standardDsTypeSize) {
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setTypeId(newType.getId()),
                singletonList(dsNewType));
        assertThatFilterHasSizeAndDoesNotContainDistributionSet(
                getDistributionSetFilterBuilder().setTypeId(standardDsType.getId()), standardDsTypeSize, dsNewType);
    }

    @Step
    private void validateSearchText(final List<DistributionSet> allDistributionSets, final String dsNamePrefix) {

        final List<DistributionSet> withTestNamePrefix = allDistributionSets.stream()
                .filter(ds -> ds.getName().startsWith(dsNamePrefix)).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setSearchText(dsNamePrefix),
                withTestNamePrefix);

        final List<DistributionSet> withTestNameExact = withTestNamePrefix.stream()
                .filter(ds -> ds.getName().equals(dsNamePrefix)).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setSearchText(dsNamePrefix + ":"), withTestNameExact);

        final List<DistributionSet> withTestNameExactAndVersionPrefix = withTestNameExact.stream()
                .filter(ds -> ds.getVersion().startsWith("1")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setSearchText(dsNamePrefix + ":1"),
                withTestNameExactAndVersionPrefix);

        final List<DistributionSet> dsWithExactNameAndVersion = withTestNameExactAndVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setSearchText(dsNamePrefix + ":1.0.0"), dsWithExactNameAndVersion);

        final List<DistributionSet> withVersionPrefix = allDistributionSets.stream()
                .filter(ds -> ds.getVersion().startsWith("1.0.")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setSearchText(":1.0."),
                withVersionPrefix);

        final List<DistributionSet> withVersionExact = withVersionPrefix.stream()
                .filter(ds -> ds.getVersion().equals("1.0.0")).collect(Collectors.toList());
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setSearchText(":1.0.0"),
                withVersionExact);

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setSearchText(":"),
                allDistributionSets);

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setSearchText(" : "),
                allDistributionSets);
    }

    @Step
    private void validateTags(final DistributionSetTag dsTagA, final DistributionSetTag dsTagB,
                              final DistributionSetTag dsTagC, final List<DistributionSet> dsWithTagA,
                              final List<DistributionSet> dsWithTagB) {

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setTagNames(singletonList(dsTagA.getName())), dsWithTagA);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setTagNames(singletonList(dsTagB.getName())), dsWithTagB);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setTagNames(Arrays.asList(dsTagA.getName(), dsTagB.getName())),
                dsWithTagA);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setTagNames(Arrays.asList(dsTagC.getName(), dsTagB.getName())),
                dsWithTagB);

        assertThatFilterDoesNotContainAnyDistributionSet(
                getDistributionSetFilterBuilder().setTagNames(singletonList(dsTagC.getName())));
    }

    @Step
    private void validateDeletedAndCompleted(final List<DistributionSet> completedStandardType,
                                             final DistributionSet dsNewType, final DistributionSet dsDeleted) {

        final List<DistributionSet> completedNotDeleted = new ArrayList<>(completedStandardType);
        completedNotDeleted.add(dsNewType);
        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setIsDeleted(Boolean.FALSE),
                completedNotDeleted);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setIsDeleted(Boolean.TRUE),
                singletonList(dsDeleted));

        assertThatFilterDoesNotContainAnyDistributionSet(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.FALSE).setIsDeleted(Boolean.TRUE));
    }

    @Step
    private void validateDeletedAndCompletedAndType(final List<DistributionSet> deletedAndCompletedAndStandardType,
                                                    final DistributionSet dsDeleted, final DistributionSetType newType, final DistributionSet dsNewType) {
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE)
                .setIsComplete(Boolean.TRUE).setTypeId(standardDsType.getId()), deletedAndCompletedAndStandardType);
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setTypeId(standardDsType.getId()).setIsDeleted(Boolean.TRUE), singletonList(dsDeleted));
        assertThatFilterDoesNotContainAnyDistributionSet(getDistributionSetFilterBuilder().setIsDeleted(Boolean.TRUE)
                .setIsComplete(Boolean.FALSE).setTypeId(standardDsType.getId()));
        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setTypeId(newType.getId()),
                singletonList(dsNewType));
    }

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<DistributionSet> completedAndStandardTypeAndSearchText, final DistributionSetType newType,
            final String text) {

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsDeleted(Boolean.FALSE)
                        .setIsComplete(Boolean.TRUE).setTypeId(standardDsType.getId()).setSearchText(text),
                completedAndStandardTypeAndSearchText);

        assertThatFilterDoesNotContainAnyDistributionSet(getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                .setIsDeleted(Boolean.TRUE).setTypeId(standardDsType.getId()).setSearchText(text + ":"));

        assertThatFilterDoesNotContainAnyDistributionSet(
                getDistributionSetFilterBuilder().setTypeId(standardDsType.getId()).setSearchText(text)
                        .setIsComplete(Boolean.FALSE).setIsDeleted(Boolean.FALSE));

        assertThatFilterDoesNotContainAnyDistributionSet(getDistributionSetFilterBuilder().setTypeId(newType.getId())
                .setSearchText(text).setIsComplete(Boolean.TRUE).setIsDeleted(Boolean.FALSE));
    }

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchText(
            final List<DistributionSet> completedAndNotDeletedStandardTypeAndFilterString,
            final DistributionSet dsDeleted, final DistributionSet dsInComplete, final DistributionSet dsNewType,
            final DistributionSetType newType, final String filterString) {

        final List<DistributionSet> completedAndStandardTypeAndFilterString = new ArrayList<>(
                completedAndNotDeletedStandardTypeAndFilterString);
        completedAndStandardTypeAndFilterString.add(dsDeleted);
        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                        .setTypeId(standardDsType.getId()).setSearchText(filterString),
                completedAndStandardTypeAndFilterString);

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setIsDeleted(Boolean.FALSE)
                        .setTypeId(standardDsType.getId()).setSearchText(filterString),
                completedAndNotDeletedStandardTypeAndFilterString);

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE)
                        .setIsDeleted(Boolean.TRUE).setTypeId(standardDsType.getId()).setSearchText(filterString),
                singletonList(dsDeleted));

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setTypeId(standardDsType.getId()).setSearchText(filterString)
                        .setIsComplete(Boolean.FALSE).setIsDeleted(Boolean.FALSE),
                singletonList(dsInComplete));

        assertThatFilterContainsOnlyGivenDistributionSets(getDistributionSetFilterBuilder().setTypeId(newType.getId())
                        .setSearchText(filterString).setIsComplete(Boolean.TRUE).setIsDeleted(Boolean.FALSE),
                singletonList(dsNewType));
    }

    @Step
    private void validateDeletedAndCompletedAndTypeAndSearchTextAndTag(
            final List<DistributionSet> completedAndStandartTypeAndSearchTextAndTagA, final DistributionSetTag dsTagA,
            final String text) {

        assertThatFilterContainsOnlyGivenDistributionSets(
                getDistributionSetFilterBuilder().setIsComplete(Boolean.TRUE).setTypeId(standardDsType.getId())
                        .setSearchText(text).setTagNames(singletonList(dsTagA.getName())),
                completedAndStandartTypeAndSearchTextAndTagA);

        assertThatFilterDoesNotContainAnyDistributionSet(getDistributionSetFilterBuilder()
                .setTypeId(standardDsType.getId()).setSearchText(text).setTagNames(singletonList(dsTagA.getName()))
                .setIsComplete(Boolean.FALSE).setIsDeleted(Boolean.FALSE));
    }

    private DistributionSetFilterBuilder getDistributionSetFilterBuilder() {
        return new DistributionSetFilterBuilder();
    }

    private void assertThatFilterContainsOnlyGivenDistributionSets(final DistributionSetFilterBuilder filterBuilder,
                                                                   final List<DistributionSet> distributionSets) {
        final int expectedDsSize = distributionSets.size();
        assertThat(distributionSetManagement.findByDistributionSetFilter(PAGE, filterBuilder.build()).getContent())
                .hasSize(expectedDsSize).containsOnly(distributionSets.toArray(new DistributionSet[expectedDsSize]));
    }

    private void assertThatFilterDoesNotContainAnyDistributionSet(final DistributionSetFilterBuilder filterBuilder) {
        assertThat(distributionSetManagement.findByDistributionSetFilter(PAGE, filterBuilder.build()).getContent())
                .isEmpty();
    }

    private void assertThatFilterHasSizeAndDoesNotContainDistributionSet(
            final DistributionSetFilterBuilder filterBuilder, final int size, final DistributionSet ds) {
        assertThat(distributionSetManagement.findByDistributionSetFilter(PAGE, filterBuilder.build()).getContent())
                .hasSize(size).doesNotContain(ds);
    }

    @Test
    @Description("Simple DS load without the related data that should be loaded lazy.")
    void findDistributionSetsWithoutLazy() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        testdataFactory.createDistributionSets(20);

        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(20);
    }

    @Test
    @Description("Deltes a DS that is no in use. Expected behaviour is a hard delete on the database.")
    void deleteUnassignedDistributionSet() {
        final DistributionSet ds1 = testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");

        // delete a ds
        assertThat(distributionSetRepository.findAll()).hasSize(2);
        distributionSetManagement.delete(ds1.getId());
        // not assigned so not marked as deleted but fully deleted
        assertThat(distributionSetRepository.findAll()).hasSize(1);
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(1);
    }

    @Test
    @Description("Deletes an invalid distribution set")
    void deleteInvalidDistributionSet() {
        final DistributionSet set = testdataFactory.createAndInvalidateDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    @Test
    @Description("Deletes an incomplete distribution set")
    void deleteIncompleteDistributionSet() {
        final DistributionSet set = testdataFactory.createIncompleteDistributionSet();
        assertThat(distributionSetRepository.findById(set.getId())).isNotEmpty();
        distributionSetManagement.delete(set.getId());
        assertThat(distributionSetRepository.findById(set.getId())).isEmpty();
    }

    @Test
    @Description("Queries and loads the metadata related to a given software module.")
    void findAllDistributionSetMetadataByDsId() {
        // create a DS
        final DistributionSet ds1 = testdataFactory.createDistributionSet("testDs1");
        final DistributionSet ds2 = testdataFactory.createDistributionSet("testDs2");

        for (int index = 0; index < quotaManagement.getMaxMetaDataEntriesPerDistributionSet(); index++) {
            createDistributionSetMetadata(ds1.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds1, "value" + index));
        }

        for (int index = 0; index <= quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 2; index++) {
            createDistributionSetMetadata(ds2.getId(),
                    new JpaDistributionSetMetadata("key" + index, ds2, "value" + index));
        }

        final Page<DistributionSetMetadata> metadataOfDs1 = distributionSetManagement
                .findMetaDataByDistributionSetId(PageRequest.of(0, 100), ds1.getId());

        final Page<DistributionSetMetadata> metadataOfDs2 = distributionSetManagement
                .findMetaDataByDistributionSetId(PageRequest.of(0, 100), ds2.getId());

        assertThat(metadataOfDs1.getNumberOfElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet());
        assertThat(metadataOfDs1.getTotalElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet());

        assertThat(metadataOfDs2.getNumberOfElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 1);
        assertThat(metadataOfDs2.getTotalElements())
                .isEqualTo(quotaManagement.getMaxMetaDataEntriesPerDistributionSet() - 1);
    }

    @Test
    @Description("Deletes a DS that is in use by either target assignment or rollout. Expected behaviour is a soft delete on the database, i.e. only marked as "
            + "deleted, kept as reference but unavailable for future use..")
    void deleteAssignedDistributionSet() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        testdataFactory.createDistributionSet("ds-1");
        testdataFactory.createDistributionSet("ds-2");
        final DistributionSet dsToTargetAssigned = testdataFactory.createDistributionSet("ds-3");
        final DistributionSet dsToRolloutAssigned = testdataFactory.createDistributionSet("ds-4");

        // create assigned DS
        final Target savedTarget = testdataFactory.createTarget();
        assignDistributionSet(dsToTargetAssigned.getId(), savedTarget.getControllerId());

        // create assigned rollout
        final Rollout rollout = createRolloutWithDependencies("rollout", dsToRolloutAssigned, List.of(savedTarget));

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        // delete assigned ds
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        distributionSetManagement.delete(Arrays.asList(dsToTargetAssigned.getId(), dsToRolloutAssigned.getId()));

        // not assigned so not marked as deleted
        assertThat(distributionSetRepository.findAll()).hasSize(4);
        assertThat(distributionSetManagement.findByCompleted(PAGE, true)).hasSize(2);
        assertThat(distributionSetManagement.findAll(PAGE)).hasSize(2);
        assertThat(distributionSetManagement.findByRsql(PAGE, TARGET_FILTER_WILD_QUERY)).hasSize(2);
        assertThat(distributionSetManagement.count()).isEqualTo(2);
    }

    @Test
    @Description("Verify that the find all by ids contains the entities which are looking for")
    @ExpectEvents({@Expect(type = DistributionSetCreatedEvent.class, count = 12),
            @Expect(type = SoftwareModuleCreatedEvent.class, count = 36),
            @Expect(type= ArtifactsCreatedEvent.class, count = 36)})
    void verifyFindDistributionSetAllById() {
        final List<Long> searchIds = new ArrayList<>();
        searchIds.add(testdataFactory.createDistributionSet("ds-4").getId());
        searchIds.add(testdataFactory.createDistributionSet("ds-5").getId());
        searchIds.add(testdataFactory.createDistributionSet("ds-6").getId());
        for (int i = 0; i < 9; i++) {
            testdataFactory.createDistributionSet("test" + i);
        }

        final List<DistributionSet> foundDs = distributionSetManagement.get(searchIds);

        assertThat(foundDs).hasSize(3);

        final List<Long> collect = foundDs.stream().map(DistributionSet::getId).collect(Collectors.toList());
        assertThat(collect).containsAll(searchIds);
    }

    @Test
    @Description("Verify that an exception is thrown when trying to get an invalid distribution set")
    void verifyGetValid() {
        final DistributionSet distributionSet = testdataFactory.createAndInvalidateDistributionSet();

        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as(DS_ERROR_MSG)
                .isThrownBy(() -> distributionSetManagement.getValid(distributionSet.getId()));
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as(DS_ERROR_MSG)
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSet.getId()));
    }

    @Test
    @Description("Verify that an exception is thrown when trying to get an incomplete distribution set")
    void verifyGetValidAndComplete() {
        final DistributionSet distributionSet = testdataFactory.createIncompleteDistributionSet();

        assertThatExceptionOfType(IncompleteDistributionSetException.class)
                .as("Incomplete distributionSet should throw an exception")
                .isThrownBy(() -> distributionSetManagement.getValidAndComplete(distributionSet.getId()));
    }

    @Test
    @Description("Verify that an exception is thrown when trying to create or update metadata for an invalid distribution set.")
    void createMetadataForInvalidDistributionSet() {
        final String knownKey1 = "myKnownKey1";
        final String knownKey2 = "myKnownKey2";
        final String knownValue = "myKnownValue";
        final String knownUpdateValue = "knownUpdateValue";

        final DistributionSet ds = testdataFactory.createDistributionSet();
        distributionSetManagement.createMetaData(ds.getId(),
                singletonList(entityFactory.generateDsMetadata(knownKey1, knownValue)));

        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(singletonList(ds.getId()), CancelationType.NONE, false), 1L);

        // assert that no new metadata can be created
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as(DS_ERROR_MSG)
                .isThrownBy(() -> distributionSetManagement.createMetaData(ds.getId(),
                        singletonList(entityFactory.generateDsMetadata(knownKey2, knownValue))));

        // assert that an existing metadata can not be updated
        assertThatExceptionOfType(InvalidDistributionSetException.class)
                .as(DS_ERROR_MSG).isThrownBy(() -> distributionSetManagement
                        .updateMetaData(ds.getId(), entityFactory.generateDsMetadata(knownKey1, knownUpdateValue)));
    }

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
    void getRolloutsCountStatisticsForDistributionSet() {
        DistributionSet ds1 = testdataFactory.createDistributionSet("DS1");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        DistributionSet ds3 = testdataFactory.createDistributionSet("DS3");
        List<Target> targets = testdataFactory.createTargets(NAME + testdataFactory.getRandomInt(), 4);
        Rollout rollout1 = createRolloutWithDependencies("rollout", ds1, targets);


        Rollout rollout2 = createRolloutWithDependencies(ROLLOUT_NAME, ds2, targets);

        // Freeze the rollout
        rolloutManagement.freeze(rollout2.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout2.getId());

        rolloutManagement.start(rollout2.getId());

        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds1.getId())).hasSize(1);
        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds2.getId())).hasSize(1);
        assertThat(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds3.getId())).isEmpty();

        Optional<Rollout> rollout = rolloutManagement.get(rollout1.getId());
        rollout.ifPresent(value -> assertThat(RolloutStatus.valueOf(String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds1.getId()).get(0).getName()))).isEqualTo(value.getStatus()));

        rollout = rolloutManagement.get(rollout2.getId());
        rollout.ifPresent(value -> assertThat(RolloutStatus.valueOf(String.valueOf(distributionSetManagement.countRolloutsByStatusForDistributionSet(ds2.getId()).get(0).getName()))).isEqualTo(value.getStatus()));
    }

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
    void getActionsCountStatisticsForDistributionSet() {
        DistributionSet ds = testdataFactory.createDistributionSet("DS");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        List<Target> targets = testdataFactory.createTargets(NAME + testdataFactory.getRandomInt(), 4);
        Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, targets);

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        List<Statistic> statistics = distributionSetManagement.countActionsByStatusForDistributionSet(ds.getId());

        assertThat(statistics).hasSize(1);
        assertThat(distributionSetManagement.countActionsByStatusForDistributionSet(ds2.getId())).isEmpty();

        statistics.forEach(statistic -> assertThat(DeviceActionStatus.valueOf(String.valueOf(statistic.getName()))).isEqualTo(DeviceActionStatus.RUNNING));
    }

    @Test
    @Description("Get the Rollouts count by status statistics for a specific Distribution Set")
    void getAutoAssignmentsCountStatisticsForDistributionSet() {
        DistributionSet ds = testdataFactory.createDistributionSet("DS");
        DistributionSet ds2 = testdataFactory.createDistributionSet("DS2");
        testdataFactory.createTargets(NAME + testdataFactory.getRandomInt(), 4);
        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("test filter 1").autoAssignDistributionSet(ds.getId()).query(TARGET_FILTER_QUERY + NAME + "*"));

        targetFilterQueryManagement.create(
                entityFactory.targetFilterQuery().create().name("test filter 2").autoAssignDistributionSet(ds.getId()).query(TARGET_FILTER_QUERY + NAME + "*"));

        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds.getId())).isEqualTo(2);
        assertThat(distributionSetManagement.countAutoAssignmentsForDistributionSet(ds2.getId())).isNull();
    }

    @Test
    @Description("Get the list of DistributionSetModule for a specific Distribution Set")
    void getDistributionSetModuleForGivenDistributionSet() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        assertThrows(EntityNotFoundException.class, () -> distributionSetManagement.getDistributionSetModule(RandomUtils.nextInt()));

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()),
                        new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(1).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(1).getTargetVersion()));

        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        List<IDistributionSetModule> distributionSetModulesActual = distributionSetManagement.getDistributionSetModule(ds.getId());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getName(), distributionSetModulesActual.get(0).getSm().getName());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getName(), distributionSetModulesActual.get(0).getVersion().getName());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(1).getSoftwareModule().getName(), distributionSetModulesActual.get(1).getSm().getName());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(1).getTargetVersion().getName(), distributionSetModulesActual.get(1).getVersion().getName());
    }

    // can be removed with java-11
    private <T> T getOrThrow(final Optional<T> opt) {
        return opt.orElseThrow(NoSuchElementException::new);
    }

    @Test
    @Description("Successfully unlinks a valid software module from an associated distribution set.")
    void givenValidSoftwareModuleAndLinkedDistributionSet_whenUnlinking_thenUnlinkingShouldBeSuccessful() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        List<IDistributionSetModule> distributionSetModulesActual = distributionSetManagement.
                getDistributionSetModule(ds.getId());
        Assertions.assertEquals(1, distributionSetModulesActual.size());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getName(), distributionSetModulesActual.get(0).getSm().getName());
        Assertions.assertEquals(artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getName(), distributionSetModulesActual.get(0).getVersion().getName());

        distributionSetManagement.unassignSoftwareModule(ds.getId(), artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId());
        List<IDistributionSetModule> distributionSetModulesAfterUnlink = distributionSetManagement.
                getDistributionSetModule(ds.getId());
        Assertions.assertEquals(0, distributionSetModulesAfterUnlink.size());
    }

    @Test
    @Description("Throws an exception when attempting to unlink a software module while the rollout is in the active state.")
    void givenRolloutInActiveState_whenUnlinkingSoftwareModule_thenShouldThrowException() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSet("ds1");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        Target target = testdataFactory.createTarget();
        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
    }

    @Test
    @Description("Throws an exception when attempting to unlink a software module while one rollout is in the active state and others are inactive.")
    void givenRolloutsInActiveState_whenUnlinkingSoftwareModule_thenShouldThrowException() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSet("ds1");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        Target target = testdataFactory.createTarget();
        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        Rollout rollout = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));

        // Freeze the rollout
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        //  This method needs to be removed once freeze API is fully functional
        updateRolloutGroupsToReadyState(rollout.getId());

        rolloutManagement.start(rollout.getId());
        createRolloutWithDependencies("rollout2", ds, List.of(target));
        createRolloutWithDependencies("rollout3", ds, List.of(target));
        rolloutHandler.handleAll();
        List<Rollout> rolloutsByDistributionSetId = rolloutRepository.findAllByDistributionSetId(ds.getId());
        assertEquals(3, rolloutsByDistributionSetId.size());
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
    }

    @Test
    @Description("Successfully unlinks a software module from an associated distribution set when the rollouts are in the inactive state.")
    void givenInActiveRollouts_whenUnlinking_thenUnlinkingShouldBeSuccessful() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);

        // Use a truly unique name for each test run
        String uniqueName = NAME + "-" + UUID.randomUUID();
        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                uniqueName,
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSet("ds1");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        Target target = testdataFactory.createTarget();
        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        createRolloutWithDependencies("rollout2", ds, List.of(target));
        createRolloutWithDependencies("rollout3", ds, List.of(target));
        rolloutHandler.handleAll();
        List<Rollout> rolloutsByDistributionSetId = rolloutRepository.findAllByDistributionSetId(ds.getId());
        assertEquals(3, rolloutsByDistributionSetId.size());
        assertDoesNotThrow(() -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
    }

    @Test
    @Description("Successfully unlinks a software module from an associated distribution set when the rollout is in the draft state.")
    void givenRolloutInCreatingState_whenUnlinking_thenUnlinkingShouldBeSuccessful() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSet("ds1");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        Target target = testdataFactory.createTarget();
        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        rolloutHandler.handleAll();
        List<Rollout> rolloutsByDistributionSetId = rolloutRepository.findAllByDistributionSetId(ds.getId());
        assertEquals(1, rolloutsByDistributionSetId.size());
        assertDoesNotThrow(() -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
    }

    @Test
    @Description("Successfully unlinks a software module from an associated distribution set when the rollout is in the deleting state.")
    void givenRolloutsInDeletingState_whenUnlinking_thenUnlinkingShouldBeSuccessful() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSet("ds1");

        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        Target target = testdataFactory.createTarget();
        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);

        Rollout rollout1 = createRolloutWithDependencies(ROLLOUT_NAME, ds, List.of(target));
        rolloutHandler.handleAll();
        List<Rollout> rolloutsByDistributionSetId = rolloutRepository.findAllByDistributionSetId(ds.getId());
        assertEquals(1, rolloutsByDistributionSetId.size());
        rolloutManagement.delete(rollout1.getId());
        assertDoesNotThrow(() -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(0).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion().getId()));
    }

    @Test
    @Description("Throws an exception when attempting to unlink a software module that does not exist.")
    void givenNonExistentSoftwareModule_whenUnlinking_thenShouldThrowException() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        assertThrows(EntityNotFoundException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                1L, 1L));
    }


    @Test
    @Description("Throws an exception when the target version ID is not linked to the specified software module ID.")
    void givenTargetVersionNotLinkedToSoftwareModule_whenValidatingTargetVersion_thenShouldThrowException() {

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        SoftwareModule module1 = testdataFactory.createSoftwareModuleApp("TestSoftware1");
        SoftwareModule module2 = testdataFactory.createSoftwareModuleApp("TestSoftware2");
        Version module1Version = testdataFactory.createVersion(module1.getId(), "TestVersion1");
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                module2.getId(), module1Version.getId()));
    }

    @Test
    @Description("Throws an exception when attempting to unlink a software module that is not linked to the specified distribution set.")
    void givenSoftwareModuleNotLinkedToDistributionSet_whenUnlinking_thenShouldThrowException() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = createSoftwareModuleWithArtifacts(
                osType,
                NAME + testdataFactory.getRandomInt(),
                "1",
                2,
                format,
                swInstallerType
        );

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers =
                List.of(new DistributionSoftwareVersionWrapper(artifactSoftwareModuleAssociationList.get(0).getSoftwareModule(), artifactSoftwareModuleAssociationList.get(0).getTargetVersion()));

        distributionSetManagement.assignSoftwareModules(ds.getId(), distributionSoftwareVersionWrappers);
        assertThrows(ValidationException.class, () -> distributionSetManagement.unassignSoftwareModule(ds.getId(),
                artifactSoftwareModuleAssociationList.get(1).getSoftwareModule().getId(), artifactSoftwareModuleAssociationList.get(1).getTargetVersion().getId()));
    }

    @Test
    @Description("Validates the list of rollout statuses and validates successfully if all statuses are valid and permitted.")
    void givenRolloutStatusesList_whenValidatingStatuses_thenShouldBeSuccessful() {
        EnumSet<RolloutStatus> expectedStatuses = EnumSet.of(
                RolloutStatus.DRAFT, RolloutStatus.DELETING, RolloutStatus.DELETED);
        Set<RolloutStatus> actualRolloutStatuses = JpaRolloutManagement.ALLOWED_ROLLOUT_STATUSES_FOR_LINKING_AND_UNLINKING;
        assertThat(actualRolloutStatuses).isNotNull().hasSize(expectedStatuses.size()).
                containsExactlyInAnyOrderElementsOf(expectedStatuses);
    }

    private List<ArtifactSoftwareModuleAssociation> createSoftwareModuleWithArtifacts(final SoftwareModuleType type, final String name,
                                                                                      final String version, final int numberArtifacts, final SoftwareModuleFormat format,
                                                                                      final SoftwareInstallerType swInstallerType) {

        Path tempFile = tempDir.resolve("test_fileName");

        SoftwareModule softwareModule1 = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name).version("1").description("description of artifact " + name).format(format)
                .swInstallerType(swInstallerType));

        SoftwareModule softwareModule2 = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(type).name(name+"2").version("2").description("description of artifact2 " + name).format(format)
                .swInstallerType(swInstallerType));

        Version swVersion1 = testdataFactory.createVersion(softwareModule1.getId(), version + numberArtifacts);
        Version swVersion2 = testdataFactory.createVersion(softwareModule2.getId(), version + numberArtifacts);

        Artifacts file = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION + "1", "1231", "1231");
        Artifacts file2 = testdataFactory.createArtifacts(tempFile.toString(), FileType.valueOf(DELTA), TEST_FILE_DESCRIPTION + "2", "123", "123");

        JpaArtifactSoftwareModuleAssociationEntity association1 = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file)
                .softwareModule((JpaSoftwareModule) softwareModule1)
                .sourceVersion((JpaVersion) swVersion1)
                .targetVersion((JpaVersion) swVersion1)
                .build();

        JpaArtifactSoftwareModuleAssociationEntity association2 = JpaArtifactSoftwareModuleAssociationEntity.builder()
                .artifact((JpaArtifacts) file2)
                .softwareModule((JpaSoftwareModule) softwareModule2)
                .sourceVersion((JpaVersion) swVersion2)
                .targetVersion((JpaVersion) swVersion2)
                .build();


        List<Artifacts> artifacts = new ArrayList<>();
        artifacts.add(file);
        artifacts.add(file2);

        Set<ArtifactSoftwareModuleAssociation> associationsSet = new HashSet<>();
        associationsSet.add(association1);
        associationsSet.add(association2);

        artifactsManagement.createOrUpdateArtifactSoftwareModuleAssociation(associationsSet);

        return associationsSet.stream().toList();
    }

    @Test
    @Description("Verifies that an exception is thrown when the target version ID does not belong to the provided software module ID")
    void givenSoftwareModuleAndInvalidTargetVersionIds_whenValidatingTargetVersion_thenShouldThrowException() {

        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACT_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_SOFTWARE_VERSIONS);
        jdbcTemplate.update(DELETE_FROM_SP_ARTIFACTS);
        jdbcTemplate.update(DELETE_FROM_SP_DS_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_BASE_SOFTWARE_MODULE);
        jdbcTemplate.update(DELETE_FROM_SP_ROLLOUT);
        jdbcTemplate.update(DELETE_FROM_SP_DISTRIBUTION_SET);
        jdbcTemplate.update(DELETE_FROM_SP_TARGET);

        final DistributionSet ds = testdataFactory.createDistributionSetWithNoSoftwareModules("ds1", "1.0");
        SoftwareModule module1 = testdataFactory.createSoftwareModuleApp("TestSoftware1");
        testdataFactory.createVersion(module1.getId(), "TestVersion1");
        assertThrows(ValidationException.class, () ->
                distributionSetManagement.unassignSoftwareModule(ds.getId(), module1.getId(), 500L));
    }

    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<DistributionSet> sets,
                                                                  final DistributionSetTag tag) {
        return distributionSetManagement.toggleTagAssignment(
                sets.stream().map(DistributionSet::getId).collect(Collectors.toList()), tag.getName());
    }




}