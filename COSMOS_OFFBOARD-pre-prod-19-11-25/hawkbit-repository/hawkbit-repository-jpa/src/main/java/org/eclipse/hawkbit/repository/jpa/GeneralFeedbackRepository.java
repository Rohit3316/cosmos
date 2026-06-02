package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaGeneralFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for managing Esp entities using JPA.
 */
@Repository
public interface GeneralFeedbackRepository extends JpaRepository<JpaGeneralFeedback, Long> {

}
