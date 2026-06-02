/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;

import java.io.Serializable;

/**
 * Combined unique key of the table {@link JpaTargetTargetTag}.
 *
 */
public class TargetTargetTagId implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long target;
    private Long tag;

    /**
     * default constructor necessary for JPA.
     */
    public TargetTargetTagId() {
        // default constructor necessary for JPA, empty.
    }

    /**
     * Constructor.
     *
     * @param tag
     *            the rollout group for this key
     * @param target
     *            the target for this key
     */
    public TargetTargetTagId(final Target target, final TargetTag tag) {
        this.tag = tag.getId();
        this.target = target.getId();
    }

    public Long getTag() {
        return tag;
    }

    public Long getTarget() {
        return target;
    }
}
