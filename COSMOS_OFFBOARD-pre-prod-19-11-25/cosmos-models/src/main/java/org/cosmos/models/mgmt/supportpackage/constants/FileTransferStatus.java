package org.cosmos.models.mgmt.supportpackage.constants;

public enum FileTransferStatus {

    /**
     * The file transfer status when the upload to the storage is in progress.
     */
    UPLOADING_TO_STORAGE,
    /**
     * The file transfer status when there is an error during the upload to the storage.
     */
    STORAGE_UPLOAD_ERROR,

    /**
     * The file transfer status when the upload to the storage is completed.
     */
    STORAGE_UPLOAD_SUCCESSFUL,

    /**
     * The file transfer status when the deletion from the storage is completed.
     */
    STORAGE_DELETE_SUCCESSFUL,

    /**
     * The file transfer status when the upload to the CDN is in progress.
     */
    UPLOADING_TO_CDN,

    /**
     * The file transfer status when there is an error during the upload to the CDN.
     */
    CDN_UPLOAD_ERROR,

    /**
     * The file transfer status when the upload to the CDN is completed.
     */
    CDN_UPLOAD_SUCCESSFUL,

    /**
     * The file transfer status when the deletion from the CDN is completed.
     */
    CDN_DELETE_SUCCESSFUL,


    /**
     * The file transfer status when the file is being deleted from the CDN.
     */
    DELETING_FROM_CDN,

    /**
     * The file transfer status when the file is being deleted from the storage.
     */
    DELETING_FROM_STORAGE,

    /**
     * The file transfer status when the file is being deleted from the storage.
     */

    STORAGE_DELETION_FAILED,

    /**
     * The file transfer status when the file is being deleted from the CDN.
     */
    CDN_DELETION_FAILED,
}
