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
import java.lang.reflect.Field;
import java.security.SignatureException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.cosmos.models.ddi.ArtifactType;
import org.cosmos.models.ddi.DdiArtifact;
import org.cosmos.models.ddi.DdiArtifactHash;
import org.cosmos.models.ddi.DdiAutoConfirmationState;
import org.cosmos.models.ddi.DdiChunk;
import org.cosmos.models.ddi.DdiConfig;
import org.cosmos.models.ddi.DdiConfirmationBase;
import org.cosmos.models.ddi.DdiControllerBase;
import org.cosmos.models.ddi.DdiDDArtifactHash;
import org.cosmos.models.ddi.DdiDsMetadata;
import org.cosmos.models.ddi.DdiEcu;
import org.cosmos.models.ddi.DdiEsp;
import org.cosmos.models.ddi.DdiEspTypes;
import org.cosmos.models.ddi.DdiFeedbackResponse;
import org.cosmos.models.ddi.DdiMetadata;
import org.cosmos.models.ddi.DdiPolling;
import org.cosmos.models.ddi.DdiRestConstants;
import org.cosmos.models.ddi.DdiRsp;
import org.cosmos.models.ddi.DdiRspTypes;
import org.cosmos.models.ddi.DdiSignatureType;
import org.cosmos.models.ddi.DdiSoftware;
import org.cosmos.models.ddi.DdiSoftwareArtifact;
import org.cosmos.models.ddi.DdiSupportPackageHash;
import org.cosmos.models.ddi.SoftwareModuleInfo;
import org.cosmos.models.kafka.FileDeleteErrorMessage;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.artifacts.constants.ArtifactsStatus;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;
import org.cosmos.models.mgmt.supportpackage.constants.FileTransferStatus;
import org.eclipse.hawkbit.api.ApiType;
import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.URLPlaceholder;
import org.eclipse.hawkbit.api.URLPlaceholder.EspRspData;
import org.eclipse.hawkbit.api.URLPlaceholder.SoftwareData;
import org.eclipse.hawkbit.feignclient.kafka.KafkaMessageService;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventHeader;
import org.eclipse.hawkbit.feignclient.kafka.event.KafkaEventTemplate;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.DistributionSetManagement;
import org.eclipse.hawkbit.repository.EcuModelManagement;
import org.eclipse.hawkbit.repository.SupportPackageManagement;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.VehicleManagement;
import org.eclipse.hawkbit.repository.VersionManagement;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.jpa.ActionArtifactRepository;
import org.eclipse.hawkbit.repository.jpa.DistributionSetModuleRepository;
import org.eclipse.hawkbit.repository.jpa.model.DistributionSetModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaAction;
import org.eclipse.hawkbit.repository.jpa.model.JpaActionArtifact;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.service.DdiSignatureService;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.EcuModel;
import org.eclipse.hawkbit.repository.model.EspEcuRollout;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.Rsp;
import org.eclipse.hawkbit.repository.model.SigningCertificateConfiguration;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareOfTarget;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetSoftware;
import org.eclipse.hawkbit.repository.model.Vehicle;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;
import org.springframework.http.HttpRequest;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Utility class for the DDI API.
 */
@Slf4j
public final class DataConversionHelper {

    private static final String DIVIDE_64_BIT_HASH_IN_2_DIGIT = "(.{2})";
    private static final String ARTIFACT_FILETYPE = "ARTIFACT";
    private static final String EXPIRED = "EXPIRED";
    private static final String ERROR = "ERROR";
    private static KafkaMessageService kafkaMessageService;


    // utility class, private constructor.
    private DataConversionHelper() {

    }

    public static void initializeKafkaMessageService(KafkaMessageService service) {
        kafkaMessageService = service;
    }

    static List<DdiChunk> createChunks(final Target target, final Action uAction,
                                       final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
                                       final HttpRequest request, final ControllerManagement controllerManagement, final VersionManagement versionManagement) {

        final Map<Long, List<SoftwareModuleInfo>> metadata = controllerManagement
                .findTargetVisibleMetaDataBySoftwareModuleId(uAction.getDistributionSet().getModules().stream()
                        .map(SoftwareModule::getId).toList());

        return new ResponseList<>(uAction.getDistributionSet().getModules().stream()
                .map(module -> {
                    List<SoftwareModuleInfo> moduleMetadata = metadata.get(module.getId());
                    DdiMetadata ddiMetadata = moduleMetadata != null ? mapMetadata(moduleMetadata) : null;
                    return new DdiChunk(mapChunkLegacyKeys(module.getType().getKey()),
                            module.getName(), module.isEncrypted() ? Boolean.TRUE : null,
                            createArtifacts(target, module, artifactUrlHandler, systemManagement, request, versionManagement),
                            ddiMetadata, module.getFormat().getKey());
                })
                .toList());

    }

    /**
     * Create Metadata object contains list of DistributionSet metadata
     *
     * @param setId
     * @param distributionSetManagement
     * @return DdiDsMetadata: list of metadata of a distribution set.
     */
    static DdiDsMetadata createDatasetMetadata(final long setId, final DistributionSetManagement distributionSetManagement) {
        List<DistributionSetMetadata> dsMetadata = distributionSetManagement
                .findMetaDataByDistributionSetId(PageRequest.of(0, Integer.MAX_VALUE), setId)
                .getContent();

        return CollectionUtils.isEmpty(dsMetadata) ? null :
                new DdiDsMetadata(dsMetadata.stream()
                        .collect(Collectors.toMap(DistributionSetMetadata::getKey, DistributionSetMetadata::getValue)));
    }

    /**
     * @param metadata
     * @return list of metadata of a software module.
     * @author josephk
     * @modfied on 31-07-2023
     * Update DdiMetadata structure, representing metadata as {"key":"value"} pair.
     */

    private static DdiMetadata mapMetadata(final List<SoftwareModuleInfo> metadata) {
        if (CollectionUtils.isEmpty(metadata)) {
            return null;
        }
        Map<String, String> convertedMap = metadata.stream()
                .collect(Collectors.toMap(
                        dto -> String.valueOf(dto.getSoftwareModuleId()),
                        dto -> String.valueOf(dto.isTargetVisible())
                ));

        return new DdiMetadata(convertedMap);
    }


    private static String mapChunkLegacyKeys(final String key) {
        if ("application".equals(key)) {
            return "bApp";
        }
        if ("runtime".equals(key)) {
            return "jvm";
        }

        return key;
    }

    static List<DdiArtifact> createArtifacts(final Target target, final SoftwareModule module,
                                             final ArtifactUrlHandler artifactUrlHandler, final SystemManagement systemManagement,
                                             final HttpRequest request, final VersionManagement versionManagement) {

        if (module.getArtifactSoftwareModuleAssociations().isEmpty()) {
            throw new EntityNotFoundException("No artifacts found for software module ID " + module.getId());
        }

        return new ResponseList<>(module.getArtifactSoftwareModuleAssociations().stream()
                .map(artifactAssociation -> createArtifact(target, artifactUrlHandler, artifactAssociation, systemManagement, request, versionManagement))
                .toList());
    }

