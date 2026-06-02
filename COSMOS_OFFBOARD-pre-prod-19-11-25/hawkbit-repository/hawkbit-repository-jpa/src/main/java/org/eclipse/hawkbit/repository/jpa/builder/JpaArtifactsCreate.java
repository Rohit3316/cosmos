/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import org.cosmos.models.mgmt.FileType;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.builder.AbstractArtifactsCreate;
import org.eclipse.hawkbit.repository.builder.ArtifactsCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;

/**
 * Create/build implementation.
 */
public class JpaArtifactsCreate extends AbstractArtifactsCreate<ArtifactsCreate> implements ArtifactsCreate {


    /**
     * Constructor
     *
     * @param artifactsManagement Artifacts management
     */
    public JpaArtifactsCreate(final ArtifactsManagement artifactsManagement) {
        super(null);
    }

    @Override
    public JpaArtifacts build() {
        JpaArtifacts jpaArtifacts = new JpaArtifacts();
        jpaArtifacts.setFileName(fileName);
        jpaArtifacts.setFileType(fileType);
        jpaArtifacts.setDescription(description);
        jpaArtifacts.setSha256Hash(sha256Hash);
        jpaArtifacts.setFileSize(fileSize);
        jpaArtifacts.setExpiryDate(expiryDate);
        jpaArtifacts.setMd5Hash(md5Hash);
        jpaArtifacts.setFileStatus(fileStatus);
        jpaArtifacts.setArtifactStatus("ACTIVE");

        return jpaArtifacts;
    }

    public ArtifactsCreate withFileStatus(String status) {
        this.fileStatus = status;
        return this;
    }

    // Optional: Fluent setter methods for additional customization
    public JpaArtifactsCreate withFileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public JpaArtifactsCreate withFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    public JpaArtifactsCreate withDescription(String description) {
        this.description = description;
        return this;
    }

    public JpaArtifactsCreate withSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
        return this;
    }

    public JpaArtifactsCreate withMd5Hash(String md5Hash) {
        this.md5Hash = md5Hash;
        return this;
    }

    public JpaArtifactsCreate withFileSize(Long fileSize) {
        this.fileSize = fileSize;
        return this;
    }

    public JpaArtifactsCreate withExpiryDate(Long expiryDate) {
        this.expiryDate = expiryDate;
        return this;
    }

    public JpaArtifactsCreate withArtifactStatus(String artifactStatus) {
        this.artifactStatus = artifactStatus;
        return this;
    }

}

