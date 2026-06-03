package org.eclipse.hawkbit.repository.jpa;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.test.util.RandomGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;


import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Feature("Component Tests - Repository")
@Story("Software Module Format Management")
class SoftwareModuleFormatManagementTest extends AbstractJpaIntegrationTest{

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp(){
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "sp_software_module_format");
    }
    @Test
    @Description("Find Software module format by name returns list of sw module formats")
    void givenRsqlSearchWhenFindThenReturnSwModuleFormat(){
        testdataFactory.findOrCreateSoftwareModuleFormat("OS");
        assertThat(softwareModuleFormatManagement.getByName("OS")).isPresent();
        assertThat(softwareModuleFormatManagement.findByRsql(PAGE,"name==*")).isNotNull();
    }

    @Test
    @Description("Count by delete as false returns the count of sw module formats")
    void givenExistingSwModuleFormatWhenCountThenReturnCount(){
        SoftwareModuleFormatCreate createdSwModuleFormat = entityFactory.softwareModuleFormat().create().key("qnx").name("QNX").description("qnx format");
        SoftwareModuleFormatCreate createdSwModuleFormat1 = entityFactory.softwareModuleFormat().create().key("os").name("OS").description("Os format");
        Collection<SoftwareModuleFormatCreate> formatCollection = new ArrayList<>();
        formatCollection.add(createdSwModuleFormat);
        formatCollection.add(createdSwModuleFormat1);
        assertThat(softwareModuleFormatManagement.create(formatCollection)).isNotNull();
        assertThat(softwareModuleFormatManagement.findAll(PAGE)).isNotNull();
        assertThat(softwareModuleFormatManagement.count()).isGreaterThan(1);
    }


    @Test
    @Description("Sw Module Format to delete from the list not found")
    void givenSwModuleFormatsNotPresentWhenDeleteThenException(){
        long id1 = RandomGenerator.getRandom().nextLong();
        long id2 = RandomGenerator.getRandom().nextLong();
        Collection<Long> ids = new ArrayList<>();
        ids.add(id1);
        ids.add(id2);
        assertThrows(EntityNotFoundException.class, () -> softwareModuleFormatManagement.delete(ids));
    }

    @Test
    @Description("Sw Module Format to delete not found")
    void givenSwModuleFormatNotPresentWhenDeleteThenException(){
        long id = RandomGenerator.getRandom().nextLong();
        assertThrows(EntityNotFoundException.class, () -> softwareModuleFormatManagement.delete(id));
    }

    @Test
    @Description("Successfully deletes sw module format")
    void givenSwModuleIdWhenDeleteThenSuccess(){
        SoftwareModuleFormat format = testdataFactory.findOrCreateSoftwareModuleFormat("OS");
        softwareModuleFormatManagement.delete(format.getId());
        assertFalse(softwareModuleFormatManagement.exists(format.getId()));
    }
}
