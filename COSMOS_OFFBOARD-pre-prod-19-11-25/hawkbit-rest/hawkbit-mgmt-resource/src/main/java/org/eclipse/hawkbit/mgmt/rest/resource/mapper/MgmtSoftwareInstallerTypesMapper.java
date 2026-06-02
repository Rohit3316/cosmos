/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */


package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.softwareinstallertype.dto.SoftwareInstallerTypeResponse;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;

import java.util.List;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtSoftwareInstallerTypesMapper {

    private MgmtSoftwareInstallerTypesMapper() {
    }

    /**
     * This method is used to map to List of  {@link SoftwareInstallerTypeResponse}s
     * @param softwareInstallerTypes
     * @return List of SoftwareInstallerTypeResponse
     */
    public static List<SoftwareInstallerTypeResponse> toSoftwareInstallerTypeResponse(List<SoftwareInstallerType> softwareInstallerTypes) {
        return softwareInstallerTypes.stream().map(response -> SoftwareInstallerTypeResponse.builder().id(response.getId().intValue()).name(response.getName()).description(response.getDescription()).build()).toList();
    }
}
