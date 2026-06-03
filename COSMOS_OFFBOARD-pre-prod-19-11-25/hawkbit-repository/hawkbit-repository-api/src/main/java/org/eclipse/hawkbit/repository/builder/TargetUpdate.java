/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;

/**
 * Builder to update an existing {@link Target} entry. Defines all fields that
 * can be updated.
 *
 */
public interface TargetUpdate {

    /**
     * @param name
     *            for {@link Target#getName()}
     * @return updated builder instance
     */
    TargetUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * @param serialNumber
     *            for {@link Target#getSerialNumber()} ()}
     * @return updated builder instance
     */
    TargetUpdate serialNumber(@Size(min = 1, max = Target.SERIAL_NUMBER_MAX_SIZE) @NotNull String serialNumber);

    /**
     * @param description
     *            for {@link Target#getDescription()}
     * @return updated builder instance
     */
    TargetUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * @param securityToken
     *            for {@link Target#getSecurityToken()}
     * @return updated builder instance
     */
    TargetUpdate securityToken(@Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE) @NotNull String securityToken);

    /**
     * @param targetTypeId
     *            for {@link Target#getTargetType()}
     * @return updated builder instance
     */
    TargetUpdate targetType(Long targetTypeId);

    /**
     * @param lastTargetQuery
     *            for {@link Target#getLastTargetQuery()}
     * @return updated builder instance
     */
    TargetUpdate lastTargetQuery(Long lastTargetQuery);

    /**
     * @param status
     *            for {@link Target#getUpdateStatus()}
     * @return updated builder instance
     */
    TargetUpdate status(@NotNull TargetUpdateStatus status);

    /**
     * @param requestAttributes for {@link Target#isRequestControllerAttributes()}
     * @return updated builder instance
     */
    TargetUpdate requestAttributes(Boolean requestAttributes);

    TargetUpdate vehicleModelId(Long vehicleModelId);
}
