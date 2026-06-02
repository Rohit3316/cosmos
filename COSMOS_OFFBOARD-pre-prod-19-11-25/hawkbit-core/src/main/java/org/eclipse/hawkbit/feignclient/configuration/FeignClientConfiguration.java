/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.feignclient.configuration;

import feign.Request;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Configuration class for customizing Feign client behavior.
 * <p>
 * It provides:
 * <ul>
 *   <li>A generic error decoder to handle Feign client exceptions.</li>
 *   <li>Custom request options with adjusted timeouts to accommodate longer processing APIs.</li>
 * </ul>
 */
@Configuration
public class FeignClientConfiguration {

    private static final int CONNECT_TIMEOUT = 10; //This is the default value - 10 seconds
    private static final int READ_TIMEOUT = 180; // Updating read timeout to 3 minutes, it is because the API internally has 3 re-tries to connect/send the
    // message to the Kafka server, this might take a while before the API responds back.

    /**
     * Provides a generic error decoder for Feign clients.
     * This decoder handles exceptions thrown during remote service invocation.
     *
     * @return a custom implementation of {@link ErrorDecoder}
     */
    @Bean
    public ErrorDecoder genericClientErrorDecoder() {
        return new GeneralClientErrorDecoder();
    }

    /**
     * Configures request options for all Feign clients using this configuration.
     * <p>
     * Sets:
     * <ul>
     *   <li>Connection timeout: 10 seconds (default)</li>
     *   <li>Read timeout: 180 seconds — increased to account for retry logic and potentially slow downstream processing</li>
     * </ul>
     *
     * @return customized {@link Request.Options}
     */
    @Bean
    public Request.Options requestOptions() {
        return new Request.Options(CONNECT_TIMEOUT, SECONDS, READ_TIMEOUT, SECONDS, true);
    }
}
