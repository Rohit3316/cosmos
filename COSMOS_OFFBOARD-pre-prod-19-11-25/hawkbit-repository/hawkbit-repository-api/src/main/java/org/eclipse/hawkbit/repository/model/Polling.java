/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import jakarta.validation.constraints.NotEmpty;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Update operations to be executed by the target.
 */
public interface Polling extends TenantAwareBaseEntity {
    /**
     * Minimum weight to indicate the priority of {@link Polling}.
     */
    int WEIGHT_MIN = 0;
    /**
     * Maximum weight to indicate the priority of {@link Polling}.
     */
    int WEIGHT_MAX = 1000;


    /**
     * @return current {@link Status} of the {@link Polling}.
     */
    Status getStatus();


    /**
     * Action status as reported by the controller.
     *
     * Be aware that JPA is persisting the ordinal number of the enum by means
     * the ordered number in the enum. So don't re-order the enums within the
     * Status enum declaration!
     *
     */
    enum Status {
        /**
         * Configuration has been polled.
         */
        POLLED,

        /**
         * Polling generated error.
         */
        ERROR,

        /**
         * Polling was successful.
         */
        SUCCESS
    }

}
