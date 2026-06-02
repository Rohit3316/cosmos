package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.ServerError;

import jakarta.validation.ValidationException;

public class PackageSizeLimitExceededException extends ValidationException {

    /**
     * Constructs a new {@code MaxPackageSizeExceededException} with a default error message
     * indicating that the maximum allowed package size has been exceeded.
     */
    public PackageSizeLimitExceededException() {
        super(ServerError.MAX_PACKAGE_SIZE_EXCEEDED.toString());
    }

    /**
     * Constructs a new {@code MaxPackageSizeExceededException} with a default error message
     * and a specified cause, indicating that the maximum allowed package size has been exceeded.
     *
     * @param cause the underlying cause of this exception
     */
    public PackageSizeLimitExceededException(final Throwable cause) {
        super(ServerError.MAX_PACKAGE_SIZE_EXCEEDED.toString(), cause);
    }

    /**
     * Constructs a new {@code MaxPackageSizeExceededException} with a custom error message
     * describing the reason for exceeding the maximum allowed package size.
     *
     * @param message the detail message explaining the exception
     */
    public PackageSizeLimitExceededException(final String message) {
        super(message);
    }

    /**
     * Constructs a new {@code MaxPackageSizeExceededException} with a custom error message
     * and a specified cause, describing the reason for exceeding the maximum allowed package size.
     *
     * @param message the detail message explaining the exception
     * @param cause   the underlying cause of this exception
     */
    public PackageSizeLimitExceededException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * Retrieves the {@link ServerError} associated with this exception,
     * representing the error condition when the maximum allowed package size is exceeded.
     *
     * @return the {@link ServerError} constant for maximum package size exceeded
     */
    public ServerError getServerError() {
        return ServerError.MAX_PACKAGE_SIZE_EXCEEDED;
    }
}
