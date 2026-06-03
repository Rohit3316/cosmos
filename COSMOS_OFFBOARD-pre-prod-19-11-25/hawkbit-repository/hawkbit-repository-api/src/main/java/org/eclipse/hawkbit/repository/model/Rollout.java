/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutConnectivityType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutDowngradeAllowed;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutPriority;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutRequiredMedia;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutStartType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutType;
import org.cosmos.models.mgmt.rollout.constants.MgmtRolloutUserAcceptanceRequired;
import org.cosmos.models.mgmt.rollout.constants.MgmtUpdateAction;
import org.cosmos.models.mgmt.rollout.constants.RetryMode;
import org.cosmos.models.mgmt.rollout.constants.RolloutStatus;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus.Status;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

import java.util.List;
import java.util.Optional;

/**
 * Interface representing the operations and properties for handling software updates
 * in large-scale IoT scenarios involving hundreds of thousands of devices.
 * <p>
 * Rollouts require secure handling of large volumes of devices during creation,
 * continuous monitoring of progress, and the ability to shut down the rollout
 * in case of emergencies. This interface also provides methods for reporting
 * and understanding the progress of a rollout at any given time.
 */
public interface Rollout extends NamedEntity {

    /**
     * Maximum allowed length for the name of the user who approved the rollout.
     */
    int APPROVED_BY_MAX_SIZE = 64;

    /**
     * Maximum allowed length for comments related to the approval decision.
     */
    int APPROVAL_REMARK_MAX_SIZE = 255;

    /**
     * Indicates whether the rollout has been deleted and is retained only for historical purposes.
     *
     * @return <code>true</code> if the rollout is deleted, <code>false</code> otherwise.
     */
    boolean isDeleted();

    /**
     * Retrieves the {@link DistributionSet} that is part of the rollout.
     *
     * @return the {@link DistributionSet} associated with this rollout.
     */
    DistributionSet getDistributionSet();

    /**
     * Returns the RSQL query used to identify the targets involved in this rollout.
     *
     * @return a string representing the target filter query.
     */
    String getTargetFilterQuery();

    /**
     * Retrieves the number of retry attempts allowed for downloads during the rollout.
     *
     * @return the maximum number of retry attempts.
     */
    Integer getDownloadRetryCount();

    /**
     * Retrieves the number of retry attempts allowed for the entire rollout process.
     *
     * @return the maximum number of retry attempts for the rollout.
     */
    Integer getRetryCount();

    /**
     * Retrieves the mode of retrying downloads during the rollout.
     *
     * @return the {@link RetryMode} for download retries.
     */

    RetryMode getLastRetryMode();

    /**
     * Retrieves the maximum allowed download duration during the rollout.
     *
     * @return the maximum download duration in milliseconds.
     */
    Integer getMaxDownloadDurationTimer();

    /**
     * Retrieves the maximum allowed Wi-Fi download duration during the rollout.
     *
     * @return the maximum Wi-Fi download duration in milliseconds.
     */
    Integer getMaxDownloadWifiDurationTimer();

    /**
     * Retrieves the maximum allowed cellular download duration during the rollout.
     *
     * @return the maximum cellular download duration in milliseconds.
     */
    Integer getMaxDownloadCellularDurationTimer();

    /**
     * Retrieves the maximum time allowed for the update process during the rollout.
     *
     * @return the maximum update time in milliseconds.
     */
    Integer getMaxUpdateTime();

    /**
     * The estimated time for updating in seconds.
     *
     * @return Retrieves the estimated time for updating in seconds
     */
    Integer getDeploymentEstimatedUpdateTime();

    /**
     * Retrieves the maximum allowed package size for the rollout.
     *
     * @return the maximum package size in bytes, or <code>null</code> if not set.
     */
    Long getMaxPackageSize();

    /**
     * Retrieves the current status of the rollout.
     *
     * @return the {@link RolloutStatus} of the rollout.
     */
    RolloutStatus getStatus();

    /**
     * Retrieves the type of action being performed during the rollout.
     *
     * @return the {@link MgmtRolloutUserAcceptanceRequired} of the rollout.
     */
    MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired();

    /**
     * Retrieves the connectivity type required for the rollout.
     *
     * @return the {@link MgmtRolloutConnectivityType} of the rollout.
     */
    MgmtRolloutConnectivityType getConnectivityType();

    /**
     * Retrieves the priority level of the rollout.
     *
     * @return the {@link MgmtRolloutPriority} of the rollout.
     */
    MgmtRolloutPriority getPriority();

    /**
     * Returns the time in milliseconds after which the rollout is forced,
     *
     * @return the forced time in milliseconds.
     */
    long getForcedTime();

