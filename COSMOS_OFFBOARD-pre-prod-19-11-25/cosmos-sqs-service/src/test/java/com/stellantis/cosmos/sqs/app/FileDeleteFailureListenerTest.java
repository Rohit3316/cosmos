package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.model.FileDeleteFailure;
import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.FileProcessingErrorLogsManagement;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import com.stellantis.cosmos.sqs.app.service.listeners.FileDeleteFailureListener;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.ActionType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;
import org.eclipse.hawkbit.feignclient.kafka.KafkaClient;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.annotation.Description;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.cosmos.models.sqs.ErrorType.PERMANENT_FAILURE;
import static org.cosmos.models.sqs.ErrorType.TRANSIENT_ERROR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class FileDeleteFailureListenerTest {

    @Mock
    private FileStatusManagerFactory fileStatusManagerFactory;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private TenantAware tenantAware;

    @Mock
    private KafkaClient kafkaClient;

    @InjectMocks
    private KafkaMessageService kafkaMessageService;

    @Mock
    private TenantMetaData tenantMetaData;
    @Mock
    private FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement;

    private FileDeleteFailureListener fileDeleteFailureListener;

    @Mock
    private ActionArtifactRepository actionArtifactRepository;

    @Mock
    private ActionStatusRepository actionStatusRepository;

    @BeforeEach
    void setUp() {

        fileDeleteFailureListener = Mockito.spy(
                new FileDeleteFailureListener(tenantAware, fileProcessingErrorLogsManagement, fileStatusManagerFactory, systemManagement, kafkaMessageService, actionArtifactRepository, actionStatusRepository)
        );
        ReflectionTestUtils.setField(fileDeleteFailureListener, "sqsQueueName", "cosmos-dev-file-delete-failure-sqs");


    }

    @Description("Given a valid message and headers, when process is invoked, then the message should be processed successfully.")
    @Test
    void givenValidMessageAndHeaders_whenProcessInvoked_thenMessageProcessedSuccessfully() {
        // Arrange
        FileDeleteFailure message = new FileDeleteFailure(1L, 1L, FileType.ARTIFACT, PERMANENT_FAILURE, "Error Occurred", StorageType.S3, 1);
        MessageHeaders headers = mock(MessageHeaders.class);

        // Mocking behavior of systemManagement
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);
        when(tenantMetaData.getTenant()).thenReturn("default");

        // Act
        String tenant = fileDeleteFailureListener.getTenant(1L);

        fileDeleteFailureListener.process(message, headers);
        // Assert
        assertEquals("default", tenant);

        // Assert
        verify(tenantAware).runAsTenant(eq("default"), any(TenantAware.TenantRunner.class));

    }

    @Description("Given the listener, when getPayloadClass is invoked, then the correct payload class should be returned.")
    @Test
    void givenListener_whenGetPayloadClassInvoked_thenCorrectPayloadClassReturned() {
        // Act
        Class<FileDeleteFailure> payloadClass = fileDeleteFailureListener.getPayloadClass();

        // Assert
        assertEquals(FileDeleteFailure.class, payloadClass);
    }

    @Test
    @Description("Given the listener, when getQueueName is invoked, then the correct queue name should be returned.")
    void givenListener_whenGetQueueNameInvoked_thenCorrectQueueNameReturned() {
        // Act
        String queueName = fileDeleteFailureListener.getQueueName();

        // Assert
        assertEquals("cosmos-dev-file-delete-failure-sqs", queueName);
    }

    @Test
    @Description("Test file status is updated and Kafka message is sent on permanent failure for S3")
    void givePermanentFailureMessageWhenTestHandlePermanentFailureStatusForS3ThenKafkaMessageSent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
        // Arrange
        FileDeleteFailure message = new FileDeleteFailure(1L, 1L, FileType.ARTIFACT, PERMANENT_FAILURE, "Error Occurred", StorageType.S3, 1);
        when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);
        when(fileProcessingErrorLogsManagement.getDistinctFailureMessages(1L, FileType.ARTIFACT)).thenReturn(List.of("Error occurred"));
        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        // Act
        Method handlePermanentFailureStatusMethod = FileDeleteFailureListener.class.getDeclaredMethod("handlePermanentFailureStatus", FileDeleteFailure.class);
        handlePermanentFailureStatusMethod.setAccessible(true);
        handlePermanentFailureStatusMethod.invoke(fileDeleteFailureListener, message);

        // Assert
        verify(artifactFileStatusManager).updateFileStatus(1L, FileTransferStatus.STORAGE_DELETION_FAILED.name());
        verify(fileProcessingErrorLogsManagement).getDistinctFailureMessages(1L, FileType.ARTIFACT);
        verify(kafkaClient).publishEvent(any(KafkaEventTemplate.class), eq("filedeleteError"));
    }


    @Test
    @Description("Test file status is updated and Kafka message is sent on permanent failure for CDN")
    void givePermanentFailureMessageWhenTestHandlePermanentFailureStatusForCDNThenKafkaMessageSent() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
        // Arrange
        FileDeleteFailure message = new FileDeleteFailure(1L, 1L, FileType.ARTIFACT, PERMANENT_FAILURE, "Error Occurred", StorageType.CDN, 1);
        when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);
        when(fileProcessingErrorLogsManagement.getDistinctFailureMessages(1L, FileType.ARTIFACT)).thenReturn(List.of("Error occurred"));
        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        // Act
        Method handlePermanentFailureStatusMethod = FileDeleteFailureListener.class.getDeclaredMethod("handlePermanentFailureStatus", FileDeleteFailure.class);
        handlePermanentFailureStatusMethod.setAccessible(true);
        handlePermanentFailureStatusMethod.invoke(fileDeleteFailureListener, message);

        // Assert
        verify(artifactFileStatusManager).updateFileStatus(1L, FileTransferStatus.CDN_DELETION_FAILED.name());
        verify(fileProcessingErrorLogsManagement).getDistinctFailureMessages(1L, FileType.ARTIFACT);
        ArgumentCaptor<KafkaEventTemplate> eventCaptor = ArgumentCaptor.forClass(KafkaEventTemplate.class);
        verify(kafkaClient, times(1)).publishEvent(eventCaptor.capture(), eq("filedeleteError"));
        KafkaEventTemplate sentEvent = eventCaptor.getValue();
        assertEquals("ARTIFACT", sentEvent.getHeader().getFileType());
    }

    @Test
    @Description("Test transient error to store in error log")
    void giveTransientFailureMessageWhenTestProcessMessageThenStoreInLog() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        FileDeleteFailure message = new FileDeleteFailure(
                1L, 1L, FileType.ARTIFACT,
                TRANSIENT_ERROR,
                "Error Occurred", StorageType.S3, 1
        );
        Method handleTransientError = FileDeleteFailureListener.class.getDeclaredMethod("handleTransientError", FileDeleteFailure.class);
        handleTransientError.setAccessible(true);
        handleTransientError.invoke(fileDeleteFailureListener, message);
        // Verify the mock call
        verify(fileProcessingErrorLogsManagement, times(1))
                .persistFileProcessingErrorLogs(any(), any(), any(), any(), any(), any());
        verify(fileProcessingErrorLogsManagement).persistFileProcessingErrorLogs(message.getFileType(), message.getErrorDescription(),
                message.getFileId(), message.getStorageType(), message.getRetryCount(), ActionType.DELETE);
    }
}

