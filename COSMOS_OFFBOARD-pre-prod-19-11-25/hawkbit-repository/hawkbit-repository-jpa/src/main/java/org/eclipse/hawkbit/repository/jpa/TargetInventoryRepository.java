/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInventory;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link JpaTargetInventory} repository.
 */
@Transactional(readOnly = true)
public interface TargetInventoryRepository extends JpaRepository<JpaTargetInventory, Long> {
    /**
     * Find {@link JpaTargetInventory} by {@link JpaTarget}
     *
     * @param target {@link JpaTarget} the target of inventory
     * @return {@link TargetInventory}
     */
    List<TargetInventory> findByTarget(JpaTarget target);
    /**
     * Finds {@link TargetInventory} records for the given controller ID, ordered by ID descending.
     * Returns the newest inventories first, with results paginated using the given {@link Pageable}.
     *
     * @param controllerId controller ID to filter by
     * @param pageable pagination configuration
     * @return page of {@link TargetInventory} records
     */
    @Query("SELECT j FROM JpaTargetInventory j WHERE j.target.controllerId = :controllerId ORDER BY j.id DESC")
    Page<TargetInventory> findByTargetInventoryInDesc(@Param("controllerId") String controllerId, Pageable pageable);

    /**
     * Counts the number of {@link JpaTargetInventory} records for the given controller ID.
     *
     * @param controllerId the controller ID to filter by
     * @return the total count of matching inventory records
     */

    @Query("SELECT COUNT(i) FROM JpaTargetInventory i WHERE i.target.controllerId = :controllerId")
    long countTargetInventory(@Param("controllerId") String controllerId);
}