    /**
     * Retrieves the timestamp for when the rollout should automatically start.
     *
     * @return the start timestamp, or <code>null</code> if no start time is set.
     */
    Long getStartAt();

    /**
     * Retrieves the timestamp for when the rollout should automatically end.
     *
     * @return the end timestamp, or <code>null</code> if no end time is set.
     */
    Long getEndAt();

    /**
     * Retrieves the total number of targets involved in the rollout.
     *
     * @return the total number of {@link Target}s in the rollout.
     */
    long getTotalTargets();

    /**
     * Retrieves the total number of groups created for the rollout.
     *
     * @return the number of {@link RolloutGroup}s created.
     */
    int getRolloutGroupsCreated();

    /**
     * Retrieves the count of targets in each status group.
     *
     * @return an object representing the total target count by {@link Status}.
     */
    TotalTargetCountStatus getTotalTargetCountStatus();

    /**
     * Retrieves the name of the user who approved or denied the rollout.
     *
     * @return the name of the user who made the approval decision.
     */
    String getApprovalDecidedBy();

    /**
     * Retrieves any remarks or comments related to the approval or denial of the rollout.
     *
     * @return the approval remark, or <code>null</code> if none was provided.
     */
    String getApprovalRemark();

    /**
     * Retrieves the weight (priority) assigned to the rollout.
     *
     * @return an {@link Optional} containing the weight, or empty if no weight was set.
     */
    Optional<Integer> getWeight();

    /**
     * Indicates whether log collection is required during the rollout.
     *
     * @return <code>true</code> if log collection is required, <code>false</code> otherwise.
     */
    Boolean isLogCollectionRequired();

    /**
     * Retrieves the maximum number of VINs (Vehicle Identification Numbers) for successful logs.
     *
     * @return the maximum number of VINs for successful logs.
     */
    Integer getLogMaxSuccessVin();

    /**
     * Retrieves the maximum number of VINs for failure logs.
     *
     * @return the maximum number of VINs for failure logs.
     */
    Integer getLogMaxFailureVin();

    /**
     * Retrieves the maximum allowed size for all log files combined during the rollout.
     *
     * @return the maximum combined log file size in bytes.
     */
    Integer getLogMaxAllFileSize();

    /**
     * Retrieves the maximum allowed size for each individual log file during the rollout.
     *
     * @return the maximum size for each log file in bytes.
     */
    Integer getLogMaxEachFileSize();

    /**
     * Retrieves the maximum number of log files allowed during the rollout.
     *
     * @return the maximum number of log files.
     */
    Integer getLogMaxNumberOfFiles();

    /**
     * Retrieves the required media type for the rollout.
     *
     * @return the {@link MgmtRolloutRequiredMedia} for the rollout.
     */
    MgmtRolloutRequiredMedia getRequiredMedia();

    /**
     * Indicates whether downgrading is allowed during the rollout.
     *
     * @return the {@link MgmtRolloutDowngradeAllowed} status of the rollout.
     */
    MgmtRolloutDowngradeAllowed getDowngradeAllowed();

    /**
     * Retrieves the required state of charge for devices participating in the rollout.
     *
     * @return a string representing the required state of charge.
     */
    String getRequiredStateOfCharge();

    /**
     * Retrieves the start type of the rollout, indicating how the rollout is initiated.
     *
     * @return the {@link MgmtRolloutStartType} for the rollout.
     */
    MgmtRolloutStartType getStartType();

    /**
     * Retrieves the actual start date of the rollout.
     *
     * @return the actual start date as a Unix timestamp, or <code>null</code> if the rollout has not started.
     */
    Long getActualStartDate();

    /**
     * Retrieves the type of the rollout.
     *
     * @return the {@link MgmtRolloutType} of the rollout.
     */
    MgmtRolloutType getType();

    /**
     * Retrieves the update action for the rollout.
     * Only applicable when type is AOTA.
     *
     * @return the {@link MgmtUpdateAction} of the rollout, or null if not applicable.
     */
    MgmtUpdateAction getUpdateAction();

    /**
     * Retrieves the list of software versions to uninstall.
     * Only required when updateAction is UNINSTALLSPECIFIC.
     *
     * @return a list of versions to uninstall, or null if not applicable.
     */
    List<String> getUpdateActionUninstallVersion();

    /**
     * Retrieves the ID of the parent rollout from which this rollout was cloned.
     *
     * @return the ID of the clonable parent rollout.
     */
    String getClonableParentRolloutId();

    /**
     * Retrieves the log level for vehicle logs during the rollout.
     *
     * @return the log level for vehicle logs.
     */
    Integer getVehicleLogLevel();

}