    private static DdiArtifact createArtifact(final Target target, final ArtifactUrlHandler artifactUrlHandler,
                                              final ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation, final SystemManagement systemManagement, final HttpRequest request, VersionManagement versionManagement) {
        Artifacts artifact = artifactSoftwareModuleAssociation.getArtifact();
        final DdiArtifact file = new DdiArtifact();
        file.setHashes(new DdiArtifactHash(null, artifact.getMd5Hash(), artifact.getSha256Hash()));
        file.setFilename(artifact.getFileName());
        file.setSize(artifact.getFileSize());
        versionManagement.getById(artifactSoftwareModuleAssociation.getSourceVersion().getId()).ifPresent(v -> file.setSourceVersion(v.getName()));
        versionManagement.getById(artifactSoftwareModuleAssociation.getTargetVersion().getId()).ifPresent(v -> file.setTargetVersion(v.getName()));
        artifactUrlHandler
                .getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(),
                                systemManagement.getTenantMetadata().getId(), target.getControllerId(), target.getId(),
                                new SoftwareData(artifactSoftwareModuleAssociation.getSoftwareModule().getId(), artifact.getFileName(), artifact.getId(),
                                        null)),
                        ApiType.DDI, request.getURI())
                .forEach(entry -> file.add(Link.of(entry.getRef()).withRel(entry.getRel()).expand()));

        return file;

    }

    public static DdiConfirmationBase createConfirmationBase(final Target target, final Action activeAction,
                                                             final DdiAutoConfirmationState autoConfirmationState) {
        final String controllerId = target.getControllerId();
        final DdiConfirmationBase confirmationBase = new DdiConfirmationBase(autoConfirmationState);
        if (autoConfirmationState.isActive()) {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class)
                            .deactivateAutoConfirmation(controllerId))
                    .withRel(DdiRestConstants.AUTO_CONFIRM_DEACTIVATE).expand());
        } else {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class)
                            .activateAutoConfirmation(controllerId, null))
                    .withRel(DdiRestConstants.AUTO_CONFIRM_ACTIVATE).expand());
        }
        if (activeAction != null && activeAction.isWaitingConfirmation()) {
            confirmationBase.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class)
                            .getConfirmationBaseAction(controllerId, activeAction.getId(), calculateEtag(activeAction), null))
                    .withRel(DdiRestConstants.CONFIRMATION_BASE).expand());
        }

        return confirmationBase;
    }

    /**
     * create DdiControllerBase based on the presents of inventoryHash and active action
     * if inventoryHash not exist, add configData link
     * else if inventoryHash and has active action, add distributionBase link
     *
     * @param hashCodeExist             boolean, inventoryHash exist?
     * @param target                    of the target that matches to controller id
     * @param activeAction              of action with status active
     * @param defaultControllerPollTime of polling time
     * @return DdiControllerBase with links
     */
    public static DdiControllerBase fromTarget(final boolean hashCodeExist, final Target target, final Action activeAction, final Action installedAction, final String defaultControllerPollTime, long pollingId) {
        final DdiControllerBase result = new DdiControllerBase(
                new DdiConfig(new DdiPolling(defaultControllerPollTime)));

        if (hashCodeExist) {
            getDeploymentBaseActionLink(result, target, activeAction);
        } else {
            getPushInventoryActionLink(result, target);
        }

        if (installedAction != null && !installedAction.isActive()) {
            result.add(
                    WebMvcLinkBuilder
                            .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class)
                                    .getControllerInstalledAction(target.getControllerId(), installedAction.getId(), null))
                            .withRel(DdiRestConstants.INSTALLED_BASE_ACTION).expand());
        }

        return activeAction != null
                ? getActiveWaitingOrCancelingOrCanceled(result, target, activeAction)
                : result;
    }

    private static DdiControllerBase getDeploymentBaseActionLink(final DdiControllerBase result, final Target target,
                                                                 final Action activeAction) {
        return activeAction != null
                && (activeAction.getStatus() == DeviceActionStatus.RUNNING
                || activeAction.getStatus() == DeviceActionStatus.DD_SENT)
                ? result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder
                        .methodOn(DdiRootController.class)
                        .getTargetBasedeploymentAction(target.getControllerId(),
                                activeAction.getId(), calculateEtag(activeAction), null))
                .withRel(DdiRestConstants.DEPLOYMENT_BASE_ACTION).expand())
                : result;
    }

    private static DdiControllerBase getPushInventoryActionLink(final DdiControllerBase result, final Target target) {
        try {
            result.add(WebMvcLinkBuilder
                    .linkTo(WebMvcLinkBuilder.methodOn(DdiRootController.class)
                            .putDeviceInventory(null, target.getControllerId())

                    )
                    .withRel(DdiRestConstants.INVENTORY).expand());
        } catch (SignatureException | JsonProcessingException | IllegalAccessException e) {
            log.error(e.getMessage());
        }
        return result;
    }

    private static DdiControllerBase getActiveWaitingOrCancelingOrCanceled(final DdiControllerBase result, final Target target,
                                                                           final Action activeAction) {
        if (activeAction.isWaitingConfirmation()) {
            result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder
                            .methodOn(DdiRootController.class)
                            .getConfirmationBaseAction(target.getControllerId(),
                                    activeAction.getId(), calculateEtag(activeAction), null))
                    .withRel(DdiRestConstants.CONFIRMATION_BASE).expand());

        } else if (activeAction.isCancelingOrCanceled()) {
            Boolean flag = result.setCanceledBasedOnExecutionStatus(activeAction.getStatus());
            if (flag != null) {
                result.setCanceled(flag);
            }
        }
        return result;
    }

    /**
     * Calculates an etag for the given {@link Action} based on the entities
     * force switch.
     *
     * @param action to calculate the etag for
     * @return the etag
     */
    private static int calculateEtag(final Action action) {
        final int prime = 31;
        int result = action.hashCode();
        int offsetPrime = 1237;
        offsetPrime = action.hasMaintenanceSchedule() && action.isMaintenanceWindowAvailable() ? 1249 : offsetPrime;

        result = prime * result + offsetPrime;
        return result;
    }

    static Map<String, String> flattenObject(Object obj, String parentKey) throws IllegalAccessException {
        Map<String, String> flattenedMap = new HashMap<>();

        if (obj instanceof Map<?, ?> map) {
            flattenedMap.putAll(flattenMap(map, parentKey));
        } else if (obj instanceof List<?> list) {
            flattenedMap.putAll(flattenList(list, parentKey));
        } else if (obj != null) {
            flattenedMap.putAll(flattenCustomObject(obj, parentKey));
        }

        return flattenedMap;
    }

    private static Map<String, String> flattenCustomObject(Object obj, String parentKey) throws IllegalAccessException {
        Map<String, String> flattenedMap = new HashMap<>();
        Map<String, Object> objectFields = getObjectFields(obj);

        for (Map.Entry<String, Object> entry : objectFields.entrySet()) {
            String fieldKey = entry.getKey();
            Object fieldValue = entry.getValue();
            String fullKey = parentKey != null ? parentKey + "." + fieldKey : fieldKey;

            flattenedMap.putAll(processFieldValue(fieldValue, fullKey));
        }

        return flattenedMap;
    }

    private static Map<String, String> processFieldValue(Object fieldValue, String fullKey) throws IllegalAccessException {
        Map<String, String> flattenedMap = new HashMap<>();
        if (fieldValue instanceof Map || fieldValue instanceof List) {
            flattenedMap.putAll(flattenObject(fieldValue, fullKey));
        } else {
            flattenedMap.put(fullKey, fieldValue != null ? fieldValue.toString() : "");
        }
        return flattenedMap;
    }

    private static Map<String, String> flattenMap(Map<?, ?> map, String parentKey) throws IllegalAccessException {
        Map<String, String> flattenedMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            String fullKey = parentKey != null ? parentKey + "." + key : key;

            if (value instanceof Map || value instanceof List) {
                flattenedMap.putAll(flattenObject(value, fullKey));
            } else if (value != null) {
                flattenedMap.putAll(flattenObject(value, fullKey));
            } else {
                flattenedMap.put(fullKey, "");
            }
        }
        return flattenedMap;
    }

    private static Map<String, String> flattenList(List<?> list, String parentKey) throws IllegalAccessException {
        Map<String, String> flattenedMap = new HashMap<>();
        for (int i = 0; i < list.size(); i++) {
            Object value = list.get(i);
            String fullKey = parentKey != null ? parentKey + "[" + i + "]" : "[" + i + "]";

            if (value instanceof Map || value instanceof List) {
                flattenedMap.putAll(flattenObject(value, fullKey));
            } else {
                flattenedMap.put(fullKey, value != null ? value.toString() : "");
            }
        }
        return flattenedMap;
    }


    private static void flattenObject(String prefix, Object obj, Map<String, String> flattenedMap) {
        // Handle null values
        if (obj == null) {
            flattenedMap.put(prefix, "");
            return;
        }

        // Handle primitive types and String
        if (obj.getClass().isPrimitive() || obj instanceof String) {
            flattenedMap.put(prefix, obj.toString());
            return;
        }

        // Handle other types
        // Here you can use reflection to get fields, methods, etc.
        // and recursively flatten nested objects if needed
        // For brevity, let's assume you have a method getObjectFields()
        // that returns a Map<String, Object> of field names and values

        Map<String, Object> objectFields = getObjectFields(obj);
        for (Map.Entry<String, Object> entry : objectFields.entrySet()) {
            String fieldKey = entry.getKey();
            Object fieldValue = entry.getValue();
            String fullKey = prefix.isEmpty() ? fieldKey : prefix + "." + fieldKey;

            flattenObject(fullKey, fieldValue, flattenedMap);
        }
    }

    @SuppressWarnings("squid:S3011")
    private static Map<String, Object> getObjectFields(Object obj) {
        // Implement this method to retrieve fields and values from your object
        // For a map, you can simply cast it to Map<String, Object>
        if (obj instanceof Map) {
            return (Map<String, Object>) obj;
        } else {
            Map<String, Object> objectFields = new HashMap<>();

            Class<?> clazz = obj.getClass();
            Field[] fields = clazz.getDeclaredFields();

            for (Field field : fields) {
                field.setAccessible(true);
                try {
                    Object fieldValue = field.get(obj);
                    if (fieldValue != null) {
                        objectFields.put(field.getName(), fieldValue);
                    }
                } catch (IllegalAccessException e) {
                    log.error(e.getMessage());
                }
            }

            return objectFields;
        }
    }

    /**
     * Creates deployment log request link using target, active action and requested tenant.
     *
     * @param result       the {@link DdiFeedbackResponse} hateoas representation model.
     * @param target       the {@link Target}
     * @param activeAction the {@link Action}, active action.
     */
    public static void getDeploymentLogLink(final DdiFeedbackResponse result, final Target target,
                                            final Action activeAction) {
        if (!result.hasLink(DdiRestConstants.LOG_COLLECTION)) {
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(DdiRestConstants.POST_DEPLOYMENT_LOGS_PATH)
                    .buildAndExpand(target.getControllerId(), activeAction.getId())
                    .toUriString();
            Link logLink = Link.of(url).withRel(DdiRestConstants.LOG_COLLECTION);
            result.add(logLink);
        }
    }

    /**
     * Adds an inventory collection link to the given DdiFeedbackResponse.
     * <p>
     * This method generates a link to the device inventory endpoint and adds it to the provided DdiFeedbackResponse.
     * If an exception occurs during the link generation, it logs the error.
     *
     * @param result The DdiFeedbackResponse to which the inventory collection link will be added.
     * @param action The action associated with the target for which the inventory collection link is generated.
     */
    public static void addInventoryCollectionLink(final DdiFeedbackResponse result, final Action action) {
        try {
            if (!result.hasLink(DdiRestConstants.INVENTORY)) {
                result.add(WebMvcLinkBuilder.linkTo(WebMvcLinkBuilder
                                .methodOn(DdiRootController.class)
                                .putDeviceInventory(null, action.getTarget().getControllerId()))
                        .withRel(DdiRestConstants.INVENTORY).expand());
            }
        } catch (SignatureException | JsonProcessingException | IllegalAccessException e) {
            log.error(e.getMessage());
        }
    }

    public static void cancelDeviceActionLink(final DdiFeedbackResponse result,
                                              final Action action) {
        if (!result.hasLink(DdiRestConstants.CANCEL_ACTION)) {
            String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(DdiRestConstants.CONTROLLER_CANCEL_ACTION_PATH)
                    .buildAndExpand(action.getTarget().getControllerId(), action.getId())
                    .toUriString();
            Link logLink = Link.of(url).withRel(DdiRestConstants.CANCEL_ACTION);
            result.add(logLink);
        }
    }


    /**
     * Creates a RSPs based on Rollout id and generates signature for rsp private key and rsp server certificate
     * This method fetches the RSPs from the Rsp rollout table for the provided Rollout id.
     *
     * @param action The action for a given rollout
     * @return A list of {@link DdiRsp} based on different file types.
     */
    public static DdiRspTypes generateRsp(final ArtifactUrlHandler artifactUrlHandler,
                                          final SystemManagement systemManagement,
                                          SupportPackageManagement supportPackageManagement, Action action,
                                          final HttpRequest request,
                                          final SigningCertificateConfiguration signingCertificateConfiguration,
                                          final DdiSignatureService ddiSignatureService) {
        log.debug("Generating RSP for action: {}", action.getId());
        DdiRspTypes ddiRspTypes = new DdiRspTypes();
        Rollout rollout = action.getRollout();
        if (action.getRollout() != null) {
            log.debug("Action {} has a rollout. Fetching RSP support packages.", action.getId());
            List<Rsp> rspList = supportPackageManagement.getRSPSupportPackages(action.getRollout().getId());
            if (!rspList.isEmpty()) {
                log.debug("Found {} RSP support packages for rollout ID: {}", rspList.size(), action.getRollout().getId());
                for (Rsp rsp : rspList) {
                    String fileSize = rsp.getMetadata().get("size");
                    log.debug("Processing RSP: {}, file size: {}", rsp.getFileName(), fileSize);
                    DdiRsp ddiRsp = new DdiRsp(
                            rsp.getFileType(),
                            rsp.getFileName(),
                            convertToBytes(fileSize),
                            "",
                            ddiSignatureService.generateAndCacheSignature(rsp.getId(), Base64.getEncoder().encodeToString(rsp.getSha256Hash().getBytes()), DdiSignatureType.RSP, signingCertificateConfiguration, rollout),
                            new DdiSupportPackageHash(rsp.getSha256Hash(), rsp.getMd5Hash())
                    );
                    log.debug("Generated DdiRsp for file: {}", rsp.getFileName());
                    artifactUrlHandler.getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(), DdiRestConstants.RSP,
                                    systemManagement.getTenantMetadata().getId(),
                                    new EspRspData(rsp.getFileName(), rsp.getSha256Hash().replaceAll(DIVIDE_64_BIT_HASH_IN_2_DIGIT, "$1/"))),
                            ApiType.DDI, request.getURI()).stream().filter(en -> en.getRel().equals("download")).forEach(entry -> ddiRsp.add(Link.of(entry.getRef()).withRel(entry.getRel()).expand()));
                    log.debug("Added download links to DdiRsp for file: {}", rsp.getFileName());
                    switch (rsp.getFileType().getName()) {
                        case "BASELINE_INVENTORY" -> ddiRspTypes.setBaselineInventory(ddiRsp);
                        case "INSTALLATION_ROLLBACK_PLAN" -> ddiRspTypes.setInstallationRollbackPlan(ddiRsp);
                        case "DTC_BLACKLIST" -> ddiRspTypes.setDtcBlacklist(ddiRsp);
                        case "RULE_ENGINE_CONFIG" -> ddiRspTypes.setRuleEngineConfig(ddiRsp);
                        case "PROXI" -> ddiRspTypes.setProxi(ddiRsp);
                        case "PROXI_SIGNATURE" -> ddiRspTypes.setProxiSignature(ddiRsp);
                        case "WHATS_NEW" -> ddiRspTypes.setWhatsNew(ddiRsp);
                        case "UDS_GLOBAL_PRE_INSTALL" -> ddiRspTypes.setUdsGlobalPreInstall(ddiRsp);
                        case "UDS_GLOBAL_POST_INSTALL" -> ddiRspTypes.setUdsGlobalPostInstall(ddiRsp);
                    }
                    log.debug("Set DdiRsp for file type: {}", rsp.getFileType().getName());
                }
            } else {
                log.debug("No RSP support packages found for rollout ID: {}", action.getRollout().getId());
            }
        } else {
            log.debug("Action {} does not have a rollout.", action.getId());
        }
        return ddiRspTypes;
    }

    /**
     * Creates a list of ECU (Electronic Control Unit) nodes for a given target and action.
     * This method fetches the software details of ECUs from the target software table for the provided target and action.
     *
     * @param target  The target for which the ECU nodes are to be fetched.
     * @param uAction The action associated with the ECU nodes.
     * @return A list of {@link DdiEcu} representing the ECU nodes for the given target and action.
     */
    static List<DdiEcu> createEcus(final Target target, final Action uAction,
                                   final ArtifactUrlHandler artifactUrlHandler,
                                   final EcuModelManagement ecuModelManagement,
                                   final ControllerManagement controllerManagement,
                                   final SystemManagement systemManagement,
                                   final HttpRequest request,
                                   final SupportPackageManagement supportPackageManagement,
                                   final VehicleManagement vehicleManagement, final DistributionSetModuleRepository distributionSetModuleRepository,
                                   final ActionArtifactRepository actionArtifactRepository,
                                   final SigningCertificateConfiguration signingCertificateConfiguration,
                                   final DdiSignatureService ddiSignatureService) {
        //get the list of ecu models by vehicle
        Set<EcuModel> ecuModelsByVehicle = vehicleManagement.get(target.getVehicleModelId()).map(Vehicle::getVehicleEcu)
                .stream().flatMap(Set::stream).collect(Collectors.toSet());
        log.debug("Ecu models assigned to Vehicle {}", ecuModelsByVehicle);
        //get the distribution set modules attached to the rollout
        Set<SoftwareModule> softwareModulesForRollout = uAction.getDistributionSet().getModules();
        log.debug("Software Modules assigned to Rollout {}", softwareModulesForRollout);
        // Exclude software modules without valid artifacts
        softwareModulesForRollout = softwareModulesForRollout.stream()
                .filter(module -> module.getArtifactSoftwareModuleAssociations().stream()
                        .map(ArtifactSoftwareModuleAssociation::getArtifact)
                        .anyMatch(DataConversionHelper::isArtifactActive))
                .collect(Collectors.toSet());

        log.debug("Software Modules after filtering: {}", softwareModulesForRollout);
        //get the list of ecu models by software module linked to the rollout
        Set<EcuModel> ecuModelsBySoftwareModule = softwareModulesForRollout.stream().map(SoftwareModule::getSoftwareEcuModels)
                .flatMap(Set::stream).collect(Collectors.toSet());
        log.debug("List of all the ecu models mapped to Software Modules which are assigned to Rollout {}", ecuModelsBySoftwareModule);
        //get the unique set of software module names linked to the rollout
        Set<String> softwareModuleNamesFromRollout =
                softwareModulesForRollout.stream()
                        .map(SoftwareModule::getName)
                        .collect(Collectors.toSet());
        //combine ecu models by vehicle and software module. And get the common ecu models
        Set<EcuModel> commonEcuModelsOfVehicleAndSoftwareModules = ecuModelsBySoftwareModule.stream().filter(ecuModelsByVehicle::contains).collect(Collectors.toSet());
        log.debug("commonEcuModelsOfVehicleAndSoftwareModules {}", commonEcuModelsOfVehicleAndSoftwareModules);
        //pass the target and module names from "unique set of software module names linked to the rollout" to get the target software
        Set<TargetSoftware> targetSoftwares = !softwareModuleNamesFromRollout.isEmpty() ? controllerManagement.getEcuNodes(target, softwareModuleNamesFromRollout) : Collections.emptySet();
        Set<String> ecuNodes = targetSoftwares.stream().map(SoftwareOfTarget::getNode).collect(Collectors.toSet());
        //get the ecu models by target software
        Set<EcuModel> ecuModelsByTargetSoftware = !ecuNodes.isEmpty() ? ecuModelManagement.getEcuModelByNodeIds(ecuNodes) : Collections.emptySet();
        //combine common ecu models by vehicle and software module with ecu models by target software and get the final ecu models
        Set<EcuModel> finalValidatedEcuModelsList = commonEcuModelsOfVehicleAndSoftwareModules.stream().filter(ecuModelsByTargetSoftware::contains).collect(Collectors.toSet());
        log.debug("finalValidatedEcuModelsList {}", finalValidatedEcuModelsList);
        List<EspEcuRollout> espEcuRollouts = new ArrayList<>();
        Set<String> ecuNodesFinal = finalValidatedEcuModelsList.stream().map(EcuModel::getEcuNodeId).collect(Collectors.toSet());
        if (finalValidatedEcuModelsList.isEmpty()) {
            log.info("No valid ECU models found after filtering by vehicle, software module, and target software.");
            return Collections.emptyList();
        }
        //get the support package based on ecu node address from espEcuRollouts for which ecu node address matches with the ecuModel node id
        if (uAction.getRollout() != null) {
            espEcuRollouts = !ecuNodesFinal.isEmpty() ? supportPackageManagement.getByRolloutIdAndEcuNodeAddressList(uAction.getRollout().getId(), ecuNodesFinal)
                    : Collections.emptyList();
        }

        // Fetch DistributionSetModules using the DistributionSet ID
        List<DistributionSetModule> distributionSetModules = distributionSetModuleRepository
                .findByDsSetId(uAction.getDistributionSet().getId());

        // Create a map of software module  -> target version ID
        Map<JpaSoftwareModule, String> softwareModuleToTargetVersionMap = distributionSetModules.stream()
                .filter(module -> module.getSm() != null && module.getVersion() != null && module.getVersion().getName() != null)
                .collect(Collectors.toMap(
                        DistributionSetModule::getSm,
                        module -> module.getVersion().getName()
                ));

        // get the support package based on ecu node address from espEcuRollouts for which ecu node address matches with the ecuModel node id
        List<EspEcuRollout> finalEspEcuRollouts = espEcuRollouts;
        log.info("finalEspEcuRollouts count for generating DD for the action {} of the rollout {} : {}", uAction.getId(),
                uAction.getRollout().getId(), finalEspEcuRollouts.size());
        return finalValidatedEcuModelsList.stream()
                .map(ecuModel -> new DdiEcu(ecuModel.getEcuNodeId(), ecuModel.getEcuModelType(),
                        createSoftware(softwareModuleToTargetVersionMap, targetSoftwares, ecuModel.getEcuNodeId(),
                                target, artifactUrlHandler, systemManagement, request, actionArtifactRepository, uAction), getESPForFileType(artifactUrlHandler, systemManagement, finalEspEcuRollouts, ecuModel.getEcuNodeId(), request, signingCertificateConfiguration, ddiSignatureService)))
                .toList();
    }


    /**
     * Retrieves the ESP types for a given ECU (Electronic Control Unit) node ID.
     *
     * @param espEcuRollouts                  the list of ESP ECU rollouts to search through
     * @param ecuNodeId                       the ECU node ID to match against
     * @param signingCertificateConfiguration the deployment certificates configuration used for generating signatures
     * @return a populated DdiEspTypes object containing the ESP types for the specified ECU node ID
     */

    private static DdiEspTypes getESPForFileType(final ArtifactUrlHandler artifactUrlHandler,
                                                 final SystemManagement systemManagement,
                                                 List<EspEcuRollout> espEcuRollouts,
                                                 String ecuNodeId,
                                                 final HttpRequest request,
                                                 final SigningCertificateConfiguration signingCertificateConfiguration,
                                                 final DdiSignatureService ddiSignatureService) {
        DdiEspTypes ddiEspTypes = new DdiEspTypes();

        for (EspEcuRollout espEcuRollout : espEcuRollouts) {
            log.debug("Processing ESP ECU rollout for ECU node address: {}", espEcuRollout.getEcuNodeAddress());
            Rollout rollout = espEcuRollout.getRollout();

            if (espEcuRollout.getEcuNodeAddress().equals(ecuNodeId)) {
                log.info("Match found for ECU node ID: {}", ecuNodeId);
                String fileSize = espEcuRollout.getSupportPackage().getMetadata().get("size");
                log.debug("File size for support package: {}", fileSize);
                DdiEsp ddiEsp = new DdiEsp(
                        espEcuRollout.getSupportPackage().getFileType(),
                        espEcuRollout.getSupportPackage().getFileName(),
                        convertToBytes(fileSize),
                        "",
                        ddiSignatureService.generateAndCacheSignature(espEcuRollout.getSupportPackage().getId(), Base64.getEncoder().encodeToString(espEcuRollout.getSupportPackage().getSha256Hash().getBytes()), DdiSignatureType.ESP, signingCertificateConfiguration, rollout),
                        new DdiSupportPackageHash(espEcuRollout.getSupportPackage().getSha256Hash(), espEcuRollout.getSupportPackage().getMd5Hash())
                );
                log.debug("Generated DdiEsp for file: {}", espEcuRollout.getSupportPackage().getFileName());
                artifactUrlHandler.getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(), DdiRestConstants.ESP,
                                systemManagement.getTenantMetadata().getId(),
                                new EspRspData(espEcuRollout.getSupportPackage().getFileName(), espEcuRollout.getSupportPackage().getSha256Hash().replaceAll(DIVIDE_64_BIT_HASH_IN_2_DIGIT, "$1/"))),
                        ApiType.DDI, request.getURI()).stream().filter(en -> en.getRel().equals("download")).forEach(entry -> ddiEsp.add(Link.of(entry.getRef()).withRel(entry.getRel()).expand()));
                log.debug("Added download links to DdiEsp for file: {}", espEcuRollout.getSupportPackage().getFileName());

                switch (espEcuRollout.getSupportPackage().getFileType().getName()) {
                    case "VARIANT_CODING" -> {
                        ddiEspTypes.setVariantCoding(ddiEsp);
                        log.debug("Set DdiEsp for file type: VARIANT_CODING");
                    }
                    case "LICENSE" -> {
                        ddiEspTypes.setLicense(ddiEsp);
                        log.debug("Set DdiEsp for file type: LICENSE");
                    }
                    case "UDS_FLOW" -> {
                        ddiEspTypes.setUdsFlow(ddiEsp);
                        log.debug("Set DdiEsp for file type: UDS_FLOW");
                    }
                    case "ECU_SCRIPT" -> {
                        ddiEspTypes.setEcuScript(ddiEsp);
                        log.debug("Set DdiEsp for file type: ECU_SCRIPT");
                    }
                    case "ADA_LICENSE" -> {
                        ddiEspTypes.setAdaLicense(ddiEsp);
                        log.debug("Set DdiEsp for file type: ADA_LICENSE");
                    }
                    case "ADA_CERTIFICATE" -> {
                        ddiEspTypes.setAdaCertificate(ddiEsp);
                        log.debug("Set DdiEsp for file type: ADA_CERTIFICATE");
                    }
                }
            }
        }

        log.debug("Completed processing ESP for file type for ECU node ID: {}", ecuNodeId);
        return ddiEspTypes;
    }

    /**
     * Converts a size string (e.g., "10MB") to bytes.
     *
     * @param size The size string to convert.
     * @return The size in bytes.
     */
    public static long convertToBytes(String size) {
        // Remove non-numeric part (e.g., "MB")
        String numericPart = size.replaceAll("\\D", "");

        // Convert to long
        long value = Long.parseLong(numericPart);

        // Convert to bytes (assuming the input is in MB)
        return value * 1024 * 1024;
    }

    /**
     * Creates a list of {@link DdiSoftware} for a given node based on the provided target software map,
     * rollout action, and uninstall versions.
     *
     * @param softwareModuleToTargetVersionMap Map of {@link JpaSoftwareModule} to target version strings.
     * @param targetSoftware                   Set of {@link TargetSoftware} representing software currently installed on the node.
     * @param nodeId                           Identifier of the target node.
     * @param target                           The target system details.
     * @param artifactUrlHandler               Handler to resolve artifact URLs.
     * @param systemManagement                 System management interface.
     * @param request                          HTTP request context.
     * @param actionArtifactRepository         Repository to fetch action artifacts.
     * @param action                           The rollout {@link Action} containing update instructions.
     * @return List of {@link DdiSoftware} objects representing the software to install, update, or uninstall.
     */
    private static List<DdiSoftware> createSoftware(
            final Map<JpaSoftwareModule, String> softwareModuleToTargetVersionMap,
            Set<TargetSoftware> targetSoftware,
            String nodeId,
            final Target target,
            final ArtifactUrlHandler artifactUrlHandler,
            final SystemManagement systemManagement,
            final HttpRequest request,
            final ActionArtifactRepository actionArtifactRepository,
            final Action action) {

        log.info("Starting software list creation for nodeId: {}", nodeId);
        Map<String, TargetSoftware> targetSoftwareMap = targetSoftware.stream()
                .collect(Collectors.toMap(TargetSoftware::getComponentId, Function.identity()));

        boolean isAota = action != null && action.getRollout() != null
                && MgmtRolloutType.AOTA == action.getRollout().getType();

        MgmtUpdateAction updateAction = action != null && action.getRollout() != null
                ? action.getRollout().getUpdateAction()
                : null;

        List<String> uninstallVersions = action != null && action.getRollout() != null
                ? sanitizeUninstallVersions(action.getRollout().getUpdateActionUninstallVersion())
                : null;

        log.debug("Rollout Type: {}, Update Action: {}, Uninstall Versions: {}",
                isAota ? "AOTA" : "FOTA", updateAction, uninstallVersions);

        return softwareModuleToTargetVersionMap.entrySet().stream()
                .filter(entry -> {
                    JpaSoftwareModule softwareModule = entry.getKey();
                    TargetSoftware reportedSoftware = targetSoftwareMap.get(softwareModule.getName());
                    boolean containsKey = reportedSoftware != null;
                    boolean nodeMatches = containsKey && reportedSoftware.getNode().equals(nodeId);
                    log.info("Processing software module {} for ECU node {} (containsKey: {}, nodeMatches: {})", softwareModule.getName(),
                            nodeId, containsKey, nodeMatches);
                    if (isAota && action.getRollout() != null) {
                        if (updateAction == MgmtUpdateAction.UNINSTALLANY) {
                            return containsKey && nodeMatches && reportedSoftware.getVersion() != null && !reportedSoftware.getVersion().isEmpty();
                        } else if (updateAction == MgmtUpdateAction.UNINSTALLSPECIFIC) {
                            boolean match = containsKey && nodeMatches && uninstallVersions != null
                                    && uninstallVersions.contains(reportedSoftware.getVersion());
                            if (!match) {
                                log.info("Skipping {} as version {} is not in uninstall list {}",
                                        softwareModule.getName(),
                                        reportedSoftware != null ? reportedSoftware.getVersion() : null,
                                        uninstallVersions);
                            }
                            return match;
                        }
                    }
                    return containsKey && nodeMatches;
                })
                .map(entry -> getDdiSoftware(target, artifactUrlHandler, systemManagement, request,
                        actionArtifactRepository, action, entry, targetSoftwareMap, isAota, updateAction, uninstallVersions))
                .toList();
    }

    /**
     * Constructs a {@link DdiSoftware} object from a software module entry, considering the update action
     * and uninstall versions if applicable.
     *
     * @param target                   The target system.
     * @param artifactUrlHandler       Handler for resolving artifact URLs.
     * @param systemManagement         System management interface.
     * @param request                  HTTP request context.
     * @param actionArtifactRepository Repository to fetch action artifacts.
     * @param action                   Rollout action containing update instructions.
     * @param entry                    Entry containing {@link JpaSoftwareModule} and target version.
     * @param targetSoftwareMap        Map of componentId to {@link TargetSoftware} for lookup.
     * @param isAota                   Flag indicating if this is an AOTA rollout.
     * @param updateAction             The update action to perform.
     * @param uninstallVersions        List of versions to uninstall, if applicable.
     * @return Constructed {@link DdiSoftware} object.
     */
    private static DdiSoftware getDdiSoftware(Target target,
                                              ArtifactUrlHandler artifactUrlHandler,
                                              SystemManagement systemManagement,
                                              HttpRequest request,
                                              ActionArtifactRepository actionArtifactRepository,
                                              Action action,
                                              Map.Entry<JpaSoftwareModule, String> entry,
                                              Map<String, TargetSoftware> targetSoftwareMap,
                                              boolean isAota,
                                              MgmtUpdateAction updateAction,
                                              List<String> uninstallVersions) {

        JpaSoftwareModule softwareModule = entry.getKey();
        String targetVersion = entry.getValue();
        TargetSoftware reportedSoftware = targetSoftwareMap.get(softwareModule.getName());
        String reportedVersion = reportedSoftware != null ? reportedSoftware.getVersion() : null;

        log.info("Processing module: {} (reported version: {}, target version: {})",
                softwareModule.getName(), reportedVersion, targetVersion);

        DdiSoftwareArtifact artifact = shouldCreateArtifact(isAota, updateAction)
                ? createSoftwareArtifacts(target, softwareModule, artifactUrlHandler, systemManagement, request,
                reportedVersion, targetVersion, actionArtifactRepository, action)
                : new DdiSoftwareArtifact();

        log.info("Artifact {} for module: {}",
                (artifact != null ? "created" : "not created"), softwareModule.getName());

        List<String> uninstallList = (updateAction == MgmtUpdateAction.UNINSTALLSPECIFIC && uninstallVersions != null)
                ? uninstallVersions
                : null;

        DdiSoftware ddiSoftware = DdiSoftware.builder()
                .swType(softwareModule.getType().getName())
                .swFormat(softwareModule.getFormat().getName())
                .swName(softwareModule.getName())
                .swInstallerType(softwareModule.getSoftwareInstallerType().getName())
                .swArtifact(artifact)
                .updateAction(updateAction)
                .uninstallVersions(uninstallList)
                .build();

        log.info("Created DdiSoftware: {} for action: {}, uninstallVersions: {}",
                softwareModule.getName(), updateAction, uninstallList);

        return ddiSoftware;
    }

    /**
     * Determines whether a software artifact should be created based on the given
     * update action and rollout type.
     * <p>
     * For AOTA (Application Over-The-Air) rollouts, artifacts are <b>not</b> created
     * when the update action is {@code UNINSTALLANY} or {@code UNINSTALLSPECIFIC}.
     * In all other cases, artifacts should be created.
     *
     * @param isAota indicates if the rollout is of type AOTA (Application OTA)
     * @param action the update action being performed (e.g., INSTALL, UNINSTALLANY, UNINSTALLSPECIFIC)
     * @return {@code true} if an artifact should be created; {@code false} otherwise
     */
    private static boolean shouldCreateArtifact(boolean isAota, MgmtUpdateAction action) {
        return !(isAota && (action == MgmtUpdateAction.UNINSTALLANY || action == MgmtUpdateAction.UNINSTALLSPECIFIC));
    }

    /**
     * Sanitizes the raw uninstall version list by removing brackets and splitting comma-separated values.
     *
     * @param rawList Raw uninstall version list from the request payload.
     * @return Sanitized list of version strings or null if input is empty.
     */
    private static List<String> sanitizeUninstallVersions(List<String> rawList) {
        if (rawList == null || rawList.isEmpty()) return null;

        List<String> sanitized = rawList.stream()
                .flatMap(s -> Arrays.stream(s.replace("[", "").replace("]", "").split(",")))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .toList();

        log.debug("Sanitized uninstall versions: {}", sanitized);
        return sanitized;
    }

    /**
     * Creates a list of software artifacts for a given target and software module.
     * This method fetches the artifact details from the software module associated with the target.
     *
     * @param target             The target for which the software artifacts are to be fetched.
     * @param module             The software module for which the software artifacts are to be fetched.
     * @param artifactUrlHandler The handler for generating artifact URLs.
     * @param systemManagement   The system management service.
     * @param request            The HTTP request context.
     * @return A list of {@link DdiSoftwareArtifact} representing the software artifacts for the given target and software module.
     */
    static DdiSoftwareArtifact createSoftwareArtifacts(final Target target,
                                                       final SoftwareModule module,
                                                       final ArtifactUrlHandler artifactUrlHandler,
                                                       final SystemManagement systemManagement,
                                                       final HttpRequest request,
                                                       final String deviceReportedVersion,
                                                       final String rolloutTargetVersion,
                                                       final ActionArtifactRepository actionArtifactRepository,
                                                       final Action action) {
        long nowEpochSeconds = Instant.now().getEpochSecond();

        // Try to find DELTA artifact match
        Optional<ArtifactSoftwareModuleAssociation> deltaAssociation = module.getArtifactSoftwareModuleAssociations().stream()
                .filter(association -> {
                    Artifacts artifact = association.getArtifact();
                    return artifact != null &&
                            artifact.getFileName() != null &&
                            !artifact.getFileName().isEmpty() &&
                            isArtifactActive(artifact) &&
                            artifact.getFileType() == FileType.DELTA &&
                            deviceReportedVersion.equals(association.getSourceVersion().getName()) &&
                            rolloutTargetVersion.equals(association.getTargetVersion().getName()) &&
                            !isArtifactExpired(artifact, nowEpochSeconds) &&
                            artifact.getFileStatus().equals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
                })
                .findFirst();

        if (deltaAssociation.isPresent()) {
            saveActionArtifactMapping(deltaAssociation.get(), action, actionArtifactRepository);
            log.debug("Artifact found with delta version for action {} of the software module {} with source version {} and " +
                            "target version: {}", action.getId(), deltaAssociation.get().getSoftwareModule().getId(),
                    deltaAssociation.get().getSourceVersion().getId(), deltaAssociation.get().getTargetVersion().getId());
            return getDDiSoftwareArtifactWithArtifactLinks(
                    target, artifactUrlHandler, deltaAssociation.get(), systemManagement, request);
        }

        // Fallback to FULL artifact match
        Optional<ArtifactSoftwareModuleAssociation> fullAssociation = module.getArtifactSoftwareModuleAssociations().stream()
                .filter(association -> {
                    Artifacts artifact = association.getArtifact();
                    return artifact != null &&
                            artifact.getFileName() != null &&
                            !artifact.getFileName().isEmpty() &&
                            isArtifactActive(artifact) &&
                            artifact.getFileType() == FileType.FULL &&
                            rolloutTargetVersion != null &&
                            rolloutTargetVersion.equals(association.getTargetVersion().getName()) &&
                            !isArtifactExpired(artifact, nowEpochSeconds) &&
                            artifact.getFileStatus().equals(FileTransferStatus.CDN_UPLOAD_SUCCESSFUL);
                })
                .findFirst();

        fullAssociation.ifPresent(artifactSoftwareModuleAssociation -> saveActionArtifactMapping(artifactSoftwareModuleAssociation, action, actionArtifactRepository));
        log.info("Artifact found with Full version for action {}", action.getId());
        return fullAssociation
                .map(assoc -> getDDiSoftwareArtifactWithArtifactLinks(target, artifactUrlHandler, assoc, systemManagement, request))
                .orElse(null);
    }

    /**
     * Persists the association between a software module and its artifacts for a given action.
     * This method creates and saves `JpaActionArtifact` entities for the provided artifact association
     * and links them to the specified action.
     *
     * @param artifactSoftwareModuleAssociation the association between the software module and its artifact
     * @param action                            the action to associate with the artifact
     * @param actionArtifactRepository          the repository used to save the `JpaActionArtifact` entities
     */
    private static void saveActionArtifactMapping(ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation, Action action,
                                                  ActionArtifactRepository actionArtifactRepository) {
        log.debug("Saving artifact association for action ID: {} and artifact ID: {}",
                action.getId(),
                artifactSoftwareModuleAssociation.getArtifact().getId());
        JpaActionArtifact jpaActionArtifact = new JpaActionArtifact();
        jpaActionArtifact.setAction((JpaAction) action);
        jpaActionArtifact.setArtifact((JpaArtifacts) artifactSoftwareModuleAssociation.getArtifact());
        actionArtifactRepository.save(jpaActionArtifact);
        log.info("Successfully saved artifact association for action ID: {} and artifact ID: {}",
                action.getId(),
                artifactSoftwareModuleAssociation.getArtifact().getId());
    }

    private static void setDDiSoftwareArtifactTargetAndSourceVersions(ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation,
                                                                      DdiSoftwareArtifact ddiSoftwareArtifact) {
        if (artifactSoftwareModuleAssociation.getArtifact().getFileType().equals(FileType.DELTA)) {
            ddiSoftwareArtifact.setSourceVersion(artifactSoftwareModuleAssociation.getSourceVersion().getName());
        }
        ddiSoftwareArtifact.setTargetVersion(artifactSoftwareModuleAssociation.getTargetVersion().getName());
    }

    private static DdiSoftwareArtifact getDDiSoftwareArtifactWithArtifactLinks(final Target target,
                                                                               final ArtifactUrlHandler artifactUrlHandler,
                                                                               final ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation,
                                                                               final SystemManagement systemManagement,
                                                                               final HttpRequest request) {
        DdiSoftwareArtifact ddiSoftwareArtifact = createDdiSoftwareArtifact(artifactSoftwareModuleAssociation);
        addArtifactLinks(ddiSoftwareArtifact, artifactUrlHandler, systemManagement, artifactSoftwareModuleAssociation, target, request);
        return ddiSoftwareArtifact;
    }

    private static void addArtifactLinks(final DdiSoftwareArtifact ddiSoftwareArtifact,
                                         final ArtifactUrlHandler artifactUrlHandler,
                                         final SystemManagement systemManagement,
                                         final ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation,
                                         Target target, HttpRequest request) {
        Artifacts artifact = artifactSoftwareModuleAssociation.getArtifact();
        artifactUrlHandler
                .getUrls(new URLPlaceholder(systemManagement.getTenantMetadata().getTenant(), DdiRestConstants.ARTIFACT,
                                systemManagement.getTenantMetadata().getId(), target.getControllerId(), target.getId(),
                                new SoftwareData(artifactSoftwareModuleAssociation.getSoftwareModule().getId(), artifact.getFileName(), artifact.getId(),
                                        artifact.getSha256Hash(), artifact.getSha256Hash().replaceAll(DIVIDE_64_BIT_HASH_IN_2_DIGIT, "$1/"))),
                        ApiType.DDI, request.getURI())
                .forEach(entry -> ddiSoftwareArtifact.add(Link.of(entry.getRef()).withRel(entry.getRel()).expand()));
    }


    private static DdiSoftwareArtifact createDdiSoftwareArtifact(ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation) {
        DdiSoftwareArtifact ddiSoftwareArtifact = new DdiSoftwareArtifact();
        Artifacts artifact = artifactSoftwareModuleAssociation.getArtifact();
        ddiSoftwareArtifact.setFilename(artifact.getFileName());
        ddiSoftwareArtifact.setSize((long) artifact.getFileSize().toString().getBytes().length);
        ddiSoftwareArtifact.setArtifactType(ArtifactType.valueOf(artifact.getFileType().toString()));
        ddiSoftwareArtifact.setHashes(new DdiDDArtifactHash(artifact.getSha256Hash(), artifact.getMd5Hash()));
        ddiSoftwareArtifact.setExpiryDate(artifact.getExpiryDate());
        setDDiSoftwareArtifactTargetAndSourceVersions(artifactSoftwareModuleAssociation, ddiSoftwareArtifact);
        return ddiSoftwareArtifact;
    }

    /**
     * Checks if the given artifact is ACTIVE.
     *
     * @param artifact the artifact to check
     * @return true if the artifact's status is "ACTIVE", false otherwise
     */
    private static boolean isArtifactActive(Artifacts artifact) {
        if (artifact == null || artifact.getArtifactStatus() == null) {
            return false;
        }
        try {
            return ArtifactsStatus.ACTIVE.equals(ArtifactsStatus.valueOf(artifact.getArtifactStatus()));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    static List<IDistributionSetModule> fetchDistributionSetModules(long distributionSetId, DistributionSetManagement distributionSetManagement) {
        return distributionSetManagement.getDistributionSetModule(distributionSetId);
    }

    /**
     * Checks if the given artifact is expired based on its signature expiry date.
     * <p>
     * If the artifact has a non-null expiry date and it is before or equal to the current epoch time,
     * the artifact is considered expired. In such cases, this method logs the expiration event
     * and sends a Kafka message to notify about the exclusion from DD generation.
     *
     * @param artifact        the artifact to be validated
     * @param nowEpochSeconds the current UTC time in epoch seconds
     * @return {@code true} if the artifact is expired and excluded from processing; {@code false} otherwise
     */
    static boolean isArtifactExpired(Artifacts artifact, long nowEpochSeconds) {
        if (artifact.getExpiryDate() != null && artifact.getExpiryDate() <= nowEpochSeconds) {
            log.debug("Processing artifact to send message: {} with expiry date: {}", artifact.getFileName(), artifact.getExpiryDate());
            log.info("Skipping expired artifact '{}' (expired at epoch: {})",
                    artifact.getFileName(), artifact.getExpiryDate());
            List<String> errorMessages = List.of("Artifact excluded from DD generation due to expired signature");
            FileDeleteErrorMessage fileDeleteErrorMessage = FileDeleteErrorMessage.builder()
                    .type(ERROR)
                    .fileId(artifact.getId())
                    .fileName(artifact.getFileName())
                    .status(EXPIRED)
                    .errorMessages(errorMessages)
                    .timestamp(Instant.now().atZone(ZoneId.of("UTC")).toEpochSecond())
                    .build();

            KafkaEventHeader header = KafkaEventHeader.builder()
                    .tenant(artifact.getTenant())
                    .fileType(ARTIFACT_FILETYPE)
                    .build();
            log.debug("Kafka Event Header For Expired Artifact: {}", header);

            KafkaEventTemplate eventTemplate = KafkaEventTemplate.builder()
                    .header(header)
                    .payload(fileDeleteErrorMessage)
                    .build();
            log.debug("Kafka Event Payload For Expired Artifact: {}", header);

            kafkaMessageService.sendKafkaEventWithType(eventTemplate, org.cosmos.kafka.utils.Constants.FILE_DELETE_ERROR);
            log.debug("Expired Artifact notification sent to DOCG for artifact ID: {}", artifact.getId());

            return true;
        }
        return false;
    }
}
