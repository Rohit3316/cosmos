package com.stellantis.cosmos.sqs.app.configuration;

import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import jakarta.annotation.PostConstruct;
import java.util.List;
/**
 * Configuration class for dynamically creating SQS listener containers.
 *
 * Dynamically registers {@link SqsMessageListenerContainer} beans for each
 * {@link BatchMessageProcessor} based on their queue names and configuration.
 *
 * Dependencies:
 * - {@link SqsAsyncClient} for interacting with SQS.
 * - {@link GenericApplicationContext} for registering Spring beans dynamically.
 * - {@link BatchMessageProcessor} implementations for processing messages.
 */

//@Configuration
//Rohit Salunkhe
@Configuration
@ConditionalOnProperty(
        value = "cosmos.sqs.enabled",
        havingValue = "true",
        matchIfMissing = true
)
//Rohit Salunkhe
public class DynamicSqsListenerConfig {


    private final SqsAsyncClient sqsAsyncClient;
    private final GenericApplicationContext genericContext;
    private final List<BatchMessageProcessor<?>> messageHandlers;

    public DynamicSqsListenerConfig(SqsAsyncClient sqsAsyncClient,
                                    GenericApplicationContext genericContext,
                                    List<BatchMessageProcessor<?>> messageHandlers) {

        this.sqsAsyncClient = sqsAsyncClient;
        this.genericContext = genericContext;
        this.messageHandlers = messageHandlers;
    }

    /**
     * Dynamically creates and registers SQS listener containers beans for each message handler.
     *
     * For each {@link BatchMessageProcessor}:
     * - Retrieves the queue name and container configuration options.
     * - Creates an {@link SqsMessageListenerContainer} using the provided SQS async client.
     * - Registers the container as a singleton Spring bean with a unique name.
     */
    @PostConstruct
    public void createListenerContainers() {

        messageHandlers.forEach(handler -> {

            String queueName = handler.getQueueName();
            // Use builder to create a listener container dynamically
            SqsMessageListenerContainer<?> container = SqsMessageListenerContainerFactory
                    .builder()
                    .configure(handler.getContainerConfigOptions()) // Fetch container options dynamically
                    .sqsAsyncClientSupplier(() -> sqsAsyncClient)
                    .messageListener(handler)
                    .build()
                    .createContainer(queueName);

            // Register the container as a Spring bean dynamically
            genericContext.registerBean(queueName + "-listener-container",
                    SqsMessageListenerContainer.class,
                    () -> container,
                    beanDefinition -> beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON));

        });
    }


}
