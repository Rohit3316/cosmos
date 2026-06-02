/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model.inventory;

import java.io.InputStream;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.eclipse.hawkbit.artifact.repository.model.ArtifactsHash;

/**
 * Use to create a new artifact.
 */
@Getter
@Builder
@AllArgsConstructor
public class ArtifactsUpload {

    private final String fileURL;


    private final InputStream inputStream;

    @NotNull
    private final String filename;

    @NotNull
    private final String fileType;

    @NotNull
    private final String description;

    @NotNull
    private final Long signatureExpiryDate;

    private final ArtifactsHash artifactsHash;

    private final Long fileSize;

    /**
     * Constructor
     *
     * @param fileURL             the URL of the file
     * @param inputStream         the input stream of the file to be uploaded
     * @param filename            the name of the file
     * @param fileType            the type of the file
     * @param description         the description of the file
     * @param signatureExpiryDate the expiry date of the file's signature
     * @param artifactsHash       the hash of the file
     */
    public ArtifactsUpload(final String fileURL, final InputStream inputStream, final String filename,
                           final String fileType, final String description, final Long signatureExpiryDate, final ArtifactsHash artifactsHash) {
        this.fileURL = fileURL;
        this.inputStream = inputStream;
        this.filename = filename;
        this.fileType = fileType;
        this.description = description;
        this.signatureExpiryDate = signatureExpiryDate;
        this.fileSize = 0L;
        this.artifactsHash = artifactsHash;
    }

    /**
     * Constructor without fileURL
     *
     * @param inputStream         the input stream of the file to be uploaded
     * @param filename            the name of the file
     * @param fileType            the type of the file
     * @param description         the description of the file
     * @param signatureExpiryDate the expiry date of the file's signature
     * @param artifactsHash       the hash of the file
     */
    public ArtifactsUpload(final InputStream inputStream, final String filename,
                           final String fileType, final String description, final Long signatureExpiryDate, final ArtifactsHash artifactsHash) {
        this.fileURL = null;
        this.inputStream = inputStream;
        this.filename = filename;
        this.fileType = fileType;
        this.description = description;
        this.signatureExpiryDate = signatureExpiryDate;
        this.fileSize = 0L;
        this.artifactsHash = artifactsHash;
    }

    /**
     * Constructor without inputStream
     *
     * @param fileURL             the URL of the file
     * @param filename            the name of the file
     * @param fileType            the type of the file
     * @param description         the description of the file
     * @param signatureExpiryDate the expiry date of the file's signature
     * @param artifactsHash       the hash of the file
     */
    public ArtifactsUpload(final String fileURL, final String filename,
                           final String fileType, final String description, final Long signatureExpiryDate, final Long fileSize, final ArtifactsHash artifactsHash) {
        this.fileURL = fileURL;
        this.inputStream = null;
        this.filename = filename;
        this.fileType = fileType;
        this.description = description;
        this.signatureExpiryDate = signatureExpiryDate;
        this.fileSize = fileSize;
        this.artifactsHash = artifactsHash;
    }

}
