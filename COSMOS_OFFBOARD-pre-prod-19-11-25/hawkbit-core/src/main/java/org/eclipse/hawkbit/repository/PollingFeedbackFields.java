/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

/**
 * Describing the fields of the PollingFeedbackType model which can be used in
 * the REST API
 */
public enum PollingFeedbackFields implements FieldNameProvider {
    /**
     * The polling_id field.
     */
    POLLING_ID("polling_id");

    private final String fieldName;

    PollingFeedbackFields(final String fieldName) {
        this.fieldName = fieldName;
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }
}