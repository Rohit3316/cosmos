package org.eclipse.hawkbit.repository.file.supportpackage.configuration;


import jakarta.validation.ValidationException;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration properties for the file-system repository for support package, e.g. the base-path
 * to store the files.
 */
@ConfigurationProperties("org.cosmos.file.support-package.file")
@Getter
@Setter
public class SupportPackageFileSystemProperties {

    private static final Pattern SIZE_PATTERN = Pattern.compile("^(\\d+)(B|KB|MB|GB)$");
    private static final long BYTES_IN_KB = 1024L;
    private static final long BYTES_IN_MB = BYTES_IN_KB * 1024L;
    private static final long BYTES_IN_GB = BYTES_IN_MB * 1024L;


    /**
     * The S3 bucket configurations.
     */
    private SupportPackageFileSystemProperties.S3bucket s3bucket = new SupportPackageFileSystemProperties.S3bucket();

    @Getter
    @Setter
    public static class S3bucket {
        /**
         * The S3 bucket name.
         */
        private String name = "cosmos-us-east-1-dev-support-packages";
        /**
         * The expiry time of presigned url in minutes.
         */
        private String presignedUrlExpiry = "10m";

        public Long getPreSignedUrlExpiryTime() {
            /**
             * pattern for presignedUrlExpiry
             * eg presignedUrlExpiry: 60s, 10m, 4h, 5d for 60seconds, 10 mins, 4hours and 5days
             */
            Pattern pattern = Pattern.compile("(\\d+)([smhd])");
            Matcher matcher = pattern.matcher(getPresignedUrlExpiry());

            if (matcher.matches()) {
                int value = Integer.parseInt(matcher.group(1));
                String unit = matcher.group(2);

                return switch (unit) {
                    case "s" -> (long) value;
                    case "m" -> value * 60L;
                    case "h" -> value * 3600L;
                    case "d" -> value * 86400L;
                    default -> throw new ValidationException("Invalid time unit in preSignedUrlExpiry");
                };
            } else {
                throw new ValidationException("Invalid format for preSignedUrlExpiry");
            }
        }
    }

    /**
     * The base-path of the directory to store the support package.
     */
    private String path = "./support-package";

    /**
     * Max file size of ESP or RSP
     */

    private String fileSize = "1KB";

    /**
     * Converts the size string (e.g., "1KB", "1MB", "1GB") to its equivalent byte representation.
     *
     * @return the size in bytes.
     * @throws IllegalArgumentException if the size format is invalid.
     */
    public long getSizeInBytes() {
        Matcher matcher = SIZE_PATTERN.matcher(getFileSize());
        if (matcher.matches()) {
            long size = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2).toUpperCase();
            return convertToBytes(size, unit);
        } else {
            throw new IllegalArgumentException("Invalid size format: " + getFileSize());
        }
    }

    /**
 * Converts the given size and unit to its equivalent byte representation.
 *
 * @param size the size value to be converted.
 * @param unit the unit of the size value. It can be one of the following: "GB", "MB", "KB", "B".
 * @return the size in bytes.
 * @throws IllegalArgumentException if the provided unit is not supported.
 */
private long convertToBytes(long size, String unit) {
    return switch (unit) {
        case "GB" -> size * BYTES_IN_GB;
        case "MB" -> size * BYTES_IN_MB;
        case "KB" -> size * BYTES_IN_KB;
        case "B" -> size;
        default -> throw new IllegalArgumentException("Unsupported unit: " + unit);
    };
}
}
