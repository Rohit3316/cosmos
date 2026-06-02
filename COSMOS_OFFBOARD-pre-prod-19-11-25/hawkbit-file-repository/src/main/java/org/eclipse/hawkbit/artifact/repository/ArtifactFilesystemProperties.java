/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import jakarta.validation.ValidationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Configuration properties for the file-system repository, e.g. the base-path
 * to store the files.
 */
@ConfigurationProperties("org.eclipse.hawkbit.repository.file")
@Setter
@Getter
public class ArtifactFilesystemProperties {
    /**
     * The S3 bucket configurations.
     */
    private S3bucket s3bucket = new S3bucket();

    @Getter
    @Setter
    public static class S3bucket {
        /**
         * The S3 bucket name.
         */
        private String name = "cosmos-us-east-1-dev-artifacts";
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


}
