package org.eclipse.hawkbit.api;

import lombok.Getter;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for managing deployment log URLs.
 * <p>
 * Provides access to S3-specific log directory patterns used for storing deployment logs.
 */
@Getter
@Component
public final class DeploymentLogUrlProperties {

    /**
     * Encapsulates S3-specific configuration properties for deployment log storage.
     * <p>
     * Contains the directory pattern used to organize deployment logs in S3 buckets.
     */
    @Getter
    public static class S3 {
        /**
         * Directory pattern for S3 logs.
         */
        private final String directory = "/{tenant}/{rolloutId}/{actionId}/";
    }

    private final S3 s3 = new S3();
}
