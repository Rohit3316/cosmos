package org.cosmos.models.ddi;

/**
 * This enum represents the different types of artifacts that can be associated with a software module.
 * Each enum value represents a specific type of artifact and is associated with a type name.
 */
public enum ArtifactType {

    /**
     * Delta artifact type. This type is used when only changes (delta) from the previous version are included in the artifact.
     */
    DELTA("delta"),

    /**
     * Full artifact type. This type is used when the complete software module, including all its parts, is included in the artifact.
     */
    FULL("full");

    /**
     * The name of the artifact type.
     */
    private final String artifactType;

    /**
     * Constructor for the ArtifactType enum.
     * @param typeName The name of the artifact type.
     */
    ArtifactType(String typeName){
        this.artifactType = typeName;
    }

    /**
     * Getter for the name of the artifact type.
     * @return The name of the artifact type.
     */
    public String getArtifactType() {
        return artifactType;
    }

    /**
     * Overridden toString method for the ArtifactType enum.
     * @return The name of the artifact type.
     */
    @Override
    public String toString() {
        return artifactType;
    }
}