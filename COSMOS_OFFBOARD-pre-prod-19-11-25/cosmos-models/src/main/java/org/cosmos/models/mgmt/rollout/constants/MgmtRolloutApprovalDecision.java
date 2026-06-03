/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.mgmt.rollout.constants;

/**
 * Enum representing the possible approval decisions for a rollout.
 * <p>
 * This enum provides the different states of approval that a rollout can be in,
 * such as whether it has been approved or denied.
 */
public enum MgmtRolloutApprovalDecision {
    /**
     * Approval decision indicating that the rollout has been granted.
     */
    APPROVED,
    /**
     * Approval decision indicating that the rollout has been denied.
     */
    DENIED

}
