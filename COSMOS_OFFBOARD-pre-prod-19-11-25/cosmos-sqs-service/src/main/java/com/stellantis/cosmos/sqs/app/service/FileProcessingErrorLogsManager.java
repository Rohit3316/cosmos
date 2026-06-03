package com.stellantis.cosmos.sqs.app.service;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sqs.ActionType;
import org.cosmos.models.sqs.FileType;
import org.cosmos.models.sqs.StorageType;
import org.eclipse.hawkbit.repository.jpa.FileProcessingErrorLogRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaFileProcessingErrorLog;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing file processing error logs.
 */
@Slf4j
@Service
public class FileProcessingErrorLogsManager implements FileProcessingErrorLogsManagement{

    private final FileProcessingErrorLogRepository fileProcessingErrorLogRepository;

    public FileProcessingErrorLogsManager(FileProcessingErrorLogRepository fileProcessingErrorLogRepository){
        this.fileProcessingErrorLogRepository = fileProcessingErrorLogRepository;
    }

    @Override
    public void persistFileProcessingErrorLogs(FileType fileType, String logMessage, Long logTypeId, StorageType storageType, Integer retryCount, ActionType action) {
        log.debug("Persisting error log: fileType={}, logMessage={}, logTypeId={}, storageType={}, retryCount={}, action={}",
                fileType, logMessage, logTypeId, storageType, retryCount, action);
        JpaFileProcessingErrorLog errorLog = JpaFileProcessingErrorLog.builder()
                .fileType(fileType).logMessage(logMessage)
                .logTypeId(logTypeId).storageType(storageType)
                .retryCount(retryCount).action(action)
                .build();

        fileProcessingErrorLogRepository.save(errorLog);
        log.debug("Error log persisted successfully: {}", errorLog);
    }

    @Override
    public List<String> getDistinctFailureMessages(Long fileId, FileType fileType) {
        return fileProcessingErrorLogRepository.findLogMessageByLogTypeIdAndFileType(fileId, fileType).stream()
                .map(JpaFileProcessingErrorLog::getLogMessage)
                .distinct()
                .toList();
    }

}
