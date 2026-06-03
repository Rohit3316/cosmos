
package org.eclipse.hawkbit.repository.jpa;

import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.dto.VehicleInventoryDTO;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jdk.jfr.Description;
import org.cosmos.models.ddi.DdiFeedbackRequestBody;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.ddi.DdiUserAcceptanceMessage;
import org.cosmos.models.ddi.DeviceInventoryDetails;
import org.cosmos.models.ddi.ExecutionType;
import org.cosmos.models.ddi.SoftwareModuleInfo;
import org.cosmos.models.kafka.GeneralErrorMessage;
import org.cosmos.models.kafka.GeneralIdleMessage;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.MaintenanceScheduleHelper;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.event.remote.CancelTargetAssignmentEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetAttributesRequestedEvent;
import org.eclipse.hawkbit.repository.event.remote.TargetPollEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityAlreadyExistsException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidTargetAttributeException;
import org.eclipse.hawkbit.repository.jpa.builder.JpaActionStatusCreate;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatusUserAcceptance;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaGeneralFeedback;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetInventory;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetSoftware;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.model.TargetSoftwareCompositeKey;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Polling;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareOfTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInventory;
import org.eclipse.hawkbit.repository.model.TargetSoftware;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.validation.annotation.Validated;

import static org.eclipse.hawkbit.repository.model.Target.CONTROLLER_ATTRIBUTE_KEY_SIZE;
import static org.eclipse.hawkbit.repository.model.Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE;

/**
 * JPA based {@link ControllerManagement} implementation.
 */
@Transactional(readOnly = true)
@Validated
public class JpaControllerManagement extends JpaActionManagement implements ControllerManagement {
    @Autowired
    private EcuModelRepository ecuModelRepository;


    private static final Logger LOG = LoggerFactory.getLogger(JpaControllerManagement.class);

    private final BlockingDeque<TargetPoll> queue;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private TargetRepository targetRepository;
    @Autowired
    private DdiSecurityProperties ddiSecurityProperties;

    @Autowired
    private SoftwareModuleRepository softwareModuleRepository;
    @Autowired
    private GeneralFeedbackRepository generalFeedbackRepository;
    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;
    @Autowired
    private SystemSecurityContext systemSecurityContext;
    @Autowired
    private EntityFactory entityFactory;
    @Autowired
    private EventPublisherHolder eventPublisherHolder;
    @Autowired
    private AfterTransactionCommitExecutor afterCommit;
    @Autowired
    private SoftwareModuleMetadataRepository softwareModuleMetadataRepository;
    @Autowired
    private PlatformTransactionManager txManager;
    @Autowired
    private TenantAware tenantAware;
    @Autowired
    private ConfirmationManagement confirmationManagement;
    @Autowired
    private TargetInventoryRepository targetInventoryRepository;

    @Autowired
    private TargetSoftwareRepository targetSoftwareRepository;

    private final KafkaMessageService kafkaMessageService;

    public JpaControllerManagement(final ScheduledExecutorService executorService, final ActionRepository actionRepository, final ActionStatusRepository actionStatusRepository, final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository, final QuotaManagement quotaManagement, final RepositoryProperties repositoryProperties, final KafkaMessageService kafkaMessageService) {
        super(actionRepository, actionStatusRepository, actionStatusUserAcceptanceRepository, quotaManagement, repositoryProperties);
        this.kafkaMessageService = kafkaMessageService;
        if (!repositoryProperties.isEagerPollPersistence()) {
            executorService.scheduleWithFixedDelay(this::flushUpdateQueue, repositoryProperties.getPollPersistenceFlushTime(), repositoryProperties.getPollPersistenceFlushTime(), TimeUnit.MILLISECONDS);

            queue = new LinkedBlockingDeque<>(repositoryProperties.getPollPersistenceQueueSize());
        } else {
            queue = null;
        }
    }

    private static boolean isStatusUnknown(final TargetUpdateStatus statusToUpdate) {
        return TargetUpdateStatus.UNKNOWN == statusToUpdate;
    }

    private static boolean isAttributeEntryValid(final Map.Entry<String, String> e) {
        return isAttributeKeyValid(e.getKey()) && isAttributeValueValid(e.getValue());
    }

    private static boolean isAttributeKeyValid(final String key) {
        return key != null && key.length() <= CONTROLLER_ATTRIBUTE_KEY_SIZE;
    }

    private static boolean isAttributeValueValid(final String value) {
        return value == null || value.length() <= CONTROLLER_ATTRIBUTE_VALUE_SIZE;
    }

    private static void copy(final Map<String, String> src, final Map<String, String> trg) {
        if (src == null || src.isEmpty()) {
            return;
        }
        src.forEach((key, value) -> {
            if (value != null) {
                trg.put(key, value);
            } else {
                trg.remove(key);
            }
        });
    }

