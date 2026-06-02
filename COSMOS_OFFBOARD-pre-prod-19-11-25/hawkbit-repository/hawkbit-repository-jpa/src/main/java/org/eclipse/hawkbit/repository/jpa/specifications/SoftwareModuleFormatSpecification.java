/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.specifications;

import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat_;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specifications class for {@link org.eclipse.hawkbit.repository.model.SoftwareModuleFormat}s. The class provides
 * Spring Data JPQL Specifications.
 */
public class SoftwareModuleFormatSpecification {

    private SoftwareModuleFormatSpecification() {
        // utility class
    }

    /**
     * {@link Specification} for retrieving {@link org.eclipse.hawkbit.repository.model.SoftwareModuleFormat}s by its
     * DELETED attribute.
     * 
     * @param isDeleted
     *            TRUE/FALSE are compared to the attribute DELETED. If NULL the
     *            attribute is ignored
     * @return the {@link org.eclipse.hawkbit.repository.model.SoftwareModuleFormat } {@link Specification}
     */
    public static Specification<JpaSoftwareModuleFormat> isDeleted(final Boolean isDeleted) {
        return (root, query, cb) -> cb.equal(root.get(JpaSoftwareModuleFormat_.deleted), isDeleted);
    }
}
