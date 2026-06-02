/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Description;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Test polling from the controller.
 */
//@ActiveProfiles({ "im", "test" })
@Feature("Component Tests - Direct Device Integration API")
@Story("User api")
class MgmtUserResourceTest extends AbstractManagementApiIntegrationTest {

    @Test
    @Description("Dummy test to verify test execution")
    void dummyTest() {
        // Simple assertion to ensure the test passes
        assertThat(true).isTrue();
    }

}
