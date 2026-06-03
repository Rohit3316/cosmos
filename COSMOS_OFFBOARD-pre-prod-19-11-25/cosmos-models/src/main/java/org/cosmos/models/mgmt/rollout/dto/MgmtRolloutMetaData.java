package org.cosmos.models.mgmt.rollout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing metadata for a management rollout.
 * <p>
 * Contains information about the parent rollout that can be cloned.
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor

public class MgmtRolloutMetaData {
    /**
     * The ID of the parent rollout that is cloneable.
     */
    private String cloneableParentRolloutId;
}
