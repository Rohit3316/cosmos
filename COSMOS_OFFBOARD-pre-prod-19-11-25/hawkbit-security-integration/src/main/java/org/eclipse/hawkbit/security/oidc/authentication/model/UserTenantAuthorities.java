package org.eclipse.hawkbit.security.oidc.authentication.model;

import lombok.Builder;
import lombok.Data;
import org.eclipse.hawkbit.repository.model.User;
import org.springframework.security.core.GrantedAuthority;

import java.util.Set;

@Data
@Builder
public class UserTenantAuthorities {
    private User user;
    private Set<String> userTenants;
    private Set<GrantedAuthority> userAuthorities;
}