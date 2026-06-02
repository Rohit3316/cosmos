package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class BadRequestException extends AbstractServerRtException {
    public BadRequestException() {
        super(ServerError.CLIENT_BAD_REQUEST);
    }

    public BadRequestException(String message) {
        super(message, ServerError.CLIENT_BAD_REQUEST);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_BAD_REQUEST, cause);
    }

    public BadRequestException(Throwable cause) {
        super(ServerError.CLIENT_BAD_REQUEST, cause);
    }
}
