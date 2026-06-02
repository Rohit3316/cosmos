package org.eclipse.hawkbit.security.oidc.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * LogoutSuccessHandler that decides where to redirect to after logout, depending on
 * the previously used auth mechanism
 */
public class OidcLogoutSuccessHandler extends SimpleUrlLogoutSuccessHandler {

    private final boolean isCustomProvider;

    public OidcLogoutSuccessHandler(final boolean isCustomProvider) {
        this.isCustomProvider = isCustomProvider;
    }

    /**
     * Implements method of {@link LogoutSuccessHandler#onLogoutSuccess(HttpServletRequest, HttpServletResponse, Authentication)}
     *
     * @param request        HttpServletRequest
     * @param response       HttpServletResponse
     * @param authentication Authentication
     * @throws IOException      Exception
     * @throws ServletException Exception
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        if ((authentication == null || authentication instanceof OAuth2AuthenticationToken) && isCustomProvider) {
            this.setTargetUrlParameter("redirect_url");
        } else if (authentication instanceof OAuth2AuthenticationToken) {
            this.setTargetUrlParameter("/");
        } else {
            this.setTargetUrlParameter("login");
        }
        super.onLogoutSuccess(request, response, authentication);
    }
}