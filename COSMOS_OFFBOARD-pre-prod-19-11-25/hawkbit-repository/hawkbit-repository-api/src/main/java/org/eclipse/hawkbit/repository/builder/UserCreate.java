/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.User;

/**
 * Builder to create a new {@link User} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface UserCreate {


    /**
     * @param firstname
     *            for {@link User#getFirstname()} ()} filled with the firstname of the user
     * @return updated builder instance
     */
    UserCreate firstname(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String firstname);

    /**
     * @param lastname
     *            for {@link User#getLastname()}
     * @return updated builder instance
     */
    UserCreate lastname(@Size(max = NamedEntity.NAME_MAX_SIZE) String lastname);


    /**
     * @param username
     *            for {@link User#getUsername()} ()}
     * @return updated builder instance
     */
    UserCreate username(@Size(max = NamedEntity.NAME_MAX_SIZE) String username);


    /**
     * @param password
     *            for {@link User#getPassword()} ()}
     * @return updated builder instance
     */
    UserCreate password(@Size(max = NamedEntity.NAME_MAX_SIZE) String password);

    UserCreate tenants(Collection<Long> tenants);

    /**
     * @return peek on current state of {@link User} in the builder
     */
    User build();

}
