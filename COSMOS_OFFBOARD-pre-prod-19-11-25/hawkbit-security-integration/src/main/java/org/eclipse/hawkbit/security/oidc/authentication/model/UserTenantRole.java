package org.eclipse.hawkbit.security.oidc.authentication.model;

import lombok.Builder;
import lombok.Data;

/**
 * User, Tenant and Role model class.
 */
@Data
@Builder
public class UserTenantRole {
    private String env;
    private String role;
    private String tenant;
}