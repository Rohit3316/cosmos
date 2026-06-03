/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseAuditEntity;
import org.eclipse.hawkbit.repository.model.BaseAuditEntity;
import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Command repository operations for all {@link BaseAuditEntity}s.
 *
 * @param <T>
 *            type if the entity type
 * @param <I>
 *            of the entity type
 */
@NoRepositoryBean
@Transactional(readOnly = true)
public interface BaseAuditEntityRepository<T extends AbstractJpaBaseAuditEntity, I extends Serializable>
        extends JpaRepository<T, I>, NoCountSliceRepository<T> {

    /**
     * Retrieves an {@link BaseEntity} by its id.
     * 
     * @param id
     *            to search for
     * @return {@link BaseEntity}
     */
    Optional<T> findById(I id);

    /**
     * Overrides
     * {@link org.springframework.data.repository.CrudRepository#saveAll(Iterable)}
     * to return a list of created entities instead of an instance of
     * {@link Iterable} to be able to work with it directly in further code
     * processing instead of converting the {@link Iterable}.
     *
     * @param entities
     *            to persist in the database
     * @return the created entities
     */
    @Override
    @Transactional
    <S extends T> List<S> saveAll(Iterable<S> entities);
}
