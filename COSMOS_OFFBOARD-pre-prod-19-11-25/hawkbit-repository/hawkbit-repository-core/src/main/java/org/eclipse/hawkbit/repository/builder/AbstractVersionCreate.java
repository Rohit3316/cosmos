/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
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
public class AbstractVersionCreate<T> extends AbstractBaseEntityBuilder {

    protected String name;

    protected String description;

    protected Integer number;

    protected Long softwareModuleId;

    public T name(final String name) {
        this.name = name;
        return (T) this;
    }

    public T description(final String description) {
        this.description = description;
        return (T) this;
    }

    public T number(final Integer number) {
        this.number = number;
        return (T) this;
    }


    public T softwareModuleId(final Long softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
        return (T) this;
    }

    public long getSoftwareModuleId() {
        return softwareModuleId;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

    public Optional<Integer> getNumber() {
        return Optional.ofNullable(number);
    }



}
