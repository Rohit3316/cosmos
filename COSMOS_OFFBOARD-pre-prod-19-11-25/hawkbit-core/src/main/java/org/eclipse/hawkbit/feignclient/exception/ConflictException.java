package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class ConflictException extends AbstractServerRtException {
    public ConflictException() {
        super(ServerError.CLIENT_CONFLICT);
    }

    public ConflictException(String message) {
        super(message, ServerError.CLIENT_CONFLICT);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_CONFLICT, cause);
    }

    public ConflictException(Throwable cause) {
        super(ServerError.CLIENT_CONFLICT, cause);
    }
}
