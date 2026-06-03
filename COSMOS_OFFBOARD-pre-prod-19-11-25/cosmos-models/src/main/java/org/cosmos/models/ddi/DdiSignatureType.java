package org.cosmos.models.ddi;

/**
 * Enum representing the types of DDI signatures.
 * <p>
 * This enum is used to specify the type of signature being handled in the DD generation process. Ex: ESP, RSP, DD.
 * </p>
 */
public enum DdiSignatureType {
    /**
     * DD signature type.
     */
    DD,

    /**
     * ESP signature type.
     */
    ESP,

    /**
     * RSP signature type.
     */
    RSP,

    /**
     * Intermediate CA signature type
     */
    INTERMEDIATE_CA
}
