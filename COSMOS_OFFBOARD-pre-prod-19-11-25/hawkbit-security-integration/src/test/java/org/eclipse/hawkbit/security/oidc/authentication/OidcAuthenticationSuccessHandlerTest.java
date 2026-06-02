package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import java.util.Set;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security Oidc Authentication Success Handler")
@Story("Validate Success Handler")
@ExtendWith(MockitoExtension.class)
class OidcAuthenticationSuccessHandlerTest extends SecurityTestDataFactory {

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private SystemSecurityContext systemSecurityContext;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private OidcAuthenticationSuccessHandler oidcAuthenticationSuccessHandler;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        oidcAuthenticationSuccessHandler = new OidcAuthenticationSuccessHandler(systemManagement, systemSecurityContext);
    }

    @Test
    void onAuthenticationSuccessWhenAuthInstanceOfAbstractAuthenticationTokenThenWithPreferredTenant() throws ServletException, IOException {
        Jwt jwtToken = createJwt();
        TenantMetaData tenantMetaData = createTenantMetaData(TENANT);
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwtToken);
        when(systemManagement.getUserPreferredTenant(token.getName())).thenReturn(tenantMetaData);
        oidcAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, token);
        Assertions.assertEquals(TENANT, ((TenantAwareAuthenticationDetails) token.getDetails()).getTenant());
    }

    @Test
    void onAuthenticationSuccessWhenAuthInstanceOfAbstractAuthenticationTokenThenWithoutPreferredTenant() throws ServletException, IOException {
        Jwt jwtToken = createJwt();
        JwtAuthenticationToken token = new JwtAuthenticationToken(jwtToken);
        when(systemManagement.getUserPreferredTenant(token.getName())).thenReturn(null);
        when(systemSecurityContext.getUserAuthorizedTenants()).thenReturn(Set.of(DEFAULT_TENANT));
        oidcAuthenticationSuccessHandler.onAuthenticationSuccess(request, response, token);
        Assertions.assertEquals(DEFAULT_TENANT, ((TenantAwareAuthenticationDetails) token.getDetails()).getTenant());
    }
}

