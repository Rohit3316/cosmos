package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class ServiceUnavailableException extends AbstractServerRtException {
    public ServiceUnavailableException() {
        super(ServerError.SERVER_SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String message) {
        super(message, ServerError.SERVER_SERVICE_UNAVAILABLE);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message, ServerError.SERVER_SERVICE_UNAVAILABLE, cause);
    }

    public ServiceUnavailableException(Throwable cause) {
        super(ServerError.SERVER_SERVICE_UNAVAILABLE, cause);
    }
}
