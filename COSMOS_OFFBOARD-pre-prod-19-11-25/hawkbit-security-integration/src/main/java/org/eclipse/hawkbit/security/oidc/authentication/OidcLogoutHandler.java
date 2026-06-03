package org.eclipse.hawkbit.security.oidc.authentication;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * LogoutHandler to invalidate OpenID Connect tokens
 */
public class OidcLogoutHandler extends SecurityContextLogoutHandler {

    private final boolean isCustomProvider;

    public OidcLogoutHandler(final boolean isCustomProvider) {
        this.isCustomProvider = isCustomProvider;
    }

    /**
     * Implements method of {@link LogoutHandler#logout(HttpServletRequest, HttpServletResponse, Authentication)}
     *
     * @param request        HttpServletRequest
     * @param response       HttpServletResponse
     * @param authentication Authentication
     */
    @Override
    public void logout(final HttpServletRequest request, final HttpServletResponse response,
                       final Authentication authentication) {
        super.logout(request, response, authentication);

        final Object principal = authentication != null ? authentication.getPrincipal() : null;
        if (principal instanceof final OidcUser user && !isCustomProvider) {
            final String endSessionEndpoint = user.getIssuer() + "/protocol/openid-connect/logout";

            final UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue());

            final RestTemplate restTemplate = new RestTemplate();
            restTemplate.getForEntity(builder.toUriString(), String.class);
        }
    }
}
