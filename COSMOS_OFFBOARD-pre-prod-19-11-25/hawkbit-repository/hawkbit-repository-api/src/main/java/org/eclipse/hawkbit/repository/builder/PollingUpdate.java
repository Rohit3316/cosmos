/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.model.Polling;
import org.eclipse.hawkbit.repository.model.Target;

import jakarta.validation.constraints.NotNull;

/**
 * Builder to update an existing {@link Polling} entry. Defines all fields that
 * can be updated.
 *
 */
public interface PollingUpdate {


    /**
     * @param status
     *            for {@link Target#getUpdateStatus()}
     * @return updated builder instance
     */
    PollingUpdate status(@NotNull Polling.Status status);
}
