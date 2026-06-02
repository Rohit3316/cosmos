package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class GatewayTimeoutException extends AbstractServerRtException {
    public GatewayTimeoutException() {
        super(ServerError.SERVER_GATEWAY_TIMEOUT);
    }

    public GatewayTimeoutException(String message) {
        super(message, ServerError.SERVER_GATEWAY_TIMEOUT);
    }

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, ServerError.SERVER_GATEWAY_TIMEOUT, cause);
    }

    public GatewayTimeoutException(Throwable cause) {
        super(ServerError.SERVER_GATEWAY_TIMEOUT, cause);
    }
}
