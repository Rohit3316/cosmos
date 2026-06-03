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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.assertj.core.util.Maps;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.eclipse.hawkbit.repository.exception.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.test.util.TestdataFactory;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Slice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;

@Feature("Component Tests - Repository")
@Story("RSQL filter target")
class RSQLTargetFieldTest extends AbstractJpaIntegrationTest {

    private static final String NAME = "name_";
    private static final String VIN = "vin_";
    private static final String DESCRIPTION = "description";
    private static final String REVISION = "revision";
    private static final String TARGET_TYPE_QUERY = "targettype.";
    private static final String NON_EXIST_QUERY = "==noExist*";
    private static final String NOT_EXIST_QUERY = ",notexist)";

    private static final String OR = ",";
    private static final String AND = ";";
    private static final String IN_QUERY = "=in=(";
    private static final String OUT_QUERY = "=out=(";
    private Target target;
    private Target target2;
    private Target target3;
    private TargetType targetType1;
    private TargetType targetType2;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;

    @BeforeEach
    void setupBeforeTest() {
        ddiSecurityProperties.setCreateTargetOnRun(true);
        final DistributionSet ds = testdataFactory.createDistributionSet("AssignedDs");

        final Map<String, String> attributes = new HashMap<>();
        final String targetName1 = NAME + testdataFactory.getRandomInt();
        target = targetManagement.create(entityFactory.target().create().controllerId(VIN + testdataFactory.getRandomInt())
                .name(targetName1).serialNumber(targetName1).description(DESCRIPTION).vehicleModelId(testdataFactory.createVehicle(targetName1).getId()).vin(targetName1));
        attributes.put(REVISION, "1.1");
        target = controllerManagement.updateControllerAttributesWithSoftware(target.getControllerId(), attributes, null, null);
        target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(target.getControllerId(), target.getName(),
                target.getSerialNumber(), target.getVehicleModelId());
        createTargetMetadata(target.getControllerId(), entityFactory.generateTargetMetadata("metaKey", "metaValue"));

        final String targetName2 = NAME + testdataFactory.getRandomInt();
        target2 = targetManagement
                .create(entityFactory.target().create().controllerId(VIN + testdataFactory.getRandomInt()).name(targetName2)
                        .serialNumber(targetName2).description(DESCRIPTION + "2").vehicleModelId(testdataFactory.createVehicle(targetName2).getId()).vin(targetName2));
        attributes.put(REVISION, "1.2");

        target2 = controllerManagement.updateControllerAttributesWithSoftware(target2.getControllerId(), attributes, null, null);
        target2 = controllerManagement.findOrRegisterTargetIfItDoesNotExist(target2.getControllerId(), target2.getName(),
                target2.getSerialNumber(), target2.getVehicleModelId());
        createTargetMetadata(target2.getControllerId(), entityFactory.generateTargetMetadata("metaKey", "value"));

        target3 = testdataFactory.createTarget(VIN + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),
                VIN + testdataFactory.getRandomInt());
        Target target4 = testdataFactory.createTarget(VIN + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId());
        testdataFactory.createTarget(VIN + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), NAME + testdataFactory.getRandomInt(), testdataFactory.createVehicle(NAME + testdataFactory.getRandomInt()).getId(),VIN + testdataFactory.getRandomInt());

        final TargetTag targetTag = targetTagManagement.create(entityFactory.tag().create().name("Tag1"));
        final TargetTag targetTag2 = targetTagManagement.create(entityFactory.tag().create().name("Tag2"));
        final TargetTag targetTag3 = targetTagManagement.create(entityFactory.tag().create().name("Tag3"));
        targetTagManagement.create(entityFactory.tag().create().name("Tag4"));

        targetManagement.assignTag(Arrays.asList(target.getControllerId(), target2.getControllerId()),
                targetTag.getId());

        targetManagement.assignTag(Arrays.asList(target3.getControllerId(), target4.getControllerId()),
                targetTag2.getId());
        targetManagement.assignTag(
                Arrays.asList(target.getControllerId(), target3.getControllerId(), target4.getControllerId()),
                targetTag3.getId());

        assignDistributionSet(ds.getId(), target.getControllerId());

        targetType1 = targetTypeManagement
                .create(entityFactory.targetType().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION));
        targetType2 = targetTypeManagement
                .create(entityFactory.targetType().create().name(NAME + testdataFactory.getRandomInt()).description(DESCRIPTION));

        targetManagement.assignType(target.getControllerId(), targetType1.getId());
        targetManagement.assignType(target2.getControllerId(), targetType2.getId());
    }

    @AfterEach
    public void tearDown() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_vehicle_model");
    }

    @Test
    @Description("Test filter target by (controller) id")
    void testFilterByParameterId() {
        assertRSQLQuery(TargetFields.ID.name() + "==" + target.getControllerId(), 1);
        assertRSQLQuery(TargetFields.ID.name() + "==" + VIN + "*", 5);
        assertRSQLQuery(TargetFields.ID.name() + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.ID.name() + "!=" + target.getControllerId(), 4);
        assertRSQLQuery(TargetFields.ID.name() + IN_QUERY + target.getControllerId() + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(TargetFields.ID.name() + OUT_QUERY + target.getControllerId() + NOT_EXIST_QUERY, 4);
    }

    @Test
    @Description("Test filter target by name")
    void testFilterByParameterName() {
        assertRSQLQuery(TargetFields.NAME.name() + "==" + target.getName(), 1);
        assertRSQLQuery(TargetFields.NAME.name() + "==" + NAME + "*", 5);
        assertRSQLQuery(TargetFields.NAME.name() + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.NAME.name() + "!=" + target.getName(), 4);
        assertRSQLQuery(TargetFields.NAME.name() + IN_QUERY + target.getName() + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(TargetFields.NAME.name() + OUT_QUERY + target.getName() + NOT_EXIST_QUERY, 4);
    }

    @Test
    @Description("Test filter target by description")
    void testFilterByParameterDescription() {
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==''", 3);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "!=''", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==" + DESCRIPTION, 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "!=" + DESCRIPTION, 4);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + "==" + DESCRIPTION + "*", 2);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + IN_QUERY + DESCRIPTION + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(TargetFields.DESCRIPTION.name() + OUT_QUERY + DESCRIPTION + NOT_EXIST_QUERY, 4);
    }

    @Test
    @Description("Test filter target by controller id")
    void testFilterByParameterControllerId() {
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==" + target.getControllerId(), 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "==" + VIN + "*", 5);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + "!=" + target.getControllerId(), 4);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + IN_QUERY + target.getControllerId() + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(TargetFields.CONTROLLERID.name() + OUT_QUERY + target.getControllerId() + NOT_EXIST_QUERY, 4);
    }

    @Test
    @Description("Test filter target by status")
    void testFilterByParameterUpdateStatus() {
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "==pending", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "!=pending", 4);
        try {
            assertRSQLQuery(TargetFields.UPDATESTATUS.name() + NON_EXIST_QUERY, 0);
            fail("RSQLParameterUnsupportedFieldException was expected since update status unknown");
        } catch (final RSQLParameterUnsupportedFieldException e) {
            // test ok - exception was excepted
        }
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=in=(pending,error)", 1);
        assertRSQLQuery(TargetFields.UPDATESTATUS.name() + "=out=(pending,error)", 4);
    }

    @Test
    @Description("Test filter target by attribute")
    void testFilterByAttribute() {
        controllerManagement.updateControllerAttributesWithSoftware(testdataFactory.createTarget().getControllerId(),
                Maps.newHashMap("test.dot", "value.dot"), null, null);

        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision!=1.1", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision==1*", 2);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + "." + REVISION + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=in=(1.1" + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".revision=out=(1.1)", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".test.dot==value.dot", 1);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.dot*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key.==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".key*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + ".*==value.dot", 0);
        assertRSQLQuery(TargetFields.ATTRIBUTE.name() + "..==value.dot", 0);
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + ".==value.dot");
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + "*==value.dot");
        assertRSQLQueryThrowsException(TargetFields.ATTRIBUTE.name() + "==value.dot");
    }

    @Test
    @Description("Test filter target by assigned ds name")
    void testFilterByAssignedDsName() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==AssignedDs", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name==A*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name" + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=in=(AssignedDs,notexist)", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".name=out=(AssignedDs,notexist)", 4);
    }

    @Test
    @Description("Test filter target by assigned ds version")
    void testFilterByAssignedDsVersion() {
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==" + TestdataFactory.DEFAULT_VERSION, 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version==*1*", 1);
        assertRSQLQuery(TargetFields.ASSIGNEDDS.name() + ".version" + NON_EXIST_QUERY, 0);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=in=(" + TestdataFactory.DEFAULT_VERSION + NOT_EXIST_QUERY, 1);
        assertRSQLQuery(
                TargetFields.ASSIGNEDDS.name() + ".version=out=(" + TestdataFactory.DEFAULT_VERSION + NOT_EXIST_QUERY, 4);
    }

    @Test
    @Description("Test filter target by tag name")
    void testFilterByTag() {
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag1", 3);
        assertRSQLQuery(TargetFields.TAG.name() + "==T*", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "!=T*", 1);
        assertRSQLQuery(TargetFields.TAG.name() + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.TAG.name() + "!=notexist", 5);
        assertRSQLQuery(TargetFields.TAG.name() + "==''", 1);
        assertRSQLQuery(TargetFields.TAG.name() + "!=''", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(Tag1,notexist)", 2);
        assertRSQLQuery(TargetFields.TAG.name() + "=in=(null)", 0);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(Tag1,notexist)", 3);
        assertRSQLQuery(TargetFields.TAG.name() + "=out=(null)", 5);
        assertRSQLQuery(TargetFields.TAG.name() + "==Tag1" + OR + TargetFields.TAG.name() + "==Tag2", 4);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag2" + AND + TargetFields.TAG.name() + "==Tag3", 1);
        assertRSQLQuery(TargetFields.TAG.name() + "!=Tag2" + OR + TargetFields.TAG.name() + "!=Tag3", 3);
    }

    @Test
    @Description("Test filter target by lastTargetQuery")
    void testFilterByLastTargetQuery() {
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "==" + target.getLastTargetQuery(), 2);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "!=" + target.getLastTargetQuery(), 3);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=lt=" + target2.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=" + target2.getLastTargetQuery(), 0);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=le=${NOW_TS}", 2);
        assertRSQLQuery(TargetFields.LASTCONTROLLERREQUESTAT.name() + "=gt=${OVERDUE_TS}", 2);
    }

    @Test
    @Description("Test filter target by metadata")
    void testFilterByMetadata() {
        createTargetMetadata(testdataFactory.createTarget().getControllerId(),
                entityFactory.generateTargetMetadata("key.dot", "value.dot"));

        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey==*v*", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey" + NON_EXIST_QUERY, 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=in=(metaValue,notexist)", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey=out=(metaValue,notexist)", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist==metaValue", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey!=metaValue", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".notExist!=metaValue", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".metaKey!=notExist", 2);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.dot==value.dot", 1);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.dot*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key.==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".key*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + ".*==value.dot", 0);
        assertRSQLQuery(TargetFields.METADATA.name() + "..==value.dot", 0);
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + ".==value.dot");
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + "*==value.dot");
        assertRSQLQueryThrowsException(TargetFields.METADATA.name() + "==value.dot");
    }

    @Test
    @Description("Test filter based on more complex RSQL queries")
    void testFilterByComplexQueries() {
        assertRSQLQuery(
                TargetFields.NAME.name() + "!=" + target.getName() + AND + TargetFields.METADATA.name() + ".metaKey!=value",
                0);
        assertRSQLQuery("(" + TargetFields.TAG.name() + "!=TAG1" + OR + TargetFields.TAG.name() + "!=TAG2)" + AND
                + TargetFields.CONTROLLERID.name() + "!=" + target3.getControllerId(), 4);
    }

    @Test
    @Description("Testing allowed RSQL keys based on TargetFields definition")
    void rsqlValidTargetFields() {
        final String rsql1 = "ID == '0123' and NAME == abcd and DESCRIPTION == absd"
                + " and CREATEDAT =lt= 0123 and LASTMODIFIEDAT =gt= 0123"
                + " and CONTROLLERID == 0123 and UPDATESTATUS == PENDING"
                + " and IPADDRESS == 0123 and LASTCONTROLLERREQUESTAT == 0123" + " and tag == beta";

        RSQLUtility.validateRsqlFor(rsql1, TargetFields.class);

        final String rsql2 = "ASSIGNEDDS.name == abcd and ASSIGNEDDS.version == 0123"
                + " and INSTALLEDDS.name == abcd and INSTALLEDDS.version == 0123";
        RSQLUtility.validateRsqlFor(rsql2, TargetFields.class);

        final String rsql3 = "ATTRIBUTE.subkey1 == test and ATTRIBUTE.subkey2 == test"
                + " and METADATA.metakey1 == abcd and METADATA.metavalue2 == asdfg";
        RSQLUtility.validateRsqlFor(rsql3, TargetFields.class);

        final String rsql4 = "CREATEDAT =lt= ${NOW_TS} and LASTMODIFIEDAT =ge= ${OVERDUE_TS}";
        RSQLUtility.validateRsqlFor(rsql4, TargetFields.class);

        final String rsql5 = "wrongfield == abcd";
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> RSQLUtility.validateRsqlFor(rsql5, TargetFields.class));

        final String rsql6 = "ATTRIBUTE.test.dot == test and ATTRIBUTE.subkey2 == test"
                + " and METADATA.test.dot == abcd and METADATA.metavalue2 == asdfg";
        RSQLUtility.validateRsqlFor(rsql6, TargetFields.class);
    }

    @Test
    @Description("Test filter by target type")
    void shouldFilterTargetsByTypeIdNameAndDescription() {
        assertRSQLQuery(TARGET_TYPE_QUERY + TargetTypeFields.NAME.name() + "==" + targetType1.getName(), 1);
        assertRSQLQuery(TARGET_TYPE_QUERY + TargetTypeFields.NAME.name() + "==" + NAME + "*", 2);
        assertRSQLQuery(TARGET_TYPE_QUERY + TargetTypeFields.NAME.name() + "!=" + targetType2.getName(), 4);
        assertRSQLQuery(TARGET_TYPE_QUERY + TargetTypeFields.NAME.name() + NON_EXIST_QUERY, 0);

        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> assertRSQLQuery(TARGET_TYPE_QUERY + "ID==1", 0));
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> assertRSQLQuery(TARGET_TYPE_QUERY + "description==" + DESCRIPTION, 0));
    }

    private void assertRSQLQuery(final String rsqlParam, final long expectedTargets) {
        final Slice<Target> findTargetPage = targetManagement.findByRsql(PAGE, rsqlParam);
        final long countTargetsAll = targetManagement.countByRsql(rsqlParam);
        assertThat(findTargetPage).isNotNull();
        assertThat(findTargetPage.getNumberOfElements()).isEqualTo(countTargetsAll).isEqualTo(expectedTargets);
    }

    private void assertRSQLQueryThrowsException(final String rsqlParam) {
        assertThatExceptionOfType(RSQLParameterUnsupportedFieldException.class)
                .isThrownBy(() -> RSQLUtility.validateRsqlFor(rsqlParam, TargetFields.class));
    }
}
