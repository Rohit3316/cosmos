package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.jaas.JaasAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.RedirectStrategy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Oidc Authentication Logout Success Handler")
@Story("Validate Logout Success Handler")
@ExtendWith(MockitoExtension.class)
class OidcLogoutSuccessHandlerTest extends SecurityTestDataFactory {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private RedirectStrategy redirectStrategy;

    private OidcLogoutSuccessHandler oidcLogoutSuccessHandler;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void onLogoutSuccessWhenOAuth2AuthenticationTokenWithCustomProviderThenCustomRedirectURL() throws ServletException, IOException {
        oidcLogoutSuccessHandler = new OidcLogoutSuccessHandler(true);
        oidcLogoutSuccessHandler.setRedirectStrategy(redirectStrategy);
        final OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(createDefaultOidcUser(),
                Set.of(new SimpleGrantedAuthority(ROLE_USER)), REGISTRATION);
        when(request.getParameter("redirect_url")).thenReturn(URL);
        oidcLogoutSuccessHandler.onLogoutSuccess(request, response, oAuth2AuthenticationToken);
        verify(redirectStrategy).sendRedirect(request, response, URL);
    }

    @Test
    void onLogoutSuccessWhenOAuth2AuthenticationTokenWithoutCustomProviderThenBaseURL() throws ServletException, IOException {
        oidcLogoutSuccessHandler = new OidcLogoutSuccessHandler(false);
        oidcLogoutSuccessHandler.setRedirectStrategy(redirectStrategy);
        final OAuth2AuthenticationToken oAuth2AuthenticationToken = new OAuth2AuthenticationToken(createDefaultOidcUser(),
                Set.of(new SimpleGrantedAuthority(ROLE_USER)), REGISTRATION);
        when(request.getParameter("/")).thenReturn("/");
        oidcLogoutSuccessHandler.onLogoutSuccess(request, response, oAuth2AuthenticationToken);
        verify(redirectStrategy).sendRedirect(request, response, "/");
    }

    @Test
    void onLogoutSuccessWhenNotOAuth2AuthenticationTokenThenLoginURL() throws ServletException, IOException {
        oidcLogoutSuccessHandler = new OidcLogoutSuccessHandler(false);
        oidcLogoutSuccessHandler.setRedirectStrategy(redirectStrategy);
        when(request.getParameter("login")).thenReturn("/login");
        oidcLogoutSuccessHandler.onLogoutSuccess(request, response, new JaasAuthenticationToken(null, null, null));
        verify(redirectStrategy).sendRedirect(request, response, "/login");
    }
}