package org.cosmos.s3;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static software.amazon.awssdk.utils.BinaryUtils.toHex;

/**
 * This class handles checksum calculations and verification for file upload chunks.
 * It supports calculating individual chunk checksums and a checksum for all chunks combined.
 */
@Service
public class ChecksumCalculator {

    private static final String SHA_256 = "SHA-256";
    private final List<String> chunkChecksums = new ArrayList<>();
    private static final Logger LOG = LoggerFactory.getLogger(S3Repository.class);

    /**
     * Adds a checksum for a specific chunk.
     *
     * @param checksum the checksum to add
     * @throws IllegalArgumentException if the checksum is null or empty
     */
    public void addChunkChecksum(String checksum) {
        if (checksum == null || checksum.isEmpty()) {
            throw new IllegalArgumentException("Checksum cannot be null or empty.");
        }
        chunkChecksums.add(checksum);
        LOG.debug("Chunk checksum added: " + checksum);
    }

    /**
     * Calculates the checksum for a chunk of data.
     *
     * @param data the chunk of data
     * @return the calculated checksum as a hexadecimal string
     * @throws IllegalArgumentException if the data is null or empty
     */
    public String calculateChunkChecksum(byte[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Cannot calculate checksum for null or empty data.");
        }
        String checksum = calculateSHA256Checksum(data);  // Calculate SHA-256 checksum for the chunk
        LOG.debug("Checksum for chunk calculated: " + checksum);
        return checksum;
    }

    /**
     * Calculates the checksum of all chunk checksums (checksum of checksums).
     *
     * @return the checksum of all chunks' checksums as a hexadecimal string
     * @throws IllegalStateException if no chunk checksums are available
     */
    public String calculateChecksumOfChecksums() {
        if (chunkChecksums.isEmpty()) {
            throw new IllegalStateException("No chunk checksums available to calculate checksum of checksums.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);  // Use SHA-256 for checksum calculation
            for (String checksum : chunkChecksums) {
                byte[] checksumBytes = hexStringToByteArray(checksum);  // Convert checksum string to byte array
                digest.update(checksumBytes);  // Update the digest with each chunk's checksum
            }
            byte[] combinedHash = digest.digest();  // Finalize the hash calculation
            return byteArrayToHex(combinedHash);  // Convert final hash to hexadecimal string
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);  // Handle algorithm not found
        }
    }

    /**
     * Converts a hexadecimal string to a byte array.
     *
     * @param s the hexadecimal string
     * @return the byte array representation
     */
    private byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));  // Convert each pair of hex chars to a byte
        }
        return data;
    }

    /**
     * Converts a byte array to a hexadecimal string.
     *
     * @param bytes the byte array
     * @return the hexadecimal string representation
     */
    private String byteArrayToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);  // Convert byte to hex
            if (hex.length() == 1) {
                hexString.append('0');  // Add leading zero for single-digit hex
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Retrieves all the chunk checksums stored in this class.
     *
     * @return a list of chunk checksums
     */
    public List<String> getAllChunkChecksums() {
        LOG.debug("Retrieving all chunk checksums.");
        return new ArrayList<>(chunkChecksums);  // Return a copy of the list to avoid external modification
    }

    /**
     * Internal utility method to calculate the SHA-256 checksum of data.
     *
     * @param data the data to calculate the checksum for
     * @return the calculated SHA-256 checksum as a hexadecimal string
     */
    private String calculateSHA256Checksum(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);  // Use SHA-256 algorithm
            byte[] hash = digest.digest(data);  // Calculate the hash
            return byteArrayToHex(hash);  // Convert the hash to hexadecimal and return it
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);  // Handle algorithm not found
        }
    }

    /**
     * Converts a Base64-encoded string to its hexadecimal string representation.
     *
     * @param base64EncodedString the Base64-encoded string
     * @return the hexadecimal string representation of the decoded data
     * @throws IllegalArgumentException if the input is null or invalid Base64
     */
    public String convertBase64ToHex(String base64EncodedString) {
        byte[] md5Hash = Base64.getDecoder().decode(base64EncodedString);
        return toHex(md5Hash);
    }

}
