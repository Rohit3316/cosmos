/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.UserManagement;
import org.eclipse.hawkbit.repository.builder.UserBuilder;
import org.eclipse.hawkbit.repository.builder.UserCreate;
import org.eclipse.hawkbit.repository.builder.UserUpdate;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Builder implementation for {@link Target}.
 *
 */
public class JpaUserBuilder implements UserBuilder {
   private final SystemManagement systemManagement;

    /**
     * @param userManagement
     *          Target type management
     */
    public JpaUserBuilder(UserManagement userManagement, SystemManagement systemManagement) {
        this.systemManagement = systemManagement;
    }

    @Override
    public UserUpdate update(final Long id) {
        return new JpaUserUpdate(id);
    }


    @Override
    public UserCreate create() {
        return new JpaUserCreate(systemManagement);
    }

}
