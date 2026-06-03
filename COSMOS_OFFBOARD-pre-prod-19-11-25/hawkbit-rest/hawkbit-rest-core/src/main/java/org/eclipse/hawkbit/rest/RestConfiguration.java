/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest;

import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.hawkbit.rest.aspect.TenantValidationAspect;
import org.eclipse.hawkbit.rest.aspect.TraceableAnnotationsHandler;
import org.eclipse.hawkbit.rest.exception.ResponseExceptionHandler;
import org.eclipse.hawkbit.rest.filter.ExcludePathAwareShallowETagFilter;
import org.eclipse.hawkbit.rest.swagger.SwaggerConfiguration;
import org.eclipse.hawkbit.rest.util.FilterHttpResponse;
import org.eclipse.hawkbit.rest.util.HttpResponseFactoryBean;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.Scope;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.context.WebApplicationContext;

/**
 * Configuration for Rest api.
 */
@Configuration
@PropertySource("classpath:/hawkbit-rest-core-defaults.properties")
@EnableHypermediaSupport(type = {HypermediaType.HAL})
@ImportAutoConfiguration(SwaggerConfiguration.class)
public class RestConfiguration {

    /**
     * Create filter for {@link HttpServletResponse}.
     */
    @Bean
    FilterHttpResponse filterHttpResponse() {
        return new FilterHttpResponse();
    }

    /**
     * Creates and returns a TenantValidationAspect bean.
     *
     * This bean enables the aspect-oriented programming (AOP)
     * for tenant ID validation in requests annotated with @TenantAware.
     *
     * @return a new instance of TenantValidationAspect
     */
    @Bean
    TenantValidationAspect tenantValidationAspect() {
        return new TenantValidationAspect();
    }

    /**
     * Creates and returns a TraceAspect bean.
     *
     * This bean enables the aspect-oriented programming (AOP)
     * for tracing method parameters annotated with
     * @TraceableMethod, @TraceableObject and @TraceableField.
     *
     * @return a new instance of TraceAspect
     */
    @Bean
    TraceableAnnotationsHandler traceAspect() {
        return new TraceableAnnotationsHandler();
    }

    /**
     * Create factory bean for {@link HttpServletResponse}.
     */
    @Bean
    FactoryBean<HttpServletResponse> httpResponseFactoryBean() {
        return new HttpResponseFactoryBean();
    }

    /**
     * Create factory bean for {@link HttpServletResponse}.
     */
    @Bean
    @Scope(value = WebApplicationContext.SCOPE_REQUEST)
    RequestResponseContextHolder requestResponseContextHolder() {
        return new RequestResponseContextHolder();
    }

    /**
     * {@link ControllerAdvice} for mapping {@link RuntimeException}s from the
     * repository to {@link HttpStatus} codes.
     */
    @Bean
    ResponseExceptionHandler responseExceptionHandler() {
        return new ResponseExceptionHandler();
    }

    /**
     * Filter registration bean for spring etag filter.
     *
     * @return the spring filter registration bean for registering an etag
     *         filter in the filter chain
     */
    @Bean
    FilterRegistrationBean<ExcludePathAwareShallowETagFilter> eTagFilter() {

        final FilterRegistrationBean<ExcludePathAwareShallowETagFilter> filterRegBean = new FilterRegistrationBean<>();
        // Exclude the URLs for downloading artifacts, so no eTag is generated
        // in the ShallowEtagHeaderFilter, just using the SH1 hash of the
        // artifact itself as 'ETag', because otherwise the file will be copied
        // in memory!
        filterRegBean.setFilter(new ExcludePathAwareShallowETagFilter(
                "/management/v1/softwaremodules/{smId}/artifacts/{artId}/download",
                "/device/v1/controllers/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/**",
                "/api/v1/downloadserver/**"));

        return filterRegBean;
    }
}
