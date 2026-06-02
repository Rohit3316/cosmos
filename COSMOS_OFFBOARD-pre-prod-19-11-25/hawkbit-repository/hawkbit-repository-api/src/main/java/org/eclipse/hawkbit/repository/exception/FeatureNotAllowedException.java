package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

/**
 * The {@link FeatureNotAllowedException} is thrown when a specific feature is not allowed.
 */
public class FeatureNotAllowedException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final ServerError THIS_ERROR = ServerError.FEATURE_NOT_ALLOWED;

    /**
     * Default constructor.
     */
    public FeatureNotAllowedException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     *
     * @param cause
     *            of the exception
     */
    public FeatureNotAllowedException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message
     *            of the exception
     * @param cause
     *            of the exception
     */
    public FeatureNotAllowedException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     *
     * @param message
     *            of the exception
     */
    public FeatureNotAllowedException(final String message) {
        super(message, THIS_ERROR);
    }
}