package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import lombok.experimental.UtilityClass;
import org.cosmos.models.mgmt.FileType;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactSoftwareModuleAssociate;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifacts;
import org.cosmos.models.mgmt.artifacts.dto.MgmtArtifactsHash;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for mapping {@link Artifacts} entities to {@link MgmtArtifacts} DTOs.
 */
@UtilityClass
public class MgmtArtifactsMapper {

    /**
     * Maps an {@link Artifacts} entity to a {@link MgmtArtifacts} DTO.
     *
     * @param artifact the artifact entity to map
     * @return the mapped DTO, or null if input is null
     */
    public MgmtArtifacts toResponse(final Artifacts artifact) {

        MgmtArtifacts.MgmtArtifactsBuilder builder = MgmtArtifacts.builder()
                .artifactId(artifact.getId())
                .filename(artifact.getFileName())
                .fileType(artifact.getFileType())
                .description(artifact.getDescription())
                .signatureExpiryDate(artifact.getExpiryDate())
                .fileSize(artifact.getFileSize() != null ? String.valueOf(artifact.getFileSize()) : null)
                .softwareModules(mapSoftwareModules(artifact.getArtifactSoftwareModuleAssociations(), artifact.getFileType()))
                .hashes(MgmtArtifactsHash.builder()
                        .sha256(artifact.getSha256Hash())
                        .md5(artifact.getMd5Hash())
                        .build());
        MgmtArtifacts artifactRest = builder.build();
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
    Set<MgmtArtifactSoftwareModuleAssociate> mapSoftwareModules(final Set<ArtifactSoftwareModuleAssociation> artifactSoftwareModuleAssociations,
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

    private static MgmtArtifactSoftwareModuleAssociate createNewMgmtArtifactSoftwareModuleAssociate(
            final ArtifactSoftwareModuleAssociation association, final FileType fileType) {
        return new MgmtArtifactSoftwareModuleAssociate(association.getSoftwareModule().getId(), null,
                FileType.FULL.equals(fileType) ? new HashSet<>() : null, null);
    }

    private static Set<String> setSourceVersionsForFull(Set<String> sourceVersionsForFull, String sourceVersion) {
        if (sourceVersionsForFull == null)
            sourceVersionsForFull = new HashSet<>();
        sourceVersionsForFull.add(sourceVersion);
        return sourceVersionsForFull;
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

    MgmtArtifactSoftwareModuleAssociate toResponse(final ArtifactSoftwareModuleAssociation artifactSoftwareModuleAssociation,
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



    /**
     * Maps a list of {@link Artifacts} entities to a list of {@link MgmtArtifacts} DTOs.
     *
     * @param artifacts the list of artifact entities
     * @return the list of mapped DTOs
     */
    public List<MgmtArtifacts> mapToMgmtArtifactsResponse(List<Artifacts> artifacts) {
        return toMgmtArtifactsResponse(artifacts);
    }

    /**
     * Maps a list of {@link Artifacts} entities to a list of {@link MgmtArtifacts} DTOs.
     *
     * @param artifacts the list of artifact entities
     * @return the list of mapped DTOs
     */
    public List<MgmtArtifacts> toMgmtArtifactsResponse(List<Artifacts> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return Collections.emptyList();
        }
        return artifacts.stream()
                .map(MgmtArtifactsMapper::toResponse)
                .toList();
    }

    /**
     * Maps software module associations to DTOs.
     * Placeholder for actual mapping logic.
     *
     * @param associations the associations to map
     * @param fileType     the file type
     * @return the mapped software modules DTOs
     */
    private <T> List<T> mapSoftwareModules(List<T> associations, FileType fileType) {
        // TODO: Implement actual mapping logic based on your domain model
        return associations == null ? Collections.emptyList() : associations;
    }
}
