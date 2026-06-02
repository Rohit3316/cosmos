/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDownloadArtifactRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.S3Service;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import java.io.InputStream;
import java.net.URL;

/**
 *
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Tag(name = "Download Artifact")
public class MgmtDownloadArtifactResource implements MgmtDownloadArtifactRestApi {

    private static final String LOCATION_HEADER = "Location";
    private static final Logger LOG = LoggerFactory.getLogger(MgmtDownloadArtifactResource.class);
    @Autowired
    private SoftwareModuleManagement softwareModuleManagement;

    @Autowired
    private ArtifactsManagement artifactsManagement;

    @Autowired
    private RequestResponseContextHolder requestResponseContextHolder;
    @Autowired
    private ArtifactFilesystemProperties artifactFilesystemProperties;

    @Autowired
    private S3Service s3Service;

    /**
     * Handles the GET request for downloading an artifact.
     *
     * @param softwareModuleId of the parent SoftwareModule
     * @param artifactId       of the related Artifacts
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    @Deprecated
    public ResponseEntity<InputStream> downloadArtifact(@PathVariable("tenantId") final Long tenantId,
                                                        @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                        @PathVariable("artifactId") @TraceableField final Long artifactId) {
        LOG.debug("Received download artifact request for artifactId and softwareModuleId");
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        ArtifactSoftwareModuleAssociation firstAssociation = artifactsManagement
                .findAssociationsByArtifactIdAndSoftwareModuleId(artifactId, softwareModuleId)
                .flatMap(associations -> associations.stream().findFirst())
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No artifact ID %s found for software module ID %s", artifactId, softwareModuleId)));


        final Artifacts artifact = firstAssociation.getArtifact();

        String fileName = artifact.getFileName();
        String tenant = artifact.getTenant();
        String bucketName = artifactFilesystemProperties.getS3bucket().getName();
        String checksum = artifact.getSha256Hash();
        String s3ObjName = s3Service.buildS3ObjectName(tenant, checksum, fileName);
        Long preSignedUrlExpiryTime = artifactFilesystemProperties.getS3bucket().getPreSignedUrlExpiryTime();
        URL url = s3Service.generatePresignedUrl(bucketName, s3ObjName, preSignedUrlExpiryTime);

        if (s3Service.isValidGetUrl(url)) {
            HttpHeaders headers = new HttpHeaders();
            headers.add(LOCATION_HEADER, url.toString());
            return new ResponseEntity<>(headers, HttpStatus.valueOf(302));
        } else {
            throw new EntityNotFoundException("Artifact Not Available on Server");
        }
    }

}