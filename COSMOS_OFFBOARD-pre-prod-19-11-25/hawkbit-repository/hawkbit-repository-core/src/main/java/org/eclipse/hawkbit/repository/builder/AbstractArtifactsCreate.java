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

/**
 * Create and update builder DTO.
 *
 * @param <T> update or create builder interface
 */
public class AbstractArtifactsCreate<T> {

    protected Long id;
    protected String fileName;
    protected FileType fileType;
    protected String description;
    protected String sha256Hash;
    protected Long fileSize;
    protected Long expiryDate;
    protected String md5Hash;
    protected String artifactStatus;
    protected String fileStatus;


    protected AbstractArtifactsCreate(Long id) {
        this.id = id;
    }

    public T fileName(final String fileName) {
        this.fileName = fileName;
        return (T) this;
    }

    public T fileType(final FileType fileType) {
        this.fileType = fileType;
        return (T) this;
    }

    public T description(final String description) {
        this.description = description;
        return (T) this;
    }

    public T sha256Hash(final String sha256Hash) {
        this.sha256Hash = sha256Hash;
        return (T) this;
    }

    public T md5Hash(final String md5Hash) {
        this.md5Hash = md5Hash;
        return (T) this;
    }

    public T fileSize(final Long fileSize) {
        this.fileSize = fileSize;
        return (T) this;
    }

    public T expiryDate(final Long expiryDate) {
        this.expiryDate = expiryDate;
        return (T) this;
    }

    public T artifactStatus(final String artifactStatus) {
        this.artifactStatus = artifactStatus;
        return (T) this;
    }

    public  T fileStatus(final String fileStatus) {
        this.fileStatus = fileStatus;
        return (T) this;
    }

    public T id(final Long id) {
        this.id = id;
        return (T) this;
    }

}
