/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ddi.rest.resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.SignatureException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.annotations.TraceableField;
import org.cosmos.annotations.TraceableMethod;
import org.cosmos.kafka.utils.Constants;
import org.cosmos.models.ddi.DdiActionFeedback;
import org.cosmos.models.ddi.DdiActionFeedbacks;
import org.cosmos.models.ddi.DdiActionHistory;
import org.cosmos.models.ddi.DdiActivateAutoConfirmation;
import org.cosmos.models.ddi.DdiAutoConfirmationState;
import org.cosmos.models.ddi.DdiCancel;
import org.cosmos.models.ddi.DdiCancelActionToStop;
import org.cosmos.models.ddi.DdiChunk;
import org.cosmos.models.ddi.DdiConfigData;
import org.cosmos.models.ddi.DdiConfigDataDevice;
import org.cosmos.models.ddi.DdiConfirmationBase;
import org.cosmos.models.ddi.DdiConfirmationBaseAction;
import org.cosmos.models.ddi.DdiConfirmationFeedback;
import org.cosmos.models.ddi.DdiControllerBase;
import org.cosmos.models.ddi.DdiDeployment;
import org.cosmos.models.ddi.DdiDeploymentBase;
import org.cosmos.models.ddi.DdiDeploymentDescriptor;
import org.cosmos.models.ddi.DdiDeploymentDescriptorBase;
import org.cosmos.models.ddi.DdiDeploymentMetadata;
import org.cosmos.models.ddi.DdiDeploymentMetadataLogs;
import org.cosmos.models.ddi.DdiDeviceInventory;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.ddi.DdiDsMetadata;
import org.cosmos.models.ddi.DdiEcu;
import org.cosmos.models.ddi.DdiFeedbackRequestBody;
import org.cosmos.models.ddi.DdiFeedbackResponse;
import org.cosmos.models.ddi.DdiRequiredStateOfCharge;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiSignatureType;
import org.cosmos.models.ddi.DdiStatus;
import org.cosmos.models.ddi.DdiUpdateMode;
import org.cosmos.models.ddi.DdiUserAcceptanceMessage;
import org.cosmos.models.ddi.DeviceInventoryDetails;
import org.cosmos.models.ddi.EncodedDdiDeploymentDescriptor;
import org.cosmos.models.ddi.InventoryWithAction;
import org.cosmos.models.kafka.InventoryMessage;
import org.cosmos.models.kafka.InventorySignature;
import org.cosmos.models.kafka.RolloutStatusPayload;
import org.cosmos.models.kafka.VehicleStatusMessage;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.PagedList;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.s3.exception.S3Exception;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.artifact.repository.ArtifactFilesystemProperties;
import org.eclipse.hawkbit.ddi.rest.api.DdiRootControllerRestApi;
import org.eclipse.hawkbit.ddi.rest.resource.config.InventoryConfig;
import org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper;
import org.eclipse.hawkbit.exception.GenericSpServerException;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.mgmt.rest.resource.awsServices.S3Service;
import org.eclipse.hawkbit.pagination.PagingUtility;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.ConfirmationManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DeploymentLogManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.PKIManagement;
import org.eclipse.hawkbit.repository.RepositoryConstants;
import org.eclipse.hawkbit.repository.RolloutManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.repository.UpdateMode;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.builder.ActionStatusCreate;
import org.eclipse.hawkbit.repository.exception.CancelActionNotAllowedException;
import org.eclipse.hawkbit.repository.exception.EntityCannotNullException;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.exception.InvalidConfirmationFeedbackException;
import org.eclipse.hawkbit.repository.exception.PackageSizeLimitExceededException;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.ActionRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetModuleRepository;
import org.eclipse.hawkbit.repository.jpa.EspRepository;
import org.eclipse.hawkbit.repository.jpa.RspRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionStatus;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.jpa.utils.SupportPackageManagementUtil;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DeploymentLogUpload;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Polling;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SoftwareOfTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.rest.aspect.VehicleTenantAware;
import org.eclipse.hawkbit.rest.util.RequestResponseContextHolder;
import org.eclipse.hawkbit.security.HawkbitSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.eclipse.hawkbit.utils.TenantConfigHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.bus.BusProperties;
import org.springframework.cloud.bus.ServiceMatcher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.multipart.MultipartFile;

import static org.eclipse.hawkbit.ddi.rest.resource.DataConversionHelper.generateRsp;
import static org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper.decodeAndCreateDeviceInventory;
import static org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper.encodeDeploymentDescriptor;
import static org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper.validateDeviceInventoryAndCreateTargetAttributes;
import static org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper.validateDeviceInventorySignatureDetails;
import static org.eclipse.hawkbit.ddi.rest.resource.helper.DdiApiHelper.validateInventorySignature;
import static org.eclipse.hawkbit.repository.RepositoryConstants.INVENTORY_HASH_KEY;


/**
 * The {@link DdiRootController} of the hawkBit server DDI API that is queried
 * by the hawkBit controller in order to pull {@link Action}s that have to be
 * fulfilled and report status updates concerning the {@link Action} processing.
 * <p>
 * Transactional (read-write) as all queries at least update the last poll time.
 */
@RestController
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Tag(name = "DDI Root Controller")
@Slf4j
public class DdiRootController implements DdiRootControllerRestApi {

    public static final String PENDING_LOG_DETAIL = "Pending Logs";
    private static final String GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET = "given action ({}) is not assigned to given target ({}).";
    private static final String FALLBACK_REMARK = "Initiated using the Device Direct Integration API without providing a remark.";
    private static final String LOCATION_HEADER = "Location";
    private static final List<DdiStatus.ExecutionStatus> VALID_EXECUTION_STATUSES = Arrays.asList(
            DdiStatus.ExecutionStatus.DOWNLOAD_IN_PROGRESS,
            DdiStatus.ExecutionStatus.DOWNLOAD_COMPLETED,
            DdiStatus.ExecutionStatus.DOWNLOAD_STARTED,
            DdiStatus.ExecutionStatus.DD_ACCEPTED,
            DdiStatus.ExecutionStatus.LOG_UPLOAD_FAILURE,
            DdiStatus.ExecutionStatus.LOG_UPLOAD_SUCCESS,
            DdiStatus.ExecutionStatus.PENDING_LOGS,
            DdiStatus.ExecutionStatus.LOG_UPLOAD_IN_PROGRESS
    );
    ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
    Validator validator = factory.getValidator();
    @Autowired
    private ConfirmationManagement confirmationManagement;
    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired(required = false)
    private ServiceMatcher serviceMatcher;
    @Autowired
    private BusProperties bus;
    @Autowired
    private ControllerManagement controllerManagement;
    @Autowired
    private EcuModelManagement ecuModelManagement;
    @Autowired
    private DistributionSetManagement distributionSetManagement;
    @Autowired
    private ArtifactsManagement artifactsManagement;
    @Autowired
    private HawkbitSecurityProperties securityProperties;
    @Autowired
    private TenantAware tenantAware;
    @Autowired
    private SystemManagement systemManagement;
    @Autowired
    private ArtifactUrlHandler artifactUrlHandler;
    @Autowired
    private RequestResponseContextHolder requestResponseContextHolder;
    @Autowired
    private EntityFactory entityFactory;
    @Autowired
    private VersionManagement versionManagement;
    @Autowired
    private SupportPackageManagement supportPackageManagement;
    @Autowired
    private InventoryConfig signatureConfig;
    @Autowired
    private DeploymentLogManagement deploymentLogManagement;
    @Autowired
    private VehicleManagement vehicleManagement;
    @Autowired
    private KafkaMessageService kafkaMessageService;
    @Autowired
    private ActionRepository actionRepository;
    @Autowired
    private EspRepository espRepository;
    @Autowired
    private RspRepository rspRepository;
    @Autowired
    private RolloutManagement rolloutManagement;
    @Autowired
    private ArtifactFilesystemProperties artifactFilesystemProperties;
    @Autowired
    private S3Service s3Service;
    @Autowired
    private ActionArtifactRepository actionArtifactRepository;
    @Autowired
    private DistributionSetModuleRepository distributionSetModuleRepository;
    @Autowired
    private PKIManagement pkiManagement;
    @Autowired
    private TargetManagement targetManagement;
    @Autowired
    private DdiSignatureService ddiSignatureService;
    @Value("${cosmos.server.dd.ecu-certificates.bucket.name}")
    private String ecuCertificatesBucketName;
    @Value("${cosmos.server.dd.ecu-certificates.issuer.name}")
    private String ecuCertificatesIssuerName;
    @Autowired
    private TenantConfigurationManagement tenantConfigurationManagement;
    @Autowired
    private SystemSecurityContext systemSecurityContext;


    private static DdiDeployment.HandlingType calculateDownloadType(final Action action) {
        if (action.isUserAcceptanceNotRequired()) {
            return DdiDeployment.HandlingType.FORCED;
        }
        return DdiDeployment.HandlingType.ATTEMPT;
    }

    private static DdiDeployment.DdiMaintenanceWindowStatus calculateMaintenanceWindow(final Action action) {
        if (action.hasMaintenanceSchedule()) {
            return action.isMaintenanceWindowAvailable() ? DdiDeployment.DdiMaintenanceWindowStatus.AVAILABLE
                    : DdiDeployment.DdiMaintenanceWindowStatus.UNAVAILABLE;
        }
        return null;
    }

    private static DdiDeployment.HandlingType calculateUpdateType(final Action action, final DdiDeployment.HandlingType downloadType) {
        if (action.hasMaintenanceSchedule()) {
            return action.isMaintenanceWindowAvailable() ? downloadType : DdiDeployment.HandlingType.SKIP;
        }
        return downloadType;
    }

    private static DdiDeployment.ConnectivityType getConnectivityType(final Rollout rollout) {
        switch (rollout.getConnectivityType()) {
            case WIFI_ONLY -> {
                return DdiDeployment.ConnectivityType.WIFI;
            }
            case CELLULAR -> {
                return DdiDeployment.ConnectivityType.CELLULAR;
            }
            default -> {
                return DdiDeployment.ConnectivityType.BOTH;
            }
        }
    }

