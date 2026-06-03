package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sqs.FileType;
import org.cosmos.sns.services.factory.SnsServiceFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageFileSystemProperties;
import org.eclipse.hawkbit.repository.file.supportpackage.configuration.SupportPackageUrlHandlerProperties;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.springframework.stereotype.Service;

import jakarta.validation.ValidationException;


@Service
public class RspFileRemovalService extends SupportPackageS3FileDeletionService implements FileRemovalService  {
    private final RspRepository rspRepository;
    public RspFileRemovalService(SupportPackageFileSystemProperties supportPackageFileSystemProperties, SupportPackageUrlHandlerProperties supportUrlHandlerProperties, TenantAware tenantAware, SnsServiceFactory snsServiceFactory, SystemManagement systemManagement, RspRepository rspRepository) {
        super(supportPackageFileSystemProperties, supportUrlHandlerProperties, tenantAware, snsServiceFactory, systemManagement);
        this.rspRepository = rspRepository;
    }

    @Override
    public void removeFileFromStorage(Long fileId) throws ValidationException {
        Rsp rsp = getRsp(fileId);
        if(rsp.getSupportPackageFileStatus().equals(FileTransferStatus.STORAGE_DELETE_SUCCESSFUL)){
            throw new ValidationException("File is already deleted from storage");
        }
        removeFileFromStorage(rsp, FileType.RSP.getType(),getStorageDirectoryPlaceHolder());
    }

    @Override
    public void removeFileFromCDN(Long fileId) {
//Implement when it is necessary to remove rsp from cdn
    }

    @Override
    public FileType getFileType() {
        return FileType.RSP;
    }

    private String getStorageDirectoryPlaceHolder(){
        return supportUrlHandlerProperties.getRsp().getS3().getDirectory();
    }
    private Rsp getRsp(Long fileId) {
        return rspRepository.findById(fileId).orElseThrow(()->new EntityNotFoundException(Rsp.class,fileId));
    }
}
