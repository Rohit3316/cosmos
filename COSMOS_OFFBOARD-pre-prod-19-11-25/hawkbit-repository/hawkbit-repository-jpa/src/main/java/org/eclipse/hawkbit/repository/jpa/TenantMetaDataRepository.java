/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * repository for operations on {@link TenantMetaData} entity.
 *
 */
@Transactional(readOnly = true)
public interface TenantMetaDataRepository extends JpaRepository<JpaTenantMetaData, Long>, JpaSpecificationExecutor<JpaTenantMetaData> {

    /**
     * Search {@link TenantMetaData} by tenant name.
     *
     * @param tenant
     *            to search for
     * @return found {@link TenantMetaData} or <code>null</code>
     */
    JpaTenantMetaData findByTenantIgnoreCase(String tenant);

    @Override
    List<JpaTenantMetaData> findAll();

    /**
     * Find all {@link TenantMetaData} by tenant names.
     *
     * @param tenants name
     *            to search for
     * @return found {@link TenantMetaData} or <code>null</code>
     */
    @Query("SELECT t FROM JpaTenantMetaData t WHERE t.tenant IN ?1")
    List<JpaTenantMetaData> findAllByTenantNames(Iterable<String> tenants);

    /**
     * @param tenant
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM JpaTenantMetaData t WHERE UPPER(t.tenant) = UPPER(:tenant)")
    void deleteByTenantIgnoreCase(@Param("tenant") String tenant);

    @Override
    // Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
    @Query("SELECT d FROM JpaTenantMetaData d WHERE d.id IN ?1")
    List<JpaTenantMetaData> findAllById(Iterable<Long> ids);

    /**
     * Custom query to find JpaTenantMetaData by ID.
     * Workaround for https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477
     */
    @Override
    @Query("SELECT d FROM JpaTenantMetaData d WHERE d.id = ?1")
    Optional<JpaTenantMetaData> findById(Long id);

    /**
     * Count number of tenants in the repository.
     * <p>
     * Calls version with an empty spec to allow injecting further specs
     *
     * @return number of tenants in the repository
     */
    @Override
    default long count() {
        return this.count(Specification.where(null));
    }
}
