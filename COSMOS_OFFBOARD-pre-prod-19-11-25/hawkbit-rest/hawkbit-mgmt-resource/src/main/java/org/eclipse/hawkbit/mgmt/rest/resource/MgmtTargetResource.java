package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.dto.VehicleInventoryDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.models.mgmt.MgmtId;
import org.cosmos.models.mgmt.MgmtMetadata;
import org.cosmos.models.mgmt.MgmtMetadataBodyPut;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.action.dto.MgmtAction;
import org.cosmos.models.mgmt.action.dto.MgmtActionRequestBodyPut;
import org.cosmos.models.mgmt.action.dto.MgmtActionStatus;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSet;
import org.cosmos.models.mgmt.distributionset.dto.MgmtTargetAssignmentResponseBody;
import org.cosmos.models.mgmt.polling.dto.MgmtPollingFeedback;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutHistoryResponse;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.target.constants.MgmtTargetAttributes;
import org.cosmos.models.mgmt.target.dto.MgmtDistributionSetAssignments;
import org.cosmos.models.mgmt.target.dto.MgmtTarget;
import org.cosmos.models.mgmt.target.dto.MgmtTargetAutoConfirm;
import org.cosmos.models.mgmt.target.dto.MgmtTargetAutoConfirmUpdate;
import org.cosmos.models.mgmt.target.dto.MgmtTargetRequestBody;
import org.cosmos.models.mgmt.target.dto.MgmtTargetTenantRequest;
import org.cosmos.models.mgmt.target.dto.MgmtTargetUpdateRequestBody;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtTargetRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDeploymentRequestMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtDistributionSetMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtPollingFeedbackMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtRolloutHistoryMapper;
import org.eclipse.hawkbit.mgmt.rest.resource.mapper.MgmtTargetMapper;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.DistributionSetModuleRepository;
import org.eclipse.hawkbit.repository.jpa.JpaRolloutManagement;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaTenantMetaData;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.PollingFeedback;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.rest.aspect.TenantAware;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
import jakarta.validation.constraints.NotNull;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST Resource handling target CRUD operations.
 */
@RestController
@Tag(name = "Targets")

public class MgmtTargetResource implements MgmtTargetRestApi {
    private static final String ACTION_TARGET_MISSING_ASSIGN_WARN = "given action ({}) is not assigned to given target ({}).";

    private static final Logger LOG = LoggerFactory.getLogger(MgmtTargetResource.class);
    private static final String MISSING_MESSAGE = "Target is missing in the system";

    private final TargetManagement targetManagement;

    private final ConfirmationManagement confirmationManagement;

    private final DeploymentManagement deploymentManagement;

    private final EntityFactory entityFactory;

    private final TenantConfigHelper tenantConfigHelper;

    private final TargetRepository targetRepository;

    private final TenantMetaDataRepository tenantMetaDataRepository;

    private final RolloutManagement rolloutManagement;


    private final DistributionSetModuleRepository distributionSetModuleRepository;

    private final ControllerManagement controllerManagement;


