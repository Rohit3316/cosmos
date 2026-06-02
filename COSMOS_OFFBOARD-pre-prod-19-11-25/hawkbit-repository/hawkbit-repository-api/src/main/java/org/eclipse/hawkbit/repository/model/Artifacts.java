/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;


import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.sns.SnsPublishable;

import java.util.Set;

/**
 * Binaries for a {@link Artifacts} Note: the decision which artifacts have
 * to be downloaded are done on the device side. e.g. Full Package, Signatures,
 * binary deltas
 */
public interface Artifacts extends TenantAwareBaseEntity, SnsPublishable {

    /**
     * @return the filename that was provided during upload.
     */
    String getFileName();

    /**
     * @return fileType
     */
    FileType getFileType();

    /**
     * @return description of the file
     */
    String getDescription();

    /**
     * Set the description of the file
     */
    void setDescription(String description);

    /**
     * @return expiry date
     */
    Long getExpiryDate();

    /**
     * @return SHA-256 hash of the artifact.
     */
    String getSha256Hash();

    /**
     * @return file size of the artifact.
     */
    Long getFileSize();

    /**
     * @return MD5 hash of the artifact.
     */
    String getMd5Hash();
    /**
     * Returns the status of the file transfer.
     *
     * @return the status of the file transfer
     */

    FileTransferStatus getFileStatus();
    /**
     * Retrieves the set of associations between this artifact and software modules.
     *
     * @return a set of {@link ArtifactSoftwareModuleAssociation} objects representing the associations.
     */
    Set<ArtifactSoftwareModuleAssociation> getArtifactSoftwareModuleAssociations();

    /**
     * @return the status of the artifact.
     */
    String getArtifactStatus();
}
