/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Collection;
import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public class AbstractUserCreateUpdate<T> extends AbstractBaseEntityBuilder {

    protected Long userId;

    protected String username;

    protected String password;

    protected String firstname;

    protected String lastname;

    protected Collection<Long> tenants;

    public T tenants(final Collection<Long> tenants) {
        this.tenants = tenants;
        return (T) this;
    }
    protected AbstractUserCreateUpdate(final Long id) {
        this.id = id;
    }

    public T username(final String username) {
        this.username = username;
        return (T) this;
    }

    public T password(final String password) {
        this.password = password;
        return (T) this;
    }

    public T firstname(final String firstname) {
        this.firstname = firstname;
        return (T) this;
    }

    public T lastname(final String lastname) {
        this.lastname = lastname;
        return (T) this;
    }

    public T id(final Long id){
        this.id = id;
        return (T) this;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public Optional<String> getFirstname() {
        return Optional.ofNullable(firstname);
    }

    public Optional<String> getLastname() {
        return Optional.ofNullable(lastname);
    }

    public Optional<Collection<Long>> getTenants() {
        return Optional.ofNullable(tenants);
    }

    public Optional<Long> getUserId() {
        return Optional.ofNullable( userId);
    }

}
