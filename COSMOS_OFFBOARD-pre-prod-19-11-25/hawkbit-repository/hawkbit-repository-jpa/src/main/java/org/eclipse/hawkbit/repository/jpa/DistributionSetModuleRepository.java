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

import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.dto.ArtifactsExpiryDTO;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DistributionSetModule} repository.
 */
@Transactional(readOnly = true)
public interface DistributionSetModuleRepository extends JpaRepository<DistributionSetModule, Long> {

    /**
     * Query to get list of {@link DistributionSetModule} for given {@link DistributionSetModule#getDsSet()}
     *
     * @param dsSetId ID of dsSet
     * @param sort    sort
     * @return DistributionSetModule list
     */
    @Query("SELECT dsm FROM DistributionSetModule dsm WHERE dsm.dsSet.id = :dsSetId")
    List<DistributionSetModule> findByDsSet(final long dsSetId, Sort sort);

    /**
     * Query to get a list of artifacts for a distribution set that are expiring before a given date.
     *
     * @param dsSetId - distribution set id
     * @param endDate - Date, to find all the expiring artifacts before it.
     * @return a list of {@link ArtifactsExpiryDTO } for given a distribution that are expiring before
     */
    @Query(value = "SELECT file_name AS fileName, sa.expiry_date AS expiryDate, ssv.name AS versionName " +
            "FROM sp_artifact_software_module sasm " +
            "JOIN sp_ds_module sdm ON sdm.module_id = sasm.software_module_id " +
            "JOIN sp_artifacts sa ON sa.id = sasm.artifact_id " +
            "JOIN sp_software_versions ssv ON ssv.id = sasm.target_version " +
            "WHERE sdm.ds_id = ?1 " +
            "AND sa.expiry_date < ?2", nativeQuery = true)
    List<Object[]> findExpiringArtifactsForADistributionSetBeforeEndDate(final long dsSetId, final long endDate);

    /**
     * Query to retrieve a list of {@link DistributionSetModule} associated with the specified
     * {@link DistributionSetModule#getDsSet()} based on the given criteria.
     *
     * @param smId            the ID of the distribution set
     * @param targetVersionId the ID of the target software version
     * @return a list of {@link DistributionSetModule} matching the given parameters
     */
    @Query("SELECT dsm FROM DistributionSetModule dsm WHERE dsm.sm.id = :smId AND dsm.version.id =:targetVersionId")
    List<DistributionSetModule> findBySoftwareModuleIdAndTargetVersionId(final long smId, final long targetVersionId);

    /**
     * Query to retrieve a list of {@link DistributionSetModule} associated with the specified
     * {@link DistributionSetModule#getDsSet()} based on the given criteria.
     *
     * @param smId            the ID of the distribution set
     * @param targetVersionId the ID of the target software version
     * @return a list of {@link DistributionSetModule} matching the given parameters
     */
    @Query("SELECT dsm FROM DistributionSetModule dsm WHERE dsm.dsSet.id = :dsSetId AND dsm.sm.id = :smId AND dsm.version.id =:targetVersionId")
    DistributionSetModule findByDsIdAndSoftwareModuleIdAndTargetVersionId(final long dsSetId, final long smId, final long targetVersionId);

    /**
     * Fetches the module IDs and corresponding software version IDs associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout for which module and version details are to be fetched.
     * @return A list of object arrays where each array contains:
     * - Element 0: The module ID ({@code Long}).
     * - Element 1: The software version ID ({@code Long}).
     */
    @Query("SELECT dsm " +
            "FROM JpaRollout r " +
            "JOIN r.distributionSet ds " +
            "JOIN DistributionSetModule dsm ON ds.id = dsm.dsSet.id " +
            "WHERE r.id = :rolloutId")
    List<DistributionSetModule> fetchDistributionSetDetailsByRolloutId(@Param("rolloutId") Long rolloutId);

    List<DistributionSetModule> findByDsSetId(Long distributionSetId);;
}
