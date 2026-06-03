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
import org.eclipse.hawkbit.repository.builder.*;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Builder implementation for {@link Target}.
 *
 */
public class JpaUserElementBuilder implements UserElementBuilder {
    private final UserManagement userManagement;
    private final SystemManagement systemManagement;

    /**
     * @param userManagement
     *          Target type management
     */
    public JpaUserElementBuilder(UserManagement userManagement, SystemManagement systemManagement) {
        this.systemManagement = systemManagement;
        this.userManagement = userManagement;
    }


    @Override
    public UserElementCreate create() {
        return new JpaUserElementCreate(userManagement, systemManagement);
    }
    @Override
    public UserElementUpdate update() {
        return new JpaUserElementUpdate(userManagement, systemManagement);
    }
}
