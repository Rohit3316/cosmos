/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
import org.eclipse.hawkbit.repository.event.remote.RolloutDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.RolloutUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.JpaRolloutMetaData;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Rollout;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TotalTargetCountStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * JPA implementation of a {@link Rollout}.
 */
@Entity
@Table(name = "sp_rollout", uniqueConstraints = @UniqueConstraint(columnNames = {"name",
        "tenant"}, name = "uk_rollout"))
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaRollout extends AbstractJpaNamedEntity implements Rollout, EventAwareEntity {

    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @CascadeOnDelete
    @OneToMany(targetEntity = JpaRolloutGroup.class, fetch = FetchType.LAZY, mappedBy = "rollout")
    private List<JpaRolloutGroup> rolloutGroups;

    @Column(name = "target_filter", length = TargetFilterQuery.QUERY_MAX_SIZE)
    @Size(min = 1, max = TargetFilterQuery.QUERY_MAX_SIZE)
    private String targetFilterQuery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "distribution_set", updatable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rolltout_ds"))
    private JpaDistributionSet distributionSet;

    @Column(name = "status", nullable = false)
    @ObjectTypeConverter(name = "rolloutstatus", objectType = RolloutStatus.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "DRAFT", dataValue = "1"),
            @ConversionValue(objectValue = "DELETING", dataValue = "5"),
            @ConversionValue(objectValue = "FREEZING", dataValue = "2"),
            @ConversionValue(objectValue = "UNFREEZING", dataValue = "4"),
            @ConversionValue(objectValue = "READY", dataValue = "3"),
            @ConversionValue(objectValue = "STARTING", dataValue = "6"),
            @ConversionValue(objectValue = "RUNNING", dataValue = "7"),
            @ConversionValue(objectValue = "PAUSING", dataValue = "8"),
            @ConversionValue(objectValue = "PAUSED", dataValue = "9"),
            @ConversionValue(objectValue = "RESUMING", dataValue = "10"),
            @ConversionValue(objectValue = "CANCELING", dataValue = "11"),
            @ConversionValue(objectValue = "CANCELED", dataValue = "12"),
            @ConversionValue(objectValue = "FINISHING", dataValue = "13"),
            @ConversionValue(objectValue = "FINISHED", dataValue = "14"),
            @ConversionValue(objectValue = "RETRYING", dataValue = "15"),
            @ConversionValue(objectValue = "ERROR", dataValue = "16"),
            @ConversionValue(objectValue = "DELETED", dataValue = "17"),
            @ConversionValue(objectValue = "CLONING", dataValue = "18"),
            @ConversionValue(objectValue = "RETRY", dataValue = "19")
    })
    @Convert("rolloutstatus")
    @NotNull
    private RolloutStatus status = RolloutStatus.DRAFT;

    @Column(name = "last_check")
    private Long lastCheck;

    @Column(name = "download_retry_count")
    private Integer downloadRetryCount;

    @Column(name = "max_download_duration_timer")
    private Integer maxDownloadDurationTimer;

    @Column(name = "max_download_wifi_duration_timer")
    private Integer maxDownloadWifiDurationTimer;

    @Column(name = "max_download_cellular_duration_timer")
    private Integer maxDownloadCellularDurationTimer;

    @Column(name = "max_update_time")
    private Integer maxUpdateTime;

    @Column(name = "deployment_estimated_update_time")
    private Integer deploymentEstimatedUpdateTime;

    @Column(name = "max_package_size", nullable = true)
    private Long maxPackageSize;

    @Column(name = "user_acceptance_required", nullable = false)
    @ObjectTypeConverter(name = "userAcceptanceRequired", objectType = MgmtRolloutUserAcceptanceRequired.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "NO", dataValue = "0"),
            @ConversionValue(objectValue = "YES", dataValue = "1")})
    @Convert("userAcceptanceRequired")
    private MgmtRolloutUserAcceptanceRequired userAcceptanceRequired = MgmtRolloutUserAcceptanceRequired.YES;

    @Column(name = "forced_time")
    private long forcedTime;

    @Column(name = "total_targets")
    private long totalTargets;

    @Column(name = "rollout_groups_created")
    private int rolloutGroupsCreated;

    @Column(name = "approval_decided_by")
    @Size(min = 1, max = Rollout.APPROVED_BY_MAX_SIZE)
    private String approvalDecidedBy;

    @Column(name = "approval_remark")
    @Size(max = Rollout.APPROVAL_REMARK_MAX_SIZE)
    private String approvalRemark;

    @Column(name = "weight")
    @Min(Action.WEIGHT_MIN)
    @Max(Action.WEIGHT_MAX)
    private Integer weight;

    @Column(name = "collection_required")
    private Boolean logCollectionRequired;

    @Column(name = "max_success_vin")
    private Integer logMaxSuccessVin;

    @Column(name = "max_failure_vin")
    private Integer logMaxFailureVin;

    @Column(name = "max_all_file_size")
    private Integer logMaxAllFileSize;

    @Column(name = "max_each_file_size")
    private Integer logMaxEachFileSize;

    @Column(name = "max_number_of_files")
    private Integer logMaxNumberOfFiles;

    @Column(name = "priority", nullable = false)
    @ObjectTypeConverter(name = "priority", objectType = MgmtRolloutPriority.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "REGULAR", dataValue = "0"),
            @ConversionValue(objectValue = "CRITICAL", dataValue = "1"),
            @ConversionValue(objectValue = "URGENT", dataValue = "2")
    })
    @Convert("priority")
    private MgmtRolloutPriority priority = MgmtRolloutPriority.REGULAR;

    @Column(name = "start_type", nullable = false)
    @ObjectTypeConverter(name = "rolloutsStartType", objectType = MgmtRolloutStartType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "MANUAL", dataValue = "0"),
            @ConversionValue(objectValue = "AUTO", dataValue = "1"),
            @ConversionValue(objectValue = "SCHEDULED", dataValue = "2")
    })
    @Convert("rolloutsStartType")
    private MgmtRolloutStartType startType = MgmtRolloutStartType.MANUAL;

    @Column(name = "connectivity_type", nullable = false)
    @ObjectTypeConverter(name = "rolloutsConnectivityType", objectType = MgmtRolloutConnectivityType.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "WIFI_ONLY", dataValue = "0"),
            @ConversionValue(objectValue = "CELLULAR", dataValue = "1"),
            @ConversionValue(objectValue = "WIFI_PREFERRED", dataValue = "2")
    })
    @Convert("rolloutsConnectivityType")
    @NotNull
    private MgmtRolloutConnectivityType connectivityType = MgmtRolloutConnectivityType.WIFI_PREFERRED;

    @Column(name = "start_at")
    private Long startAt;

    @Column(name = "actualStartDate")
    private Long actualStartDate;

    @Column(name = "end_at")
    @NotNull
    private Long endAt;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "required_media", nullable = false)
    @ObjectTypeConverter(name = "requiredMedia", objectType = MgmtRolloutRequiredMedia.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "FROM_CDN", dataValue = "0"),
            @ConversionValue(objectValue = "FROM_USB", dataValue = "1")})
    @Convert("requiredMedia")
    @NotNull
    private MgmtRolloutRequiredMedia requiredMedia = MgmtRolloutRequiredMedia.FROM_CDN;

    @Column(name = "downgrade_allowed", nullable = false)
    @ObjectTypeConverter(name = "downgradeAllowed", objectType = MgmtRolloutDowngradeAllowed.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "NO", dataValue = "0"),
            @ConversionValue(objectValue = "YES", dataValue = "1")})
    @Convert("downgradeAllowed")
    @NotNull
    private MgmtRolloutDowngradeAllowed downgradeAllowed = MgmtRolloutDowngradeAllowed.NO;

    /**
     * Rollout type (FOTA/AOTA)
     */
    @Column(name = "type", nullable = false, length = 4)
    @Enumerated(EnumType.STRING)
    @NotNull
    private MgmtRolloutType type;

    /**
     * Update action for AOTA rollouts: INSTALL, UNINSTALLANY, UNINSTALLSPECIFIC
     */
    @Column(name = "update_action")
    @Enumerated(EnumType.STRING)
    private MgmtUpdateAction updateAction;

    /**
     * Specific versions to uninstall if updateAction = UNINSTALLSPECIFIC
     */
    @Column(name = "update_action_uninstall_version", columnDefinition = "VARCHAR", length = 4096)
    private String updateActionUninstallVersion;


    @Column(name = "required_state_of_charge")
    private String requiredStateOfCharge;

    @Column(name = "retry_count")
    private int retryCount;

    // Add this above the field
    @ObjectTypeConverter(
            name = "retryModeConverter",
            objectType = RetryMode.class,
            dataType = String.class,
            conversionValues = {
                    @ConversionValue(objectValue = "ALL_SUCCEEDED_VEHICLES", dataValue = "ALL_SUCCEEDED_VEHICLES"),
                    @ConversionValue(objectValue = "ALL_FAILED_VEHICLES", dataValue = "ALL_FAILED_VEHICLES"),
                    @ConversionValue(objectValue = "ALL_CANCELED_VEHICLES", dataValue = "ALL_CANCELED_VEHICLES"),
                    @ConversionValue(objectValue = "ALL_NOT_EXECUTED_VEHICLES", dataValue = "ALL_NOT_EXECUTED_VEHICLES"),
                    @ConversionValue(objectValue = "FULL",dataValue = "FULL"),
                    @ConversionValue(objectValue = "INDIVIDUAL_SUCCEEDED_VEHICLES", dataValue = "INDIVIDUAL_SUCCEEDED_VEHICLES"),
                    @ConversionValue(objectValue = "INDIVIDUAL_FAILED_VEHICLES", dataValue = "INDIVIDUAL_FAILED_VEHICLES"),
                    @ConversionValue(objectValue = "INDIVIDUAL_CANCELED_VEHICLES", dataValue = "INDIVIDUAL_CANCELED_VEHICLES"),
                    @ConversionValue(objectValue = "INDIVIDUAL_NOT_EXECUTED_VEHICLES", dataValue = "INDIVIDUAL_NOT_EXECUTED_VEHICLES")
            }
    )
    @Convert("retryModeConverter")
    @Column(name = "last_retry_mode")
    private RetryMode lastRetryMode;

    @OneToMany(mappedBy = "rolloutId", fetch = FetchType.LAZY)
    private List<JpaRolloutMetaData> jpaRolloutMetaDataList;

    @Column(name = "vehicle_log_level")
    private Integer vehicleLogLevel;

    @Transient
    private transient TotalTargetCountStatus totalTargetCountStatus;

    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(DirectToFieldChangeRecord.class::isInstance).map(DirectToFieldChangeRecord.class::cast)
                .toList();

        return changes.stream().anyMatch(changeRecord -> DELETED_PROPERTY.equals(changeRecord.getAttribute())
                && Boolean.parseBoolean(changeRecord.getNewValue().toString()));
    }

    @Override
    public DistributionSet getDistributionSet() {
        return distributionSet;
    }

    public void setDistributionSet(final DistributionSet distributionSet) {
        this.distributionSet = (JpaDistributionSet) distributionSet;
    }

    public List<RolloutGroup> getRolloutGroups() {
        if (rolloutGroups == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(rolloutGroups);
    }

    @Override
    public String getTargetFilterQuery() {
        return targetFilterQuery;
    }

    @Override
    public Integer getDownloadRetryCount() {
        return downloadRetryCount;
    }

    @Override
    public Integer getRetryCount() {
        return retryCount;
    }

    @Override
    public RetryMode getLastRetryMode() {
        return lastRetryMode;
    }

    @Override
    public Integer getMaxDownloadDurationTimer() {
        return maxDownloadDurationTimer;
    }

    @Override
    public Integer getMaxDownloadWifiDurationTimer() {
        return maxDownloadWifiDurationTimer;
    }

    @Override
    public Integer getMaxDownloadCellularDurationTimer() {
        return maxDownloadCellularDurationTimer;
    }

    @Override
    public Long getMaxPackageSize() {
        return maxPackageSize;
    }

    @Override
    public Integer getMaxUpdateTime() {
        return maxUpdateTime;
    }

    @Override
    public Integer getDeploymentEstimatedUpdateTime() {
        return deploymentEstimatedUpdateTime;
    }

    @Override
    public RolloutStatus getStatus() {
        return status;
    }

    public long getLastCheck() {
        return lastCheck;
    }

    public void setLastCheck(final long lastCheck) {
        this.lastCheck = lastCheck;
    }

    @Override
    public Long getStartAt() {
        return startAt;
    }

    @Override
    public Long getEndAt() {
        return endAt;
    }

    @Override
    public MgmtRolloutUserAcceptanceRequired getUserAcceptanceRequired() {
        return userAcceptanceRequired;
    }

    @Override
    public MgmtRolloutConnectivityType getConnectivityType() {
        return connectivityType;
    }

    @Override
    public long getForcedTime() {
        return forcedTime;
    }

    @Override
    public Optional<Integer> getWeight() {
        return Optional.ofNullable(weight);
    }

    @Override
    public long getTotalTargets() {
        return totalTargets;
    }

    @Override
    public int getRolloutGroupsCreated() {
        return rolloutGroupsCreated;
    }

    @Override
    public TotalTargetCountStatus getTotalTargetCountStatus() {
        if (totalTargetCountStatus == null) {
            totalTargetCountStatus = new TotalTargetCountStatus(totalTargets, userAcceptanceRequired);
        }
        return totalTargetCountStatus;
    }

    @Override
    public String toString() {
        return "Rollout [ targetFilterQuery=" + targetFilterQuery + ", distributionSet=" + distributionSet + ", status="
                + status + ", lastCheck=" + lastCheck + ", getName()=" + getName() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new RolloutCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new RolloutUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));

        if (isSoftDeleted(descriptorEvent)) {
            EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutDeletedEvent(getTenant(),
                    getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new RolloutDeletedEvent(getTenant(),
                getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String getApprovalDecidedBy() {
        return approvalDecidedBy;
    }

    @Override
    public String getApprovalRemark() {
        return approvalRemark;
    }

    @Override
    public Boolean isLogCollectionRequired() {
        return logCollectionRequired;
    }

    @Override
    public Integer getLogMaxSuccessVin() {
        return logMaxSuccessVin;
    }

    @Override
    public Integer getLogMaxFailureVin() {
        return logMaxFailureVin;
    }

    @Override
    public Integer getLogMaxAllFileSize() {
        return logMaxAllFileSize;
    }

    @Override
    public Integer getLogMaxEachFileSize() {
        return logMaxEachFileSize;
    }

    @Override
    public Integer getLogMaxNumberOfFiles() {
        return logMaxNumberOfFiles;
    }

    @Override
    public MgmtRolloutType getType() {
        return type;
    }

    @Override
    public MgmtUpdateAction getUpdateAction() {
        return updateAction;
    }

    @Override
    public List<String> getUpdateActionUninstallVersion() {
        if (updateActionUninstallVersion == null || updateActionUninstallVersion.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.stream(updateActionUninstallVersion.split(","))
                .map(String::trim)
                .toList();
    }

    @Override
    public String getClonableParentRolloutId() {
        if (jpaRolloutMetaDataList == null || jpaRolloutMetaDataList.isEmpty()) {
            return  null;
        }
        return jpaRolloutMetaDataList.stream().filter(rolloutMetaData -> "cloneableParentRolloutId".equals(rolloutMetaData.getKey()))
                .findFirst()
                .map(JpaRolloutMetaData::getValue)
                .orElse(null);
    }
}