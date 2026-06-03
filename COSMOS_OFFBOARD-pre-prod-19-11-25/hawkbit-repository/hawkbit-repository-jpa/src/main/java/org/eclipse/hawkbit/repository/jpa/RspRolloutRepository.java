package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaRspRollout;
import org.eclipse.hawkbit.repository.model.RspRollout;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RspRolloutRepository extends JpaRepository<JpaRspRollout,Long> {

    /**
     * Deletes all {@link JpaRspRollout} entities associated with the given rollout ID and support package IDs.
     *
     * @param rolloutId         The ID of the rollout.
     * @param supportPackageIds The IDs of the support packages.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaRspRollout e WHERE e.rollout.id = :rolloutId AND e.supportPackage.id IN :supportPackageIds")
    void deleteByRolloutIdAndSupportPackageIdIn(Long rolloutId, List<Long> supportPackageIds);

    /**
     * Retrieves the number of {@link JpaRspRollout} entities associated with the given support package ID.
     *
     * @param supportPackageId The ID of the support package.
     * @return The number of {@link JpaRspRollout} entities associated with the given support package ID.
     */
    Long countBySupportPackageId(Long supportPackageId);

    RspRollout findByRolloutIdAndSupportPackageId(Long rolloutId, Long supportPackageId);

    /*
    * Clean up the table
    * */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaRspRollout t WHERE t.tenant = :tenant")
    void deleteByTenant(@Param("tenant") String tenant);
}
