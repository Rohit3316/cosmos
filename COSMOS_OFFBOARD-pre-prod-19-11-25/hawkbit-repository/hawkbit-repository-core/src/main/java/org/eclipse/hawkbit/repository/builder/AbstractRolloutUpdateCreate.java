/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
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
import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Optional;

/**
 * Abstract builder class for  updating a rollout.
 * Provides methods to set various properties of a rollout, following the fluent interface pattern.
 *
 * @param <T> the type of the builder subclass
 */
public abstract class AbstractRolloutUpdateCreate<T> extends AbstractNamedEntityBuilder<T> {

    // Rollout-related properties
    protected Long set;  // Distribution set ID of the rollout

    @ValidString
    protected String targetFilterQuery;  // Query for filtering rollout targets

    protected MgmtRolloutUserAcceptanceRequired userAcceptanceRequired;  // Indicates if user acceptance is required
    protected MgmtRolloutConnectivityType connectivityType;  // Connectivity type used in the rollout
    protected MgmtRolloutPriority priority;  // Priority level of the rollout
    protected MgmtRolloutStartType startType;  // Start type of the rollout
    protected Long forcedTime;  // Time for forcing an action
    protected Long startAt;  // Start time of the rollout
    protected Long endAt;  // End time of the rollout

    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    protected Integer weight;  // Weight of the rollout action

    // Log-related properties
    protected Boolean logCollectionRequired;  // Specifies if log collection is required
    protected Integer logMaxSuccessVin;  // Maximum number of successful VINs to log
    protected Integer logMaxFailureVin;  // Maximum number of failed VINs to log
    protected Integer logMaxAllFileSize;  // Maximum size of all log files combined
    protected Integer logMaxEachFileSize;  // Maximum size of each individual log file
    protected Integer logMaxNumberOfFiles;  // Maximum number of log files

    // Download-related properties
    protected Integer downloadRetryCount;  // Number of retries allowed for downloads
    protected Integer maxDownloadDurationTimer;  // Maximum duration for downloads (in seconds)
    protected Integer maxDownloadWifiDurationTimer;  // Maximum Wi-Fi download duration (in seconds)
    protected Integer maxDownloadCellularDurationTimer;  // Maximum cellular download duration (in seconds)

    // Update-related properties
    protected Integer maxUpdateTime;  // Maximum update time for the rollout
    protected MgmtRolloutRequiredMedia requiredMedia;  // Required media type for the rollout
    protected MgmtRolloutDowngradeAllowed downgradeAllowed;  // Specifies if downgrades are allowed
    protected String requiredStateOfCharge;  // Required state of charge for the rollout

    /**
     * Sets the distribution set ID for the rollout.
     *
     * @param set the ID of the distribution set
     * @return this builder instance
     */
    public T set(final long set) {
        this.set = set;
        return (T) this;
    }

