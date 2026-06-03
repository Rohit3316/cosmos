package org.cosmos.models.sqs;

import com.fasterxml.jackson.annotation.JsonValue;

public enum FileType {
ARTIFACT("ARTIFACT"),ESP("ESP"),RSP("RSP");

    private final String type;

    FileType(String fileType) {
        this.type = fileType;
    }

    @JsonValue
    public String getType() {
        return type;
    }
}
