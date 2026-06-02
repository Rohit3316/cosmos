package org.eclipse.hawkbit.mgmt.rest.resource.hashgenerator;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;

/**
 * This class provides functionality to calculate checksums for input streams.
 * It implements the {@link HashGenerator} interface.
 *
 */
@Service
public class ChecksumProvider implements HashGenerator {

    /**
     * Calculates the checksum of the given input stream using the specified hash algorithm.
     *
     * @param hashAlgorithm The hash algorithm to use for the checksum calculation.
     * @param inputStream The input stream to calculate the checksum for.
     * @return The checksum as a hexadecimal string.
     * @throws IOException If an error occurs while reading from the input stream.
     */
    @Override
    public String calculateChecksum(HashAlgorithm hashAlgorithm, InputStream inputStream) throws IOException {
        MessageDigest md = hashAlgorithm.getMessageDigest();
        byte[] byteArray = new byte[1024];
        int bytesCount;


        while ((bytesCount = inputStream.read(byteArray)) != -1) {
            md.update(byteArray, 0, bytesCount);
        }


        inputStream.close();

        byte[] bytes = md.digest();

        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
