package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.model.FileDeleteSuccess;
import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import com.stellantis.cosmos.sqs.app.service.listeners.FileDeleteSuccessListener;
import io.qameta.allure.Description;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.service.ArtifactsFileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalService;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileDeleteSuccessListenerTest {

    @Mock
    private FileStatusManagerFactory fileStatusManagerFactory;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private TenantAware tenantAware;

    @Mock
    private TenantMetaData tenantMetaData;

    private FileDeleteSuccessListener fileDeleteSuccessListener;

    private ArtifactsRepository artifactRepository;

    @BeforeEach
    void setUp() {

        fileDeleteSuccessListener = Mockito.spy(
                new FileDeleteSuccessListener(fileStatusManagerFactory, systemManagement, tenantAware, artifactRepository)
        );
        ReflectionTestUtils.setField(fileDeleteSuccessListener, "sqsQueueName", "cosmos-dev-file-delete-success-sqs");
    }

    @Description("Given a valid message and headers, when process is invoked, then the message should be processed successfully.")
    @Test
    void givenValidMessageAndHeaders_whenProcessInvoked_thenMessageProcessedSuccessfully() {
        // Arrange
        FileDeleteSuccess message = new FileDeleteSuccess(1L, FileType.ARTIFACT, FileTransferStatus.STORAGE_DELETE_SUCCESSFUL, 1L);
        MessageHeaders headers = mock(MessageHeaders.class);

        // Mocking behavior of systemManagement
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);
        when(tenantMetaData.getTenant()).thenReturn("default");

        // Act
        String tenant = fileDeleteSuccessListener.getTenant(1L);

        fileDeleteSuccessListener.process(message, headers);
        // Assert
        assertEquals("default", tenant);

        // Assert
        verify(tenantAware).runAsTenant(eq("default"), any(TenantAware.TenantRunner.class));

    }


    @Description("Given the listener, when getPayloadClass is invoked, then the correct payload class should be returned.")
    @Test
    void givenListener_whenGetPayloadClassInvoked_thenCorrectPayloadClassReturned() {
        // Act
        Class<FileDeleteSuccess> payloadClass = fileDeleteSuccessListener.getPayloadClass();

        // Assert
        assertEquals(FileDeleteSuccess.class, payloadClass);
    }

    @Test
    @Description("Given the listener, when getQueueName is invoked, then the correct queue name should be returned.")
    void givenListener_whenGetQueueNameInvoked_thenCorrectQueueNameReturned() {
        // Act
        String queueName = fileDeleteSuccessListener.getQueueName();

        // Assert
        assertEquals("cosmos-dev-file-delete-success-sqs", queueName);
    }

    @Test
    @Description("Test that the file status is updated when a delete success message is processed.")
    void givenDeleteSuccessMessageWhenProcessDeleteMessageThenUpdateFileStatus() {
        try (MockedStatic<FileRemovalServiceFactory> mockedFactory = Mockito.mockStatic(FileRemovalServiceFactory.class)) {
            FileRemovalService mockService = Mockito.mock(ArtifactsFileRemovalService.class);
            mockedFactory.when(() -> FileRemovalServiceFactory.getInstance(FileType.ARTIFACT)).thenReturn(mockService);

            // Arrange
            ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
            when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);

            JpaArtifacts artifact = mock(JpaArtifacts.class);
            when(artifact.getArtifactStatus()).thenReturn(FileTransferStatus.DELETING_FROM_CDN.name());
            ArtifactsRepository artifactsRepositoryMock = mock(ArtifactsRepository.class);
            when(artifactsRepositoryMock.getArtifactsById(1L)).thenReturn(Optional.of(artifact));
            ReflectionTestUtils.setField(fileDeleteSuccessListener, "artifactsRepository", artifactsRepositoryMock);

            FileDeleteSuccess message = new FileDeleteSuccess(1L, FileType.ARTIFACT, FileTransferStatus.CDN_DELETE_SUCCESSFUL, 1L);

            // Act
            ReflectionTestUtils.invokeMethod(fileDeleteSuccessListener, "processDeleteMessage", message);

            // Assert
            verify(artifactFileStatusManager).updateFileStatus(1L, FileTransferStatus.CDN_DELETE_SUCCESSFUL.name());
            verify(mockService).removeFileFromStorage(1L);
        }
    }

    @Test
    @Description("Should update file status when transition from DELETING_FROM_STORAGE to STORAGE_DELETE_SUCCESSFUL")
    void givenActiveStorageDeleteStatusWhenDeleteSuccessfulThenUpdated() {
        Artifacts artifact = mock(Artifacts.class);
        when(artifact.getArtifactStatus()).thenReturn(FileTransferStatus.DELETING_FROM_STORAGE.name());
        Optional<Artifacts> artifactsOpt = Optional.of(artifact);

        FileDeleteSuccess message = new FileDeleteSuccess(1L, FileType.ARTIFACT, FileTransferStatus.STORAGE_DELETE_SUCCESSFUL, 1L);

        boolean result = ReflectionTestUtils.invokeMethod(FileDeleteSuccessListener.class, "isShouldUpdate", message, artifactsOpt);

        assertTrue(result);
    }

    @Test
    @Description("Should update file status when transition from DELETING_FROM_CDN to CDN_DELETE_SUCCESSFUL")
    void givenActiveCDNDeleteStatusWhenDeleteSuccessfulThenUpdated() {
        Artifacts artifact = mock(Artifacts.class);
        when(artifact.getArtifactStatus()).thenReturn(FileTransferStatus.DELETING_FROM_CDN.name());
        Optional<Artifacts> artifactsOpt = Optional.of(artifact);

        FileDeleteSuccess message = new FileDeleteSuccess(2L, FileType.ARTIFACT, FileTransferStatus.CDN_DELETE_SUCCESSFUL, 2L);

        boolean result = ReflectionTestUtils.invokeMethod(FileDeleteSuccessListener.class, "isShouldUpdate", message, artifactsOpt);

        assertTrue(result);
    }

    @Test
    @Description("Should not update file status when transition from CDN_DELETE_SUCCESSFUL to DELETING_FROM_CDN")
    void givenDeletingFromCdnStatusWhenDeleteSuccessfulThenNotUpdate() {
        Artifacts artifact = mock(Artifacts.class);
        when(artifact.getArtifactStatus()).thenReturn(FileTransferStatus.CDN_DELETE_SUCCESSFUL.name());
        Optional<Artifacts> artifactsOpt = Optional.of(artifact);

        FileDeleteSuccess message = new FileDeleteSuccess(3L, FileType.ARTIFACT, FileTransferStatus.DELETING_FROM_CDN, 3L);

        boolean result = ReflectionTestUtils.invokeMethod(FileDeleteSuccessListener.class, "isShouldUpdate", message, artifactsOpt);

        assertFalse(result);
    }

    @Test
    @Description("Should not update file status when transition from STORAGE_DELETE_SUCCESSFUL to DELETING_FROM_STORAGE")
    void givenDeletingFromStorageStatusWhenDeleteSuccessfulThenNotUpdate() {
        Artifacts artifact = mock(Artifacts.class);
        when(artifact.getArtifactStatus()).thenReturn(FileTransferStatus.STORAGE_DELETE_SUCCESSFUL.name());
        Optional<Artifacts> artifactsOpt = Optional.of(artifact);

        FileDeleteSuccess message = new FileDeleteSuccess(4L, FileType.ARTIFACT, FileTransferStatus.DELETING_FROM_STORAGE, 4L);

        boolean result = ReflectionTestUtils.invokeMethod(FileDeleteSuccessListener.class, "isShouldUpdate", message, artifactsOpt);

        assertFalse(result);
    }
}
