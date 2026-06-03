/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Additional Distribution set metadata to be provided for the target/device.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiDsMetadata extends HashMap<String, String> {

    public DdiDsMetadata(){}

    public DdiDsMetadata(Map<String, String> dsMetadata) {
        this.putAll(dsMetadata);
    }
}

