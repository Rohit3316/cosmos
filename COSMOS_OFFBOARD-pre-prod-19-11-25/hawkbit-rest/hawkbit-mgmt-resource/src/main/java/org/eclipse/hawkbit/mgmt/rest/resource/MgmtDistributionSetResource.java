/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import io.swagger.v3.oas.annotations.Hidden;
import org.cosmos.models.mgmt.MgmtMetadata;
import org.cosmos.models.mgmt.MgmtMetadataBodyPut;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPost;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPut;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetStatistics;
import org.cosmos.models.mgmt.distributionset.dto.MgmtInvalidateDistributionSetRequestBody;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentRequestBody;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentResponseBody;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignedSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssignments;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.targetfilter.dto.MgmtTargetFilterQuery;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtDistributionSetRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDeploymentRequestMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRestModelMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtSoftwareModuleMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetFilterQueryMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetInvalidationManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.DistributionSoftwareVersionWrapper;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
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

import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import java.text.MessageFormat;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * REST Resource handling for {@link DistributionSet} CRUD operations.
 */
@RestController
@Hidden
public class MgmtDistributionSetResource implements MgmtDistributionSetRestApi {
    private static final Logger LOG = LoggerFactory.getLogger(MgmtDistributionSetResource.class);

    private final SoftwareModuleManagement softwareModuleManagement;

    private final ArtifactsManagement artifactsManagement;

    private final RolloutManagement rolloutManagement;

    private final TargetManagement targetManagement;

    private final TargetFilterQueryManagement targetFilterQueryManagement;

    private final DeploymentManagement deployManagament;

    private final SystemManagement systemManagement;

    private final EntityFactory entityFactory;

    private final DistributionSetManagement distributionSetManagement;


    private final DistributionSetTypeManagement distributionSetTypeManagement;

    private final SystemSecurityContext systemSecurityContext;

    private final DistributionSetInvalidationManagement distributionSetInvalidationManagement;

    private final TenantConfigHelper tenantConfigHelper;

    private final VersionManagement versionManagement;

    private final JpaRolloutManagement jpaRolloutManagement;

    MgmtDistributionSetResource(final SoftwareModuleManagement softwareModuleManagement, final ArtifactsManagement artifactsManagement, RolloutManagement rolloutManagement,
                                final TargetManagement targetManagement, final TargetFilterQueryManagement targetFilterQueryManagement,
                                final DeploymentManagement deployManagament, final SystemManagement systemManagement,
                                final EntityFactory entityFactory, final DistributionSetManagement distributionSetManagement,
                                final DistributionSetTypeManagement distributionSetTypeManagement, final SystemSecurityContext systemSecurityContext,
                                final DistributionSetInvalidationManagement distributionSetInvalidationManagement,
                                final TenantConfigurationManagement tenantConfigurationManagement,
                                final VersionManagement versionManagement,
                                final JpaRolloutManagement jpaRolloutManagement
    ) {
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactsManagement = artifactsManagement;
        this.rolloutManagement = rolloutManagement;
        this.targetManagement = targetManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.deployManagament = deployManagament;
        this.systemManagement = systemManagement;
        this.entityFactory = entityFactory;
        this.distributionSetManagement = distributionSetManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.distributionSetInvalidationManagement = distributionSetInvalidationManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
        this.versionManagement = versionManagement;
        this.jpaRolloutManagement = jpaRolloutManagement;
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtDistributionSet>> getDistributionSets(
            @PathVariable("tenantId") long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<DistributionSet> findDsPage;
        final long countModulesAll;
        if (rsqlParam != null) {
            findDsPage = distributionSetManagement.findByRsql(pageable, rsqlParam);
            countModulesAll = ((Page<DistributionSet>) findDsPage).getTotalElements();
        } else {
            findDsPage = distributionSetManagement.findAll(pageable);
            countModulesAll = distributionSetManagement.count();
        }

        final List<MgmtDistributionSet> rest = MgmtDistributionSetMapper.toResponseFromDsList(findDsPage.getContent(), tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, countModulesAll));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSet> getDistributionSet(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId) {
        final DistributionSet foundDs = distributionSetManagement.getOrElseThrowException(distributionSetId);

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(foundDs, tenantId);
        MgmtDistributionSetMapper.addLinks(foundDs, response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtDistributionSet>> createDistributionSets(
            @PathVariable("tenantId") long tenantId,
            @RequestBody final List<MgmtDistributionSetRequestBodyPost> sets) {

        LOG.debug("creating {} distribution sets", sets.size());
        // set default Ds type if ds type is null
        final String defaultDsKey = systemSecurityContext
                .runAsSystem(systemManagement.getTenantMetadata().getDefaultDsType()::getKey);
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(defaultDsKey));

        //check if there is already deleted DS Type
        for (MgmtDistributionSetRequestBodyPost ds : sets) {
            final Optional<DistributionSetType> opt = distributionSetTypeManagement.getByKey(ds.getType());
            opt.ifPresent(dsType -> {
                if (dsType.isDeleted()) {
                    final String text = "Cannot create Distribution Set from type with key {0}. Distribution Set Type already deleted!";
                    final String message = MessageFormat.format(text, dsType.getKey());
                    throw new ValidationException(message);
                }
            });
            if (ds.getModules() != null) {
                ds.getModules().forEach(sm -> {
                    if (sm.getSoftwareVersionTargetId() == null || sm.getSoftwareVersionTargetId().isBlank() || sm.getId() == null) {
                        throw new ValidationException("Software Module Id / Software Version Target Id cannot be null");
                    }
                    SoftwareModule module = softwareModuleManagement.get(sm.getId())
                            .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, sm.getId()));
                    ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation = artifactsManagement.findFirstBySoftwareModuleIdAndTargetVersionId(module.getId(), Long.parseLong(sm.getSoftwareVersionTargetId()))
                            .orElseThrow(() -> new EntityNotFoundException(
                                    String.format("No artifact association found for software module %d with target version %s", module.getId(), sm.getSoftwareVersionTargetId())
                            ));
                    // Log added to satisfy SonarQube requirement that artifactSoftwareModuleAssociation variable must be used
                    LOG.debug("ArtifactSoftwareModuleAssociations {}", artifactSoftwareModuleAssociation);
                });
            }
        }

        final Collection<DistributionSet> createdDSets = distributionSetManagement
                .create(MgmtDistributionSetMapper.dsFromRequest(sets, entityFactory));

        LOG.debug("{} distribution sets created, return status {}", sets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDistributionSets(createdDSets, tenantId),
                HttpStatus.CREATED);
    }

