/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.cosmos.models.mgmt.FileType;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.BaseEntity;

/**
 * Builder to create a new {@link Artifacts} entry. Defines all fields that can be
 * set at creation time. Other fields are set by the repository automatically,
 * e.g. {@link BaseEntity#getCreatedAt()}.
 */
public interface ArtifactsCreate {

    ArtifactsCreate fileName(String fileName);

    ArtifactsCreate fileType(FileType fileType);

    ArtifactsCreate description(String description);

    ArtifactsCreate sha256Hash(String sha256Hash);

    ArtifactsCreate fileSize(Long fileSize);

    ArtifactsCreate expiryDate(Long expiryDate);

    ArtifactsCreate md5Hash(String md5Hash);

    ArtifactsCreate artifactStatus(String artifactStatus);

    ArtifactsCreate fileStatus(String status);

    /**
     * @return peek on current state of {@link Artifacts} in the builder
     */
    Artifacts build();

}
