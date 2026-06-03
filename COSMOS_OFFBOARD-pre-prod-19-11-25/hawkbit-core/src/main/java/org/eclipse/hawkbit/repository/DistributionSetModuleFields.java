/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.List;

/**
 * Describing the fields of the DistributionSetType model which can be used in
 * the REST API e.g. for sorting etc.
 *
 *
 *
 *
 */
public enum DistributionSetModuleFields implements FieldNameProvider {

    DSTYPE("dsType" ),

    SOFTWAREVERSIONID( "sm",SoftwareModuleFields.ID.getFieldName(), SoftwareModuleFields.NAME.getFieldName());

    private final String fieldName;
    private List<String> subEntityAttributes;


    private DistributionSetModuleFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    private DistributionSetModuleFields(final String fieldName, final String... subEntityAttributes) {
        this.fieldName = fieldName;
        this.subEntityAttributes = Arrays.asList(subEntityAttributes);
    }

    @Override
    public List<String> getSubEntityAttributes() {
        return subEntityAttributes;
    }


    @Override
    public String getFieldName() {
        return fieldName;
    }
}
