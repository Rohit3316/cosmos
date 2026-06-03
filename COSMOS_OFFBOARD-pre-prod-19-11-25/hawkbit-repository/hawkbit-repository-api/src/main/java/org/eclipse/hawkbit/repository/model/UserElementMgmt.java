/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * <p>
 * The {@link UserElementMgmt} is the user that can do operation on the platform
 * </p>
 */
public interface UserElementMgmt extends BaseEntity {

    /**
     * @return return the User itself
     */
    User getUser();


    /**
     * @return return the TenantMetadata
     */
    TenantMetaData getTenant();

}


