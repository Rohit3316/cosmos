package org.cosmos.sns.services;

import java.util.concurrent.CompletableFuture;

import org.cosmos.models.sns.SnsPublishable;
import software.amazon.awssdk.services.sns.model.PublishResponse;

public interface ISnsService<T extends SnsPublishable> {

    /**
     * Publishes a message to S3 or CDN upload SNS topic asynchronously.
     *
     * @return A CompletableFuture representing the result of the publish operation.
     */
    CompletableFuture<PublishResponse> publishMessage(T message);
}
