/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.tenancy.configuration;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

/**
 * The {@link #InvalidTenantConfigurationKeyException} is thrown when an invalid
 * configuration key is used.
 *
 */
public class InvalidTenantConfigurationKeyException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final ServerError THIS_ERROR = ServerError.CONFIGURATION_KEY_INVALID;

    /**
     * Default constructor.
     */
    public InvalidTenantConfigurationKeyException() {
        super(THIS_ERROR);
    }

    /**
     * Parameterized constructor.
     * 
     * @param cause
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final Throwable cause) {
        super(THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     * @param cause
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final String message, final Throwable cause) {
        super(message, THIS_ERROR, cause);
    }

    /**
     * Parameterized constructor.
     * 
     * @param message
     *            of the exception
     */
    public InvalidTenantConfigurationKeyException(final String message) {
        super(message, THIS_ERROR);
    }

}
