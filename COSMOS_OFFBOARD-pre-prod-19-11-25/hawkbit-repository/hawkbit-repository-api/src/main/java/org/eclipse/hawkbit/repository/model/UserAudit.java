package org.eclipse.hawkbit.repository.model;

/**
 * User Audit
 */
public interface UserAudit extends BaseEntity {

    /**
     * {@link UserAudit} user
     *
     * @return User
     */
    User getUser();

    /**
     * {@link UserAudit} role
     *
     * @return Role
     */
    Role getRole();

    /**
     * {@link UserAudit} tenant
     *
     * @return TenantMetaData
     */
    TenantMetaData getTenant();
}
