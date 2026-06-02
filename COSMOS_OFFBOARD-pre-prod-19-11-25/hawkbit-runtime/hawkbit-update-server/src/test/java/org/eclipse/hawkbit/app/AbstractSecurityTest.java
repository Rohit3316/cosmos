/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import org.eclipse.hawkbit.repository.test.util.SharedSqlTestDatabaseExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.function.FunctionConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = {"hawkbit.dmf.rabbitmq.enabled=false",
        "springfox.documentation.enabled=false", "aws.paramstore.enabled=false"})
@ImportAutoConfiguration(exclude = {FunctionConfiguration.class})
@Import(MockSqsConfig.class)
@ExtendWith(SharedSqlTestDatabaseExtension.class)
@ActiveProfiles({"test"})
@TestPropertySource(locations = "classpath:/default-test.properties")
public abstract class AbstractSecurityTest {

    protected MockMvc mvc;
    @Autowired
    private WebApplicationContext context;

    @BeforeEach
    public void setup() {
        final DefaultMockMvcBuilder builder = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity()).dispatchOptions(true);
        mvc = builder.build();
    }

}
