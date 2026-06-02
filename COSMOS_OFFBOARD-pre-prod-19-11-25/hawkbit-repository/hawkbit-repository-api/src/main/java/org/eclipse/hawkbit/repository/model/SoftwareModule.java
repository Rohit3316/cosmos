/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.List;
import java.util.Set;

/**
 * Software package as sub element of a {@link DistributionSet}.
 *
 */
public interface SoftwareModule extends NamedVersionedEntity {
    /**
     * Maximum length of software vendor.
     */
    int VENDOR_MAX_SIZE = 256;

    /**
     * @return set of all artifacts software module associations
     */
    Set<ArtifactSoftwareModuleAssociation> getArtifactSoftwareModuleAssociations();


    /**
     * @return the vendor of this software module
     */
    String getVendor();


    /**
     * @return the type of the software module
     */
    SoftwareModuleType getType();


    /**
     * @return the type of the software module
     */
    SoftwareModuleFormat getFormat();

    SoftwareInstallerType getSoftwareInstallerType();


    /**
     * @return {@code true} if this software module is marked as deleted
     *         otherwise {@code false}
     */
    boolean isDeleted();

    /**
     * @return immutable list of {@link DistributionSet}s the module is assigned
     *         to
     */
    List<DistributionSet> getAssignedTo();

    List<IDistributionSetModule> getDsmRelation();

    Set<EcuModel> getAssociatedEcuModels();


    /**
     * @return {@code true} if this software module is marked as encrypted
     *         otherwise {@code false}
     */
    boolean isEncrypted();

    /**
     * Get {@link SoftwareModule}'s {@link EcuModel}
     * @return Set of {@link EcuModel}
     */
    Set<EcuModel> getSoftwareEcuModels();



}
