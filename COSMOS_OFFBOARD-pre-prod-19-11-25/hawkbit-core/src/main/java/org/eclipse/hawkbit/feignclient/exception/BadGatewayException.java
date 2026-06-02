package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class BadGatewayException extends AbstractServerRtException {
    public BadGatewayException() {
        super(ServerError.SERVER_BAD_GATEWAY);
    }

    public BadGatewayException(String message) {
        super(message, ServerError.SERVER_BAD_GATEWAY);
    }

    public BadGatewayException(String message, Throwable cause) {
        super(message, ServerError.SERVER_BAD_GATEWAY, cause);
    }

    public BadGatewayException(Throwable cause) {
        super(ServerError.SERVER_BAD_GATEWAY, cause);
    }
}
