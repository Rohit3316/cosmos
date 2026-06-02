package org.cosmos.models.mgmt.softwaremodule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request body for PUT/POST Software Module Versions.
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MgmtAddVersionResponse {
    @Schema(example = "16")
    private Long id;
}
