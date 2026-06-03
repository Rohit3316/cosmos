/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.softwaremodule.dto;

import java.util.List;

/**
 * Request body for target PUT/POST users.
 *
 */
public class MgmtGetVersionResponse {
    private List<VersionResponse> version;

    public List<VersionResponse> getVersion() {
        return version;
    }

    public void setVersion(List<VersionResponse> version) {
        this.version = version;
    }
}
