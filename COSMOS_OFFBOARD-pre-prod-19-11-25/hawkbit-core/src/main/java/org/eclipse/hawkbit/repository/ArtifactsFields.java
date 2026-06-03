package org.eclipse.hawkbit.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Enum representing the available fields for artifact sorting and filtering.
 * Each field maps to a database or DTO property.
 */
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
public enum ArtifactsFields implements FieldNameProvider {
    /** The unique identifier for the artifact. */
    ID("id"),
    /** The name of the artifact file. */
    NAME("fileName"),
    /** The status of the artifact. */
    STATUS("artifactStatus"),
    /** The status of the file. */
    FILE_STATUS("fileStatus"),
    /** The type of the file. */
    FILE_TYPE("fileType");

    private final String fieldName;

}