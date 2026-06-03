/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactReplacementRequest;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsRequest;
import org.cosmos.models.mgmt.artifacts.dto.SoftwareModuleArtifactBindingRequest;
import org.cosmos.s3.model.S3FileUpload;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.builder.ArtifactsCreate;
import org.eclipse.hawkbit.repository.exception.ArtifactUploadFailedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidMD5HashException;
import org.eclipse.hawkbit.repository.exception.InvalidSHA1HashException;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.ArtifactUpload;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SourceTargetVersionPair;
import org.eclipse.hawkbit.repository.model.constants.ArtifactsAuditStatus;
import org.eclipse.hawkbit.repository.model.inventory.ArtifactsUpload;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.Set;


/**
 * Service for {@link Artifacts} management operations.
 */
public interface ArtifactsManagement {

    /**
     * Persists artifacts as provided by given
     * <p>
     * <p>
     * . assign the
     * artifacts in addition to given {@link Artifacts}.
     *
     * @return uploaded {@link Artifacts}
     * @throws EntityNotFoundException       if given software module does not exist
     * @throws EntityAlreadyExistsException  if File with that name already exists in the Software Module
     * @throws ArtifactUploadFailedException if upload fails with internal server errors
     * @throws InvalidMD5HashException       if check against provided MD5 checksum failed
     * @throws InvalidSHA1HashException      if check against provided SHA1 checksum failed
     * @throws ConstraintViolationException  if {@link ArtifactUpload} contains invalid values
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts create(@NotNull @Valid ArtifactsCreate c);

    /**
     * Searches for {@link Artifacts} with given {@link Identifiable}.
     *
     * @param id to search for
     * @return an {@link Optional} containing the found {@link Artifacts}, or {@link Optional#empty()} if not found
     */

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<Artifacts> getArtifactsById(@NotNull @Valid long id);

    /**
     * Deletes {@link Artifacts} based on given id.
     *
     * @param id of the {@link Artifacts} that has to be deleted.
     * @throws EntityNotFoundException if artifacts with given ID does not exist
     */
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    void deleteArtifactsById(final Long id, final ArtifactsAuditStatus status);

    /**
     * Searches for {@link Artifacts} with given file name.
     *
     * @param fileName to search for
     * @return found List of {@link Artifacts}s.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<Artifacts> findByFileName(final String fileName);

    /**
     * Searches for {@link Artifacts} with the given SHA-256 hash.
     *
     * @param sha256 the SHA-256 hash to search for
     * @return a list of found {@link Artifacts}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<Artifacts> findBySha256Hash(final String sha256);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long countAssociationsByArtifactIdAndSoftwareModuleId(long artifactId, long softwareModuleId);

    /**
     * Creates or updates the given {@link Artifacts} and the given
     * {@link ArtifactSoftwareModuleAssociation}s.
     *
     * @param associationList to be created or updated
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    void createOrUpdateArtifactSoftwareModuleAssociation(Set<ArtifactSoftwareModuleAssociation> associationList);

    /**
     * Updates the given {@link Artifacts}.
     *
     * @param artifacts to be updated
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_UPDATE_TARGET)
    void update(Artifacts artifacts);

    /**
     * Retrieves all artifacts.
     *
     * @param pageable pagination parameter
     * @return the found {@link Artifacts}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Page<Artifacts> findAll(@NotNull Pageable pageable);

    /**
     * Retrieves all artifacts.
     *
     * @return the found {@link Artifacts}s
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    long count();

    /**
     * Searches for {@link Artifacts} with given {@link Identifiable}.
     *
     * @param filename to search for
     * @return an {@link Optional} containing the found {@link Artifacts}, or {@link Optional#empty()} if not found
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    boolean artifactExistsByFilename(String filename);

    /* Searches for an {@link ArtifactSoftwareModuleAssociation} with the given artifact ID and software module ID.
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given artifact ID.
     *
     * @param artifactId the ID of the artifact.
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    List<ArtifactSoftwareModuleAssociation> findAssociationByArtifactId(@NotNull @Valid long artifactId);

    /**
     * Searches for {@link ArtifactSoftwareModuleAssociation} entities with the given artifact ID and software module ID.
     *
     * @param artifactId       the ID of the artifact
     * @param softwareModuleId the ID of the software module
     * @return a {@link List} of {@link ArtifactSoftwareModuleAssociation} entities matching the given artifact ID and software module ID.
     * If no associations are found, an empty {@link List} is returned.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsByArtifactIdAndSoftwareModuleId(@NotNull @Valid long artifactId, @NotNull @Valid long softwareModuleId);

    /**
     * Searches for {@link ArtifactSoftwareModuleAssociation} entities with the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     * @return a {@link List} of {@link ArtifactSoftwareModuleAssociation} entities matching the given software module ID.
     * If no associations are found, an empty {@link List} is returned.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<List<ArtifactSoftwareModuleAssociation>> findAssociationsBySoftwareModuleId(@NotNull @Valid long softwareModuleId);

    /**
     * Deletes the {@link ArtifactSoftwareModuleAssociation} with the given ID.
     *
     * @param id the ID of the association to delete
     */
    void deleteArtifactSoftwareModuleAssociation(long id);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts saveArtifacts(ArtifactsUpload artifactsUpload, final Long fileSize, MessageDigest md5Digest) throws IOException;


