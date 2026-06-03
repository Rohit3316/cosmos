/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TenantAwareBaseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring data repositories for {@link TargetFilterQuery}s.
 *
 */
@Transactional(readOnly = true)
public interface TargetFilterQueryRepository
        extends BaseEntityRepository<JpaTargetFilterQuery, Long>, JpaSpecificationExecutor<JpaTargetFilterQuery> {

    /**
     * Find customer target filter by name
     *
     * @param name
     *         name of target
     * @return custom target filter
     */
    Optional<TargetFilterQuery> findByName(String name);

    /**
     * Find list of all custom target filters.
     */
    @Override
    Page<JpaTargetFilterQuery> findAll(Pageable pageable);

    /**
     * Sets the auto assign distribution sets and user acceptance required to null which
     * match the ds ids.
     *
     * @param dsIds
     *            distribution set ids to be set to null
     */
    @Modifying
    @Transactional
    @Query("update JpaTargetFilterQuery d set d.autoAssignDistributionSet = NULL, d.autoAssignUserAcceptanceRequired = NULL where d.autoAssignDistributionSet in :ids")
    void unsetAutoAssignDistributionSetAndUserAcceptanceRequired(@Param("ids") Long... dsIds);

    /**
     * Counts all target filters that have a given auto assign distribution set
     * assigned.
     *
     * @param autoAssignDistributionSetId
     *            the id of the distribution set
     * @return the count
     */
    long countByAutoAssignDistributionSetId(long autoAssignDistributionSetId);

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
    @Query("DELETE FROM JpaTargetFilterQuery t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);

    /**
     * Find all the Target by nameFilter.
     *
     * @param nameFilter
     *            to filter the data by name string
     * @param pageRequest
     *            to slice the page
     */
    Slice<TargetFilterQuery> findByNameContainingIgnoreCase(String nameFilter, Pageable pageRequest);

    /**
     * Count the Target by nameFilter.
     *
     * @param nameFilter
     *            to filter the data by name string
     */
    long countByNameContainingIgnoreCase(String nameFilter);
}
