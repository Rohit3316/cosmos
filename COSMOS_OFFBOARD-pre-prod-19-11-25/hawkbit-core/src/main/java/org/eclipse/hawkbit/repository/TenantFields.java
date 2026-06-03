package org.eclipse.hawkbit.repository;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Describing the fields of the TargetType model which can be used in
 * the REST API
 */
@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
public enum TenantFields implements FieldNameProvider {
    /**
     * The id field.
     */
    ID("id"),
    /**
     * The value field.
     */
    VALUE("value");

    private final String fieldName;
}
