package org.eclipse.hawkbit.feignclient.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class UnsupportedMediaTypeException extends AbstractServerRtException {


    public UnsupportedMediaTypeException() {
        super(ServerError.CLIENT_UNSUPPORTED_MEDIA_TYPE);
    }

    public UnsupportedMediaTypeException(String message) {
        super(message, ServerError.CLIENT_UNSUPPORTED_MEDIA_TYPE);
    }

    public UnsupportedMediaTypeException(String message, Throwable cause) {
        super(message, ServerError.CLIENT_UNSUPPORTED_MEDIA_TYPE, cause);
    }

    public UnsupportedMediaTypeException(Throwable cause) {
        super(ServerError.CLIENT_UNSUPPORTED_MEDIA_TYPE, cause);
    }
}
