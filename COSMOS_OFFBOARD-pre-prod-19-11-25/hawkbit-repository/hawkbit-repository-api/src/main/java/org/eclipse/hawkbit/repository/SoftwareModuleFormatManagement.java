/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatUpdate;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing {@link SoftwareModuleFormat}s.
 *
 */
public interface SoftwareModuleFormatManagement
        extends RepositoryManagement<SoftwareModuleFormat, SoftwareModuleFormatCreate, SoftwareModuleFormatUpdate> {

    /**
     *
     * @param key
     *            to search for
     * @return {@link SoftwareModuleFormat} in the repository with given
     *         {@link SoftwareModuleFormat#getKey()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleFormat> getByKey(@NotEmpty String key);

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleFormat}s in the repository with given
     *         {@link SoftwareModuleFormat#getName()}
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Optional<SoftwareModuleFormat> getByName(@NotEmpty String name);

    /**
     * Finds all {@link SoftwareModuleFormat}s in the repository with the given names.
     *
     * @param names the list of names to search for
     * @return an {@link Optional} containing a list of {@link SoftwareModuleFormat}s with the given names, or an empty {@link Optional} if none are found
     */
    Optional<List<SoftwareModuleFormat>> findBykeyIn(@NotEmpty List<String> names);
    /**
     * Creates a new {@link List<SoftwareModuleFormat>} in the repository.
     *
     * @param moduleFormatList the {@link List<SoftwareModuleFormat>} to be created
     */
    void create(List<SoftwareModuleFormat> moduleFormatList);
}
