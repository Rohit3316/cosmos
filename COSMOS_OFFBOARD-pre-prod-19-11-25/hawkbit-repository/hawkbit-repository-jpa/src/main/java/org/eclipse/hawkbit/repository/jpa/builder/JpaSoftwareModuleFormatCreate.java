/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.builder.AbstractSoftwareModuleTypeUpdateCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleFormatCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModuleFormat;

/**
 * Create/build implementation.
 *
 *
 */
public class JpaSoftwareModuleFormatCreate extends AbstractSoftwareModuleTypeUpdateCreate<SoftwareModuleFormatCreate>
        implements SoftwareModuleFormatCreate {



    @Override
    public JpaSoftwareModuleFormat build() {
        return new JpaSoftwareModuleFormat(key, name, description);
    }


}
