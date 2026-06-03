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
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.ecu.dto.EcuModels;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignEcuModelRequestBody;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleMetadata;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleMetadataBodyPut;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPut;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.SoftwareInstallerTypeManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleFormatManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaSoftwareModuleManagement;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareInstallerType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleFormat;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ValidationException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * REST Resource handling for {@link SoftwareModule} and related
 * {@link Artifacts} CRUD operations.
 */
@RestController
@Tag(name = "Software Modules")
public class MgmtSoftwareModuleResource implements MgmtSoftwareModuleRestApi {

    private static final Logger LOG = LoggerFactory.getLogger(MgmtSoftwareModuleResource.class);

    private final ArtifactsManagement artifactsManagement;

    private final SoftwareModuleManagement softwareModuleManagement;

    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    private final SoftwareModuleFormatManagement softwareModuleFormatManagement;

    private final SoftwareInstallerTypeManagement softwareInstallerTypeManagement;

    private final ArtifactUrlHandler artifactUrlHandler;

    private final SystemManagement systemManagement;

    private final EntityFactory entityFactory;

    private final VersionManagement versionManagement;

    private final JpaSoftwareModuleManagement jpaSoftwareModuleManagement;

    MgmtSoftwareModuleResource(final ArtifactsManagement artifactsManagement,
                               final SoftwareModuleManagement softwareModuleManagement,
                               final SoftwareModuleTypeManagement softwareModuleTypeManagement,
                               final SoftwareModuleFormatManagement softwareModuleFormatManagement,
                               final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
                               final EntityFactory entityFactory, final VersionManagement versionManagement,
                               final SoftwareInstallerTypeManagement softwareInstallerTypeManagement,
                               final JpaSoftwareModuleManagement jpaSoftwareModuleManagement) {
        this.artifactsManagement = artifactsManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
        this.softwareModuleFormatManagement = softwareModuleFormatManagement;
        this.artifactUrlHandler = artifactUrlHandler;
        this.systemManagement = systemManagement;
        this.entityFactory = entityFactory;
        this.versionManagement = versionManagement;
        this.softwareInstallerTypeManagement = softwareInstallerTypeManagement;
        this.jpaSoftwareModuleManagement = jpaSoftwareModuleManagement;
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtSoftwareModule>> getSoftwareModules(@PathVariable("tenantId") final Long tenantId,
                                                                            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<SoftwareModule> findModulesAll;
        final long countModulesAll;
        if (rsqlParam != null) {
            findModulesAll = softwareModuleManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<SoftwareModule>) findModulesAll).getTotalElements();
        } else {
            findModulesAll = softwareModuleManagement.findAll(pageable);
            countModulesAll = softwareModuleManagement.count();
        }

