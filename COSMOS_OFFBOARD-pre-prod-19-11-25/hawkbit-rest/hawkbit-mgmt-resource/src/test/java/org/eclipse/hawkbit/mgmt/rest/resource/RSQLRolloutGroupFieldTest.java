/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;

@Feature("Component Tests - Repository")
@Story("RSQL filter rollout group")
public class RSQLRolloutGroupFieldTest extends AbstractManagementApiIntegrationTest {

    private Long rolloutGroupId;
    private Rollout rollout;

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private JpaProperties jpaProperties;

    public static final String KAFKA_ROLLOUT_STATUS_ENDPOINT = "/kafka/rolloutstatus";

    private static ClientAndServer mockServer;

    @BeforeAll
    static void mockPublishRolloutStatus() {
        int port = findFreePort();
        mockServer = startClientAndServer(port);
        System.setProperty("mock.server.port", String.valueOf(port));
        mockServer.when(HttpRequest.request().withMethod("POST").withPath(KAFKA_ROLLOUT_STATUS_ENDPOINT)).
                respond(HttpResponse.response().withStatusCode(200));
    }

    @AfterAll
    static void stop() {
        mockServer.stop();
    }


    @BeforeEach
    public void setupBeforeTest() {
        final int amountTargets = 20;
        testdataFactory.createTargets(amountTargets, "rollout", "rollout");
        rollout = testdataFactory.addNewRollout(MgmtRolloutMapper.fromRequest(testdataFactory.buildDefaultRolloutRequest("test")));
        rollout = rolloutManagement.get(rollout.getId()).get();
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model", "sp_rollout");
    }

    @Test
    @Description("Test filter rollout group by  id")
    public void testFilterByParameterId() {
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==" + rolloutGroupId, 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "!=" + rolloutGroupId, 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==" + -1, 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "!=" + -1, 0);

        // Not supported for numbers
        if (Database.POSTGRESQL.equals(jpaProperties.getDatabase())) {
            return;
        }

        assertRSQLQuery(RolloutGroupFields.ID.name() + "==*", 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "==noexist*", 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "=in=(" + rolloutGroupId + ",10000000)", 0);
        assertRSQLQuery(RolloutGroupFields.ID.name() + "=out=(" + rolloutGroupId + ",10000000)", 0);
    }

    @Test
    @Description("Test filter rollout group by name")
    public void testFilterByParameterName() {
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==group-1", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "!=group-1", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==*", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=in=(group-1,group-2)", 0);
        assertRSQLQuery(RolloutGroupFields.NAME.name() + "=out=(group-1,group-2)", 0);
    }

    @Test
    @Description("Test filter rollout group by description")
    public void testFilterByParameterDescription() {
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group-1", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "!=group-1", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==group*", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "==noExist*", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=in=(group-1,notexist)", 0);
        assertRSQLQuery(RolloutGroupFields.DESCRIPTION.name() + "=out=(group-1,notexist)", 0);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedTargets) {
        final Page<RolloutGroup> findTargetPage = rolloutGroupManagement.findByRolloutAndRsql(PageRequest.of(0, 100),
                rollout.getId(), rsqlParam);
        final long countTargetsAll = findTargetPage.getTotalElements();
        assertThat(findTargetPage).isNotNull();
        assertThat(countTargetsAll).isEqualTo(expectedTargets);
    }

}
