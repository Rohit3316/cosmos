package org.eclipse.hawkbit.repository.jpa.utils;

import java.util.List;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;

/**
 * Service interface for managing software module types.
 */
public interface SoftwareModuleTypeService {
    /**
     * Adds the default software module types for a specified tenant.
     * <p>
     * This method ensures that a tenant has the necessary default software
     * module types required for the application to function properly.
     * If a module type does not already exist for the specified tenant, it is created;
     * otherwise, the existing module type is retrieved and returned.
     * </p>
     * <p>
     * It is typically called during the tenant creation or initialization process.
     * </p>
     *
     * @param tenant the name of the tenant for whom the default module types are to be added.
     *               This is a non-null and case-insensitive string that identifies the tenant.
     * @return a list of {@link JpaSoftwareModuleType} representing the created or existing
     *         module types for the specified tenant.
     */
    List<JpaSoftwareModuleType> addDefaultModuleTypesForTenant(String tenant);
}