    @Override
    @TenantAware
        public ResponseEntity<Void> deleteDistributionSet(@PathVariable("tenantId") Long tenantId, @PathVariable("distributionSetId") final Long distributionSetId) {
        jpaRolloutManagement.validateNoAssociatedRolloutsOrThrow(distributionSetId, MgmtRestConstants.DELETE_DS);
        distributionSetManagement.delete(distributionSetId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSet> updateDistributionSet(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final MgmtDistributionSetRequestBodyPut toUpdate) {

        DistributionSetUpdate distributionSetUpdate = entityFactory.distributionSet()
                .update(distributionSetId).name(toUpdate.getName()).description(toUpdate.getDescription())
                .requiredMigrationStep(toUpdate.isRequiredMigrationStep())
                .softwareDowngradeEnabled(toUpdate.isSoftwareDowngradeEnabled());
        if (!Objects.isNull(toUpdate.getVersion())) {
            distributionSetUpdate.version(toUpdate.getVersion());
        }
        final DistributionSet updated = distributionSetManagement.update(distributionSetUpdate);

        final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(updated, tenantId);
        MgmtDistributionSetMapper.addLinks(updated, response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTarget>> getAssignedTargets(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("tenantId") final Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsAssignedDS;
        if (rsqlParam != null) {
            targetsAssignedDS = this.targetManagement.findByAssignedDistributionSetAndRsql(pageable, distributionSetId,
                    rsqlParam);
        } else {
            targetsAssignedDS = this.targetManagement.findByAssignedDistributionSet(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsAssignedDS.getContent(), tenantConfigHelper, tenantId),
                        targetsAssignedDS.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTarget>> getInstalledTargets(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("tenantId") final Long tenantId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        // check if distribution set exists otherwise throw exception
        // immediately
        distributionSetManagement.getOrElseThrowException(distributionSetId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<Target> targetsInstalledDS;
        if (rsqlParam != null) {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSetAndRsql(pageable,
                    distributionSetId, rsqlParam);
        } else {
            targetsInstalledDS = this.targetManagement.findByInstalledDistributionSet(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetMapper.toResponse(targetsInstalledDS.getContent(), tenantConfigHelper, tenantId),
                        targetsInstalledDS.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTargetFilterQuery>> getAutoAssignTargetFilterQueries(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetFilterQuerySortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<TargetFilterQuery> targetFilterQueries = targetFilterQueryManagement
                .findByAutoAssignDSAndRsql(pageable, distributionSetId, rsqlParam);

        return ResponseEntity
                .ok(new PagedList<>(MgmtTargetFilterQueryMapper.toResponse(targetFilterQueries.getContent(),
                        tenantConfigHelper.isConfirmationFlowEnabled(), tenantId), targetFilterQueries.getTotalElements()));
    }

    @Override
    @Deprecated
    @TenantAware
    public ResponseEntity<MgmtTargetAssignmentResponseBody> createAssignedTarget(
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final List<MgmtTargetAssignmentRequestBody> assignments,
            @RequestParam(value = "offline", required = false) final Boolean offline,
            @PathVariable("tenantId") final Long tenantId) {
        if (Boolean.TRUE.equals(offline)) {
            final List<Entry<String, Long>> offlineAssignments = assignments.stream()
                    .map(assignment -> new SimpleEntry<>(assignment.getId(), distributionSetId))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(MgmtDistributionSetMapper
                    .toResponse(deployManagament.offlineAssignedDistributionSets(offlineAssignments), tenantId));
        }

        final List<DeploymentRequest> deploymentRequests = assignments.stream().map(dsAssignment -> {
            final boolean isConfirmationRequired = dsAssignment.isConfirmationRequired() == null
                    ? tenantConfigHelper.isConfirmationFlowEnabled()
                    : dsAssignment.isConfirmationRequired();
            return MgmtDeploymentRequestMapper.createAssignmentRequestBuilder(dsAssignment, distributionSetId)
                    .setConfirmationRequired(isConfirmationRequired).build();
        }).toList();

        final List<DistributionSetAssignmentResult> assignmentResults = deployManagament
                .assignDistributionSets(deploymentRequests);
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponse(assignmentResults, tenantId));
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<DistributionSetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = distributionSetManagement.findMetaDataByDistributionSetIdAndRsql(pageable, distributionSetId,
                    rsqlParam);
        } else {
            metaDataPage = distributionSetManagement.findMetaDataByDistributionSetId(pageable, distributionSetId);
        }

        return ResponseEntity
                .ok(new PagedList<>(MgmtDistributionSetMapper.toResponseDsMetadata(metaDataPage.getContent()),
                        metaDataPage.getTotalElements()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtMetadata> getMetadataValue(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("key") final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSetMetadata findOne = distributionSetManagement
                .getMetaDataByDistributionSetId(distributionSetId, metadataKey)
                .orElseThrow(() -> new EntityNotFoundException(DistributionSetMetadata.class, distributionSetId,
                        metadataKey));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(findOne));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtMetadata> updateMetadata(@PathVariable("tenantId") long tenantId, @PathVariable("distributionSetId") final Long distributionSetId,
                                                       @PathVariable("key") final String metadataKey, @RequestBody final MgmtMetadataBodyPut metadata) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final DistributionSetMetadata updated = distributionSetManagement.updateMetaData(distributionSetId,
                entityFactory.generateDsMetadata(metadataKey, metadata.getValue()));
        return ResponseEntity.ok(MgmtDistributionSetMapper.toResponseDsMetadata(updated));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteMetadata(@PathVariable("tenantId") long tenantId,
                                               @PathVariable("distributionSetId") final Long distributionSetId,
                                               @PathVariable("key") final String metadataKey) {
        // check if distribution set exists otherwise throw exception
        // immediately
        distributionSetManagement.deleteMetaData(distributionSetId, metadataKey);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtMetadata>> createMetadata(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestBody final List<MgmtMetadata> metadataRest) {
        // check if distribution set exists otherwise throw exception
        // immediately
        final List<DistributionSetMetadata> created = distributionSetManagement.createMetaData(distributionSetId,
                MgmtDistributionSetMapper.fromRequestDsMetadata(metadataRest, entityFactory));
        return new ResponseEntity<>(MgmtDistributionSetMapper.toResponseDsMetadata(created), HttpStatus.CREATED);

    }

    @Override
    @TenantAware
    public ResponseEntity<Void> assignSoftwareModules(@PathVariable("tenantId") long tenantId, @PathVariable("distributionSetId") final Long distributionSetId,
                                                      @RequestBody final List<MgmtSoftwareModuleAssignments> softwareModuleAssigments)
            throws InterruptedException {

        List<Rollout> rollouts = rolloutManagement.findByDistributionSetId(distributionSetId);

        for (Rollout rollout : rollouts) {
            if (!EnumSet.of(RolloutStatus.DRAFT, RolloutStatus.DELETING).contains(rollout.getStatus())) {
                throw new ValidationException("Rollout must be in one of the following stages:  DRAFT, DELETING.");
            }
        }

        List<DistributionSoftwareVersionWrapper> wrap = new ArrayList<>();
        for (MgmtSoftwareModuleAssignments swModuleAssignment : softwareModuleAssigments) {
            if (swModuleAssignment.getSoftwareVersionTargetId() == null || swModuleAssignment.getId() == null
                    || swModuleAssignment.getSoftwareVersionTargetId().isBlank()) {
                throw new ValidationException("Software Module Id / Software Version Target Id cannot be null or blank.");
            }
            SoftwareModule module = softwareModuleManagement.get(swModuleAssignment.getId())
                    .orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, swModuleAssignment.getId()));

            List<IDistributionSetModule> distributionSetModules = distributionSetManagement.getDistributionSetModule(distributionSetId);

            boolean isModuleLinked = distributionSetModules.stream()
                    .anyMatch(distSetModule -> distSetModule.getSm().getId().equals(swModuleAssignment.getId()));

            if (isModuleLinked) {
                throw new ValidationException("The software module " + swModuleAssignment.getId() + " is already linked to the distribution set " + distributionSetId + ".");
            }

            Set<EcuModel> ecuModels = module.getAssociatedEcuModels();

            if (ecuModels.isEmpty()) {
                throw new ValidationException("Software module " + swModuleAssignment.getId() + " has no associated ECU models and cannot be linked to the distribution set.");
            }

            ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation = artifactsManagement.findFirstBySoftwareModuleIdAndTargetVersionId(module.getId(), Long.parseLong(swModuleAssignment.getSoftwareVersionTargetId()))
                    .orElseThrow(() -> new EntityNotFoundException(ArtifactSoftwareModuleAssociation.class, module.getId()));

            Version version = artifactSoftwareModuleAssociation.getTargetVersion();

            wrap.add(new DistributionSoftwareVersionWrapper(module, version));
        }

        distributionSetManagement.assignSoftwareModules(distributionSetId, wrap);

        return ResponseEntity.ok().build();
    }

    private Version getVersion(Long versionId) {
        return versionManagement.getById(versionId).orElseThrow(() -> new EntityNotFoundException(Version.class, versionId));
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteAssignSoftwareModules(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @PathVariable("softwareModuleId") final Long softwareModuleId,
            @RequestParam("version") final Long version) {
        distributionSetManagement.unassignSoftwareModule(distributionSetId, softwareModuleId, version);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtAssignedSoftwareModule>> getAssignedSoftwareModules(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {
        List<IDistributionSetModule> dsModules = distributionSetManagement.getDistributionSetModule(distributionSetId);
        return ResponseEntity.ok(new PagedList<>(MgmtSoftwareModuleMapper.toAssignedResponse(tenantId, dsModules, artifactsManagement),
                dsModules.size()));
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetStatistics> getRolloutsCountByStatusForDistributionSet(@PathVariable("tenantId") long tenantId, Long distributionSetId) {
        MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder();
        distributionSetManagement.countRolloutsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalRolloutPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetStatistics> getActionsCountByStatusForDistributionSet(@PathVariable("tenantId") long tenantId, Long distributionSetId) {
        MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder();
        distributionSetManagement.countActionsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalActionPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetStatistics> getAutoAssignmentsCountForDistributionSet(@PathVariable("tenantId") long tenantId, Long distributionSetId) {
        MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder();
        statistics.addTotalAutoAssignments(distributionSetManagement.countAutoAssignmentsForDistributionSet(distributionSetId));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    @TenantAware
    public ResponseEntity<MgmtDistributionSetStatistics> getStatisticsForDistributionSet(@PathVariable("tenantId") long tenantId, Long distributionSetId) {
        MgmtDistributionSetStatistics.Builder statistics = new MgmtDistributionSetStatistics.Builder();
        distributionSetManagement.countRolloutsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalRolloutPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        distributionSetManagement.countActionsByStatusForDistributionSet(distributionSetId).forEach(statistic ->
                statistics.addTotalActionPerStatus(String.valueOf(statistic.getName()), Long.parseLong(statistic.getData().toString())));
        statistics.addTotalAutoAssignments(distributionSetManagement.countAutoAssignmentsForDistributionSet(distributionSetId));
        return ResponseEntity.ok(statistics.build());
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> invalidateDistributionSet(
            @PathVariable("tenantId") long tenantId,
            @PathVariable("distributionSetId") final Long distributionSetId,
            @Valid @RequestBody final MgmtInvalidateDistributionSetRequestBody invalidateRequestBody) {
        distributionSetInvalidationManagement
                .invalidateDistributionSet(new DistributionSetInvalidation(Collections.singletonList(distributionSetId),
                        MgmtRestModelMapper.convertCancelationType(invalidateRequestBody.getActionCancelationType()),
                        invalidateRequestBody.isCancelRollouts()), tenantId);
        return ResponseEntity.ok().build();
    }
}