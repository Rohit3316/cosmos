package org.eclipse.hawkbit.repository.jpa.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.UserAudit;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

/**
 * JPA implementation of a {@link UserAudit}.
 */
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "sp_user_audit", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "role_id", "tenant_id"},
        name = "uk_user_role_tenant"))
public class JpaUserAudit extends AbstractJpaBaseEntity implements UserAudit {

    private static final long serialVersionUID = 1L;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_audit_user"))
    @NotNull
    private JpaUser user;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "role_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_audit_role"))
    @NotNull
    private JpaRole role;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_audit_tenant"))
    @NotNull
    private JpaTenantMetaData tenant;

    @Column(name = "deleted_at")
    private long deletedAt;
}
