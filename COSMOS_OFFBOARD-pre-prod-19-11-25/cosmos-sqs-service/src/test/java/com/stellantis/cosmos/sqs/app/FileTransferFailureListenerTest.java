package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.model.FileTransferFailure;
import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.FileProcessingErrorLogsManagement;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import com.stellantis.cosmos.sqs.app.service.listeners.FileTransferFailureListener;
import org.cosmos.models.kafka.FileUploadErrorMessage;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FileTransferFailureListenerTest {

    @Mock
    private FileStatusManagerFactory fileStatusManagerFactory;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private TenantAware tenantAware;

    @Mock
    private KafkaClient kafkaClient;

    @Mock
    private TenantMetaData tenantMetaData;

    @InjectMocks
    private KafkaMessageService kafkaMessageService;

    @Mock
    private FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement;

    private FileTransferFailureListener fileTransferFailureListener;

    @Mock
    private ActionArtifactRepository actionArtifactRepository;

    @Mock
    private ActionStatusRepository actionStatusRepository;

    @BeforeEach
    void setUp() {

        fileTransferFailureListener = Mockito.spy(
                new FileTransferFailureListener(fileProcessingErrorLogsManagement, fileStatusManagerFactory, systemManagement, tenantAware, kafkaMessageService, actionArtifactRepository, actionStatusRepository)
        );
        ReflectionTestUtils.setField(fileTransferFailureListener, "sqsQueueName", "cosmos-dev-file-transfer-failure-server-sqs");
    }

    @Description("Given a valid message and headers, when process is invoked, then the message should be processed successfully.")
    @Test
    void givenValidMessageAndHeaders_whenProcessInvoked_thenMessageProcessedSuccessfully() {
        // Arrange
        FileTransferFailure message = new FileTransferFailure(1L, FileType.ARTIFACT, "PERMANENT_ERROR", StorageType.S3, 1L, "Error occurred", 1);
        MessageHeaders headers = mock(MessageHeaders.class);

        // Mocking behavior of systemManagement
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);
        when(tenantMetaData.getTenant()).thenReturn("default");

        // Act
        String tenant = fileTransferFailureListener.getTenant(1L);

        fileTransferFailureListener.process(message, headers);
        // Assert
        assertEquals("default", tenant);

        // Assert
        verify(tenantAware).runAsTenant(eq("default"), any(TenantAware.TenantRunner.class));

    }

    @Description("Given the listener, when getPayloadClass is invoked, then the correct payload class should be returned.")
    @Test
    void givenListener_whenGetPayloadClassInvoked_thenCorrectPayloadClassReturned() {
        // Act
        Class<FileTransferFailure> payloadClass = fileTransferFailureListener.getPayloadClass();

        // Assert
        assertEquals(FileTransferFailure.class, payloadClass);
    }

    @Test
    @Description("Given the listener, when getQueueName is invoked, then the correct queue name should be returned.")
    void givenListener_whenGetQueueNameInvoked_thenCorrectQueueNameReturned() {
        // Act
        String queueName = fileTransferFailureListener.getQueueName();

        // Assert
        assertEquals("cosmos-dev-file-transfer-failure-server-sqs", queueName);
    }

    @Test
    @Description("Test file status is updated and Kafka message is sent on permanent failure for S3")
    void givePermanentFailureMessageWhenTestHandlePermanentFailureStatusForS3ThenKafkaMessageSent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
        // Arrange
        FileTransferFailure message = new FileTransferFailure(1L, FileType.ARTIFACT, "PERMANENT_ERROR", StorageType.S3, 1L, "Error occurred", 1);
        when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);
        when(artifactFileStatusManager.getFileName(1L)).thenReturn("fileName");
        when(fileProcessingErrorLogsManagement.getDistinctFailureMessages(1L, FileType.ARTIFACT)).thenReturn(List.of("Error occurred"));
        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        // Act
        Method handlePermanentFailureStatusMethod = FileTransferFailureListener.class.getDeclaredMethod("handlePermanentFailureStatus", FileTransferFailure.class);
        handlePermanentFailureStatusMethod.setAccessible(true);
        handlePermanentFailureStatusMethod.invoke(fileTransferFailureListener, message);

        // Assert
        verify(artifactFileStatusManager).updateFileStatus(1L, "STORAGE_UPLOAD_ERROR");
        verify(artifactFileStatusManager).getFileName(1L);
        verify(fileProcessingErrorLogsManagement).getDistinctFailureMessages(1L, FileType.ARTIFACT);
        ArgumentCaptor<KafkaEventTemplate> eventCaptor = ArgumentCaptor.forClass(KafkaEventTemplate.class);
        verify(kafkaClient, times(1)).publishEvent(eventCaptor.capture(), eq("fileuploaderror"));
        KafkaEventTemplate sentEvent = eventCaptor.getValue();
        assertEquals("ARTIFACT", sentEvent.getHeader().getFileType());
    }


    @Test
    @Description("Test file status is updated and Kafka message is sent on permanent failure for CDN")
    void givePermanentFailureMessageWhenTestHandlePermanentFailureStatusForCDNThenKafkaMessageSent() throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
        // Arrange
        FileTransferFailure message = new FileTransferFailure(1L, FileType.ARTIFACT, "PERMANENT_ERROR", StorageType.CDN, 1L, "Error occurred", 1);
        when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);
        when(artifactFileStatusManager.getFileName(1L)).thenReturn("fileName");
        when(fileProcessingErrorLogsManagement.getDistinctFailureMessages(1L, FileType.ARTIFACT)).thenReturn(List.of("Error occurred"));
        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        // Act
        Method handlePermanentFailureStatusMethod = FileTransferFailureListener.class.getDeclaredMethod("handlePermanentFailureStatus", FileTransferFailure.class);
        handlePermanentFailureStatusMethod.setAccessible(true);
        handlePermanentFailureStatusMethod.invoke(fileTransferFailureListener, message);

        // Assert
        verify(artifactFileStatusManager).updateFileStatus(1L, "CDN_UPLOAD_ERROR");
        verify(artifactFileStatusManager).getFileName(1L);
        verify(fileProcessingErrorLogsManagement).getDistinctFailureMessages(1L, FileType.ARTIFACT);
        verify(kafkaClient).publishEvent(any(KafkaEventTemplate.class), eq("fileuploaderror"));
    }

}
