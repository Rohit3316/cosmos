package org.eclipse.hawkbit.security.oidc.authentication;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.hawkbit.security.oidc.authentication.exception.ExceptionInfo;
import org.eclipse.hawkbit.security.oidc.authentication.exception.OAuth2AccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Exception handler class for rest authentication exceptions {@link AuthenticationEntryPoint}
 */
public class OidcRestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Implements method of {@link AuthenticationEntryPoint#commence(HttpServletRequest, HttpServletResponse, AuthenticationException)}
     *
     * @param request       HttpServletRequest
     * @param response      HttpServletResponse
     * @param authException AuthenticationException
     * @throws IOException      Exception
     * @throws ServletException Exception
     */
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        response.setStatus(authException instanceof OAuth2AccessDeniedException
                ? HttpStatus.FORBIDDEN.value()
                : HttpStatus.UNAUTHORIZED.value());

        response.getOutputStream().println(mapper.writeValueAsString(ExceptionInfo.builder()
                .name(authException.getClass().getSimpleName())
                .message(authException.getMessage())
                .build()));
    }
}