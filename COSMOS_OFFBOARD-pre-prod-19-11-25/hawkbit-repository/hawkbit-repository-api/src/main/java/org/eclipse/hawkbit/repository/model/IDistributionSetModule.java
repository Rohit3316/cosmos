/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * {@link DistributionSetModule} is an abstract definition used in
 * {@link DistributionSetType}s and includes additional {@link SoftwareModule}
 * specific information.
 *
 */
public interface IDistributionSetModule  {


     Version getVersion();
    SoftwareModule getSm();
    DistributionSet getDsSet();
}
