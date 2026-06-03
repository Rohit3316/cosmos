package org.eclipse.hawkbit.security.oidc.authentication;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.hawkbit.repository.jpa.model.JpaPermission;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.UserElement;
import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;

public class SecurityTestDataFactory {

    static final String ROLE = "ROLE";
    static final String API_ROLE = "API";
    static final String ROLE_USER = "ROLE_USER";
    static final String TENANT = "TENANT";
    static final String DEFAULT_TENANT = "DEFAULT";

    static final String ENV = "ENV";

    static final String ROLE_PERMISSION = "permission";

    static final String ROLE_CLAIMS = "AUG.ENV_ROLE_TENANT";

    static final String API_ROLE_CLAIMS = "AUG.API";

    static final String USERNAME_CLAIM = "username";

    static final String TOKEN = "TEST_TOKEN";

    static final String FIRSTNAME_CLAIM = "firstname";
    static final String LASTNAME_CLAIM = "lastname";
    static final String EMAIL_CLAIM = "email";

    static final String REGISTRATION = "testRegistration";
    static final String CLIENT_ID = "testClientID";
    static final String URL = "http://testURL";
    static final String USERNAME_ATTRIBUTE = "username";
    static final String ISSUER_CLAIM = "iss";
    static final String CLIENT_ID_CLAIM = "client_id";
    static final String SCOPE = "openid,prd:aug,profile";
    static final String SCOPE_AUG_CLAIM = "aud";
    static final String SCOPE_AUG = "aug";


    ClientRegistration createClientRegistration() {
        return ClientRegistration.withRegistrationId(REGISTRATION)
                .clientId(CLIENT_ID)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .issuerUri(URL)
                .redirectUri(URL)
                .authorizationUri(URL)
                .jwkSetUri(URL)
                .tokenUri(URL)
                .userInfoUri(URL)
                .userNameAttributeName(USERNAME_ATTRIBUTE)
                .build();
    }

    OidcSecurityProperties.Provider.Oidc createProviderOidc() {
        OidcSecurityProperties.Provider.Oidc oidc = new OidcSecurityProperties.Provider.Oidc();
        oidc.setIssuerUri(URL);
        oidc.setJwkSetUri(URL);
        return oidc;
    }

    OidcSecurityProperties.Registration.Oidc createRegistrationOidc() {
        OidcSecurityProperties.Registration.Oidc oidc = new OidcSecurityProperties.Registration.Oidc();
        oidc.setEnv(ENV);
        oidc.setClientId(CLIENT_ID);
        oidc.setScope(SCOPE);
        return oidc;
    }

    Map<String, Object> createStandardSSOClaims(List<String> roles) {
        Map<String, Object> roleClaims = new HashMap<>();
        roleClaims.put("roles", roles);
        Map<String, Object> clientIdClaim = new HashMap<>();
        clientIdClaim.put(CLIENT_ID, roleClaims);
        Map<String, Object> resources = new HashMap<>();
        resources.put("resource_access", clientIdClaim);
        return resources;
    }

    <T> Map<String, Object> createSTLASSOClaims(List<T> roles) {
        Map<String, Object> roleClaims = new HashMap<>();
        roleClaims.put("roles", roles);
        roleClaims.put(USERNAME_CLAIM, USERNAME_CLAIM);
        roleClaims.put(FIRSTNAME_CLAIM, FIRSTNAME_CLAIM);
        roleClaims.put(LASTNAME_CLAIM, LASTNAME_CLAIM);
        roleClaims.put(EMAIL_CLAIM, EMAIL_CLAIM);
        roleClaims.put(ISSUER_CLAIM, URL);
        roleClaims.put(CLIENT_ID_CLAIM, CLIENT_ID);
        roleClaims.put(SCOPE_AUG_CLAIM, new ArrayList<>(Collections.singleton(SCOPE_AUG.toUpperCase())));
        return roleClaims;
    }

    Map<String, Object> createJwtTokenHeaders() {
        Map<String, Object> headers = new HashMap<>();
        headers.put("test", "test");
        return headers;
    }

    JpaTenantMetaData createTenantMetaData(String tenant) {
        JpaTenantMetaData tenantMetaData = new JpaTenantMetaData();
        tenantMetaData.setTenant(tenant);
        return tenantMetaData;
    }

    JpaRole createRole(String role) {
        return new JpaRole(role, role, Set.of(new JpaPermission(ROLE_PERMISSION, ROLE_PERMISSION)));
    }

    JpaUser createUser(String tenant, String role) {
        JpaUser user = new JpaUser();
        user.setUsername(USERNAME_CLAIM);
        user.setFirstname(FIRSTNAME_CLAIM);
        user.setLastname(LASTNAME_CLAIM);
        user.setEmail(EMAIL_CLAIM);
        user.setUserElements(List.of(new UserElement(user, createTenantMetaData(tenant))));
        user.setRoles(Set.of(createRole(role)));
        return user;
    }

    UserTenantAuthorities createUserTenantAuthorities() {
        return UserTenantAuthorities.builder()
                .user(new JpaUser(USERNAME_CLAIM, EMAIL_CLAIM, FIRSTNAME_CLAIM, LASTNAME_CLAIM))
                .userAuthorities(Set.of(new SimpleGrantedAuthority(ROLE_USER)))
                .userTenants(Set.of(DEFAULT_TENANT)).build();
    }

    Jwt createJwt() {
        return new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(5),
                createJwtTokenHeaders(), createSTLASSOClaims(List.of(API_ROLE)));
    }

    Jwt createJwt(Map<String, Object> claims) {
        return new Jwt(TOKEN, Instant.now(), Instant.now().plusSeconds(5),
                createJwtTokenHeaders(), claims);
    }

    OAuth2AccessToken createOAuth2AccessToken() {
        return new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, TOKEN, Instant.now(),
                Instant.now().plusSeconds(5));
    }

    OidcIdToken createOidcIdToken() {
        Map<String, Object> claims = new HashMap<>();
        claims.put(USERNAME_ATTRIBUTE, USERNAME_ATTRIBUTE);
        claims.put("sub", USERNAME_ATTRIBUTE);
        return new OidcIdToken(TOKEN, Instant.now(), Instant.now().plusSeconds(5), claims);
    }

    OAuth2User createDefaultOidcUser() {
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put(USERNAME_ATTRIBUTE, USERNAME_ATTRIBUTE);
        userAttributes.put("sub", USERNAME_ATTRIBUTE);
        return new DefaultOAuth2User(new LinkedHashSet<>(), userAttributes, USERNAME_ATTRIBUTE);
    }

    OidcUserInfo createOidcUserInfo() {
        return new OidcUserInfo(createSTLASSOClaims(List.of(API_ROLE)));
    }
}
