package org.eclipse.hawkbit.repository.jpa.service;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.hawkbit.repository.jpa.SoftwareModuleTypeRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleType;
import org.eclipse.hawkbit.repository.jpa.utils.SoftwareModuleTypeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of the {@link SoftwareModuleTypeService} interface.
 * <p>
 * This service handles operations related to software module types, such as
 * adding default module types for a specific tenant during initialization.
 * </p>
 */
@Service
public class SoftwareModuleTypeServiceImpl implements SoftwareModuleTypeService {

    /**
     * Predefined software module types that will be added for a new tenant.
     */
    private static final List<String> PREDEFINED_TYPES = List.of(
            "SOFTWARE", "CALIBRATION", "DATABASE", "APPLICATION", "MIDDLEWARE"
    );
    /**
     * Constructor for the {@code SoftwareModuleTypeServiceImpl}.
     *
     * @param moduleTypeRepository the repository for managing software module type entities.
     */
    private final SoftwareModuleTypeRepository moduleTypeRepository;
    /**
     * Default maximum number of assignments allowed for a software module type.
     * Loaded from the application configuration property `hawkbit.default.maxAssignments`.
     */
    @Value("${hawkbit.default.maxAssignments}")
    private Integer defaultMaxAssignments;

    public SoftwareModuleTypeServiceImpl(SoftwareModuleTypeRepository moduleTypeRepository) {
        this.moduleTypeRepository = moduleTypeRepository;
    }

    /**
     * Adds the default software module types for the specified tenant.
     * <p>
     * This method checks if each predefined type already exists for the tenant,
     * and if not, creates and saves a new {@link JpaSoftwareModuleType} entity with
     * default values.
     * </p>
     *
     * @param tenant the name of the tenant for which the default module types will be added.
     */
    @Override
    @Transactional
    public List<JpaSoftwareModuleType> addDefaultModuleTypesForTenant(String tenant) {
        List<JpaSoftwareModuleType> createdOrExistingModuleTypes = new ArrayList<>();

        PREDEFINED_TYPES.forEach(type -> {
            JpaSoftwareModuleType moduleType = moduleTypeRepository.findByTypeName(type)
                    .orElseGet(() -> {
                        JpaSoftwareModuleType newModuleType = new JpaSoftwareModuleType();
                        newModuleType.setTenant(tenant);
                        newModuleType.setKey(type.toLowerCase());
                        newModuleType.setName(type.toUpperCase());
                        newModuleType.setColour(generateDefaultColour(type));
                        newModuleType.setDescription("Created By Default");
                        newModuleType.setMaxAssignments(defaultMaxAssignments);
                        return moduleTypeRepository.save(newModuleType);
                    });
            createdOrExistingModuleTypes.add(moduleType);
        });

        return createdOrExistingModuleTypes;
    }


    /**
     * Generates a default color for a given software module type.
     * <p>
     * Each type is assigned a specific RGB color, and a default gray color is
     * used for unrecognized types.
     * </p>
     *
     * @param type the name of the software module type.
     * @return the RGB color code as a string.
     */
    private String generateDefaultColour(String type) {
        return switch (type.toUpperCase()) {
            case "SOFTWARE" -> "rgb(255,0,0)";
            case "CALIBRATION" -> "rgb(0,255,0)";
            case "APPLICATION" -> "rgb(0,0,255)";
            case "DATABASE" -> "rgb(255,255,0)";
            case "MIDDLEWARE" -> "rgb(0,255,255)";
            default -> "rgb(128,128,128)";
        };
    }
}
