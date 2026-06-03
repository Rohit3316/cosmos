package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.configuration.DynamicSqsListenerConfig;
import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.qameta.allure.Description;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.time.Duration;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
@ExtendWith(MockitoExtension.class)
class DynamicSqsListenerConfigTest {

    @Mock
    private SqsAsyncClient sqsAsyncClient;

    @Mock
    private GenericApplicationContext genericContext;

    @Mock
    private BatchMessageProcessor<?> handler;

    @InjectMocks
    private DynamicSqsListenerConfig config;

    @Description("Given a list of handlers, when createListenerContainers is invoked, then the corresponding listener containers should be registered as beans.")
    @Test
     void givenHandlers_whenCreateListenerContainers_thenRegisterBeans() {
        String queueName = "testQueue";
            Mockito.when(handler.getQueueName()).thenReturn(queueName);
        Mockito.when(handler.getContainerConfigOptions()).thenReturn(options -> {
            options.maxMessagesPerPoll(10).pollTimeout(Duration.ofSeconds(10));
        });
        List<BatchMessageProcessor<?>> handlers = List.of(handler);
        ReflectionTestUtils.setField(config, "messageHandlers", handlers);
            // Act
            config.createListenerContainers();

            // Assert
            Mockito.verify(genericContext, Mockito.times(1)).registerBean(
                    eq(queueName + "-listener-container"),
                    eq(SqsMessageListenerContainer.class),
                    any(),
                    any()
            );
    }
}
