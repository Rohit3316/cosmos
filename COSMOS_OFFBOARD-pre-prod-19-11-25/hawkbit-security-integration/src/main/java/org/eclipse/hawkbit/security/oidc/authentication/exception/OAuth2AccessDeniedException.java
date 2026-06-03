package org.eclipse.hawkbit.security.oidc.authentication.exception;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception class to throw OAuth2 access denied exception {@link AuthenticationException}
 */
public class OAuth2AccessDeniedException extends AuthenticationException {
    public OAuth2AccessDeniedException(String msg, Throwable cause) {
        super(msg, cause);
    }

    public OAuth2AccessDeniedException(String msg) {
        super(msg);
    }
}
