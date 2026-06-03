/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import com.google.common.collect.Lists;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.validation.Valid;
import jakarta.validation.ValidationException;
import jakarta.validation.constraints.NotNull;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.kafka.RolloutStatusPayload;
import org.cosmos.models.mgmt.MgmtSoftwareModuleRequest;
import org.cosmos.models.mgmt.action.dto.DeviceActionStatusTimestampResponse;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.distributionset.dto.MgmtDistributionSetRequestBodyPost;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutApprovalDecision;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtCloneRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryFullRolloutRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRetryIndividualDeviceRequestBody;
import org.cosmos.models.mgmt.rollout.dto.RetryMultipleDevicesRequest;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroup;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleAssignments;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.DistributionSetTypeManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.RolloutStatusCache;
import org.eclipse.hawkbit.repository.SoftwareModuleManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetFilterQueryManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.DistributionSetCreate;
import org.eclipse.hawkbit.repository.builder.GenericRolloutUpdate;
import org.eclipse.hawkbit.repository.builder.RolloutUpdate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.EntityReadOnlyException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.configuration.Constants;
import org.eclipse.hawkbit.repository.jpa.model.AbstractJpaBaseEntity_;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSet;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout_;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetFilterQuery;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.jpa.model.helper.JpaRolloutMetaData;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.StartNextGroupRolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.jpa.rsql.RSQLUtility;
import org.eclipse.hawkbit.repository.jpa.service.CdnFileUploadService;
import org.eclipse.hawkbit.repository.jpa.service.HandleRolloutSchedulerService;
import org.eclipse.hawkbit.repository.jpa.service.RolloutAsyncService;
import org.eclipse.hawkbit.repository.jpa.specifications.RolloutSpecification;
import org.eclipse.hawkbit.repository.jpa.specifications.TargetSpecifications;
import org.eclipse.hawkbit.repository.jpa.utils.EpochTimeValidator;
import org.eclipse.hawkbit.repository.jpa.utils.RolloutMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.utils.RolloutSchedulerUtils;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.eclipse.hawkbit.repository.model.RolloutGroupsValidation;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TotalTargetCountActionStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.configuration.TenantConfigurationProperties;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.server.ResponseStatusException;

import static java.time.ZoneOffset.UTC;
import static org.cosmos.models.mgmt.MgmtRestConstants.EMPTY_JSON;
import static org.eclipse.hawkbit.repository.RolloutHelper.parseGroupsJson;


/**
 * JPA implementation of {@link RolloutManagement}.
 */
@Transactional(readOnly = true)
@Slf4j
public class JpaRolloutManagement implements RolloutManagement {


    public static final Set<RolloutStatus> ALLOWED_ROLLOUT_STATUSES_FOR_LINKING_AND_UNLINKING = Set.of(RolloutStatus.DRAFT, RolloutStatus.DELETING, RolloutStatus.DELETED);
    public static final Set<RolloutStatus> NONACTIVE_ROLLOUTS = Set.of(RolloutStatus.FINISHED, RolloutStatus.CANCELED);
    public static final String DEFAULT_VERSION = "1.0";
    private static final List<RolloutStatus> ACTIVE_ROLLOUTS = Arrays.asList(RolloutStatus.READY, RolloutStatus.STARTING,
            RolloutStatus.RETRYING, RolloutStatus.FREEZING, RolloutStatus.UNFREEZING, RolloutStatus.RUNNING,
            RolloutStatus.PAUSING, RolloutStatus.PAUSED, RolloutStatus.RESUMING, RolloutStatus.CANCELING, RolloutStatus.FINISHING, RolloutStatus.DELETING, RolloutStatus.RETRY);
    private static final List<RolloutStatus> ACTIVE_ROLLOUTS_END = List.of(RolloutStatus.STARTING, RolloutStatus.DRAFT, RolloutStatus.RUNNING);
    private static final List<DeviceActionStatus> ACTIVE_ACTION_STATUS_FOR_EXPIRED_ROLLOUT = List.of(DeviceActionStatus.RUNNING, DeviceActionStatus.PAUSED, DeviceActionStatus.PAUSING,
            DeviceActionStatus.RESUMING, DeviceActionStatus.RETRYING);
    private static final List<RolloutGroupStatus> ACTIVE_ROLLOUT_GROUP_STATUSES_FOR_EXPIRED_ROLLOUT = List.of(RolloutGroupStatus.DRAFT, RolloutGroupStatus.CREATING, RolloutGroupStatus.UNFREEZING,
            RolloutGroupStatus.READY, RolloutGroupStatus.FREEZING, RolloutGroupStatus.QUEUED,
            RolloutGroupStatus.RUNNING, RolloutGroupStatus.PAUSED, RolloutGroupStatus.QUEUING,
            RolloutGroupStatus.PAUSING, RolloutGroupStatus.STARTING, RolloutGroupStatus.RESUMING);
    private static final List<RolloutStatus> ACTIVE_ROLLOUT_STATUS_FOR_EXPIRED_ROLLOUT = List.of(RolloutStatus.STARTING, RolloutStatus.RUNNING, RolloutStatus.PAUSING, RolloutStatus.PAUSED, RolloutStatus.RESUMING, RolloutStatus.RETRYING);
    private static final String NATIVE_QUERY_FOR_ACTIVE_ROLLOUT_END_AT_NOW = "SELECT id, tenant FROM sp_rollout sp " +
            " WHERE sp.status IN (%s) and sp.end_at IS NOT NULL and sp.end_at < #" + JpaRollout_.END_AT;
    private static final List<RolloutStatus> ROLLOUT_STATUS_STOPPABLE = Arrays.asList(RolloutStatus.RUNNING, RolloutStatus.PAUSED, RolloutStatus.DRAFT, RolloutStatus.STARTING);
    private static final String SECONDS = " seconds.";
    private static final List<RolloutStatus> STATUSES_TO_BE_CANCELING = List.of(RolloutStatus.RUNNING, RolloutStatus.PAUSED);
    private static final List<DeviceActionStatus> CHECK_DEVICE_STATUSES_TO_PAUSE_DEVICE = List.of(
            DeviceActionStatus.RUNNING,
            DeviceActionStatus.PAUSED,
            DeviceActionStatus.CANCELED,
            DeviceActionStatus.FINISHED_SUCCESS,
            DeviceActionStatus.FINISHED_FAILURE,
            DeviceActionStatus.DD_SENT
    );
    private static final List<DeviceActionStatus> CHECK_DEVICE_STATUSES_TO_RESUME_DEVICE = List.of(
            DeviceActionStatus.PAUSED,
            DeviceActionStatus.RUNNING,
            DeviceActionStatus.CANCELED,
            DeviceActionStatus.FINISHED_SUCCESS,
            DeviceActionStatus.FINISHED_FAILURE,
            DeviceActionStatus.DD_SENT
    );
    private static final List<RolloutStatus> VALID_RETRY_STATUSES = List.of(
            RolloutStatus.RUNNING,
            RolloutStatus.FINISHED,
            RolloutStatus.CANCELED
    );
    private static final List<DeviceActionStatus> VALID_RETRY_DEVICE_STATUSES = List.of(
            DeviceActionStatus.FINISHED_SUCCESS,
            DeviceActionStatus.FINISHED_FAILURE,
            DeviceActionStatus.FINISHED_NOT_EXECUTED,
            DeviceActionStatus.CANCELED
    );
    private static final List<RolloutStatus> VALID_RETRY_ROLLOUT_UPDATE_STATUSES = List.of(
            RolloutStatus.FINISHED,
            RolloutStatus.CANCELED
    );
    private static final List<RolloutStatus> VALID_FULL_RETRY_ROLLOUT_STATUS = List.of(RolloutStatus.FINISHED, RolloutStatus.CANCELED);
    private final TargetManagement targetManagement;
    private final DistributionSetManagement distributionSetManagement;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final DistributionSetTagRepository distributionSetTagRepository;
    private final TargetRepository targetRepository;
    private final TargetTargetTagRepository targetTargetTagRepository;
    private final SupportPackageManagement supportPackageManagement;
    private final TargetTagManagement targetTagManagement;
    private final TargetFilterQueryManagement targetFilterQueryManagement;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final SoftwareModuleManagement softwareModuleManagement;
    private final ArtifactsManagement artifactsManagement;
    private final EntityFactory entityFactory;
    private final VersionManagement versionManagement;
    private final SystemManagement systemManagement;
    private final DistributionSetTypeManagement distributionSetTypeManagement;
    private final Database database;
    private final KafkaMessageService kafkaMessageService;
    private final DeploymentManagement deploymentManagement;

    @Autowired
    private CdnFileUploadService<BaseSupportPackage> supportPackageCdnFileUploadService;

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private RolloutRepository rolloutRepository;
    @Autowired
    private RolloutMetaDataRepository rolloutMetaDataRepository;
    @Autowired
    private RolloutGroupRepository rolloutGroupRepository;
    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private DistributionSetRepository distributionSetRepository;
    @Autowired
    private RolloutStatusCache rolloutStatusCache;
    @Autowired
    private StartNextGroupRolloutGroupSuccessAction startNextRolloutGroupAction;
    @Autowired
    private RolloutTargetGroupRepository rolloutTargetGroupRepository;
    @Autowired
    private JpaRolloutGroupManagement jpaRolloutGroupManagement;
    @Autowired
    @Lazy
    private HandleRolloutSchedulerService handleRolloutSchedulerService;
    @Autowired
    private RolloutAsyncService rolloutAsyncService;

    @Value("${rollout.valid.end.date.days}")
    private long validEndDateOffset;

    public JpaRolloutManagement(final TargetManagement targetManagement, final DistributionSetManagement distributionSetManagement, final VirtualPropertyReplacer virtualPropertyReplacer, final Database database, final RolloutApprovalStrategy rolloutApprovalStrategy, final TenantConfigurationManagement tenantConfigurationManagement, final SystemSecurityContext systemSecurityContext, final DistributionSetTagRepository distributionSetTagRepository, SupportPackageManagement supportPackageManagement, TargetTagManagement targetTagManagement, TargetFilterQueryManagement targetFilterQueryManagement, RolloutGroupManagement rolloutGroupManagement, final SoftwareModuleManagement softwareModuleManagement, final ArtifactsManagement artifactsManagement, final EntityFactory entityFactory, final VersionManagement versionManagement, final SystemManagement systemManagement, final DistributionSetTypeManagement distributionSetTypeManagement, final TargetRepository targetRepository, final TargetTargetTagRepository targetTargetTagRepository, final KafkaMessageService kafkaMessageService, DeploymentManagement deploymentManagement) {
        this.targetManagement = targetManagement;
        this.distributionSetManagement = distributionSetManagement;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.database = database;
        this.distributionSetTagRepository = distributionSetTagRepository;
        this.supportPackageManagement = supportPackageManagement;
        this.targetTagManagement = targetTagManagement;
        this.targetFilterQueryManagement = targetFilterQueryManagement;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.softwareModuleManagement = softwareModuleManagement;
        this.artifactsManagement = artifactsManagement;
        this.entityFactory = entityFactory;
        this.versionManagement = versionManagement;
        this.systemManagement = systemManagement;
        this.distributionSetTypeManagement = distributionSetTypeManagement;
        this.targetRepository = targetRepository;
        this.targetTargetTagRepository = targetTargetTagRepository;
        this.kafkaMessageService = kafkaMessageService;
        this.deploymentManagement = deploymentManagement;
    }

    public static String createRolloutLockKey(final String tenant) {
        return tenant + "-rollout";
    }

    private static void checkIfDeleted(final Long rolloutId, final RolloutStatus status) {
        if (RolloutStatus.DELETING == status || RolloutStatus.DELETED == status) {
            log.debug("Rollout {} is soft deleted and cannot be changed", rolloutId);
            throw new EntityReadOnlyException("Rollout " + rolloutId + " is soft deleted and cannot be changed");
        }
    }

