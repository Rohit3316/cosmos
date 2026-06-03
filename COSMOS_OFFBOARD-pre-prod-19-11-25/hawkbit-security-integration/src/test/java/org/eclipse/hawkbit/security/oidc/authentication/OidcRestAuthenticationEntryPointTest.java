package org.eclipse.hawkbit.security.oidc.authentication;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import java.io.IOException;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.hawkbit.security.oidc.authentication.exception.OAuth2AccessDeniedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Feature("Unit Tests - Security OIDC Rest Authentication Entry Point")
@Story("Validate OIDC Authentication Entry Point")
@ExtendWith(MockitoExtension.class)
class OidcRestAuthenticationEntryPointTest extends SecurityTestDataFactory {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private ServletOutputStream outputStream;

    @Captor
    private ArgumentCaptor<String> stringArgumentCaptor;

    private OidcRestAuthenticationEntryPoint entryPoint;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
        entryPoint = new OidcRestAuthenticationEntryPoint();
    }

    @Test
    void commenceWhenOAuth2AccessDeniedExceptionThenForbidden() throws ServletException, IOException {
        OAuth2AccessDeniedException oAuth2AccessDeniedException =
                new OAuth2AccessDeniedException("OAuth2AccessDeniedException");
        when(response.getOutputStream()).thenReturn(outputStream);

        entryPoint.commence(request, response, oAuth2AccessDeniedException);
        verify(response).setStatus(HttpStatus.FORBIDDEN.value());
        verify(outputStream).println(stringArgumentCaptor.capture());
        String exceptionInfo = stringArgumentCaptor.getValue();
        Assertions.assertEquals("{\"name\":\"OAuth2AccessDeniedException\",\"message\":\"OAuth2AccessDeniedException\"}", exceptionInfo);
    }

    @Test
    void commenceWhenOAuth2AuthenticationExceptionThenForbidden() throws ServletException, IOException {
        OAuth2AuthenticationException oAuth2AuthenticationException =
                new OAuth2AuthenticationException(new OAuth2Error("OAuth2AuthenticationException"),
                        "OAuth2AuthenticationException");
        when(response.getOutputStream()).thenReturn(outputStream);

        entryPoint.commence(request, response, oAuth2AuthenticationException);
        verify(response).setStatus(HttpStatus.UNAUTHORIZED.value());
        verify(outputStream).println(stringArgumentCaptor.capture());
        String exceptionInfo = stringArgumentCaptor.getValue();
        Assertions.assertEquals("{\"name\":\"OAuth2AuthenticationException\"," +
                "\"message\":\"OAuth2AuthenticationException\"}", exceptionInfo);
    }
}