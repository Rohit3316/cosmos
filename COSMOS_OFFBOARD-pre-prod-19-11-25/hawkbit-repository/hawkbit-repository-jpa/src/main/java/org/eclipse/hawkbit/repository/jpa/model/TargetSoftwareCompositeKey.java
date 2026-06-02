/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serializable;


/**
 * The Target Metadata composite key which contains the meta data key and the ID
 * of the Target itself.
 *
 */
public final class TargetSoftwareCompositeKey implements Serializable {
    private static final long serialVersionUID = 1L;

    private String node;
    private String componentId;

    private Long target;

    public TargetSoftwareCompositeKey() {
        // Default constructor for JPA.
    }

    public TargetSoftwareCompositeKey(Long target, String node, String componentId ) {
        this.target = target;
        this.node = node;
        this.componentId = componentId;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public Long getTarget() {
        return target;
    }

    public void setTarget(Long target) {
        this.target = target;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TargetSoftwareCompositeKey that = (TargetSoftwareCompositeKey) o;

        if (node != null ? !node.equals(that.node) : that.node != null) return false;
        if (componentId != null ? !componentId.equals(that.componentId) : that.componentId != null) return false;
        return target != null ? target.equals(that.target) : that.target == null;
    }

    @Override
    public int hashCode() {
        int result = node != null ? node.hashCode() : 0;
        result = 31 * result + (componentId != null ? componentId.hashCode() : 0);
        result = 31 * result + (target != null ? target.hashCode() : 0);
        return result;
    }
}
