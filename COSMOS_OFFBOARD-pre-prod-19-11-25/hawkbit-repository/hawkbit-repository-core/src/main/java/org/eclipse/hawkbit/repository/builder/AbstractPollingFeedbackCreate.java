/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public class AbstractPollingFeedbackCreate<T> {

    protected Long pollingId;
    protected String feedback;

    protected AbstractPollingFeedbackCreate(Long pollingId) {
        this.pollingId = pollingId;
    }

    public T feedback(final String feedback) {
        this.feedback = feedback;
        return (T) this;
    }

    public T pollingId(final Long pollingId) {
        this.pollingId = pollingId;
        return (T) this;
    }

    public T id(final Long id) {
        this.pollingId = id;
        return (T) this;
    }
}
