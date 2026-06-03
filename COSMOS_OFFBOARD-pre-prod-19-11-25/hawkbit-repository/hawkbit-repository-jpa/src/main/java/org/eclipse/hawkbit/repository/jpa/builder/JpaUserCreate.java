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
import org.eclipse.hawkbit.repository.builder.AbstractUserCreateUpdate;
import org.eclipse.hawkbit.repository.builder.UserCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Create/build implementation.
 *
 */
public class JpaUserCreate extends AbstractUserCreateUpdate<UserCreate> implements UserCreate {


    private final SystemManagement systemManagement;

    /**
     * Constructor
     *
     *          Target type management
     */
    JpaUserCreate( final SystemManagement systemManagement) {
        super(null);
        this.systemManagement = systemManagement;
    }

    @Override
    public JpaUser build() {
        JpaUser user = new JpaUser(username, password, firstname,lastname);
            Collection<TenantMetaData> result = findTenantMetadataWithExceptionIfNotFound(tenants);
        for (TenantMetaData tenant :result) {
            user.addTenant(tenant);
        }
        return user;
    }


    private Collection<TenantMetaData> findTenantMetadataWithExceptionIfNotFound(final Collection<Long> tenantMetadataId){
        if (CollectionUtils.isEmpty(tenantMetadataId)) {
            return Collections.emptyList();
        }

        final Collection<TenantMetaData> foundTenants = systemManagement.getTenants(tenantMetadataId);
        if(foundTenants.size() < tenantMetadataId.size()){
            throw new EntityNotFoundException(TenantMetaData.class, tenantMetadataId);
        }
        return foundTenants;
    }

}
