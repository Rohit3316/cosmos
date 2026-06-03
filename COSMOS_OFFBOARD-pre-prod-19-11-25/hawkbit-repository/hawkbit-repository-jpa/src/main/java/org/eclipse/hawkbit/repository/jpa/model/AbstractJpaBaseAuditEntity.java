/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.hawkbit.im.authentication.TenantAwareAuthenticationDetails;
import org.eclipse.hawkbit.repository.exception.TenantNotExistException;
import org.eclipse.hawkbit.repository.jpa.model.helper.TenantAwareHolder;
import org.eclipse.hawkbit.repository.model.BaseAuditEntity;
import org.eclipse.hawkbit.repository.model.helper.SystemManagementHolder;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;

/**
 * Holder of the base attributes common to all audit entities.
 *
 */
@MappedSuperclass
@Access(AccessType.FIELD)
@EntityListeners({ AuditingEntityListener.class, EntityPropertyChangeListener.class, EntityInterceptorListener.class })
@Setter
@Getter
public abstract class AbstractJpaBaseAuditEntity implements BaseAuditEntity {
    private static final long serialVersionUID = 1L;
    private static final long MILLISECONDS_THRESHOLD = 1_000_000_000_000L;
    protected static final int USERNAME_FIELD_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant", nullable = false, length = 40)
    @Size(min = 1, max = 40)
    @NotNull
    private String tenant;

    private String createdBy;
    private String lastModifiedBy;
    private long createdAt;
    private long lastModifiedAt;

    @Version
    @Column(name = "optlock_revision")
    private int optLockRevision;

    /**
     * Default constructor needed for JPA entities.
     */
    protected AbstractJpaBaseAuditEntity() {
        // Default constructor needed for JPA entities.
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_at", nullable = false)
    public long getCreatedAt() {
        return createdAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "created_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_at", nullable = false)
    public long getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    @Access(AccessType.PROPERTY)
    @Column(name = "last_modified_by", nullable = false, length = USERNAME_FIELD_LENGTH)
    public String getLastModifiedBy() {
        return lastModifiedBy;
    }

    /**
     * PrePersist listener method for all {@link TenantAwareBaseEntity}
     * entities.
     */
    @PrePersist
    void prePersist() {
        // before persisting the entity check the current ID of the tenant by
        // using the TenantAware
        // service
        final String currentTenant = SystemManagementHolder.getInstance().currentTenant();
        if (currentTenant == null) {
            throw new TenantNotExistException("Tenant " + TenantAwareHolder.getInstance().getTenantAware().getCurrentTenant() + " does not exists, cannot create entity " + this.getClass());
        }
        setTenant(currentTenant.toUpperCase());
    }

    @Override
    public String getTenant() {
        return tenant;
    }

    @LastModifiedDate
    public void setLastModifiedAt(final long lastModifiedAt) {

        if (isController()) {
            return;
        }

        //check if lastModifiedAt is in milliseconds, if so convert to seconds
        this.lastModifiedAt = (lastModifiedAt > MILLISECONDS_THRESHOLD) ? lastModifiedAt / 1000 : lastModifiedAt;
    }

    @LastModifiedBy
    public void setLastModifiedBy(final String lastModifiedBy) {
        if (isController()) {
            return;
        }

        this.lastModifiedBy = lastModifiedBy;
    }

    @Override
    public int getOptLockRevision() {
        return optLockRevision;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " [id=" + id + "]";
    }

    private boolean isController() {
        return SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication()
                .getDetails() instanceof TenantAwareAuthenticationDetails
                && ((TenantAwareAuthenticationDetails) SecurityContextHolder.getContext().getAuthentication()
                .getDetails()).isController();
    }

    /**
     * Defined equals/hashcode strategy for the repository in general is that an
     * entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
     * 
     * @see Object#hashCode()
     */
    @Override
    // Exception squid:S864 - generated code
    @SuppressWarnings({ "squid:S864" })
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (id == null ? 0 : id.hashCode());
        result = prime * result + optLockRevision;
        result = prime * result + this.getClass().getName().hashCode();
        return result;
    }

    /**
     * Defined equals/hashcode strategy for the repository in general is that an
     * entity is equal if it has the same {@link #getId()} and
     * {@link #getOptLockRevision()} and class.
     * 
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(this.getClass().isInstance(obj))) {
            return false;
        }
        final AbstractJpaBaseAuditEntity other = (AbstractJpaBaseAuditEntity) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        return optLockRevision == other.optLockRevision;
    }

}
