package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class InternalServerErrorException extends AbstractServerRtException {
    public InternalServerErrorException() {
        super(ServerError.SERVER_INTERNAL_SERVER_ERROR);
    }

    public InternalServerErrorException(String message) {
        super(message, ServerError.SERVER_INTERNAL_SERVER_ERROR);
    }

    public InternalServerErrorException(String message, Throwable cause) {
        super(message, ServerError.SERVER_INTERNAL_SERVER_ERROR, cause);
    }

    public InternalServerErrorException(Throwable cause) {
        super(ServerError.SERVER_INTERNAL_SERVER_ERROR, cause);
    }
}
