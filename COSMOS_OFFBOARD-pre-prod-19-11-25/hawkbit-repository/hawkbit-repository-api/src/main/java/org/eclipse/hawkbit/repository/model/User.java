/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Set;

/**
 * <p>
 * The {@link User} is the user that can do operation on the platform
 * </p>
 */
public interface User extends BaseEntity {

    /**
     *
     * @return the username of the user
     */
    String getUsername();

    /**
     *
     * @return the email of the user
     */
    String getEmail();

    /**
     *
     * @return user is active/inactive
     */
    boolean isActive();

    /**
     *
     * @return the first name of the user
     */
    String getFirstname();

    /**
     *
     * @return the lastname of the user
     */
    String getLastname();

    /**
     *
     * @return the lastname of the user
     */
    List<TenantMetaData> getTenantMetadata();

    /**
     *
     * @return the lastname of the user
     */
    Set<Role> getRoles();
}
