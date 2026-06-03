package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import org.eclipse.hawkbit.repository.jpa.RoleRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.UserRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.exception.OAuth2AccessDeniedException;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.eclipse.hawkbit.security.util.JwtUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.ALL_TENANTS;
import static org.eclipse.hawkbit.security.SystemSecurityContext.USER_AUTHORIZED_TENANTS_CLAIM_KEY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Jwt Token Extractor")
@Story("Extract token claims from Jwt token")
@ExtendWith(MockitoExtension.class)
class JwtAuthoritiesExtractorTest extends SecurityTestDataFactory {

    @Captor
    ArgumentCaptor<Callable<JpaUser>> callableCaptor;
    @Mock
    private GrantedAuthoritiesMapper authoritiesMapper;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private TenantMetaDataRepository tenantMetaDataRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private OidcUserAuditService oidcUserAudit;
    @Mock
    private SystemSecurityContext systemSecurityContext;
    @Mock
    private OidcSecurityProperties oidcSecurityProperties;
    @Mock
    private OidcSecurityProperties.Provider provider;
    @Mock
    private OidcSecurityProperties.Registration registration;
    @Mock
    private Jwt jwtToken;
    private JwtAuthoritiesExtractor jwtAuthoritiesExtractor;

    @BeforeEach
    public void init() {
        jwtAuthoritiesExtractor = new JwtAuthoritiesExtractor(authoritiesMapper, roleRepository, tenantMetaDataRepository,
                userRepository, oidcUserAudit, systemSecurityContext, oidcSecurityProperties);
    }

    @Test
    void extractUserTenantAuthoritiesFromClientRegistrationAndTokenStringCaughtExceptions() {
        try (MockedStatic<JwtUtil> utilities = Mockito.mockStatic(JwtUtil.class)) {
            NimbusJwtDecoder nimbusJwtDecoder = mock(NimbusJwtDecoder.class);

            OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
            when(oidcSecurityProperties.getProvider()).thenReturn(provider);
            when(provider.getOidc()).thenReturn(oidc);
            utilities.when(() -> JwtUtil.decoder(oidc.getJwkSetUri(), oidc.getIssuerUri()))
                    .thenReturn(nimbusJwtDecoder);

            when(nimbusJwtDecoder.decode(TOKEN)).thenReturn(jwtToken);
            when(jwtToken.getClaims()).thenReturn(null);

            // Token with invalid claims
            ClientRegistration clientRegistration = createClientRegistration();
            Assertions.assertThrows(OAuth2AuthenticationException.class,
                    () -> jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN));

            // Token with empty claims
            reset(jwtToken);
            Map<String, Object> claims = createStandardSSOClaims(Collections.emptyList());
            when(jwtToken.getClaims()).thenReturn(claims);
            Assertions.assertNull(jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN));

