/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.Format;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;

/**
 * Builder to create a new {@link SoftwareModule} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface SoftwareModuleCreate {
    /**
     * @param name
     *            for {@link SoftwareModule#getName()}
     * @return updated builder instance
     */
    SoftwareModuleCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param version
     *            for {@link SoftwareModule#getVersion()}
     * @return updated builder instance
     */
    SoftwareModuleCreate version(@Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE) @NotNull String version);

    /**
     * @param description
     *            for {@link SoftwareModule#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param vendor
     *            for {@link SoftwareModule#getVendor()}
     * @return updated builder instance
     */
    SoftwareModuleCreate vendor(@Size(max = SoftwareModule.VENDOR_MAX_SIZE) String vendor);

    /**
     * @param typeKey
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    SoftwareModuleCreate type(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotNull String typeKey);

    /**
     * @param maxAssignments
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    SoftwareModuleCreate maxAssignments(@Size(min = 1, max = Type.KEY_MAX_SIZE) @NotNull Integer maxAssignments);

    /**
     * @param type
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate type(final SoftwareModuleType type) {
        return type(Optional.ofNullable(type).map(SoftwareModuleType::getKey).orElse(""));
    }

    /**
     * @param maxAssignments
     *            for {@link SoftwareModule#getMaxAssignments()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate maxAssignments(final SoftwareModuleType maxAssignments) {
        return maxAssignments(Optional.ofNullable(maxAssignments).map(SoftwareModuleType::getMaxAssignments).orElse(1));
    }

    /**
     * @param formatKey
     *            for {@link SoftwareModule#getFormat()} ()}
     * @return updated builder instance
     */
    SoftwareModuleCreate format(@Size(min = 1, max = Format.KEY_MAX_SIZE) @NotNull String formatKey);

    /**
     * @param format
     *            for {@link SoftwareModule#getType()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate format(final SoftwareModuleFormat format) {
        return format(Optional.ofNullable(format).map(SoftwareModuleFormat::getKey).orElse(""));
    }


    /**
     * @param encrypted
     *            if should be encrypted
     * @return updated builder instance
     */
    SoftwareModuleCreate encrypted(boolean encrypted);

    /**
     * @param swInstallerType
     *            for {@link SoftwareModule#getSoftwareInstallerType()}
     * @return updated builder instance
     */
    SoftwareModuleCreate swInstallerType(@Size(min = 1, max = SoftwareInstallerType.INSTALLER_TYPE_NAME_MAX_SIZE) @NotNull String swInstallerType);

    /**
     * @param swInstallerType
     *            for {@link SoftwareModule#getSoftwareInstallerType()}
     * @return updated builder instance
     */
    default SoftwareModuleCreate swInstallerType(final SoftwareInstallerType swInstallerType) {
        return swInstallerType(Optional.ofNullable(swInstallerType).map(SoftwareInstallerType::getName).orElse(""));
    }

    /**
     * @return peek on current state of {@link SoftwareModule} in the builder
     */
    SoftwareModule build();
}
