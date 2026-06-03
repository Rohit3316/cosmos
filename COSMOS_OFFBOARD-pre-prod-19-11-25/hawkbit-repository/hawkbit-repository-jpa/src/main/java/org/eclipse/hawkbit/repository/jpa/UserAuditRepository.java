package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.JpaUserAudit;
import org.eclipse.hawkbit.repository.model.UserAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * The repository interface for the {@link UserAudit} model.
 */
@Repository
@Transactional(readOnly = true)
public interface UserAuditRepository extends JpaRepository<JpaUserAudit, Long> {

    List<JpaUserAudit> findByUser(final JpaUser user);

    JpaUserAudit findByUserAndRoleAndTenant(final JpaUser user, final JpaRole role, final JpaTenantMetaData tenant);
}