/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.ValidString;
import org.springframework.util.StringUtils;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public abstract class AbstractActionStatusCreate<T> {

    protected DeviceActionStatus status;

    protected Long occurredAt;

    protected Integer code;

    protected List<@ValidString String> messages;

    protected Long actionId;

    protected String errorCode;
    protected String userAcceptanceMessageJob1;

    public Long getActionId() {
        return actionId;
    }

    public T status(final DeviceActionStatus status) {
        this.status = status;

        return (T) this;
    }

    public T occurredAt(final long occurredAt) {
        this.occurredAt = occurredAt;

        return (T) this;
    }

    public T code(final int code) {
        this.code = code;

        return (T) this;
    }

    public T messages(final Collection<String> messages) {
        if (this.messages == null) {
            this.messages = messages.stream().map(StringUtils::trimWhitespace).collect(Collectors.toList());
        } else {
            this.messages.addAll(messages.stream().map(StringUtils::trimWhitespace).collect(Collectors.toList()));
        }

        return (T) this;
    }

    public T errorCode(final String errorCode) {
        this.errorCode = errorCode;
        return (T) this;
    }

    public T userAcceptanceMessageJob1(final String userAcceptanceMessageJob1) {
        this.userAcceptanceMessageJob1 = userAcceptanceMessageJob1;
        return (T) this;
    }

    public T message(final String message) {
        if (this.messages == null) {
            this.messages = new ArrayList<>();
        }
        this.messages.add(StringUtils.trimWhitespace(message));

        return (T) this;
    }

    public Optional<Long> getOccurredAt() {
        return Optional.ofNullable(occurredAt);
    }

}
