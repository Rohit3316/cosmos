package org.eclipse.hawkbit.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.mockito.Mockito;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;

import java.util.concurrent.CompletableFuture;

/**
 * Test configuration for mocking the AWS SQS Async Client.
 * <p>
 * Provides a mock implementation of {@link SqsAsyncClient} for use in tests,
 * returning a dummy queue URL for any {@link GetQueueUrlRequest}.
 */
@TestConfiguration
public class MockSqsConfig {

    /**
     * Provides a mocked {@link SqsAsyncClient} bean.
     * The mock returns a completed future with a dummy queue URL for any getQueueUrl request.
     *
     * @return mocked SqsAsyncClient
     */
    @Bean
    public SqsAsyncClient sqsAsyncClient() {
        SqsAsyncClient mockSqsClient = Mockito.mock(SqsAsyncClient.class);
        CompletableFuture<GetQueueUrlResponse> future = CompletableFuture.completedFuture(
                GetQueueUrlResponse.builder().queueUrl("dummy-url").build()
        );
        Mockito.when(mockSqsClient.getQueueUrl(Mockito.any(GetQueueUrlRequest.class))).thenReturn(future);
        return mockSqsClient;
    }
}