package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.cosmos.models.mgmt.target.dto.MgmtPollStatus;

/**
 *
 * This class represents the response structure for target actions in a management rollout.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MgmtRolloutTargetActionsResponse {

    /**
     * The id of the target.
     */
    @JsonProperty(required = true)
    private Long targetId;

    /**
     * The VIN of the vehicle.
     */
    @JsonProperty(required = true)
    private String vin;

    /**
     * The id of the action.
     */
    @JsonProperty
    private Long actionId;

    /**
     * The serial number of the vehicle.
     */
    @JsonProperty
    private String serialNumber;

    /**
     * The controller id of the vehicle.
     */
    @JsonProperty
    private String controllerId;

    /**
     * The vehicle module id.
     */
    @JsonProperty
    private Long vehicleModuleId;

    /**
     * The polling status of the vehicle.
     */
    @JsonProperty
    private MgmtPollStatus pollingStatus;

    /**
     * The list of device action statuses & device feedback received.
     */
    @JsonProperty
    private List<String> deviceActionStatus;

    /**
     * The download progress of the package.
     */
    @JsonProperty
    private Integer downloadProgress;

    /**
     * The error code if any.
     */
    @JsonProperty
    private String errorCode;

    /**
     * The action status of the actionId of the target.
     */
    @JsonProperty
    private String actionStatus;
}

