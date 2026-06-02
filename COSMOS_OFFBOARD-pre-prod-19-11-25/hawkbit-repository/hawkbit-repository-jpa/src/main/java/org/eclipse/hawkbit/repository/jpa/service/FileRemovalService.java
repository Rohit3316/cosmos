package org.eclipse.hawkbit.repository.jpa.service;

import org.cosmos.models.sqs.FileType;

import jakarta.validation.ValidationException;


/**
 * Service interface for handling file deletion from storage.
 */
public interface FileRemovalService {

    /**
     * Removes the specified file from storage.
     *
     * @param fileId the ID of the file to be removed
     */
    void removeFileFromStorage(Long fileId) throws ValidationException;

    /**
     * Removes the specified file from the CDN.
     *
     * @param fileId the ID of the file to be removed
     */
    void removeFileFromCDN(Long fileId);

    /**
     * Gets the type of the file.
     *
     * @return the file type
     */
    FileType getFileType();

}
