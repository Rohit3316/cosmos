/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.eclipse.hawkbit.cache.DownloadIdCache;
import org.eclipse.hawkbit.ddi.rest.resource.DdiApiConfiguration;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtApiConfiguration;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.ControllerTenantAwareAuthenticationDetailsSource;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.DosFilter;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticateAnonymousDownloadFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticateSecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedGatewaySecurityTokenFilter;
import org.eclipse.hawkbit.security.HttpControllerPreAuthenticatedSecurityHeaderFilter;
import org.eclipse.hawkbit.security.HttpDownloadAuthenticationFilter;
import org.eclipse.hawkbit.security.PreAuthTokenSourceTrustAuthenticationProvider;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.OidcBearerTokenAuthenticationFilter;
import org.eclipse.hawkbit.security.oidc.authentication.OidcRestAuthenticationEntryPoint;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.cosmos.models.ddi.DdiRestConstants.INVENTORY;
import static org.cosmos.models.ddi.DdiRestConstants.INVENTORYDETAILS;

/**
 * All configurations related to HawkBit's authentication and authorization
 * layer.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Order(value = Ordered.HIGHEST_PRECEDENCE)
@PropertySource("classpath:/hawkbit-security-defaults.properties")
public class SecurityManagedConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(SecurityManagedConfiguration.class);

    private static final int DOS_FILTER_ORDER = -200;

    private static FilterRegistrationBean<DosFilter> dosFilter(final Collection<String> includeAntPaths,
                                                               final HawkbitSecurityProperties.Dos.Filter filterProperties,
                                                               final HawkbitSecurityProperties.Clients clientProperties) {

        final FilterRegistrationBean<DosFilter> filterRegBean = new FilterRegistrationBean<>();

        filterRegBean.setFilter(new DosFilter(includeAntPaths, filterProperties.getMaxRead(),
                filterProperties.getMaxWrite(), filterProperties.getWhitelist(), clientProperties.getBlacklist(),
                clientProperties.getRemoteIpHeader()));

        return filterRegBean;
    }

    /**
     * @return the {@link UserAuthenticationFilter} to include into the hawkBit
     * security configuration.
     * @throws Exception lazy bean exception maybe if the authentication manager
     *                   cannot be instantiated
     */
    @Bean
    @ConditionalOnMissingBean
    // Exception squid:S00112 - Is aspectJ proxy
    @SuppressWarnings({"squid:S00112"})
    UserAuthenticationFilter userAuthenticationFilter(final AuthenticationConfiguration configuration)
            throws Exception {
        return new UserAuthenticationFilterBasicAuth(configuration.getAuthenticationManager());
    }

    /**
     * Filter to protect the hawkBit server system management interface against
     * to many requests.
     *
     * @param securityProperties for filter configuration
     * @return the spring filter registration bean for registering a denial of
     * service protection filter in the filter chain
     */
    @Bean
    @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
    public FilterRegistrationBean<DosFilter> dosSystemFilter(final HawkbitSecurityProperties securityProperties) {

        final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Collections.emptyList(),
                securityProperties.getDos().getFilter(), securityProperties.getClients());
        filterRegBean.setUrlPatterns(List.of("/system/*"));
        filterRegBean.setOrder(DOS_FILTER_ORDER);
        filterRegBean.setName("dosSystemFilter");

        return filterRegBean;
    }

    private static final class UserAuthenticationFilterBasicAuth extends BasicAuthenticationFilter
            implements UserAuthenticationFilter {

        private UserAuthenticationFilterBasicAuth(final AuthenticationManager authenticationManager) {
            super(authenticationManager);
        }

    }

    /**
     * Security configuration for the hawkBit server DDI interface.
     */
    @Configuration
    @Order(300)
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerSecurityConfigurationAdapter {

        private static final String[] DDI_ANT_MATCHERS = {DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/confirmation/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/actions/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/configs/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/pollings/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/allFeedback/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/feedback",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/" + INVENTORY,
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING
                        + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts",
                DdiRestConstants.DEVICE_V2_TENANTS_REQUEST_MAPPING + "/{controllerId}/actions/**",
                DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING + "/{controllerId}/" + INVENTORYDETAILS

        };

        private final ControllerManagement controllerManagement;
        private final TenantConfigurationManagement tenantConfigurationManagement;
        private final TenantAware tenantAware;
        private final DdiSecurityProperties ddiSecurityConfiguration;
        private final HawkbitSecurityProperties securityProperties;
        private final SystemSecurityContext systemSecurityContext;
        private final AuthenticationConfiguration authConfiguration;

        @Autowired
        ControllerSecurityConfigurationAdapter(final ControllerManagement controllerManagement,
                                               final TenantConfigurationManagement tenantConfigurationManagement,
                                               final TenantAware tenantAware,
                                               final DdiSecurityProperties ddiSecurityConfiguration,
                                               final HawkbitSecurityProperties securityProperties,
                                               final SystemSecurityContext systemSecurityContext,
                                               final AuthenticationConfiguration authConfiguration) {
            this.controllerManagement = controllerManagement;
            this.tenantConfigurationManagement = tenantConfigurationManagement;
            this.tenantAware = tenantAware;
            this.ddiSecurityConfiguration = ddiSecurityConfiguration;
            this.securityProperties = securityProperties;
            this.systemSecurityContext = systemSecurityContext;
            this.authConfiguration = authConfiguration;
        }

        /**
         * Filter to protect the hawkBit server DDI interface against to many
         * requests.
         *
         * @param securityProperties for filter configuration
         * @return the spring filter registration bean for registering a denial
         * of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosDDiFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(Arrays.asList(DDI_ANT_MATCHERS),
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiFilter");

            return filterRegBean;
        }

        @Bean
        public SecurityFilterChain controllerSecurityFilterChain(HttpSecurity http) throws Exception {

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource =
                    new ControllerTenantAwareAuthenticationDetailsSource();

            final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter =
                    new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                            ddiSecurityConfiguration.getRp().getCnHeader(),
                            ddiSecurityConfiguration.getRp().getSslIssuerHashHeader(),
                            tenantConfigurationManagement,
                            tenantAware, systemSecurityContext);
            securityHeaderFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            securityHeaderFilter.setCheckForPrincipalChanges(true);
            securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter =
                    new HttpControllerPreAuthenticateSecurityTokenFilter(
                            tenantConfigurationManagement, tenantAware, controllerManagement, systemSecurityContext);
            securityTokenFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            securityTokenFilter.setCheckForPrincipalChanges(true);
            securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter =
                    new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                            tenantConfigurationManagement, tenantAware, systemSecurityContext);
            gatewaySecurityTokenFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
            gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            http.securityMatchers(matchers -> matchers.requestMatchers(DDI_ANT_MATCHERS))
                    .csrf(AbstractHttpConfigurer::disable);

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
            }

            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {

                LOG.info(
                        "******************\n** Anonymous controller security enabled, should only be used for developing purposes **\n******************");

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        List.of(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
                anonymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

                http.securityContext(AbstractHttpConfigurer::disable)
                        .anonymous(anonymous -> anonymous.authenticationFilter(anonymousFilter));
            } else {

                http.addFilter(securityHeaderFilter)
                        .addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter)
                        .anonymous(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                (request, response, authException) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            }

            // Configure authentication provider
            http.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));

            return http.build();
        }
    }

    /**
     * Security configuration for the hawkBit server DDI download interface.
     */
    @Configuration
    @Order(301)
    @ConditionalOnClass(DdiApiConfiguration.class)
    static class ControllerDownloadSecurityConfigurationAdapter {

        private static final String DDI_DL_ANT_MATCHER = DdiRestConstants.DEVICE_V1_TENANTS_REQUEST_MAPPING
                + "/{controllerId}/softwaremodules/{softwareModuleId}/artifacts/*";

        private final ControllerManagement controllerManagement;
        private final TenantConfigurationManagement tenantConfigurationManagement;
        private final TenantAware tenantAware;
        private final DdiSecurityProperties ddiSecurityConfiguration;
        private final HawkbitSecurityProperties securityProperties;
        private final SystemSecurityContext systemSecurityContext;
        private final AuthenticationConfiguration authConfiguration;

        @Autowired
        ControllerDownloadSecurityConfigurationAdapter(final ControllerManagement controllerManagement,
                                                       final TenantConfigurationManagement tenantConfigurationManagement,
                                                       final TenantAware tenantAware,
                                                       final DdiSecurityProperties ddiSecurityConfiguration,
                                                       final HawkbitSecurityProperties securityProperties,
                                                       final SystemSecurityContext systemSecurityContext,
                                                       final AuthenticationConfiguration authConfiguration) {
            this.controllerManagement = controllerManagement;
            this.tenantConfigurationManagement = tenantConfigurationManagement;
            this.tenantAware = tenantAware;
            this.ddiSecurityConfiguration = ddiSecurityConfiguration;
            this.securityProperties = securityProperties;
            this.systemSecurityContext = systemSecurityContext;
            this.authConfiguration = authConfiguration;
        }

        /**
         * Filter to protect the hawkBit server DDI download interface against
         * to many requests.
         *
         * @param securityProperties for filter configuration
         * @return the spring filter registration bean for registering a denial
         * of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosDDiDlFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(List.of(DDI_DL_ANT_MATCHER),
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosDDiDlFilter");

            return filterRegBean;
        }

        @Bean
        public SecurityFilterChain controllerDownloadSecurityFilterChain(HttpSecurity http) throws Exception {

            final ControllerTenantAwareAuthenticationDetailsSource authenticationDetailsSource =
                    new ControllerTenantAwareAuthenticationDetailsSource();

            final HttpControllerPreAuthenticatedSecurityHeaderFilter securityHeaderFilter =
                    new HttpControllerPreAuthenticatedSecurityHeaderFilter(
                            ddiSecurityConfiguration.getRp().getCnHeader(),
                            ddiSecurityConfiguration.getRp().getSslIssuerHashHeader(),
                            tenantConfigurationManagement,
                            tenantAware, systemSecurityContext);
            securityHeaderFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            securityHeaderFilter.setCheckForPrincipalChanges(true);
            securityHeaderFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateSecurityTokenFilter securityTokenFilter =
                    new HttpControllerPreAuthenticateSecurityTokenFilter(
                            tenantConfigurationManagement, tenantAware, controllerManagement, systemSecurityContext);
            securityTokenFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            securityTokenFilter.setCheckForPrincipalChanges(true);
            securityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticatedGatewaySecurityTokenFilter gatewaySecurityTokenFilter =
                    new HttpControllerPreAuthenticatedGatewaySecurityTokenFilter(
                            tenantConfigurationManagement, tenantAware, systemSecurityContext);
            gatewaySecurityTokenFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            gatewaySecurityTokenFilter.setCheckForPrincipalChanges(true);
            gatewaySecurityTokenFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            final HttpControllerPreAuthenticateAnonymousDownloadFilter controllerAnonymousDownloadFilter =
                    new HttpControllerPreAuthenticateAnonymousDownloadFilter(
                            tenantConfigurationManagement, tenantAware, systemSecurityContext);
            controllerAnonymousDownloadFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());
            controllerAnonymousDownloadFilter.setCheckForPrincipalChanges(true);
            controllerAnonymousDownloadFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

            http.securityMatchers(matchers -> matchers.requestMatchers(DDI_DL_ANT_MATCHER))
                    .csrf(AbstractHttpConfigurer::disable);

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
            }

            if (ddiSecurityConfiguration.getAuthentication().getAnonymous().isEnabled()) {

                LOG.info(
                        "******************\n** Anonymous controller security enabled, should only be used for developing purposes **\n******************");

                final AnonymousAuthenticationFilter anonymousFilter = new AnonymousAuthenticationFilter(
                        "controllerAnonymousFilter", "anonymous",
                        List.of(new SimpleGrantedAuthority(SpringEvalExpressions.CONTROLLER_ROLE_ANONYMOUS)));
                anonymousFilter.setAuthenticationDetailsSource(authenticationDetailsSource);

                http.securityContext(AbstractHttpConfigurer::disable)
                        .anonymous(anonymous -> anonymous.authenticationFilter(anonymousFilter));
            } else {

                http.addFilter(securityHeaderFilter)
                        .addFilter(securityTokenFilter)
                        .addFilter(gatewaySecurityTokenFilter)
                        .addFilter(controllerAnonymousDownloadFilter)
                        .anonymous(AbstractHttpConfigurer::disable)
                        .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                        .exceptionHandling(ex -> ex.authenticationEntryPoint(
                                (request, response, authException) -> response.setStatus(HttpStatus.UNAUTHORIZED.value())))
                        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            }

            // Configure authentication provider
            http.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));

            return http.build();
        }
    }

    /**
     * A Websecurity config to handle and filter the download ids.
     */
    @Configuration
    @EnableWebSecurity
    @Order(320)
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class IdRestSecurityConfigurationAdapter {

        @Autowired
        private DdiSecurityProperties ddiSecurityConfiguration;

        @Autowired
        private DownloadIdCache downloadIdCache;

        @Autowired
        private AuthenticationConfiguration authConfiguration;

        @Bean
        public SecurityFilterChain downloadIdSecurityFilterChain(HttpSecurity http) throws Exception {

            final HttpDownloadAuthenticationFilter downloadIdAuthenticationFilter =
                    new HttpDownloadAuthenticationFilter(downloadIdCache);
            downloadIdAuthenticationFilter.setAuthenticationManager(authConfiguration.getAuthenticationManager());

            http.securityMatchers(matchers -> matchers.requestMatchers("/**/downloadId/**"))
                    .csrf(AbstractHttpConfigurer::disable)
                    .anonymous(AbstractHttpConfigurer::disable)
                    .addFilterBefore(downloadIdAuthenticationFilter, AuthorizationFilter.class)
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            // Configure authentication provider
            http.authenticationProvider(new PreAuthTokenSourceTrustAuthenticationProvider(
                    ddiSecurityConfiguration.getRp().getTrustedIPs()));

            return http.build();
        }
    }

    /**
     * Security configuration for the REST management API.
     */
    @Configuration
    @Order(350)
    @EnableWebSecurity
    @ConditionalOnClass(MgmtApiConfiguration.class)
    public static class RestSecurityConfigurationAdapter {

        @Autowired
        @Lazy
        private UserAuthenticationFilter userAuthenticationFilter;

        @Autowired(required = false)
        private OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter;

        @Autowired(required = false)
        private OidcRestAuthenticationEntryPoint oidcRestAuthenticationEntryPoint;

        @Autowired(required = false)
        private InMemoryClientRegistrationRepository clientRegistrationRepository;

        @Autowired
        private SystemManagement systemManagement;

        @Autowired
        private HawkbitSecurityProperties securityProperties;

        @Autowired
        private SystemSecurityContext systemSecurityContext;

        /**
         * Filter to protect the hawkBit server Management interface against to
         * many requests.
         *
         * @param securityProperties for filter configuration
         * @return the spring filter registration bean for registering a denial
         * of service protection filter in the filter chain
         */
        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.dos.filter", name = "enabled", matchIfMissing = true)
        public FilterRegistrationBean<DosFilter> dosMgmtFilter(final HawkbitSecurityProperties securityProperties) {

            final FilterRegistrationBean<DosFilter> filterRegBean = dosFilter(null,
                    securityProperties.getDos().getFilter(), securityProperties.getClients());
            filterRegBean.setUrlPatterns(Arrays.asList("/rest/*", "/api/*"));
            filterRegBean.setOrder(DOS_FILTER_ORDER);
            filterRegBean.setName("dosMgmtFilter");

            return filterRegBean;
        }

        @Bean
        public SecurityFilterChain restSecurityFilterChain(HttpSecurity http) throws Exception {

            http.securityMatchers(matchers -> matchers.requestMatchers("/rest/**", "/management/**", "/system/**"))
                    .csrf(AbstractHttpConfigurer::disable);

            if (securityProperties.getCors().isEnabled()) {
                http.cors(cors -> cors.configurationSource(request -> corsConfiguration()));
            }

            if (securityProperties.isRequireSsl()) {
                http.requiresChannel(channel -> channel.anyRequest().requiresSecure());
            }

            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers(MgmtRestConstants.BASE_SYSTEM_MAPPING + "/**")
                    .hasAnyAuthority(SpPermission.SYSTEM_ADMIN)
                    .anyRequest().authenticated());

            if (oidcBearerTokenAuthenticationFilter != null) {

                // Only get the first client registration. Testing against every
                // client could increase the attack vector
                final ClientRegistration clientRegistration = clientRegistrationRepository != null
                        && clientRegistrationRepository.iterator().hasNext()
                        ? clientRegistrationRepository.iterator().next()
                        : null;

                if (Objects.isNull(clientRegistration) || Objects.isNull(clientRegistration.getProviderDetails())) {
                    throw new IllegalArgumentException("There must be a valid client registration");
                }

                if (clientRegistration.getProviderDetails().getJwkSetUri() != null) {
                    http.oauth2ResourceServer(oauth2 -> oauth2
                            .jwt(jwt -> jwt.jwkSetUri(clientRegistration.getProviderDetails().getJwkSetUri()))
                            .authenticationEntryPoint(oidcRestAuthenticationEntryPoint));
                }

                oidcBearerTokenAuthenticationFilter.setClientRegistration(clientRegistration);

                http.addFilterAfter(oidcBearerTokenAuthenticationFilter, BearerTokenAuthenticationFilter.class);
            } else {
                final BasicAuthenticationEntryPoint basicAuthEntryPoint = new BasicAuthenticationEntryPoint();
                basicAuthEntryPoint.setRealmName(securityProperties.getBasicRealm());

                http.addFilterBefore(new Filter() {
                    @Override
                    public void init(final FilterConfig filterConfig) throws ServletException {
                        userAuthenticationFilter.init(filterConfig);
                    }

                    @Override
                    public void doFilter(final ServletRequest request, final ServletResponse response,
                                         final FilterChain chain) throws IOException, ServletException {
                        userAuthenticationFilter.doFilter(request, response, chain);
                    }

                    @Override
                    public void destroy() {
                        userAuthenticationFilter.destroy();
                    }
                }, RequestHeaderAuthenticationFilter.class);

                http.httpBasic(basic -> basic.authenticationEntryPoint(basicAuthEntryPoint));
            }

            http.addFilterAfter(
                    new AuthenticationSuccessTenantMetadataCreationFilter(systemManagement, systemSecurityContext),
                    SessionManagementFilter.class);

            http.anonymous(AbstractHttpConfigurer::disable)
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

            return http.build();
        }

        @Bean
        @ConditionalOnProperty(prefix = "hawkbit.server.security.cors", name = "enabled", matchIfMissing = false)
        CorsConfiguration corsConfiguration() {
            final CorsConfiguration corsConfiguration = new CorsConfiguration();

            corsConfiguration.setAllowedOrigins(securityProperties.getCors().getAllowedOrigins());
            corsConfiguration.setAllowCredentials(true);
            corsConfiguration.setAllowedHeaders(securityProperties.getCors().getAllowedHeaders());
            corsConfiguration.setAllowedMethods(securityProperties.getCors().getAllowedMethods());
            corsConfiguration.setExposedHeaders(securityProperties.getCors().getExposedHeaders());

            return corsConfiguration;
        }
    }
}

/**
 * Servletfilter to create metadata after successful authentication over
 * RESTful.
 */
class AuthenticationSuccessTenantMetadataCreationFilter implements Filter {

    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;

    AuthenticationSuccessTenantMetadataCreationFilter(final SystemManagement systemManagement,
                                                      final SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // not needed
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        lazyCreateTenantMetadata();
        chain.doFilter(request, response);

    }

    private void lazyCreateTenantMetadata() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            systemSecurityContext.runAsSystem(systemManagement::getTenantMetadata);
        }
    }

    @Override
    public void destroy() {
        // not needed
    }

}
