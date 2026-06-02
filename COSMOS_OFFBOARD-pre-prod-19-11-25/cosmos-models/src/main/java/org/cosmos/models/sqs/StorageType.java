package org.cosmos.models.sqs;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the type of storage.
 */
public enum StorageType {
    S3("S3"), CDN("CDN");

    private final String type;

    /**
     * Constructor for StorageType.
     *
     * @param storageType the type of storage
     */
    StorageType(String storageType) {
        this.type = storageType;
    }

    /**
     * Gets the type of storage.
     *
     * @return the type of storage
     */
    @JsonValue
    public String getType() {
        return type;
    }
}
