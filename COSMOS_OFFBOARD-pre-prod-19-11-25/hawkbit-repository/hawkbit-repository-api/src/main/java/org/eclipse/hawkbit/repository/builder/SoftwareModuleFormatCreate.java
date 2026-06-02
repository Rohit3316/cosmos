/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Builder to create a new {@link SoftwareModuleFormat} entry. Defines all fields
 * that can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface SoftwareModuleFormatCreate {
    /**
     * @param key
     *            for {@link SoftwareModuleFormat#getKey()}
     * @return updated builder instance
     */
    SoftwareModuleFormatCreate key(@Size(min = 1, max = SoftwareModuleFormat.KEY_MAX_SIZE) @NotNull String key);

    /**
     * @param name
     *            for {@link SoftwareModuleFormat#getName()}
     * @return updated builder instance
     */
    SoftwareModuleFormatCreate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link SoftwareModuleFormat#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleFormatCreate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);


    /**
     * @return peek on current state of {@link SoftwareModuleFormat} in the
     *         builder
     */
    SoftwareModuleFormat build();
}
