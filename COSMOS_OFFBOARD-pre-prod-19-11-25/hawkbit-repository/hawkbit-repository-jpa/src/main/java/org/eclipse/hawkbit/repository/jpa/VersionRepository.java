package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * {@link JpaVersion} repository.
 */
@Transactional(readOnly = true)
public interface VersionRepository extends JpaRepository<JpaVersion, Long>, JpaSpecificationExecutor<JpaVersion> {

    /**
     * Counts all {@link Version id}s for a specific {@code softwareModuleId}
     *
     * @param softwareModuleId this is the software module id
     * @param targetVersionId  this is the target version id
     * @param sourceVersionId  this is the source version id
     * @return count of {@link Version id}s for specific software module id
     */
    @Query("SELECT COUNT(v.id) FROM JpaVersion v where v.softwareModuleId = :sm AND (v.id=:targetVersionId or v.id=:sourceVersionId)")
    Long countBySoftwareModuleIdAndTargetOrSourceVersionId(@Param("sm") JpaSoftwareModule softwareModuleId,
                                                           @Param("targetVersionId") Long targetVersionId, @Param("sourceVersionId") Long sourceVersionId);


    /**
     * Find {@link Version} by {@link Version#getSoftwareModuleId()}
     *
     * @param pageRequest      {@link PageRequest}
     * @param softwareModuleId {@link Version#getSoftwareModuleId()}
     * @return {@link Version}
     */
    List<JpaVersion> findBySoftwareModuleId(PageRequest pageRequest, SoftwareModule softwareModuleId);


    /**
     * Retrieves all {@link Version} for a specific {@code softwareModuleId}
     *
     * @param software
     * @return {@link Version}s for specific software module id
     */
    List<JpaVersion> findBySoftwareModuleId(JpaSoftwareModule software);


    /**
     * Count all {@link Version} for a specific {@code softwareModuleId}
     *
     * @param sm {@link Version#getSoftwareModuleId()}
     * @return Count of {@link Version}s for specific software module id
     */
    Long countBySoftwareModuleId(PageRequest pageRequest, JpaSoftwareModule sm);

    /**
     * Find {@link Version}
     *
     * @param name             {@link Version#getName()}
     * @param number           {@link Version#getNumber()}
     * @param softwareModuleId {@link Version#getSoftwareModuleId()}
     * @return {@link Version}
     */
    @Query("SELECT v from JpaVersion v where v.softwareModuleId.id = :softwareModuleId " +
            " AND (v.name = :name or v.number = :number)")
    Optional<Version> findByNameOrNumberAndSoftwareModuleIdId(@Param("name") final String name,
                                                              @Param("number") final Integer number,
                                                              @Param("softwareModuleId") final Integer softwareModuleId);
}
