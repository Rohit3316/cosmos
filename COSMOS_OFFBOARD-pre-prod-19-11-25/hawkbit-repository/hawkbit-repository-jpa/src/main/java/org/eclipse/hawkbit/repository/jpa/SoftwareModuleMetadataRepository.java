/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.jpa.model.SwMetadataCompositeKey;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link SoftwareModuleMetadata} repository.
 */
@Transactional(readOnly = true)
public interface SoftwareModuleMetadataRepository
        extends JpaRepository<JpaSoftwareModuleMetadata, SwMetadataCompositeKey>,
        JpaSpecificationExecutor<JpaSoftwareModuleMetadata> {

    /**
     * Locates the meta data entries that match the given software module ID and
     * target visibility flag.
     *
     * @param page          The pagination parameters.
     * @param moduleId      The ID of the software module.
     * @param targetVisible The target visibility flag.
     * @return A {@link Page} with the matching meta data entries.
     */
    Page<JpaSoftwareModuleMetadata> findBySoftwareModuleIdAndTargetVisible(Pageable page, Long moduleId,
                                                                           boolean targetVisible);

    /**
     * Locates the meta data entries that match the given software module IDs
     * and target visibility flag.
     *
     * @param page          The pagination parameters.
     * @param moduleId      List of software module IDs.
     * @param targetVisible The target visibility flag.
     * @return A {@link Page} with the matching meta data entries.
     */
    @Query(
            value = """
                        SELECT smd.sw_id AS softwareModuleId, smd.target_visible
                        FROM sp_sw_metadata smd
                        JOIN sp_base_software_module sm ON sm.id = smd.sw_id
                        WHERE sm.id = ANY (?1)
                          AND smd.target_visible = ?2
                    """,
            countQuery = """
                        SELECT COUNT(*)
                        FROM sp_sw_metadata smd
                        JOIN sp_base_software_module sm ON sm.id = smd.sw_id
                        WHERE sm.id = ANY (?1)
                          AND smd.target_visible = ?2
                    """,
            nativeQuery = true
    )
    Page<Object[]> findBySoftwareModuleIdInAndTargetVisible(Pageable page,
                                                            @Param("moduleId") Long[] moduleId,
                                                            @Param("targetVisible") boolean targetVisible);


    /**
     * Counts the meta data entries that are associated with the addressed
     * software module.
     *
     * @param moduleId The ID of the software module.
     * @return The number of meta data entries associated with the software
     * module.
     */
    long countBySoftwareModuleId(@Param("moduleId") Long moduleId);

}
