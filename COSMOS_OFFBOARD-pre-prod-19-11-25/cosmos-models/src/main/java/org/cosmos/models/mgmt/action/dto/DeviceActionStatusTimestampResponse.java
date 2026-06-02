package org.cosmos.models.mgmt.action.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import org.cosmos.models.ddi.DdiDownload;
import org.cosmos.models.mgmt.device.constants.DeviceActionStatus;

/**
 * Response record representing the device action status and its corresponding timestamp for a device action.
 *
 * @param status    the status of the device action (non-null)
 * @param timestamp the timestamp of the status in seconds since epoch (non-null)
 * @param download the download information
 * @param messages  the list of messages associated with the action status
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Jacksonized
@Builder(toBuilder = true)
public record DeviceActionStatusTimestampResponse(
        @NonNull DeviceActionStatus status,
        @NonNull Long timestamp,
        DdiDownload download,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<String> messages
) {
}