    @Override
    public String getPollingTime() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.POLLING_TIME_INTERVAL, String.class).getValue());
    }

    /**
     * Returns the configured minimum polling interval.
     *
     * @return current {@link TenantConfigurationKey#MIN_POLLING_TIME_INTERVAL}.
     */
    @Override
    public String getMinPollingTime() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.MIN_POLLING_TIME_INTERVAL, String.class).getValue());
    }

    /**
     * Returns the count to be used for reducing polling interval while calling
     * {@link ControllerManagement#getPollingTimeForAction(long)}.
     *
     * @return configured value of
     * {@link TenantConfigurationKey#MAINTENANCE_WINDOW_POLL_COUNT}.
     */
    @Override
    public int getMaintenanceWindowPollCount() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(TenantConfigurationKey.MAINTENANCE_WINDOW_POLL_COUNT, Integer.class).getValue());
    }

    @Override
    public String getPollingTimeForAction(final long actionId) {

        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);

        if (!action.hasMaintenanceSchedule() || action.isMaintenanceScheduleLapsed()) {
            return getPollingTime();
        }

        return new EventTimer(getPollingTime(), getMinPollingTime(), ChronoUnit.SECONDS).timeToNextEvent(getMaintenanceWindowPollCount(), action.getMaintenanceWindowStartTime().orElse(null));
    }

    @Override
    public Optional<Action> getActionForDownloadByTargetAndSoftwareModule(final String controllerId, final long moduleId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        throwExceptionIfSoftwareModuleDoesNotExist(moduleId);

        final List<Action> action = actionRepository.findActionByTargetAndSoftwareModule(controllerId, moduleId, true);

        if (action.isEmpty() || action.get(0).isCancelingOrCanceled()) {
            return Optional.empty();
        }

        return Optional.ofNullable(action.get(0));
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private void throwExceptionIfTargetDoesNotExist(final Long targetId) {
        if (!targetRepository.existsById(targetId)) {
            throw new EntityNotFoundException(Target.class, targetId);
        }
    }

    private void throwExceptionIfSoftwareModuleDoesNotExist(final Long moduleId) {
        if (!softwareModuleRepository.existsById(moduleId)) {
            throw new EntityNotFoundException(SoftwareModule.class, moduleId);
        }
    }

    @Override
    public Optional<Action> findActiveActionWithHighestWeight(final String controllerId) {
        return findActiveActionsWithHighestWeight(controllerId, 1).stream().findFirst();
    }

    @Override
    public List<Action> findActiveActionsWithHighestWeight(final String controllerId, final int maxActionCount) {
        return findActiveActionsWithHighestWeightConsideringDefault(controllerId, maxActionCount);
    }

    @Override
    public int getWeightConsideringDefault(final Action action) {
        return super.getWeightConsideringDefault(action);
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
        return actionRepository.getActionById(actionId, true);
    }

    @Override
    public List<Action> getActiveActionsByExternalRef(@NotNull final List<String> externalRefs) {
        return actionRepository.findByExternalRefInAndActive(externalRefs, true);
    }

    @Override
    public void deleteExistingTarget(@NotEmpty final String controllerId) {
        final Target target = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
        targetRepository.deleteById(target.getId());
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = ConcurrencyFailureException.class, exclude = EntityAlreadyExistsException.class, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target findOrRegisterTargetIfItDoesNotExist(final String controllerId, final String name, final String serialNumber, Long vehicleModelId) {
        final Specification<JpaTarget> spec = (targetRoot, query, cb) -> cb.equal(targetRoot.get(JpaTarget_.controllerId), controllerId);
        return targetRepository.findOne(spec).map(target -> updateTarget(target)).orElseGet(() -> verifyCreateTargetOnRun(controllerId, name, serialNumber, vehicleModelId));
    }

    private Target verifyCreateTargetOnRun(final String controllerId, final String name, final String serialNumber, Long vehicleModelId) {
        if (ddiSecurityProperties.isCreateTargetOnRun()) {
            return createTarget(controllerId, name, serialNumber, vehicleModelId);
        } else {
            throwExceptionIfTargetDoesNotExist(controllerId);
            return null;
        }
    }

    private Target createTarget(final String controllerId, final String name, final String serialNumber, Long vehicleModelId) {
        final Target result = targetRepository.save((JpaTarget) entityFactory.target().create().controllerId(controllerId).description("Plug and Play target: " + controllerId).name(name).serialNumber(serialNumber).status(TargetUpdateStatus.REGISTERED).lastTargetQuery(Instant.now().getEpochSecond()).vehicleModelId(vehicleModelId).vin(controllerId.split("_")[0]).build());

        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(new TargetPollEvent(result, eventPublisherHolder.getApplicationId())));

        return result;
    }

    /**
     * Flush the update queue by means to persisting
     * {@link Target#getLastTargetQuery()}.
     */
    private void flushUpdateQueue() {
        LOG.debug("Run flushUpdateQueue.");

        final int size = queue.size();
        if (size <= 0) {
            return;
        }

        LOG.debug("{} events in flushUpdateQueue.", size);

        final Set<TargetPoll> events = Sets.newHashSetWithExpectedSize(queue.size());
        final int drained = queue.drainTo(events);

        if (drained <= 0) {
            return;
        }

        try {
            events.stream().collect(Collectors.groupingBy(TargetPoll::getTenant)).forEach((tenant, polls) -> {
                final TransactionCallback<Void> createTransaction = status -> updateLastTargetQueries(tenant, polls);
                tenantAware.runAsTenant(tenant, () -> DeploymentHelper.runInNewTransaction(txManager, "flushUpdateQueue", createTransaction));
            });
        } catch (final RuntimeException ex) {
            LOG.error("Failed to persist UpdateQueue content.", ex);
            return;
        }

        LOG.debug("{} events persisted.", drained);
    }

    private Void updateLastTargetQueries(final String tenant, final List<TargetPoll> polls) {
        LOG.debug("Persist {} targetqueries.", polls.size());

        final List<List<String>> pollChunks = Lists.partition(polls.stream().map(TargetPoll::getControllerId).toList(), Constants.MAX_ENTRIES_IN_STATEMENT);

        pollChunks.forEach(chunk -> {
            setLastTargetQuery(tenant, Instant.now().getEpochSecond(), chunk);
            chunk.forEach(controllerId -> afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(new TargetPollEvent(controllerId, tenant, eventPublisherHolder.getApplicationId()))));
        });

        return null;
    }

    /**
     * Sets {@link Target#getLastTargetQuery()} by native SQL in order to avoid
     * raising opt lock revision as this update is not mission critical and in
     * fact only written by {@link ControllerManagement}, i.e. the target
     * itself.
     */
    private void setLastTargetQuery(final String tenant, final long currentTimeSeconds, final List<String> chunk) {
        final int updated = targetRepository.updateLastTargetQuery(currentTimeSeconds, chunk, tenant);

        if (updated < chunk.size()) {
            LOG.error("Targets polls could not be applied completely ({} instead of {}).", updated, chunk.size());
        }
    }

    /**
     * Stores target directly to DB in case either {@link Target}
     * or {@link Target#getUpdateStatus()} or {@link Target#getName()} changes
     * or the buffer queue is full.
     */
    private Target updateTarget(final JpaTarget toUpdate) {
        if (isStoreEager(toUpdate) || !queue.offer(new TargetPoll(toUpdate))) {
            if (isStatusUnknown(toUpdate.getUpdateStatus())) {
                toUpdate.setUpdateStatus(TargetUpdateStatus.REGISTERED);
            }
            toUpdate.setLastTargetQuery(Instant.now().getEpochSecond());
            afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(new TargetPollEvent(toUpdate, eventPublisherHolder.getApplicationId())));
            return targetRepository.save(toUpdate);
        }
        return toUpdate;
    }

    private boolean isStoreEager(final JpaTarget toUpdate) {
        return repositoryProperties.isEagerPollPersistence() || isStatusUnknown(toUpdate.getUpdateStatus());
    }


    /**
     * Retrieves the vehicle inventory details for a given controller ID.
     * <p>
     * This method fetches the target by controller ID, retrieves its inventories,
     * and maps each ECU and its SCOMO details to a list of {@link VehicleInventoryDTO}.
     * </p>
     *
     * @param controllerId the unique identifier of the controller
     * @return a list of {@link VehicleInventoryDTO} containing inventory details for each ECU and SCOMO
     * @throws EntityNotFoundException if the target with the given controller ID does not exist
     */
    @Override
    public List<VehicleInventoryDTO> getVehicleInventoryDetails(String controllerId) {
        // Log the controller ID being processed
        LOG.info("In service layer Details: {}", controllerId);

        // Fetch the target entity by controller ID, throw exception if not found
        final JpaTarget target = targetRepository.findByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
        LOG.info("Target Details: {}", target);

        // Retrieve all inventories associated with the target
        List<TargetInventory> inventories = targetInventoryRepository.findByTarget(target);
        LOG.info("Inventory Details: {}", inventories);
        LOG.info("Inventories: {}", inventories);

        // Process each inventory and map ECU and SCOMO details to DTOs
        return inventories.stream()
                // Filter out inventories with null inventory or ECU list
                .filter(inventory -> inventory.getInventory() != null && inventory.getInventory().getEcuList() != null)
                // For each inventory, process its ECU list
                .flatMap(inventory -> inventory.getInventory().getEcuList().stream()
                        // For each ECU, process its SCOMO list and model details
                        .flatMap(ecu -> {
                            final String nodeId = ecu.getNodeAddress();
                            final String serialNumber = ecu.getSerialNumber();

                            // Fetch ECU model details by node ID
                            final List<EcuModel> ecuModels = ecuModelRepository.findByEcuNodeId(Set.of(nodeId));
                            final String modelType;
                            final String modelName;
                            if (!ecuModels.isEmpty()) {
                                // Use the first model if available
                                EcuModel model = ecuModels.get(0);
                                modelType = model.getEcuModelType();
                                modelName = model.getEcuModelName();
                            } else {
                                // Set to null if no model found
                                modelType = null;
                                modelName = null;
                            }

                            // If SCOMO list is present and not empty, map each SCOMO to a DTO
                            if (ecu.getScomos() != null && !ecu.getScomos().isEmpty()) {
                                return ecu.getScomos().stream()
                                        .map(scomo -> new VehicleInventoryDTO(
                                                serialNumber,
                                                nodeId,
                                                modelType,
                                                modelName,
                                                scomo.getScomoId(),
                                                scomo.getSwVersion()
                                        ));
                            } else {
                                // If no SCOMO, create a DTO with null SCOMO fields
                                return Stream.of(new VehicleInventoryDTO(
                                        serialNumber,
                                        nodeId,
                                        modelType,
                                        modelName,
                                        null,
                                        null
                                ));
                            }
                        })
                )
                // Collect all DTOs into a list
                .toList();
    }



    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addCancelActionStatus(final ActionStatusCreate c) {
        final JpaActionStatusCreate create = (JpaActionStatusCreate) c;

        final JpaAction action = getActionAndThrowExceptionIfNotFound(create.getActionId());

        if (!action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("The action is not in canceling state.");
        }

        final JpaActionStatus actionStatus = create.build();

        switch (actionStatus.getStatus()) {
            case CANCELED, FINISHED_SUCCESS, FINISHED_FAILURE, FINISHED_NOT_EXECUTED:
                handleFinishedCancelation(actionStatus, action);
                break;
            case ERROR_RESPONSE_CODE, CANCELED_REJECT:
                // Cancellation rejected. Back to running.
                action.setStatus(DeviceActionStatus.RUNNING);
                break;
            default:
                // information status entry - check for a potential DOS attack
                assertActionStatusQuota(action);
                assertActionStatusMessageQuota(actionStatus);
                break;
        }

        actionStatus.setAction(actionRepository.save(action));
        actionStatusRepository.save(actionStatus);

        return action;
    }

    private void handleFinishedCancelation(final JpaActionStatus actionStatus, final JpaAction action) {
        // in case of successful cancellation we also report the success at
        // the canceled action itself.
        actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Cancellation completion is finished sucessfully.");
        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action addUpdateActionStatus(final ActionStatusCreate statusCreate, DdiStatus ddiStatus) {
        return addActionStatus((JpaActionStatusCreate) statusCreate, ddiStatus);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void addUserAcceptanceStatus(DdiStatus status, Action action) {
        DdiUserAcceptanceMessage userAcceptanceMessage = status.getUserAcceptanceMessage();

        LOG.debug("Received user acceptance message: {}", userAcceptanceMessage);
        if (!status.getExecution().equals(userAcceptanceMessage.getUserResponse())) {
            throw new ValidationException("Execution status does not match with provided user acceptance message status");
        }

        //If the user has scheduled the update, validate scheduled time is provided
        if (DdiStatus.ExecutionStatus.USER_SCHEDULED.equals(userAcceptanceMessage.getUserResponse()) &&
                (userAcceptanceMessage.getScheduledTime() <= 0 || Instant.ofEpochSecond(userAcceptanceMessage.getScheduledTime()).isBefore(Instant.now()))) {
            throw new ValidationException("Invalid or past scheduled time provided");
        }

        //We've just stored the action status already, find last one for foreign key mapping.
        JpaActionStatus latestActionStatus = actionStatusRepository.findFirstByActionIdOrderByOccurredAtDesc(action.getId());
        LOG.debug("Latest action status: {}", latestActionStatus);

        //store the received user acceptance
        var jpaActionStatusUserAcceptance = JpaActionStatusUserAcceptance.builder()
                .timeStampOfPrompt(userAcceptanceMessage.getTimeStampOfPrompt())
                .userResponse(DeviceActionStatus.valueOf(userAcceptanceMessage.getUserResponse().toString()))
                .prompt(userAcceptanceMessage.getPrompt())
                .vin(userAcceptanceMessage.getVin())
                .otaMasterSerialNumber(userAcceptanceMessage.getOtaMasterSN())
                .ecuHMISerialNumber(userAcceptanceMessage.getEcuHMISN())
                .actionStatus(latestActionStatus)
                .scheduledTime(userAcceptanceMessage.getScheduledTime())
                .build();

        actionStatusUserAcceptanceRepository.save(jpaActionStatusUserAcceptance);
    }

    @Override
    protected void onActionStatusUpdate(final DeviceActionStatus updatedActionStatus, final JpaAction action) {
        switch (updatedActionStatus) {
            case ERROR_RESPONSE_CODE:
                final JpaTarget target = (JpaTarget) action.getTarget();
                target.setUpdateStatus(TargetUpdateStatus.ERROR);
                handleErrorOnAction(action, target);
                break;
            case FINISHED_FAILURE, FINISHED_NOT_EXECUTED:
                handleFinishedAndStoreInTargetStatus(action).ifPresent(this::requestControllerAttributes);
                break;
            // we need to add the case for DOWNLOAD if needed

            default:
                break;
        }
    }


    private void requestControllerAttributes(final String controllerId) {
        final JpaTarget target = (JpaTarget) getByControllerId(controllerId).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        target.setRequestControllerAttributes(true);

        eventPublisherHolder.getEventPublisher().publishEvent(new TargetAttributesRequestedEvent(tenantAware.getCurrentTenant(), target.getId(), target.getControllerId(), JpaTarget.class, eventPublisherHolder.getApplicationId()));
    }

    private void handleErrorOnAction(final JpaAction mergedAction, final JpaTarget mergedTarget) {
        mergedAction.setActive(false);
        mergedAction.setStatus(DeviceActionStatus.ERROR_RESPONSE_CODE);
        mergedTarget.setAssignedDistributionSet(null);

        targetRepository.save(mergedTarget);
    }

    /**
     * Handles the case where the {@link DeviceActionStatus#FINISHED} status is
     * reported by the device. In case the update is finished, a controllerId
     * will be returned to trigger a request for attributes.
     *
     * @param action updated action
     * @return a present controllerId in case the attributes needs to be
     * requested.
     */
    private Optional<String> handleFinishedAndStoreInTargetStatus(final JpaAction action) {
        final JpaTarget target = (JpaTarget) action.getTarget();
        action.setActive(false);
        action.setStatus(action.getStatus());
        final JpaDistributionSet ds = (JpaDistributionSet) entityManager.merge(action.getDistributionSet());

        target.setInstalledDistributionSet(ds);
        target.setInstallationDate(Instant.now().getEpochSecond());


        // check if the assigned set is equal to the installed set (not
        // necessarily the case as another update might be pending already).
        if (target.getAssignedDistributionSet() != null && target.getAssignedDistributionSet().getId().equals(target.getInstalledDistributionSet().getId())) {
            target.setUpdateStatus(TargetUpdateStatus.IN_SYNC);
        }

        targetRepository.save(target);
        entityManager.detach(ds);

        return Optional.of(target.getControllerId());
    }

    @Override
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateControllerAttributesWithSoftware(final String controllerId, final Map<String, String> data, final List<SoftwareOfTarget> targetSoftwares, final UpdateMode mode) {

        validateAttributes(data, targetSoftwares);

        final JpaTarget target = getJpaTarget(controllerId);
        final Map<String, String> controllerAttributes = target.getControllerAttributes();
        final List<JpaTargetSoftware> softwares = prepareTargetSoftwares(targetSoftwares, target);

        final UpdateMode updateMode = mode != null ? mode : UpdateMode.MERGE;
        applyUpdateMode(updateMode, data, controllerAttributes, softwares, target);

        assertTargetAttributesQuota(target);
        return targetRepository.save(target);
    }

    private void validateAttributes(Map<String, String> data, List<SoftwareOfTarget> targetSoftwares) {
        if (data.entrySet().stream().anyMatch(e -> !isAttributeEntryValid(e))) {
            throw new InvalidTargetAttributeException();
        }

        if (targetSoftwares != null) {
            for (SoftwareOfTarget software : targetSoftwares) {
                if (!isAttributeKeyValid(software.getComponentId()) || !isAttributeValueValid(software.getNode()) || !isAttributeValueValid(software.getVersion())) {
                    throw new InvalidTargetAttributeException();
                }
            }
        }
    }

    private JpaTarget getJpaTarget(String controllerId) {
        return targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    private List<JpaTargetSoftware> prepareTargetSoftwares(List<SoftwareOfTarget> targetSoftwares, JpaTarget target) {
        List<JpaTargetSoftware> softwares = new ArrayList<>();
        if (targetSoftwares != null) {
            targetSoftwares.forEach(sw -> softwares.add(new JpaTargetSoftware(sw.getNode(), sw.getComponentId(), sw.getVersion(), target)));
        }
        return softwares;
    }

    private void applyUpdateMode(UpdateMode updateMode, Map<String, String> data, Map<String, String> controllerAttributes, List<JpaTargetSoftware> softwares, JpaTarget target) {

        switch (updateMode) {
            case REMOVE -> applyRemoveMode(data, controllerAttributes, softwares);
            case REPLACE -> applyReplaceMode(data, controllerAttributes, softwares, target);
            case MERGE -> applyMergeMode(data, controllerAttributes, softwares, target);
            default -> throw new IllegalStateException("The update mode " + updateMode + " is not supported.");
        }
    }

    private void applyRemoveMode(Map<String, String> data, Map<String, String> controllerAttributes, List<JpaTargetSoftware> softwares) {
        data.keySet().forEach(controllerAttributes::remove);
        if (!softwares.isEmpty()) {
            targetSoftwareRepository.deleteAll(softwares);
        }
    }

    private void applyReplaceMode(Map<String, String> data, Map<String, String> controllerAttributes, List<JpaTargetSoftware> softwares, JpaTarget target) {
        controllerAttributes.clear();
        copy(data, controllerAttributes);
        target.setRequestControllerAttributes(false);

        if (!softwares.isEmpty()) {
            List<JpaTargetSoftware> targetSoftwarePresent = targetSoftwareRepository.getByTargetId(target.getId());
            deleteObsoleteSoftwares(targetSoftwarePresent, softwares);
            targetSoftwareRepository.saveAll(softwares);
        }
    }

    private void applyMergeMode(Map<String, String> data, Map<String, String> controllerAttributes, List<JpaTargetSoftware> softwares, JpaTarget target) {
        copy(data, controllerAttributes);
        target.setRequestControllerAttributes(false);
        if (!softwares.isEmpty()) {
            targetSoftwareRepository.saveAll(softwares);
        }
    }

    private void deleteObsoleteSoftwares(List<JpaTargetSoftware> targetSoftwarePresent, List<JpaTargetSoftware> softwares) {
        Map<TargetSoftwareCompositeKey, JpaTargetSoftware> softwareMap = new HashMap<>();
        for (JpaTargetSoftware softwareItem : softwares) {
            softwareMap.put(softwareItem.getId(), softwareItem);
        }

        List<JpaTargetSoftware> recordsNotInSoftware = new ArrayList<>();
        for (JpaTargetSoftware targetSoftware : targetSoftwarePresent) {
            if (!softwareMap.containsKey(targetSoftware.getId())) {
                recordsNotInSoftware.add(targetSoftware);
            }
        }
        targetSoftwareRepository.deleteAll(recordsNotInSoftware);
    }


    /**
     * Fetch Target Attribute for given {@link Target} and key.
     *
     * @param targetId targetId of given target
     * @param key      target attribute key
     * @return String attribute_value
     */
    @Override
    public String getTargetAttributeByKey(final long targetId, final String key) {
        return targetRepository.getTargetAttributesByTargetIdAndKey(targetId, key);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateControllerAttributes(final String controllerId, final Map<String, String> data, final UpdateMode mode) {

        /*
         * Constraints on attribute keys & values are not validated by
         * EclipseLink. Hence, they are validated here.
         */
        if (data.entrySet().stream().anyMatch(e -> !isAttributeEntryValid(e))) {
            throw new InvalidTargetAttributeException();
        }

        final JpaTarget target = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        // get the modifiable attribute map
        final Map<String, String> controllerAttributes = target.getControllerAttributes();
        final UpdateMode updateMode = mode != null ? mode : UpdateMode.MERGE;
        switch (updateMode) {
            case REMOVE:
                // remove the addressed attributes
                data.keySet().forEach(controllerAttributes::remove);
                break;
            case REPLACE:
                // clear the attributes before adding the new attributes
                controllerAttributes.clear();
                copy(data, controllerAttributes);
                target.setRequestControllerAttributes(false);
                break;
            case MERGE:
                // just merge the attributes in
                copy(data, controllerAttributes);
                target.setRequestControllerAttributes(false);
                break;
            default:
                // unknown update mode
                throw new IllegalStateException("The update mode " + updateMode + " is not supported.");
        }
        assertTargetAttributesQuota(target);

        return targetRepository.save(target);
    }

    private void assertTargetAttributesQuota(final JpaTarget target) {
        final int limit = quotaManagement.getMaxAttributeEntriesPerTarget();
        QuotaHelper.assertAssignmentQuota(target.getId(), target.getControllerAttributes().size(), limit, "Attribute", Target.class.getSimpleName(), null);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action registerRetrieved(final long actionId, final String message) {
        return handleRegisterRetrieved(actionId, message);
    }

    /**
     * Registers retrieved status for given {@link Target} and {@link Action} if
     * it does not exist yet.
     *
     * @param actionId to the handle status for
     * @param message  for the status
     * @return the updated action in case the status has been changed to
     * {@link DeviceActionStatus#DD_SENT}
     */
    private Action handleRegisterRetrieved(final Long actionId, final String message) {
        final JpaAction action = getActionAndThrowExceptionIfNotFound(actionId);
        // do a manual query with CriteriaBuilder to avoid unnecessary field
        // queries and an extra
        // count query made by spring-data when using pageable requests, we
        // don't need an extra count
        // query, we just want to check if the last action status is a retrieved
        // or not.
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        final CriteriaQuery<Object[]> queryActionStatus = cb.createQuery(Object[].class);
        final Root<JpaActionStatus> actionStatusRoot = queryActionStatus.from(JpaActionStatus.class);
        final CriteriaQuery<Object[]> query = queryActionStatus.multiselect(actionStatusRoot.get(AbstractJpaBaseEntity_.id), actionStatusRoot.get((JpaActionStatus_.status))).where(cb.equal(actionStatusRoot.get(JpaActionStatus_.action).get(AbstractJpaBaseEntity_.id), actionId)).orderBy(cb.desc(actionStatusRoot.get(AbstractJpaBaseEntity_.id)));
        final List<Object[]> resultList = entityManager.createQuery(query).setFirstResult(0).setMaxResults(1).getResultList();

        // if the latest status is not in retrieve state then we add a retrieved
        // state again, we want
        // to document a deployment retrieved status and a cancel retrieved
        // status, but multiple
        // retrieves after the other we don't want to store to protect to
        // overflood action status in
        // case controller retrieves a action multiple times.
        if (resultList.isEmpty() || (DeviceActionStatus.DD_SENT != resultList.get(0)[1])) {
            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, DeviceActionStatus.DD_SENT, Instant.now().getEpochSecond(), message));

            // don't change the action status itself in case the action is in
            // canceling state otherwise
            // we modify the action status and the controller won't get the
            // cancel job anymore.
            if (!action.isCancelingOrCanceled()) {
                action.setStatus(DeviceActionStatus.DD_SENT);
                return actionRepository.save(action);
            }
        }
        return action;
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public ActionStatus addInformationalActionStatus(final ActionStatusCreate c) {
        final JpaActionStatusCreate create = (JpaActionStatusCreate) c;
        final JpaAction action = getActionAndThrowExceptionIfNotFound(create.getActionId());
        final JpaActionStatus statusMessage = create.build();
        statusMessage.setAction(action);

        assertActionStatusQuota(action);
        assertActionStatusMessageQuota(statusMessage);

        return actionStatusRepository.save(statusMessage);
    }

    @Override
    public Optional<Target> getByControllerId(final String controllerId) {
        return targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).map(Target.class::cast);
    }

    @Override
    public Optional<Target> get(final long targetId) {
        return targetRepository.findById(targetId).map(t -> t);
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final long actionId) {
        if (!actionRepository.existsById(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    @Override
    public List<String> getActionHistoryMessages(final long actionId, final int messageCount) {
        // Just return empty list in case messageCount is zero.
        if (messageCount == 0) {
            return Collections.emptyList();
        }

        // For negative and large value of messageCount, limit the number of
        // messages.
        final int limit = messageCount < 0 || messageCount >= RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT ? RepositoryConstants.MAX_ACTION_HISTORY_MSG_COUNT : messageCount;

        final PageRequest pageable = PageRequest.of(0, limit, Sort.by(Direction.DESC, "occurredAt"));
        final Page<String> messages = actionStatusRepository.findMessagesByActionIdAndMessageNotLike(pageable, actionId, RepositoryConstants.SERVER_MESSAGE_PREFIX + "%");

        LOG.debug("Retrieved {} message(s) from action history for action {}.", messages.getNumberOfElements(), actionId);

        return messages.getContent();
    }

    @Override
    public Optional<SoftwareModule> getSoftwareModule(final long id) {
        return softwareModuleRepository.findById(id).map(s -> s);
    }

    /**
     * @param moduleId of the {@link SoftwareModule}
     * @return
     */
    public Map<Long, List<SoftwareModuleInfo>> findTargetVisibleMetaDataBySoftwareModuleId(final Collection<Long> moduleId) {
        Long[] moduleIdArray = moduleId.toArray(new Long[0]); // Convert List<Long> to Long[]
        Page<Object[]> page = softwareModuleMetadataRepository.findBySoftwareModuleIdInAndTargetVisible(PageRequest.of(0, RepositoryConstants.MAX_META_DATA_COUNT), moduleIdArray, true);

        return page.getContent().stream().map(row -> new SoftwareModuleInfo((Long) row[0],   // SoftwareModuleId from Object[]
                (Boolean) row[1]  // Target visibility flag from Object[]
        )).collect(Collectors.groupingBy(SoftwareModuleInfo::getSoftwareModuleId)); // Group by SoftwareModuleId
    }

    /**
     * Cancels given {@link Action} for this {@link Target}. The method will
     * immediately add a {@link DeviceActionStatus#CANCELED} status to the action. However,
     * it might be possible that the controller will continue to work on the
     * cancellation. The controller needs to acknowledge or reject the
     * cancellation using postCancelActionFeedback.
     *
     * @param actionId to be canceled
     * @return canceled {@link Action}
     * @throws CancelActionNotAllowedException in case the given action is not active or is already canceled
     * @throws EntityNotFoundException         if action with given actionId does not exist.
     */
    @Override
    @Modifying
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Action cancelAction(final long actionId) {
        LOG.debug("cancelAction({})", actionId);

        final JpaAction action = actionRepository.findById(actionId).orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        if (action.isActive()) {
            LOG.debug("action ({}) was still active. Change to {}.", action, DeviceActionStatus.CANCELING);
            action.setStatus(DeviceActionStatus.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, DeviceActionStatus.CANCELING, Instant.now().getEpochSecond(), "manual cancelation requested"));
            final Action saveAction = actionRepository.save(action);
            cancelAssignDistributionSetEvent(action);

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException("Action [id: " + action.getId() + "] is not active and cannot be canceled");
        }
    }

    @Override
    public void updateActionExternalRef(final long actionId, @NotEmpty final String externalRef) {
        actionRepository.updateExternalRef(actionId, externalRef);
    }

    @Override
    public Optional<Action> getActionByExternalRef(@NotEmpty final String externalRef) {
        return actionRepository.findByExternalRef(externalRef);
    }

    @Override
    public Optional<Action> getInstalledActionByTarget(final String controllerId) {
        final JpaTarget jpaTarget = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        final JpaDistributionSet installedDistributionSet = jpaTarget.getInstalledDistributionSet();
        if (null != installedDistributionSet) {
            return actionRepository.findFirstByTargetIdAndDistributionSetIdAndStatusInAndActiveIsTrueOrderByIdDesc(jpaTarget.getId(), installedDistributionSet.getId(), List.of(DeviceActionStatus.FINISHED_SUCCESS, DeviceActionStatus.FINISHED_FAILURE, DeviceActionStatus.FINISHED_NOT_EXECUTED));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public AutoConfirmationStatus activateAutoConfirmation(final String controllerId, final String initiator, final String remark) {
        return confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);
    }

    @Override
    public void deactivateAutoConfirmation(final String controllerId) {
        confirmationManagement.deactivateAutoConfirmation(controllerId);
    }

    private void cancelAssignDistributionSetEvent(final Action action) {
        afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher().publishEvent(new CancelTargetAssignmentEvent(action, eventPublisherHolder.getApplicationId())));
    }

    // for testing
    void setTargetRepository(final TargetRepository targetRepositorySpy) {
        this.targetRepository = targetRepositorySpy;
    }

    /**
     * commented these as a part of removing sp_polling and its references.
     */
    @Deprecated
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Polling setPolling(Target target, Polling.Status status) {
//        final JpaPollingCreate pollingCreate = (JpaPollingCreate) entityFactory.polling().create().target(target).status(Polling.Status.POLLED);
//        return pollingRepository.save(pollingCreate.build());
        return null;
    }

    @Deprecated
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void setPollingById(Long id, Polling.Status status) {
//        Polling polling = pollingRepository.findById(id).orElseThrow(() -> new EntityNotFoundException(Polling.class, id));
//        pollingRepository.setStatus(status, Instant.now().getEpochSecond(), polling.getId());
//        LOG.debug("Updating the status to \"{}\" for record with id: {}", status, polling.getId());
    }

    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @Description("Add general feedback for controller id. Which is not related to any rollout specific")
    public void addFeedbackByControllerId(Target target, DdiFeedbackRequestBody feedbackRequestBody) {
        //Validate if ExecutionType is ERC then ErrorCode is mandatory
        if (feedbackRequestBody.getExecution().equals(ExecutionType.ERC) && (Objects.isNull(feedbackRequestBody.getErrorCode()) || feedbackRequestBody.getErrorCode().isEmpty())) {
            throw new ValidationException("ErrorCode is mandatory when execution is ERC");
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            //save feedback when all validations are passed
            generalFeedbackRepository.save(JpaGeneralFeedback.builder().targetId(target.getId())
                    .code(feedbackRequestBody.getCode()).details(mapper.writeValueAsString(feedbackRequestBody.getDetails()))
                    .execution(feedbackRequestBody.getExecution())
                    .errorCode(mapper.writeValueAsString(feedbackRequestBody.getErrorCode()))
                    .timestamp(feedbackRequestBody.getTimestamp()).build());
            // Convert feedbackRequestBody to JSON string
            String feedbackJson = mapper.writeValueAsString(feedbackRequestBody);

            // Check if ExecutionType is not ERC, and send to GeneralIdleMessage
            if (!feedbackRequestBody.getExecution().equals(ExecutionType.ERC)) {
                // Create GeneralIdleMessage from feedbackRequestBody
                GeneralIdleMessage generalIdleMessage = GeneralIdleMessage.builder()
                        .vehicleId(target.getControllerId())
                        .status("IDLE")
                        .messages(Collections.singletonList(String.valueOf(feedbackRequestBody.getDetails())))
                        .timestamp(feedbackRequestBody.getTimestamp())
                        .build();

                String[] vinParts = target.getControllerId().split("_", 2);
                String vin = vinParts.length > 0 ? vinParts[0] : "";
                String otaMasterSerialNumber = vinParts.length > 1 ? vinParts[1] : "";
                // Build KafkaEventHeader
                KafkaEventHeader header = KafkaEventHeader.builder()
                        .tenant(target.getTenant())
                        .vin(vin)
                        .otaMasterSerialNumber(otaMasterSerialNumber)
                        .build();

                // Wrap in KafkaEventTemplate
                KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                        .header(header)
                        .payload(generalIdleMessage)
                        .build();

                // Send the GeneralIdleMessage using KafkaMessageService with messageType
                kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.GENERAL_IDLE);
            }

            // If ExecutionType is ERC, create and send GeneralErrorMessage
            if (feedbackRequestBody.getExecution().equals(ExecutionType.ERC)) {
                GeneralErrorMessage generalErrorMessage = GeneralErrorMessage.builder()
                        .vehicleId(target.getControllerId())
                        .status("ERC")
                        .errorCode(Collections.singletonList(String.valueOf(feedbackRequestBody.getErrorCode())))
                        .errorMessages(Collections.singletonList(String.valueOf(feedbackRequestBody.getDetails())))
                        .timestamp(feedbackRequestBody.getTimestamp())
                        .build();

                String[] vinParts = target.getControllerId().split("_", 2);
                String vin = vinParts.length > 0 ? vinParts[0] : "";
                String otaMasterSerialNumber = vinParts.length > 1 ? vinParts[1] : "";

                // Build KafkaEventHeader
                KafkaEventHeader header = KafkaEventHeader.builder()
                        .tenant(target.getTenant())
                        .vin(vin)
                        .otaMasterSerialNumber(otaMasterSerialNumber)
                        .build();

                // Wrap in KafkaEventTemplate
                KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                        .header(header)
                        .payload(generalErrorMessage)
                        .build();

                // Send the GeneralErrorMessage using KafkaMessageService with messageType
                kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.GENERAL_ERROR);
            }


        } catch (JsonProcessingException e) {
            LOG.error("Cannot convert DdiFeedbackRequestBody into JSON String");
            throw org.eclipse.persistence.exceptions.ValidationException.cannotCastToClass(feedbackRequestBody, DdiFeedbackRequestBody.class, String.class);
        }
    }

    /**
     * This method adds some mandatory device inventory
     * attributes to sp target table
     **/
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Target updateTargetAttributes(String controllerId, Map<String, String> targetAttributes) {
        final JpaTarget target = targetRepository.findByControllerId(controllerId).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
        final Map<String, String> controllerAttributes = target.getControllerAttributes();
        copy(targetAttributes, controllerAttributes);
        assertTargetAttributesQuota(target);
        return targetRepository.save(target);
    }

    /**
     * This method adds device inventory details to sp_target_inventory table for given {@link Target}
     *
     * @param controllerId     the controllerId of {@link Target}
     * @param inventoryDetails the {@link DeviceInventoryDetails}
     * @return {@link TargetInventory}
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public TargetInventory updateTargetInventory(String controllerId, DeviceInventoryDetails inventoryDetails, String rawInventoryDetails) {
        final JpaTarget target = targetRepository.findByControllerId(controllerId).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        // Replace target softwares from device inventory
        Set<TargetSoftware> targetSoftwares = inventoryDetails.getEcuList().stream().flatMap(ecu -> ecu.getScomos().stream().map(scomos -> new AbstractMap.SimpleImmutableEntry<>(ecu.getNodeAddress(), scomos))).map(e -> (TargetSoftware) new JpaTargetSoftware(e.getKey(), e.getValue().getScomoId(), e.getValue().getSwVersion(), target)).collect(Collectors.toSet());
        updateTargetSoftware(controllerId, targetSoftwares);

        // Build target inventory entity
        JpaTargetInventory.JpaTargetInventoryBuilder builder = JpaTargetInventory.builder()
                .target(target)
                .inventory(inventoryDetails);

        // Only set rawInventory if rawInventoryDetails is provided
        if (rawInventoryDetails != null && !rawInventoryDetails.isBlank()) {
            builder.rawInventory(rawInventoryDetails);
        }

        return targetInventoryRepository.save(builder.build());
    }

    /**
     * This method fetch device inventory details from sp_target_inventory table for given {@link Target}
     *
     * @param controllerId the controllerId of {@link Target}
     * @return {@link TargetInventory}
     */
    @Override
    public List<TargetInventory> getTargetInventory(String controllerId) {
        final JpaTarget target = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId)).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
        return targetInventoryRepository.findByTarget(target);
    }

    /**
     * This method adds device inventory's, ecu's software details to sp_target_software table for given {@link Target}
     *
     * @param controllerId the controllerId of {@link Target}
     * @param softwares    the set of {@link TargetSoftware}
     * @return set of {@link TargetSoftware}
     */
    @Override
    @Transactional
    public Set<TargetSoftware> updateTargetSoftware(String controllerId, Set<TargetSoftware> softwares) {
        final JpaTarget target = targetRepository.findByControllerId(controllerId).orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
        target.setSoftwares(new ArrayList<>(softwares));
        targetRepository.save(target);
        return softwares;
    }

    /**
     * This method counts no of {@link ActionStatus}'s available for given {@link Rollout#getId()} and {@link DeviceActionStatus}
     *
     * @param rolloutId the {@link Rollout#getId()}
     * @param status    the {@link DeviceActionStatus}
     * @return Long count of {@link ActionStatus}
     */
    @Override
    public Long countActionStatusByRolloutIdAndStatus(Long rolloutId, DeviceActionStatus status) {
        return actionStatusRepository.countByRolloutIdAndStatus(rolloutId, status);
    }

    /**
     * Retrieves the ECU (Electronic Control Unit) nodes for a given target and a set of software modules.
     * This method fetches the software details of ECUs from the target software table for the provided target and software modules.
     *
     * @param target          The target for which the ECU nodes are to be fetched.
     * @param softwareModules The set of software modules associated with the ECU nodes.
     * @return A set of {@link TargetSoftware} representing the ECU nodes for the given target and software modules.
     */
    @Override
    public Set<TargetSoftware> getEcuNodes(Target target, Set<String> softwareModules) {
        return targetSoftwareRepository.getByTargetIdAndComponentIds(target, softwareModules).stream().map(TargetSoftware.class::cast).collect(Collectors.toSet());
    }

    /**
     * EventTimer to handle reduction of polling interval based on maintenance
     * window start time. Class models the next polling time as an event to be
     * raised and time to next polling as a timer. The event, in this case the
     * polling, should happen when timer expires. Class makes use of java.time
     * package to manipulate and calculate timer duration.
     */
    private static class EventTimer {

        private final String defaultEventInterval;
        private final Duration defaultEventIntervalDuration;

        private final String minimumEventInterval;
        private final Duration minimumEventIntervalDuration;

        private final TemporalUnit timeUnit;

        /**
         * Constructor.
         *
         * @param defaultEventInterval default timer value to use for interval between events.
         *                             This puts an upper bound for the timer value
         * @param minimumEventInterval for loading {@link DistributionSet#getModules()}. This
         *                             puts a lower bound to the timer value
         * @param timeUnit             representing the unit of time to be used for timer.
         */
        EventTimer(final String defaultEventInterval, final String minimumEventInterval, final TemporalUnit timeUnit) {
            this.defaultEventInterval = defaultEventInterval;
            this.defaultEventIntervalDuration = MaintenanceScheduleHelper.convertToISODuration(defaultEventInterval);

            this.minimumEventInterval = minimumEventInterval;
            this.minimumEventIntervalDuration = MaintenanceScheduleHelper.convertToISODuration(minimumEventInterval);

            this.timeUnit = timeUnit;
        }

        /**
         * This method calculates the time interval until the next event based
         * on the desired number of events before the time when interval is
         * reset to default. The return value is bounded by
         * {@link EventTimer#defaultEventInterval} and
         * {@link EventTimer#minimumEventInterval}.
         *
         * @param eventCount     number of events desired until the interval is reset to
         *                       default. This is not guaranteed as the interval between
         *                       events cannot be less than the minimum interval
         * @param timerResetTime time when exponential forwarding should reset to default
         * @return String in HH:mm:ss format for time to next event.
         */
        String timeToNextEvent(final int eventCount, final ZonedDateTime timerResetTime) {
            final ZonedDateTime currentTime = ZonedDateTime.now();

            // If there is no reset time, or if we already past the reset time,
            // return the default interval.
            if (timerResetTime == null || currentTime.compareTo(timerResetTime) > 0) {
                return defaultEventInterval;
            }

            // Calculate the interval timer based on desired event count.
            final Duration currentIntervalDuration = Duration.of(currentTime.until(timerResetTime, timeUnit), timeUnit).dividedBy(eventCount);

            // Need not return interval greater than the default.
            if (currentIntervalDuration.compareTo(defaultEventIntervalDuration) > 0) {
                return defaultEventInterval;
            }

            // Should not return interval less than minimum.
            if (currentIntervalDuration.compareTo(minimumEventIntervalDuration) < 0) {
                return minimumEventInterval;
            }

            return String.format("%02d:%02d:%02d", currentIntervalDuration.toHours(), currentIntervalDuration.toMinutes() % 60, currentIntervalDuration.getSeconds() % 60);
        }
    }

    private static class TargetPoll {

        private final String tenant;
        private final String controllerId;

        TargetPoll(final Target target) {
            this.tenant = target.getTenant();
            this.controllerId = target.getControllerId();
        }

        public String getTenant() {
            return tenant;
        }

        public String getControllerId() {
            return controllerId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (controllerId == null ? 0 : controllerId.hashCode());
            result = prime * result + (tenant == null ? 0 : tenant.hashCode());
            return result;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TargetPoll other = (TargetPoll) obj;
            if (controllerId == null) {
                if (other.controllerId != null) {
                    return false;
                }
            } else if (!controllerId.equals(other.controllerId)) {
                return false;
            }
            if (tenant == null) {
                return other.tenant == null;
            } else return tenant.equals(other.tenant);
        }


    }
}
