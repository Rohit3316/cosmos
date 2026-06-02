package org.eclipse.hawkbit.repository.model;

/**
 * Represents the configuration for signing certificates used in the system.
 * This interface extends BaseEntity to inherit common entity properties.
 */
public interface SigningCertificateConfiguration extends BaseEntity{

/**
     * Gets the Public Key Infrastructure (PKI) identifier.
     *
     * @return the PKI identifier as a String
     */
    String getPki();

    /**
     * Gets the ECU ID issuer.
     *
     * @return the ECU ID issuer as a String
     */
    String getEcuIdIssuer();

    /**
     * Gets the file path to the DD certificate.
     *
     * @return the DD certificate path as a String
     */
    String getDdCertificatePath();

    /**
     * Gets the file path to the ESP certificate.
     *
     * @return the ESP certificate path as a String
     */
    String getEspCertificatePath();

    /**
     * Gets the file path to the RSP certificate.
     *
     * @return the RSP certificate path as a String
     */
    String getRspCertificatePath();

    /**
     * Gets the file path to the intermediate CA certificate.
     *
     * @return the intermediate CA certificate path as a String
     */
    String getIntermediateCACertificatePath();

    /**
     * Gets the ssm param store path to the DD private key.
     *
     * @return the DD private key path as a String
     */
    String getDdPrivateKeyPath();

    /**
     * Gets the ssm param store path to the ESP private key.
     *
     * @return the ESP private key path as a String
     */
    String getEspPrivateKeyPath();

    /**
     * Gets the ssm param store path to the RSP private key.
     *
     * @return the RSP private key path as a String
     */
    String getRspPrivateKeyPath();
}
