package org.eclipse.hawkbit.security.oidc.authentication;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.im.authentication.UserAuthenticationFilter;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.eclipse.hawkbit.security.SystemSecurityContext.USER_AUTHORIZED_TENANTS_CLAIM_KEY;

/**
 * Security filter class for filtering OIDC token based authentication requests.
 */
public class OidcBearerTokenAuthenticationFilter implements UserAuthenticationFilter, Filter {

    private static final Logger LOG = LoggerFactory.getLogger(OidcBearerTokenAuthenticationFilter.class);

    private final boolean isCustomProvider;
    private final JwtAuthoritiesExtractor authoritiesExtractor;
    private final JwtAuthoritiesValidator authoritiesValidator;
    private final SystemManagement systemManagement;
    private final OidcUserAuditService userAudit;
    private ClientRegistration clientRegistration;

    public OidcBearerTokenAuthenticationFilter(final boolean isCustomProvider,
                                               final JwtAuthoritiesExtractor authoritiesExtractor,
                                               final JwtAuthoritiesValidator authoritiesValidator,
                                               final OidcUserAuditService userAudit,
                                               final SystemManagement systemManagement) {
        this.isCustomProvider = isCustomProvider;
        this.authoritiesExtractor = authoritiesExtractor;
        this.authoritiesValidator = authoritiesValidator;
        this.userAudit = userAudit;
        this.systemManagement = systemManagement;
    }

    public void setClientRegistration(final ClientRegistration clientRegistration) {
        this.clientRegistration = clientRegistration;
    }

    /**
     * Implements method of {@link UserAuthenticationFilter#doFilter(ServletRequest, ServletResponse, FilterChain)}
     *
     * @param request  the servlet request
     * @param response the servlet response
     * @param chain    the filterchain
     * @throws IOException      Exception
     * @throws ServletException Exception
     */
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof final JwtAuthenticationToken jwtAuthenticationToken) {

            final Jwt jwt = jwtAuthenticationToken.getToken();
            final OidcIdToken idToken = new OidcIdToken(jwt.getTokenValue(), jwt.getIssuedAt(), jwt.getExpiresAt(),
                    jwt.getClaims());
            final OidcUserInfo userInfo;
            final UserTenantAuthorities userTenantAuthorities;
            if (isCustomProvider) {
                authoritiesValidator.accessTokenValidator(jwt);
                userTenantAuthorities = authoritiesExtractor.extract(jwt);
                userInfo = authoritiesExtractor.extract(jwt, new OidcUserInfo(jwt.getClaims()),
                        userTenantAuthorities.getUserTenants());
            } else {
                userTenantAuthorities = authoritiesExtractor.extract(clientRegistration.getClientId(),
                        jwt);
                Map<String, Object> claims = new HashMap<>(jwt.getClaims());
                claims.put(USER_AUTHORIZED_TENANTS_CLAIM_KEY, userTenantAuthorities.getUserTenants());
                userInfo = new OidcUserInfo(claims);
            }

            if (!authoritiesValidator.isValidUserTenantAuthorities(userTenantAuthorities)) {
                LOG.error("Error during validating User Tenant Authorities for user : {} {}",
                        userTenantAuthorities.getUser().getFirstname(), userTenantAuthorities.getUser().getLastname());
                ((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            } else {
                userAudit.updateUserAuthenticationAudit(userTenantAuthorities);
            }

            final String userNameAttributeName = clientRegistration.getProviderDetails().getUserInfoEndpoint()
                    .getUserNameAttributeName();
            final OidcUser user;
            if (!StringUtils.isBlank(userNameAttributeName)) {
                user = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(), idToken, userInfo, userNameAttributeName);
            } else {
                user = new DefaultOidcUser(userTenantAuthorities.getUserAuthorities(), idToken, userInfo);
            }

            final OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(user,
                    userTenantAuthorities.getUserAuthorities(), clientRegistration.getRegistrationId());

            SecurityContextHolder.getContext().setAuthentication(oAuth2AuthenticationToken);

            /* Authenticate with preferred tenant, if not present then choose first tenant from user's authorized tenants */
            final TenantMetaData preferredTenant = systemManagement.getUserPreferredTenant(userTenantAuthorities.getUser().getUsername());
            if (preferredTenant != null) {
                oAuth2AuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(preferredTenant.getTenant(), false));
            } else {
                final String userAuthorizedTenant = userTenantAuthorities.getUserTenants().stream().findFirst()
                        .orElseThrow(InsufficientPermissionException::new);
                oAuth2AuthenticationToken.setDetails(new TenantAwareAuthenticationDetails(userAuthorizedTenant, false));
            }
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(final FilterConfig filterConfig) {
        // Nothing to do
    }

    @Override
    public void destroy() {
        // Nothing to do
    }
}
