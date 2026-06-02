package org.eclipse.hawkbit.repository.exception;

/**
 * Exception thrown when an ECU ID certificate is not found in the certificate store (e.g., S3).
 */
public class EcuIdCertificateNotFoundException extends RuntimeException {
	public EcuIdCertificateNotFoundException(String message) {
		super(message);
	}

	public EcuIdCertificateNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
