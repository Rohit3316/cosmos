package org.eclipse.hawkbit.mgmt.rest.resource.hashgenerator;

import java.io.IOException;
import java.io.InputStream;
/**
 * This interface defines a contract for generating checksums for input streams.
 * It provides a method to calculate the checksum based on a specified hash algorithm.
 */
public interface HashGenerator {

    /**
     * Calculates the checksum of the input stream using the specified hash algorithm.
     *
     * @param hashAlgorithm The hash algorithm to use for checksum calculation.
     * @param inputStream The input stream for which the checksum needs to be calculated.
     * @return The checksum as a string.
     * @throws IOException If an I/O error occurs while reading from the input stream.
     */
    String calculateChecksum(HashAlgorithm hashAlgorithm, InputStream inputStream) throws IOException;
}