    /**
     * Validates the rollout status.
     *
     * @param rollout        the current status of the rollout
     * @param expectedStatus the expected status of the rollout
     * @throws RolloutIllegalStateException if the current status does not match the expected status
     */
    private static void validateRolloutStatus(JpaRollout rollout, RolloutStatus expectedStatus) {
        if (expectedStatus != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout must be in the " + expectedStatus + " state to proceed, but the current state is "
                    + rollout.getStatus());
        }
    }

    /**
     * Converts a list of distribution set requests into entity creation objects.
     */
    static List<DistributionSetCreate> dsFromRequest(final Collection<MgmtDistributionSetRequestBodyPost> sets,
                                                     final EntityFactory entityFactory) {
        return sets.stream().map(dsRest -> fromRequest(dsRest, entityFactory)).toList();
    }

    /**
     * Converts a single distribution set request into an entity creation object.
     */
    private static DistributionSetCreate fromRequest(final MgmtDistributionSetRequestBodyPost dsRest,
                                                     final EntityFactory entityFactory) {

        final Map<Long, Long> smChanged = new HashMap<>();
        if (dsRest.getOs() != null) {
            smChanged.put(dsRest.getOs().getId(), Long.parseLong(dsRest.getOs().getSoftwareVersionTargetId()));
        }

        if (dsRest.getApplication() != null) {
            smChanged.put(dsRest.getApplication().getId(), Long.parseLong(dsRest.getApplication().getSoftwareVersionTargetId()));
        }

        if (dsRest.getRuntime() != null) {
            smChanged.put(dsRest.getRuntime().getId(), Long.parseLong(dsRest.getRuntime().getSoftwareVersionTargetId()));
        }

        if (dsRest.getModules() != null) {
            dsRest.getModules().forEach(module -> smChanged.put(module.getId(), Long.parseLong(module.getSoftwareVersionTargetId())));
        }

        return entityFactory.distributionSet().create().name(dsRest.getName())
                .version(Objects.isNull(dsRest.getVersion()) ? DEFAULT_VERSION : dsRest.getVersion())
                .description(dsRest.getDescription()).type(dsRest.getType()).modules(smChanged)
                .requiredMigrationStep(dsRest.isRequiredMigrationStep()).softwareDowngradeEnabled(dsRest.isSoftwareDowngradeEnabled());
    }

    private static <E extends Enum<E>> boolean containsAllEnums(final List<E> list1, final List<E> list2) throws ValidationException {
        // Convert the first list to a Set for O(1) lookup
        Set<E> set2 = EnumSet.copyOf(list2);
        return set2.containsAll(list1);
    }

    private static void validateRolloutInputDates(Rollout rollout) {
        Long rolloutEndDate = rollout.getEndAt();
        Long rolloutStartDate = rollout.getStartAt();
        log.debug("Rollout End date:{}", rolloutEndDate);
        log.debug("Rollout Start date:{}", rolloutStartDate);

        if (Objects.isNull(rolloutEndDate)) {
            log.debug("Rollout End date is null");
            throw new ValidationException("End date cannot be null");
        }
        // Check if the start date is after the end date
        if (rolloutStartDate != null && rolloutStartDate > rolloutEndDate) {
            throw new ValidationException("End Date should be greater than Start Date");
        }
    }

    /**
     * Maps the rest request body into rollout group object
     *
     * @param restRequest json body provided in the request
     * @return {@link RolloutGroup}
     */
    private static RolloutGroup getRolloutGroupsFromRequest(final MgmtRolloutGroup restRequest) {
        JpaRolloutGroup jpaRolloutGroup = getJpaRolloutGroupFromRequest(restRequest);
        RolloutGroupConditions conditions = RolloutHelper.buildRolloutGroupConditions(restRequest, true);
        if (conditions != null) {
            JpaRolloutGroup.addSuccessAndErrorConditionsAndActions(jpaRolloutGroup, conditions);
        }
        log.debug("Rollout Group from request: {}", jpaRolloutGroup);
        return jpaRolloutGroup;
    }

    /**
     * @param restRequest json body provided in the request
     * @return {@link JpaRolloutGroup}
     */
    private static JpaRolloutGroup getJpaRolloutGroupFromRequest(MgmtRolloutGroup restRequest) {
        JpaRolloutGroup jpaRolloutGroup = new JpaRolloutGroup();
        jpaRolloutGroup.setName(restRequest.getName());
        jpaRolloutGroup.setDescription(restRequest.getDescription());
        jpaRolloutGroup.setTargetFilterQuery(restRequest.getTargetFilterQuery());
        if (restRequest.getTargetPercentage() == null) {
            throw new ValidationException("TargetPercentage is required for all the groups.");
        }
        jpaRolloutGroup.setTargetPercentage(restRequest.getTargetPercentage());
        if (restRequest.getSuccessCondition() != null) {
            jpaRolloutGroup.setSuccessConditionExp(restRequest.getSuccessCondition().getExpression());
        }
        if (restRequest.getErrorCondition() != null) {
            jpaRolloutGroup.setErrorConditionExp(restRequest.getErrorCondition().getExpression());
        }
        return jpaRolloutGroup;
    }

    @Override
    public Page<Rollout> findAll(final Pageable pageable, final boolean deleted) {
        return JpaManagementHelper.findAllWithCountBySpec(rolloutRepository, pageable,
                Collections
                        .singletonList(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort())));
    }

    @Override
    public Page<Rollout> findByRsql(final Pageable pageable, final String rsqlParam, final boolean deleted) {
        final List<Specification<JpaRollout>> specList = Lists.newArrayListWithExpectedSize(2);
        specList.add(RSQLUtility.buildRsqlSpecification(rsqlParam, RolloutFields.class, virtualPropertyReplacer, database));
        specList.add(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort()));

        return JpaManagementHelper.findAllWithCountBySpec(rolloutRepository, pageable, specList);
    }

    @Override
    public Optional<Rollout> get(final long rolloutId) {
        return rolloutRepository.findById(rolloutId).map(Rollout.class::cast);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout approveOrDeny(final long rolloutId, final MgmtRolloutApprovalDecision decision) {
        return this.approveOrDeny(rolloutId, decision, null);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout approveOrDeny(final long rolloutId, final MgmtRolloutApprovalDecision decision, final String remark) {
        log.debug("approveOrDeny rollout called for rollout {} with decision {}", rolloutId, decision);
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        switch (decision) {
            case APPROVED:
                rollout.setStatus(RolloutStatus.DRAFT);
                break;
            case DENIED:
                break;
            default:
                throw new IllegalArgumentException("Unknown approval decision: " + decision);
        }
        rollout.setApprovalDecidedBy(rolloutApprovalStrategy.getApprovalUser(rollout));
        if (remark != null) {
            rollout.setApprovalRemark(remark);
        }
        JpaRollout savedRollout = rolloutRepository.save(rollout);
        // Payload for rollout status
        RolloutStatusPayload payload = RolloutStatusPayload.builder()
                .type("INFO")
                .status(savedRollout.getStatus().toString())
                .errorCode(Collections.emptyList())
                .errorMessages(Collections.emptyList())
                .timestamp(Instant.now().getEpochSecond())
                .build();
        // Build KafkaEventHeader
        // Header
        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .rolloutName(savedRollout.getName())
                .build();

        // Wrap in KafkaEventTemplate
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(payload)
                .build();

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.ROLLOUT_STATUS);
        return savedRollout;
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout freeze(final long rolloutId) {
        log.debug("freeze Rollout called for rollout {}", rolloutId);
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.checkIfRolloutCanFreeze(rollout);
        rollout.setStatus(RolloutStatus.FREEZING);
        rollout.setLastCheck(0);
        return rolloutRepository.save(rollout);
    }

    /**
     * Starts the rollout process for the specified rollout ID. This method performs several validations
     * to ensure that all required support packages and artifacts are uploaded to the CDN before starting the rollout.
     * If any validation fails, a {@link ValidationException} is thrown.
     *
     * @param rolloutId the ID of the rollout to be started
     * @return the updated {@link Rollout} entity with the status set to STARTING
     * @throws ValidationException          if any required support packages or artifacts are not uploaded to the CDN
     * @throws EntityNotFoundException      if the specified rollout is not found
     * @throws RolloutIllegalStateException if the rollout cannot be started due to its current status
     */

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))

    public Rollout start(final long rolloutId) throws EntityNotFoundException, RolloutIllegalStateException {
        log.debug("startRollout called for rollout {}", rolloutId);

        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.checkIfRolloutCanStarted(rollout, rollout);
        //TODO: Other validations to be implemented as part of COSMOS-1290
        List<Rsp> rspSupportPackages = supportPackageManagement.getRSPSupportPackages(rolloutId);

        // Retrieve and validate RSP support packages
        List<Rsp> pendingRspToBeUploaded = getPendingRspToBeUploadedToCdn(rspSupportPackages);
        if (!pendingRspToBeUploaded.isEmpty()) {
            validateRSPPackages(pendingRspToBeUploaded);

            // Upload RSP files
            pendingRspToBeUploaded.forEach(supportPackageCdnFileUploadService::uploadFile);
        }

        rollout.setStatus(RolloutStatus.STARTING);
        rollout.setLastCheck(0);
        return rolloutRepository.save(rollout);
    }

    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @Override
    public void resumeDeviceAction(final long rolloutId, final String controllerId) {
        log.debug("rolloutId: {}, controllerId: {}", rolloutId, controllerId);
        getRolloutAndThrowExceptionIfNotFound(rolloutId);
        findTargetbyControllerIdAndThrowIfNotFound(controllerId);

        Optional<JpaRollout> rollout = rolloutRepository.getRolloutById(rolloutId);
        JpaAction actions = actionRepository.getActionByRolloutIdAndControllerIdByStatus(rolloutId, controllerId, DeviceActionStatus.PAUSED, true)
                .orElseThrow(() -> new RolloutIllegalStateException("Device is not in PAUSED state"));
        log.debug("Fetched the action that is associated with rolloutId and controllerId: {}", actions);

        //TODO : Once we have DeviceAction Scheduler FrameWork we need to look into this
        handleActions(List.of(actions), rollout.get(), CHECK_DEVICE_STATUSES_TO_RESUME_DEVICE, List.of(DeviceActionStatus.PAUSED), RolloutStatus.RESUMING, DeviceActionStatus.RUNNING);

        log.debug(" Updated action status and saved it in Repository: {}", actions);
    }

    private void findTargetbyControllerIdAndThrowIfNotFound(final String controllerId) {
        targetManagement.getByControllerID(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    /**
     * Returns a list of RSPs pending upload to the CDN, filtering out those already uploaded or in the process of uploading.
     */
    private List<Rsp> getPendingRspToBeUploadedToCdn(List<Rsp> rspSupportPackages) {
        return rspSupportPackages.stream().filter(rsp -> rsp.getSupportPackageFileStatus() == null || (
                !rsp.getSupportPackageFileStatus().equals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL) &&
                        !rsp.getSupportPackageFileStatus().equals(FileTransferStatus.UPLOADING_TO_CDN))).toList();
    }

    /**
     * Validates that all RSP support packages are ready for upload.
     */
    private void validateRSPPackages(final List<Rsp> rspSupportPackages) {
        log.debug("Validating RSP support packages. Total packages: {}", rspSupportPackages.size());
        boolean isStorageUploaded = rspSupportPackages.stream()
                .allMatch(rsp -> rsp.getSupportPackageFileStatus() != null &&
                        rsp.getSupportPackageFileStatus().equals(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL));

        if (!isStorageUploaded) {
            throw new ValidationException("All RSP packages must be uploaded to storage before starting the rollout.");
        }
    }

    /**
     * Pauses a device action associated with a specific rolloutId and controllerId which is in running.
     * retrieves the rolloutId and throws an exception if it is not found.
     * retrieves controllerId and throws an exception if it is not found.
     * fetches the actions associated with rolloutId and controllerId.
     * updates the status of the action from running to paused.
     * if action is not in running state, throws an exception.
     *
     * @param rolloutId
     * @param controllerId
     */
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @Override
    public void pauseDeviceAction(final long rolloutId, final String controllerId) {
        log.debug("rolloutId: {}, controllerId: {}", rolloutId, controllerId);
        getRolloutAndThrowExceptionIfNotFound(rolloutId);
        findTargetbyControllerIdAndThrowIfNotFound(controllerId);

        Optional<JpaRollout> rollout = rolloutRepository.getRolloutById(rolloutId);
        JpaAction actions = actionRepository.getActionByRolloutIdAndControllerIdByStatus(rolloutId, controllerId, DeviceActionStatus.RUNNING, true)
                .orElseThrow(() -> new RolloutIllegalStateException("Device is not in RUNNING state"));
        log.debug("Fetched the action that is associated with rolloutId and controllerId: {}", actions);

        //TODO : Once we have DeviceAction Scheduler FrameWork we need to look into this
        handleActions(List.of(actions), rollout.get(), CHECK_DEVICE_STATUSES_TO_PAUSE_DEVICE, List.of(DeviceActionStatus.RUNNING), RolloutStatus.PAUSING, DeviceActionStatus.PAUSED);

        log.debug(" Updated action status and saved it in Repository: {}", actions);
    }

    private void handleActions(List<JpaAction> actions, JpaRollout rollout, List<DeviceActionStatus> checkDeviceStatuses, List<DeviceActionStatus> desiredState, RolloutStatus setRolloutStatus, DeviceActionStatus setDeviceStatus) {
        List<JpaAction> actionsByRolloutIdIn = actionRepository.findActionByRolloutIdAndActive(rollout.getId(), true);
        long countOfActionsInDesiredState = actionsByRolloutIdIn.stream()
                .filter(action -> desiredState.contains(action.getStatus()))
                .count();

        final boolean checkActionStatus = actionsByRolloutIdIn.stream().allMatch(action -> checkDeviceStatuses.contains(action.getStatus()));
        if (checkActionStatus && countOfActionsInDesiredState == 1) {
            RolloutSchedulerUtils.updateRolloutStatus(rollout, setRolloutStatus, rolloutRepository);
        } else {
            actions.forEach(action -> {
                action.setStatus(setDeviceStatus);
            });
            actionRepository.saveAll(actions);
        }
    }

    /**
     * Checks if the status of a given action is either RUNNING or PAUSED or DD_SENT
     * if so, it updates the status to CANCELLING and saves it in the repository
     *
     * @param actions
     * @return
     */
    private boolean updateActionStatusIfNeeded(Optional<JpaAction> actions) {
        if (DeviceActionStatus.RUNNING.equals(actions.get().getStatus()) || DeviceActionStatus.PAUSED.equals(actions.get().getStatus()) || DeviceActionStatus.DD_SENT.equals(actions.get().getStatus())) {
            log.debug("Updated action status to CANCELING and saved in Repository: {}", actions);
            return true;
        }
        return false;
    }

    /**
     * Cancel a device action associated with a specific rolloutId and controllerId which is in RUNNING, PAUSED or DD_SENT
     * retrieves the rolloutId and throws an exception if it is not found
     * retrieves controllerId and throws an exception if it is not found
     * fetches the actions associated with rolloutId and controllerId
     * updates the status of the action from running/paused to canceled
     * if action is not in running/paused state, throws an exception.
     *
     * @param rolloutId
     * @param controllerId
     */
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    @Override
    public void cancelDeviceAction(final long rolloutId, final String controllerId) {
        log.debug("rolloutId: {}, controllerId: {}", rolloutId, controllerId);
        getRolloutAndThrowExceptionIfNotFound(rolloutId);
        findTargetbyControllerIdAndThrowIfNotFound(controllerId);

        final Optional<JpaAction> actions = actionRepository.getActionByRolloutIdAndControllerId(rolloutId, controllerId, true);
        log.debug("Fetched the action that is associated with rolloutId and controllerId: {}", actions);

        if (!updateActionStatusIfNeeded(actions)) {
            throw new RolloutIllegalStateException("Cannot cancel this Device because it is not in RUNNING, PAUSED or DD_SENT state. Current status:"
                    + actions.get().getStatus().name().toLowerCase());
        }
        actions.get().setStatus(DeviceActionStatus.CANCELING);
        //TODO : Once we have DeviceAction Scheduler we can set the status to CANCELING
        actions.get().setStatus(DeviceActionStatus.CANCELED);
        actions.get().setActive(false);
        actionRepository.save(actions.get());
        log.debug("Updated action status to CANCELED and saved in Repository: {}", actions);
    }

    /**
     * checks the status of the rollout if it is in RUNNING update it to PUASING
     * once it is in PAUSING the scheduler will update the status of the rollout and rolloutGroups, actions to  PAUSED
     *
     * @param rolloutId the rollout to be paused.
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void pauseRollout(final long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.RUNNING);
        rollout.setStatus(RolloutStatus.PAUSING);
        saveRolloutStatus(rollout);
    }

    /**
     * check the status of the rollout if it is in RUNNING / PAUSED update it to CANCELING
     * once it is in CANCELING the scheduler will update the status to CANCELED
     *
     * @param rolloutId the rollout to be canceled.
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void cancelRollout(final long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        // Check if the rollout is either in RUNNING or PAUSED state
        RolloutHelper.verifyRolloutInStatuses(rollout, STATUSES_TO_BE_CANCELING);
        rollout.setStatus(RolloutStatus.CANCELING);
        rolloutRepository.save(rollout);
    }

    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void resumeRollout(final long rolloutId) {
        // Retrieve the rollout entity by its ID and throw an exception if it is not found
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        // Check if the rollout is in the PAUSED state
        RolloutHelper.verifyRolloutInStatus(rollout, (RolloutStatus.PAUSED));
        rollout.setStatus(RolloutStatus.RESUMING);
        rolloutRepository.save(rollout);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void delete(final long rolloutId) {
        final JpaRollout jpaRollout = findJpaRolloutOrThrow(rolloutId);
        validateRolloutStatus(jpaRollout);
        unlinkAndDeleteSupportPackages(rolloutId);
        jpaRollout.setStatus(RolloutStatus.DELETING);
        rolloutRepository.save(jpaRollout);
        deleteAssociatedDistributionSets(rolloutId);
    }

    private JpaRollout findJpaRolloutOrThrow(final long rolloutId) {
        return rolloutRepository.findById(rolloutId)
                .orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
    }

    private void validateRolloutStatus(final JpaRollout jpaRollout) {
        if (RolloutStatus.DELETING.equals(jpaRollout.getStatus())) {
            return;
        }
        if (!jpaRollout.getStatus().equals(RolloutStatus.DRAFT)) {
            throw new ValidationException(String.format(
                    "Rollout can be deleted only in DRAFT status but the current status of the rollout with Id: %s is %s.",
                    jpaRollout.getId(), jpaRollout.getStatus()));
        }
    }

    private void unlinkAndDeleteSupportPackages(final long rolloutId) {
        List<Esp> esp = supportPackageManagement.getESPSupportPackages(rolloutId);
        List<Rsp> rsp = supportPackageManagement.getRSPSupportPackages(rolloutId);
        if (!esp.isEmpty() || !rsp.isEmpty()) {
            supportPackageManagement.deleteSupportPackage(rolloutId, esp, rsp);
        }
    }

    private void deleteAssociatedDistributionSets(final long rolloutId) {
        List<Long> distributionSetIds = distributionSetRepository.findAssignedToRolloutDistributionSetsById(List.of(rolloutId));

        log.debug("Found DistributionSet IDs associated with Rollout ID {}: {}", rolloutId, distributionSetIds);
        for (Long distributionSetId : distributionSetIds) {
            if (canDeleteDistributionSet(distributionSetId)) {
                distributionSetRepository.deleteByIdIn(List.of(distributionSetId));
                log.info("Deleted DistributionSet ID: {}", distributionSetId);
            } else {
                log.warn("DistributionSet ID {} cannot be deleted as it is still assigned to other rollouts or targets", distributionSetId);
            }
        }
    }

    private boolean canDeleteDistributionSet(Long distributionSetId) {
        List<Long> assignedToOtherRollouts = distributionSetRepository.findAssignedToRolloutDistributionSetsById(List.of(distributionSetId));
        List<Long> assignedToTargets = distributionSetRepository.findAssignedToTargetDistributionSetsById(List.of(distributionSetId));
        return assignedToOtherRollouts.isEmpty() && assignedToTargets.isEmpty();
    }

    @Deprecated
    private void deleteUnusedTags(Long distributionSetId) {
        List<JpaDistributionSetTag> tags = distributionSetTagRepository.findAllById(List.of(distributionSetId));
        for (JpaDistributionSetTag tag : tags) {
            if (!distributionSetTagRepository.existsByName(tag.getName())) {
                distributionSetTagRepository.deleteByName(tag.getName());
            }
        }
    }

    @Override
    public Rollout addTargetFilterQuery(final Rollout rollout, String targetFilterQuery) {
        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setTargetFilterQuery(targetFilterQuery);
        return rolloutRepository.save(jpaRollout);
    }

    @Override
    public long count() {
        return rolloutRepository.count(
                RolloutSpecification.isDeletedWithDistributionSet(false, Sort.by(Direction.DESC, AbstractJpaBaseEntity_.ID)));
    }

    @Override
    public long countByFilters(final String searchText) {
        return rolloutRepository.count(RolloutSpecification.likeName(searchText, false));
    }

    @Override
    public long countByDistributionSetIdAndRolloutIsStoppable(final long setId) {
        return rolloutRepository.countByDistributionSetIdAndStatusIn(setId, ROLLOUT_STATUS_STOPPABLE);
    }

    @Override
    public Slice<Rollout> findByFiltersWithDetailedStatus(final Pageable pageable, final String searchText, final boolean deleted) {
        final Slice<Rollout> findAll = JpaManagementHelper.findAllWithoutCountBySpec(rolloutRepository, pageable, Collections.singletonList(RolloutSpecification.likeName(searchText, deleted)));
        setRolloutStatusDetails(findAll);
        return findAll;
    }

    @Override
    public List<Long> findActiveRollouts() {
        return rolloutRepository.findByStatusIn(ACTIVE_ROLLOUTS);
    }

    @Override
    public List<Rollout> findByActiveRolloutsEndAtNow() {

        // Created native query to bypass tenant validation and will work with all database.
        // Modifying this query should be taken care for cross database execution.
        final Query selectQuery = entityManager
                .createNativeQuery(String.format(NATIVE_QUERY_FOR_ACTIVE_ROLLOUT_END_AT_NOW,
                        getStringRolloutStatusToEnd()), JpaRollout.class);
        selectQuery.setParameter(JpaRollout_.END_AT, Instant.now().getEpochSecond());
        return selectQuery.getResultList();
    }

    @Override
    public Optional<Rollout> getByName(final String rolloutName) {
        return rolloutRepository.findByName(rolloutName);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout update(final RolloutUpdate u) {
        final GenericRolloutUpdate update = (GenericRolloutUpdate) u;
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(update.getId());

        checkIfDeleted(update.getId(), rollout.getStatus());

        updateRolloutDetails(update, rollout);

        update.getSet().ifPresent(setId -> {
            final DistributionSet set = distributionSetManagement.getValidAndComplete(setId);
            if (!isTenantDistributionSoftwareDowngradeEnabled() && set.isSoftwareDowngradeEnabled()) {
                throw new ValidationException("Current tenant's configuration does not allow software downgrade.");
            }
            rollout.setDistributionSet(set);
        });
        if (rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
            rollout.setApprovalDecidedBy(null);
            rollout.setApprovalRemark(null);
        }
        updateLogSettings(update, rollout);
        validateStartDate(rollout);
        validateEndDate(rollout);
        //Validate download timers
        validateDownloadTimers(rollout);
        validateMaxUpdateTime(rollout);

        return rolloutRepository.save(rollout);
    }

    private void updateRolloutDetails(GenericRolloutUpdate update, JpaRollout rollout) {
        update.getName().ifPresent(rollout::setName);
        update.getDescription().ifPresent(rollout::setDescription);
        update.getPriority().ifPresent(rollout::setPriority);
        update.getStartType().ifPresent(rollout::setStartType);
        update.getUserAcceptanceRequired().ifPresent(rollout::setUserAcceptanceRequired);
        update.getConnectivityType().ifPresent(rollout::setConnectivityType);
        update.getForcedTime().ifPresent(rollout::setForcedTime);
        update.getWeight().ifPresent(rollout::setWeight);
        rollout.setStartAt(update.getStartAt().orElse(rollout.getStartAt()));
        rollout.setEndAt(update.getEndAt().orElse(rollout.getEndAt()));
    }

    private void updateLogSettings(GenericRolloutUpdate update, JpaRollout rollout) {
        update.isLogCollectionRequired().ifPresent(rollout::setLogCollectionRequired);
        update.getLogMaxSuccessVin().ifPresent(rollout::setLogMaxSuccessVin);
        update.getLogMaxFailureVin().ifPresent(rollout::setLogMaxFailureVin);
        update.getLogMaxAllFileSize().ifPresent(rollout::setLogMaxAllFileSize);
        update.getLogMaxEachFileSize().ifPresent(rollout::setLogMaxEachFileSize);
        update.getLogMaxNumberOfFiles().ifPresent(rollout::setLogMaxNumberOfFiles);
        update.getDownloadRetryCount().ifPresent(rollout::setDownloadRetryCount);
        update.getMaxDownloadDurationTimer().ifPresent(rollout::setMaxDownloadDurationTimer);
        update.getMaxDownloadWifiDurationTimer().ifPresent(rollout::setMaxDownloadWifiDurationTimer);
        update.getMaxDownloadCellularDurationTimer().ifPresent(rollout::setMaxDownloadCellularDurationTimer);
        update.getRequiredMedia().ifPresent(rollout::setRequiredMedia);
        update.getDowngradeAllowed().ifPresent(rollout::setDowngradeAllowed);
        update.getRequiredStateOfCharge().ifPresent(rollout::setRequiredStateOfCharge);
        update.getMaxUpdateTime().ifPresent(rollout::setMaxUpdateTime);
    }

    private JpaRollout getRolloutAndThrowExceptionIfNotFound(final Long rolloutId) {
        return rolloutRepository.findById(rolloutId).orElseThrow(() -> new EntityNotFoundException(Rollout.class, rolloutId));
    }

    @Override
    public Slice<Rollout> findAllWithDetailedStatus(final Pageable pageable, final boolean deleted) {
        final Slice<Rollout> rollouts = JpaManagementHelper.findAllWithoutCountBySpec(rolloutRepository, pageable, Collections.singletonList(RolloutSpecification.isDeletedWithDistributionSet(deleted, pageable.getSort())));
        setRolloutStatusDetails(rollouts);
        return rollouts;
    }

    @Override
    public Optional<Rollout> getWithDetailedStatus(final long rolloutId) {
        final Optional<Rollout> rollout = get(rolloutId);

        if (!rollout.isPresent()) {
            return rollout;
        }

        List<TotalTargetCountActionStatus> rolloutStatusCountItems = rolloutStatusCache.getRolloutStatus(rolloutId);

        if (CollectionUtils.isEmpty(rolloutStatusCountItems)) {
            rolloutStatusCountItems = actionRepository.getStatusCountByRolloutId(rolloutId, true);
            rolloutStatusCache.putRolloutStatus(rolloutId, rolloutStatusCountItems);
        }

        final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(rolloutStatusCountItems, rollout.get().getTotalTargets(), rollout.get().getUserAcceptanceRequired());
        ((JpaRollout) rollout.get()).setTotalTargetCountStatus(totalTargetCountStatus);
        return rollout;
    }

    @Override
    public boolean exists(final long rolloutId) {
        return rolloutRepository.existsById(rolloutId);
    }

    private Map<Long, List<TotalTargetCountActionStatus>> getStatusCountItemForRollout(final List<Long> rollouts) {
        if (rollouts.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<Long, List<TotalTargetCountActionStatus>> fromCache = rolloutStatusCache.getRolloutStatus(rollouts);

        final List<Long> rolloutIds = rollouts.stream().filter(id -> !fromCache.containsKey(id)).toList();

        if (!rolloutIds.isEmpty()) {
            final List<TotalTargetCountActionStatus> resultList = actionRepository.getStatusCountByRolloutId(rolloutIds, true);
            final Map<Long, List<TotalTargetCountActionStatus>> fromDb = resultList.stream().collect(Collectors.groupingBy(TotalTargetCountActionStatus::getId));

            rolloutStatusCache.putRolloutStatus(fromDb);

            fromCache.putAll(fromDb);
        }

        return fromCache;
    }

    private void setRolloutStatusDetails(final Slice<Rollout> rollouts) {
        final List<Long> rolloutIds = rollouts.getContent().stream().map(Rollout::getId).toList();
        final Map<Long, List<TotalTargetCountActionStatus>> allStatesForRollout = getStatusCountItemForRollout(rolloutIds);

        if (allStatesForRollout != null && !allStatesForRollout.isEmpty()) {
            rollouts.forEach(rollout -> {
                final TotalTargetCountStatus totalTargetCountStatus = new TotalTargetCountStatus(allStatesForRollout.get(rollout.getId()), rollout.getTotalTargets(), rollout.getUserAcceptanceRequired());
                ((JpaRollout) rollout).setTotalTargetCountStatus(totalTargetCountStatus);
            });
        }
    }

    @Override
    @Transactional
    public void cancelRolloutsForDistributionSet(final DistributionSet set) {
        // stop all rollouts for this distribution set
        rolloutRepository.findByDistributionSetAndStatusIn(set, ROLLOUT_STATUS_STOPPABLE).forEach(rollout -> {
            final JpaRollout jpaRollout = (JpaRollout) rollout;
            jpaRollout.setStatus(RolloutStatus.CANCELING);
            rolloutRepository.save(jpaRollout);
            log.debug("Rollout {} canceled", jpaRollout.getId());
        });
    }

    private RolloutGroupsValidation validateTargetsInGroups(final List<RolloutGroup> groups, final String baseFilter, final long totalTargets, final Long dsTypeId) {
        final List<Long> groupTargetCounts = new ArrayList<>(groups.size());
        final Map<String, Long> targetFilterCounts = groups.stream()
                .map(group -> RolloutHelper.getGroupTargetFilter(baseFilter, group)).distinct()
                .collect(Collectors.toMap(Function.identity(),
                        groupTargetFilter -> targetManagement.countByRsqlAndCompatible(groupTargetFilter, dsTypeId)));

        long unusedTargetsCount = 0;

        for (int i = 0; i < groups.size(); i++) {
            final RolloutGroup group = groups.get(i);
            final String groupTargetFilter = RolloutHelper.getGroupTargetFilter(baseFilter, group);
            RolloutHelper.verifyRolloutGroupTargetPercentage(group.getTargetPercentage());

            final long targetsInGroupFilter = targetFilterCounts.get(groupTargetFilter);
            final long overlappingTargets = countOverlappingTargetsWithPreviousGroups(baseFilter, groups, group, i, targetFilterCounts);

            final long realTargetsInGroup;
            // Assume that targets which were not used in the previous groups
            // are used in this group
            if (overlappingTargets > 0 && unusedTargetsCount > 0) {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets + unusedTargetsCount;
                unusedTargetsCount = 0;
            } else {
                realTargetsInGroup = targetsInGroupFilter - overlappingTargets;
            }

            final long reducedTargetsInGroup = Math.round(group.getTargetPercentage() / 100 * (double) realTargetsInGroup);
            groupTargetCounts.add(reducedTargetsInGroup);
            unusedTargetsCount += realTargetsInGroup - reducedTargetsInGroup;

        }

        return new RolloutGroupsValidation(totalTargets, groupTargetCounts);
    }

    private long countOverlappingTargetsWithPreviousGroups(final String baseFilter, final List<RolloutGroup> groups, final RolloutGroup group, final int groupIndex, final Map<String, Long> targetFilterCounts) {
        // there can't be overlapping targets in the first group
        if (groupIndex == 0) {
            return 0;
        }
        final List<RolloutGroup> previousGroups = groups.subList(0, groupIndex);
        final String overlappingTargetsFilter = RolloutHelper.getOverlappingWithGroupsTargetFilter(baseFilter, previousGroups, group);

        if (targetFilterCounts.containsKey(overlappingTargetsFilter)) {
            return targetFilterCounts.get(overlappingTargetsFilter);
        } else {
            final long overlappingTargets = targetManagement.countByRsql(overlappingTargetsFilter);
            targetFilterCounts.put(overlappingTargetsFilter, overlappingTargets);
            return overlappingTargets;
        }
    }

    @Override
    public void startAllGroups(final long rolloutId) {
        // Initial validation of the rollout and its groups
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        RolloutHelper.verifyRolloutInStatus(rollout, RolloutStatus.RUNNING);
        final List<RolloutGroup> allGroups = rollout.getRolloutGroups();
        RolloutHelper.verifyAllRolloutGroupsStatus(allGroups, RolloutGroupStatus.QUEUED);
        //Invoke Async method to start the groups one by one
        log.info("Starting of the Rollout Groups in Async mode");
        rolloutAsyncService.startAllGroupsAsync(rollout, allGroups);
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void triggerNextGroup(final long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        if (RolloutStatus.RUNNING != rollout.getStatus()) {
            throw new RolloutIllegalStateException("Rollout is not in running state");
        }
        final List<RolloutGroup> groups = rollout.getRolloutGroups();

        final boolean isNextGroupTriggerable = groups.stream().anyMatch(g -> RolloutGroupStatus.QUEUED.equals(g.getStatus()));

        if (!isNextGroupTriggerable) {
            throw new RolloutIllegalStateException("Rollout does not have any groups left to be triggered");
        }

        final RolloutGroup latestRunning = groups.stream().sorted(Comparator.comparingLong(RolloutGroup::getId).reversed()).filter(g -> RolloutGroupStatus.RUNNING.equals(g.getStatus())).findFirst().orElseThrow(() -> new RolloutIllegalStateException("No group is running"));

        startNextRolloutGroupAction.exec(rollout, latestRunning);
    }

    @Override
    public boolean isTenantDistributionSoftwareDowngradeEnabled() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).isDistributionSoftwareDowngradeEnabled();
    }

    @Override
    public List<Rollout> findNotFinishedStatusRolloutByTargetId(Target targetId) {
        return Collections.unmodifiableList(rolloutRepository.findNotFinishedStatusRolloutByTargetId((JpaTarget) targetId, RolloutStatus.FINISHED));
    }

    @Override
    public List<Rollout> findAllRolloutByTargetIds(List<Target> targets) {
        List<JpaTarget> jpaTargetList = targets.stream().map(JpaTarget.class::cast).toList();
        return rolloutRepository.findAllRolloutByTargetIds(jpaTargetList);
    }

    /**
     * @param targetId the targetId
     * @return list of rollouts
     */
    @Override
    public List<Rollout> findAllRolloutByTargetId(Target targetId) {
        return Collections.unmodifiableList(rolloutRepository.findAllRolloutByTargetId((JpaTarget) targetId));
    }

    private String getStringRolloutStatusToEnd() {
        return ACTIVE_ROLLOUTS_END.stream().map(status -> String.valueOf(status.getValue())).collect(Collectors.joining(","));
    }

    @Override
    public void ensureRolloutIsInactiveOrThrow(long distributionSetId, String operationType) {
        List<Rollout> rolloutsByDistribution = rolloutRepository.findAllByDistributionSetId(distributionSetId);
        boolean allValidRollouts = rolloutsByDistribution.stream().allMatch(rollout -> ALLOWED_ROLLOUT_STATUSES_FOR_LINKING_AND_UNLINKING.contains(rollout.getStatus()));
        if (!allValidRollouts) {
            throw new ValidationException("The " + operationType + " operation cannot be performed as it is associated with the Rollout");
        }
    }

    public void validateNoAssociatedRolloutsOrThrow(long distributionSetId, String operationType) {
        List<Rollout> rolloutsByDistribution = rolloutRepository.findAllByDistributionSetId(distributionSetId);
        if (!rolloutsByDistribution.isEmpty()) {
            throw new ValidationException("The " + operationType + " operation cannot be performed because there is an associated Rollout.");
        }
    }

    /**
     * Finds a list of Rollout entities by the given distribution set ID.
     *
     * @param distributionSetId the ID of the distribution set to search for
     * @return a list of Rollout entities associated with the provided distribution set ID
     */
    @Override
    public List<Rollout> findByDistributionSetId(Long distributionSetId) {
        return rolloutRepository.findByDistributionSetId(distributionSetId);
    }

    /*
     * Validates the download duration timers.
     *
     * @param rollout the rollout to validate
     */
    private void validateDownloadTimers(Rollout rollout) {
        if (rollout.getMaxDownloadDurationTimer()
                < Math.max(rollout.getMaxDownloadWifiDurationTimer(), rollout.getMaxDownloadCellularDurationTimer())) {
            throw new ValidationException("Max download duration timer should be greater than either max wifi or cellular download duration timer");
        }
        if (rollout.getMaxDownloadDurationTimer()
                > Math.addExact(rollout.getMaxDownloadWifiDurationTimer(), rollout.getMaxDownloadCellularDurationTimer())) {
            throw new ValidationException("Max download duration timer should be less than sum of wifi and cellular download duration timer");
        }
    }

    /**
     * Validates DeploymentEstimatedUpdateTime of the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateDeploymentEstimatedUpdateTime(Rollout rollout) {
        if (Objects.isNull(rollout.getDeploymentEstimatedUpdateTime())) {
            log.debug("Estimated Update Time is null");
            throw new ValidationException("Estimated Update Time must not be null");
        }
        if (rollout.getDeploymentEstimatedUpdateTime() > rollout.getMaxUpdateTime()) {
            log.debug("estimatedUpdateTime should be less than or equal to maxUpdateTime");
            throw new ValidationException("Estimated Update Time should be less than or equal to Max Update Time value of " + rollout.getMaxUpdateTime());
        }
    }

    /**
     * Creates and saves a new {@link Rollout} entity in the repository.
     * This method sets default values for the provided {@link Rollout},
     * performs necessary validation, and attempts to save it. If a
     * concurrency issue occurs, the operation will retry a specified
     * number of times.
     *
     * @param rollout the {@link Rollout} entity to be created and saved
     * @return the saved {@link Rollout} entity
     * @throws ConcurrencyFailureException if a concurrency failure occurs
     *                                     during the save operation, with retry attempts as specified
     *                                     by {@link Constants#TX_RT_MAX} and delay by
     *                                     {@link Constants#TX_RT_DELAY}
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout create(Rollout rollout) {
        setDefaultValuesInRollout((JpaRollout) rollout);
        validateRollout(rollout);
        setVehicleLogLevel((JpaRollout) rollout);
        return rolloutRepository.save((JpaRollout) rollout);
    }

    /**
     * Sets the vehicle log level for the given rollout.
     * If the vehicle log level is not specified, it retrieves the default value
     * from tenant configuration and sets it. If the specified vehicle log level
     * is outside the valid range (0-7), a ValidationException is thrown.
     *
     * @param rollout the rollout entity for which to set the vehicle log level
     * @throws ValidationException if the specified vehicle log level is invalid
     */
    private void setVehicleLogLevel(JpaRollout rollout) {
        Integer vehicleLogLevel = rollout.getVehicleLogLevel();
        if (vehicleLogLevel == null) {
            int defaultLevel = tenantConfigurationManagement
                    .getConfigurationValue(
                            TenantConfigurationProperties.TenantConfigurationKey.VEHICLE_LOG_LEVEL,
                            Integer.class
                    )
                    .getValue();

            rollout.setVehicleLogLevel(defaultLevel);
        } else {
            if (vehicleLogLevel < 0 || vehicleLogLevel > 7) {
                throw new ValidationException("Invalid Vehicle Log Level");
            }
        }
    }

    /**
     * Unfreezes a specified rollout, converting the rollout and group state from READY to DRAFT.
     *
     * <p>This method changes the status of the rollout to allow further actions.
     * It requires the user to have the appropriate authorization to manage rollouts.
     *
     * @param rolloutId the ID of the rollout to unfreeze, must not be null
     * @throws EntityNotFoundException      if the rollout with the given ID does not exist
     * @throws RolloutIllegalStateException if the rollout or any of its groups are not in a state that can be unfrozen
     * @throws ValidationException          if the rollout group is empty
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void unfreeze(Long rolloutId) {
        final JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        // TODO: This is an intermittent solution for rollout stuck in FREEZING until ERROR state has been added.
        RolloutHelper.verifyRolloutInStatuses(rollout, List.of(RolloutStatus.READY, RolloutStatus.FREEZING));
        if (rollout.getStatus().equals(RolloutStatus.READY)) {
            final List<JpaRolloutGroup> rolloutGroups = getRolloutGroupsByRolloutId(rolloutId);
            validateRolloutGroups(rolloutGroups, RolloutGroupStatus.READY);
            rolloutGroups.forEach(rolloutGroup -> rolloutGroup.setStatus(RolloutGroupStatus.DRAFT));
            rollout.setRolloutGroups(rolloutGroups);
        }
        rollout.setStatus(RolloutStatus.DRAFT);
        rolloutRepository.save(rollout);
    }

    /**
     * Validates that the rollout type is set.
     *
     * @param rollout the rollout to validate
     * @throws ValidationException if the rollout type is null
     */
    private void validateType(Rollout rollout) {
        if (rollout.getType() == null) {
            log.debug("Rollout type is null");
            throw new ValidationException("Rollout type should be FOTA or AOTA");
        }
    }

    /**
     * Validates that updateAction is set when the rollout type is AOTA.
     *
     * @param rollout the rollout to validate
     * @throws ValidationException if updateAction is null for AOTA rollouts
     */
    private void validateUpdateAction(Rollout rollout) {
        if (rollout.getType() == MgmtRolloutType.AOTA && rollout.getUpdateAction() == null) {
            log.debug("UpdateAction is null for AOTA rollout");
            throw new ValidationException("Update Action is mandatory with type as AOTA and should be one of INSTALL/UNINSTALLANY/UNINSTALLSPECIFIC");
        }
    }

    /**
     * Validates that at least one version is provided when updateAction is UNINSTALLSPECIFIC.
     *
     * @param rollout the rollout to validate
     * @throws ValidationException if updateActionUninstallVersion is null or empty when updateAction is UNINSTALLSPECIFIC
     */
    private void validateUpdateActionUninstallVersion(Rollout rollout) {
        if (hasInvalidUninstallVersionValues(rollout)) {
            log.debug("No valid versions provided for UNINSTALLSPECIFIC action");
            throw new ValidationException("Valid Update Action Uninstall Version is mandatory with updateAction as UNINSTALLSPECIFIC and cannot be empty or null for AOTA type rollouts");
        }
    }

    /**
     * Validates that updateActionUninstallVersion is not null or empty when updateAction is UNINSTALLSPECIFIC.
     *
     * @param rollout the rollout to validate
     * @throws ValidationException if updateActionUninstallVersion is null or empty when updateAction is UNINSTALLSPECIFIC
     */
    private boolean hasInvalidUninstallVersionValues(Rollout rollout) {
        // Skip validation if not AOTA or not UNINSTALLSPECIFIC
        if (rollout.getType() != MgmtRolloutType.AOTA ||
                rollout.getUpdateAction() != MgmtUpdateAction.UNINSTALLSPECIFIC) {
            return false;
        }
        List<String> versions = rollout.getUpdateActionUninstallVersion();
        // If versions is null or empty, it's invalid
        if (versions == null || versions.isEmpty()) {
            return true;
        }
        // If any version is null, empty, or whitespace, it's invalid
        return versions.stream().anyMatch(version -> version == null || version.isBlank());
    }

    @Override
    public List<AssociatedTargetsToRolloutGroup> addDeviceDetails(Rollout rollout, List<String> registeredControllerIds, String groupsBody) {

        //create a tag with the rollout name if not exist and assign it to all the targets
        Optional<TargetTag> targetTagResult = targetTagManagement.getByName(rollout.getName());
        TargetTag tag;
        if (targetTagResult.isPresent()) {
            tag = targetTagResult.get();
        } else {
            tag = targetTagManagement.createTargetTag(rollout.getName(), null, null);
        }
        targetTagManagement.associateTagWithTargets(tag, registeredControllerIds);
        log.debug("Created tag and associated with targets: {}", tag);

        //Create a target filter with the above tag if not exist and assign it to the rollout
        Optional<TargetFilterQuery> targetFilterResult = targetFilterQueryManagement.getByName(rollout.getName());
        TargetFilterQuery targetFilter;
        if (targetFilterResult.isPresent()) {
            targetFilter = targetFilterResult.get();
        } else {
            targetFilter = getTargetFilterQuery(rollout, tag);
        }
        rollout = assignTargetFilterToRollout(targetFilter, rollout);
        log.debug("Created target filter and assigned to the rollout: {}", rollout);

        //This one is to persist tags on the targets.
        entityManager.flush();

        List<MgmtRolloutGroup> groups;

        if (groupsBody == null || groupsBody.isEmpty() || (groups = parseGroupsJson(groupsBody)).isEmpty()) {
            //Since no groups are provided, create one new rollout group with default conditions and add all the targets into it
            return createAndAddGroupsWithDefaultValues(rollout);//DEFAULT GROUPING
        } else {
            log.debug("Provided grouping conditions: {}", groups);
            return createAndAddGroupsWithGroupConditions(rollout, groups, registeredControllerIds);//With provided grouping Conditions
        }
    }

    /**
     * Updates the status of the rollout groups associated with the given rollout.
     * <p>
     * This method ensures that all provided rollout groups are in either {@code DRAFT} or {@code READY} status before proceeding.
     * If the rollout is in RUNNING or PAUSED status, it updates the group status to QUEUED.
     *
     * @param rollout                           The {@link Rollout} entity for which the groups are updated. Must not be null.
     * @param groupsBody                        A JSON string representing the new groups to be associated with the rollout.
     * @param allAssignedTargetsToRolloutGroups A list of {@link AssociatedTargetsToRolloutGroup} entities representing the targets assigned to the groups.
     * @return A list of updated {@link AssociatedTargetsToRolloutGroup} entities.
     * @throws ValidationException If any of the provided rollout groups are not in DRAFT or READY status status.
     */

    public List<AssociatedTargetsToRolloutGroup> updateRolloutGroupsForNewlyRegisteredControllerIds(
            @NotNull @Valid Rollout rollout,
            String groupsBody,
            List<AssociatedTargetsToRolloutGroup> allAssignedTargetsToRolloutGroups) {

        // Ensure all rollout groups are in DRAFT or READY status
        boolean validInitialStatuses = allAssignedTargetsToRolloutGroups.stream().allMatch(
                assoc -> {
                    RolloutGroupStatus status = assoc.getRolloutGroup().getStatus();
                    return status == RolloutGroupStatus.DRAFT || status == RolloutGroupStatus.READY;
                });

        if (!validInitialStatuses) {
            throw new ValidationException("New rollout groups must be in DRAFT or READY status");
        }

        List<Long> newGroupIds = allAssignedTargetsToRolloutGroups.stream().map(assoc -> assoc.getRolloutGroup().getId()).distinct().toList();
        List<JpaRolloutGroup> newGroups = rolloutGroupRepository.findAllById(newGroupIds);
        newGroups.forEach(group -> {
            RolloutGroupStatus status = group.getStatus();

            if (status == RolloutGroupStatus.DRAFT) {
                // Validate ESPs, attempt CDN upload
                List<Esp> pendingEspPackages = handleRolloutSchedulerService.validateAndUploadEspPackagesToCDN(rollout, group);

                // Move to READY
                RolloutSchedulerUtils.updateRolloutGroupStatus(group, RolloutGroupStatus.READY, rolloutGroupRepository);

                log.debug("Processed DRAFT group ID: {} → Status: {}", group.getId(), group.getStatus());
            } else if (status == RolloutGroupStatus.READY) {
                // Check if packages are already uploaded to CDN
                boolean allUploaded = handleRolloutSchedulerService.isEspCdnUploadComplete(rollout, group);

                if (allUploaded) {
                    RolloutSchedulerUtils.updateRolloutGroupStatus(group, RolloutGroupStatus.QUEUED, rolloutGroupRepository);
                }

                log.debug("Processed READY group ID: {} → Status: {}", group.getId(), group.getStatus());
            }
        });

        return allAssignedTargetsToRolloutGroups;
    }

    /**
     * Validates that all mandatory ESP (Electronic Software Package) file types are present for the registered controllers.
     * <p>
     * This method checks if each controller associated with the given rollout contains all mandatory ESP file types
     * as defined in the tenant configuration. If any controller is missing mandatory ESP file types or if the ESP upload
     * status is invalid, a {@link ValidationException} is thrown.
     * </p>
     *
     * @param rollout                 The {@link Rollout} entity for which the validation is performed. Must not be null.
     * @param registeredControllerIds A list of controller IDs to validate. Must not be null or empty.
     * @throws ValidationException If any controller does not contain all mandatory ESP file types or if the ESP upload status is invalid.
     */
    @Override
    @Transactional
    public void validateMandatoryEspForRegisteredControllers(Rollout rollout, List<String> registeredControllerIds) {

        List<MgmtSupportPackageFileType> mandatoryEspFileTypes =
                TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement)
                        .getTenantConfigurationMandatoryEsp();

        for (String controllerId : registeredControllerIds) {
            List<Esp> espList = this.supportPackageManagement.getEspByRolloutIdAndControllerId(controllerId, rollout.getId());
            List<MgmtSupportPackageFileType> espFileTypeList = espList.stream()
                    .map(BaseSupportPackage::getFileType)
                    .toList();
            if (mandatoryEspFileTypes.isEmpty()) {
                return;
            }
            if (espFileTypeList.size() < mandatoryEspFileTypes.size() || !containsAllEnums(mandatoryEspFileTypes, espFileTypeList)) {
                throw new ValidationException("controller with controllerId " + controllerId + " do not contain all the mandatory Esp");
            }


        }
    }

    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteDeviceDetails(Rollout rollout, List<String> registeredControllerIds, Boolean deleteEsp) {
        log.debug("Starting removeDeviceDetails for rollout ID: {}, with registered controller IDs: {}", rollout.getId(), registeredControllerIds);

        // Fetch target IDs associated with the provided controller IDs
        List<Long> targetIds = targetManagement.findTargetIdByControllerIds(registeredControllerIds);
        log.debug("Found {} target IDs associated with provided controller IDs.", targetIds.size());

        // Fetch all relevant rollout groups by target IDs and the provided rollout ID
        List<JpaRolloutGroup> rolloutGroups = fetchRolloutGroups(rollout.getId(), targetIds);
        log.debug("Found {} rollout groups associated with the targets.", rolloutGroups.size());

        // Throw an exception if no associated rollout groups are found
        if (rolloutGroups.isEmpty()) {
            log.error("No targets are associated with the provided rollout ID: {}", rollout.getId());
            throw new ValidationException("Targets are not associated with the provided rollout");
        }

        processDeviceDetailsDeletion(rolloutGroups, targetIds);
        if (deleteEsp) {
            List<Esp> espPackages = supportPackageManagement.getESPSupportPackages(rollout.getId());
            if (!espPackages.isEmpty()) {
                supportPackageManagement.deleteSupportPackage(rollout.getId(), espPackages, Collections.emptyList());
            }
        }
        log.debug("Completed removeDeviceDetails for rollout ID: {}", rollout.getId());
    }

    // Helper method to fetch all relevant rollout groups associated with the target IDs
    private List<JpaRolloutGroup> fetchRolloutGroups(Long rolloutId, List<Long> targetIds) {
        log.debug("Fetching rollout groups for rollout ID: {} and target IDs: {}", rolloutId, targetIds);
        return rolloutGroupRepository.findAllRolloutGroupsByTargetIds(rolloutId, targetIds);
    }

    // Helper method to get the list of targets associated with the current group
    private List<Long> getGroupTargets(JpaRolloutGroup rolloutGroup, List<Long> targetIds) {
        return rolloutGroup.getRolloutTargetGroup().stream().map(targetGroup -> targetGroup.getId().getTarget()).filter(targetIds::contains).collect(Collectors.toList());
    }

    // Helper method to remove a rollout group and update the associated rollout entity
    private void removeRolloutGroupAndUpdateRollout(JpaRolloutGroup rolloutGroup, int removedTargetsCount) {
        log.debug("Removing rollout group ID: {} and updating the associated rollout entity.", rolloutGroup.getId());

        RolloutGroup parentGroupId = rolloutGroup.getParent();

        // Directly delete the rollout group
        rolloutGroupRepository.delete(rolloutGroup);

        List<JpaRolloutGroup> childGroups = rolloutGroupRepository.findByParentId(rolloutGroup.getId());
        for (JpaRolloutGroup child : childGroups) {
            log.debug("Reassigning parent of group ID: {} from {} to {}", child.getId(), rolloutGroup.getId(), parentGroupId);
            child.setParent(parentGroupId);
            rolloutGroupRepository.save(child);
        }
        log.debug("Deleted group ID: {} and reassigned {} child groups to parent group ID: {}", rolloutGroup.getId(), childGroups.size(), parentGroupId);

        // Update the parent rollout entity and save it
        JpaRollout jpaRollout = (JpaRollout) rolloutGroup.getRollout();
        updateRolloutAfterGroupRemoval(jpaRollout, removedTargetsCount);
    }

    // Helper method to update the rollout after a group removal
    private void updateRolloutAfterGroupRemoval(JpaRollout jpaRollout, int removedTargetsCount) {
        log.debug("Updating rollout ID: {} after group removal. Decreasing total targets by {}", jpaRollout.getId(), removedTargetsCount);
        jpaRollout.setRolloutGroupsCreated(jpaRollout.getRolloutGroupsCreated() - 1);
        jpaRollout.setTotalTargets(jpaRollout.getTotalTargets() - removedTargetsCount);
        rolloutRepository.save(jpaRollout);
    }

    // Helper method to update the rollout group with the remaining targets
    private void updateRolloutGroupTargets(JpaRolloutGroup rolloutGroup, List<Long> groupTargets, int remainingGroupTargets) {
        int targetsCount = groupTargets.size();
        log.debug("Updating rollout group ID: {}. Removing {} targets and setting new total targets: {}", rolloutGroup.getId(), targetsCount, remainingGroupTargets);

        // Delete targets from the group
        rolloutTargetGroupRepository.deleteTargetIdAndRolloutGroupAssociation(groupTargets, rolloutGroup);

        // Update the total targets in the rollout group and save it
        rolloutGroup.setTotalTargets(remainingGroupTargets);
        rolloutGroupRepository.save(rolloutGroup);

        // Update the parent rollout entity
        JpaRollout jpaRollout = (JpaRollout) rolloutGroup.getRollout();
        updateRolloutAfterGroupUpdate(jpaRollout, targetsCount);
    }

    // Helper method to update the parent rollout after a group update
    private void updateRolloutAfterGroupUpdate(JpaRollout jpaRollout, int removedTargetsCount) {
        log.debug("Updating rollout ID: {} after group update. Decreasing total targets by {}", jpaRollout.getId(), removedTargetsCount);
        jpaRollout.setTotalTargets(jpaRollout.getTotalTargets() - removedTargetsCount);
        rolloutRepository.save(jpaRollout);
    }

    private void processDeviceDetailsDeletion(List<JpaRolloutGroup> rolloutGroups, List<Long> targetIds) {
        // Update each rollout group based on the number of matching targets
        for (JpaRolloutGroup rolloutGroup : rolloutGroups) {
            log.debug("Processing rollout group ID: {}", rolloutGroup.getId());

            List<Long> groupTargets = getGroupTargets(rolloutGroup, targetIds);
            log.debug("Found {} matching targets for rollout group ID: {}", groupTargets.size(), rolloutGroup.getId());

            // Calculate the updated number of total targets
            int remainingTargets = rolloutGroup.getTotalTargets() - groupTargets.size();
            log.debug("Total targets before update: {}, after removal: {}", rolloutGroup.getTotalTargets(), remainingTargets);

            if (remainingTargets == 0) {
                // Remove the rollout group if no targets remain
                log.debug("No targets remaining for rollout group ID: {}, removing group.", rolloutGroup.getId());
                removeRolloutGroupAndUpdateRollout(rolloutGroup, groupTargets.size());
            } else {
                // Update the rollout group and update the parent rollout
                log.debug("Updating rollout group ID: {} with new total target count: {}", rolloutGroup.getId(), remainingTargets);
                updateRolloutGroupTargets(rolloutGroup, groupTargets, remainingTargets);
            }
        }
    }

    /**
     * Validates the rollout groups.
     *
     * @param rolloutGroups the list of rollout groups to validate
     * @param status        the expected status of the rollout groups
     * @throws ValidationException          if the list of rollout groups is empty
     * @throws RolloutIllegalStateException if any of the rollout groups do not match the expected status
     */
    private void validateRolloutGroups(List<JpaRolloutGroup> rolloutGroups, RolloutGroupStatus status) {
        if (rolloutGroups.isEmpty()) {
            throw new ValidationException("Rollout must have at least one group to proceed");
        }
        final boolean isValidGroupStatus = rolloutGroups.stream().allMatch(rolloutGroup -> status.equals(rolloutGroup.getStatus()));
        if (!isValidGroupStatus) {
            throw new RolloutIllegalStateException("Rollout groups must be in the " + status + " state to proceed, " + "but one or more groups are not");
        }
    }

    /**
     * Retrieves the rollout groups associated with the given rollout ID.
     *
     * @param rolloutId the ID of the rollout
     * @return the list of rollout groups associated with the given rollout ID
     */
    private List<JpaRolloutGroup> getRolloutGroupsByRolloutId(Long rolloutId) {
        return rolloutGroupRepository.findByRolloutId(rolloutId);
    }

    private void saveRolloutStatus(JpaRollout rolloutId) {
        rolloutRepository.save(rolloutId);
    }

    private void saveActionStatus(List<JpaAction> actions) {
        actionRepository.saveAll(actions);
    }

    private List<Long> getRunningRolloutGroups(Long rolloutId) {
        final List<JpaRolloutGroup> rolloutGroups = getRolloutGroupsByRolloutId(rolloutId);
        // Filter the rollout groups to get only those that are in the RUNNING state and collect to a list
        return rolloutGroups.stream().filter(group -> RolloutGroupStatus.RUNNING.equals(group.getStatus())).map(JpaRolloutGroup::getId).toList();
    }

    private List<Long> getPausedRolloutGroups(Long rolloutId) {
        final List<JpaRolloutGroup> rolloutGroups = getRolloutGroupsByRolloutId(rolloutId);
        // Filter the rollout groups to get only those that are in the RUNNING state and collect to a list
        return rolloutGroups.stream().filter(group -> RolloutGroupStatus.PAUSED.equals(group.getStatus())).map(JpaRolloutGroup::getId).toList();
    }

    private List<JpaAction> getRunningActionsByRolloutGroupIds(List<Long> rolloutGroupIds, long rolloutId) {
        List<Long> targetIds = rolloutTargetGroupRepository.getTargetIdsByRolloutGroupIds(rolloutGroupIds);
        List<JpaAction> actionIds = actionRepository.getActionIdsByTargetIdAndRolloutId(targetIds, rolloutId, true);
        return actionIds.stream().filter(action -> DeviceActionStatus.RUNNING.equals(action.getStatus())).toList();
    }

    private List<JpaAction> getPausedActionsByRolloutGroupIds(List<Long> rolloutGroupIds, long rolloutId) {
        List<Long> targetIds = rolloutTargetGroupRepository.getTargetIdsByRolloutGroupIds(rolloutGroupIds);
        List<JpaAction> actionIds = actionRepository.getActionIdsByTargetIdAndRolloutId(targetIds, rolloutId, true);
        return actionIds.stream().filter(action -> DeviceActionStatus.PAUSED.equals(action.getStatus())).toList();
    }

    /**
     * Validates the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateRollout(Rollout rollout) {
        validateName(rollout);
        validateStartDate(rollout);
        validateEndDate(rollout);
        validateMaxUpdateTime(rollout);
        validateLogRequest(rollout);
        validateDownloadTimers(rollout);
        validateDeploymentEstimatedUpdateTime(rollout);
        validateType(rollout);
        validateUpdateAction(rollout);
        validateUpdateActionUninstallVersion(rollout);
    }

    /**
     * Validates name of the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateName(Rollout rollout) {
        if (Objects.isNull(rollout.getName())) {
            log.debug("Rollout name is null");
            throw new ValidationException("Rollout name must not be null");
        }
        if (rollout.getName().isEmpty()) {
            log.debug("Rollout name is empty");
            throw new ValidationException("Rollout name must not be empty");
        }
        if (rollout.getName().contains(" ")) {
            log.debug("Rollout name contains space");
            throw new ValidationException("Rollout name must not contain space");
        }
    }

    /**
     * Validates the log request.
     *
     * @param rollout the rollout to validate
     */
    private void validateLogRequest(Rollout rollout) {

        // Validates that the maximum size of each log file is less than or equal to the total log size divided by the number of files.
        if (rollout.getLogMaxEachFileSize() > (rollout.getLogMaxAllFileSize() / rollout.getLogMaxNumberOfFiles())) {
            throw new ValidationException("Log Max Each File Size (in Kilo Bytes) must be less than or equal to " + "Log Max All File Size (in Kilo Bytes) / Log Max Number of Files");
        }
    }

    /*
     * Validates the end date of the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateEndDate(Rollout rollout) {


        validateRolloutInputDates(rollout);
        Long rolloutEndDate = rollout.getEndAt();

        EpochTimeValidator.validateEpochTimeInSeconds(rolloutEndDate);
        long validEndDateInSeconds = Instant.now().getEpochSecond() + Duration.ofDays(validEndDateOffset).getSeconds();

        log.debug("Valid End date (Min Required in Seconds): {}", validEndDateInSeconds);
        log.debug("Given End Date (Seconds): {}", rolloutEndDate);

        if (rolloutEndDate < validEndDateInSeconds) {
            throw new ValidationException(String.format("End Date (in UTC Time Zone) must be at least %d days ahead in the future", validEndDateOffset));
        }
    }

    /**
     * Validates the start date of the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateStartDate(Rollout rollout) {
        // Validates that the start date is provided if the rollout start type is scheduled.
        if (MgmtRolloutStartType.SCHEDULED.equals(rollout.getStartType())) {
            log.debug("Rollout Start type is scheduled");
            Long startAtTimestamp = rollout.getStartAt();

            if (startAtTimestamp == null) {
                log.debug("Rollout Start date is null");
                throw new ValidationException("Start date is required for scheduled rollouts");
            }

            // Ensure timestamp is in seconds, not milliseconds
            EpochTimeValidator.validateEpochTimeInSeconds(startAtTimestamp);

            // Convert the Long timestamp to LocalDateTime
            LocalDateTime startAt = Instant.ofEpochSecond(startAtTimestamp).atZone(UTC).toLocalDateTime();

            // Validate that the start date is not in the past.
            if (startAt.isBefore(Instant.now().atZone(UTC).toLocalDateTime())) {
                log.debug("Rollout Start date is past date");
                throw new ValidationException("Start Date cannot be in the past");
            }
        }
        if (!MgmtRolloutStartType.SCHEDULED.equals(rollout.getStartType()) && rollout.getStartAt() != null) {
            log.debug("Start Date is only required for SCHEDULED rollout current startType is {}", rollout.getStartType());
            throw new ValidationException("Start Date can be provided only for SCHEDULED rollout and the current rollout startType is " + rollout.getStartType());
        }
    }

    /**
     * Validates the maximum update time of the rollout.
     *
     * @param rollout the rollout to validate
     */
    private void validateMaxUpdateTime(Rollout rollout) {
        if (rollout.getMaxUpdateTime() > getTenantConfigurationMaxUpdateTimeKey()) {
            throw new ValidationException("Max Update Time should not be more than " + getTenantConfigurationMaxUpdateTimeKey() + SECONDS);
        }
        if (rollout.getMaxUpdateTime() < getTenantConfigurationMinUpdateTimeKey()) {
            throw new ValidationException("Max Update Time should not be less than " + getTenantConfigurationMinUpdateTimeKey() + SECONDS);
        }
    }

    /**
     * Sets default values in the rollout request body if they are not provided.
     *
     * @param jpaRollout the jpa rollout body
     */
    private void setDefaultValuesInRollout(JpaRollout jpaRollout) {

        // Set default collection required if not provided
        setDefaultIfNull(jpaRollout::isLogCollectionRequired, jpaRollout::setLogCollectionRequired, getTenantConfigurationCollectionRequired());

        // Set default maximum success VIN if not provided
        setDefaultIfNull(jpaRollout::getLogMaxSuccessVin, jpaRollout::setLogMaxSuccessVin, getTenantConfigurationMaxSuccessVin());

        // Set default maximum failure VIN if not provided
        setDefaultIfNull(jpaRollout::getLogMaxFailureVin, jpaRollout::setLogMaxFailureVin, getTenantConfigurationMaxFailureVin());

        // Set default maximum all file size if not provided
        setDefaultIfNull(jpaRollout::getLogMaxAllFileSize, jpaRollout::setLogMaxAllFileSize, getTenantConfigurationMaxAllFileSize());

        // Set default maximum number of files if not provided
        setDefaultIfNull(jpaRollout::getLogMaxNumberOfFiles, jpaRollout::setLogMaxNumberOfFiles, getTenantConfigurationMaxNumberOfFiles());

        // Set default maximum each file size if not provided
        setDefaultIfNull(jpaRollout::getLogMaxEachFileSize, jpaRollout::setLogMaxEachFileSize, getTenantConfigurationMaxEachFileSize());

        // Set default downgrade allowed if not provided
        setDefaultIfNull(jpaRollout::getDowngradeAllowed, jpaRollout::setDowngradeAllowed, getTenantConfigurationDowngradeAllowed());

        // Set default download retry count if not provided
        setDefaultIfNull(jpaRollout::getDownloadRetryCount, jpaRollout::setDownloadRetryCount, getTenantConfigurationMaxRetryCountKey());

        // Set default max download duration timer if not provided
        setDefaultIfNull(jpaRollout::getMaxDownloadDurationTimer, jpaRollout::setMaxDownloadDurationTimer, getTenantConfigurationMaxDownloadTimerKey());

        // Set default max download wifi duration timer if not provided
        setDefaultIfNull(jpaRollout::getMaxDownloadWifiDurationTimer, jpaRollout::setMaxDownloadWifiDurationTimer, getTenantConfigurationMaxWifiDownloadTimerKey());

        // Set default max download cellular duration timer if not provided
        setDefaultIfNull(jpaRollout::getMaxDownloadCellularDurationTimer, jpaRollout::setMaxDownloadCellularDurationTimer, getTenantConfigurationMaxCellularDownloadTimerKey());

        // Set default max update time if not provided
        setDefaultIfNull(jpaRollout::getMaxUpdateTime, jpaRollout::setMaxUpdateTime, getTenantConfigurationMaxUpdateTimeKey());
    }

    /**
     * Retrieves the downgrade allowed configuration from the tenant configuration.
     *
     * @return the downgrade allowed configuration
     */
    private MgmtRolloutDowngradeAllowed getTenantConfigurationDowngradeAllowed() {
        return tenantConfigurationManagement.getConfigurationValue(TenantConfigurationProperties
                .TenantConfigurationKey.ARTIFACT_DOWNGRADE_ENABLED, Boolean.class).getValue().equals(false)
                ? MgmtRolloutDowngradeAllowed.NO : MgmtRolloutDowngradeAllowed.YES;
    }

    /**
     * Retrieves the maximum size of each log file in bytes for deployment logs from the tenant configuration.
     *
     * @return the maximum size of each log file in bytes
     */
    private Integer getTenantConfigurationMaxEachFileSize() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_MAX_EACH_FILE_SIZE, Integer.class).getValue());
    }

    private Boolean getTenantConfigurationCollectionRequired() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_COLLECTION_REQUIRED, Boolean.class).getValue());
    }

    private Integer getTenantConfigurationMaxSuccessVin() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_MAX_SUCCESS_VIN, Integer.class).getValue());
    }

    private Integer getTenantConfigurationMaxFailureVin() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_MAX_FAILURE_VIN, Integer.class).getValue());
    }

    private Integer getTenantConfigurationMaxAllFileSize() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_MAX_ALL_FILE_SIZE, Integer.class).getValue());
    }

    private Integer getTenantConfigurationMaxNumberOfFiles() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_LOG_MAX_NUMBER_OF_FILES, Integer.class).getValue());
    }

    /**
     * Retrieves the maximum download duration timer from the tenant configuration.
     *
     * @return the maximum download duration timer
     */
    private Integer getTenantConfigurationMaxDownloadTimerKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    /**
     * Retrieves the maximum WiFi download duration timer from the tenant configuration.
     *
     * @return the maximum WiFi download duration timer
     */
    private Integer getTenantConfigurationMaxWifiDownloadTimerKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_WIFI_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    /**
     * Retrieves the maximum cellular download duration timer from the tenant configuration.
     *
     * @return the maximum cellular download duration timer
     */
    private Integer getTenantConfigurationMaxCellularDownloadTimerKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_MAX_CELLULAR_DOWNLOAD_DURATION_TIMER, Integer.class).getValue());
    }

    /**
     * Retrieves the maximum retry count for artifact downloads from the tenant configuration.
     *
     * @return the maximum retry count
     */
    private Integer getTenantConfigurationMaxRetryCountKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.ARTIFACT_DOWNLOAD_RETRY_COUNT, Integer.class).getValue());
    }

    /**
     * Retrieves the maximum update time from the tenant configuration.
     *
     * @return the maximum update time
     */
    private Integer getTenantConfigurationMaxUpdateTimeKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_MAX_UPDATE_TIME, Integer.class).getValue());
    }

    /**
     * Retrieves the minimum update time from the tenant configuration.
     *
     * @return the minimum update time
     */
    private Integer getTenantConfigurationMinUpdateTimeKey() {
        return systemSecurityContext.runAsSystem(() -> tenantConfigurationManagement
                .getConfigurationValue(TenantConfigurationProperties.TenantConfigurationKey.DEPLOYMENT_MIN_UPDATE_TIME, Integer.class).getValue());
    }

    /**
     * Helper method to set a default value if the current value is null.
     *
     * @param getter       the getter method
     * @param setter       the setter method
     * @param defaultValue the default value to set
     * @param <T>          the type of the value
     */
    private <T> void setDefaultIfNull(Supplier<T> getter, Consumer<T> setter, T defaultValue) {
        if (getter.get() == null) {
            setter.accept(defaultValue);
        }
    }

    /**
     * @param rollout Rollout to create target filter query
     * @param tag     tag to build target filter query
     * @return TargetFilterQuery
     */
    private TargetFilterQuery getTargetFilterQuery(Rollout rollout, TargetTag tag) {
        JpaTargetFilterQuery targetFilter = new JpaTargetFilterQuery();
        targetFilter.setName(rollout.getName());
        targetFilter.setQuery("tag==" + tag.getName());
        targetFilter.setTenant(rollout.getTenant());
        return targetFilterQueryManagement.create(targetFilter);
    }

    /**
     * Create target filter with 'tag==tag_name' and assign it to the rollout
     *
     * @param targetFilter to create a target filter
     * @param rollout      to associate the target filter with
     * @return updated rollout
     */
    private Rollout assignTargetFilterToRollout(TargetFilterQuery targetFilter, Rollout rollout) {
        targetFilterQueryManagement.verifyTargetFilterQuerySyntax(targetFilter.getQuery());
        return addTargetFilterQuery(rollout, targetFilter.getQuery());
    }

    /**
     * Validates the provided software module request.
     * Ensures the existence of the software module and its association with the target version.
     * Throws appropriate exceptions if validation fails.
     */
    public void validateSoftwareModuleAssociation(List<MgmtSoftwareModuleRequest> sms) {
        sms.forEach(sm -> {
            log.debug("Initiating validation: Fetching Software Module with ID: {} for association with Target Version ID: {}", sm.getModuleId(), sm.getSoftwareVersionTargetId());
            ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation = artifactsManagement.findFirstBySoftwareModuleIdAndTargetVersionId(sm.getModuleId(), sm.getSoftwareVersionTargetId()).orElseThrow(() -> new ValidationException(String.format("Software Module %s Target Version %s is not assigned with any artifact.", sm.getModuleId(), sm.getSoftwareVersionTargetId())));
            if (!artifactSoftwareModuleAssociation.getArtifact().getFileStatus().equals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL)) {
                throw new ValidationException(String.format("Artifact for the module %s on target version %s is not uploaded to CDN.", sm.getModuleId(), sm.getSoftwareVersionTargetId()));
            }
            log.debug("Validation successful for Software Module ID: {} and Software Version Target ID: {}", artifactSoftwareModuleAssociation.getSoftwareModule().getId(), artifactSoftwareModuleAssociation.getTargetVersion().getId());
        });

    }

    /**
     * Determines the appropriate handling for the distribution set association.
     * If no distribution set is found, creates one. Otherwise, processes the existing set.
     */
    @Override
    public void associateSoftwareModulesToVersion(Long tenantId, Rollout rollout, List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        log.info("Initiating the association of a distribution set to rollout with ID: {} for tenantId: {}", rollout.getId(), tenantId);

        if (isDistributionSetMissing(rollout)) {
            handleMissingDistributionSetLogic(tenantId, softwareModuleRequests, rollout);
        } else {
            handleExistingDistributionSetLogic(tenantId, softwareModuleRequests, rollout);
        }

        log.info("Completed the association process for a distribution set with rollout ID: {} for tenantId: {}", rollout.getId(), tenantId);
    }

    /**
     * Handles the logic when no distribution set is associated with a rollout.
     * Creates and associates a new distribution set if none is found, or updates the rollout with an existing set.
     *
     * @param tenantId               the ID of the tenant
     * @param softwareModuleRequests the request containing software module association details
     * @param rollout                the {@link Rollout} object for which the distribution set is managed
     */
    private void handleMissingDistributionSetLogic(Long tenantId, List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {
        boolean noAssociationsFound = true;
        IDistributionSetModule foundDistributionSet = findFirstMatchingDistributionSet(softwareModuleRequests);

        if (foundDistributionSet != null) {
            noAssociationsFound = false;
        }

        if (noAssociationsFound) {
            log.info("No distribution set is currently associated with rollout ID: {}. Creating and associating a new distribution set.", rollout.getId());
            handleMissingDistributionSet(tenantId, softwareModuleRequests, rollout);
            log.info("Successfully created and associated a new distribution set for rollout ID: {}", rollout.getId());
        } else {
            updateRolloutDistributionSet(rollout, foundDistributionSet.getDsSet());
            entityManager.flush();
            entityManager.clear();
        }
    }

    /**
     * Finds the first matching distribution set for the provided software module association request.
     * Iterates through the modules and retrieves the first distribution set matching the module ID and target version ID.
     *
     * @param softwareModuleRequests the request containing software module association details
     * @return the first matching {@link IDistributionSetModule}, or null if none are found
     */
    private IDistributionSetModule findFirstMatchingDistributionSet(List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        for (MgmtSoftwareModuleRequest module : softwareModuleRequests) {
            List<IDistributionSetModule> distributionSetModules = distributionSetManagement.findDsModuleBySoftwareModuleIdAndTargetVersionId(
                    module.getModuleId(),
                    module.getSoftwareVersionTargetId()
            ).stream().toList();

            if (!distributionSetModules.isEmpty()) {
                return distributionSetModules.get(0);
            }
        }
        return null;
    }

    /**
     * Updates the distribution set associated with a rollout.
     *
     * @param rollout         the rollout to update
     * @param distributionSet the new distribution set to associate with the rollout
     * @return the updated rollout entity
     * @throws EntityNotFoundException if the specified rollout is not found
     */
    @Override
    @Transactional
    @Retryable(include = {ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public Rollout updateRolloutDistributionSet(Rollout rollout, DistributionSet distributionSet) {
        final JpaRollout rolloutUpdated = getRolloutAndThrowExceptionIfNotFound(rollout.getId());
        rolloutUpdated.setDistributionSet(distributionSet);
        return rolloutRepository.save(rolloutUpdated);
    }

    /**
     * Checks if the rollout is missing an associated distribution set.
     *
     * @return true if no distribution set is found, false otherwise
     */
    private boolean isDistributionSetMissing(Rollout rollout) {
        return Objects.isNull(rollout.getDistributionSet());
    }

    /**
     * Handles cases where the rollout is missing a distribution set.
     * Creates a new set and associates it with the provided software modules.
     */
    private void handleMissingDistributionSet(Long tenantId, List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {
        log.debug("Handling missing distribution set for rollout ID: {} and tenant ID: {}", rollout.getId(), tenantId);

        log.debug("Creating and assigning a new distribution set for rollout ID: {}", rollout.getId());
        createAndAssignDistributionSet(tenantId, softwareModuleRequests, rollout);
        log.debug("Successfully created and assigned a new distribution set for rollout ID: {}", rollout.getId());

        log.debug("Associating software modules to rollout ID: {}", rollout.getId());
        log.debug("Successfully associated software modules to rollout ID: {}", rollout.getId());
    }

    /**
     * Handles cases where a distribution set is already associated with the rollout.
     * Validates whether the set is shared with other rollouts and processes accordingly.
     *
     * @param tenantId               the ID of the tenant
     * @param softwareModuleRequests the list of software module requests to associate with the rollout
     * @param rollout                the rollout being processed
     */
    private void handleExistingDistributionSetLogic(Long tenantId, List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {
        log.debug("Starting logic to handle existing distribution set for rollout ID: {} and tenant ID: {}", rollout.getId(), tenantId);

        log.debug("Fetching existing draft rollouts for rollout ID: {}", rollout.getId());
        List<Rollout> existingDraftRollouts = findDraftRollouts(rollout);
        log.debug("Fetched {} draft rollouts for rollout ID: {}", existingDraftRollouts.size(), rollout.getId());

        if (isSingleDraftRollout(existingDraftRollouts)) {
            log.debug("Single draft rollout detected for rollout ID: {}. Proceeding with module association.", rollout.getId());
            associateModulesWithRollout(softwareModuleRequests, rollout);
            log.debug("Modules successfully associated with rollout ID: {}", rollout.getId());
        } else if (isMultiDraftRollouts(existingDraftRollouts)) {
            log.debug("Multiple draft rollouts detected for rollout ID: {}. Evaluating the need for a new distribution set.", rollout.getId());

            DistributionSet oldDistributionSet = rollout.getDistributionSet();
            if (checkNeedCreateNewDistSet(oldDistributionSet, softwareModuleRequests)) {
                log.debug("Creating a new distribution set for rollout ID: {}.", rollout.getId());
                DistributionSet newDistributionSet = createAndAssignDistributionSet(tenantId, softwareModuleRequests, rollout);
                log.debug("Successfully created and assigned new distribution set with ID: {} for rollout ID: {}", newDistributionSet.getId(), rollout.getId());

                log.debug("Reassociating old modules with the new distribution set for rollout ID: {}", rollout.getId());
                List<MgmtSoftwareModuleRequest> oldAssociations = associateOldModules(oldDistributionSet, newDistributionSet);
                softwareModuleRequests.addAll(oldAssociations);

                log.debug("Associating software modules with rollout ID: {}", rollout.getId());
                associateModulesWithRollout(softwareModuleRequests, rollout);
                log.debug("All modules successfully associated with rollout ID: {}", rollout.getId());
            }
        }
    }

    /**
     * Checks whether a new distribution set is required based on the provided old distribution set
     * and the list of software module requests.
     *
     * @param oldDistributionSet     the existing distribution set associated with the rollout
     * @param softwareModuleRequests the list of software module requests to be evaluated
     * @return true if a new distribution set is needed, false otherwise
     */
    private boolean checkNeedCreateNewDistSet(DistributionSet oldDistributionSet, List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        log.debug("Evaluating necessity for creating a new distribution set for distribution set ID: {}", oldDistributionSet.getId());
        for (MgmtSoftwareModuleRequest module : softwareModuleRequests) {
            log.debug("Checking module with ID: {} and target version ID: {} against the old distribution set ID: {}.", module.getModuleId(), module.getSoftwareVersionTargetId(), oldDistributionSet.getId());

            IDistributionSetModule dsModule = distributionSetManagement.findByDsIdAndSoftwareModuleIdAndTargetVersionId(oldDistributionSet.getId(), module.getModuleId(), module.getSoftwareVersionTargetId());

            if (Objects.isNull(dsModule)) {
                log.debug("Module with ID: {} requires a new distribution set.", module.getModuleId());
                return true;
            }
        }
        log.debug("No new distribution set required for the provided modules.");
        return false;
    }

    /**
     * Creates and assigns a new distribution set to the rollout.
     */
    private DistributionSet createAndAssignDistributionSet(Long tenantId, List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {
        log.debug("Starting the process to create and assign a new distribution set for rollout ID: {} and tenant ID: {}", rollout.getId(), tenantId);

        log.debug("Creating new distribution set for tenant ID: {}", tenantId);
        DistributionSet newDistributionSet = createDistributionSet(tenantId, softwareModuleRequests);
        log.debug("Successfully created new distribution set with ID: {}", newDistributionSet.getId());

        log.debug("Assigning the new distribution set (ID: {}) to rollout ID: {}", newDistributionSet.getId(), rollout.getId());
        updateRolloutDistributionSet(rollout, newDistributionSet);
        entityManager.flush();
        entityManager.clear();
        log.debug("Successfully assigned distribution set (ID: {}) to rollout ID: {}", newDistributionSet.getId(), rollout.getId());
        return newDistributionSet;
    }

    /**
     * Finds rollouts in DRAFT status that share the same distribution set as the given rollout.
     */
    private List<Rollout> findDraftRollouts(Rollout rollout) {
        return findByDistributionSetIdAndStatus(Collections.singletonList(rollout.getDistributionSet()), List.of(RolloutStatus.DRAFT));
    }

    /**
     * Retrieves a list of rollouts based on the provided distribution sets and statuses.
     *
     * @param distributionSets the list of distribution sets to filter rollouts
     * @param status           the collection of statuses to filter rollouts
     * @return a list of rollouts matching the provided distribution sets and statuses
     */
    @Override
    public List<Rollout> findByDistributionSetIdAndStatus(List<DistributionSet> distributionSets, List<RolloutStatus> status) {
        return rolloutRepository.findByDistributionSetInAndStatusIn(distributionSets, status);
    }

    /**
     * Determines if the current distribution set is shared with only one draft rollout.
     *
     * @return true if only one draft rollout is found, false otherwise
     */
    private boolean isSingleDraftRollout(List<Rollout> draftRollouts) {
        return draftRollouts.size() == 1;
    }

    /**
     * Determines if a new distribution set should be created based on the number of draft rollouts sharing the current set.
     *
     * @return true if multiple rollouts share the same set, false otherwise
     */
    private boolean isMultiDraftRollouts(List<Rollout> draftRollouts) {
        return draftRollouts.size() > 1;
    }

    /**
     * Creates a new distribution set from the provided request.
     */
    private DistributionSet createDistributionSet(Long tenantId, List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        MgmtDistributionSetRequestBodyPost distributionSetRequest = createDistributionSetRequest(softwareModuleRequests);
        List<MgmtDistributionSetRequestBodyPost> distributionSetList = List.of(distributionSetRequest);
        return createDS(tenantId, distributionSetList);
    }

    /**
     * Creates a distribution set request object from the provided module association request.
     */
    private MgmtDistributionSetRequestBodyPost createDistributionSetRequest(List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        MgmtDistributionSetRequestBodyPost set = new MgmtDistributionSetRequestBodyPost();
        set.setName(generateUniqueName("DistributionSet_"));
        set.setDescription(generateUniqueName("DistributionSet_"));
        set.setRequiredMigrationStep(false);
        set.setSoftwareDowngradeEnabled(false);
        set.setType("os");

        List<MgmtSoftwareModuleAssignments> modules = mapModulesFromRequest(softwareModuleRequests);
        set.setModules(modules);

        return set;
    }

    /**
     * Generates a unique name using a given prefix and a random UUID.
     */
    private String generateUniqueName(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    /**
     * Maps software module details from the request to the assignment objects for a distribution set.
     */
    private List<MgmtSoftwareModuleAssignments> mapModulesFromRequest(List<MgmtSoftwareModuleRequest> softwareModuleRequests) {
        return softwareModuleRequests.stream().map(module -> {
            MgmtSoftwareModuleAssignments assignment = new MgmtSoftwareModuleAssignments();
            assignment.setId(module.getModuleId());
            assignment.setSoftwareVersionTargetId(module.getSoftwareVersionTargetId().toString());
            return assignment;
        }).toList();
    }

    /**
     * Creates a distribution set from a list of distribution set requests.
     */
    private DistributionSet createDS(long tenantId, List<MgmtDistributionSetRequestBodyPost> sets) {
        log.debug("Starting the creation of Distribution Set for tenant ID: {}", tenantId);
        log.debug("Assigning default distribution set type to the provided sets.");
        assignDefaultDistributionSetType(sets);
        log.debug("Validating the distribution sets.");
        validateDistributionSets(sets);
        log.debug("Creating distribution sets from the provided request.");

        Collection<DistributionSet> createdDSets = createDistributionSets(sets);

        if (createdDSets.isEmpty()) {
            throw new IllegalStateException("No Distribution Set was created!");
        }

        log.debug("Successfully created distribution sets.");
        return createdDSets.stream().toList().get(0);
    }

    /**
     * Attempts to create a distribution set (DistributionSet) through polling, periodically checking if the creation
     * was successful. The method performs a maximum of 10 seconds of retries, with a polling interval of 500 milliseconds
     * between each attempt.
     *
     * @param sets The list of request objects for creating the distribution sets. Each item in the list represents a request
     *             to create a new distribution set.
     * @return A collection of {@link DistributionSet} objects that were created if the creation was successful.
     * @throws IllegalStateException If the timeout of 10 seconds is exceeded without successfully creating the distribution
     *                               set.
     * @throws InterruptedException  If the thread is interrupted while waiting (sleeping) during polling.
     */
    public Collection<DistributionSet> createDistributionSets(List<MgmtDistributionSetRequestBodyPost> sets) {
        long timeout = 10000;
        long pollingInterval = 500;
        long startTime = System.currentTimeMillis();
        String errorMessage = "";

        Collection<DistributionSet> createdDSets;
        List<DistributionSetCreate> request = dsFromRequest(sets, entityFactory);

        do {
            try {
                createdDSets = distributionSetManagement.create(request);
            } catch (Exception e) {
                log.error("Error creating Distribution Set: {}", e.getMessage());
                createdDSets = null;
                errorMessage = e.getMessage();
            }

            if (createdDSets != null && !createdDSets.isEmpty()) {
                return createdDSets;
            }

            try {
                Thread.sleep(pollingInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted while waiting for Distribution Set creation", e);
            }
        } while (System.currentTimeMillis() - startTime < timeout);

        throw new ValidationException("Timeout reached while waiting for Distribution Set creation: " + errorMessage);
    }

    /**
     * Assigns a default type to distribution sets that do not specify a type.
     */
    private void assignDefaultDistributionSetType(List<MgmtDistributionSetRequestBodyPost> sets) {
        String defaultDsKey = systemSecurityContext.runAsSystem(systemManagement.getTenantMetadata().getDefaultDsType()::getKey);
        sets.stream().filter(ds -> ds.getType() == null).forEach(ds -> ds.setType(defaultDsKey));
    }

    /**
     * Validates the properties of each distribution set in the list.
     */
    private void validateDistributionSets(List<MgmtDistributionSetRequestBodyPost> sets) {
        log.debug("Starting validation of distribution sets.");
        for (MgmtDistributionSetRequestBodyPost ds : sets) {
            log.debug("Validating distribution set type");
            validateDistributionSetType(ds);
        }
    }

    /**
     * Validates the type of a distribution set to ensure it is not deleted.
     */
    private void validateDistributionSetType(MgmtDistributionSetRequestBodyPost ds) {
        log.debug("Validating Distribution Set type with key: {}", ds.getType());

        Optional<DistributionSetType> opt = distributionSetTypeManagement.getByKey(ds.getType());

        opt.ifPresent(dsType -> {
            if (dsType.isDeleted()) {
                String message = MessageFormat.format("Cannot create Distribution Set from type with key {0}. Distribution Set Type already deleted!", dsType.getKey());
                log.error("Validation failed: {}", message);
                throw new ValidationException(message);
            }
        });
    }

    /**
     * Associates a list of software modules with the specified rollout.
     * If an association between the rollout and a module with the given target version does not already exist,
     * it retrieves the required software module and version information, then creates the association.
     *
     * @param softwareModuleRequests the request containing the modules and their target versions to associate
     * @param rollout                the rollout with which the modules should be associated
     */
    private void associateModulesWithRollout(List<MgmtSoftwareModuleRequest> softwareModuleRequests, Rollout rollout) {
        log.info("Starting association of software modules with rollout ID: {}", rollout.getId());

        softwareModuleRequests.forEach(module -> createDistributionSetModule(module, rollout));

        log.info("Completed association of software modules with rollout ID: {}", rollout.getId());
    }

    /**
     * Associates a list of software modules with the specified rollout.
     * If an association between the rollout and a module with the given target version does not already exist,
     * it retrieves the required software module and version information, then creates the association.
     *
     * @param oldDistributionSet this is the old rollout
     * @param newDistributionSet the distribution set with which the modules should be associated
     */
    private List<MgmtSoftwareModuleRequest> associateOldModules(DistributionSet oldDistributionSet, DistributionSet newDistributionSet) {
        log.info("Starting old association of software modules with distributionSet ID: {}", oldDistributionSet.getId());

        List<IDistributionSetModule> oldDsModules = distributionSetManagement.getDistributionSetModule(oldDistributionSet.getId());


        if (oldDsModules.isEmpty()) {
            log.warn("No distribution set modules found for Id {}", oldDistributionSet.getId());
        }

        List<MgmtSoftwareModuleRequest> softwareModuleRequests = oldDsModules.stream()
                .map(module -> new MgmtSoftwareModuleRequest(module.getSm().getId(), module.getVersion().getId()))
                .toList();
        log.info("Completed association of software modules with ID: {}", newDistributionSet.getId());

        return softwareModuleRequests;
    }

    /**
     * Creates a distribution set module for a given software module and rollout.
     * If the distribution set exists, it checks if the association already exists. If not, it creates the association.
     *
     * @param module  the software module to be associated
     * @param rollout the rollout containing the distribution set
     */
    private void createDistributionSetModule(MgmtSoftwareModuleRequest module, Rollout rollout) {

        if (rollout.getDistributionSet().getId() != null) {
            log.debug("Distribution set ID found for rollout: {}", rollout.getDistributionSet().getId());

            IDistributionSetModule distributionSetModules = distributionSetManagement.findByDsIdAndSoftwareModuleIdAndTargetVersionId(rollout.getDistributionSet().getId(), module.getModuleId(), module.getSoftwareVersionTargetId());

            if (Objects.isNull(distributionSetModules)) {
                log.debug("No existing distribution set module found for module ID: {} and target version ID: {}. Creating new module association.", module.getModuleId(), module.getSoftwareVersionTargetId());

                createDSModule(rollout.getDistributionSet(), module.getModuleId(), module.getSoftwareVersionTargetId());

                log.debug("Created new distribution set module for module: {} with target version: {} under distribution set ID: {}", module.getModuleId(), module.getSoftwareVersionTargetId(), rollout.getDistributionSet().getId());
            } else {
                log.debug("Distribution set module already exists for module ID: {} and target version ID: {}. No new module created.", module.getModuleId(), module.getSoftwareVersionTargetId());
            }
        } else {
            log.warn("No DistributionSet ID found for rollout: {}", rollout.getId());
        }
    }

    /**
     * Creates a new association between a distribution set, a software module, and a version.
     * <p>
     * This method retrieves the specified software module and target version by their IDs.
     * If either is not found, it throws an `EntityNotFoundException`. Once both entities are
     * retrieved, it creates a new `DistributionSetModule` object that links the provided
     * distribution set, software module, and version. The new association is then persisted
     * in the database.
     * <p>
     * Logging is used to track the process, including errors for missing entities and
     * successful retrievals.
     *
     * @param distributionSet the distribution set to associate with the software module and version
     * @param smId            the ID of the software module to retrieve and associate
     * @param versionId       the ID of the version to retrieve and associate
     * @throws EntityNotFoundException if the software module or version with the specified IDs is not found
     */
    private void createDSModule(DistributionSet distributionSet, Long smId, Long versionId) {
        SoftwareModule softwareModule = softwareModuleManagement.get(smId).orElseThrow(() -> {
            log.error("Software module with ID {} not found", smId);
            return new EntityNotFoundException(SoftwareModule.class, smId);
        });

        Version targetVersion = versionManagement.getById(versionId).orElseThrow(() -> {
            log.error("Version with ID {} not found", versionId);
            return new EntityNotFoundException(Version.class, versionId);
        });

        log.debug("Found software module: {} and version: {}. Proceeding to create DistributionSetModule.", softwareModule.getId(), targetVersion.getId());

        DistributionSetModule distributionSetModule = new DistributionSetModule(
                (JpaDistributionSet) distributionSet,
                (JpaSoftwareModule) softwareModule,
                (JpaVersion) targetVersion
        );

        distributionSetManagement.createDistributionSetModule(distributionSetModule);
        entityManager.flush();
        entityManager.clear();
    }

    /**
     * Create rollout groups with default conditions
     *
     * @param rollout to associate the group with
     * @return list of created groups with associations
     */
    private List<AssociatedTargetsToRolloutGroup> createAndAddGroupsWithDefaultValues(Rollout rollout) {

        RolloutGroup createdRolloutGroup = rolloutGroupManagement.createDefaultRolloutGroup(rollout);

        entityManager.flush(); //Persist the groups before assigning targets

        return rolloutGroupManagement.assignTargetsToRolloutGroups(rollout, List.of(createdRolloutGroup));
    }

    /**
     * Create rollout groups with provided grouping conditions
     *
     * @param rollout       to associate the group with
     * @param groups        rollout group details
     * @param controllerIds list of provided controllerIds to be added in groups
     * @return list of created groups with associations
     */
    private List<AssociatedTargetsToRolloutGroup> createAndAddGroupsWithGroupConditions(Rollout rollout, List<MgmtRolloutGroup> groups, List<String> controllerIds) {
        List<RolloutGroup> rolloutGroups = groups.stream().map(JpaRolloutManagement::getRolloutGroupsFromRequest).toList();
        List<RolloutGroup> createdRolloutGroups = rolloutGroupManagement.createRolloutGroups(rolloutGroups, rollout, controllerIds);

        entityManager.flush(); //Persist the groups before assigning targets

        return rolloutGroupManagement.assignTargetsToRolloutGroups(rollout, createdRolloutGroups);
    }

    /**
     * @param rolloutId to check associated controller ids
     * @return associated controller ids
     */
    @Override
    public List<String> getControllerIdsByRolloutId(long rolloutId) {
        log.info("fetching the list of Rollout associated controller ids");
        List<Target> targets = new ArrayList<>();
        rolloutGroupRepository.findByRolloutId(rolloutId).forEach(rolloutGroup -> targets
                .addAll(targetManagement.get(rolloutTargetGroupRepository.getTargetIdsByRolloutGroupIds(Collections.singletonList(rolloutGroup.getId())))));
        return targets.stream().map(Target::getControllerId).toList();
    }

    /**
     * Updates the rollout with the provided details.
     * Validates the rollout and updates its properties based on the request.
     * If the distribution set is provided, it checks for software downgrade permissions.
     *
     * @param rollout        The existing rollout to update.
     * @param rolloutRequest The new details for the rollout.
     * @return The updated rollout entity.
     */

    @Override
    public Rollout updateRollout(Rollout rollout, Rollout rolloutRequest) {
        JpaRollout jpaRollout = (JpaRollout) rollout;
        checkIfDeleted(jpaRollout.getId(), jpaRollout.getStatus());
        log.debug("Validating rollout: {}", rollout.getId());

        if (rolloutRequest.getDistributionSet() != null) {
            final DistributionSet set = distributionSetManagement.getValidAndComplete(rolloutRequest.getDistributionSet().getId());
            if (!isTenantDistributionSoftwareDowngradeEnabled() && set.isSoftwareDowngradeEnabled()) {
                throw new ValidationException("Current tenant's configuration does not allow software downgrade.");
            }
            jpaRollout.setDistributionSet(set);
        }
        log.debug("Updating rollout: {}", rollout.getId());
        updateRolloutDetails(jpaRollout, rolloutRequest);

        if (rolloutRequest.getVehicleLogLevel() != null) {
            jpaRollout.setVehicleLogLevel(rolloutRequest.getVehicleLogLevel());
            setVehicleLogLevel(jpaRollout);
        }

        if (rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
            jpaRollout.setApprovalDecidedBy(null);
            jpaRollout.setApprovalRemark(null);
        }
        validateRollout(rollout);
        return rolloutRepository.save(jpaRollout);
    }

    /**
     * Updates the details of the rollout based on the provided update object.
     * This method sets various properties of the rollout, including description, priority, user acceptance requirements,
     * connectivity type, weight, start and end times, required state of charge, log collection requirements, and more.
     *
     * @param rollout The rollout to be updated.
     * @param update  The object containing the new values for the rollout.
     */

    private void updateRolloutDetails(JpaRollout rollout, Rollout update) {
        Optional.ofNullable(update.getDescription()).ifPresent(rollout::setDescription);
        Optional.ofNullable(update.getPriority()).ifPresent(rollout::setPriority);
        Optional.ofNullable(update.getUserAcceptanceRequired()).ifPresent(rollout::setUserAcceptanceRequired);
        Optional.ofNullable(update.getConnectivityType()).ifPresent(rollout::setConnectivityType);
        update.getWeight().ifPresent(rollout::setWeight);
        Optional.ofNullable(update.getStartType()).ifPresent(rollout::setStartType);
        Optional.ofNullable(update.getStartAt()).ifPresent(rollout::setStartAt);
        Optional.ofNullable(update.getEndAt()).ifPresent(rollout::setEndAt);
        Optional.ofNullable(update.getDeploymentEstimatedUpdateTime()).ifPresent(rollout::setDeploymentEstimatedUpdateTime);
        Optional.ofNullable(update.getRequiredStateOfCharge())
                .ifPresentOrElse(val -> {
                    String trimmed = val.trim();
                    if (!trimmed.equals(EMPTY_JSON)) {
                        rollout.setRequiredStateOfCharge(trimmed);
                    } else {
                        rollout.setRequiredStateOfCharge(null);
                    }
                }, () -> rollout.setRequiredStateOfCharge(null));
        Optional.ofNullable(update.isLogCollectionRequired()).ifPresent(rollout::setLogCollectionRequired);
        Optional.ofNullable(update.getLogMaxSuccessVin()).ifPresent(rollout::setLogMaxSuccessVin);
        Optional.ofNullable(update.getLogMaxFailureVin()).ifPresent(rollout::setLogMaxFailureVin);
        Optional.ofNullable(update.getLogMaxEachFileSize()).ifPresent(rollout::setLogMaxEachFileSize);
        Optional.ofNullable(update.getLogMaxAllFileSize()).ifPresent(rollout::setLogMaxAllFileSize);
        Optional.ofNullable(update.getLogMaxNumberOfFiles()).ifPresent(rollout::setLogMaxNumberOfFiles);
        Optional.ofNullable(update.getDownloadRetryCount()).ifPresent(rollout::setDownloadRetryCount);
        Optional.ofNullable(update.getMaxDownloadDurationTimer()).ifPresent(rollout::setMaxDownloadDurationTimer);
        Optional.ofNullable(update.getMaxDownloadWifiDurationTimer()).ifPresent(rollout::setMaxDownloadWifiDurationTimer);
        Optional.ofNullable(update.getMaxDownloadCellularDurationTimer()).ifPresent(rollout::setMaxDownloadCellularDurationTimer);
        Optional.ofNullable(update.getRequiredMedia()).ifPresent(rollout::setRequiredMedia);
        Optional.ofNullable(update.getDowngradeAllowed()).ifPresent(rollout::setDowngradeAllowed);
        Optional.ofNullable(update.getMaxUpdateTime()).ifPresent(rollout::setMaxUpdateTime);
        Optional.ofNullable(update.getMaxPackageSize()).ifPresent(rollout::setMaxPackageSize);
        Optional.ofNullable(update.getVehicleLogLevel()).ifPresent(rollout::setVehicleLogLevel);
        rollout.setType(update.getType());
        rollout.setUpdateAction(update.getUpdateAction());
        rollout.setUpdateActionUninstallVersion(update.getUpdateActionUninstallVersion() == null ? null : String.join(",", update.getUpdateActionUninstallVersion()));
    }


    /**
     * Deletes a rollout group and target groups from a specified rollout and rolloutGroup. If the group has associated target IDs,
     * those are removed before the group is deleted. Also, updates the rollout's total target count to 0.
     *
     * @param rollout      The Rollout from which the group will be deleted.
     * @param rolloutGroup The RolloutGroup to be deleted.
     */
    @Override
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteRolloutGroupTargets(final Rollout rollout, final RolloutGroup rolloutGroup) {
        log.debug("Deleting group with ID: {}", rolloutGroup.getId());
        deleteRolloutTargetGroup(rolloutGroup);

        log.debug("Deleting RolloutGroup ID: {}", rolloutGroup.getId());
        rolloutGroupRepository.deleteByIds(Collections.singletonList(rolloutGroup.getId()));

    }

    /**
     * Deletes the targets associated with the given RolloutGroup.
     * <p>
     * This method retrieves the target IDs linked to the specified rollout group
     * and removes any existing associations, including related tags.
     * If no targets are found, the method exits without performing any operations.
     * </p>
     *
     * @param rolloutGroup The RolloutGroup whose associated targets will be deleted.
     */
    private void deleteRolloutTargetGroup(final RolloutGroup rolloutGroup) {
        List<Long> targetIds = Optional.ofNullable(
                rolloutTargetGroupRepository.getTargetIdsByRolloutGroupIds(Collections.singletonList(rolloutGroup.getId()))
        ).orElse(Collections.emptyList());

        deleteTargetTargetTags(targetIds, rolloutGroup);

    }

    /**
     * Removes associations between targets and a rollout group, as well as specific tags related to the rollout.
     * <p>
     * This method ensures that targets are disassociated from the provided rollout group by:
     * <ul>
     *     <li>Checking if the provided target IDs list is empty; if so, exits early.</li>
     *     <li>Retrieving the rollout group name and its corresponding tag.</li>
     *     <li>Removing the association between the targets and the related tag if it exists.</li>
     *     <li>Logging the completion of the deletion process.</li>
     * </ul>
     * This operation is transactional and supports automatic retry in case of concurrency failures.
     *
     * @param targetIds    List of target IDs from which associations and tags should be removed.
     * @param rolloutGroup The rollout group whose associations need to be deleted.
     */
    @Transactional
    @Retryable(include = {
            ConcurrencyFailureException.class}, maxAttempts = Constants.TX_RT_MAX, backoff = @Backoff(delay = Constants.TX_RT_DELAY))
    public void deleteTargetTargetTags(List<Long> targetIds, RolloutGroup rolloutGroup) {
        String nameRollout = rolloutGroup.getRollout().getName();
        Optional<TargetTag> targetTag = targetTagManagement.getByName(nameRollout);
        if (!targetIds.isEmpty() && targetTag.orElse(null) != null) {
            targetTargetTagRepository.deleteByTargetIdInAndTagId(targetIds, targetTag.get().getId());
        }
        log.debug("Deletion process completed for RolloutGroup ID: {}", rolloutGroup.getId());
    }


    /**
     * Calculates and updates the total number of targets for a given rollout.
     * <p>
     * This method retrieves all {@link RolloutGroup} entities associated with the rollout,
     * sums the number of targets for each group, and updates the total target count in the rollout entity.
     * </p>
     * <p>
     * The update is performed in a single transaction to ensure data consistency.
     * </p>
     *
     * @param rollout The {@link Rollout} entity whose total targets need to be updated.
     */
    @Override
    @Transactional
    public void updateTotalTargetsForRollout(Rollout rollout) {
        log.debug("Fetching all rollout groups for rollout ID: {}", rollout.getId());

        Page<RolloutGroup> totalGroups = rolloutGroupManagement.findByRolloutWithDetailedStatus(Pageable.unpaged(), rollout.getId());
        log.debug("Fetched {} rollout groups for rollout ID: {}", totalGroups.getTotalElements(), rollout.getId());

        long totalTarget = totalGroups.getContent().stream()
                .mapToLong(group -> rolloutGroupManagement.countTargetsOfRolloutsGroup(group.getId()))
                .sum();
        log.debug("Calculated total targets for rollout ID {}: {}", rollout.getId(), totalTarget);

        long groupsCreated = rolloutGroupRepository.countByRolloutId(rollout.getId());
        log.debug("Number of groups created for rollout ID {}: {}", rollout.getId(), groupsCreated);

        JpaRollout jpaRollout = (JpaRollout) rollout;
        jpaRollout.setTotalTargets(totalTarget);
        jpaRollout.setRolloutGroupsCreated((int) groupsCreated);

        rolloutRepository.save(jpaRollout);
        log.debug("Updated total targets and groups created for Rollout ID: {}", jpaRollout.getId());
    }

    @Override
    @Transactional
    public long handleEndRollouts() {
        Long nowInSeconds = Instant.now().atZone(ZoneId.of("UTC")).toEpochSecond();
        log.debug("Current time in seconds: {}", nowInSeconds);

        log.debug("Updating active action statuses for expired rollouts.");
        actionRepository.updateActiveActionStatusForExpiredRollout(DeviceActionStatus.FINISHED_NOT_EXECUTED, ACTIVE_ACTION_STATUS_FOR_EXPIRED_ROLLOUT, nowInSeconds);

        log.debug("Updating rollout group statuses for expired rollouts.");
        rolloutGroupRepository.updateStatusByStatusAndRollout(RolloutGroupStatus.FINISHING, ACTIVE_ROLLOUT_GROUP_STATUSES_FOR_EXPIRED_ROLLOUT, nowInSeconds);

        log.debug("Updating rollout statuses for expired rollouts.");
        rolloutRepository.updateStatusByStatusAndRollout(RolloutStatus.FINISHING, ACTIVE_ROLLOUT_STATUS_FOR_EXPIRED_ROLLOUT, nowInSeconds);

        log.debug("Completed handling end of rollouts.");
        return 0;
    }

    /**
     * Retrieves the action status timestamps for a specific rollout and controller within a tenant.
     * <p>
     * This endpoint allows clients to fetch the status history of a device action, identified by its rollout ID and controller ID,
     * for a given tenant. The response contains a list of status timestamp objects, each representing a status change event for the action.
     * </p>
     *
     * @param controllerId the unique identifier of the controller (device); must not be {@code null} or empty
     * @param rolloutId    the unique identifier of the rollout; must not be {@code null}
     * @return
     * @throws EntityNotFoundException if the specified action or controller does not exist for the tenant
     * @throws ValidationException     if the action does not belong to the specified controller or tenant
     */
    @Override
    public List<DeviceActionStatusTimestampResponse> fetchActionStatuses(final long rolloutId, final String controllerId) {
        verifyRolloutExists(rolloutId);
        throwExceptionIfTargetDoesNotExist(controllerId);

        log.debug("Received getActionStatus request for controllerId={}, rolloutId={}", controllerId, rolloutId);

        List<JpaAction> actionsByRolloutIdIn = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rolloutId, controllerId, true);

        final List<DeviceActionStatusTimestampResponse> actionStatus = actionsByRolloutIdIn.stream()
                .flatMap(action -> deploymentManagement
                        .getActionStatusTimestamps(action.getId(), controllerId)
                        .stream())
                .collect(Collectors.toList());

        ResponseEntity.ok(actionStatus);
        return actionStatus;
    }

    /**
     * Fetches the actions associated with targets in a specific rollout.
     * <p>
     * This method retrieves all targets associated with the given rollout ID and maps them to their corresponding actions.
     * If a target does not have an associated action, it will be included in the result with a null action value.
     * </p>
     *
     * @param rolloutId the ID of the rollout for which to fetch target actions
     * @return a map where each key is a Target and each value is its associated Action (or null if no action exists)
     */
    @Override
    public Map<Target, Action> fetchRolloutTargetActions(long rolloutId) {
        // Get paginated targets
        List<JpaTarget> targets = targetRepository.findTargetsByRolloutId(rolloutId);

        if (targets.isEmpty()) {
            log.debug("No targets found for rollout ID: {}", rolloutId);
            return Collections.emptyMap();
        }

        List<Long> targetIds = targets.stream().map(JpaTarget::getId).toList();

        // Find all actions for these targets and rollout
        Map<Long, JpaAction> targetIdToActionMap = actionRepository.findByRolloutIdAndTargetIdIn(targetIds, rolloutId, true)
                .stream().collect(Collectors.toMap(action -> action.getTarget().getId(), action -> action,
                        (existing, replacement) -> replacement));

        // Create result map with target as key and action as value (null if not found)
        Map<Target, Action> result = new HashMap<>();
        for (JpaTarget target : targets) {
            if (!targetIdToActionMap.containsKey(target.getId())) {
                log.debug("No action found for target ID: {}", target.getId());
                result.put(target, null); // No action found for this target
                continue;
            }
            log.debug("Found action for target ID: {}", target.getId());
            result.put(target, targetIdToActionMap.get(target.getId()));
        }

        log.debug("Found {} target actions for rollout ID: {}", result.size(), rolloutId);
        return result;
    }

    /**
     * Retries multiple devices in a rollout based on the provided request details.
     * <p>
     * This method validates the current status of the rollout and ensures that the retry operation
     * is allowed only if the rollout is in one of the following states: RUNNING, FINISHED, or CANCELED.
     * If valid, it updates the rollout status to RETRYING and persists the changes.
     * </p>
     *
     * @param tenantId                    the ID of the tenant
     * @param rolloutId                   the ID of the rollout
     * @param retryMultipleDevicesRequest the request object containing retry details
     * @throws IllegalArgumentException if the rollout is not in a valid state for retry
     */
    @Override
    public void retryMultipleDevices(Long tenantId, Long rolloutId, RetryMultipleDevicesRequest retryMultipleDevicesRequest) {
        JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        if (!VALID_RETRY_STATUSES.contains(rollout.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rollout must be in RUNNING, FINISHED, or CANCELED state for retry"
            );
        }
        if (VALID_RETRY_ROLLOUT_UPDATE_STATUSES.contains(rollout.getStatus())) {
            rollout.setStatus(RolloutStatus.RETRYING);
            rollout.setDescription(retryMultipleDevicesRequest.getDescription());
            rollout.setStartType(retryMultipleDevicesRequest.getStartType());
            rollout.setStartAt(retryMultipleDevicesRequest.getStartDate());
            rollout.setEndAt(retryMultipleDevicesRequest.getEndDate());
            validateStartDate(rollout);
            validateEndDate(rollout);
        }

        // Handle Vehicle Log Level
        List<JpaAction> actionsToRetry = actionRepository.findActionByRolloutIdAndActive(rolloutId, true);
        handleVehicleLogLevel(retryMultipleDevicesRequest.getVehicleLogLevel(), actionsToRetry);

        rollout.setLastRetryMode(retryMultipleDevicesRequest.getRetryMode());
        rolloutRepository.save(rollout);
    }

    /**
     * Retries individual device in a rollout based on the provided request details.
     * <p>
     * This method validates the current status of the rollout and device and ensures that the retry operation
     * is allowed only if the rollout is in one of the following states: RUNNING, FINISHED, or CANCELED
     * and device is in one of the following states: FINISHED_SUCCESS, FINISHED_FAILURE, FINISHED_NOT_EXECUTED or CANCELED.
     * If valid, it updates the rollout status to RETRYING and persists the changes.
     * </p>
     *
     * @param rolloutId                        the ID of the rollout
     * @param controllerId                     the controllerId of the target
     * @param retryIndividualDeviceRequestBody the request object containing retry details
     * @throws IllegalArgumentException if the rollout is not in a valid state for retry
     */
    @Override
    public void retryIndividualDevice(Long rolloutId, String controllerId, MgmtRetryIndividualDeviceRequestBody retryIndividualDeviceRequestBody) {
        JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutId);
        findTargetbyControllerIdAndThrowIfNotFound(controllerId);
        if (!VALID_RETRY_STATUSES.contains(rollout.getStatus())) {
            throw new ValidationException(String.format("Rollout must be in RUNNING, FINISHED or CANCELED state for retry, current status is %s ", rollout.getStatus()));
        }
        final JpaAction action = actionRepository.findByRolloutIdAndTargetControllerIdAndActive(rolloutId, controllerId, false)
                .stream().max(Comparator.comparing(JpaAction::getId)).orElse(null);

        if (action == null || !VALID_RETRY_DEVICE_STATUSES.contains(action.getStatus())) {
            throw new ValidationException("Device status must be in FINISHED_SUCCESS, FINISHED_FAILURE, FINISHED_NOT_EXECUTED or CANCELED state for retry ");
        }
        if (VALID_RETRY_ROLLOUT_UPDATE_STATUSES.contains(rollout.getStatus())) {
            rollout.setStatus(RolloutStatus.RETRYING);
            rollout.setDescription(retryIndividualDeviceRequestBody.getDescription());
            rollout.setStartType(retryIndividualDeviceRequestBody.getStartType());
            rollout.setStartAt(retryIndividualDeviceRequestBody.getStartDate());
            rollout.setEndAt(retryIndividualDeviceRequestBody.getEndDate());
            validateStartDate(rollout);
            validateEndDate(rollout);
        }
        rollout.setLastRetryMode(retryIndividualDeviceRequestBody.getRetryMode());
        rolloutRepository.save(rollout);

        //handle vehicle log level
        handleVehicleLogLevel(retryIndividualDeviceRequestBody.getVehicleLogLevel(), List.of(action));

        //update existing device
        action.setActive(false);
        actionRepository.save(action);

        //update rollout target group
        List<Long> targetIds = targetManagement.findTargetIdByControllerIds(Collections.singletonList(controllerId));
        List<JpaRolloutGroup> rolloutGroups = fetchRolloutGroups(rollout.getId(), targetIds);
        rolloutTargetGroupRepository.findByTargetIdAndRolloutGroupId(targetIds.getFirst(), rolloutGroups.getFirst().getId()).ifPresent(rolloutTargetGroup -> {
            rolloutTargetGroup.setRetryEnabled(true);
            rolloutTargetGroup.setDeviceRetryCount(rolloutTargetGroup.getDeviceRetryCount() + 1);
            rolloutTargetGroupRepository.save(rolloutTargetGroup);
        });
    }

    /**
     * @param vehicleLogLevel
     * @param actions
     */
    private void handleVehicleLogLevel(Integer vehicleLogLevel, List<JpaAction> actions) {
        if (vehicleLogLevel == null) {
            return;
        }

        if (vehicleLogLevel < 0 || vehicleLogLevel > 7) {
            throw new ValidationException("Invalid Vehicle Log Level");
        }

        for (JpaAction action : actions) {
            action.setVehicleLogLevel(vehicleLogLevel);
        }

        actionRepository.saveAll(actions);
    }

    private void buildRetryRollout(JpaRollout rollout, MgmtRetryFullRolloutRequestBody retryRolloutRequestBody) {
        rollout.setDescription(retryRolloutRequestBody.getDescription());
        rollout.setStartType(retryRolloutRequestBody.getStartType());
        rollout.setStartAt(retryRolloutRequestBody.getStartDate());
        rollout.setEndAt(retryRolloutRequestBody.getEndDate());
        rollout.setStatus(RolloutStatus.RETRYING);
        rollout.setRetryCount((rollout.getRetryCount() == null ? 0 : rollout.getRetryCount()) + 1);
        rollout.setLastRetryMode(RetryMode.FULL);
    }

    /**
     * Retries a full rollout with updated details.
     * <p>
     * This method validates that the rollout is in a FINISHED or CANCELED state before allowing a full retry.
     * It updates the rollout's description, start type, start and end dates, sets the status to RETRYING,
     * updates the last retry mode to FULL, increments the retry count, and validates the new dates.
     * The updated rollout is then persisted.
     *
     * @param rolloutID                   the ID of the rollout to retry
     * @param retryFullRolloutRequestBody the request body containing new rollout details
     */

    @Override
    public void retryFullRollout(Long rolloutID, MgmtRetryFullRolloutRequestBody retryFullRolloutRequestBody) {
        JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(rolloutID);
        log.debug("Fetched rollout with ID: {} and status: {}", rolloutID, rollout.getStatus());
        if (!VALID_FULL_RETRY_ROLLOUT_STATUS.contains(rollout.getStatus())) {
            throw new ValidationException("Rollout must be in FINISHED or CANCELED,current status is" + rollout.getStatus());
        }

        buildRetryRollout(rollout, retryFullRolloutRequestBody);
        log.info("Retrying full rollout for rolloutID: {} with new request details.", rolloutID);

        // Handle Vehicle Log Level
        Integer requestedLogLevel = retryFullRolloutRequestBody.getVehicleLogLevel();
        if (requestedLogLevel != null) {
            rollout.setVehicleLogLevel(requestedLogLevel);
        } else {
            log.debug("Vehicle Log Level not provided, using existing rollout's level: {}", rollout.getVehicleLogLevel());
        }

        setVehicleLogLevel(rollout);
        validateStartDate(rollout);
        validateEndDate(rollout);
        log.debug("Validating start and end dates for rolloutID: {}", rolloutID);
        rolloutRepository.save(rollout);

    }


    private JpaRollout buildCloneRollout(JpaRollout parentRollout, MgmtCloneRolloutRequestBody cloneRequest) {

        boolean isScheduled = MgmtRolloutStartType.SCHEDULED.equals(cloneRequest.getStartType()) ||
                (cloneRequest.getStartType() == null && MgmtRolloutStartType.SCHEDULED.equals(parentRollout.getStartType()));

        if (isScheduled) {
            if (cloneRequest.getStartDate() == null) {
                throw new ValidationException("Start date is required for scheduled rollout");
            }
        }
        Long startAt = cloneRequest.getStartDate();

        return JpaRollout.builder()
                .priority(parentRollout.getPriority())
                .connectivityType(parentRollout.getConnectivityType())
                .userAcceptanceRequired(parentRollout.getUserAcceptanceRequired())
                .status(parentRollout.getStatus())
                .downgradeAllowed(parentRollout.getDowngradeAllowed())
                .requiredMedia(parentRollout.getRequiredMedia())
                .requiredStateOfCharge(parentRollout.getRequiredStateOfCharge())
                .logCollectionRequired(parentRollout.isLogCollectionRequired())
                .logMaxSuccessVin(parentRollout.getLogMaxSuccessVin())
                .logMaxFailureVin(parentRollout.getLogMaxFailureVin())
                .logMaxAllFileSize(parentRollout.getLogMaxAllFileSize())
                .logMaxEachFileSize(parentRollout.getLogMaxEachFileSize())
                .logMaxNumberOfFiles(parentRollout.getLogMaxNumberOfFiles())
                .maxDownloadDurationTimer(parentRollout.getMaxDownloadDurationTimer())
                .maxDownloadCellularDurationTimer(parentRollout.getMaxDownloadCellularDurationTimer())
                .maxDownloadWifiDurationTimer(parentRollout.getMaxDownloadWifiDurationTimer())
                .maxUpdateTime(parentRollout.getMaxUpdateTime())
                .deploymentEstimatedUpdateTime(parentRollout.getDeploymentEstimatedUpdateTime())
                .maxPackageSize(parentRollout.getMaxPackageSize())
                .downloadRetryCount(parentRollout.getDownloadRetryCount())
                .type(parentRollout.getType())
                .updateAction(parentRollout.getUpdateAction())
                .updateActionUninstallVersion(parentRollout.getUpdateActionUninstallVersion() != null && !parentRollout.getUpdateActionUninstallVersion().isEmpty() ? parentRollout.getUpdateActionUninstallVersion().toString() : null)
                // Override only these fields from cloneRequest
                .startType(cloneRequest.getStartType() != null ? cloneRequest.getStartType() : parentRollout.getStartType())
                .startAt(startAt)
                .endAt(cloneRequest.getEndDate())
                .build();
    }

    private JpaRolloutMetaData buildJpaRolloutMetaData(JpaRollout clonedRollout, Long parentRolloutId) {
        JpaRolloutMetaData rolloutMetaData = new JpaRolloutMetaData();
        rolloutMetaData.setRolloutId(clonedRollout);
        rolloutMetaData.setKey("cloneableParentRolloutId");
        rolloutMetaData.setValue(String.valueOf(parentRolloutId));
        rolloutMetaData.setTenant(clonedRollout.getTenant());
        return rolloutMetaData;
    }

    /**
     * Clones an existing rollout with new details.
     * <p>
     * This method retrieves the specified rollout by its ID, creates a clone of it with the provided
     * name and description, sets its status to CLONING, and saves the cloned rollout. It also creates
     * metadata to link the cloned rollout to its parent.
     * </p>
     *
     * @param RolloutId      the ID of the rollout to be cloned
     * @param rolloutRequest the request body containing new rollout details
     * @return the newly created cloned rollout
     */
    @Override
    public Rollout cloneRollout(Long RolloutId, MgmtCloneRolloutRequestBody rolloutRequest) {
        JpaRollout rollout = getRolloutAndThrowExceptionIfNotFound(RolloutId);
        log.debug("Fetched rollout with ID: {} for cloning.", RolloutId);

        JpaRollout cloneRollout = buildCloneRollout(rollout, rolloutRequest);
        cloneRollout.setName(rolloutRequest.getName());
        cloneRollout.setDescription(rolloutRequest.getDescription() != null ? rolloutRequest.getDescription() : rollout.getDescription());
        cloneRollout.setStatus(RolloutStatus.CLONING);
        log.info("Cloning rollout with ID: {} using new request details.", RolloutId);
        JpaRollout clonedRollout = (JpaRollout) create(cloneRollout);

        JpaRolloutMetaData rolloutMetaData = buildJpaRolloutMetaData(clonedRollout, RolloutId);
        rolloutMetaDataRepository.save(rolloutMetaData);
        log.info("Cloned rollout created with ID: {} from parent rollout ID: {}", clonedRollout.getId(), RolloutId);

        if (clonedRollout.getJpaRolloutMetaDataList() == null) {
            clonedRollout.setJpaRolloutMetaDataList(new ArrayList<>());
        }
        clonedRollout.getJpaRolloutMetaDataList().add(rolloutMetaData);

        return clonedRollout;
    }

    private void verifyRolloutExists(final long rolloutId) {
        if (!rolloutRepository.existsById(rolloutId)) {
            throw new EntityNotFoundException(Rollout.class, rolloutId);
        }
    }

    private void throwExceptionIfTargetDoesNotExist(final String controllerId) {
        if (!targetRepository.exists(TargetSpecifications.hasControllerId(controllerId))) {
            throw new EntityNotFoundException(Target.class, controllerId);
        }
    }

}