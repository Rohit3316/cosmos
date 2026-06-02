package com.stellantis.cosmos.sqs.app.service.core;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.listener.ListenerMode;
import io.awspring.cloud.sqs.listener.MessageListener;
import io.awspring.cloud.sqs.listener.QueueNotFoundStrategy;
import io.awspring.cloud.sqs.listener.SqsContainerOptionsBuilder;
import io.awspring.cloud.sqs.listener.acknowledgement.Acknowledgement;
import io.awspring.cloud.sqs.listener.acknowledgement.handler.AcknowledgementMode;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SnsMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * Abstract base class for batch processing of messages from SQS.
 *
 * Provides methods for handling and processing collections of messages in batch mode.
 * Implements {@link MessageListener} for message consumption and integrates with
 * SQS messaging using a configurable {@link SqsMessagingMessageConverter}.
 *
 * Key Responsibilities:
 * - Defines abstract methods for message processing, payload type retrieval, and queue name.
 * - Handles message acknowledgment after successful processing.
 * - Configures SQS listener container options via {@link SqsContainerOptionsBuilder}.
 * Subclasses must implement:
 *  - {@link #process(Object, MessageHeaders)}
 *  - {@link #getPayloadClass()}
 *  - {@link #getQueueName()}
 *
 *  * @param <U> The type of the message payload to be processed.
 */
 @Slf4j
public abstract class BatchMessageProcessor<U> implements MessageListener<Object> {

    /**
     * Processes a single message with its headers.
     *
     * @param message the message payload of type {@code U} to be processed
     * @param headers the headers associated with the message
     */
    public abstract void process(U message, MessageHeaders headers);

    /**
     * Returns the class type of the message payload.
     *
     * @return the {@link Class} representing the payload type {@code U}
     */
    public abstract Class<U> getPayloadClass();

    /**
     * Returns the name of the SQS queue to be listened to.
     *
     * @return the name of the SQS queue as a {@link String}
     */
    public abstract String getQueueName();

    /**
     * Handles a batch of messages received from the SQS queue.
     *
     * Logs the received messages and delegates processing to the {@link #process(Collection)} method.
     *
     * @param messages a collection of messages to be processed
     */
    @Override
    public void onMessage(@NonNull Collection<Message<Object>> messages) {
        log.debug("messages arrived:{}", messages);
        process(messages);
    }
    /**
     * Handles a single message received from the SQS queue.
     *
     * This implementation throws {@link UnsupportedOperationException} as single messages
     * are not processed by default.
     *
     * Override the method for the custom behaviour
     *
     * @param message the single message to be processed
     * @throws UnsupportedOperationException always, as single message processing is not supported
     */
    @Override
    public void onMessage(@NonNull Message<Object> message) {
        throw new UnsupportedOperationException("Single Messages are not processed by default");
    }
    /**
     * Configures options for the SQS container.
     *
     * Sets up various configurations for batch message processing, including:
     * - Queue not found strategy
     * - Batch listener mode
     * - Manual acknowledgment mode
     * - Payload type mapping for messages
     * - Message converter
     * - Poll timeout and acknowledgment interval
     *
     * Override this method to customize the container Configuration
     *
     * @return a {@link Consumer} to customize {@link SqsContainerOptionsBuilder}
     */
    public Consumer<SqsContainerOptionsBuilder> getContainerConfigOptions() {
        AbstractMessagingMessageConverter<U> sqsConverter = getMessagingMessageConverter();
        sqsConverter.setPayloadTypeMapper(message -> getPayloadClass());
        return options -> options
                .queueNotFoundStrategy(QueueNotFoundStrategy.FAIL)
                .maxMessagesPerPoll(10)
                .listenerMode(ListenerMode.BATCH)
                .acknowledgementMode(AcknowledgementMode.MANUAL)
                .acknowledgementThreshold(0)
                .messageConverter(sqsConverter)
                .acknowledgementInterval(Duration.ofSeconds(5))
                .pollTimeout(Duration.ofSeconds(10));
    }

    /**
     * Retrieves the configured SQS messaging message converter.
     *
     * Casts and returns the {@link SqsMessagingMessageConverter} as
     * {@link AbstractMessagingMessageConverter} for the specific payload type {@code U}.
     *
     * Override this method to customize the converter
     * @return the {@link AbstractMessagingMessageConverter} for processing messages
     */

    @SuppressWarnings("unchecked")
    public AbstractMessagingMessageConverter<U> getMessagingMessageConverter() {
        return (AbstractMessagingMessageConverter<U>) createSqsMessageConverter(new ObjectMapper());
    }

    /**
     * Creates a configured SQS message converter.
     * Creates a {@link SqsMessagingMessageConverter} with a {@link SnsMessageConverter}
     * and a {@link MappingJackson2MessageConverter} for the payload.
     *
     * @param objectMapper the {@link ObjectMapper} for message serialization and deserialization
     * @return the configured {@link SqsMessagingMessageConverter}
     */
    private SqsMessagingMessageConverter createSqsMessageConverter(ObjectMapper objectMapper){
        MappingJackson2MessageConverter payloadConverter=new MappingJackson2MessageConverter();
        SnsMessageConverter snsMessageConverter =new SnsMessageConverter(payloadConverter,objectMapper);
        SqsMessagingMessageConverter messageConverter= new SqsMessagingMessageConverter();
        messageConverter.setPayloadMessageConverter(snsMessageConverter);
        return messageConverter;
    }

    /**
     * Processes a collection of messages in batch mode.
     *
     * Filters messages based on successful processing:
     * - Calls {@link #process(Object, MessageHeaders)} for each message.
     * - Logs message headers and payload during processing.
     * - Catches and logs exceptions for failed message processing.
     *
     * Acknowledges successfully processed messages.
     *
     * @param messages the collection of messages to be processed
     */

    @SuppressWarnings("unchecked cast")
    public void process(Collection<Message<Object>> messages) {

        List<Message<Object>> ackedMessages = messages.stream()
                .filter(message -> {
                    try {
                        MessageHeaders headers= message.getHeaders();
                        Object messagePayload= message.getPayload();
                        log.debug("headers:{}", headers);
                        SnsMessageConverter.SnsMessageWrapper wrappedPayload = (SnsMessageConverter.SnsMessageWrapper) messagePayload;
                        process((U) wrappedPayload.message(), headers);
                        return true; // Not added to the list if processing is successful
                    } catch (Exception e) {
                        log.error("exception:{}", e.getMessage());
                        return false; // Add to the list if an exception occurs
                    }
                })
                .toList();
        acknowledge(ackedMessages);
    }
    /**
     * Acknowledges a list of successfully processed messages.
     *
     * Delegates the acknowledgment to {@link Acknowledgement#acknowledge(List)}.
     *
     * Override this method for custom behaviour of acknowledgment process
     * @param messages the list of messages to be acknowledged
     */

    public void acknowledge(List<Message<Object>> messages) {
        Acknowledgement.acknowledge(messages);
    }

}
