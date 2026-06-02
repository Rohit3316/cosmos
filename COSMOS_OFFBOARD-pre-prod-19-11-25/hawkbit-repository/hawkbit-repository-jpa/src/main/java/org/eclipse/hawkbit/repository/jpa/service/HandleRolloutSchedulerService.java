/**
 * The `HandleRolloutSchedulerService` class manages the lifecycle of rollouts, ensuring consistent state transitions
 * for rollouts, associated  rollout groups and actions. The class interacts with repositories and utility methods
 * to enforce business rules and finalize state changes. Logging and tracing annotations are used for debugging and
 * monitoring purposes.
 */

package org.eclipse.hawkbit.repository.jpa.service;


import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.persistence.OptimisticLockException;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.annotations.TraceableObject;
import org.cosmos.models.kafka.RolloutStatusPayload;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutGroupStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupTarget;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.cosmos.models.mgmt.supportpackage.constants.MgmtSupportPackageFileType;
import org.eclipse.hawkbit.exception.AbstractServerRtException;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.DeploymentManagement;
import org.eclipse.hawkbit.repository.Identifiable;
import org.eclipse.hawkbit.repository.RolloutApprovalStrategy;
import org.eclipse.hawkbit.repository.RolloutGroupManagement;
import org.eclipse.hawkbit.repository.RolloutHelper;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.event.remote.RolloutGroupDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.RolloutIllegalStateException;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.ActionStatusRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutGroupRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutRepository;
import org.eclipse.hawkbit.repository.jpa.RolloutTargetGroupRepository;
import org.eclipse.hawkbit.repository.jpa.TargetRepository;
import org.eclipse.hawkbit.repository.jpa.executor.AfterTransactionCommitExecutor;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRolloutGroup;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.EvaluatorNotConfiguredException;
import org.eclipse.hawkbit.repository.jpa.rollout.condition.RolloutGroupEvaluationManager;
import org.eclipse.hawkbit.repository.jpa.utils.RolloutSchedulerUtils;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.BaseSupportPackage;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.utils.SupportPackageUtils;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.validation.ValidationException;


@Slf4j
public class HandleRolloutSchedulerService {

    public static final Logger LOGGER = LoggerFactory.getLogger(HandleRolloutSchedulerService.class);
    private static final int PAGE_SIZE = 100;
    private static final int FIRST_PAGE_NUMBER = 0;
    private static final List<DeviceActionStatus> ROLLOUT_FINISH_STATUS_LIST = List.of(DeviceActionStatus.FINISHED_FAILURE, DeviceActionStatus.FINISHED_SUCCESS, DeviceActionStatus.CANCELED);
    private static final List<DeviceActionStatus> PAUSING_DEVICE_ACTION_STATUSES = List.of(
            DeviceActionStatus.PAUSED,
            DeviceActionStatus.PAUSING,
            DeviceActionStatus.DD_SENT,
            DeviceActionStatus.CANCELED,
            DeviceActionStatus.FINISHED_FAILURE,
            DeviceActionStatus.FINISHED_SUCCESS
    );
    private static final List<RolloutGroupStatus> PAUSE_ROLLOUT_GROUPS = List.of(
            RolloutGroupStatus.PAUSED,
            RolloutGroupStatus.CANCELED,
            RolloutGroupStatus.FINISHED,
            RolloutGroupStatus.QUEUED
    );
    private static final List<DeviceActionStatus> CANCELING_DEVICE_ACTION_STATUSES = List.of(
            DeviceActionStatus.CANCELING,
            DeviceActionStatus.CANCELED,
            DeviceActionStatus.FINISHED_SUCCESS,
            DeviceActionStatus.FINISHED_FAILURE);
    private static final List<DeviceActionStatus> CURRENT_STATUSES_FOR_CANCELING = List.of(
            DeviceActionStatus.RUNNING,

            DeviceActionStatus.PAUSED, DeviceActionStatus.DD_SENT);
    private static final List<DeviceActionStatus> RESUMING_DEVICE_ACTION =
            List.of(DeviceActionStatus.RESUMING,
                    DeviceActionStatus.RUNNING,
                    DeviceActionStatus.DD_SENT,
                    DeviceActionStatus.CANCELED,
                    DeviceActionStatus.FINISHED_FAILURE,
                    DeviceActionStatus.FINISHED_SUCCESS);
    private static final List<DeviceActionStatus> PAUSED_DEVICE_ACTION = List.of(DeviceActionStatus.PAUSED);
    private static final List<DeviceActionStatus> RUNNING_DEVICE_ACTION = List.of(DeviceActionStatus.RUNNING);
    private static final List<RolloutGroupStatus> PAUSED_GROUP = List.of(RolloutGroupStatus.PAUSED);
    private static final List<RolloutGroupStatus> RESUMING_ROLLOUT_GROUPS =
            List.of(RolloutGroupStatus.RUNNING,
                    RolloutGroupStatus.CANCELED,
                    RolloutGroupStatus.FINISHED,
                    RolloutGroupStatus.QUEUED,
                    RolloutGroupStatus.DRAFT,
                    RolloutGroupStatus.READY);
    private static final List<RolloutGroupStatus> RUNNING_GROUP = List.of(RolloutGroupStatus.RUNNING);
    private static final List<RolloutGroupStatus> CANCELED_GROUP = List.of(RolloutGroupStatus.CANCELED);
    private static final List<RolloutGroupStatus> STATES_TO_BE_CANCEL = List.of(RolloutGroupStatus.RUNNING, RolloutGroupStatus.QUEUED, RolloutGroupStatus.PAUSED);
    private static ActionRepository actionRepository;
    private static ActionStatusRepository actionStatusRepository;
    private static RolloutGroupRepository rolloutGroupRepository;
    private static RolloutTargetGroupRepository rolloutTargetGroupRepository;
    private static RolloutRepository rolloutRepository;
    private final TenantConfigurationManagement tenantConfigurationManagement;
    private final SupportPackageManagement supportPackageManagement;
    private final SystemSecurityContext systemSecurityContext;
    private final RolloutApprovalStrategy rolloutApprovalStrategy;
    private final RolloutGroupManagement rolloutGroupManagement;
    private final RolloutManagement rolloutManagement;
    private final DeploymentManagement deploymentManagement;
    private final AfterTransactionCommitExecutor afterCommit;
    private final TenantAware tenantAware;
    private final EventPublisherHolder eventPublisherHolder;
    private final EntityManager entityManager;
    private final RolloutGroupEvaluationManager evaluationManager;
    private final KafkaMessageService kafkaMessageService;
    private final TargetRepository targetRepository;
    private final SystemManagement systemManagement;

    @Autowired
    private DeviceRetryService deviceRetryService;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private CdnFileUploadService<BaseSupportPackage> supportPackageCdnFileUploadService;


    public HandleRolloutSchedulerService(RolloutRepository rolloutRepository,
                                         ActionRepository actionRepository,
                                         RolloutGroupRepository rolloutGroupRepository,
                                         RolloutTargetGroupRepository rolloutTargetGroupRepository,
                                         TenantConfigurationManagement tenantConfigurationManagement,
                                         SupportPackageManagement supportPackageManagement, SystemSecurityContext systemSecurityContext,
                                         RolloutApprovalStrategy rolloutApprovalStrategy, RolloutGroupManagement rolloutGroupManagement,
                                         RolloutManagement rolloutManagement, DeploymentManagement deploymentManagement, AfterTransactionCommitExecutor afterCommit,
                                         TenantAware tenantAware, EventPublisherHolder eventPublisherHolder, EntityManager entityManager, RolloutGroupEvaluationManager evaluationManager,
                                         KafkaMessageService kafkaMessageService, TargetRepository targetRepository, SystemManagement systemManagement) {
        HandleRolloutSchedulerService.rolloutRepository = rolloutRepository;
        HandleRolloutSchedulerService.actionRepository = actionRepository;
        HandleRolloutSchedulerService.rolloutGroupRepository = rolloutGroupRepository;
        HandleRolloutSchedulerService.rolloutTargetGroupRepository = rolloutTargetGroupRepository;
        this.tenantConfigurationManagement = tenantConfigurationManagement;
        this.supportPackageManagement = supportPackageManagement;
        this.systemSecurityContext = systemSecurityContext;
        this.rolloutApprovalStrategy = rolloutApprovalStrategy;
        this.rolloutGroupManagement = rolloutGroupManagement;
        this.rolloutManagement = rolloutManagement;
        this.deploymentManagement = deploymentManagement;
        this.afterCommit = afterCommit;
        this.tenantAware = tenantAware;
        this.eventPublisherHolder = eventPublisherHolder;
        this.entityManager = entityManager;
        this.evaluationManager = evaluationManager;
        this.kafkaMessageService = kafkaMessageService;
        this.targetRepository = targetRepository;
        this.systemManagement = systemManagement;
    }

