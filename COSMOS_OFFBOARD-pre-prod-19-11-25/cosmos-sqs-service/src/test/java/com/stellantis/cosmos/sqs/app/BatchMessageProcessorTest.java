package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import io.awspring.cloud.sqs.support.converter.AbstractMessagingMessageConverter;
import io.awspring.cloud.sqs.support.converter.SnsMessageConverter;
import io.awspring.cloud.sqs.support.converter.SqsMessagingMessageConverter;

import io.qameta.allure.Description;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchMessageProcessorTest {


    @Description("Test the batch processing of messages when onMessage method is invoked.")
    @Test
     void givenMessages_whenOnMessage_thenProcessBatch() {
       BatchMessageProcessor<?> processor= getSpiedBatchProcessor();

        // Mock SqsMessagingMessageConverter
        MessageHeaders mockHeaders = mock(MessageHeaders.class);

        // Arrange
        Message<Object> mockMessage = mock(Message.class);
        List<Message<Object>> messages = List.of(mockMessage);

        // Mock payload
        Mockito.when(mockMessage.getPayload())
                .thenReturn(new SnsMessageConverter.SnsMessageWrapper("", "payload"));
        when(mockMessage.getHeaders()).thenReturn(mockHeaders);


        doNothing().when(processor).acknowledge(messages);
        // Act
        processor.onMessage(messages);

        // Assert
        Mockito.verify(mockMessage).getPayload();
        Mockito.verify(mockMessage).getHeaders();

    }

    @Description("Given a message is received, when processing fails, then the acknowledgment should not include that message.")
    @Test
     void givenMessageIsReceivedWhenProcessingFailsThenAcknowledgementShouldNotHaveThatMessage() {

        BatchMessageProcessor<?> processor= getBadSpiedBatchProcessor();
        MessageHeaders mockHeaders = mock(MessageHeaders.class);

        // Arrange
        Message<Object> mockMessage = mock(Message.class);
        List<Message<Object>> messages = List.of(mockMessage);

        // Mock payload
        Mockito.when(mockMessage.getPayload())
                .thenReturn(new SnsMessageConverter.SnsMessageWrapper("", "payload"));
        when(mockMessage.getHeaders()).thenReturn(mockHeaders);
        processor.process(messages);

        ArgumentCaptor<List<Message<Object>>> argumentCaptor= ArgumentCaptor.forClass(List.class);
        verify(processor).acknowledge(argumentCaptor.capture());
        Assertions.assertEquals(0,argumentCaptor.getValue().size());
    }

    @Test
    @Description("Test the creation of SqsMessageConverter.")
    void givenBatchProcessor_whenCreateSqsMessageConverter_thenSqsMessageConverter() {
        AbstractMessagingMessageConverter<?> converter = getSpiedBatchProcessor().getMessagingMessageConverter();
        assertNotNull(converter, "SqsMessagingMessageConverter should not be null");
        assertInstanceOf(SqsMessagingMessageConverter.class, converter);
    }

    private BatchMessageProcessor<?> getSpiedBatchProcessor(){
        return Mockito.spy(new BatchMessageProcessor<String>() {
            @Override
            public void process(String message, MessageHeaders headers) {

            }

            @Override
            public Class<String> getPayloadClass() {
                return String.class;
            }

            @Override
            public String getQueueName() {
                return "test-queue";
            }
        });
    }

    private BatchMessageProcessor<?> getBadSpiedBatchProcessor(){

            return Mockito.spy(new BatchMessageProcessor<String>() {
                @Override
                public void process(String message, MessageHeaders headers) {
                    throw new UnsupportedOperationException("unsupported ops");
                }

                @Override
                public Class<String> getPayloadClass() {
                    return String.class;
                }

                @Override
                public String getQueueName() {
                    return "test-queue";
                }
            });
        }

}
