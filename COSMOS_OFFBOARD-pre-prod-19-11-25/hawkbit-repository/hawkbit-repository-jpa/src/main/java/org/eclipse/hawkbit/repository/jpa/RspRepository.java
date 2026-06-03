package org.eclipse.hawkbit.repository.jpa;

import java.util.List;
import java.util.Optional;
import jdk.jfr.Description;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RspRepository extends JpaRepository<JpaRsp, Long>, JpaSpecificationExecutor<JpaRsp> {

    /**
     * fina distinct Rsp for given fileType, rollout and sha256
     *
     * @param fileType the fileType of rsp
     * @param rollout  The ID of the rollout to find Rsp entities
     * @param sha256   the sha256 to check other existing rspRollouts
     * @return list of Rsp
     */
    @Query("SELECT DISTINCT sr " +
            "FROM JpaRsp sr " +
            "JOIN sr.rspRollouts se " +
            "WHERE se.rollout = :rollout AND sr.fileType = :fileType AND sr.sha256Hash NOT LIKE :sha256")
    @Description("find distinct Rsp by fileType and rollout with different sha256Hash")
    Optional<List<Rsp>> findListOfRspWithSha256HashNotLike(
            @Param("fileType") MgmtSupportPackageFileType fileType,
            @Param("rollout") Rollout rollout,
            @Param("sha256") String sha256);

    Optional<Rsp> findBySha256HashIgnoreCase(String sha256);

    /**
     * Finds all Rsp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to find Rsp entities for.
     * @return A list of Rsp entities associated with the given rollout ID.
     */
    @Query("SELECT sr FROM JpaRsp sr " +
            "JOIN sr.rspRollouts se " +
            "WHERE se.rollout.id = :rolloutId ")
    List<Rsp> findRSPSupportPackagesByRolloutId(@Param("rolloutId") Long rolloutId);

    /**
     * Counts the number of Rsp entities associated with a given rollout ID and file type.
     *
     * @param rolloutId The ID of the rollout to count Rsp entities for.
     * @param fileType  The file type to filter by.
     * @return The count of Rsp entities associated with the given rollout ID and file type.
     */
    long countByRspRolloutsRolloutIdAndFileType(Long rolloutId, MgmtSupportPackageFileType fileType);

    /**
     * Retrieves a paginated list of Rsp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to find Rsp entities for.
     * @param pageable  The pagination information, including page number and size.
     * @return A Page containing Rsp entities associated with the given rollout ID.
     */
    @Query("SELECT sr " +
            "FROM JpaRsp sr " +
            "JOIN sr.rspRollouts se " +
            "WHERE se.rollout.id = :rolloutId")
    Page<Rsp> findRSPSupportPackagesByRolloutId(@Param("rolloutId") Long rolloutId, Pageable pageable);


    /**
     * Finds all Rsp entities associated with a given rollout ID and support package ID.
     *
     * @param rolloutId        The ID of the rollout to find Rsp entities for.
     * @param supportPackageId The ID of the support package to find Rsp entities for.
     * @return A list of Rsp entities associated with the given rollout ID and support package ID.
     */
    @Query("SELECT sr " +
            "FROM JpaRsp sr " +
            "JOIN sr.rspRollouts se " +
            "WHERE se.rollout.id = :rolloutId AND sr.id = :supportPackageId")
    List<Rsp> findRSPSupportPackagesByPackageId(@Param("rolloutId") Long rolloutId,
                                                @Param("supportPackageId") Long supportPackageId);

    /**
     * Counts the number of Rsp entities associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout to count Rsp entities for.
     * @return The count of Rsp entities associated with the given rollout ID.
     */
    long countByRspRolloutsRolloutId(Long rolloutId);

}
