/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.Version;

/**
 * Builder to create a new {@link User} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface VersionCreate {

    /**
     * @param name
     *            for {@link Version#getName()} filled with the firstname of the user
     * @return updated builder instance
     */
    VersionCreate name(@Size(min = 1, max = Version.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param description
     *            for {@link Version#getDescription()}
     * @return updated builder instance
     */
    VersionCreate description(@Size(max = Version.DESCRIPTION_MAX_SIZE) String description);


    /**
     * @param number
     *            for {@link Version#getNumber()}
     * @return updated builder instance
     */
    VersionCreate number(@Size(max = Version.NUMBER_MAX_SIZE) Integer number);

    /**
     * @param softwareModuleId
     *            for {@link Version#getSoftwareModuleId()}
     * @return updated builder instance
     */
    VersionCreate softwareModuleId(Long softwareModuleId);

    /**
     * @return peek on current state of {@link Version} in the builder
     */
    Version build();

}
