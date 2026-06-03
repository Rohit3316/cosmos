/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.Set;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.jpa.model.TargetSoftwareCompositeKey;
import org.eclipse.hawkbit.repository.model.Target;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * {@link org.eclipse.hawkbit.repository.model.TargetSoftware} repository.
 */
@Transactional(readOnly = true)
public interface TargetSoftwareRepository
        extends JpaRepository<JpaTargetSoftware, TargetSoftwareCompositeKey>,
        JpaSpecificationExecutor<JpaTargetSoftware> {

    /**
     * Counts the target software entries that match the given target ID.
     * 
     * @param id
     *            of the target.
     * 
     * @return The number of matching target softwareentries.
     */
    long countByTargetId(@Param("id") Long id);

    @Transactional
    @Modifying
    @Query("SELECT ts FROM JpaTargetSoftware ts WHERE ts.target.id = :targetId")
    List<JpaTargetSoftware> getByTargetId(@Param("targetId") Long targetId);

    /**
     * Retrieves a set of {@link JpaTargetSoftware} entities that match the given target ID and a set of component IDs.
     *
     * @param targetId The target ID to match.
     * @param componentIds The set of component IDs to match.
     * @return A set of {@link JpaTargetSoftware} entities that match the given target ID and component IDs.
     */
    @Transactional
    @Modifying
    @Query("SELECT ts FROM JpaTargetSoftware ts WHERE ts.target = :targetId AND ts.componentId IN :componentIds")
    Set<JpaTargetSoftware> getByTargetIdAndComponentIds(@Param("targetId") Target targetId,
            @Param("componentIds") Set<String> componentIds);

}

