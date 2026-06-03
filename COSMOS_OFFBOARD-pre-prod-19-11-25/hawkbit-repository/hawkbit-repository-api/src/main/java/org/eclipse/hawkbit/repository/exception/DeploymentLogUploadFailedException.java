package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

public final class DeploymentLogUploadFailedException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new FileUploadFailedException with
     * {@link ServerError#DEPLOYMENT_LOG_UPLOAD_FAILED} error.
     */
    public DeploymentLogUploadFailedException() {
        super(ServerError.DEPLOYMENT_LOG_UPLOAD_FAILED);
    }

    /**
     * @param cause
     *            for the exception
     */
    public DeploymentLogUploadFailedException(final Throwable cause) {
        super(ServerError.DEPLOYMENT_LOG_UPLOAD_FAILED, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public DeploymentLogUploadFailedException(final String message) {
        super(message, ServerError.DEPLOYMENT_LOG_UPLOAD_FAILED);
    }

    /**
     * @param message
     *            for the error
     * @param cause
     *            of the error
     */
    public DeploymentLogUploadFailedException(final String message, final Throwable cause) {
        super(message, ServerError.DEPLOYMENT_LOG_UPLOAD_FAILED, cause);
    }

}
