/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import jakarta.validation.ValidationException;

import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Create/build implementation.
 *
 */
public class JpaSoftwareModuleCreate extends AbstractSoftwareModuleUpdateCreate<SoftwareModuleCreate>
        implements SoftwareModuleCreate {

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;
    private final SoftwareModuleFormatManagement softwareModuleFormatManagement;
    private final SoftwareInstallerTypeManagement softwareInstallerTypeManagement;

    private boolean encrypted;

    JpaSoftwareModuleCreate(final SoftwareModuleTypeManagement softwareModuleTypeManagement,
                            final SoftwareModuleFormatManagement softwareModuleFormatManagement,
                            final SoftwareInstallerTypeManagement softwareInstallerTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.softwareModuleFormatManagement = softwareModuleFormatManagement;
        this.softwareInstallerTypeManagement = softwareInstallerTypeManagement;
    }


    @Override
    public SoftwareModuleCreate encrypted(final boolean encrypted) {
        this.encrypted = encrypted;
        return this;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    @Override
    public JpaSoftwareModule build() {
        return new JpaSoftwareModule(getSoftwareModuleTypeFromKeyString(type), name, version, description, vendor,
                encrypted, getSoftwareModuleFormatFromKeyString(format), getSoftwareInstallerTypeByName(swInstallerType));
    }

    private SoftwareModuleType getSoftwareModuleTypeFromKeyString(final String type) {
        if (type == null) {
            throw new ValidationException("type cannot be null");
        }

        return softwareModuleTypeManagement.getByKey(type.trim())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleType.class, type.trim()));
    }

    private SoftwareModuleFormat getSoftwareModuleFormatFromKeyString(final String format) {
        if (format == null) {
            throw new ValidationException("format cannot be null");
        }

        return softwareModuleFormatManagement.getByKey(format.trim())
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleFormat.class, format.trim()));
    }

    private SoftwareInstallerType getSoftwareInstallerTypeByName(final String swInstallerType) {
        if (swInstallerType == null) {
            throw new ValidationException("swInstallerType cannot be null");
        }
        return softwareInstallerTypeManagement.getSwInstallerTypeByName(swInstallerType.trim());
    }


}
