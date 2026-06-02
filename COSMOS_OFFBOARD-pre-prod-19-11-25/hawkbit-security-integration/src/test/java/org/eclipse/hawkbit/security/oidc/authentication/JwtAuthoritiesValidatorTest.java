package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Jwt Token Validator")
@Story("Validate Jwt token")
@ExtendWith(MockitoExtension.class)
class JwtAuthoritiesValidatorTest extends SecurityTestDataFactory {

    private static final String INVALID = "invalid";
    @Mock
    private OidcSecurityProperties oidcSecurityProperties;
    @Mock
    private OidcSecurityProperties.Provider provider;
    @Mock
    private OidcSecurityProperties.Registration registration;


    private JwtAuthoritiesValidator jwtAuthoritiesValidator;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        jwtAuthoritiesValidator = new JwtAuthoritiesValidator(oidcSecurityProperties);
    }

    @Test
    void accessTokenValidatorWhenValidateJwtThenSuccess() {
        when(oidcSecurityProperties.getProvider()).thenReturn(provider);
        when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
        OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
        when(provider.getOidc()).thenReturn(oidc);
        OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
        when(registration.getOidc()).thenReturn(reOidc);

        Jwt jwt = createJwt();
        jwtAuthoritiesValidator.accessTokenValidator(jwt);
        verify(oidcSecurityProperties).getProvider();
    }

    @Test
    void accessTokenValidatorWhenValidateJwtThenSuccessOnPrdScopeNotPresent() {
        when(oidcSecurityProperties.getProvider()).thenReturn(provider);
        when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
        OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
        oidc.setAuthorizedClients(List.of(CLIENT_ID));
        when(provider.getOidc()).thenReturn(oidc);
        OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
        reOidc.setClientId("");
        reOidc.setScope("");
        when(registration.getOidc()).thenReturn(reOidc);

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE));
        claims.put(SCOPE_AUG_CLAIM, new ArrayList<>(Collections.singleton(CLIENT_ID)));
        Jwt jwt = createJwt(claims);
        jwtAuthoritiesValidator.accessTokenValidator(jwt);
        verify(oidcSecurityProperties, times(3)).getProvider();
    }

    @Test
    void accessTokenValidatorWhenValidateJwtThenFailedOnIssuer() {
        when(oidcSecurityProperties.getProvider()).thenReturn(provider);
        OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
        when(provider.getOidc()).thenReturn(oidc);

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE));
        claims.put(ISSUER_CLAIM, INVALID);
        Jwt jwt = createJwt(claims);
        Assertions.assertThrows(OAuth2AuthenticationException.class, () -> jwtAuthoritiesValidator.accessTokenValidator(jwt));
    }

    @Test
    void accessTokenValidatorWhenValidateJwtThenFailedOnClientId() {
        when(oidcSecurityProperties.getProvider()).thenReturn(provider);
        when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
        OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
        when(provider.getOidc()).thenReturn(oidc);
        OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
        when(registration.getOidc()).thenReturn(reOidc);

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE));
        claims.put(CLIENT_ID_CLAIM, INVALID);
        Jwt jwt = createJwt(claims);
        Assertions.assertThrows(OAuth2AuthenticationException.class, () -> jwtAuthoritiesValidator.accessTokenValidator(jwt));
    }

    @Test
    void accessTokenValidatorWhenValidateJwtThenFailedOnScope() {
        when(oidcSecurityProperties.getProvider()).thenReturn(provider);
        when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
        OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
        when(provider.getOidc()).thenReturn(oidc);
        OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
        when(registration.getOidc()).thenReturn(reOidc);

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE));
        claims.put(SCOPE_AUG_CLAIM, new ArrayList<>(Collections.singleton(INVALID)));
        Jwt jwt = createJwt(claims);
        Assertions.assertThrows(OAuth2AuthenticationException.class, () -> jwtAuthoritiesValidator.accessTokenValidator(jwt));
    }
}