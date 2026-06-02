package org.cosmos.models.ddi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Provide action history information to the controller as part of response to
 * {@link DdiRootControllerRestApi#getControllerBasedeploymentAction} and
 * {@link DdiRootControllerRestApi#getControllerBaseconfirmationAction}:
 * 1. Current action status at the server; 2. List of messages from action history
 * that were sent to server earlier by the controller using
 * {@link DdiActionFeedback}.
 */

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({"status", "messages"})
public class DdiActionHistory {

    @JsonProperty("status")
    @Schema(example = "RUNNING")

    private final String actionStatus;

    @JsonProperty("messages")
    private final List<String> messages;

    /**
     * Parameterized constructor for creating {@link DdiActionHistory}.
     *
     * @param actionStatus
     *            is the current action status at the server
     * @param messages
     *            is a list of messages retrieved from action history.
     */
    @JsonCreator
    public DdiActionHistory(@JsonProperty("status") final String actionStatus,
                            @JsonProperty("messages") List<String> messages) {
        this.actionStatus = actionStatus;
        this.messages = messages;
    }

    @Override
    public String toString() {
        return "Action history [" + "status=" + actionStatus + ", messages={" + messages.toString() + "}]";
    }
}