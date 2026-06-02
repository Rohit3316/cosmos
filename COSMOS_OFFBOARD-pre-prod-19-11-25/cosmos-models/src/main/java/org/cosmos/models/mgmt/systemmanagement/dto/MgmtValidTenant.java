package org.cosmos.models.mgmt.systemmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jdk.jfr.Description;
import lombok.Getter;

/**`
 * Model representation of a valid tenant as json.
 */
@Getter
@Schema(description="Representation of a valid tenant with its name and ID")
public class MgmtValidTenant {

    @Schema(description = "Name of the tenant", example = "tenantA")
    private String tenantName;

    @Schema(description = "tenant ID", example = "101")
    private Long tenantId;

    public MgmtValidTenant(String tenantName, Long tenantId) {
        this.tenantName = tenantName;
        this.tenantId = tenantId;
    }

}
