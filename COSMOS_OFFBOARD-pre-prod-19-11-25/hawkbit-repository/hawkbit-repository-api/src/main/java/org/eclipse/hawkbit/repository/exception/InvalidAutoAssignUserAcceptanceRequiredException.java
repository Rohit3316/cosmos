/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.exception.ServerError;

/**
 * Thrown if an user acceptance required for auto-assignment is neither 'no', nor 'yes'.
 */
public class InvalidAutoAssignUserAcceptanceRequiredException extends AbstractServerRtException {

    private static final long serialVersionUID = 1L;
    private static final ServerError THIS_ERROR = ServerError.AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED_INVALID;

    /**
     * Default constructor.
     */
    public InvalidAutoAssignUserAcceptanceRequiredException() {
        super(THIS_ERROR);
    }
}
