package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class UnAuthorizedException extends AbstractServerRtException {

    public UnAuthorizedException() {
        super(ServerError.CLIENT_UNAUTHORIZED);
    }

    public UnAuthorizedException(String message) {
        super(message, ServerError.CLIENT_UNAUTHORIZED);
    }

    public UnAuthorizedException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_UNAUTHORIZED, cause);
    }

    public UnAuthorizedException(Throwable cause) {
        super(ServerError.CLIENT_UNAUTHORIZED, cause);
    }
}
