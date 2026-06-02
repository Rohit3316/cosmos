package org.eclipse.hawkbit.security.oidc.authentication;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.repository.jpa.RoleRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.UserRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.UserElement;
import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.OidcSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.exception.OAuth2AccessDeniedException;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantRole;
import org.eclipse.hawkbit.security.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.util.CollectionUtils;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.ALL_TENANTS;
import static org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions.API_ROLE;
import static org.eclipse.hawkbit.security.SystemSecurityContext.USER_AUTHORIZED_TENANTS_CLAIM_KEY;

/**
 * Utility class to extract authorities out of the jwt. It interprets the user's
 * role as their authorities.
 */
public class JwtAuthoritiesExtractor {

    private static final OAuth2Error INVALID_REQUEST = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST);
    private static final OAuth2Error ACCESS_DENIED = new OAuth2Error(OAuth2ErrorCodes.ACCESS_DENIED);
    private static final String DEFAULT_TENANT = "DEFAULT";
    private static final String USERNAME_CLAIM = "username";
    private static final String FIRSTNAME_CLAIM = "firstname";
    private static final String LASTNAME_CLAIM = "lastname";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "roles";
    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthoritiesExtractor.class);
    private final GrantedAuthoritiesMapper authoritiesMapper;
    private final RoleRepository roleRepository;
    private final TenantMetaDataRepository tenantMetaDataRepository;
    private final UserRepository userRepository;
    private final OidcUserAuditService oidcUserAudit;
    private final SystemSecurityContext systemSecurityContext;
    private final OidcSecurityProperties oidcSecurityProperties;

    public JwtAuthoritiesExtractor(final GrantedAuthoritiesMapper authoritiesMapper, final RoleRepository roleRepository,
                                   final TenantMetaDataRepository tenantMetaDataRepository, final UserRepository userRepository,
                                   final OidcUserAuditService oidcUserAudit, final SystemSecurityContext systemSecurityContext,
                                   final OidcSecurityProperties oidcSecurityProperties) {
        super();
        this.authoritiesMapper = authoritiesMapper;
        this.roleRepository = roleRepository;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
        this.userRepository = userRepository;
        this.oidcUserAudit = oidcUserAudit;
        this.systemSecurityContext = systemSecurityContext;
        this.oidcSecurityProperties = oidcSecurityProperties;
    }

    /**
     * This method is used to extract user's roles/authorities from JWT token of standard OIDC provides.
     *
     * @param clientRegistration registered client
     * @param tokenValue         String
     * @return set of GrantedAuthority
     */

    UserTenantAuthorities extract(final ClientRegistration clientRegistration, final String tokenValue) {
        try {
            // Token is already verified by spring security
            final Jwt token = JwtUtil.decoder(oidcSecurityProperties.getProvider().getOidc().getJwkSetUri(),
                    oidcSecurityProperties.getProvider().getOidc().getIssuerUri()).decode(tokenValue);

            return extract(clientRegistration.getClientId(), token);
        } catch (final OAuth2AuthenticationException | OAuth2AccessDeniedException o) {
            throw o;
        } catch (final Exception e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    /**
     * This method is used to extract claims from standard OIDC token and create set of GrantedAuthority
     *
     * @param clientId String
     * @param token    Jwt token
     * @return set of GrantedAuthority
     */
    @SuppressWarnings("unchecked")
    UserTenantAuthorities extract(final String clientId, final Jwt token) {
        try {
            final Map<String, Object> resourceMap = (Map<String, Object>) token.getClaims().get("resource_access");

            final Map<String, Map<String, Object>> clientResource = (Map<String, Map<String, Object>>) resourceMap
                    .get(clientId);
            if (CollectionUtils.isEmpty(clientResource)) {
                return null;
            }

            final List<String> roles = (List<String>) clientResource.get(ROLE_CLAIM);
            if (!CollectionUtils.isEmpty(roles)) {
                return extractUserTenantAuthorities(roles, token);
            }

            return null;
        } catch (final OAuth2AccessDeniedException o) {
            throw o;
        } catch (final Exception e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    /**
     * This method is used to extract JWT token object from token value of custom (Stellantis) OIDC provides.
     *
     * @param tokenValue String token value
     * @return JWT token
     */

    Jwt extract(final String tokenValue) {
        try {
            // Token is already verified by spring security
            return JwtUtil.decoder(oidcSecurityProperties.getProvider().getOidc().getJwkSetUri(),
                    oidcSecurityProperties.getProvider().getOidc().getIssuerUri()).decode(tokenValue);
        } catch (final JwtException e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    /**
     * This method is used to extract user from JWT token of custom (Stellantis) OIDC provides.
     *
     * @param token    JWT token
     * @param userInfo OidcUserInfo userinfo
     * @return OidcUserInfo user
     */
    OidcUserInfo extract(Jwt token, OidcUserInfo userInfo, final Set<String> tenants) {
        try {
            if (token != null && token.getClaims() != null) {
                final Map<String, Object> claims = new HashMap<>(userInfo.getClaims());
                claims.put("preferred_username", token.getClaimAsString(USERNAME_CLAIM));
                claims.put("given_name", token.getClaimAsString(FIRSTNAME_CLAIM));
                claims.put("family_name", token.getClaimAsString(LASTNAME_CLAIM));
                claims.put(EMAIL_CLAIM, token.getClaimAsString(EMAIL_CLAIM));
                claims.put(USER_AUTHORIZED_TENANTS_CLAIM_KEY, tenants);
                if (!claims.containsKey("sub")) {
                    claims.put("sub", token.getClaimAsString(USERNAME_CLAIM));
                }
                return new OidcUserInfo(claims);
            }
            return userInfo;
        } catch (final Exception e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    /**
     * This method is used to extract user's roles/authorities from JWT token of custom (Stellantis) OIDC provides.
     * And validate the role/environment/tenant extracted from JWT token.
     *
     * @param token Jwt token
     * @return set of GrantedAuthority
     */
    @SuppressWarnings("unchecked")
    UserTenantAuthorities extract(final Jwt token) {
        try {
            List<String> roles = token.getClaims() != null && token.getClaims().get(ROLE_CLAIM) != null
                    ? getRolesFromClaims(token.getClaims().get(ROLE_CLAIM)) : Collections.emptyList();

            if (!CollectionUtils.isEmpty(roles)) {
                return extractUserTenantAuthorities(roles, token);
            }
            LOG.debug("Extracted empty list of roles from JWT token claims");
            return null;

        } catch (final OAuth2AccessDeniedException o) {
            throw o;
        } catch (final Exception e) {
            throw new OAuth2AuthenticationException(INVALID_REQUEST, e);
        }
    }

    private UserTenantAuthorities extractUserTenantAuthorities(final List<String> roles, final Jwt token) {
        List<GrantedAuthority> authorities = new ArrayList<>();
        Set<String> tenants = new HashSet<>();
        boolean roleFound = false;
        String username = token.getClaimAsString(USERNAME_CLAIM) != null ? token.getClaimAsString(USERNAME_CLAIM) : "";
        JpaUser jpaUser = userRepository.findByUsername(username).orElse(createUser(username, token));
        for (String role : roles) {

            //AUG.ENV_ROLE_TENANT -> (User) or AUG.API -> (M2M)
            UserTenantRole userTenantRole = extractUserTenantRole(role);

            Role cosmosRole = roleRepository.findByName(userTenantRole.getRole()).orElse(null);
            JpaTenantMetaData tenantMetaData = tenantMetaDataRepository.findByTenantIgnoreCase(userTenantRole.getTenant());

            if (cosmosRole != null && tenantMetaData != null && oidcSecurityProperties.getRegistration().getOidc()
                    .getEnv().equalsIgnoreCase(userTenantRole.getEnv())) {

                // synchronize user with COSMOS
                jpaUser = authUserSynchronize(jpaUser, cosmosRole, tenantMetaData);

                // insert to user audit if not recorded
                oidcUserAudit.insertUserAuthenticationAudit(jpaUser, (JpaRole) cosmosRole,
                        tenantMetaData);

                tenants.add(tenantMetaData.getTenant());

                if (API_ROLE.equals(cosmosRole.getName())) {
                    tenants.add(ALL_TENANTS);
                }

                authorities.add(new SimpleGrantedAuthority(userTenantRole.getRole()));
                List<String> roleAuthorities = cosmosRole.getPermissions().stream().map(Permission::getName)
                        .toList();
                if (!CollectionUtils.isEmpty(roleAuthorities)) {
                    authorities.addAll(AuthorityUtils.createAuthorityList(roleAuthorities.toArray(new String[0])));
                }
                roleFound = true;
            }
        }

        if (!roleFound) {
            LOG.error("User {} {} with roles {} was not found in COSMOS", jpaUser.getFirstname(), jpaUser.getLastname(),
                    roles);
            throw new OAuth2AccessDeniedException(ACCESS_DENIED + ": Environment/Role/Tenant is invalid");
        }

        if (authoritiesMapper != null) {
            return UserTenantAuthorities.builder()
                    .user(jpaUser)
                    .userTenants(tenants)
                    .userAuthorities(new LinkedHashSet<>(authoritiesMapper.mapAuthorities(authorities)))
                    .build();
        }
        return UserTenantAuthorities.builder()
                .user(jpaUser)
                .userTenants(tenants)
                .userAuthorities(new LinkedHashSet<>(authorities))
                .build();
    }

    /**
     * Extreact role/environment/Tenant from 'role' claim of a WT token
     *
     * @param role String
     * @return UserTenantRole
     */
    private UserTenantRole extractUserTenantRole(String role) {
        String[] roles = role.split("_");
        if (roles.length > 1) {
            return UserTenantRole.builder()
                    .env(getEnvRole(roles[0]))
                    .role(roles[1])
                    .tenant(roles[2])
                    .build();
        } else if (roles.length == 1) {
            return UserTenantRole.builder()
                    .env(oidcSecurityProperties.getRegistration().getOidc()
                            .getEnv())
                    .role(getEnvRole(roles[0]))
                    .tenant(DEFAULT_TENANT)
                    .build();
        } else {
            LOG.error("Claim with invalid role structure: {}", role);
            throw new OAuth2AuthenticationException(INVALID_REQUEST, "Claim with invalid role structure: " + role);
        }
    }

    /**
     * Get the user environment from env value of a role claim.
     *
     * @param env String (AUG.ENV)
     * @return String ENV
     */
    private String getEnvRole(String env) {
        return StringUtils.substringAfter(env, ".");
    }

    /**
     * Get roles from roles claim of custom (Stellantis) OIDC provider's token
     *
     * @param roles String/JSONArray
     * @return list of String
     */
    private List<String> getRolesFromClaims(Object roles) {
        if (roles instanceof final String role) {
            return List.of(role);
        } else if (roles instanceof final JSONArray rolesArray) {
            return rolesArray.stream().map(Object::toString).toList();
        } else if (roles instanceof List && ((List<?>) roles).get(0) instanceof String) {
            return ((List<?>) roles).stream().map(Object::toString).toList();
        } else {
            LOG.error("Claim with invalid structure, (roles): {}", roles);
            throw new OAuth2AuthenticationException(INVALID_REQUEST, "Claim with invalid structure, (roles)");
        }
    }

    /**
     * Synchronize user, user-role and user-tenant with COSMOS.
     * Insert if user doesn't exist in COSMOS/Update user-role and user-tenant if user exist.
     *
     * @param user           JpaUser
     * @param cosmosRole     Role role
     * @param tenantMetaData TenantMetaData tenant
     */
    private JpaUser authUserSynchronize(final JpaUser user, Role cosmosRole, TenantMetaData tenantMetaData) {
        return systemSecurityContext.runAsSystem(() -> {
            if (user.getTenantMetadata().isEmpty() || !user.getTenantMetadata().contains(tenantMetaData)) {
                List<UserElement> userElements = user.getUserElements() != null
                        ? user.getUserElements() : new ArrayList<>();
                userElements.add(new UserElement(user, tenantMetaData));
                user.setUserElements(userElements);
            }
            if (user.getRoles() == null || !user.getRoles().contains(cosmosRole)) {
                Set<Role> roles = user.getRoles() != null ? user.getRoles() : new HashSet<>();
                roles.add(cosmosRole);
                user.setRoles(roles);
            }
            return userRepository.save(user);
        });
    }

    /**
     * Create user from JWT token.
     *
     * @param username String
     * @param token    Jwt token
     * @return JpaUser user
     */
    private JpaUser createUser(final String username, final Jwt token) {
        String firstname = token.getClaimAsString(FIRSTNAME_CLAIM) != null ? token.getClaimAsString(FIRSTNAME_CLAIM) : "";
        String lastname = token.getClaimAsString(LASTNAME_CLAIM) != null ? token.getClaimAsString(LASTNAME_CLAIM) : "";
        String email = token.getClaimAsString(EMAIL_CLAIM) != null ? token.getClaimAsString(EMAIL_CLAIM) : "";
        return new JpaUser(username, email, firstname, lastname);
    }
}
