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
 * Software package as sub element of a {@link DistributionSet}.
 *
 */
public class DistributionSoftwareVersionWrapper  {

    private SoftwareModule softwareModule;
    private Version version;

    public DistributionSoftwareVersionWrapper(SoftwareModule softwareModule, Version version) {
        this.softwareModule = softwareModule;
        this.version = version;
    }

    public SoftwareModule getSoftwareModule() {
        return softwareModule;
    }

    public void setSoftwareModule(SoftwareModule softwareModule) {
        this.softwareModule = softwareModule;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }
}
