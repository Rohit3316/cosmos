/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.AbstractDistributionSetUpdateCreate;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSoftwareVersionWrapper;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.springframework.util.StringUtils;

/**
 * Create/build implementation.
 *
 */
public class JpaDistributionSetCreate extends AbstractDistributionSetUpdateCreate<DistributionSetCreate>
        implements DistributionSetCreate {

    @ValidString
    private String type;

    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final VersionManagement versionManagement;

    JpaDistributionSetCreate(final DistributionSetTypeManagement distributionSetTypeManagement,
            final SoftwareModuleManagement softwareManagement,
                             final VersionManagement versionManagement) {
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.softwareModuleManagement = softwareManagement;
        this.versionManagement = versionManagement;
    }

    @Override
    public JpaDistributionSet build() {

        List<DistributionSoftwareVersionWrapper> dsvw = new ArrayList<>();
        if(modules != null) {
            modules.forEach((key, value) -> {
                Optional<SoftwareModule> sm = softwareModuleManagement.get(key);
                if (sm.isEmpty())
                    throw new EntityNotFoundException(SoftwareModule.class, key);
                Optional<Version> v = versionManagement.getById(value);
                if (v.isEmpty())
                    throw new EntityNotFoundException(Version.class, value);
                dsvw.add(new DistributionSoftwareVersionWrapper(sm.get(), v.get()));
            });
        }
        return new JpaDistributionSet(name, version, description,
                Optional.ofNullable(type).map(this::findDistributionSetTypeWithExceptionIfNotFound).orElse(null),
                dsvw,
                Optional.ofNullable(requiredMigrationStep).orElse(Boolean.FALSE),
                Optional.ofNullable(softwareDowngradeEnabled).orElse(Boolean.FALSE));
    }

    @Override
    public DistributionSetCreate type(final String type) {
        this.type = StringUtils.trimWhitespace(type);
        return this;
    }

    public String getType() {
        return type;
    }

    private DistributionSetType findDistributionSetTypeWithExceptionIfNotFound(final String distributionSetTypekey) {
        return distributionSetTypeManagement.getByKey(distributionSetTypekey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetType.class, distributionSetTypekey));
    }

}
