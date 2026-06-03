/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.util;

import com.google.common.collect.Lists;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.RandomStringUtils;
import org.awaitility.Awaitility;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.ecu.dto.MgmtEcuModelRequest;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutDeployment;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutLogRequest;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.cosmos.models.mgmt.vehicle.dto.EcuModelAssignmentRequest;
import org.cosmos.models.mgmt.vehicle.dto.MgmtVehicleRequest;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.Constants;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentLogManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TargetTypeManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UserManagement;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.builder.TargetCreate;
import org.eclipse.hawkbit.repository.builder.TargetTypeCreate;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.eclipse.hawkbit.repository.model.DeploymentLogUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.Polling;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.repository.model.Version;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Data generator utility for tests.
 */
public class TestdataFactory {

    public static final String VISIBLE_SM_MD_KEY = "visibleMetdataKey";
    public static final String VISIBLE_SM_MD_VALUE = "visibleMetdataValue";
    public static final String INVISIBLE_SM_MD_KEY = "invisibleMetdataKey";
    public static final String INVISIBLE_SM_MD_VALUE = "invisibleMetdataValue";
    /**
     * default {@link Target#getControllerId()}.
     */
    public static final String DEFAULT_CONTROLLER_ID = "targetExist";
    /**
     * Default {@link SoftwareModule#getVendor()}.
     */
    public static final String DEFAULT_VENDOR = "Vendor Limited, California";
    /**
     * Default {@link NamedVersionedEntity#getVersion()}.
     */
    public static final String DEFAULT_VERSION = "1.0";
    /**
     * Default {@link NamedEntity#getDescription()}.
     */
    public static final String DEFAULT_DESCRIPTION = "Desc: " + randomDescriptionShort();
    /**
     * Key of test default {@link DistributionSetType}.
     */
    public static final String DS_TYPE_DEFAULT = "test_default_ds_type";
    /**
     * Key of test "os" {@link SoftwareModuleType} : mandatory firmware in
     * {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_OS = "os";
    /**
     * Name of installerType "0" {@link SoftwareInstallerType} : mandatory firmware.
     */
    public static final String SM_INSTALLER_TYPE_0 = "0";
    /**
     * Key of test "runtime" {@link SoftwareModuleType} : optional firmware in
     * {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_RT = "runtime";
    /**
     * Key of test "application" {@link SoftwareModuleType} : optional software
     * in {@link #DS_TYPE_DEFAULT}.
     */
    public static final String SM_TYPE_APP = "application";
    public static final String SM_FORMAT_APP = "qnx";
    public static final String DEFAULT_COLOUR = "#000000";
    public static final Boolean DEFAULT_LOG_COLLECTION_REQUIRED = false;
    public static final Boolean LOG_COLLECTION_REQUIRED = true;
    public static final Integer LOG_MAX_SUCCESS_VIN = 4;
    public static final Integer LOG_MAX_FAILURE_VIN = 8;
    public static final Integer DEFAULT_LOG_MAX_SUCCESS_VIN = 100;
    public static final Integer DEFAULT_LOG_MAX_FAILURE_VIN = 20;
    public static final Integer DEFAULT_LOG_MAX_ALL_FILE_SIZE = 1048576;
    public static final Integer DEFAULT_LOG_MAX_EACH_FILE_SIZE = 209715;
    public static final Integer DEFAULT_LOG_MAX_NUMBER_OF_FILES = 5;
    public static final Integer DEFAULT_DOWNLOAD_RETRY_COUNT = 5;
    public static final Integer DEFAULT_MAX_DOWNLOAD_DURATION_TIMER = 6;
    public static final Integer DEFAULT_MAX_WIFI_DOWNLOAD_DURATION_TIMER = 3;
    public static final Integer DEFAULT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER = 3;
    public static final Integer DEFAULT_MAX_UPDATE_TIME = 1800;
    public static final Integer DEFAULT_DEPLOYMENT_ESTIMATED_UPDATE_TIME = 1500;
    public static final Long DEFAULT_ROLLOUT_MAXIMUM_PACKAGE_SIZE = 10485760L; // 10 MB
    private static final String SAMPLE_NAME = "sample1";
    private static final String CONTROLLER_ID_FORMATER = "%s-%05d";
    private static final String DESCRIPTION = " description";
    private static final Long VALID_ROLLOUT_START_DATE = Instant.now().plus(5, ChronoUnit.HOURS).getEpochSecond();
    private static final Long VALID_ROLLOUT_END_DATE = Instant.now().plus(52, ChronoUnit.HOURS).getEpochSecond();
    private static final String TEST1 = "test1";
    private static final String TEST2 = "test2";
    private static final String TEST3 = "test3";
    private static final String FILE1 = "file1";
    private static final String FILE2 = "file2";
    private static final String FILE3 = "file3";
    private static final String SHA_256 = "SHA_256";
    private static final String VALUE_5120 = "5120";
    private static final Long SIZE = 123L;
    private final Random random = RandomGenerator.getRandom();

    @Autowired
    private ControllerManagement controllerManagament;

    @Autowired
    private SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    private SoftwareModuleTypeManagement softwareModuleTypeManagement;

    @Autowired
    private SoftwareModuleFormatManagement softwareModuleFormatManagement;

    @Autowired
    private SoftwareInstallerTypeManagement softwareInstallerTypeManagement;

    @Autowired
    private DistributionSetManagement distributionSetManagement;

    @Autowired
    private DistributionSetInvalidationManagement distributionSetInvalidationManagement;

    @Autowired
    private DistributionSetTypeManagement distributionSetTypeManagement;

    @Autowired
    private TargetManagement targetManagement;

    @Autowired
    private TargetTypeManagement targetTypeManagement;

    @Autowired
    private DeploymentManagement deploymentManagement;

    @Autowired
    private TargetTagManagement targetTagManagement;

    @Autowired
    private DistributionSetTagManagement distributionSetTagManagement;

    @Autowired
    private EntityFactory entityFactory;


    @Autowired
    private SupportPackageManagement supportPackageManagement;

    @Autowired
    private RolloutManagement rolloutManagement;

    @Autowired
    private RolloutHandler rolloutHandler;

    @Autowired
    private QuotaManagement quotaManagement;

    @Autowired
    private UserManagement userManagement;

    @Autowired
    private VersionManagement versionManagement;

    @Autowired
    private VehicleManagement vehicleManagement;

    @Autowired
    private EcuModelManagement ecuModelManagement;

    @Autowired
    private ArtifactsManagement artifactsManagement;

    @Autowired
    private DeploymentLogManagement deploymentLogManagement;

    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;

    @Autowired
    private SystemManagement systemManagement;


    public static String randomDescriptionShort() {
        return randomText(100);
    }

    private static String randomDescriptionLong() {
        return randomText(200);
    }

    private static String randomText(final int len) {
        return RandomStringUtils.randomAlphanumeric(len);
    }

    private static String generateSha256Hash(String data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return generateSha256Hash(bytes);
    }

