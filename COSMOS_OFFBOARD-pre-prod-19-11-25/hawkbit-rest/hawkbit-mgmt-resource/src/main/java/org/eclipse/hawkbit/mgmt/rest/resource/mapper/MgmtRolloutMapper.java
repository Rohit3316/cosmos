/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ValidationException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutActionLinks;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredStateOfCharge;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutCondition;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutCondition.Condition;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutDeployment;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutErrorAction;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutErrorAction.ErrorAction;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutLogRequest;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutMetaData;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutModuleVersion;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutResponseBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutRestRequestBody;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutSuccessAction;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutSuccessAction.SuccessAction;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutTargetActionsResponse;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutTotalTargetCountActionStatus;
import org.cosmos.models.mgmt.rollout.dto.MgmtRolloutUpdateRequest;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtAddDeviceDetailsResponse;
import org.cosmos.models.mgmt.rolloutgroup.dto.MgmtRolloutGroupResponseBody;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtBaseSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtFileUrlSupportPackageCreateRequest;
import org.cosmos.models.mgmt.supportpackage.dto.MgmtSupportPackage;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRolloutRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtSupportPackageResource;
import org.eclipse.hawkbit.repository.jpa.model.JpaEsp;
import org.eclipse.hawkbit.repository.jpa.model.JpaEspEcuRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRollout;
import org.eclipse.hawkbit.repository.jpa.model.JpaRsp;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.Esp;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupErrorCondition;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessAction;
import org.eclipse.hawkbit.repository.model.RolloutGroup.RolloutGroupSuccessCondition;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.dto.AssociatedTargetsToRolloutGroup;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtRolloutMapper {

    private static final String NOT_SUPPORTED = " is not supported";
    private static final ObjectMapper objectMapper = new ObjectMapper();


    private MgmtRolloutMapper() {
        // Utility class
    }

    /**
     * Converts a list of Rollout entities to a list of MgmtRolloutResponseBody.
     *
     * @param tenantId    the tenant ID
     * @param rollouts    the list of Rollout entities
     * @param withDetails whether to include detailed links
     * @return a list of MgmtRolloutResponseBody
     */
    public static List<MgmtRolloutResponseBody> mapToRolloutResponseList(Long tenantId, final List<Rollout> rollouts, final boolean withDetails) {
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(rollout -> mapToRolloutResponse(tenantId, withDetails, rollout)).toList();
    }

    /**
     * Converts a Rollout entity to a MgmtRolloutResponseBody.
     *
     * @param tenantId    the tenant ID
     * @param rollout     the Rollout entity
     * @param withDetails whether to include detailed links
     * @return the MgmtRolloutResponseBody object
     */
    private static MgmtRolloutResponseBody mapToRolloutResponse(Long tenantId, boolean withDetails, Rollout rollout) {
        MgmtRolloutResponseBody getAllRolloutResponse = buildRolloutResponse(rollout);
        populateRolloutDetails(getAllRolloutResponse, rollout);
        if (withDetails) {
            addRetrievalLinks(getAllRolloutResponse, tenantId, rollout.getId());
        }
        return getAllRolloutResponse;
    }

    /**
     * Converts a Rollout entity to a MgmtRolloutResponseBody for an update operation.
     *
     * @param tenantId the tenant ID
     * @param rollout  the Rollout entity
     * @return the response object
     */
     public static MgmtRolloutResponseBody mapToUpdateRolloutResponse(Long tenantId, final Rollout rollout) {

        MgmtRolloutResponseBody updateRolloutResponse = buildRolloutResponse(rollout);
        populateRolloutDetails(updateRolloutResponse, rollout);
        // Currently, the links are identical to those used for creation.
        addCreationLinks(updateRolloutResponse, tenantId, rollout.getId());
        return updateRolloutResponse;
    }

    /**
     * Converts a Rollout entity to a MgmtRolloutResponseBody for retrieval operation.
     *
     * @param tenantId           the tenant ID
     * @param rollout            the Rollout entity
     * @param modulesAndVersions the list of module versions associated with the rollout
     * @param targetCounts       the total target count action status
     * @return the response object
     */
    public static MgmtRolloutResponseBody mapToGetRolloutResponse(Long tenantId, final Rollout rollout, List<MgmtRolloutModuleVersion> modulesAndVersions,
                                                           MgmtRolloutTotalTargetCountActionStatus targetCounts) {

        MgmtRolloutResponseBody getRolloutResponse = getRolloutResponse(rollout, modulesAndVersions, targetCounts);
        populateRolloutDetails(getRolloutResponse, rollout);
        addRetrievalLinks(getRolloutResponse, tenantId, rollout.getId());
        return getRolloutResponse;
    }

    /**
     * Converts a Rollout entity to a MgmtRolloutResponseBody for a create operation.
     *
     * @param tenantId the tenant ID
     * @param rollout  the Rollout entity
     * @return the response object
     */
    public static MgmtRolloutResponseBody mapToCreateRolloutResponse(Long tenantId, final Rollout rollout) {
        MgmtRolloutResponseBody createRolloutResponse = buildRolloutResponse(rollout);
        populateRolloutDetails(createRolloutResponse, rollout);
        addCreationLinks(createRolloutResponse, tenantId, rollout.getId());
        return createRolloutResponse;
    }

    public static List<MgmtRolloutGroupResponseBody> toResponseRolloutGroup(Long tenantId, final List<RolloutGroup> rollouts,
                                                                     final boolean confirmationFlowEnabled, final boolean withDetails) {
        if (rollouts == null) {
            return Collections.emptyList();
        }

        return rollouts.stream().map(group -> toResponseRolloutGroup(tenantId, group, withDetails, confirmationFlowEnabled))
                .toList();
    }

    public static MgmtRolloutGroupResponseBody toResponseRolloutGroup(Long tenantId, final RolloutGroup rolloutGroup,
                                                               final boolean withDetailedStatus, final boolean confirmationFlowEnabled) {
        final MgmtRolloutGroupResponseBody body = new MgmtRolloutGroupResponseBody();
        body.setCreatedAt(rolloutGroup.getCreatedAt());
        body.setCreatedBy(rolloutGroup.getCreatedBy());
        body.setDescription(rolloutGroup.getDescription());
        body.setLastModifiedAt(rolloutGroup.getLastModifiedAt());
        body.setLastModifiedBy(rolloutGroup.getLastModifiedBy());
        body.setName(rolloutGroup.getName());
        body.setRolloutGroupId(rolloutGroup.getId());
        body.setStatus(rolloutGroup.getStatus().toString().toLowerCase());
        body.setTargetPercentage(rolloutGroup.getTargetPercentage());
        body.setTargetFilterQuery(rolloutGroup.getTargetFilterQuery());
        body.setTotalTargets(rolloutGroup.getTotalTargets());

        if (confirmationFlowEnabled) {
            body.setConfirmationRequired(rolloutGroup.isConfirmationRequired());
        }

        body.setSuccessCondition(new MgmtRolloutCondition(map(rolloutGroup.getSuccessCondition()),
                rolloutGroup.getSuccessConditionExp()));
        body.setSuccessAction(
                new MgmtRolloutSuccessAction(map(rolloutGroup.getSuccessAction()), rolloutGroup.getSuccessActionExp()));

        body.setErrorCondition(
                new MgmtRolloutCondition(map(rolloutGroup.getErrorCondition()), rolloutGroup.getErrorConditionExp()));
        body.setErrorAction(
                new MgmtRolloutErrorAction(map(rolloutGroup.getErrorAction()), rolloutGroup.getErrorActionExp()));

        if (withDetailedStatus) {
            for (final TotalTargetCountStatus.Status status : TotalTargetCountStatus.Status.values()) {
                body.addTotalTargetsPerStatus(status.name().toLowerCase(),
                        rolloutGroup.getTotalTargetCountStatus().getTotalTargetCountByStatus(status));
            }
        }

        body.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroup(tenantId, rolloutGroup.getRollout().getId(),
                rolloutGroup.getId())).withSelfRel());
        return body;
    }

    /**
     * Build the response body for device details api
     *
     * @param assignedTargetsToRolloutGroups list of {@link AssociatedTargetsToRolloutGroup}
     * @param rollout                        the rollout
     * @param unregisteredTargetDevices      list of unregistered targets
     * @param tenantId                       the tenant id
     * @return response object {@link MgmtAddDeviceDetailsResponse}
     */
    public static MgmtAddDeviceDetailsResponse toResponseDeviceDetails(List<AssociatedTargetsToRolloutGroup> assignedTargetsToRolloutGroups,
                                                                Rollout rollout, List<String> unregisteredTargetDevices,
                                                                List<String> duplicateTargetDevices, Long tenantId) {

        List<MgmtAddDeviceDetailsResponse.Group> associatedGroups = assignedTargetsToRolloutGroups.stream()
                .map(assignedTargetsToRolloutGroup -> MgmtAddDeviceDetailsResponse.Group.builder()
                        .id(assignedTargetsToRolloutGroup.getRolloutGroup().getId())
                        .name(assignedTargetsToRolloutGroup.getRolloutGroup().getName())
                        .totalTargets(assignedTargetsToRolloutGroup.getTargets().size())
                        .targetDevices(assignedTargetsToRolloutGroup.getTargets().stream().map(Target::getControllerId).toList())
                        .build()).toList();

        return MgmtAddDeviceDetailsResponse.builder()
                .id(rollout.getId())
                .unregisteredTargetDevices(unregisteredTargetDevices)
                .duplicateTargetDevices(duplicateTargetDevices)
                .tagName(rollout.getName())//tag name is same as rollout name
                .totalGroups(assignedTargetsToRolloutGroups.size())
                .groups(associatedGroups).build();

    }

    // Method to process Esp packages
    private static List<MgmtSupportPackage> processEspPackages(List<Esp> espPackages) {
        Map<Long, MgmtSupportPackage.MgmtSupportPackageBuilder> packageMap = espPackages.stream()
                .collect(Collectors.toMap(
                        Esp::getId,
                        esp -> {
                            List<String> controllerIds = esp.getEspEcuRollouts().stream()
                                    .map(espEcuRollout -> espEcuRollout.getControllerId()) // Access controllerId directly
                                    .distinct()
                                    .collect(Collectors.toList());

                            String ecuNodeAddr = esp.getEspEcuRollouts().stream()
                                    .map(espEcuRollout -> espEcuRollout.getEcuNodeAddress())
                                    .findFirst()
                                    .orElse(null);

                            return MgmtSupportPackage.builder()
                                    .type(esp.getFileType().getCategory())
                                    .supportPackageId(esp.getId())
                                    .filename(esp.getFileName())
                                    .fileType(esp.getFileType().name())
                                    .fileUrl(esp.getFileUrl())
                                    .md5(esp.getMd5Hash())
                                    .sha_256(esp.getSha256Hash())
                                    .file_version(esp.getFileVersion())
                                    .controllerIds(controllerIds)
                                    .ecuNodeAddr(ecuNodeAddr)
                                    .fileContentDescription(esp.getFileContentDescription())
                                    .fileInfoUrl(esp.getFileInfoUrl())
                                    .fileMetadata(esp.getMetadata());
                        },
                        (existing, newEntry) -> {
                            List<String> existingControllerIds = existing.build().getControllerIds();
                            List<String> newControllerIds = newEntry.build().getControllerIds();
                            Set<String> combinedControllerIds = new HashSet<>(existingControllerIds);
                            combinedControllerIds.addAll(newControllerIds);
                            return existing.controllerIds(new ArrayList<>(combinedControllerIds));
                        },
                        LinkedHashMap::new
                ));
        return espPackages.stream()
                .map(esp -> packageMap.get(esp.getId()).build())
                .collect(Collectors.toList());
    }


    // Method to process Rsp packages
    private static List<MgmtSupportPackage> processRspPackages(List<Rsp> rspPackages) {
        return rspPackages.stream()
                .map(rsp -> MgmtSupportPackage.builder()
                        .type(rsp.getFileType().getCategory())
                        .supportPackageId(rsp.getId())
                        .filename(rsp.getFileName())
                        .fileType(rsp.getFileType().name())
                        .fileUrl(rsp.getFileUrl())
                        .md5(rsp.getMd5Hash())
                        .sha_256(rsp.getSha256Hash())
                        .file_version(rsp.getFileVersion())
                        .controllerIds(null)
                        .ecuNodeAddr(null)
                        .fileContentDescription(rsp.getFileContentDescription())
                        .fileInfoUrl(rsp.getFileInfoUrl())
                        .fileMetadata(rsp.getMetadata())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Converts a list of ESP and RSP support packages to a list of {@link MgmtSupportPackage} objects.
     * The ESP and RSP support packages are processed separately and then concatenated into a single list.
     *
     * @param espPackages the list of ESP support packages to be converted
     * @param rspPackages the list of RSP support packages to be converted
     * @return a list of {@link MgmtSupportPackage} objects representing the ESP and RSP support packages
     */
    public static List<MgmtSupportPackage> toResponseRollout(List<Esp> espPackages, List<Rsp> rspPackages) {
        List<MgmtSupportPackage> espResults = processEspPackages(espPackages);
        List<MgmtSupportPackage> rspResults = processRspPackages(rspPackages);
        return Stream.concat(espResults.stream(), rspResults.stream())
                .collect(Collectors.toList());
    }

    /**
     * Converts a list of ESP and RSP support packages to a list of {@link MgmtSupportPackage} objects.
     * The ESP and RSP support packages are processed separately and then concatenated into a single list.
     *
     * @param espSupportPackages the list of ESP support packages to be converted
     * @return a list of {@link MgmtSupportPackage} objects representing the ESP and RSP support packages
     */
    public static MgmtSupportPackage toResponseEspRolloutPkgId(List<Esp> espSupportPackages) {
        List<String> controllerIds = espSupportPackages.stream()
                .flatMap(esp -> esp.getEspEcuRollouts().stream())
                .map(espEcuRollout -> espEcuRollout.getControllerId())
                .distinct()
                .collect(Collectors.toList());

        String ecuNodeAddr = espSupportPackages.stream()
                .flatMap(esp -> esp.getEspEcuRollouts().stream())
                .map(espEcuRollout -> espEcuRollout.getEcuNodeAddress())
                .findFirst()
                .orElse(null);

        Esp firstPackage = espSupportPackages.get(0);
        return MgmtSupportPackage.builder()
                .type(firstPackage.getFileType().getCategory())
                .supportPackageId(firstPackage.getId())
                .filename(firstPackage.getFileName())
                .fileType(firstPackage.getFileType().name())
                .fileUrl(firstPackage.getFileUrl())
                .md5(firstPackage.getMd5Hash())
                .sha_256(firstPackage.getSha256Hash())
                .file_version(firstPackage.getFileVersion())
                .controllerIds(controllerIds)
                .ecuNodeAddr(ecuNodeAddr)
                .fileContentDescription(firstPackage.getFileContentDescription())
                .fileInfoUrl(firstPackage.getFileInfoUrl())
                .fileMetadata(firstPackage.getMetadata())
                .build();
    }

    /**
     * Converts a list of ESP and RSP support packages to a list of {@link MgmtSupportPackage} objects.
     * The ESP and RSP support packages are processed separately and then concatenated into a single list.
     *
     * @param rspSupportPackages the list of RSP support packages to be converted
     * @return a list of {@link MgmtSupportPackage} objects representing the ESP and RSP support packages
     */
    public static MgmtSupportPackage toResponseRspRolloutPkgId(List<Rsp> rspSupportPackages) {
        Rsp firstPackage = rspSupportPackages.get(0);
        return MgmtSupportPackage.builder()
                .type(firstPackage.getFileType().getCategory())
                .supportPackageId(firstPackage.getId())
                .filename(firstPackage.getFileName())
                .fileType(firstPackage.getFileType().name())
                .fileUrl(firstPackage.getFileUrl())
                .md5(firstPackage.getMd5Hash())
                .sha_256(firstPackage.getSha256Hash())
                .file_version(firstPackage.getFileVersion())
                .controllerIds(null)
                .ecuNodeAddr(null)
                .fileContentDescription(firstPackage.getFileContentDescription())
                .fileInfoUrl(firstPackage.getFileInfoUrl())
                .fileMetadata(firstPackage.getMetadata())
                .build();
    }

    private static Condition map(final RolloutGroupSuccessCondition rolloutCondition) {
        if (RolloutGroupSuccessCondition.THRESHOLD == rolloutCondition) {
            return Condition.THRESHOLD;
        }
        throw new IllegalArgumentException("Rollout group condition " + rolloutCondition + NOT_SUPPORTED);
    }

    private static Condition map(final RolloutGroupErrorCondition rolloutCondition) {
        if (RolloutGroupErrorCondition.THRESHOLD == rolloutCondition) {
            return Condition.THRESHOLD;
        }
        throw new IllegalArgumentException("Rollout group condition " + rolloutCondition + NOT_SUPPORTED);
    }

    private static RolloutGroupErrorAction map(final ErrorAction action) {
        if (ErrorAction.PAUSE == action) {
            return RolloutGroupErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Error Action " + action + NOT_SUPPORTED);
    }

    private static RolloutGroupSuccessAction map(final SuccessAction action) {
        if (SuccessAction.NEXTGROUP == action) {
            return RolloutGroupSuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Success Action " + action + NOT_SUPPORTED);
    }

    private static SuccessAction map(final RolloutGroupSuccessAction successAction) {
        if (RolloutGroupSuccessAction.NEXTGROUP == successAction) {
            return SuccessAction.NEXTGROUP;
        }
        throw new IllegalArgumentException("Rollout group success action " + successAction + NOT_SUPPORTED);
    }

    private static ErrorAction map(final RolloutGroupErrorAction errorAction) {
        if (RolloutGroupErrorAction.PAUSE == errorAction) {
            return ErrorAction.PAUSE;
        }
        throw new IllegalArgumentException("Rollout group error action " + errorAction + NOT_SUPPORTED);
    }


    public static Rollout fromRequest(final MgmtRolloutRestRequestBody rolloutRestRequestBody) {
        JpaRollout jpaRollouts = buildJpaRollouts(rolloutRestRequestBody);
        jpaRollouts.setName(rolloutRestRequestBody.getName());
        jpaRollouts.setDescription(rolloutRestRequestBody.getDescription());
        return jpaRollouts;
    }

    /**
     * Converts a MgmtRolloutUpdateRequest to a Rollout entity.
     *
     * @param updateRequest the MgmtRolloutUpdateRequest
     * @return the Rollout entity
     */
    public static Rollout fromRequest(final Rollout existingRollout, final MgmtRolloutUpdateRequest updateRequest) {
        JpaRollout jpaRollout = buildUpdateRollout(existingRollout, updateRequest);
        // set required state of charge only if it is not null
        if (updateRequest.getDeploymentMetadata() != null &&
                !updateRequest.getDeploymentMetadata().getRequiredStateOfCharge().isEmpty()) {
            try {
                jpaRollout.setRequiredStateOfCharge(objectMapper.writeValueAsString(updateRequest.getDeploymentMetadata().getRequiredStateOfCharge()));
            } catch (JsonProcessingException e) {
                throw new ValidationException("Failed to serialize required state of charge");
            }
        }
        jpaRollout.setDescription(updateRequest.getDescription());
        jpaRollout.setType(updateRequest.getType());
        jpaRollout.setUpdateAction(updateRequest.getUpdateAction());
        jpaRollout.setUpdateActionUninstallVersion(updateRequest.getUpdateActionUninstallVersion() == null || updateRequest.getUpdateActionUninstallVersion().isEmpty() ? null : String.join(",", updateRequest.getUpdateActionUninstallVersion()));
        return jpaRollout;
    }

    /**
     * Builds a MgmtCreateRolloutsResponse from a Rollouts entity.
     *
     * @param rollout the Rollouts entity
     * @return the response object
     */
    private static MgmtRolloutResponseBody buildRolloutResponse(final Rollout rollout) {
        MgmtRolloutLogRequest logRequest = buildLogRequest(rollout);
        Map<MgmtRolloutRequiredStateOfCharge, String> requiredStateOfChargeStringMap = Optional.ofNullable(rollout.getRequiredStateOfCharge())
                .map(stateOfCharge -> {
                    try {
                        return objectMapper.readValue(stateOfCharge, new TypeReference<Map<MgmtRolloutRequiredStateOfCharge, String>>() {
                        });
                    } catch (JsonProcessingException e) {
                        throw new ValidationException(e);
                    }
                })
                .orElseGet(HashMap::new);
        MgmtRolloutDeployment deploymentMetadata = MgmtRolloutDeployment.builder()
                .requiredMedia(rollout.getRequiredMedia())
                .downgradeAllowed(rollout.getDowngradeAllowed())
                .requiredStateOfCharge(requiredStateOfChargeStringMap)
                .estimatedUpdateTime(rollout.getDeploymentEstimatedUpdateTime())
                .build();

        MgmtRolloutMetaData rolloutMetaData= rollout.getClonableParentRolloutId()!=null
               ?MgmtRolloutMetaData.builder().cloneableParentRolloutId(rollout.getClonableParentRolloutId())
                .build():null;

        return MgmtRolloutResponseBody.builder()
                .name(rollout.getName())
                .rolloutId(rollout.getId())
                .userAcceptanceRequired(rollout.getUserAcceptanceRequired())
                .status(rollout.getStatus().toString().toLowerCase())
                .startAt(rollout.getStartAt())
                .endAt(rollout.getEndAt())
                .connectivityType(MgmtRestModelMapper.mapConnectivityType(rollout.getConnectivityType(), MgmtRolloutConnectivityType.class))
                .log(logRequest)
                .deploymentMetadata(deploymentMetadata)
                .rolloutMetaData(rolloutMetaData)
                .downloadRetryCount(rollout.getDownloadRetryCount())
                .maxDownloadDurationTimer(rollout.getMaxDownloadDurationTimer())
                .maxDownloadWifiDurationTimer(rollout.getMaxDownloadWifiDurationTimer())
                .maxDownloadCellularDurationTimer(rollout.getMaxDownloadCellularDurationTimer())
                .maxUpdateTime(rollout.getMaxUpdateTime())
                .maxPackageSize(rollout.getMaxPackageSize())
                .priority(rollout.getPriority())
                .startType(rollout.getStartType())
                .totalGroups(rollout.getRolloutGroupsCreated())
                .build();
    }


    /**
     * Builds the response body for a single rollout.
     *
     * @param rollout            The {@link Rollout} entity containing rollout details.
     * @param modulesAndVersions A list of {@link MgmtRolloutModuleVersion} containing module and version details for the rollout.
     * @param targetCounts       The {@link MgmtRolloutTotalTargetCountActionStatus} containing total target counts and their statuses.
     * @return An instance of {@link MgmtRolloutResponseBody} that represents the response for the given rollout.
     */
    private static MgmtRolloutResponseBody getRolloutResponse(final Rollout rollout, List<MgmtRolloutModuleVersion> modulesAndVersions, MgmtRolloutTotalTargetCountActionStatus targetCounts) {

        MgmtRolloutLogRequest logRequest = buildLogRequest(rollout);
        Map<MgmtRolloutRequiredStateOfCharge, String> requiredStateOfChargeStringMap = Optional.ofNullable(rollout.getRequiredStateOfCharge())
                .map(stateOfCharge -> {
                    try {
                        return objectMapper.readValue(stateOfCharge, new TypeReference<Map<MgmtRolloutRequiredStateOfCharge, String>>() {
                        });
                    } catch (JsonProcessingException e) {
                        throw new ValidationException(e);
                    }
                })
                .orElseGet(HashMap::new);

        MgmtRolloutDeployment deploymentMetadata = MgmtRolloutDeployment.builder()
                .requiredMedia(rollout.getRequiredMedia())
                .downgradeAllowed(rollout.getDowngradeAllowed())
                .requiredStateOfCharge(requiredStateOfChargeStringMap)
                .estimatedUpdateTime(rollout.getDeploymentEstimatedUpdateTime())
                .build();

        return buildRolloutResponse(rollout, modulesAndVersions, targetCounts, logRequest, deploymentMetadata);
    }

    /**
     * Builds an instance of MgmtRolloutResponseBody using the provided rollout details,
     * module versions, target counts, log request, and deployment metadata.
     **/
    private static MgmtRolloutResponseBody buildRolloutResponse(Rollout rollout, List<MgmtRolloutModuleVersion> modulesAndVersions, MgmtRolloutTotalTargetCountActionStatus targetCounts, MgmtRolloutLogRequest logRequest,
                                                                MgmtRolloutDeployment deploymentMetadata) {
        Long startAt = rollout.getStartAt();
        return MgmtRolloutResponseBody.builder()
                .name(rollout.getName())
                .rolloutId(rollout.getId())
                .userAcceptanceRequired(rollout.getUserAcceptanceRequired())
                .status(rollout.getStatus().toString().toLowerCase())
                .startAt(startAt)
                .endAt(rollout.getEndAt())
                .connectivityType(MgmtRestModelMapper.mapConnectivityType(rollout.getConnectivityType(), MgmtRolloutConnectivityType.class))
                .log(logRequest)
                .modules(modulesAndVersions)
                .totalTargets(targetCounts.getTotalTargets())
                .totalTargetsPerStatus(targetCounts.getTotalTargetsPerStatus())
                .deploymentMetadata(deploymentMetadata)
                .downloadRetryCount(rollout.getDownloadRetryCount())
                .maxDownloadDurationTimer(rollout.getMaxDownloadDurationTimer())
                .maxDownloadWifiDurationTimer(rollout.getMaxDownloadWifiDurationTimer())
                .maxDownloadCellularDurationTimer(rollout.getMaxDownloadCellularDurationTimer())
                .maxUpdateTime(rollout.getMaxUpdateTime())
                .totalGroups(rollout.getRolloutGroupsCreated())
                .startType(rollout.getStartType())
                .priority(rollout.getPriority())
                .build();
    }


    /**
     * Adds relevant links to the rollout response for the creation process.
     *
     * @param response  the rollout response object
     * @param tenantId  the tenant ID
     * @param rolloutId the rollout ID
     */
    private static void addCreationLinks(MgmtRolloutResponseBody response, Long tenantId, Long rolloutId) {
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).associateSoftwareModules(tenantId, rolloutId, null))
                .withRel(MgmtRolloutActionLinks.SOFTWARES.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroups(tenantId, rolloutId,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null, null))
                .withRel(MgmtRolloutActionLinks.GROUPS.getLink()).expand());
        response.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtSupportPackageResource.class).getAllSupportPackages(tenantId, rolloutId,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null))
                .withRel(MgmtRolloutActionLinks.SUPPORT_PACKAGES.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).freeze(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.FREEZE.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRollout(tenantId, rolloutId)).withSelfRel().expand());
    }

    /**
     * Adds detailed links to the rollout response for retrieval.
     *
     * @param response  the rollout response object
     * @param tenantId  the tenant ID
     * @param rolloutId the rollout ID
     */
    private static void addRetrievalLinks(MgmtRolloutResponseBody response, Long tenantId, Long rolloutId) {
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).start(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.START.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).pause(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.PAUSE.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).resume(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.RESUME.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).triggerNextGroup(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.TRIGGER_NEXT_GROUP.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).associateSoftwareModules(tenantId, rolloutId, null))
                .withRel(MgmtRolloutActionLinks.SOFTWARES.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRolloutGroups(tenantId, rolloutId,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null, null))
                .withRel(MgmtRolloutActionLinks.GROUPS.getLink()).expand());
        response.add(linkTo(methodOn(MgmtSupportPackageResource.class).getAllSupportPackages(tenantId, rolloutId,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null))
                .withRel(MgmtRolloutActionLinks.SUPPORT_PACKAGES.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).delete(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.DELETE.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).cancel(tenantId, rolloutId))
                .withRel(MgmtRolloutActionLinks.CANCEL.getLink()).expand());
        response.add(linkTo(methodOn(MgmtRolloutRestApi.class).getRollout(tenantId, rolloutId)).withSelfRel().expand());
    }

    /**
     * Populates the response object with rollout details.
     *
     * @param response the response object
     * @param rollout  the Rollout entity
     */
    private static void populateRolloutDetails(MgmtRolloutResponseBody response, Rollout rollout) {
        response.setCreatedAt(rollout.getCreatedAt());
        response.setCreatedBy(rollout.getCreatedBy());
        response.setDescription(rollout.getDescription());
        response.setLastModifiedAt(rollout.getLastModifiedAt());
        response.setLastModifiedBy(rollout.getLastModifiedBy());
    }


    /**
     * Builds a JpaRollouts entity from a MgmtCreateRolloutsRequest.
     *
     * @param rolloutRestRequestBody the request object
     * @return the JpaRollouts entity
     */
    private static JpaRollout buildJpaRollouts(final MgmtRolloutRestRequestBody rolloutRestRequestBody) {
        return JpaRollout.builder()
                .priority(rolloutRestRequestBody.getPriority())
                .connectivityType(rolloutRestRequestBody.getConnectivityType())
                .startType(rolloutRestRequestBody.getStartType())
                .userAcceptanceRequired(rolloutRestRequestBody.getUserAcceptanceRequired())
                .startAt(rolloutRestRequestBody.getStartDate())
                .endAt(rolloutRestRequestBody.getEndDate())
                .status(RolloutStatus.DRAFT)
                .downgradeAllowed(getDowngradeAllowed(rolloutRestRequestBody))
                .requiredMedia(getRequiredMedia(rolloutRestRequestBody))
                .requiredStateOfCharge(getRequiredStateOfCharge(rolloutRestRequestBody))
                .logCollectionRequired(getLogCollectionRequired(rolloutRestRequestBody))
                .logMaxSuccessVin(getLogMaxSuccessVin(rolloutRestRequestBody))
                .logMaxFailureVin(getLogMaxFailureVin(rolloutRestRequestBody))
                .logMaxAllFileSize(getLogMaxAllFileSize(rolloutRestRequestBody))
                .logMaxEachFileSize(getLogMaxEachFileSize(rolloutRestRequestBody))
                .logMaxNumberOfFiles(getLogMaxNumberOfFiles(rolloutRestRequestBody))
                .maxDownloadDurationTimer(rolloutRestRequestBody.getMaxDownloadDurationTimer())
                .maxDownloadCellularDurationTimer(rolloutRestRequestBody.getMaxDownloadCellularDurationTimer())
                .maxDownloadWifiDurationTimer(rolloutRestRequestBody.getMaxDownloadWifiDurationTimer())
                .maxUpdateTime(rolloutRestRequestBody.getMaxUpdateTime())
                .deploymentEstimatedUpdateTime(rolloutRestRequestBody.getDeploymentMetadata().getEstimatedUpdateTime())
                .maxPackageSize(rolloutRestRequestBody.getMaxPackageSize())
                .downloadRetryCount(rolloutRestRequestBody.getDownloadRetryCount())
                .requiredMedia(rolloutRestRequestBody.getDeploymentMetadata().getRequiredMedia() != null ? rolloutRestRequestBody.getDeploymentMetadata().getRequiredMedia() : MgmtRolloutRequiredMedia.FROM_CDN)
                .type(rolloutRestRequestBody.getType() != null ? rolloutRestRequestBody.getType() : null)
                .updateAction(rolloutRestRequestBody.getUpdateAction() != null ? rolloutRestRequestBody.getUpdateAction() : null)
                .updateActionUninstallVersion(rolloutRestRequestBody.getUpdateActionUninstallVersion() != null &&
                        !rolloutRestRequestBody.getUpdateActionUninstallVersion().isEmpty() ?
                        String.join(",", rolloutRestRequestBody.getUpdateActionUninstallVersion()) :
                        null)
                .vehicleLogLevel(rolloutRestRequestBody.getVehicleLogLevel())
                .build();
    }

    private static JpaRollout buildUpdateRollout(Rollout existingRollout, MgmtRolloutUpdateRequest updateRequest) {
        return JpaRollout.builder()
                .priority(updateRequest.getPriority())
                .startType(updateRequest.getStartType())
                .maxUpdateTime(updateRequest.getMaxUpdateTime())
                .connectivityType(updateRequest.getConnectivityType())
                .endAt(updateRequest.getEndAt())
                .requiredStateOfCharge(updateRequest.getDeploymentMetadata() == null ? existingRollout.getRequiredStateOfCharge() : (updateRequest.getDeploymentMetadata().getRequiredStateOfCharge().isEmpty() ? null : updateRequest.getDeploymentMetadata().getRequiredStateOfCharge().toString()))
                .downgradeAllowed(updateRequest.getDeploymentMetadata() == null ? existingRollout.getDowngradeAllowed() : updateRequest.getDeploymentMetadata().getDowngradeAllowed())
                .deploymentEstimatedUpdateTime(updateRequest.getDeploymentMetadata() == null ? existingRollout.getDeploymentEstimatedUpdateTime() : updateRequest.getDeploymentMetadata().getEstimatedUpdateTime())
                .downloadRetryCount(updateRequest.getDownloadRetryCount())
                .logCollectionRequired(updateRequest.getLog().getCollectionRequired())
                .logMaxAllFileSize(updateRequest.getLog().getMaxAllFileSize())
                .logMaxFailureVin(updateRequest.getLog().getMaxFailureVin())
                .logMaxEachFileSize(updateRequest.getLog().getMaxEachFileSize())
                .logMaxNumberOfFiles(updateRequest.getLog().getMaxNumberOfFiles())
                .logMaxSuccessVin(updateRequest.getLog().getMaxSuccessVin())
                .maxDownloadCellularDurationTimer(updateRequest.getMaxDownloadCellularDurationTimer())
                .maxDownloadDurationTimer(updateRequest.getMaxDownloadDurationTimer())
                .maxDownloadWifiDurationTimer(updateRequest.getMaxDownloadWifiDurationTimer())
                .weight(updateRequest.getWeight())
                .startAt(updateRequest.getStartAt())
                .requiredMedia(updateRequest.getDeploymentMetadata() == null ? existingRollout.getRequiredMedia() : updateRequest.getDeploymentMetadata().getRequiredMedia())
                .maxPackageSize(updateRequest.getMaxPackageSize() == null ? existingRollout.getMaxPackageSize() : updateRequest.getMaxPackageSize())
                .vehicleLogLevel(updateRequest.getVehicleLogLevel())
                .userAcceptanceRequired(updateRequest.getUserAcceptanceRequired())
                .type(updateRequest.getType() != null ? updateRequest.getType() : null)
                .updateAction(updateRequest.getUpdateAction() != null ? updateRequest.getUpdateAction() : null)
                .updateActionUninstallVersion(updateRequest.getUpdateActionUninstallVersion() != null && !updateRequest.getUpdateActionUninstallVersion().isEmpty() ? updateRequest.getUpdateActionUninstallVersion().toString() : null)
                .build();
    }

    private static Boolean getLogCollectionRequired(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getCollectionRequired() : null;
    }

    private static Integer getLogMaxSuccessVin(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getMaxSuccessVin() : null;
    }

    private static Integer getLogMaxFailureVin(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getMaxFailureVin() : null;
    }

    private static Integer getLogMaxAllFileSize(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getMaxAllFileSize() : null;
    }

    private static Integer getLogMaxEachFileSize(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getMaxEachFileSize() : null;
    }

    private static Integer getLogMaxNumberOfFiles(MgmtRolloutRestRequestBody request) {
        return request.getLog() != null ? request.getLog().getMaxNumberOfFiles() : null;
    }

    private static String getRequiredStateOfCharge(MgmtRolloutRestRequestBody request) {
        try {
            return request.getDeploymentMetadata() != null ? objectMapper.writeValueAsString(request.getDeploymentMetadata().getRequiredStateOfCharge()) : null;
        } catch (JsonProcessingException e) {
            throw new ValidationException(e);
        }
    }

    private static MgmtRolloutRequiredMedia getRequiredMedia(MgmtRolloutRestRequestBody request) {
        return request.getDeploymentMetadata() != null ? request.getDeploymentMetadata().getRequiredMedia() : null;
    }

    private static MgmtRolloutDowngradeAllowed getDowngradeAllowed(MgmtRolloutRestRequestBody request) {
        return request.getDeploymentMetadata() != null ? request.getDeploymentMetadata().getDowngradeAllowed() : null;
    }

    private static MgmtRolloutLogRequest buildLogRequest(Rollout rollout) {
        if (!Boolean.TRUE.equals(rollout.isLogCollectionRequired())) {
            return MgmtRolloutLogRequest.builder()
                    .collectionRequired(false)
                    .level(rollout.getVehicleLogLevel())
                    .build();
        }
        return MgmtRolloutLogRequest.builder()
                .collectionRequired(true)
                .level(rollout.getVehicleLogLevel())
                .maxSuccessVin(rollout.getLogMaxSuccessVin())
                .maxFailureVin(rollout.getLogMaxFailureVin())
                .maxEachFileSize(rollout.getLogMaxEachFileSize())
                .maxAllFileSize(rollout.getLogMaxAllFileSize())
                .maxNumberOfFiles(rollout.getLogMaxNumberOfFiles())
                .build();

    }

    /**
     * Maps a JpaTarget and Action to a MgmtRolloutTargetActionsResponse.
     *
     * @param target the JpaTarget to map
     * @param action the List of Action associated with the target
     * @return a MgmtRolloutTargetActionsResponse containing the mapped data
     */
   public static MgmtRolloutTargetActionsResponse mapToTargetActionResponse(Target target, Action action) {
        // Create a builder for MgmtRolloutTargetActionsResponse
        MgmtRolloutTargetActionsResponse.MgmtRolloutTargetActionsResponseBuilder builder =
                MgmtRolloutTargetActionsResponse.builder();
        builder.targetId(target.getId())
                .vin(target.getVin())
                .serialNumber(target.getSerialNumber())
                .controllerId(target.getControllerId())
                .vehicleModuleId(target.getVehicleModelId());
        // Set the action details if available
        if (action != null) {
            action.getStatus();
            builder.actionId(action.getId())
                    .actionStatus(action.getStatus().name().toUpperCase());
        }
        return builder.build();
    }

    /**
     * Converts an `MgmtBaseSupportPackageCreateRequest` and a `Rollout` object
     * into a `JpaEsp` entity. The method maps request fields and associated
     * rollouts to populate the `JpaEsp` object, including child ECU rollouts if applicable.
     *
     * @param request The request object containing details of the support package to create.
     *                It may include fields such as file type, name, URL, metadata, and version.
     * @param rollout The `Rollout` object representing the rollout associated with this support package.
     * @return A `JpaEsp` object populated with the data from the provided request and rollout.
     */
    public static JpaEsp fromEspRequest(MgmtBaseSupportPackageCreateRequest request, Rollout rollout) {
        JpaEsp esp = new JpaEsp();
        esp.setFileType(request.getFileType());
        esp.setFileName(request.getFileName());
        esp.setFileInfoUrl(request.getFileInfoUrl());
        esp.setFileContentDescription(request.getFileContentDescription());
        esp.setFileVersion(request.getFileVersion());
        esp.setSha256Hash(request.getSha256());
        if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                String jsonMetadata = mapper.writeValueAsString(request.getFileMetadata());
                esp.setFileMetadata(jsonMetadata);
            } catch (JsonProcessingException e) {
                esp.setFileMetadata("{}");
            }
        } else {
            esp.setFileMetadata("{}");
        }

        if (request instanceof MgmtFileUrlSupportPackageCreateRequest urlReq) {
            esp.setFileUrl(urlReq.getFileUrl());
        }

        if (request.getControllerIds() != null && !request.getControllerIds().isEmpty()) {
            List<JpaEspEcuRollout> ecuRollouts = request.getControllerIds().stream()
                    .map(controllerId -> {
                        JpaEspEcuRollout ecuRollout = new JpaEspEcuRollout();
                        ecuRollout.setControllerId(controllerId);
                        String ecuNodeAddress = request.getEcuNodeAddress();
                        if (ecuNodeAddress != null && !ecuNodeAddress.isEmpty()) {
                            ecuRollout.setEcuNodeAddress(ecuNodeAddress);
                        }
                        ecuRollout.setRollout((JpaRollout) rollout);
                        ecuRollout.setSupportPackage(esp);
                        return ecuRollout;
                    })
                    .toList();
            esp.setEspEcuRollouts(ecuRollouts);
        }
        return esp;
    }

    /**
     * Converts a {@link MgmtBaseSupportPackageCreateRequest} object into a {@link JpaRsp} object.
     *
     * @param request the support package creation request containing file information
     * @return a {@link JpaRsp} object populated with data from the given request
     */

    public static JpaRsp fromRspRequest(MgmtBaseSupportPackageCreateRequest request) {
        JpaRsp rsp = new JpaRsp();
        rsp.setFileType(request.getFileType());
        rsp.setFileName(request.getFileName());
        rsp.setFileInfoUrl(request.getFileInfoUrl());

        if (request.getFileMetadata() != null && !request.getFileMetadata().isEmpty()) {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String jsonMetadata = objectMapper.writeValueAsString(request.getFileMetadata());
                rsp.setFileMetadata(jsonMetadata);
            } catch (JsonProcessingException e) {
                rsp.setFileMetadata("{}");
                throw new ValidationException("Invalid metadata format", e);
            }
        } else {
            rsp.setFileMetadata("{}");
        }

        rsp.setFileContentDescription(request.getFileContentDescription());
        rsp.setFileVersion(request.getFileVersion());
        rsp.setSha256Hash(request.getSha256());
        if (request instanceof MgmtFileUrlSupportPackageCreateRequest) {
            String fileUrl = ((MgmtFileUrlSupportPackageCreateRequest) request).getFileUrl();
            rsp.setFileUrl(fileUrl);
        }
        return rsp;
    }

}