    /**
     * Sets the target filter query for the rollout.
     *
     * @param targetFilterQuery the query used to filter rollout targets
     * @return this builder instance
     */
    public T targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = StringUtils.trimWhitespace(targetFilterQuery);
        return (T) this;
    }

    /**
     * Specifies whether user acceptance is required for the rollout.
     *
     * @param userAcceptanceRequired whether user acceptance is required
     * @return this builder instance
     */
    public T userAcceptanceRequired(final MgmtRolloutUserAcceptanceRequired userAcceptanceRequired) {
        this.userAcceptanceRequired = userAcceptanceRequired;
        return (T) this;
    }

    /**
     * Sets the connectivity type for the rollout.
     *
     * @param connectivityType the connectivity type
     * @return this builder instance
     */
    public T connectivityType(final MgmtRolloutConnectivityType connectivityType) {
        this.connectivityType = connectivityType;
        return (T) this;
    }

    /**
     * Sets the priority level of the rollout.
     *
     * @param priority the priority level
     * @return this builder instance
     */
    public T priority(final MgmtRolloutPriority priority) {
        this.priority = priority;
        return (T) this;
    }

    /**
     * Sets the start type of the rollout.
     *
     * @param startType the type of start
     * @return this builder instance
     */
    public T startType(final MgmtRolloutStartType startType) {
        this.startType = startType;
        return (T) this;
    }

    /**
     * Sets the required state of charge for the rollout.
     *
     * @param requiredStateOfCharge the required state of charge
     * @return this builder instance
     */
    public T requiredStateOfCharge(final String requiredStateOfCharge) {
        this.requiredStateOfCharge = requiredStateOfCharge;
        return (T) this;
    }

    /**
     * Specifies whether downgrades are allowed for the rollout.
     *
     * @param downgradeAllowed whether downgrades are allowed
     * @return this builder instance
     */
    public T downgradeAllowed(final MgmtRolloutDowngradeAllowed downgradeAllowed) {
        this.downgradeAllowed = downgradeAllowed;
        return (T) this;
    }

    /**
     * Sets the required media for the rollout.
     *
     * @param requiredMedia the type of required media
     * @return this builder instance
     */
    public T requiredMedia(final MgmtRolloutRequiredMedia requiredMedia) {
        this.requiredMedia = requiredMedia;
        return (T) this;
    }

    /**
     * Sets the forced time for actions in the rollout.
     *
     * @param forcedTime the forced time
     * @return this builder instance
     */
    public T forcedTime(final Long forcedTime) {
        this.forcedTime = forcedTime;
        return (T) this;
    }

    /**
     * Sets the weight used for prioritizing the rollout.
     *
     * @param weight the weight value
     * @return this builder instance
     */
    public T weight(final Integer weight) {
        this.weight = weight;
        return (T) this;
    }

    /**
     * Sets the start time of the rollout.
     *
     * @param startAt the start time in milliseconds since the epoch
     * @return this builder instance
     */
    public T startAt(final Long startAt) {
        this.startAt = startAt;
        return (T) this;
    }

    /**
     * Sets the end time of the rollout.
     *
     * @param endAt the end time in milliseconds since the epoch
     * @return this builder instance
     */
    public T endAt(final Long endAt) {
        this.endAt = endAt;
        return (T) this;
    }

    /**
     * Specifies whether log collection is required.
     *
     * @param logCollectionRequired whether log collection is required
     * @return this builder instance
     */
    public T logCollectionRequired(final Boolean logCollectionRequired) {
        this.logCollectionRequired = logCollectionRequired;
        return (T) this;
    }

    /**
     * Sets the maximum number of successful VIN logs to store.
     *
     * @param logMaxSuccessVin the maximum number of successful VIN logs
     * @return this builder instance
     */
    public T logMaxSuccessVin(final Integer logMaxSuccessVin) {
        this.logMaxSuccessVin = logMaxSuccessVin;
        return (T) this;
    }

    /**
     * Sets the maximum number of failed VIN logs to store.
     *
     * @param logMaxFailureVin the maximum number of failed VIN logs
     * @return this builder instance
     */
    public T logMaxFailureVin(final Integer logMaxFailureVin) {
        this.logMaxFailureVin = logMaxFailureVin;
        return (T) this;
    }

    /**
     * Sets the maximum size of all log files combined.
     *
     * @param logMaxAllFileSize the maximum size in bytes
     * @return this builder instance
     */
    public T logMaxAllFileSize(final Integer logMaxAllFileSize) {
        this.logMaxAllFileSize = logMaxAllFileSize;
        return (T) this;
    }

    /**
     * Sets the maximum size of each individual log file.
     *
     * @param logMaxEachFileSize the maximum size in bytes
     * @return this builder instance
     */
    public T logMaxEachFileSize(final Integer logMaxEachFileSize) {
        this.logMaxEachFileSize = logMaxEachFileSize;
        return (T) this;
    }

    /**
     * Sets the maximum number of log files.
     *
     * @param logMaxNumberOfFiles the maximum number of log files
     * @return this builder instance
     */
    public T logMaxNumberOfFiles(final Integer logMaxNumberOfFiles) {
        this.logMaxNumberOfFiles = logMaxNumberOfFiles;
        return (T) this;
    }

    /**
     * Sets the download retry count.
     *
     * @param downloadRetryCount the number of retries for downloads
     * @return this builder instance
     */
    public T downloadRetryCount(final Integer downloadRetryCount) {
        this.downloadRetryCount = downloadRetryCount;
        return (T) this;
    }

    /**
     * Sets the maximum download duration (in seconds).
     *
     * @param maxDownloadDurationTimer the maximum download time
     * @return this builder instance
     */
    public T maxDownloadDurationTimer(final Integer maxDownloadDurationTimer) {
        this.maxDownloadDurationTimer = maxDownloadDurationTimer;
        return (T) this;
    }

    /**
     * Sets the maximum Wi-Fi download duration (in seconds).
     *
     * @param maxDownloadWifiDurationTimer the maximum Wi-Fi download time
     * @return this builder instance
     */
    public T maxDownloadWifiDurationTimer(final Integer maxDownloadWifiDurationTimer) {
        this.maxDownloadWifiDurationTimer = maxDownloadWifiDurationTimer;
        return (T) this;
    }

    /**
     * Sets the maximum cellular download duration (in seconds).
     *
     * @param maxDownloadCellularDurationTimer the maximum cellular download time
     * @return this builder instance
     */
    public T maxDownloadCellularDurationTimer(final Integer maxDownloadCellularDurationTimer) {
        this.maxDownloadCellularDurationTimer = maxDownloadCellularDurationTimer;
        return (T) this;
    }

    /**
     * Sets the maximum update time (in seconds).
     *
     * @param maxUpdateTime the maximum update time
     * @return this builder instance
     */
    public T maxUpdateTime(final Integer maxUpdateTime) {
        this.maxUpdateTime = maxUpdateTime;
        return (T) this;
    }

    // Getters (wrapped in Optional)
    public Optional<Long> getSet() {
        return Optional.ofNullable(set);
    }

    public Optional<MgmtRolloutUserAcceptanceRequired> getUserAcceptanceRequired() {
        return Optional.ofNullable(userAcceptanceRequired);
    }

    public Optional<MgmtRolloutConnectivityType> getConnectivityType() {
        return Optional.ofNullable(connectivityType);
    }

    public Optional<Long> getForcedTime() {
        return Optional.ofNullable(forcedTime);
    }

    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    public Optional<Long> getStartAt() {
        return Optional.ofNullable(startAt);
    }

    public Optional<Long> getEndAt() {
        return Optional.ofNullable(endAt);
    }

    public Optional<Boolean> isLogCollectionRequired() {
        return Optional.ofNullable(logCollectionRequired);
    }

    public Optional<Integer> getLogMaxSuccessVin() {
        return Optional.ofNullable(logMaxSuccessVin);
    }

    public Optional<Integer> getLogMaxFailureVin() {
        return Optional.ofNullable(logMaxFailureVin);
    }

    public Optional<Integer> getLogMaxAllFileSize() {
        return Optional.ofNullable(logMaxAllFileSize);
    }

    public Optional<Integer> getLogMaxEachFileSize() {
        return Optional.ofNullable(logMaxEachFileSize);
    }

    public Optional<Integer> getLogMaxNumberOfFiles() {
        return Optional.ofNullable(logMaxNumberOfFiles);
    }

    public Optional<Integer> getDownloadRetryCount() {
        return Optional.ofNullable(downloadRetryCount);
    }

    public Optional<Integer> getMaxDownloadDurationTimer() {
        return Optional.ofNullable(maxDownloadDurationTimer);
    }

    public Optional<Integer> getMaxDownloadWifiDurationTimer() {
        return Optional.ofNullable(maxDownloadWifiDurationTimer);
    }

    public Optional<Integer> getMaxDownloadCellularDurationTimer() {
        return Optional.ofNullable(maxDownloadCellularDurationTimer);
    }

    public Optional<MgmtRolloutRequiredMedia> getRequiredMedia() {
        return Optional.ofNullable(requiredMedia);
    }

    public Optional<MgmtRolloutDowngradeAllowed> getDowngradeAllowed() {
        return Optional.ofNullable(downgradeAllowed);
    }

    public Optional<String> getRequiredStateOfCharge() {
        return Optional.ofNullable(requiredStateOfCharge);
    }

    public Optional<Integer> getMaxUpdateTime() {
        return Optional.ofNullable(maxUpdateTime);
    }

    public Optional<MgmtRolloutPriority> getPriority() {
        return Optional.ofNullable(priority);
    }

        public Optional<MgmtRolloutStartType> getStartType() {
       return Optional.ofNullable(startType);
        }
    }


