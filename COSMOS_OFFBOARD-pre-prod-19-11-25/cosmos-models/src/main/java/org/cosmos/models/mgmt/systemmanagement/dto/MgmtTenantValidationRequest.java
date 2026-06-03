package org.cosmos.models.mgmt.systemmanagement.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Model representation of a tenant validation request as json.
 */
@Setter
@Getter
@Schema(description = "Request object containing a list of tenants to validate")
public class MgmtTenantValidationRequest {

    @NotEmpty
    @Schema(description = "List of tenant names from the operator's JWT", example = "[\"tenantA\", \"tenantB\"]")
    private List<String> tenants;

}
