/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.mgmt.action.dto.DeviceActionStatusTimestampResponse;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.QuotaManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.remote.TargetAssignDistributionSetEvent;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.ForceQuitActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.IncompatibleTargetTypeException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.MultiAssignmentIsNotEnabledException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus_;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction_;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget_;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.DeploymentHelper;
import org.eclipse.hawkbit.repository.jpa.utils.QuotaHelper;
import org.eclipse.hawkbit.repository.jpa.utils.WeightValidationHelper;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.hawkbit.repository.model.DeploymentRequest;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetAssignmentResult;
import org.eclipse.hawkbit.repository.model.DistributionSetInvalidation.CancelationType;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.TargetWithUserAcceptanceRequired;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.ListJoin;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.validation.ValidationException;
import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties.TenantConfigurationKey.REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED;

/**
 * JPA implementation for {@link DeploymentManagement}.
 */
@Transactional(readOnly = true)
@Validated
public class JpaDeploymentManagement extends JpaActionManagement implements DeploymentManagement {

    private static final Logger LOG = LoggerFactory.getLogger(JpaDeploymentManagement.class);

    /**
     * Maximum amount of Actions that are started at once.
     */
    private static final int ACTION_PAGE_LIMIT = 1000;
    /**
     * Max amount of targets that are handled in one transaction.
     */
    private static final int TRANSACTION_TARGETS = 5_000;

    private static final String MESSAGES = "messages";
    private static final String OTA_RUNNING_STATUS = "OTA is running";

    private static final String QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT = "DELETE FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at LIMIT "
            + ACTION_PAGE_LIMIT;

    private static final EnumMap<Database, String> QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED;

    static {
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED = new EnumMap<>(Database.class);
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.put(Database.SQL_SERVER, "DELETE TOP (" + ACTION_PAGE_LIMIT
                + ") FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at ");
        QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.put(Database.POSTGRESQL,
                "DELETE FROM sp_action WHERE id IN (SELECT id FROM sp_action WHERE tenant=#tenant AND status IN (%s) AND last_modified_at<#last_modified_at LIMIT "
                        + ACTION_PAGE_LIMIT + ")");
    }

    private final EntityManager entityManager;
    private final DistributionSetManagement distributionSetManagement;
    private final DistributionSetRepository distributionSetRepository;
    private final TargetRepository targetRepository;
    private final AuditorAware<String> auditorProvider;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final PlatformTransactionManager txManager;
    private final OnlineDsAssignmentStrategy onlineDsAssignmentStrategy;
    private final OfflineDsAssignmentStrategy offlineDsAssignmentStrategy;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final TenantAware tenantAware;
    private final Database database;
    private final RetryTemplate retryTemplate;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final RolloutRepository rolloutRepository;
    private final TargetManagement targetManagement;
    private final KafkaMessageService kafkaMessageService;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected JpaDeploymentManagement(final EntityManager entityManager, final ActionRepository actionRepository,
                                      final DistributionSetManagement distributionSetManagement,
                                      final DistributionSetRepository distributionSetRepository, final TargetRepository targetRepository,
                                      final ActionStatusRepository actionStatusRepository, final AuditorAware<String> auditorProvider,
                                      final EventPublisherHolder eventPublisherHolder, final AfterTransactionCommitExecutor afterCommit,
                                      final VirtualPropertyReplacer virtualPropertyReplacer, final PlatformTransactionManager txManager,
                                      final TenantConfigurationManagement tenantConfigurationManagement, final QuotaManagement quotaManagement,
                                      final SystemSecurityContext systemSecurityContext, final TenantAware tenantAware, final Database database,
                                      final RepositoryProperties repositoryProperties, final ActionStatusUserAcceptanceRepository actionStatusUserAcceptanceRepository,
                                      final RolloutGroupManagement rolloutGroupManagement, final RolloutRepository rolloutRepository, TargetManagement targetManagement,
                                      final KafkaMessageService kafkaMessageService) {
        super(actionRepository, actionStatusRepository, actionStatusUserAcceptanceRepository, quotaManagement, repositoryProperties);
        this.entityManager = entityManager;
        this.distributionSetRepository = distributionSetRepository;
        this.distributionSetManagement = distributionSetManagement;
        this.targetRepository = targetRepository;
        this.auditorProvider = auditorProvider;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.txManager = txManager;
        this.kafkaMessageService = kafkaMessageService;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.rolloutRepository = rolloutRepository;
        this.targetManagement = targetManagement;
        onlineDsAssignmentStrategy = new OnlineDsAssignmentStrategy(targetRepository, afterCommit, eventPublisherHolder,
                actionRepository, actionStatusRepository, quotaManagement, this::isMultiAssignmentsEnabled,
                this::isConfirmationFlowEnabled);
        offlineDsAssignmentStrategy = new OfflineDsAssignmentStrategy(targetRepository, afterCommit,
                eventPublisherHolder, actionRepository, actionStatusRepository, quotaManagement,
                this::isMultiAssignmentsEnabled, this::isConfirmationFlowEnabled);
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.tenantAware = tenantAware;
        this.database = database;
        this.retryTemplate = createRetryTemplate();
    }

    private static Map<Long, List<TargetWithUserAcceptanceRequired>> convertRequest(
            final Collection<DeploymentRequest> deploymentRequests) {
        return deploymentRequests.stream().collect(Collectors.groupingBy(DeploymentRequest::getDistributionSetId,
                Collectors.mapping(DeploymentRequest::getTargetWithUserAcceptanceRequired, Collectors.toList())));
    }

    private static void checkIfRequiresMultiAssignment(final Collection<DeploymentRequest> deploymentRequests) {
        final long distinctTargetsInRequest = deploymentRequests.stream()
                .map(request -> request.getTargetWithUserAcceptanceRequired().getControllerId()).distinct().count();
        if (distinctTargetsInRequest < deploymentRequests.size()) {
            throw new MultiAssignmentIsNotEnabledException();
        }
    }

    /**
     * split tIDs length into max entries in-statement because many database
     * have constraint of max entries in in-statements e.g. Oracle with maximum
     * 1000 elements, so we need to split the entries here and execute multiple
     * statements
     */
    private static List<List<Long>> getTargetEntitiesAsChunks(final List<JpaTarget> targetEntities) {
        return Lists.partition(targetEntities.stream().map(Target::getId).collect(Collectors.toList()),
                Constants.MAX_ENTRIES_IN_STATEMENT);
    }

