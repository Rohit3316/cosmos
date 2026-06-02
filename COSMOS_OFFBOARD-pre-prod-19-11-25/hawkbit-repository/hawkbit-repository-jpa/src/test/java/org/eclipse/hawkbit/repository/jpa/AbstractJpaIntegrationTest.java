/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetTagAssignmentResult;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetTypeAssignmentResult;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.eclipse.hawkbit.repository.test.util.AbstractIntegrationTest;
import org.eclipse.hawkbit.repository.test.util.RolloutTestApprovalStrategy;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.transaction.annotation.Transactional;

@ContextConfiguration(classes = {RepositoryApplicationConfiguration.class, TestConfiguration.class,
        TestChannelBinderConfiguration.class})
@TestPropertySource(locations = "classpath:/jpa-test.properties")
public abstract class AbstractJpaIntegrationTest extends AbstractIntegrationTest {

    protected static final String INVALID_TEXT_HTML = "</noscript><br><script>";
    protected static final String NOT_EXIST_ID = "12345678990";
    protected static final long NOT_EXIST_IDL = Long.parseLong(NOT_EXIST_ID);
    protected static final String SP_TARGET_FILTER_QUERY = "sp_target_filter_query";
    protected static final String SP_TARGET_TAG = "sp_target_tag";
    protected static final String SP_TARGET = "sp_target";
    protected static final String SP_VEHICLE_ECU = "sp_vehicle_ecu";
    protected static final String SP_VEHICLE_MODEL = "sp_vehicle_model";
    protected static final String SP_ACTION = "sp_action";
    protected static final String SP_ROLLOUTGROUP = "sp_rolloutgroup";
    protected static final String SP_ROLLOUT = "sp_rollout";
    protected static final String SP_DISTRIBUTION_SET = "sp_distribution_set";
    protected static final String SP_ARTIFACT_SOFTWARE_MODULE = "sp_artifact_software_module";
    protected static final String SP_SOFTWARE_VERSIONS = "sp_software_versions";
    protected static final String SP_BASE_SOFTWARE_MODULE = "sp_base_software_module";
    @PersistenceContext
    protected EntityManager entityManager;
    @Autowired
    protected ArtifactsManagement artifactsManagement;
    @Autowired
    protected TargetRepository targetRepository;
    @Autowired
    protected ActionRepository actionRepository;
    @Autowired
    protected DistributionSetRepository distributionSetRepository;
    @Autowired
    protected SoftwareModuleRepository softwareModuleRepository;
    @Autowired
    protected TenantMetaDataRepository tenantMetaDataRepository;
    @Autowired
    protected DistributionSetTypeRepository distributionSetTypeRepository;
    @Autowired
    protected SoftwareModuleTypeRepository softwareModuleTypeRepository;
    @Autowired
    protected TargetTagRepository targetTagRepository;
    @Autowired
    protected TargetTypeRepository targetTypeRepository;
    @Autowired
    protected DistributionSetTagRepository distributionSetTagRepository;
    @Autowired
    protected SoftwareModuleMetadataRepository softwareModuleMetadataRepository;
    @Autowired
    protected ActionStatusRepository actionStatusRepository;
    @Autowired
    protected ArtifactsRepository artifactsRepository;
    @Autowired
    protected RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    protected RolloutTargetGroupRepository rolloutTargetGroupRepository;
    @Autowired
    protected RolloutRepository rolloutRepository;
    @Autowired
    protected TenantConfigurationProperties tenantConfigurationProperties;
    @Autowired
    protected RolloutTestApprovalStrategy approvalStrategy;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JpaProperties jpaProperties;

    protected static void verifyThrownExceptionBy(final ThrowingCallable tc, final String objectType) {
        Assertions.assertThatExceptionOfType(EntityNotFoundException.class).isThrownBy(tc)
                .withMessageContaining(NOT_EXIST_ID).withMessageContaining(objectType);
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, SP_TARGET_FILTER_QUERY, SP_TARGET_TAG,SP_TARGET,SP_VEHICLE_ECU,SP_VEHICLE_MODEL,SP_ACTION,SP_ROLLOUTGROUP,SP_ROLLOUT,SP_DISTRIBUTION_SET,SP_ARTIFACT_SOFTWARE_MODULE,SP_SOFTWARE_VERSIONS,SP_BASE_SOFTWARE_MODULE
        );
    }

    protected Database getDatabase() {
        return jpaProperties.getDatabase();
    }

    @Transactional(readOnly = true)
    protected List<Action> findActionsByRolloutAndStatus(final Rollout rollout, final DeviceActionStatus actionStatus) {
        return Lists.newArrayList(actionRepository.findByRolloutIdAndStatusAndActive(PAGE, rollout.getId(), actionStatus, true));
    }

    protected TargetTagAssignmentResult toggleTagAssignment(final Collection<Target> targets, final TargetTag tag) {
        return targetManagement.toggleTagAssignment(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), tag.getName());
    }

    public DistributionSetTagAssignmentResult toggleTagAssignment(final Collection<DistributionSet> sets,
                                                                  final DistributionSetTag tag) {
        return distributionSetManagement.toggleTagAssignment(
                sets.stream().map(DistributionSet::getId).collect(Collectors.toList()), tag.getName());
    }

    protected TargetTypeAssignmentResult initiateTypeAssignment(final Collection<Target> targets, final TargetType type) {
        return targetManagement.assignType(
                targets.stream().map(Target::getControllerId).collect(Collectors.toList()), type.getId());
    }

}
