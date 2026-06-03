package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class MethodNotAllowedException extends AbstractServerRtException {
    public MethodNotAllowedException() {
        super(ServerError.CLIENT_METHOD_NOT_ALLOWED);
    }

    public MethodNotAllowedException(String message) {
        super(message, ServerError.CLIENT_METHOD_NOT_ALLOWED);
    }

    public MethodNotAllowedException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_METHOD_NOT_ALLOWED, cause);
    }

    public MethodNotAllowedException(Throwable cause) {
        super(ServerError.CLIENT_METHOD_NOT_ALLOWED, cause);
    }
}
