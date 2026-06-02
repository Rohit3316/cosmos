/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.app;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.ResultActions;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"hawkbit.dmf.rabbitmq.enabled=false",
        "springfox.documentation.enabled=false",
        "aws.paramstore.enabled=false",
        "hawkbit.server.security.cors.enabled=true",
        "hawkbit.server.security.cors.allowedOrigins=" + CorsTest.ALLOWED_ORIGIN_FIRST + ","
                + CorsTest.ALLOWED_ORIGIN_SECOND,
        "hawkbit.server.security.cors.exposedHeaders=Access-Control-Allow-Origin"})
@Feature("Integration Test - Security")
@Story("CORS")
class CorsTest extends AbstractSecurityTest {

    static final String ALLOWED_ORIGIN_FIRST = "http://test.first.origin";
    static final String ALLOWED_ORIGIN_SECOND = "http://test.second.origin";

    private static final String INVALID_ORIGIN = "http://test.invalid.origin";
    private static final String INVALID_CORS_REQUEST = "Invalid CORS request";

    @WithUserDetails("admin")
    @Test
    @Description("Ensures that Cors is working.")
    void validateCorsRequest() throws Exception {
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_FIRST).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_FIRST));
        performOptionsRequestToRestWithOrigin(ALLOWED_ORIGIN_SECOND).andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN_SECOND));

        final String invalidOriginResponseBody = performOptionsRequestToRestWithOrigin(INVALID_ORIGIN)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn().getResponse()
                .getContentAsString();
        assertThat(invalidOriginResponseBody).isEqualTo(INVALID_CORS_REQUEST);

        final String ddiBaseRequest = DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING.replace("{tenant}", "DEFAULT");
        final String invalidCorsUrlResponseBody = performOptionsRequestToUrlWithOrigin(ddiBaseRequest, ALLOWED_ORIGIN_FIRST)
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).andReturn()
                .getResponse().getContentAsString();
        assertThat(invalidCorsUrlResponseBody).isEqualTo(INVALID_CORS_REQUEST);
    }

    private ResultActions performOptionsRequestToRestWithOrigin(final String origin) throws Exception {
        return performOptionsRequestToUrlWithOrigin(MgmtRestConstants.BASE_V1_REQUEST_MAPPING, origin);
    }

    private ResultActions performOptionsRequestToUrlWithOrigin(final String url, final String origin) throws Exception {
        return mvc.perform(options(url).header("Access-Control-Request-Method", "GET").header("Origin", origin));
    }
}