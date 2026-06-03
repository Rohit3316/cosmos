/**
 * Copyright (c) 2019 Kiwigrid GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.autoconfigure.security;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.RoleRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.UserRepository;
import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.JwtAuthoritiesExtractor;
import org.eclipse.hawkbit.security.oidc.authentication.JwtAuthoritiesOidcUserService;
import org.eclipse.hawkbit.security.oidc.authentication.JwtAuthoritiesValidator;
import org.eclipse.hawkbit.security.oidc.authentication.OidcAuthenticationSuccessHandler;
import org.eclipse.hawkbit.security.oidc.authentication.OidcBearerTokenAuthenticationFilter;
import org.eclipse.hawkbit.security.oidc.authentication.OidcLogoutHandler;
import org.eclipse.hawkbit.security.oidc.authentication.OidcLogoutSuccessHandler;
import org.eclipse.hawkbit.security.oidc.authentication.OidcRestAuthenticationEntryPoint;
import org.eclipse.hawkbit.security.oidc.authentication.OidcUserAuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.oauth2.client.ClientsConfiguredCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.authority.mapping.SimpleAuthorityMapper;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

/**
 * Auto-configuration for OpenID Connect user management.
 */
@Configuration
@Conditional(value = ClientsConfiguredCondition.class)
public class OidcUserManagementAutoConfiguration {

    @Value("#{'${spring.security.oauth2.client.provider.oidc.type:}' == 'custom'}")
    private boolean customOidc;


    /**
     * @return the oauth2 user details service to load a user from oidc user
     * manager
     */
    @Bean
    @ConditionalOnMissingBean
    public OAuth2UserService<OidcUserRequest, OidcUser> oidcUserDetailsService(final JwtAuthoritiesExtractor extractor,
                                                                               final JwtAuthoritiesValidator authoritiesValidator,
                                                                               final OidcUserAuditService oidcUserAudit) {
        return new JwtAuthoritiesOidcUserService(extractor, customOidc, authoritiesValidator, oidcUserAudit);
    }

    /**
     * @return the logout success handler for OpenID Connect
     */
    @Bean
    public LogoutSuccessHandler oidcLogoutSuccessHandler() {
        return new OidcLogoutSuccessHandler(customOidc);
    }

    /**
     * @return the OpenID Connect authentication success handler
     */
    @Bean
    public AuthenticationSuccessHandler oidcAuthenticationSuccessHandler(
            final SystemManagement systemManagement, final SystemSecurityContext systemSecurityContext) {
        return new OidcAuthenticationSuccessHandler(systemManagement, systemSecurityContext);
    }

    /**
     * @return the OpenID Connect logout handler
     */
    @Bean
    public LogoutHandler oidcLogoutHandler() {
        return new OidcLogoutHandler(customOidc);
    }

    /**
     * @return a jwt authorities extractor which interprets the roles of a user
     * as their authorities.
     */

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthoritiesValidator jwtAuthoritiesValidator(final OidcSecurityProperties oidcSecurityProperties) {
        return new JwtAuthoritiesValidator(oidcSecurityProperties);
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthoritiesExtractor jwtAuthoritiesExtractor(final RoleRepository roleRepository,
                                                           final TenantMetaDataRepository tenantMetaDataRepository,
                                                           final UserRepository userRepository,
                                                           final OidcUserAuditService oidcUserAudit,
                                                           final SystemSecurityContext systemSecurityContext,
                                                           final OidcSecurityProperties oidcSecurityProperties) {
        final SimpleAuthorityMapper authorityMapper = new SimpleAuthorityMapper();
        authorityMapper.setPrefix("");
        authorityMapper.setConvertToUpperCase(true);

        return new JwtAuthoritiesExtractor(authorityMapper, roleRepository, tenantMetaDataRepository, userRepository,
                oidcUserAudit, systemSecurityContext, oidcSecurityProperties);
    }

    /**
     * @return an authentication filter for using OAuth2 Bearer Tokens.
     */
    @Bean
    @ConditionalOnMissingBean
    public OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter(final JwtAuthoritiesExtractor authoritiesExtractor,
                                                                                   final JwtAuthoritiesValidator authoritiesValidator,
                                                                                   final OidcUserAuditService userAudit,
                                                                                   final SystemManagement systemManagement) {
        return new OidcBearerTokenAuthenticationFilter(customOidc, authoritiesExtractor, authoritiesValidator, userAudit, systemManagement);
    }

    /**
     * @return the OpenID Connect rest authentication exception entry point
     */
    @Bean
    public OidcRestAuthenticationEntryPoint oidcRestAuthenticationEntryPoint() {
        return new OidcRestAuthenticationEntryPoint();
    }

    /**
     * @return OidcUserAudit
     * auditing user authentication activities.
     */
    @Bean
    public OidcUserAuditService oidcUserAudit() {
        return new OidcUserAuditService();
    }
}