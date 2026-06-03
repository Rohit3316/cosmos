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
 * Additional metadata to be provided for the target/device.
 * @author T7437JK,
 * @modified on 31-07-2023.
 * Extended class Hashmap for key value pairing of metadata
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiMetadata extends HashMap<String, String> {

    public DdiMetadata(){}

    public DdiMetadata(Map<String, String> metadata) {
        this.putAll(metadata);
    }
}