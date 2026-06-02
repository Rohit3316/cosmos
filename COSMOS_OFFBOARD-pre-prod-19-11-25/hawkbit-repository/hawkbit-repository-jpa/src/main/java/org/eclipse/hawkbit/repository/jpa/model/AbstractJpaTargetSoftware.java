/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Objects;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.eclipse.hawkbit.repository.model.TargetSoftware;

/**
 * Meta data for entities.
 */
@MappedSuperclass
public abstract class AbstractJpaTargetSoftware implements TargetSoftware {
    private static final long serialVersionUID = 1L;


    @Id
    @Column(name = "node", nullable = false, length = TargetSoftware.KEY_MAX_SIZE, updatable = false)
    @Size(min = 1, max = TargetSoftware.KEY_MAX_SIZE)
    @NotNull
    private String node;

    @Id
    @Column(name = "component_id", nullable = false, length = TargetSoftware.KEY_MAX_SIZE, updatable = false)
    @Size(min = 1, max = TargetSoftware.KEY_MAX_SIZE)
    @NotNull
    private String componentId;


    @Column(name = "version", length = TargetSoftware.VALUE_MAX_SIZE)
    @Size(max = TargetSoftware.VALUE_MAX_SIZE)
    @NotNull
    private String version;

    protected AbstractJpaTargetSoftware(final String node, final String componentId, final String version) {
        this.node = node;
        this.componentId = componentId;
        this.version = version;
    }

    protected AbstractJpaTargetSoftware() {
        // Default constructor needed for JPA entities
    }

    @Override
    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    @Override
    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AbstractJpaTargetSoftware that = (AbstractJpaTargetSoftware) o;

        if (!Objects.equals(node, that.node)) return false;
        if (!Objects.equals(componentId, that.componentId)) return false;
        return Objects.equals(version, that.version);
    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
        result = 31 * result + (version != null ? version.hashCode() : 0);
        return result;
    }
}
