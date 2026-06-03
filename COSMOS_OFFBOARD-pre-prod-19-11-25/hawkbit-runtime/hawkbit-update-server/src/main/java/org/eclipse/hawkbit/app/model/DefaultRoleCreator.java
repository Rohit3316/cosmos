package org.eclipse.hawkbit.app.model;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Class to create default roles.
 */
@Builder
@Data
public class DefaultRoleCreator {
    private String name;
    private String description;
    private Set<String> permissions;
    private boolean hasAllPermissions;
    private boolean excludePermissions;
}
