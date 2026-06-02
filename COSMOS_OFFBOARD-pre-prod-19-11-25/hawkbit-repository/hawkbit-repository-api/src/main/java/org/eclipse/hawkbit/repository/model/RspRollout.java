package org.eclipse.hawkbit.repository.model;

/**
 * This interface represents a relationship between a Rollout Specific Package (Rsp) and a Rollout.
 * It provides methods to retrieve the associated Rsp and Rollout objects.
 */
public interface RspRollout {

    /**
     * Retrieves the Rollout Specific Package (Rsp) associated with this relationship.
     *
     * @return The Rsp object associated with this relationship.
     */
    Rsp getSupportPackage();

    /**
     * Retrieves the Rollout associated with this relationship.
     *
     * @return The Rollout object associated with this relationship.
     */
    Rollout getRollout();
}