    /**
     * Persists artifacts as provided by given InputStream. assign the
     * artifacts in addition to given {@link Artifacts}.
     *
     * @return uploaded {@link Artifacts}
     * @throws EntityNotFoundException       if given software module does not exist
     * @throws EntityAlreadyExistsException  if File with that name already exists in the Software Module
     * @throws ArtifactUploadFailedException if upload fails with internal server errors
     * @throws InvalidMD5HashException       if check against provided MD5 checksum failed
     * @throws InvalidSHA1HashException      if check against provided SHA1 checksum failed
     * @throws ConstraintViolationException  if {@link ArtifactUpload} contains invalid values
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts intiateFileUpload(ArtifactsUpload artifactsUpload,Long identifier,Long tenantId,String sha256) throws IOException;

    /**
     * Purge the {@link Artifacts} from CDN and unlink software modules.
     *
     * @param artifact the {@link Artifacts} entity for which the associations are being deleted and purged.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_DELETE_REPOSITORY)
    void purgeArtifacts(Artifacts artifact);

    /**
     * Upload the {@link Artifacts} from EFS to S3.
     *
     * @param sha256   the SHA-256 hash of the artifact file, used to generate the S3 key path.
     * @param filename the name of the file to be uploaded.
     * @return an {@link S3FileUpload} instance with configured bucket name, key path, and filename.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    S3FileUpload getS3FileUploadEntity(String sha256, String filename);

    /**
     * Retrieves the count of all artifact software module associations
     *
     * @return the count of all artifact software module associations
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Long artifactSoftwareModuleAssociationCount();

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given artifact ID, software module ID. source version ID, target version ID.
     *
     * @param softwareModuleId the ID of software module
     * @param sourceVersionId  the ID of source version
     * @param targetVersionId  the ID of target version
     * @param artifactId       the ID of artifact
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<ArtifactSoftwareModuleAssociation> findArtifactSoftwareModuleAssociation(Long softwareModuleId, Long sourceVersionId, Long targetVersionId, Long artifactId);

    /**
     * Returns all the associations for artifacts with the given software module ID.
     *
     * @param softwareModuleId the ID of the software module
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Set<SourceTargetVersionPair<Long, Long>> findDistinctSourceAndTargetVersionsBySoftwareModuleId(Long softwareModuleId);

    /**
     * Get local artifacts for a base software module.
     *
     * @param pageReq Pageable parameter
     * @param swId    software module id
     * @return Page<Artifacts>
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<Artifacts> findBySoftwareModule(@NotNull Pageable pageReq, long swId);

    /*
     * Persists artifacts as provided by given Input.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Artifacts saveArtifactInitial(ArtifactsUpload artifactsUpload);

    /**
     * Searches for an {@link ArtifactSoftwareModuleAssociation} with the given software module ID and target version ID.
     *
     * @param softwareModuleId the ID of the software module
     * @param targetVersionId  the ID of the target version
     * @return an {@link Optional} containing the found {@link ArtifactSoftwareModuleAssociation}, or {@link Optional#empty()} if not found
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_TARGET)
    Optional<ArtifactSoftwareModuleAssociation> findFirstBySoftwareModuleIdAndTargetVersionId(Long softwareModuleId, Long targetVersionId);

    /**
     * Creates artifacts using a file URL and the provided artifact request.
     *
     * @param artifactsRequest the request containing artifact metadata and file URL
     * @param tenantId the tenant ID for which the artifact is being created
     * @return the saved {@link Artifacts}
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts createArtifactFromFileURL(@NotNull MgmtArtifactsRequest artifactsRequest, @NotNull Long tenantId);

    /**
     * Replaces an existing artifact with a new file and updates its metadata.
     *
     * @param mgmtReplaceArtifactsRequest the request containing information about the artifact to be replaced and the new file/metadata
     * @param tenantId the tenant ID
     * @throws Exception if the replacement operation fails
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts replaceArtifacts(MgmtArtifactReplacementRequest mgmtReplaceArtifactsRequest, Long tenantId) throws Exception;

    /**
     * Creates a new {@link Artifacts} entity using the provided multipart file and metadata.
     *
     * @param file                the multipart file to upload
     * @param filename            the name of the file
     * @param fileType            the type of the file as a string
     * @param description         the description of the artifact (optional)
     * @param sha256              the SHA-256 hash of the file
     * @param signatureExpiryDate the expiry date of the artifact signature (optional)
     * @return the created {@link Artifacts} entity
     * @throws Exception if the creation or upload fails
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_TARGET)
    Artifacts createArtifactFromMultipartFile(@NotNull MultipartFile file, @NotNull String filename, @NotNull String fileType,
                                              String description, @NotNull String sha256, Long signatureExpiryDate) throws Exception;


    /**
     * Unlinks artifact from software module associations.
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    void unlinkArtifactSoftwareModuleAssociation(Long tenantId, String artifactId, String softwareModuleId);

    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_CREATE_REPOSITORY)
    void createArtifactSoftwareModuleAssociation(String artifactId, Long tenantId, SoftwareModuleArtifactBindingRequest request);

    /**
     * Retrieves a paginated list of {@link Artifacts} matching the given RSQL filter expression.
     * <p>
     * Supports advanced filtering using RSQL syntax and pagination via {@link Pageable}.
     * </p>
     *
     * @param pageable  the pagination and sorting information (must not be {@code null})
     * @param rsqlParam the RSQL filter string (must not be {@code null} or blank)
     * @return a {@link Page} of {@link Artifacts} matching the filter criteria
     * @throws IllegalArgumentException if {@code rsqlParam} is blank
     */
    @PreAuthorize(SpPermission.SpringEvalExpressions.HAS_AUTH_READ_REPOSITORY)
    Page<Artifacts> findByRsql(@NotNull Pageable pageable, @NotNull String rsqlParam);

}
