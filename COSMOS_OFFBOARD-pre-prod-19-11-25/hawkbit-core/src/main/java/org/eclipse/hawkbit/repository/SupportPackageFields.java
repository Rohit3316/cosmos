package org.eclipse.hawkbit.repository;


public enum SupportPackageFields implements FieldNameProvider {
    SHA256("sha256Hash"),

    CONTROLLERID("controllerId"),

    MD5("md5Hash"),

    FILE_VERSION("fileVersion"),

    TENANT("tenant"),

    FILENAME("fileName"),

    FILETYPE("fileType"),

    ID("id"),

    STATUS("fileStatus");

    private final String fieldName;

    SupportPackageFields(String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}