        final List<MgmtSoftwareModule> rest = MgmtSoftwareModuleMapper.toResponse(tenantId, findModulesAll.getContent());
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId) {

        LOG.debug(" Received request to get Software Module ");
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(tenantId, module);
        MgmtSoftwareModuleMapper.addLinks(tenantId, module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModule> getSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                @PathVariable("scomoId") @TraceableField final String scomoId) {

        LOG.debug(" Received request to get Software Module ");

        validateScomo(scomoId);

        final SoftwareModule module = softwareModuleManagement.getByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(tenantId, module);
        MgmtSoftwareModuleMapper.addLinks(tenantId, module, response);

        return ResponseEntity.ok(response);
    }



    @Override
    @TenantAware
    public ResponseEntity<List<MgmtSoftwareModule>> createSoftwareModules(@PathVariable("tenantId") final Long tenantId,
                                                                          @RequestBody final List<MgmtSoftwareModuleRequestBodyPost> softwareModules) {

        LOG.debug("creating {} softwareModules", softwareModules.size());

        for (final MgmtSoftwareModuleRequestBodyPost sm : softwareModules) {
            final Optional<SoftwareModuleType> opt = softwareModuleTypeManagement.getByKey(sm.getType().toLowerCase());
            opt.ifPresent(smType -> {
                if (smType.isDeleted()) {
                    final String text = "Cannot create Software Module from type with key {0}. Software Module Type already deleted!";
                    final String message = MessageFormat.format(text, smType.getKey());
                    throw new ValidationException(message);
                }
            });

            final Optional<SoftwareModuleFormat> smf = softwareModuleFormatManagement.getByKey(sm.getType());
            smf.ifPresent(smFormat -> {
                if (smFormat.isDeleted()) {
                    final String text = "Cannot create Software Module from format with key {0}. Software Module Format already deleted!";
                    final String message = MessageFormat.format(text, smFormat.getKey());
                    throw new ValidationException(message);
                }
            });

            final SoftwareInstallerType swInstallerType = softwareInstallerTypeManagement.getSwInstallerTypeByName(sm.getSwInstallerType());
            if (swInstallerType.isDeleted()) {
                final String text = "Cannot create Software Module from installer type with name {0}. Software Installer Type already deleted!";
                final String message = MessageFormat.format(text, swInstallerType.getName());
                throw new ValidationException(message);
            }

            // Validation for unique Software Module name per tenant

            String softwareModuleName = sm.getName();
            boolean moduleExists = softwareModuleManagement.existsByName(softwareModuleName);

            if (moduleExists) {
                String errorMessage = MessageFormat.format(
                        "Software Module with name ''{0}'' already exists for tenant ",
                        softwareModuleName
                );
                throw new ValidationException(errorMessage);
            }

        }

        final Collection<SoftwareModule> createdSoftwareModules = softwareModuleManagement
                .create(MgmtSoftwareModuleMapper.smFromRequest(entityFactory, softwareModules));
        LOG.debug("{} softwareModules created, return status {}", softwareModules.size(), HttpStatus.CREATED);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(MgmtSoftwareModuleMapper.toResponse(tenantId, createdSoftwareModules));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                   @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                                   @RequestBody final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {
        LOG.debug("Received request to update Software Module");
        final SoftwareModule module = softwareModuleManagement
                .update(entityFactory.softwareModule().update(softwareModuleId)
                        .description(restSoftwareModule.getDescription()).vendor(restSoftwareModule.getVendor()));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(tenantId, module);
        MgmtSoftwareModuleMapper.addLinks(tenantId, module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModule> updateSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                   @PathVariable("scomoId") @TraceableField final String scomoId,
                                                                   @RequestBody @TraceableObject final MgmtSoftwareModuleRequestBodyPut restSoftwareModule) {
        LOG.debug("Received request to update Software Module");
        validateScomo(scomoId);

        Long softwareModuleId = softwareModuleManagement.getIdByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        final SoftwareModule module = softwareModuleManagement
                .update(entityFactory.softwareModule().update(softwareModuleId)
                        .description(restSoftwareModule.getDescription()).vendor(restSoftwareModule.getVendor()));

        final MgmtSoftwareModule response = MgmtSoftwareModuleMapper.toResponse(tenantId, module);
        MgmtSoftwareModuleMapper.addLinks(tenantId, module, response);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                     @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId) {
        LOG.debug("Received request to delete Software Module");
        final SoftwareModule module = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        jpaSoftwareModuleManagement.checkSoftwareModuleLinkWithDistributionSet(softwareModuleId);
        softwareModuleManagement.delete(module.getId());

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                     @PathVariable("scomoId") @TraceableField final String scomoId) {
        LOG.debug("Received request to delete Software Module");

        validateScomo(scomoId);
        Long softwareModuleId = softwareModuleManagement.getIdByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        jpaSoftwareModuleManagement.checkSoftwareModuleLinkWithDistributionSet(softwareModuleId);
        softwareModuleManagement.delete(softwareModuleId);

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtSoftwareModuleMetadata>> getMetadata(@PathVariable("tenantId") final Long tenantId,
                                                                             @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                                             @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        LOG.debug("Received request to get Software Module Metadata");
        // check if software module exists otherwise throw exception immediately
        SoftwareModule softwareModule = softwareModuleManagement.get(softwareModuleId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, softwareModuleId));
        LOG.debug("Software Module Id {} exists.", softwareModule.getId());

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeSoftwareModuleMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<SoftwareModuleMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = softwareModuleManagement.findMetaDataByRsql(pageable, softwareModuleId, rsqlParam);
        } else {
            metaDataPage = softwareModuleManagement.findMetaDataBySoftwareModuleId(pageable, softwareModuleId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtSoftwareModuleMapper.toResponseSwMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModuleMetadata> getMetadataValue(@PathVariable("tenantId") final Long tenantId,
                                                                       @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId, @PathVariable("key") final String key) {
        LOG.debug("Received request to get Software Module Metadata");
        final SoftwareModuleMetadata findOne = softwareModuleManagement
                .getMetaDataBySoftwareModuleId(softwareModuleId, key)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModuleMetadata.class, softwareModuleId, key));

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(findOne));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModuleMetadata> updateMetadata(@PathVariable("tenantId") final Long tenantId,
                                                                     @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId, @PathVariable("key") final String key,
                                                                     @RequestBody final MgmtSoftwareModuleMetadataBodyPut metadata) {
        LOG.debug("Received request to update Software Module Metadata");
        final SoftwareModuleMetadata updated = softwareModuleManagement
                .updateMetaData(entityFactory.softwareModuleMetadata().update(softwareModuleId, key)
                        .value(metadata.getValue()).targetVisible(metadata.isTargetVisible()));

        return ResponseEntity.ok(MgmtSoftwareModuleMapper.toResponseSwMetadata(updated));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteMetadata(@PathVariable("tenantId") final Long tenantId,
                                               @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                               @PathVariable("key") final String key) {
        LOG.debug("Received request to delete Software Module Metadata");
        softwareModuleManagement.deleteMetaData(softwareModuleId, key);

        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<List<MgmtSoftwareModuleMetadata>> createMetadata(@PathVariable("tenantId") final Long tenantId,
                                                                           @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                                           @RequestBody final List<MgmtSoftwareModuleMetadata> metadataRest) {
        LOG.debug("Received request to create Software Module Metadata");
        final List<SoftwareModuleMetadata> created = softwareModuleManagement.createMetaData(
                MgmtSoftwareModuleMapper.fromRequestSwMetadata(entityFactory, softwareModuleId, metadataRest));

        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtSoftwareModuleMapper.toResponseSwMetadata(created));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> assignEcuModelToSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                               @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                               @RequestBody final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody) {
        LOG.debug("Received request to assign ECU Models to Software Module");
        List<EcuModels> ecuModels = assignEcuModelRequestBody.getEcuModels();
        if (ecuModels.isEmpty()) {
            throw new ValidationException("One more ecuModel/s does not exist in the system");
        }
        List<Long> ecuModelsIds = ecuModels.stream()
                .map(EcuModels::getEcuModelId)
                .collect(Collectors.toList());

        softwareModuleManagement.assignEcuModel(softwareModuleId, ecuModelsIds);
        LOG.debug("ECU Models {} assigned successfully to the Software Model {}", ecuModelsIds, softwareModuleId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> assignEcuModelToSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                               @PathVariable("scomoId") @TraceableField final String scomoId,
                                                               @RequestBody @TraceableObject final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody) {
        LOG.debug("Received request to assign ECU Models to Software Module");
        List<EcuModels> ecuModels = assignEcuModelRequestBody.getEcuModels();
        if (ecuModels.isEmpty()) {
            throw new ValidationException("One more ecuModel/s does not exist in the system");
        }
        List<Long> ecuModelsIds = ecuModels.stream()
                .map(EcuModels::getEcuModelId)
                .collect(Collectors.toList());

        validateScomo(scomoId);
        SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        softwareModuleManagement.assignEcuModel(softwareModule, ecuModelsIds);
        LOG.debug("ECU Models {} assigned successfully to the Software Model name {}", ecuModelsIds, scomoId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> removeEcuModelsFromSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                  @PathVariable("softwareModuleId") @TraceableField final Long softwareModuleId,
                                                                  @RequestBody final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody) {
        LOG.debug("Received request to remove ECU Models from Software Module");
        List<EcuModels> ecuModels = assignEcuModelRequestBody.getEcuModels();
        if (ecuModels.isEmpty()) {
            throw new ValidationException("Ecu Models Must not be empty");
        }
        List<Long> ecuModelsIds = ecuModels.stream()
                .map(EcuModels::getEcuModelId)
                .toList();
        softwareModuleManagement.removeAssociatedEcuModels(softwareModuleId, ecuModelsIds);
        LOG.debug("ECU Models association removed successfully from the Software Model {}", softwareModuleId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> removeEcuModelsFromSoftwareModule(@PathVariable("tenantId") final Long tenantId,
                                                                  @PathVariable("scomoId") @TraceableField final String scomoId,
                                                                  @RequestBody @TraceableObject final MgmtAssignEcuModelRequestBody assignEcuModelRequestBody) {
        LOG.debug("Received request to remove ECU Models from Software Module");
        List<EcuModels> ecuModels = assignEcuModelRequestBody.getEcuModels();
        if (ecuModels.isEmpty()) {
            throw new ValidationException("Ecu Models Must not be empty");
        }
        List<Long> ecuModelsIds = ecuModels.stream()
                .map(EcuModels::getEcuModelId)
                .toList();
        validateScomo(scomoId);
        SoftwareModule softwareModule = softwareModuleManagement.getByScomoName(scomoId)
                .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, scomoId));

        softwareModuleManagement.removeAssociatedEcuModels(softwareModule, ecuModelsIds);
        LOG.debug("ECU Models association removed successfully from the Software Model name {}", scomoId);
        return ResponseEntity.ok().build();
    }


    private void validateScomo(String scomoId) {
        LOG.debug("Validating scomoId: {}", scomoId);
        if (scomoId == null || scomoId.isEmpty()) {
            throw new EntityNotFoundException("Scomo Id cannot be null or empty.");
        }
    }
}