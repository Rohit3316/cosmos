package org.cosmos.models.mgmt.rolloutgroup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.hateoas.RepresentationModel;

import java.util.List;

/**
 *
 * Model representation of response body for Add Device Details API
 *
 */
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MgmtAddDeviceDetailsResponse extends RepresentationModel<MgmtAddDeviceDetailsResponse> {
    @Schema(description = "Unique identifier of the response", example = "12345")
    /** Unique identifier of the response. */
    private Long id;

    @Schema(description = "Total number of groups", example = "3")
    /** Total number of groups. */
    private int totalGroups;

    @Schema(description = "Tag name associated with the device details", example = "production")
    /** Tag name associated with the device details. */
    private String tagName;

    @Schema(description = "List of group details")
    /** List of group details. */
    private List<Group> groups;

    @Schema(description = "List of unregistered target device identifiers", example = "[\"19UYA31581L000283_TF1163520J02283\", \"19UYA31581L000301_TF1163520J02301\"]")
    /** List of unregistered target device identifiers. */
    private List<String> unregisteredTargetDevices;

    @Schema(description = "List of duplicate target device identifiers", example = "[\"19UYA31581L000658_TF1163520J02658\"]")
    /** List of duplicate target device identifiers. */
    private List<String> duplicateTargetDevices;

    /**
     * Subclass to represent grouping details in the response body for Add Device Details API.
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Group {
        @Schema(description = "Unique identifier of the group", example = "101")
        /** Unique identifier of the group. */
        private Long id;

        @Schema(description = "Name of the group", example = "Group Alpha")
        /** Name of the group. */
        private String name;

        @Schema(description = "List of target device identifiers in the group", example = "[\"19UYA31581L000633_TF1163520J02633\", \"19UYA31581L000654_TF1163520J02654\"]")
        /** List of target device identifiers in the group. */
        private List<String> targetDevices;

        @Schema(description = "Total number of target devices in the group", example = "2")
        /** Total number of target devices in the group. */
        private int totalTargets;
    }
}