    private static String generateSha256Hash(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static void waitForSeconds(long seconds) {
        var startTime = Instant.now().getEpochSecond();
        Awaitility.await().atMost(seconds + 3, TimeUnit.SECONDS)
                .until(() -> Instant.now().getEpochSecond() > startTime + seconds);
    }

    /**
     * Creates a {@link DistributionSet} in the repository, including three {@link SoftwareModule}s
     * of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}, and {@link #SM_TYPE_APP} with the specified version.
     * Each software module is assigned a version and associated with an artifact.
     * The distribution set is created with the given migration and downgrade flags.
     *
     * @param prefix the prefix for software module and distribution set names, vendor, and description
     * @return the created {@link DistributionSet} entity
     */
    public DistributionSet createDistributionSet(final String prefix) {
        return createDistributionSet(prefix, DEFAULT_VERSION, false, false);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *               vendor and description.
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSetWithOutArtifacts(final String prefix) {
        return createDistributionSetWithOutArtifacts(prefix, DEFAULT_VERSION, false, false);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet() {
        return createDistributionSet(UUID.randomUUID().toString(), DEFAULT_VERSION, false, false);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param modules of {@link DistributionSet#getModules()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final Collection<SoftwareModule> modules) {
        return createDistributionSet("", DEFAULT_VERSION, false, modules);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param modules of {@link DistributionSet#getModules()}
     * @param prefix  for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *                vendor and description.
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final Collection<SoftwareModule> modules, final String prefix) {
        return createDistributionSet(prefix, DEFAULT_VERSION, false, modules);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION}.
     *
     * @param prefix                  for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *                                vendor and description.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final boolean isRequiredMigrationStep, final boolean isSoftwareDowngradeEnabled) {
        return createDistributionSet(prefix, DEFAULT_VERSION, isRequiredMigrationStep, isSoftwareDowngradeEnabled);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *               vendor and description.
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final Collection<DistributionSetTag> tags) {
        return createDistributionSet(prefix, DEFAULT_VERSION, tags);
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     /**
     * Creates a {@link DistributionSet} in the repository, including three {@link SoftwareModule}s
     * of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}, and {@link #SM_TYPE_APP} with the specified version.
     * Each software module is assigned a version and associated with an artifact.
     * The distribution set is created with the given migration and downgrade flags.
     *
     * @param prefix the prefix for software module and distribution set names, vendor, and description
     * @param version the version string to use for the software modules and distribution set
     * @param isRequiredMigrationStep whether the distribution set requires a migration step
     * @param isSoftwareDowngradeEnabled whether software downgrade is enabled for the distribution set
     * @return the created {@link DistributionSet} entity
     */
    public DistributionSet createDistributionSet(final String prefix, final String version,
                                                 final boolean isRequiredMigrationStep, final boolean isSoftwareDowngradeEnabled) {


        SoftwareModuleCreate t = entityFactory.softwareModule().create()
                .type(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE)).name(prefix + SM_TYPE_APP)
                .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                .version(version + "." + new SecureRandom().nextInt(100)).description(randomDescriptionLong())
                .vendor(prefix + " vendor Limited, California")
                .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0));


        SoftwareModuleCreate t1 = t.format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP));


        final SoftwareModule appMod = softwareModuleManagement.create(t1);

        final SoftwareModule runtimeMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_RT))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(prefix + "app runtime").version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor GmbH, Stuttgart, Germany")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));
        final SoftwareModule osMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_OS))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(prefix + " Firmware").version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor Limited Inc, California")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));

        final Version v = createVersion(osMod.getId(), SAMPLE_NAME);
        final Version v2 = createVersion(runtimeMod.getId(), "sample2");
        final Version v3 = createVersion(appMod.getId(), "sample3");

        final Map<Long, Long> moduleVersionMap = Map.of(
                osMod.getId(), v.getId(),
                runtimeMod.getId(), v2.getId(),
                appMod.getId(), v3.getId()
        );

        for (Map.Entry<Long, Long> entry : moduleVersionMap.entrySet()) {
            Long moduleId = entry.getKey();
            Long versionId = entry.getValue();

            Long expiryDate = Instant.now().plus(30, ChronoUnit.DAYS).toEpochMilli();
            Artifacts artifact = createArtifactsWithExpiryDate("Artifact for DS ", FileType.FULL, "Description", "123", SHA_256, expiryDate, FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.toString());

            SoftwareModuleArtifactBindingRequest bindingRequest = SoftwareModuleArtifactBindingRequest.builder()
                    .softwareModuleId(Math.toIntExact(moduleId))
                    .sourceVersion(Collections.singletonList(Math.toIntExact(versionId)))
                    .targetVersion(Math.toIntExact(versionId))
                    .build();
            Long tenantId = systemManagement.getTenantMetadata().getTenantId();
            artifactsManagement.createArtifactSoftwareModuleAssociation(artifact.getId().toString(), tenantId, bindingRequest);
        }

        Map<Long, Long> map = Map.of(osMod.getId(), v.getId(),
                runtimeMod.getId(), v2.getId(),
                appMod.getId(), v3.getId()
        );
        return distributionSetManagement.create(
                entityFactory.distributionSet().create().name(prefix != null && prefix.length() > 0 ? prefix : "DS")
                        .version(version).description(randomDescriptionShort()).type(findOrCreateDefaultTestDsType())
                        .modules(map)
                        .requiredMigrationStep(isRequiredMigrationStep)
                        .softwareDowngradeEnabled(isSoftwareDowngradeEnabled));
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP}.
     *
     * @param prefix                  for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *                                vendor and description.
     * @param version                 {@link DistributionSet#getVersion()} and
     *                                {@link SoftwareModule#getVersion()} extended by a random
     *                                number.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSetWithOutArtifacts(final String prefix, final String version,
                                                 final boolean isRequiredMigrationStep, final boolean isSoftwareDowngradeEnabled) {


        SoftwareModuleCreate t = entityFactory.softwareModule().create()
                .type(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE)).name(prefix + SM_TYPE_APP)
                .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                .version(version + "." + new SecureRandom().nextInt(100)).description(randomDescriptionLong())
                .vendor(prefix + " vendor Limited, California")
                .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0));


        SoftwareModuleCreate t1 = t.format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP));


        final SoftwareModule appMod = softwareModuleManagement.create(t1);

        final SoftwareModule runtimeMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_RT))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(prefix + "app runtime").version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor GmbH, Stuttgart, Germany")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));
        final SoftwareModule osMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_OS))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(prefix + " Firmware").version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(prefix + " vendor Limited Inc, California")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));

        final Version v = createVersion(osMod.getId(), SAMPLE_NAME);
        final Version v2 = createVersion(runtimeMod.getId(), "sample2");
        final Version v3 = createVersion(appMod.getId(), "sample3");

        Map<Long, Long> map = Map.of(osMod.getId(), v.getId(),
                runtimeMod.getId(), v2.getId(),
                appMod.getId(), v3.getId()
        );
        return distributionSetManagement.create(
                entityFactory.distributionSet().create().name(prefix != null && prefix.length() > 0 ? prefix : "DS")
                        .version(version).description(randomDescriptionShort()).type(findOrCreateDefaultTestDsType())
                        .modules(map)
                        .requiredMigrationStep(isRequiredMigrationStep)
                        .softwareDowngradeEnabled(isSoftwareDowngradeEnabled));
    }

    /**
     * Adds {@link SoftwareModuleMetadata} to every module of given
     * {@link DistributionSet}.
     * <p>
     * {@link #VISIBLE_SM_MD_VALUE}, {@link #VISIBLE_SM_MD_KEY} with
     * {@link SoftwareModuleMetadata#isTargetVisible()} and
     * {@link #INVISIBLE_SM_MD_KEY}, {@link #INVISIBLE_SM_MD_VALUE} without
     * {@link SoftwareModuleMetadata#isTargetVisible()}
     *
     * @param set to add metadata to
     */
    public void addSoftwareModuleMetadata(final DistributionSet set) {
        set.getModules().forEach(this::addTestModuleMetadata);
    }

    private void addTestModuleMetadata(final SoftwareModule module) {
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                .key(VISIBLE_SM_MD_KEY).value(VISIBLE_SM_MD_VALUE).targetVisible(true));
        softwareModuleManagement.createMetaData(entityFactory.softwareModuleMetadata().create(module.getId())
                .key(INVISIBLE_SM_MD_KEY).value(INVISIBLE_SM_MD_VALUE).targetVisible(false));

    }

    /**
     * Creates {@link DistributionSet} in repository.
     *
     * @param prefix                  for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *                                vendor and description.
     * @param version                 {@link DistributionSet#getVersion()} and
     *                                {@link SoftwareModule#getVersion()} extended by a random
     *                                number.
     * @param isRequiredMigrationStep for {@link DistributionSet#isRequiredMigrationStep()}
     * @param modules                 for {@link DistributionSet#getModules()}
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version,
                                                 final boolean isRequiredMigrationStep, final Collection<SoftwareModule> modules) {

        HashMap<Long, Long> map = new HashMap<>();
        for (SoftwareModule sm : modules) {
            Version v = createVersion(sm.getId(), SAMPLE_NAME + getRandomInt());
            map.put(sm.getId(), v.getId());

        }

        return distributionSetManagement.create(
                entityFactory.distributionSet().create().name(prefix != null && prefix.length() > 0 ? prefix : "DS")
                        .version(version).description(randomDescriptionShort()).type(findOrCreateDefaultTestDsType())
                        .modules(map)
                        .requiredMigrationStep(isRequiredMigrationStep));
    }

    /**
     * Creates {@link DistributionSet} in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP}.
     *
     * @param prefix  for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *                vendor and description.
     * @param version {@link DistributionSet#getVersion()} and
     *                {@link SoftwareModule#getVersion()} extended by a random
     *                number.updat
     * @return {@link DistributionSet} entity.
     */
    public DistributionSet createDistributionSet(final String prefix, final String version,
                                                 final Collection<DistributionSetTag> tags) {

        final DistributionSet set = createDistributionSet(prefix, version, false, false);

        tags.forEach(tag -> distributionSetManagement.toggleTagAssignment(Collections.singletonList(set.getId()), tag.getName()));

        return distributionSetManagement.get(set.getId()).orElse(null);

    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     *
     * @param number of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final int number) {

        return createDistributionSets("", number);
    }

    /**
     * Create a list of {@link DistributionSet}s without modules, i.e.
     * incomplete.
     *
     * @param number of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSetsWithoutModules(final int number) {

        final List<DistributionSet> sets = Lists.newArrayListWithExpectedSize(number);
        for (int i = 0; i < number; i++) {
            sets.add(distributionSetManagement
                    .create(entityFactory.distributionSet().create().name("DS" + i).version(DEFAULT_VERSION + "." + i)
                            .description(randomDescriptionShort()).type(findOrCreateDefaultTestDsType())));
        }

        return sets;
    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     *
     * @param prefix for {@link SoftwareModule}s and {@link DistributionSet}s name,
     *               vendor and description.
     * @param number of {@link DistributionSet}s to create
     * @return {@link List} of {@link DistributionSet} entities
     */
    public List<DistributionSet> createDistributionSets(final String prefix, final int number) {

        final List<DistributionSet> sets = new ArrayList<>();
        for (int i = 0; i < number; i++) {
            sets.add(createDistributionSet(prefix + i, DEFAULT_VERSION + "." + i, false, false));
        }

        return sets;
    }

    /**
     * Creates {@link DistributionSet}s in repository with
     * {@link #DEFAULT_DESCRIPTION} and
     * {@link DistributionSet#isRequiredMigrationStep()} <code>false</code>.
     *
     * @param name    {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @return {@link DistributionSet} entity
     */
    public DistributionSet createDistributionSetWithNoSoftwareModules(final String name, final String version) {

        return distributionSetManagement.create(entityFactory.distributionSet().create().name(name).version(version)
                .description(DEFAULT_DESCRIPTION).type(findOrCreateDefaultTestDsType()));
    }

    /**
     * Creates {@link SoftwareModule} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param typeKey of the {@link SoftwareModuleType}
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModule(final String typeKey) {
        return createSoftwareModule(typeKey, "", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);
    }

    /**
     * Creates an {@link Version} with default vendor and version values,
     * and a randomly generated description in the repository.
     *
     * @param softwareModule the {@link SoftwareModule} instance to be associated
     * @return the persisted {@link Version}
     */
    public Version createVersionForSoftwareModule(SoftwareModule softwareModule) {
        return versionManagement.create(entityFactory.version().create().name("name").softwareModuleId(softwareModule.getId()).description(DESCRIPTION).number(1));
    }

    /**
     * Saves the specified {@link ArtifactSoftwareModuleAssociation} set with default vendor and version values,
     * and a randomly generated description in the repository.
     *
     * @param softwareModule the set of {@link ArtifactSoftwareModuleAssociation} instances to be saved
     */
    public void saveArtifactSoftwareModuleAssociation(Set<ArtifactSoftwareModuleAssociation> softwareModule) {
        saveArtifactSMAssociation(softwareModule);
    }

    /**
     * Saves the specified set of {@link ArtifactSoftwareModuleAssociation} instances with default vendor and version values,
     * and a randomly generated description in the repository.
     *
     * @param softwareModule the set of {@link ArtifactSoftwareModuleAssociation} instances to be saved
     */
    public void saveArtifactSMAssociation(Set<ArtifactSoftwareModuleAssociation> softwareModule) {
        artifactsManagement.createOrUpdateArtifactSoftwareModuleAssociation(softwareModule);
    }

    /**
     * Creates {@link SoftwareModule} of type
     * {@value Constants#SMT_DEFAULT_APP_KEY} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleApp() {
        return createSoftwareModule(Constants.SMT_DEFAULT_APP_KEY, "", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);
    }

    /**
     * Creates {@link SoftwareModule} of type
     * {@value Constants#SMT_DEFAULT_APP_KEY} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param prefix added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleApp(final String prefix) {
        return createSoftwareModule(Constants.SMT_DEFAULT_APP_KEY, prefix, false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);
    }

    /**
     * Creates {@link SoftwareModule} of type
     * {@value Constants#SMT_DEFAULT_OS_KEY} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleOs() {
        return createSoftwareModule(Constants.SMT_DEFAULT_OS_KEY, "", false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);
    }

    /**
     * Creates {@link SoftwareModule} of type
     * {@value Constants#SMT_DEFAULT_OS_KEY} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param prefix added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModuleOs(final String prefix) {
        return createSoftwareModule(Constants.SMT_DEFAULT_OS_KEY, prefix, false, Constants.SMF_DEFAULT_OS_KEY, SM_INSTALLER_TYPE_0);
    }

    /**
     * Creates {@link SoftwareModule} with {@link #DEFAULT_VENDOR} and
     * {@link #DEFAULT_VERSION} and random generated
     * {@link Target#getDescription()} in the repository.
     *
     * @param typeKey of the {@link SoftwareModuleType}
     * @param prefix  added to name and version
     * @return persisted {@link SoftwareModule}.
     */
    public SoftwareModule createSoftwareModule(final String typeKey, final String prefix, final boolean encrypted,
                                               final String formatKey, final String installerTypeName) {
        return softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(findOrCreateSoftwareModuleType(typeKey)).name(prefix + typeKey).version(prefix + DEFAULT_VERSION)
                .description(randomDescriptionShort()).vendor(DEFAULT_VENDOR).encrypted(encrypted)
                .format(findOrCreateSoftwareModuleFormat(formatKey)).swInstallerType(findSoftwareInstallerTypeByName(installerTypeName)));
    }

    /**
     * @return persisted {@link Target} with {@link #DEFAULT_CONTROLLER_ID}.
     */
    public Target createTarget() {
        return createTarget(DEFAULT_CONTROLLER_ID);
    }

    /**
     * @param controllerId of the target
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId) {
        return createTarget(controllerId, controllerId);
    }

    /**
     * @param controllerId of the target
     * @param targetName   name of the target
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId, final String targetName) {
        return createTarget(controllerId, targetName, controllerId, createVehicle("STLA" + getRandomInt()).getId(),randomText(14));
    }

    /**
     * @param controllerId of the target
     * @param targetName   name of the target
     * @param serialNumber serialNumber of the target
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId, final String targetName, final String serialNumber, final Long vehicleModelId) {
        final Target target = targetManagement
                .create(entityFactory.target().create().controllerId(controllerId).name(targetName).serialNumber(serialNumber).vehicleModelId(vehicleModelId)
                        .vin(randomText(14)));
        assertTargetProperlyCreated(target);
        return target;
    }

    public Target createTarget(final String controllerId, final String targetName, final String serialNumber, final Long vehicleModelId, final String vin) {
        final Target target = targetManagement
                .create(entityFactory.target().create().controllerId(controllerId).name(targetName).serialNumber(serialNumber).vehicleModelId(vehicleModelId).vin(vin));
        assertTargetProperlyCreated(target);
        return target;
    }

    /**
     * @param controllerId of the target
     * @param targetName   name of the target
     * @param targetTypeId target type id
     * @return persisted {@link Target}
     */
    public Target createTarget(final String controllerId, final String targetName, final String serialNumber, final Long targetTypeId, final Long vehicleModelId, final String vin) {
        final Target target = targetManagement.create(
                entityFactory.target().create().controllerId(controllerId).name(targetName).serialNumber(serialNumber).targetType(targetTypeId).vehicleModelId(vehicleModelId).vin(vin));
        assertTargetProperlyCreated(target);
        return target;
    }

    private void assertTargetProperlyCreated(final Target target) {
        assertThat(target.getCreatedBy()).isNotNull();
        assertThat(target.getCreatedAt()).isNotNull();
        assertThat(target.getLastModifiedBy()).isNotNull();
        assertThat(target.getLastModifiedAt()).isNotNull();

        assertThat(target.getUpdateStatus()).isEqualTo(TargetUpdateStatus.UNKNOWN);
    }

    private void assertPollingProperlyCreated(final Polling polling) {
        assertThat(polling.getCreatedBy()).isNotNull();
        assertThat(polling.getCreatedAt()).isNotNull();
        assertThat(polling.getLastModifiedBy()).isNotNull();
        assertThat(polling.getLastModifiedAt()).isNotNull();
    }

    private void assertArtifactsProperlyCreated(final Artifacts artifacts) {
        assertThat(artifacts.getCreatedBy()).isNotNull();
        assertThat(artifacts.getCreatedAt()).isNotNull();
        assertThat(artifacts.getLastModifiedBy()).isNotNull();
        assertThat(artifacts.getLastModifiedAt()).isNotNull();
    }

    /**
     * Creates {@link DistributionSet}s in repository including three
     * {@link SoftwareModule}s of types {@link #SM_TYPE_OS}, {@link #SM_TYPE_RT}
     * , {@link #SM_TYPE_APP} with {@link #DEFAULT_VERSION} followed by an
     * iterative number and {@link DistributionSet#isRequiredMigrationStep()}
     * <code>false</code>.
     * <p>
     * In addition it updates the created {@link DistributionSet}s and
     * {@link SoftwareModule}s to ensure that
     * {@link BaseEntity#getLastModifiedAt()} and
     * {@link BaseEntity#getLastModifiedBy()} is filled.
     *
     * @return persisted {@link DistributionSet}.
     */
    public DistributionSet createUpdatedDistributionSet() {
        DistributionSet set = createDistributionSet("");
        set = distributionSetManagement.update(
                entityFactory.distributionSet().update(set.getId()).description("Updated " + DEFAULT_DESCRIPTION));

        set.getModules().forEach(module -> softwareModuleManagement.update(
                entityFactory.softwareModule().update(module.getId()).description("Updated " + DEFAULT_DESCRIPTION)));

        // load also lazy stuff
        return distributionSetManagement.getWithDetails(set.getId()).orElse(null);
    }

    /**
     * @return {@link DistributionSetType} with key {@link #DS_TYPE_DEFAULT} and
     * {@link SoftwareModuleType}s {@link #SM_TYPE_OS},
     * {@link #SM_TYPE_RT} , {@link #SM_TYPE_APP}.
     */
    public DistributionSetType findOrCreateDefaultTestDsType() {
        final List<SoftwareModuleType> mand = new ArrayList<>();
        mand.add(findOrCreateSoftwareModuleType(SM_TYPE_OS));

        final List<SoftwareModuleType> opt = new ArrayList<>();
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE));
        opt.add(findOrCreateSoftwareModuleType(SM_TYPE_RT));

        return findOrCreateDistributionSetType(DS_TYPE_DEFAULT, "OS (FW) mandatory, runtime (FW) and app (SW) optional",
                mand, opt);
    }

    /**
     * Creates {@link DistributionSetType} in repository.
     *
     * @param dsTypeKey  {@link DistributionSetType#getKey()}
     * @param dsTypeName {@link DistributionSetType#getName()}
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName) {
        return distributionSetTypeManagement.getByKey(dsTypeKey)
                .orElseGet(() -> distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                        .key(dsTypeKey).name(dsTypeName).description(randomDescriptionShort()).colour("black")));
    }

    /**
     * Finds {@link DistributionSetType} in repository with given
     * {@link DistributionSetType#getKey()} or creates if it does not exist yet.
     *
     * @param dsTypeKey  {@link DistributionSetType#getKey()}
     * @param dsTypeName {@link DistributionSetType#getName()}
     * @param mandatory  {@link DistributionSetType#getMandatoryModuleTypes()}
     * @param optional   {@link DistributionSetType#getOptionalModuleTypes()}
     * @return persisted {@link DistributionSetType}
     */
    public DistributionSetType findOrCreateDistributionSetType(final String dsTypeKey, final String dsTypeName,
                                                               final Collection<SoftwareModuleType> mandatory, final Collection<SoftwareModuleType> optional) {
        return distributionSetTypeManagement.getByKey(dsTypeKey)
                .orElseGet(() -> distributionSetTypeManagement.create(entityFactory.distributionSetType().create()
                        .key(dsTypeKey).name(dsTypeName).description(randomDescriptionShort()).colour("black")
                        .optional(optional.stream().map(SoftwareModuleType::getId).toList())
                        .mandatory(mandatory.stream().map(SoftwareModuleType::getId).toList())));
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given
     * {@link SoftwareModuleType#getKey()} or creates if it does not exist yet
     * with {@link SoftwareModuleType#getMaxAssignments()} = 1.
     *
     * @param key {@link SoftwareModuleType#getKey()}
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key) {
        return findOrCreateSoftwareModuleType(key, 1);
    }

    /**
     * Finds {@link SoftwareModuleType} in repository with given
     * {@link SoftwareModuleType#getKey()} or creates if it does not exist yet.
     *
     * @param key            {@link SoftwareModuleType#getKey()}
     * @param maxAssignments {@link SoftwareModuleType#getMaxAssignments()}
     * @return persisted {@link SoftwareModuleType}
     */
    public SoftwareModuleType findOrCreateSoftwareModuleType(final String key, final Integer maxAssignments) {
        return softwareModuleTypeManagement.getByKey(key)
                .orElseGet(() -> softwareModuleTypeManagement.create(entityFactory.softwareModuleType().create()
                        .key(key).name(key).description(randomDescriptionShort()).colour("#ffffff")
                        .maxAssignments(maxAssignments)));
    }

    public SoftwareModuleFormat findOrCreateSoftwareModuleFormat(final String key) {
        return softwareModuleFormatManagement.getByKey(key)
                .orElseGet(() -> softwareModuleFormatManagement.create(entityFactory.softwareModuleFormat().create()
                        .key(key).name(key).description(randomDescriptionShort())));
    }

    /**
     * Finds {@link SoftwareInstallerType} in repository with given
     * {@link SoftwareInstallerType#getName()}
     *
     * @param name {@link SoftwareInstallerType#getName()}
     * @return persisted {@link SoftwareInstallerType}
     */
    public SoftwareInstallerType findSoftwareInstallerTypeByName(final String name) {
        return softwareInstallerTypeManagement.getSwInstallerTypeByName(name);
    }

    /**
     * Creates a {@link DistributionSet}.
     *
     * @param name    {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @param type    {@link DistributionSet#getType()}
     * @param modules {@link DistributionSet#getModules()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSet createDistributionSet(final String name, final String version,
                                                 final DistributionSetType type, final Collection<SoftwareModule> modules) {

        HashMap<Long, Long> map = new HashMap<>();
        for (SoftwareModule sm : modules) {
            Version v = createVersion(sm.getId(), SAMPLE_NAME + getRandomInt());
            map.put(sm.getId(), v.getId());

        }

        return distributionSetManagement.create(entityFactory.distributionSet().create().name(name).version(version)
                .description(randomDescriptionShort()).type(type)
                .modules(map));
    }

    /**
     * Generates {@link DistributionSet} object without persisting it.
     *
     * @param name                  {@link DistributionSet#getName()}
     * @param version               {@link DistributionSet#getVersion()}
     * @param type                  {@link DistributionSet#getType()}
     * @param modules               {@link DistributionSet#getModules()}
     * @param requiredMigrationStep {@link DistributionSet#isRequiredMigrationStep()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSet generateDistributionSet(final String name, final String version,
                                                   final DistributionSetType type, final Collection<SoftwareModule> modules,
                                                   final boolean requiredMigrationStep) {
        HashMap<Long, Long> map = new HashMap<>();
        for (SoftwareModule sm : modules) {
            Version v = createVersion(sm.getId(), SAMPLE_NAME + getRandomInt());
            map.put(sm.getId(), v.getId());

        }
        return entityFactory.distributionSet().create().name(name).version(version)
                .description(randomDescriptionShort()).type(type)
                .modules(map)
                .requiredMigrationStep(requiredMigrationStep)
                .softwareDowngradeEnabled(false)
                .build();
    }

    /**
     * Generates {@link DistributionSet} object without persisting it.
     *
     * @param name    {@link DistributionSet#getName()}
     * @param version {@link DistributionSet#getVersion()}
     * @param type    {@link DistributionSet#getType()}
     * @param modules {@link DistributionSet#getModules()}
     * @return the created {@link DistributionSet}
     */
    public DistributionSet generateDistributionSet(final String name, final String version,
                                                   final DistributionSetType type, final Collection<SoftwareModule> modules) {
        return generateDistributionSet(name, version, type, modules, false);
    }

    /**
     * builder method for generating a {@link DistributionSet}.
     *
     * @param name {@link DistributionSet#getName()}
     * @return the generated {@link DistributionSet}
     */
    public DistributionSet generateDistributionSet(final String name) {
        return generateDistributionSet(name, DEFAULT_VERSION, findOrCreateDefaultTestDsType(), Collections.emptyList(),
                false);
    }

    /**
     * Creates {@link Target}s in repository and with
     * {@link #DEFAULT_CONTROLLER_ID} as prefix for
     * {@link Target#getControllerId()}.
     *
     * @param number of {@link Target}s to create
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargets(final int number) {
        return createTargets(DEFAULT_CONTROLLER_ID, number);
    }

    public List<Target> createTargets(final String prefix, final int number) {

        final List<TargetCreate> targets = Lists.newArrayListWithExpectedSize(number);
        for (int i = 0; i < number; i++) {
            targets.add(entityFactory.target().create().name(prefix + i).controllerId(prefix + i)
                    .serialNumber(prefix + i).vehicleModelId(createVehicle(prefix + i).getId()).vin(randomText(14)));
        }

        return createTargets(targets);
    }

    /**
     * Creates {@link Target}s in repository and with {@link TargetType}.
     *
     * @param number             of {@link Target}s to create
     * @param controllerIdPrefix prefix for the controller id
     * @param targetType         targetType of targets to create
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargetsWithType(final int number, final String controllerIdPrefix,
                                              final TargetType targetType) {

        final List<TargetCreate> targets = Lists.newArrayListWithExpectedSize(number);
        for (int i = 0; i < number; i++) {
            targets.add(entityFactory.target().create().name(controllerIdPrefix + i).controllerId(controllerIdPrefix + i)
                    .serialNumber(controllerIdPrefix + i).targetType(targetType.getId()).vehicleModelId(createVehicle(controllerIdPrefix + i).getId()).vin(randomText(14)));
        }

        return createTargets(targets);
    }

    /**
     * Creates {@link Target}s in repository and with given targetIds.
     *
     * @param targetIds specifies the IDs of the targets
     * @return {@link List} of {@link Target} entities
     */
    public List<Target> createTargets(final String... targetIds) {

        final List<TargetCreate> targets = new ArrayList<>();
        for (final String targetId : targetIds) {
            targets.add(entityFactory.target().create().name(targetId).controllerId(targetId).serialNumber(targetId).vehicleModelId(createVehicle(targetId).getId()).vin(randomText(14)));
        }

        return createTargets(targets);
    }

    /**
     * Builds {@link Target} objects with given prefix for
     * {@link Target#getControllerId()} followed by a number suffix.
     *
     * @param start              value for the controllerId suffix
     * @param numberOfTargets    of {@link Target}s to generate
     * @param controllerIdPrefix for {@link Target#getControllerId()} generation.
     * @return list of {@link Target} objects
     */
    private List<Target> generateTargets(final int start, final int numberOfTargets, final String controllerIdPrefix) {
        final List<Target> targets = Lists.newArrayListWithExpectedSize(numberOfTargets);
        for (int i = start; i < start + numberOfTargets; i++) {
            targets.add(entityFactory.target().create().controllerId(controllerIdPrefix + i).build());
        }

        return targets;
    }

    /**
     * Builds {@link Target} objects with given prefix for
     * {@link Target#getControllerId()} followed by a number suffix starting
     * with 0.
     *
     * @param numberOfTargets    of {@link Target}s to generate
     * @param controllerIdPrefix for {@link Target#getControllerId()} generation.
     * @return list of {@link Target} objects
     */
    public List<Target> generateTargets(final int numberOfTargets, final String controllerIdPrefix) {
        return generateTargets(0, numberOfTargets, controllerIdPrefix);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets number of targets to create
     * @param prefix          prefix used for the controller ID and description
     * @return set of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String prefix) {
        return createTargets(numberOfTargets, prefix, prefix);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets    number of targets to create
     * @param controllerIdPrefix prefix used for the controller ID
     * @param descriptionPrefix  prefix used for the description
     * @return set of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String controllerIdPrefix,
                                      final String descriptionPrefix) {

        final List<TargetCreate> targets = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> entityFactory.target().create()
                        .name(controllerIdPrefix + i)
                        .controllerId(String.format(CONTROLLER_ID_FORMATER, controllerIdPrefix, i))
                        .serialNumber(controllerIdPrefix + i)
                        .description(descriptionPrefix + i)
                        .vehicleModelId(createVehicle(controllerIdPrefix + i).getId())
                        .vin(randomText(14)))
                .toList();
        return createTargets(targets);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets    number of targets to create
     * @param controllerIdPrefix prefix used for the controller ID
     * @param name               name of target
     * @param serialNumber       serialNumber of target
     * @param descriptionPrefix  prefix used for the description
     * @return set of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String controllerIdPrefix, String name, String serialNumber,
                                      final String descriptionPrefix) {

        final List<TargetCreate> targets = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> entityFactory.target().create()
                        .name(name + i)
                        .controllerId(String.format(CONTROLLER_ID_FORMATER, controllerIdPrefix, i))
                        .serialNumber(serialNumber + i)
                        .description(descriptionPrefix + i)
                        .vehicleModelId(createVehicle(name + i).getId()).vin(randomText(14)))
                .toList();
        return createTargets(targets);
    }

    private List<Target> createTargets(final Collection<TargetCreate> targetCreates) {
        // init new instance of array list since the TargetManagement#create
        // will
        // provide a unmodifiable list
        final List<Target> createdTargets = targetManagement.create(targetCreates);
        return new ArrayList<>(createdTargets);
    }

    /**
     * builds a set of {@link Target} fixtures from the given parameters.
     *
     * @param numberOfTargets    number of targets to create
     * @param controllerIdPrefix prefix used for the controller ID
     * @param descriptionPrefix  prefix used for the description
     * @param lastTargetQuery    last time the target polled
     * @return set of {@link Target}
     */
    public List<Target> createTargets(final int numberOfTargets, final String controllerIdPrefix,
                                      final String descriptionPrefix, final Long lastTargetQuery) {

        final List<TargetCreate> targets = IntStream.range(0, numberOfTargets)
                .mapToObj(i -> entityFactory.target().create()
                        .name(String.format(CONTROLLER_ID_FORMATER, controllerIdPrefix, i))
                        .controllerId(String.format(CONTROLLER_ID_FORMATER, controllerIdPrefix, i))
                        .serialNumber(String.format(CONTROLLER_ID_FORMATER, controllerIdPrefix, i))
                        .description(descriptionPrefix + i).lastTargetQuery(lastTargetQuery)
                        .vehicleModelId(createVehicle(controllerIdPrefix + i).getId())
                        .vin(randomText(14))
                )
                .toList();
        return createTargets(targets);
    }

    /**
     * Starting from a list of {@link Target} sets their polling status as polled.
     *
     * @param targetCreates targets to poll
     * @return set of {@link Polling}
     * Deprecated this method as part of removing sp_polling and its references
     */
    @Deprecated
    public List<Polling> addPolling(final Collection<Target> targetCreates) {
        List<Polling> pollings = new ArrayList<>();
        for (Target target : targetCreates) {
            Polling elem = controllerManagament.setPolling(target, Polling.Status.POLLED);
            pollings.add(elem);
        }
        return pollings;
    }

    /**
     * Create a set of {@link TargetTag}s.
     *
     * @param number    number of {@link TargetTag}. to be created
     * @param tagPrefix prefix for the {@link TargetTag#getName()}
     * @return the created set of {@link TargetTag}s
     */
    public List<TargetTag> createTargetTags(final int number, final String tagPrefix) {
        final List<TagCreate> result = Lists.newArrayListWithExpectedSize(number);

        for (int i = 0; i < number; i++) {
            result.add(entityFactory.tag().create().name(tagPrefix + i).description(tagPrefix + i)
                    .colour(String.valueOf(i)));
        }

        return targetTagManagement.create(result);
    }

    /**
     * Creates {@link DistributionSetTag}s in repository.
     *
     * @param number of {@link DistributionSetTag}s
     * @return the persisted {@link DistributionSetTag}s
     */
    public List<DistributionSetTag> createDistributionSetTags(final int number) {
        final List<TagCreate> result = Lists.newArrayListWithExpectedSize(number);

        for (int i = 0; i < number; i++) {
            result.add(
                    entityFactory.tag().create().name("tag" + i).description("tagdesc" + i).colour(String.valueOf(i)));
        }

        return distributionSetTagManagement.create(result);
    }

    private Action sendUpdateActionStatusToTarget(final DeviceActionStatus status, final Action updActA,
                                                  final Collection<String> msgs) {

        return controllerManagament.addUpdateActionStatus(
                entityFactory.actionStatus().create(updActA.getId()).status(status).messages(msgs), null);
    }

    /**
     * Append {@link ActionStatus} to all {@link Action}s of given
     * {@link Target}s.
     *
     * @param targets to add {@link ActionStatus}
     * @param status  to add
     * @param message to add
     * @return updated {@link Action}.
     */
    public List<Action> sendUpdateActionStatusToTargets(final Collection<Target> targets, final DeviceActionStatus status,
                                                        final String message) {
        return sendUpdateActionStatusToTargets(targets, status, Collections.singletonList(message));
    }

    /**
     * Append {@link ActionStatus} to all {@link Action}s of given
     * {@link Target}s.
     *
     * @param targets to add {@link ActionStatus}
     * @param status  to add
     * @param msgs    to add
     * @return updated {@link Action}.
     */
    public List<Action> sendUpdateActionStatusToTargets(final Collection<Target> targets, final DeviceActionStatus status,
                                                        final Collection<String> msgs) {
        final List<Action> result = new ArrayList<>();
        for (final Target target : targets) {
            final List<Action> findByTarget = deploymentManagement
                    .findActionsByTarget(target.getControllerId(), PageRequest.of(0, 400)).getContent();
            for (final Action action : findByTarget) {
                result.add(sendUpdateActionStatusToTarget(status, action, msgs));
            }
        }
        return result;
    }

    /**
     * Create {@link Rollout} with a new {@link DistributionSet} and
     * {@link Target}s.
     *
     * @return created {@link Rollout}
     */
    public Rollout createAndStartRollout(Rollout rollout) {
        rollout = addNewRollout(rollout);
        rolloutHandler.handleAll();
        rollout = reloadRollout(rollout);
        return startAndReloadRollout(rollout);
    }

    private Rollout startAndReloadRollout(final Rollout rollout) {
        rolloutManagement.freeze(rollout.getId());
        rolloutHandler.handleAll();
        rolloutManagement.start(rollout.getId());
        rolloutHandler.handleAll();

        return reloadRollout(rollout);
    }

    private Rollout reloadRollout(final Rollout rollout) {
        return rolloutManagement.get(rollout.getId()).orElseThrow(NoSuchElementException::new);
    }

    /**
     * Finds {@link TargetType} in repository with given
     * {@link TargetType#getName()} or creates if it does not exist yet. No ds
     * types are assigned on creation.
     *
     * @param targetTypeName {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public TargetType findOrCreateTargetType(final String targetTypeName) {
        return targetTypeManagement.getByName(targetTypeName)
                .orElseGet(() -> targetTypeManagement.create(entityFactory.targetType().create().name(targetTypeName)
                        .description(targetTypeName + DESCRIPTION).colour(DEFAULT_COLOUR)));
    }

    /**
     * Creates {@link TargetType} in repository with given
     * {@link TargetType#getName()}. Compatible distribution set types are
     * assigned on creation
     *
     * @param targetTypeName {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public TargetType createTargetType(final String targetTypeName, final List<DistributionSetType> compatibleDsTypes) {
        return targetTypeManagement.create(entityFactory.targetType().create().name(targetTypeName)
                .description(targetTypeName + DESCRIPTION).colour(DEFAULT_COLOUR)
                .compatible(compatibleDsTypes.stream().map(DistributionSetType::getId).toList()));
    }

    /**
     * Creates {@link Vehicle} in repository with given
     * {@link Vehicle#getId()}.
     * assigned on creation
     *
     * @param vehicleModelName {@link Vehicle#getId()}
     * @return persisted {@link Vehicle}
     */
    public Vehicle createVehicle(final String vehicleModelName) {
        return vehicleManagement.create(entityFactory.vehicle().create().name(vehicleModelName));
    }

    /**
     * Creates {@link TargetType} in repository with given
     * {@link TargetType#getName()}. No ds types are assigned on creation.
     *
     * @param targetTypePrefix {@link TargetType#getName()}
     * @return persisted {@link TargetType}
     */
    public List<TargetType> createTargetTypes(final String targetTypePrefix, final int count) {
        final List<TargetTypeCreate> result = Lists.newArrayListWithExpectedSize(count);
        for (int i = 0; i < count; i++) {
            result.add(entityFactory.targetType().create().name(targetTypePrefix + i)
                    .description(targetTypePrefix + DESCRIPTION).colour(DEFAULT_COLOUR));
        }
        return targetTypeManagement.create(result);
    }

    /**
     * Creates a distribution set and directly invalidates it. No actions will
     * be canceled and no rollouts will be stopped with this invalidation.
     *
     * @return created invalidated {@link DistributionSet}
     */
    public DistributionSet createAndInvalidateDistributionSet() {
        final DistributionSet distributionSet = createDistributionSet();
        distributionSetInvalidationManagement.invalidateDistributionSet(
                new DistributionSetInvalidation(Collections.singletonList(distributionSet.getId()), CancelationType.NONE, false), 1L);
        return distributionSet;
    }

    /**
     * Creates a distribution set that has no software modules assigned, so it
     * is incomplete.
     *
     * @return created incomplete {@link DistributionSet}
     */
    public DistributionSet createIncompleteDistributionSet() {
        return distributionSetManagement.create(entityFactory.distributionSet().create()
                .name(UUID.randomUUID().toString()).version(DEFAULT_VERSION).description(randomDescriptionShort())
                .type(findOrCreateDefaultTestDsType()).requiredMigrationStep(false));
    }

    public Version createVersion(long softwareModuleIda, String name) {
        return versionManagement.create(entityFactory.version().create()
                .name(name)
                .description("test")
                .softwareModuleId(softwareModuleIda)
                .number(random.nextInt(3000)));
    }

    public Version createVersion(long softwareModuleIda, String name, int number) {
        return versionManagement.create(entityFactory.version().create()
                .name(name)
                .description("test")
                .softwareModuleId(softwareModuleIda)
                .number(number));
    }

    public List<Version> getVersionsBySoftware(SoftwareModule software) {
        return versionManagement.findVersionBySoftware(software);
    }

    public List<Vehicle> addNewVehicleModels(List<Vehicle> vehicleList) {
        return vehicleManagement.create(vehicleList);
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleManagement.get(id);

    }

    public List<EcuModel> addNewEcuModels(List<EcuModel> ecuModelList) {
        return ecuModelManagement.create(ecuModelList);
    }

    public Rollout addNewRollout(Rollout rollout) {
        Rollout savedRollout = rolloutManagement.create(rollout);
        rolloutHandler.handleAll();
        return reloadRollout(savedRollout);
    }

    public Optional<EcuModel> getEcuModelById(Long id) {
        return ecuModelManagement.get(id);

    }

    public void deleteAllEcuModels() {
        ecuModelManagement.deleteAll();
    }

    public void assignEcuSoftwareModules(Long softwareModuleId, List<Long> ecuModelIds) {
        softwareModuleManagement.assignEcuModel(softwareModuleId, ecuModelIds);
    }

    public void deleteAllVehicleModels() {
        vehicleManagement.deleteAll();
    }

    public int getRandomInt() {
        return this.random.nextInt();
    }

    /**
     * Get {@link Target} for given controllerId
     *
     * @param controllerId the controllerId of {@link Target}
     * @return {@link Target}
     */
    public Target getTarget(String controllerId) {
        return targetManagement.getByControllerID(controllerId).orElse(null);
    }

    /**
     * This method fetch device inventory details from sp_target_inventory table for given {@link Target}
     *
     * @param controllerId the controllerId of {@link Target}
     * @return {@link TargetInventory}
     */
    public List<TargetInventory> getTargetInventory(String controllerId) {
        return controllerManagament.getTargetInventory(controllerId);
    }

    public boolean vehicleEcuExists(Vehicle vehicleModelId, List<Long> ids) {
        return !ecuModelManagement.getAllEcuForVehicleModelId(vehicleModelId, ids).isEmpty();
    }

    /**
     * Creates a new Artifacts entity in the repository.
     *
     * @param fileName    The name of the file associated with the Artifacts.
     * @param fileType    The type of the file associated with the Artifacts.
     * @param description A brief description of the Artifacts.
     * @param fileSize    The size of the file associated with the Artifacts.
     * @param sha256Hash  The SHA-256 hash of the file associated with the Artifacts.
     * @return The newly created Artifacts entity.
     */
    public Artifacts createArtifacts(final String fileName, final FileType fileType, final String description, final String fileSize, final String sha256Hash) {
        final Artifacts artifacts = createArtifactsWithExpiryDate(fileName, fileType, description, fileSize, sha256Hash, Instant.now().plus(1, ChronoUnit.DAYS).getEpochSecond(), null);
        assertArtifactsProperlyCreated(artifacts);
        return artifacts;
    }

    /**
     * Creates a new association between an artifact and one or more software modules in the repository.
     *
     * @param iAssociationList A set of ArtifactSoftwareModuleAssociation entities to link to the artifact.
     */
    public void createArtifactsSoftwareModuleAssociation(Set<ArtifactSoftwareModuleAssociation> iAssociationList) {
        artifactsManagement.createOrUpdateArtifactSoftwareModuleAssociation(iAssociationList);

    }

    /**
     * Creates a new Artifacts entity in the repository with an expiry date.
     *
     * @param fileName    The name of the file associated with the Artifacts.
     * @param fileType    The type of the file associated with the Artifacts.
     * @param description A brief description of the Artifacts.
     * @param fileSize    The size of the file associated with the Artifacts.
     * @param sha256Hash  The SHA-256 hash of the file associated with the Artifacts.
     * @param expiryDate  The expiry date of the Artifacts.
     * @return The newly created Artifacts entity.
     * @throws IllegalArgumentException If the expiry date is in the past.
     */
    public Artifacts createArtifactsWithExpiryDate(final String fileName, final FileType fileType, final String description, final String fileSize, final String sha256Hash, final long expiryDate, String status) {

        final Artifacts artifacts = artifactsManagement
                .create(entityFactory.artifacts().create().fileName(fileName).fileType(fileType).description(description).expiryDate(expiryDate).fileSize(Long.valueOf(fileSize)).sha256Hash(sha256Hash).artifactStatus("ACTIVE").fileStatus(status));
        assertArtifactsProperlyCreated(artifacts);
        return artifacts;
    }

    /**
     * Create a {@link DistributionSet} with a {@link SoftwareModule} and {@link Version}.
     *
     * @param modulesTargetVersions      the software modules and their target versions.
     * @param isRequiredMigrationStep    the required migration step flag.
     * @param isSoftwareDowngradeEnabled the software downgrade enabled flag.
     * @return the created distribution set.
     */
    public DistributionSet createDistributionSetWithSwModuleAndTargetVersion(Map<Long, Long> modulesTargetVersions,
                                                                             final boolean isRequiredMigrationStep, final boolean isSoftwareDowngradeEnabled) {
        return distributionSetManagement.create(
                entityFactory.distributionSet().create().name("DS")
                        .version("3.0.2").description(randomDescriptionShort()).type(findOrCreateDefaultTestDsType())
                        .modules(modulesTargetVersions)
                        .requiredMigrationStep(isRequiredMigrationStep)
                        .softwareDowngradeEnabled(isSoftwareDowngradeEnabled));
    }

    public List<SoftwareModule> createSoftwareModulesForDD(final List<String> name, final String version) {

        final SoftwareModule appMod = softwareModuleManagement.create(entityFactory.softwareModule().create()
                .type(findOrCreateSoftwareModuleType(SM_TYPE_APP, Integer.MAX_VALUE))
                .name(name.get(0))
                .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                .version(version + "." + new SecureRandom().nextInt(100)).description(randomDescriptionLong())
                .vendor(" vendor Limited, California")
                .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));

        final SoftwareModule runtimeMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_RT))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(name.get(1)).version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(" vendor GmbH, Stuttgart, Germany")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));

        final SoftwareModule osMod = softwareModuleManagement
                .create(entityFactory.softwareModule().create().type(findOrCreateSoftwareModuleType(SM_TYPE_OS))
                        .format(findOrCreateSoftwareModuleFormat(SM_FORMAT_APP))
                        .name(name.get(2)).version(version + "." + new SecureRandom().nextInt(100))
                        .description(randomDescriptionLong()).vendor(" vendor Limited Inc, California")
                        .swInstallerType(findSoftwareInstallerTypeByName(SM_INSTALLER_TYPE_0)));
        return List.of(appMod, runtimeMod, osMod);
    }

    public DeploymentLog createDeploymentLog(String fileName, Action action) throws IOException {
        final MockMultipartFile file = new MockMultipartFile("file", "origFilename", null,
                RandomStringUtils.randomAlphanumeric(5 * 1024).getBytes());

        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(fileName)
                .fileOriginalName(file.getOriginalFilename())
                .action(action)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .sequence(1)
                .isLastFile(true)
                .build();
        return deploymentLogManagement.create(deploymentLogUpload, file);
    }

    public File createTempFileWithRandomContent(int sizeInBytes, String fileName) throws IOException {
        File tempFile = Files.createTempFile(fileName, ".txt").toFile();//NOSONAR

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            byte[] data = new byte[sizeInBytes];
            random.nextBytes(data);
            fos.write(data);
        }
        return tempFile;
    }

    public Version createDiffVersionForSoftwareModule(SoftwareModule softwareModule) {
        return versionManagement.create(entityFactory.version().create().name("name").softwareModuleId(softwareModule.getId()).description("description").number(new SecureRandom().nextInt(100)));
    }

    public DistributionSet createDistributionSetWithVersion(final String prefix, final boolean isRequiredMigrationStep, final boolean isSoftwareDowngradeEnabled) {
        return createDistributionSet(prefix, DEFAULT_VERSION, isRequiredMigrationStep, isSoftwareDowngradeEnabled);
    }

    public void addTenantConfigurations(final String key, String value) {
        tenantConfigurationManagement.addOrUpdateConfiguration(key, value);
    }

    public String getTenantConfigurations(final String key) {
        return tenantConfigurationManagement
                .getGlobalConfigurationValue(key, String.class);
    }

    public MgmtBaseSupportPackageCreateRequest createESPForRollout(List<String> vinList, MgmtSupportPackageFileType fileType) throws NoSuchAlgorithmException {
        Map<String, String> fileMetadata = Map.of("size", "20MB");
        String ecuNodeAddress = "30 A0";
        String sha256 = generateSha256Hash("sampledata");

        return MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl("https://example.com/new-file.pdf")
                .fileName("newFile.pdf")
                .fileType(fileType)
                .sha256(sha256)
                .fileVersion("1")
                .controllerIds(vinList)
                .ecuNodeAddress(ecuNodeAddress)
                .fileContentDescription("This file contains the latest firmware update.")
                .fileInfoUrl("https://example.com/release-notes")
                .fileMetadata(fileMetadata)
                .build();
    }

    public MgmtBaseSupportPackageCreateRequest createEspRspForRollout(List<String> vinList, MgmtSupportPackageFileType fileType, String ecuNodeAddress) throws NoSuchAlgorithmException {
        Map<String, String> fileMetadata = Map.of("size", "20MB");
        String sha256 = generateSha256Hash("sampledata" + randomText(2));

        return MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl("https://example.com/rspFile" + randomText(2) + ".pdf")
                .fileName("rspFile" + randomText(2) + ".pdf")
                .fileType(fileType)
                .sha256(sha256)
                .fileVersion("1" + getRandomInt())
                .controllerIds(vinList)
                .ecuNodeAddress(ecuNodeAddress)
                .fileContentDescription("This RSP file contains the latest firmware update.")
                .fileInfoUrl("https://example.com/release-notes")
                .fileMetadata(fileMetadata)
                .build();


    }

    public Long convertToBytes(String size) {
        // Remove non-numeric part (e.g., "MB")
        String numericPart = size.replaceAll("\\D", "");

        // Convert to long
        long value = Long.parseLong(numericPart);

        // Convert to bytes (assuming the input is in MB)
        return value * 1024 * 1024;
    }

    public MgmtRolloutRestRequestBody buildDefaultRolloutRequest(String rolloutName, MgmtRolloutStartType... startType) {
        MgmtRolloutRestRequestBody mgmtRolloutRestRequestBody = new MgmtRolloutRestRequestBody();
        mgmtRolloutRestRequestBody.setName(rolloutName);
        mgmtRolloutRestRequestBody.setDescription(DEFAULT_DESCRIPTION);
        mgmtRolloutRestRequestBody.setPriority(MgmtRolloutPriority.REGULAR);
        mgmtRolloutRestRequestBody.setStartType(startType.length > 0 ? startType[0] : MgmtRolloutStartType.SCHEDULED);
        mgmtRolloutRestRequestBody.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        mgmtRolloutRestRequestBody.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        mgmtRolloutRestRequestBody.setStartDate(mgmtRolloutRestRequestBody.getStartType().equals(MgmtRolloutStartType.SCHEDULED) ? VALID_ROLLOUT_START_DATE : null);
        mgmtRolloutRestRequestBody.setEndDate(VALID_ROLLOUT_END_DATE);
        MgmtRolloutLogRequest logRequest = MgmtRolloutLogRequest.builder().
                collectionRequired(DEFAULT_LOG_COLLECTION_REQUIRED).
                maxSuccessVin(DEFAULT_LOG_MAX_SUCCESS_VIN)
                .maxFailureVin(DEFAULT_LOG_MAX_FAILURE_VIN).
                maxNumberOfFiles(DEFAULT_LOG_MAX_NUMBER_OF_FILES).
                maxAllFileSize(DEFAULT_LOG_MAX_ALL_FILE_SIZE).
                maxEachFileSize(DEFAULT_LOG_MAX_EACH_FILE_SIZE).build();
        MgmtRolloutDeployment rolloutDeployment = MgmtRolloutDeployment.builder().downgradeAllowed(MgmtRolloutDowngradeAllowed.NO)
                .requiredMedia(MgmtRolloutRequiredMedia.FROM_CDN).estimatedUpdateTime(DEFAULT_DEPLOYMENT_ESTIMATED_UPDATE_TIME).requiredStateOfCharge(null).build();
        mgmtRolloutRestRequestBody.setLog(logRequest);
        mgmtRolloutRestRequestBody.setDeploymentMetadata(rolloutDeployment);
        mgmtRolloutRestRequestBody.setMaxDownloadDurationTimer(DEFAULT_MAX_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxDownloadWifiDurationTimer(DEFAULT_MAX_WIFI_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxDownloadCellularDurationTimer(DEFAULT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxUpdateTime(DEFAULT_MAX_UPDATE_TIME);
        mgmtRolloutRestRequestBody.setMaxPackageSize(DEFAULT_ROLLOUT_MAXIMUM_PACKAGE_SIZE);
        mgmtRolloutRestRequestBody.setDownloadRetryCount(DEFAULT_DOWNLOAD_RETRY_COUNT);
        mgmtRolloutRestRequestBody.setType(MgmtRolloutType.FOTA);
        return mgmtRolloutRestRequestBody;
    }

    public MgmtRolloutRestRequestBody buildRolloutWithCollectionRequiredRequest(String rolloutName) {
        MgmtRolloutRestRequestBody mgmtRolloutRestRequestBody = new MgmtRolloutRestRequestBody();
        mgmtRolloutRestRequestBody.setName(rolloutName);
        mgmtRolloutRestRequestBody.setDescription(DEFAULT_DESCRIPTION);
        mgmtRolloutRestRequestBody.setPriority(MgmtRolloutPriority.REGULAR);
        mgmtRolloutRestRequestBody.setStartType(MgmtRolloutStartType.MANUAL);
        mgmtRolloutRestRequestBody.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        mgmtRolloutRestRequestBody.setConnectivityType(MgmtRolloutConnectivityType.WIFI_PREFERRED);
        mgmtRolloutRestRequestBody.setStartDate(VALID_ROLLOUT_START_DATE);
        mgmtRolloutRestRequestBody.setEndDate(VALID_ROLLOUT_END_DATE);
        MgmtRolloutLogRequest logRequest = MgmtRolloutLogRequest.builder().
                collectionRequired(LOG_COLLECTION_REQUIRED).
                maxSuccessVin(LOG_MAX_SUCCESS_VIN)
                .maxFailureVin(LOG_MAX_FAILURE_VIN).
                maxNumberOfFiles(DEFAULT_LOG_MAX_NUMBER_OF_FILES).
                maxAllFileSize(DEFAULT_LOG_MAX_ALL_FILE_SIZE).
                maxEachFileSize(DEFAULT_LOG_MAX_EACH_FILE_SIZE).build();
        MgmtRolloutDeployment rolloutDeployment = MgmtRolloutDeployment.builder().downgradeAllowed(MgmtRolloutDowngradeAllowed.NO)
                .requiredMedia(MgmtRolloutRequiredMedia.FROM_CDN).requiredStateOfCharge(null).build();
        mgmtRolloutRestRequestBody.setLog(logRequest);
        mgmtRolloutRestRequestBody.setDeploymentMetadata(rolloutDeployment);
        mgmtRolloutRestRequestBody.setMaxDownloadDurationTimer(DEFAULT_MAX_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxDownloadWifiDurationTimer(DEFAULT_MAX_WIFI_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxDownloadCellularDurationTimer(DEFAULT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER);
        mgmtRolloutRestRequestBody.setMaxUpdateTime(DEFAULT_MAX_UPDATE_TIME);
        mgmtRolloutRestRequestBody.setDownloadRetryCount(DEFAULT_DOWNLOAD_RETRY_COUNT);
        return mgmtRolloutRestRequestBody;
    }

    public MgmtSoftwareModuleRequestBodyPost getRandomValidCreateSoftwareModuleRequestBody(SoftwareModuleType osType, SoftwareModuleFormat format, SoftwareInstallerType swInstallerType) {
        return getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType, "swModule-" + RandomStringUtils.randomAlphanumeric(5));

    }


    public MgmtSoftwareModuleRequestBodyPost getRandomValidCreateSoftwareModuleRequestBody(SoftwareModuleType osType, SoftwareModuleFormat format, SoftwareInstallerType swInstallerType, String name) {

        return getRandomValidCreateSoftwareModuleRequestBody(osType, format, swInstallerType,
                "swModule-" + RandomStringUtils.randomAlphanumeric(5),
                MgmtRestConstants.DEFAULT_REQUEST_BODY_SM_VERSION);

    }


    public MgmtSoftwareModuleRequestBodyPost getRandomValidCreateSoftwareModuleRequestBody(SoftwareModuleType osType, SoftwareModuleFormat format,
                                                                                           SoftwareInstallerType swInstallerType, String name,
                                                                                           String version) {
        return MgmtSoftwareModuleRequestBodyPost.builder()
                .type(osType.getName().toLowerCase())
                .vendor("vendor-" + RandomStringUtils.randomAlphanumeric(5))
                .name(name)
                .format(format.getName().toLowerCase())
                .version(version)
                .swInstallerType(swInstallerType.getName().toLowerCase())
                .encrypted(Boolean.FALSE)
                .build();
    }

    public MgmtAddVersionRequestBody getRandomMgmtAddVersionRequestBody() {
        return MgmtAddVersionRequestBody.builder()
                .name("versionName" + RandomStringUtils.randomAlphanumeric(6))
                .number((int) (Math.random() * 100))
                .build();

    }

    public MgmtArtifactsRequest getRandomValidCreateArtifactsRequestBody(String sha256, String fileUrl, String fileType, String fileName) {
        return MgmtArtifactsRequest.builder()
                .fileURL(fileUrl)
                .description("description")
                .filename(RandomStringUtils.randomAlphanumeric(7))
                .fileType(fileType)
                .sha256(sha256)
                .signatureExpiryDate(Instant.now().plus(10, ChronoUnit.DAYS).getEpochSecond())
                .build();
    }

    public MgmtSoftwareModuleRequest getAssociateSwModuleWithRolloutRequestBody(long moduleId, long versionId) {
        return new MgmtSoftwareModuleRequest(moduleId, versionId);
    }

    public MgmtBaseSupportPackageCreateRequest getCreateSupportPackageWithFileUrlRequestBody(MgmtSupportPackageFileType fileType, String ecuNodeId, String fileUrl, List<String> controllerIds) throws NoSuchAlgorithmException {

        return MgmtFileUrlSupportPackageCreateRequest.builder()
                .fileUrl(fileUrl)
                .fileName(RandomStringUtils.randomAlphabetic(5))
                .fileType(fileType)
                .sha256(generateSha256Hash(RandomStringUtils.randomAlphanumeric(12)))
                .fileVersion(RandomStringUtils.randomAlphanumeric(3))
                .controllerIds(controllerIds)
                .ecuNodeAddress(ecuNodeId)
                .fileContentDescription(RandomStringUtils.randomAlphanumeric(5))
                .fileInfoUrl("https://example.com/release-notes")
                .fileMetadata(Map.of("size", "20"))
                .build();
    }

    public @NotNull SupportPackageTestData getSupportPackageTestData(String targetPrefix, String rolloutPrefix, String rolloutName, String ecuNodeAddress, String distributionSet, String sha256, MgmtSupportPackageFileType fileType) throws NoSuchAlgorithmException {
        SupportPackageTestData testData = new SupportPackageTestData();
        testData.setTargetPrefix(targetPrefix);
        testData.setRolloutPrefix(rolloutPrefix);
        testData.setRolloutName(rolloutName);
        testData.setEcuNodeAddress(ecuNodeAddress);
        testData.setDistributionSetName(distributionSet);
        testData.setFileType(fileType);
        testData.setSha256(sha256);
        return testData;
    }

    public List<MgmtVehicleRequest> getAddVehicleModelRequestBody() {
        return List.of(MgmtVehicleRequest.builder()
                .name("vehicleModelName-" + RandomStringUtils.randomAlphanumeric(5))
                .build());
    }

    public List<MgmtEcuModelRequest> getCreateEcuModelRequestBody() {
        return List.of(MgmtEcuModelRequest.builder()
                .ecuModelName("ecuModelName" + RandomStringUtils.randomAlphanumeric(4))
                .ecuModelType("BT")
                .ecuNodeId("HeX" + RandomStringUtils.randomAlphanumeric(5))
                .build());
    }

    public List<EcuModelAssignmentRequest> getAssociateEcuModelAndVehicleRequestBody(List<Long> modelIds) {
        return modelIds.stream().map(modelId -> EcuModelAssignmentRequest.builder().ecuModelId(modelId).build()).toList();
    }

    public List<MgmtTargetRequestBody> getCreateTargetRequestBody(Long modelId, int totalTargets) {
        return IntStream.range(0, totalTargets).boxed().map(i -> {
            String serialNumber = "serial-" + RandomStringUtils.randomAlphanumeric(6);
            String vin = "vin-" + RandomStringUtils.randomAlphanumeric(6);
            return MgmtTargetRequestBody.builder()
                    .name("name-" + RandomStringUtils.randomAlphanumeric(5))
                    .serialNumber(serialNumber)
                    .description(RandomStringUtils.randomAlphanumeric(5))
                    .requestAttributes(false)
                    .vehicleModelId(modelId)
                    .vin(vin)
                    .build();
        }).toList();
    }

    @Getter
    @Setter
    public static class SupportPackageTestData {
        private String rolloutName = "newRollout";
        private int amountTargets = 10;
        private String targetPrefix = "newSupportPackageTarget";
        private String rolloutPrefix = "newRollout";
        private String distributionSetName = "newDistributionSet";
        private String testFileName = "newFile.pdf";
        private String fileUrl = "https://example.com/new-file.pdf";
        private String version = "2.0.0";
        private Map<String, String> fileMetadata = Map.of("size", "20MB");
        private MgmtSupportPackageFileType fileType = MgmtSupportPackageFileType.LICENSE;
        private String ecuNodeAddress = "30 A0";
        private String sha256 = generateSha256Hash(RandomStringUtils.randomAlphanumeric(4));

        public SupportPackageTestData() throws NoSuchAlgorithmException {
        }

    }


}