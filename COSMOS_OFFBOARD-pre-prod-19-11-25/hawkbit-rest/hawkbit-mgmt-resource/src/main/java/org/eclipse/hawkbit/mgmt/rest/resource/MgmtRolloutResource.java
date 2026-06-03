/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.annotations.TraceableObject;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.action.dto.DeviceActionStatusTimestampResponse;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPost;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutApprovalDecision;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryIndividualDeviceRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutModuleVersion;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutResponseBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutTargetActionsResponse;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutTotalTargetCountActionStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutUpdateRequest;
import org.cosmos.models.mgmt.rollout.dto.RetryMultipleDevicesRequest;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupResponseBody;
import org.cosmos.models.mgmt.softwaremodule.dto.AssociatedModuleResponse;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssignments;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssociationResponse;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.FeatureNotAllowedException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSoftwareVersionWrapper;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
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
import org.springframework.web.multipart.MultipartFile;

import static org.eclipse.hawkbit.repository.jpa.JpaTargetManagement.DUPLICATE_KEY_IN_CONTROLLER_ID_MAP;
import static org.eclipse.hawkbit.repository.jpa.JpaTargetManagement.REGISTERED_KEY_IN_CONTROLLER_ID_MAP;
import static org.eclipse.hawkbit.repository.jpa.JpaTargetManagement.UNREGISTERED_KEY_IN_CONTROLLER_ID_MAP;

/**
 * REST Resource handling rollout CRUD operations.
 */
@RestController
@Tag(name = "Rollouts")
@Slf4j
public class MgmtRolloutResource implements MgmtRolloutRestApi {

    private final RolloutManagement rolloutManagement;
    private final ActionRepository actionRepository;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final RolloutGroupRepository rolloutGroupRepository;
    private final ArtifactsManagement artifactsManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final TenantConfigHelper tenantConfigHelper;
    private final TargetManagement targetManagement;
    private final EntityFactory entityFactory;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final TenantMetaDataRepository tenantMetaDataRepository;
    private final SystemManagement systemManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final RolloutRepository rolloutRepository;
    private final TargetRepository targetRepository;
    private final SupportPackageManagement supportPackageManagement;
    private final HandleRolloutSchedulerService handleRolloutSchedulerService;

    MgmtRolloutResource(final RolloutManagement rolloutManagement, ActionRepository actionRepository, final RolloutGroupManagement rolloutGroupManagement, SoftwareModuleManagement softwareModuleManagement, RolloutGroupRepository rolloutGroupRepository, ArtifactsManagement artifactsManagement, final SystemSecurityContext systemSecurityContext, final TenantConfigurationManagement tenantConfigurationManagement, final TargetManagement targetManagement, final EntityFactory entityFactory, final DistributionSetManagement distributionSetManagement, final DistributionSetTypeManagement distributionSetTypeManagement, final TenantMetaDataRepository tenantMetaDataRepository, final SystemManagement systemManagement, RolloutRepository rolloutRepository, TargetRepository targetRepository, final SupportPackageManagement supportPackageManagement, final HandleRolloutSchedulerService handleRolloutSchedulerService) {
        this.rolloutManagement = rolloutManagement;
        this.actionRepository = actionRepository;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.rolloutGroupRepository = rolloutGroupRepository;
        this.distributionSetManagement = distributionSetManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactsManagement = artifactsManagement;
        this.targetManagement = targetManagement;
        this.entityFactory = entityFactory;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
        this.systemManagement = systemManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
        this.rolloutRepository = rolloutRepository;
        this.targetRepository = targetRepository;
        this.supportPackageManagement = supportPackageManagement;
        this.handleRolloutSchedulerService = handleRolloutSchedulerService;
    }

    private static MgmtRepresentationMode parseRepresentationMode(final String representationModeParam) {
        return MgmtRepresentationMode.fromValue(representationModeParam).orElseGet(() -> {
            // no need for a 400, just apply a safe fallback
            log.warn("Received an invalid representation mode: {}", representationModeParam);
            return MgmtRepresentationMode.COMPACT;
        });
    }

    /**
     * checks whether the Rollout is in FINISHED or CANCELED state.
     */

