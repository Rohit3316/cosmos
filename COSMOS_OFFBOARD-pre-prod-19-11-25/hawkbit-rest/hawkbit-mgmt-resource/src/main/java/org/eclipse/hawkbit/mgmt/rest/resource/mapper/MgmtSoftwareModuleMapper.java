/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.MgmtRestConstants;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactSoftwareModuleAssociate;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsHash;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtAssignedSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModule;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleMetadata;
import org.cosmos.models.mgmt.softwaremodule.dto.MgmtSoftwareModuleRequestBodyPost;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleRestApi;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtSoftwareModuleTypeRestApi;
import org.eclipse.hawkbit.mgmt.rest.resource.MgmtSoftwareModuleResource;
import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleCreate;
import org.eclipse.hawkbit.repository.builder.SoftwareModuleMetadataCreate;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.IDistributionSetModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleMetadata;
import org.eclipse.hawkbit.repository.model.Version;
import org.eclipse.hawkbit.rest.data.ResponseList;
import org.springframework.hateoas.server.mvc.WebMvcLinkBuilder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtSoftwareModuleMapper {
    private MgmtSoftwareModuleMapper() {
        // Utility class
    }

    private static SoftwareModuleCreate fromRequest(final EntityFactory entityFactory,
                                                    final MgmtSoftwareModuleRequestBodyPost smsRest) {
        return entityFactory.softwareModule().create().type(smsRest.getType().toLowerCase()).name(smsRest.getName())
                .format(smsRest.getFormat())
                .version(smsRest.getVersion()).description(smsRest.getDescription()).vendor(smsRest.getVendor())
                .encrypted(smsRest.isEncrypted()).swInstallerType(smsRest.getSwInstallerType());
    }

    public static List<SoftwareModuleMetadataCreate> fromRequestSwMetadata(final EntityFactory entityFactory,
                                                                    final Long softwareModuleId, final Collection<MgmtSoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream()
                .map(metadataRest -> entityFactory.softwareModuleMetadata().create(softwareModuleId)
                        .key(metadataRest.getKey()).value(metadataRest.getValue())
                        .targetVisible(metadataRest.isTargetVisible()))
                .toList();
    }

    public static List<SoftwareModuleCreate> smFromRequest(final EntityFactory entityFactory,
                                                    final Collection<MgmtSoftwareModuleRequestBodyPost> smsRest) {
        if (smsRest == null) {
            return Collections.emptyList();
        }

        return smsRest.stream().map(smRest -> fromRequest(entityFactory, smRest)).toList();
    }

    public static List<MgmtSoftwareModule> toResponse(Long tenantId, final Collection<SoftwareModule> softwareModules) {
        if (softwareModules == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                softwareModules.stream().map(module -> MgmtSoftwareModuleMapper.toResponse(tenantId, module)).toList());
    }

    public static List<MgmtAssignedSoftwareModule> toAssignedResponse(Long tenantId, final Collection<IDistributionSetModule> distributionSetModules, ArtifactsManagement artifactsManagement) {
        if (distributionSetModules == null) {
            return Collections.emptyList();
        }

        return new ResponseList<>(
                distributionSetModules.stream().map(dsModule -> MgmtSoftwareModuleMapper.toAsignedResponse(tenantId, dsModule, artifactsManagement)).toList());
    }

    public static List<MgmtSoftwareModuleMetadata> toResponseSwMetadata(final Collection<SoftwareModuleMetadata> metadata) {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return metadata.stream().map(MgmtSoftwareModuleMapper::toResponseSwMetadata).toList();
    }

    public static MgmtSoftwareModuleMetadata toResponseSwMetadata(final SoftwareModuleMetadata metadata) {
        final MgmtSoftwareModuleMetadata metadataRest = new MgmtSoftwareModuleMetadata();
        metadataRest.setKey(metadata.getKey());
        metadataRest.setValue(metadata.getValue());
        metadataRest.setTargetVisible(metadata.isTargetVisible());
        return metadataRest;
    }

    public static MgmtSoftwareModule toResponse(Long tenantId, final SoftwareModule softwareModule) {
        if (softwareModule == null) {
            return null;
        }

        final MgmtSoftwareModule response = new MgmtSoftwareModule();
        MgmtRestModelMapper.mapNamedToNamed(response, softwareModule);
        response.setModuleId(softwareModule.getId());
        response.setType(softwareModule.getType().getKey());
        response.setFormat(softwareModule.getFormat().getKey());
        response.setTypeName(softwareModule.getType().getName());
        response.setVendor(softwareModule.getVendor());
        response.setDeleted(softwareModule.isDeleted());
        response.setEncrypted(softwareModule.isEncrypted());
        response.setSwInstallerType(softwareModule.getSoftwareInstallerType().getName());

        // set associated ECU Models of the software module
        response.setEcuModels(MgmtEcuModelMapper.toMgmtSoftwareEcuModelResponse(softwareModule.getAssociatedEcuModels()));

        // Map each ArtifactSoftwareModuleAssociation to a MgmtArtifacts DTO and set to response
        response.setArtifacts(
                softwareModule.getArtifactSoftwareModuleAssociations().stream()
                        .map(association -> toCreateResponse(association.getArtifact()))
                        .toList()
        );

        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(tenantId, response.getModuleId()))
                .withSelfRel().expand());

        return response;
    }


    public static MgmtAssignedSoftwareModule toAsignedResponse(Long tenantId, final IDistributionSetModule dsModule, ArtifactsManagement artifactsManagement) {
        if (dsModule == null) {
            return null;
        }

        final MgmtAssignedSoftwareModule response = new MgmtAssignedSoftwareModule();
        MgmtRestModelMapper.mapNamedToNamed(response, dsModule.getSm());
        response.setModuleId(dsModule.getSm().getId());
        response.setType(dsModule.getSm().getType().getKey());
        response.setFormat(dsModule.getSm().getFormat().getKey());
        response.setTypeName(dsModule.getSm().getType().getName());
        response.setVendor(dsModule.getSm().getVendor());
        response.setDeleted(dsModule.getSm().isDeleted());
        response.setEncrypted(dsModule.getSm().isEncrypted());

        List<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociationList = artifactsManagement.findAssociationsBySoftwareModuleId(dsModule.getSm().getId())
                .orElseThrow(() -> new EntityNotFoundException(String.format("No artifacts found associated with the given software module ID %d", dsModule.getSm().getId())));

        List<Version> sourceVersions = artifactSoftwareModuleAssociationList.stream()
                .filter(association -> association.getTargetVersion().getName().equals(dsModule.getVersion().getName()))
                .map(ArtifactSoftwareModuleAssociation::getSourceVersion)
                .collect(Collectors.toList());

        response.setSoftwareVersionTargetName(dsModule.getVersion().getName());
        response.setSoftwareVersionSourceName(
                sourceVersions.stream().map(Version::getName).toList());

        response.add(linkTo(methodOn(MgmtSoftwareModuleRestApi.class).getSoftwareModule(tenantId, response.getModuleId()))
                .withSelfRel().expand());

        return response;
    }

    public static void addLinks(Long tenantId, final SoftwareModule softwareModule, final MgmtSoftwareModule response) {
        response.add(linkTo(
                methodOn(MgmtSoftwareModuleTypeRestApi.class).getSoftwareModuleType(tenantId, softwareModule.getType().getId()))
                .withRel(MgmtRestConstants.SOFTWAREMODULE_V1_TYPE).expand());

        response.add(WebMvcLinkBuilder.linkTo(methodOn(MgmtSoftwareModuleResource.class).getMetadata(tenantId, response.getModuleId(),
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE,
                MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE, null, null)).withRel("metadata")
                .expand().expand());
    }

    public static MgmtArtifacts toResponse(final Artifacts artifact) {
        final MgmtArtifacts artifactRest = MgmtArtifacts.builder()
                .artifactId(artifact.getId())
                .filename(artifact.getFileName())
                .fileType(artifact.getFileType())
                .description(artifact.getDescription())
                .signatureExpiryDate(artifact.getExpiryDate())
                .fileSize(String.valueOf(artifact.getFileSize()))
                .softwareModules(toResponse(artifact.getArtifactSoftwareModuleAssociations(), artifact.getFileType()))
                .hashes(MgmtArtifactsHash.builder()
                        .sha256(artifact.getSha256Hash()).md5(artifact.getMd5Hash()).build())
                .build();
        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);
        return artifactRest;
    }

    public static MgmtArtifacts toCreateResponse(final Artifacts artifact) {
        final MgmtArtifacts artifactRest = MgmtArtifacts.builder()
                .artifactId(artifact.getId())
                .filename(artifact.getFileName())
                .fileType(artifact.getFileType())
                .description(artifact.getDescription())
                .signatureExpiryDate(artifact.getExpiryDate())
                .fileSize(String.valueOf(artifact.getFileSize()))
                .hashes(MgmtArtifactsHash.builder()
                        .sha256(artifact.getSha256Hash()).md5(artifact.getMd5Hash()).build())
                .build();
        MgmtRestModelMapper.mapBaseToBase(artifactRest, artifact);
        return artifactRest;
    }

    /**
     * Mapper to convert collection of {@link ArtifactSoftwareModuleAssociation} into collection of {@link MgmtArtifactSoftwareModuleAssociate}
     *
     * @param artifactSoftwareModuleAssociations the {@link Artifacts} associations with {@link SoftwareModule}
     * @param fileType                           the {@link Artifacts#getFileType()}
     * @return {@link MgmtArtifactSoftwareModuleAssociate} the set of {@link ArtifactSoftwareModuleAssociation} response
     */
    public static Set<MgmtArtifactSoftwareModuleAssociate> toResponse(final Collection<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociations,
                                                               final FileType fileType) {
        if (artifactSoftwareModuleAssociations == null) {
            return Collections.emptySet();
        }
        Set<MgmtArtifactSoftwareModuleAssociate> associates = new HashSet<>();
        artifactSoftwareModuleAssociations.forEach(association ->
                associates.add(setContainsAssociation(associates, association, fileType))
        );
        return associates;
    }

    /**
     * Mapper to convert {@link ArtifactSoftwareModuleAssociation} into {@link MgmtArtifactSoftwareModuleAssociate}
     *
     * @param associates  the set of {@link ArtifactSoftwareModuleAssociation} response
     * @param association the {@link ArtifactSoftwareModuleAssociation}
     * @param fileType    the {@link Artifacts#getFileType()}
     * @return {@link MgmtArtifactSoftwareModuleAssociate} the {@link ArtifactSoftwareModuleAssociation} response
     */
    private static MgmtArtifactSoftwareModuleAssociate setContainsAssociation(Set<MgmtArtifactSoftwareModuleAssociate> associates,
                                                                              ArtifactSoftwareModuleAssociation association, final FileType fileType) {
        return associates.stream()
                .filter(associate -> associate.getSoftwareModuleId().equals(association.getSoftwareModule().getId()))
                .findFirst().map(associate -> toResponse(association, associate, fileType))
                .orElse(toResponse(association, createNewMgmtArtifactSoftwareModuleAssociate(association, fileType), fileType));
    }

    private static MgmtArtifactSoftwareModuleAssociate createNewMgmtArtifactSoftwareModuleAssociate(
            final ArtifactSoftwareModuleAssociation association, final FileType fileType) {
        return new MgmtArtifactSoftwareModuleAssociate(association.getSoftwareModule().getId(), null,
                FileType.FULL.equals(fileType) ? new HashSet<>() : null, null);
    }

    public static MgmtArtifactSoftwareModuleAssociate toResponse(final ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation,
                                                          final MgmtArtifactSoftwareModuleAssociate associate, final FileType fileType) {
        associate.setSoftwareModuleId(artifactSoftwareModuleAssociation.getSoftwareModule().getId());
        associate.setTargetVersion(artifactSoftwareModuleAssociation.getTargetVersion().getName());
        if (FileType.DELTA.equals(fileType)) {
            associate.setSourceVersionForDelta(artifactSoftwareModuleAssociation.getSourceVersion().getName());
        } else if (FileType.FULL.equals(fileType) && artifactSoftwareModuleAssociation.getSourceVersion() == null) {
            associate.setSourceVersionsForFull(Collections.emptySet());
        } else if (FileType.FULL.equals(fileType) && !artifactSoftwareModuleAssociation.getTargetVersion().getName()
                .equals(artifactSoftwareModuleAssociation.getSourceVersion().getName())) {
            associate.setSourceVersionsForFull(setSourceVersionsForFull(associate.getSourceVersionsForFull(),
                    artifactSoftwareModuleAssociation.getSourceVersion().getName()));
        }
        return associate;
    }

    private static Set<String> setSourceVersionsForFull(Set<String> sourceVersionsForFull, String sourceVersion) {
        if (sourceVersionsForFull == null)
            sourceVersionsForFull = new HashSet<>();
        sourceVersionsForFull.add(sourceVersion);
        return sourceVersionsForFull;
    }
}
