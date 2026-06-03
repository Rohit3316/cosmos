/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link SoftwareModuleFormat}.
 *
 */
@Transactional(readOnly = true)
public interface SoftwareModuleFormatRepository
        extends BaseEntityRepository<JpaSoftwareModuleFormat, Long>, JpaSpecificationExecutor<JpaSoftwareModuleFormat>{

    /**
     * @param pageable
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return found {@link SoftwareModuleFormat}s.
     */
    Page<SoftwareModuleFormat> findByDeleted(Pageable pageable, boolean isDeleted);

    /**
     * @param isDeleted
     *            to <code>true</code> if only marked as deleted have to be
     *            count or all undeleted.
     * @return number of {@link SoftwareModuleFormat}s in the repository.
     */
    Long countByDeleted(boolean isDeleted);

    /**
     *
     * @param key
     *            to search for
     * @return all {@link SoftwareModuleFormat}s in the repository with given
     *         {@link SoftwareModuleFormat#getKey()}
     */
    Optional<SoftwareModuleFormat> findByKey(String key);

    /**
     * Finds all {@link SoftwareModuleFormat}s in the repository with the given keys.
     *
     * @param keys the list of keys to search for
     * @return an {@link Optional} containing a list of {@link SoftwareModuleFormat}s with the given keys, or an empty {@link Optional} if none are found
     */
    Optional<List<SoftwareModuleFormat>> findByKeyIn(List<String> keys);

    /**
     *
     * @param name
     *            to search for
     * @return all {@link SoftwareModuleFormat}s in the repository with given
     *         {@link SoftwareModuleFormat#getName()}
     */
    Optional<SoftwareModuleFormat> findByName(String name);

    /**
     * Deletes all {@link TenantAwareBaseEntity} of a given tenant. For safety
     * reasons (this is a "delete everything" query after all) we add the tenant
     * manually to query even if this will by done by {@link EntityManager}
     * anyhow. The DB should take care of optimizing this away.
     *
     * @param tenant
     *            to delete data from
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaSoftwareModuleFormat t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaSoftwareModuleType d WHERE d.id IN ?1")
    List<JpaSoftwareModuleFormat> findAllById(Iterable<Long> ids);
}
