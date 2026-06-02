/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
@ConfigurationProperties(prefix = "hawkbit.default")
public abstract class AbstractSoftwareModuleTypeUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {
    @ValidString
    protected String colour;
    @ValidString
    protected String key;

    public T colour(final String colour) {
        this.colour = StringUtils.trimWhitespace(colour);
        return (T) this;
    }

    public Optional<String> getColour() {
        return Optional.ofNullable(colour);
    }

    public T key(final String key) {
        this.key = StringUtils.trimWhitespace(key);
        return (T) this;
    }

    public Optional<String> getKey() {
        return Optional.ofNullable(key);
    }

}
