/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.Rollout;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Builder interface to update an existing {@link Rollout} entry. This defines
 * all fields that can be modified in a {@link Rollout} entity.
 */
public interface RolloutUpdate {

    /**
     * Set the name of the {@link Rollout}.
     *
     * @param name
     *            the new name for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate name(@Size(min = 1, max = NamedEntity.NAME_MAX_SIZE) @NotNull String name);

    /**
     * Set the description of the {@link Rollout}.
     *
     * @param description
     *            the new description for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate description(@Size(max = NamedEntity.DESCRIPTION_MAX_SIZE) String description);

    /**
     * Set the ID of the {@link DistributionSet} associated with the {@link Rollout}.
     *
     * @param setId
     *            the ID of the {@link DistributionSet}
     * @return updated builder instance
     */
    RolloutUpdate set(long setId);

    /**
     * Set the {@link MgmtRolloutUserAcceptanceRequired} of the {@link Rollout}.
     *
     * @param userAcceptanceRequired
     *            the {@link -MgmtRolloutUserAcceptanceRequired} for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate userAcceptanceRequired(@NotNull MgmtRolloutUserAcceptanceRequired userAcceptanceRequired);

    /**
     * Set the connectivity type of the {@link Rollout}.
     *
     * @param connectivityType
     *            the {@link MgmtRolloutConnectivityType} for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate connectivityType(@NotNull MgmtRolloutConnectivityType connectivityType);

    /**
     * Set the forced time for the {@link Rollout}.
     *
     * @param forcedTime
     *            the forced time for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate forcedTime(Long forcedTime);

    /**
     * Set the weight of {@link Action}s created by the {@link Rollout}.
     *
     * @param weight
     *            the weight for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate weight(Integer weight);

    /**
     * Set the start time of the {@link Rollout}.
     *
     * @param startAt
     *            the start time for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate startAt(Long startAt);

    /**
     * Set the end time of the {@link Rollout}.
     *
     * @param endAt
     *            the end time for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate endAt(Long endAt);

    /**
     * Set whether log collection is required for the {@link Rollout}.
     *
     * @param logCollectionRequired
     *            whether log collection is required
     * @return updated builder instance
     */
    RolloutUpdate logCollectionRequired(@NotNull Boolean logCollectionRequired);

    /**
     * Set the maximum number of successful VINs for the log of the {@link Rollout}.
     *
     * @param logMaxSuccessVin
     *            the maximum number of successful VINs
     * @return updated builder instance
     */
    RolloutUpdate logMaxSuccessVin(@NotNull Integer logMaxSuccessVin);

    /**
     * Set the maximum number of failed VINs for the log of the {@link Rollout}.
     *
     * @param logMaxFailureVin
     *            the maximum number of failed VINs
     * @return updated builder instance
     */
    RolloutUpdate logMaxFailureVin(@NotNull Integer logMaxFailureVin);

    /**
     * Set the maximum size of each log file for the {@link Rollout}.
     *
     * @param logMaxEachFileSize
     *            the maximum size of each log file
     * @return updated builder instance
     */
    RolloutUpdate logMaxEachFileSize(@NotNull Integer logMaxEachFileSize);

    /**
     * Set the maximum total size of all log files for the {@link Rollout}.
     *
     * @param logMaxAllFileSize
     *            the maximum total size of all log files
     * @return updated builder instance
     */
    RolloutUpdate logMaxAllFileSize(@NotNull Integer logMaxAllFileSize);

    /**
     * Set the maximum number of log files for the {@link Rollout}.
     *
     * @param logMaxFile
     *            the maximum number of log files
     * @return updated builder instance
     */
    RolloutUpdate logMaxNumberOfFiles(@NotNull Integer logMaxFile);

    /**
     * Set the retry count for downloading in the {@link Rollout}.
     *
     * @param downloadRetryCount
     *            the retry count for downloads
     * @return updated builder instance
     */
    RolloutUpdate downloadRetryCount(@NotNull Integer downloadRetryCount);

    /**
     * Set the maximum download duration timer for the {@link Rollout}.
     *
     * @param maxDownloadDurationTimer
     *            the maximum download duration timer
     * @return updated builder instance
     */
    RolloutUpdate maxDownloadDurationTimer(@NotNull Integer maxDownloadDurationTimer);

    /**
     * Set the maximum Wi-Fi download duration timer for the {@link Rollout}.
     *
     * @param maxDownloadWifiDurationTimer
     *            the maximum Wi-Fi download duration timer
     * @return updated builder instance
     */
    RolloutUpdate maxDownloadWifiDurationTimer(@NotNull Integer maxDownloadWifiDurationTimer);

    /**
     * Set the maximum cellular download duration timer for the {@link Rollout}.
     *
     * @param maxDownloadCellularDurationTimer
     *            the maximum cellular download duration timer
     * @return updated builder instance
     */
    RolloutUpdate maxDownloadCellularDurationTimer(@NotNull Integer maxDownloadCellularDurationTimer);

    /**
     * Set the required media for the {@link Rollout}.
     *
     * @param requiredMedia
     *            the required media type for the {@link Rollout}
     * @return updated builder instance
     */
    RolloutUpdate requiredMedia(@NotNull MgmtRolloutRequiredMedia requiredMedia);

    /**
     * Set whether downgrade is allowed for the {@link Rollout}.
     *
     * @param downgradeAllowed
     *            whether downgrade is allowed
     * @return updated builder instance
     */
    RolloutUpdate downgradeAllowed(@NotNull MgmtRolloutDowngradeAllowed downgradeAllowed);

    /**
     * Set the required state of charge for the {@link Rollout}.
     *
     * @param requiredStateOfCharge
     *            the required state of charge
     * @return updated builder instance
     */
    RolloutUpdate requiredStateOfCharge(String requiredStateOfCharge);

    /**
     * Set the maximum update time for the {@link Rollout}.
     *
     * @param maxUpdateTime
     *            the maximum update time
     * @return updated builder instance
     */
    RolloutUpdate maxUpdateTime(@NotNull Integer maxUpdateTime);
    /**
     * Set the priority of the {@link Rollout}.
     *
     * @param priority
     *            the {@link MgmtRolloutPriority} for the {@link Rollout}
     * @return updated builder instance
     */

    RolloutUpdate priority(@NotNull MgmtRolloutPriority priority);

    /**
     * Set the start type of the {@link Rollout}.
     *
     * @param startType
     *            the {@link MgmtRolloutStartType} for the {@link Rollout}
     * @return updated builder instance
     */

    RolloutUpdate startType(@NotNull MgmtRolloutStartType startType);
}

