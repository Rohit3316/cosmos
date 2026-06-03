package com.stellantis.cosmos.sqs.app.service.listeners;

import com.stellantis.cosmos.sqs.app.model.FileTransferSuccess;
import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.kafka.FileUploadStatusMessage;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

/**
 * Listener for processing file transfer success events.
 * <p>
 * Extends {@link BatchMessageProcessor} to handle messages of type {@link FileTransferSuccess}.
 * Processes messages by updating the file status through the {@link FileStatusManagerFactory}
 * and running operations in a tenant-aware context.
 */
@Service
@Slf4j
public class FileTransferSuccessListener extends BatchMessageProcessor<FileTransferSuccess> {

    private static final String INFO = "INFO";
    private final FileStatusManagerFactory fileStatusManagerFactory;
    private final SystemManagement systemManagement;
    private final TenantAware tenantAware;
    @Value("${cosmos-file-transfer-success-server-sqs.name}")
    private String sqsQueueName;
    private final KafkaMessageService kafkaMessageService;

    public FileTransferSuccessListener(final FileStatusManagerFactory fileStatusManagerFactory,
                                       final SystemManagement systemManagement,
                                       final TenantAware tenantAware, final KafkaMessageService kafkaMessageService) {
        this.fileStatusManagerFactory = fileStatusManagerFactory;
        this.systemManagement = systemManagement;
        this.tenantAware = tenantAware;
        this.kafkaMessageService = kafkaMessageService;
    }

    @Override
    public void process(@Valid FileTransferSuccess message, MessageHeaders headers) {
        String tenant = getTenant(message.getTenantId());
        tenantAware.runAsTenant(tenant, processMessage(message, tenant));
    }

    @Override
    public Class<FileTransferSuccess> getPayloadClass() {
        return FileTransferSuccess.class;
    }

    @Override
    public String getQueueName() {
        return sqsQueueName;
    }

    private TenantAware.TenantRunner<Void> processMessage(FileTransferSuccess message, String tenant) {
        return () -> {
            fileStatusManagerFactory.getInstance(message.getFileType())
                    .updateFileStatus(message.getFileId(), message.getFileUploadStatus(), message.getChecksum(), tenant);

            sendFileUploadStatusToDOCG(message);
            log.debug("File upload status message sent to DOCG for file id: {}", message.getFileId());
            return null;
        };
    }

    public String getTenant(@NotNull Long tenantId) {
        TenantMetaData tenantMetaData = systemManagement.getTenantMetadataNoPermission(tenantId);
        return tenantMetaData.getTenant();
    }

    private void sendFileUploadStatusToDOCG(FileTransferSuccess message) {
        log.debug("Preparing to send Kafka event for file upload success status update for file id: {}", message.getFileId());

        fileStatusManagerFactory.getInstance(message.getFileType()).updateFileStatus(message.getFileId(), message.getFileUploadStatus());
        String fileName = fileStatusManagerFactory.getInstance(message.getFileType()).getFileName(message.getFileId());

        FileUploadStatusMessage fileUploadStatusMessage = FileUploadStatusMessage.builder().
                type("INFO").
                fileId(message.getFileId()).
                fileName(fileName).
                status(message.getFileUploadStatus()).
                fileType(message.getFileType().toString()).
                timestamp(Instant.now().getEpochSecond()).
                build();

        KafkaEventHeader header = KafkaEventHeader.builder().
                tenant(systemManagement.getTenantMetadata().getTenant()).
                fileType(message.getFileType().name().toUpperCase()).
                build();
        log.debug("Header to send Kafka event for file upload success status update: {}  for fileType: {}", header);

        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder().
                header(header).
                payload(fileUploadStatusMessage).
                build();
        log.debug("Payload to send Kafka event for file upload success status update: {}", eventTemplate);

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, Constants.FILE_UPLOAD);
        log.debug("Just sent Kafka event for file upload success status update.");
    }
}
