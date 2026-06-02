package com.stellantis.cosmos.sqs.app.service.listeners;

import com.stellantis.cosmos.sqs.app.model.FileDeleteFailure;
import com.stellantis.cosmos.sqs.app.service.FileProcessingErrorLogsManagement;
import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.ActionType;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.cosmos.models.sqs.ErrorType.PERMANENT_FAILURE;
import static org.cosmos.models.sqs.ErrorType.TRANSIENT_ERROR;

/**
 * Listener for processing file deletion failure events.
 * <p>
 * Extends {@link BatchMessageProcessor} to handle messages of type {@link FileDeleteFailure}.
 * Processes messages by updating the file status through the {@link FileStatusManagerFactory}
 * and running operations in a tenant-aware context.
 */

// @Service
// @Slf4j

//Rohit Salunkhe
@Service
@org.springframework.context.annotation.Profile("!local")
@Slf4j
//Rohit Salunkhe

public class FileDeleteFailureListener extends BatchMessageProcessor<FileDeleteFailure> {

    private static final String ERROR = "ERROR";
    @Value("${cosmos-file-delete-failure-server-sqs.name}")
    private String sqsQueueName;

    private final TenantAware tenantAware;
    private final FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement;
    private final FileStatusManagerFactory fileStatusManagerFactory;
    private final SystemManagement systemManagement;
    private final KafkaMessageService kafkaMessageService;
    private final ActionArtifactRepository actionArtifactRepository;
    private final ActionStatusRepository actionStatusRepository;

    public FileDeleteFailureListener(final TenantAware tenantAware, final FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement,
                                     final FileStatusManagerFactory fileStatusManagerFactory, final SystemManagement systemManagement,
                                     final KafkaMessageService kafkaMessageService, ActionArtifactRepository actionArtifactRepository, ActionStatusRepository actionStatusRepository) {
        this.tenantAware = tenantAware;
        this.fileProcessingErrorLogsManagement = fileProcessingErrorLogsManagement;
        this.fileStatusManagerFactory = fileStatusManagerFactory;
        this.systemManagement = systemManagement;
        this.kafkaMessageService = kafkaMessageService;
        this.actionArtifactRepository = actionArtifactRepository;
        this.actionStatusRepository = actionStatusRepository;
    }

    @Override
    public String getQueueName() {
        return sqsQueueName;
    }

    @Override
    public Class<FileDeleteFailure> getPayloadClass() {
        return FileDeleteFailure.class;
    }

    @Override
    public void process(FileDeleteFailure message, MessageHeaders headers) {
        log.info("Processing file delete failure message: {}", message);
        String tenant = getTenant(message.getTenantId());
        tenantAware.runAsTenant(tenant, processMessage(message));
    }

    public String getTenant(@NotNull Long tenantId) {
        TenantMetaData tenantMetaData = systemManagement.getTenantMetadataNoPermission(tenantId);
        return tenantMetaData.getTenant();
    }

    private TenantAware.TenantRunner<Void> processMessage(FileDeleteFailure message) {
        return () -> {
            log.debug("Processing file deletion failure message: {}", message);
            if (TRANSIENT_ERROR.equals(message.getErrorType())) {
                handleTransientError(message);
            } else if (PERMANENT_FAILURE.equals(message.getErrorType())) {
                handlePermanentFailureStatus(message);
            }
            return null;
        };
    }

    /**
     * Handles the scenario where a permanent failure occurs during file deletion.
     * <p>
     * This method processes the failure by:
     * <ul>
     *   <li>Logging the error details.</li>
     *   <li>Determining the appropriate file status based on the storage type.</li>
     *   <li>Fetching the file name and distinct error messages related to the failure.</li>
     *   <li>Updating the file status in the respective repository.</li>
     *   <li>Retrieving action IDs and associated error codes for the failed file.</li>
     *   <li>Building and sending a Kafka event to notify about the failure.</li>
     * </ul>
     *
     * @param message the {@link FileDeleteFailure} message containing details of the failure.
     */
    private void handlePermanentFailureStatus(FileDeleteFailure message) {
        log.error("Permanent failure occurred for file ID: {}. Error description: {}", message.getFileId(), message.getErrorDescription());
        log.debug("Preparing to send Kafka event for file delete failure status update for file id: {}", message.getFileId());
        FileTransferStatus fileStatus = switch (message.getStorageType()) {
            case S3 -> FileTransferStatus.STORAGE_DELETION_FAILED;
            case CDN -> FileTransferStatus.CDN_DELETION_FAILED;
        };
        String fileName = fileStatusManagerFactory.getInstance(message.getFileType()).getFileName(message.getFileId());
        List<String> errorMessages = fileProcessingErrorLogsManagement.getDistinctFailureMessages(message.getFileId(), message.getFileType());
        fileStatusManagerFactory.getInstance(message.getFileType())
                .updateFileStatus(message.getFileId(), fileStatus.name());
        List<Long> actionIds = actionArtifactRepository.findActionsByArtifactId(message.getFileId())
                .stream()
                .map(JpaAction::getId)
                .distinct().toList();

        List<String> errorCodes = actionStatusRepository.findByActionIdIn(actionIds)
                .stream()
                .map(JpaActionStatus::getErrorCode)
                .distinct().toList();

        FileDeleteErrorMessage fileDeleteErrorMessage = FileDeleteErrorMessage.builder()
                .type(ERROR)
                .fileId(message.getFileId())
                .fileName(fileName)
                .status(fileStatus.name())
                .errorMessages(errorMessages)
                .errorCodes(errorCodes)
                .timestamp(Instant.now().atZone(ZoneId.of("UTC")).toEpochSecond())
                .build();

        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .fileType(message.getFileType().name().toUpperCase())
                .build();
        log.debug("Header to send Kafka event for File Delete Failure status update: {}", header);

        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().
                header(header).
                payload(fileDeleteErrorMessage).
                build();
        log.debug("Payload to send Kafka event for File Delete Failure status update: {}", eventTemplate);

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, Constants.FILE_DELETE_ERROR);
        log.debug("Just sent Kafka event for File Delete Failure status update.");
    }

    private void handleTransientError(FileDeleteFailure message) {
        log.debug("Transient error occurred while file deletion. Persisting error logs.");
        fileProcessingErrorLogsManagement.persistFileProcessingErrorLogs(
                message.getFileType(),
                message.getErrorDescription(),
                message.getFileId(),
                message.getStorageType(),
                message.getRetryCount(),
                ActionType.DELETE);
    }
}
