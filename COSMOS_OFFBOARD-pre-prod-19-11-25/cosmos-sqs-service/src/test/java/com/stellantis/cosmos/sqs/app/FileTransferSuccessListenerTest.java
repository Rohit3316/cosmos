package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.model.FileTransferSuccess;
import com.stellantis.cosmos.sqs.app.service.ArtifactFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.EspFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.RspFileStatusManager;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import com.stellantis.cosmos.sqs.app.service.listeners.FileTransferSuccessListener;
import io.qameta.allure.Description;
import org.cosmos.models.sqs.FileType;
import org.cosmos.s3.ChecksumCalculator;
import org.cosmos.s3.S3Repository;
import org.eclipse.hawkbit.api.ArtifactUrlHandlerProperties;
import org.eclipse.hawkbit.feignclient.kafka.KafkaClient;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.EspEcuRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RspRolloutRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.MessageHeaders;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileTransferSuccessListenerTest {

    @Mock
    private FileStatusManagerFactory fileStatusManagerFactory;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private TenantAware tenantAware;

    @Mock
    private TenantMetaData tenantMetaData;

    @Mock
    private KafkaClient kafkaClient;

    @InjectMocks
    private KafkaMessageService kafkaMessageService;

    private FileTransferSuccessListener fileTransferSuccessListener;

    @Mock
    private ArtifactsRepository artifactsRepository;

    @Mock
    private S3Repository s3Repository;

    @Mock
    private SupportPackageUrlHandlerProperties supportPackageUrlHandlerProperties;

    @Mock
    private ArtifactUrlHandlerProperties artifactUrlHandlerProperties;

    @Mock
    private RspRepository rspRepository;

    @Mock
    private EspRepository espRepository;

    @Mock
    private ChecksumCalculator checksumCalculator;

    @Mock
    private SupportPackageManagement supportPackageManagement;

    @InjectMocks
    private ArtifactFileStatusManager artifactFileStatusManager;

    @InjectMocks
    private EspFileStatusManager espFileStatusManager;

    @InjectMocks
    private RspFileStatusManager rspFileStatusManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        fileTransferSuccessListener = Mockito.spy(
                new FileTransferSuccessListener(fileStatusManagerFactory, systemManagement, tenantAware, kafkaMessageService)
        );
        ReflectionTestUtils.setField(fileTransferSuccessListener, "sqsQueueName", "cosmos-dev-file-transfer-success-server-sqs");

        // Manually inject the mock artifactsRepository into artifactFileStatusManager
        ReflectionTestUtils.setField(artifactFileStatusManager, "artifactsRepository", artifactsRepository);
        ReflectionTestUtils.setField(espFileStatusManager, "espRepository", espRepository);
        ReflectionTestUtils.setField(rspFileStatusManager, "rspRepository", rspRepository);
        ReflectionTestUtils.setField(artifactFileStatusManager, "checksumCalculator", checksumCalculator);
        ReflectionTestUtils.setField(espFileStatusManager, "checksumCalculator", checksumCalculator);
        ReflectionTestUtils.setField(rspFileStatusManager, "checksumCalculator", checksumCalculator);
        ReflectionTestUtils.setField(artifactFileStatusManager, "s3Repository", s3Repository);
        ReflectionTestUtils.setField(espFileStatusManager, "s3Repository", s3Repository);
        ReflectionTestUtils.setField(rspFileStatusManager, "s3Repository", s3Repository);
        ReflectionTestUtils.setField(artifactFileStatusManager, "artifactUrlHandlerProperties", artifactUrlHandlerProperties);
        ReflectionTestUtils.setField(espFileStatusManager, "supportPackageUrlHandlerProperties", supportPackageUrlHandlerProperties);
        ReflectionTestUtils.setField(rspFileStatusManager, "supportPackageUrlHandlerProperties", supportPackageUrlHandlerProperties);
        ReflectionTestUtils.setField(kafkaMessageService, "kafkaClient", kafkaClient);
    }

    @Description("Given a valid message and headers, when process is invoked, then the message should be processed successfully.")
    @Test
    void givenValidMessageAndHeaders_whenProcessInvoked_thenMessageProcessedSuccessfully() {
        // Arrange
        FileTransferSuccess message = new FileTransferSuccess(FileType.ARTIFACT, "checksum123", 1L, "SUCCESS", 1L);
        MessageHeaders headers = mock(MessageHeaders.class);

        // Mocking behavior of systemManagement
        when(systemManagement.getTenantMetadataNoPermission(1L)).thenReturn(tenantMetaData);
        when(tenantMetaData.getTenant()).thenReturn("default");

        // Act
        String tenant = fileTransferSuccessListener.getTenant(1L);

        fileTransferSuccessListener.process(message, headers);
        // Assert
        assertEquals("default", tenant);

        // Assert
        verify(tenantAware).runAsTenant(eq("default"), any(TenantAware.TenantRunner.class));

    }

    @Description("Calling sendFileUploadStatusToDOCG should send the file upload status message to DOCG.")
    @Test
    void givenFileUploadStatusMessageWhenSendFileUploadStatusToDOCGThenMessageIsSent() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // Arrange
        ArtifactFileStatusManager artifactFileStatusManager = mock(ArtifactFileStatusManager.class);
        FileTransferSuccess message = new FileTransferSuccess(FileType.ARTIFACT, "checksum123", 1L, "SUCCESS", 1L);

        when(fileStatusManagerFactory.getInstance(FileType.ARTIFACT)).thenReturn(artifactFileStatusManager);
        when(artifactFileStatusManager.getFileName(1L)).thenReturn("fileName");
        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        // Act
        Method handleFileUploadSuccessStatusMethod = FileTransferSuccessListener.class.getDeclaredMethod("sendFileUploadStatusToDOCG", FileTransferSuccess.class);
        handleFileUploadSuccessStatusMethod.setAccessible(true);
        handleFileUploadSuccessStatusMethod.invoke(fileTransferSuccessListener, message);

        // Assert
        verify(artifactFileStatusManager).updateFileStatus(1L, "SUCCESS");
        verify(artifactFileStatusManager).getFileName(1L);

        ArgumentCaptor<KafkaEventTemplate> eventCaptor = ArgumentCaptor.forClass(KafkaEventTemplate.class);
        verify(kafkaClient, times(1)).publishEvent(eventCaptor.capture(), eq("fileupload"));
        KafkaEventTemplate sentEvent = eventCaptor.getValue();
        assertEquals("ARTIFACT", sentEvent.getHeader().getFileType());
    }


    @Description("Given the listener, when getPayloadClass is invoked, then the correct payload class should be returned.")
    @Test
    void givenListener_whenGetPayloadClassInvoked_thenCorrectPayloadClassReturned() {
        // Act
        Class<FileTransferSuccess> payloadClass = fileTransferSuccessListener.getPayloadClass();

        // Assert
        assertEquals(FileTransferSuccess.class, payloadClass);
    }

    @Test
    @Description("Given the listener, when getQueueName is invoked, then the correct queue name should be returned.")
    void givenListener_whenGetQueueNameInvoked_thenCorrectQueueNameReturned() {
        // Act
        String queueName = fileTransferSuccessListener.getQueueName();

        // Assert
        assertEquals("cosmos-dev-file-transfer-success-server-sqs", queueName);
    }

    @Test
    @Description("Given a file ID, status, and checksum, when updateFileStatus is invoked, then the file status should be updated successfully in the respective repositories.")
    void givenFileIdAndStatusWhenUpdateFileStatusThenFileStatusUpdatedSuccessfully() {
        Long fileId = 1L;
        String status = "STORAGE_UPLOAD_SUCCESSFUL";
        String base64EncodedMD5 = "1B2M2Y8AsgTpgAmY7PhCfg==";
        String hexString = "d41d8cd98f00b204e9800998ecf8427e";

        JpaArtifacts mockArtifact = new JpaArtifacts();
        mockArtifact.setSha256Hash(hexString);
        mockArtifact.setFileName("test");
        when(artifactsRepository.findById(fileId)).thenReturn(Optional.of(mockArtifact));
        when(checksumCalculator.convertBase64ToHex(base64EncodedMD5)).thenReturn(hexString);
        when(artifactsRepository.save(mockArtifact)).thenReturn(mockArtifact);
        when(artifactUrlHandlerProperties.getS3()).thenReturn(new ArtifactUrlHandlerProperties.S3());
        artifactFileStatusManager.updateFileStatus(fileId, status, base64EncodedMD5, "default");

        verify(artifactsRepository).findById(fileId);
        verify(checksumCalculator).convertBase64ToHex(base64EncodedMD5);
        verify(artifactsRepository).save(mockArtifact);

        JpaEsp mockEsp = new JpaEsp();
        mockEsp.setSha256Hash(hexString);
        mockEsp.setFileName("test");
        when(espRepository.findById(fileId)).thenReturn(Optional.of(mockEsp));
        when(espRepository.save(mockEsp)).thenReturn(mockEsp);


        when(supportPackageUrlHandlerProperties.getEsp()).thenReturn(new SupportPackageUrlHandlerProperties.Esp());
        espFileStatusManager.updateFileStatus(fileId, status, base64EncodedMD5, "default");

        verify(espRepository).findById(fileId);
        verify(checksumCalculator, times(2)).convertBase64ToHex(base64EncodedMD5);
        verify(espRepository).save(mockEsp);

        JpaRsp mockRsp = new JpaRsp();
        mockRsp.setSha256Hash(hexString);
        mockRsp.setFileName("test");
        when(rspRepository.findById(fileId)).thenReturn(Optional.of(mockRsp));
        when(rspRepository.save(mockRsp)).thenReturn(mockRsp);

        when(supportPackageUrlHandlerProperties.getRsp()).thenReturn(new SupportPackageUrlHandlerProperties.Rsp());
        rspFileStatusManager.updateFileStatus(fileId, status, base64EncodedMD5, "default");

        verify(rspRepository).findById(fileId);
        verify(checksumCalculator, times(3)).convertBase64ToHex(base64EncodedMD5);
        verify(rspRepository).save(mockRsp);

        assertEquals(status, mockArtifact.getFileStatus().toString());
        assertEquals(hexString, mockArtifact.getMd5Hash());
    }

}
