/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

/**
 * Composite key for {@link DistributionSetTypeElement}.
 */
@Embeddable
public class DistributionSetModuleCompositeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "ds_id", nullable = false, updatable = false)
    private Long dsId;

    @Column(name = "module_id", nullable = false, updatable = false)
    private Long moduleId;

    @Column(name = "software_version_id", nullable = false, updatable = false)
    private Long softwareVersionId;
    /**
     * Default constructor.
     */
    public DistributionSetModuleCompositeKey() {
    }

    /**
     * Constructor.
     *
     * @param ds
     *            in the key
     * @param module
     *            in the key
     */
    public DistributionSetModuleCompositeKey(final JpaDistributionSet ds, final JpaSoftwareModule module, final JpaVersion softwareVersionId) {
        this.dsId = ds.getId();
        this.moduleId = module.getId();
        this.softwareVersionId = softwareVersionId.getId();
    }

    public Long getDsId() {
        return dsId;
    }

    public void setDsId(Long dsId) {
        this.dsId = dsId;
    }

    public Long getModuleId() {
        return moduleId;
    }

    public void setModuleId(Long moduleId) {
        this.moduleId = moduleId;
    }

    public Long getSoftwareVersionId() {
        return softwareVersionId;
    }

    public void setSoftwareVersionId(Long softwareVersionId) {
        this.softwareVersionId = softwareVersionId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DistributionSetModuleCompositeKey that = (DistributionSetModuleCompositeKey) o;

        if (dsId != null ? !dsId.equals(that.dsId) : that.dsId != null) return false;
        if (moduleId != null ? !moduleId.equals(that.moduleId) : that.moduleId != null) return false;
        return softwareVersionId != null ? softwareVersionId.equals(that.softwareVersionId) : that.softwareVersionId == null;
    }

    @Override
    public int hashCode() {
        int result = dsId != null ? dsId.hashCode() : 0;
        result = 31 * result + (moduleId != null ? moduleId.hashCode() : 0);
        result = 31 * result + (softwareVersionId != null ? softwareVersionId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DistributionSetModuleCompositeKey{" +
                "dsId=" + dsId +
                ", moduleId=" + moduleId +
                ", softwareVersionId=" + softwareVersionId +
                '}';
    }
}
