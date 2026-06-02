/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * Managed filter entity.
 * 
 * Supported operators.
 * <ul>
 * <li>{@code Equal to : ==}</li>
 * <li>{@code Not equal to : !=}</li>
 * <li>{@code Less than : =lt= or <}</li>
 * <li>{@code Less than or equal to : =le= or <=}</li>
 * <li>{@code Greater than operator : =gt= or >}</li>
 * <li>{@code Greater than or equal to : =ge= or >=}</li>
 * </ul>
 * Examples of RSQL expressions in both FIQL-like and alternative notation:
 * <ul>
 * <li>{@code version==2.0.0}</li>
 * <li>{@code name==targetId1;description==plugAndPlay}</li>
 * <li>{@code name==targetId1 and description==plugAndPlay}</li>
 * <li>{@code name==targetId1;description==plugAndPlay}</li>
 * <li>{@code name==targetId1 and description==plugAndPlay}</li>
 * <li>{@code name==targetId1,description==plugAndPlay,updateStatus==UNKNOWN}</li>
 * <li>{@code name==targetId1 or description==plugAndPlay or updateStatus==UNKNOWN}</li>
 * </ul>
 *
 */
public interface TargetFilterQuery extends TenantAwareBaseEntity {
    /**
     * Maximum length of query filter string.
     */
    int QUERY_MAX_SIZE = 1024;

    /**
     * Allowed values for auto-assign user acceptance required
     */
    Set<MgmtRolloutUserAcceptanceRequired> ALLOWED_AUTO_ASSIGN_USER_ACCEPTANCE_REQUIRED = Collections
            .unmodifiableSet(EnumSet.of(MgmtRolloutUserAcceptanceRequired.NO, MgmtRolloutUserAcceptanceRequired.YES));

    /**
     * @return name of the {@link TargetFilterQuery}.
     */
    String getName();

    /**
     * @return RSQL query
     */
    String getQuery();

    /**
     * @return the auto assign {@link DistributionSet} if given.
     */
    DistributionSet getAutoAssignDistributionSet();

    /**
     * @return the auto assign {@link MgmtRolloutUserAcceptanceRequired} if given.
     */
    MgmtRolloutUserAcceptanceRequired getAutoAssignUserAcceptanceRequired();

    /**
     * @return the weight of the {@link Action}s created during an auto
     *         assignment.
     */
    Optional<Integer> getAutoAssignWeight();

    /**
     * @return the user that triggered the auto assignment
     */
    String getAutoAssignInitiatedBy();

    /**
     * @return if confirmation is required for configured auto assignment
     *         (considered with confirmation flow active)
     */
    boolean isConfirmationRequired();
}
