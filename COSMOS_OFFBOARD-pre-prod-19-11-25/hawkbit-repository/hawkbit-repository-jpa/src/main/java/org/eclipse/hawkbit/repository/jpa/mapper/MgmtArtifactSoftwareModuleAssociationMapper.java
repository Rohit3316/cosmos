package org.eclipse.hawkbit.repository.jpa.mapper;

import org.eclipse.hawkbit.repository.jpa.model.JpaArtifactSoftwareModuleAssociationEntity;
import org.eclipse.hawkbit.repository.jpa.model.JpaArtifacts;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaVersion;
import org.eclipse.hawkbit.repository.model.ArtifactSoftwareModuleAssociation;
import org.eclipse.hawkbit.repository.model.Artifacts;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.Version;

/**
 * A mapper for artifact software module association
 */
public class MgmtArtifactSoftwareModuleAssociationMapper {
    private MgmtArtifactSoftwareModuleAssociationMapper() {
    }

    /**
     * Creates a new {@link ArtifactSoftwareModuleAssociation} entity based on the provided parameters.
     *
     * @param artifact      the {@link Artifacts} entity associated with the association
     * @param sourceVersion the {@link Version} entity representing the source version of the association
     * @param targetVersion the {@link Version} entity representing the target version of the association
     * @param module        the {@link SoftwareModule} entity representing the software module associated with the association
     * @return a new {@link ArtifactSoftwareModuleAssociation} entity built from the provided parameters
     */
    public static ArtifactSoftwareModuleAssociation toArtifactSoftwareModuleAssociationEntity(Artifacts artifact, Version sourceVersion, Version targetVersion, SoftwareModule module) {
        return JpaArtifactSoftwareModuleAssociationEntity.builder()
                .softwareModule((JpaSoftwareModule) module)
                .sourceVersion((JpaVersion) sourceVersion)
                .artifact((JpaArtifacts) artifact)
                .targetVersion((JpaVersion) targetVersion)
                .build();
    }
}
