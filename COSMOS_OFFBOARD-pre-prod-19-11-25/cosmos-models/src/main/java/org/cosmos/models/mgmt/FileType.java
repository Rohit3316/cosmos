package org.cosmos.models.mgmt;

public enum FileType {

    DELTA("delta"),
    FULL("full");

    private final String typeName;

    FileType(String typeName){
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

}
