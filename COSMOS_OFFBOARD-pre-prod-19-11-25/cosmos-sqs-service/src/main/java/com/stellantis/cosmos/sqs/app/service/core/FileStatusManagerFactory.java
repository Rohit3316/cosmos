package com.stellantis.cosmos.sqs.app.service.core;

import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.sqs.FileType;
import com.stellantis.cosmos.sqs.app.service.FileStatusManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;

/**
 * Factory class for managing and retrieving {@link FileStatusManagement} implementations.
 *
 * Maintains a registry of implementations mapped by {@link FileType}.
 * Initializes the registry during the post-construction phase and provides
 * instances based on the file type.
 *
 * Logs details about the registry and errors when an implementation for a
 * specific file type is not found.
 *
 * @see FileStatusManagement
 * @see FileType
 */

@Slf4j
@Service
public class FileStatusManagerFactory {

    private final List<FileStatusManagement> fileStatusManagementList;

    private static final EnumMap<FileType,FileStatusManagement> fileStatusManagerRegistry=new EnumMap<>(FileType.class);

    public FileStatusManagerFactory(List<FileStatusManagement> fileStatusManagementList) {
        this.fileStatusManagementList = fileStatusManagementList;
    }

    @PostConstruct
     protected void initializer(){
        log.debug("No. of Implementations found:{}",fileStatusManagementList.size());
        fileStatusManagementList.forEach(fileStatusManager->fileStatusManagerRegistry.put(fileStatusManager.getFileType(),fileStatusManager));
        log.debug("Registry initialized with the map:{}",fileStatusManagerRegistry);
    }

    /**
     * Retrieves the {@link FileStatusManagement} implementation for the given {@link FileType}.
     *
     * @param fileType the type of file for which the implementation is needed
     * @return the corresponding {@link FileStatusManagement} implementation
     */
    public FileStatusManagement getInstance(FileType fileType){
        if(fileStatusManagerRegistry.containsKey(fileType)){
            return fileStatusManagerRegistry.get(fileType);
        }else {
            log.error("Implementation don't exist for the fileType :{}",fileType.toString());
            throw new EntityNotFoundException("Implementation don't exist for the fileType:"+fileType.toString());
        }
    }
}
