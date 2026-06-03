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
 * Repository model constants.
 *
 */
public final class RepositoryModelConstants {

    public static final Long NO_FORCE_TIME = 0L;

    /**
     * default version for software module.
     * case of {@link SoftwareModule#getVersion()}.
     */
    public static final String DEFAULT_SM_VERSION = "0";

    private RepositoryModelConstants() {
        // Utility class.
    }

}
