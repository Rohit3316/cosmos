package org.eclipse.hawkbit.security.oidc.authentication;

import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.InsufficientPermissionException;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * OpenID Connect Authentication Success Handler which load tenant data
 */
public class OidcAuthenticationSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    private final SystemManagement systemManagement;

    private final SystemSecurityContext systemSecurityContext;

    public OidcAuthenticationSuccessHandler(final SystemManagement systemManagement,
                                            final SystemSecurityContext systemSecurityContext) {
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
    }

    /**
     * Implements method of {@link AuthenticationSuccessHandler#onAuthenticationSuccess(HttpServletRequest, HttpServletResponse, Authentication)}
     *
     * @param request        HttpServletRequest
     * @param response       HttpServletResponse
     * @param authentication Authentication
     * @throws ServletException Exception
     * @throws IOException      Exception
     */
    @Override
    public void onAuthenticationSuccess(final HttpServletRequest request, final HttpServletResponse response,
                                        final Authentication authentication) throws ServletException, IOException {
        if (authentication instanceof final AbstractAuthenticationToken token) {
            /* Authenticate with preferred tenant, if not present then choose first tenant from user's authorized tenants */
            final TenantMetaData preferredTenant = systemManagement.getUserPreferredTenant(token.getName());
            if (preferredTenant != null) {
                token.setDetails(new TenantAwareAuthenticationDetails(preferredTenant.getTenant(), false));
            } else {
                final Set<String> userAuthorizedTenants = systemSecurityContext.getUserAuthorizedTenants();
                token.setDetails(new TenantAwareAuthenticationDetails(userAuthorizedTenants.stream().findFirst()
                        .orElseThrow(InsufficientPermissionException::new), false));
            }
        }

        super.onAuthenticationSuccess(request, response, authentication);
    }
}

