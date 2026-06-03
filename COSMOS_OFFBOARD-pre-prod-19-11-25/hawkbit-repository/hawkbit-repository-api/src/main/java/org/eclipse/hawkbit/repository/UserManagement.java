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
import org.eclipse.hawkbit.repository.builder.UserCreate;
import org.eclipse.hawkbit.repository.builder.UserUpdate;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.User;
import org.springframework.security.access.prepost.PreAuthorize;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * Management service for {@link User}s.
 *
 */
public interface UserManagement {

    /**
     * creating a new {@link User}.
     *
     * @param create
     *            to be created
     * @return the created {@link User}
     * 
     * @throws EntityAlreadyExistsException
     *             given target already exists.
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link UserCreate} for field constraints.
     *
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    User create(@NotNull @Valid UserCreate create);

    /**
     * updates the {@link User}.
     *
     * @param update
     *            to be updated
     *
     * @return the updated {@link User}
     *
     * @throws EntityNotFoundException
     *             if given target does not exist
     * @throws ConstraintViolationException
     *             if fields are not filled as specified. Check
     *             {@link UserUpdate} for field constraints.
     */
    @PreAuthorize(SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    User update(@NotNull @Valid UserUpdate update);

    User getUserByIdNoPermission(@NotNull @Valid long userId);



}
