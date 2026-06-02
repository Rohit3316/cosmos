package org.eclipse.hawkbit.repository.model;

/**
 * This interface represents an association between an artifact and a software module.
 * It contains methods to retrieve the associated artifact, software module, source version, and target version.
 */
public interface ArtifactSoftwareModuleAssociation extends BaseEntity {

    /**
     * Returns the associated artifact.
     *
     * @return the artifact
     */
    Long getId();

    /**
     * Returns the associated artifact.
     *
     * @return the artifact
     */
    Artifacts getArtifact();

    /**
     * Returns the associated software module.
     *
     * @return the software module
     */
    SoftwareModule getSoftwareModule();

    /**
     * Returns the source version of the association.
     *
     * @return the source version
     */
    Version getSourceVersion();

    /**
     * Returns the target version of the association.
     *
     * @return the target version
     */
    Version getTargetVersion();
}
