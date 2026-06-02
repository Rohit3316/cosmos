/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.User;

import jakarta.validation.constraints.NotEmpty;

/**
 * Builder for {@link User}.
 *
 */
public interface UserBuilder {

    /**
     * @param id
     *            of the updatable entity
     * @return builder instance
     */
    UserUpdate update(@NotEmpty Long id);
    /**
     * @return builder instance
     */
    UserCreate create();
}
