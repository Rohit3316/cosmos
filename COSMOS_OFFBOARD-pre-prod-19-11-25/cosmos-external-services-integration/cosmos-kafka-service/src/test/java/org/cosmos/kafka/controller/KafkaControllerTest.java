package org.cosmos.kafka.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.jfr.Description;
import org.cosmos.kafka.service.KafkaProducerService;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.InventorySignature;
import org.cosmos.models.kafka.RolloutErrorMessage;
import org.cosmos.models.kafka.RolloutStatusMessage;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * KafkaControllerTest is a test class for the KafkaController.
 */
@ExtendWith(MockitoExtension.class)
class KafkaControllerTest {

    private KafkaController kafkaController;
    private KafkaProducerService<Object> kafkaProducerService;

    @BeforeEach
    void setUp() {
        kafkaProducerService = mock(KafkaProducerService.class);
        kafkaController = new KafkaController(kafkaProducerService);
        // Manually set the topic value
        //topic = "test-topic";
        //ReflectionTestUtils.setField(kafkaController, "topic", topic);
    }

    @Test
    @Description("Test for sending inventory message successfully")
    void givenHandleInventoryWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        InventoryMessage message = new InventoryMessage();
        message.setInventoryDetails("Sample inventory details");
        message.setInventorySignature(new InventorySignature("signature", "SHA256withECC"));
        message.setVin("");
        // No serialization here, just pass the object
        doNothing().when(kafkaProducerService).sendMessage(Constants.INVENTORY, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendInventory(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.INVENTORY, message);
    }

    @Test
    @Description("Test for sending rollout status message successfully")
    void givenHandleRolloutStatusWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        RolloutStatusMessage message = new RolloutStatusMessage();
        message.setRolloutName("Rollout1");
        message.setStatus("Started");
        message.setStartTime(Instant.now().getEpochSecond());

        doNothing().when(kafkaProducerService).sendMessage(Constants.ROLLOUT_STATUS, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendRolloutStatus(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.ROLLOUT_STATUS, message);
    }

    @Test
    @Description("Test for sending vehicle status message successfully")
    void givenHandleVehicleStatusWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        VehicleStatusMessage message = new VehicleStatusMessage();
        message.setRolloutName("Rollout1");
        message.setVehicleId("1234567890");
        message.setStatus("Operational");
        message.setMessages(new String[]{"Message1", "Message2"});
        message.setTimestamp(Instant.now().getEpochSecond());

        doNothing().when(kafkaProducerService).sendMessage(Constants.VEHICLE_STATUS, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendVehicleStatus(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.VEHICLE_STATUS, message);
    }

    @Test
    @Description("Test for sending RolloutError message successfully")
    void givenHandleRolloutErrorWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        RolloutErrorMessage message = RolloutErrorMessage.builder()
                .rolloutName("Sample_Rollout")
                .status("ERROR")
                .errorCode("E001")
                .errorMessages(Collections.singletonList("Error message"))
                .timeStamp(System.currentTimeMillis())
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.ROLLOUT_ERROR, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendRolloutError(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.ROLLOUT_ERROR, message);
    }

    @Test
    @Description("Test for sending FileUploadStatus message successfully")
    void givenHandleFileUploadStatusWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        FileUploadStatusMessage message = FileUploadStatusMessage.builder().
                status("STORAGE_UPLOAD_SUCCESSFUL").fileType("RSP").fileId(123L).timestamp(1234567L).build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.FILE_UPLOAD, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendFileUploadStatus(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.FILE_UPLOAD, message);
    }

    @Test
    @Description("Test for sending FileUploadError message successfully")
    void givenHandleFileUploadErrorWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        FileUploadErrorMessage message =  FileUploadErrorMessage.builder()
                .status("STORAGE_UPLOAD_ERROR")
                .timestamp(1234567L)
                .fileId(123L)
                .fileType("RSP")
                .fileName("TestFile1")
                .errorMessages(List.of("Error1", "Error2"))
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.FILE_UPLOAD_ERROR, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendFileUploadError(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.FILE_UPLOAD_ERROR, message);
    }

    @Test
    @Description("Test for sending GeneralIdle message successfully")
    void givenHandleGeneralIdleWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        GeneralIdleMessage message = GeneralIdleMessage.builder()
                .vehicleId("1234567890")
                .status("IDLE")
                .messages(Collections.singletonList("Idle message"))
                .timestamp(System.currentTimeMillis())
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.GENERAL_IDLE, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendGeneralIdle(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.GENERAL_IDLE, message);
    }

    @Test
    @Description("Test for sending GeneralError message successfully")
    void givenHandleGeneralErrorWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        GeneralErrorMessage message = GeneralErrorMessage.builder()
                .vehicleId("1234567890")
                .status("ERROR")
                .errorCode(Collections.singletonList("E001"))
                .errorMessages(Collections.singletonList("Error message"))
                .timestamp(System.currentTimeMillis())
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.GENERAL_ERROR, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendGeneralError(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.GENERAL_ERROR, message);
    }

    @Test
    @Description("Test for sending FileDeleteError message successfully")
    void givenHandleFileDeleteErrorWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        FileDeleteErrorMessage message =  FileDeleteErrorMessage.builder()
                .status("STORAGE_DELETION_FAILED")
                .timestamp(1234567L)
                .fileId(123L)
                .fileType("RSP")
                .errorMessages(List.of("Error1", "Error2"))
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.FILE_DELETE_ERROR, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendFileDeleteError(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.FILE_DELETE_ERROR, message);
    }

    @Test
    @Description("Test for sending DdArtifactExpiry message successfully")
    void givenHandleDdArtifactExpiryWhenValidRequestThenCreated() throws JsonProcessingException {
        // Arrange
        FileDeleteErrorMessage message =  FileDeleteErrorMessage.builder()
                .status("EXCLUDED_DUE_TO_SIGNATURE_EXPIRY")
                .timestamp(1234567L)
                .fileId(123L)
                .fileType("ARTIFACT")
                .errorMessages(List.of("Artifact excluded from DD generation due to expired signature"))
                .build();

        doNothing().when(kafkaProducerService).sendMessage(Constants.DD_ARTIFACT_EXPIRY, message);

        // Act
        ResponseEntity<Void> response = kafkaController.sendDdArtifactExpiry(message);

        // Assert
        assertEquals(201, response.getStatusCodeValue());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.DD_ARTIFACT_EXPIRY, message);
    }



   @Test
    void testSendMessage_Success() {
        // Arrange
        String message = "Test Message";
        doNothing().when(kafkaProducerService).sendMessage(Constants.ROLLOUT_STATUS, message);

        // Act
        ResponseEntity<String> response = kafkaController.sendMessage(message);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Message sent: " + message, response.getBody());
        verify(kafkaProducerService, times(1)).sendMessage(Constants.ROLLOUT_STATUS, message);
    }

    @Test
    void testSendMessage_Failure() {
        // Arrange
        String message = "Test Message";
        doThrow(new RuntimeException("Kafka error")).when(kafkaProducerService).sendMessage(Constants.ROLLOUT_STATUS, message);

        // Act & Assert
        try {
            kafkaController.sendMessage(message);
        } catch (RuntimeException e) {
            assertEquals("Kafka error", e.getMessage());
        }
        verify(kafkaProducerService, times(1)).sendMessage(Constants.ROLLOUT_STATUS, message);
    }
}