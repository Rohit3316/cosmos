package org.cosmos.sns.services;

/**
 * Represents the type of SNS service.
 */
public enum SnsServiceType {

    /**
     * S3 file transfer service.
     */
    S3_FILE_TRANSFER,

    /**
     * CDN delete service.
     */
    CDN_DELETE,

    /**
     * CDN upload service.
     */
    CDN_UPLOAD,


    S3_FILE_DELETE
}