/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.exception;

/**
 * Exception used by the REST API in case of invalid sort parameter syntax.
 * 
 *
 *
 *
 */
public class SortParameterSyntaxErrorException extends AbstractServerRtException {

    /**
    * 
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new SortParameterSyntaxErrorException with
     * {@link ServerError#REST_SORT_PARAM_SYNTAX} error.
     */
    public SortParameterSyntaxErrorException() {
        super(ServerError.REST_SORT_PARAM_SYNTAX);
    }
}
