package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.eclipse.hawkbit.security.SystemSecurityContext.USER_AUTHORIZED_TENANTS_CLAIM_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Oidc User Service")
@Story("Extract token claims from user token")
@ExtendWith(MockitoExtension.class)
class JwtAuthoritiesOidcUserServiceTest extends SecurityTestDataFactory {

    @Mock
    private JwtAuthoritiesExtractor authoritiesExtractor;

    @Mock
    private JwtAuthoritiesValidator authoritiesValidator;

    @Mock
    private OidcUserAuditService oidcUserAudit;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;

    @Mock
    private OidcUserRequest oidcUserRequest;

    private JwtAuthoritiesOidcUserService jwtAuthoritiesOidcUserService;

    @BeforeEach
    void inti() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserWhenOidcUserRequestWithStandardFollowSuccessThenLoadOidcUser() {
        jwtAuthoritiesOidcUserService = new JwtAuthoritiesOidcUserService(authoritiesExtractor, false,
                authoritiesValidator, oidcUserAudit);
        jwtAuthoritiesOidcUserService.setOauth2UserService(oAuth2UserService);

        ClientRegistration clientRegistration = createClientRegistration();
        OAuth2AccessToken auth2AccessToken = createOAuth2AccessToken();
        UserTenantAuthorities userTenantAuthorities = createUserTenantAuthorities();

        when(oidcUserRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(oidcUserRequest.getAccessToken()).thenReturn(auth2AccessToken);
        when(oidcUserRequest.getIdToken()).thenReturn(createOidcIdToken());
        when(oAuth2UserService.loadUser(oidcUserRequest)).thenReturn(createDefaultOidcUser());
        when(authoritiesExtractor.extract(clientRegistration, auth2AccessToken.getTokenValue())).thenReturn(userTenantAuthorities);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(true);

        OidcUser oidcUser = jwtAuthoritiesOidcUserService.loadUser(oidcUserRequest);
        Assertions.assertEquals(userTenantAuthorities.getUserTenants(), oidcUser.getUserInfo().getClaim(USER_AUTHORIZED_TENANTS_CLAIM_KEY));
        Assertions.assertEquals(USERNAME_ATTRIBUTE, oidcUser.getName());
        verify(oidcUserAudit).updateUserAuthenticationAudit(userTenantAuthorities);

        // Jwt authorities validator failed on user UserTenantAuthorities validation
        loadUserWhenOidcUserRequestWithValidationFailedThenThrowException(userTenantAuthorities);
    }

    @Test
    void loadUserWhenOidcUserRequestWithCustomFollowSuccessThenLoadOidcUser() {
        jwtAuthoritiesOidcUserService = new JwtAuthoritiesOidcUserService(authoritiesExtractor, true,
                authoritiesValidator, oidcUserAudit);
        jwtAuthoritiesOidcUserService.setOauth2UserService(oAuth2UserService);

        ClientRegistration clientRegistration = createClientRegistration();
        OAuth2AccessToken auth2AccessToken = createOAuth2AccessToken();
        OAuth2User oAuth2User = createDefaultOidcUser();
        UserTenantAuthorities userTenantAuthorities = createUserTenantAuthorities();
        Jwt jwtToken = createJwt();

        when(oidcUserRequest.getClientRegistration()).thenReturn(clientRegistration);
        when(oidcUserRequest.getAccessToken()).thenReturn(auth2AccessToken);
        when(oidcUserRequest.getIdToken()).thenReturn(createOidcIdToken());
        when(oAuth2UserService.loadUser(oidcUserRequest)).thenReturn(oAuth2User);
        when(authoritiesExtractor.extract(auth2AccessToken.getTokenValue())).thenReturn(jwtToken);
        when(authoritiesExtractor.extract(jwtToken)).thenReturn(userTenantAuthorities);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(true);

        OidcUser oidcUser = jwtAuthoritiesOidcUserService.loadUser(oidcUserRequest);
        Assertions.assertEquals(USERNAME_ATTRIBUTE, oidcUser.getName());
        verify(oidcUserAudit).updateUserAuthenticationAudit(userTenantAuthorities);
        verify(authoritiesExtractor).extract(eq(jwtToken), any(OidcUserInfo.class), eq(userTenantAuthorities.getUserTenants()));

        // Jwt authorities validator failed on user UserTenantAuthorities validation
        loadUserWhenOidcUserRequestWithValidationFailedThenThrowException(userTenantAuthorities);

        // Jwt authorities validator failed on JWT token validation
        loadUserWhenOidcUserRequestWithCustomValidationFailedOnJwtTokenValidationThenThrowException(jwtToken);
    }

    void loadUserWhenOidcUserRequestWithValidationFailedThenThrowException(UserTenantAuthorities userTenantAuthorities) {
        reset(authoritiesValidator);
        when(authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)).thenReturn(false);
        Assertions.assertThrows(OAuth2AuthenticationException.class,
                () -> jwtAuthoritiesOidcUserService.loadUser(oidcUserRequest));
    }

    void loadUserWhenOidcUserRequestWithCustomValidationFailedOnJwtTokenValidationThenThrowException(Jwt jwtToken) {
        doThrow(OAuth2AuthenticationException.class).when(authoritiesValidator).accessTokenValidator(jwtToken);
        Assertions.assertThrows(OAuth2AuthenticationException.class,
                () -> jwtAuthoritiesOidcUserService.loadUser(oidcUserRequest));
    }
}
