/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;


import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.service.CdnFileUploadService;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * A Jpa implementation of {@link RolloutExecutor}
 */

@Slf4j
public class JpaRolloutExecutor implements RolloutExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRolloutExecutor.class);

    // TODO: Include CANCELING status once the scheduler for device action is ready
    private static final List<DeviceActionStatus> ROLLOUT_FINISH_STATUS_LIST = List.of(DeviceActionStatus.FINISHED_FAILURE, DeviceActionStatus.FINISHED_SUCCESS, DeviceActionStatus.CANCELED);

    /**
     * Max amount of targets that are handled in one transaction.
     */
    private static final int TRANSACTION_TARGETS = 5_000;
    private static final int FIRST_PAGE_NUMBER = 0;
    private static final int PAGE_SIZE = 100;

    private final RolloutTargetGroupRepository rolloutTargetGroupRepository;
    @PersistenceContext
    private final EntityManager entityManager;
    private final RolloutRepository rolloutRepository;
    private final ActionRepository actionRepository;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final AfterTransactionCommitExecutor afterCommit;
    private final TenantAware tenantAware;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final QuotaManagement quotaManagement;
    private final DeploymentManagement deploymentManagement;
    private final TargetManagement targetManagement;
    private final EventPublisherHolder eventPublisherHolder;
    private final PlatformTransactionManager txManager;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final RolloutGroupEvaluationManager evaluationManager;
    private final RolloutManagement rolloutManagement;
    private final KafkaMessageService kafkaMessageService;
    private final ActionStatusRepository actionStatusRepository;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SupportPackageManagement supportPackageManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final SystemManagement systemManagement;

    @Autowired
    private CdnFileUploadService<BaseSupportPackage> supportPackageCdnFileUploadService;
    @Autowired
    private HandleRolloutSchedulerService rolloutSchedulerService;


    /**
     * Constructor
     */
    public JpaRolloutExecutor(final RolloutTargetGroupRepository rolloutTargetGroupRepository,
                              final EntityManager entityManager, final RolloutRepository rolloutRepository,
                              final ActionRepository actionRepository, final RolloutGroupRepository rolloutGroupRepository,
                              final AfterTransactionCommitExecutor afterCommit, final TenantAware tenantAware,
                              final RolloutGroupManagement rolloutGroupManagement, final QuotaManagement quotaManagement,
                              final DeploymentManagement deploymentManagement, final TargetManagement targetManagement,
                              final EventPublisherHolder eventPublisherHolder, final PlatformTransactionManager txManager,
                              final RolloutApprovalStrategy rolloutApprovalStrategy, final RolloutGroupEvaluationManager evaluationManager,
                              final RolloutManagement rolloutManagement, final ActionStatusRepository actionStatusRepository,
                              final SupportPackageManagement supportPackageManagement,
                              final SystemManagement systemManagement, final TenantConfigurationManagement tenantConfigurationManagement,
                              final SystemSecurityContext systemSecurityContext, final KafkaMessageService kafkaMessageService) {
        this.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.entityManager = entityManager;
        this.rolloutRepository = rolloutRepository;
        this.actionRepository = actionRepository;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.afterCommit = afterCommit;
        this.tenantAware = tenantAware;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.quotaManagement = quotaManagement;
        this.deploymentManagement = deploymentManagement;
        this.targetManagement = targetManagement;
        this.eventPublisherHolder = eventPublisherHolder;
        this.txManager = txManager;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.evaluationManager = evaluationManager;
        this.rolloutManagement = rolloutManagement;
        this.actionStatusRepository = actionStatusRepository;
        this.supportPackageManagement = supportPackageManagement;
        this.systemManagement = systemManagement;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.kafkaMessageService = kafkaMessageService;
    }

    @Override
    public void execute(final Rollout rollout) {
        MDC.put("rolloutName", rollout.getName());
        MDC.put("rolloutID", rollout.getId().toString());
        LOGGER.debug("handle rollout {}", rollout.getId());
        if (!rollout.getStatus().equals(RolloutStatus.FINISHING) && rollout.getEndAt() < Instant.now().atZone(ZoneId.of("UTC")).toEpochSecond()) {
            return;
        }
        switch (rollout.getStatus()) {
//            case DRAFT -> handleDraftRollout((JpaRollout)rollout); // Handles rollout in draft state
            case FREEZING ->
                    rolloutSchedulerService.handleFreezeRollout((JpaRollout) rollout); // handles Rollout in freezing state
            case READY ->
                    rolloutSchedulerService.handleReadyRollout((JpaRollout) rollout); // handles rollout in ready state
            //RSP will uploaded for AUTO and SCHEDULED rollouts
            case STARTING ->
                    rolloutSchedulerService.handleStartingRollout((JpaRollout) rollout);
            case DELETING ->
                    rolloutSchedulerService.handleDeleteRollout((JpaRollout) rollout);// handles rollout in deleting state
            case RUNNING ->
                    rolloutSchedulerService.handleRunningRollout((JpaRollout) rollout);// handles rollout in running state
            case PAUSING ->
                    rolloutSchedulerService.handlePausingRollout((JpaRollout) rollout); //handles rollout in pausing state
            case RESUMING ->
                    rolloutSchedulerService.handleResumingRollout((JpaRollout) rollout); //handles rollout in resuming state
            case CANCELING ->
                    rolloutSchedulerService.handleCancelRollout((JpaRollout) rollout);// handles rollout in cancelling state
            case FINISHING ->
                    rolloutSchedulerService.handleFinishingRollout((JpaRollout) rollout);//handles rollout in Finishing state
            case RETRY ->
                   rolloutSchedulerService.handleRetryRollout((JpaRollout) rollout); //handles rollout in retry state
            case RETRYING ->
                    rolloutSchedulerService.handleRetryingRollout((JpaRollout) rollout);//handles rollout in retrying state
            default ->
                    LOGGER.error("Rollout {} in status {},rolloutName: {} not supposed to be handled!", rollout.getId(), rollout.getStatus(), rollout.getName());
        }
    }
}
