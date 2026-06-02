package org.eclipse.hawkbit.security.oidc.authentication;

import org.eclipse.hawkbit.repository.jpa.UserAuditRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.jpa.model.JpaUser;
import org.eclipse.hawkbit.repository.jpa.model.JpaUserAudit;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.security.oidc.authentication.model.UserTenantAuthorities;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

/**
 * class used to audit user authentication activity.
 */
public class OidcUserAuditService {

    @Autowired
    private UserAuditRepository userAuditRepository;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    /**
     * Method to audit user's revoked role and tenant from OIDC provider.
     * Looks for any audit data of the authenticated user and validate if the user still have that roles and tenants.
     * if not update the audit entry with deleted field.
     *
     * @param userTenantAuthorities UserTenantAuthorities
     */
    public void updateUserAuthenticationAudit(final UserTenantAuthorities userTenantAuthorities) {
        JpaUser user = (JpaUser) userTenantAuthorities.getUser();
        systemSecurityContext.runAsSystem(() -> {
            List<JpaUserAudit> userAudit = userAuditRepository.findByUser(user);
            if (userAudit != null && !userAudit.isEmpty()) {
                long deletedAtTime = Instant.now().getEpochSecond();
                userAudit = userAudit.stream().filter(ua -> !isUserWithTenantAndRoleExist(ua, user))
                        .map(a -> {
                            a.setDeletedAt(deletedAtTime);
                            return a;
                        }).toList();
                userAuditRepository.saveAll(userAudit);
            }
            return null;
        });
    }

    /**
     * Methos to insert audit data for user's roles and tenants.
     * Verify if audit already presented, if not insert it.
     *
     * @param user   JpaUser
     * @param role   role
     * @param tenant JpaTenantMetaData
     */
    public void insertUserAuthenticationAudit(JpaUser user, JpaRole role, JpaTenantMetaData tenant) {
        systemSecurityContext.runAsSystem(() -> {
            JpaUserAudit userAudit = userAuditRepository.findByUserAndRoleAndTenant(user, role, tenant);
            if (userAudit == null) {
                userAuditRepository.save(JpaUserAudit.builder()
                        .user(user)
                        .role(role)
                        .tenant(tenant)
                        .build());
            }
            return null;
        });
    }

    /**
     * Method to validate user has the roles and tenants audit entry.
     *
     * @param userAudit JpaUserAudit
     * @param user      JpaUser
     * @return boolean
     */
    private boolean isUserWithTenantAndRoleExist(JpaUserAudit userAudit, JpaUser user) {
        return user.getRoles() != null && user.getRoles().contains(userAudit.getRole())
                && user.getTenantMetadata() != null && user.getTenantMetadata().contains(userAudit.getTenant());
    }
}
