package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class NotFoundException extends AbstractServerRtException {

    public NotFoundException() {
        super(ServerError.CLIENT_NOT_FOUND);
    }

    public NotFoundException(String message) {
        super(message, ServerError.CLIENT_NOT_FOUND);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_NOT_FOUND, cause);
    }

    public NotFoundException(Throwable cause) {
        super(ServerError.CLIENT_NOT_FOUND, cause);
    }
}