    /*
     *
     * This method handles the rollout which is in Freezing Status
     * */
    @Transactional
    @TraceableMethod
    public void handleFreezeRollout(@TraceableObject JpaRollout rollout) throws ValidationException, AbstractServerRtException {
        log.info("Starting rollout freezing process for rolloutId: {}, rolloutName: {}", rollout.getId(), rollout.getName());

        if(!validateScheduledStartDate(rollout)){
            return;
        }

        validateActiveArtifactsUploadedToCdn(rollout);

        List<MgmtSupportPackageFileType> mandatoryRspFileTypes = getmandatoryRspFileTypeList();
        List<MgmtSupportPackageFileType> mandatoryEspFileTypes = getMandatoryEspFileTypeList();

        log.debug("Fetching RSP and ESP files for rolloutId: {}", rollout.getName());
        List<Rsp> rspList = getAllRspForRollout(rollout.getId());
        List<Esp> espList = getAllEspForRollout(rollout.getId());

        try {
            log.debug("Validating RSP files for rolloutId: {}", rollout.getName());
            SupportPackageUtils.validateRspToHandleFrozenRollout(rspList, mandatoryRspFileTypes, rollout);
            log.debug("Validating if all ESPs are uploaded for rolloutId: {}", rollout.getName());
            SupportPackageUtils.validateEspUploadStatus(espList);
        } catch (ValidationException e) {
            log.error("Validation failed for rolloutId: {}, error: {}", rollout.getName(), e.getMessage());
            sendRolloutStatusEvent(rollout, "ERROR", RolloutStatus.FREEZING, Collections.emptyList(), List.of(e.getMessage()));

            throw e;
        }

        JpaRollout jpaRollout = rollout;
        if (!rolloutApprovalStrategy.isApprovalNeeded(rollout)) {
            try {
                log.debug("Updating rollout group status after successful validations for rolloutId: {}", rollout.getName());
                updateRolloutGroupStatusAfterSuccessfulValidations(rollout, mandatoryEspFileTypes);
            } catch (ValidationException e) {
                log.error("Failed to update rollout group status for rolloutId: {}, error: {}", rollout.getName(), e.getMessage());
                sendRolloutStatusEvent(rollout, "ERROR", RolloutStatus.FREEZING, Collections.emptyList(), List.of(e.getMessage()));
                throw e;
            }

            JpaRollout savedRollout = RolloutSchedulerUtils.updateRolloutStateAndSendToKafka(rollout, RolloutStatus.READY, rolloutRepository);
            sendRolloutStatusEvent(savedRollout, "INFO", savedRollout.getStatus(), Collections.emptyList(), Collections.emptyList());


        } else {
            log.info("Rollout requires approval. RolloutId: {}, Switching to WAITING_FOR_APPROVAL", rollout.getName());
            rolloutApprovalStrategy.onApprovalRequired(rollout);
        }
    }

