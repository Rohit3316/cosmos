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
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.cosmos.models.annotations.ValidEpochTime;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Feedback channel for ConfigData action.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class DdiFeedbackRequestBody {

    @JsonProperty
    @Schema(example = "200")
    @NotNull(message = "Code is mandatory")
    @Positive(message = "Code should be a positive number")
    private final Integer code;

    @JsonProperty
    @Schema(example = "[\"string 1\", \"string 2\"]")
    @NotNull(message = "Details are mandatory")
    @NotEmpty(message = "Details are mandatory")
    private final List<String> details;

    @JsonProperty
    @Schema(example = "ERC")
    @NotNull(message = "Execution is mandatory")
    private ExecutionType execution;

    @JsonProperty
    @Schema(example = "[\"ERR_03920181\", \"ERR_03920182\"]")
    private final List<String> errorCode;

    @ValidEpochTime
    @JsonProperty
    @Schema(example = "1742916946")
    @NotNull(message = "Timestamp is mandatory")
    private final Long timestamp;

    /**
     * constructor.
     *
     * @param code feedback code
     * @param details feedbacks
     * @param execution feedback type
     * @param errorCode error code details
     * @param timestamp date and time of feedback
     */
    @JsonCreator
    public DdiFeedbackRequestBody(@JsonProperty(value = "code") final Integer code,
                                  @JsonProperty(value = "details") final List<String> details,
                                  @JsonProperty(value = "execution") final ExecutionType execution,
                                  @JsonProperty(value = "errorCode") final List<String> errorCode,
                                  @JsonProperty(value = "timestamp") final Long timestamp) {
        this.code = code;
        this.details = details;
        this.execution = execution;
        this.errorCode = errorCode;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DdiFeedbackRequestBody [code=" + code + ", details=" + details
                + ", execution=" + execution + ", errorCode=" + errorCode + ", timestamp=" + timestamp
                +", toString()=" + super.toString() + "]";
    }

}