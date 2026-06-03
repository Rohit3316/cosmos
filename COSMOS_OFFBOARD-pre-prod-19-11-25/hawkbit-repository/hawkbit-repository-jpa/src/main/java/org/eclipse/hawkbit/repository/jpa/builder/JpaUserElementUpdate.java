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
import org.eclipse.hawkbit.repository.builder.AbstractUserElementUpdateCreate;
import org.eclipse.hawkbit.repository.builder.UserElementUpdate;
import org.eclipse.hawkbit.repository.jpa.model.UserElement;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.User;

/**
 * Create/build implementation.
 *
 */
public class JpaUserElementUpdate extends AbstractUserElementUpdateCreate<UserElementUpdate> implements UserElementUpdate {

    private final UserManagement userManagement;
    private final SystemManagement systemManagement;
    /**
     * Constructor
     *
     * @param userManagement
     *          user  management
     * @param systemManagement
     *          system management management
     */
    JpaUserElementUpdate(final UserManagement userManagement, final SystemManagement systemManagement) {
        this.userManagement = userManagement;
        this.systemManagement = systemManagement;
    }



    @Override
    public UserElement build() {
        UserElement userTenant = new UserElement();
        if(tenantId != null){
            //Exception will be trown inside the method if tenant not present
            TenantMetaData tenant = systemManagement.getTenantMetadataNoPermission(tenantId);
            userTenant.setTenant(tenant);
        }
        if(userId != null){
            //Exception will be trown inside the method if user not present
            User user = userManagement.getUserByIdNoPermission(userId);
            userTenant.setUser(user);
        }

        return userTenant;
    }

}
