package org.eclipse.hawkbit.repository.event.remote.entity;

import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.eclipse.hawkbit.feignclient.kafka.KafkaClient;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
public class KafkaMessageUtilTest {

    @Mock
    private KafkaClient kafkaClient;

    @Captor
    private ArgumentCaptor<GeneralIdleMessage> generalIdleMessageCaptor;

    @Captor
    private ArgumentCaptor<GeneralErrorMessage> generalErrorMessageCaptor;

    @InjectMocks
    private KafkaMessageService kafkaMessageService;



    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }


    // Test sending General Feedback 'IDLE' successfully
    @Test
    void testSendGeneralFeedbackIdleSuccess(CapturedOutput output) {
        // Create a mock GeneralIdleMessage
        GeneralIdleMessage generalIdleMessage = new GeneralIdleMessage();

        // Act
        kafkaMessageService.sendGeneralFeedbackIdle(generalIdleMessage);

        // Verify that publishGeneralFeedbackIdle was called with the correct message
        verify(kafkaClient).publishGeneralFeedbackIdle(generalIdleMessageCaptor.capture());
        assertEquals(generalIdleMessage, generalIdleMessageCaptor.getValue());

        // Verify that the info log message is captured

        assertTrue(output.getOut().contains("General feedback 'IDLE' status message sent successfully"));
    }

    // Test sending General Feedback 'IDLE' with an exception
    @Test
    void testSendGeneralFeedbackIdleWithException(CapturedOutput output) {
        GeneralIdleMessage generalIdleMessage = new GeneralIdleMessage();

        // Simulate an exception thrown by KafkaClient
        doThrow(new RuntimeException("Kafka Error")).when(kafkaClient).publishGeneralFeedbackIdle(generalIdleMessage);

        // Act
        kafkaMessageService.sendGeneralFeedbackIdle(generalIdleMessage);

        // Verify that error is logged
        assertTrue(output.getOut().contains("Error occurred while sending general feedback 'IDLE' status message"));
    }

    // Test sending General Feedback 'ERROR' successfully
    @Test
    void testSendGeneralFeedbackErrorSuccess(CapturedOutput output) {
        // Create a mock GeneralErrorMessage
        GeneralErrorMessage generalErrorMessage = new GeneralErrorMessage();

        // Act
        kafkaMessageService.sendGeneralFeedbackError(generalErrorMessage);

        // Verify that publishGeneralFeedbackError was called with the correct message
        verify(kafkaClient).publishGeneralFeedbackError(generalErrorMessageCaptor.capture());
        assertEquals(generalErrorMessage, generalErrorMessageCaptor.getValue());

        // Verify that the info log message is captured
        assertTrue(output.getOut().contains("General feedback 'ERC' status message sent successfully"));
    }

    // Test sending General Feedback 'ERROR' with an exception
    @Test
    void testSendGeneralFeedbackErrorWithException(CapturedOutput output) {
        GeneralErrorMessage generalErrorMessage = new GeneralErrorMessage();

        // Simulate an exception thrown by KafkaClient
        doThrow(new RuntimeException("Kafka Error")).when(kafkaClient).publishGeneralFeedbackError(generalErrorMessage);

        // Act
        kafkaMessageService.sendGeneralFeedbackError(generalErrorMessage);

        // Verify that error is logged
        assertTrue(output.getOut().contains("Error occurred while sending general feedback 'ERC' status message"));
    }


}

