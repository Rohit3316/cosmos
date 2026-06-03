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

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

public abstract class AbstractNamedEntityBuilder<T> extends AbstractBaseEntityBuilder {

    @ValidString
    protected String name;

    @ValidString
    protected String description;

    protected Integer maxAssignments;

    public T name(final String name) {
        this.name = StringUtils.trimWhitespace(name);
        return (T) this;
    }

    public T description(final String description) {
        this.description = StringUtils.trimWhitespace(description);
        return (T) this;
    }

    public T maxAssignments(final Integer maxAssignments) {
        this.maxAssignments = maxAssignments;
        return (T) this;
    }
    public Integer getMaxAssignments() {
        return maxAssignments;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public Optional<String> getDescription() {
        return Optional.ofNullable(description);
    }

}
