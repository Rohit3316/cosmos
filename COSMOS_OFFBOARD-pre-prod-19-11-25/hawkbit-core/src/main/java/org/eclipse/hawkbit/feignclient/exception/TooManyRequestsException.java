package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class TooManyRequestsException extends AbstractServerRtException {
    public TooManyRequestsException() {
        super(ServerError.CLIENT_TOO_MANY_REQUESTS);
    }

    public TooManyRequestsException(String message) {
        super(message, ServerError.CLIENT_TOO_MANY_REQUESTS);
    }

    public TooManyRequestsException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_TOO_MANY_REQUESTS, cause);
    }

    public TooManyRequestsException(Throwable cause) {
        super(ServerError.CLIENT_TOO_MANY_REQUESTS, cause);
    }
}
