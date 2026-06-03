/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.AbstractVersionCreate;
import org.eclipse.hawkbit.repository.builder.VersionCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

/**
 * Create/build implementation.
 */
public class JpaVersionCreate extends AbstractVersionCreate<VersionCreate> implements VersionCreate {

    private final VersionManagement versionManagement;
    private final SoftwareModuleManagement softwareModuleManagement;

    /**
     * Constructor
     *
     * @param versionManagement Target type management
     */
    JpaVersionCreate(final VersionManagement versionManagement, final SoftwareModuleManagement softwareModuleManagement) {
        this.versionManagement = versionManagement;
        this.softwareModuleManagement = softwareModuleManagement;
    }


    @Override
    public JpaVersion build() {
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        versionManagement.findByNameOrNumberAndModuleId(name, number, Math.toIntExact(module.getId()))
                .ifPresent(v -> {
                    throw new EntityAlreadyExistsException();
                });

        return new JpaVersion(name, description, number, module);
    }

}
