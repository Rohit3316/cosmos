package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class ForbiddenException extends AbstractServerRtException {
    public ForbiddenException() {
        super(ServerError.CLIENT_FORBIDDEN);
    }

    public ForbiddenException(String message) {
        super(message, ServerError.CLIENT_FORBIDDEN);
    }

    public ForbiddenException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_FORBIDDEN, cause);
    }

    public ForbiddenException(Throwable cause) {
        super(ServerError.CLIENT_FORBIDDEN, cause);
    }
}
