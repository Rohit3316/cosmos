package org.eclipse.hawkbit.repository;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(force = true)
@AllArgsConstructor
@Getter
/**
 * Enum representing inventory fields with their corresponding field names.
 * Implements the {@link FieldNameProvider} interface to provide field name functionality.
 */

public enum InventoryFields  implements FieldNameProvider{
    /**
     * The id field.
     */
    ID("id");

    private final String fieldName;

}
