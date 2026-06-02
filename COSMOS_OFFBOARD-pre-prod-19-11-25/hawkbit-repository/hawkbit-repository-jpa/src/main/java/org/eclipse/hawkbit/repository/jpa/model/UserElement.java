/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.UserElementMgmt;
import org.eclipse.persistence.annotations.CascadeOnDelete;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * JPA implementation of {@link }.
 *
 */
@Entity
@Table(name = "sp_user_tenant")
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class UserElement extends AbstractJpaBaseEntity implements UserElementMgmt {

    private static final long serialVersionUID = 1L;


    @CascadeOnDelete
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_tenant_user"))
    private JpaUser user;

    @CascadeOnDelete
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_tenant_tenant"))
    private JpaTenantMetaData tenant;

    /**
     * default constructor for JPA.
     */
    public UserElement() {
        // JPA constructor
    }

    public UserElement(User user, TenantMetaData tenant) {
        super();
        this.user = (JpaUser) user;
        this.tenant = (JpaTenantMetaData) tenant;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = (JpaUser) user;
    }

    public JpaTenantMetaData getTenant() {
        return tenant;
    }

    public void setTenant(TenantMetaData tenant) {
        this.tenant = (JpaTenantMetaData) tenant;
    }
}
