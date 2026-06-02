package org.cosmos.models.kafka;

import lombok.Builder;
import lombok.Data;

/**
 * Represents the signature associated with inventory details.
 * This class encapsulates the signature and its type for inventory messages.
 */
@Data
@Builder
public class InventorySignature {

    /**
     * The signature string associated with the inventory details.
     * This field is used to store the cryptographic signature.
     */
    private String signature;

    /**
     * The type of signature used, such as the algorithm or method.
     * This field indicates the type of cryptographic signature.
     */
    private String signatureType;

    /**
     * Constructs an InventorySignature with the specified signature and signature type.
     *
     * @param signature the cryptographic signature string
     * @param signatureType the type of signature used (e.g., SHA256withECC)
     */
    public InventorySignature(String signature, String signatureType) {
        this.signature = signature;
        this.signatureType = signatureType;
    }

    /**
     * Default constructor for InventorySignature.
     * Initializes an instance with default values.
     */
    public InventorySignature() {}
}