    private static boolean isRolloutFinishedOrCanceled(RolloutStatus status) {
        return EnumSet.of(RolloutStatus.FINISHED, RolloutStatus.CANCELED, RolloutStatus.CANCELING, RolloutStatus.FINISHING).contains(status);
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtRolloutResponseBody>> getRollouts(@PathVariable("tenantId") Long tenantId, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam, final String representationModeParam) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutSortParam(sortParam);

        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Rollout> rollouts;
        final long totalElements;
        if (rsqlParam != null) {
            if (isFullMode) {
                rollouts = this.rolloutManagement.findByFiltersWithDetailedStatus(pageable, rsqlParam, false);
                totalElements = this.rolloutManagement.countByFilters(rsqlParam);
            } else {
                final Page<Rollout> findRolloutsAll = this.rolloutManagement.findByRsql(pageable, rsqlParam, false);
                totalElements = findRolloutsAll.getTotalElements();
                rollouts = findRolloutsAll;
            }
        } else {
            if (isFullMode) {
                rollouts = this.rolloutManagement.findAllWithDetailedStatus(pageable, false);
                totalElements = this.rolloutManagement.count();
            } else {
                final Page<Rollout> findRolloutsAll = this.rolloutManagement.findAll(pageable, false);
                totalElements = findRolloutsAll.getTotalElements();
                rollouts = findRolloutsAll;
            }
        }

        final List<MgmtRolloutResponseBody> rest = MgmtRolloutMapper.mapToRolloutResponseList(tenantId, rollouts.getContent(), isFullMode);

        return ResponseEntity.ok(new PagedList<>(rest, totalElements));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtRolloutResponseBody> getRollout(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received get rollout details request");
        final Rollout findRolloutById = rolloutManagement.getWithDetailedStatus(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        List<MgmtRolloutModuleVersion> modulesAndVersions = getModulesAndVersionsByRolloutId(rolloutId);
        MgmtRolloutTotalTargetCountActionStatus targetCounts = getTargetCountsByRolloutId(rolloutId);
        return ResponseEntity.ok(MgmtRolloutMapper.mapToGetRolloutResponse(tenantId, findRolloutById, modulesAndVersions, targetCounts));
    }

    /**
     * Retrieves a list of modules and their corresponding software version targets associated with a given rollout ID.
     *
     * @param rolloutId The ID of the rollout for which to retrieve modules and software version targets.
     * @return A list of {@link MgmtRolloutModuleVersion} objects, each containing the module ID and its associated software version target ID.
     */
    private List<MgmtRolloutModuleVersion> getModulesAndVersionsByRolloutId(Long rolloutId) {
        List<IDistributionSetModule> modules = distributionSetManagement.getModulesAndVersionsByRolloutId(rolloutId);
        return modules.stream().map(dsm -> new MgmtRolloutModuleVersion(dsm.getSm().getId(), dsm.getVersion().getId())).collect(Collectors.toList());
    }

    /**
     * Retrieves the total target counts and their statuses for a given rollout ID.
     *
     * @param rolloutId The ID of the rollout for which target counts and statuses need to be fetched.
     * @return An instance of {@link MgmtRolloutTotalTargetCountActionStatus} containing the total target counts and counts per status.
     */
    private MgmtRolloutTotalTargetCountActionStatus getTargetCountsByRolloutId(Long rolloutId) {
        Map<String, Long> totalTargetsPerStatus = Arrays.stream(TotalTargetCountStatus.Status.values()).collect(Collectors.toMap(status -> status.name().toLowerCase(), status -> 0L));

        List<JpaRolloutGroup> rolloutGroups = rolloutGroupRepository.findByRolloutId(rolloutId);
        long totalTargets = 0;

        for (JpaRolloutGroup rolloutGroup : rolloutGroups) {
            totalTargets += rolloutGroup.getTotalTargets();
            String statusKey = rolloutGroup.getStatus().name().toLowerCase();
            if (totalTargetsPerStatus.containsKey(statusKey)) {
                totalTargetsPerStatus.put(statusKey, totalTargetsPerStatus.get(statusKey) + rolloutGroup.getTotalTargets());
            } else {
                log.debug("Status '{}' is not recognized, skipping or handling it separately.", statusKey);
            }
        }

        MgmtRolloutTotalTargetCountActionStatus response = new MgmtRolloutTotalTargetCountActionStatus();
        response.setTotalTargets(totalTargets); // Use the summed value of totalTargets
        response.setTotalTargetsPerStatus(totalTargetsPerStatus);
        return response;
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtRolloutResponseBody> create(@PathVariable("tenantId") @TraceableField Long tenantId, @RequestBody @NotNull @Valid @TraceableObject final MgmtRolloutRestRequestBody rolloutRequestBody) {
        log.debug("Received create rollout request");
        Rollout savedRollout = rolloutManagement.create(MgmtRolloutMapper.fromRequest(rolloutRequestBody));
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.mapToCreateRolloutResponse(tenantId, savedRollout));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> approve(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, final String remark) {
        log.debug("Received approve rollout request");
        rolloutManagement.approveOrDeny(rolloutId, MgmtRolloutApprovalDecision.APPROVED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deny(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, final String remark) {
        log.debug("Received deny rollout request");
        rolloutManagement.approveOrDeny(rolloutId, MgmtRolloutApprovalDecision.DENIED, remark);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> freeze(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received freeze rollout request");
        this.rolloutManagement.freeze(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> start(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received start rollout request");
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        if (!rollout.getStartType().equals(MgmtRolloutStartType.MANUAL)) {
            throw new ValidationException(String.format("Rollout with id %s cannot be started manually as the start " + "type is %s", rollout.getId(), rollout.getStartType()));
        }
        this.rolloutManagement.start(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> pauseDeviceAction(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("controllerId") @TraceableField final String controllerId) {
        log.debug("Received pause device action request");
        this.rolloutManagement.pauseDeviceAction(rolloutId, controllerId);
        return ResponseEntity.accepted().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> cancelDeviceAction(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("controllerId") @TraceableField final String controllerId) {
        log.debug("Received cancel device action request");
        this.rolloutManagement.cancelDeviceAction(rolloutId, controllerId);
        return ResponseEntity.accepted().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> pause(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received pause rollout request");
        this.rolloutManagement.pauseRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> cancel(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received cancel rollout request");
        this.rolloutManagement.cancelRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> delete(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received delete rollout request");
        this.rolloutManagement.delete(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> resume(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received resume rollout request");
        this.rolloutManagement.resumeRollout(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtRolloutGroupResponseBody>> getRolloutGroups(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam, final String representationModeParam) {

        log.debug("Received get all RolloutGroups for a rollout request");
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeRolloutGroupSortParam(sortParam);

        final boolean isFullMode = parseRepresentationMode(representationModeParam) == MgmtRepresentationMode.FULL;

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<RolloutGroup> rolloutGroups;
        if (rsqlParam != null) {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsqlWithDetailedStatus(pageable, rolloutId, rsqlParam);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutAndRsql(pageable, rolloutId, rsqlParam);
            }
        } else {
            if (isFullMode) {
                rolloutGroups = this.rolloutGroupManagement.findByRolloutWithDetailedStatus(pageable, rolloutId);
            } else {
                rolloutGroups = this.rolloutGroupManagement.findByRollout(pageable, rolloutId);
            }
        }

        final List<MgmtRolloutGroupResponseBody> rest = MgmtRolloutMapper.toResponseRolloutGroup(tenantId, rolloutGroups.getContent(), tenantConfigHelper.isConfirmationFlowEnabled(), isFullMode);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroups.getTotalElements()));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtAddDeviceDetailsResponse> addDeviceDetailsApi(Long tenantId, @TraceableField Long rolloutId, MultipartFile targetDevices, String groupsBody) {

        log.debug("Received request to add device details ");
        //validate the rollout and distribution set
        Rollout rollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        //Check for the allowed rollout state for this API
        RolloutHelper.verifyRolloutInStatusForAddDeviceDetails(rollout);

        if (rollout.getDistributionSet() == null) {
            log.error("Distribution Set is not present in the rollout");
            throw new ValidationException("Distribution Set is not present in the rollout");
        }

        //validate the file attached in the request
        if (targetDevices == null || targetDevices.isEmpty()) {
            log.error("The target devices file must be provided and cannot be empty.");
            throw new ValidationException("The target devices file must be provided and cannot be empty.");
        }

        // Validate the MIME type and extension of the uploaded file
        validateMimeTypeAndExtension(targetDevices);

        //list of all the controller ids in the file
        List<String> attachedTargetDevices = RolloutHelper.parseControllerIdsFromCSV(targetDevices);

        if (attachedTargetDevices.isEmpty()) {
            log.error("No Target devices found in the file");
            throw new ValidationException("No Target devices found in the file");
        }

        //Check the provided controller ids and segregate them into registered and unregistered controller ids
        Map<String, List<String>> controllerIdsMap = targetManagement.separateRegisteredAndUnregisteredControllerIds(attachedTargetDevices.stream().distinct().toList());
        List<String> associatedControllerIds = rolloutManagement.getControllerIdsByRolloutId(rolloutId);

        List<String> newRegisteredControllerIds = controllerIdsMap.get(REGISTERED_KEY_IN_CONTROLLER_ID_MAP).stream().filter(id -> !associatedControllerIds.contains(id)).toList();
        List<String> duplicateControllerIds = controllerIdsMap.get(REGISTERED_KEY_IN_CONTROLLER_ID_MAP).stream().filter(associatedControllerIds::contains).toList();

        controllerIdsMap.put(REGISTERED_KEY_IN_CONTROLLER_ID_MAP, newRegisteredControllerIds);
        controllerIdsMap.put(DUPLICATE_KEY_IN_CONTROLLER_ID_MAP, duplicateControllerIds);


        log.debug("Registered controller ids: {}", newRegisteredControllerIds);

        if (newRegisteredControllerIds.isEmpty()) {
            log.error("No new registered target devices found in the file");
            throw new ValidationException("No new registered target devices found in the file");
        }
        rolloutManagement.validateMandatoryEspForRegisteredControllers(rollout, newRegisteredControllerIds);
        List<AssociatedTargetsToRolloutGroup> assignedTargetsToRolloutGroups = rolloutManagement.addDeviceDetails(rollout, newRegisteredControllerIds, groupsBody);

        if (rollout.getStatus() == RolloutStatus.RUNNING || rollout.getStatus() == RolloutStatus.PAUSED) {
            // ✅ If running/paused,update rollout groups.
            rolloutManagement.updateRolloutGroupsForNewlyRegisteredControllerIds(rollout, groupsBody, assignedTargetsToRolloutGroups);
        }

        return ResponseEntity.ok().body(MgmtRolloutMapper.toResponseDeviceDetails(assignedTargetsToRolloutGroups, rollout, controllerIdsMap.get(UNREGISTERED_KEY_IN_CONTROLLER_ID_MAP), controllerIdsMap.get(DUPLICATE_KEY_IN_CONTROLLER_ID_MAP), tenantId));
    }

    /**
     * Validates that the uploaded file is in CSV format by checking both its MIME type and file extension.
     *
     * @param targetDevices the uploaded file to validate.
     * @throws ValidationException if the file is not in CSV format.
     */
    private void validateMimeTypeAndExtension(MultipartFile targetDevices) throws ValidationException {

        String contentType = targetDevices.getContentType();
        String fileName = targetDevices.getOriginalFilename();

        boolean isCsvMimeType = contentType != null && contentType.equalsIgnoreCase("text/csv");
        boolean isCsvExtension = fileName != null && fileName.toLowerCase().endsWith(".csv");

        if (!(isCsvMimeType && isCsvExtension)) {
            log.error("Invalid file format: Expected CSV but received Content-Type: {}, File Name: {}", contentType, fileName);
            throw new ValidationException("The file must be in CSV format.");
        }
    }

    @Override
    @TenantAware
    public ResponseEntity<Void> deleteDeviceDetailsApi(Long tenantId, Long rolloutId, MultipartFile targetDevices, Boolean deleteEsp) {

        log.debug("Received request to delete device details for rollout ID: {}", rolloutId);

        // Validate and fetch the rollout
        Rollout rollout = validateAndFetchRollout(rolloutId);

        // Verify rollout status in draft status
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.DRAFT);

        // Validate the uploaded file
        validateTargetDevicesFile(targetDevices);

        // Validate the MIME type
        validateMimeTypeAndExtension(targetDevices);

        // Parse and validate target devices from the file
        List<String> attachedTargetDevices = parseAndValidateTargetDevices(targetDevices);

        // Separate registered and unregistered controller IDs
        List<String> registeredControllerIds = extractAndValidateRegisteredControllerIds(attachedTargetDevices);

        // Remove device details from the rollout
        rolloutManagement.deleteDeviceDetails(rollout, registeredControllerIds, deleteEsp);

        log.debug("Successfully deleted device details for rollout ID: {}", rolloutId);
        return ResponseEntity.ok().build();
    }

    // Fetch and validate rollout
    private Rollout validateAndFetchRollout(Long rolloutId) {
        log.debug("Validating rollout with ID: {}", rolloutId);
        return rolloutManagement.get(rolloutId).orElseThrow(() -> {
            log.error("Rollout not found for ID: {}", rolloutId);
            return new EntityNotFoundException(Rollout.class, rolloutId);
        });
    }

    // Validate the uploaded file
    private void validateTargetDevicesFile(MultipartFile targetDevices) {
        if (targetDevices == null || targetDevices.isEmpty()) {
            log.error("The target devices file must be provided and cannot be empty.");
            throw new ValidationException("The target devices file must be provided and cannot be empty.");
        }
        log.debug("Target devices file validation passed.");
    }

    // Parse and validate target devices
    private List<String> parseAndValidateTargetDevices(MultipartFile targetDevices) {
        List<String> devices = RolloutHelper.parseControllerIdsFromCSV(targetDevices);
        if (devices.isEmpty()) {
            log.error("No target devices found in the uploaded file.");
            throw new ValidationException("No target devices found in the uploaded file.");
        }
        log.debug("Parsed {} target devices from the file.", devices.size());
        return devices.stream().distinct().toList(); // Remove duplicates
    }

    // Extract and validate registered controller IDs
    private List<String> extractAndValidateRegisteredControllerIds(List<String> attachedTargetDevices) {
        log.debug("Separating registered and unregistered controller IDs.");
        Map<String, List<String>> controllerIdsMap = targetManagement.separateRegisteredAndUnregisteredControllerIds(attachedTargetDevices);

        List<String> registeredControllerIds = controllerIdsMap.get(REGISTERED_KEY_IN_CONTROLLER_ID_MAP);
        if (registeredControllerIds == null || registeredControllerIds.isEmpty()) {
            log.error("No registered target devices found in the file.");
            throw new ValidationException("No registered target devices found in the file.");
        }
        log.debug("Found {} registered controller IDs.", registeredControllerIds.size());
        return registeredControllerIds;
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtRolloutGroupResponseBody> getRolloutGroup(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("groupId") final Long groupId) {
        log.debug("Received get a single RolloutGroup for a rollout request");
        findRolloutOrThrowException(rolloutId);

        final RolloutGroup rolloutGroup = rolloutGroupManagement.getWithDetailedStatus(groupId).orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, rolloutId));

        if (!Objects.equals(rolloutId, rolloutGroup.getRollout().getId())) {
            throw new EntityNotFoundException(RolloutGroup.class, groupId);
        }

        return ResponseEntity.ok(MgmtRolloutMapper.toResponseRolloutGroup(tenantId, rolloutGroup, true, tenantConfigHelper.isConfirmationFlowEnabled()));
    }

    private void findRolloutOrThrowException(final Long rolloutId) {
        if (!rolloutManagement.exists(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtTarget>> getRolloutGroupTargets(@PathVariable("tenantId") final Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("groupId") final Long groupId, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam, @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam) {
        log.debug("Received get all RolloutGroupTargets for a rollout request");
        findRolloutOrThrowException(rolloutId);
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Page<Target> rolloutGroupTargets;
        if (rsqlParam != null) {
            rolloutGroupTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroupByRsql(pageable, groupId, rsqlParam);
        } else {
            final Page<Target> pageTargets = this.rolloutGroupManagement.findTargetsOfRolloutGroup(pageable, groupId);
            rolloutGroupTargets = pageTargets;
        }
        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(rolloutGroupTargets.getContent(), tenantConfigHelper, tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, rolloutGroupTargets.getTotalElements()));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> triggerNextGroup(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received triggerNextGroup of a rollout request");
        this.rolloutManagement.triggerNextGroup(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> startAllGroups(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId) {
        log.debug("Received request to start all the groups of a rollout");
        this.rolloutManagement.startAllGroups(rolloutId);
        return ResponseEntity.accepted().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unfreeze(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField Long rolloutId) {
        log.debug("Received unfreeze rollout request");
        rolloutManagement.unfreeze(rolloutId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> resumeDeviceAction(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("controllerId") @TraceableField final String controllerId) {
        log.debug("Received resume device action request");
        this.rolloutManagement.resumeDeviceAction(rolloutId, controllerId);
        return ResponseEntity.accepted().build();

    }

    /**
     * Associates software modules with a rollout.
     *
     * @param softwareModuleRequests List of software modules to be associated.
     * @param rolloutId              ID of the rollout.
     * @param tenantId               ID of the tenant.
     * @return ResponseEntity containing the updated distribution set information.
     * @throws IllegalArgumentException if softwareModuleRequests is null or empty.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtSoftwareModuleAssociationResponse> associateSoftwareModules(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField Long rolloutId, @Valid @RequestBody List<MgmtSoftwareModuleRequest> softwareModuleRequests) {

        log.debug("Received associateSoftwareModules for a rollout request");
        validateAssociateSoftwareModulesRequest(softwareModuleRequests);
        final Rollout rollout = rolloutManagement.getWithDetailedStatus(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.DRAFT);
        rolloutManagement.validateSoftwareModuleAssociation(softwareModuleRequests);
        DistributionSet distributionSet = rollout.getDistributionSet();

        if (distributionSet == null) {
            distributionSet = createAndPopulateDistributionSet(softwareModuleRequests, rollout);
        } else {
            log.debug("Existing DistributionSet [{}] found. Adding new modules.", distributionSet.getId());
            distributionSet = updateDistributionSetWithModules(distributionSet, softwareModuleRequests);
        }

        // Build and return the response
        return ResponseEntity.ok(buildResponse(distributionSet));
    }

    /**
     * Validates that the software module requests list is not null or empty.
     *
     * @param softwareModuleRequests List of software module requests.
     * @throws IllegalArgumentException if the list is null or empty.
     */
    private void validateAssociateSoftwareModulesRequest(List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        if (softwareModuleRequests == null || softwareModuleRequests.isEmpty()) {
            throw new IllegalArgumentException("The request must contain at least one SoftwareModuleRequest.");
        }
        for (MgmtSoftwareModuleRequest module : softwareModuleRequests) {
            validateSoftwareModule(module);
        }
    }

    /**
     * Updates the existing DistributionSet by adding new software modules.
     *
     * @param distributionSet        The existing DistributionSet.
     * @param softwareModuleRequests The new software modules to add.
     * @return The updated DistributionSet.
     */
    private DistributionSet updateDistributionSetWithModules(DistributionSet distributionSet, List<MgmtSoftwareModuleRequest> softwareModuleRequests) {

        // Convert incoming software modules to DistributionSoftwareVersionWrapper format
        List<DistributionSoftwareVersionWrapper> distributionSoftwareVersionWrappers = convertToSoftwareVersionWrappers(softwareModuleRequests);

        // Preserve existing modules before updating
        List<IDistributionSetModule> existingModules = new ArrayList<>(distributionSet.getDistributionSetModules());

        // Assign new software modules
        DistributionSet updatedDistributionSet = distributionSetManagement.assignSoftwareModules(distributionSet.getId(), distributionSoftwareVersionWrappers);

        // Merge previous modules into the updated DistributionSet
        updatedDistributionSet.getDistributionSetModules().addAll(existingModules);
        log.debug("Updated DistributionSet [{}] with {} new modules.", updatedDistributionSet.getId(), distributionSoftwareVersionWrappers.size());

        return updatedDistributionSet;
    }

    /**
     * Builds the response object with distribution set details and associated modules.
     *
     * @param distributionSet The DistributionSet to include in the response.
     * @return A response object containing distribution set details and associated modules.
     */
    private MgmtSoftwareModuleAssociationResponse buildResponse(DistributionSet distributionSet) {
        MgmtSoftwareModuleAssociationResponse response = new MgmtSoftwareModuleAssociationResponse();
        response.setDistributionSetId(distributionSet.getId());
        response.setDistributionSetType(distributionSet.getType().getName());

        // Map each module to its response object
        List<AssociatedModuleResponse> associatedModules = distributionSet.getDistributionSetModules().stream().map(module -> new AssociatedModuleResponse(module.getSm().getId(), module.getVersion().getId())).toList();

        response.setAssociatedModules(associatedModules);

        return response;
    }

    /**
     * Creates a new DistributionSet and populates it with the given software modules.
     * This method handles the creation of a DistributionSet, associates the provided
     * software modules with it, and updates the given Rollout with the newly created DistributionSet.
     */
    private DistributionSet createAndPopulateDistributionSet(List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {

        log.debug("No existing DistributionSet found. Creating a new one.");
        List<MgmtSoftwareModuleAssignments> assignments = softwareModuleRequests.stream().map(module -> {
            MgmtSoftwareModuleAssignments assignment = new MgmtSoftwareModuleAssignments();
            assignment.setId(module.getModuleId());
            assignment.setSoftwareVersionTargetId(module.getSoftwareVersionTargetId().toString());
            return assignment;
        }).toList();

        // Retrieve the key of the default DistributionSetType for the tenant
        String defaultDistributionTypeKey = getDefaultDistributionTypeKeyForTenant();

        log.info("defaultDistributionTypeKey:{}", defaultDistributionTypeKey);
        // Prepare the DistributionSetCreate request
        MgmtDistributionSetRequestBodyPost newDistributionSetRequest = new MgmtDistributionSetRequestBodyPost();
        newDistributionSetRequest.setName(generateUniqueName("DistributionSet_"));
        newDistributionSetRequest.setDescription(generateUniqueName("DistributionSet_"));
        newDistributionSetRequest.setRequiredMigrationStep(false);
        newDistributionSetRequest.setSoftwareDowngradeEnabled(false);
        newDistributionSetRequest.setType(defaultDistributionTypeKey);
        newDistributionSetRequest.setModules(assignments);
        List<DistributionSetCreate> createRequests = MgmtDistributionSetMapper.dsFromRequest(Collections.singletonList(newDistributionSetRequest), entityFactory);

        DistributionSet newDistributionSet = distributionSetManagement.create(createRequests.get(0));
        if (newDistributionSet == null) {
            throw new IllegalStateException("Failed to create a new DistributionSet.");
        }

        log.debug("New DistributionSet created with ID: {}", newDistributionSet.getId());
        rolloutManagement.updateRolloutDistributionSet(rollout, newDistributionSet);
        return newDistributionSet;
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unlinkSoftwareModules(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField Long rolloutId, @Valid @RequestBody List<MgmtSoftwareModuleRequest> softwareModuleRequests) {

        log.debug("Received unlinkSoftware Modules for a rollout request");

        final Rollout rollout = rolloutManagement.getWithDetailedStatus(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.DRAFT);
        if (softwareModuleRequests == null || softwareModuleRequests.isEmpty()) {
            throw new ValidationException("Software Module ID/Software Version Target ID cannot be Empty or Null");
        }

        DistributionSet distributionSet = rollout.getDistributionSet();
        if (distributionSet == null) {
            throw new ValidationException("Distribution Set is not present in the rollout");
        }

        for (MgmtSoftwareModuleRequest moduleRequest : softwareModuleRequests) {
            Long moduleId = moduleRequest.getModuleId();
            Long targetVersionId = moduleRequest.getSoftwareVersionTargetId();

            if (moduleId == null || targetVersionId == null) {
                throw new ValidationException("Software Module ID/Software Version Target ID cannot be null");
            }

            List<IDistributionSetModule> modules = distributionSetManagement.findDsModuleBySoftwareModuleIdAndTargetVersionId(moduleId, targetVersionId);
            if (modules.isEmpty()) {
                throw new ValidationException("One or more software modules do not exist in the current Distribution Set");
            }
            distributionSetManagement.unassignSoftwareModule(distributionSet.getId(), moduleId, targetVersionId);
        }

        log.debug("Successfully unlinked software modules from Distribution Set [{}] for Rollout [{}]", distributionSet.getId(), rolloutId);

        return ResponseEntity.ok().build();
    }

    /**
     * Converts a list of software module requests into DistributionSoftwareVersionWrapper objects.
     *
     * @param softwareModuleRequests List of requests containing module and version target IDs.
     * @return List of wrappers with SoftwareModule and associated Version.
     * @throws EntityNotFoundException If a module or association is not found.
     */
    private List<DistributionSoftwareVersionWrapper> convertToSoftwareVersionWrappers(List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        List<DistributionSoftwareVersionWrapper> wrappers = new ArrayList<>();
        for (MgmtSoftwareModuleRequest request : softwareModuleRequests) {
            SoftwareModule module = softwareModuleManagement.get(request.getModuleId()).orElseThrow(() -> new EntityNotFoundException(SoftwareModule.class, request.getModuleId()));

            ArtifactSoftwareModuleAssociation association = artifactsManagement.findFirstBySoftwareModuleIdAndTargetVersionId(module.getId(), request.getSoftwareVersionTargetId()).orElseThrow(() -> new EntityNotFoundException(ArtifactSoftwareModuleAssociation.class, module.getId()));

            Version version = association.getTargetVersion();
            wrappers.add(new DistributionSoftwareVersionWrapper(module, version));
        }
        return wrappers;
    }

    /**
     * Generates a unique name using a given prefix and a random UUID.
     */
    private String generateUniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /**
     * Validates the update rollout request and updates a rollout.
     *
     * @return the updated rollout details.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtRolloutResponseBody> updateRollout(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField Long rolloutId, @Valid @RequestBody MgmtRolloutUpdateRequest newRolloutUpdateRequest) {

        log.debug("Received update rollout request");

        Rollout existingRollout = rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));

        Rollout updatedRollout = updateRolloutBasedOnStatus(existingRollout, newRolloutUpdateRequest);
        if (updatedRollout == null) {
            throw new IllegalStateException("Rollout update failed, resulting in a null object." + rolloutId);
        }
        MgmtRolloutResponseBody responseBody = MgmtRolloutMapper.mapToUpdateRolloutResponse(tenantId, updatedRollout);
        return ResponseEntity.ok(responseBody);
    }

    /**
     * Updates the rollout based on its current status.
     */
    private Rollout updateRolloutBasedOnStatus(Rollout existingRollout, MgmtRolloutUpdateRequest mgmtRolloutUpdateRequest) {
        RolloutStatus status = existingRollout.getStatus();
        Rollout updateRequest = MgmtRolloutMapper.fromRequest(existingRollout, mgmtRolloutUpdateRequest);
        if (status == RolloutStatus.DRAFT) {
            // Full update allowed for DRAFT status
            return rolloutManagement.updateRollout(existingRollout, updateRequest);

        }

        if (isRolloutFinishedOrCanceled(status)) {
            throw new ValidationException("Cannot update rollout details for FINISHED/CANCELED/FINISHING/CANCELING state, current state: " + status);
        }

        // Validate that only endDate and log level is updated
        if (!isOnlyEndDateUpdated(existingRollout, mgmtRolloutUpdateRequest)) {
            throw new ValidationException("Only End Date and Log Level can be updated, current state: " + status);
        }

        JpaRollout jpaRollout = (JpaRollout) existingRollout;
        jpaRollout.setEndAt(mgmtRolloutUpdateRequest.getEndAt());
        jpaRollout.setVehicleLogLevel(mgmtRolloutUpdateRequest.getVehicleLogLevel());
        return rolloutManagement.updateRollout(existingRollout, jpaRollout);
    }

    /**
     * Checks if only the end date is updated in the rollout update request.
     * <p>
     * This method compares the fields of the existing rollout and the update request to determine if only the end date
     * (`endAt`) is being updated. It returns true if all other fields are either null in the request or match the values
     * in the existing rollout. If any other field is updated, it returns false.
     *
     * @param existingRollout The existing rollout object.
     * @param request         The rollout update request object.
     * @return true if only the end date is updated, false otherwise.
     */


    private boolean isOnlyEndDateUpdated(Rollout existingRollout, MgmtRolloutUpdateRequest request) {
        return request.getEndAt() != null && !request.getEndAt().equals(existingRollout.getEndAt());
    }


    /**
     * Constructs a full update request for a rollout.
     * <p>
     * This method maps the details from the `MgmtRolloutUpdateRequest` object into a
     * `RolloutUpdate` builder, including all the fields that need to be updated.
     **/
    private RolloutUpdate buildFullUpdateRequest(Long rolloutId, MgmtRolloutUpdateRequest rolloutRequest) {
        ObjectMapper mapper = new ObjectMapper();
        String requiredStateOfChargeString = null;
        try {
            requiredStateOfChargeString = mapper.writeValueAsString(rolloutRequest.getDeploymentMetadata().getRequiredStateOfCharge());
        } catch (JsonProcessingException e) {
            log.error("Error serializing requiredStateOfCharge", e);
        }
        return entityFactory.rollout().update(rolloutId).description(rolloutRequest.getDescription()).priority(rolloutRequest.getPriority()).startType(rolloutRequest.getStartType()).forcedTime(rolloutRequest.getForcedTime()).maxUpdateTime(rolloutRequest.getMaxUpdateTime()).connectivityType(rolloutRequest.getConnectivityType()).endAt(rolloutRequest.getEndAt()).downgradeAllowed(rolloutRequest.getDeploymentMetadata().getDowngradeAllowed()).downloadRetryCount(rolloutRequest.getDownloadRetryCount()).logCollectionRequired(rolloutRequest.getLog().getCollectionRequired()).logMaxAllFileSize(rolloutRequest.getLog().getMaxAllFileSize()).logMaxFailureVin(rolloutRequest.getLog().getMaxFailureVin()).logMaxEachFileSize(rolloutRequest.getLog().getMaxEachFileSize()).logMaxNumberOfFiles(rolloutRequest.getLog().getMaxNumberOfFiles()).logMaxSuccessVin(rolloutRequest.getLog().getMaxSuccessVin()).maxDownloadCellularDurationTimer(rolloutRequest.getMaxDownloadCellularDurationTimer()).maxDownloadDurationTimer(rolloutRequest.getMaxDownloadDurationTimer()).maxDownloadWifiDurationTimer(rolloutRequest.getMaxDownloadWifiDurationTimer()).weight(rolloutRequest.getWeight()).startAt(rolloutRequest.getStartAt()).requiredMedia(rolloutRequest.getDeploymentMetadata().getRequiredMedia()).requiredStateOfCharge(requiredStateOfChargeString).userAcceptanceRequired(rolloutRequest.getUserAcceptanceRequired());
    }

    private RolloutUpdate buildFullUpdateRequest(Long rolloutId, MgmtRolloutUpdateRequest rolloutRequest, Rollout rollout) {
        RolloutUpdate rolloutUpdate = entityFactory.rollout().update(rolloutId).description(rollout.getDescription()).priority(rollout.getPriority()).startType(rollout.getStartType()).forcedTime(rollout.getForcedTime()).maxUpdateTime(rollout.getMaxUpdateTime()).connectivityType(rollout.getConnectivityType()).endAt(rolloutRequest.getEndAt()).downgradeAllowed(rollout.getDowngradeAllowed()).downloadRetryCount(rollout.getDownloadRetryCount()).logCollectionRequired(rollout.isLogCollectionRequired()).logMaxAllFileSize(rollout.getLogMaxAllFileSize()).logMaxFailureVin(rollout.getLogMaxFailureVin()).logMaxEachFileSize(rollout.getLogMaxEachFileSize()).logMaxNumberOfFiles(rollout.getLogMaxNumberOfFiles()).maxDownloadCellularDurationTimer(rollout.getMaxDownloadCellularDurationTimer()).maxDownloadDurationTimer(rollout.getMaxDownloadDurationTimer()).maxDownloadWifiDurationTimer(rollout.getMaxDownloadWifiDurationTimer()).startAt(rollout.getStartAt()).logMaxSuccessVin(rollout.getLogMaxSuccessVin()).requiredMedia(rollout.getRequiredMedia()).requiredStateOfCharge(rollout.getRequiredStateOfCharge()).userAcceptanceRequired(rollout.getUserAcceptanceRequired());
        if (rollout.getWeight().isPresent()) {
            rolloutUpdate.weight(rollout.getWeight().get());
        }
        return rolloutUpdate;
    }

    /**
     * Retrieves the key of the default distribution type for a specific tenant.
     *
     * @return the key of the default distribution type.
     * @throws IllegalStateException if no default distribution type is configured for the tenant.
     */
    private String getDefaultDistributionTypeKeyForTenant() {

        // Retrieve the default distribution type for the tenant

        return systemSecurityContext.runAsSystem(systemManagement.getTenantMetadata().getDefaultDsType()::getKey);

    }

    /**
     * Validates the basic attributes of a software module request.
     * Ensures that all required fields are present and have valid values.
     *
     * @param module the {@link MgmtSoftwareModuleRequest} to validate
     * @throws ValidationException if any field is null or contains invalid values
     */
    void validateSoftwareModule(MgmtSoftwareModuleRequest module) {
        if (module.getModuleId() == null || module.getModuleId() <= 0) {
            throw new ValidationException("Module ID cannot be null and must be a positive integer.");
        }
        if (module.getSoftwareVersionTargetId() == null || module.getSoftwareVersionTargetId() <= 0) {
            throw new ValidationException("Software Version Target ID cannot be null and must be a positive integer.");
        }
    }

    /**
     * Deletes the group identified by `groupId` from the specified rollout.
     * This method first validates the deletion by ensuring the group belongs to the specified rollout
     * and that the group can be deleted based on its current status.
     * If the validation passes, the group is deleted and the total target count for the rollout is updated.
     *
     * @param tenantId  The ID of the tenant (not directly used in the method, but part of the URL).
     * @param rolloutId The ID of the rollout from which the group will be deleted.
     * @param groupId   The ID of the group to be deleted.
     * @return A ResponseEntity with HTTP status 200 (OK) if the group is successfully deleted.
     * @throws EntityNotFoundException If the group does not exist, does not belong to the specified rollout,
     *                                 or cannot be deleted due to its current state.
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteRolloutGroup(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField Long rolloutId, @PathVariable("groupId") Long groupId) {
        log.debug("Received delete rollout group request");
        Rollout rollout = RolloutHelper.validateRolloutAndStatus(rolloutId, rolloutManagement, RolloutStatus.DRAFT);
        RolloutGroup rolloutGroup = RolloutHelper.validateRolloutGroupAndStatus(rolloutId, groupId, rolloutGroupManagement, RolloutGroupStatus.DRAFT);
        rolloutManagement.deleteRolloutGroupTargets(rollout, rolloutGroup);
        rolloutManagement.updateTotalTargetsForRollout(rollout);

        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the action status timestamps for a specific rollout and controller within a tenant.
     * <p>
     * This endpoint allows clients to fetch the status history of a device action, identified by its rollout ID and controller ID,
     * for a given tenant. The response contains a list of status timestamp objects, each representing a status change event for the action.
     * </p>
     *
     * @param tenantId     the unique identifier of the tenant; must not be {@code null}
     * @param controllerId the unique identifier of the controller (device); must not be {@code null} or empty
     * @param rolloutId    the unique identifier of the rollout; must not be {@code null}
     * @return {@link ResponseEntity} containing a list of {@link DeviceActionStatusTimestampResponse} objects representing the action status history
     * @throws EntityNotFoundException if the specified action or controller does not exist for the tenant
     * @throws ValidationException     if the action does not belong to the specified controller or tenant
     */
    @TenantAware
    @TraceableMethod
    @Override
    public ResponseEntity<List<DeviceActionStatusTimestampResponse>> fetchActionStatuses(@PathVariable("tenantId") Long tenantId, @PathVariable("rolloutId") @TraceableField final Long rolloutId, @PathVariable("controllerId") @TraceableField final String controllerId) {
        log.debug("Received fetchActionStatuses of a rollout request");
        List<DeviceActionStatusTimestampResponse> actionStatus = rolloutManagement.fetchActionStatuses(rolloutId, controllerId);
        return ResponseEntity.ok(actionStatus);
    }

    /**
     * Handles the GET request for retrieving all targets for a specific rollout.
     *
     * @param rolloutId the ID of the rollout to retrieve targets for
     * @param tenantId  the ID of the tenant
     * @return a list of all targets for the specified rollout with status OK.
     */
    @Override
    @TraceableMethod
    @TenantAware
    public ResponseEntity<List<MgmtRolloutTargetActionsResponse>> getTargetsActionsByRolloutId(@PathVariable("tenantId") final Long tenantId, @PathVariable("rolloutId") final Long rolloutId) {


        // Validate that the rollout exists
        rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException("Rollout with id " + rolloutId + " does not exist"));

        // Find targets & actions by rollout ID
        final Map<Target, Action> targetsActionsMap = rolloutManagement.fetchRolloutTargetActions(rolloutId);
        // If no targets are found, return an empty list
        if (targetsActionsMap.isEmpty()) {
            log.debug("No targets found for rollout ID {}", rolloutId);
            return ResponseEntity.ok(List.of());
        }
        // Convert targets and actions to MgmtTarget DTOs
        log.info("Received {} targetsActions for the rollout ID {}", targetsActionsMap.size(), rolloutId);

        // Map to response DTOs
        List<MgmtRolloutTargetActionsResponse> responses = targetsActionsMap.entrySet().stream().map(entry -> MgmtRolloutMapper.mapToTargetActionResponse(entry.getKey(), entry.getValue())).toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Retries a full rollout for the specified tenant and rollout ID.
     *
     * @param tenantID                the ID of the tenant for which the rollout is being retried
     * @param rolloutId               the ID of the rollout to retry
     * @param retryFullRolloutRequest the request body containing retry parameters
     * @return ResponseEntity with HTTP status indicating the result of the operation
     */

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> retryFullRollout(@PathVariable("tenantId") Long tenantID,
                                                 @PathVariable("rolloutId") Long rolloutId,
                                                 @RequestBody MgmtRetryFullRolloutRequestBody retryFullRolloutRequest) {

        rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException("Rollout with id " + rolloutId + " does not exist"));

        if (!tenantConfigHelper.isFullRolloutEnabled()) {
            throw new FeatureNotAllowedException(
                    "Retry mode '%s' is not enabled for this tenant");
        }
        rolloutManagement.retryFullRollout(rolloutId, retryFullRolloutRequest);

        return ResponseEntity.ok().build();
    }

    /**
     * Handles the retry operation for multiple devices in a rollout.
     * <p>
     * This method validates the retry mode based on tenant configuration and triggers the retry operation
     * for the specified devices in the rollout.
     * </p>
     *
     * @param tenantId                    the ID of the tenant
     * @param rolloutId                   the ID of the rollout
     * @param retryMultipleDevicesRequest the request object containing retry details
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) if the operation is successful
     * @throws IllegalArgumentException if the retry mode is not enabled for the tenant
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> retryMultipleDevices(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("rolloutId") Long rolloutId,
            @RequestBody RetryMultipleDevicesRequest retryMultipleDevicesRequest) {
        log.debug("Received retry multiple devices request for rollout {}", rolloutId);

        // Validate that the rollout exists
        rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException("Rollout with id " + rolloutId + " does not exist"));

        // Validate if the retry mode is allowed for the tenant
        boolean isRetryAllowed = switch (retryMultipleDevicesRequest.getRetryMode()) {
            case ALL_FAILED_VEHICLES -> tenantConfigHelper.isRetryAllFailedVehiclesEnabled();
            case ALL_CANCELED_VEHICLES -> tenantConfigHelper.isRetryAllCanceledVehiclesEnabled();
            case ALL_SUCCEEDED_VEHICLES -> tenantConfigHelper.isRetryAllSucceededVehiclesEnabled();
            case ALL_NOT_EXECUTED_VEHICLES -> tenantConfigHelper.isRetryAllNotExecutedVehiclesEnabled();
            default -> false;
        };

        if (!isRetryAllowed) {
            throw new FeatureNotAllowedException(
                    String.format("Retry mode '%s' is not enabled for this tenant", retryMultipleDevicesRequest.getRetryMode()));
        }

        // Trigger the retry operation
        rolloutManagement.retryMultipleDevices(tenantId, rolloutId, retryMultipleDevicesRequest);
        log.debug("Retry operation completed successfully for rollout {}", rolloutId);

        return ResponseEntity.ok().build();
    }


    /**
     * Clones an existing rollout for the specified tenant and rollout ID.
     *
     * @param tenantID                the ID of the tenant for which the rollout is being cloned
     * @param rolloutID               the ID of the rollout to clone
     * @param cloneRolloutRequestBody the request body containing clone parameters
     * @return ResponseEntity containing the cloned rollout details
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtRolloutResponseBody> cloneRollout(@PathVariable("tenantId") Long tenantID,
                                                                @PathVariable("rolloutId") Long rolloutID,
                                                                @RequestBody MgmtCloneRolloutRequestBody cloneRolloutRequestBody) {

        if (!tenantConfigHelper.isCloneRolloutEnabled()) {
            throw new FeatureNotAllowedException(String.format("Clone Rollout '%s' not enabled for this tenant", tenantID));
        }
        rolloutManagement.get(rolloutID).orElseThrow(() -> new EntityNotFoundException("Rollout with id " + rolloutID + " does not exist"));
        Rollout clonedRollout = rolloutManagement.cloneRollout(rolloutID, cloneRolloutRequestBody);
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtRolloutMapper.mapToCreateRolloutResponse(tenantID, clonedRollout));
    }

    /**
     * Handles the retry operation for an individual device in a rollout.
     *
     * @param tenantId     the ID of the tenant
     * @param rolloutId    the ID of the rollout
     * @param controllerId the ID of the controller/device
     * @param retryRequest the request object containing retry details
     * @return a {@link ResponseEntity} with HTTP status 200 (OK) if the operation is successful
     */
    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> retryIndividualDevice(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("rolloutId") Long rolloutId,
            @PathVariable("controllerId") String controllerId,
            @RequestBody MgmtRetryIndividualDeviceRequestBody retryRequest) {
        log.debug("Received retry individual device request for rollout {}", rolloutId);

        // Validate that the rollout exists
        rolloutManagement.get(rolloutId).orElseThrow(() -> new EntityNotFoundException("Rollout with id " + rolloutId + " does not exist"));

        // Validate if the retry mode is allowed for the tenant
        boolean isRetryAllowed = switch (retryRequest.getRetryMode()) {
            case INDIVIDUAL_FAILED_VEHICLES -> tenantConfigHelper.isRetryIndividualFailedVehiclesEnabled();
            case INDIVIDUAL_CANCELED_VEHICLES -> tenantConfigHelper.isRetryIndividualCanceledVehiclesEnabled();
            case INDIVIDUAL_SUCCEEDED_VEHICLES -> tenantConfigHelper.isRetryIndividualSucceededVehiclesEnabled();
            case INDIVIDUAL_NOT_EXECUTED_VEHICLES -> tenantConfigHelper.isRetryIndividualNotExecutedVehiclesEnabled();
            default -> false;
        };

        if (!isRetryAllowed) {
            throw new FeatureNotAllowedException(
                    String.format("Retry mode '%s' is not enabled for this tenant", retryRequest.getRetryMode()));
        }

        // Trigger the retry operation
        rolloutManagement.retryIndividualDevice(rolloutId, controllerId, retryRequest);
        log.debug("Retry operation completed successfully for rollout {}", rolloutId);

        return ResponseEntity.ok().build();
    }

}