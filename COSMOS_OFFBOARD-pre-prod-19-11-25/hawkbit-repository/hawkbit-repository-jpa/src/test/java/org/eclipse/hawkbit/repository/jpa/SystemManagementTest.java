/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.ByteArrayInputStream;
import java.util.List;

import org.cosmos.models.mgmt.FileType;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.report.model.TenantUsage;
import org.eclipse.hawkbit.repository.test.util.DisposableSqlTestDatabaseExtension;
import org.eclipse.hawkbit.repository.test.util.RandomGenerator;
import org.eclipse.hawkbit.repository.test.util.WithSpringAuthorityRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

@Feature("Component Tests - Repository")
@Story("System Management")
@ExtendWith(DisposableSqlTestDatabaseExtension.class)
class SystemManagementTest extends AbstractJpaIntegrationTest {

    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";
    private static final String DEFAULT_TENANT = "default";
    private static final String TENANT = "tenant";
    private static final String BUMLUX = "bumlux";
    private static final String TO_BE_DEPLOYED = "to be deployed";

    @Test
    @Description("Ensures that findTenants returns all tenants and not only restricted to the tenant which currently is logged in")
    public void findTenantsReturnsAllTenantsNotOnlyWhichLoggedIn() throws Exception {
        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(1);

        createTestTenantsForSystemStatistics(2, 0, 0, 0);

        assertThat(systemManagement.findTenants(PAGE).getContent()).hasSize(3);
    }

    @Test
    @Description("Ensures that getSystemUsageStatisticsWithTenants returns the usage of all tenants and not only the first 1000 (max page size).")
    public void systemUsageReportCollectsStatisticsOfManyTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(1050, 0, 0, 0);

        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(1051); // +1 from the setup
    }

    @Test
    @Description("Checks that the system report calculates correctly the targets size of all tenants in the system")
    public void systemUsageReportCollectsTargetsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(1, 0, 100, 0);

        // overall data
        assertThat(systemManagement.getSystemUsageStatistics().getOverallTargets()).isEqualTo(100);
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isEqualTo(0);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(2);
        assertThat(tenants).containsOnly(new TenantUsage(DEFAULT_TENANT), new TenantUsage(TENANT + "0").setTargets(100));

    }

    @Test
    @Description("Checks that the system report calculates correctly the actions size of all tenants in the system")
    public void systemUsageReportCollectsActionsOfAllTenants() throws Exception {
        // Prepare tenants
        createTestTenantsForSystemStatistics(1, 0, 20, 2);

        // 2 tenants, 100 targets each, 2 deployments per target => 400
        assertThat(systemManagement.getSystemUsageStatistics().getOverallActions()).isEqualTo(40);

        // per tenant data
        final List<TenantUsage> tenants = systemManagement.getSystemUsageStatisticsWithTenants().getTenants();
        assertThat(tenants).hasSize(2);
        assertThat(tenants).containsOnly(new TenantUsage(DEFAULT_TENANT),
                new TenantUsage(TENANT + "0").setTargets(20).setActions(40));
    }

    private byte[] createTestTenantsForSystemStatistics(final int tenants, final int artifactSize, final int targets, final int updates) throws Exception {
        final byte[] random = generateRandomBytes(artifactSize);

        for (int i = 0; i < tenants; i++) {
            final String tenantName = TENANT + i;
            WithSpringAuthorityRule.runAs(
                    WithSpringAuthorityRule.withUserAndTenant(BUMLUX, tenantName, true, true, false, SpringEvalExpressions.SYSTEM_ROLE),
                    () -> executeTenantOperations(tenantName, random, artifactSize, targets, updates)
            );
        }

        return random;
    }

    private byte[] generateRandomBytes(final int size) {
        final byte[] random = new byte[size];
        RandomGenerator.getRandom().nextBytes(random);
        return random;
    }

    private Void executeTenantOperations(final String tenantName, final byte[] random, final int artifactSize, final int targets, final int updates) throws Exception {
        systemManagement.getTenantMetadata(tenantName);

//        if (artifactSize > 0) {
//            createArtifacts(random);
//        }

        if (targets > 0) {
            createTargetsAndUpdates(targets, updates);
        }

        return null;
    }


    private void createTargetsAndUpdates(final int targets, final int updates) throws Exception {
        final List<Target> createdTargets = createTestTargets(targets);
        if (updates > 0) {
            for (int x = 0; x < updates; x++) {
                final DistributionSet ds = testdataFactory.createDistributionSet(TO_BE_DEPLOYED + x, true, false);
                assignDistributionSet(ds, createdTargets);
            }
        }
    }


    private List<Target> createTestTargets(final int targets) {
        return testdataFactory.createTargets(targets, NAME + testdataFactory.getRandomInt(), DESCRIPTION);
    }



}
