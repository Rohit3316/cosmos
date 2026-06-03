package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTargetTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This interface defines the repository for managing {@link JpaTargetTargetTag} entities.
 * It extends the Spring Data JPA {@link JpaRepository} interface, which provides basic CRUD operations
 * for {@link JpaTargetTargetTag} entities.
 * <p>
 * Additional query methods specific to {@link JpaTargetTargetTag} entities are also defined here,
 * such as deleting associations based on target and tag IDs.
 * </p>
 */
@Repository
public interface TargetTargetTagRepository extends JpaRepository<JpaTargetTargetTag, Long> {

    /**
     * Deletes all {@link JpaTargetTargetTag} associations for the specified target IDs and tag ID.
     * This method removes the relationships between the targets and the given tag.
     *
     * @param targetIds A list of target IDs whose associations with the tag should be deleted.
     * @param tagId The ID of the tag whose associations with the specified targets should be removed.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM JpaTargetTargetTag e WHERE e.target.id IN :targetIds AND e.tag.id = :tagId ")
    void deleteByTargetIdInAndTagId(@Param("targetIds") List<Long> targetIds, @Param("tagId") Long tagId);
}