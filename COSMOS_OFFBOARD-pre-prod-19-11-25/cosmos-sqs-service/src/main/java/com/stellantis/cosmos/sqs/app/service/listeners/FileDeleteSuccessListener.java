package com.stellantis.cosmos.sqs.app.service.listeners;

import com.stellantis.cosmos.sqs.app.model.FileDeleteSuccess;
import com.stellantis.cosmos.sqs.app.service.core.BatchMessageProcessor;
import com.stellantis.cosmos.sqs.app.service.core.FileStatusManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.jpa.ArtifactsRepository;
import org.eclipse.hawkbit.repository.jpa.service.FileRemovalServiceFactory;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Listener for processing file delete success messages.
 */
@Service
@Slf4j
public class FileDeleteSuccessListener extends BatchMessageProcessor<FileDeleteSuccess> {

    private final FileStatusManagerFactory fileStatusManagerFactory;
    private final SystemManagement systemManagement;
    private final TenantAware tenantAware;
    @Value("${cosmos-file-delete-success-server-sqs.name}")
    private String sqsQueueName;
    private final ArtifactsRepository artifactsRepository;
    private final List<FileTransferStatus> ACTIVE_DELETE_STATUSES = List.of(
            FileTransferStatus.DELETING_FROM_STORAGE,
            FileTransferStatus.DELETING_FROM_CDN);


    public FileDeleteSuccessListener(FileStatusManagerFactory fileStatusManagerFactory,
                                     SystemManagement systemManagement,
                                     TenantAware tenantAware, ArtifactsRepository artifactsRepository
    ) {
        this.fileStatusManagerFactory = fileStatusManagerFactory;
        this.systemManagement = systemManagement;
        this.tenantAware = tenantAware;

        this.artifactsRepository = artifactsRepository;
    }

    @Override
    public String getQueueName() {
        return sqsQueueName;
    }


    @Override
    public Class<FileDeleteSuccess> getPayloadClass() {
        return FileDeleteSuccess.class;
    }

    public String getTenant(@NotNull Long tenantId) {
        TenantMetaData tenantMetaData = systemManagement.getTenantMetadataNoPermission(tenantId);
        return tenantMetaData.getTenant();
    }

    @Override
    public void process(@Valid FileDeleteSuccess message, MessageHeaders headers) {
        log.debug("Processing file delete success message: {}", message);
        String tenant = getTenant(message.getTenantId());
        tenantAware.runAsTenant(tenant, processMessage(message));
    }


    private TenantAware.TenantRunner<Void> processMessage(FileDeleteSuccess message) {
        return () -> {
            processDeleteMessage(message);
            return null;
        };
    }

    private void processDeleteMessage(FileDeleteSuccess message) {
        Optional<Artifacts> artifactsOpt = artifactsRepository.getArtifactsById(message.getFileId());
        boolean shouldUpdate = isShouldUpdate(message, artifactsOpt);

        if (shouldUpdate) {
            fileStatusManagerFactory.getInstance(message.getFileType())
                    .updateFileStatus(message.getFileId(), message.getStatus().name());
            log.debug("File delete status updated for file id: {} and file type: {} to file Status: {}", message.getFileId(), message.getFileType(), message.getStatus());
        }
        if (message.getStatus().equals(FileTransferStatus.CDN_DELETE_SUCCESSFUL) && message.getFileType() == FileType.ARTIFACT) {
            FileRemovalServiceFactory.getInstance(message.getFileType()).removeFileFromStorage(message.getFileId());
            log.debug("File delete status updated for file id: {} and file type: {} to file Status: {}", message.getFileId(), message.getFileType(), message.getStatus());
        }
    }

    private static boolean isShouldUpdate(FileDeleteSuccess message, Optional<Artifacts> artifactsOpt) {
        Artifacts artifact = artifactsOpt.get();
        String currentStatus = artifact.getArtifactStatus();
        FileTransferStatus newStatus = message.getStatus();

        if (FileTransferStatus.DELETING_FROM_STORAGE.name().equals(currentStatus)
                && FileTransferStatus.STORAGE_DELETE_SUCCESSFUL.equals(newStatus)) {
             return true;
        } else if (FileTransferStatus.DELETING_FROM_CDN.name().equals(currentStatus)
                && FileTransferStatus.CDN_DELETE_SUCCESSFUL.equals(newStatus)) {
            return true;
        } else if ((FileTransferStatus.CDN_DELETE_SUCCESSFUL.name().equals(currentStatus)
                && FileTransferStatus.DELETING_FROM_CDN.equals(newStatus))
                || (FileTransferStatus.STORAGE_DELETE_SUCCESSFUL.name().equals(currentStatus)
                && FileTransferStatus.DELETING_FROM_STORAGE.equals(newStatus))) {
            return false;
        }
        return false;
    }
}
