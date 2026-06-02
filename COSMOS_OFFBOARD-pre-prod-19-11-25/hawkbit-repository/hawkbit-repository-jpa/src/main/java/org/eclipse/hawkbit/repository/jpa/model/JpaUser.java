/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * JPA implementation of {@link User}.
 *
 */
@Entity
@Table(name = "sp_user", uniqueConstraints = @UniqueConstraint(columnNames = {
                "username" }, name = "uk_username"))
@NamedEntityGraphs({
        @NamedEntityGraph(name = "User.all")
})
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaUser extends AbstractJpaBaseEntity implements User {

    private static final long serialVersionUID = 1L;

    @Column(name = "username")
    @NotNull
    private String username;


    @Column(name = "email", nullable = false)
    @NotNull
    private String email;

    @Column(name = "active")
    @ObjectTypeConverter(name = "userActive", objectType = Boolean.class, dataType = String.class, conversionValues = {
            @ConversionValue(objectValue = "true", dataValue = "TRUE"),
            @ConversionValue(objectValue = "false", dataValue = "FALSE") })
    @Convert("userActive")
    private boolean active = true;

    @Column(name = "firstname")
    @NotNull
    private String firstname;

    @Column(name = "lastname")
    @NotNull
    private String lastname;

    @CascadeOnDelete
    @OneToMany(mappedBy = "user", targetEntity = UserElement.class,orphanRemoval = true,
            fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST })
    private List<UserElement> userElements;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaRole.class, fetch = FetchType.LAZY)
    @JoinTable(name = "sp_user_role",
            joinColumns = {
                    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_role_user"))
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_user_role_role"))}
    )
    private Set<Role> roles;


    /**
     * Constructor.
     *
     * @param username
     *            username of the {@link User}
     * @param email
     *            email of the {@link User}
     * @param firstname
     *            firstname of the {@link User}
     * @param lastname
     *            lastname of the {@link User}
     *
     */
    public JpaUser(final String username, final String email, final String firstname, final String lastname) {
        this.username = username;
        this.email = email;
        this.lastname = lastname;
        this.firstname = firstname;
    }


    /**
     * Constructor
     */
    public JpaUser() {
        // empty constructor for JPA.
    }


    public JpaUser addTenant(final TenantMetaData tenantMetadata){
        return setTenant(tenantMetadata);
    }

    private JpaUser setTenant(final TenantMetaData tenantMetaData) {
        if (userElements == null) {
            userElements = new ArrayList<>();
        }
        userElements.add(new UserElement(this, tenantMetaData));
        return this;
    }

    public void removeTenantsIfNotPresent(Collection<Long> tenantIds){
        userElements.removeIf(ue -> !tenantIds.contains(ue.getTenant().getId()));
    }
    public void removeAllTenantsFromUser(){
        userElements.clear();
    }

    /**
     * @param username
     *
     */
    public void setUsername(final String username) {
        this.username = username;
    }

    /**
     * @param email
     *
     */
    public void setEmail(final String email) {
        this.email = email;
    }

    /**
     * @param active
     *
     */
    public void setActive(final boolean active) {
        this.active = active;
    }

    /**
     * @param firstname
     *
     */
    public void setFirstname(final String firstname) {
        this.firstname = firstname;
    }

    /**
     * @param lastname
     *
     */
    public void setLastname(final String lastname) {
        this.lastname = lastname;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    public void setUserElements(List<UserElement> userElements) {
        this.userElements = userElements;
    }

	@Override
	public String toString() {
		return "JpaTarget [username=" + username + ", firstname=" + firstname + ", lastname=" + lastname + " id="
				+ getId() + "]";
	}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JpaUser jpaUser = (JpaUser) o;
        return Objects.equals(username, jpaUser.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), username, email, active, firstname, lastname, userElements, roles);
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getEmail() {
        return email;
    }

    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public String getFirstname() {
        return firstname;
    }

    @Override
    public String getLastname() {
        return lastname;
    }

    @Override
    public Set<Role> getRoles() {
        return roles;
    }

    public List<UserElement> getUserElements() {
        return userElements;
    }

    @Override
    public List<TenantMetaData> getTenantMetadata() {

        if(userElements == null){
            return Collections.emptyList();
        }

        return userElements.stream().map(UserElement::getTenant).collect(Collectors.toList());
    }
}
