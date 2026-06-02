package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public class FileDownloadFailureException  extends AbstractServerRtException {
    public FileDownloadFailureException(String errorMessage) {
        this(errorMessage,ServerError.FILE_DOWNLOAD_FAILED);
    }

    public FileDownloadFailureException(String message, ServerError error) {
        super(message, error);
    }
}
