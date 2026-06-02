package org.eclipse.hawkbit.repository;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
/**
 * Enum representing Ecu Model fields with their corresponding field names.
 * Implements the {@link FieldNameProvider} interface to provide field name functionality.
 */
public enum EcuModelFields implements FieldNameProvider {

    /**
     * The unique identifier for the ecu model.
     */
    ID("id");

    private final String fieldName;
}