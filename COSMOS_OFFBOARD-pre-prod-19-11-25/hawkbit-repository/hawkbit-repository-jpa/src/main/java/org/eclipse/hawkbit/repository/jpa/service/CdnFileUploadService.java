package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.sns.SnsPublishable;
import org.springframework.scheduling.annotation.Async;

/**
 * A service interface for uploading files to a CDN (Content Delivery Network).
 * Supports asynchronous file uploads for any type that implements {@link SnsPublishable}.
 *
 * @param <T> the type of the file to be uploaded, which must implement {@link SnsPublishable}.
 */
public interface CdnFileUploadService<T extends SnsPublishable> {

    /**
     * Uploads the specified file to the CDN asynchronously.
     *
     * @param file the file to upload, must not be null.
     */
    @Async
    void uploadFile(T file) throws IllegalArgumentException;

}