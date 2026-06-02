package org.eclipse.hawkbit.app;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.app.model.DefaultRoleCreator;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.jpa.PermissionRepository;
import org.eclipse.hawkbit.repository.jpa.RoleRepository;
import org.eclipse.hawkbit.repository.jpa.TenantMetaDataRepository;
import org.eclipse.hawkbit.repository.jpa.model.JpaPermission;
import org.eclipse.hawkbit.repository.jpa.model.JpaRole;
import org.eclipse.hawkbit.repository.model.Permission;
import org.eclipse.hawkbit.repository.model.Role;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.test.matcher.EventVerifier;
import org.eclipse.hawkbit.repository.test.util.CleanupTestExecutionListener;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestExecutionListeners;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Feature("Integration Test - Application Runner")
@Story("Insert seed/default data into DB on application start-up")
// Cleaning repository will fire "delete" events. We won't count them to the test execution.
// So, the order execution between EventVerifier and Cleanup is important!
@TestExecutionListeners(listeners = {EventVerifier.class, CleanupTestExecutionListener.class},
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
class ApplicationRunnerOnStartUpTest extends AbstractSecurityTest {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private TenantMetaDataRepository tenantMetaDataRepository;

    @Autowired
    private SystemSecurityContext systemSecurityContext;

    @Test
    void runTest() {
        permissionsInsertionTest();
        rolesInsertionTest();
        tenantInsertionTest();
    }

    private void permissionsInsertionTest() {
        Set<String> permissions = new HashSet<>(SpPermission.getAllAuthorities());
        List<JpaPermission> cosmosPermission = permissionRepository.findAll();
        Set<String> permissionNames = cosmosPermission.stream().map(Permission::getName).collect(Collectors.toSet());
        Assertions.assertEquals(permissions, permissionNames);
    }

    private void rolesInsertionTest() {
        List<DefaultRoleCreator> defaultRoleCreators = ApplicationRunnerOnStartUp.DEFAULT_ROLES;
        List<JpaRole> cosmosRoles = roleRepository.findAll();
        Assertions.assertEquals(defaultRoleCreators.size(), cosmosRoles.size());

        Map<String, DefaultRoleCreator> roleCreatorMap = defaultRoleCreators.stream()
                .collect(Collectors.toMap(DefaultRoleCreator::getName, d -> d));

        Map<String, JpaRole> cosmosRolesMap = cosmosRoles.stream()
                .collect(Collectors.toMap(Role::getName, r -> r));

        List<JpaPermission> permissions = permissionRepository.findAll();

        roleCreatorMap.forEach((name, defaultRoleCreator) -> {

            Assertions.assertTrue(cosmosRolesMap.containsKey(name));

            Set<String> permissionNames;
            if (defaultRoleCreator.isHasAllPermissions()) {
                permissionNames = permissions.stream().map(Permission::getName).collect(Collectors.toSet());
            } else if (defaultRoleCreator.isExcludePermissions()) {
                Set<String> excludePermissions = defaultRoleCreator.getPermissions();
                permissionNames = permissions.stream()
                        .filter(p -> !excludePermissions.contains(p.getName()))
                        .map(Permission::getName).collect(Collectors.toSet());
            } else {
                permissionNames = defaultRoleCreator.getPermissions();
            }

            Set<String> cosmosRolePermissions = cosmosRolesMap.get(name).getPermissions().stream()
                    .map(Permission::getName).collect(Collectors.toSet());

            Assertions.assertEquals(permissionNames, cosmosRolePermissions);
        });
    }

    private void tenantInsertionTest() {
        Set<String> tenants = new HashSet<>(ApplicationRunnerOnStartUp.DEFAULT_TENANTS);
        tenants.forEach(tenant ->
            systemSecurityContext.runAsSystemAsTenant(() -> {
                TenantMetaData cosmosTenants = tenantMetaDataRepository.findByTenantIgnoreCase(tenant);
                Assertions.assertEquals(tenant, cosmosTenants.getTenant());
                return null;
            }, tenant)
        );
    }
}
