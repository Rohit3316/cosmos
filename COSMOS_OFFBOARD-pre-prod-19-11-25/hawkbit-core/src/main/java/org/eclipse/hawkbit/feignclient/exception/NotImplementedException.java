package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class NotImplementedException extends AbstractServerRtException {
    public NotImplementedException() {
        super(ServerError.SERVER_NOT_IMPLEMENTED);
    }

    public NotImplementedException(String message) {
        super(message, ServerError.SERVER_NOT_IMPLEMENTED);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, ServerError.SERVER_NOT_IMPLEMENTED, cause);
    }

    public NotImplementedException(Throwable cause) {
        super(ServerError.SERVER_NOT_IMPLEMENTED, cause);
    }
}
