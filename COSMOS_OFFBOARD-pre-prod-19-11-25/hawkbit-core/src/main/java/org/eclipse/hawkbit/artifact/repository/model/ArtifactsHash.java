/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.artifact.repository.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Database representation of artifacts hash.
 */
@Getter
@Builder
@AllArgsConstructor
public class ArtifactsHash {

    /**
     * The md5 hash.
     */
    private final String md5;

    /**
     * The sha256 hash.
     */
    private final String sha256;
}
