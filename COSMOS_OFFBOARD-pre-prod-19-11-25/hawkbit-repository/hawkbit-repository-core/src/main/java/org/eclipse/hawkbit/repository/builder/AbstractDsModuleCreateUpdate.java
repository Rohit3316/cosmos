/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import java.util.Optional;

/**
 * Create and update builder DTO.
 *
 * @param <T>
 *            update or create builder interface
 */
public class AbstractDsModuleCreateUpdate<T> extends AbstractBaseEntityBuilder{

    protected Integer softwareVersionId;

    protected Integer moduleId;

    protected Integer dsId;


    protected AbstractDsModuleCreateUpdate(final Integer dsId, final Integer moduleId) {
        this.moduleId = moduleId;
        this.dsId = dsId;
    }

    public T dsId(final Integer dsId) {
        this.dsId = dsId;
        return (T) this;
    }

    public T moduleId(final Integer moduleId){
        this.moduleId = moduleId;
        return (T) this;
    }

    public T softwareVersionId(final Integer softwareVersionId) {
        this.softwareVersionId = softwareVersionId;
        return (T) this;
    }


    public Optional<Integer> getSoftwareVersionId() {
        return Optional.ofNullable(softwareVersionId);
    }

    public Optional<Integer> getModuleId() {
        return Optional.ofNullable(moduleId);
    }

    public Optional<Integer> getDsId() {
        return Optional.ofNullable(dsId);
    }

}
