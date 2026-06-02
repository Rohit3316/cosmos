package org.eclipse.hawkbit.security.oidc.authentication;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.eclipse.hawkbit.security.SystemSecurityContext.USER_AUTHORIZED_TENANTS_CLAIM_KEY;

/**
 * Extended {@link OidcUserService} supporting JWT containing authorities
 */
public class JwtAuthoritiesOidcUserService extends OidcUserService {

    private static final Logger LOG = LoggerFactory.getLogger(JwtAuthoritiesOidcUserService.class);

    private final JwtAuthoritiesExtractor authoritiesExtractor;
    private final JwtAuthoritiesValidator authoritiesValidator;

    private final OidcUserAuditService oidcUserAudit;

    private final boolean isCustomProvider;

    public JwtAuthoritiesOidcUserService(final JwtAuthoritiesExtractor authoritiesExtractor, final boolean isCustomProvider,
                                         final JwtAuthoritiesValidator authoritiesValidator, final OidcUserAuditService oidcUserAudit) {
        super();

        this.authoritiesExtractor = authoritiesExtractor;

        this.isCustomProvider = isCustomProvider;
        this.authoritiesValidator = authoritiesValidator;
        this.oidcUserAudit = oidcUserAudit;
    }

    /**
     * This method is used to load authenticated user from OIDC user request.
     *
     * @param userRequest oidc user request
     * @return OidcUser
     */
    @Override
    public OidcUser loadUser(final OidcUserRequest userRequest) {
        final OidcUser user = super.loadUser(userRequest);
        final ClientRegistration clientRegistration = userRequest.getClientRegistration();
        return isCustomProvider ? customUserCreate(userRequest, user, clientRegistration)
                : userCreate(userRequest, user, clientRegistration);
    }

    /**
     * This method is used to create user from standard OIDC provider
     *
     * @param userRequest        oidc user request
     * @param user               oidc user
     * @param clientRegistration registered client
     * @return OidcUser
     */
    private OidcUser userCreate(OidcUserRequest userRequest, OidcUser user, ClientRegistration clientRegistration) {
        final UserTenantAuthorities userTenantAuthorities = authoritiesExtractor.extract(clientRegistration,
                userRequest.getAccessToken().getTokenValue());
        if (!authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)) {
            LOG.error("Error during validating User Tenant Authorities for user : {} {}",
                    userTenantAuthorities.getUser().getFirstname(), userTenantAuthorities.getUser().getLastname());
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST),
                    "UserTenantAuthorities validation failed for user " + userTenantAuthorities.getUser().getFirstname() + " "
                            + userTenantAuthorities.getUser().getLastname());
        } else {
            oidcUserAudit.updateUserAuthenticationAudit(userTenantAuthorities);
        }

        Map<String, Object> userInfoClaims = new HashMap<>(user.getUserInfo().getClaims());
        userInfoClaims.put(USER_AUTHORIZED_TENANTS_CLAIM_KEY, userTenantAuthorities.getUserTenants());
        OidcUserInfo userInfo = new OidcUserInfo(userInfoClaims);
        final String userNameAttributeName = clientRegistration.getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        OidcUser oidcUser;
        if (!StringUtils.isBlank(userNameAttributeName)) {
            oidcUser = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(),
                    userRequest.getIdToken(), userInfo, userNameAttributeName);
        } else {
            oidcUser = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(),
                    userRequest.getIdToken(), userInfo);
        }
        return oidcUser;
    }

    /**
     * This method is used to create user from custom (Stellantis IDP) OIDC provider
     *
     * @param userRequest        oidc user request
     * @param user               oidc user
     * @param clientRegistration registered client
     * @return OidcUser
     */
    private OidcUser customUserCreate(OidcUserRequest userRequest, OidcUser user, ClientRegistration clientRegistration) {

        Jwt token = authoritiesExtractor.extract(userRequest.getAccessToken().getTokenValue());

        authoritiesValidator.accessTokenValidator(token);
        final UserTenantAuthorities userTenantAuthorities = authoritiesExtractor.extract(token);
        if (!authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)) {
            LOG.error("Error during validating User Tenant Authorities for user : {} {}",
                    userTenantAuthorities.getUser().getFirstname(), userTenantAuthorities.getUser().getLastname());
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST),
                    "UserTenantAuthorities validation failed for user " + userTenantAuthorities.getUser().getFirstname() + " "
                            + userTenantAuthorities.getUser().getLastname());
        } else {
            oidcUserAudit.updateUserAuthenticationAudit(userTenantAuthorities);
        }

        final String userNameAttributeName = clientRegistration.getProviderDetails().getUserInfoEndpoint()
                .getUserNameAttributeName();
        OidcUser oidcUser;
        if (!StringUtils.isBlank(userNameAttributeName)) {
            oidcUser = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(), userRequest.getIdToken(),
                    authoritiesExtractor.extract(token, user.getUserInfo(), userTenantAuthorities.getUserTenants()),
                    userNameAttributeName);
        } else {
            oidcUser = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(), userRequest.getIdToken(),
                    authoritiesExtractor.extract(token, user.getUserInfo(), userTenantAuthorities.getUserTenants()));
        }
        return oidcUser;
    }
}
