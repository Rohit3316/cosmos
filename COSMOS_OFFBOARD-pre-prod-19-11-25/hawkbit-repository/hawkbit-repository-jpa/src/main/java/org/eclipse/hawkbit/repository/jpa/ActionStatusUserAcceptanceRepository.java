package org.eclipse.hawkbit.repository.jpa;


import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusUserAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link JpaActionStatusUserAcceptance}.
 */
@Transactional(readOnly = true)
public interface ActionStatusUserAcceptanceRepository extends JpaRepository<JpaActionStatusUserAcceptance, Long> {
}
