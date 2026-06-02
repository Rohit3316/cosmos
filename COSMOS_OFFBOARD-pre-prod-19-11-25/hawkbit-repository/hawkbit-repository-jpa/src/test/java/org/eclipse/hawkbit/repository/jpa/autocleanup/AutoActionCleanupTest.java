/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.autocleanup;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_EXPIRY;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ACTION_STATUS;
import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.ACTION_CLEANUP_ENABLED;

/**
 * Test class for {@link AutoActionCleanup}.
 *
 */
@Feature("Component Tests - Repository")
@Story("Action cleanup handler")
class AutoActionCleanupTest extends AbstractJpaIntegrationTest {

    @Autowired
    private AutoActionCleanup autoActionCleanup;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Target trg1;
    private Target trg2;
    private Target trg3;

    private DistributionSet ds1;

    private DistributionSet ds2;

    @BeforeEach
    public void setUp() {
        trg1 = testdataFactory.createTarget("trg1", "trg1", "trg1", testdataFactory.createVehicle("STLA-Brain").getId(), "19UYA31581L200400");
        trg2 = testdataFactory.createTarget("trg2", "trg2", "trg2", testdataFactory.createVehicle("Dcross").getId(), "19UYA31581L200401");
        trg3 = testdataFactory.createTarget("trg3", "trg3", "trg3", testdataFactory.createVehicle("NEA 2.0").getId(),"19UYA31581L200402");

        ds1 = testdataFactory.createDistributionSet("ds1");
        ds2 = testdataFactory.createDistributionSet("ds2");
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model", "sp_target", "sp_distribution_set", "sp_action");
    }


    @Test
    @Description("Verifies that running actions are not cleaned up.")
    void runningActionsAreNotCleanedUp() {
        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, DeviceActionStatus.CANCELED, DeviceActionStatus.ERROR_RESPONSE_CODE);

        assignDistributionSet(ds1.getId(), trg1.getControllerId());
        assignDistributionSet(ds2.getId(), trg2.getControllerId());

        assertThat(actionRepository.count()).isEqualTo(2);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
    }

    @Test
    @Description("Verifies that nothing is cleaned up if the cleanup is disabled.")
    void cleanupDisabled() {

        // cleanup config for this test case
        setupCleanupConfiguration(false, 0, DeviceActionStatus.CANCELED);
        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        assignDistributionSet(ds2.getId(), trg2.getControllerId());

        setActionToCanceled(action1);

        assertThat(actionRepository.count()).isEqualTo(2);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
    }

    @Test
    @Description("Verifies that canceled actions are cleaned up.")
    void canceledActionsAreCleanedUp() throws InterruptedException {
        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, DeviceActionStatus.CANCELED);

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        TestdataFactory.waitForSeconds(1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(2);
        assertThat(actionRepository.getActionById(action2, false)).isPresent();
        assertThat(actionRepository.getActionById(action3, true)).isPresent();
    }

    @Test
    @Description("Verifies that canceled and failed actions are cleaned up once they expired.")
    @SuppressWarnings("squid:S2925")
    void canceledAndFailedActionsAreCleanedUpWhenExpired() throws InterruptedException {
        // cleanup config for this test case
        setupCleanupConfiguration(true, 500 / 1000, DeviceActionStatus.CANCELED, DeviceActionStatus.ERROR_RESPONSE_CODE);

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        autoActionCleanup.run();

        // actions have not expired yet
        assertThat(actionRepository.count()).isEqualTo(3);

        // wait for expiry to elapse
        Thread.sleep(8000);

        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.getActionById(action3, true)).isPresent();
    }

    private void setActionToCanceled(final Long id) {
        deploymentManagement.cancelAction(id);
        deploymentManagement.forceQuitAction(id);
    }

    private void setActionToFailed(final Long id) {
        controllerManagement.addUpdateActionStatus(entityFactory.actionStatus().create(id).status(DeviceActionStatus.ERROR_RESPONSE_CODE), null);
    }

    private void setupCleanupConfiguration(final boolean cleanupEnabled, final long expiry, final DeviceActionStatus... status) {
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ENABLED, cleanupEnabled);
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ACTION_EXPIRY, expiry);
        tenantConfigurationManagement.addOrUpdateConfiguration(ACTION_CLEANUP_ACTION_STATUS,
                Arrays.stream(status).map(DeviceActionStatus::toString).collect(Collectors.joining(",")));
    }

    @Test
    @Description("Verifies that canceled and failed actions are cleaned up.")
    void canceledAndFailedActionsAreCleanedUp() {
        // cleanup config for this test case
        setupCleanupConfiguration(true, 0, DeviceActionStatus.CANCELED, DeviceActionStatus.ERROR_RESPONSE_CODE);

        final Long action1 = getFirstAssignedActionId(assignDistributionSet(ds1.getId(), trg1.getControllerId()));
        final Long action2 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg2.getControllerId()));
        final Long action3 = getFirstAssignedActionId(assignDistributionSet(ds2.getId(), trg3.getControllerId()));

        assertThat(actionRepository.count()).isEqualTo(3);

        setActionToCanceled(action1);
        setActionToFailed(action2);

        assertThat(actionRepository.count()).isEqualTo(3);

        TestdataFactory.waitForSeconds(2);
        autoActionCleanup.run();

        assertThat(actionRepository.count()).isEqualTo(1);
        assertThat(actionRepository.getActionById(action3, true)).isPresent();
    }

}