    private static DistributionSetAssignmentResult buildAssignmentResult(final JpaDistributionSet distributionSet,
                                                                         final List<JpaAction> assignedActions, final int totalTargetsForAssignment) {
        final int alreadyAssignedTargetsCount = totalTargetsForAssignment - assignedActions.size();

        return new DistributionSetAssignmentResult(distributionSet, alreadyAssignedTargetsCount, assignedActions);
    }

    private static String getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(final Database database) {
        return QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED.getOrDefault(database,
                QUERY_DELETE_ACTIONS_BY_STATE_AND_LAST_MODIFIED_DEFAULT);
    }

    private static String formatInClauseWithNumberKeys(final int count) {
        return formatInClause(IntStream.range(0, count).mapToObj(String::valueOf).collect(Collectors.toList()));
    }

    private static String formatInClause(final Collection<String> elements) {
        return "#" + String.join(",#", elements);
    }

    private static RetryTemplate createRetryTemplate() {
        final RetryTemplate template = new RetryTemplate();

        final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
        backOffPolicy.setBackOffPeriod(Constants.TX_RT_DELAY);
        template.setBackOffPolicy(backOffPolicy);

        final SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(Constants.TX_RT_MAX,
                Collections.singletonMap(ConcurrencyFailureException.class, true));
        template.setRetryPolicy(retryPolicy);

        return template;
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> offlineAssignedDistributionSets(
            final Collection<Entry<String, Long>> assignments) {
        final Collection<Entry<String, Long>> distinctAssignments = assignments.stream().distinct()
                .collect(Collectors.toList());

        enforceMaxAssignmentsPerRequest(distinctAssignments.size());
        final List<DeploymentRequest> deploymentRequests = distinctAssignments.stream()
                .map(entry -> DeploymentManagement.deploymentRequest(entry.getKey(), entry.getValue()).build())
                .collect(Collectors.toList());

        return assignDistributionSets(tenantAware.getCurrentUsername(), deploymentRequests, null,
                offlineDsAssignmentStrategy);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> assignDistributionSets(
            final List<DeploymentRequest> deploymentRequests) {
        return assignDistributionSets(tenantAware.getCurrentUsername(), deploymentRequests, null);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<DistributionSetAssignmentResult> assignDistributionSets(final String initiatedBy,
                                                                        final List<DeploymentRequest> deploymentRequests, final String actionMessage) {
        WeightValidationHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .validate(deploymentRequests);
        return assignDistributionSets(initiatedBy, deploymentRequests, actionMessage, onlineDsAssignmentStrategy);
    }

    private List<DistributionSetAssignmentResult> assignDistributionSets(final String initiatedBy,
                                                                         final List<DeploymentRequest> deploymentRequests, final String actionMessage,
                                                                         final AbstractDsAssignmentStrategy strategy) {
        final List<DeploymentRequest> validatedRequests = validateRequestForAssignments(deploymentRequests);
        final Map<Long, List<TargetWithUserAcceptanceRequired>> assignmentsByDsIds = convertRequest(validatedRequests);

        final List<DistributionSetAssignmentResult> results = assignmentsByDsIds.entrySet().stream()
                .map(entry -> assignDistributionSetToTargetsWithRetry(initiatedBy, entry.getKey(), entry.getValue(),
                        actionMessage, strategy))
                .collect(Collectors.toList());
        strategy.sendDeploymentEvents(results);
        return results;
    }

    private List<DeploymentRequest> validateRequestForAssignments(List<DeploymentRequest> deploymentRequests) {
        if (!isMultiAssignmentsEnabled()) {
            deploymentRequests = deploymentRequests.stream().distinct().collect(Collectors.toList());
            checkIfRequiresMultiAssignment(deploymentRequests);
        }
        checkForTargetTypeCompatibility(deploymentRequests);
        checkQuotaForAssignment(deploymentRequests);
        return deploymentRequests;
    }

    private void checkForTargetTypeCompatibility(final List<DeploymentRequest> deploymentRequests) {
        final List<String> controllerIds = deploymentRequests.stream().map(DeploymentRequest::getControllerId)
                .distinct().collect(Collectors.toList());
        final List<Long> distSetIds = deploymentRequests.stream().map(DeploymentRequest::getDistributionSetId)
                .distinct().collect(Collectors.toList());

        if (controllerIds.size() > 1 && distSetIds.size() > 1) {
            throw new IllegalStateException(
                    "Assigning multiple Targets to multiple Distribution Sets simultaneously is not allowed!");
        }

        if (distSetIds.size() == 1) {
            checkCompatibilityForSingleDsAssignment(distSetIds.iterator().next(), controllerIds);
        } else {
            checkCompatibilityForMultiDsAssignment(controllerIds.iterator().next(), distSetIds);
        }
    }

    private void checkCompatibilityForSingleDsAssignment(final Long distSetId, final List<String> controllerIds) {
        final DistributionSetType distSetType = distributionSetManagement.getValidAndComplete(distSetId).getType();
        final Set<String> incompatibleTargetTypes = Lists.partition(controllerIds, Constants.MAX_ENTRIES_IN_STATEMENT)
                .stream()
                .map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids)
                        .and(TargetSpecifications.notCompatibleWithDistributionSetType(distSetType.getId()))))
                .flatMap(List::stream).map(Target::getTargetType).map(TargetType::getName).collect(Collectors.toSet());

        if (!incompatibleTargetTypes.isEmpty()) {
            throw new IncompatibleTargetTypeException(incompatibleTargetTypes, distSetType.getName());
        }
    }

