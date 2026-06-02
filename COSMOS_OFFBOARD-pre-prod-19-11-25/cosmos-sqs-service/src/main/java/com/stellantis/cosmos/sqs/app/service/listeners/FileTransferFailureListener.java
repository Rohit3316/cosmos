package com.stellantis.cosmos.sqs.app.service.listeners;

import com.stellantis.cosmos.sqs.app.model.FileTransferFailure;
import com.stellantis.cosmos.sqs.app.service.FileProcessingErrorLogsManagement;
import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.kafka.FileUploadErrorMessage;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.ActionType;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * Listener for processing file transfer failure events.
 * <p>
 * Extends {@link BatchMessageProcessor} to handle messages of type {@link FileTransferFailure}.
 * Processes messages by updating the file status through the {@link FileStatusManagerFactory}
 * and running operations in a tenant-aware context.
 */
@Service
@Slf4j
public class FileTransferFailureListener extends BatchMessageProcessor<FileTransferFailure> {
    private static final String ERROR = "ERROR";
    @Value("${cosmos-file-transfer-failure-server-sqs.name}")
    private String sqsQueueName;
    private final FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement;
    private final FileStatusManagerFactory fileStatusManagerFactory;
    private final SystemManagement systemManagement;
    private final TenantAware tenantAware;
    private final KafkaMessageService kafkaMessageService;
    private static final String TRANSIENT_ERROR_STATUS = "TRANSIENT_FAILURE";
    private static final String PERMANENT_FAILURE_STATUS = "PERMANENT_ERROR";
    private final ActionArtifactRepository actionArtifactRepository;
    private final ActionStatusRepository actionStatusRepository;

    public FileTransferFailureListener(final FileProcessingErrorLogsManagement fileProcessingErrorLogsManagement, final FileStatusManagerFactory fileStatusManagerFactory,
                                       final SystemManagement systemManagement, final TenantAware tenantAware, final KafkaMessageService kafkaMessageService, ActionArtifactRepository actionArtifactRepository, ActionStatusRepository actionStatusRepository) {
        this.fileProcessingErrorLogsManagement = fileProcessingErrorLogsManagement;
        this.fileStatusManagerFactory = fileStatusManagerFactory;
        this.systemManagement = systemManagement;
        this.tenantAware = tenantAware;
        this.kafkaMessageService = kafkaMessageService;
        this.actionArtifactRepository = actionArtifactRepository;
        this.actionStatusRepository = actionStatusRepository;
    }

    @Override
    public void process(@Valid FileTransferFailure message, MessageHeaders headers) {
        String tenant = getTenant(message.getTenantId());
        tenantAware.runAsTenant(tenant, processMessage(message));
    }

    private TenantAware.TenantRunner<Void> processMessage(FileTransferFailure message) {
        return () -> {

            log.debug("Processing file transfer failure message: {}", message);

            if (TRANSIENT_ERROR_STATUS.equals(message.getErrorType())) {
                log.debug("Transient error occurred while file transfer. Persisting error logs.");

                fileProcessingErrorLogsManagement.persistFileProcessingErrorLogs(
                        message.getFileType(),
                        message.getErrorDescription(),
                        message.getFileId(),
                        message.getStorageType(),
                        message.getRetryCount(),
                        ActionType.UPLOAD);

            } else if (PERMANENT_FAILURE_STATUS.equals(message.getErrorType())) {
                handlePermanentFailureStatus(message);
            }
            return null;
        };
    }

    @Override
    public Class<FileTransferFailure> getPayloadClass() {
        return FileTransferFailure.class;
    }

    @Override
    public String getQueueName() {
        return sqsQueueName;
    }

    public String getTenant(@NotNull Long tenantId) {
        TenantMetaData tenantMetaData = systemManagement.getTenantMetadataNoPermission(tenantId);
        return tenantMetaData.getTenant();
    }

    private void handlePermanentFailureStatus(FileTransferFailure message) {
        log.debug("Permanent failure occurred while file transfer. Updating the file upload error status for File ID: {}", message.getFileId());

        FileTransferStatus fileStatus = switch (message.getStorageType()) {
            case S3 -> FileTransferStatus.STORAGE_UPLOAD_ERROR;
            case CDN -> FileTransferStatus.CDN_UPLOAD_ERROR;
        };

        fileStatusManagerFactory.getInstance(message.getFileType()).updateFileStatus(message.getFileId(), fileStatus.name());
        String fileName = fileStatusManagerFactory.getInstance(message.getFileType()).getFileName(message.getFileId());
        List<String> errorMessages = fileProcessingErrorLogsManagement.getDistinctFailureMessages(message.getFileId(), message.getFileType());
        List<Long> actionIds = actionArtifactRepository.findActionsByArtifactId(message.getFileId())
                .stream()
                .map(JpaAction::getId)
                .distinct().toList();

        List<String> errorCodes = actionStatusRepository.findByActionIdIn(actionIds)
                .stream()
                .map(JpaActionStatus::getErrorCode)
                .distinct().toList();

        FileUploadErrorMessage fileUploadErrorMessage = FileUploadErrorMessage.builder().
                type(ERROR).
                fileId(message.getFileId()).
                fileName(fileName).
                status(fileStatus.name()).
                errorCode(errorCodes).
                errorMessages(errorMessages).
                timestamp(Instant.now().getEpochSecond()).
                build();


        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .fileType(message.getFileType().name().toUpperCase())
                .build();
        log.debug("Header For file upload failure Status: {}", header);


        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(fileUploadErrorMessage)
                .build();
        log.debug("Payload For file upload failure Status: {}", header);

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.FILE_UPLOAD_ERROR);
        log.debug("File Upload Failure Status notification sent to DOCG: {}", message.getFileId()  );
    }

}
