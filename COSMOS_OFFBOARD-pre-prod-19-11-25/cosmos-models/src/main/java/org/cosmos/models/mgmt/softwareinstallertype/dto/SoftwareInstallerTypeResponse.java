/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.cosmos.models.mgmt.softwareinstallertype.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import jakarta.validation.constraints.NotNull;

/**
 * Response body of GET Software Installer Types.
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class SoftwareInstallerTypeResponse {

    @JsonProperty("id")
    @NotNull
    private int id;

    @JsonProperty("name")
    @NotNull
    private String name;

    @JsonProperty("description")
    @NotNull
    private String description;

}
