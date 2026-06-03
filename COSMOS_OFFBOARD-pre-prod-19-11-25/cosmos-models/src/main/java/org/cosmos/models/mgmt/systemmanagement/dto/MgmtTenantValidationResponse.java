package org.cosmos.models.mgmt.systemmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Model representation of a tenant validation response as json.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Response object containing a list of valid tenants")
public class MgmtTenantValidationResponse {

    @Schema(description = "List of valid tenants retrieved from database")
    private List<MgmtValidTenant> validTenants;
}
