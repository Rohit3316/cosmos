/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.im.authentication.SpPermission.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.builder.UserCreate;
import org.eclipse.hawkbit.repository.builder.VersionCreate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

/**
 * Management service for {@link org.eclipse.hawkbit.repository.model.Version}s.
 */
public interface VersionManagement {

    /**
     * creating a new {@link org.eclipse.hawkbit.repository.model.Version}.
     *
     * @param create to be created
     * @return the created {@link org.eclipse.hawkbit.repository.model.Version}
     * @throws EntityAlreadyExistsException given target already exists.
     * @throws ConstraintViolationException if fields are not filled as specified. Check
     *                                      {@link UserCreate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    Version create(@NotNull @Valid VersionCreate create);

    /**
     * @param id Software Version ID
     * @return Software Version
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Version> getById(long id);

    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void deleteVersion(Integer versionId);

    /**
     * Counts {@link Version ids}s based a given software module id
     * and source or target version
     *
     * @param softwareModuleId,
     * @param targetVersionId,
     * @param sourceVersionId   to look for.
     * @return Count of found{@link Version id}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Long countVersionsForSoftwareModuleId(SoftwareModule softwareModuleId, Long targetVersionId, Long sourceVersionId);

    /**
     * Find {@link Version}s
     *
     * @return List of found{@link Version}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<Version> findAllVersionsBySoftwareModuleId(PageRequest pageRequest, Long softwareModuleId);

    /**
     * Find {@link Version}s
     *
     * @return List of found{@link Version}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    List<Version> findVersionBySoftware(SoftwareModule software);

    /**
     * Count {@link Version}s
     *
     * @return Count of found{@link Version}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Long count(PageRequest pageRequest, Long softwareModuleId);

    /**
     * Find {@link Version}
     *
     * @param name     the {@link Version#getName()}}
     * @param number   the {@link Version#getNumber()}
     * @param moduleId {@link Version#getSoftwareModuleId()#getById(long)}
     * @return {@link Version}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY + SpringEvalExpressions.HAS_AUTH_OR
            + SpringEvalExpressions.IS_CONTROLLER)
    Optional<Version> findByNameOrNumberAndModuleId(final String name, final Integer number, final Integer moduleId);

}
