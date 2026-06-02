package org.cosmos.s3;

import org.springframework.beans.factory.annotation.Value;

public class DataSizeConverter {

    @Value("${org.cosmos.file.artifacts.multipart.file.size:10MB}")
    private String chunkSize;
    private static final long BYTES_IN_KB = 1024;
    private static final long BYTES_IN_MB = 1024 * BYTES_IN_KB;
    private static final long BYTES_IN_GB = 1024 * BYTES_IN_MB;

    /**
     * Converts the given size and unit to its equivalent byte representation.
     *
     * @param size the size value to be converted.
     * @param unit the unit of the size value. It can be one of the following: "GB", "MB", "KB", "B".
     * @return the size in bytes.
     * @throws IllegalArgumentException if the provided unit is not supported.
     */
    private long convertToBytes(long size, String unit) {
        unit = unit.toUpperCase();  // Normalize unit to uppercase for consistency
        return switch (unit) {
            case "GB" -> size * BYTES_IN_GB;
            case "MB" -> size * BYTES_IN_MB;
            case "KB" -> size * BYTES_IN_KB;
            case "B" -> size;
            default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
        };
    }

    /**
     * Retrieves the chunk size in bytes by parsing the value from the properties.
     *
     * @return the chunk size in bytes.
     */
    public int getChunkSizeInBytes() {
        // Parse the chunk size and unit (e.g., "10MB") from the property
        String[] parts = chunkSize.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)"); // Split into size and unit
        if (parts.length == 2) {
            long size = Long.parseLong(parts[0]);
            String unit = parts[1].toUpperCase(); // Ensure the unit is uppercase
            long chunkSizeInBytes = convertToBytes(size, unit);

            // Check if the chunk size exceeds the max value of int
            if (chunkSizeInBytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("Chunk size exceeds the maximum allowed value of 2GB.");
            }

            return (int) chunkSizeInBytes;
        } else {
            throw new IllegalArgumentException("Invalid chunk size format: " + chunkSize);
        }
    }
}
