/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import jakarta.validation.constraints.NotNull;

/**
 * Service for managing {@link SoftwareInstallerType}s.
 *
 */
public interface SoftwareInstallerTypeManagement {

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareInstallerType}s in the repository
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    SoftwareInstallerType getSwInstallerTypeByName(@NotEmpty String name);

    /**
     *  Find all {@link SoftwareInstallerType}s in the repository
     *
     * @param pageable the page request to page the result
     * @return a paged result of all {@link SoftwareInstallerType}s in the repository
     */

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    List<SoftwareInstallerType> findAllSoftwareInstallerTypes(Pageable pageable);

    /** Retrieves {@link Page} of all {@link BaseEntity} of given type.
     *
     * @param pageable
     *            paging parameter
     * @return all {@link SoftwareInstallerType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<SoftwareInstallerType> findAll(@NotNull Pageable pageable);

    /**
     * @return number of {@link SoftwareInstallerType}s in the repository.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long count();

}
