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
import org.cosmos.annotations.TraceableObject;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAddVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtGetVersionResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.VersionResponse;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareVersionRestApi;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SourceTargetVersionPair;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Software versions management Resources
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Tag(name = "Software Version")
public class MgmtSoftwareVersionResource implements MgmtSoftwareVersionRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtSoftwareVersionResource.class);

    private final VersionManagement versionManagement;

    private final EntityFactory entityFactory;

    private final SoftwareModuleManagement softwareModuleManagement;

    private final ArtifactsManagement artifactManagement;

    MgmtSoftwareVersionResource(final VersionManagement versionManagement, final EntityFactory entityFactory,
                                final SoftwareModuleManagement softwareModuleManagement,
                                final ArtifactsManagement artifactManagement) {
        this.entityFactory = entityFactory;
        this.versionManagement = versionManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactManagement = artifactManagement;
    }

    /**
     * Handles the POST request for adding new software module versions.
     *
     * @param addSoftwareVersion the new software module versions
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtAddVersionResponse> addSoftwareVersion(@PathVariable("tenantId") Long tenantId,
                                                                     @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                                     @RequestBody @Valid @TraceableObject MgmtAddVersionRequestBody addSoftwareVersion) {
        LOG.debug("Received request to add software version");
        Version version = versionManagement.create(entityFactory.version().create().name(addSoftwareVersion.getName())
                .description(addSoftwareVersion.getDescription()).softwareModuleId(softwareModuleId)
                .number(addSoftwareVersion.getNumber()));
        return new ResponseEntity<>(MgmtAddVersionResponse.builder().id(version.getId()).build(), HttpStatus.CREATED);
    }

    /**
     * Handles the GET request for software version.
     *
     * @param versionId info for the software version
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> getVersion(@PathVariable("tenantId") Long tenantId,
                                                             @PathVariable("softwareModuleId") @TraceableField final Integer softwareModuleId,
                                                             @PathVariable("versionId") final Integer versionId) {
        LOG.debug("Received request to get software version");
        try {
            Optional<Version> version = versionManagement.getById(versionId);
            if (version.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            Version currentVersion = version.get();

            if (currentVersion.getSoftwareModuleId().getId().intValue() != softwareModuleId) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }

            MgmtGetVersionResponse response = new MgmtGetVersionResponse();
            VersionResponse versionResponse = new VersionResponse();
            versionResponse.setId(currentVersion.getId());
            versionResponse.setName(currentVersion.getName());
            versionResponse.setDescription(currentVersion.getDescription());
            versionResponse.setNumber(currentVersion.getNumber());
            response.setVersion(List.of(versionResponse));
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof EntityNotFoundException) {
                throw new EntityNotFoundException(e.getMessage());
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles the GET request for software version.
     *
     * @param versionId info for the software version
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> deleteVersion(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("softwareModuleId") @TraceableField final Integer softwareModuleId,
            @PathVariable("versionId") final Integer versionId) {
        LOG.debug("Received request to delete software version");
        try {
            Optional<SoftwareModule> sm = softwareModuleManagement.get(softwareModuleId);
            LOG.info("Softwaremodule: {}", sm.orElse(null));
            if (sm.isEmpty()) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }

            // Get all versions for the software module
            List<Version> versions = versionManagement.findVersionBySoftware(sm.get());
            LOG.info("Versions for softwareModuleId {}: {}", softwareModuleId, versions);
            if (versions == null || versions.isEmpty()) {
                throw new EntityNotFoundException(Version.class, versionId);
            }

            boolean foundVersion = false;
            Version currentVersion = null;
            for (Version v : versions) {
                if (v.getId().intValue() == versionId) {
                    currentVersion = v;
                    foundVersion = true;
                    break;
                }
            }
            LOG.info("Current version: {}", currentVersion);
            if (!foundVersion) {
                throw new EntityNotFoundException(Version.class, versionId);
            }
            if (currentVersion.getSoftwareModuleId().getId().intValue() != softwareModuleId) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }

            Set<SourceTargetVersionPair<Long,Long>> sourceTargetVersionPairs = artifactManagement.findDistinctSourceAndTargetVersionsBySoftwareModuleId(sm.get().getId());
            LOG.info("SourceTargetVersionPairs: {}", sourceTargetVersionPairs);
            boolean isVersionAlreadyExists = false;
            for (SourceTargetVersionPair<Long, Long> pair : sourceTargetVersionPairs) {
                LOG.info("Checking pair: sourceVersionId={}, targetVersionId={}", pair.getSourceVersionId(), pair.getTargetVersionId());
                for (Version v : versions) {
                    Long sourceVersionId = pair.getSourceVersionId();
                    Long targetVersionId = pair.getTargetVersionId();
                    LOG.info("Comparing sourceVersionId {} with versionId {}", sourceVersionId, v.getId());
                    LOG.info("Comparing targetVersionId {} with versionId {}", targetVersionId, v.getId());
                    if ((sourceVersionId != null && sourceVersionId.equals(v.getId())) ||
                            (targetVersionId != null && targetVersionId.equals(v.getId()))) {
                        isVersionAlreadyExists = true;
                        LOG.info("Match found for version {}. Setting isVersionAlreadyExists to true and breaking loop.", v.getId());
                        break;
                    }
                }
                if (isVersionAlreadyExists) {
                    break;
                }
            }
            LOG.info("Final isVersionAlreadyExists: {}", isVersionAlreadyExists);
            if (isVersionAlreadyExists) {
                throw new EntityAlreadyExistsException("Version cannot be deleted as there are artifacts associated with version.");
            }
            versionManagement.deleteVersion(versionId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            LOG.error("Message: {}", e.getMessage());
            if (e instanceof EntityAlreadyExistsException) {
                throw new EntityAlreadyExistsException(e.getMessage());
            } else if (e instanceof EntityNotFoundException) {
                throw new EntityNotFoundException(e.getMessage());
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }



    /**
     * Handles the GET request for software versions.
     *
     * @param softwareModuleId to fetch all the associated software versions
     * @return responseEntity with status ok if successful and list of all software
     * versions
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> getVersions(@PathVariable("tenantId") Long tenantId,
                                                              @PathVariable("softwareModuleId") @TraceableField final Integer softwareModuleId) {
        LOG.debug("Received request to get software versions");
        try {

            Optional<SoftwareModule> res = softwareModuleManagement.get(softwareModuleId);
            if (res.isEmpty()) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }
            List<Version> versionList = versionManagement.findVersionBySoftware(res.get());
            MgmtGetVersionResponse response = new MgmtGetVersionResponse();
            List<VersionResponse> versionResponses = new ArrayList<>();
            if (versionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            versionList.forEach(currentVersion -> {

                VersionResponse versionResponse = new VersionResponse();
                versionResponse.setId(currentVersion.getId());
                versionResponse.setName(currentVersion.getName());
                versionResponse.setDescription(currentVersion.getDescription());
                versionResponse.setNumber(currentVersion.getNumber());
                versionResponses.add(versionResponse);
            });
            response.setVersion(versionResponses);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof EntityNotFoundException) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles the POST request for adding new software module versions.
     *
     * @param tenantId               the tenant id
     * @param scomoId                the name of scomo which is software module name
     * @param addSoftwareVersion     the new software module versions
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtAddVersionResponse> addSoftwareVersion(Long tenantId, @TraceableField String scomoId, @TraceableObject MgmtAddVersionRequestBody addSoftwareVersion) {
        LOG.debug("Received request to add software version for scomo: {}", scomoId);
        validateScomo(scomoId);
        SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));
        LOG.debug("Creating new version for software module with id: {}", softwareModule.getId());
        Version version = versionManagement.create(entityFactory.version().create().name(addSoftwareVersion.getName())
                .description(addSoftwareVersion.getDescription()).softwareModuleId(softwareModule.getId())
                .number(addSoftwareVersion.getNumber()));
        LOG.debug("Software version created with id: {}", version.getId());
        return new ResponseEntity<>(MgmtAddVersionResponse.builder().id(version.getId()).build(), HttpStatus.CREATED);
    }

    /**
     * Handles the GET request for software version.
     *
     * @param tenantId  the tenant id
     * @param scomoId   the name of scomo which is software module name
     * @param versionId info for the software version
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> getVersion(Long tenantId, @TraceableField String scomoId, Integer versionId) {
        LOG.debug("Received request to get software version for scomo: {}", scomoId);
        try {
            Optional<Version> version = versionManagement.getById(versionId);
            if (version.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            validateScomo(scomoId);
            Version currentVersion = version.get();
            SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoId)
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));
            LOG.debug("Checking if version belongs to software module with id: {}", softwareModule.getId());
            if (currentVersion.getSoftwareModuleId().getId().intValue() != softwareModule.getId()) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModule.getId());
            }

            MgmtGetVersionResponse response = new MgmtGetVersionResponse();
            VersionResponse versionResponse = new VersionResponse();
            versionResponse.setId(currentVersion.getId());
            versionResponse.setName(currentVersion.getName());
            versionResponse.setDescription(currentVersion.getDescription());
            versionResponse.setNumber(currentVersion.getNumber());
            response.setVersion(List.of(versionResponse));
            LOG.debug("Returning software version response for version id: {}", versionId);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof EntityNotFoundException) {
                throw new EntityNotFoundException(e.getMessage());
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles the DELETE request for software version.
     *
     * @param versionId info for the software version
     * @param scomoId   the name of scomo which is software module name
     * @param tenantId  the tenant id
     * @return responseEntity with status ok if successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> deleteVersion(Long tenantId, @TraceableField String scomoId, Integer versionId) {
        LOG.debug("Received request to delete software version for scomo: {}", scomoId);
        try {
            validateScomo(scomoId);
            Long softwareModuleId = softwareModuleManagement.getByScomoName(scomoId)
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId)).getId();
            Optional<SoftwareModule> sm = softwareModuleManagement.get(softwareModuleId);
            if (sm.isEmpty()) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }
            LOG.debug("Getting version by id: {}", versionId);
            Optional<Version> version = versionManagement.getById(versionId);
            if (version.isEmpty()) {
                throw new EntityNotFoundException(Version.class, versionId);
            }
            Version currentVersion = version.get();
            LOG.debug("Checking if version belongs to software module with id: {}", softwareModuleId);
            if (currentVersion.getSoftwareModuleId().getId().intValue() != softwareModuleId) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }
            LOG.debug("Checking if version is associated with any artifacts");
            Set<SourceTargetVersionPair<Long,Long>> sourceTargetVersionPairs=artifactManagement.findDistinctSourceAndTargetVersionsBySoftwareModuleId(sm.get().getId());
            boolean isVersionAlreadyExists= sourceTargetVersionPairs.stream()
                    .map(sourceTargetVersionPair->sourceTargetVersionPair.getSourceVersionId().equals(currentVersion.getId())||sourceTargetVersionPair.getTargetVersionId().equals(currentVersion.getId()))
                    .findFirst()
                    .orElse(Boolean.FALSE);
            if (isVersionAlreadyExists) {
                throw new EntityAlreadyExistsException("Version cannot be deleted as there are artifacts associated with version.");
            }
            LOG.debug("Deleting version with id: {}", versionId);
            versionManagement.deleteVersion(versionId);
            LOG.debug("Version with id: {} deleted successfully", versionId);
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof EntityAlreadyExistsException) {
                throw new EntityAlreadyExistsException(e.getMessage());
            } else if (e instanceof EntityNotFoundException) {
                throw new EntityNotFoundException(e.getMessage());
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Handles the GET request for software versions.
     *
     * @param tenantId  the tenant id
     * @param scomoId   the name of scomo which is software module name
     * @return responseEntity with status ok if successful and list of all software
     * versions
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtGetVersionResponse> getVersions(Long tenantId, @TraceableField String scomoId) {
        LOG.debug("Received request to get software versions for scomo: {}", scomoId);
        try {
            validateScomo(scomoId);
            Long softwareModuleId = softwareModuleManagement.getByScomoName(scomoId)
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId)).getId();
            Optional<SoftwareModule> res = softwareModuleManagement.get(softwareModuleId);
            if (res.isEmpty()) {
                throw new EntityNotFoundException(SoftwareModule.class, softwareModuleId);
            }
            LOG.debug("Finding versions for software module with id: {}", softwareModuleId);
            List<Version> versionList = versionManagement.findVersionBySoftware(res.get());
            MgmtGetVersionResponse response = new MgmtGetVersionResponse();
            List<VersionResponse> versionResponses = new ArrayList<>();
            if (versionList.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            versionList.forEach(currentVersion -> {

                VersionResponse versionResponse = new VersionResponse();
                versionResponse.setId(currentVersion.getId());
                versionResponse.setName(currentVersion.getName());
                versionResponse.setDescription(currentVersion.getDescription());
                versionResponse.setNumber(currentVersion.getNumber());
                versionResponses.add(versionResponse);
            });
            response.setVersion(versionResponses);
            LOG.debug("Returning software versions response for scomo: {}", scomoId);
            return new ResponseEntity<>(response, HttpStatus.OK);

        } catch (Exception e) {
            LOG.error(e.getMessage());
            if (e instanceof EntityNotFoundException) {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateScomo(String scomoId) {
        LOG.debug("Validating scomoId: {}", scomoId);
        if (scomoId == null || scomoId.isEmpty()) {
            throw new EntityNotFoundException("Scomo Id cannot be null or empty.");
        }
    }

}