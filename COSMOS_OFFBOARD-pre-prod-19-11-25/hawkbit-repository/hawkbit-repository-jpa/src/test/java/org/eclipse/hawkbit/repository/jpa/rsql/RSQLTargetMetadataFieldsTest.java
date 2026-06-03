/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.eclipse.hawkbit.repository.TargetMetadataFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.MetaData;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

@Feature("Component Tests - Repository")
@Story("RSQL filter target metadata")
public class RSQLTargetMetadataFieldsTest extends AbstractJpaIntegrationTest {

    private static final String CONTROLLER_ID = "target";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setupBeforeTest() {

        testdataFactory.createTarget(CONTROLLER_ID, CONTROLLER_ID, CONTROLLER_ID, testdataFactory.createVehicle("STLA-Brain").getId(),CONTROLLER_ID);

        final List<MetaData> metadata = new ArrayList<>(5);
        for (int i = 0; i < 5; i++) {
            metadata.add(entityFactory.generateTargetMetadata("" + i, "" + i));
        }

        targetManagement.createMetaData(CONTROLLER_ID, metadata);

        targetManagement.createMetaData(CONTROLLER_ID,
                Collections.singletonList(entityFactory.generateTargetMetadata("emptyValueTest", null)));
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model");
    }

    @Test
    @Description("Test filter target metadata by key")
    public void testFilterByParameterKey() {
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "==1", 1);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "!=1", 5);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "=in=(1,2)", 2);
        assertRSQLQuery(TargetMetadataFields.KEY.name() + "=out=(1,2)", 4);
    }

    @Test
    @Description("Test filter target metadata by value")
    public void testFilterByParameterValue() {
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "==''", 1);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "!=''", 5);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "==1", 1);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "!=1", 5);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "=in=(1,2)", 2);
        assertRSQLQuery(TargetMetadataFields.VALUE.name() + "=out=(1,2)", 4);
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedEntities) {

        final Page<TargetMetadata> findEnitity = targetManagement
                .findMetaDataByControllerIdAndRsql(PageRequest.of(0, 100), CONTROLLER_ID, rsqlParam);
        final long countAllEntities = findEnitity.getTotalElements();
        assertThat(findEnitity).isNotNull();
        assertThat(countAllEntities).isEqualTo(expectedEntities);
    }
}
