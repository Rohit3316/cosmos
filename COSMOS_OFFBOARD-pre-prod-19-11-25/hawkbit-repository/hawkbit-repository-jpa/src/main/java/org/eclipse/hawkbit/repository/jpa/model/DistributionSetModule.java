/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;
import java.util.Objects;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * Relation element between a {@link DistributionSetType} and its
 * {@link SoftwareModuleType} elements.
 */
@Entity
@Table(name = "sp_ds_module")
public class DistributionSetModule implements IDistributionSetModule, Serializable {
    private static final long serialVersionUID = 1L;

    @EmbeddedId
    private DistributionSetModuleCompositeKey key;


    @CascadeOnDelete
    @MapsId("dsId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ds_id", nullable = false)
    private JpaDistributionSet dsSet;


    @CascadeOnDelete
    @MapsId("moduleId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private JpaSoftwareModule sm;


    @CascadeOnDelete
    @MapsId("softwareVersionId")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "software_version_id", nullable = false)
    private JpaVersion version;


    public DistributionSetModule() {
        // Default constructor for JPA
    }

    public DistributionSetModule(final JpaDistributionSet dsSet, final JpaSoftwareModule sm, final JpaVersion version) {
        this.key = new DistributionSetModuleCompositeKey(dsSet, sm, version);
        this.dsSet = dsSet;
        this.sm = sm;
        this.version = version;
    }


    public DistributionSetModuleCompositeKey getKey() {
        return key;
    }

    public JpaDistributionSet getDsSet() {
        return dsSet;
    }

    public JpaSoftwareModule getSm() {
        return sm;
    }

    public JpaVersion getVersion() {
        return version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributionSetModule that = (DistributionSetModule) o;

        if (!Objects.equals(key, that.key)) return false;
        if (!Objects.equals(dsSet, that.dsSet)) return false;
        if (!Objects.equals(sm, that.sm)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (dsSet != null ? dsSet.hashCode() : 0);
        result = 31 * result + (sm != null ? sm.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DistributionSetModule{" +
                "key=" + key +

                '}';
    }
}
