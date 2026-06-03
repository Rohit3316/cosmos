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
import org.eclipse.hawkbit.repository.model.User;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Collection;

/**
 * Builder to update an existing {@link User} entry. Defines all fields that
 * can be updated.
 *
 */
public interface UserUpdate {

    /**
     * @param firstname
     *            for {@link User#getFirstname()}
     * @return updated builder instance
     */
    UserUpdate firstname(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String firstname);

    /**
     * @param lastname
     *            for {@link User#getLastname()}
     * @return updated builder instance
     */
    UserUpdate lastname(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String lastname);

    UserUpdate tenants(Collection<Long> tenants);




}
