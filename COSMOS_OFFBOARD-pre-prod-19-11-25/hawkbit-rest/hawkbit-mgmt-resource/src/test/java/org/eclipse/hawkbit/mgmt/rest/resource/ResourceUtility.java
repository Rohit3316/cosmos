/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.io.IOException;

import org.cosmos.models.mgmt.PagedList;
import org.eclipse.hawkbit.rest.json.model.ExceptionInfo;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility additions for the REST API tests.
 */
public final class ResourceUtility {
    private static final ObjectMapper mapper = new ObjectMapper();

    private ResourceUtility() {
    }

    static ExceptionInfo convertException(final String jsonExceptionResponse)
            throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(jsonExceptionResponse, ExceptionInfo.class);
    }

    static <T> PagedList<T> mapResponse(final String responseBody)
            throws JsonParseException, JsonMappingException, IOException {
        return mapper.readValue(responseBody, PagedList.class);
    }
}