    private static void addMessageIfEmpty(final String text, final List<String> messages) {
        if (messages != null && messages.isEmpty()) {
            messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + text + ".");
        }
    }

    private static ActionStatusCreate generateActionCancelStatus(final DdiActionFeedback feedback, final Target target,
                                                                 final Long actionId, final EntityFactory entityFactory) {

        final ActionStatusCreate actionStatusCreate = entityFactory.actionStatus().create(actionId);

        final List<String> messages = new ArrayList<>();
        final DeviceActionStatus status;
        switch (feedback.getStatus().getExecution()) {
            case CANCELED:
                status = handleCaseCancelCanceled(feedback, target, actionId, messages);
                break;
            case CANCELED_REJECT:
                log.info("Target rejected the cancelation request (actionId: {}, controllerId: {}).", actionId,
                        target.getControllerId());
                status = DeviceActionStatus.CANCELED_REJECT;
                messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX + "Target rejected the cancellation request.");
                break;
            default:
                status = DeviceActionStatus.RUNNING;
                break;
        }

        if (feedback.getStatus().getDetails() != null) {
            messages.addAll(feedback.getStatus().getDetails());
        }

        final Integer code = feedback.getStatus().getCode();
        if (code != null) {
            actionStatusCreate.code(code);
            messages.add("Device reported status code: " + code);
        }

        return actionStatusCreate.status(status).messages(messages);

    }

    private static DeviceActionStatus handleCaseCancelCanceled(final DdiActionFeedback feedback, final Target target,
                                                               final Long actionId, final List<String> messages) {
        final DeviceActionStatus status;
        log.error(
                "Target reported cancel for a cancel which is not supported by the server (actionId: {}, controllerId: {}) as we got {} report.",
                actionId, target.getControllerId(), feedback.getStatus().getExecution());
        status = DeviceActionStatus.CANCELED;
        messages.add(RepositoryConstants.SERVER_MESSAGE_PREFIX
                + "Target reported cancel for a cancel which is not supported by the server.");
        return status;
    }

    /**
     * Retrieve the update mode from the given update message.
     */
    private static UpdateMode getUpdateMode(final DdiConfigData configData) {
        final DdiUpdateMode mode = configData.getMode();
        if (mode != null) {
            return UpdateMode.valueOf(mode.name());
        }
        return null;
    }

    private static DdiDeploymentDescriptor.HandlingType calculateUpdateType(final Action action, final DdiDeploymentDescriptor.HandlingType downloadType) {
        log.debug("Calculating update type for action: {}, downloadType: {}", action.getId(), downloadType);
        if (action.hasMaintenanceSchedule()) {
            log.debug("Action {} has a maintenance schedule.", action.getId());
            boolean isMaintenanceWindowAvailable = action.isMaintenanceWindowAvailable();
            log.debug("Maintenance window available for action {}: {}", action.getId(), isMaintenanceWindowAvailable);
            return isMaintenanceWindowAvailable ? downloadType : DdiDeploymentDescriptor.HandlingType.SKIP;
        }
        log.debug("Action {} does not have a maintenance schedule. Returning downloadType: {}", action.getId(), downloadType);
        return downloadType;
    }

    /**
     * Determines the download type for a given action.
     * This method checks the properties of the action to decide the download type.
     * If the action is download only or is forced or time forced, the download type is set to FORCED.
     * Otherwise, the download type is set to ATTEMPT.
     *
     * @param action The action for which the download type is to be determined.
     * @return The download type for the given action.
     */
    private static DdiDeploymentDescriptor.HandlingType calculateDownloadTypeDescriptor(final Action action) {
        log.debug("Calculating download type descriptor for action: {}", action.getId());
        if (action.isUserAcceptanceNotRequired()) {
            log.debug("Action {} does not require user acceptance. Setting download type to FORCED.", action.getId());
            return DdiDeploymentDescriptor.HandlingType.FORCED;
        }
        log.debug("Action {} requires user acceptance. Setting download type to ATTEMPT.", action.getId());
        return DdiDeploymentDescriptor.HandlingType.ATTEMPT;
    }

    /* Determines the connectivity type for a given rollout.
     * This method checks the connectivity type of the rollout to decide the connectivity type.
     * If the connectivity type is WIFI, the connectivity type is set to WIFI_ONLY.
     * If the connectivity type is CELLULAR, the connectivity type is set to CELLULAR.
     * Otherwise, by default the connectivity type is set to WIFI_PREFERRED.
     *
     * @param rollout The rollout for which the connectivity type is to be determined.
     * @return The connectivity type for the given rollout.
     */
    private static DdiDeploymentMetadata.ConnectivityType getConnectivityTypeDescriptor(final Rollout rollout) {
        switch (rollout.getConnectivityType()) {
            case WIFI_ONLY -> {
                return DdiDeploymentMetadata.ConnectivityType.WIFI_ONLY;
            }
            case CELLULAR -> {
                return DdiDeploymentMetadata.ConnectivityType.CELLULAR;
            }
            default -> {
                return DdiDeploymentMetadata.ConnectivityType.WIFI_PREFERRED;
            }
        }
    }

    private static DdiDeploymentMetadata generateDeploymentMetadata(Action action, Long expirationDate) {
        DdiDeploymentMetadata builder = DdiDeploymentMetadata.builder()
                .requiredStateOfCharge(new DdiRequiredStateOfCharge("dummyKey"))
                .connectivityType(DdiDeploymentMetadata.ConnectivityType.WIFI_PREFERRED)
                .build();
        if (action.getRollout() != null) {
            builder.setConnectivityType(getConnectivityTypeDescriptor(action.getRollout()));
            builder.setEndDate(action.getRollout().getEndAt());
            builder.setDownloadRetryCount(action.getRollout().getDownloadRetryCount());
            builder.setMaxDownloadDurationTimer(action.getRollout().getMaxDownloadDurationTimer());
            builder.setMaxDownloadWifiDurationTimer(action.getRollout().getMaxDownloadWifiDurationTimer());
            builder.setMaxDownloadCellularDurationTimer(action.getRollout().getMaxDownloadCellularDurationTimer());
            builder.setMaxUpdateTime(action.getRollout().getMaxUpdateTime().longValue());
            builder.setEstimatedUpdateTime(action.getRollout().getDeploymentEstimatedUpdateTime());
            builder.setType(action.getRollout().getType());
            if (Boolean.TRUE.equals(action.getRollout().isLogCollectionRequired())) {
                builder.setLogs(DdiDeploymentMetadataLogs.builder().
                        maxNumberOfFiles(action.getRollout().getLogMaxNumberOfFiles())
                        .maxAllFileSize(action.getRollout().getLogMaxAllFileSize()).
                        maxEachFileSize(action.getRollout().getLogMaxEachFileSize()).build());
            }
        }
        // Default value for RequiredMedia will be from CDN
        builder.setRequiredMedia(DdiDeploymentMetadata.RequiredMedia.FROM_CDN);
        // Default value for DownGradeAllowed is NO
        builder.setDowngradeAllowed(DdiDeploymentMetadata.DowngradeAllowed.NO);
        builder.setDdExpiryDate(expirationDate);
        return builder;
    }

    private static boolean isValidExecutionStatus(DdiStatus status) {
        return status.getExecution() != null &&
                (VALID_EXECUTION_STATUSES.contains(status.getExecution()) || validateFinishStatus(status));
    }

    private static boolean isValidUserAcceptanceMessageExecutionStatus(DdiStatus status) {
        boolean isValid = (DdiStatus.ExecutionStatus.USER_SCHEDULED.equals(status.getExecution()) ||
                DdiStatus.ExecutionStatus.USER_ACCEPTED.equals(status.getExecution()) ||
                DdiStatus.ExecutionStatus.USER_IGNORED.equals(status.getExecution())) &&
                status.getUserAcceptanceMessage() != null;

        if (!isValid) {
            log.debug("User Acceptance Message is not provided");
        }

        return isValid;
    }

    private static boolean isValidJob1UserAcceptanceMessageExecutionStatus(DdiStatus status) {
        boolean isValid = (DdiStatus.ExecutionStatus.USER_SCHEDULED.equals(status.getExecution()) ||
                DdiStatus.ExecutionStatus.USER_ACCEPTED.equals(status.getExecution())) &&
                status.getUserAcceptanceMessageJob1() != null;

        if (!isValid) {
            log.debug("Job 1 User Acceptance Message is not provided");
        }

        return isValid;
    }

    private static boolean validateFinishStatus(DdiStatus status) {
        if (status.getExecution().equals(DdiStatus.ExecutionStatus.FINISHED_FAILURE) || status.getExecution().equals(DdiStatus.ExecutionStatus.FINISHED_SUCCESS)) {
            return Objects.nonNull(status.getInventoryHash());
        } else {
            return false;
        }
    }

    /**
     * Checks if the given DdiStatus represents a finished status.
     *
     * @param ddiStatus the DdiStatus to check
     * @return true if the status is either FINISHED_SUCCESS or FINISHED_FAILURE, false otherwise
     */
    private static boolean isFinished(DdiStatus ddiStatus) {
        return DdiStatus.ExecutionStatus.FINISHED_SUCCESS.equals(ddiStatus.getExecution())
                || DdiStatus.ExecutionStatus.FINISHED_FAILURE.equals(ddiStatus.getExecution())
                || DdiStatus.ExecutionStatus.CANCELED_ACCEPT.equals(ddiStatus.getExecution());
    }

    /**
     * Converts a DdiDeploymentDescriptor object into a JSON string and encodes it in Base64.
     *
     * @param deploymentDescriptor the DdiDeploymentDescriptor object to encode
     * @return a Base64-encoded JSON string representing the deployment descriptor
     * @throws RuntimeException if serialization fails
     */
    public static String encodeDeploymentDescriptorToBase64(DdiDeploymentDescriptor deploymentDescriptor) {
        ObjectMapper objectMapper = new ObjectMapper();
        String base64DD;

        try {
            // Convert the object to JSON
            String jsonDD = objectMapper.writeValueAsString(deploymentDescriptor);

            // Encode the JSON in Base64
            base64DD = Base64.getEncoder().encodeToString(jsonDD.getBytes(StandardCharsets.UTF_8));

        } catch (JsonProcessingException e) {
            // log and handle the error
            log.error("Error serializing DdiDeploymentDescriptor", e);
            throw new RuntimeException("Failed to generate Base64 for deployment descriptor", e);
        }

        return base64DD;
    }

    @Override
    @VehicleTenantAware

    @Deprecated
    @TraceableMethod
    //TODO:This APi can be removed once the flow is completed
    public ResponseEntity<DdiDeploymentBase> getControllerBasedeploymentAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                                               @TraceableField @PathVariable("actionId") final Long actionId,
                                                                               @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
                                                                               @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount) {
        log.debug("getControllerBasedeploymentAction({},{})", controllerId, resource);

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        checkAndCancelExpiredAction(action);

        if (!action.isCancelingOrCanceled() && !action.isWaitingConfirmation()
                && (action.getRollout() == null
                || !RolloutStatus.PAUSED.equals(action.getRollout().getStatus()))) {

            final DdiDeploymentBase base = generateDdiDeploymentBase(target, action, actionHistoryMessageCount);

            log.debug("Found an active UpdateAction for target {}. returning deployment: {}", controllerId, base);

            controllerManagement.registerRetrieved(action.getId(), RepositoryConstants.SERVER_MESSAGE_PREFIX
                    + "Target retrieved update action and should start now the download.");

            return new ResponseEntity<>(base, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    @VehicleTenantAware
    @Deprecated
    @TraceableMethod
    public ResponseEntity<DdiFeedbackResponse> postBaseDeploymentActionFeedback(
            @Valid @RequestBody final DdiActionFeedback feedback,
            @TraceableField @PathVariable("controllerId") final String controllerId,
            @TraceableField @PathVariable("actionId") @NotNull final Long actionId) {
        log.debug("Received request to retrieve post base deployment action feedback");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isWaitingConfirmation()) {
            return ResponseEntity.notFound().build();
        }

        if (!action.isActive()) {
            log.warn("Updating action {} with feedback {} not possible since action not active anymore.",
                    action.getId(), feedback.getStatus());
            return new ResponseEntity<>(HttpStatus.GONE);
        }


        DdiFeedbackResponse ddiFeedbackResponse = createDeploymentFeedbackResponse();
        processActionStatusAndNotifyDocg(action, feedback.getStatus(), target, ddiFeedbackResponse);


        return ResponseEntity.ok(ddiFeedbackResponse);
    }

    /**
     * Processes the action status and notifies DOCG (Device Operation Center Gateway).
     * <p>
     * This method updates the action status based on the provided DdiStatus and notifies DOCG if the status is valid.
     * It handles the finished success and failure cases by invoking the respective methods.
     *
     * @param action              The action to be processed.
     * @param ddiStatus           The status of the action.
     * @param target              The target associated with the action.
     * @param ddiFeedbackResponse The feedback response to be updated.
     */
    private void processActionStatusAndNotifyDocg(Action action, DdiStatus ddiStatus, Target target, DdiFeedbackResponse ddiFeedbackResponse) {
        log.debug("Processing action status and notifying DOCG for action: {}, status: {}, target: {}", action.getId(), ddiStatus.getExecution(), target.getControllerId());

        action = handleActionStatusUpdate(action, ddiStatus, target);
        notifyDocg(action, ddiStatus, target);
        handleUserAcceptanceMessage(action, ddiStatus, target);

        if (isFinished(ddiStatus)) {
            log.debug("Handling finished success / finished failure/cancled accept for action: {}", action.getId());
            handleFinishedStatus(ddiStatus, target, action, ddiFeedbackResponse);
        }

        final List<JpaActionStatus> actionStatus = actionRepository.getActionStatusByActionId(action.getId());
        boolean isFinishedSuccess = actionStatus.stream()
                .anyMatch(status -> DeviceActionStatus.FINISHED_SUCCESS.equals(status.getStatus()));

        boolean isFinishedFailure = actionStatus.stream()
                .anyMatch(status -> DeviceActionStatus.FINISHED_FAILURE.equals(status.getStatus()));


        // The device regularly responds back with its status to the offboard using “Intermediate Feedback API”
        // checks if the device feedback was in “FINISHED_SUCCESS“ or “FINISHED_FAILURE” and device is already in CANCELED
        // if yes, then update the action status to FINISHED_SUCCESS OR FINISHED_FAILURE else send cancelField response
        if (action.isCancelingOrCanceled()) {
            log.debug("Action {} is in CANCELED state.", action.getId());
            if (isFinishedSuccess || isFinishedFailure) {
                //TODO:Once the device action scheduler is implemented, we need to change it to FINISHING
                log.debug("Action {} is already in FINISHED_SUCCESS or FINISHED_FAILURE state.", action.getId());
                if (isFinishedSuccess) {
                    action.setStatus(DeviceActionStatus.FINISHED_SUCCESS);
                } else {
                    action.setStatus(DeviceActionStatus.FINISHED_FAILURE);
                }
                actionRepository.save((JpaAction) action);
            } else {
                ddiFeedbackResponse.setCanceledBasedOnExecutionStatus(DdiStatus.ExecutionStatus.CANCELED_ACCEPT);

            }
        }
        if (ddiStatus.getExecution().equals(DdiStatus.ExecutionStatus.CANCELED_REJECT) &&
                !action.isCancelingOrCanceled()) {
            ddiFeedbackResponse.setCanceledBasedOnExecutionStatus(DdiStatus.ExecutionStatus.CANCELED_REJECT);
        }
    }

    /**
     * Handles the user acceptance message for the given action and status.
     * <p>
     * This method processes the user acceptance message and sends the appropriate
     * vehicle status message to DOCG.
     * It handles both the user acceptance message and Job 1 user acceptance message.
     *
     * @param action    the action associated with the user acceptance message
     * @param ddiStatus the status of the action
     * @param target    the target associated with the action
     */
    private void handleUserAcceptanceMessage(Action action, DdiStatus ddiStatus, Target target) {
        //User acceptance message needs different handling, hence didn't add it to isValidExecutionStatus().
        if (isValidUserAcceptanceMessageExecutionStatus(ddiStatus)) {
            log.debug("User acceptance execution status is valid, sending vehicle status message to DOCG.");
            try {
                controllerManagement.addUserAcceptanceStatus(ddiStatus, action);
                sendUserAcceptanceMessageToDOCG(ddiStatus, target.getControllerId(), action);
            } catch (Exception e) {
                log.error("Error while processing user acceptance message: {}", e.getMessage());
                //TODO: In future, send the error message to DOCG on error topic.
            }
        }

        //Job 1 user acceptance message was saved to db above. Relay the same message to DOCG.
        if (isValidJob1UserAcceptanceMessageExecutionStatus(ddiStatus)) {

            String message = switch (ddiStatus.getExecution()) {
                case USER_ACCEPTED -> "User has accepted to proceed with installation";
                case USER_SCHEDULED -> "User has scheduled to proceed with the installation";
                default -> "";
            };

            log.debug(message);
            log.debug("Sending Job 1 user acceptance message to DOCG for target: {}, action: {}", target.getControllerId(), action.getId());
            sendVehicleStatusMessageToDocg(ddiStatus, new String[]{message, ddiStatus.getUserAcceptanceMessageJob1()}, target.getControllerId(), action);
        }
    }

    /**
     * Handles the notification to DOCG for the given action and status.
     * <p>
     * This method checks if the execution status of the provided DdiStatus is valid.
     * If valid, it sends a vehicle status message to DOCG.
     *
     * @param action    the action associated with the status
     * @param ddiStatus the status of the action
     * @param target    the target associated with the action
     */
    private void notifyDocg(Action action, DdiStatus ddiStatus, Target target) {
        if (isValidExecutionStatus(ddiStatus)) {
            log.debug("Status is valid for execution, sending vehicle status message to DOCG.");
            sendVehicleStatusMessageToDocg(ddiStatus, target.getControllerId(), action);
        }
    }

    /**
     * Handles the action status update for the given action and status.
     * <p>
     * This method generates an action status update based on the provided DdiStatus,
     * updates the action status in the table, and returns the updated action.
     *
     * @param action    the action to be updated
     * @param ddiStatus the status of the action
     * @param target    the target associated with the action
     * @return the updated action
     */
    private Action handleActionStatusUpdate(Action action, DdiStatus ddiStatus, Target target) {
        ActionStatusCreate actionStatusCreate = generateUpdateStatus(ddiStatus, target.getControllerId(), action.getId());
        log.debug("Generated action status update: {}", actionStatusCreate);

        Action updatedAction = controllerManagement.addUpdateActionStatus(actionStatusCreate, ddiStatus);
        log.debug("Updated action status in controller management: {}", updatedAction);
        return updatedAction;
    }

    /**
     * Handles the finished status for the given action and status.
     * <p>
     * This method checks if the inventory hash exists for the target. If not, it adds an inventory collection link
     * to the feedback response. It also updates the pending logs status for the target.
     *
     * @param status              the status of the action
     * @param target              the target associated with the action
     * @param action              the action to be processed
     * @param ddiFeedbackResponse the feedback response to be updated
     */
    private void handleFinishedStatus(DdiStatus status, Target target, Action action, DdiFeedbackResponse ddiFeedbackResponse) {
        if (!hashCodeExist(status.getInventoryHash(), target)) {
            log.debug("Inventory mismatch detected for target: {}, adding inventory collection link.", target.getControllerId());
            DataConversionHelper.addInventoryCollectionLink(ddiFeedbackResponse, action);
        }

        log.debug("Updating pending logs status for target: {}", target.getControllerId());
        updatePendingLogsStatus(action, target, ddiFeedbackResponse);
    }

    /**
     * Updates the pending logs status for the given action and target.
     * <p>
     * This method checks if deployment logs are requested for the action and updates the action status
     * to pending logs if necessary. It also notifies DOCG and adds a deployment log link to the feedback response.
     *
     * @param action              the action to be updated
     * @param target              the target associated with the action
     * @param ddiFeedbackResponse the feedback response to be updated
     */
    private void updatePendingLogsStatus(Action action, Target target, DdiFeedbackResponse ddiFeedbackResponse) {
        log.debug("Entering updatePendingLogsStatus with action: {}, target: {}, ddiFeedbackResponse: {}", action.getId(), target.getControllerId(), ddiFeedbackResponse);
        if (DdiApiHelper.isRequestDeploymentLog(action.getRollout(), action.getStatus(), controllerManagement)) {
            List<String> details = List.of(PENDING_LOG_DETAIL);
            log.debug("Setting pending logs status details: {}", details);
            DdiStatus pendingLogsStatus = new DdiStatus(DdiStatus.ExecutionStatus.PENDING_LOGS, details);

            log.debug("Processing action status and notifying DOCG with pending logs status: {}", pendingLogsStatus);
            action = handleActionStatusUpdate(action, pendingLogsStatus, target);
            notifyDocg(action, pendingLogsStatus, target);

            log.debug("Checking if deployment logs are requested for action: {}", action.getId());
            log.debug("Deployment logs requested for target: {}, action: {}", target.getControllerId(), action.getId());
            DataConversionHelper.getDeploymentLogLink(ddiFeedbackResponse, target, action);
        }
    }

    private DdiFeedbackResponse createDeploymentFeedbackResponse() {
        return new DdiFeedbackResponse();
    }

    /**
     * overloaded method to sendVehicleStatusMessageToDocg()
     *
     * @param ddiStatus    the status
     * @param controllerId the controller id
     * @param action       the action
     */
    private void sendVehicleStatusMessageToDocg(DdiStatus ddiStatus, String controllerId, Action action) {
        sendVehicleStatusMessageToDocg(ddiStatus, ddiStatus.getDetails().toArray(String[]::new), controllerId, action);
    }

    /**
     * Sends the vehicle status message to DOCG.
     *
     * @param ddiStatus    the status
     * @param messages     the messages
     * @param controllerId the controller id
     * @param action       the action
     */
    private void sendVehicleStatusMessageToDocg(DdiStatus ddiStatus, String[] messages, String controllerId, Action action) {
        VehicleStatusMessage vehicleStatusMessage = VehicleStatusMessage.builder()
                .vehicleId(controllerId)
                .rolloutName(action.getRollout().getName())
                .status(ddiStatus.getExecution().toString())
                .messages(messages)
                .timestamp(Instant.now().getEpochSecond())
                .build();

        String[] vinParts = controllerId.split("_", 2);
        String vin = vinParts.length > 0 ? vinParts[0] : "";
        String otaMasterSerialNumber = vinParts.length > 1 ? vinParts[1] : "";
        // Build KafkaEventHeader
        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(action.getTarget().getTenant())
                .vin(vin)
                .rolloutName(action.getRollout().getName())
                .otaMasterSerialNumber(otaMasterSerialNumber)
                .build();

        // Wrap in KafkaEventTemplate
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(vehicleStatusMessage)
                .build();

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, Constants.VEHICLE_STATUS);
    }

    /**
     * Builds the message based on the status and sends it to DOCG.
     *
     * @param ddiStatus    contains user acceptance message
     * @param controllerId of the target
     * @param action       the action
     */
    private void sendUserAcceptanceMessageToDOCG(DdiStatus ddiStatus, String controllerId, Action action) {
        DdiUserAcceptanceMessage userAcceptanceMessage = ddiStatus.getUserAcceptanceMessage();
        String message = switch (userAcceptanceMessage.getUserResponse()) {
            case USER_ACCEPTED -> "User has accepted to proceed with installation";
            case USER_IGNORED -> "User has ignored to proceed with installation";
            case USER_SCHEDULED ->
                    "User has scheduled to proceed with the installation for: " + Instant.ofEpochSecond(userAcceptanceMessage.getScheduledTime());
            default -> "Invalid user Acceptance Message";
        };

        log.debug(message);
        log.debug("Sending user acceptance message to DOCG for target: {}, action: {}", controllerId, action.getId());
        sendVehicleStatusMessageToDocg(ddiStatus, new String[]{message, userAcceptanceMessage.toJsonFormattedMessage()}, controllerId, action);
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiFeedbackResponse> postBaseDeploymentActionFeedbackList(
            @Valid @RequestBody final DdiActionFeedbacks feedback,
            @TraceableField @PathVariable("controllerId") final String controllerId,
            @TraceableField @PathVariable("actionId") @NotNull final Long actionId) {
        log.debug("Received request to retrieve post base deployment action feedback list");
        DdiFeedbackResponse ddiFeedbackResponse = createDeploymentFeedbackResponse();
        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isWaitingConfirmation()) {
            return ResponseEntity.notFound().build();
        }

        if (!action.isActive()) {
            log.warn("Updating action {} with feedback {} not possible since action not active anymore.",
                    action.getId(), feedback.getStatuses());
            return new ResponseEntity<>(HttpStatus.GONE);
        }

        if (feedback.getStatuses() != null && !feedback.getStatuses().isEmpty()) {
            validateFeedbackStatuses(feedback.getStatuses());

            feedback.getStatuses().sort(Comparator.comparing(DdiStatus::getTimestamp));
            for (DdiStatus ddiStatus : feedback.getStatuses()) {

                processActionStatusAndNotifyDocg(action, ddiStatus, target, ddiFeedbackResponse);
            }
        }

        return new ResponseEntity<>(ddiFeedbackResponse, HttpStatus.OK);

    }

    private ActionStatusCreate generateUpdateStatus(final DdiStatus ddiStatus, final String controllerId,
                                                    final Long actionId) {

        final ActionStatusCreate actionStatusCreate = entityFactory.actionStatus().create(actionId);

        final List<String> messages = new ArrayList<>();

        if (!CollectionUtils.isEmpty(ddiStatus.getDetails())) {
            messages.addAll(ddiStatus.getDetails());
        }

        final Integer code = ddiStatus.getCode();
        if (code != null) {
            actionStatusCreate.code(code);
            messages.add("Device reported status code: " + code);
        }

        if (ddiStatus.getUserAcceptanceMessageJob1() != null) {
            log.debug("UserAcceptanceMessageJob1 has been provided, adding it to the action status.");
            actionStatusCreate.userAcceptanceMessageJob1(ddiStatus.getUserAcceptanceMessageJob1());
            log.debug("Added user acceptance message job1: {}", ddiStatus.getUserAcceptanceMessageJob1());
        }

        final DeviceActionStatus status;
        switch (ddiStatus.getExecution()) {

            case DD_ACCEPTED:
                log.debug("Controller confirmed action (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.DD_ACCEPTED;
                addMessageIfEmpty("Target accepted the update.", messages);
                break;

            case DOWNLOAD_STARTED:
                log.debug("Controller confirmed download started (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.DOWNLOAD_STARTED;
                addMessageIfEmpty("Target confirmed download started.", messages);
                break;
            case DOWNLOAD_IN_PROGRESS:
                log.debug("Controller confirmed download in progress (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.DOWNLOAD_IN_PROGRESS;
                addMessageIfEmpty("Download is in progress.", messages);
                break;
            case DOWNLOAD_COMPLETED:
                log.debug("Controller confirmed download complete (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.DOWNLOAD_COMPLETED;
                addMessageIfEmpty("Download was successful.", messages);
                break;
            case LOG_UPLOAD_IN_PROGRESS:
                log.debug("Controller confirmed log upload in progress (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.LOG_UPLOAD_IN_PROGRESS;
                addMessageIfEmpty("log upload is in progress.", messages);
                break;

            case LOG_UPLOAD_SUCCESS:
                log.debug("Controller confirmed log upload success (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.LOG_UPLOAD_SUCCESS;
                addMessageIfEmpty("log upload was successful.", messages);
                break;

            case LOG_UPLOAD_FAILURE:
                log.debug("Controller confirmed log upload failure (actionId: {}, controllerId: {}) as we got {} report.",
                        actionId, controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.LOG_UPLOAD_FAILURE;
                addMessageIfEmpty("log upload failed.", messages);
                break;

            case CANCELED:
                log.debug("Controller confirmed cancel (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.CANCELED;
                addMessageIfEmpty("Target confirmed cancellation.", messages);
                break;
            case FINISHED_SUCCESS:
                log.debug("Controller confirmed successful installation of all downloaded packages (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.FINISHED_SUCCESS;
                addMessageIfEmpty("The update was successfully installed", messages);
                break;
            case FINISHED_FAILURE:
                log.debug("Controller confirmed failed installation of all downloaded packages (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.FINISHED_FAILURE;
                addMessageIfEmpty("The update failed to install", messages);
                break;
            case PENDING_LOGS:
                log.debug("Logs requested for  (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.PENDING_LOGS;
                addMessageIfEmpty("The update failed to install", messages);
                break;
            case USER_ACCEPTED:
                log.debug("User accepted the update (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.USER_ACCEPTED;
                addMessageIfEmpty("The update was accepted to install", messages);
                break;
            case USER_IGNORED:
                log.debug("User ignored the update (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.USER_IGNORED;
                addMessageIfEmpty("The update was ignored to install", messages);
                break;
            case USER_SCHEDULED:
                log.debug("User has scheduled the update (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.USER_SCHEDULED;
                addMessageIfEmpty("The update was scheduled to install", messages);
                break;
            case CANCELED_ACCEPT:
                log.debug("Controller confirmed  the cancelation request (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.CANCELED;
                addMessageIfEmpty("Device accepted the cancelation", messages);
                break;

            case CANCELED_REJECT:
                log.debug("Controller rejected the cancelation request (actionId: {}, controllerId: {}) as we got {} report.", actionId,
                        controllerId, ddiStatus.getExecution());
                status = DeviceActionStatus.DD_SENT;
                addMessageIfEmpty("Device rejected Cancelation", messages);
                break;

            default:
                status = handleDefaultCase(ddiStatus, controllerId, actionId, messages);
                break;
        }

        return actionStatusCreate.status(status).messages(messages).errorCode(getErrorCode(ddiStatus));
    }

    private DeviceActionStatus handleDefaultCase(final DdiStatus ddiStatus, final String controllerId, final Long actionId,
                                                 final List<String> messages) {
        final DeviceActionStatus status;
        log.debug("Controller reported intermediate status (actionId: {}, controllerId: {}) as we got {} report.",
                actionId, controllerId, ddiStatus.getExecution());
        status = DeviceActionStatus.RUNNING;
        addMessageIfEmpty("Target reported " + ddiStatus.getExecution(), messages);
        return status;
    }

    private String getErrorCode(DdiStatus status) {
        return status != null
                && status.getErrorCode() != null
                ? String.join(",", status.getErrorCode()) : null;
    }

    /**
     * To check whether inventoryHash present for the target, if yes check active action
     * And response back with configData/Distribution link based on inventoryHash exist
     *
     * @param controllerId of the target that matches to controller id
     * @param hash         polling hash of the target
     * @return response that contains configData/Distribution links
     */
    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiControllerBase> getInventoryHash(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                              @RequestParam(value = DdiRestConstants.HASH, required = true) final String hash,
                                                              @RequestHeader(required = false) HttpHeaders headers) {

        log.debug("Received request to retrieve inventory hash");
        if (headers != null) {
            String userAgent = headers.getFirst("User-Agent");
            if (userAgent != null) {
                log.info("Poll for update from client User-Agent: {}", userAgent);
            }
            log.debug("Headers received: {}", headers.toSingleValueMap());
        }
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                controllerId, controllerId, DdiRestConstants.DEFAULT_VEHICLE_MODEL_ID);
        final Action activeAction = controllerManagement.findActiveActionWithHighestWeight(controllerId).orElse(null);

        boolean hashCodeExists = hashCodeExist(hash, target);
        long pollingId = 0;
        final Action installedAction = controllerManagement.getInstalledActionByTarget(controllerId).orElse(null);

        checkAndCancelExpiredAction(activeAction);
        return new ResponseEntity<>(DataConversionHelper.fromTarget(hashCodeExists,
                target, activeAction, installedAction,
                activeAction == null ? controllerManagement.getPollingTime()
                        : controllerManagement.getPollingTimeForAction(activeAction.getId()),
                pollingId), HttpStatus.OK);
    }

    /**
     * Add the feedback given as input
     *
     * @param feedbackRequestBody feedback for the specific controller id
     * @param controllerId        of the target that matches to controller id
     * @return response that success or failing
     * deprecated since we are removing sp_polling and its references
     */
    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> feedback(@TraceableField final String controllerId, final DdiFeedbackRequestBody feedbackRequestBody) {
        log.debug("Received request to update feedback");
        final Target target = findTarget(controllerId);
        controllerManagement.addFeedbackByControllerId(target, feedbackRequestBody);
        return ResponseEntity.ok().build();

    }

    private boolean hashCodeExist(String inventoryHash, Target target) {
        String attributeValue = controllerManagement.getTargetAttributeByKey(target.getId(), INVENTORY_HASH_KEY);
        return attributeValue != null && attributeValue.equals(inventoryHash);
    }

    // Deprecated since we are removing sp_polling and its references
    @Deprecated
    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<String> putPollingData(@Valid @RequestBody final DdiConfigData configData,
                                                 @TraceableField @PathVariable("controllerId") final String controllerId,
                                                 @PathVariable("pollingId") final String pollingId) {
        try {
            log.debug("Received request to update polling data");
            updateControllerAttributesWithSoftware(configData, controllerId);
            controllerManagement.setPollingById(Long.parseLong(pollingId), Polling.Status.SUCCESS);
        } catch (Exception e) {
            log.error("Failed to update polling data", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    private void updateControllerAttributesWithSoftware(DdiConfigData configData, String controllerId) throws IllegalAccessException {
        Map<String, String> flattenedMap;
        List<SoftwareOfTarget> listSoftware = new ArrayList<>();

        for (Map.Entry<String, DdiConfigDataDevice> ecuRecord : configData.getData().entrySet()) {
            String ecuID = ecuRecord.getKey();
            DdiConfigDataDevice ecuValue = ecuRecord.getValue();
            if (ecuValue.getSw() != null) {
                ecuValue.getSw().forEach(sw -> listSoftware.add(entityFactory.generateTargetSoftware(sw.getSwComponentID(), ecuID, sw.getSwVersion())));
            }
            ecuValue.setSw(null);
        }
        flattenedMap = DataConversionHelper.flattenObject(configData.getData(), null);
        controllerManagement.updateControllerAttributesWithSoftware(controllerId, flattenedMap, listSoftware, getUpdateMode(configData));
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiCancel> getControllerCancelAction(@TraceableField @PathVariable("controllerId") @NotEmpty final String controllerId,
                                                               @TraceableField @PathVariable("actionId") @NotNull final Long actionId) {
        log.debug("Received request to retrieve controller cancel action");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isCancelingOrCanceled()) {
            final DdiCancel cancel = new DdiCancel(String.valueOf(action.getId()),
                    new DdiCancelActionToStop(String.valueOf(action.getId())));

            log.debug("Found an active CancelAction for target {}. returning cancel: {}", controllerId, cancel);

            controllerManagement.registerRetrieved(action.getId(), RepositoryConstants.SERVER_MESSAGE_PREFIX
                    + "Target retrieved cancel action and should start now the cancellation.");

            return new ResponseEntity<>(cancel, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> postCancelActionFeedback(@Valid @RequestBody final DdiActionFeedback feedback,
                                                         @TraceableField @PathVariable("controllerId") @NotEmpty final String controllerId,
                                                         @TraceableField @PathVariable("actionId") @NotNull final Long actionId) {
        log.debug("Received request to update cancel action feedback");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        controllerManagement
                .addCancelActionStatus(generateActionCancelStatus(feedback, target, action.getId(), entityFactory));
        return ResponseEntity.ok().build();
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiDeploymentBase> getControllerInstalledAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                                          @TraceableField @PathVariable("actionId") final Long actionId,
                                                                          @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount) {
        log.debug("Received request to retrieve controller installed action");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        if (action.isActive() || action.isCancelingOrCanceled()) {
            return ResponseEntity.notFound().build();
        }

        final DdiDeploymentBase base = generateDdiDeploymentBase(target, action, actionHistoryMessageCount);

        log.debug("Found an installed UpdateAction for target {}. returning deployment: {}", controllerId, base);
        return new ResponseEntity<>(base, HttpStatus.OK);

    }

    private Target findTarget(final String controllerId) {
        return controllerManagement.getByControllerId(controllerId)
                .orElseThrow(() -> new EntityNotFoundException(Target.class, controllerId));
    }

    private Action findActionForTarget(final Long actionId, final Target target) {
        final Action action = controllerManagement.findActionWithDetails(actionId)
                .orElseThrow(() -> new EntityNotFoundException(Action.class, actionId));
        return verifyActionBelongsToTarget(action, target);
    }

    private Action verifyActionBelongsToTarget(final Action action, final Target target) {
        if (!action.getTarget().getId().equals(target.getId())) {
            log.debug(GIVEN_ACTION_IS_NOT_ASSIGNED_TO_GIVEN_TARGET, action.getId(), target.getId());
            throw new EntityNotFoundException(
                    "Not a valid action (" + action.getId() + ") for target: " + target.getControllerId(), null);
        }
        return action;
    }

    /**
     * If the action has a maintenance schedule defined but is no longer valid,
     * cancel the action.
     *
     * @param action is the {@link Action} to check.
     */
    private void checkAndCancelExpiredAction(final Action action) {
        if (action != null && action.hasMaintenanceSchedule() && action.isMaintenanceScheduleLapsed()) {
            log.debug("Action {} has a maintenance schedule and it is lapsed. Attempting to cancel.", action.getId());
            try {
                controllerManagement.cancelAction(action.getId());
                log.debug("Successfully canceled action {}", action.getId());
            } catch (final CancelActionNotAllowedException e) {
                log.info("Cancel action not allowed for action {}: {}", action.getId(), e.getMessage());
            }
        } else {
            log.debug("Action {} does not have a lapsed maintenance schedule or is null.", action != null ? action.getId() : "null");
        }
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiConfirmationBaseAction> getConfirmationBaseAction(@TraceableField @PathVariable("controllerId") final String controllerId,
                                                                               @TraceableField @PathVariable("actionId") final Long actionId,
                                                                               @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
                                                                               @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount) {
        log.debug("Received request to retrieve confirmation base action");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        checkAndCancelExpiredAction(action);

        if (!action.isCancelingOrCanceled() && action.isWaitingConfirmation()
                && (action.getRollout() == null
                || !RolloutStatus.PAUSED.equals(action.getRollout().getStatus()))) {

            final DdiConfirmationBaseAction base = generateDdiConfirmationBase(target, action,
                    actionHistoryMessageCount);

            log.debug("Found an active UpdateAction for target {}. Returning confirmation: {}", controllerId, base);

            return new ResponseEntity<>(base, HttpStatus.OK);
        }

        return ResponseEntity.notFound().build();
    }

    private DdiDeploymentBase generateDdiDeploymentBase(final Target target, final Action action,
                                                        final Integer actionHistoryMessageCount) {
        final DdiActionHistory actionHistory = generateDdiActionHistory(action, actionHistoryMessageCount).orElse(null);
        final DdiDeployment ddiDeployment = generateDdiDeployment(target, action);
        return new DdiDeploymentBase(Long.toString(action.getId()), ddiDeployment, actionHistory);
    }

    private DdiConfirmationBaseAction generateDdiConfirmationBase(final Target target, final Action action,
                                                                  final Integer actionHistoryMessageCount) {
        final DdiActionHistory actionHistory = generateDdiActionHistory(action, actionHistoryMessageCount).orElse(null);
        final DdiDeployment ddiDeployment = generateDdiDeployment(target, action);
        return new DdiConfirmationBaseAction(Long.toString(action.getId()), ddiDeployment, actionHistory);
    }

    /**
     * @param target
     * @param action
     * @return DdiDeployment: deployment object
     * @author T7437JK
     * @Modified on 03/08/2023
     * updated DdiDeployment with datasetMetadata field i.e. DdiDsMetadata object
     */
    private DdiDeployment generateDdiDeployment(final Target target, final Action action) {
        final List<DdiChunk> chunks = DataConversionHelper.createChunks(target, action, artifactUrlHandler,
                systemManagement, new ServletServerHttpRequest(requestResponseContextHolder.getHttpServletRequest()),
                controllerManagement, versionManagement);
        final DdiDsMetadata datasetMetadata = DataConversionHelper.createDatasetMetadata(action.getDistributionSet().getId(),
                distributionSetManagement);
        final DdiDeployment.HandlingType downloadType = calculateDownloadType(action);
        final DdiDeployment.HandlingType updateType = calculateUpdateType(action, downloadType);
        final DdiDeployment.ConnectivityType connectivityType = action.getRollout() != null
                ? getConnectivityType(action.getRollout())
                : DdiDeployment.ConnectivityType.BOTH;
        final DdiDeployment.DdiMaintenanceWindowStatus maintenanceWindow = calculateMaintenanceWindow(action);
        return new DdiDeployment(downloadType, updateType, chunks, maintenanceWindow, datasetMetadata, connectivityType);
    }

    private Optional<DdiActionHistory> generateDdiActionHistory(final Action action,
                                                                final Integer actionHistoryMessageCount) {
        final List<String> actionHistoryMsgs = controllerManagement.getActionHistoryMessages(action.getId(),
                actionHistoryMessageCount == null ? Integer.parseInt(DdiRestConstants.NO_ACTION_HISTORY)
                        : actionHistoryMessageCount);
        return actionHistoryMsgs.isEmpty() ? Optional.empty()
                : Optional.of(new DdiActionHistory(action.getStatus().name(), actionHistoryMsgs));
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> postConfirmationActionFeedback(@Valid @RequestBody final DdiConfirmationFeedback feedback,
                                                               @TraceableField @PathVariable("controllerId") final String controllerId,
                                                               @TraceableField @PathVariable("actionId") @NotNull final Long actionId) {
        log.debug("Received request to update confirmation action feedback");

        final Target target = findTarget(controllerId);
        final Action action = findActionForTarget(actionId, target);

        try {

            switch (feedback.getConfirmation()) {
                case CONFIRMED:
                    log.info("Controller confirmed the action (actionId: {}, controllerId: {}) as we got {} report.",
                            actionId, controllerId, feedback.getConfirmation());
                    confirmationManagement.confirmAction(actionId, feedback.getCode(), feedback.getDetails());
                    break;
                case DENIED:
                default:
                    log.debug("Controller denied the action (actionId: {}, controllerId: {}) as we got {} report.",
                            actionId, controllerId, feedback.getConfirmation());
                    confirmationManagement.denyAction(actionId, feedback.getCode(), feedback.getDetails());
                    break;
            }
        } catch (final InvalidConfirmationFeedbackException e) {
            if (e.getReason() == InvalidConfirmationFeedbackException.Reason.ACTION_CLOSED) {
                log.warn("Updating action {} with confirmation {} not possible since action not active anymore.",
                        action.getId(), feedback.getConfirmation());
                return new ResponseEntity<>(HttpStatus.GONE);
            } else if (e.getReason() == InvalidConfirmationFeedbackException.Reason.NOT_AWAITING_CONFIRMATION) {
                log.debug("Action is not waiting for confirmation, deny request.");
                return ResponseEntity.notFound().build();
            }
        }

        return ResponseEntity.ok().build();
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<DdiConfirmationBase> getConfirmationBase(@TraceableField final String controllerId) {
        log.debug("Received request to retrieve confirmation base");
        final Target target = controllerManagement.findOrRegisterTargetIfItDoesNotExist(controllerId,
                controllerId, controllerId, DdiRestConstants.DEFAULT_VEHICLE_MODEL_ID);
        final Action activeAction = controllerManagement.findActiveActionWithHighestWeight(controllerId).orElse(null);

        final DdiAutoConfirmationState autoConfirmationState = getAutoConfirmationState(controllerId);

        final DdiConfirmationBase confirmationBase = DataConversionHelper.createConfirmationBase(target, activeAction,
                autoConfirmationState);
        return new ResponseEntity<>(confirmationBase, HttpStatus.OK);
    }

    private DdiAutoConfirmationState getAutoConfirmationState(final String controllerId) {
        return confirmationManagement.getStatus(controllerId).map(status -> {
            final DdiAutoConfirmationState state = DdiAutoConfirmationState.active(status.getActivatedAt());
            state.setInitiator(status.getInitiator());
            state.setRemark(status.getRemark());
            log.trace("Returning state auto-conf state active [initiator='{}' | activatedAt={}] for device {}",
                    controllerId, status.getInitiator(), status.getActivatedAt());
            return state;
        }).orElseGet(() -> {
            log.trace("Returning state auto-conf state disabled for device {}", controllerId);
            return DdiAutoConfirmationState.disabled();
        });
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> activateAutoConfirmation(@TraceableField final String controllerId,
                                                         final DdiActivateAutoConfirmation body) {
        log.debug("Received request to activate auto confirmation");
        final String initiator = body == null ? null : body.getInitiator();
        final String remark = body == null ? FALLBACK_REMARK : body.getRemark();
        log.debug("Activate auto-confirmation request for device '{}' with payload: [initiator='{}' | remark='{}'",
                controllerId, initiator, remark);
        confirmationManagement.activateAutoConfirmation(controllerId, initiator, remark);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> deactivateAutoConfirmation(@TraceableField final String controllerId) {
        log.debug("Received request to deactivate auto confirmation");
        confirmationManagement.deactivateAutoConfirmation(controllerId);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Handles a request to update a device's inventory data for the specified controller.
     * <p>
     * The method performs the following:
     * <ul>
     *   <li>Validates mandatory inventory signature fields.</li>
     *   <li>Checks whether inventory signature validation is enabled for the tenant via
     *       {@link TenantConfigHelper}.</li>
     *   <li>If enabled, validates the provided inventory signature using the configured
     *       signature validation logic.</li>
     *   <li>Delegates to {@link #updateDeviceInventoryAndKafka(DdiDeviceInventory, String)}
     *       to persist the inventory details and publish an event to Kafka.</li>
     * </ul>
     *
     * @param inventory    the incoming {@link DdiDeviceInventory} request payload containing
     *                     signature and encoded inventory details
     * @param controllerId the unique identifier of the controller/target device
     * @return HTTP {@code 200 OK} if the inventory was successfully validated and updated
     * @throws SignatureException      if signature validation fails when enabled
     * @throws JsonProcessingException if decoding the inventory JSON fails
     * @throws IllegalAccessException  if inventory details cannot be accessed due to reflection issues
     */
    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> putDeviceInventory(
            @Valid @RequestBody final DdiDeviceInventory inventory,
            @TraceableField @PathVariable("controllerId") final String controllerId) throws SignatureException, JsonProcessingException, IllegalAccessException {

        log.debug("Received request to update device inventory");
        validateDeviceInventorySignatureDetails(inventory);

        //Get the inventory signature validation based on the configuration flag
        TenantConfigHelper tenantConfigHelper = TenantConfigHelper
                .usingContext(systemSecurityContext, tenantConfigurationManagement);

        validateInventorySignature(signatureConfig, inventory, tenantConfigHelper);
        updateDeviceInventoryAndKafka(inventory, controllerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the device inventory for a given controller and publishes an event
     * with the updated inventory details to Kafka.
     * <p>
     * The method:
     * <ul>
     *   <li>Decodes the {@link DdiDeviceInventory} into a {@link DeviceInventoryDetails} object.</li>
     *   <li>Validates and updates the target's attributes.</li>
     *   <li>Updates the target's inventory record in the repository.</li>
     *   <li>Builds an {@link InventoryMessage} representing the updated inventory.</li>
     *   <li>Extracts VIN and OTA Master Serial Number from the VIN string.</li>
     *   <li>Builds a {@link KafkaEventHeader} with tenant and VIN information.</li>
     *   <li>Wraps the header and payload into a {@link KafkaEventTemplate}.</li>
     *   <li>Publishes the event to Kafka with message type {@link Constants#INVENTORY}.</li>
     * </ul>
     *
     * <p><b>Transactional:</b> Inventory update and event publishing occur in a single transaction
     * to ensure consistency between database state and Kafka event publishing.</p>
     *
     * @param inventory    the incoming {@link DdiDeviceInventory} payload
     * @param controllerId the unique identifier of the controller/target device
     * @throws JsonProcessingException if decoding of the inventory JSON fails
     */
    @Transactional
    public void updateDeviceInventoryAndKafka(DdiDeviceInventory inventory, String controllerId) throws JsonProcessingException {
        DeviceInventoryDetails inventoryDetails = decodeAndCreateDeviceInventory(inventory);
        controllerManagement.updateTargetAttributes(controllerId, validateDeviceInventoryAndCreateTargetAttributes(inventory, inventoryDetails, controllerId));
        controllerManagement.updateTargetInventory(controllerId, inventoryDetails, inventory.getRawInventoryDetails());

        //Prepare an inventory message and send it to Kafka topic
        var inventoryMessage = InventoryMessage.builder()
                .inventoryDetails(inventory.getInventoryDetails())
                .vin(controllerId)
                .inventorySignature(
                        InventorySignature.builder()
                                .signatureType(inventory.getInventorySignature().getSignatureType())
                                .signature(inventory.getInventorySignature().getSignature()).build()).build();

        String[] vinParts = inventoryDetails.getVin().split("_", 2);
        String vin = vinParts.length > 0 ? vinParts[0] : "";
        String otaMasterSerialNumber = vinParts.length > 1 ? vinParts[1] : "";

        // Build KafkaEventHeader from inventoryDetails
        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .vin(vin)
                .otaMasterSerialNumber(otaMasterSerialNumber)
                .build();

        // Wrap in KafkaEventTemplate
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(inventoryMessage)
                .build();

        // Pass messageType for InventoryMessage
        kafkaMessageService.sendKafkaEventWithType(eventTemplate, Constants.INVENTORY);

    }

    /**
     * Publishes deployment logs for a specific controller and action.
     * <p>
     * This method receives deployment log data from the device, processes it, and stores or forwards the logs as required.
     * It is typically called by the device after a deployment operation to report log information.
     * <p>
     * The method expects a valid deployment log payload in the request body, along with the controller and action identifiers as path variables.
     * <p>
     * Typical use cases include:
     * <ul>
     *   <li>Storing deployment logs for audit and troubleshooting purposes.</li>
     *   <li>Forwarding logs to external systems or services for further analysis.</li>
     * </ul>
     *
     * @param file         the deployment logs payload sent by the device
     * @param controllerId the unique identifier of the controller (vehicle/device) reporting the logs
     * @param actionId     the unique identifier of the deployment action associated with the logs
     * @return a {@link ResponseEntity} with HTTP status indicating the result of the operation (usually 200 OK or error status)
     */
    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<Void> publishDeploymentLogs(
            @TraceableField @PathVariable("controllerId") @NotEmpty final String controllerId,
            @TraceableField @PathVariable("actionId") @NotNull final Long actionId,
            @RequestPart(value = "file") @NotNull final MultipartFile file,
            @RequestParam(value = "filename") @NotEmpty String fileName,
            @RequestParam(value = "sequence", required = false) final Integer sequence,
            @RequestParam(value = "isLastFile") @NotNull final Boolean isLastFile) {

        log.debug("Received request to publish deployment logs for controllerId: {}, actionId: {}", controllerId, actionId);

        if (file.isEmpty()) {
            log.error("Uploaded file is empty for controllerId: {}, actionId: {}", controllerId, actionId);
            throw new EntityCannotNullException("Uploaded file is empty.");
        }

        if (fileName == null) {
            log.warn("Filename is null, using original filename for controllerId: {}, actionId: {}", controllerId, actionId);
            fileName = file.getOriginalFilename();
        }

        final Target target = findTarget(controllerId);
        log.debug("Found target for controllerId: {}", controllerId);

        final Action action = findActionForTarget(actionId, target);
        log.debug("Found action for actionId: {} and target: {}", actionId, target);

        log.debug("Preparing DeploymentLogUpload for file: {}, originalName: {}, contentType: {}, fileSize: {}, sequence: {}, isLastFile: {}",
                fileName, file.getOriginalFilename(), file.getContentType(), file.getSize(), sequence, isLastFile);

        DeploymentLogUpload deploymentLogUpload = DeploymentLogUpload.builder()
                .filename(SupportPackageManagementUtil.sanitizeFileName(fileName))
                .fileOriginalName(SupportPackageManagementUtil.sanitizeFileName(file.getOriginalFilename()))
                .action(action)
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .sequence(sequence)
                .isLastFile(isLastFile)
                .build();

        try {
            deploymentLogManagement.create(deploymentLogUpload, file);
            log.info("Deployment log published successfully for controllerId: {}, actionId: {}", controllerId, actionId);
            return ResponseEntity.ok().build();
        } catch (S3Exception e) {
            throw new GenericSpServerException(e.getMessage(), e);
        }

    }

    @Override
    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<?> getTargetBasedeploymentAction(
            @TraceableField @PathVariable("controllerId") final String controllerId,
            @TraceableField @PathVariable("actionId") final Long actionId,
            @RequestParam(value = "c", required = false, defaultValue = "-1") final int resource,
            @RequestParam(value = "actionHistory", defaultValue = DdiRestConstants.NO_ACTION_HISTORY) final Integer actionHistoryMessageCount) {

        log.debug("Received request to retrieve target based deployment action for controllerId: {}, actionId: {}, resource: {}, actionHistoryMessageCount: {}",
                controllerId, actionId, resource, actionHistoryMessageCount);

        final Target target = findTarget(controllerId);
        log.debug("Found target for controllerId: {}", controllerId);

        final Action action = findActionForTarget(actionId, target);
        log.debug("Found action for actionId: {} and target: {}", actionId, target);

        checkAndCancelExpiredAction(action);
        log.debug("Checked and canceled expired action if necessary for actionId: {}", actionId);
        if (action.getStatus() == DeviceActionStatus.RUNNING || action.getStatus() == DeviceActionStatus.DD_SENT) {

            if (action.getRollout() != null && action.getRollout().getMaxPackageSize() != null &&
                    hasExceededPackageSizeForRollout(action.getRollout())) {
                handleExceededPackageSize(action.getRollout());
            }

            DdiDeploymentDescriptorBase base = generateDdiDeploymentBaseDescriptor(target, action);

            log.debug("Generated DdiDeploymentDescriptorBase for actionId: {}", actionId);

            if (base.getDeploymentDescription().getEcus().isEmpty()) {
                log.error("DD generation failed for action {}: ECU list is empty.", actionId);
                sendVehicleStatusMessage(action.getRollout().getName(), target.getControllerId(),
                        DeviceActionStatus.FINISHED_FAILURE.toString(), "DD Generation Failed: ECU list is empty.");
            } else {
                log.debug("DD generation succeeded for actionId: {} with ECUs: {}", actionId, base.getDeploymentDescription().getEcus());
            }

            log.debug("Found an active UpdateAction for target {}. Returning deployment: {}", controllerId, base);

            sendVehicleStatusMessage(action.getRollout().getName(), target.getControllerId(),
                    DeviceActionStatus.DD_SENT.toString(), "Download description created successfully.");
            updateActionStatus((JpaAction) action, DeviceActionStatus.DD_SENT);
            log.debug("Updated action status to DD_SENT for actionId: {}", actionId);

            base = validateDdiDeploymentDescriptorBase(base);
            log.debug("Validated DdiDeploymentDescriptorBase for actionId: {}", actionId);
            return new ResponseEntity<>(new EncodedDdiDeploymentDescriptor(encodeDeploymentDescriptorToBase64(base.getDeploymentDescription()), base.getDeploymentSignature()), HttpStatus.OK);
        } else {
            log.debug("No active UpdateAction found for target {}. Returning 400 Bad Request.", controllerId);
            throw new ValidationException("DD cannot be generated at the moment");
        }
    }

    public DdiDeploymentDescriptorBase validateDdiDeploymentDescriptorBase(@Valid DdiDeploymentDescriptorBase base) {
        Set<ConstraintViolation<DdiDeploymentDescriptorBase>> violations = validator.validate(base);
        if (!violations.isEmpty()) {
            base = new DdiDeploymentDescriptorBase();
        }
        return base;
    }

    /**
     * Handles the scenario when the package size for a rollout has been exceeded.
     * <p>
     * This method can be extended to log the event, notify relevant systems,
     * or trigger additional actions such as alerting the user or updating the rollout status.
     * Currently, it logs a warning message indicating the exceeded package size.
     *
     * @param rollout the {@link Rollout} object for which the package size has been exceeded
     */
    private void handleExceededPackageSize(Rollout rollout) {
        log.warn("Cancelling rollout '{}' (ID: {}) due to package size violation.", rollout.getName(), rollout.getId());
        rolloutManagement.cancelRollout(rollout.getId());
        sendRolloutErrorMessage(rollout.getName());
        throw new PackageSizeLimitExceededException("Maximum package size exceeded for rollout: " + rollout.getName());
    }

    /**
     * Checks if the package size for the given rollout has exceeded its maximum allowed size.
     *
     * @param rollout the rollout to check
     * @return true if the package size has exceeded the maximum allowed size, false otherwise
     */
    private boolean hasExceededPackageSizeForRollout(Rollout rollout) {
        long totalPackageSize = calculateTotalPackageSizeForRollout(rollout);
        log.debug("Checking package size for rollout '{}': totalPackageSize={}, maxPackageSize={}", rollout.getName(), totalPackageSize, rollout.getMaxPackageSize());
        boolean exceeded = totalPackageSize > rollout.getMaxPackageSize();
        if (exceeded) {
            log.warn("Package size exceeded for rollout '{}': totalPackageSize={} > maxPackageSize={}", rollout.getName(), totalPackageSize, rollout.getMaxPackageSize());
        }
        return exceeded;
    }

    /**
     * Calculates the total package size for a given rollout.
     * <p>
     * This method iterates through all packages associated with the rollout and sums their sizes in bytes.
     * It is used to determine if the total size of all packages exceeds the maximum allowed package size for the rollout.
     * <p>
     * If the rollout or its packages are null, the method returns 0.
     *
     * @param rollout the {@link Rollout} object containing package information
     * @return the total size of all packages in bytes; returns 0 if no packages are present
     */
    private long calculateTotalPackageSizeForRollout(Rollout rollout) {
        log.debug("Calculating total package size for rollout: {}", rollout.getId());

        long artifactSize = rollout.getDistributionSet().getModules().stream()
                .flatMap(module -> module.getArtifactSoftwareModuleAssociations().stream())
                .mapToLong(assoc -> assoc.getArtifact().getFileSize())
                .sum();
        log.debug("Artifact size for rollout {}: {} bytes", rollout.getId(), artifactSize);

        long espSize = espRepository.findESPSupportPackagesByRolloutId(rollout.getId()).stream()
                .mapToLong(Esp::getFileSize)
                .sum();
        log.debug("ESP support package size for rollout {}: {} bytes", rollout.getId(), espSize);

        long rspSize = rspRepository.findRSPSupportPackagesByRolloutId(rollout.getId()).stream()
                .mapToLong(Rsp::getFileSize)
                .sum();
        log.debug("RSP support package size for rollout {}: {} bytes", rollout.getId(), rspSize);

        long totalSize = artifactSize + espSize + rspSize;

        log.debug("Total package size for rollout {}: {} bytes", rollout.getId(), totalSize);

        return totalSize;
    }

    private void updateActionStatus(JpaAction action, DeviceActionStatus status) {
        action.setStatus(status);
        actionRepository.save(action);
    }

    private void sendVehicleStatusMessage(String rolloutName, String vehicleId, String status, String message) {
        var vehicleStatusMessage = VehicleStatusMessage.builder()
                .rolloutName(rolloutName)
                .vehicleId(vehicleId)
                .status(status)
                .messages(new String[]{message})
                .timestamp(Instant.now().getEpochSecond())
                .build();

        String[] vinParts = vehicleId.split("_", 2);
        String vin = vinParts.length > 0 ? vinParts[0] : "";
        String otaMasterSerialNumber = vinParts.length > 1 ? vinParts[1] : "";
        // Build KafkaEventHeader
        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .vin(vin)
                .rolloutName(rolloutName)
                .otaMasterSerialNumber(otaMasterSerialNumber)
                .build();

        // Wrap in KafkaEventTemplate
        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(vehicleStatusMessage)
                .build();

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, Constants.VEHICLE_STATUS);
    }

    private void sendRolloutErrorMessage(String rolloutName) {
        log.debug("Sending rollout error message to Kafka for Rollout: '{}'.", rolloutName);
        RolloutStatusPayload payload = RolloutStatusPayload.builder()
                .type("ERROR")
                .status(RolloutStatus.CANCELING.toString())
                .errorCode(Collections.emptyList())
                .errorMessages(List.of("Max Package size exceeded, canceling rollout"))
                .timestamp(Instant.now().getEpochSecond())
                .build();

        KafkaEventHeader header = KafkaEventHeader.builder()
                .tenant(systemManagement.getTenantMetadata().getTenant())
                .rolloutName(rolloutName)
                .build();

        KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                .header(header)
                .payload(payload)
                .build();

        kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.ROLLOUT_STATUS);
        log.debug("Rollout error message sent for rollout: '{}'", rolloutName);
    }


    /**
     * Generates a DdiDeploymentDescriptor object for a given target and action.
     * This method fetches the ECU details and creates a DdiDeploymentDescriptor object for the provided target and action.
     *
     * @param target The target for which the DdiDeploymentDescriptor is to be generated.
     * @param action The action associated with the DdiDeploymentDescriptor.
     * @return A {@link DdiDeploymentDescriptor} representing the deployment descriptor for the given target and action.
     */
    private DdiDeploymentDescriptor generateDdiDeploymentDescriptor(final Target target, final Action action, SigningCertificateConfiguration signingCertificateConfiguration, DdiSignatureService ddiSignatureService, Long expirationDate) {
        log.debug("Generating DdiDeploymentDescriptor for target: {}, action: {}", target.getId(), action.getId());

        final List<DdiEcu> ecus = DataConversionHelper.createEcus(target, action, artifactUrlHandler, ecuModelManagement, controllerManagement,
                systemManagement, new ServletServerHttpRequest(requestResponseContextHolder.getHttpServletRequest()),
                supportPackageManagement, vehicleManagement, distributionSetModuleRepository, actionArtifactRepository, signingCertificateConfiguration, ddiSignatureService);

        // Filter out ECUs with empty software lists
        List<DdiEcu> filteredEcus = ecus.stream()
                .filter(ecu -> ecu.getSoftware() != null &&
                        ecu.getSoftware().stream().anyMatch(
                                software -> software.getSwArtifact() != null
                        ))
                .toList();


        if (filteredEcus.isEmpty()) {

            log.error("DD generation skipped: No ECUs found for target {}, action {}. Returning HTTP 400.",
                    target.getId(), action.getId());

            updateActionStatus((JpaAction) action, DeviceActionStatus.FINISHING_FAILURE);
            //TODO:This has to be moved once deviceexecutorframework is implemented
            updateActionStatus((JpaAction) action, DeviceActionStatus.FINISHED_FAILURE);

            throw new ValidationException("DD cannot be generated, no updatable ECUs/software modules found");
        }
        log.debug("Created ECUs for target: {}, action: {}", target.getId(), action.getId());

        final DdiDeploymentDescriptor.HandlingType downloadType = calculateDownloadTypeDescriptor(action);

        final DdiDeploymentDescriptor.HandlingType updateType = calculateUpdateType(action, downloadType);

        final DdiDeploymentDescriptor ddiDeploymentDescriptor = new DdiDeploymentDescriptor(action.getRollout().getName(), action.getRollout().getDescription(), action.getId(), downloadType, updateType, ecus, generateRsp(artifactUrlHandler, systemManagement, supportPackageManagement, action,
                new ServletServerHttpRequest(requestResponseContextHolder.getHttpServletRequest()), signingCertificateConfiguration, ddiSignatureService), generateDeploymentMetadata(action, expirationDate));
        log.info("Generated DdiDeploymentDescriptor for target: {}, action: {}", target.getId(), action.getId());

        return ddiDeploymentDescriptor;
    }

    /**
     * Generates a DdiDeploymentDescriptorBase object for a given target and action.
     * This method fetches the action history and creates a DdiDeploymentDescriptor object for the provided target and action.
     *
     * @param target The target for which the DdiDeploymentDescriptorBase is to be generated.
     * @param action The action associated with the DdiDeploymentDescriptorBase.
     * @return A {@link DdiDeploymentDescriptorBase} representing the deployment descriptor base for the given target and action.
     */
    private DdiDeploymentDescriptorBase generateDdiDeploymentBaseDescriptor(final Target target, final Action action) {
        log.debug("Generating DdiDeploymentDescriptorBase for target: {}, action: {}", target.getId(), action.getId());

        // String certificateKey = target.getSerialNumber() + CERTIFICATE_FILE_EXTENSION;
        //ddiSignatureService.getOrCacheEcuIssuer(serialNoForECUCertificates, ecuCertificatesBucketName, certificateKey);

        //TODO:Hardcoding the issuer name via properties file for now, as we only have this one signing configuration.
        // This will be replaced with getting the issuer name dynamically from the ECU certificates later.
        String issuer = ecuCertificatesIssuerName;
        SigningCertificateConfiguration signingCertificateConfiguration = pkiManagement.getSigningCertificateConfiguration(issuer);
        Long expirationDate = getCertificateExpirationDate(signingCertificateConfiguration);
        final DdiDeploymentDescriptor ddiDeployment = generateDdiDeploymentDescriptor(target, action, signingCertificateConfiguration, ddiSignatureService, expirationDate);
        log.debug("Generated DdiDeploymentDescriptor for target: {}, action: {}", target.getId(), action.getId());

        Rollout rollout = action.getRollout();

        String deploymentSignature = ddiSignatureService.generateSignature(encodeDeploymentDescriptor(ddiDeployment), DdiSignatureType.DD, signingCertificateConfiguration, rollout).getSignature();
        log.debug("Generated deployment signature for target: {}, action: {}", target.getId(), action.getId());

        return new DdiDeploymentDescriptorBase(ddiDeployment, deploymentSignature);
    }

    /**
     * Validates the list of DdiStatus objects to ensure all required fields are present.
     * Throws a ValidationException with an appropriate error message if any validation fails.
     *
     * @param statuses the list of DdiStatus objects to validate
     * @throws ValidationException if any required field is missing or invalid
     */
    private void validateFeedbackStatuses(List<DdiStatus> statuses) {
        statuses.stream().forEach(status -> {
            if (status.getTimestamp() == null || status.getTimestamp() <= 0) {
                throw new ValidationException("Timestamp is required and must be a positive number for each status");
            }
            if (Objects.isNull(status.getCode())) {
                throw new ValidationException("Code cannot be null");
            }
            if (!validateErrorCodeForFailureStatuses(status)) {
                throw new ValidationException("ErrorCode cannot be null for execution statuses FINISHED_FAILURE/LOG_UPLOAD_FAILURE/ERROR_RESPONSE_CODE/CANCELED_ACCEPT/CANCELED_REJECT");
            }
            if (isFinished(status) && (status.getInventoryHash() == null || status.getInventoryHash().isEmpty())) {
                throw new ValidationException("InventoryHash cannot be null for execution statuses FINISHED_SUCCESS and FINISHED_FAILURE and CANCELED_ACCEPT");
            }
            if (status.getExecution() == DdiStatus.ExecutionStatus.USER_ACCEPTED &&
                    status.getUserAcceptanceMessageJob1() == null) {
                throw new ValidationException("UserAcceptanceMessageJob1 is required for execution status USER_ACCEPTED");
            }
            if (validateDownloadPercentage(status.getDownload())) {
                throw new ValidationException("Download percentage must be between 0 and 100");
            }
        });
    }

    private boolean validateErrorCodeForFailureStatuses(DdiStatus status) {
        return (status.getExecution() != DdiStatus.ExecutionStatus.FINISHED_FAILURE &&
                status.getExecution() != DdiStatus.ExecutionStatus.LOG_UPLOAD_FAILURE &&
                status.getExecution() != DdiStatus.ExecutionStatus.ERROR_RESPONSE_CODE &&
                status.getExecution() != DdiStatus.ExecutionStatus.CANCELED_ACCEPT &&
                status.getExecution() != DdiStatus.ExecutionStatus.CANCELED_REJECT) ||
                (status.getErrorCode() != null && !status.getErrorCode().isEmpty() &&
                        !status.getErrorCode().stream().allMatch(code -> code == null || code.trim().isEmpty()));
    }

    @VehicleTenantAware
    @TraceableMethod
    public ResponseEntity<PagedList<InventoryWithAction>> fetchInventoryDetails(@PathVariable("controllerId") String controllerId,
                                                                                @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_OFFSET, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET) final int pagingOffsetParam,
                                                                                @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_PAGING_LIMIT, defaultValue = MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT) final int pagingLimitParam,
                                                                                @RequestParam(value = MgmtRestConstants.REQUEST_PARAMETER_SORTING, required = false) final String sortParam) {
        final int sanitizedOffsetParam = PagingUtility.sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = PagingUtility.sanitizePageLimitParam(pagingLimitParam);
        final Sort sorting = PagingUtility.sanitizeInventoryFieldsSortParam(sortParam);

        final Pageable pageable = new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sorting);
        final Page<InventoryWithAction> inventoryDetails;
        final long totalCount;

        inventoryDetails = targetManagement.findByTargetInventoryInDesc(controllerId, pageable);
        totalCount = targetManagement.countByTargetControllerId(controllerId);


        final PagedList<InventoryWithAction> deviceInventoryDetails = new PagedList<>(inventoryDetails.getContent(), totalCount);
        return ResponseEntity.ok(deviceInventoryDetails);
    }


    /**
     * Validates the minimum and maximum percentage for the download percentage
     *
     * @param download The attribute showing the download information
     * @return true if the value is invalid
     */
    private boolean validateDownloadPercentage(DdiDownload download) {
        if (download != null) {
            Integer percentage = download.getPercentage();
            return percentage == null || percentage < 0 || percentage > 100;
        }
        return false;
    }

    /**
     * Returns the expiration date of the X.509 certificate used for DDI signing.
     *
     * @param signingCertificateConfiguration the configuration containing information
     *                                        to retrieve the DDI signing certificate
     * @return the expiration date of the certificate, in epoch milliseconds
     * @throws ValidationException if the certificate cannot be retrieved or parsed
     */
    public Long getCertificateExpirationDate(SigningCertificateConfiguration signingCertificateConfiguration) {
        DdiSignatureType type = DdiSignatureType.DD;
        X509Certificate certificate = ddiSignatureService.getCertificate(type, signingCertificateConfiguration);
        return certificate.getNotAfter().getTime();
    }


}
