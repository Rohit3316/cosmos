/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

import jakarta.validation.constraints.Size;

/**
 * Builder to update an existing {@link SoftwareModuleType} entry. Defines all
 * fields that can be updated.
 *
 */
public interface SoftwareModuleFormatUpdate {
    /**
     * @param description
     *            for {@link SoftwareModuleType#getDescription()}
     * @return updated builder instance
     */
    SoftwareModuleFormatUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

}
