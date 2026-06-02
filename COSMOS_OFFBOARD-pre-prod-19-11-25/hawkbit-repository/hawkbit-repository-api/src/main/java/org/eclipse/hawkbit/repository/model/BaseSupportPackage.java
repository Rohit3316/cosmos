package org.eclipse.hawkbit.repository.model;



import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.cosmos.models.sns.SnsPublishable;

import java.util.Map;

/**
 * Interface to access the common attributes of a support package.
 */
public interface BaseSupportPackage extends TenantAwareBaseEntity, SnsPublishable {
    /**
     * Returns the name of the file.
     *
     * @return the name of the file
     */
    String getFileName();

    /**
     * Returns the URL of the file.
     *
     * @return the URL of the file
     */
    String getFileUrl();

    /**
     * Returns the SHA-256 hash of the file.
     *
     * @return the SHA-256 hash of the file
     */
    String getSha256Hash();

    /**
     * Returns the MD5 hash of the file.
     *
     * @return the MD5 hash of the file
     */
    String getMd5Hash();

    /**
     * Returns the version of the file.
     *
     * @return the version of the file
     */
    String getFileVersion();

    /**
     * Returns the description of the file content.
     *
     * @return the description of the file content
     */
    String getFileContentDescription();

    /**
     * Returns the URL of the file information.
     *
     * @return the URL of the file information
     */
    String getFileInfoUrl();

    /**
     * Returns a map of metadata associated with the support package.
     *
     * @return a map of metadata
     */
    Map<String, String> getMetadata();

    /**
     * Returns the type of the support package file.
     *
     * @return the type of the support package file
     */
    MgmtSupportPackageFileType getFileType();

    /**
     * Returns the status of the file transfer.
     *
     * @return the status of the file transfer
     */

    FileTransferStatus getSupportPackageFileStatus();

    /**
     * Returns the size of the file in bytes.
     *
     * @return the file size in bytes, or {@code null} if not available
     */
    Long getFileSize();
}
