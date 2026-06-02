/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.repository.RolloutExecutor;
import org.eclipse.hawkbit.repository.RolloutHandler;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.transaction.PlatformTransactionManager;

import jakarta.validation.ValidationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * JPA implementation of {@link RolloutHandler}.
 */
public class JpaRolloutHandler implements RolloutHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(JpaRolloutHandler.class);
    public static final String END_ROLLOUT = "-endRollout";

    private final TenantAware tenantAware;
    private final RolloutManagement rolloutManagement;
    private final RolloutExecutor rolloutExecutor;
    private final LockRegistry lockRegistry;
    private final PlatformTransactionManager txManager;

    /**
     * Constructor
     * 
     * @param tenantAware
     *            the {@link TenantAware} bean holding the tenant information
     * @param rolloutManagement
     *            to fetch rollout related information from the datasource
     * @param rolloutExecutor
     *            to trigger executions for a specific rollout
     * @param lockRegistry
     *            to lock processes
     * @param txManager
     *            transaction manager interface
     */
    public JpaRolloutHandler(final TenantAware tenantAware, final RolloutManagement rolloutManagement,
            final RolloutExecutor rolloutExecutor, final LockRegistry lockRegistry,
            final PlatformTransactionManager txManager) {
        this.tenantAware = tenantAware;
        this.rolloutManagement = rolloutManagement;
        this.rolloutExecutor = rolloutExecutor;
        this.lockRegistry = lockRegistry;
        this.txManager = txManager;
    }

    @Override
    public void handleAll() {
        final List<Long> rollouts = rolloutManagement.findActiveRollouts();

        if (rollouts.isEmpty()) {
            return;
        }

        final String handlerId = createRolloutLockKey(tenantAware.getCurrentTenant());
        final Lock lock = lockRegistry.obtain(handlerId);
        if (!lock.tryLock()) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Could not perform lock {}", lock);
            }
            return;
        }

        try {
            LOGGER.trace("Trigger handling {} rollouts.", rollouts.size());
            rollouts.forEach(rolloutId -> {
                try {
                    handleRolloutInNewTransaction(rolloutId, handlerId);
                } catch (Exception e) {
                    LOGGER.error(
                            "Rollout ID {} failed. Root cause: {}",
                            rolloutId,
                            ExceptionUtils.getRootCause(e).getMessage(),
                            e
                    );
                }
            });
        }
        finally {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Unlock lock {}", lock);
            }
            lock.unlock();
        }
    }

    @Override
    public void handleEnd() {
        LOGGER.debug("Starting handleEnd for tenant: {}", tenantAware.getCurrentTenant());
        tenantAware.runAsTenant(tenantAware.getCurrentTenant(),()->{
            DeploymentHelper.runInNewTransaction(txManager, getRolloutEndTxnName(), status -> {
                LOGGER.debug("Executing handleEndRollouts in a new transaction.");
                return rolloutManagement.handleEndRollouts();
            });
            return null;
        });

        LOGGER.debug("Completed handleEnd for tenant: {}", tenantAware.getCurrentTenant());
    }


    private  String getRolloutEndTxnName(){
        return tenantAware.getCurrentTenant()+END_ROLLOUT;
    }

    private static String createRolloutLockKey(final String tenant) {
        return tenant + "-rollout";
    }

    private void handleRolloutInNewTransaction(final long rolloutId, final String handlerId) {
        DeploymentHelper.runInNewTransaction(txManager, handlerId + "-" + rolloutId, status -> {
            rolloutManagement.get(rolloutId).ifPresentOrElse(
                    rollout -> runInUserContext(rollout, () -> {
                        try{
                            rolloutExecutor.execute(rollout);
                        }catch (ValidationException | AbstractServerRtException e){
                            LOGGER.error("Error while executing rollout with id {}.error message:{}",rolloutId,e.getMessage(),e);
                            throw e;
                        }
                    }),
                    () -> LOGGER.error("Could not retrieve rollout with id {}. Will not continue with execution.",
                            rolloutId));
            return 0L;
        });
    }

    private void runInUserContext(final BaseEntity rollout, final Runnable handler) {
        DeploymentHelper.runInNonSystemContext(handler, () -> Objects.requireNonNull(rollout.getCreatedBy()),
                tenantAware);
    }

}