    private List<MgmtSupportPackageFileType> getMandatoryEspFileTypeList() {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).getTenantConfigurationMandatoryEsp();

    }

    private List<MgmtSupportPackageFileType> getmandatoryRspFileTypeList() throws ValidationException {
        return TenantConfigHelper.usingContext(systemSecurityContext, tenantConfigurationManagement).getTenantConfigurationMandatoryRsp();
    }

    private List<Rsp> getAllRspForRollout(Long rolloutId) {
        log.debug("Retrieved RSP packages for rolloutId: {}", rolloutId);
        return supportPackageManagement.getRSPSupportPackages(rolloutId);
    }

    private List<Esp> getAllEspForRollout(Long rolloutId) {
        log.debug("Retrieved ESP packages for rolloutId: {}", rolloutId);
        return supportPackageManagement.getESPSupportPackages(rolloutId);
    }

    public void updateRolloutGroupStatusAfterSuccessfulValidations(Rollout rollout,
                                                                   List<MgmtSupportPackageFileType> mandatoryEspFileTypes)
            throws ValidationException {
        log.debug("Starting update of rollout group statuses after validation. RolloutId={}, RolloutName={}",
                rollout.getId(), rollout.getName());
        int page = FIRST_PAGE_NUMBER;
        Page<RolloutGroup> rolloutGroupPage;

        do {
            rolloutGroupPage = rolloutGroupManagement.findByRollout(PageRequest.of(page, PAGE_SIZE), rollout.getId());
            log.debug("Processing page {} for rolloutId={}, found {} rollout groups.",
                    page, rollout.getId(), rolloutGroupPage.getNumberOfElements());
            for (RolloutGroup group : rolloutGroupPage.getContent()) {
                log.debug("Validating ESPs for rolloutId= {} ,rolloutGroupId={}, rolloutGroupName={}", rollout.getId(), group.getId(), group.getName());
                // Perform validation checks
                SupportPackageUtils.withEspValidationContextForFrozenRollout(rolloutGroupManagement, supportPackageManagement)
                        .checkAllEspUploadedAndMandatoryEspAvailable(mandatoryEspFileTypes, rollout.getId(), group);

                // Update rollout group status
                JpaRolloutGroup jpaRolloutGroup = (JpaRolloutGroup) group;
                RolloutSchedulerUtils.updateRolloutGroupStatus(jpaRolloutGroup, RolloutGroupStatus.READY, rolloutGroupRepository);
                log.debug("Updated rolloutGroupId={} status to READY.", jpaRolloutGroup.getId());
            }
            page++;
        } while (rolloutGroupPage.hasNext());
        log.debug("Completed update of rollout group statuses for rolloutId={}.", rollout.getId());
    }

    @Transactional
    @TraceableMethod
    public void handleReadyRollout(@TraceableObject final JpaRollout rollout) {
        if(!validateScheduledStartDate(rollout)){
            return;
        };
        if (rollout.getStartType().equals(MgmtRolloutStartType.AUTO) || rollout.getStartType().equals(MgmtRolloutStartType.SCHEDULED)) {
            log.info("Initiating rollout start: rolloutId={}, rolloutName={}", rollout.getId(), rollout.getName());
            rolloutManagement.start(rollout.getId());
        }
    }

    /**
     * Handles the starting of a rollout. This method checks the status of ESP support packages
     * and ensures they are uploaded to the CDN before proceeding with the rollout.
     *
     * @param rollout the rollout to be started
     */
    @Transactional
    @TraceableMethod
    public void handleStartingRollout(@TraceableObject final JpaRollout rollout) throws ValidationException {
        log.info("Starting rollout process for rolloutId: {}, rolloutName:{}", rollout.getId(), rollout.getName());

        log.debug("handleStartingRollout called for rollout {}, rolloutName: {}", rollout.getId(), rollout.getName());
        final JpaRollout jpaRollout = rollout;

        log.debug("Fetching the oldest rollout group for rollout {},rolloutName: {}", rollout.getId(), rollout.getName());
        JpaRolloutGroup oldestRolloutGroup = rolloutGroupRepository.findByRolloutOrderByIdAsc(jpaRollout).stream().findFirst().orElse(null);
        if (oldestRolloutGroup == null) {
            log.warn("No rollout group found for rollout {}", jpaRollout.getId());
            return;
        }
        LOGGER.debug("Validating artifacts and support packages upload to CDN for rollout {},rolloutName: {}", rollout.getId(), rollout.getName());
        validateArtifactsAndSupportPackagesUploadToCDN(rollout, oldestRolloutGroup);

        LOGGER.debug("Fetching groups to be scheduled for rollout {},rolloutName: {}", rollout.getId(), rollout.getName());
        final List<JpaRolloutGroup> groupsToBeScheduled = rolloutGroupRepository.findByRolloutAndStatus(rollout, RolloutGroupStatus.READY);

        LOGGER.debug("Updating status to QUEUED for groups to be scheduled for rollout {},,rolloutName: {}", rollout.getId(), rollout.getName());
        groupsToBeScheduled.forEach(group -> RolloutSchedulerUtils.updateRolloutGroupStatus(group, RolloutGroupStatus.QUEUED, rolloutGroupRepository));

        LOGGER.debug("Creating actions for the oldest rollout group for rollout {},,rolloutName: {}", rollout.getId(), rollout.getName());
        deploymentManagement.createActionsForRolloutGroup(rollout, oldestRolloutGroup);

        LOGGER.debug("Starting the first rollout group for rollout {},,rolloutName: {}", rollout.getId(), rollout.getName());
        startFirstRolloutGroup(jpaRollout);
    }

    private void validateArtifactsAndSupportPackagesUploadToCDN(Rollout rollout, RolloutGroup rolloutGroup) {
        // Validate RSP support packages are uploaded to CDN
        validatingRspsUploadedToCdn(rollout);
        // Validate all artifacts for the modules are uploaded to CDN
        validateActiveArtifactsUploadedToCdn(rollout);
        //validate if all ESps are uploaded to CDN
        validateESPUploadToCDN(rollout, rolloutGroup);
    }


    /**
     * Validates that all RSP support packages for the given rollout are uploaded to the CDN.
     * Ensures that each RSP has a status of CDN_UPLOADED_SUCCESSFUL before proceeding.
     * Throws a ValidationException if validation fails.
     */
    private void validatingRspsUploadedToCdn(Rollout rollout) {
        log.debug("Starting validation for RSPs uploaded to CDN for rolloutId={}, rolloutName={}",
                rollout.getId(), rollout.getName());

        List<Rsp> rspSupportPackages = supportPackageManagement.getRSPSupportPackages(rollout.getId());
        log.debug("Retrieved {} RSP support packages for rolloutId={}, rolloutName= {}", rspSupportPackages.size(), rollout.getId(), rollout.getName());

        List<Rsp> unUploadedRsps = rspSupportPackages.stream()
                .filter(rsp -> rsp.getSupportPackageFileStatus() != null &&
                        !rsp.getSupportPackageFileStatus().name().equalsIgnoreCase(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.name()))
                .toList();

        if (!unUploadedRsps.isEmpty()) {
            List<Long> rspIds = unUploadedRsps.stream().map(Identifiable::getId).toList();
            log.warn("Validation failed: {} RSP(s) are not uploaded to CDN for rolloutId={}. Unuploaded RSP IDs={}",
                    unUploadedRsps.size(), rollout.getId(), rspIds);
            throw new ValidationException(String.format(
                    "All RSP support packages should be uploaded to CDN before starting the rollout %s", rspIds));
        }

        log.debug("Validation successful: All RSP support packages are uploaded to CDN for rolloutId={}, rolloutName= {}", rollout.getId(), rollout.getName());
    }

    private boolean validateScheduledStartDate(Rollout rollout) {
        long currentEpochSeconds = Instant.now().getEpochSecond();

        if (MgmtRolloutStartType.SCHEDULED.equals(rollout.getStartType())) {
            Long startAt = rollout.getStartAt();
            if (startAt != null && startAt < currentEpochSeconds) {
                    log.warn("Scheduled rollout {} start date is in the past (startAt={} epoch).", rollout.getId(), startAt);
                    return false;
            }
        }
        return true;
    }

    /**
     * Validates that all artifacts associated with the modules in the given rollout are uploaded to the CDN.
     * Checks that each artifact has a status of CDN_UPLOADED_SUCCESSFUL.
     * Throws a ValidationException if validation fails.
     * */
    private void validateActiveArtifactsUploadedToCdn(Rollout rollout) {
        log.debug("Starting validation for artifacts uploaded to CDN for rolloutId={}, rolloutName={}",
                rollout.getId(), rollout.getName());

        rollout.getDistributionSet().getModules().forEach(module ->
                validateModuleArtifactsUploadedToCdn(module, rollout)
        );

        log.debug("Completed artifact validation for rolloutId={}", rollout.getId());
    }


    /**
     * Validate all artifact-related checks for a single module within a rollout.
     */
    private void validateModuleArtifactsUploadedToCdn(SoftwareModule module, Rollout rollout) {
        log.debug("Processing moduleId={} for rolloutId={}", module.getId(), rollout.getId());

        List<Artifacts> artifacts = extractArtifactsFromModule(module);
        log.debug("Retrieved {} artifacts for moduleId={} in rolloutId={}", artifacts.size(), module.getId(), rollout.getId());

        validateArtifactsExist(artifacts, module, rollout);

        // Check ACTIVE vs non-ACTIVE
        List<Artifacts> activeArtifacts = validateAndReturnActiveArtifacts(artifacts, module, rollout);

        // Check signature expiry for active artifacts
        validateSignatureExpiry(activeArtifacts, module, rollout);

        // Check CDN upload status for active artifacts
        validateActiveArtifactsUploadStatus(activeArtifacts, module, rollout);

        log.debug("Validation successful: All ACTIVE artifacts uploaded to CDN for moduleId={} in rolloutId={}", module.getId(), rollout.getId());
    }

    /**
     * Extract artifact entities from module associations.
     */
    private List<Artifacts> extractArtifactsFromModule(SoftwareModule module) {
        return module.getArtifactSoftwareModuleAssociations().stream()
                .map(ArtifactSoftwareModuleAssociation::getArtifact)
                .toList();
    }

    /**
     * Ensure that module has at least one artifact.
     */
    private void validateArtifactsExist(List<Artifacts> artifacts, SoftwareModule module, Rollout rollout) {
        if (artifacts.isEmpty()) {
            log.warn("No artifacts found for moduleId={} in rolloutId={}.", module.getId(), rollout.getId());
            throw new ValidationException(String.format("No artifacts associated with moduleId %s in rolloutId %s", module.getId(), rollout.getId()));
        }
    }

    /**
     * Validate artifact ACTIVE status. Returns list of active artifacts.
     * Throws if all artifacts are non-ACTIVE.
     */
    private List<Artifacts> validateAndReturnActiveArtifacts(List<Artifacts> artifacts, SoftwareModule module, Rollout rollout) {
        List<Artifacts> nonActiveArtifacts = artifacts.stream()
                .filter(a -> a.getArtifactStatus() == null || !a.getArtifactStatus().equalsIgnoreCase(ArtifactsStatus.ACTIVE.name()))
                .toList();

        if (!nonActiveArtifacts.isEmpty()) {
            List<Long> artifactIds = nonActiveArtifacts.stream().map(Identifiable::getId).collect(Collectors.toList());

            if (nonActiveArtifacts.size() == artifacts.size()) {
                log.warn("Validation failed: all {} artifact(s) are not ACTIVE for rolloutId={}, moduleId={}. Artifact IDs={}",
                        nonActiveArtifacts.size(), rollout.getId(), module.getId(), artifactIds);
                throw new ValidationException(String.format("All artifacts are NOT ACTIVE for rolloutId %s TO start the rollout. Artifact IDs %s", rollout.getId(), artifactIds));
            } else {
                log.warn("Some artifacts are not ACTIVE for rolloutId={}, moduleId={}. Non-active Artifact IDs={}",
                        rollout.getId(), module.getId(), artifactIds);
            }
        }

        return artifacts.stream()
                .filter(a -> a.getArtifactStatus() != null && a.getArtifactStatus().equalsIgnoreCase(ArtifactsStatus.ACTIVE.name()))
                .toList();
    }

    /**
     * Validate signature (expiry) for active artifacts.
     */
    private void validateSignatureExpiry(List<Artifacts> activeArtifacts, SoftwareModule module, Rollout rollout) {
        if (activeArtifacts.isEmpty()) {
            return;
        }

        long nowEpoch = Instant.now().getEpochSecond();
        List<Artifacts> expiredSignatureArtifacts = activeArtifacts.stream()
                .filter(artifact -> {
                    Long expiry = artifact.getExpiryDate();
                    return expiry != null && expiry < nowEpoch;
                })
                .toList();

        if (!expiredSignatureArtifacts.isEmpty()) {
            List<Long> artifactIds = expiredSignatureArtifacts.stream().map(Identifiable::getId).collect(Collectors.toList());
            if (expiredSignatureArtifacts.size() == activeArtifacts.size()) {
                log.warn("Validation failed: all {} artifact(s) have expired signatures for rolloutId={}, moduleId={}. Artifact IDs={}",
                        expiredSignatureArtifacts.size(), rollout.getId(), module.getId(), artifactIds);
                throw new ValidationException(String.format(
                        "All Artifacts have signature's expired affecting rollout to start. Artifacts %s", artifactIds));
            } else {
                log.warn("Some artifacts have expired signatures for rolloutId={}, moduleId={}. Expired Artifact IDs={}",
                        rollout.getId(), module.getId(), artifactIds);
            }
        }
    }

    /**
     * Validate CDN upload status for ACTIVE artifacts.
     */
    private void validateActiveArtifactsUploadStatus(List<Artifacts> activeArtifacts, SoftwareModule module, Rollout rollout) {
        if (activeArtifacts.isEmpty()) {
            return;
        }

        List<Artifacts> unUploadedArtifacts = activeArtifacts.stream()
                .filter(artifact -> artifact.getFileStatus() != null &&
                        !artifact.getFileStatus().equals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL))
                .toList();

        if (!unUploadedArtifacts.isEmpty()) {
            List<Long> artifactIds = unUploadedArtifacts.stream().map(Identifiable::getId).collect(Collectors.toList());

            if (unUploadedArtifacts.size() == activeArtifacts.size()) {
                log.warn("Validation failed: all {} ACTIVE artifact(s) are not uploaded to CDN for rolloutId={}, moduleId={}. Artifact IDs={}",
                        unUploadedArtifacts.size(), rollout.getId(), module.getId(), artifactIds);
                throw new ValidationException(String.format(
                        "All ACTIVE artifacts are not uploaded to CDN to start the rollout %s", artifactIds));
            } else {
                log.warn("Some artifacts are not uploaded to CDN for rolloutId={}, moduleId={}. Unuploaded artifact IDs={}",
                        rollout.getId(), module.getId(), artifactIds);
            }
        }
    }



    /**
     * Validates whether all ESP support packages have been uploaded to the CDN for a given rollout group.
     * If any package is still pending (not uploaded), this method attempts to upload them
     * and throws a ValidationException if any remain unuploaded.
     */
    public void validateESPUploadToCDN(final Rollout rollout, final RolloutGroup rolloutGroup) {
        List<Esp> pendingEspPackages = fetchAndUploadPendingEspPackages(rollout, rolloutGroup);
        if (!pendingEspPackages.isEmpty()) {
            throw new ValidationException(String.format("All ESP packages with ids:%s should be uploaded to CDN before starting the rollout %s", pendingEspPackages.stream()
                    .map(Identifiable::getId)
                    .toList(), rollout.getId()));
        }
    }

    /**
     * Fetches ESP packages for the given rollout group and returns those that are pending CDN upload.
     * Also triggers upload for any pending packages.
     */
    public List<Esp> fetchAndUploadPendingEspPackages(final Rollout rollout, final RolloutGroup rolloutGroup) {
        List<String> controllerIds = rolloutTargetGroupRepository.getTargetControllerIdsByRolloutGroupId(List.of(rolloutGroup.getId()));
        List<Esp> espSupportPackages = supportPackageManagement.getESPSupportPackages(rollout.getId(), controllerIds);
        log.debug("ESP support  packages size: {},rollout={}", espSupportPackages.size(), rollout.getId());
        // Filter ESPs not yet uploaded to CDN
        //Pending packages are the packages which are not uploaded to CDN
        List<Esp> pendingEspPackages = espSupportPackages.stream()
                .filter(esp -> esp.getSupportPackageFileStatus() == null || esp.getSupportPackageFileStatus() != FileTransferStatus.CDN_UPLOAD_SUCCESSFUL)
                .toList();
        log.debug("Pending ESP support packages size: {}", pendingEspPackages.size());
        log.debug("Pending esp packages: {}", pendingEspPackages.stream().map(Identifiable::getId).toList());
        uploadEspFilesToCdn(pendingEspPackages);
        return pendingEspPackages;
    }

    /**
     * Wrapper method used to validate and upload ESP packages to CDN.
     */
    public List<Esp> validateAndUploadEspPackagesToCDN(final Rollout rollout, final RolloutGroup rolloutGroup) {
        return fetchAndUploadPendingEspPackages(rollout, rolloutGroup);
    }

    /**
     * Checks whether all ESP support packages for the given rollout group have been uploaded to the CDN.
     * This method does not trigger uploads — it only verifies the upload status.
     */
    public boolean isEspCdnUploadComplete(final Rollout rollout, final RolloutGroup rolloutGroup) {
        List<String> controllerIds = rolloutTargetGroupRepository.getTargetControllerIdsByRolloutGroupId(List.of(rolloutGroup.getId()));
        List<Esp> espSupportPackages = supportPackageManagement.getESPSupportPackages(rollout.getId(), controllerIds);
        return espSupportPackages.stream()
                .allMatch(esp -> FileTransferStatus.CDN_UPLOAD_SUCCESSFUL.equals(esp.getSupportPackageFileStatus()));

    }

    /**
     * Uploads ESP support packages to the CDN. Only ESP support packages that are in the STORAGE_UPLOADED_SUCCESSFUL status
     * will be uploaded.
     *
     * @param espSupportPackages the list of ESP support packages
     */
    private void uploadEspFilesToCdn(List<Esp> espSupportPackages) {
        espSupportPackages.stream()
                .filter(Objects::nonNull)
                .filter(esp -> esp.getSupportPackageFileStatus() != null && esp.getSupportPackageFileStatus().equals(FileTransferStatus.STORAGE_UPLOAD_SUCCESSFUL))
//                we will upload all the esps which are successfully uploaded to Storage
                .forEach(supportPackageCdnFileUploadService::uploadFile);
    }

    /**
     * Starts the first rollout group for the given rollout.
     * This method verifies the rollout status, fetches the first rollout group,
     * sets its status to RUNNING, and updates the rollout status to RUNNING.
     *
     * @param jpaRollout the rollout to start the first group for
     */
    private void startFirstRolloutGroup(final JpaRollout jpaRollout) {
        LOGGER.debug("startFirstRolloutGroup called for rollout {}", jpaRollout.getId());
        RolloutHelper.verifyRolloutInStatus(jpaRollout, RolloutStatus.STARTING);

        LOGGER.debug("Fetching the first rollout group for rollout {}", jpaRollout.getId());
        final JpaRolloutGroup rolloutGroup = rolloutGroupRepository.findByRolloutOrderByIdAsc(jpaRollout).stream()
                .findFirst()
                .orElse(null);
        if (rolloutGroup == null) {
            LOGGER.warn("No rollout group found for rollout {}", jpaRollout.getId());
            return;
        }

        LOGGER.debug("Setting status to RUNNING for rollout group {}", rolloutGroup.getId());
        RolloutSchedulerUtils.updateRolloutGroupStatus(rolloutGroup, RolloutGroupStatus.RUNNING, rolloutGroupRepository);

        LOGGER.debug("Updating status to RUNNING for rollout {}", jpaRollout.getId());
        RolloutSchedulerUtils.updateRolloutStatusWithLastCheck(jpaRollout, RolloutStatus.RUNNING, 0, rolloutRepository);

        LOGGER.debug("Rollout {} and its first group {} are now RUNNING", jpaRollout.getId(), rolloutGroup.getId());
    }

    @Transactional
    @TraceableMethod
    public void handleDeleteRollout(@TraceableObject final JpaRollout rollout) {
        log.info("handleDeleteRollout called for {} ,rolloutName: {}", rollout.getId(), rollout.getName());

        log.debug("Checking if there are actions beyond scheduled for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());

        // check if there are actions beyond schedule
        boolean hardDeleteRolloutGroups = !actionRepository.existsByRolloutIdAndStatusNotIn(rollout.getId(),
                DeviceActionStatus.USER_SCHEDULED, true);
        if (hardDeleteRolloutGroups) {
            log.debug("Rollout {} ,rolloutName: {} has no actions other than scheduled, proceeding to hard delete", rollout.getId(), rollout.getName());
            hardDeleteRollout(rollout);
            return;
        }

        log.debug("Cleaning up scheduled actions for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());
        deleteScheduledActions(rollout);

        final boolean hasScheduledActionsLeft = actionRepository.countByRolloutIdAndStatusAndActive(rollout.getId(),
                DeviceActionStatus.USER_SCHEDULED, true) > 0;
        log.debug("Has scheduled actions left for rolloutId={},rolloutName: {}: {}", rollout.getId(), rollout.getName(), hasScheduledActionsLeft);

        if (hasScheduledActionsLeft) {
            log.debug("Exiting handleDeleteRollout, scheduled actions are still left for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());
            return;
        }

        log.debug("Checking if there are any actions left for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());
        hardDeleteRolloutGroups = !actionRepository.existsByRolloutId(rollout.getId(), true);
        if (hardDeleteRolloutGroups) {
            log.debug("Rollout {},rolloutName: {} has no actions left, proceeding to hard delete", rollout.getId(), rollout.getName());
            hardDeleteRollout(rollout);
            return;
        }

        log.debug("Setting soft delete for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());
        RolloutSchedulerUtils.updateRolloutAndSetBoolean(rollout, RolloutStatus.DELETED, true, rolloutRepository);

        log.debug("Sending rollout group deleted events for rolloutId={},rolloutName: {}", rollout.getId(), rollout.getName());
        sendRolloutGroupDeletedEvents(rollout);
    }


    private void hardDeleteRollout(final JpaRollout rollout) {
        log.debug("Initiating hard delete for rolloutId={}, rolloutName={}", rollout.getId(), rollout.getName());
        sendRolloutGroupDeletedEvents(rollout);
        log.debug("Rollout group delete events sent for rolloutId={}", rollout.getId());
        rolloutRepository.delete(rollout);
        log.debug("Successfully deleted rolloutId={}", rollout.getId());
    }


    private void deleteScheduledActions(final JpaRollout rollout) {
        log.debug("Starting deletion of scheduled actions for rolloutId={}, rolloutName={}", rollout.getId(), rollout.getName());

        Long scheduledActionCount = actionRepository.countByRolloutIdAndStatusAndActive(rollout.getId(), DeviceActionStatus.USER_SCHEDULED, true);
        log.debug("Found {} scheduled actions for rolloutId={}", scheduledActionCount, rollout.getId());

        if (scheduledActionCount > 0) {
            final Slice<JpaAction> scheduledActions = findScheduledActionsByRollout(rollout, Math.toIntExact(scheduledActionCount));
            log.debug("Retrieved {} scheduled actions for deletion", scheduledActions.getNumberOfElements());

            if (scheduledActions.getNumberOfElements() > 0) {
                try {
                    final Iterable<JpaAction> iterable = scheduledActions::iterator;
                    final List<Long> actionIds = StreamSupport.stream(iterable.spliterator(), false)
                            .map(Action::getId)
                            .collect(Collectors.toList());

                    log.debug("Deleting scheduled actions with IDs={}", actionIds);
                    actionRepository.deleteByIdIn(actionIds);

                    log.debug("Scheduling RolloutUpdatedEvent after commit for rolloutId={}", rollout.getId());
                    afterCommit.afterCommit(() -> eventPublisherHolder.getEventPublisher()
                            .publishEvent(new RolloutUpdatedEvent(rollout, eventPublisherHolder.getApplicationId())));

                    log.debug("Flushing entity manager to ensure scheduled actions are deleted");
                    entityManager.flush();
                } catch (final RuntimeException e) {
                    LOGGER.error("Exception during deletion of actions of rollout {}", rollout, e);
                }
            }
        } else {
            log.debug("No scheduled actions found for rolloutId={}", rollout.getId());
        }
    }

    private void sendRolloutGroupDeletedEvents(final JpaRollout rollout) {
        log.debug("Starting to send RolloutGroupDeletedEvents for rolloutId={}", rollout.getId());
        final List<Long> groupIds = rollout.getRolloutGroups().stream()
                .map(RolloutGroup::getId)
                .collect(Collectors.toList());

        log.debug("Found {} rollout groups to delete for rolloutId={}", groupIds.size(), rollout.getId());
        afterCommit.afterCommit(() -> {
            groupIds.forEach(rolloutGroupId -> {
                log.debug("Publishing RolloutGroupDeletedEvent for groupId={}", rolloutGroupId);
                eventPublisherHolder.getEventPublisher()
                        .publishEvent(new RolloutGroupDeletedEvent(
                                tenantAware.getCurrentTenant(),
                                rolloutGroupId,
                                JpaRolloutGroup.class,
                                eventPublisherHolder.getApplicationId()
                        ));
            });
        });
        log.debug("RolloutGroupDeletedEvents scheduled for commit for rolloutId={}", rollout.getId());
    }

    private Slice<JpaAction> findScheduledActionsByRollout(final JpaRollout rollout, final int noOfRecords) {
        return actionRepository.findByRolloutIdAndStatusAndActive(PageRequest.of(0, noOfRecords), rollout.getId(),
                DeviceActionStatus.USER_SCHEDULED, true);
    }

    @Transactional
    @TraceableMethod
    public void handleRunningRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("handleRunningRollout called for rollout {},rolloutName={}", rollout.getId(), rollout.getName());

        final List<JpaRolloutGroup> rolloutGroupsRunning = rolloutGroupRepository.findByRolloutAndStatus(rollout,
                RolloutGroupStatus.RUNNING);

        if (rolloutGroupsRunning.isEmpty()) {
            executeLatestRolloutGroup(rollout);
        } else {
            LOGGER.debug("Rollout {},rolloutName={}has {} running groups", rollout.getId(), rollout.getName(), rolloutGroupsRunning.size());
            executeRolloutGroups(rollout, rolloutGroupsRunning);
        }

        // Update rollout groups that are in DRAFT or READY status to progress them
        updateDraftAndReadyRolloutGroups(rollout);

        if (isRolloutComplete(rollout)) {
            LOGGER.info("Rollout {},rolloutName={}is finished, setting FINISHED status", rollout, rollout.getName());
            RolloutSchedulerUtils.updateRolloutStatus(rollout, RolloutStatus.FINISHING, rolloutRepository);
        }
    }

    private boolean isRolloutComplete(final JpaRollout rollout) {
        // ensure that changes in the same transaction count
        entityManager.flush();
        final Long groupsActiveLeft = rolloutGroupRepository.countByRolloutIdAndStatusOrStatus(rollout.getId(),
                RolloutGroupStatus.RUNNING, RolloutGroupStatus.QUEUED);
        return groupsActiveLeft == 0;
    }

    private void executeLatestRolloutGroup(final JpaRollout rollout) {
        final List<JpaRolloutGroup> latestRolloutGroup = rolloutGroupRepository
                .findByRolloutAndStatusNotOrderByIdDesc(rollout, RolloutGroupStatus.QUEUED);
        if (latestRolloutGroup.isEmpty()) {
            return;
        }
        executeRolloutGroupSuccessAction(rollout, latestRolloutGroup.get(0));
    }

    private void executeRolloutGroupSuccessAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        evaluationManager.getSuccessActionEvaluator(rolloutGroup.getSuccessAction()).exec(rollout, rolloutGroup);
    }


    private void executeRolloutGroups(final JpaRollout rollout, final List<JpaRolloutGroup> rolloutGroups) {
        for (final JpaRolloutGroup rolloutGroup : rolloutGroups) {

            final long targetCount = countTargetsFrom(rolloutGroup);
            if (rolloutGroup.getTotalTargets() != targetCount) {
                updateTotalTargetCount(rolloutGroup, targetCount);
            }

            // error state check, do we need to stop the whole
            // rollout because of error?
            final boolean isError = checkErrorState(rollout, rolloutGroup);
            if (isError) {
                LOGGER.info("Rollout {} {} has error, calling error action", rollout.getName(), rollout.getId());
                callErrorAction(rollout, rolloutGroup);
            } else {
                // not in error so check finished state, do we need to
                // start the next group?
                final RolloutGroup.RolloutGroupSuccessCondition finishedCondition = rolloutGroup.getSuccessCondition();
                evaluateAndExecuteGroupSuccessAction(rollout, rolloutGroup, finishedCondition);
                if (isRolloutGroupComplete(rollout, rolloutGroup)) {
                    RolloutSchedulerUtils.updateRolloutGroupStatus(rolloutGroup, RolloutGroupStatus.FINISHING, rolloutGroupRepository);
                }
            }
        }
    }

    private void evaluateAndExecuteGroupSuccessAction(final Rollout rollout, final RolloutGroup rolloutGroup,
                                                      final RolloutGroup.RolloutGroupSuccessCondition successionCondition) {
        LOGGER.trace("Checking finish condition {} on rolloutgroup {}", successionCondition, rolloutGroup);
        try {
            final boolean canProceedToNextGroup = evaluationManager.getSuccessConditionEvaluator(successionCondition).eval(rollout,
                    rolloutGroup, rolloutGroup.getSuccessConditionExp());
            if (canProceedToNextGroup) {
                LOGGER.debug("Rolloutgroup {} is finished, starting next group", rolloutGroup);
                executeRolloutGroupSuccessAction(rollout, rolloutGroup);
            } else {
                LOGGER.debug("Rolloutgroup {} is still running", rolloutGroup);
            }
        } catch (final EvaluatorNotConfiguredException e) {
            LOGGER.error("Something bad happened when accessing the finish condition or success action bean {}",
                    successionCondition.name(), e);
        }
    }

    private void updateTotalTargetCount(final JpaRolloutGroup rolloutGroup, final long countTargetsOfRolloutGroup) {
        final JpaRollout jpaRollout = (JpaRollout) rolloutGroup.getRollout();
        final long updatedTargetCount = jpaRollout.getTotalTargets()
                - (rolloutGroup.getTotalTargets() - countTargetsOfRolloutGroup);
        RolloutSchedulerUtils.updatedRolloutAndGroupsTotalTargetCount(jpaRollout, rolloutGroup, updatedTargetCount, countTargetsOfRolloutGroup, rolloutRepository, rolloutGroupRepository);
    }

    private long countTargetsFrom(final JpaRolloutGroup rolloutGroup) {
        return rolloutGroupManagement.countTargetsOfRolloutsGroup(rolloutGroup.getId());
    }

    private void callErrorAction(final Rollout rollout, final RolloutGroup rolloutGroup) {
        try {
            evaluationManager.getErrorActionEvaluator(rolloutGroup.getErrorAction()).exec(rollout, rolloutGroup);
        } catch (final EvaluatorNotConfiguredException e) {
            LOGGER.error("Something bad happened when accessing the error action bean {}",
                    rolloutGroup.getErrorAction().name(), e);
        }
    }

    private boolean isRolloutGroupComplete(final JpaRollout rollout, final JpaRolloutGroup rolloutGroup) {
        final Long actionsLeftForRollout = actionRepository.countByRolloutAndRolloutGroupAndStatusNotInAndActive(rollout, rolloutGroup,
                ROLLOUT_FINISH_STATUS_LIST, true);

        return actionsLeftForRollout == 0;
    }

    private boolean checkErrorState(final Rollout rollout, final RolloutGroup rolloutGroup) {

        final RolloutGroup.RolloutGroupErrorCondition errorCondition = rolloutGroup.getErrorCondition();

        if (errorCondition == null) {
            // there is no error condition, so return false, don't have error.
            return false;
        }
        try {
            return evaluationManager.getErrorConditionEvaluator(errorCondition).eval(rollout, rolloutGroup,
                    rolloutGroup.getErrorConditionExp());
        } catch (final EvaluatorNotConfiguredException e) {
            LOGGER.error("Something bad happened when accessing the error condition bean {}", errorCondition.name(), e);
            return false;
        }
    }

    /**
     * Responsible for handling the PAUSING of a rollout and updating rollout to PAUSED state
     * retrieves the IDs of paused rollout groups
     * retrieves the target and action IDs associated with these rollout groups
     * processes the actions by setting their status to pausing
     * sets the status of these rollout groups to pausing
     * if all actions are in PAUSING, DD_SENT, CANCELED, FINISHED then update the actions for each rolloutGroup to PAUSED and then update rolloutGroups to PAUSED
     * if all rolloutGroups are PAUSED, CANCELED and FINISHED sets the rollout status to PAUSED
     *
     * @param rollout
     */
    @Transactional
    @TraceableMethod
    public void handlePausingRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("handlePausingRollout called for rollout {}, rolloutName={}", rollout.getId(), rollout.getName());

        List<Long> rolloutGroupsList = getRolloutGroupsByListOfStatuses(rollout, RUNNING_GROUP);

        if (!rolloutGroupsList.isEmpty()) {
            processActionsForGroup(rolloutGroupsList, RUNNING_DEVICE_ACTION, DeviceActionStatus.PAUSING, RolloutGroupStatus.PAUSING, true);
            finalizeRolloutStateChange(rolloutGroupsList, rollout, DeviceActionStatus.PAUSING, PAUSING_DEVICE_ACTION_STATUSES, DeviceActionStatus.PAUSED, RolloutGroupStatus.PAUSED, PAUSE_ROLLOUT_GROUPS, RolloutStatus.PAUSED);
        }
    }

    /**
     * Responsible for handling the resuming of a rollout
     * retrieves the IDs of paused rolloutGroups
     * sets the status of these rollout groups to resuming
     * retrieves the target and action IDs associated with these rollout groups
     * processes the actions by setting their status to resuming
     * if all actions are in resuming, then update the actions to running and then update rolloutGroups to running
     * if all rolloutGroups are resuming, sets the rollout status to running
     *
     * @param rollout
     */
    @Transactional
    @TraceableMethod
    public void handleResumingRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("handleResumingRollout called for rollout {}, rolloutName={}", rollout.getId(), rollout.getName());

        List<Long> rolloutGroupsList = getRolloutGroupsByListOfStatuses(rollout, PAUSED_GROUP);
        if (!rolloutGroupsList.isEmpty()) {
            processActionsForGroup(rolloutGroupsList, PAUSED_DEVICE_ACTION, DeviceActionStatus.RESUMING, RolloutGroupStatus.RESUMING, true);
            finalizeRolloutStateChange(rolloutGroupsList, rollout, DeviceActionStatus.RESUMING, RESUMING_DEVICE_ACTION, DeviceActionStatus.RUNNING, RolloutGroupStatus.RUNNING, RESUMING_ROLLOUT_GROUPS, RolloutStatus.RUNNING);
        }
    }

    /**
     * Responsible for handling the cancellation of a rollout
     * retrieves the IDs of paused, running, and queued rollout groups
     * sets the status of these rollout groups to canceling
     * retrieves the target and action IDs associated with these rollout groups
     * processes the actions by setting their status to canceling
     * if all actions are in canceling, then update the actions to canceled and then update rolloutGroups to canceled
     * if all rolloutGroups are canceled, sets the rollout status to canceled
     *
     * @param rollout
     */
    @Transactional
    @TraceableMethod
    public void handleCancelRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("handleCancelRollout called for rollout {},rolloutName={}", rollout.getId(), rollout.getName());

        List<Long> rolloutGroupsList = getRolloutGroupsByListOfStatuses(rollout, STATES_TO_BE_CANCEL);

        if (!rolloutGroupsList.isEmpty()) {
            processActionsForGroup(rolloutGroupsList, CURRENT_STATUSES_FOR_CANCELING, DeviceActionStatus.CANCELING, RolloutGroupStatus.CANCELING, false);
            finalizeRolloutStateChange(rolloutGroupsList, rollout, DeviceActionStatus.CANCELING, CANCELING_DEVICE_ACTION_STATUSES, DeviceActionStatus.CANCELED, RolloutGroupStatus.CANCELED, CANCELED_GROUP, RolloutStatus.CANCELED);
        }
    }

    /**
     * Handles the finishing of a rollout by updating the status of rollout groups and the rollout itself.
     * Sets rollout groups to FINISHED and verifies all groups are updated before finalizing the rollout.
     *
     * @param rollout the rollout to be finished
     */
    @Transactional
    @TraceableMethod
    public void handleFinishingRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("handleFinishingRollout called for rollout {}, rolloutName={}", rollout.getId(), rollout.getName());

        List<Long> rolloutGroups = getRolloutGroupsByListOfStatuses(rollout, List.of(RolloutGroupStatus.FINISHING));
        RolloutSchedulerUtils.setRolloutGroupsStatus(rolloutGroups, RolloutGroupStatus.FINISHED, rolloutGroupRepository);
        if (RolloutSchedulerUtils.checkIfAllGroupsUpdated(rollout, List.of(RolloutGroupStatus.FINISHED), rolloutGroupRepository)) {
            RolloutSchedulerUtils.updateRolloutStatus(rollout, RolloutStatus.FINISHED, rolloutRepository);
            sendRolloutStatusEvent(rollout, "INFO", rollout.getStatus(), Collections.emptyList(), Collections.emptyList());
        } else {
            throw new RolloutIllegalStateException("Not all groups are in the expected state");
        }

    }

    /**
     * Deactivates all actions associated with the specified rollout group.
     * Actions are processed in batches for efficiency. Each action's "active" flag is set to false.
     * Logs the progress and completion of the deactivation process.
     *
     * @param rolloutGroupId the ID of the rollout group whose actions should be deactivated
     */
    private void deactivateAllActionsForRolloutGroup(Long rolloutGroupId) {
        LOGGER.debug("Initiating deactivation of actions for rolloutGroupId={}", rolloutGroupId);
        int currentPage = FIRST_PAGE_NUMBER;
        Page<JpaAction> actionsBatch;
        int totalDeactivated = 0;

        do {
            actionsBatch = RolloutSchedulerUtils.getActionsByRolloutGroup(
                    List.of(rolloutGroupId), PageRequest.of(currentPage, PAGE_SIZE), actionRepository);

            if (!actionsBatch.isEmpty()) {
                actionsBatch.forEach(action -> action.setActive(false));
                actionRepository.saveAll(actionsBatch.getContent());
                totalDeactivated += actionsBatch.getNumberOfElements();
                LOGGER.info("Deactivated {} actions in batch for rolloutGroupId={}", actionsBatch.getNumberOfElements(), rolloutGroupId);
            }
            currentPage++;
        } while (actionsBatch.hasNext());

        LOGGER.info("Completed deactivation of actions for rolloutGroupId={}. Total deactivated: {}", rolloutGroupId, totalDeactivated);
    }

    /**
     * Processes a full retry for the given rollout.
     * Finds eligible rollout groups (in FINISHED or CANCELED state), sets them to RETRYING,
     * deactivates all actions, validates that all actions are inactive, then sets groups to RETRY.
     * If all groups are in RETRY, updates the rollout status to RETRY.
     * Throws RolloutIllegalStateException if any group fails to reach the expected state.
     *
     * @param rollout the rollout entity to process for full retry
     * @throws RolloutIllegalStateException if not all groups reach the RETRY state
     */
    @Transactional
    @TraceableMethod
    public void handleFullRetryMode(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("Starting handleFullRetryMode for rolloutId={}, rolloutName={}", rollout.getId(), rollout.getName());

        // Step 1: Identify rollout groups eligible for retry (those in FINISHED or CANCELED state)
        List<Long> groupIds = findEligibleGroupsForFullRetry(rollout);

        // Step 2: Set eligible groups to RETRYING status
        LOGGER.info("Setting groups {} to RETRYING for rolloutId={}", groupIds, rollout.getId());
        RolloutSchedulerUtils.setRolloutGroupsStatus(groupIds, RolloutGroupStatus.RETRYING, rolloutGroupRepository);

        // Step 3: For each group, deactivate all actions and validate inactivity
        for (Long groupId : groupIds) {
            deactivateAllActionsForRolloutGroup(groupId);
            validateAllActionsInactiveForGroup(rollout, groupId);

            // Step 4: Move group to RETRY status after successful deactivation
            RolloutSchedulerUtils.setRolloutGroupsStatus(List.of(groupId), RolloutGroupStatus.RETRY, rolloutGroupRepository);
            LOGGER.info("Setting groupId={} to RETRY after deactivating actions", groupId);
        }

        // Step 5: Ensure all groups are now in RETRY state before updating rollout status
        LOGGER.debug("Checking if all groups are in RETRY state for rolloutId={}", rollout.getId());
        if (!RolloutSchedulerUtils.checkIfAllGroupsUpdated(rollout, List.of(RolloutGroupStatus.RETRY), rolloutGroupRepository)) {
            LOGGER.error("Not all groups are in the expected RETRY state for rollout {}", rollout.getId());
            throw new RolloutIllegalStateException("Not all groups are in the expected state");
        }

        // Step 6: Update the rollout status to RETRY
        RolloutSchedulerUtils.updateRolloutStatus(rollout, RolloutStatus.RETRY, rolloutRepository);
        LOGGER.info("Rollout {} status updated to RETRY", rollout.getId());
    }

    /**
     * Validates that all actions for the specified rollout group are inactive.
     * Throws RolloutIllegalStateException if any active actions are found.
     * Logs the validation process and any warnings.
     *
     * @param rolloutEntity  the rollout entity containing the group
     * @param rolloutGroupId the ID of the rollout group to check
     * @throws RolloutIllegalStateException if any actions are still active
     */
    public void validateAllActionsInactiveForGroup(JpaRollout rolloutEntity, Long rolloutGroupId) {
        LOGGER.debug("Validating all actions are inactive for rolloutId={}, rolloutGroupId={}", rolloutEntity.getId(), rolloutGroupId);
        boolean allActionsInactive = actionRepository.findByRolloutIdAndRolloutGroupId(rolloutEntity.getId(), rolloutGroupId)
                .stream().noneMatch(JpaAction::isActive);
        if (!allActionsInactive) {
            LOGGER.error("Active actions found for rolloutGroupId={}", rolloutGroupId);
            throw new RolloutIllegalStateException("Active actions exist for rolloutGroupId=" + rolloutGroupId);
        }
        LOGGER.info("All actions are inactive for rolloutGroupId={}", rolloutGroupId);
    }

    /**
     * Retrieves the IDs of rollout groups eligible for a full retry operation.
     * Eligible groups are those in either CANCELED or FINISHED status.
     * Throws RolloutIllegalStateException if no eligible groups are found.
     *
     * @param rollout the rollout entity to check for eligible groups
     * @return list of eligible rollout group IDs for retry
     * @throws RolloutIllegalStateException if no eligible groups are found
     */
    private List<Long> findEligibleGroupsForFullRetry(JpaRollout rollout) {
        List<Long> eligibleGroupIds = getRolloutGroupsByListOfStatuses(
                rollout, List.of(RolloutGroupStatus.CANCELED, RolloutGroupStatus.FINISHED));
        LOGGER.debug("Eligible rollout group IDs for full retry in rolloutId={}: {}", rollout.getId(), eligibleGroupIds);

        if (eligibleGroupIds.isEmpty()) {
            LOGGER.error("No eligible rollout groups found for full retry in rolloutId={}", rollout.getId());
            throw new RolloutIllegalStateException("Not all groups are in the expected state");
        }
        LOGGER.info("Found {} eligible rollout groups for full retry in rolloutId={}", eligibleGroupIds.size(), rollout.getId());
        return eligibleGroupIds;
    }

    /**
     * Processes the retry operation for the given rollout.
     * Validates that all rollout groups are in RETRY state and all actions are inactive.
     * If validation passes, updates the rollout status to FREEZING.
     * Throws RolloutIllegalStateException if any group is not in RETRY state or has active actions.
     *
     * @param rollout the rollout entity to process for retry
     * @throws RolloutIllegalStateException if validation fails
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TraceableMethod
    public void handleRetryRollout(@TraceableObject final JpaRollout rollout) {
        LOGGER.debug("Starting retry process for rolloutId={}, rolloutName={}", rollout.getId(), rollout.getName());

        // Step 1: Ensure all groups are in RETRY state
        if (!RolloutSchedulerUtils.checkIfAllGroupsUpdated(rollout, List.of(RolloutGroupStatus.RETRY), rolloutGroupRepository)) {
            LOGGER.error("Not all groups are in the expected RETRY state for rollout {}", rollout.getId());
            throw new RolloutIllegalStateException("Not all groups are in the expected state");
        }

        // Step 2: Get all group IDs in RETRY state
        List<Long> groups = getRolloutGroupsByListOfStatuses(rollout, List.of(RolloutGroupStatus.RETRY));
        if (groups.isEmpty()) {
            LOGGER.error("No rollout groups found for full retry in rolloutId={}", rollout.getId());
            throw new RolloutIllegalStateException("Not all groups are in the expected state");
        }

        // Step 3: Validate all actions are inactive for each group
        for (Long group : groups) {
            validateAllActionsInactiveForGroup(rollout, group);
        }

        // Step 4: Update rollout status to FREEZING
        RolloutSchedulerUtils.updateRolloutStatus(rollout, RolloutStatus.FREEZING, rolloutRepository);
        LOGGER.info("Rollout ID {} status set to FREEZING.", rollout.getId());
    }

    /**
     * Orchestrates the action flagging phase when a rollout moves to RETRYING status.
     * Delegates to specific handlers based on the last RetryMode.
     *
     * @param rollout The JpaRollout entity in RETRYING status.
     */
    @Transactional
    public void handleRetryingRollout(final JpaRollout rollout) {
        LOGGER.info("Starting handleRetryingRollout orchestration for rollout {}", rollout.getId());

        JpaRollout currentRollout = rolloutRepository.findById(rollout.getId())
                .orElseThrow(() -> new EntityNotFoundException(JpaRollout.class, rollout.getId()));

        if (currentRollout.getLastRetryMode() == null) {
            LOGGER.error("Rollout {} is in RETRYING status but lastRetryMode is NULL.", rollout.getId());
            return;
        }

        switch (currentRollout.getLastRetryMode()) {
            case FULL -> handleFullRetryMode(currentRollout);
            case ALL_SUCCEEDED_VEHICLES -> handleAllSucceededVehiclesMode(currentRollout);
            case ALL_FAILED_VEHICLES -> handleAllFailedVehiclesMode(currentRollout);
            case ALL_CANCELED_VEHICLES -> handleAllCanceledVehiclesMode(currentRollout);
            case ALL_NOT_EXECUTED_VEHICLES -> handleAllNotExecutedVehiclesMode(currentRollout);
            default -> {
                LOGGER.error("Unsupported retry mode '{}' for rollout ID: {}. Halting orchestration.",
                        currentRollout.getLastRetryMode(), currentRollout.getId());
                throw new IllegalStateException("Unsupported retry mode: " + currentRollout.getLastRetryMode());
            }
        }

        LOGGER.info("Completed handleRetryingRollout orchestration for rollout {}", currentRollout.getId());
    }

    private void handleAllSucceededVehiclesMode(JpaRollout rollout) {
        List<Integer> statusesToFilter = deviceRetryService.getActionStatusesToRetry(RetryMode.ALL_SUCCEEDED_VEHICLES);
        processTargetedActions(rollout, statusesToFilter);
    }

    private void handleAllFailedVehiclesMode(JpaRollout rollout) {
        List<Integer> statusesToFilter = deviceRetryService.getActionStatusesToRetry(RetryMode.ALL_FAILED_VEHICLES);
        processTargetedActions(rollout, statusesToFilter);
    }

    private void handleAllCanceledVehiclesMode(JpaRollout rollout) {
        List<Integer> statusesToFilter = deviceRetryService.getActionStatusesToRetry(RetryMode.ALL_CANCELED_VEHICLES);
        processTargetedActions(rollout, statusesToFilter);
    }

    private void handleAllNotExecutedVehiclesMode(JpaRollout rollout) {
        List<Integer> statusesToFilter = deviceRetryService.getActionStatusesToRetry(RetryMode.ALL_NOT_EXECUTED_VEHICLES);
        processTargetedActions(rollout, statusesToFilter);
    }


    /**
     * CORE LOGIC: Finds actions based on filtered statuses, flags them for retry,
     * and finally transitions the Rollout to RETRY status.
     * * NOTE: This implementation uses the less efficient, multi-query approach
     * with 'getActionsAndGroupsByRolloutAndStatus' as requested by the user.
     */
    private void processTargetedActions(JpaRollout rollout, List<Integer> statusesToFilter) {

        List<JpaAction> actionsToRetry = new ArrayList<>();
        for (Integer statusValue : statusesToFilter) {
            List<JpaAction> foundActions = actionRepository.getActionsAndGroupsByRolloutAndStatus(
                    rollout.getId(),
                    statusValue
            );
            actionsToRetry.addAll(foundActions);
        }

        LOGGER.debug("Found {} total actions for retry for rollout {}", actionsToRetry.size(), rollout.getId());

        int updatedCount = 0;
        RetryMode retryMode = rollout.getLastRetryMode();

        for (JpaAction action : actionsToRetry) {
            try {
                deviceRetryService.updateActionAndTargetGroup(action.getId(), retryMode);
                updatedCount++;
            } catch (OptimisticLockException e) {
                LOGGER.error("Failed to update action {} due to concurrent access (OptimisticLockException).", action.getId());
            } catch (Exception e) {
                LOGGER.error("Fatal error processing action {}: {}", action.getId(), e.getMessage());
            }
        }

        try {
            deviceRetryService.moveRolloutToRetryStatus(rollout.getId(), updatedCount);
        } catch (Exception e) {
            LOGGER.error("FATAL: Failed to update Rollout status to RETRY for {}. Reason: {}",
                    rollout.getId(), e.getMessage());
        }
    }

    /**
     * Processes actions for a list of rollout groups by updating their status and the status of the associated actions.
     *
     * @param rolloutGroupsList List of rollout group IDs
     * @param currentStatuses   List of current action statuses to check against
     * @param setDeviceStatus   New status to set for the actions
     * @param setGroupStatus    New status to set for the rollout groups
     */
    public void processActionsForGroup(List<Long> rolloutGroupsList, List<DeviceActionStatus> currentStatuses, DeviceActionStatus setDeviceStatus, RolloutGroupStatus setGroupStatus, boolean active) {
        Page<JpaAction> actionPage;
        for (Long groupId : rolloutGroupsList) {
            int page = FIRST_PAGE_NUMBER;

            log.debug("Starting action status update for RolloutGroup ID: {}", groupId);

            do {
                actionPage = RolloutSchedulerUtils.getActionsByRolloutGroup(List.of(groupId),
                        PageRequest.of(page, PAGE_SIZE), actionRepository);

                log.debug("Processing page {} with {} actions for RolloutGroup ID: {}", page, actionPage.getNumberOfElements(), groupId);

                actionPage.forEach(action -> RolloutSchedulerUtils.processActions(action, currentStatuses, setDeviceStatus, actionRepository, active));
                log.debug("Action ID {} updated to status: {}", actionPage.getContent().listIterator(), setDeviceStatus);

                page++;
            } while (actionPage.hasNext());

            log.debug("Completed update of all action statuses to '{}' for RolloutGroup ID: {}", setDeviceStatus, groupId);

            RolloutSchedulerUtils.setRolloutGroupsStatus(List.of(groupId), setGroupStatus, rolloutGroupRepository);
            log.debug("RolloutGroup ID {} status set to '{}'.", groupId, setGroupStatus);
        }
    }

    /**
     * Finalizes the state change of a rollout by checking the status of actions and rollout groups accordingly.
     *
     * @param rolloutGroupsList
     * @param rollout
     * @param checkDeviceStatuses
     * @param setDeviceStatus
     * @param setGroupStatus
     * @param checkRolloutGroupStatus
     * @param setRolloutStatus
     */
    public void finalizeRolloutStateChange(
            List<Long> rolloutGroupsList,
            JpaRollout rollout,
            DeviceActionStatus checkDeviceActionStatusesForProcessRolloutGroups,
            List<DeviceActionStatus> checkDeviceStatuses,
            DeviceActionStatus setDeviceStatus,
            RolloutGroupStatus setGroupStatus,
            List<RolloutGroupStatus> checkRolloutGroupStatus,
            RolloutStatus setRolloutStatus) {
        List<JpaAction> updatedActions = RolloutSchedulerUtils.getActionsByRolloutGroup(rolloutGroupsList,
                PageRequest.of(0, PAGE_SIZE), actionRepository).getContent();
        log.debug("Fetched {} actions for {} rollout groups", updatedActions.size(), rolloutGroupsList.size());

        if (RolloutSchedulerUtils.checkAllActionsIfCompleted(updatedActions, checkDeviceStatuses)) {
            log.debug("All actions are in the expected completed statuses: {}", checkDeviceStatuses);

            RolloutSchedulerUtils.processRolloutGroups(rolloutGroupsList, checkDeviceActionStatusesForProcessRolloutGroups, setDeviceStatus, setGroupStatus, rolloutGroupRepository, actionRepository);
            log.debug("Processed rollout groups with status update: {}", setGroupStatus);

            if (RolloutSchedulerUtils.checkIfAllGroupsUpdated(rollout, checkRolloutGroupStatus, rolloutGroupRepository)) {
                log.debug("All rollout groups are now in the expected status: {}", checkRolloutGroupStatus);

                RolloutSchedulerUtils.updateRolloutStatus(rollout, setRolloutStatus, rolloutRepository);
                log.debug("Rollout {} status updated to: {}", rollout.getId(), setRolloutStatus);
            } else {
                log.warn("Not all rollout groups are in the expected status: {}", checkRolloutGroupStatus);
                throw new RolloutIllegalStateException("Not all groups are in the expected state");
            }
        } else {
            log.warn("Not all actions are in the expected status: {}", checkDeviceStatuses);
            throw new RolloutIllegalStateException("Not all actions are in the expected state");
        }

    }

    /**
     * Retrieves the rollout groups by status
     *
     * @param rolloutId
     * @return
     */
    public List<Long> getRolloutGroupsByListOfStatuses(JpaRollout rolloutId, List<RolloutGroupStatus> statuses) {
        final List<JpaRolloutGroup> rolloutGroups = RolloutSchedulerUtils.getRolloutGroupsByRolloutId(rolloutId, rolloutGroupRepository);
        return rolloutGroups.stream()
                .filter(group -> statuses.contains(group.getStatus()))
                .map(JpaRolloutGroup::getId)
                .toList();
    }

    /**
     * Updates rollout groups with status DRAFT or READY for the given rollout.
     * <p>
     * This method performs the following steps:
     * <ul>
     *     <li>Fetches all rollout groups with status DRAFT or READY associated with the given rollout.</li>
     *     <li>Retrieves all targets associated with those rollout groups.</li>
     *     <li>Builds mappings of targets to the rollout groups.</li>
     *     <li>Calls the rollout management service to finalize the rollout groups.</li>
     * </ul>
     * </p>
     *
     * @param rollout the {@link JpaRollout} entity for which draft rollout groups should be processed; must not be {@code null}.
     */
    private void updateDraftAndReadyRolloutGroups(final JpaRollout rollout) {
        // Fetch all rollout groups with status DRAFT or READY for the given rollout
        List<JpaRolloutGroup> groups = rolloutGroupRepository.findByRolloutAndStatusIn(
                rollout, List.of(RolloutGroupStatus.DRAFT, RolloutGroupStatus.READY));
        if (groups.isEmpty()) {
            LOGGER.debug("No draft or ready rollout groups found for rollout ID: {}", rollout.getId());
            return;
        }

        // Extract IDs of these rollout groups
        List<Long> groupIds = groups.stream()
                .map(JpaRolloutGroup::getId)
                .toList();

        // Fetch all targets grouped by rollout group IDs
        List<MgmtRolloutGroupTarget> groupTargets = rolloutTargetGroupRepository.getTargetsGroupedByRolloutGroupId(groupIds);
        if (groupTargets.isEmpty()) {
            LOGGER.debug("No targets associated with draft or ready rollout groups");
            return;
        }

        // Extract target IDs and fetch JpaTargets
        List<Long> targetIds = groupTargets.stream()
                .map(MgmtRolloutGroupTarget::targetId)
                .distinct()
                .toList();

        List<JpaTarget> targets = targetRepository.findAllById(targetIds);

        // Map targetId -> JpaTarget for quick lookup
        Map<Long, JpaTarget> targetMap = targets.stream()
                .collect(Collectors.toMap(JpaTarget::getId, t -> t));

        // Map rolloutGroupId -> List<JpaTarget>
        Map<Long, List<JpaTarget>> targetsByGroupId = new HashMap<>();
        for (MgmtRolloutGroupTarget mgmtTarget : groupTargets) {
            JpaTarget target = targetMap.get(mgmtTarget.targetId());
            if (target != null) {
                targetsByGroupId.computeIfAbsent(mgmtTarget.rolloutGroupId(), k -> new ArrayList<>()).add(target);
            }
        }

        // Prepare list of rollout groups with their assigned targets
        List<AssociatedTargetsToRolloutGroup> assignedTargets = new ArrayList<>();
        for (JpaRolloutGroup group : groups) {
            List<JpaTarget> jpaTargets = targetsByGroupId.getOrDefault(group.getId(), List.of());
            if (!jpaTargets.isEmpty()) {
                // Convert List<JpaTarget> to List<Target>
                List<Target> groupTargetsList = new ArrayList<>(jpaTargets);
                assignedTargets.add(
                        AssociatedTargetsToRolloutGroup.builder()
                                .rolloutGroup(group)
                                .targets(groupTargetsList)
                                .build()
                );
            }
        }

        if (assignedTargets.isEmpty()) {
            LOGGER.debug("No targets were mapped to draft rollout groups");
            return;
        }

        // Finalize rollout groups — promote them to READY and QUEUED as appropriate
        rolloutManagement.updateRolloutGroupsForNewlyRegisteredControllerIds(
                rollout,
                null,
                assignedTargets
        );
    }

    /**
     * Publishes a rollout status event to Kafka.
     *
     * @param rollout       the rollout entity containing rollout details
     * @param type          the event type (e.g., INFO, ERROR)
     * @param status        the rollout status
     * @param errorCodes    optional error codes (null defaults to empty list)
     * @param errorMessages optional error messages (null defaults to empty list)
     */
    private void sendRolloutStatusEvent(JpaRollout rollout,
                                        String type,
                                        RolloutStatus status,
                                        List<String> errorCodes,
                                        List<String> errorMessages) {
        RolloutStatusPayload payload = RolloutStatusPayload.builder()
                .type(type)
                .status(status.toString())
                .errorCode(errorCodes != null ? errorCodes : Collections.emptyList())
                .errorMessages(errorMessages != null ? errorMessages : Collections.emptyList())
                .timestamp(Instant.now().getEpochSecond())
                .build();

        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .rolloutName(rollout.getName())
                .build();

        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(payload)
                .build();

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.ROLLOUT_STATUS);
    }


}