    MgmtTargetResource(final TargetManagement targetManagement, final DeploymentManagement deploymentManagement,
                       final ConfirmationManagement confirmationManagement, final EntityFactory entityFactory,
                       final SystemSecurityContext systemSecurityContext,
                       final TenantConfigurationManagement tenantConfigurationManagement, TargetRepository targetRepository, TenantMetaDataRepository tenantMetaDataRepository, RolloutManagement rolloutManagement,
                       DistributionSetModuleRepository distributionSetModuleRepository,
                       ControllerManagement controllerManagement) {
        this.targetManagement = targetManagement;
        this.deploymentManagement = deploymentManagement;
        this.confirmationManagement = confirmationManagement;
        this.entityFactory = entityFactory;
        this.targetRepository = targetRepository;
        this.tenantMetaDataRepository = tenantMetaDataRepository;
        this.rolloutManagement = rolloutManagement;
        this.tenantConfigHelper = TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement);
        this.distributionSetModuleRepository = distributionSetModuleRepository;
        this.controllerManagement = controllerManagement;
    }
    /**
     * API endpoint: GET /management/v1/controllers/{currentTargetId}/inventory
     * Returns vehicle inventory details for the given controllerId.
     */

    public ResponseEntity<List<VehicleInventoryDTO>> getVehicleInventoryDetails(@PathVariable("currentTargetId") String currentTargetId) {

        List<VehicleInventoryDTO> inventory = controllerManagement.getVehicleInventoryDetails(currentTargetId);
        return ResponseEntity.ok(inventory);
    }


    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtTarget> getTarget(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") long tenantId) {
        LOG.debug("Received get Target request");
        final Target findTarget = findTargetWithExceptionIfNotFound(controllerId);
        // to single response include poll status
        final MgmtTarget response = MgmtTargetMapper.toResponse(findTarget, tenantConfigHelper, tenantId);
        MgmtTargetMapper.addPollStatus(findTarget, response);
        MgmtTargetMapper.addTargetLinks(response, tenantId);
        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    public ResponseEntity<PagedList<MgmtTarget>> getTargets(
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
            @PathVariable("tenantId") Long tenantId) {

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeTargetSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Slice<Target> findTargetsAll;
        final long countTargetsAll;
        if (rsqlParam != null) {
            findTargetsAll = targetManagement.findByRsql(pageable, rsqlParam);
            countTargetsAll = targetManagement.countByRsql(rsqlParam);
        } else {
            findTargetsAll = targetManagement.findAll(pageable);
            countTargetsAll = targetManagement.count();
        }

        final List<MgmtTarget> rest = MgmtTargetMapper.toResponse(findTargetsAll.getContent(), tenantConfigHelper, tenantId);
        return ResponseEntity.ok(new PagedList<>(rest, countTargetsAll));
    }

    // Deprecated since we are removing sp_polling and its references
    @Override
    @Deprecated
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtPollingFeedback> getAllGenericFeedback(
            @TraceableField @PathVariable("controllerId") final String controllerId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
            @PathVariable("tenantId") final Long tenantId) {

        LOG.debug("Received get All Generic Feedback request");
        final Sort sorting = PagingUtility.sanitizePollingFeedbackSortParam(sortParam);

        final Pageable pageable = PageRequest.of(pagingOffsetParam, pagingLimitParam, sorting);
        final Target findTarget = findTargetWithExceptionIfNotFound(controllerId);
        final Long findPollingId = findPollingsWithExceptionIfNotFound(findTarget);
        final List<PollingFeedback> listPollingFeedback = findPollingFeedbackByIdWithExceptionIfNotFound(findPollingId, pageable);

        return ResponseEntity.ok(MgmtPollingFeedbackMapper.toResponse(listPollingFeedback));
    }

    @Override
    @TenantAware
    public ResponseEntity<List<MgmtTarget>> createTargets(@RequestBody final List<MgmtTargetRequestBody> targets, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("creating {} targets", targets.size());
        final Collection<Target> createdTargets = this.targetManagement
                .create(MgmtTargetMapper.fromRequest(entityFactory, targets));
        LOG.debug("{} targets created, return status {}", targets.size(), HttpStatus.CREATED);
        return new ResponseEntity<>(MgmtTargetMapper.toResponse(createdTargets, tenantConfigHelper, tenantId),
                HttpStatus.CREATED);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtTarget> updateTarget(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                   @RequestBody final MgmtTargetUpdateRequestBody targetRest,
                                                   @PathVariable("tenantId") final Long tenantId) {

        LOG.debug("Received update Target request");
        // Retrieve the target from the management service
        Target target = targetManagement.getByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(MISSING_MESSAGE));


        //Check if the target is associated with any rollouts
        List<Rollout> rollouts = rolloutManagement.findAllRolloutByTargetId(target);
        if (!rollouts.isEmpty()) {
            LOG.info("Target is associated with rollouts, but only description will be updated.");
        }

        // Update only the description of the target
        Target updatedTarget = targetManagement.update(
                entityFactory.target().update(controllerId)
                        .description(targetRest.getDescription()));

        // Map the updated target to the response object
        final MgmtTarget response = MgmtTargetMapper.toResponse(updatedTarget, tenantConfigHelper, tenantId);
        MgmtTargetMapper.addPollStatus(updatedTarget, response);
        MgmtTargetMapper.addTargetLinks(response, tenantId);

        return ResponseEntity.ok(response);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteTarget(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {

        LOG.debug("Received delete target request");
        Target target = targetManagement.getByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(MISSING_MESSAGE));
        List<Rollout> rollouts = rolloutManagement.findAllRolloutByTargetId(target);
        if (!rollouts.isEmpty()) {
            String rolloutNames = rollouts.stream()
                    .map(Rollout::getName)
                    .collect(Collectors.joining(", "));

            throw new ValidationException(String.format(
                    "Unable to delete %s, since it is part of Rollouts [%s]",
                    target.getName(),
                    rolloutNames
            ));
        }
        this.targetManagement.deleteByControllerID(controllerId);
        LOG.debug("{} target deleted, return status {}", controllerId, HttpStatus.OK);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> unassignTargetType(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received unassign Target type request");
        this.targetManagement.unAssignType(controllerId);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> assignTargetType(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                 @RequestBody final MgmtId targetTypeId, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received assign Target Type request");
        this.targetManagement.assignType(controllerId, targetTypeId.getId());
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtTargetAttributes> getAttributes(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received get attributes request");
        final Map<String, String> controllerAttributes = targetManagement.getControllerAttributes(controllerId);
        controllerAttributes.putAll(targetManagement.getControllerSoftwareAttributes(controllerId));
        if (controllerAttributes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        final MgmtTargetAttributes result = new MgmtTargetAttributes();
        result.putAll(controllerAttributes);

        return ResponseEntity.ok(result);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtTargetAttributes> getSoftwareAttributes(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received get Software Attributes request");
        final Map<String, String> controllerAttributes = targetManagement.getControllerSoftwareAttributes(controllerId);
        if (controllerAttributes.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        final MgmtTargetAttributes result = new MgmtTargetAttributes();
        result.putAll(controllerAttributes);

        return ResponseEntity.ok(result);
    }


    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtAction>> getAllActions(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
                                                               @PathVariable("tenantId") final Long tenantId) {

        LOG.debug("Received get Action History request");
        findTargetWithExceptionIfNotFound(controllerId);

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionSortParam(sortParam);
        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);

        final Slice<Action> activeActions;
        final Long totalActionCount;
        if (rsqlParam != null) {
            activeActions = this.deploymentManagement.findActionsByTarget(rsqlParam, controllerId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(rsqlParam, controllerId);
        } else {
            activeActions = this.deploymentManagement.findActionsByTarget(controllerId, pageable);
            totalActionCount = this.deploymentManagement.countActionsByTarget(controllerId);
        }

        return ResponseEntity.ok(
                new PagedList<>(MgmtTargetMapper.toResponse(controllerId, activeActions.getContent(), tenantId, deploymentManagement), totalActionCount));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtAction> getAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                @PathVariable("actionId") final Long actionId,
                                                @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Action request");
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        if (!action.getTarget().getControllerId().equals(controllerId)) {
            LOG.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), controllerId);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(MgmtTargetMapper.toResponseWithLinks(controllerId, action, tenantId, deploymentManagement));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> cancelAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                             @PathVariable("actionId") final Long actionId,
                                             @RequestParam(value = "force", required = false, defaultValue = "false") final boolean force,
                                             @PathVariable("tenantId") Long tenantId) {
        LOG.debug("Received cancel Action request");
        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getControllerId().equals(controllerId)) {
            LOG.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, actionId, controllerId);
            return ResponseEntity.notFound().build();
        }

        if (force) {
            this.deploymentManagement.forceQuitAction(actionId);
        } else {
            this.deploymentManagement.cancelAction(actionId);
        }
        // both functions will throw an exception, when action is in wrong
        // state, which is mapped by MgmtResponseExceptionHandler.

        return ResponseEntity.noContent().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtActionStatus>> getActionStatusList(
            @TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("actionId") final Long actionId,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
            @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Action status list request");
        final Target target = findTargetWithExceptionIfNotFound(controllerId);

        final Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getTarget().getId().equals(target.getId())) {
            LOG.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), target.getId());
            return ResponseEntity.notFound().build();
        }

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeActionStatusSortParam(sortParam);

        final Page<ActionStatus> statusList = this.deploymentManagement.findActionStatusByAction(
                new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting), action.getId());

        PagedList<MgmtActionStatus> result = new PagedList<>(MgmtTargetMapper.toActionStatusRestResponse(statusList.getContent(), deploymentManagement),
                statusList.getTotalElements());

        if (action.isDeploymentLogAvailable()) {
            return ResponseEntity.ok(result
                    .add(linkTo(methodOn(MgmtActionRestApi.class).getActionsDeploymentLogs(action.getTarget().getControllerId(), action.getId(), tenantId))
                            .withRel(MgmtRestConstants.DEPLOYMENT_LOGS_LINK).expand()));
        }

        return ResponseEntity.ok(result);

    }

    @Override
    @TenantAware
    @TraceableMethod
    @Deprecated
    public ResponseEntity<MgmtDistributionSet> getAssignedDistributionSet(
            @TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Assigned DistributionSet request");
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getAssignedDistributionSet(controllerId)
                .map(ds -> {
                    final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(ds, tenantId);
                    MgmtDistributionSetMapper.addLinks(ds, response, tenantId);

                    return response;
                }).orElse(null);

        if (distributionSetRest == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(distributionSetRest);
    }

    @Override
    @TenantAware
    @TraceableMethod
    @Deprecated
    public ResponseEntity<MgmtTargetAssignmentResponseBody> postAssignedDistributionSet(
            @TraceableField @PathVariable("controllerId") final String controllerId,
            @RequestBody final MgmtDistributionSetAssignments dsAssignments,
            @RequestParam(value = "offline", required = false) final Boolean offline,
            @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received post Assigned DistributionSet request");

        if (Boolean.TRUE.equals(offline)) {
            final List<Entry<String, Long>> offlineAssignments = dsAssignments.stream()
                    .map(dsAssignment -> new SimpleEntry<String, Long>(controllerId, dsAssignment.getId()))
                    .collect(Collectors.toList());
            return ResponseEntity.ok(MgmtDistributionSetMapper
                    .toResponse(deploymentManagement.offlineAssignedDistributionSets(offlineAssignments), tenantId));
        }
        findTargetWithExceptionIfNotFound(controllerId);

        final List<DeploymentRequest> deploymentRequests = dsAssignments.stream().map(dsAssignment -> {
            final boolean isConfirmationRequired = dsAssignment.isConfirmationRequired() == null
                    ? tenantConfigHelper.isConfirmationFlowEnabled()
                    : dsAssignment.isConfirmationRequired();
            return MgmtDeploymentRequestMapper.createAssignmentRequestBuilder(dsAssignment, controllerId)
                    .setConfirmationRequired(isConfirmationRequired).build();
        }).toList();

        final List<DistributionSetAssignmentResult> assignmentResults = deploymentManagement
                .assignDistributionSets(deploymentRequests);
        return ResponseEntity.status(HttpStatus.CREATED).body(MgmtDistributionSetMapper.toResponse(assignmentResults, tenantId));
    }

    @Override
    @TenantAware
    @Deprecated
    @TraceableMethod
    public ResponseEntity<MgmtDistributionSet> getInstalledDistributionSet(
            @TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Installed DistributionSet request");
        final MgmtDistributionSet distributionSetRest = deploymentManagement.getInstalledDistributionSet(controllerId)
                .map(set -> {
                    final MgmtDistributionSet response = MgmtDistributionSetMapper.toResponse(set, tenantId);
                    MgmtDistributionSetMapper.addLinks(set, response, tenantId);

                    return response;
                }).orElse(null);

        if (distributionSetRest == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(distributionSetRest);
    }

    /**
     * Find {@link Target} for given targetId else throw not found error
     *
     * @param controllerId the id of target
     * @return {@link Target}
     */
    private Target findTargetWithExceptionIfNotFound(final String controllerId) {
        return targetManagement.getByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    /**
     * Find {@link Polling#getId()} for given {@link Target} else throw not found error
     *
     * @param target {@link Target}
     * @return Long the Id of {@link Polling}
     * Deprecated this as a part of removing sp_polling and its references
     * This method is getting called at getAllGenericFeedback API which is deprecated
     */
    @Deprecated
    private Long findPollingsWithExceptionIfNotFound(final Target target) {
//        return pollingManagement.getByTargetID(target)
//                .orElseThrow(() -> new EntityNotFoundException("Entity with ID " + target + " not found."));
        return null;
    }

    /**
     * Find the list of {@link PollingFeedback} for given {@link Polling#getId()}
     *
     * @param {@link Polling#getId()}
     * @param {@link Pageable}
     * @return PollingFeedback list
     */
    private List<PollingFeedback> findPollingFeedbackByIdWithExceptionIfNotFound(final Long id, @NotNull Pageable pageable) {
//        return pollingFeedbackManagement.findByPollingId(id, pageable);
        return null;
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtAction> updateAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                   @PathVariable("actionId") final Long actionId, @RequestBody final MgmtActionRequestBodyPut actionUpdate,
                                                   @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received update Action request");

        Action action = deploymentManagement.findAction(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        if (!action.getTarget().getControllerId().equals(controllerId)) {
            LOG.warn(ACTION_TARGET_MISSING_ASSIGN_WARN, action.getId(), controllerId);
            return ResponseEntity.notFound().build();
        }

        if (!actionUpdate.getUserAcceptanceRequired().equals(MgmtRolloutUserAcceptanceRequired.NO)) {
            throw new ValidationException("Resource supports only switch to FORCED.");
        }

        action = deploymentManagement.forceTargetAction(actionId);

        return ResponseEntity.ok(MgmtTargetMapper.toResponseWithLinks(controllerId, action, tenantId, deploymentManagement));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<MgmtMetadata>> getMetadata(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam,
                                                               @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SEARCH, required = false) final String rsqlParam,
                                                               @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Metadata request");

        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeDistributionSetMetadataSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<TargetMetadata> metaDataPage;

        if (rsqlParam != null) {
            metaDataPage = targetManagement.findMetaDataByControllerIdAndRsql(pageable, controllerId, rsqlParam);
        } else {
            metaDataPage = targetManagement.findMetaDataByControllerId(pageable, controllerId);
        }

        return ResponseEntity.ok(new PagedList<>(MgmtTargetMapper.toResponseTargetMetadata(metaDataPage.getContent()),
                metaDataPage.getTotalElements()));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtMetadata> getMetadataValue(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                         @PathVariable("key") final String key,
                                                         @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get getMetadataValue request");
        final TargetMetadata findOne = targetManagement.getMetaDataByControllerId(controllerId, key)
                .orElseThrow(() -> new EntityNotFoundException(TargetMetadata.class, controllerId, key));
        return ResponseEntity.ok(MgmtTargetMapper.toResponseTargetMetadata(findOne));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtMetadata> updateMetadata(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                       @PathVariable("key") final String key, @RequestBody final MgmtMetadataBodyPut metadata,
                                                       @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received update Metadata request");
        final TargetMetadata updated = targetManagement.updateMetadata(controllerId,
                entityFactory.generateTargetMetadata(key, metadata.getValue()));
        return ResponseEntity.ok(MgmtTargetMapper.toResponseTargetMetadata(updated));
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deleteMetadata(@TraceableField @PathVariable("controllerId") final String controllerId,
                                               @PathVariable("key") final String key,
                                               @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received delete Metadata request");
        targetManagement.deleteMetaData(controllerId, key);
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<List<MgmtMetadata>> createMetadata(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                             @RequestBody final List<MgmtMetadata> metadataRest,
                                                             @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received create Metadata request");
        final List<TargetMetadata> created = targetManagement.createMetaData(controllerId,
                MgmtTargetMapper.fromRequestTargetMetadata(metadataRest, entityFactory));
        return new ResponseEntity<>(MgmtTargetMapper.toResponseTargetMetadata(created), HttpStatus.CREATED);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<MgmtTargetAutoConfirm> getAutoConfirmStatus(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received get Auto Confirm Status request");
        final Target findTarget = findTargetWithExceptionIfNotFound(controllerId);
        final MgmtTargetAutoConfirm state = MgmtTargetMapper.getTargetAutoConfirmResponse(findTarget, tenantId);
        return ResponseEntity.ok(state);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> activateAutoConfirm(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                    @RequestBody(required = false) final MgmtTargetAutoConfirmUpdate update, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received active Auto confirm request");
        final String initiator = getNullIfEmpty(update, MgmtTargetAutoConfirmUpdate::getInitiator);
        final String remark = getNullIfEmpty(update, MgmtTargetAutoConfirmUpdate::getRemark);
        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private <T, R> R getNullIfEmpty(final T object, final Function<T, R> extractMethod) {
        return object == null ? null : extractMethod.apply(object);
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> deactivateAutoConfirm(@TraceableField @PathVariable("controllerId") final String controllerId, @PathVariable("tenantId") final Long tenantId) {
        LOG.debug("Received deactivate Auto Confirm request");
        confirmationManagement.deactivateAutoConfirmation(controllerId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Updates the tenant of a specified target, given its controller ID and tenant ID, following several validation checks.
     * This operation is authorized and tenant-aware, ensuring only valid tenants with necessary permissions can perform updates.
     *
     * @param targetId the unique identifier of the target (controller ID) for which the tenant is to be updated
     * @param tenantId the ID of the tenant making the update request
     * @param request  contains the new tenant details in a {@link MgmtTargetTenantRequest} object
     * @return {@link ResponseEntity<Void>} with HTTP status 200 (OK) upon successful tenant update
     * @throws EntityNotFoundException if the target or tenant cannot be found
     * @throws ValidationException     if the new tenant matches the current tenant or if the target is associated with an active rollout
     */

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<Void> updateTenant(@TraceableField @PathVariable("controllerId") String targetId,
                                             @PathVariable("tenantId") Long tenantId, @RequestBody MgmtTargetTenantRequest request) {
        LOG.debug("Received update tenant request");

        Target target = targetManagement.getByControllerID(targetId)
                .orElseThrow(() -> new EntityNotFoundException(MISSING_MESSAGE));

        String currentTenant = targetRepository.findTenantByControllerId(targetId)
                .orElseThrow(() -> new EntityNotFoundException("Tenant not found for controllerId " + targetId));

        // Check if the requested tenant exists
        JpaTenantMetaData tenantMetaData = tenantMetaDataRepository.findByTenantIgnoreCase(request.getTenant());
        if (tenantMetaData == null) {
            throw new ValidationException("The requested tenant name '" + request.getTenant() + "' does not exist or is not valid.");
        }

        // Ensure tenant is Different from Current Tenant
        if (request.getTenant().equalsIgnoreCase(currentTenant)) {
            throw new ValidationException("New tenant must be different from the current tenant");
        }

        //Check if the target is associated with any rollouts
        Set<RolloutStatus> allowedStatuses = JpaRolloutManagement.NONACTIVE_ROLLOUTS;
        List<Rollout> rollouts = rolloutManagement.findAllRolloutByTargetId(target);

        boolean rolloutStatus = rollouts.stream().allMatch(rollout ->
                allowedStatuses.contains(rollout.getStatus())
        );
        if (!rolloutStatus) {
            throw new ValidationException("Cannot update tenant for target with active rollout");
        }

        targetManagement.updateTenant(targetId, currentTenant, request.getTenant());
        return ResponseEntity.ok().build();
    }

    @Override
    @TenantAware
    @TraceableMethod
    public ResponseEntity<List<MgmtRolloutHistoryResponse>> getRolloutHistoryByVin(
            @PathVariable("tenantId") Long tenantId,
            @PathVariable("vin") String vin,
            @RequestParam(value = "offset", required = false, defaultValue = "0") int offset,
            @RequestParam(value = "limit", required = false, defaultValue = "100") int limit) {
        Target target = targetManagement.getByControllerID(vin)
                .orElseThrow(() -> new EntityNotFoundException("VIN not found: " + vin));
        List<Rollout> rollouts = rolloutManagement.findAllRolloutByTargetId(target);
        if (rollouts == null || rollouts.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<MgmtRolloutHistoryResponse> response = MgmtRolloutHistoryMapper.mapToRolloutHistoryResponse(rollouts, distributionSetModuleRepository);
        return ResponseEntity.ok(response);
    }

}

