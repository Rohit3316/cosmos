package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Oidc Bearer Token Authentication Filter")
@Story("Extract token claims from Bearer token")
@ExtendWith(MockitoExtension.class)
class OidcBearerTokenAuthenticationFilterTest extends SecurityTestDataFactory {

    @Mock
    private JwtAuthoritiesExtractor authoritiesExtractor;

    @Mock
    private JwtAuthoritiesValidator authoritiesValidator;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private OidcUserAuditService userAudit;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private ServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Captor
    private ArgumentCaptor<OAuth2AuthenticationToken> tokenArgumentCaptor;

    private OidcBearerTokenAuthenticationFilter oidcBearerTokenAuthenticationFilter;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void doFilterWhenAuthenticationObjectNotInstanceJwtAuthenticationTokenThenExit() throws ServletException, IOException {
        oidcBearerTokenAuthenticationFilter = new OidcBearerTokenAuthenticationFilter(true,
                authoritiesExtractor, authoritiesValidator, userAudit, systemManagement);
        when(securityContext.getAuthentication()).thenReturn(new JaasAuthenticationToken(null, null, null));
        oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain);
        verify(chain).doFilter(request, response);
    }

    @Test
    void doFilterCustomProviderWhenAuthenticationObjectInstanceJwtAuthenticationTokenThenAuthenticate() throws ServletException, IOException {
        oidcBearerTokenAuthenticationFilter = new OidcBearerTokenAuthenticationFilter(true,
                authoritiesExtractor, authoritiesValidator, userAudit, systemManagement);
        oidcBearerTokenAuthenticationFilter.setClientRegistration(createClientRegistration());
        Jwt jwtToken = createJwt();
        UserTenantAuthorities userTenantAuthorities = createUserTenantAuthorities();
        OidcUserInfo userInfo = createOidcUserInfo();

        when(securityContext.getAuthentication()).thenReturn(new JwtAuthenticationToken(jwtToken));
        when(authoritiesExtractor.extract(jwtToken)).thenReturn(userTenantAuthorities);
        when(authoritiesExtractor.extract(eq(jwtToken), any(OidcUserInfo.class),
                eq(userTenantAuthorities.getUserTenants()))).thenReturn(userInfo);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(true);
        when(systemManagement.getUserPreferredTenant(userTenantAuthorities.getUser().getUsername())).thenReturn(null);

        oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain);
        verify(authoritiesValidator).accessTokenValidator(jwtToken);
        verify(userAudit).updateUserAuthenticationAudit(userTenantAuthorities);
        verify(securityContext).setAuthentication(tokenArgumentCaptor.capture());
        OAuth2AuthenticationToken oAuth2AuthenticationToken = tokenArgumentCaptor.getValue();
        Assertions.assertEquals(userTenantAuthorities.getUserAuthorities(), new HashSet<>(oAuth2AuthenticationToken.getAuthorities()));
        Assertions.assertEquals(REGISTRATION, oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
        Assertions.assertEquals(USERNAME_ATTRIBUTE, oAuth2AuthenticationToken.getPrincipal().getName());

        //Validate there is no preferred tenant.
        TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails = (TenantAwareAuthenticationDetails) oAuth2AuthenticationToken.getDetails();
        Assertions.assertTrue(userTenantAuthorities.getUserTenants().contains(tenantAwareAuthenticationDetails.getTenant()));

        //Validate there is preferred tenant.
        doFilterCustomProviderWhenAuthInstanceJATThenAuthWithPreferredTenant(userTenantAuthorities);
    }

    void doFilterCustomProviderWhenAuthInstanceJATThenAuthWithPreferredTenant(UserTenantAuthorities userTenantAuthorities) throws ServletException, IOException {
        reset(systemManagement);
        TenantMetaData tenantMetaData = createTenantMetaData(TENANT);
        when(systemManagement.getUserPreferredTenant(userTenantAuthorities.getUser().getUsername())).thenReturn(tenantMetaData);
        oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain);
        verify(securityContext, times(2)).setAuthentication(tokenArgumentCaptor.capture());
        TenantAwareAuthenticationDetails tenantAwareAuthenticationDetails = (TenantAwareAuthenticationDetails) tokenArgumentCaptor.getValue().getDetails();
        Assertions.assertEquals(tenantMetaData.getTenant(), tenantAwareAuthenticationDetails.getTenant());
    }

    @Test
    void doFilterStandardProviderWhenAuthInstanceJATThenAuthenticate() throws ServletException, IOException {
        oidcBearerTokenAuthenticationFilter = new OidcBearerTokenAuthenticationFilter(false,
                authoritiesExtractor, authoritiesValidator, userAudit, systemManagement);
        ClientRegistration clientRegistration = createClientRegistration();
        Jwt jwtToken = createJwt();
        UserTenantAuthorities userTenantAuthorities = createUserTenantAuthorities();

        oidcBearerTokenAuthenticationFilter.setClientRegistration(clientRegistration);
        when(securityContext.getAuthentication()).thenReturn(new JwtAuthenticationToken(jwtToken));
        when(authoritiesExtractor.extract(clientRegistration.getClientId(), jwtToken)).thenReturn(userTenantAuthorities);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(true);

        oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain);
        verify(userAudit).updateUserAuthenticationAudit(userTenantAuthorities);
        verify(securityContext).setAuthentication(tokenArgumentCaptor.capture());
        OAuth2AuthenticationToken oAuth2AuthenticationToken = tokenArgumentCaptor.getValue();

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE));
        claims.put("tenants", Set.of(DEFAULT_TENANT));

        Assertions.assertEquals(claims, ((DefaultOidcUser) oAuth2AuthenticationToken.getPrincipal()).getUserInfo().getClaims());
    }

    @Test
    void doFilterWhenAuthoritiesValidatorFailedThenThrowsException() throws ServletException, IOException {
        oidcBearerTokenAuthenticationFilter = new OidcBearerTokenAuthenticationFilter(true,
                authoritiesExtractor, authoritiesValidator, userAudit, systemManagement);
        oidcBearerTokenAuthenticationFilter.setClientRegistration(createClientRegistration());
        Jwt jwtToken = createJwt();
        UserTenantAuthorities userTenantAuthorities = createUserTenantAuthorities();
        OidcUserInfo userInfo = createOidcUserInfo();

        when(securityContext.getAuthentication()).thenReturn(new JwtAuthenticationToken(jwtToken));

        doThrow(OAuth2AuthenticationException.class).when(authoritiesValidator).accessTokenValidator(jwtToken);
        Assertions.assertThrows(OAuth2AuthenticationException.class, () ->
            oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain)
        );

        // Validator failed on User Tenant Authorities validation.
        reset(authoritiesValidator);
        doNothing().when(authoritiesValidator).accessTokenValidator(jwtToken);
        when(authoritiesExtractor.extract(jwtToken)).thenReturn(userTenantAuthorities);
        when(authoritiesExtractor.extract(eq(jwtToken), any(OidcUserInfo.class),
                eq(userTenantAuthorities.getUserTenants()))).thenReturn(userInfo);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(false);
        oidcBearerTokenAuthenticationFilter.doFilter(request, response, chain);
        verify(response).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

}
