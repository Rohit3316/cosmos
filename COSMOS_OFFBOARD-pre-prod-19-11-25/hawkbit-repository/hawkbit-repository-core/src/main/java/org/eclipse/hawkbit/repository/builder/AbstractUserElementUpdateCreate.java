/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public class AbstractUserElementUpdateCreate<T> extends AbstractBaseEntityBuilder {

    protected Long userId;

    protected Long tenantId;


    public T userId(final Long userId) {
        this.userId = userId;
        return (T) this;
    }

    public T tenantId(final Long tenantId) {
        this.tenantId = tenantId;
        return (T) this;
    }

    public Optional<Long> getUserId() {
        return Optional.ofNullable( userId);
    }

    public Optional<Long> getTenantId() {
        return  Optional.ofNullable(tenantId);
    }
}
