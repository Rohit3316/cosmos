package org.eclipse.hawkbit.repository.jpa;

import java.util.Optional;
import org.eclipse.hawkbit.repository.jpa.model.JpaDeploymentLog;
import org.eclipse.hawkbit.repository.model.DeploymentLog;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeploymentLog} repository.
 */
@Transactional(readOnly = true)
public interface DeploymentLogRepository extends BaseEntityRepository<JpaDeploymentLog, Long> {

    @Query("SELECT SUM(la.fileSize) FROM JpaDeploymentLog la")
    Optional<Long> getSumOfUndeletedDeploymentLogSize();

    @Query("SELECT COUNT(a) FROM JpaDeploymentLog a WHERE a.action = :actionId")
    Optional<Long> findDeploymentLogCountbyActionId(@Param("actionId") Long actionId);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM JpaDeploymentLog a WHERE a.action = :actionId AND a.fileName = :fileName AND a.sequence = :sequence")
    boolean deploymentLogExistsForActionIdFileNameAndSequence(@Param("actionId") Long actionId,
                                                              @Param("fileName") String fileName,
                                                              @Param("sequence") Integer sequence);
}
