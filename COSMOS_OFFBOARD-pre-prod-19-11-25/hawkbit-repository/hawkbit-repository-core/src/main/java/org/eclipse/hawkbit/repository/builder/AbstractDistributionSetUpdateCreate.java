/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractDistributionSetUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    @ValidString
    protected String version;
    protected Boolean requiredMigrationStep;
    protected Boolean softwareDowngradeEnabled;

    protected Map<Long, Long> modules;

    public T modules(final Map<Long, Long> modules) {
        this.modules = modules;
        return (T) this;
    }

    public Map<Long, Long> getModules() {
        return modules;
    }

    public T requiredMigrationStep(final Boolean requiredMigrationStep) {
        this.requiredMigrationStep = requiredMigrationStep;
        return (T) this;
    }

    public Boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    public T version(final String version) {
        this.version = StringUtils.trimWhitespace(version);
        return (T) this;
    }

    public Optional<String> getVersion() {
        return Optional.ofNullable(version);
    }
    
    public T softwareDowngradeEnabled(final Boolean softwareDowngradeEnabled) {
        this.softwareDowngradeEnabled = softwareDowngradeEnabled;
        return (T) this;
    }
    
    public Boolean isSoftwareDowngradeEnabled() {
        return softwareDowngradeEnabled;
    }

}
