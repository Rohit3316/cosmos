package org.eclipse.hawkbit.repository.jpa.service;


import org.cosmos.models.sqs.FileType;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;

@Service
public class FileRemovalServiceFactory {
    private final List<FileRemovalService> fileRemovalServiceList;

    private static final EnumMap<FileType,FileRemovalService> fileRemovalServiceMap = new EnumMap<>(FileType.class);
    public FileRemovalServiceFactory(List<FileRemovalService> fileRemovalServiceList) {
        this.fileRemovalServiceList = fileRemovalServiceList;
    }

    @PostConstruct
    public void init() {
       fileRemovalServiceList.forEach(fileRemovalService -> fileRemovalServiceMap.put(fileRemovalService.getFileType(), fileRemovalService));
    }
    public static  FileRemovalService getInstance(FileType fileType) {
        return fileRemovalServiceMap.get(fileType);
    }
}
