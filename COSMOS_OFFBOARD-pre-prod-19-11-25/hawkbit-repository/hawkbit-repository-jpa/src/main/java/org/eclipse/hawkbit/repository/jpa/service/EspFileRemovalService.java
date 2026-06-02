package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.stereotype.Service;

import jakarta.validation.ValidationException;


@Service
public class EspFileRemovalService extends SupportPackageS3FileDeletionService implements FileRemovalService {
    private final EspRepository espRepository;
    public EspFileRemovalService(SupportPackageFileSystemProperties supportPackageFileSystemProperties,
                                 SupportPackageUrlHandlerProperties supportUrlHandlerProperties,
                                 TenantAware tenantAware,
                                 SnsServiceFactory snsServiceFactory,
                                 SystemManagement systemManagement,
                                 EspRepository espRepository) {
        super(supportPackageFileSystemProperties, supportUrlHandlerProperties, tenantAware, snsServiceFactory, systemManagement);
        this.espRepository = espRepository;
    }

    @Override
    public void removeFileFromStorage(Long fileId) {
        Esp esp= getEsp(fileId);
        if(esp.getSupportPackageFileStatus().equals(FileTransferStatus.STORAGE_DELETE_SUCCESSFUL)){
            throw new ValidationException("File is already deleted from storage");
        }
        removeFileFromStorage(esp, FileType.ESP.getType(),getStorageDirectoryPlaceHolder());

    }

    @Override
    public void removeFileFromCDN(Long fileId) {
//Implement when it is necessary to remove esp from cdn
    }

    @Override
    public FileType getFileType() {
        return FileType.ESP;
    }

    private Esp getEsp(Long fileId) {
        return espRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException(Esp.class,fileId));
    }

    private String getStorageDirectoryPlaceHolder(){
        return supportUrlHandlerProperties.getEsp().getS3().getDirectory();
    }
}
