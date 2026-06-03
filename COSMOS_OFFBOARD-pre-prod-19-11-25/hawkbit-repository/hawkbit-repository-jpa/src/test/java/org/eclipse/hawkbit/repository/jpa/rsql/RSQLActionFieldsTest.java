/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

@Feature("Component Tests - Repository")
@Story("RSQL filter actions")
class RSQLActionFieldsTest extends AbstractJpaIntegrationTest {

    private JpaTarget target;
    private JpaAction action;
    private static final Logger LOG= LoggerFactory.getLogger(RSQLActionFieldsTest.class);

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setupBeforeTest() {
        final DistributionSet dsA = testdataFactory.createDistributionSet("daA");
        target = (JpaTarget) targetManagement
                .create(entityFactory.target().create().controllerId("targetId123")
                        .name("targetName123").serialNumber("targetSrNo123").description("targetId123").vehicleModelId(testdataFactory.createVehicle("STLA-Brain").getId()).vin("19UYA31581L200400"));
        action = new JpaAction();
        action.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
        action.setDistributionSet(dsA);
        action.setTarget(target);
        action.setStatus(DeviceActionStatus.RUNNING);
        action.setWeight(45);
        action.setInitiatedBy(tenantAware.getCurrentUsername());
        target.addAction(action);

        actionRepository.save(action);
        for (int i = 0; i < 10; i++) {
            final JpaAction newAction = new JpaAction();
            newAction.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.YES);
            newAction.setDistributionSet(dsA);
            newAction.setActive((i % 2) == 0);
            newAction.setStatus(DeviceActionStatus.RUNNING);
            newAction.setTarget(target);
            newAction.setWeight(45);
            newAction.setInitiatedBy(tenantAware.getCurrentUsername());
            actionRepository.save(newAction);
            target.addAction(newAction);
        }

    }

    @AfterEach
    public void tearDown(){
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model");
    }

    @Test
    @Description("Test filter action by id")
    public void testFilterByParameterId() {
        assertRSQLQuery(ActionFields.ID.name() + "==" + action.getId(), 1);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + action.getId(), 10);
        assertRSQLQuery(ActionFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(ActionFields.ID.name() + "!=" + -1, 11);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(getDatabase())) {
            return;
        }

        assertRSQLQuery(ActionFields.ID.name() + "==*", 11);
        assertRSQLQuery(ActionFields.ID.name() + "==noexist*", 0);
        assertRSQLQuery(ActionFields.ID.name() + "=in=(" + action.getId() + ",10000000)", 1);
        assertRSQLQuery(ActionFields.ID.name() + "=out=(" + action.getId() + ",10000000)", 10);
    }

    @Test
    @Description("Test action by status")
    public void testFilterByParameterStatus() {
        assertRSQLQuery(ActionFields.STATUS.name() + "==pending", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "!=pending", 6);
        assertRSQLQuery(ActionFields.STATUS.name() + "=in=(pending)", 5);
        assertRSQLQuery(ActionFields.STATUS.name() + "=out=(pending)", 6);

        try {
            assertRSQLQuery(ActionFields.STATUS.name() + "==true", 5);
            fail("Missing expected RSQLParameterUnsupportedFieldException because status cannot be compared with 'true'");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            LOG.error("Missing expected RSQLParameterUnsupportedFieldException because status cannot be compared with 'true'", e);
        }
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Slice<Action> findEnitity = deploymentManagement.findActionsByTarget(rsqlParam, target.getControllerId(),
                PageRequest.of(0, 100));
        final long countAllEntities = deploymentManagement.countActionsByTarget(rsqlParam, target.getControllerId());
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
