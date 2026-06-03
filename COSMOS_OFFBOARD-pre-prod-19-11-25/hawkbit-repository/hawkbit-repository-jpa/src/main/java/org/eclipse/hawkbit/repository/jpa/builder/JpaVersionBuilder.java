/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.VersionBuilder;
import org.eclipse.hawkbit.repository.builder.VersionCreate;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Builder implementation for {@link Target}.
 *
 */
public class JpaVersionBuilder implements VersionBuilder {
    private final VersionManagement versionManagement;
    private final SoftwareModuleManagement softwareModuleManagement;

    /**
     * @param versionManagement
     *          Target type management
     */
    public JpaVersionBuilder(VersionManagement versionManagement, SoftwareModuleManagement softwareModuleManagement) {
        this.versionManagement = versionManagement;
        this.softwareModuleManagement = softwareModuleManagement;
    }


    @Override
    public VersionCreate create() {
        return new JpaVersionCreate(versionManagement, softwareModuleManagement);
    }

}
