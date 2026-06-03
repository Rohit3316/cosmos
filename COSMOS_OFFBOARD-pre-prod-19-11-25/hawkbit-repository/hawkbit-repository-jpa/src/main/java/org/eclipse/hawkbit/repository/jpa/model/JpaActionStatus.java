/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.ActionStatus;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;

/**
 * Entity to store the status for a specific action.
 */
@Table(name = "sp_action_status", indexes = {
        @Index(name = "sp_idx_action_status_02", columnList = "tenant,action,status"),
        @Index(name = "sp_idx_action_status_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "ActionStatus.withMessages", attributeNodes = { @NamedAttributeNode("messages") })
@Entity
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaActionStatus extends AbstractJpaTenantAwareBaseEntity implements ActionStatus {
    private static final int MESSAGE_ENTRY_LENGTH = 512;

    private static final long serialVersionUID = 1L;

    @Column(name = "target_occurred_at", nullable = false, updatable = false)
    private long occurredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "action", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_act_stat_action"))
    @NotNull
    private JpaAction action;

    @Column(name = "status", nullable = false, updatable = false)
    @ObjectTypeConverter(name = "action_status", objectType = DeviceActionStatus.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "STARTING", dataValue = "0"),
            @ConversionValue(objectValue = "RUNNING", dataValue = "1"),
            @ConversionValue(objectValue = "PAUSING", dataValue = "2"),
            @ConversionValue(objectValue = "PAUSED", dataValue = "3"),
            @ConversionValue(objectValue = "RESUMING", dataValue = "4"),
            @ConversionValue(objectValue = "CANCELING", dataValue = "5"),
            @ConversionValue(objectValue = "CANCELED", dataValue = "6"),
            @ConversionValue(objectValue = "RETRYING", dataValue = "7"),
            @ConversionValue(objectValue = "DD_SENT", dataValue = "8"),
            @ConversionValue(objectValue = "DD_ACCEPTED", dataValue = "9"),
            @ConversionValue(objectValue = "DOWNLOAD_STARTED", dataValue = "10"),
            @ConversionValue(objectValue = "DOWNLOAD_IN_PROGRESS", dataValue = "11"),
            @ConversionValue(objectValue = "DOWNLOAD_COMPLETED", dataValue = "12"),
            @ConversionValue(objectValue = "USER_SCHEDULED", dataValue = "13"),
            @ConversionValue(objectValue = "USER_ACCEPTED", dataValue = "14"),
            @ConversionValue(objectValue = "USER_IGNORED", dataValue = "15"),
            @ConversionValue(objectValue = "FINISHING_SUCCESS", dataValue = "16"),
            @ConversionValue(objectValue = "FINISHING_FAILURE", dataValue = "17"),
            @ConversionValue(objectValue = "FINISHED_SUCCESS", dataValue = "18"),
            @ConversionValue(objectValue = "FINISHED_FAILURE", dataValue = "19"),
            @ConversionValue(objectValue = "LOG_UPLOAD_IN_PROGRESS", dataValue = "20"),
            @ConversionValue(objectValue = "LOG_UPLOAD_SUCCESS", dataValue = "21"),
            @ConversionValue(objectValue = "LOG_UPLOAD_FAILURE", dataValue = "22"),
            @ConversionValue(objectValue = "FINISHING_NOT_EXECUTED", dataValue = "23"),
            @ConversionValue(objectValue = "FINISHED_NOT_EXECUTED", dataValue = "24"),
            @ConversionValue(objectValue = "CANCELED_ACCEPT", dataValue = "25"),
            @ConversionValue(objectValue = "CANCELED_REJECT", dataValue = "26"),
            @ConversionValue(objectValue = "ERROR_RESPONSE_CODE", dataValue = "27"),
            @ConversionValue(objectValue = "PENDING_LOGS", dataValue = "28")
    })
    @Convert("action_status")
    @NotNull
    private DeviceActionStatus status;

    @CascadeOnDelete
    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "sp_action_status_messages", joinColumns = @JoinColumn(name = "action_status_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_stat_msg_act_stat"), updatable = false, nullable = false), indexes = {
            @Index(name = "sp_idx_action_status_msgs_01", columnList = "action_status_id") })
    @Column(name = "detail_message", length = MESSAGE_ENTRY_LENGTH, nullable = false, updatable = false)
    private List<String> messages;

    @Column(name = "code", nullable = true, updatable = false)
    private Integer code;

    @Column(name = "error_code", updatable = false)
    private String errorCode;

    @Column(name = "userAcceptanceMessageJob1", updatable = false)
    private String userAcceptanceMessageJob1;

    @Column(name = "download_progress", nullable = true, updatable = false)
    private String downloadProgress;

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action
     *            the action for this action status
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     */
    public JpaActionStatus(final Action action, final DeviceActionStatus status, final long occurredAt) {
        this.action = (JpaAction) action;
        this.status = status;
        this.occurredAt = occurredAt;
    }

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param action
     *            the action for this action status
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     * @param message
     *            the message which should be added to this action status
     */
    public JpaActionStatus(final JpaAction action, final DeviceActionStatus status, final long occurredAt, final String message) {
        this.action = action;
        this.status = status;
        this.occurredAt = occurredAt;
        addMessage(message);
    }

    /**
     * Creates a new {@link ActionStatus} object.
     *
     * @param status
     *            the status for this action status
     * @param occurredAt
     *            the occurred timestamp
     */
    public JpaActionStatus(final DeviceActionStatus status, final long occurredAt) {
        this.status = status;
        this.occurredAt = occurredAt;
    }

    /**
     * JPA default constructor.
     */
    public JpaActionStatus() {
        // JPA default constructor.
    }

    @Override
    public long getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(final long occurredAt) {
        this.occurredAt = occurredAt;
    }

    public final void addMessage(final String message) {
        if (message != null) {
            if (messages == null) {
                messages = new ArrayList<>((message.length() / MESSAGE_ENTRY_LENGTH) + 1);
            }
            Splitter.fixedLength(MESSAGE_ENTRY_LENGTH).split(message).forEach(messages::add);
        }
    }

    public List<String> getMessages() {
        if (messages == null) {
            messages = Collections.emptyList();
        }

        return Collections.unmodifiableList(messages);
    }

    @Override
    public Action getAction() {
        return action;
    }

    public void setAction(final Action action) {
        this.action = (JpaAction) action;
    }

    @Override
    public DeviceActionStatus getStatus() {
        return status;
    }

    public void setStatus(final DeviceActionStatus status) {
        this.status = status;
    }

    public Optional<Integer> getCode() {
        return Optional.ofNullable(code);
    }

    public void setCode(final Integer code) {
        this.code = code;
    }

    @Override
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(final String errorCode) {
        this.errorCode = errorCode;
    }

    @Override
    public String getUserAcceptanceMessageJob1() {
        return userAcceptanceMessageJob1;
    }

    public void setUserAcceptanceMessageJob1(final String userAcceptanceMessageJob1) {
        this.userAcceptanceMessageJob1 = userAcceptanceMessageJob1;
    }

    public String getDownloadProgress() {
        return downloadProgress;
    }

    public void setDownloadProgress(final String downloadProgress) {
        this.downloadProgress = downloadProgress;
    }
}