            //Token with invalid role structure
            reset(jwtToken);
            Map<String, Object> invalidClaims = createStandardSSOClaims(List.of("testRole"));
            when(jwtToken.getClaims()).thenReturn(invalidClaims);
            Assertions.assertThrows(OAuth2AuthenticationException.class,
                    () -> jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN));
        }
    }

    @Test
    void extractUTAFromCRAndTokenStringForNewUserWithInvalidRoleOrTenant() {
        try (MockedStatic<JwtUtil> utilities = Mockito.mockStatic(JwtUtil.class)) {
            NimbusJwtDecoder nimbusJwtDecoder = mock(NimbusJwtDecoder.class);

            OidcSecurityProperties.Provider.Oidc oidc = createProviderOidc();
            when(oidcSecurityProperties.getProvider()).thenReturn(provider);
            when(provider.getOidc()).thenReturn(oidc);
            utilities.when(() -> JwtUtil.decoder(oidc.getJwkSetUri(), oidc.getIssuerUri()))
                    .thenReturn(nimbusJwtDecoder);

            when(nimbusJwtDecoder.decode(TOKEN)).thenReturn(jwtToken);
            Map<String, Object> claims = createStandardSSOClaims(List.of(ROLE_CLAIMS));
            when(jwtToken.getClaims()).thenReturn(claims);
            when(jwtToken.getClaimAsString(USERNAME_CLAIM)).thenReturn(USERNAME_CLAIM);

            // User creation
            when(userRepository.findByUsername(USERNAME_CLAIM)).thenReturn(Optional.empty());
            when(jwtToken.getClaimAsString(FIRSTNAME_CLAIM)).thenReturn(FIRSTNAME_CLAIM);
            when(jwtToken.getClaimAsString(LASTNAME_CLAIM)).thenReturn(LASTNAME_CLAIM);
            when(jwtToken.getClaimAsString(EMAIL_CLAIM)).thenReturn(EMAIL_CLAIM);


            ClientRegistration clientRegistration = createClientRegistration();

            // Invalid role
            when(roleRepository.findByName(ROLE)).thenReturn(Optional.empty());
            JpaTenantMetaData tenantMetaData = createTenantMetaData(TENANT);
            when(tenantMetaDataRepository.findByTenantIgnoreCase(TENANT)).thenReturn(tenantMetaData);
            Assertions.assertThrows(OAuth2AccessDeniedException.class,
                    () -> jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN));

            // Invalid tenant
            reset(roleRepository, tenantMetaDataRepository);
            when(roleRepository.findByName(ROLE)).thenReturn(Optional.of(createRole(ROLE)));
            when(tenantMetaDataRepository.findByTenantIgnoreCase(TENANT)).thenReturn(null);
            Assertions.assertThrows(OAuth2AccessDeniedException.class,
                    () -> jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN));
        }
    }

    @Test
    void extractUTAFromCRAndTokenStringForNewUserWithValidRoleAndTenant() throws Exception {
        try (MockedStatic<JwtUtil> utilities = Mockito.mockStatic(JwtUtil.class)) {
            NimbusJwtDecoder nimbusJwtDecoder = mock(NimbusJwtDecoder.class);

            OidcSecurityProperties.Provider.Oidc providerOidc = createProviderOidc();
            when(oidcSecurityProperties.getProvider()).thenReturn(provider);
            when(provider.getOidc()).thenReturn(providerOidc);
            utilities.when(() -> JwtUtil.decoder(providerOidc.getJwkSetUri(), providerOidc.getIssuerUri()))
                    .thenReturn(nimbusJwtDecoder);

            when(nimbusJwtDecoder.decode(TOKEN)).thenReturn(jwtToken);
            Map<String, Object> claims = createStandardSSOClaims(List.of(ROLE_CLAIMS));
            when(jwtToken.getClaims()).thenReturn(claims);
            when(jwtToken.getClaimAsString(USERNAME_CLAIM)).thenReturn(USERNAME_CLAIM);

            // User creation
            when(userRepository.findByUsername(USERNAME_CLAIM)).thenReturn(Optional.empty());
            when(jwtToken.getClaimAsString(FIRSTNAME_CLAIM)).thenReturn(FIRSTNAME_CLAIM);
            when(jwtToken.getClaimAsString(LASTNAME_CLAIM)).thenReturn(LASTNAME_CLAIM);
            when(jwtToken.getClaimAsString(EMAIL_CLAIM)).thenReturn(EMAIL_CLAIM);


            ClientRegistration clientRegistration = createClientRegistration();

            when(roleRepository.findByName(ROLE)).thenReturn(Optional.of(createRole(ROLE)));
            when(tenantMetaDataRepository.findByTenantIgnoreCase(TENANT)).thenReturn(createTenantMetaData(TENANT));
            when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
            OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
            when(registration.getOidc()).thenReturn(reOidc);
            when(systemSecurityContext.runAsSystem(any())).thenReturn(createUser(TENANT, ROLE));

            UserTenantAuthorities userTenantAuthorities = jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN);

            // Verify synchronize user with COSMOS i.e. authUserSynchronize
            verify(systemSecurityContext).runAsSystem(callableCaptor.capture());
            Callable<JpaUser> callable = callableCaptor.getValue();
            callable.call();
            verify(userRepository).save(createUser(TENANT, ROLE));

            // Verify synchronize user with COSMOS i.e. authUserSynchronize
            verify(oidcUserAudit).insertUserAuthenticationAudit(createUser(TENANT, ROLE), createRole(ROLE), createTenantMetaData(TENANT));

            verify(authoritiesMapper).mapAuthorities(AuthorityUtils.createAuthorityList(ROLE, ROLE_PERMISSION));
            Assertions.assertEquals(createUser(TENANT, ROLE), userTenantAuthorities.getUser());
            Assertions.assertEquals(Set.of(TENANT), userTenantAuthorities.getUserTenants());
            jwtAuthoritiesExtractor = new JwtAuthoritiesExtractor(null, roleRepository, tenantMetaDataRepository,
                    userRepository, oidcUserAudit, systemSecurityContext, oidcSecurityProperties);
            userTenantAuthorities = jwtAuthoritiesExtractor.extract(clientRegistration, TOKEN);
            Assertions.assertEquals(createUser(TENANT, ROLE), userTenantAuthorities.getUser());
            Assertions.assertEquals(Set.of(TENANT), userTenantAuthorities.getUserTenants());
            Assertions.assertEquals(new LinkedHashSet<>(AuthorityUtils.createAuthorityList(ROLE, ROLE_PERMISSION)),
                    userTenantAuthorities.getUserAuthorities());
        }
    }

    @Test
    void extractUTAFromJwtTokenForExistingUserWithM2MRoleAndDefaultTenantCaughtExceptions() {

        // Token with invalid claims
        when(jwtToken.getClaims()).thenReturn(null);
        Assertions.assertNull(jwtAuthoritiesExtractor.extract(jwtToken));

        // Token with empty claims
        reset(jwtToken);
        Map<String, Object> claims = createSTLASSOClaims(null);
        when(jwtToken.getClaims()).thenReturn(claims);
        Assertions.assertNull(jwtAuthoritiesExtractor.extract(jwtToken));

        //Token with invalid role structure
        reset(jwtToken);
        Map<String, Object> invalidClaims = createSTLASSOClaims(List.of(10));
        when(jwtToken.getClaims()).thenReturn(invalidClaims);
        Assertions.assertThrows(OAuth2AuthenticationException.class, () -> jwtAuthoritiesExtractor.extract(jwtToken));
    }

    @Test
    void extractUTAFromJwtTokenForExistingUserWithM2MRoleAndDefaultTenant() throws Exception {

        Map<String, Object> claims = createSTLASSOClaims(List.of(API_ROLE_CLAIMS));
        when(jwtToken.getClaims()).thenReturn(claims);
        when(jwtToken.getClaimAsString(USERNAME_CLAIM)).thenReturn(USERNAME_CLAIM);

        // Existing user
        when(userRepository.findByUsername(USERNAME_CLAIM)).thenReturn(Optional.of(createUser(DEFAULT_TENANT, API_ROLE)));

        when(roleRepository.findByName(API_ROLE)).thenReturn(Optional.of(createRole(API_ROLE)));
        when(tenantMetaDataRepository.findByTenantIgnoreCase(DEFAULT_TENANT)).thenReturn(createTenantMetaData(DEFAULT_TENANT));
        when(oidcSecurityProperties.getRegistration()).thenReturn(registration);
        OidcSecurityProperties.Registration.Oidc reOidc = createRegistrationOidc();
        when(registration.getOidc()).thenReturn(reOidc);
        when(systemSecurityContext.runAsSystem(any())).thenReturn(createUser(DEFAULT_TENANT, API_ROLE));

        UserTenantAuthorities userTenantAuthorities = jwtAuthoritiesExtractor.extract(jwtToken);

        // Verify synchronize user with COSMOS i.e. authUserSynchronize
        verify(systemSecurityContext).runAsSystem(callableCaptor.capture());
        Callable<JpaUser> callable = callableCaptor.getValue();
        callable.call();
        verify(userRepository).save(createUser(DEFAULT_TENANT, API_ROLE));

        // Verify synchronize user with COSMOS i.e. authUserSynchronize
        verify(oidcUserAudit).insertUserAuthenticationAudit(createUser(DEFAULT_TENANT, API_ROLE),
                createRole(API_ROLE), createTenantMetaData(DEFAULT_TENANT));

        // Verify authoritiesMapper
        verify(authoritiesMapper).mapAuthorities(AuthorityUtils.createAuthorityList(API_ROLE, ROLE_PERMISSION));

        // Verify UserTenantAuthorities
        Assertions.assertEquals(createUser(DEFAULT_TENANT, API_ROLE), userTenantAuthorities.getUser());
        Assertions.assertEquals(Set.of(DEFAULT_TENANT, ALL_TENANTS), userTenantAuthorities.getUserTenants());
    }

    @Test
    void extractJwtTokenFromTokenString() {
        try (MockedStatic<JwtUtil> utilities = Mockito.mockStatic(JwtUtil.class)) {
            NimbusJwtDecoder nimbusJwtDecoder = mock(NimbusJwtDecoder.class);
            OidcSecurityProperties.Provider.Oidc providerOidc = createProviderOidc();
            when(oidcSecurityProperties.getProvider()).thenReturn(provider);
            when(provider.getOidc()).thenReturn(providerOidc);
            utilities.when(() -> JwtUtil.decoder(providerOidc.getJwkSetUri(), providerOidc.getIssuerUri()))
                    .thenReturn(nimbusJwtDecoder);

            when(nimbusJwtDecoder.decode(TOKEN)).thenReturn(jwtToken);
            Assertions.assertEquals(jwtToken, jwtAuthoritiesExtractor.extract(TOKEN));
        }
    }

    @Test
    void extractOidcUserInfoFromFromJwtTokenAndTenants() {
        Map<String, Object> existingClaims = new HashMap<>();
        existingClaims.put("clientID", CLIENT_ID);
        OidcUserInfo oidcUserInfoInput = new OidcUserInfo(existingClaims);

        //Verify empty claims
        when(jwtToken.getClaims()).thenReturn(null);
        Assertions.assertEquals(oidcUserInfoInput, jwtAuthoritiesExtractor
                .extract(jwtToken, oidcUserInfoInput, Set.of(DEFAULT_TENANT, TENANT)));

        //Verify oidcUserInfo with claims
        Map<String, Object> headers = createJwtTokenHeaders();
        Jwt token = new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(5),
                headers, createSTLASSOClaims(List.of(API_ROLE)));
        OidcUserInfo oidcUserInfoOutput = jwtAuthoritiesExtractor
                .extract(token, oidcUserInfoInput, Set.of(DEFAULT_TENANT, TENANT));
        Assertions.assertEquals(USERNAME_CLAIM, oidcUserInfoOutput.getClaims().get("preferred_username"));
        Assertions.assertEquals(FIRSTNAME_CLAIM, oidcUserInfoOutput.getClaims().get("given_name"));
        Assertions.assertEquals(LASTNAME_CLAIM, oidcUserInfoOutput.getClaims().get("family_name"));
        Assertions.assertEquals(EMAIL_CLAIM, oidcUserInfoOutput.getClaims().get(EMAIL_CLAIM));
        Assertions.assertEquals(Set.of(DEFAULT_TENANT, TENANT),
                oidcUserInfoOutput.getClaims().get(USER_AUTHORIZED_TENANTS_CLAIM_KEY));
        Assertions.assertEquals(USERNAME_CLAIM, oidcUserInfoOutput.getClaims().get("sub"));
    }
}
