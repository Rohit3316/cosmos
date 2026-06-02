/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 * <p>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.Collections;
import java.util.List;

/**
 * Details status information concerning the action processing.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class DdiStatus {

    @NotNull
    @Valid
    private ExecutionStatus execution;

    private DdiDownload download;

    @Schema(example = "200")
    private Integer code;

    private List<String> details;

    @Schema(example = "[\"ERR_03920181\", \"ERR_03920182\"]")
    private List<String> errorCode;

    @Schema(example = "1737079413000", required = true)
    private Long timestamp;

    @Schema(example = "ZsGZQ4bvMP4MRjYjEGMGkpCNnm79k0pOW9USF35ti3hxMRioagVS2rk1h9m4uXpK")
    private String inventoryHash;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("userAcceptanceMessage")
    private DdiUserAcceptanceMessage userAcceptanceMessage;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty("userAcceptanceMessageJob1")
    private String userAcceptanceMessageJob1;

    public DdiStatus(final ExecutionStatus execution, final List<String> details) {
        this.execution = execution;
        this.details = details;
    }


    /**
     * Constructor.
     *
     * @param execution status
     * @param download    information
     * @param code      as optional code (can be null)
     * @param details   as optional addition
     */
    public DdiStatus(final ExecutionStatus execution,
                     final DdiDownload download, final Integer code,
                     final List<String> details, final Long timestamp) {
        this.execution = execution;
        this.download = download;
        this.code = code;
        this.errorCode = null;
        this.details = details;
        this.timestamp = timestamp;
    }

    /**
     * Constructor.
     *
     * @param execution status
     * @param download    information
     * @param code      as optional code (can be null)
     * @param errorCode as optional error code (can be null)
     * @param details   as optional addition
     */
    public DdiStatus(final ExecutionStatus execution,
                     final DdiDownload download, final Integer code,
                     final List<String> details, final List<String> errorCode) {
        this.execution = execution;
        this.download = download;
        this.code = code;
        this.details = details;
        this.errorCode = errorCode;
    }

    /**
     * Constructor.
     *
     * @param execution status
     * @param download    information
     * @param code      as optional code (can be null)
     * @param details   as optional addition
     * @param timestamp is used for ordering feebdacks based on timestamp
     */
    @JsonCreator
    public DdiStatus(@JsonProperty("execution") final ExecutionStatus execution,
                     @JsonProperty("download") final DdiDownload download, @JsonProperty("code") final Integer code,
                     @JsonProperty("details") final List<String> details, @JsonProperty("errorCode") final List<String> errorCode, @JsonProperty("timestamp") final Long timestamp) {
        this.execution = execution;
        this.download = download;
        this.code = code;
        this.details = details;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    /**
     * Constructor
     *
     * @param execution             status
     * @param timestamp             timestamp
     * @param download                information
     * @param code                  as optional code (can be null)
     * @param details               as optional addition
     * @param userAcceptanceMessage use acceptance details
     */
    public DdiStatus(final ExecutionStatus execution,
                     final Long timestamp,
                     final DdiDownload download,
                     final Integer code,
                     final List<String> details,
                     final DdiUserAcceptanceMessage userAcceptanceMessage,
                     final String userAcceptanceMessageJob1) {
        this.execution = execution;
        this.timestamp = timestamp;
        this.download = download;
        this.code = code;
        this.errorCode = null;
        this.details = details;
        this.userAcceptanceMessage = userAcceptanceMessage;
        this.userAcceptanceMessageJob1 = userAcceptanceMessageJob1;
    }

    public DdiStatus(ExecutionStatus executionStatus, List<String> messages, String inventoryHash) {
        this.execution = executionStatus;
        this.details = messages;
        this.inventoryHash = inventoryHash;
    }

    /**
     * Constructor
     *
     * @param executionStatus the execution status
     * @param messages        the list of messages
     * @param inventoryHash   the inventory hash
     * @param timestamp       the timestamp
     */
    public DdiStatus(ExecutionStatus executionStatus, List<String> messages, String inventoryHash, Long timestamp) {
        this.execution = executionStatus;
        this.details = messages;
        this.inventoryHash = inventoryHash;
        this.timestamp = timestamp;
    }

    public List<String> getDetails() {
        if (details == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(details);
    }

    @Override
    public String toString() {
        return "Statuses [execution=" + execution + ", download=" + download + ", code="
                + code + ", details=" + details + ", errorCode=" + errorCode + "]";
    }

    /**
     * The element status contains information about the execution of the
     * operation.
     */
    public enum ExecutionStatus {

        /**
         * Execution was suspended from outside.
         */
        CANCELED("canceled"),

        /**
         * DD has been received by device
         */
        DD_ACCEPTED("dd_accepted"),

        /**
         * Artifact download started
         */
        DOWNLOAD_STARTED("download_started"),

        /**
         * Download has started but has not yet finished
         */
        DOWNLOAD_IN_PROGRESS("download_in_progress"),

        /**
         * Update completed successfully
         */
        FINISHED_SUCCESS("finished_success"),

        /**
         * Failed to update
         */
        FINISHED_FAILURE("finished_failure"),

        /**
         * Log upload is in progress.
         */
        LOG_UPLOAD_IN_PROGRESS("log_upload_in_progress"),

        /**
         * Log upload was successful.
         */
        LOG_UPLOAD_SUCCESS("log_upload_success"),

        /**
         * Log upload failed.
         */
        LOG_UPLOAD_FAILURE("log_upload_failure"),
        /**
         * Log upload pending.
         */
        PENDING_LOGS("pending_logs"),
        /**
         * User has scheduled the action
         */
        USER_SCHEDULED("user_scheduled"),
        /**
         * User has accepted the action
         */
        USER_ACCEPTED("user_accepted"),
        /**
         * User has ignored the action
         */
        USER_IGNORED("user_ignored"),
        /**
         * User has error response for the action
         */
        ERROR_RESPONSE_CODE("error_response_code"),

        /**
         * Execution is starting.
         */
        STARTING("starting"),

        /**
         * Execution is retrying after a failure.
         */
        RETRYING("retrying"),

        /**
         * Execution is paused.
         */
        PAUSED("paused"),

        /**
         * Execution is in the process of pausing.
         */
        PAUSING("pausing"),

        /**
         * Execution is resuming after being paused.
         */
        RESUMING("resuming"),

        /**
         * Execution is currently running.
         */
        RUNNING("running"),

        /**
         * Execution is in the process of being canceled.
         */
        CANCELING("canceling"),

        /**
         * Execution finished but was not executed.
         */
        FINISHING_NOT_EXECUTED("finishing_not_executed"),

        /**
         * Execution finished successfully.
         */
        FINISHING_SUCCESS("finishing_success"),

        /**
         * Execution finished with a failure.
         */
        FINISHING_FAILURE("finishing_failure"),

        /**
         * Execution finished but was not executed.
         */
        FINISHED_NOT_EXECUTED("finished_not_executed"),

        /**
         * DD has been sent to the device.
         */
        DD_SENT("dd_sent"),

        /**
         * Execution was canceled and accepted.
         */
        CANCELED_ACCEPT("canceled_accept"),

        /**
         * Execution was canceled and rejected.
         */
        CANCELED_REJECT("canceled_reject"),

        /**
         * Artifact download completed successfully.
         */
        DOWNLOAD_COMPLETED("download_completed");

        @Schema(example = "finished_success")
        private final String name;

        ExecutionStatus(final String name) {
            this.name = name;
        }

        @JsonValue
        public String getName() {
            return name;
        }
    }

}