    private void checkCompatibilityForMultiDsAssignment(final String controllerId, final List<Long> distSetIds) {
        final Target target = targetRepository.findOne(TargetSpecifications.hasControllerId(controllerId))
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));

        if (target.getTargetType() != null) {
            // we assume that list of assigned DS is less than
            // MAX_ENTRIES_IN_STATEMENT
            final Set<DistributionSetType> incompatibleDistSetTypes = distributionSetManagement.get(distSetIds).stream()
                    .map(DistributionSet::getType).collect(Collectors.toSet());
            incompatibleDistSetTypes.removeAll(target.getTargetType().getCompatibleDistributionSetTypes());

            if (!incompatibleDistSetTypes.isEmpty()) {
                final Set<String> distSetTypeNames = incompatibleDistSetTypes.stream().map(DistributionSetType::getName)
                        .collect(Collectors.toSet());
                throw new IncompatibleTargetTypeException(target.getTargetType().getName(), distSetTypeNames);
            }
        }
    }

    private DistributionSetAssignmentResult assignDistributionSetToTargetsWithRetry(final String initiatedBy,
                                                                                    final Long dsID, final Collection<TargetWithUserAcceptanceRequired> targetsWithUserAcceptanceRequired, final String actionMessage,
                                                                                    final AbstractDsAssignmentStrategy assignmentStrategy) {
        final RetryCallback<DistributionSetAssignmentResult, ConcurrencyFailureException> retryCallback = retryContext -> assignDistributionSetToTargets(
                initiatedBy, dsID, targetsWithUserAcceptanceRequired, actionMessage, assignmentStrategy);
        return retryTemplate.execute(retryCallback);
    }

    /**
     * method assigns the {@link DistributionSet} to all {@link Target}s by
     * their IDs with a specific {@link MgmtRolloutUserAcceptanceRequired} and {@code forcetime}.
     * <p>
     * <p>
     * In case the update was executed offline (i.e. not managed by hawkBit) the
     * handling differs my means that:<br/>
     * A. it ignores targets completely that are in
     * {@link TargetUpdateStatus#PENDING}.<br/>
     * B. it created completed actions.<br/>
     * C. sets both installed and assigned DS on the target and switches the
     * status to {@link TargetUpdateStatus#IN_SYNC} <br/>
     * D. does not send a {@link TargetAssignDistributionSetEvent}.<br/>
     *
     * @param initiatedBy                       the username of the user who initiated the assignment
     * @param dsID                              the ID of the distribution set to assign
     * @param targetsWithUserAcceptanceRequired a list of all targets and their user acceptance required
     * @param actionMessage                     an optional message to be written into the action status
     * @param assignmentStrategy                the assignment strategy (online /offline)
     * @return the assignment result
     * @throws IncompleteDistributionSetException if mandatory {@link SoftwareModuleType} are not assigned as
     *                                            define by the {@link DistributionSetType}.
     */
    private DistributionSetAssignmentResult assignDistributionSetToTargets(final String initiatedBy, final Long dsID,
                                                                           final Collection<TargetWithUserAcceptanceRequired> targetsWithUserAcceptanceRequired, final String actionMessage,
                                                                           final AbstractDsAssignmentStrategy assignmentStrategy) {

        final JpaDistributionSet distributionSetEntity = (JpaDistributionSet) distributionSetManagement
                .getValidAndComplete(dsID);
        final List<String> providedTargetIds = targetsWithUserAcceptanceRequired.stream().map(TargetWithUserAcceptanceRequired::getControllerId)
                .distinct().collect(Collectors.toList());

        final List<String> existingTargetIds = Lists.partition(providedTargetIds, Constants.MAX_ENTRIES_IN_STATEMENT)
                .stream().map(ids -> targetRepository.findAll(TargetSpecifications.hasControllerIdIn(ids)))
                .flatMap(List::stream).map(JpaTarget::getControllerId).collect(Collectors.toList());

        final List<JpaTarget> targetEntities = assignmentStrategy.findTargetsForAssignment(existingTargetIds,
                distributionSetEntity.getId());

        if (targetEntities.isEmpty()) {
            return allTargetsAlreadyAssignedResult(distributionSetEntity, existingTargetIds.size());
        }

        final List<TargetWithUserAcceptanceRequired> existingTargetsWithUserAcceptanceRequired = targetsWithUserAcceptanceRequired.stream()
                .filter(target -> existingTargetIds.contains(target.getControllerId())).collect(Collectors.toList());

        final List<JpaAction> assignedActions = doAssignDistributionSetToTargets(initiatedBy,
                existingTargetsWithUserAcceptanceRequired, actionMessage, assignmentStrategy, distributionSetEntity,
                targetEntities);
        return buildAssignmentResult(distributionSetEntity, assignedActions, existingTargetsWithUserAcceptanceRequired.size());
    }

    private DistributionSetAssignmentResult allTargetsAlreadyAssignedResult(
            final JpaDistributionSet distributionSetEntity, final int alreadyAssignedCount) {
        // detaching as it is not necessary to persist the set itself
        entityManager.detach(distributionSetEntity);
        // return with nothing as all targets had the DS already assigned
        return new DistributionSetAssignmentResult(distributionSetEntity, alreadyAssignedCount,
                Collections.emptyList());
    }

    private List<JpaAction> doAssignDistributionSetToTargets(final String initiatedBy,
                                                             final Collection<TargetWithUserAcceptanceRequired> targetsWithUserAcceptanceRequired, final String actionMessage,
                                                             final AbstractDsAssignmentStrategy assignmentStrategy, final JpaDistributionSet distributionSetEntity,
                                                             final List<JpaTarget> targetEntities) {
        final List<List<Long>> targetEntitiesIdsChunks = getTargetEntitiesAsChunks(targetEntities);

        if (!isMultiAssignmentsEnabled()) {
            closeOrCancelActiveActions(assignmentStrategy, targetEntitiesIdsChunks);
        }
        // cancel all scheduled actions which are in-active, these actions were
        // not active before and the manual assignment which has been done
        // cancels them
        targetEntitiesIdsChunks.forEach(this::cancelInactiveScheduledActionsForTargets);
        setAssignedDistributionSetAndTargetUpdateStatus(assignmentStrategy, distributionSetEntity,
                targetEntitiesIdsChunks);
        final Map<TargetWithUserAcceptanceRequired, JpaAction> assignedActions = createActions(initiatedBy, targetsWithUserAcceptanceRequired,
                targetEntities, assignmentStrategy, distributionSetEntity);
        // create initial action status when action is created so we remember
        // the initial running status because we will change the status
        // of the action itself and with this action status we have a nicer
        // action history.
        createActionsStatus(assignedActions, assignmentStrategy, actionMessage);

        detachEntitiesAndSendTargetUpdatedEvents(distributionSetEntity, targetEntities, assignmentStrategy);
        return new ArrayList<>(assignedActions.values());
    }

    private void checkQuotaForAssignment(final Collection<DeploymentRequest> deploymentRequests) {
        if (!deploymentRequests.isEmpty()) {
            enforceMaxAssignmentsPerRequest(deploymentRequests.size());
            enforceMaxActionsPerTarget(deploymentRequests);
        }
    }

    private void enforceMaxAssignmentsPerRequest(final int requestedActions) {
        QuotaHelper.assertAssignmentRequestSizeQuota(requestedActions,
                quotaManagement.getMaxTargetDistributionSetAssignmentsPerManualAssignment());
    }

    private void enforceMaxActionsPerTarget(final Collection<DeploymentRequest> deploymentRequests) {
        final int quota = quotaManagement.getMaxActionsPerTarget();

        final Map<String, Long> countOfTargtInRequest = deploymentRequests.stream()
                .map(DeploymentRequest::getControllerId)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));


        countOfTargtInRequest.forEach((controllerId, count) -> QuotaHelper.assertAssignmentQuota(controllerId, count,
                quota, Action.class, Target.class, id -> actionRepository.countByTarget_ControllerIdAndActive(id, false)));
    }

    private void closeOrCancelActiveActions(final AbstractDsAssignmentStrategy assignmentStrategy,
                                            final List<List<Long>> targetIdsChunks) {
        if (isActionsAutocloseEnabled()) {
            assignmentStrategy.closeActiveActions(targetIdsChunks);
        } else {
            assignmentStrategy.cancelActiveActions(targetIdsChunks);
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void cancelInactiveScheduledActionsForTargets(final List<Long> targetIds) {
        if (!isMultiAssignmentsEnabled()) {
            actionRepository.switchStatus(DeviceActionStatus.CANCELED, targetIds, false, DeviceActionStatus.USER_SCHEDULED);
        } else {
            LOG.debug("The Multi Assignments feature is enabled: No need to cancel inactive scheduled actions.");
        }
    }

    private void setAssignedDistributionSetAndTargetUpdateStatus(final AbstractDsAssignmentStrategy assignmentStrategy,
                                                                 final JpaDistributionSet set, final List<List<Long>> targetIdsChunks) {
        final String currentUser = auditorProvider.getCurrentAuditor().orElse(null);
        assignmentStrategy.setAssignedDistributionSetAndTargetStatus(set, targetIdsChunks, currentUser);
    }

    private Map<TargetWithUserAcceptanceRequired, JpaAction> createActions(final String initiatedBy,
                                                                           final Collection<TargetWithUserAcceptanceRequired> targetsWithUserAcceptanceRequired, final List<JpaTarget> targets,
                                                                           final AbstractDsAssignmentStrategy assignmentStrategy, final JpaDistributionSet set) {

        final Map<TargetWithUserAcceptanceRequired, JpaAction> persistedActions = new LinkedHashMap<>();

        for (final TargetWithUserAcceptanceRequired twt : targetsWithUserAcceptanceRequired) {
            final JpaAction targetAction = assignmentStrategy.createTargetAction(initiatedBy, twt, targets, set);
            if (targetAction != null) {
                persistedActions.put(twt, actionRepository.save(targetAction));
            }
        }
        return persistedActions;
    }

    private void createActionsStatus(final Map<TargetWithUserAcceptanceRequired, JpaAction> actions,
                                     final AbstractDsAssignmentStrategy assignmentStrategy, final String actionMessage) {
        actionStatusRepository.saveAll(actions.entrySet().stream().map(entry -> {
            final JpaAction action = entry.getValue();
            final JpaActionStatus actionStatus = assignmentStrategy.createActionStatus(action, actionMessage);
            return actionStatus;
        }).collect(Collectors.toList()));
    }

    private void setInitialActionStatusOfRolloutGroup(final List<JpaAction> actions) {
        final List<JpaActionStatus> statusList = new ArrayList<>();
        for (final JpaAction action : actions) {
            final JpaActionStatus actionStatus = onlineDsAssignmentStrategy.createActionStatus(action, null);
            statusList.add(actionStatus);
        }
        actionStatusRepository.saveAll(statusList);
    }

    private void detachEntitiesAndSendTargetUpdatedEvents(final JpaDistributionSet set, final List<JpaTarget> targets,
                                                          final AbstractDsAssignmentStrategy assignmentStrategy) {
        // detaching as it is not necessary to persist the set itself
        entityManager.detach(set);
        // detaching as the entity has been updated by the JPQL query above
        targets.forEach(entityManager::detach);
        assignmentStrategy.sendTargetUpdatedEvents(set, targets);
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action cancelAction(final long actionId) {
        LOG.debug("cancelAction({})", actionId);

        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (action.isCancelingOrCanceled()) {
            throw new CancelActionNotAllowedException("Actions in canceling or canceled state cannot be canceled");
        }

        if (action.isActive()) {
            LOG.debug("action ({}) was still active. Change to {}.", action, DeviceActionStatus.CANCELING);
            action.setStatus(DeviceActionStatus.CANCELING);

            // document that the status has been retrieved
            actionStatusRepository.save(new JpaActionStatus(action, DeviceActionStatus.CANCELING, Instant.now().getEpochSecond(),
                    RepositoryConstants.SERVER_MESSAGE_PREFIX + "manual cancelation requested"));
            final Action saveAction = actionRepository.save(action);

            onlineDsAssignmentStrategy.cancelAssignment(action);

            return saveAction;
        } else {
            throw new CancelActionNotAllowedException(action.getId() + " is not active and cannot be canceled");
        }
    }

    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceQuitAction(final long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.isCancelingOrCanceled()) {
            throw new ForceQuitActionNotAllowedException(
                    action.getId() + " is not canceled yet and cannot be force quit");
        }

        if (!action.isActive()) {
            throw new ForceQuitActionNotAllowedException(action.getId() + " is not active and cannot be force quit");
        }

        LOG.warn("action ({}) was still active and has been force quite.", action);

        // document that the status has been retrieved
        actionStatusRepository.save(new JpaActionStatus(action, DeviceActionStatus.CANCELED, Instant.now().getEpochSecond(),
                RepositoryConstants.SERVER_MESSAGE_PREFIX + "A force quit has been performed."));

        DeploymentHelper.successCancellation(action, actionRepository, targetRepository);

        return actionRepository.save(action);
    }

    @Override
    public long startScheduledActionsByRolloutGroupParent(final long rolloutId, final long distributionSetId,
                                                          final Long rolloutGroupParentId) {
        long totalActionsCount = 0L;
        long lastStartedActionsCount;
        do {
            lastStartedActionsCount = startScheduledActionsByRolloutGroupParentInNewTransaction(rolloutId,
                    distributionSetId, rolloutGroupParentId, ACTION_PAGE_LIMIT);
            totalActionsCount += lastStartedActionsCount;
        } while (lastStartedActionsCount > 0);

        return totalActionsCount;
    }

    private long startScheduledActionsByRolloutGroupParentInNewTransaction(final Long rolloutId,
                                                                           final Long distributionSetId, final Long rolloutGroupParentId, final int limit) {
        return DeploymentHelper.runInNewTransaction(txManager, "startScheduledActions-" + rolloutId, status -> {
            final Page<Action> rolloutGroupActions = findActionsByRolloutAndRolloutGroupParent(rolloutId,
                    rolloutGroupParentId, limit);

            if (rolloutGroupActions.getContent().isEmpty()) {
                return 0L;
            }

            final List<Action> newTargetAssignments = handleTargetAssignments(rolloutGroupActions);

            if (!newTargetAssignments.isEmpty()) {
                onlineDsAssignmentStrategy.sendDeploymentEvents(distributionSetId, newTargetAssignments);
            }

            return rolloutGroupActions.getTotalElements();
        });
    }

    private List<Action> handleTargetAssignments(final Page<Action> rolloutGroupActions) {
        // Close actions already assigned and collect pending assignments
        final List<JpaAction> pendingTargetAssignments = rolloutGroupActions.getContent().stream()
                .map(JpaAction.class::cast).map(this::closeActionIfSetWasAlreadyAssigned).filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (pendingTargetAssignments.isEmpty()) {
            return new ArrayList<>(pendingTargetAssignments);
        }
        // check if old actions needs to be canceled first
        return startScheduledActionsAndHandleOpenCancellationFirst(pendingTargetAssignments);
    }

    private Page<Action> findActionsByRolloutAndRolloutGroupParent(final Long rolloutId,
                                                                   final Long rolloutGroupParentId, final int limit) {

        final PageRequest pageRequest = PageRequest.of(0, limit);
        if (rolloutGroupParentId == null) {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIsNullAndStatusAndActive(pageRequest, rolloutId,
                    DeviceActionStatus.USER_SCHEDULED, true);
        } else {
            return actionRepository.findByRolloutIdAndRolloutGroupParentIdAndStatusAndActive(pageRequest, rolloutId,
                    rolloutGroupParentId, DeviceActionStatus.USER_SCHEDULED, true);
        }
    }

    private JpaAction closeActionIfSetWasAlreadyAssigned(final JpaAction action) {

        if (isMultiAssignmentsEnabled()) {
            return action;
        }

        final JpaTarget target = (JpaTarget) action.getTarget();
        if (target.getAssignedDistributionSet() != null
                && action.getDistributionSet().getId().equals(target.getAssignedDistributionSet().getId())) {
            // the target has already the distribution set assigned, we don't
            // need to start the scheduled action, just finish it.
            LOG.debug("Target {} has distribution set {} assigned. Closing action...", target.getControllerId(),
                    action.getDistributionSet().getName());
            action.setStatus(DeviceActionStatus.FINISHED_NOT_EXECUTED);
            action.setActive(false);
            setSkipActionStatus(action);
            actionRepository.save(action);
            return null;
        }

        return action;
    }

    private List<Action> startScheduledActionsAndHandleOpenCancellationFirst(final List<JpaAction> actions) {
        if (!isMultiAssignmentsEnabled()) {
            closeOrCancelOpenDeviceActions(actions);
        }
        final List<JpaAction> savedActions = activateActionsOfRolloutGroup(actions);
        setInitialActionStatusOfRolloutGroup(savedActions);
        setAssignmentOnTargets(savedActions);
        return Collections.unmodifiableList(savedActions);
    }

    private void closeOrCancelOpenDeviceActions(final List<JpaAction> actions) {
        final List<Long> targetIds = actions.stream().map(JpaAction::getTarget).map(Target::getId)
                .collect(Collectors.toList());
        if (isActionsAutocloseEnabled()) {
            onlineDsAssignmentStrategy.closeObsoleteUpdateActions(targetIds);
        } else {
            onlineDsAssignmentStrategy.overrideObsoleteUpdateActions(targetIds);
        }
    }

    private List<JpaAction> activateActionsOfRolloutGroup(final List<JpaAction> actions) {
        List<JpaAction> startedActions = new ArrayList<>();
        actions.forEach(action -> {
            action.setActive(true);
            final boolean confirmationRequired = action.getRolloutGroup().isConfirmationRequired()
                    && action.getTarget().getAutoConfirmationStatus() == null;
            if (isConfirmationFlowEnabled() && confirmationRequired) {
                return;
            }
            action.setStatus(DeviceActionStatus.RUNNING);
            startedActions.add(action);
        });
        List<JpaAction> savedActions = actionRepository.saveAll(actions);
        sendActionRunningStatus(startedActions);
        return savedActions;
    }

    private void sendActionRunningStatus(List<JpaAction> startedActions) {
        startedActions.forEach(action -> kafkaMessageService.publishVehicleStatus(action.getRollout().getName(),
                action.getTarget().getControllerId(), OTA_RUNNING_STATUS));
    }

    private void setAssignmentOnTargets(final List<JpaAction> actions) {
        final List<JpaTarget> assignedDsTargets = actions.stream().map(savedAction -> {
            final JpaTarget mergedTarget = (JpaTarget) entityManager.merge(savedAction.getTarget());
            mergedTarget.setAssignedDistributionSet(savedAction.getDistributionSet());
            mergedTarget.setUpdateStatus(TargetUpdateStatus.PENDING);
            return mergedTarget;
        }).collect(Collectors.toList());

        targetRepository.saveAll(assignedDsTargets);
    }

    private void setSkipActionStatus(final JpaAction action) {
        final JpaActionStatus actionStatus = new JpaActionStatus();
        actionStatus.setAction(action);
        actionStatus.setOccurredAt(action.getCreatedAt());
        actionStatus.setStatus(DeviceActionStatus.RUNNING);
        actionStatus.addMessage(RepositoryConstants.SERVER_MESSAGE_PREFIX
                + "Distribution Set is already assigned. Skipping this action.");
        actionStatusRepository.save(actionStatus);
    }

    @Override
    public Optional<Action> findAction(final long actionId) {
        return actionRepository.getActionById(actionId, true);
    }

    @Override
    public Optional<Action> findActionWithDetails(final long actionId) {
        return actionRepository.getActionById(actionId, true);
    }

    @Override
    public Slice<Action> findActionsByTarget(final String controllerId, final Pageable pageable) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return actionRepository.findByTargetControllerIdAndActive(pageable, controllerId, true);
    }

    @Override
    public Page<Action> findActionsByTarget(final String rsqlParam, final String controllerId,
                                            final Pageable pageable) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        final List<Specification<JpaAction>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database),
                byControllerIdSpec(controllerId));

        return JpaManagementHelper.findAllWithCountBySpec(actionRepository, pageable, specList);
    }

    private Specification<JpaAction> byControllerIdSpec(final String controllerId) {
        return (root, query, cb) -> cb.equal(root.get(JpaAction_.target).get(JpaTarget_.controllerId), controllerId);
    }

    @Override
    public Page<Action> findActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return actionRepository.findByActiveAndTarget(pageable, controllerId, true);
    }

    @Override
    public Page<Action> findInActiveActionsByTarget(final Pageable pageable, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);

        return actionRepository.findByActiveAndTarget(pageable, controllerId, false);
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
    public long countActionsByTarget(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return actionRepository.countByTarget_ControllerIdAndActive(controllerId, true);
    }

    @Override
    public long countActionsByTarget(final String rsqlParam, final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        final List<Specification<JpaAction>> specList = Arrays.asList(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database),
                byControllerIdSpec(controllerId));

        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    public Optional<Action> findByTargetIdAndActionIdAndActive(@Param("controllerId") String controllerId, @Param("actionId") Long actionId, boolean active) {
        return actionRepository.findFirstByTargetIdAndActionId(controllerId, actionId, active);
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

    private void throwExceptionIfDistributionSetDoesNotExist(final Long dsId) {
        if (!distributionSetRepository.existsById(dsId)) {
            throw new EntityNotFoundException(DistributionSet.class, dsId);
        }
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Action forceTargetAction(final long actionId) {
        final JpaAction action = actionRepository.findById(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));

        if (!action.getUserAcceptanceRequired().equals(MgmtRolloutUserAcceptanceRequired.NO)) {
            action.setUserAcceptanceRequired(MgmtRolloutUserAcceptanceRequired.NO);
            return actionRepository.save(action);
        }
        return action;
    }

    @Override
    public Page<ActionStatus> findActionStatusByAction(final Pageable pageReq, final long actionId) {
        verifyActionExists(actionId);

        return actionStatusRepository.findByActionId(pageReq, actionId);
    }

    /**
     * Maps each {@link JpaActionStatus} to a {@link DeviceActionStatusTimestampResponse},
     * including only messages that do not start with "Device reported status code".
     *
     * @param actionId     the ID of the action
     * @param controllerId the controller ID of the target device
     * @return a list of {@link DeviceActionStatusTimestampResponse} containing status and occurred-at timestamp pairs
     * @throws EntityNotFoundException if the action or controller does not exist
     */
    @Override
    public List<DeviceActionStatusTimestampResponse> getActionStatusTimestamps(final Long actionId, final String controllerId) {
        verifyActionExists(actionId);

        if (!isActionLinkedToController(actionId, controllerId)) {
            LOG.warn("Action with ID: {} does not belong to target with controller ID: {}", actionId, controllerId);
            throw new ValidationException("Action does not belong to the specified target.");
        }

        List<JpaActionStatus> statuses = actionStatusRepository.findStatusesByActionIdAndControllerId(actionId, controllerId);

        return statuses.stream()
                .map(status -> new DeviceActionStatusTimestampResponse(
                        status.getStatus(),
                        status.getOccurredAt(),
                        parseDownloadIfApplicable(status),
                        Optional.ofNullable(status.getMessages())
                                .orElse(Collections.emptyList())
                                .stream()
                                .filter(msg -> !msg.startsWith("Device reported status code"))
                                .toList()

                ))
                .toList();
    }

    /**
     * Checks if the specified action is linked to the given controller.
     *
     * @param actionId     the ID of the action to check
     * @param controllerId the controller ID to validate against the action's target
     * @return true if the action belongs to the specified controller, false otherwise
     */
    private boolean isActionLinkedToController(final Long actionId, final String controllerId) {
        return actionRepository.getActionById(actionId, true)
                .map(Action::getTarget)
                .map(Target::getControllerId)
                .filter(controllerId::equals)
                .isPresent();
    }

    private void verifyActionExists(final long actionId) {
        if (!actionRepository.existsById(actionId)) {
            throw new EntityNotFoundException(Action.class, actionId);
        }
    }

    @Override
    public long countActionStatusByAction(final long actionId) {
        verifyActionExists(actionId);

        return actionStatusRepository.countByActionId(actionId);
    }

    @Override
    public Page<String> findMessagesByActionStatusId(final Pageable pageable, final long actionStatusId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<String> msgQuery = cb.createQuery(String.class);
        final Root<JpaActionStatus> as = msgQuery.from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> join = as.joinList(MESSAGES, JoinType.LEFT);
        final CriteriaQuery<String> selMsgQuery = msgQuery.select(join);
        selMsgQuery.where(cb.equal(as.get(JpaActionStatus_.id), actionStatusId));

        final List<String> result = new ArrayList<>(entityManager.createQuery(selMsgQuery)
                .setFirstResult((int) pageable.getOffset()).setMaxResults(pageable.getPageSize()).getResultList());

        return new PageImpl<>(result, pageable, result.size());
    }

    @Override
    public Page<String> findMessagesWithErrorCodeByActionStatusId(final Pageable pageable, final long actionStatusId) {

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<String> query = cb.createQuery(String.class);
        Root<JpaActionStatus> root = query.from(JpaActionStatus.class);

        ListJoin<JpaActionStatus, String> messagesJoin = root.joinList(MESSAGES, JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get(JpaActionStatus_.id), actionStatusId));

        Expression<String> errorCodeExpression = cb.function("CONCAT", String.class,
                cb.function("COALESCE", String.class,
                        cb.concat(root.get(JpaActionStatus_.errorCode).as(String.class), cb.literal(": ")),
                        cb.literal("")),
                messagesJoin);

        query.select(errorCodeExpression);

        query.where(predicates.toArray(new Predicate[0]));

        List<String> result = entityManager.createQuery(query).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize()).getResultList();
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        countQuery.select(cb.count(countQuery.from(JpaActionStatus.class)));
        Long totalCount = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(result, pageable, totalCount);
    }

    @Override
    public long countMessagesByActionStatusId(final long actionStatusId) {
        final CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        final CriteriaQuery<Long> countMsgQuery = cb.createQuery(Long.class);
        final Root<JpaActionStatus> countMsgQueryFrom = countMsgQuery.distinct(true).from(JpaActionStatus.class);
        final ListJoin<JpaActionStatus, String> cJoin = countMsgQueryFrom.joinList(MESSAGES, JoinType.LEFT);
        countMsgQuery.select(cb.count(cJoin))
                .where(cb.equal(countMsgQueryFrom.get(JpaActionStatus_.id), actionStatusId));

        return entityManager.createQuery(countMsgQuery).getSingleResult();
    }

    @Override
    public Page<ActionStatus> findActionStatusAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithCountBySpec(actionStatusRepository, pageable, null);
    }

    @Override
    public long countActionStatusAll() {
        return actionStatusRepository.count();
    }

    @Override
    public long countActionsAll() {
        return actionRepository.count();
    }

    @Override
    public long countActions(final String rsqlParam) {
        final List<Specification<JpaAction>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database));
        return JpaManagementHelper.countBySpec(actionRepository, specList);
    }

    @Override
    public long countActionsByDistributionSetIdAndActiveIsTrue(final Long distributionSet) {
        return actionRepository.countByDistributionSetIdAndActiveIsTrue(distributionSet);
    }

    @Override
    public long countActionsByDistributionSetIdAndActiveIsTrueAndStatusIsNot(final Long distributionSet,
                                                                             final DeviceActionStatus status) {
        return actionRepository.countByDistributionSetIdAndActiveIsTrueAndStatusIsNot(distributionSet, status);
    }

    @Override
    public Slice<Action> findActionsByDistributionSet(final Pageable pageable, final long dsId) {
        throwExceptionIfDistributionSetDoesNotExist(dsId);
        return actionRepository.findByDistributionSetId(pageable, dsId);
    }

    @Override
    public Slice<Action> findActionsAll(final Pageable pageable) {
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, pageable, null);
    }

    @Override
    public Slice<Action> findActions(final String rsqlParam, final Pageable pageable) {
        final List<Specification<JpaAction>> specList = List.of(
                RSQLUtility.buildRsqlSpecification(rsqlParam, ActionFields.class, virtualPropertyReplacer, database));
        return JpaManagementHelper.findAllWithoutCountBySpec(actionRepository, pageable, specList);
    }

    @Override
    public Optional<DistributionSet> getAssignedDistributionSet(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return distributionSetRepository.findAssignedToTarget(controllerId);
    }

    @Override
    public Optional<DistributionSet> getInstalledDistributionSet(final String controllerId) {
        throwExceptionIfTargetDoesNotExist(controllerId);
        return distributionSetRepository.findInstalledAtTarget(controllerId);
    }

    @Override
    @Transactional(readOnly = false)
    public int deleteActionsByStatusAndLastModifiedBefore(final Set<DeviceActionStatus> status, final long lastModified) {
        if (status.isEmpty()) {
            return 0;
        }
        /*
         * We use a native query here because Spring JPA does not support to
         * specify a LIMIT clause on a DELETE statement. However, for this
         * specific use case (action cleanup), we must specify a row limit to
         * reduce the overall load on the database.
         */

        final int statusCount = status.size();
        final DeviceActionStatus[] statusArr = status.toArray(new DeviceActionStatus[statusCount]);

        final String queryStr = String.format(getQueryForDeleteActionsByStatusAndLastModifiedBeforeString(database),
                formatInClauseWithNumberKeys(statusCount));
        final Query deleteQuery = entityManager.createNativeQuery(queryStr);

        IntStream.range(0, statusCount)
                .forEach(i -> deleteQuery.setParameter(String.valueOf(i), statusArr[i].ordinal()));
        deleteQuery.setParameter("tenant", tenantAware.getCurrentTenant().toUpperCase());
        deleteQuery.setParameter("last_modified_at", lastModified);

        LOG.debug("Action cleanup: Executing the following (native) query: {}", deleteQuery);
        return deleteQuery.executeUpdate();
    }

    @Override
    public boolean hasPendingCancellations(final String controllerId) {
        return actionRepository.existsByTargetControllerIdAndStatusAndActiveIsTrue(controllerId,
                DeviceActionStatus.CANCELING);
    }

    protected ActionRepository getActionRepository() {
        return actionRepository;
    }

    protected boolean isActionsAutocloseEnabled() {
        return getConfigValue(REPOSITORY_ACTIONS_AUTOCLOSE_ENABLED, Boolean.class);
    }

    private boolean isMultiAssignmentsEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .isMultiAssignmentsEnabled();
    }

    private boolean isConfirmationFlowEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                .isConfirmationFlowEnabled();
    }

    private <T extends Serializable> T getConfigValue(final String key, final Class<T> valueType) {
        return systemSecurityContext
                .runAsSystem(() -> tenantConfigurationManagement.getConfigurationValue(key, valueType).getValue());
    }

    @Override
    @Transactional
    public void cancelActionsForDistributionSet(final CancelationType cancelationType, final DistributionSet set) {
        actionRepository.findByDistributionSetAndActiveIsTrueAndStatusIsNot(set, DeviceActionStatus.CANCELING).forEach(action -> {
            final JpaAction jpaAction = (JpaAction) action;
            cancelAction(jpaAction.getId());
            LOG.debug("Action {} canceled", jpaAction.getId());
        });
        if (cancelationType == CancelationType.FORCE) {
            actionRepository.findByDistributionSetAndActiveIsTrue(set).forEach(action -> {
                final JpaAction jpaAction = (JpaAction) action;
                forceQuitAction(jpaAction.getId());
                LOG.debug("Action {} force canceled", jpaAction.getId());
            });
        }
    }


    @Override
    public long createActionsForRolloutGroup(final Rollout rollout, final RolloutGroup group) {
        LOG.debug("Starting to create actions for rollout group: {} in rollout: {}", group.getId(), rollout.getId());
        long totalActionsCreated = 0;
        try {
            long actionsCreated;
            do {
                actionsCreated = createActionsForTargetsInNewTransaction(rollout.getId(), group.getId(), TRANSACTION_TARGETS);
                totalActionsCreated += actionsCreated;
                LOG.debug("Created {} actions for rollout group: {} in rollout: {}", actionsCreated, group.getId(), rollout.getId());
            } while (actionsCreated > 0);

        } catch (final TransactionException e) {
            LOG.warn("Transaction assigning Targets to RolloutGroup failed for rollout group: {} in rollout: {}", group.getId(), rollout.getId(), e);
            return 0;
        }
        LOG.debug("Finished creating actions for rollout group: {} in rollout: {}. Total actions created: {}", group.getId(), rollout.getId(), totalActionsCreated);
        return totalActionsCreated;
    }

    /**
     * Creates actions for targets in a new transaction for a given rollout and group.
     *
     * @param rolloutId the ID of the rollout
     * @param groupId   the ID of the rollout group
     * @param limit     the maximum number of targets to process in one transaction
     * @return the number of targets processed in this transaction
     * @throws EntityNotFoundException if the rollout or group is not found
     */
    private Long createActionsForTargetsInNewTransaction(final long rolloutId, final long groupId, final int limit) {
        LOG.debug("Starting new transaction to create actions for targets in rollout: {}, group: {}, limit: {}", rolloutId, groupId, limit);
        return DeploymentHelper.runInNewTransaction(txManager, "createActionsForTargets", status -> {
            final PageRequest pageRequest = PageRequest.of(0, limit);
            final Rollout rollout = rolloutRepository.findById(rolloutId)
                    .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
            final RolloutGroup group = rolloutGroupManagement.get(groupId)
                    .orElseThrow(() -> new EntityNotFoundException(RolloutGroup.class, groupId));

            LOG.debug("Retrieved rollout: {} and group: {}", rollout, group);

            final DistributionSet distributionSet = rollout.getDistributionSet();
            final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired = rollout.getUserAcceptanceRequired();
            final long forceTime = rollout.getForcedTime();

            LOG.debug("Distribution set: {}, User acceptance required: {}, Force time: {}", distributionSet, userAcceptanceRequired, forceTime);

            final Slice<Target> targets = targetManagement.findByInRolloutGroupWithoutAction(pageRequest, groupId);
            LOG.debug("Found {} targets in group: {}", targets.getNumberOfElements(), groupId);

            if (targets.getNumberOfElements() > 0) {
                createRunningActions(targets.getContent(), distributionSet, userAcceptanceRequired, forceTime, rollout, group);
                LOG.debug("Created running actions for targets in group: {}", groupId);
            }

            LOG.debug("Finished creating actions for targets in group: {}. Total targets processed: {}", groupId, targets.getNumberOfElements());
            return Long.valueOf(targets.getNumberOfElements());
        });
    }

    /**
     * Creates an action entry into the action repository.  A scheduled action
     * is created in running state.
     */
    private void createRunningActions(final Collection<Target> targets, final DistributionSet distributionSet,
                                      final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired, final Long forcedTime, final Rollout rollout,
                                      final RolloutGroup rolloutGroup) {
        LOG.debug("Starting to create running actions for rollout group: {} in rollout: {}", rolloutGroup.getId(), rollout.getId());
        targets.forEach(target -> {
            LOG.debug("Creating action for target: {}", target.getId());

            assertActionsPerTargetQuota(target, 1);

            final JpaAction action = new JpaAction();
            action.setTarget(target);
            action.setActive(true);
            action.setDistributionSet(distributionSet);
            action.setUserAcceptanceRequired(userAcceptanceRequired);
            action.setForcedTime(forcedTime);
            action.setStatus(DeviceActionStatus.RUNNING);
            action.setRollout(rollout);
            action.setRolloutGroup(rolloutGroup);
            action.setInitiatedBy(rollout.getCreatedBy());
            rollout.getWeight().ifPresent(action::setWeight);
            actionRepository.save(action);

            LOG.debug("Created action for target: {} with action ID: {}", target.getId(), action.getId());
        });
        LOG.debug("Finished creating running actions for rollout group: {} in rollout: {}", rolloutGroup.getId(), rollout.getId());
    }


    /**
     * Enforces the quota defining the maximum number of {@link Action}s per
     * {@link Target}.
     *
     * @param target    The target
     * @param requested number of actions to check
     */
    private void assertActionsPerTargetQuota(final Target target, final int requested) {
        final int quota = quotaManagement.getMaxActionsPerTarget();
        QuotaHelper.assertAssignmentQuota(target.getControllerId(), requested, quota, Action.class, Target.class,
                id -> actionRepository.countByTarget_ControllerIdAndActive(id, true));
    }

    /**
     * Parse the download to readable json
     *
     * @param status Action status details
     * @return DdiDownload download details
     */
    private DdiDownload parseDownloadIfApplicable(JpaActionStatus status) {
        if (status.getStatus() == DeviceActionStatus.DOWNLOAD_IN_PROGRESS) {
            String json = status.getDownloadProgress();
            if (json != null) {
                try {
                    return objectMapper.readValue(json, DdiDownload.class);
                } catch (Exception e) {
                    LOG.warn("Failed to parse downloadProgress JSON: {}", json, e);
                }
            }
        }
        return null;
    }
}
