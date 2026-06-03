package org.eclipse.hawkbit.repository.jpa.builder;

import org.eclipse.hawkbit.repository.ArtifactsManagement;
import org.eclipse.hawkbit.repository.builder.ArtifactsBuilder;
import org.eclipse.hawkbit.repository.builder.ArtifactsCreate;

public class JpaArtifactsBuilder implements ArtifactsBuilder {

    private final ArtifactsManagement artifactsManagement;

    /**
     * @param artifactsManagement Artifacts management
     */
    public JpaArtifactsBuilder(ArtifactsManagement artifactsManagement) {
        this.artifactsManagement = artifactsManagement;
    }

    @Override
    public ArtifactsCreate create() {
        return new JpaArtifactsCreate(artifactsManagement);
    }

}
