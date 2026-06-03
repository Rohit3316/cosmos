package org.cosmos.models.mgmt.artifacts.constants;

public enum ArtifactsStatus {

    /**
     * The artifact is active and available for use.
     */

    ACTIVE,

    /**
     * The artifact has been purged and is no longer available.
     */

    PURGED,

    /**
     * The artifact has been deleted and is permanently removed.
     */

    DELETED,

    /**
     * The artifact has been replaced by another artifact.
     */

    REPLACED



}
