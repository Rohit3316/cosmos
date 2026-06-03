/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Meta data for entities, a (key/value) store.
 *
 */
public interface TargetSoftware  extends SoftwareOfTarget{
    /**
     * Maximum length of targetSoftware key.
     */
    int KEY_MAX_SIZE = 128;

    /**
     * Maximum length of targetSoftware value.
     */
    int VALUE_MAX_SIZE = 128;


    Target getTarget();


    /**
     * @return {@link BaseEntity#getId()} the targetSoftware is related to
     */
    default Long getEntityId() {
        return getTarget().getId();
    }

}
