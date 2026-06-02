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
import java.util.List;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Builder to create a new {@link ActionStatus} entry. Defines all fields that
 * can be set at creation time. Other fields are set by the repository
 * automatically, e.g. {@link BaseEntity#getCreatedAt()}.
 *
 */
public interface ActionStatusCreate {
    /**
     * @param status
     *            {@link ActionStatus#getStatus()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate status(@NotNull DeviceActionStatus status);

    /**
     * @param occurredAt
     *            for {@link ActionStatus#getOccurredAt()}
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate occurredAt(long occurredAt);

    ActionStatusCreate code(int code);

    /**
     * @param messages it will take list of messages
     *
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate messages(Collection<String> messages);

    /**
     * @param message it will take message
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate message(String message);

    /**
     * @param errorCode error code from device.
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate errorCode(String errorCode);

    /**
     *
     * @param userAcceptanceMessageJob1 user acceptance message for job1 from device
     * @return updated {@link ActionStatusCreate} object
     */
    ActionStatusCreate userAcceptanceMessageJob1(String userAcceptanceMessageJob1);

    /**
     * @return peek on current state of {@link ActionStatus} in the builder
     */
    ActionStatus build();
}
