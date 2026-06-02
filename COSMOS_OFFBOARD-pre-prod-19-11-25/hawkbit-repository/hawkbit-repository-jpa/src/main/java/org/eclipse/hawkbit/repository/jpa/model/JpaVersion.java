/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.User;
import org.eclipse.hawkbit.repository.model.Version;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

/**
 * JPA implementation of {@link User}.
 */
@Entity
@Table(name = "sp_software_versions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"name", "software_module_id"}, name = "unique_constraint_name_sid"),
        @UniqueConstraint(columnNames = {"number", "software_module_id"}, name = "unique_constraint_number_sid"),
})
public class JpaVersion extends AbstractJpaBaseEntity implements Version {

    private static final long serialVersionUID = 1L;

    @Column(name = "name", length = Version.NAME_MAX_SIZE)
    @NotNull
    private String name;

    @Column(name = "description", length = Version.DESCRIPTION_MAX_SIZE)
    private String description;

    @Column(name = "number", length = Version.NUMBER_MAX_SIZE)
    @NotNull
    private Integer number;

    @ManyToOne(optional = false, cascade = {CascadeType.PERSIST}, fetch = FetchType.LAZY)
    @JoinColumn(name = "software_module_id", referencedColumnName = "id", nullable = false,
            updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_version_sm"))
    private JpaSoftwareModule softwareModuleId;


    /**
     * Constructor.
     *
     * @param name           username of the {@link Version}
     * @param description    password of the {@link Version}
     * @param number         firstname of the {@link Version}
     * @param softwareModule lastname of the {@link Version}
     */
    public JpaVersion(String name, String description, Integer number, SoftwareModule softwareModule) {
        this.name = name;
        this.description = description;
        this.number = number;
        this.softwareModuleId = (JpaSoftwareModule) softwareModule;
    }


    public JpaVersion() {
    }

    public JpaSoftwareModule getSoftwareModuleId() {
        return softwareModuleId;
    }

    public void setSoftwareModuleId(JpaSoftwareModule softwareModuleId) {
        this.softwareModuleId = softwareModuleId;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    /**
     * @param description
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    @Override
    public Integer getNumber() {
        return number;
    }

    /**
     * @param number
     */
    public void setNumber(final Integer number) {
        this.number = number;
    }

    @Override
    public String toString() {
        return "JpaVersion{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", number=" + number +
                ", softwareModuleId=" + softwareModuleId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        JpaVersion that = (JpaVersion) o;
        return Objects.equals(name, that.name) && Objects.equals(description, that.description) && Objects.equals(number, that.number) && Objects.equals(softwareModuleId, that.softwareModuleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), name, description, number, softwareModuleId);
    }
}
