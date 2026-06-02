/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link Format} is an abstract definition for {@link DistributionSetType}s and
 * {@link SoftwareModuleType}s
 */
public interface Format extends NamedEntity {
    /**
     * Maximum length of key.
     */
    int KEY_MAX_SIZE = 64;

    /**
     * @return business key.
     */
    String getKey();

    /**
     * @return <code>true</code> if the type is deleted and only kept for
     *         history purposes.
     */
    boolean isDeleted();

}
