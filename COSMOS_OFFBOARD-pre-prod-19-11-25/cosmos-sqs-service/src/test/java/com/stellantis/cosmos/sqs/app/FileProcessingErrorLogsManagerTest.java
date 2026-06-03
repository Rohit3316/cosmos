package com.stellantis.cosmos.sqs.app;

import com.stellantis.cosmos.sqs.app.service.FileProcessingErrorLogsManager;
import io.qameta.allure.Description;
import org.cosmos.models.sqs.ActionType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;
import org.eclipse.hawkbit.repository.jpa.FileProcessingErrorLogRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaFileProcessingErrorLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for {@link FileProcessingErrorLogsManager}
 */
class FileProcessingErrorLogsManagerTest {
    @Mock
    private FileProcessingErrorLogRepository fileProcessingErrorLogRepository;

    @InjectMocks
    private FileProcessingErrorLogsManager fileProcessingErrorLogsManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Description("Given file processing error logs, when persisted, then they should be saved correctly.")
    @Test
    void givenFileProcessingErrorLogs_whenPersisted_thenSavedCorrectly() {
        FileType fileType = FileType.ARTIFACT;
        String logMessage = "Error message";
        Long logTypeId = 1L;
        StorageType storageType = StorageType.CDN;
        Integer retryCount = 3;

        fileProcessingErrorLogsManager.persistFileProcessingErrorLogs(fileType, logMessage, logTypeId, storageType, retryCount, ActionType.UPLOAD);

        ArgumentCaptor<JpaFileProcessingErrorLog> captor = ArgumentCaptor.forClass(JpaFileProcessingErrorLog.class);
        verify(fileProcessingErrorLogRepository, times(1)).save(captor.capture());

        JpaFileProcessingErrorLog savedLog = captor.getValue();
        assertEquals(fileType, savedLog.getFileType());
        assertEquals(logMessage, savedLog.getLogMessage());
        assertEquals(logTypeId, savedLog.getLogTypeId());
        assertEquals(storageType, savedLog.getStorageType());
        assertEquals(retryCount, savedLog.getRetryCount());
        assertEquals(ActionType.UPLOAD, savedLog.getAction());
    }

@Description("Given file ID and file type, when retrieving distinct failure messages, then they should be returned correctly.")
@Test
void givenFileIdAndFileType_whenRetrievingDistinctFailureMessages_thenReturnedCorrectly() {
    Long fileId = 1L;
    FileType fileType = FileType.ARTIFACT;
    List<JpaFileProcessingErrorLog> logs = Arrays.asList(
            JpaFileProcessingErrorLog.builder().logMessage("Error 1").build(),
            JpaFileProcessingErrorLog.builder().logMessage("Error 2").build(),
            JpaFileProcessingErrorLog.builder().logMessage("Error 1").build()
    );

    when(fileProcessingErrorLogRepository.findLogMessageByLogTypeIdAndFileType(fileId, fileType)).thenReturn(logs);
    List<String> distinctMessages = fileProcessingErrorLogsManager.getDistinctFailureMessages(fileId, fileType);
    assertEquals(2, distinctMessages.size());
    assertEquals("Error 1", distinctMessages.get(0));
    assertEquals("Error 2", distinctMessages.get(1));
}
}