package org.eclipse.hawkbit.repository.exception;

/**
 * Exception thrown if there is an issue with upload of ECU certificates.
 *
 */
public class EcuCertificateException extends RuntimeException {
    public EcuCertificateException(String message) {
        super(message);
    }
}