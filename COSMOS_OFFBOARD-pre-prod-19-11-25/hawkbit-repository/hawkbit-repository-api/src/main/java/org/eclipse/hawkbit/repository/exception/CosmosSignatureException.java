package org.eclipse.hawkbit.repository.exception;

public class CosmosSignatureException extends RuntimeException {
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Creates the Exception from it's cause
     * @param cause the original exception
     */
    public CosmosSignatureException(final Exception cause) {
        super(cause);
    }

    /**
     * Creates the Exception from it's cause
     * @param errorWhileGeneratingSignature the error message
     * @param e the original exception
     */
    public CosmosSignatureException(String errorWhileGeneratingSignature, Exception e) {
        super(errorWhileGeneratingSignature, e);
    }

    /**
     * Creates the Exception from it's cause
     * @param message the error message
     */
    public CosmosSignatureException(String message) {
        super(message);
    }
}
