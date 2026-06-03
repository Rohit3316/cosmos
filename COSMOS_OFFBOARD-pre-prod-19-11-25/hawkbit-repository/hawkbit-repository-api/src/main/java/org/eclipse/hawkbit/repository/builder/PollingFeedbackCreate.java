/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.PollingFeedback;

/**
 * Builder to create a new {@link PollingFeedback} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface PollingFeedbackCreate {

    PollingFeedbackCreate feedback(String feedback);

    PollingFeedbackCreate pollingId(Long pollingId);

    /**
     * @return peek on current state of {@link PollingFeedback} in the builder
     */
    PollingFeedback build();

}
