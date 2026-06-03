package org.cosmos.sns.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;

import org.cosmos.models.sns.SnsPublishable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

/**
 * Abstract service for publishing messages to SNS topics.
 */
public abstract class SnsService<T extends SnsPublishable> implements ISnsService<T> {

    private static final Logger LOG = LoggerFactory.getLogger(SnsService.class);

    private final SnsAsyncClient snsAsyncClient;

    protected SnsService(SnsAsyncClient snsAsyncClient) {
        this.snsAsyncClient = snsAsyncClient;
    }

    protected abstract String getTopicArn(T message);

    public abstract SnsServiceType getSnsServiceType();

    /**
     * Publishes a message to S3 or CDN upload SNS topic asynchronously.
     *
     * @return A CompletableFuture representing the result of the publish operation.
     */
    @Override
    public CompletableFuture<PublishResponse> publishMessage(T message) {
        try {
            // Serialize the message to JSON
            String jsonMessage = new ObjectMapper().writeValueAsString(message);

            // Get the appropriate SNS topic ARN based on the type of the message
            String topicArn = getTopicArn(message);

            // Publish the message to the determined SNS topic
            return publishMessage(jsonMessage, topicArn);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            // Log an error message if there is an exception during JSON serialization or request creation
            LOG.error("Failed to serialize message for SNS reason: {}", e.getMessage());
            // Return a failed CompletableFuture to indicate that the operation did not complete successfully
            return CompletableFuture.failedFuture(e);
        }
    }
    private CompletableFuture<PublishResponse> publishMessage(String message, String topicArn) {

        // Create a PublishRequest object using the builder pattern
        PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)   // Specify the SNS topic ARN
                .message(message)    // Set the JSON message
                .build();               // Build the request

        // Asynchronously publish the message to SNS
        CompletableFuture<PublishResponse> publishResponse = snsAsyncClient.publish(publishRequest);

        // Handle success and failure asynchronously
        publishResponse.whenComplete((response, exception) -> {
            if (exception != null) {
                // Log an error message if publishing fails
                LOG.error("Failed to publish message to SNS", exception);
            } else {
                // Check if the messageId is present and log accordingly
                String messageId = response.messageId();
                if (messageId != null) {
                    LOG.info("Message published to SNS with messageId: {}", messageId);
                } else {
                    LOG.warn("Message published to SNS, but no messageId was returned.");
                }
            }
        });

        // Return the CompletableFuture for further handling by the caller
        return publishResponse;


    }


}