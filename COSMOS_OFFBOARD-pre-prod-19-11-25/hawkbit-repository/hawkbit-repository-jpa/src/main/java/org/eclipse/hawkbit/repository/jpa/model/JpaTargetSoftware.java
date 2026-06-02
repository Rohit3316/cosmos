/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetSoftware;

/**
 * Meta data for {@link Target}.
 */
@IdClass(TargetSoftwareCompositeKey.class)
@Entity
@Table(name = "sp_target_software")
public class JpaTargetSoftware extends AbstractJpaTargetSoftware implements TargetSoftware {
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_software_target"))
    private JpaTarget target;

    public JpaTargetSoftware() {
        // default public constructor for JPA
    }

    /**
     * @param node
     * @param componentId
     * @param version
     */
    public JpaTargetSoftware(final String node, final String componentId, final String version) {
        super(node, componentId, version);
    }

    /**
     * @param node
     * @param componentId
     * @param version
     * @param target
     */
    public JpaTargetSoftware(final String node, final String componentId, final String version, final Target target) {
        super(node, componentId, version);
        this.target = (JpaTarget) target;
    }

    public TargetSoftwareCompositeKey getId() {
        return new TargetSoftwareCompositeKey(target.getId(), getNode(), getComponentId());
    }

    @Override
    public Target getTarget() {
        return target;
    }

    public void setTarget(final Target target) {
        this.target = (JpaTarget) target;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((target == null) ? 0 : target.hashCode());
        return result;
    }

    @Override
    // exception squid:S2259 - obj is checked for null in super
    @SuppressWarnings("squid:S2259")
    public boolean equals(final Object obj) {
        if (!super.equals(obj)) {
            return false;
        }
        final JpaTargetSoftware other = (JpaTargetSoftware) obj;
        if (target == null) {
            return other.target == null;
        } else return target.getControllerId().equals(other.target.getControllerId());
    }
}
