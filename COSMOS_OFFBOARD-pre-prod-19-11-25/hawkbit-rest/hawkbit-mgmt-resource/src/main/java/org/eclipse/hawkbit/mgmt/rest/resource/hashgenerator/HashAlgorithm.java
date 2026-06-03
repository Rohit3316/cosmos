package org.eclipse.hawkbit.mgmt.rest.resource.hashgenerator;



import org.eclipse.hawkbit.exception.GenericSpServerException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.Supplier;

/**
 * Enum representing different hash algorithms that can be used for hashing.
 * Each enum constant represents a specific algorithm and provides a supplier for creating a MessageDigest instance.
 * This approach is used to avoid creating a MessageDigest instance of a non existing algorithm
 */
public enum HashAlgorithm {
    /**
     * MD5 hash algorithm.
     * Uses the MessageDigest.getInstance("MD5") method to create a MessageDigest instance.
     */
    MD5(()-> {
        try {
            return MessageDigest.getInstance("MD5");//NOSONAR
        } catch (NoSuchAlgorithmException e) {
            throw new GenericSpServerException(e);
        }
    }),

    /**
     * SHA-256 hash algorithm.
     * Uses the MessageDigest.getInstance("SHA-256") method to create a MessageDigest instance.
     */
    SHA_256(()-> {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new GenericSpServerException(e);
        }
    });

    /**
     * Supplier for creating a MessageDigest instance for the specific hash algorithm.
     */
    private final Supplier<MessageDigest> digestSupplier;

    /**
     * Constructor for HashAlgorithm enum.
     * @param digestSupplier Supplier for creating a MessageDigest instance for the specific hash algorithm.
     */
    HashAlgorithm(Supplier<MessageDigest> digestSupplier) {
        this.digestSupplier = digestSupplier;
    }

    /**
     * Gets the MessageDigest instance for the specific hash algorithm.
     * @return MessageDigest instance for the specific hash algorithm.
     */
    public MessageDigest getMessageDigest(){
        return digestSupplier.get();
    }
}
