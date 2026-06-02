package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 * DTO representing details of a management action, including its status, activity state,
 * error codes, and timestamp information.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MgmtActionDetails {
    /**
     * The unique identifier for the action.
     */
    @JsonProperty
    private Long actionId;

    /**
     * The current status of the action.
     */
    @JsonProperty
    private String actionStatus;

    /**
     * The activity state of the action.
     */
    @JsonProperty
    private boolean active;

    /**
     * The error code if any.
     */
    @JsonProperty
    private List<String> errorCode;

    /**
     * The timestamp of the action.
     */
    @JsonProperty
    private Long timestamp;


}
